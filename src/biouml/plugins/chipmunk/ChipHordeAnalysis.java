package biouml.plugins.chipmunk;

import java.util.ArrayList;
import java.util.List;

import ru.autosome.ChIPAct;
import ru.autosome.ChIPApp.PreprocessMode;
import ru.autosome.ChIPHorde;
import ru.autosome.ChIPMunk;
import ru.autosome.assist.Conductor;
import ru.autosome.ytilib.MunkResult;
import ru.autosome.ytilib.WPCM;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.security.SessionThread;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.bsa.Nucleotide15LetterAlphabet;
import ru.biosoft.bsa.analysis.FrequencyMatrix;

/**
 * @author lan
 *
 */
@ClassIcon( "resources/ChIPHorde.gif" )
public class ChipHordeAnalysis extends ChipMunkAnalysis
{
    public ChipHordeAnalysis(DataCollection origin, String name)
    {
        super(origin, name, new ChipHordeAnalysisParameters());
    }

    @Override
    public void setParameters(AnalysisParameters params) throws IllegalArgumentException
    {
        try
        {
            parameters = (ChipHordeAnalysisParameters)params;
        }
        catch( Exception ex )
        {
            throw new IllegalArgumentException("Wrong parameters");
        }
    }

    @Override
    public ChipHordeAnalysisParameters getParameters()
    {
        return (ChipHordeAnalysisParameters)parameters;
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        checkRange("nMotifs", 1, 10);
    }

    @Override
    public FrequencyMatrix[] justAnalyzeAndPut() throws Exception
    {
        Conductor conductor = createConductor();
        ChIPAct.Parameters actParameters = createActParameters(conductor);
        ChIPMunk.Parameters munkParameters = createMunkParameters();
        
        ArrayList<Integer[]> ranges = new ArrayList<>();
        Integer[] range = new Integer[] {getParameters().getStartLength(), getParameters().getStopLength()};
        for(int i=0; i<getParameters().getNMotifs(); i++) ranges.add(range);
        
        final ChIPHorde chiphorde = new ChIPHorde(ranges, getParameters().getMode().equals("Mask") ? PreprocessMode.MASK : PreprocessMode.FILTER,
                actParameters, munkParameters);
        
        runThread(conductor, new SessionThread("ChipHorde-Main")
        {
            @Override
            public void run()
            {
                chiphorde.launchViaConductor();
            }
        });
        checkConductorState(conductor);
        DataCollection<FrequencyMatrix> libraryDE = getMatrixLibrary();

        List<MunkResult> results = chiphorde.getResult();
        List<FrequencyMatrix> matrices = new ArrayList<>();
        int mat = 0;
        for(MunkResult result: results)
        {
            mat++;
            FrequencyMatrix weightMatrix = new FrequencyMatrix(libraryDE, getParameters().getMatrixName() + "_" + mat, Nucleotide15LetterAlphabet.getInstance(), null, transposeMatrix(result.getWPCM().getMatrix()), false);
            libraryDE.put(weightMatrix);
            matrices.add(weightMatrix);
        }
        log.info(mat+" matrices are stored in "+libraryDE.getCompletePath());
        jobControl.resultsAreReady(matrices.toArray());
        return matrices.toArray(new FrequencyMatrix[matrices.size()]);
    }
}
