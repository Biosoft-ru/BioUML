package biouml.plugins.gtrd.master;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.jetbrains.bio.big.BedEntry;

import biouml.plugins.gtrd.DNaseExperiment;
import biouml.plugins.gtrd.master.index.ListOfSitesWrapper;
import biouml.plugins.gtrd.master.meta.Metadata;
import biouml.plugins.gtrd.master.meta.json.MetadataSerializer;
import biouml.plugins.gtrd.master.sites.GenomeLocation;
import biouml.plugins.gtrd.master.sites.MasterSite;
import biouml.plugins.gtrd.master.sites.MasterSite.Status;
import biouml.plugins.gtrd.master.sites.bedconv.BedEntryToMasterSite;
import biouml.plugins.gtrd.master.sites.dnase.DNasePeak;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.track.big.BedEntryConverter;
import ru.biosoft.bsa.track.big.BigBedTrack;
import ru.biosoft.rtree.RTree;

//MasterTrack is a collection of MasterSites stored as bigBed file along with the corresponding metadata json file
public class MasterTrack extends BigBedTrack<MasterSite>
{
    private Metadata metadata;
    private Path metadataPath;
    
    public static final String PROP_HIDE_RETIRED = "hideRetired";
    private boolean hideRetired;
    
    public MasterTrack(DataCollection<?> parent, Properties props) throws IOException
    {
        this(parent, props,  true);
    }
    
    public MasterTrack(DataCollection<?> parent, Properties properties, boolean open) throws IOException
    {
        super( parent, properties, false );
        this.metadataPath = getMetadataPath(Paths.get(bbPath));
        hideRetired = Boolean.parseBoolean( properties.getProperty( PROP_HIDE_RETIRED, "false" ) );
        if(open)
            open();
    }

    @Override
    protected void initViewBuilder(Properties properties)
    {
        viewBuilder = new MasterTrackViewBuilder();
    }
    
    
    @Override
    protected BedEntryConverter createConverter(Properties props)
    {
        return null;//will be initialized later, after metadata
    }
    
    @Override
    public void open() throws IOException
    {
        super.open();
        if(Files.exists( metadataPath ))
        {
            readMetadata( );
            converter = new BedEntryToMasterSite( this, new Properties() );
        }
        initAnnotations();
    }
    
    @Override
    public void close()
    {
        super.close();
        metadata = null;
        converter = null;
    }
    
    public String getStableName()
    {
        return "mt." + metadata.tf.uniprotName + ".v" + metadata.getVersion() + ".bb";
    }
    
    public Path getMetadataPath()
    {
        return metadataPath;
    }

    public static Path getMetadataPath(Path bbPath)
    {
        String name = bbPath.getFileName().toString();
        if(name.toLowerCase().endsWith( ".bb" ) || name.toLowerCase().endsWith( ".bigbed" ))
        {
            int dotIdx = name.lastIndexOf( '.' );
            name = name.substring( 0, dotIdx );
        }
        name += ".json";
        
        return bbPath.resolveSibling( name );
    }
    
    public void writeMetadata(Metadata metadata) throws IOException
    {
        setMetadata( metadata );
        MetadataSerializer serializer = new MetadataSerializer();
        String json = serializer.toJSON( metadata );
        Files.write( metadataPath, json.getBytes( "utf8" ) );
    }

    private void readMetadata() throws IOException
    {
        setMetadata( MetadataSerializer.readMetadata( metadataPath ) );
    }
    
    public Metadata getMetadata()
    {
        return metadata;
    }
    private void setMetadata(Metadata metadata)
    {
        this.metadata = metadata;
        if(metadata != null)
            converter =  new BedEntryToMasterSite( this, new Properties() );
        else
            converter = null;
    }
    
    
    /**
     * Special implementation to fetch masterSite subcomponents by site name (stable id),
     * This method is used by genome browser when selecting individual components of masterSite.
     * 
     * siteName - stable name of site, one of:
     *   masterSite (ms.AHR_HUMAN.123.v2)
     *   chipSeqPeakId (p.EXP003098.gem.10719),
     *   chipExoPeakId (p.EEXP003098.gem.10719),
     *   dnasePeakId (p.DEXP003098_1.macs2.10719),
     *   dnaseFootprintId (p.DEXP003098_1.wellington_macs2.10719),
     *   dnaseClusterId (dc.CELL001234.macs2.3343),
     *   histonesPeakId (p.HEXP003098.macs2.10719),
     *   histoneClusterId (hc.H3K27me.CELL001234.macs2.3343),
     *   mnasePeakId (p.MEXP003098.macs2.10719),
     *   motifId (w.HOCOMOCOv11.AHR_HUMAN.H11MO.0.B.6397)
     */
    @Override
    public Site getSite(String chrPath, String siteName, int from, int to) throws Exception
    {
        if(siteName.startsWith( MasterSite.PREFIX ))
            return super.getSite( chrPath, siteName, from, to );
        for(Site site : getSites( chrPath, from, to ))
        {
            MasterSite ms = (MasterSite)site;
            List<List<? extends GenomeLocation>> collections = Arrays.asList(
                    ms.getChipSeqPeaks(), ms.getChipExoPeaks(),
                    ms.getMotifs(),
                    ms.getDnaseClusters(), ms.getDnasePeaks(),
                    ms.getDnaseFootprints(),ms.getFootprintClusters(),
                    ms.getAtacClusters(),
                    ms.getFaireClusters(),
                    ms.getHistonesPeaks(), ms.getHistonesClusters(),
                    ms.getMnasePeaks() );
            for(List<? extends GenomeLocation> list : collections )
                for(GenomeLocation s : list)
                {
                    if(s.getStableId().equals( siteName ))
                        return s;
                }
        }
        return null;
    }
    
    @Override
    protected List<MasterSite> fromBedList(List<BedEntry> bedList)
    {
        List<MasterSite> result = super.fromBedList( bedList );
        if(!hideRetired)
            return result;
        return result.stream().filter( ms->ms.getStatus()!=Status.RETIRED ).collect( Collectors.toList() );
    }
    
    @Override
    public int count(String chr, int from, int to) throws IOException
    {
        if(hideRetired)
            return query( chr, from, to ).size();
        else
            return super.count( chr, from, to );
    }

    //Allow to add dnase annotations on the fly
    public static final String PROP_DNASE_FOLDER = "dnase";//ru.biosoft.access.core.DataElementPath to folder with macs2/DPEAKSXXXXXX_1.bb and hotspot2/DPEAKSXXXXXX_1.bb files
    
    private List<BigBedTrack<DNasePeak>> dnaseTracks;
    private void initAnnotations()
    {
        String dnaseFolder = getInfo().getProperties().getProperty( PROP_DNASE_FOLDER );
        if(dnaseFolder != null)
        {
            DataElementPath folderPath = DataElementPath.create( dnaseFolder );
            dnaseTracks = new ArrayList<>();
            for(DNaseExperiment exp : metadata.dnaseExperiments.values())
            {
                for( String peakCaller : DNaseExperiment.OPEN_CHROMATIN_PEAK_CALLERS )
                    for( String repId : exp.getRepIds() )
                    {
                        //file name pattern: DPEAKS002068_rep1_MACS2_421.bb
                        DataElementPath trackPath = folderPath.getChildPath( peakCaller.toUpperCase(), exp.getPeakId() + "_" + repId + ".bb" );
                        if( trackPath.exists() )
                        {
                            BigBedTrack<DNasePeak> track = trackPath.getDataElement( BigBedTrack.class );
                            dnaseTracks.add( track );
                        }
                    }
            }
        }
    }
    
    private void addAnnotations(List<MasterSite> mss, String chr) throws IOException
    {
        if(dnaseTracks != null)
        {
            RTree index = new RTree();
            index.build( new ListOfSitesWrapper( mss ) );
            for( BigBedTrack<DNasePeak> track : dnaseTracks )
            {
                List<DNasePeak> chrPeaks = track.query( chr );
                for(DNasePeak peak : chrPeaks)
                    index.findOverlapping( peak.getFrom(), peak.getTo(), idx->{
                        MasterSite ms = mss.get( idx );
                        ms.getDnasePeaks().add( peak );
                    } );
            }
        }
    }
    
    private void addAnnotations(List<MasterSite> mss, String chr, int from, int to) throws IOException
    {
        if(dnaseTracks != null)
            for( BigBedTrack<DNasePeak> track : dnaseTracks )
                for( MasterSite ms : mss )
                {
                    List<DNasePeak> overlappingPeaks = track.query( ms.getChr(), ms.getFrom(), ms.getTo() );
                    ms.getDnasePeaks().addAll( overlappingPeaks );
                }
    }
    
    
    @Override
    protected void postProcessSites(List<MasterSite> sites, String chr) throws IOException
    {
        addAnnotations( sites, chr );
    }
    
    @Override
    protected void postProcessSites(List<MasterSite> sites, String chr, int from, int to) throws IOException
    {
        addAnnotations( sites, chr, from, to );
    }
    
    public static MasterTrack create(DataElementPath path, Properties props) throws Exception
    {
        return create( path, props, MasterTrack.class );
    }
}
