package ru.biosoft.analysis.optimization;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

@PropertyName("Method parameters")
@PropertyDescription("Method parameters.")
public class OptimizationMethodParameters extends AbstractAnalysisParameters
{
    private DataElementPath diagramPath;
    private DataElementPath resultPath;
    private boolean applyState = false;
    private long randomSeed;
    private boolean randomSeedHidden = true;
    private DataElementPath startingParameters;
    private boolean useStartingParameters; 

    /**
     * The random values generator used in the stochastic optimization methods.
     */
    protected Random random;
    
    @PropertyName("Diagram")
    @PropertyDescription("Path to diagram.")
    public DataElementPath getDiagramPath()
    {
        return diagramPath;
    }
    
    /**
     * for internal usage only!
     * @param diagramPath
     */
    public void setDiagramPath(DataElementPath diagramPath)
    {
        DataElementPath oldValue = this.diagramPath;
        this.diagramPath = diagramPath;
        firePropertyChange("diagramPath", oldValue, diagramPath);
    }

    @PropertyName("Optimization result")
    @PropertyDescription("Path to optimization result folder.")
    public DataElementPath getResultPath()
    {
        return resultPath;
    }
    public void setResultPath(DataElementPath resultPath)
    {
        Object oldValue = this.resultPath;
        this.resultPath = resultPath;
        firePropertyChange("resultPath", oldValue, resultPath);
    }

    @PropertyName("Random seed")
    @PropertyDescription("The random seed used in the stochastic optimization methods.")
    public long getRandomSeed()
    {
        return randomSeed;
    }
    public void setRandomSeed(long seed)
    {
        Object oldValue = this.randomSeed;
        this.randomSeed = seed;
        firePropertyChange("randomSeed", oldValue, seed);
    }

    public Random initRandom()
    {
        if(random == null || randomSeedHidden)
            random = new Random();
        if(!randomSeedHidden)
            random.setSeed(randomSeed);
        return random;
    }

    @PropertyName("Arbitrary random seed")
    @PropertyDescription("Is a random seed arbitrary or you want to specify it?")
    public boolean isRandomSeedHidden()
    {
        return randomSeedHidden;
    }
    public void setRandomSeedHidden(boolean randomSeedHidden)
    {
        Object oldValue = this.randomSeedHidden;
        this.randomSeedHidden = randomSeedHidden;
        firePropertyChange("randomSeedHidden", oldValue, randomSeedHidden);
    }

    public boolean isNotStochastic()
    {
        return random == null;
    }

    @PropertyName("Apply state")
    @PropertyDescription("Apply state after estimation.")
    public boolean isApplyState()
    {
        return applyState;
    }

    public void setApplyState(boolean applyState) 
    {
        Object oldValue = this.applyState;
        this.applyState = applyState;
        firePropertyChange("applyState", oldValue, applyState);
    }
    
    @Override
    public void read(Properties properties, String prefix)
    {
        diagramPath = DataElementPath.create(properties.getProperty(prefix + "diagramPath"));
    }

    @Override
    public void write(Properties properties, String prefix)
    {
        properties.put(prefix + "diagramPath", diagramPath.toString());
    }

    @PropertyName("Starting parameter values")
    public DataElementPath getStartingParameters()
    {
        return startingParameters;
    }
    public void setStartingParameters(DataElementPath startingParameters)
    {
        this.startingParameters = startingParameters;
    }
    
    @PropertyName("External starting parameters")
    public boolean isUseStartingParameters()
    {
        return useStartingParameters;
    }
    public void setUseStartingParameters(boolean useStartingParameters)
    {
        Object oldValue = this.useStartingParameters;
        this.useStartingParameters = useStartingParameters;
        firePropertyChange("useStartingParameters", oldValue, useStartingParameters);
    }
    
    public boolean isStartingParametersHidden()
    {
        return !isUseStartingParameters();
    }

    public static class StateInfo
    {
        protected List<String> resultList;
        protected String statePath;

        public StateInfo(String statePath)
        {
            this.statePath = statePath;
            this.resultList = new ArrayList<>();
        }

        public String getPath()
        {
            return this.statePath;
        }

        public void addResult(String resultPath)
        {
            if( !resultList.contains(resultPath) )
                resultList.add(resultPath);
        }

        public String[] getResults()
        {
            return resultList.toArray(new String[resultList.size()]);
        }

        public void setResults(List<String> results)
        {
            this.resultList = results;
        }
    }   
}
