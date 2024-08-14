package biouml.plugins.server;

public class ModuleProtocol
{

    public static final String DATABASE_SERVICE = "module.service";
    
    //////////////////////////////////////////////
    // Diagram constants
    //
    
    public static final int DB_MODELE_TYPE_VERSION         = 150;
    
    public static final int DB_MODELE_TYPE_DIAGRAM_TYPES   = 151;
    
    public static final int DB_MODELE_TYPE_CATEGORY        = 152;
    
    public static final int DB_MODELE_TYPE_CHECK_SUPPORT   = 153;
    
    public static final int DB_MODELE_EXTERNAL_COLLECTIONS = 157;
    
    //////////////////////////////////////////////
    // Command keys
    //
    
    /**
     * Argument key, that describe class name
     */
    public static final String KEY_CLASS    = "class";
    
    /**
     * Argument key, that describe diagram name
     */
    public static final String KEY_DIAGRAM  = "diagram";
    
    /**
     * Argument key, that describe diagram type
     */
    public static final String KEY_DIAGRAM_TYPE = "type";
    
    /**
     * Argument key, that describe relative node name
     */
    public static final String KEY_NODE     = "node";
    
    /**
     * Argument key, that describe relative base kernel name
     */
    public static final String KEY_KERNEL   = "kernel";
    
    /**
     * Argument key, that describe search options
     */
    public static final String KEY_OPTIONS  = "options";
    
    /**
     * Argument key, that describe node infos
     */
    public static final String KEY_INFOs  = "infos";
    
}
