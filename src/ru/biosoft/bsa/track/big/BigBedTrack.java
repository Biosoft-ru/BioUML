package ru.biosoft.bsa.track.big;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.jetbrains.bio.CompressionType;
import org.jetbrains.bio.big.BedEntry;
import org.jetbrains.bio.big.BigBedFile;

import com.google.common.collect.Lists;

import kotlin.Pair;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.Repository;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.security.Permission;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.WithSite;
import ru.biosoft.util.LazyValue;
import ru.biosoft.util.TempFiles;

//Not thread safe, only Track methods are thread safe
public class BigBedTrack<T> extends BigTrack
{
    public static final String PROP_CONVERTER_CLASS = BedEntryConverter.PROP_PREFIX + "class";
    
    public static final String PROP_VIEW_BUILDER_CLASS = "ViewBuilderClass";
    public static final String PROP_UNIQUE_ID_COLUMN = "BigBedIdColumn";//0 - is the first column after chr\tstart\tend
    
    
    protected BedEntryConverter<T> converter;
    
    public BigBedTrack(DataCollection<?> parent, Properties properties) throws IOException
    {
        this(parent, properties, true);
    }
    
    public BigBedTrack(DataCollection<?> parent, Properties properties, boolean open) throws IOException
    {
        super(parent, properties, open);
        this.converter = createConverter( properties );
    }
    
    @Override
	public BigBedFile getBBFile()
    {
        return (BigBedFile)bbFile;
    }
    
    @Override
    protected void doOpen() throws IOException
    {
        bbFile = BigBedFile.read(bbPath);
    }
    
    //sites should be sorted by chr,from
    public void write(List<T> sites, Map<String, Integer> chromSizes) throws IOException
    {
        write(sites, chromSizes, 3, 3, null);
    }
    
    public void write(List<T> sites, Map<String, Integer> chromSizes, int totalFieldCount, int bedFieldCount, String autoSql) throws IOException
    {
        List<BedEntry> bedEntries = Lists.transform( sites, converter::toBedEntry );
        List<Pair<String, Integer>> kotlinChromSizes = chromSizes.entrySet().stream()
                .map( e->new kotlin.Pair<>( e.getKey(), e.getValue() ) )
                .collect( Collectors.toList() );
        try
        {
        	BigBedFile.write(bedEntries, kotlinChromSizes, Paths.get(bbPath), 1024, 1, CompressionType.DEFLATE,  ByteOrder.nativeOrder(),
                    null,//cancel checker
                    totalFieldCount,
                    bedFieldCount, //field count that are standard BED fields
                    autoSql);
        	//reopen
        	close();open();
        }
        catch (Exception e)
        {
        	Files.deleteIfExists(Paths.get(bbPath));
        	throw e;
        }
    }
   
    
    public List<T> query(String chr) throws IOException
    {
        chr = externalToInternalName( chr );
        List<BedEntry> bedList = getBBFile().query(chr);
        List<T> result = fromBedList( bedList );
        postProcessSites( result, chr );
        return result;
    }
    
    //from, to - one based, both inclusive
    public List<T> query(String chr, int from, int to) throws IOException
    {
        chr = externalToInternalName( chr );
        List<BedEntry> bedList = getBBFile().query( chr, from-1, to, true );
        List<T> result = fromBedList( bedList );
        postProcessSites( result, chr, from, to );
        return result;
    }
    
    protected void postProcessSites(List<T> sites, String chr, int from, int to) throws IOException {}
    protected void postProcessSites(List<T> sites, String chr) throws IOException {}
    
    public int count(String chr, int from, int to) throws IOException
    {
        chr = externalToInternalName( chr );
        return bbFile.query( chr, from - 1, to, true ).size();
    }

    //TODO: methods to iterate over sites without loading all of them into memory
    public List<T> queryAll() throws IOException
    {
        List<T> result = new ArrayList<>();
        for(String chr : getChromosomes())
            result.addAll( query( chr ) );
        return result;
    }
    
    protected List<T> fromBedList(List<BedEntry> bedList)
    {
        List<T> result = new ArrayList<>(bedList.size());
        for(BedEntry bed : bedList)
        {
            if(chrMapping != null)
            {
                String chr = internalToExternal( bed.getChrom() );
                bed = new BedEntry( chr, bed.getStart(), bed.getEnd(), bed.getRest() );
            }
            T t = converter.fromBedEntry( bed );
            result.add( t );
        }
        return result;
    }
    
    public BedEntryConverter<T> getConverter()
    {
        return converter;
    }
    public void setConverter(BedEntryConverter<T> converter)
    {
        this.converter = converter;
    }

    protected BedEntryConverter<T> createConverter(Properties props)
    {
        String className = props.getProperty( PROP_CONVERTER_CLASS, BedEntryToSite.class.getName() );
        if( className != null )
        {
            try
            {
                String plugins = props.getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY);
                Class<?> c = ClassLoading.loadClass( className, plugins );
                Constructor<?> constructor = c.getConstructor( BigBedTrack.class, Properties.class );
                return (BedEntryConverter<T>)constructor.newInstance( this, props );

            }
            catch( Exception e )
            {
                throw new RuntimeException( e );
            }
        }else
            return null;
    }
    
   
    
    //*** ru.biosoft.bsa.Track implementation
    
    protected Site transformToSite(Object obj)
    {
    	if(obj instanceof WithSite)
    		return ((WithSite) obj).getSite();
    	else
    		return (Site)obj;
    }

    protected Object lock = new Object();
    @Override
    public DataCollection<Site> getSites(String chrPath, int from, int to)
    {
        String chrName = DataElementPath.create( chrPath ).getName();
        List<?> siteList;
        try
        {
            synchronized(lock)
            {
                siteList = query( chrName, from, to );
            }
            
        }
        catch( IOException e )
        {
            throw new RuntimeException(e);
        }
        VectorDataCollection<Site> result = new VectorDataCollection<>( "" );
        for(Object s : siteList)
            result.put( transformToSite(s) );
        return result;
    }

    @Override
    public int countSites(String chrPath, int from, int to) throws Exception
    {
        String chrName = DataElementPath.create( chrPath ).getName();
        try
        {
            synchronized(lock)
            {
                return count( chrName, from, to );
            }
        }
        catch( IOException e )
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Site getSite(String chrPath, String siteName, int from, int to) throws Exception
    {
        String chrName = DataElementPath.create( chrPath ).getName();
        List<?> siteList;
        try
        {
            synchronized(lock)
            {
                siteList = query( chrName, from, to );
            }
        }
        catch( IOException e )
        {
            throw new RuntimeException(e);
        }
        for(Object obj : siteList)
        {
            Site s = transformToSite( obj );
            if(s.getName().equals( siteName ))
              return s;
            
        }
        return null;
    }

    private LazyValue<BigBedSitesCollection> lazyAllSites = new LazyValue<>( () -> new BigBedSitesCollection( BigBedTrack.this ) );
    @Override
    public DataCollection<Site> getAllSites() 
    {
        return lazyAllSites.get();
    }

    
    public static <T> BigBedTrack<T> create(DataElementPath path, Properties props) throws Exception
    {
        return create( path, props, BigBedTrack.class );
    }
    
    protected static <T extends BigBedTrack<S>, S> T create(DataElementPath path, Properties props, Class<T> clazz) throws Exception
    {
        //A little bit of BioUML repository hell
        DataCollectionUtils.createFoldersForPath( path );
        
        String name = path.getName();
        DataCollection<?> parent = path.optParentCollection();
        
        Properties trackProperties = new Properties();
        trackProperties.put(DataCollectionConfigConstants.NAME_PROPERTY, name);
        trackProperties.put(DataCollectionConfigConstants.CLASS_PROPERTY, clazz.getName());
        trackProperties.put(DataCollectionConfigConstants.FILE_PROPERTY, name);
        trackProperties.putAll( props );

        T track;
        
        DataCollection<?> typeSpecificCollection = DataCollectionUtils.getTypeSpecificCollection(parent, BigBedTrack.class);
        if(typeSpecificCollection instanceof Repository)
        {
            Repository parentRepository = (Repository)typeSpecificCollection;
            track = (T)DataCollectionUtils.fetchPrimaryCollection(parentRepository.createDataCollection(name, trackProperties, null, null, null), Permission.WRITE);
        }
        else
        {
            File bamFolder = TempFiles.dir( ".bb_folder" );
            trackProperties.put( DataCollectionConfigConstants.FILE_PATH_PROPERTY, bamFolder.getAbsolutePath() );
            Constructor<T> constructor = clazz.getConstructor( ru.biosoft.access.core.DataCollection.class, Properties.class );
            track = constructor.newInstance( parent, trackProperties );
        }
        return track;
    }
    
    
    @Override
    public List<String> getIndexes()
    {
        return bbFile.getExtraIndices().stream()
                .map(x->x.getName())
                .filter(x->x!=null)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Site> queryIndex(String index, String query)
    {
        List<BedEntry> bedList = getBBFile().queryExtraIndex( index, query );
        List<T> objList = fromBedList( bedList );
        List<Site> siteList = new ArrayList<>();
        for(T obj : objList)
            siteList.add( transformToSite( obj ) );
        return siteList;
    }
}
