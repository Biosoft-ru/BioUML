package biouml.plugins.lucene.biohub;

import java.io.File;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.KeywordField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import biouml.model.Module;
import biouml.standard.type.Base;
import biouml.workbench.graphsearch.QueryEngine;
import biouml.workbench.graphsearch.QueryOptions;
import biouml.workbench.graphsearch.SearchElement;
import one.util.streamex.StreamEx;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.util.TextUtil;

/**
 * Index builder for {@link LuceneBasedBioHub}
 */
public class IndexBuilder
{
    protected Logger log = Logger.getLogger(IndexBuilder.class.getName());

    protected String hubFolderPath;
    protected Analyzer analyzer = new StandardAnalyzer();

    public IndexBuilder(String hubFolderPath)
    {
        this.hubFolderPath = hubFolderPath;
    }

    /**
     * Build indexes for data collection elements
     */
    public void buildIndexes(DataCollection[] elements, QueryEngine queryEngine, JobControl jobControl)
    {
        QueryOptions queryOptions = new QueryOptions(1, BioHub.DIRECTION_DOWN);
        DataElementPath modulePath = Module.getModulePath(elements[0]);
        TargetOptions dbOptions = new TargetOptions(modulePath);
        //TODO: !!!
        try
        {
            if ( queryEngine.canSearchLinked(dbOptions) == 0 )
            {
                log.log(Level.SEVERE, "Selected query engine can not be used to this module");
                return;
            }
        }
        catch (Exception e)
        {
            log.log(Level.SEVERE, "Can not check query engine availability", e);
            return;
        }

        String indexPath = null;
        File folder = new File(hubFolderPath + File.separator + modulePath);
        if ( !folder.exists() )
        {
            folder.mkdirs();
        }
        indexPath = folder.getAbsolutePath();
        IndexWriter writer = null;
        try
        {
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(OpenMode.CREATE);
            Directory index = FSDirectory.open(Paths.get(indexPath));
            writer = new IndexWriter(index, iwc);
            //writer = new IndexWriter(indexPath, analyzer, true);
        }
        catch (Exception e)
        {
            log.log(Level.SEVERE, "Can not create index writer", e);
            return;
        }

        int totalSize = StreamEx.of(elements).mapToInt(ru.biosoft.access.core.DataCollection::getSize).sum();
        double currentPercent = 0.0;
        double part = 100.0 / totalSize;

        for ( DataCollection<?> dc : elements )
        {
            for ( ru.biosoft.access.core.DataElement de : dc )
            {
                try
                {
                    Base inputDataElement = de.cast(Base.class);
                    SearchElement inputElement = new SearchElement(inputDataElement);
                    SearchElement[] outputElements = queryEngine.searchLinked(new SearchElement[] { inputElement }, queryOptions, dbOptions, null);
                    for ( SearchElement outputElement : outputElements )
                    {
                        if ( outputElement.getLinkedFromPath().equals(inputElement.getPath()) )
                        {
                            addToIndex(inputElement, outputElement, writer);
                        }
                    }
                }
                catch (Exception e)
                {
                    log.log(Level.SEVERE, "Can not create index for input element: " + de.getCompletePath());
                }

                currentPercent += part;
                if ( jobControl != null )
                {
                    jobControl.setPreparedness((int) currentPercent);
                }
            }
        }

        try
        {
            writer.close();
        }
        catch (Exception e)
        {
        }
    }

    protected void addToIndex(SearchElement element1, SearchElement element2, IndexWriter writer) throws Exception
    {
        String path1 = element1.getPath();
        String path2 = element2.getPath();

        String rType = element2.getRelationType();
        float length = element2.getLinkedLength();
        String path = TextUtil.nullToEmpty( element2.getLinkedPath() );
        writer.addDocument(fillDocument(path1, path2, rType, length, path));
    }

    protected Document fillDocument(String path1, String path2, String relationType, float length, String path)
    {
        Document document = new Document();
        document.add(new StringField(Constants.ELEMENT1_PATH_FIELD, path1, Field.Store.YES));
        document.add(new StringField(Constants.ELEMENT2_PATH_FIELD, path2, Field.Store.YES));
        document.add(new StringField(Constants.RELATION_TYPE_FIELD, relationType, Field.Store.YES));
        document.add(new StringField(Constants.LENGTH_FIELD, String.valueOf(length), Field.Store.YES));
        document.add(new StringField(Constants.PATH_FIELD, path, Field.Store.YES));
        return document;
    }
}
