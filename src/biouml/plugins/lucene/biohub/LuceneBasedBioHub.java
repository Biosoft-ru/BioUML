package biouml.plugins.lucene.biohub;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.CollectorManager;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import one.util.streamex.StreamEx;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;

/**
 * BioHub implementation based on lucene indexes.
 */
public class LuceneBasedBioHub extends BioHubSupport
{
    protected Logger log = Logger.getLogger(LuceneBasedBioHub.class.getName());

    /**
     * Constant for index path parameter
     */
    public static final String HUB_DIR_ATTR = "hubPath";

    protected Analyzer analyzer = new WhitespaceAnalyzer();

    public LuceneBasedBioHub(Properties properties)
    {
        super(properties);
    }

    @Override
    public int getPriority(TargetOptions dbOptions)
    {
        DataElementPathSet collectionNames = dbOptions.getUsedCollectionPaths();
        if( collectionNames.size() == 1 )
        {
            if( getIndexesPath(collectionNames.first()) != null )
            {
                return 10;
            }
        }
        return 0;
    }

    @Override
    public Element[] getReference(Element startElement, TargetOptions dbOptions, String[] relationTypes, int maxLength, int direction)
    {
        DataElementPathSet collectionNames = dbOptions.getUsedCollectionPaths();
        if( collectionNames.size() > 0 )
        {
            Set<String> ignoreSet = new HashSet<>();
            ignoreSet.add(startElement.getPath());
            List<Element> resultList = search(collectionNames.first(), startElement.getPath(), direction, maxLength, ignoreSet);
            return resultList.toArray(new Element[resultList.size()]);
        }
        return null;
    }

    @Override
    public List<Element[]> getMinimalPaths(Element key, Element[] targets, TargetOptions dbOptions, String[] relationTypes, int maxLength,
            int direction)
    {
        Set<Element> targetSet = StreamEx.of( targets ).toSet();
        DataElementPathSet collectionNames = dbOptions.getUsedCollectionPaths();
        if( collectionNames.size() == 0 )
            return Collections.emptyList();

        String startAccession = key.getAccession();
        List<Element[]> resultmain = new ArrayList<>();
        for( FoundElement found : findPaths( collectionNames.first(), key, targetSet, maxLength, direction ) )
        {
            List<Element> result = new ArrayList<>();
            while( !startAccession.equals( found.element.getAccession() ) )
            {
                result.add( found.element );
                found = found.prev;
            }
            result.add( key );
            resultmain.add( result.toArray( new Element[0] ) );
        }
        return resultmain;
    }

    @Override
    public Element[] getMinimalPath(Element element1, Element element2, TargetOptions dbOptions, String[] relationTypes, int maxLength,
            int direction)
    {
        return getMinimalPaths( element1, new Element[] {element2}, dbOptions, relationTypes, maxLength, direction ).get( 0 );
    }

    /**
     * Returns absolute path to indexes folder or null if indexes folder doesn't exists
     */
    protected String getIndexesPath(DataElementPath collectionPath)
    {
        String pathPrefix = "";
        String applicationPath = System.getProperty("biouml.server.path");
        if( applicationPath != null )
        {
            pathPrefix = applicationPath + File.separator;
        }
        File hubFolder = new File(pathPrefix + properties.getProperty(HUB_DIR_ATTR));
        if( hubFolder.exists() )
        {
            hubFolder = new File(hubFolder.getAbsolutePath() + File.separator + collectionPath);
            if( !hubFolder.exists() )
            {
                if( collectionPath.exists() )
                    log.log(Level.SEVERE, "Can't find indexes folder for collection: " + collectionPath);
                return null;
            }
            return hubFolder.getAbsolutePath();
        }
        return null;
    }

    private static class FoundElement
    {
        Element element;
        FoundElement prev;
        float length = 0;
        public FoundElement(Element element, FoundElement prev, float length)
        {
            this.element = element;
            this.prev = prev;
            this.length = length;
        }
        public FoundElement(Element element)
        {
            this.element = element;
            this.prev = null;
            this.length = 0;
        }
        public String getPath()
        {
            return element.getPath();
        }
    }

    private List<FoundElement> findPaths(DataElementPath collectionPath, Element start, Set<Element> targets, float maxLength,
            int direction)
    {
        Map<String, FoundElement> foundNodes = new HashMap<>();
        Set<String> ignoreSet = new HashSet<>();
        ignoreSet.add( start.getPath() );
        List<FoundElement> currentStart = Collections.singletonList( new FoundElement( start ) );

        String indexesPath = getIndexesPath( collectionPath );
        if( indexesPath != null )
        {
            while( !currentStart.isEmpty() )
            {
                List<FoundElement> nextStart = new ArrayList<>();
                for( FoundElement curNode : currentStart )
                {
                    float curLength = maxLength - curNode.length;
                    List<Element> nextLinked = getLinked( curLength, direction, ignoreSet, indexesPath, curNode.getPath() );

                    for( Element linked : nextLinked )
                    {
                        float nextLength = curNode.length + linked.getLinkedLength();
                        FoundElement next = foundNodes.get( linked.getAccession() );
                        if( next == null || nextLength < next.length )
                        {
                            next = new FoundElement( linked, curNode, nextLength );
                            foundNodes.put( linked.getAccession(), next );
                            nextStart.add( next );
                        }
                    }
                }
                currentStart = nextStart;
            }
        }
        return StreamEx.of( targets ).map( t -> foundNodes.get( t.getAccession() ) ).nonNull().toList();
    }

    private List<Element> getLinked(float maxLength, int direction, Set<String> ignorePath, String indexesPath, String elementPath)
    {
        List<Element> resultList = new ArrayList<>();
        IndexSearcher searcher = null;
        IndexReader reader = null;
        try
        {
            Directory index = FSDirectory.open(Paths.get(indexesPath));
            reader = DirectoryReader.open(index);

            searcher = new IndexSearcher(reader);
            String searchPath = QueryParser.escape(elementPath);//.replace("/", "\\/");
            if ( direction == BioHub.DIRECTION_DOWN || direction == BioHub.DIRECTION_BOTH )
            {
                QueryParser parser = new QueryParser(Constants.ELEMENT1_PATH_FIELD, analyzer);
                Query query = parser.parse(searchPath);
                if ( query != null )
                {
                    query = searcher.rewrite(query);

                    AllHitsCollectorManager ahc = new AllHitsCollectorManager(searcher, resultList, BioHub.DIRECTION_DOWN, maxLength, ignorePath);
                    searcher.search(query, ahc);
                }
            }

            if ( direction == BioHub.DIRECTION_UP || direction == BioHub.DIRECTION_BOTH )
            {
                QueryParser parser = new QueryParser(Constants.ELEMENT2_PATH_FIELD, analyzer);
                Query query = parser.parse(searchPath);
                if ( query != null )
                {
                    query = searcher.rewrite(query);

                    AllHitsCollectorManager ahc = new AllHitsCollectorManager(searcher, resultList, BioHub.DIRECTION_UP, maxLength, ignorePath);
                    searcher.search(query, ahc);
                }
            }
        }
        catch (Exception e)
        {
            log.log(Level.SEVERE, "Can not search element in hub", e);
        }
        finally
        {
            if ( reader != null )
            {
                try
                {
                    reader.close();
                }
                catch (Exception e)
                {
                }
            }
        }

        return resultList;
    }

    protected List<Element> search(DataElementPath collectionPath, String elementPath, int direction, float maxLength, Set<String> ignorePath)
    {
        List<Element> resultList = new ArrayList<>();
        String indexesPath = getIndexesPath(collectionPath);
        if( indexesPath != null )
        {
            resultList = getLinked( maxLength, direction, ignorePath, indexesPath, elementPath );

            //recursively search
            int size = resultList.size();
            for( int i = 0; i < size; i++ )
            {
                Element element = resultList.get(i);
                if( element.getLinkedLength() < maxLength )
                {
                    List<Element> innerResults = search(collectionPath, element.getPath(), direction,
                            maxLength - element.getLinkedLength(), ignorePath);
                    resultList.addAll(innerResults);
                }
            }
        }
        else
        {
            log.log(Level.SEVERE, "Hub directory doesn't exists");
        }
        return resultList;
    }

    /**
     * Lucene result element collector
     */
    private static class AllHitsCollector extends SimpleCollector
    {
        protected List<Element> resultList;
        protected IndexSearcher searcher;
        protected int direction;
        protected float maxLength;
        protected Set<String> ignorePath;

        public AllHitsCollector(IndexSearcher searcher, List<Element> resultList, int direction, float maxLength, Set<String> ignorePath)
        {
            this.searcher = searcher;
            this.resultList = resultList;
            this.direction = direction;
            this.maxLength = maxLength;
            this.ignorePath = ignorePath;
        }

        @Override
        public ScoreMode scoreMode()
        {
            //???
            return ScoreMode.COMPLETE;
        }

        @Override
        public void collect(int doc) throws IOException
        {
            String fromField = null;
            String toField = null;
            if ( direction == BioHub.DIRECTION_DOWN )
            {
                fromField = Constants.ELEMENT1_PATH_FIELD;
                toField = Constants.ELEMENT2_PATH_FIELD;
            }
            else
            {
                fromField = Constants.ELEMENT2_PATH_FIELD;
                toField = Constants.ELEMENT1_PATH_FIELD;
            }

            StoredFields storedFields = searcher.storedFields();
            Document doc_;
            try
            {
                doc_ = storedFields.document(doc);
            }
            catch (IOException e)
            {
                return;
            }

            String targetPath = doc_.getField(toField).stringValue();

            if ( ignorePath != null && ignorePath.contains(targetPath) )
                return;

            float length = -1;
            String lengthStr = doc_.getField(Constants.LENGTH_FIELD).stringValue();
            if ( lengthStr != null )
            {
                try
                {
                    length = Float.parseFloat(lengthStr);
                }
                catch (NumberFormatException e)
                {
                }
            }

            if ( length <= maxLength )
            {
                Element element = new Element(targetPath);
                element.setLinkedFromPath(doc_.getField(fromField).stringValue());
                element.setLinkedLength(length);

                element.setLinkedPath(doc_.getField(Constants.PATH_FIELD).stringValue());
                element.setRelationType(doc_.getField(Constants.RELATION_TYPE_FIELD).stringValue());
                element.setLinkedDirection(direction);
                resultList.add(element);
                if ( ignorePath != null )
                {
                    ignorePath.add(targetPath);
                }
            }

        }
    }

    private static class AllHitsCollectorManager implements CollectorManager<AllHitsCollector, Object[]>
    {
        protected List<Element> resultList;
        protected IndexSearcher searcher;
        protected int direction;
        protected float maxLength;
        protected Set<String> ignorePath;

        public AllHitsCollectorManager(IndexSearcher searcher, List<Element> resultList, int direction, float maxLength, Set<String> ignorePath)
        {
            this.searcher = searcher;
            this.resultList = resultList;
            this.direction = direction;
            this.maxLength = maxLength;
            this.ignorePath = ignorePath;
        }

        @Override
        public AllHitsCollector newCollector() throws IOException
        {
            return new AllHitsCollector(searcher, resultList, direction, maxLength, ignorePath);
        }

        @Override
        public Object[] reduce(Collection<AllHitsCollector> collectors) throws IOException
        {
            // ???
            return null;
        }

    }
}
