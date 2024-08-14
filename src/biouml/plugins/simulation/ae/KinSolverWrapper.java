package biouml.plugins.simulation.ae;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.simulation.Options;

@SuppressWarnings ( "serial" )
public class KinSolverWrapper extends Options implements AeSolver
{
    private static final String ETACHOICE1 = "choice 1";
    private static final String ETACHOICE2 = "choice 2";
    private static final String ETACONSTANT = "constant";
    private static final String LINE_SEARCH = "Line search";
    private static final String NEWTON = "Newton";

    public KinSolverWrapper()
    {
    }

    private int status = 0;
    private KinSolver solver = null;

    private double ftolerance = KinSolver.FTOL_DEFAULT;
    private double tolerance = KinSolver.STOL_DEFAULT;
    private int maxIter = KinSolver.MXITER_DEFAULT;
    private int maxSetups = KinSolver.MSBSET_DEFAULT;
    private String strategy = NEWTON;
    private String etaFlag = ETACHOICE1;

    @Override
    public double[] solve(double[] initialGuess, AeModel model) throws Exception
    {
        solver = new KinSolver(model, initialGuess);
        solver.setFtol(ftolerance);
        solver.setStol(tolerance);
        solver.setMaximumIterations(maxIter);
        solver.setMaxSetups(maxSetups);
        solver.setEtaFlag(getEtaFlagType());
        status = solver.start(initialGuess, getStrategyType());
        initialGuess = solver.getY();
        return initialGuess;
    }

    @Override
    public boolean isSuccess()
    {
        return status >= 0;
    }

    @Override
    public String getMessage()
    {
        if( solver == null )
            return "";
        return solver.flagToMessage.get(status);
    }

    private int getEtaFlagType()
    {
        switch( etaFlag )
        {
            case ETACONSTANT:
                return KinSolver.ETACONSTANT;
            case ETACHOICE2:
                return KinSolver.ETACHOICE2;
            case ETACHOICE1:
            default:
                return KinSolver.ETACHOICE1;
        }
    }

    private int getStrategyType()
    {
        return LINE_SEARCH.equals(strategy) ? KinSolver.LINESEARCH : KinSolver.NONE;
    }

    @PropertyName ( "Function tolerance" )
    @PropertyDescription ( "stopping tolerance on L2-norm of function value" )
    public double getFtolerance()
    {
        return ftolerance;
    }
    public void setFtolerance(double ftol)
    {
        Object oldValue = this.ftolerance;
        this.ftolerance = ftol;
        firePropertyChange("ftolerance", oldValue, ftol);
    }

    @PropertyName ( "Step tolerance" )
    @PropertyDescription ( "scaled step length tolerance" )
    public double getTolerance()
    {
        return tolerance;
    }
    public void setTolerance(double tolerance)
    {
        Object oldValue = this.tolerance;
        this.tolerance = tolerance;
        firePropertyChange("ftolerance", oldValue, tolerance);
    }

    @PropertyName ( "Max iterations" )
    @PropertyDescription ( "maximum number of nonlinear iterations" )
    public int getMaxIter()
    {
        return maxIter;
    }
    public void setMaxIter(int maxIter)
    {
        Object oldValue = this.maxIter;
        this.maxIter = maxIter;
        firePropertyChange("maxIter", oldValue, maxIter);
    }

    @PropertyName ( "Max setups" )
    @PropertyDescription ( "maximum number of nonlinear iterations that may be performed between "
            + "calls to the linear solver setup routine (setup)" )
    public int getMaxSetups()
    {
        return maxSetups;
    }
    public void setMaxSetups(int maxSetups)
    {
        Object oldValue = this.maxSetups;
        this.maxSetups = maxSetups;
        firePropertyChange("maxSetups", oldValue, maxSetups);
    }

    @PropertyName ( "Solving strategy" )
    public String getStrategy()
    {
        return strategy;
    }
    public void setStrategy(String strategy)
    {
        Object oldValue = this.strategy;
        this.strategy = strategy;
        firePropertyChange("strategy", oldValue, this.strategy);
    }

    @PropertyName ( "Eta flag" )
    public String getEtaFlag()
    {
        return etaFlag;
    }
    public void setEtaFlag(String etaFlag)
    {
        Object oldValue = this.etaFlag;
        this.etaFlag = etaFlag;
        firePropertyChange("etaFlag", oldValue, this.etaFlag);
    }

    private static final String[] availableStrategies = new String[] {NEWTON, LINE_SEARCH};
    public String[] getAvailableStrategies()
    {
        return availableStrategies.clone();
    }

    private static final String[] availableEtaFlags = new String[] {ETACHOICE1, ETACHOICE2, ETACONSTANT};
    public String[] getAvailableEtaFlags()
    {
        return availableEtaFlags.clone();
    }
}
