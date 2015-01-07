
-- add map layer; 
INSERT INTO oskari_maplayer(type, name, url,
            srs_name,
            groupid, 
            minscale, maxscale, 
            url, locale) 
  VALUES('wfslayer', 'rysp_liikennevayla_turku',
        'EPSG:3067',
        'http://opaskartta.turku.fi/TeklaOGCWeb/WFS.ashx',
        
        (select id from oskari_layergroup where locale like '%Turku%'), 
         10000, 1, 
         'wfs', '{fi:{name:"Liikenneväylä - Turku", subtitle:"RYSP - kantakartta"},sv:{name:"Liikenneväylä - Turku", subtitle:"RYSP - kantakartta"},en:{name:"Liikenneväylä - Turku", subtitle:"RYSP - kantakartta"}}'
  );
         

         
-- link to inspire theme; 
INSERT INTO oskari_maplayer_themes(maplayerid, 
                                   themeid) 
  VALUES((SELECT MAX(id) FROM oskari_maplayer), 
         (SELECT id FROM portti_inspiretheme WHERE locale LIKE '%Transport networks%')); 
         
         
-- add template model stuff; 
INSERT INTO portti_wfs_template_model(name, description, type, request_template, response_template) 
VALUES (
    'Turku Liikenneväylä', 'Turku Liikenneväylä PoC', 'mah taip', 
    '/fi/nls/oskari/fe/input/format/gml/krysp/kanta_Liikennevayla_wfs_template.xml', 
    'fi.nls.oskari.fi.rysp.recipe.kanta.RYSP_kanta_Liikennevayla_Parser');          

-- add wfs specific layer data; 
INSERT INTO portti_wfs_layer ( 
    maplayer_id, 
    layer_name, 
    gml_geometry_property, gml_version, gml2_separator, 
    max_features, 
    feature_namespace, 
    properties, 
    feature_type, 
    selected_feature_params, 
    feature_params_locales, 
    geometry_type, 
    selection_sld_style_id, get_map_tiles, get_feature_info, tile_request, wms_layer_id, 
    feature_element, feature_namespace_uri, 
    geometry_namespace_uri, 
    get_highlight_image, 
    wps_params, 
    tile_buffer, 
    job_type, 
    wfs_template_model_id) 
    VALUES ( (select max(id) from oskari_maplayer), 
      'rysp_liikennevayla_turku', 
       'geom', '3.1.1', false, 
       1000, 
       'kanta', 
       '', 
       '{"default" : "*geometry:Geometry,name:String,beginLifespanVersion:String,endLifespanVersion:String"}', 
       '{}', 
       '{}', 
       '2d', 
       NULL, true, true, false, NULL, 
    'Liikennevayla', 'http://www.paikkatietopalvelu.fi/gml/kantakartta', 
    '', 
    true, '{}', '{ "default" : 1, "oskari_custom" : 1}', 
    'oskari-feature-engine', (select max(id) from portti_wfs_template_model)); 
    
-- add wfs layer styles; 
INSERT INTO portti_wfs_layer_style (name,sld_style) VALUES(
    'oskari-feature-engine',
    '/fi/nls/oskari/fe/output/style/krysp/kanta_Liikennevayla.xml'
);

-- link wfs layer styles; 
INSERT INTO portti_wfs_layers_styles (wfs_layer_id,wfs_layer_style_id) VALUES(
    (select max(id) from portti_wfs_layer),
    (select max(id) from portti_wfs_layer_style));
    
