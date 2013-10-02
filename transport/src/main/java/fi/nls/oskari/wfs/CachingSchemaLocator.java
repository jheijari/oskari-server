package fi.nls.oskari.wfs;

import java.io.BufferedInputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import fi.nls.oskari.cache.JedisManager;
import fi.nls.oskari.log.LogFactory;
import org.eclipse.xsd.XSDSchema;
import org.eclipse.xsd.util.XSDSchemaLocator;
import org.geotools.xml.Schemas;

import redis.clients.jedis.Jedis;

import fi.nls.oskari.log.Logger;
import fi.nls.oskari.utils.HttpHelper;
import fi.nls.oskari.utils.XMLHelper;

/**
 * XSD Schema loader with caching for Geotools' XML parser
 */
public class CachingSchemaLocator implements XSDSchemaLocator {

	private static final String cacheHashKey = "hSchemas";
	
    private static final Logger log = LogFactory.getLogger(CachingSchemaLocator.class);

	private static Map<String, XSDSchema> cache = new ConcurrentHashMap<String, XSDSchema>();
	private String username;
	private String password;

	/**
	 * Constructs loader
	 * 
	 * @param username
	 * @param password
	 */
	public CachingSchemaLocator(String username, String password) {
		this.username = username;
		this.password = password;
		if(cache.size() == 0) {
			init();
		}
	}
	
	/**
	 * Init cache from jedis
	 */
	public static void init() {
		log.debug("Init schemas");
		Set<String> schemas = JedisManager.hkeys(cacheHashKey);
		Iterator<String> it = schemas.iterator();
		while(it.hasNext()) {
			String field = (String)it.next();
			String str = JedisManager.hget(cacheHashKey, field);
			XSDSchema xsd = XMLHelper.StringToXSDSchema(str);
			cache.put(field, xsd);
		}
	}
	
	/**
	 * Loads schema from given location and caches it
	 * 
	 * @param schema
	 * @param namespaceURI
	 * @param rawSchemaLocationURI
	 * @param resolvedSchemaLocationURI
	 * 
	 * @see org.eclipse.xsd.util.XSDSchemaLocator#locateSchema(org.eclipse.xsd.XSDSchema, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
    public XSDSchema locateSchema(
    		XSDSchema schema, 
    		String namespaceURI, 
    		String rawSchemaLocationURI, 
    		String resolvedSchemaLocationURI) {
		
		String url = rawSchemaLocationURI;
		if (url == null) {
			return null;
		}
		
		XSDSchema foundSchema = cache.get(url);
		if (foundSchema != null) {
			return foundSchema;
		}
		
		try {
			if (url.toLowerCase().startsWith("https")) {
				BufferedInputStream response = HttpHelper.getRequestStream(url, "application/xml", username, password);
				foundSchema = XMLHelper.InputStreamToXSDSchema(response);
			} else {			
				foundSchema = Schemas.parse(url);
			}
		} catch (Exception e) {
	        log.error(e, "Failed to locate Schema '" + url + "'");
		}
	    
		if (foundSchema != null) {
			log.debug("Caching schema", url);

			JedisManager.hset(cacheHashKey, url, XMLHelper.XSDSchemaToString(foundSchema));
			cache.put(url, foundSchema);
		}

		return foundSchema;
	}
}