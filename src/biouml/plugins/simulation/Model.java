package biouml.plugins.simulation;

import java.util.Map;

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
 * @see SimualtionEngine
 */


public interface Model extends Cloneable
{
    public double[] getInitialValues() throws Exception;
    
    public void init() throws Exception;
    
    public void init(double[] initialValues, Map<String, Double> parameters) throws Exception;
    
    public boolean isInit();

    public double[] getCurrentValues() throws Exception;
    public void setCurrentValues(double[] values) throws Exception;
        
    /**Returns current values of all model variables and parameters without any additional calculations. Method must be overridden by subclasses */
    public double[] getCurrentState() throws Exception;
    
    public Model clone();
}
