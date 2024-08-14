package biouml.plugins.gtrd.master.analyses;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.gtrd.ATACExperiment;
import biouml.plugins.gtrd.CellLine;
import biouml.plugins.gtrd.ChIPExperiment;
import biouml.plugins.gtrd.ChIPexoExperiment;
import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.DNaseExperiment;
import biouml.plugins.gtrd.ExperimentType;
import biouml.plugins.gtrd.FAIREExperiment;
import biouml.plugins.gtrd.HistonesExperiment;
import biouml.plugins.gtrd.MNaseExperiment;
import biouml.plugins.gtrd.analysis.OpenPerTFView;
import biouml.plugins.gtrd.analysis.OpenPerTFView.SpeciesSelector;
import biouml.plugins.gtrd.analysis.OpenPerTFView.TFSelector;
import biouml.plugins.gtrd.master.MasterTrack;
import biouml.plugins.gtrd.master.meta.Metadata;
import biouml.plugins.gtrd.master.meta.TF;
import biouml.plugins.gtrd.master.sites.GenomeLocation;
import biouml.plugins.gtrd.master.sites.MasterSite;
import biouml.plugins.gtrd.master.sites.PWMMotif;
import biouml.plugins.gtrd.master.sites.bsaconv.GEMChIPexoSiteConverter;
import biouml.plugins.gtrd.master.sites.bsaconv.Hotspot2DNaseSiteConverter;
import biouml.plugins.gtrd.master.sites.bsaconv.MACS2DNaseSiteConverter;
import biouml.plugins.gtrd.master.sites.bsaconv.PeakzillaChIPexoSiteConverter;
import biouml.plugins.gtrd.master.sites.bsaconv.SiteConverter;
import biouml.plugins.gtrd.master.sites.bsaconv.UnionGEMChIPSeqSiteConverter;
import biouml.plugins.gtrd.master.sites.bsaconv.UnionMACS2ChIPSeqSiteConverter;
import biouml.plugins.gtrd.master.sites.bsaconv.UnionPICSChIPSeqSiteConverter;
import biouml.plugins.gtrd.master.sites.bsaconv.UnionSISSRSChIPSeqSiteConverter;
import biouml.plugins.gtrd.master.sites.bsaconv.WellingtonHotspot2SiteConverter;
import biouml.plugins.gtrd.master.sites.bsaconv.WellingtonMACS2SiteConverter;
import biouml.plugins.gtrd.master.sites.chipexo.ChIPexoPeak;
import biouml.plugins.gtrd.master.sites.chipseq.ChIPSeqPeak;
import biouml.plugins.gtrd.master.sites.dnase.DNasePeak;
import biouml.plugins.gtrd.master.utils.StringPool;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.ChrIntervalMap;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.jobcontrol.Iteration;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;

public class ExportMasterTrack extends AnalysisMethodSupport<ExportMasterTrack.Parameters>
{
    public ExportMasterTrack(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        if(parameters.isAllFactors())
        {
            TFSelector tfSelector = new TFSelector();
            tfSelector.setBean( parameters );
            String[] tfList = tfSelector.getAvailableValues();
            for(int i = 0; i < tfList.length; i++)
            {
                String tf = tfList[i];
                if(tf.equals( TFSelector.NOT_SELECTED ))
                    continue;
                log.info( "Exporting " + tf );
                jobControl.pushProgress( i*100/tfList.length, (i+1)*100/tfList.length );
                parameters.setTf( tf );
                exportFactor( parameters.getUniprotId() );
                jobControl.popProgress();
            }
        }
        else
            exportFactor(parameters.getUniprotId());
        
        return new Object[] {};
    }

    public void exportFactor(String uniprotId) throws Exception, AssertionError
    {
        log.info( "Loading metadata" );
        jobControl.pushProgress( 0, 20 );
        long startTime = System.currentTimeMillis();
        Metadata metadata = ExportMetadata.loadMetadata(uniprotId, log);
        long time = System.currentTimeMillis();
        jobControl.popProgress();
        log.info( "Done loading metadata in " + (time - startTime) + "ms" );
        
        MasterTrack masterTrack = createMasterTrack(metadata);
        if(masterTrack == null)
        {
            log.info( "Output already exists, skipping" );
            return;
        }
        
        log.info( "Loading meta-clusters" );
        jobControl.pushProgress( 20, 40 );
        startTime= time;
        DataElementPath folder = DataElementPath.create( "databases/GTRD/Data/clusters/" + parameters.getOrganism().getLatinName() + "/By TF/" + uniprotId );
        SqlTrack metaClusters = folder.getChildPath( "meta clusters" ).getDataElement( SqlTrack.class );
        List<MasterSite> masterSites = createMasterSitesFromMetaClusters(metaClusters);
        time = System.currentTimeMillis();
        jobControl.popProgress();
        log.info( "Done loading meta-clusters in " + (time - startTime) + "ms" );
        

        if( parameters.isLoadChIPSeq() )
        {
            log.info( "Loading chip-seq peaks" );
            jobControl.pushProgress( 40, 50 );
            startTime = time;
            loadAllChIPSeqPeaks( metadata, folder, masterSites );
            time = System.currentTimeMillis();
            jobControl.popProgress();
            log.info( "Done loading chip-seq peaks in " + ( time - startTime ) + "ms" );
        }
        
        if( parameters.isLoadChIPExo() )
        {
            log.info( "Loading chip-exo peaks" );
            jobControl.pushProgress( 50, 60 );
            startTime = time;
            loadChipExoPeaks( uniprotId, metadata, masterSites );
            time = System.currentTimeMillis();
            jobControl.popProgress();
            log.info( "Done loading chip-exo peaks in " + ( time - startTime ) + "ms" );
        }
        
        if( parameters.isLoadMotifs() )
        {
            log.info( "Loading motifs" );
            jobControl.pushProgress( 60, 70 );
            startTime = time;
            metadata.siteModels = loadMotifs( folder, masterSites );
            time = System.currentTimeMillis();
            jobControl.popProgress();
            log.info( "Done loading motifs in " + ( time - startTime ) + "ms" );
        }
        
        if( parameters.isLoadDNase() )
        {
            log.info( "Loading dnase" );
            jobControl.pushProgress( 70, 80 );
            startTime = time;
            loadDnasePeaks( metadata, masterSites );
            time = System.currentTimeMillis();
            jobControl.popProgress();
            log.info( "Done loading dnase in " + ( time - startTime ) + "ms" );
        }
        
        log.info( "Writing master track" );
        jobControl.pushProgress( 80, 90 );
        startTime = time;
        writeMasterTrack(masterSites, masterTrack, metadata, metaClusters);
        time = System.currentTimeMillis();
        jobControl.popProgress();
        log.info("Done writing master track in " + (time - startTime) + "ms" );
    }

    private void writeMasterTrack(List<MasterSite> masterSites, MasterTrack masterTrack, Metadata metadata, SqlTrack origin) throws Exception
    {
        Map<String, Integer> chromSizes = new LinkedHashMap<>();
        for(DataElementPath chrPath : origin.getChromosomesPath().getChildren())
        {
            Sequence chrSeq = chrPath.getDataElement( AnnotatedSequence.class ).getSequence();
            chromSizes.put(chrSeq.getName(), chrSeq.getLength());
        }
        
        log.info("Writing metadata to " + masterTrack.getMetadataPath());        
        masterTrack.writeMetadata( metadata );
        
        log.info("Writing big bed to " + masterTrack.getFilePath());
        long startTime = System.currentTimeMillis();
        masterTrack.write( masterSites, chromSizes );
        log.info("Time to write big bed: " + (System.currentTimeMillis() - startTime) + "ms");

        masterTrack.getCompletePath().save( masterTrack );
    }

    private MasterTrack createMasterTrack(Metadata metadata) throws Exception
    {
        String name = "mt." + metadata.tf.uniprotName + ".v" + metadata.getVersion() + ".bb";
        DataElementPath sequencesPath = TrackUtils.getPrimarySequencesPath( TrackUtils.getEnsemblPath( parameters.getOrganism() ) );
        DataElementPath folder = parameters.getResultPath();
        DataElementPath path = folder.getChildPath( name );
        if(parameters.isAllFactors() && path.exists())
            return null;
        return createMasterTrack( path, sequencesPath );
    }
    
    public static MasterTrack createMasterTrack(DataElementPath path, DataElementPath sequencesCollectionPath) throws Exception
    {
        Properties props = new Properties();
        props.setProperty( Track.SEQUENCES_COLLECTION_PROPERTY, sequencesCollectionPath.toString() );
        return MasterTrack.create( path, props );
    }

    private List<MasterSite> createMasterSitesFromMetaClusters(SqlTrack metaClusters)
    {
        List<MasterSite> masterSites = new ArrayList<>();
        Collection<Site> metaClustersC = DataCollectionUtils.asCollection( metaClusters.getAllSites(), Site.class );
        jobControl.forCollection( metaClustersC, new Iteration<Site>() {
            public boolean run(Site mc)
            {
                MasterSite ms = new MasterSite();
                
                ms.setChr( StringPool.get( mc.getOriginalSequence().getName() ) );
                ms.setFrom( mc.getFrom() );
                ms.setTo( mc.getTo() );
                DynamicPropertySet ps = mc.getProperties();
                ms.setId( (Integer)ps.getValue( "id" ) );
                ms.setSummit( (Integer)ps.getValue( "summit" ) );
                masterSites.add( ms );
                return true;
            }
        });
        Collections.sort( masterSites, GenomeLocation.ORDER_BY_LOCATION );
        return masterSites;
    }

    public void loadAllChIPSeqPeaks(Metadata metadata, DataElementPath folder, List<MasterSite> masterSites)
            throws AssertionError
    {
        TIntObjectHashMap<MasterSite> masterSitesById = new TIntObjectHashMap<>();
        for(MasterSite ms : masterSites)
            masterSitesById.put( ms.getId(), ms );
        
        TIntIntHashMap clusterToMetaClusterMACS2 = new TIntIntHashMap();
        TIntIntHashMap clusterToMetaClusterSISSRS = new TIntIntHashMap();
        TIntIntHashMap clusterToMetaClusterGEM = new TIntIntHashMap();
        TIntIntHashMap clusterToMetaClusterPICS = new TIntIntHashMap();
        TableDataCollection table = folder.getChildPath( "meta cluster to cluster" ).getDataElement( TableDataCollection.class );
        int colMetaClusterId = table.getColumnModel().getColumnIndex( "Meta cluster id" );
        int colClusterId = table.getColumnModel().getColumnIndex( "Cluster id" );
        int colClusterPeakCaller = table.getColumnModel().getColumnIndex( "Cluster peak caller" );
        for(RowDataElement row : table)
        {
            Object[] values = row.getValues();
            int metaClusterId = (Integer)values[colMetaClusterId];
            int clusterId = (Integer)values[colClusterId];
            String peakCaller = (String)values[colClusterPeakCaller];
            TIntIntHashMap res;
            switch(peakCaller)
            {
                case "MACS2": res = clusterToMetaClusterMACS2;break;
                case "SISSRS": res = clusterToMetaClusterSISSRS; break;
                case "GEM": res = clusterToMetaClusterGEM; break;
                case "PICS": res = clusterToMetaClusterPICS; break;
                default:
                    throw new AssertionError();
            }
            res.put( clusterId, metaClusterId );
        }
        
        Function<String, ChIPseqExperiment> expSupplier = metadata.chipSeqExperiments::get;
        
        loadChIPSeqPeaks( folder, "MACS2", new UnionMACS2ChIPSeqSiteConverter( expSupplier ), masterSitesById, clusterToMetaClusterMACS2 );            
        loadChIPSeqPeaks( folder, "SISSRS", new UnionSISSRSChIPSeqSiteConverter( expSupplier ), masterSitesById, clusterToMetaClusterSISSRS );
        loadChIPSeqPeaks( folder, "GEM", new UnionGEMChIPSeqSiteConverter( expSupplier ), masterSitesById, clusterToMetaClusterGEM );
        loadChIPSeqPeaks( folder, "PICS", new UnionPICSChIPSeqSiteConverter( expSupplier ), masterSitesById, clusterToMetaClusterPICS );
    }

    public <T extends ChIPSeqPeak> void loadChIPSeqPeaks(DataElementPath clustersFolder, String peakCaller,
            SiteConverter<T> converter,
            TIntObjectHashMap<MasterSite> masterSitesById, TIntIntHashMap clusterToMetaCluster)
    {
        DataElementPath peaksPath = clustersFolder.getChildPath( peakCaller.toUpperCase() + " peaks" );
        if(!peaksPath.exists())
            return;
        Map<String, Integer> peakToCluster = loadPeakToCluster( clustersFolder.getChildPath( peakCaller.toUpperCase() + " cluster to peak" ) );
        SqlTrack peaks = peaksPath.getDataElement( SqlTrack.class );
        for( Site peak : peaks.getAllSites() )
        {
            DynamicPropertySet ps = peak.getProperties();
            String peakId = (String)ps.getValue( "id" );
            if(!peakToCluster.containsKey( peakId ))
                continue;
            int clusterId = peakToCluster.get( peakId );
            if(!clusterToMetaCluster.containsKey( clusterId ))
                continue;
            int metaClusterId = clusterToMetaCluster.get( clusterId );
            MasterSite ms = masterSitesById.get( metaClusterId );
            if(ms == null)
                log.severe( "Not found meta clusterId: " + metaClusterId );

            T chipSeqPeak = converter.createFromSite( peak );
            ms.getChipSeqPeaks().add( chipSeqPeak );
        }
    }

    private void loadDnasePeaks(Metadata metadata, List<MasterSite> masterSites)
    {
        Map<String, List<MasterSite>> mssByCells = new HashMap<>();
        for(MasterSite ms : masterSites)
        {
            Set<String> cells = new HashSet<>();
            for(ChIPSeqPeak p : ms.getChipSeqPeaks())
            {
                String cellId = p.getExp().getCell().getName();
                cells.add( cellId );
            }
            for(String cellId : cells)
                mssByCells.computeIfAbsent( cellId, k->new ArrayList<>() ).add( ms );
        }
        
        for(DNaseExperiment exp : metadata.dnaseExperiments.values())
        {
            String cellId = exp.getCell().getName();
            List<MasterSite> mssForCell = mssByCells.get(cellId);
            if(mssForCell == null)
                continue;
            
            loadDNasePeaksFromPaths( exp.getMacsPeaks(), mssForCell , new MACS2DNaseSiteConverter( exp ),  ms->ms.getDnasePeaks() );
            loadDNasePeaksFromPaths( exp.getHotspotPeaks(), mssForCell, new Hotspot2DNaseSiteConverter( exp ),  ms->ms.getDnasePeaks() );
            loadDNasePeaksFromPaths( exp.getMacsWelingtonPeaks(), mssForCell , new WellingtonMACS2SiteConverter( exp ),  ms->ms.getDnaseFootprints() );
            loadDNasePeaksFromPaths( exp.getHotspotWelingtonPeaks(), mssForCell , new WellingtonHotspot2SiteConverter( exp ),  ms->ms.getDnaseFootprints() );
        }
    }
    
    private <T extends DNasePeak>  void loadDNasePeaksFromPaths(DataElementPathSet trackPathSet, List<MasterSite> masterSites,
            SiteConverter<T> converter,
            Function<MasterSite, List<? super T>> targetListExtractor)
    {
        for(DataElementPath trackPath : trackPathSet)
        {
            SqlTrack track = trackPath.optDataElement( SqlTrack.class );
            ChrIntervalMap<T> peaks = new ChrIntervalMap<>(); 
            if(track == null)
                continue;
            
            int replicate = Integer.parseInt(trackPath.getName().split( "_", 2 )[1]);
            for(Site site : track.getAllSites())
            {
                T peak = converter.createFromSite( site );
                peak.setReplicate( replicate );
                peaks.add( peak.getChr(), peak.getFrom(), peak.getTo(), peak );
            }
            for(MasterSite ms : masterSites)
            {
                Collection<T> overlappingDNasePeaks = peaks.getIntervals( ms.getChr(), ms.getFrom(), ms.getTo() );
                List<? super T> targetList = targetListExtractor.apply( ms );
                targetList.addAll( overlappingDNasePeaks );
            }
        }
    }
    
    //ChIPexo
    private void loadChipExoPeaks(String tfUniprotId, Metadata metadata, List<MasterSite> masterSites)
    {
        for(ChIPexoExperiment exp : metadata.chipExoExperiments.values())
        {
            if(exp.getExpType() != ExperimentType.NORMAL && exp.getExpType() != ExperimentType.UNSPECIFIED)
                continue;
            if(!tfUniprotId.equals(exp.getTfUniprotId()))
                continue;
            DataElementPath trackPath = exp.getPeaksByPeakCaller( "gem" );
            if(trackPath.exists())
            {
                SqlTrack track = trackPath.getDataElement( SqlTrack.class );
                loadChipExoPeaksFromTrack(masterSites, track, new GEMChIPexoSiteConverter(exp));
            }
            trackPath = exp.getPeaksByPeakCaller( "peakzilla" );
            if(trackPath.exists())
            {
                SqlTrack track = trackPath.getDataElement( SqlTrack.class );
                loadChipExoPeaksFromTrack(masterSites, track, new PeakzillaChIPexoSiteConverter(exp));
            }
        }
    }
    
    private void loadChipExoPeaksFromTrack(List<MasterSite> masterSites, SqlTrack track, SiteConverter<? extends ChIPexoPeak> converter)
    {
        ChrIntervalMap<ChIPexoPeak> peaks = new ChrIntervalMap<>(); 
        
        for(Site site : track.getAllSites())
        {
            ChIPexoPeak peak = converter.createFromSite( site );
            peaks.add( peak.getChr(), peak.getFrom(), peak.getTo(), peak );
        }
        for(MasterSite ms : masterSites)
        {
            Collection<ChIPexoPeak> overlappingPeaks = peaks.getIntervals( ms.getChr(), ms.getFrom(), ms.getTo() );
            ms.getChipExoPeaks().addAll(overlappingPeaks);
        }
    }

    public DataElementPathSet loadMotifs(DataElementPath folder, List<MasterSite> masterSites)
    {
        DataElementPath siteModelOrigin = null;
        SqlTrack motifsTrack = folder.getChildPath( "motifs" ).optDataElement( SqlTrack.class );
        ChrIntervalMap<PWMMotif> motifsIndex = new ChrIntervalMap<>();
        
        Set<String> modelNames = new HashSet<>();
        if(motifsTrack != null)
        {
            for(Site s : motifsTrack.getAllSites())
            {
                PWMMotif m = new PWMMotif();
                m.setId( Integer.parseInt(s.getName()) );
                m.setChr( StringPool.get( s.getOriginalSequence().getName() ) );
                m.setFrom( s.getFrom() );
                m.setTo( s.getTo() );
                m.setForwardStrand( s.getStrand() == StrandType.STRAND_PLUS );
                DynamicPropertySet props = s.getProperties();
                m.setScore( ((Number)props.getValue( "score" )).floatValue() );
                SiteModel siteModel = (SiteModel)props.getValue( "siteModel" );
                String name = siteModel.getName().replaceFirst( "HOCOMOCOv11.", "" );
                modelNames.add( name );
                m.setSiteModelPath( siteModel.getCompletePath().getSiblingPath( name ) );
                motifsIndex.add( m.getChr(), m.getFrom(), m.getTo(), m );
            }
        }
        for(MasterSite ms : masterSites)
        {
            ms.getMotifs().addAll( motifsIndex.getIntervals( ms.getChr(), ms.getFrom() + ms.getSummit() - 300, ms.getFrom() + ms.getSummit() + 300 ) );
        }

        DataElementPath db = DataElementPath.create( "databases/HOCOMOCO v11/Data/PWM_HUMAN_mono_pval=0.0001" );
        return new DataElementPathSet( db, modelNames );
    }
    
    private Map<String, Integer> loadPeakToCluster(DataElementPath tablePath)
    {
        Map<String, Integer> res = new HashMap<>();
        if(!tablePath.exists())
            return res;
        TableDataCollection table = tablePath.getDataElement( TableDataCollection.class );
        int colClusterId = table.getColumnModel().getColumnIndex( "Cluster id" );
        int colPeakId = table.getColumnModel().getColumnIndex( "Peak id" );
        for(RowDataElement row : table)
        {
            Object[] values = row.getValues();
            int clusterId = (Integer)values[colClusterId];
            String peakId = (String)values[colPeakId];// PEAKS045178.174745
            res.put( peakId, clusterId );
        }
        return res;
    }
    

    

    public static class Parameters extends OpenPerTFView.Parameters
    {
        private boolean allFactors = false;
        public boolean isAllFactors()
        {
            return allFactors;
        }
        public void setAllFactors(boolean allFactors)
        {
            boolean oldValue = this.allFactors;
            this.allFactors = allFactors;
            firePropertyChange( "allFactors", oldValue, allFactors );
        }
        
        private DataElementPath resultPath;

        @PropertyName("Result path")
        public DataElementPath getResultPath()
        {
            return resultPath;
        }
        public void setResultPath(DataElementPath resultPath)
        {
            Object oldValue = this.resultPath;
            this.resultPath = resultPath;
            firePropertyChange( "resultPath", oldValue, resultPath );
        }

        private boolean loadDNase = true;

        public boolean isLoadDNase()
        {
            return loadDNase;
        }
        public void setLoadDNase(boolean loadDNase)
        {
            boolean oldValue = this.loadDNase;
            this.loadDNase = loadDNase;
            firePropertyChange( "loadDNase", oldValue, loadDNase );
        }
        
        private boolean loadChIPSeq = true;
        public boolean isLoadChIPSeq()
        {
            return loadChIPSeq;
        }
        public void setLoadChIPSeq(boolean loadChIPSeq)
        {
            boolean oldValue = this.loadChIPSeq;
            this.loadChIPSeq = loadChIPSeq;
            firePropertyChange( "loadChIPSeq", oldValue, loadChIPSeq );
        }
        
        private boolean loadChIPExo = true;
        public boolean isLoadChIPExo()
        {
            return loadChIPExo;
        }
        public void setLoadChIPExo(boolean loadChIPExo)
        {
            boolean oldValue = this.loadChIPExo;
            this.loadChIPExo = loadChIPExo;
            firePropertyChange( "loadChIPExo", oldValue, loadChIPExo );
        }

        private boolean loadMotifs = true;

        public boolean isLoadMotifs()
        {
            return loadMotifs;
        }
        public void setLoadMotifs(boolean loadMotifs)
        {
            boolean oldValue = this.loadMotifs;
            this.loadMotifs = loadMotifs;
            firePropertyChange( "loadMotifs", oldValue, loadMotifs );
        }
        
    }
    
    public static class ParametersBeanInfo extends OpenPerTFView.ParametersBeanInfo
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            property( "organism" ).editor( SpeciesSelector.class ).hideChildren().add();
            property("allFactors").add();
            property("tf").editor( TFSelector.class ).hidden( "isAllFactors" ).add();
            property("loadDNase").add();
            property("loadChIPSeq").add();
            property("loadChIPExo").add();
            property("loadMotifs").add();
            property("resultPath").outputElement( FolderCollection.class ).add();
        }
    }
}
