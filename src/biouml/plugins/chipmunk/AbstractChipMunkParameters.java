package biouml.plugins.chipmunk;

import ru.autosome.assist.AShapeProvider;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
public abstract class AbstractChipMunkParameters extends AbstractAnalysisParameters
{
    static final String[] SHAPES = {"flat", "single", "double"};
    static final String[] HORDE_MODES = {"Filter", "Mask"};
    
    private DataElementPath inputSequences;
    private int threadCount = SecurityManager.getMaximumThreadsNumber();
    private int stepLimit = 10;
    private int tryLimit = 100;
    private int startLength = 16;
    private int stopLength = 10;
    private double zoopsFactor = 1.0;
    private DataElementPath outputLibrary;
    private String matrixName;
    private String shape = "flat";
    private boolean useProfiles = true;

    @PropertyName("Input sequences")
    @PropertyDescription("Collection containing input reads.")
    public DataElementPath getInputSequences()
    {
        return inputSequences;
    }

    public void setInputSequences(DataElementPath inputSequences)
    {
        Object oldValue = this.inputSequences;
        this.inputSequences = inputSequences;
        firePropertyChange("inputSequences", oldValue, inputSequences);
    }

    @PropertyName("Number of threads")
    @PropertyDescription("Number of concurrent threads when processing")
    public int getThreadCount()
    {
        return threadCount;
    }

    public void setThreadCount(int threadCount)
    {
        Object oldValue = this.threadCount;
        this.threadCount = threadCount;
        firePropertyChange("threadCount", oldValue, threadCount);
    }

    @PropertyName("Step limit")
    @PropertyDescription("Step limit")
    public int getStepLimit()
    {
        return stepLimit;
    }

    public void setStepLimit(int stepLimit)
    {
        Object oldValue = this.stepLimit;
        this.stepLimit = stepLimit;
        firePropertyChange("stepLimit", oldValue, stepLimit);
    }

    @PropertyName("Try limit")
    @PropertyDescription("Try limit")
    public int getTryLimit()
    {
        return tryLimit;
    }

    public void setTryLimit(int tryLimit)
    {
        Object oldValue = this.tryLimit;
        this.tryLimit = tryLimit;
        firePropertyChange("tryLimit", oldValue, tryLimit);
    }

    @PropertyName("Start length")
    @PropertyDescription("Start length of the matrix")
    public int getStartLength()
    {
        return startLength;
    }

    public void setStartLength(int startLength)
    {
        Object oldValue = this.startLength;
        this.startLength = startLength;
        firePropertyChange("startLength", oldValue, startLength);
    }

    @PropertyName("Stop length")
    @PropertyDescription("Stop length of the matrix")
    public int getStopLength()
    {
        return stopLength;
    }

    public void setStopLength(int stopLength)
    {
        Object oldValue = this.stopLength;
        this.stopLength = stopLength;
        firePropertyChange("stopLength", oldValue, stopLength);
    }

    @PropertyName("ZOOPS factor")
    @PropertyDescription("Zero-or-one-occurence-per-sequence factor")
    public double getZoopsFactor()
    {
        return zoopsFactor;
    }

    public void setZoopsFactor(double zoopsFactor)
    {
        Object oldValue = this.zoopsFactor;
        this.zoopsFactor = zoopsFactor;
        firePropertyChange("zoopsFactor", oldValue, zoopsFactor);
    }

    @PropertyName("Output matrix library")
    @PropertyDescription("Path to the matrix library to put matrix into (will be created if not specified)")
    public DataElementPath getOutputLibrary()
    {
        return outputLibrary;
    }

    public void setOutputLibrary(DataElementPath outputLibrary)
    {
        Object oldValue = this.outputLibrary;
        this.outputLibrary = outputLibrary;
        firePropertyChange("outputLibrary", oldValue, outputLibrary);
    }

    @PropertyName("Matrix name")
    @PropertyDescription("Name of created matrix")
    public String getMatrixName()
    {
        return matrixName;
    }

    public void setMatrixName(String matrixName)
    {
        Object oldValue = this.matrixName;
        this.matrixName = matrixName;
        firePropertyChange("matrixName", oldValue, matrixName);
    }

    public DataElementPath getMatrixPath()
    {
        return getOutputLibrary() == null || getMatrixName() == null?null:getOutputLibrary().getChildPath(getMatrixName());
    }

    public void setMatrixPath(DataElementPath path)
    {
        if(path == null) return;
        setOutputLibrary(path.getParentPath());
        setMatrixName(path.getName());
    }

    @PropertyName("Use peak profiles")
    @PropertyDescription("Whether to use peak profiles (if available)")
    public boolean isUseProfiles()
    {
        return useProfiles;
    }

    public void setUseProfiles(boolean useProfiles)
    {
        Object oldValue = this.useProfiles;
        this.useProfiles = useProfiles;
        firePropertyChange("useProfiles", oldValue, useProfiles);
    }

    @Override
    public @Nonnull String[] getOutputNames()
    {
        return new String[] {"matrixPath"};
    }

    @PropertyName("Motif shape")
    @PropertyDescription("Motif shape")
    public String getShape()
    {
        return shape;
    }

    public void setShape(String shape)
    {
        Object oldValue = this.shape;
        this.shape = shape;
        firePropertyChange("shape", oldValue, shape);
    }
    
    public AShapeProvider getShapeProvider()
    {
        if( shape == null )
            return null;
        if( shape.equals("single") )
            return AShapeProvider.SingleBox;
        if( shape.equals("double") )
            return AShapeProvider.DoubleBox;
        return null;
    }
}
