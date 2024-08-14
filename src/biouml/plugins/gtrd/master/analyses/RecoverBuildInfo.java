package biouml.plugins.gtrd.master.analyses;

import biouml.plugins.gtrd.master.MasterTrack;
import biouml.plugins.gtrd.master.meta.BuildInfo;
import biouml.plugins.gtrd.master.meta.Metadata;
import biouml.plugins.gtrd.master.sites.MasterSite;
import biouml.plugins.gtrd.master.sites.chipexo.ChIPexoPeak;
import biouml.plugins.gtrd.master.sites.chipseq.ChIPSeqPeak;
import biouml.plugins.gtrd.master.sites.dnase.DNaseCluster;
import biouml.plugins.gtrd.master.sites.dnase.DNaseFootprint;
import biouml.plugins.gtrd.master.sites.dnase.DNasePeak;
import biouml.plugins.gtrd.master.sites.dnase.FootprintCluster;
import biouml.plugins.gtrd.master.sites.histones.HistonesCluster;
import biouml.plugins.gtrd.master.sites.histones.HistonesPeak;
import biouml.plugins.gtrd.master.sites.mnase.MNasePeak;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.util.bean.BeanInfoEx2;

public class RecoverBuildInfo extends AnalysisMethodSupport<RecoverBuildInfo.Parameters>
{
    
    public RecoverBuildInfo(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        MasterTrack masterTrack = parameters.getInputMasterTrackPath().getDataElement( MasterTrack.class );
        Metadata metadata = masterTrack.getMetadata();
        metadata = new Metadata( metadata );
        metadata.setName( parameters.getOutputMetadataPath().getName() );
        metadata.setOrigin( parameters.getOutputMetadataPath().optParentCollection() );
        
        BuildInfo masterTrackInfo = metadata.buildInfo;
        masterTrackInfo.clear();
        
        for(String chr : masterTrack.getChromosomes())
        {
            for(MasterSite ms : masterTrack.query( chr ))
            {
                for(ChIPSeqPeak p : ms.getChipSeqPeaks())
                    masterTrackInfo.addChipSeqPeaks( p.getPeakCaller(), p.getExp().getPeakId() );
                for(ChIPexoPeak p : ms.getChipExoPeaks())
                    masterTrackInfo.addChipExoPeaks( p.getPeakCaller(), p.getExp().getPeakId() );
                for(DNasePeak p : ms.getDnasePeaks())
                    masterTrackInfo.addDNasePeaks( p.getPeakCaller(), p.getExp().getPeakId() );
                for(HistonesPeak p : ms.getHistonesPeaks())
                    masterTrackInfo.addHistonePeaks( p.getPeakCaller(), p.getExp().getPeakId() );
                for(DNaseFootprint p : ms.getDnaseFootprints())
                    masterTrackInfo.addFootprints( p.getPeakCaller(), p.getExp().getPeakId() );
                for(MNasePeak p : ms.getMnasePeaks())
                    masterTrackInfo.addMNasePeaks( p.getPeakCaller(), p.getExp().getPeakId() );
                for(DNaseCluster c : ms.getAtacClusters())
                    masterTrackInfo.addAtacClusters( c.getPeakCaller(), c.getCell().getName() );
                for(DNaseCluster c : ms.getFaireClusters())
                    masterTrackInfo.addFaireClusters( c.getPeakCaller(), c.getCell().getName() );
                for(DNaseCluster c : ms.getDnaseClusters())
                    masterTrackInfo.addDNaseClusters( c.getPeakCaller(), c.getCell().getName() );
                for(HistonesCluster c : ms.getHistonesClusters())
                    masterTrackInfo.addHistoneClusters( c.getPeakCaller(), c.getTarget(), c.getCell().getName(), 0 );
                for(FootprintCluster c : ms.getFootprintClusters())
                    masterTrackInfo.addFootprintClusters( c.getPeakCaller(), c.getCell().getName() );
                
            }
        }
        
        parameters.getOutputMetadataPath().save( metadata );
        return metadata;
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath inputMasterTrackPath;
        private DataElementPath outputMetadataPath;
        public DataElementPath getInputMasterTrackPath()
        {
            return inputMasterTrackPath;
        }
        public void setInputMasterTrackPath(DataElementPath inputMasterTrackPath)
        {
            Object  oldValue = this.inputMasterTrackPath;
            this.inputMasterTrackPath = inputMasterTrackPath;
            firePropertyChange( "inputMasterTrackPath", oldValue, inputMasterTrackPath );
        }
        public DataElementPath getOutputMetadataPath()
        {
            return outputMetadataPath;
        }
        public void setOutputMetadataPath(DataElementPath outputMetadataPath)
        {
            Object oldValue = this.outputMetadataPath;
            this.outputMetadataPath = outputMetadataPath;
            firePropertyChange( "outputMetadataPath", oldValue, outputMetadataPath );
        }
        
    }
    
    public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }
        
        protected void initProperties() throws Exception {
            property( "inputMasterTrackPath" ).inputElement( MasterTrack.class ).add();
            property( "outputMetadataPath" ).outputElement( Metadata.class ).add();
        }
    }
}
