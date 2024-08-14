package biouml.plugins.lucene;

/**
 * Define common functions and constants for data excahnage between client and server
 * for LuceneService
 */
public class LuceneProtocol
{
    public static final String LUCENE_SERVICE = "lucene.service";

    //////////////////////////////////////////////
    // Common constants
    //

    public static final int DB_TEST_HAVE_LUCENE_DATABASE = 30;

    public static final int DB_LIST_WITH_BUILD_INDEX = 31;

    public static final int DB_GET_INDEXES = 32;

    public static final int DB_SEND_INDEXES = 33;

    public static final int DB_TEST_HAVE_INDEX = 34;

    public static final int DB_CREATE_INDEX = 35;

    public static final int DB_DELETE_INDEX = 36;

    public static final int DB_ADD_TO_INDEX = 37;

    public static final int DB_DELETE_FROM_INDEX = 38;

    public static final int DB_LUCENE_SEARCH = 39;

    public static final int DB_LIST_WITH_PROPERTY = 40;

    public static final int DB_LUCENE_SEARCH_RECURSIVE = 41;

    public static final String KEY_NAME = "name";

    public static final String KEY_INDEXES = "indexes";

    public static final String KEY_QUERY = "query";

    public static final String KEY_FIELDS = "fields";

    public static final String KEY_FORMATTER_PREFIX = "prefix";

    public static final String KEY_FORMATTER_POSTFIX = "postfix";

    public static final String KEY_VIEW = "view";

    public static final String KEY_FROM = "from";

    public static final String KEY_TO = "to";

    public static final String KEY_PROPERTY = "property";

}