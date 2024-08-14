package biouml.plugins.gtrd;

import biouml.standard.type.Species;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;

public class HistonesExperiment extends ChIPExperiment
{
    private String target;
    
    public HistonesExperiment(DataCollection<?> parent, String id)
    {
        super(parent, id);
    }
    
    public HistonesExperiment(DataCollection<?> parent, String id, String antibody, String target, CellLine cell, Species specie,
            String treatment, String controlId, ExperimentType expType) throws Exception
    {
        super(parent, id);
        this.cell = cell;
        this.specie = specie;
        this.treatment = treatment;
        this.antibody = antibody;
        this.target = target;
        this.controlId = controlId;
        this.expType = expType;
    }

    public String getTarget()
    {
        return target;
    }
    public void setTarget(String target)
    {
        this.target = target;
    }
    
    public static final String PEAK_CALLER_MACS2 = "macs2";
    public static final String[] PEAK_CALLERS = new String[] {PEAK_CALLER_MACS2};
    
    public String[] getPeakCallers()
    {
        return PEAK_CALLERS;
    }
    
    public DataElementPath getMacsPeaks()
    {
        return getPeakByPeakCaller( PEAK_CALLER_MACS2 );
    }
    
    public DataElementPath getPeakByPeakCaller(String peakCaller)
    {
        if( this.isControlExperiment() )
            return null;
    	
    	return DataElementPath.create( ChIPseqExperimentSQLTransformer.DEFAULT_GTRD_PEAKS + "/Histone Modifications/" + peakCaller + "/" + this.getPeakId() );
    }

    public static final String DESIGN = "ChIP-seq_HM";
    
    @Override
    public String getDesign()
    {
        return DESIGN;
    }
    
    @Override
    public boolean isControlExperiment()
    {
        return super.isControlExperiment() || expType == ExperimentType.HIST_CONTROL;
    }
}
