package biouml.plugins.chipmunk;

import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

import ru.autosome.assist.Conductor;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.SessionThreadFactory;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.WeightMatrixCollection;
import ru.biosoft.jobcontrol.JobControl;

/**
 * @author lan
 */
public abstract class AbstractChipMunkAnalysis<T extends AbstractChipMunkParameters> extends AnalysisMethodSupport<T>
{
    public AbstractChipMunkAnalysis(DataCollection<?> origin, String name, T parameters)
    {
        super(origin, name, parameters);
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkPaths(parameters.getInputNames(), new String[] {"outputLibrary"});
        checkNotEmpty("matrixName");
        checkRange("startLength", 5, 30);
        checkRange("stopLength", 5, 30);
        checkRange("threadCount", 1, SecurityManager.getMaximumThreadsNumber());
        checkRange("stepLimit", 1, 100);
        checkRange("tryLimit", 1, 1000);
        checkRange("zoopsFactor", 0.0, 10.0);
        if( ! ( DataCollectionUtils.checkPrimaryElementType(parameters.getOutputLibrary().optParentCollection(),
                FolderCollection.class) ) )
            throw new IllegalArgumentException("Output library must be located in your project");
    }

    protected static String getSequenceString(ru.biosoft.bsa.Sequence sequence)
    {
        StringBuilder seq = new StringBuilder();
        byte[] buffer = new byte[1024];
        int length = sequence.getLength();
        int start = sequence.getStart();
        for(int i=0; i<length; i++)
        {
            buffer[i%buffer.length] = sequence.getLetterAt(i+start);
            if((i+1)%buffer.length==0)
            {
                seq.append(new String(buffer, StandardCharsets.ISO_8859_1));
            }
        }
        seq.append(new String(buffer, 0, length%buffer.length, StandardCharsets.ISO_8859_1));
        return seq.toString();
    }

    /**
     * @param matrix
     * @return
     */
    protected static double[][] transposeMatrix(double[][] matrix)
    {
        int length = matrix[0].length;
        double[][] result = new double[length][];
        for(int i=0; i<length; i++)
        {
            result[i] = new double[matrix.length];
            for(int j=0; j<matrix.length; j++)
                result[i][j] = matrix[j][i];
        }
        return result;
    }

    protected Conductor createConductor()
    {
        Conductor conductor = new Conductor();
        conductor.setOutputPrinter(new LogPrinter(log, Level.INFO));
        conductor.setMessagePrinter(new LogPrinter(log, Level.FINE));
        conductor.setThreadFactory(new SessionThreadFactory());
        return conductor;
    }

    /**
     * Throws an Exception if conductor in erroneous state
     * @param conductor to check
     */
    protected void checkConductorState(Conductor conductor) throws Exception
    {
        if(conductor.getStatus() == Conductor.Status.ERROR)
        {
            throw conductor.getError();
        }
        if(conductor.getStatus() == Conductor.Status.FAIL)
        {
            throw new Exception(getName()+" cannot find a motif");
        }
    }

    protected DataCollection<FrequencyMatrix> getMatrixLibrary() throws Exception
    {
        DataElementPath library = getParameters().getOutputLibrary();
        return WeightMatrixCollection.createMatrixLibrary(library, log);
    }

    /**
     * Launches ChIPMunk thread and waits till it finishes
     * @param conductor
     * @param t
     * @throws InterruptedException
     */
    protected void runThread(Conductor conductor, Thread t) throws InterruptedException
    {
        t.start();
        while(t.isAlive())
        {
            jobControl.setPreparedness(conductor.getDone());
            if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST)
                conductor.setStatus(Conductor.Status.INTERRUPTED);
            Thread.sleep(1000);
        }
    }
}
