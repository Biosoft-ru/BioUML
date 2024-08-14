package ru.biosoft.bsa;

import java.io.File;
import java.util.Properties;

import javax.annotation.Nonnull;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.Entry;
import ru.biosoft.access.core.CloneableDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.view.DefaultTrackViewBuilder;
import ru.biosoft.bsa.view.TrackViewBuilder;
import ru.biosoft.exception.ExceptionRegistry;

/**
 * Track which is a parent collection for sequences in Embl/Genbank file
 * @author lan
 */
public class GenbankSequenceCollection extends TransformedDataCollection<Entry, AnnotatedSequence>
        implements TrackOnSequences, CloneableDataElement
{
    private final String fileDirPath;
    private final String format;

    /**
     * @param parent
     * @param properties    
     * @throws Exception
     */
    public GenbankSequenceCollection(DataCollection<?> parent, Properties properties) throws Exception
    {
        super( parent, processProperties( properties, parent ) );
        this.fileDirPath = properties.getProperty( DataCollectionConfigConstants.FILE_PATH_PROPERTY, "" );
        this.format = SequenceImporter
                .getFormatForTransformer( properties.getProperty( DataCollectionConfigConstants.TRANSFORMER_CLASS, "" ), "" );
    }
    
    /**
     * @param properties
     * @param parent
     * @return
     */
    private static Properties processProperties(Properties properties, DataCollection<?> parent)
    {
        properties.put(SqlTrack.LABEL_PROPERTY, "name");
        if(parent != null)
            properties.put( Track.SEQUENCES_COLLECTION_PROPERTY,
                    DataElementPath.create( parent, properties.getProperty( DataCollectionConfigConstants.NAME_PROPERTY ) ).toString() );
        return properties;
    }

    @Override
    public DataCollection<Site> getSites(String sequence, int from, int to)
    {
        DataElementPath path = DataElementPath.create(sequence);
        AnnotatedSequence seq;
        try
        {
            seq = get(path.getName());
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }
        for(Track t : seq)//actually we always have one or zero tracks per sequence
        {
            return t.getSites( sequence, from, to );
        }
        return new VectorDataCollection<>("Sites", Site.class, null);
    }

    @Override
    public int countSites(String sequence, int from, int to) throws Exception
    {
       return getSites( sequence, from, to ).getSize();
    }

    @Override
    public Site getSite(String sequence, String siteName, int from, int to)
    {
        for(Site s : getSites( sequence, from, to ))
            if(s.getName().equals(siteName))
                return s;
        return null;
    }

    @Override
    public @Nonnull DataCollection<Site> getAllSites()
    {
        VectorDataCollection<Site> result = new VectorDataCollection<>("Sites", Site.class, null);
        DataElementPath path = DataElementPath.create(this);
        for(String name: getNameList())
        {
            String sequencePath = path.getChildPath( name ).toString();
            Sequence seq;
            try
            {
                seq = get( name ).getSequence();
            }
            catch( Exception e )
            {
                throw ExceptionRegistry.translateException( e );
            }
            DataCollection<Site> sites = getSites( sequencePath, seq.getStart(), seq.getStart() + seq.getLength() );
            for( Site s : sites )
                result.put( s );
        }
        return result;
    }

    protected TrackViewBuilder viewBuilder = new DefaultTrackViewBuilder();
    @Override
    public TrackViewBuilder getViewBuilder()
    {
        return viewBuilder;
    }

    @Override
    public GenbankSequenceCollection clone(DataCollection parent, String name) throws CloneNotSupportedException
    {
        try
        {
            SequenceImporter si = new SequenceImporter();
            Properties pr = new Properties();
            pr.setProperty( DataElementImporter.SUFFIX, format );
            si.init( pr );
            File file = new File( fileDirPath + '/' + this.getName() + '.' + format ); //TODO: rework this somehow
            if( fileDirPath.isEmpty() || si.accept( parent, file ) == DataElementImporter.ACCEPT_UNSUPPORTED )
                throw new CloneNotSupportedException();

            GenbankSequenceCollection clone = (GenbankSequenceCollection)si.doImport( parent, file, name, null, null );
            DataCollectionUtils.copyAnalysisParametersInfo( this, clone );
            return clone;
        }
        catch( Exception e )
        {
            throw new CloneNotSupportedException();
        }
    }
}
