package com.developmentontheedge.server;


/**
 * Define common functions and constants for data excahnage between client and server
 * for LuceneService
 */
public class DoteProtocol
{

    public static final String DOTE_SERVICE = "dote.service";

    public static final String LOG4J_SERVICE = "log4j.service";

    //////////////////////////////////////////////
    // Command keys
    //
    /**
     * Argument key, that present JobControl listener id.
     */
    public static final String KEY_LISTENER = "listener";

    /**
     * Argument key, that present current preperedness.
     */
    public static final String KEY_PREPEREDNESS = "preperedness";

    /**
     * Argument key, that present category name.
     */
    public static final String KEY_CATEGORY = "category";

    /**
     * Argument key, that present time.
     */
    public static final String KEY_TIME = "time";

    //////////////////////////////////////////////
    // JobControl constants
    //
    public static final int DB_JOBCONTROL_SET_LISTENER = 30;

    public static final int DB_JOBCONTROL_START = 31;

    public static final int DB_JOBCONTROL_STOP = 32;

    public static final int DB_JOBCONTROL_RESUME = 33;

    public static final int DB_JOBCONTROL_CANCEL = 34;

    public static final int DB_JOBCONTROL_GET_PREPAREDNESS = 35;

    public static final int DB_JOBCONTROL_GET_MESSAGE = 36;

    public static final int DB_JOBCONTROL_GET_STATUS = 37;

    public static final int DB_JOBCONTROL_GET_REMAINEDTIME = 38;

    public static final int DB_JOBCONTROL_GET_ELAPSEDTIME = 39;

    public static final int DB_JOBCONTROL_GET_STARTEDTIME = 40;

    public static final int DB_JOBCONTROL_GET_ENDEDTIME = 41;

    public static final int DB_JOBCONTROL_GET_CREATEDTIME = 42;

    public static final int DB_JOBCONTROL_SET_PREPAREDBESS = 43;

    public static final int DB_JOBCONTROL_GET_EVENTS = 44;


    //////////////////////////////////////////////
    // Log4j constants
    //

    public static final int DB_LOG4J_ADD_LISTENER = 50;

    //   public static final int DB_LOG4J_REMOVE_LISTENER = 51;

    public static final int DB_LOG4J_GET_EVENTS = 52;


}