package biouml.plugins.modelreduction;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.plugins.simulation.ae.AeConjugateGradientSolver;
import biouml.plugins.simulation.ae.AeLevenbergMarquardSolver;
import biouml.plugins.simulation.ae.AeNelderMeadSolver;
import biouml.plugins.simulation.ae.AeSolver;
import biouml.plugins.simulation.ae.KinSolverWrapper;
import biouml.plugins.simulation.ae.NewtonSolverWrapperEx;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;

@SuppressWarnings ( "serial" )
@PropertyName ( "Parameters" )
public class AlgebraicSteadyStateParameters extends AbstractAnalysisParameters
{
    public AlgebraicSteadyStateParameters()
    {
        setSolverName(LEVENBERG_MARQUARD);
    }

    public static final String KIN_SOLVER = "KinSolver";
    public static final String NEWTON_SOLVER = "NewtonSolver";
    public static final String CONJUGATE_GRADIENT = "ConjugateGradient";
    public static final String NELDER_MEAD = "NelderMead";
    public static final String LEVENBERG_MARQUARD = "LevenbergMarquard";
    private DataElementPath inputPath;

    public static final String OUTPUT_TABLE_TYPE = "Table";
    public static final String OUTPUT_SIMULATION_RESULT_TYPE = "Simulation result";
    private String outputType = OUTPUT_TABLE_TYPE;
    private DataElementPath outputTable;
    private DataElementPath outputSimulationResult;

    private boolean onlyConstantParameters = false;
    private String solverName;
    private AeSolver solver;

    private String[] events = new String[] {};

    @PropertyName ( "Input path" )
    @PropertyDescription ( "Path to input diagram" )
    public DataElementPath getInputPath()
    {
        return inputPath;
    }
    public void setInputPath(DataElementPath modelPath)
    {
        Object oldValue = this.inputPath;
        this.inputPath = modelPath;
        firePropertyChange("inputPath", oldValue, modelPath);
    }

    @PropertyName ( "Output type" )
    @PropertyDescription ( "Type of output" )
    public String getOutputType()
    {
        return outputType;
    }
    public void setOutputType(String outputType)
    {
        Object oldValue = this.outputType;
        this.outputType = outputType;
        firePropertyChange("*", oldValue, outputType);
    }

    @PropertyName ( "Output table" )
    @PropertyDescription ( "Path to output table" )
    public DataElementPath getOutputTable()
    {
        return outputTable;
    }
    public void setOutputTable(DataElementPath outputTable)
    {
        Object oldValue = this.outputTable;
        this.outputTable = outputTable;
        firePropertyChange("outputTable", oldValue, outputTable);
    }
    public boolean isOutputTableHidden()
    {
        return !OUTPUT_TABLE_TYPE.equals(outputType);
    }

    @PropertyName ( "Output simualtion result" )
    @PropertyDescription ( "Path to output simulation result" )
    public DataElementPath getOutputSimulationResult()
    {
        return outputSimulationResult;
    }
    public void setOutputSimulationResult(DataElementPath outputSimulationResult)
    {
        Object oldValue = this.outputSimulationResult;
        this.outputSimulationResult = outputSimulationResult;
        firePropertyChange("outputSimulationResult", oldValue, outputSimulationResult);
    }
    public boolean isOutputSimulationResultHidden()
    {
        return !OUTPUT_SIMULATION_RESULT_TYPE.equals(outputType);
    }

    @PropertyName ( "Only constant parameters" )
    @PropertyDescription ( "Flag to show that all parameters which are not set by equations should be switched to constant" )
    public boolean isOnlyConstantParameters()
    {
        return onlyConstantParameters;
    }
    public void setOnlyConstantParameters(boolean onlyConstantParameters)
    {
        Object oldValue = this.onlyConstantParameters;
        this.onlyConstantParameters = onlyConstantParameters;
        firePropertyChange("onlyConstantParameters", oldValue, onlyConstantParameters);
    }

    @PropertyName ( "Solver name" )
    @PropertyDescription ( "Name of solver which will be used" )
    public String getSolverName()
    {
        return solverName;
    }
    public void setSolverName(String solverName)
    {
        Object oldValue = this.solverName;
        this.solverName = solverName;
        switch( solverName )
        {
            case LEVENBERG_MARQUARD:
                setSolver(new AeLevenbergMarquardSolver());
                break;
            case CONJUGATE_GRADIENT:
                setSolver(new AeConjugateGradientSolver());
                break;
            case NELDER_MEAD:
                setSolver(new AeNelderMeadSolver());
                break;
            case NEWTON_SOLVER:
                setSolver(new NewtonSolverWrapperEx());
                break;
            case KIN_SOLVER:
                setSolver(new KinSolverWrapper());
                break;
            default:
                break;
        }
        firePropertyChange("solverName", oldValue, solverName);
    }

    @PropertyName ( "Solver parameters" )
    @PropertyDescription ( "Parameters of solver which will be used" )
    public AeSolver getSolver()
    {
        return solver;
    }
    public void setSolver(AeSolver solver)
    {
        Object oldValue = this.solver;
        this.solver = solver;
        if( solver != null )
        {
            ComponentModel model = ComponentFactory.getModel(this);
            ComponentFactory.recreateChildProperties(model);
            if( solver instanceof Option )
                ( (Option)solver ).setParent(this);
        }
        firePropertyChange("solver", oldValue, solver);
    }

    static final String[] availableSolvers = new String[] {LEVENBERG_MARQUARD, CONJUGATE_GRADIENT, NELDER_MEAD, NEWTON_SOLVER, KIN_SOLVER};

    public String[] getEvents()
    {
        return events;
    }
    public void setEvents(String[] events)
    {
        Object oldValue = this.events;
        this.events = events;
        firePropertyChange("events", oldValue, events);
    }
}
