package biouml.plugins.gtrd.master.analyses.percellclusters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import biouml.plugins.gtrd.master.MasterTrack;
import biouml.plugins.gtrd.master.meta.Metadata;
import biouml.plugins.gtrd.master.sites.MasterSite;
import biouml.plugins.gtrd.master.sites.chipexo.ChIPexoPeak;
import biouml.plugins.gtrd.master.sites.chipseq.ChIPSeqPeak;
import biouml.plugins.gtrd.master.sites.dnase.DNaseCluster;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.track.big.BigBedTrack;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.bean.BeanInfoEx2;

public class ExportPerCellClusters extends AnalysisMethodSupport<ExportPerCellClusters.Parameters>
{
    public ExportPerCellClusters(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        MasterTrack mt = parameters.getMasterTrack().getDataElement( MasterTrack.class );
        
        Map<String, List<PerCellCluster>> byCell = new HashMap<>();
        Metadata metadata = mt.getMetadata();
        for(String cell : metadata.cells.keySet())
            byCell.put(cell, new ArrayList<>());
        
        for(String chr : mt.getChromosomes())
            for(MasterSite ms : mt.query( chr ))
            {
                Map<String, PerCellCluster> clusters = new HashMap<>();
                Set<String> chipSeqExperiments = new HashSet<>();
                for(ChIPSeqPeak p : ms.getChipSeqPeaks())
                {
                    String cell = p.getExp().getCell().getName();
                    if(!clusters.containsKey( cell ))
                        clusters.put( cell, createPerCellCluster(ms) );
                    chipSeqExperiments.add( p.getExp().getName() );
                }
                for(String expId : chipSeqExperiments)
                {
                    String cellId = metadata.chipSeqExperiments.get( expId ).getCell().getName();
                    clusters.get( cellId ).chipSeqExpCount++;
                }
                
                Set<String> chipExoExperiments = new HashSet<>();
                for(ChIPexoPeak p : ms.getChipExoPeaks())
                {
                    String cell = p.getExp().getCell().getName();
                    if(!clusters.containsKey( cell ))
                        clusters.put( cell, createPerCellCluster(ms) );
                    chipExoExperiments.add( p.getExp().getName() );
                }
                for(String expId : chipExoExperiments)
                {
                    String cellId = metadata.chipExoExperiments.get( expId ).getCell().getName();
                    clusters.get( cellId ).chipExoExpCount++;
                }
                
                for(DNaseCluster dnase : ms.getDnaseClusters())
                {
                    String cell = dnase.getCell().getName();
                    PerCellCluster cluster = clusters.get( cell );
                    if(cluster == null)
                        continue;
                    cluster.dnasePeakCount += dnase.getPeakCount();
                }
                for(PerCellCluster cluster : clusters.values())
                    cluster.motifCount = ms.getMotifs().size();
                
                clusters.forEach( (cell, cluster) -> byCell.get( cell ).add( cluster ) );
            }
        
        for(String cell : byCell.keySet())
        {
            List<PerCellCluster> clusters = byCell.get( cell );
            
            String uniprotName = metadata.tf.uniprotName;
            uniprotName = TextUtil.split( uniprotName, '_' )[0];//remove _HUMAN
            String name = uniprotName + "_" + metadata.tf.uniprotId + "_Metaclusters_" + cell + ".bb";
            DataElementPath outPath = parameters.getOutFolder().getChildPath( name );
            
            Properties props = new Properties();
            props.setProperty( BigBedTrack.PROP_CONVERTER_CLASS, BedEntryToPerCellCluster.class.getName() );
            String seqCollectionPath = mt.getInfo().getProperty( Track.SEQUENCES_COLLECTION_PROPERTY );
            props.setProperty( Track.SEQUENCES_COLLECTION_PROPERTY, seqCollectionPath );
            
            BigBedTrack<PerCellCluster> bbTrack = BigBedTrack.create( outPath, props );
            bbTrack.write( clusters, mt.getChromSizes() );
            outPath.save( bbTrack );
        }
        return parameters.getOutFolder();
    }

    private PerCellCluster createPerCellCluster(MasterSite ms)
    {
        PerCellCluster res = new PerCellCluster();
        res.setChr( ms.getChr() );
        res.setFrom( ms.getFrom() );
        res.setTo( ms.getTo() );
        res.setSummit( ms.getSummit() );
        res.setMasterSiteId( ms.getStableId() );
        return res;
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath masterTrack;
        private DataElementPath outFolder;
        public DataElementPath getMasterTrack()
        {
            return masterTrack;
        }
        public void setMasterTrack(DataElementPath masterTrack)
        {
            Object oldValue = this.masterTrack;
            this.masterTrack = masterTrack;
            firePropertyChange( "masterTrack", oldValue, masterTrack );
        }
        public DataElementPath getOutFolder()
        {
            return outFolder;
        }
        public void setOutFolder(DataElementPath outFolder)
        {
            Object oldValue = this.outFolder;
            this.outFolder = outFolder;
            firePropertyChange( "outFolder", oldValue, outFolder );
        }
    }
    
    public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
    {
        public ParametersBeanInfo() {
            super( Parameters.class );
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            property("masterTrack").inputElement( MasterTrack.class ).add();
            property("outFolder").outputElement( FolderCollection.class ).add();
        }
    }
}
