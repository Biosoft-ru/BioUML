package biouml.plugins.chipmunk;

import java.util.ArrayList;
import java.util.List;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.security.SessionThread;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.DiNucleotideAlphabet;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.autosome.di.ChIPAct;
import ru.autosome.di.ChIPMunk;
import ru.autosome.assist.AMatrix;
import ru.autosome.assist.Conductor;
import ru.autosome.di.ytilib.MunkResult;
import ru.autosome.di.ytilib.Peak;
import ru.autosome.di.ytilib.Sequence;

/**
 * @author lan
 *
 */
@ClassIcon( "resources/ChIPMunk.gif" )
public class DiChipMunkAnalysis extends AbstractChipMunkAnalysis<DiChipMunkAnalysisParameters>
{
    public DiChipMunkAnalysis(DataCollection origin, String name)
    {
        super(origin, name, new DiChipMunkAnalysisParameters());
    }

    protected DiChipMunkAnalysis(DataCollection origin, String name, DiChipMunkAnalysisParameters parameters)
    {
        super(origin, name, parameters);
    }

    MunkResult result;
    @Override
    public FrequencyMatrix[] justAnalyzeAndPut() throws Exception
    {
        Conductor conductor = createConductor();
        ChIPAct.Parameters actParameters = createActParameters(conductor);
        ChIPMunk.Parameters munkParameters = createMunkParameters();
        
        final ChIPMunk chipmunk = new ChIPMunk(actParameters, munkParameters);
        
        runThread(conductor, new SessionThread("ChipMunk-Main")
        {
            @Override
            public void run()
            {
                result = (MunkResult)chipmunk.launchViaConductor();
            }
        });
        checkConductorState(conductor);
        DataCollection<FrequencyMatrix> libraryDE = getMatrixLibrary();
        
        FrequencyMatrix weightMatrix = new FrequencyMatrix(libraryDE, getParameters().getMatrixName(), DiNucleotideAlphabet.getInstance(), null, transposeMatrix(result.getWPCM().getMatrix()), false);
        libraryDE.put(weightMatrix);
        log.info("Matrix is stored in "+libraryDE.getCompletePath());
        return new FrequencyMatrix[]{weightMatrix};
    }

    protected Sequence[] getSequences()
    {
        log.info("Fetching sequences...");
        List<Sequence> sequenceSet = new ArrayList<>();
        
        try
        {
            for( Site site : parameters.getInputSequences().getDataElement(Track.class).getAllSites() )
            {
                String seqStr = getSequenceString(site.getSequence());
                double[] values = (double[])site.getProperties().getValue("profile");
                Sequence sequence = parameters.isUseProfiles() && values != null ? new Peak(seqStr, values) : new Sequence(seqStr);
                sequenceSet.add(sequence);
            }
        }
        catch( Exception e )
        {
            for( AnnotatedSequence as : parameters.getInputSequences().getDataCollection(AnnotatedSequence.class) )
            {
                String seq = getSequenceString(as.getSequence());
                Sequence chipSequence = new Sequence(seq);
                sequenceSet.add(chipSequence);
            }
        }
        return sequenceSet.toArray(new Sequence[sequenceSet.size()]);
    }

    protected ChIPAct.Parameters createActParameters(Conductor conductor)
    {
        ArrayList<Sequence[]> sequenceSets = new ArrayList<>();
        sequenceSets.add(getSequences());
        ChIPAct.Parameters actParameters = new ChIPAct.Parameters(conductor, sequenceSets);
        actParameters.setThreadCount(getParameters().getThreadCount());
        actParameters.setStepLimit(getParameters().getStepLimit());
        actParameters.setTryLimit(getParameters().getTryLimit());
        actParameters.setLocalBackground(getParameters().isLocalBackground());
        return actParameters;
    }

    protected ChIPMunk.Parameters createMunkParameters()
    {
        ChIPMunk.Parameters munkParameters = new ChIPMunk.Parameters(getParameters().getStartLength(), getParameters().getStopLength(), false, getParameters().getZoopsFactor());
        munkParameters.setShapeProvider(getParameters().getShapeProvider());
        return munkParameters;
    }
}
