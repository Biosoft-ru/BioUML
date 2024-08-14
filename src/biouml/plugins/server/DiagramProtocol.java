package biouml.plugins.server;

import biouml.plugins.server.access.AccessProtocol;

public class DiagramProtocol extends AccessProtocol
{
    
    public static final String DIAGRAM_SERVICE = "diagram.service";
    
    //////////////////////////////////////////////
    // Diagram constants
    //
    
    public static final int DB_GET_DIAGRAM  = 210;
    
    public static final int DB_GET_DIAGRAM2 = 211;
    
    public static final int DB_PUT_DIAGRAM  = 212;
    
    public static final int GET_DIAGRAM_TYPES  = 213;
    
    public static final int CREATE_DIAGRAM  = 214;
    
    public static final int GET_CONVERT_TYPES  = 215;
    
    public static final int DB_CONVERT_DIAGRAM  = 216;
    
    //////////////////////////////////////////////
    // Command keys
    //
    
    /**
     * Argument key, that describe module name
     */
    public static final String KEY_DATABASE   = "module";
    
    /**
     * Argument key, that describe diagram name
     */
    public static final String KEY_DIAGRAM  = "diagram";
        
    /**
     * Argument key, that describe info entry
     */
    public static final String KEY_INFO     = "info";
    
    /**
     * Argument key, that describe diagram data
     */
    public static final String KEY_DATA     = "data";
    
    /**
     * Argument key, that describe path to Diagram collection
     */
    public static final String KEY_DC       = "dc";
    
    /**
     * Argument key, that describe Diagram type to be created
     */
    public static final String KEY_TYPE       = "type";
    
    /**
     * Argument key, that describe class of diagram types
     */
    public static final String KEY_TYPE_CLASS = "class";
        
}
