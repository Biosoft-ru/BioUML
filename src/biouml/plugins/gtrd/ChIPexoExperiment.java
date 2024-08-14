package biouml.plugins.gtrd;

import biouml.standard.type.Species;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;

public class ChIPexoExperiment extends ChIPTFExperiment
{
    private static final String DESIGN = "ChIP-exo";

    public ChIPexoExperiment(DataCollection<?> parent, String id )
    {
        super( parent, id );
    }
    
    public ChIPexoExperiment(DataCollection<?> parent, String id, String antibody, String tfUniprotId, CellLine cell, Species specie,
            String treatment, String controlId, ExperimentType expType) throws Exception
    {
        super(parent, id );
        this.cell = cell;
        this.specie = specie;
        this.treatment = treatment;
        this.antibody = antibody;
        this.tfUniprotId = tfUniprotId;
        this.controlId = controlId;
        this.expType = expType;
    }

    public static final String PEAK_CALLER_PEAKZILLA = "peakzilla";
    public static final String PEAK_CALLER_GEM = "gem";
    public static final String[] PEAK_CALLERS = new String[] {PEAK_CALLER_GEM, PEAK_CALLER_PEAKZILLA};
    
    public String[] getPeakCallers()
    {
        return PEAK_CALLERS;
    }
    
    public DataElementPath getGemPeaks()
    {
        return getPeaksByPeakCaller( PEAK_CALLER_GEM );
    }
    
    public DataElementPath getPeakzillaPeaks()
    {
        return getPeaksByPeakCaller( PEAK_CALLER_PEAKZILLA );
    }
    
    public DataElementPath getPeaksByPeakCaller(String peakCaller)
    {
    	if( this.isControlExperiment() )
            return null;
    	return DataElementPath.create( ChIPseqExperimentSQLTransformer.DEFAULT_GTRD_PEAKS + "/ChIP-exo/" + peakCaller + "/" + getPeakId() );
    }
    
    @Override
    public String getDesign()
    {
        return DESIGN;
    }
    
    @Override
    public boolean isControlExperiment()
    {
        return super.isControlExperiment() || expType == ExperimentType.CHIPEXO_CONTROL;
    }
}
