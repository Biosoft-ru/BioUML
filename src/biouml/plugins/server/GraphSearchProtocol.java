package biouml.plugins.server;

/**
 * Graph search service constants
 */
public class GraphSearchProtocol
{
    public static final String GRAPH_SEARH_SERVICE = "graphsearch.service";
    
    //////////////////////////////////////////////
    // Search constants
    //
    
    public static final int GRAPH_SEARCH_START                = 501;
    
    public static final int GRAPH_SEARCH_PRIORITY             = 502;
    
    public static final int GRAPH_SEARCH_STATUS               = 503;
    
    public static final int GRAPH_SEARCH_RESULT               = 504;
    
    public static final int GET_DIAGRAM_TYPES                 = 505;
    
    //////////////////////////////////////////////
    // Command keys
    //
    
    /**
     * Argument key, that describes search options
     */
    public static final String KEY_OPTIONS    = "options";
    
    /**
     * Argument key, that describes input elements
     */
    public static final String KEY_ELEMENTS    = "elements";
    
    /**
     * Argument key, that describes search type
     */
    public static final String KEY_SEARCHTYPE    = "searchType";
    
    /**
     * Argument key, that for process ID
     */
    public static final String KEY_ID    = "id";
}
