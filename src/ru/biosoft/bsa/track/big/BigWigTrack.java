package ru.biosoft.bsa.track.big;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;


import com.developmentontheedge.beans.DynamicProperty;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.Repository;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.security.Permission;
import ru.biosoft.bigbed.BigWigFile;
import ru.biosoft.bigbed.ChromInfo;
import ru.biosoft.bigbed.WigEntry;
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
    
    public List<Site> query(String chr) throws IOException
    {
    	ChromInfo chrInfo = getChromInfo(chr);
        if(chrInfo == null)
        	return Collections.emptyList();
        
        List<WigEntry> wigSections = getBBFile().queryIntervals(chrInfo.id, 0, chrInfo.length, 0);
        List<Site> result = fromWigEntries( wigSections );
        return result;
    }
    
    //from, to - one based, both inclusive
    public List<Site> query(String chr, int from, int to) throws IOException
    {
    	ChromInfo chrInfo = getChromInfo(chr);
        if(chrInfo == null)
        	return Collections.emptyList();
        
        List<WigEntry> wigEntries = getBBFile().queryIntervals(chrInfo.id, from-1, to, 0);
        List<Site> result = fromWigEntries( wigEntries );
        return result;
    }
    
    public float[] loadProfile(String chr, int from, int to) throws IOException
    {
    	float[] result = new float[to - from + 1];
    	
    	ChromInfo chrInfo = getChromInfo(chr);
        if(chrInfo == null)
        	return result;
        
        List<WigEntry> wigEntries = getBBFile().queryIntervals(chrInfo.id, from-1, to, 0);
        
        for(WigEntry wig : wigEntries)
        {
        	for(int x = wig.start + 1; x <= wig.end; x++)
        	{
        		int offset = x - from;
        		if(offset >= 0 && offset < result.length)
        			result[offset] = (float)wig.val;
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
        return bbFile.queryIntervals( chr, from - 1, to, 0 ).size();
    }

    public List<Site> queryAll() throws IOException
    {
        List<Site> result = new ArrayList<>();
        for(String chr : getChromosomes())
            result.addAll( query( chr ) );
        return result;
    }
    
	protected List<Site> fromWigEntries(List<WigEntry> bedList) {
		List<Site> result = new ArrayList<>(bedList.size());
		for (WigEntry bed : bedList) {
			ChromInfo chrInfo = chromById.get(bed.chrId);
			String chrName = chrInfo.name;
			chrName = internalToExternal(chrName);
			Sequence seq = getChromosomeSequence(chrName);
			int start = bed.start;
			int end = bed.end;
			double score = bed.val;
			String name = chrName + ":" + (start + 1) + "-" + end;
			Site s = new SiteImpl(null, name, start + 1, end - start, StrandType.STRAND_NOT_KNOWN, seq);
			s.getProperties().add(new DynamicProperty(Site.SCORE_PD, Float.class, score));
			result.add(s);
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
