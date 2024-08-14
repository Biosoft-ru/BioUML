package biouml.plugins.agentmodeling;

import java.io.File;
import java.util.logging.Level;
import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.FileCollection;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.UniformSpan;
import biouml.standard.simulation.ResultListener;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.simulation.access.SimulationResultTransformer;

@Deprecated
public class SimulationResultAgent extends SimulationAgent implements ResultListener
{

    SimulationResult simulationResult;
    SimulationResultTransformer transformer = new SimulationResultTransformer();
    private int index = 0;

    protected double timeIncrement = 0.01;

    public SimulationResultAgent(String name) throws Exception
    {
        this(name, new UniformSpan(0, 100, 0.01));
    }

    public SimulationResultAgent(String name, Span span)// throws Exception
    {
        super(name, span);
        simulationResult = new SimulationResult(null, name);
        init();
    }

    @Override
    public double getPriority()
    {
        return OBSERVER_AGENT_PRIORITY;
    }

    @Override
    public void iterate()
    {
        try
        {
            index++;
            if( index >= span.getLength() )
            {
                isAlive = false;
            }
            else
            {
                currentTime = span.getTime(index);
            }
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            currentTime = Double.POSITIVE_INFINITY;
        }
    }

    @Override
    public void init()
    {
        index = 0;
        currentTime = initialTime;
        isAlive = true;
    }

    public double getInitialTime()
    {
        return initialTime;
    }
    public void setInitialTime(double t)
    {
        span = new UniformSpan(initialTime, getCompletionTime(), getTimeIncrement());
        initialTime = t;
        init();
    }

    public double getCompletionTime()
    {
        return completionTime;
    }
    public void setCompletionTime(double t)
    {
        span = new UniformSpan(getInitialTime(), completionTime, getTimeIncrement());
        completionTime = t;
        init();
    }


    public double getTimeIncrement()
    {
        return this.timeIncrement;
    }
    public void setTimeIncrement(double t)
    {
        span = new UniformSpan(getInitialTime(), getCompletionTime(), timeIncrement);
        timeIncrement = t;
        init();
    }

    private File outputFile = null;

    public void setFile(File file)
    {
        this.outputFile = file;

        try
        {
            outputFile.createNewFile();
            Properties props = new Properties();
            props.put(DataCollectionConfigConstants.NAME_PROPERTY, "testFileDC");
            props.put(DataCollectionConfigConstants.FILE_PATH_PROPERTY, outputFile.getAbsolutePath());
            props.put(FileCollection.FILE_FILTER, "");
            FileCollection fdc = new FileCollection(null, props);
            transformer.init(fdc, null);
        }
        catch( Exception ex )
        {
            outputFile = null;
        }
    }

    public File getFile()
    {
        return outputFile;
    }

    @Override
    public void start(Object model)
    {

    }

    @Override
    public void add(double t, double[] y) throws Exception
    {
        if( isAlive )
            simulationResult.add(t, y);
    }

    @Override
    public void die()
    {
        if( outputFile != null )
        {
            try
            {
                outputFile.mkdirs();
                transformer.transformOutput(simulationResult);
            }
            catch( Exception ex )
            {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }

        super.die();
    }

    @Override
    public double[] getCurrentValues()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getVariableNames()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double[] getUpdatedValues() throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void setUpdated()
    {
        // TODO Auto-generated method stub
        
    }
}
