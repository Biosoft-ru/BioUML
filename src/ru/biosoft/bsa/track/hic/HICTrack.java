package ru.biosoft.bsa.track.hic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.ChrCache;
import ru.biosoft.bsa.ChrNameMapping;
import ru.biosoft.bsa.GenomeSelector;
import ru.biosoft.bsa.GenomeSelector.GenomeSelectorTrackUpdater;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.view.TrackViewBuilder;

//Hi-C track to show .hic files in the biouml genome browser
//Hi-C data actually doesn't fit good into @ru.biosoft.bsa.Track representation since it is not collection of sites
//Hi-C data represented as one site per chromosome
//HICTrack implements DataCollection to  have .getInfo().getProperties(), it is size always 0
public class HICTrack extends AbstractDataCollection<DataElement> implements Track, AutoCloseable {

	public static final String PROP_HIC_PATH = "HICPath";
	
	private String hicFilePath;

	private ChrNameMapping chrMapping;

	private ChrCache chrCache;

	private GenomeSelector genomeSelector;
	
	public HICTrack(DataCollection<?> parent, Properties properties) throws IOException {
		super(parent, properties);
		hicFilePath = getPathFromProps(properties);

		if (!isRemote())
			getInfo().addUsedFile(new File(hicFilePath));

		chrMapping = ChrNameMapping.getMapping(properties);

		DataElementPath seqBase = DataElementPath.create(properties.getProperty(Track.SEQUENCES_COLLECTION_PROPERTY));
		chrCache = new ChrCache(seqBase);
		setGenomeSelector(new GenomeSelector(this));
	}
	
	@Override
	public List<String> getNameList() {
		return Collections.emptyList();
	}
	@Override
	protected DataElement doGet(String name) throws Exception {
		return null;
	}

	@Override
	public DataCollection<Site> getSites(String chrPath, int from, int to) {
		DataCollection<Site> result = new VectorDataCollection<Site>(null, new Properties());
		result.put(getSiteForChromosome(chrPath, result));
		return result;
	}

	@Override
	public int countSites(String sequence, int from, int to) throws Exception {
		return 1;
	}

	@Override
	public Site getSite(String chrPath, String siteName, int from, int to) throws Exception {
		
		return getSiteForChromosome(chrPath, null);
	}
	
	private Site getSiteForChromosome(String chrPath, DataCollection<?> parent)
	{
		String chrName = DataElementPath.create( chrPath ).getName();
		Sequence sequence = chrCache.getSequence(chrName);
		Site site = new SiteImpl(parent, chrName, sequence.getStart(), sequence.getLength(), StrandType.STRAND_PLUS, sequence);
		return site;
	}

	@Override
	public DataCollection<Site> getAllSites() throws UnsupportedOperationException {
		throw new  UnsupportedOperationException();
	}

	
	private HICTrackViewBuilder viewBuilder;
	private Object biewBuilderLock = new Object();
	
	@Override
	public TrackViewBuilder getViewBuilder() {
		if(viewBuilder == null)
			synchronized(biewBuilderLock)
			{
				if(viewBuilder == null)
					viewBuilder = new HICTrackViewBuilder(this);
			}
		return viewBuilder;
	}


	public void setChrNameMapping(ChrNameMapping chrNameMapping)
    {
        this.chrMapping = chrNameMapping;
        chrCache.clear();
    }
    public String internalToExternal(String chr)
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
            GenomeSelectorTrackUpdater listener = new GenomeSelectorTrackUpdater( this, genomeSelector, () -> {
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


    
	
	private static String getPathFromProps(Properties props) {
		String pathStr = props.getProperty(PROP_HIC_PATH);
		if (pathStr == null) {
			// Case when .bb file is stored in repository
			String filePath = props.getProperty(DataCollectionConfigConstants.FILE_PATH_PROPERTY);// set by parent
																									// GenericDataCollection
			String name = props.getProperty(DataCollectionConfigConstants.NAME_PROPERTY);
			if (filePath == null || name == null)
				throw new IllegalArgumentException(
						PROP_HIC_PATH + " or " + (DataCollectionConfigConstants.FILE_PATH_PROPERTY + " and "
								+ DataCollectionConfigConstants.NAME_PROPERTY) + " should be specified");
			return Paths.get(filePath, name).toString();
		} else
			return pathStr;
	}

	public static boolean isRemotePath(String path) {
		return path.startsWith("http://") || path.startsWith("https://") || path.startsWith("ftp://");
	}

	public boolean isRemote() {
		return isRemotePath(hicFilePath);
	}

	public String getFilePath() {
		return hicFilePath;
	}
	    

}
