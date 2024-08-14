package biouml.plugins.server;

import biouml.plugins.server.access.AccessProtocol;

public class SqlEditorProtocol extends AccessProtocol
{

    public static final String SQL_EDITOR_SERVICE = "sqleditor.service";

    //////////////////////////////////////////////
    // SqlEditor constants
    //

    public static final int DB_GET_TABLES = 101;

    public static final int DB_EXECUTE = 102;

    public static final int DB_GET_COLUMNS = 103;

    public static final int DB_GET_TABLES_ONLY = 104;

    //////////////////////////////////////////////
    // SqlEditor arguments
    //
    /**
     * Argument key, that contain query
     */
    public static final String KEY_QUERY = "query";
    /**
     * Argument key, that contain start row number
     */
    public static final String KEY_START = "start";
    /**
     * Argument key, that contain row count
     */
    public static final String KEY_LENGTH = "length";

    public static final String KEY_TABLE = "table";
}