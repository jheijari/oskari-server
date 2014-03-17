package fi.nls.oskari.control.metadata;

import fi.mml.portti.service.search.SearchCriteria;
import fi.nls.oskari.cache.Cache;
import fi.nls.oskari.cache.CacheManager;
import fi.nls.oskari.domain.SelectItem;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.IOHelper;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.PropertyUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.DataInputStream;
import java.net.HttpURLConnection;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: SMAKINEN
 * Date: 11.3.2014
 * Time: 13:59
 * To change this template use File | Settings | File Templates.
 */
public class MetadataFieldHandler {

    private static final Logger log = LogFactory.getLogger(MetadataFieldHandler.class);

    private final static NodeList EMPTY_NODELIST = new EmptyNodeList();

    private MetadataField field = null;
    private String serverURL = PropertyUtil.get("metadata.catalogue.server", "http://geonetwork.nls.fi");
    private String serverPath = PropertyUtil.get("metadata.catalogue.path", "/geonetwork/srv/en/csw");
    private String queryParams = "?" + PropertyUtil.get("metadata.catalogue.queryParams", "SERVICE=CSW&VERSION=2.0.2&request=GetDomain&PropertyName=");
    private Cache<Set<SelectItem>> cache = CacheManager.getCache(MetadataFieldHandler.class.getCanonicalName());
/*
catalogue.geonetwork.search.url=http://geonetwork.nls.fi/geonetwork/srv/en/csw
catalogue.geonetwork.server.url=http://geonetwork.nls.fi
 */

    public String getPropertyName() {
        return getMetadataField().getProperty();
    }

    public void setMetadataField(final MetadataField field) {
        this.field = field;
    }

    public MetadataField getMetadataField() {
        return field;
    }

    public String getSearchURL() {
        return serverURL + serverPath + queryParams;
    }

    public JSONArray getOptions(final String language) {
        JSONArray values = new JSONArray();
        Set<SelectItem> items = getProperties();
        for(SelectItem item : items) {
            final JSONObject value = JSONHelper.createJSONObject("val", item.getValue());
            JSONHelper.putValue(value, "locale", item.getName(true));
            values.put(value);
        }

        return values;
    }

    public void handleParam(final String param, final SearchCriteria criteria) {
        if(param == null || !param.isEmpty()) {
            // empty param -> skip
            return;
        }
        final MetadataField field = getMetadataField();
        if(field.isMulti()) {
            String[] values = param.split("\\s*,\\s*");
            criteria.addParam(getPropertyName(), values);
        }
        else {
            criteria.addParam(getPropertyName(), param);
        }
    }

    private Set<SelectItem> getProperties() {
        return getProperties(getPropertyName());
    }

    private Set<SelectItem> getProperties(final String propertyName) {
        Set<SelectItem> response = cache.get(propertyName);
        if(response != null) {
            return response;
        }

        response = new TreeSet<SelectItem>();

        final String url = getSearchURL() + propertyName;
        final NodeList valueList = getTags(url, "csw:Value");
        for (int i = 0; i < valueList.getLength(); i++) {
            String value = valueList.item(i).getChildNodes().item(0).getTextContent();
            response.add(new SelectItem(null, value));
        }
        cache.put(propertyName, response);
        return response;
    }

    private static NodeList getTags(final String url, final String tagName) {
        DataInputStream dis = null;
        try {
            final HttpURLConnection con = IOHelper.getConnection(url);
            dis = new DataInputStream(IOHelper.debugResponse(con.getInputStream()));
            final DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final Document doc = dBuilder.parse(dis);
            doc.getDocumentElement().normalize();
            return doc.getElementsByTagName(tagName);
        } catch (Exception e) {
            log.error("Error parsing tags (", tagName, ") from response at", url, ". Message:", e.getMessage());
        }
        finally {
            try {
                dis.close();
            } catch (Exception ignored) {}
        }
        // default to empty NodeList
        return EMPTY_NODELIST;
    }
}