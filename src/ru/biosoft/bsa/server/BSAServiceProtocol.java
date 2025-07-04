package ru.biosoft.bsa.server;

/**
 * BSA service protocol
 */
public class BSAServiceProtocol
{
    public static final String BSA_SERVICE = "bsa.service";

    public static final String KEY_DE = "de";
    public static final String CHR_NAME_MAPPING = "chr_name_mapping";
    public static final String FROM = "from";
    public static final String TO = "to";
    public static final String TRACKS = "tracks";
    public static final String SEQUENCE_NAME = "sequence";
    public static final String POSITION = "pos";
    public static final String LOGSCALE = "logscale";
    public static final String SITE_ID = "site";
    public static final String EXPORT_FORMAT = "format";
    public static final String SEARCH_DIRECTION = "direction";
    public static final String COLORSCHEME = "colorscheme";
    public static final String DISPLAY_MODE = "mode";
    public static final String TAG_COLORS = "tagColors";
    public static final String TARGET_PATH = "targetPath";
    public static final String PROJECT = "project";
    public static final String SEARCH_QUERY = "query";
    public static final String SEARCH_INDEX = "index";
    public static final String TEMPLATE_NAME = "templateName";
    
    public static final String COLORSHEME_BEAN = "beans/bsa/colorscheme";
    public static final String COLORSHEME_NAME = "colorscheme/";
    public static final String VIEWOPTIONS_BEAN = "bsa/viewoptions/";

    //////////////////////////////////////////////
    // Constants
    //

    public static final int DB_TRACK_SITES = 40;
    public static final int DB_SEQUENCE_START = 41;
    public static final int DB_SEQUENCE_LENGTH = 42;
    public static final int DB_SEQUENCE_PART = 43;
    public static final int DB_SEQUENCE_REGION = 46;
    public static final int DB_TRACK_SITE_COUNT = 44;
    public static final int DB_TRACK_SITES_VIEW = 45;
    public static final int DB_TRACK_SITE_POS = 48;
    public static final int DB_SITE_INFO = 47;
    public static final int DB_TRACK_SITE_NEAREST = 49;
    public static final int GET_SCHEME_LEGEND = 53;
    public static final int DB_SEQUENCE_PROPERTIES = 54;
    public static final int DB_SAVE_PROJECT = 55;
    public static final int DB_TRACK_SEARCH = 56;
    public static final int DB_TRACK_INDEX_LIST = 57;
    public static final int CREATE_COMBINED_TRACK = 58;
    public static final int CREATE_GC_TRACK = 59;

}
