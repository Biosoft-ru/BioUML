package biouml.plugins.gtrd;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;

public class MNaseExperiment extends Experiment
{
    public MNaseExperiment(DataCollection<?> parent, String id)
    {
        super(parent, id);
    }
    
    public static final String PEAK_CALLER_DANPOS2 = "danpos2";
    public static final String[] PEAK_CALLERS = new String[] {PEAK_CALLER_DANPOS2};
    
    public String[] getPeakCallers()
    {
        return PEAK_CALLERS;
    }

    public DataElementPath getDanpos2Peaks()
    {
        return getPeaksByPeakCaller( PEAK_CALLER_DANPOS2 );
    }
    
    public DataElementPath getPeaksByPeakCaller(String peakCaller)
    {
        return DataElementPath.create( ChIPseqExperimentSQLTransformer.DEFAULT_GTRD_PEAKS + "/MNase-seq/" + peakCaller + "/" + this.getPeakId() );
    }
    
    public static final String DESIGN = "MNase-seq";
    
    @Override
    public String getDesign()
    {
        return DESIGN;
    }
}
