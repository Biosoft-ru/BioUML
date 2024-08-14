package ru.biosoft.server;

/**
 *
 */
public class Connection
{

    /**
     * Status Ok.
     */
    public static final int OK = 0;
    /**
     * Status Error.
     */
    public static final int ERROR = -1;

    /**
     * Disconnected.
     */
    public static final int DISCONNECT = -2;

    /**
     * Symple format.
     */
    public static final int FORMAT_SIMPLE = 2;
    /**
     * Compressed format.
     */
    public static final int FORMAT_GZIP = 4;

    /**
     * Key, that describes current service.
     */
    public static final String KEY_SERVICE = "service";

    /**
     * Key, that describes corrent comand
     */
    public static final String KEY_COMMAND = "command";

    /**
     * Key, that describes data collection
     */
    public static final String KEY_DC = "dc";

    /**
     * Key, that describes data element inside dc
     */
    public static final String KEY_DE = "de";

}
