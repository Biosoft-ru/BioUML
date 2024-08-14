package biouml.plugins.server.access;

import java.io.IOException;
import java.util.Map;

import java.util.logging.Logger;

import ru.biosoft.access.core.QuerySystem;

public interface ClientQuerySystem extends QuerySystem
{
    public static final Logger log = Logger.getLogger(ClientQuerySystem.class.getName());


    public static final String QUERY_SERVICE = "query.service";

    //////////////////////////////////////////////
    // Command keys
    //
    /**
     * Argument key, that present root data collection.
     */
    public static final String KEY_DC = "dc";

    /**
     * Argument key, that present index.
     */
    public static final String KEY_INDEX = "index";

    /**
     * Argument key, that present key.
     */
    public static final String KEY_KEY = "key";

    /**
     * Argument key, that present value.
     */
    public static final String KEY_VALUE = "value";

    /**
     * Argument key, that present value.
     */
    public static final String KEY_DATA = "data";

    //////////////////////////////////////////////////////////////
    // QuerySystem constants
    //
    public static final int DB_SET_DC = 151;

    public static final int DB_GET_INDEXES = 152;

    public static final int DB_CLOSE = 153;

    //////////////////////////////////////////////////////////////
    // Index constants
    //

    public static final int DB_INDEX_CLOSE = 160;

    public static final int DB_INDEX_CHECK_VALID = 161;

    public static final int DB_INDEX_GET_SIZE = 162;

    public static final int DB_INDEX_GET_ALL = 163;

    public static final int DB_INDEX_PUT = 164;

    public static final int DB_INDEX_REMOVE = 165;

    public static final int DB_INDEX_PUT_ALL = 166;

    public static final int DB_INDEX_CLEAR = 167;

    ///////////////////////////////////////////////
    // Index excange functions
    //
    public void closeIndex(String name) throws IOException;

    public boolean checkValidIndex(String name) throws IOException;

    public int getIndexSize(String name) throws IOException;

    public Map indexGet(String name) throws IOException, ClassNotFoundException;

    public Object indexPut(String name, Object key, Object value) throws IOException, ClassNotFoundException;

    public Object indexRemove(String name, Object key) throws IOException, ClassNotFoundException;

    public void indexPutAll(String name, Map map) throws IOException;

    public void indexClear(String name) throws IOException;

}
