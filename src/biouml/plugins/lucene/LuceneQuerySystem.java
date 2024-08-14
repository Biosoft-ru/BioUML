package biouml.plugins.lucene;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import javax.annotation.Nonnull;

import java.util.logging.Logger;
import org.apache.lucene.queryparser.classic.ParseException;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.QuerySystemWithIndexRebuilder;
import com.developmentontheedge.beans.DynamicPropertySet;
import ru.biosoft.jobcontrol.JobControl;


public interface LuceneQuerySystem extends QuerySystemWithIndexRebuilder
{
    public static int MAX_DEFAULT_SEARCH_RESULTS_COUNT = 1000;

    public static Formatter DEFAULT_FORMATTER = new Formatter("<B><font color=red>", "</font></B>");

    public static final Logger log = Logger.getLogger( LuceneQuerySystem.class.getName() );

    public static MessageBundle messageBundle = new MessageBundle();


    ///////////////////////////////////////////////////////////////////

    /** Directory name where Lucene indexes will be created. */
    public final static String LUCENE_INDEX_DIRECTORY = "lucene-directory";

    /**
     * String with bean properties that should be indexed by Lucene,
     * ';' is used as delimiter.
     * <p>For example bean has properties name, title, and synonyms
     * that should be indexed. For this purpose config file should
     * contain following string:
     * <pre>lucene-indexes=name;title;synonyms</pre>
     */
    public final static String LUCENE_INDEXES = "lucene-indexes";

    /**
     * Lucene indexes helper class name. Lucene helper provides an
     * access to available property names and to property values for
     * non-typical collections (for example, Diagrams)
     */
    public final static String LUCENE_HELPER = "lucene-helper";

    /**
     * Data collection name field - it should not intersect with other
     * fields in root data collection
     */
    public static final String DATA_COLLECTION_NAME_FIELD = "dc";

    /**
     * Data collection name field - it should not intersect with other
     * fields in root data collection
     */
    public static final String DATA_ELEMENT_RELATIVE_NAME = "relativeName";

    /**
     * Full text field for special default search (use when
     * search performed over all fields)
     */
    public static final String FULL_TEXT_FIELD = "full-text";

    ///////////////////////////////////////////////////////////////////

    /**
     * Return raw indexes string
     * @param relativeName
     * @return
     */
    public String getIndexes(String relativeName);

    /**
     * Set new indexes string
     * @param relativeName
     * @param indexes
     * @throws Exception
     */
    public void setIndexes(String relativeName, String indexes) throws Exception;

    /**
     * Test if this module have lucene directory for creating new lucene index
     * @return
     */
    public boolean testHaveLuceneDir();

    /**
     * Get all data collection which have already build index
     * @return
     * @throws IOException
     */
    public Collection<String> getDCWithBuildIndex() throws IOException;

    /**
     * This function rebuild entire index
     * for module of this data collection
     * @param log
     * @param jobControl
     * @throws IOException
     * @throws ParseException
     */
    public void createIndex(Logger log, JobControl jobControl) throws IOException, ParseException;

    /**
     * Delete entire index
     * @param cat
     * @throws ParseException
     * @throws IOException
     */
    public void deleteIndex(Logger cat) throws IOException, ParseException;

    /**
     * Simple function for adding (or replacing) new data elements to the index
     * @param relativeName - relative name of this data element
     * @param deep TODO
     * @param cat
     * @param jobControl
     * @throws IOException
     * @throws ParseException
     */
    public void addToIndex(String relativeName, boolean deep, Logger cat, JobControl jobControl) throws IOException, ParseException;

    /**
     * Delete data element with specified relative name from index
     * @param relativeName
     * @param cat
     * @throws IOException
     * @throws ParseException
     */
    public void deleteFromIndex(String relativeName, Logger cat) throws IOException, ParseException;

    /**
     * Search function, which can perform search only in
     * particular data collection and return results in general
     * or in alternative view
     * @param relativeName - constraint data collection name
     * @param queryString - query string
     * @param formatter - base external formatter
     * @param alternativeView
     * @return - results of search as array of DynamicPropertySet classes
     * @throws IOException
     * @throws ParseException
     * @throws IntrospectionException
     */
    public DynamicPropertySet[] search(String relativeName, String queryString, Formatter formatter, boolean alternativeView)
            throws IOException, ParseException, IntrospectionException;

    /**
     * Search function, which can perform search in
     * data collection and it's childs
     * @param relativeName - constraint data collection name
     * @param queryString - query string
     * @param formatter - base external formatter
     * @param from
     * @param to
     * @return - results of search as array of DynamicPropertySet classes
     * @throws IOException
     * @throws ParseException
     * @throws IntrospectionException
     */
    public DynamicPropertySet[] searchRecursive(String relativeName, String queryString, Formatter formatter, int from, int to)
            throws IOException, ParseException, IntrospectionException;

    /**
     * Search function, which can perform search only in
     * particular data collection and return results in general
     * or in alternative view
     * @param relativeName - constraint data collection name
     * @param queryString - query string
     * @param formatter - base external formatter
     * @param alternativeView
     * @param from
     * @param to
     * @return - results of search as array of DynamicPropertySet classes
     * @throws IOException
     * @throws ParseException
     * @throws IntrospectionException
     */
    public DynamicPropertySet[] search(String relativeName, String queryString, String[] fields, Formatter formatter,
            boolean alternativeView, int from, int to) throws IOException, ParseException, IntrospectionException;

    /**
     * Search function, which can perform search only in
     * particular data collection, but only in defined fields
     * @param relativeName - constraint data collection name
     * @param queryString - query string
     * @param fields - array of search fields or null for full field search
     * @param formatter - base external formatter
     * @param alternativeView
     * @return - results of search as array of DynamicPropertySet classes
     * @throws IOException
     * @throws ParseException
     * @throws IntrospectionException
     */
    public DynamicPropertySet[] search(String relativeName, String queryString, String[] fields, Formatter formatter,
            boolean alternativeView) throws IOException, ParseException, IntrospectionException;

    /**
     * Special additional function for searching all internal data collections
     * which contain in module, from which origin this data colection
     * @return
     */
    public List<String> getCollectionsNamesWithIndexes();

    /**
     * return instance of data element from it relative name or null
     * @param relativeName
     * @return
     */
    public DataElement getDataElement(String relativeName);

    /**
     * return all properties names
     * @param relativeName
     * @return
     */
    public @Nonnull Vector<String> getPropertiesNames(String relativeName);

    public DataCollection<?> getCollection();

    /**
     * @param term prefix
     * @param relativeName
     * @return list of up to 10 terms starting with given term
     */
    public Collection<String> getSuggestions(String term, String relativeName);
}