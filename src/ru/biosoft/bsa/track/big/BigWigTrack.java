package ru.biosoft.bsa.track.big;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.jetbrains.bio.CompressionType;
import org.jetbrains.bio.ScoredInterval;
import org.jetbrains.bio.big.BedGraphSection;
import org.jetbrains.bio.big.BigFile;
import org.jetbrains.bio.big.BigWigFile;
import org.jetbrains.bio.big.WigSection;

import com.developmentontheedge.beans.DynamicProperty;

import gnu.trove.list.TFloatList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import kotlin.Pair;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.Repository;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.security.Permission;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.util.TempFiles;

//Not thread safe, only Track methods are thread safe
public class BigWigTrack extends BigTrack
{
    public BigWigTrack(DataCollection<?> parent, Properties properties) throws IOException
    {
        this(parent, properties, true);
    }
    
    public BigWigTrack(DataCollection<?> parent, Properties properties, boolean open) throws IOException
    {
        super(parent, properties, open);
    }
    
    @Override
    protected BigWigFile getBBFile()
    {
        return (BigWigFile)bbFile;
    }
    
    @Override
    protected void doOpen() throws IOException
    {
        bbFile = BigWigFile.read(bbPath);
    }
    
    //sites should be sorted by chr,from
    public void write(List<Site> sites, Map<String, Integer> chromSizes) throws IOException
    {
        List<Pair<String, Integer>> kotlinChromSizes = chromSizes.entrySet().stream()
                .map( e->new kotlin.Pair<>( e.getKey(), e.getValue() ) )
                .collect( Collectors.toList() );
        List<BedGraphSection> wigSections = new ArrayList<>();
        
        String chr = null;
        TIntList startOffsets = null;
        TIntList endOffsets = null;
        TFloatList values = null;

        for(Site site : sites)
        {
            if(!site.getOriginalSequence().getName().equals( chr ))
            {
                if(chr != null)
                   addNewSection( wigSections, chr, startOffsets, endOffsets, values );
                chr = site.getOriginalSequence().getName();
                startOffsets = new TIntArrayList();
                endOffsets = new TIntArrayList();
                values = new TFloatArrayList();
            }
            startOffsets.add( site.getFrom() - 1 );
            endOffsets.add( site.getTo() );
            values.add( ((Number)site.getProperties().getValue( Site.SCORE_PROPERTY )).floatValue() );
        }
        if(!startOffsets.isEmpty())
            addNewSection( wigSections, chr, startOffsets, endOffsets, values );
        
        BigWigFile.write( wigSections, kotlinChromSizes, Paths.get(bbPath), 1, CompressionType.DEFLATE );
        //reopen
        close();open();
    }
    
    private static void addNewSection(List<BedGraphSection> sections, String chr, TIntList startOffsets, TIntList endOffsets, TFloatList values)
    {
        BedGraphSection section = new BedGraphSection( chr, startOffsets, endOffsets, values );
        sections.add( section );
    }
    
    public List<Site> query(String chr) throws IOException
    {
        chr = externalToInternalName( chr );
        List<WigSection> wigSections = getBBFile().query(chr);
        List<Site> result = fromWigSections( wigSections );
        return result;
    }
    
    //from, to - one based, both inclusive
    public List<Site> query(String chr, int from, int to) throws IOException
    {
        chr = externalToInternalName( chr );
        List<WigSection> bedList = getBBFile().query( chr, from-1, to, true );
        List<Site> result = fromWigSections( bedList );
        return result;
    }
    
    public float[] loadProfile(String chr, int from, int to) throws IOException
    {
        chr = externalToInternalName( chr );
        List<WigSection> bedList = getBBFile().query( chr, from-1, to, true );
        float[] result = new float[to - from + 1];
        for(WigSection bed : bedList)
        {
            Iterator<ScoredInterval> it = bed.query().iterator();
            while(it.hasNext())
            {
                ScoredInterval si = it.next();
                int start = si.component1();
                int end = si.component2();
                float score = si.component3();
                for(int x = start + 1; x <= end; x++)
                    result[x-from] = score;
            }
        }
        return result;
    }
    
    public float[] loadProfile(String chr) throws IOException
    {
        Sequence seq = getChromosomeSequence( chr );
        return loadProfile( chr, 1, seq.getLength() );
    }
    
    public int count(String chr, int from, int to) throws IOException
    {
        chr = externalToInternalName( chr );
        return bbFile.query( chr, from - 1, to, true ).size();
    }

    //TODO: methods to iterate over sites without loading all of them into memory
    public List<Site> queryAll() throws IOException
    {
        List<Site> result = new ArrayList<>();
        for(String chr : getChromosomes())
            result.addAll( query( chr ) );
        return result;
    }
    
    protected List<Site> fromWigSections(List<WigSection> bedList)
    {
        List<Site> result = new ArrayList<>(bedList.size());
        for(WigSection bed : bedList)
        {
            String chr = bed.getChrom();
            if(chrMapping != null)
                chr = internalToExternal( bed.getChrom() );
            
            Sequence seq = getChromosomeSequence( chr );
            
            Iterator<ScoredInterval> it = bed.query().iterator();
            while(it.hasNext())
            {
                ScoredInterval si = it.next();
                int start = si.component1();
                int end = si.component2();
                float score = si.component3();
                String name = chr + ":" + (start+1) + "-" + end;
                Site s = new SiteImpl( null, name, start + 1, end - start, StrandType.STRAND_NOT_KNOWN, seq );
                s.getProperties().add( new DynamicProperty( Site.SCORE_PD, Float.class, score ) );
                result.add( s );
            }
        }
        return result;
    }
    
    protected Object lock = new Object();
    @Override
    public DataCollection<Site> getSites(String chrPath, int from, int to)
    {
        String chrName = DataElementPath.create( chrPath ).getName();
        List<Site> siteList;
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
        for(Site s : siteList)
            result.put( s );
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
        List<Site> siteList;
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
        for(Site s : siteList)
        {
            if(s.getName().equals( siteName ))
              return s;
        }
        return null;
    }

    @Override
    public DataCollection<Site> getAllSites() throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException();
    }

    public static BigWigTrack create(DataElementPath path, Properties props) throws Exception
    {
        //A little bit of BioUML repository hell
        DataCollectionUtils.createFoldersForPath( path );
        
        String name = path.getName();
        DataCollection<?> parent = path.optParentCollection();
        
        Properties trackProperties = new Properties();
        trackProperties.put(DataCollectionConfigConstants.NAME_PROPERTY, name);
        trackProperties.put(DataCollectionConfigConstants.CLASS_PROPERTY, BigWigTrack.class.getName());
        trackProperties.put(DataCollectionConfigConstants.FILE_PROPERTY, name);
        trackProperties.putAll( props );

        BigWigTrack track;
        
        DataCollection<?> typeSpecificCollection = DataCollectionUtils.getTypeSpecificCollection(parent, BigWigTrack.class);
        if(typeSpecificCollection instanceof Repository)
        {
            Repository parentRepository = (Repository)typeSpecificCollection;
            track = (BigWigTrack)DataCollectionUtils.fetchPrimaryCollection(parentRepository.createDataCollection(name, trackProperties, null, null, null), Permission.WRITE);
        }
        else
        {
            File bamFolder = TempFiles.dir( ".bb_folder" );
            trackProperties.put( DataCollectionConfigConstants.FILE_PATH_PROPERTY, bamFolder.getAbsolutePath() );
            track = new BigWigTrack( parent, trackProperties );
        }
        return track;
    }
    
    @Override
    protected void initViewBuilder(Properties properties)
    {
        viewBuilder = new BigWigTrackViewBuilder();
    }
}
