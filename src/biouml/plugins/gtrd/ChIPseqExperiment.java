package biouml.plugins.gtrd;

import biouml.standard.type.Species;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;

public class ChIPseqExperiment extends ChIPTFExperiment
{
    public ChIPseqExperiment(DataCollection<?> parent, String id )
    {
        super( parent, id );
    }
    
    public ChIPseqExperiment(DataCollection<?> parent, String id, String antibody, String tfUniprotId, CellLine cell, Species specie,
            String treatment, String controlId, ExperimentType expType) throws Exception
    {
        super(parent, id);
        this.cell = cell;
        this.specie = specie;
        this.treatment = treatment;
        this.antibody = antibody;
        this.tfUniprotId = tfUniprotId;
        this.controlId = controlId;
        this.expType = expType;
    }

    public static final String PEAK_CALLER_PICS = "pics";
    public static final String PEAK_CALLER_GEM = "gem";
    public static final String PEAK_CALLER_SISSRS = "sissrs";
    public static final String PEAK_CALLER_MACS2 = "macs2";
    public static final String[] PEAK_CALLERS = new String[] {PEAK_CALLER_MACS2, PEAK_CALLER_SISSRS, PEAK_CALLER_GEM, PEAK_CALLER_PICS};
    
    public String[] getPeakCallers()
    {
        return PEAK_CALLERS;
    }
    
    public DataElementPath getMacsPeaks()
    {
        return getPeaksByPeakCaller( PEAK_CALLER_MACS2 );
    }
    
    public DataElementPath getSissrsPeaks()
    {
        return getPeaksByPeakCaller( PEAK_CALLER_SISSRS );
    }
    
    public DataElementPath getGemPeaks()
    {
        return getPeaksByPeakCaller( PEAK_CALLER_GEM );
    }
    
    public DataElementPath getPicsPeaks()
    {
        return getPeaksByPeakCaller(PEAK_CALLER_PICS);
    }
    
    public DataElementPath getPeaksByPeakCaller(String peakCaller)
    {
        if( this.isControlExperiment() )
            return null;
        return DataElementPath.create( ChIPseqExperimentSQLTransformer.DEFAULT_GTRD_PEAKS + "/" + peakCaller + "/" + getPeakId() );
    }

    public static final String DESIGN = "ChIP-seq";
    
    @Override
    public String getDesign()
    {
        return DESIGN;
    }
}
