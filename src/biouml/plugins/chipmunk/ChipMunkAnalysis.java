package biouml.plugins.chipmunk;

import java.util.ArrayList;
import java.util.List;

import ru.autosome.ChIPAct;
import ru.autosome.ChIPMunk;
import ru.autosome.assist.Conductor;
import ru.autosome.ytilib.MunkResult;
import ru.autosome.ytilib.Peak;
import ru.autosome.ytilib.Sequence;
import ru.autosome.ytilib.WPCM;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.security.SessionThread;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Nucleotide15LetterAlphabet;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.FrequencyMatrix;

/**
 * @author lan
 *
 */
@ClassIcon( "resources/ChIPMunk.gif" )
public class ChipMunkAnalysis extends AbstractChipMunkAnalysis<ChipMunkAnalysisParameters>
{
    public ChipMunkAnalysis(DataCollection origin, String name)
    {
        this(origin, name, new ChipMunkAnalysisParameters());
    }
    
    protected ChipMunkAnalysis(DataCollection origin, String name, ChipMunkAnalysisParameters parameters)
    {
        super(origin, name, parameters);
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        checkRange("gcPercent", -1.0, 1.0);
    }

    private MunkResult result;
    
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
        WPCM pwm = result.getWPCM();

        if( result != null )
        {
            DataCollection<FrequencyMatrix> libraryDE = getMatrixLibrary();

            FrequencyMatrix weightMatrix = new FrequencyMatrix( libraryDE, getParameters().getMatrixName(),
                    Nucleotide15LetterAlphabet.getInstance(), null, transposeMatrix( pwm.getMatrix() ), false );
            libraryDE.put( weightMatrix );
            log.info( "Matrix is stored in " + libraryDE.getCompletePath() );
            return new FrequencyMatrix[] {weightMatrix};
        }
        else
            return new FrequencyMatrix[0];
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
        if(getParameters().getGcPercent() >= 0 )
            actParameters.setGCPercent(getParameters().getGcPercent());
        return actParameters;
    }

    protected ChIPMunk.Parameters createMunkParameters()
    {
        ChIPMunk.Parameters munkParameters = new ChIPMunk.Parameters(getParameters().getStartLength(), getParameters().getStopLength(), false, getParameters().getZoopsFactor());
        munkParameters.setShapeProvider(getParameters().getShapeProvider());
        return munkParameters;
    }
}
