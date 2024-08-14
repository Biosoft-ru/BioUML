package biouml.plugins.lucene;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.swing.JFrame;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.KeywordField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermVectors;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.NullFragmenter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;
import com.developmentontheedge.beans.editors.HtmlEditor;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.DefaultQuerySystem;
import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.exception.SearchException;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.TextUtil;

/**
 *
 */
public class LuceneQuerySystemImpl extends DefaultQuerySystem implements LuceneQuerySystem
{
    private static final String SORT_PROPERTY_NAME = "sort";
    private static final Pattern WORD_SYMBOLS = Pattern.compile( "[\\w\\-\\_\\/\\.]+$" );
    private static final int MAX_SUGGESTIONS = 10;
    protected String luceneDir = null;
    protected Analyzer analyzer = new WhitespaceAnalyzer();
    protected DataCollection<?> module = null;

    /**
     * @param dc
     */
    public LuceneQuerySystemImpl(DataCollection<?> dc) throws Exception
    {
        super( dc );

        module = dc;

        if( dc.getInfo().getProperty( LUCENE_INDEX_DIRECTORY ) == null )
            throw new Exception( "Index directory is not specified for LuceneQuerySystem." );

        luceneDir = getLuceneDir();
    }

    private DataCollection<?> getSubCollection(String relativeName)
    {
        DataElement de = CollectionFactory.getDataElement( relativeName, module );
        if( de instanceof DataCollection )
            return (DataCollection<?>)de;
        return null;
    }

    @Override
    public DataCollection<?> getCollection()
    {
        return module;
    }

    protected synchronized String getLuceneDir()
    {
        String path = module.getInfo().getProperties().getProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, "." );
        if( path == null )
            throw new NullPointerException( "Missing " + DataCollectionConfigConstants.CONFIG_PATH_PROPERTY + " field property in data collection "
                    + module.getCompletePath() );
        String dir = module.getInfo().getProperty( LUCENE_INDEX_DIRECTORY );
        if( dir == null )
            throw new NullPointerException( "Missing " + LUCENE_INDEX_DIRECTORY + " field property in data collection "
                    + module.getCompletePath() );

        return path + "/" + dir;
    }

    /*Set indexes directory, can be used from Index Rebuilder*/
    public void setLuceneDir(String dir)
    {
        this.luceneDir = dir;
    }

    @Override
    public boolean testHaveLuceneDir()
    {
        if( luceneDir == null )
            return false;
        else
            return true;
    }

    @Override
    public synchronized String getIndexes(String relativeName)
    {
        DataCollection<?> dc = getSubCollection( relativeName );
        if( dc != null )
        {
            String indexes = dc.getInfo().getProperty( LUCENE_INDEXES );
            if( indexes == null )
                return "";
            if( indexes.trim().length() > 0 )
            {
                if( LuceneUtils.getPropertiesNames( dc ).contains( "name" ) )
                    indexes = LuceneUtils.indexedFields( indexes ).prepend( "name" ).joining( ";" );
                return indexes.trim();
            }
        }
        return "";
    }

    @Override
    public synchronized void setIndexes(String relativeName, String indexes) throws Exception
    {
        DataCollection<?> dc = getSubCollection( relativeName );
        if( dc != null )
        {
            if( indexes == null )
            {
                if( dc.getInfo().getProperty( LUCENE_INDEXES ) != null )
                    dc.getInfo().writeProperty( LUCENE_INDEXES, "" );
            }
            else
                dc.getInfo().writeProperty( LUCENE_INDEXES, indexes );
        }
    }

    @Override
    public synchronized boolean testHaveIndex() throws IOException
    {
        Directory directory = getLuceneDirectory();
        if ( directory != null )
        {
            return DirectoryReader.indexExists(directory);
        }
        return false;
    }

    @Override
    public Collection<String> getSuggestions(String term, String relativeName)
    {
        Matcher matcher = WORD_SYMBOLS.matcher( term );
        if( !matcher.find() )
            return Collections.emptyList();
        String subTerm = term.substring( matcher.start() ).toLowerCase();

        DirectoryReader reader = null;
        Set<String> result = new TreeSet<>();
        try
        {
            Directory directory = getLuceneDirectory();
            if ( directory == null || !DirectoryReader.indexExists(directory) )
            {
                return Collections.emptyList();
            }
            reader = DirectoryReader.open(directory);
            HashSet<String> fields = new HashSet<>();

            Set<String> visited = new HashSet<>();
            int watched = 0;

            StoredFields storedFields = reader.storedFields();
            for ( int i = 0; i < reader.numDocs(); i++ )
            {
                Document doc = storedFields.document(i);
                if ( doc == null )
                    continue;
                String dcName = doc.get(DATA_COLLECTION_NAME_FIELD);
                if ( !dcName.startsWith(relativeName) )
                    continue;

                if ( !visited.contains(dcName) )
                {
                    DataCollection<?> subCollection = getSubCollection(dcName);
                    if ( subCollection != null )
                    {
                        LuceneUtils.indexedFields(subCollection.getInfo().getProperty(LUCENE_INDEXES)).forEach(fields::add);
                        visited.add(dcName);
                    }
                }
                for ( String fieldName : fields )
                {
                    String val = doc.get(fieldName);
                    if ( val != null )
                    {
                        watched++;
                        if ( watched / 500 + result.size() > MAX_SUGGESTIONS )
                            return result;

                        if ( !val.toLowerCase().startsWith(subTerm) )
                            continue;
                        result.add(term.substring(0, matcher.start()) + val);
                        if ( result.size() > MAX_SUGGESTIONS )
                            return result;
                    }
                }
            }
        }
        catch (Exception e)
        {
            return Collections.emptyList();
        }
        finally
        {
            if ( reader != null )
                try
                {
                    reader.close();
                }
                catch (IOException e)
                {
                }
        }

        return result;
    }

    //TODO: check
    @Override
    public Collection<String> getDCWithBuildIndex() throws IOException
    {
        Set<String> nameSet = new HashSet<>();
        if( testHaveIndex() )
        {
            //File index = new File( luceneDir );
            Directory index = FSDirectory.open(Paths.get(luceneDir));
            DirectoryReader reader = DirectoryReader.open(index);
            //IndexReader reader = IndexReader.open( index );
            try

            {
                TermVectors vectors = reader.termVectors();
                for ( int i = 0; i < reader.numDocs(); i++ )
                {
                    Terms fields = vectors.get(i, DATA_COLLECTION_NAME_FIELD);
                    TermsEnum termsEnum = fields.iterator();
                    BytesRef termRef;
                    while ( (termRef = termsEnum.next()) != null )
                    {
                        String dcName = termRef.utf8ToString();
                        if( dcName != null && dcName.length() > 0 )
                            nameSet.add( dcName );
                    }
                }
            }
            finally
            {
                reader.close();
            }
        }
        return nameSet;
    }

    @Override
    public synchronized void createIndex(Logger cat, JobControl jobControl) throws IOException, ParseException
    {
        try
        {
            addToIndex( module, cat, jobControl, true );
        }
        catch( Exception e )
        {
            throw new IOException( e.getMessage() );
        }
    }

    @Override
    public synchronized void addToIndex(String relativeName, boolean deep, Logger cat, JobControl jobControl) throws IOException,
            ParseException
    {
        DataElementPath path = module.getCompletePath().getRelativePath( relativeName );
        DataCollection<? extends DataElement> parent = path.optParentCollection();
        if( DataCollectionUtils.checkPrimaryElementType( parent, FolderCollection.class ) )
        {
            try
            {
                SecurityManager.runPrivileged(() -> {
                    IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
                    OpenMode mode = testHaveIndex() ? OpenMode.CREATE_OR_APPEND : OpenMode.CREATE;
                    iwc.setOpenMode(mode);
                    Directory index = FSDirectory.open(Paths.get(luceneDir));
                    IndexWriter writer = new IndexWriter(index, iwc);

                    try
                    {
                        replaceFolderElementIndex(parent, path.getName(), writer, cat, deep);
                    }
                    finally
                    {
                        writer.flush();
                        writer.close();
                    }
                    return null;
                });
            }
            catch (Exception e)
            {
                if ( cat != null )
                    cat.log(Level.SEVERE, "Indexing failed.", e);
            }
            return;
        }
        DataElement rde = CollectionFactory.getDataElement( relativeName, module );
        if( rde != null )
        {
            addToIndex( rde, cat, jobControl, !testHaveIndex() );
        }
    }

    protected void addToIndex(DataElement de, Logger cat, JobControl jobControl, boolean createNew) throws IOException, ParseException
    {
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        if ( createNew )
        {
            // Create a new index in the directory, removing any
            // previously indexed documents:
            iwc.setOpenMode(OpenMode.CREATE);
        }
        else
        {
            // Add new documents to an existing index:
            iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
        }

        try
        {
            SecurityManager.runPrivileged(() -> {
                Directory dir = FSDirectory.open(Paths.get(luceneDir));
                IndexWriter writer = new IndexWriter(dir, iwc);
                //IndexWriter writer = new IndexWriter(index, analyzer, createNew);
                try
                {
                    if ( de != null )
                    {
                        if ( cat != null )
                            cat.info("Start indexing.");

                        if ( de instanceof DataCollection )
                        {
                            replaceToIndex((DataCollection<?>) de, writer, cat, jobControl);
                        }
                        else
                        {
                            replaceToIndex(de, de.getOrigin(), writer, cat, jobControl);
                        }

                        if ( cat != null )
                            cat.info("Index is ready for use.");
                    }
                }
                catch (IOException ioe)
                {
                    if ( cat != null )
                        cat.log(Level.SEVERE, "Indexing failed.", ioe);
                    throw ioe;
                }
                catch (ParseException pe)
                {
                    if ( cat != null )
                        cat.log(Level.SEVERE, "Indexing failed.", pe);
                    throw pe;
                }
                finally
                {
                    writer.flush();
                    writer.close();
                }
                return null;
            });
        }
        catch (Exception e)
        {
            if ( cat != null )
                cat.log(Level.SEVERE, "Indexing failed.", e);
        }
    }

    private void replaceFolderIndex(DataCollection<?> dc, IndexWriter writer, Logger cat)
    {
        if( cat != null )
        {
            cat.info( "Indexing " + dc.getCompletePath() );
        }
        for( String name : dc.getNameList() )
        {
            try
            {
                replaceFolderElementIndex( dc, name, writer, cat, true );
            }
            catch( Exception e )
            {
                if( cat != null )
                {
                    cat.log(Level.SEVERE, "Unable to index " + dc.getCompletePath().getChildPath( name ) + ": " + e.getMessage() );
                }
            }
        }
        try
        {
            DataCollection<DataElement> parentCollection = dc.getCompletePath().getParentCollection();
            if( DataCollectionUtils.checkPrimaryElementType( parentCollection, FolderCollection.class ) )
            {
                dc.close();
                ( (AbstractDataCollection<?>)parentCollection ).removeFromCache( dc.getName() );
            }
        }
        catch( Exception e )
        {
            ExceptionRegistry.log( e );
        }
    }

    private void replaceFolderElementIndex(DataCollection<?> dc, String name, IndexWriter writer, Logger cat, boolean deep)
            throws CorruptIndexException, IOException
    {
        DataElementPath path = dc.getCompletePath();
        String childPath = path.getChildPath( name ).toString();
        writer.deleteDocuments( new Term( DATA_ELEMENT_RELATIVE_NAME, childPath ) );
        DataElementDescriptor descriptor = dc.getDescriptor( name );
        Document document = new Document();
        document.add(new StringField(DATA_COLLECTION_NAME_FIELD, path.getPathDifference(module.getCompletePath()), Field.Store.YES));
        document.add(new KeywordField(DATA_ELEMENT_RELATIVE_NAME, childPath, Field.Store.YES));
        document.add(new StringField("name", name, Field.Store.YES));
        document.add(new TextField(FULL_TEXT_FIELD, name, Field.Store.NO));
        writer.addDocument( document );

        if( deep && FolderCollection.class.isAssignableFrom( descriptor.getType() ) )
        {
            try
            {
                replaceFolderIndex( (DataCollection<?>)dc.get( name ), writer, cat );
            }
            catch( Exception e )
            {
                if( cat != null )
                {
                    cat.log(Level.SEVERE, "Unable to index " + dc.getCompletePath().getChildPath( name ) + ": " + e.getMessage() );
                }
            }
        }
    }

    protected void replaceToIndex(DataCollection<?> dc, IndexWriter writer, Logger cat, JobControl jobControl) throws IOException
    {
        if( !dc.getCompletePath().equals( module.getCompletePath() ) )
        {
            if( checkBound( dc ) )
            {
                String dcName = CollectionFactory.getRelativeName( dc, module );
                writer.deleteDocuments( new Term( DATA_COLLECTION_NAME_FIELD, dcName ) );
                String indexes = dc.getInfo().getProperty( LUCENE_INDEXES );
                if( indexes != null )
                {
                    doAddDataCollectionToIndex( dc, dcName, writer, indexes, cat, jobControl );
                }
            }
        }

        if( DataCollectionUtils.checkPrimaryElementType( dc, FolderCollection.class ) )
        {
            replaceFolderIndex( dc, writer, cat );
            return;
        }
        if( LuceneUtils.checkIfChildIndexPossible( dc ) )
        {
            for( ru.biosoft.access.core.DataElement de : dc )
            {
                try
                {
                    if( de instanceof DataCollection )
                    {
                        replaceToIndex( (DataCollection<?>)de, writer, cat, jobControl );
                    }
                }
                catch( Exception e )
                {
                    cat.log(Level.SEVERE, "Can not index data collection '" + DataElementPath.create( de ) + "': ", e );
                }
            }
        }
    }

    protected void replaceToIndex(DataElement de, DataCollection<?> dc, IndexWriter writer, Logger cat, JobControl jobControl)
            throws IOException, ParseException
    {
        if( checkBound( dc ) )
        {
            writer.deleteDocuments( new Term( DATA_ELEMENT_RELATIVE_NAME, DataElementPath.create( de ).toString() ) );
            String indexes = de.getOrigin().getInfo().getProperty( LUCENE_INDEXES );
            if( indexes != null && indexes.trim().length() > 0 )
            {
                String dcName = CollectionFactory.getRelativeName( dc, module );
                doAddDataElementToIndex( de, dcName, writer, indexes, cat );
            }
        }
    }

    protected boolean checkBound(DataCollection<?> dc)
    {
        return LuceneUtils.checkIfChildIndexPossible( dc.getOrigin() );
    }

    protected void doAddDataCollectionToIndex(DataCollection<?> dc, String dcName, IndexWriter writer, String indexes, Logger cat,
            JobControl jobControl)
    {
        if( cat != null )
            cat.log(Level.FINE, "Create index for " + dcName + " data collection" );

        int count = 0;
        List<String> names = dc.getNameList();
        int currentProgress = 0;
        for( String name : names )
        {
            count++;
            try
            {
                DataElement de = dc.get( name );
                doAddDataElementToIndex( de, dcName, writer, indexes, cat );
                if( jobControl != null )
                {
                    int progress = ( 100 * count ) / dc.getSize();
                    if( currentProgress != progress )
                    {
                        currentProgress = progress;
                        jobControl.setPreparedness( progress );
                        try
                        {
                            // Sleep for supporting data exchange - because
                            // of server cannot send data during index building
                            Thread.sleep( 1 );
                        }
                        catch( Throwable t )
                        {
                        }
                    }
                    if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
                        return;
                }
            }
            catch( Throwable t )
            {
                if( cat != null )
                {
                    cat.log(Level.SEVERE, "Can not add element '" + DataElementPath.create( dc, name ) + "' to index: " + t.getMessage() );
                }
            }
        }
    }

    protected void doAddDataElementToIndex(DataElement de, String dcName, IndexWriter writer, String indexes, Logger cat)
            throws IOException, ParseException
    {
        if( cat != null && cat.isLoggable( Level.FINE ) )
            cat.log(Level.FINE, "Create index for " + de.getName() + " data element" );
        writer.addDocument(fillDocument(de, dcName, indexes, cat));
    }

    protected Document fillDocument(DataElement de, String dcName, String indexes, Logger cat) throws ParseException
    {
        Document document = new Document();

        document.add(new KeywordField(DATA_COLLECTION_NAME_FIELD, dcName, Field.Store.YES));
        //document.add( new Field( DATA_COLLECTION_NAME_FIELD, dcName, Field.Store.YES, Field.Index.UN_TOKENIZED ) );

        document.add(new KeywordField(DATA_ELEMENT_RELATIVE_NAME, DataElementPath.create(de).toString(), Field.Store.YES));
        //        document.add( new Field( DATA_ELEMENT_RELATIVE_NAME, DataElementPath.create( de ).toString(), Field.Store.YES,
        //                Field.Index.UN_TOKENIZED ) );

        document.add(new KeywordField("name", de.getName(), Field.Store.YES));
        //document.add( new Field( "name", de.getName(), Field.Store.YES, Field.Index.UN_TOKENIZED ) );
        StringBuilder fullText = new StringBuilder( de.getName() );

        List<String> luceneIndexes = LuceneUtils.indexedFields( indexes ).toList();
        if( luceneIndexes.size() == 0 )
            throw new ParseException( "There is no valid lucene index property: " + indexes );

        LuceneHelper helper = LuceneUtils.getLuceneHelper( de.getOrigin() );

        for( String field : luceneIndexes )
        {
            if( field.equalsIgnoreCase( "name" ) || field.equalsIgnoreCase( FULL_TEXT_FIELD )
                    || field.equalsIgnoreCase( DATA_ELEMENT_RELATIVE_NAME ) || field.equalsIgnoreCase( DATA_COLLECTION_NAME_FIELD ) )
            {
                continue;
            }

            String value;
            if( helper == null )
            {
                value = getBeanValue( de, field, cat );
            }
            else
            {
                value = helper.getBeanValue( de, field );
            }
            if( value != null && value.length() > 0 )
            {
                document.add(new TextField(field, value, Field.Store.YES));

                //document.add( new Field( field, value, Field.Store.YES, Field.Index.TOKENIZED ) );
                fullText.append( " " );
                fullText.append( value );
            }
        }

        document.add(new TextField(FULL_TEXT_FIELD, fullText.toString(), Field.Store.NO));

        return document;
    }

    protected static String getBeanValue(DataElement de, String name, Logger cat)
    {
        try
        {
            // buid getter name
            if( name.length() >= 2 )
            {
                Object val = null;
                try
                {
                    val = BeanUtil.getBeanPropertyValue( de, name );
                }
                catch( Exception e )
                {
                }
                if( val == null )
                    return null;

                if( val.getClass().isArray() )
                {
                    StringBuilder buf = new StringBuilder();
                    int length = Array.getLength( val );
                    for( int i = 0; i < length; i++ )
                    {
                        Object obj = Array.get( val, i );
                        if( obj != null )
                        {
                            buf.append( obj.toString() );
                            buf.append( "\n" );
                        }
                    }
                    return buf.toString();
                }

                return val.toString();
            }
        }
        catch( Throwable t )
        {
            ExceptionRegistry.log( t );
            if( cat != null )
                cat.log(Level.SEVERE, new DataElementReadException( t, de, name ).getMessage() );
        }

        return null;
    }

    @Override
    public synchronized void deleteIndex(Logger cat) throws IOException, ParseException
    {
        addToIndex( null, cat, null, true );
    }

    @Override
    public synchronized void deleteFromIndex(String relativeName, Logger cat) throws IOException, ParseException
    {
        deleteFromIndex( module.getCompletePath().getRelativePath( relativeName ), cat );
    }

    protected void deleteFromIndex(DataElementPath dePath, Logger cat) throws IOException
    {

        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        Directory dir = FSDirectory.open(Paths.get(luceneDir));
        IndexWriter writer = new IndexWriter(dir, iwc);

        try
        {
            deleteFromIndex(dePath, writer, cat);
        }
        catch( IOException ioe )
        {
            if( cat != null )
                cat.log(Level.SEVERE, "Indexing failed.", ioe );
            throw ioe;
        }
        finally
        {
            writer.close();
        }
    }

    protected void deleteFromIndex(DataElementPath dePath, IndexWriter writer, Logger cat) throws IOException
    {
        if( cat != null && log.isLoggable( Level.FINE ) )
            cat.info( "Delete index for data element " + dePath.getName() );

        writer.deleteDocuments(new Term(DATA_COLLECTION_NAME_FIELD, dePath.getPathDifference(module.getCompletePath())));
        writer.deleteDocuments(new Term(DATA_ELEMENT_RELATIVE_NAME, dePath.toString()));
    }

    @Override
    public synchronized DynamicPropertySet[] search(String relativeName, String queryString, Formatter formatter, boolean alternativeView)
            throws IOException, ParseException, IntrospectionException
    {
        DataCollection<?> dc = getSubCollection( relativeName );
        if( dc != null && testHaveLuceneIndexes( dc ) )
            return search( dc, relativeName, queryString, new QueryParser( FULL_TEXT_FIELD, analyzer ), formatter, alternativeView, 0,
                    MAX_DEFAULT_SEARCH_RESULTS_COUNT );
        return null;
    }

    @Override
    public synchronized DynamicPropertySet[] search(String relativeName, String queryString, String[] fields, Formatter formatter,
            boolean alternativeView) throws IOException, ParseException, IntrospectionException
    {
        return search( relativeName, queryString, fields, formatter, alternativeView, 0, MAX_DEFAULT_SEARCH_RESULTS_COUNT );
    }

    @Override
    public synchronized DynamicPropertySet[] search(String relativeName, String queryString, String[] fields, Formatter formatter,
            boolean alternativeView, int from, int to) throws IOException, ParseException, IntrospectionException
    {
        DataCollection<?> dc = getSubCollection( relativeName );
        if( dc != null && testHaveLuceneIndexes( dc ) )
        {
            QueryParser parser = null;
            if( fields == null )
                parser = new QueryParser( FULL_TEXT_FIELD, analyzer );
            else if( fields.length == 0 )
                parser = new QueryParser( FULL_TEXT_FIELD, analyzer );
            else if( fields.length == 1 )
                parser = new QueryParser( fields[0], analyzer );
            else if( fields.length > 1 )
                parser = new MultiFieldQueryParser( fields, analyzer );
            return search( dc, relativeName, queryString, parser, formatter, alternativeView, from, to );
        }
        return null;
    }

    protected boolean testHaveLuceneIndexes(DataCollection<?> dc)
    {
        String indexes = dc.getInfo().getProperty( LUCENE_INDEXES );
        if( indexes != null )
        {
            return LuceneUtils.indexedFields( indexes ).anyMatch( x -> true );
        }
        return false;
    }

    protected DynamicPropertySet[] search(DataCollection<?> dc, String constraint, String queryString, QueryParser parser,
            Formatter formatter, boolean alternativeView, int from, int to) throws IOException
    {
        if( queryString == null )
            return null;
        if( queryString.trim().length() == 0 )
            return null;

        Directory index = FSDirectory.open(Paths.get(luceneDir));
        DirectoryReader reader = DirectoryReader.open(index);
        PrefixQuery additionalQuery = new PrefixQuery(new Term(DATA_COLLECTION_NAME_FIELD, constraint));

        IndexSearcher searcher = new IndexSearcher(reader);
        try
        {

            Query baseQuery = parser.parse(queryString);
            if ( baseQuery == null )
                return null;

            Query query = null;
            if ( constraint != null )
            {
                BooleanQuery.Builder b = new BooleanQuery.Builder();
                b.add(baseQuery, BooleanClause.Occur.MUST);
                b.add(additionalQuery, BooleanClause.Occur.FILTER);
                query = b.build();
            }
            else
                query = baseQuery;
                query = searcher.rewrite( query );

                org.apache.lucene.search.highlight.Formatter htmlFormatter = null;
                if( formatter == null )
                    htmlFormatter = new SimpleHTMLFormatter( "", "" );
                else
                    htmlFormatter = new SimpleHTMLFormatter( formatter.getPrefix(), formatter.getPostfix() );
                Highlighter highlighter = new Highlighter(htmlFormatter, new QueryScorer(baseQuery));
                highlighter.setTextFragmenter( new NullFragmenter() );

                TopDocs foundDocs = searcher.search(query, to);
                StoredFields storedFields = searcher.storedFields();
                ScoreDoc[] hits = foundDocs.scoreDocs;
                if ( hits.length > 0 )
                {
                    return processHits(dc, storedFields, hits, highlighter, alternativeView, from, to);
                }
        }
        catch( Exception e )
        {
            throw new SearchException( e, dc.getCompletePath(), queryString );
        }
        finally
        {
            reader.close();
        }

        return null;
    }

    @Override
    public DynamicPropertySet[] searchRecursive(String relativeName, String queryString, Formatter formatter, int from, int to)
            throws IOException, ParseException, IntrospectionException
    {
        DataCollection<?> rdc = null;
        if( relativeName == null )
        {
            rdc = module;
        }
        else
        {
            rdc = getSubCollection( relativeName );
        }
        if( rdc != null )
        {
            Map<DataCollection<?>, String> dcMap = new HashMap<>();
            //look for child collections with lucene indexes
            fillDCMapForSearch( dcMap, rdc, relativeName );
            if( dcMap.size() == 0 )
            {
                //look for parent collection with lucene indexes
                List<DataCollection<?>> parents = rdc.parents().collect( Collectors.toList() );
                for( DataCollection<?> parentDC : parents )
                {
                    int idx = relativeName.lastIndexOf( '/' );
                    if( idx != -1 )
                    {
                        relativeName = relativeName.substring( 0, idx );
                    }
                    else
                    {
                        //relative name can not be null
                        break;
                    }
                    if( testHaveLuceneIndexes( parentDC ) )
                    {
                        dcMap.put( parentDC, relativeName );
                        break;
                    }
                }
            }

            List<DynamicPropertySet> result = new ArrayList<>();
            if( dcMap.size() > 0 )
            {
                boolean alternativeView = dcMap.size() > 1;
                for( Map.Entry<DataCollection<?>, String> entry : dcMap.entrySet() )
                {
                    DynamicPropertySet[] sr = search( entry.getKey(), entry.getValue(), queryString, new QueryParser( FULL_TEXT_FIELD,
                            analyzer ), formatter, alternativeView, from, to );
                    if( sr != null )
                    {
                        result.addAll( Arrays.asList( sr ) );
                    }
                }
                final String scoreProperty = messageBundle.getResourceString( "COLUMN_SCORE" );
                result.sort( Comparator.comparing( dps -> (Float)dps.getValue( scoreProperty ) ) );
            }
            return result.toArray( new DynamicPropertySet[result.size()] );
        }
        return null;
    }

    protected void fillDCMapForSearch(Map<DataCollection<?>, String> dcMap, DataCollection<?> dc, String relativeName)
    {
        try
        {
            DataCollection<?> luceneCollection = LuceneUtils.getLuceneParent( dc );
            for( String path : LuceneUtils.getCollectionsNames( dc, LUCENE_INDEXES ) )
            {
                dcMap.put( CollectionFactory.getDataElement( path, luceneCollection, DataCollection.class ), path );
            }
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

    protected DynamicPropertySet[] processHits(DataCollection<?> dc, StoredFields storedFields, ScoreDoc[] hits, Highlighter highlighter,
            boolean alternativeView, int from, int to) throws IOException, ParseException
    {
        if( from < 0 )
            from = 0;
        DynamicPropertySet[] dpsArray = null;
        if( !alternativeView )
        {
            //System.out.println("General view");
            if( to <= 0 )
                to = hits.length;
            else if ( to > hits.length )
                to = hits.length;
            if( to - from > 0 )
            {
                dpsArray = new DynamicPropertySet[to - from];
                PropertyExtractor extractor = new PropertyExtractor();
                Collection<String> fields = new HashSet<>();
                for( int i = from; i < to; i++ )
                {
                    Document doc = storedFields.document(hits[i].doc);
                    String fullName = doc.getField( DATA_ELEMENT_RELATIVE_NAME ).stringValue();
                    DynamicPropertySet dps = getDPS(fullName, doc, hits[i].score, analyzer, highlighter, extractor);
                    dpsArray[i - from] = dps;
                    for( DynamicProperty property : dps )
                    {
                        fields.add( property.getName() );
                    }
                }
                for( int i = 0; i < dpsArray.length; i++ )
                {
                    dpsArray[i] = normalizeDPS( dpsArray[i], fields, extractor );
                }
            }
        }
        else
        {
            //System.out.println("Alternative view");
            List<DynamicPropertySet> dpslist = getAlternativeView(dc, storedFields, hits, to, highlighter, analyzer);
            if( !dpslist.isEmpty() )
            {
                if( to <= 0 || to > dpslist.size() )
                    to = dpslist.size();
                if( to - from > 0 )
                {
                    dpsArray = new DynamicPropertySet[to - from];
                    for( int i = from; i < to; i++ )
                        dpsArray[i - from] = dpslist.get( i );
                }
            }
        }

        if( dpsArray != null && dpsArray.length > 0 )
        {
            for( DynamicPropertySet set : dpsArray )
            {
                for( DynamicProperty dp : set )
                {
                    dp.setReadOnly( true );
                    dp.getDescriptor().setPropertyEditorClass( HtmlEditor.class );
                }
            }
        }

        return dpsArray;
    }

    private List<DynamicPropertySet> getAlternativeView(DataCollection<?> dc, StoredFields storedFields, ScoreDoc[] hits, int to, Highlighter highlighter, Analyzer analyzer2)
    {
        int count = 0;
        List<DynamicPropertySet> dpslist = new ArrayList<>(to);
        boolean isTitle = false;
        for ( int i = 0; i < hits.length; i++ )
        {
            Document doc;
            try
            {
                doc = storedFields.document(hits[i].doc);

            List<IndexableField> docFields = doc.getFields();
            for ( IndexableField docField : docFields )
            {
                if ( !docField.name().equals(DATA_COLLECTION_NAME_FIELD) && !docField.name().equals(FULL_TEXT_FIELD) )
                {
                    String fragment = highlighter.getBestFragment(analyzer, docField.name(), docField.stringValue());
                    if ( fragment == null )
                        continue;
                    IndexableField name = doc.getField("name");
                    if ( name != null )
                    {
                        DynamicPropertySetSupport dpss = new DynamicPropertySetSupport();
                        //String dcName = CollectionFactory.getRelativeName(dc, module);
                        String fullName = doc.getField(DATA_ELEMENT_RELATIVE_NAME).stringValue();
                        dpss.add(getFullNameField(fullName));
                        DynamicProperty dp = new DynamicProperty(messageBundle.getResourceString("COLUMN_ELEMENT_NAME"), String.class, name.stringValue());
                        dpss.add(dp);
                        dp = new DynamicProperty(messageBundle.getResourceString("COLUMN_FIELD_NAME"), String.class, docField.name());
                        dpss.add(dp);
                        dp = new DynamicProperty(messageBundle.getResourceString("COLUMN_FIELD_DATA"), String.class, fragment);
                        dpss.add(dp);
                        IndexableField title = doc.getField("title");
                        if ( title != null )
                        {
                            isTitle = true;
                            dp = new DynamicProperty(messageBundle.getResourceString("COLUMN_TITLE"), String.class, "" + title.stringValue());
                        }
                        else
                            dp = new DynamicProperty(messageBundle.getResourceString("COLUMN_TITLE"), String.class, "");
                        dpss.add(dp);

                        dpss.add(getScoreField(hits[i].score));
                        dpslist.add(dpss);
                        count++;
                        if ( to > 0 )
                            if ( count >= to )
                                break;
                    }
                }
            }
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        }
        if ( !isTitle )
        {
            for ( DynamicPropertySet dps : dpslist )
            {
                dps.remove(messageBundle.getResourceString("COLUMN_TITLE"));
            }
        }
        return dpslist;
    }

    protected DynamicPropertySet normalizeDPS(Iterable<DynamicProperty> dps, Collection<String> fields, PropertyExtractor extractor)
    {
        Map<String, DynamicProperty> map = new HashMap<>();
        for( DynamicProperty dp : dps )
        {
            map.put( dp.getName(), dp );
        }
        for( String field : fields )
        {
            if( !map.containsKey( field ) )
            {
                DynamicProperty dp = extractor.createDynamicProperty( null, field );
                dp.setValue( " " );
                map.put( dp.getName(), dp );
            }
        }
        List<DynamicProperty> sortedProperties = new ArrayList<>( map.values() );
        sortedProperties.sort( Comparator.comparing( prop -> {
            Object v = prop.getDescriptor().getValue( SORT_PROPERTY_NAME );
            return v == null ? "?" : v.toString();
        } ) );
        DynamicPropertySet result = new DynamicPropertySetSupport();
        sortedProperties.forEach( result::add );
        return result;
    }


    protected DynamicPropertySet getDPS(String fullName, Document doc, float score, Analyzer analyzer, Highlighter highlighter,
            PropertyExtractor extractor) throws IOException, ParseException
    {
        DynamicPropertySet dps = new DynamicPropertySetSupport();

        dps.add( getFullNameField( fullName ) );

        //test if field "Name" absent in this document
        if( doc.getField( "name" ) == null )
            throw new ParseException( "Data collection don't have field \"Name\"" );
        DynamicProperty dp = extractor.createDynamicProperty( fullName, "name" );
        setFieldValue( dp, highlighter, analyzer, doc.getField( "name" ) );
        dps.add( dp );

        //add all searchable fields
        List<IndexableField> docFields = doc.getFields();
        for ( IndexableField docField : docFields )
        {
            if( !docField.name().equalsIgnoreCase( DATA_COLLECTION_NAME_FIELD )
                    && !docField.name().equalsIgnoreCase( DATA_ELEMENT_RELATIVE_NAME )
                    && !docField.name().equalsIgnoreCase( FULL_TEXT_FIELD ) && !docField.name().equalsIgnoreCase( "name" ) )
            {
                dp = extractor.createDynamicProperty( fullName, docField.name() );
                setFieldValue( dp, highlighter, analyzer, docField );
                dps.add( dp );
            }
        }

        dps.add( getScoreField( score ) );

        return dps;
    }

    protected DynamicProperty getFullNameField(String fullName)
    {
        DynamicProperty dp = new DynamicProperty( messageBundle.getResourceString( "COLUMN_FULL_NAME" ), DataElementPath.class,
                DataElementPath.create( fullName ) );
        dp.getDescriptor().setValue( SORT_PROPERTY_NAME, "!begin" );
        return dp;
    }

    protected DynamicProperty getScoreField(float score)
    {
        DynamicProperty dp = new DynamicProperty( messageBundle.getResourceString( "COLUMN_SCORE" ), Float.class, score );
        dp.setHidden( true );
        dp.getDescriptor().setValue( SORT_PROPERTY_NAME, "end" );
        return dp;
    }

    protected void setFieldValue(DynamicProperty dp, Highlighter highlighter, Analyzer analyzer, IndexableField docField) throws IOException
    {
        Object value = null;
        if( !dp.getType().equals( String.class ) )
        {
            value = TextUtil.fromString(dp.getType(), docField.stringValue());
        }
        if( value == null )
        {
            try
            {
                value = highlighter.getBestFragment(analyzer, docField.name(), docField.stringValue());
            }
            catch (Exception e)
            {
            }
        }
        if( value == null )
        {
            value = String.valueOf(docField.stringValue());
        }
        dp.setValue( value );
    }

    @Override
    public synchronized List<String> getCollectionsNamesWithIndexes()
    {
        try
        {
            return LuceneUtils.getCollectionsNames( module, LUCENE_INDEXES );
        }
        catch( Exception e )
        {
            log.log( Level.SEVERE, e.getMessage(), e );
        }
        return Collections.emptyList();
    }

    @Override
    public synchronized ru.biosoft.access.core.DataElement getDataElement(String relativeName)
    {
        return CollectionFactory.getDataElement( relativeName, module );
    }

    @Override
    public synchronized @Nonnull Vector<String> getPropertiesNames(String relativeName)
    {
        DataCollection<?> dc = getSubCollection( relativeName );
        if( dc == null )
            return new Vector<>();
        return LuceneUtils.getPropertiesNames( dc );
    }

    @Override
    public void showRebuildIndexesUI(JFrame parent) throws Exception
    {
        RebuildIndexDialog rebuildDialog = new RebuildIndexDialog( parent, this );
        rebuildDialog.doModal();
    }


    private static class PropertyExtractor
    {
        private static class SimpleProperty
        {
            private Class<?> type;
            private final PropertyDescriptor descriptor;

            public SimpleProperty(String name)
            {
                this.type = String.class;
                descriptor = BeanUtil.createDescriptor( name );
                descriptor.setDisplayName( generateDisplayName( name ) );
            }

            private String generateDisplayName(String name)
            {
                String newName;
                if( name.equals( "name" ) )
                    newName = "Accession";
                else
                    newName = name.substring( 0, 1 ).toUpperCase() + name.substring( 1 );
                newName = newName.replace( "/", ": " );
                newName = newName.replaceAll( "([a-z])([A-Z])", "$1 $2" );
                return newName;
            }

            public SimpleProperty(Property property, String sortingValue)
            {
                this.type = property.getValueClass();
                if( this.type.isArray() )
                    this.type = String.class;
                descriptor = BeanUtil.createDescriptor( property.getName() );
                descriptor.setValue( SORT_PROPERTY_NAME, sortingValue );
                if( property.getDisplayName().equals( property.getName() ) )
                    descriptor.setDisplayName( generateDisplayName( property.getDisplayName() ) );
                else
                    descriptor.setDisplayName( property.getDisplayName() );
            }

            public DynamicProperty createDynamicProperty()
            {
                return new DynamicProperty( descriptor, type );
            }
        }

        private final Map<String, SimpleProperty> map = new HashMap<>();

        public DynamicProperty createDynamicProperty(String path, String fieldName)
        {
            return getProperty( path, fieldName ).createDynamicProperty();
        }

        private SimpleProperty getProperty(String path, String fieldName)
        {
            SimpleProperty property = map.get( fieldName );
            if( property != null )
                return property;
            try
            {
                ComponentModel model = ComponentFactory.getModel( CollectionFactory.getDataElementChecked( path, true ) );
                Property beanProperty = model.findProperty( fieldName );
                if( beanProperty != null )
                {
                    property = new SimpleProperty( beanProperty, BeanUtil.getPropertySortingValue( model, fieldName ) );
                }
            }
            catch( Exception e )
            {
            }
            if( property == null )
            {
                property = new SimpleProperty( fieldName );
            }
            map.put( fieldName, property );
            return property;
        }
    }

    private Directory getLuceneDirectory() throws IOException
    {
        Path directoryPath = Paths.get(luceneDir);
        Directory directory = null;
        if ( Files.exists(directoryPath) && Files.isDirectory(directoryPath) )
        {
            directory = FSDirectory.open(directoryPath);
            return directory;
        }
        return null;
    }
}
