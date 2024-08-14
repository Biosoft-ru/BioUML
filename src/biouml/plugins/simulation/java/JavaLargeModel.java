package biouml.plugins.simulation.java;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import biouml.plugins.simulation.SimulationEngine;
import one.util.streamex.EntryStream;

/**
 * WARNING: READ BEFORE EDITING THIS CLASS!
 * 
 * This class and some others constitutes separate jar which is used during model simulation in BioUML,
 * therefore all class on which it depends should also added to this jar
 * jar file used for simulation is specified by SimulationEngine
 * 
 * Before adding any new dependencies here - please think twice.
 * 
 * If you add dependency - add this class (and all classes from which it depends) to build_bdk.xml
 * (see biouml.plugins.simulation building)
 * @see SimulationEngine
 */
abstract public class JavaLargeModel extends JavaBaseModel
{
    protected ExecutorService executor;
    private Map<String, Integer> varNameMapping;
    protected double[] var;

    public JavaLargeModel()
    {

    }
    
    public void setNameToIndex(Map<String, Integer> mapping)
    {
        this.varNameMapping = mapping;
    }
    
    public Map<String, Integer> getNameToIndex()
    {
        return new HashMap<>( varNameMapping );
    }
    
    public Integer getIndex(String name)
    {
        return varNameMapping.get(name);
    }
    
    @Override
    public void init(double[] initialValues, Map<String, Double> parameters) throws Exception
    {
        this.initialValues = Arrays.copyOf(initialValues, initialValues.length);
        this.x_values = Arrays.copyOf(initialValues, initialValues.length);
        EntryStream.of(parameters).filter(e->!e.getKey().equals("time")).forEach(e -> var[getIndex(e.getKey())] = e.getValue());
        isInit = true;
    }

    public class Task implements Callable<Void>
    {

        @Override
        public Void call() throws Exception
        {
            return null;
        }

    }

    public void finalize()
    {
        if( executor != null )
            executor.shutdown();
    }
}
