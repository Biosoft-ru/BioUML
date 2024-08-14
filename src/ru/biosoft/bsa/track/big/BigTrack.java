package ru.biosoft.bsa.track.big;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.jetbrains.bio.RomBuffer;
import org.jetbrains.bio.RomBufferFactory;
import org.jetbrains.bio.big.BPlusLeaf;
import org.jetbrains.bio.big.BigFile;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.ChrCache;
import ru.biosoft.bsa.ChrNameMapping;
import ru.biosoft.bsa.GenomeSelector;
import ru.biosoft.bsa.GenomeSelector.GenomeSelectorTrackUpdater;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.view.DefaultTrackViewBuilder;
import ru.biosoft.bsa.view.SiteViewOptions;
import ru.biosoft.bsa.view.TrackViewBuilder;

public abstract class BigTrack extends AbstractDataCollection<DataElement> implements Track, AutoCloseable
{
    public static final String PROP_BIGBED_PATH = "BigBedPath";//should be the uri to file, for example file:///tmp/test.bb
    public static final String PROP_VIEW_BUILDER_CLASS = "ViewBuilderClass";
    
    protected String bbPath;
    protected ChrNameMapping chrMapping;
    protected ChrCache chrCache;
    protected Map<String, Integer> chromSizes;
    protected GenomeSelector genomeSelector;
    protected BigFile<?> bbFile;
    protected long siteCount;
    
    protected BigTrack(DataCollection<?> parent, Properties properties, boolean open) throws IOException
    {
        super(parent, properties);
        this.bbPath = getPathFromProps( properties );
        if(!isRemote())
            getInfo().addUsedFile( new File(bbPath) );
        
        chrMapping = ChrNameMapping.getMapping( properties );
        
        DataElementPath seqBase = DataElementPath.create( properties.getProperty( Track.SEQUENCES_COLLECTION_PROPERTY ) );
        chrCache = new ChrCache( seqBase );
        setGenomeSelector( new GenomeSelector( this ) );
        if(open)
            open();
        initViewBuilder(properties);
    }
    
    public String getFilePath()
    {
        return bbPath;
    }
    
    
    protected abstract BigFile<?> getBBFile();
    
    
    //open for reading, should be called once before any reading operation
    public void open() throws IOException
    {
        if(isRemote() || Files.exists( Paths.get( bbPath ) ))
        {
            doOpen();
            initSiteCount();
        }
    }
    
    public void close()
    {
        if(bbFile != null)
        {
            bbFile.close();
            bbFile = null;
        }
    }

    protected abstract void doOpen() throws IOException;
    
    protected void initSiteCount() throws IOException
    {
        RomBufferFactory factory = bbFile.getBuffFactory$big();
        try(RomBuffer input = factory.create())
        {
            long offset = bbFile.header.getUnzoomedDataOffset();
            input.setPosition( offset );
            siteCount = input.readLong();
        }

    }
    
    public static boolean isRemotePath(String path)
    {
        return path.startsWith( "http://" ) || path.startsWith( "https://") || path.startsWith( "ftp://" );
    }
    
    public boolean isRemote()
    {
        return isRemotePath( bbPath );
    }
    
    public long getSiteCount()
    {
        return siteCount;
    }

    public long getSizeOnDisk()
    {
        if(!isRemote())
            return new File(getFilePath()).length();
        return -1;
    }
    
    public void setChrNameMapping(ChrNameMapping chrNameMapping)
    {
        this.chrMapping = chrNameMapping;
        chrCache.clear();
        chromSizes = null;
    }
    protected String internalToExternal(String chr)
    {
        if(chrMapping == null)
            return chr;
        String res = chrMapping.srcToDst( chr );
        if(res == null)
            res = chr;
        return res;
    }
    protected String externalToInternalName(String chr)
    {
        if(chrMapping == null)
            return chr;
        String res = chrMapping.dstToSrc( chr );
        if(res == null)
            res = chr;
        return res;
    }
    
    
    public Sequence getChromosomeSequence(String chrName)
    {
        return chrCache.getSequence( chrName );
    }
    public Set<String> getChromosomes()
    {
        return getChromSizes().keySet();
    }
    public Map<String, Integer> getChromSizes()
    {
        if(chromSizes == null)
        {
            Map<String, BPlusLeaf> prefetched = getBBFile().getPrefetchedChr2Leaf$big();
            chromSizes = new LinkedHashMap<>();
            for(BPlusLeaf leaf : prefetched.values())
            {
                String chr = leaf.getKey();
                chr = internalToExternal( chr );
                chromSizes.put(chr, leaf.getSize());
            }
        }
        return chromSizes;
    }
    

    protected TrackViewBuilder viewBuilder;
    @Override
    public TrackViewBuilder getViewBuilder()
    {
        return viewBuilder;
    }
    protected TrackViewBuilder createDefaultTrackViewBuilder()
    {
        DefaultTrackViewBuilder res = new DefaultTrackViewBuilder() {
            @Override
            public SiteViewOptions createViewOptions()
            {
                SiteViewOptions options = super.createViewOptions();
                options.setShowTitle( false );
                return options;
            }
        };
        return res;
    }
    protected void initViewBuilder(Properties properties)
    {
        if(!properties.containsKey( PROP_VIEW_BUILDER_CLASS ))
        {
            viewBuilder = createDefaultTrackViewBuilder();
            return;
        }
        String className = properties.getProperty( PROP_VIEW_BUILDER_CLASS );
        try
        {
            Class c = Class.forName( className );
            viewBuilder = (TrackViewBuilder)c.newInstance();
            viewBuilder.init( properties );
        }
        catch( Exception e )
        {
            throw new RuntimeException(e);
        }
    }
    
    private static String getPathFromProps(Properties props)
    {
        String pathStr = props.getProperty( PROP_BIGBED_PATH );
        if(pathStr == null)
        {
            //Case when .bb file is stored in repository
            String filePath = props.getProperty( DataCollectionConfigConstants.FILE_PATH_PROPERTY );//set by parent GenericDataCollection
            String name = props.getProperty( DataCollectionConfigConstants.NAME_PROPERTY );
            if(filePath == null || name == null)
                throw new IllegalArgumentException(PROP_BIGBED_PATH + " or " + (DataCollectionConfigConstants.FILE_PATH_PROPERTY + " and " + DataCollectionConfigConstants.NAME_PROPERTY) + " should be specified");
            return Paths.get(filePath, name).toString();
        }
        else
            return pathStr;
    }
    

    //we inherit from DataCollection only to make BioUML use the constructor BigBedTrack(DataCollection<?> parent, Properties properties) when reading default.config
    @Override
    public List<String> getNameList()
    {
        return Collections.emptyList();
    }
    
    @Override
    protected DataElement doGet(String name) throws Exception
    {
        return null;
    }
    
    //Properties for BeanInfo editor
    @PropertyName ( "Chromosome name mapping" )
    @PropertyDescription ( "Chromosome name mapping" )
    public String getChrMapping()
    {
        if( chrMapping == null )
            return ChrNameMapping.NONE_MAPPING;
        return chrMapping.getName();
    }
    public void setChrMapping(String chrNameMappingStr)
    {
        if( chrMapping == null || !chrMapping.getName().equals( chrNameMappingStr ) )
        {
            setChrNameMapping( ChrNameMapping.getMapping( chrNameMappingStr ) );
            if( chrMapping != null )
                TrackUtils.addTrackProperty( this, ChrNameMapping.PROP_CHR_MAPPING, chrNameMappingStr );
            else
                TrackUtils.addTrackProperty( this, ChrNameMapping.PROP_CHR_MAPPING, null );
        }
    }

    @PropertyName ( "Genome (sequences collection)" )
    @PropertyDescription ( "Genome (sequences collection)" )
    public GenomeSelector getGenomeSelector()
    {
        return genomeSelector;
    }

    public void setGenomeSelector(GenomeSelector genomeSelector)
    {
        if( genomeSelector != this.genomeSelector )
        {
            GenomeSelectorTrackUpdater listener = new GenomeSelectorTrackUpdater( BigTrack.this, genomeSelector, () -> {
                chrCache.clear();
                String seqBaseStr = getInfo().getProperties().getProperty( Track.SEQUENCES_COLLECTION_PROPERTY );
                if( seqBaseStr != null )
                {
                    DataElementPath seqBase = DataElementPath.create( seqBaseStr );
                    chrCache.setSeqBase( seqBase );
                }
                return;
            } );
            genomeSelector.addPropertyChangeListener( listener );
        }
        this.genomeSelector = genomeSelector;
    }


}
