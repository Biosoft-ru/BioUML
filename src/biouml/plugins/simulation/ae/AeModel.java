
package biouml.plugins.simulation.ae;

/*
   this is the ODE interface (or abstract superclass).  Any ODE that is defined in
   this package or that the user wants to define must implement (be a subclass
   of) this class
 */

/*
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


public interface AeModel
{
    public double[] solveAlgebraic(double[] z);
    
    /**
     * Constraints for algebraic variables (from aeModel)
     * are supported by KinSolver. If it's null then no constraints are defined;
     * Must be array with length equal to parameter number;
     * i'th value can be:
     * <li>1 mean that i'th parameter must be positive
     * <li>-1 means that i'th parameter must be negative
     * <li>0 means that there are no constraints for i'th parameter
     */
    public double[] getConstraints();
}
