package biouml.workbench.module.xml;

public class XmlModuleConstants
{
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_SQL = "SQL";
    
    public static final String DB_DATABASE_ELEMENT =          "dbModule";
    public static final String NAME_ATTR =                  "name";
    public static final String TITLE_ATTR =                 "title";
    public static final String DESCRIPTION_ATTR =           "description";
    public static final String VERSION_ATTR =               "version";
    public static final String TYPE_ATTR =                  "type";
    public static final String DATABASE_TYPE_ATTR =         "databaseType";
    public static final String DATABASE_VERSION_ATTR =      "databaseVersion";
    public static final String DATABASE_NAME_ATTR =         "databaseName";
    
    public static final String JDBC_CONNECTION_ELEMENT =    "jdbcConnection";
    public static final String JDBC_CONNECTION_NAME_ATTR =  "name";
    public static final String JDBC_CONNECTION_DRIVER_ATTR ="jdbcDriverClass";
    public static final String JDBC_CONNECTION_URL_ATTR =   "jdbcURL";
    public static final String JDBC_CONNECTION_USER_ATTR =  "jdbcUser";
    public static final String JDBC_CONNECTION_PASSWORD_ATTR ="jdbcPassword";
    
    public static final String PROPERTIES_ELEMENT =         "properties";
    
    public static final String PROPERTY_ELEMENT =           "property";
    public static final String PROPERTY_NAME_ATTR =         "name";
    public static final String PROPERTY_TYPE_ATTR =         "type";
    public static final String PROPERTY_SHORT_DESCRIPTION_ATTR = "short-description";
    public static final String PROPERTY_VALUE_ATTR =         "value";
    
    public static final String TAGS_ELEMENT =               "tags";
    
    public static final String TAG_ELEMENT =                "tag";
    
    public static final String PROPERTYREF_ELEMENT =        "propertyRef";
    
    public static final String DEPENDENCIES_ELEMENT =       "dependencies";
    
    public static final String EXTERNAL_DATABASE_ELEMENT =    "externalModule";
    public static final String EXTERNAL_DATABASE_NAME_ATTR =  "name";
    
    public static final String EXTERNAL_TYPE_ELEMENT =      "externalType";
    public static final String EXTERNAL_TYPE_NAME_ATTR =    "name";
    public static final String EXTERNAL_TYPE_SECTION_ATTR = "section";
    public static final String EXTERNAL_TYPE_READONLY_ATTR ="readOnly";
    
    public static final String GRAPHIC_NOTATION_ELEMENT =   "graphicNotation";
    public static final String GRAPHIC_NOTATION_NAME_ATTR = "name";
    public static final String GRAPHIC_NOTATION_TYPE_ATTR = "type";
    public static final String GRAPHIC_NOTATION_CLASS_ATTR ="class";
    public static final String GRAPHIC_NOTATION_PATH_ATTR = "path";
    
    public static final String GRAPHIC_NOTATION_TYPE_JAVA = "Java";
    public static final String GRAPHIC_NOTATION_TYPE_XML =  "XML";
    
    public static final String TYPES_ELEMENT =              "types";
    
    public static final String INTERNAL_TYPE_ELEMENT =      "internalType";
    public static final String INTERNAL_TYPE_NAME_ATTR =    "name";
    public static final String INTERNAL_TYPE_SECTION_ATTR = "section";
    public static final String INTERNAL_TYPE_CLASS_ATTR =   "class";
    public static final String INTERNAL_TYPE_TRANSFORMER_ATTR = "transformer";
    public static final String INTERNAL_TYPE_IDFORMAT_ATTR ="id-format";
    
    public static final String QUERY_SYSTEM_ELEMENT =       "querySystem";
    public static final String QUERY_SYSTEM_CLASS_ATTR =    "class";
    public static final String QUERY_SYSTEM_LUCENE_ATTR =   "luceneIndexes";
    
    public static final String INDEX_ELEMENT =              "index";
    public static final String INDEX_ELEMENT_NAME_ATTR =    "name";
    public static final String INDEX_ELEMENT_CLASS_ATTR =   "class";
    public static final String INDEX_ELEMENT_TABLE_ATTR =   "table";
}
