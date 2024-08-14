package biouml.plugins.gtrd;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;

public class FAIREExperiment extends Experiment
{
    public FAIREExperiment(DataCollection<?> parent, String id)
    {
        super(parent, id);
    }
    
    public static final String PEAK_CALLER_MACS2 = "macs2";
    public static final String[] PEAK_CALLERS = new String[] {PEAK_CALLER_MACS2};
    
    public String[] getPeakCallers()
    {
        return PEAK_CALLERS;
    }

    public DataElementPath getMacs2Peaks()
    {
        return getPeaksByPeakCaller( PEAK_CALLER_MACS2 );
    }
    
    public DataElementPath getPeaksByPeakCaller(String peakCaller)
    {
        return DataElementPath.create( ChIPseqExperimentSQLTransformer.DEFAULT_GTRD_PEAKS + "/FAIRE-seq/" + peakCaller + "/" + this.getPeakId() );
    }

    public static final String DESIGN = "FAIRE-seq";
    
    @Override
    public String getDesign()
    {
        return DESIGN;
    }
}
