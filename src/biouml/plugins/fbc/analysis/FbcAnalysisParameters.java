package biouml.plugins.fbc.analysis;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.fbc.ApacheModelCreator;
import biouml.plugins.fbc.FbcConstant;
import biouml.plugins.fbc.FbcModelCreator;
import biouml.plugins.fbc.GLPKModelCreator;
import biouml.plugins.fbc.GurobiModelCreator;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.exception.InternalException;

@SuppressWarnings ( "serial" )
public class FbcAnalysisParameters extends AbstractAnalysisParameters
{
    static final String[] AVAILABLE_SOLVER_TYPES = FbcConstant.getAvailableSolverNames();
    static final String[] OBJECTIVE_TYPES = new String[] {FbcConstant.MIN, FbcConstant.MAX};

    private DataElementPath diagramPath;
    private DataElementPath fbcDataTablePath;
    private DataElementPath fbcResultPath;
    private String typeObjectiveFunction = FbcConstant.MAX;
    private String solverType = FbcConstant.APACHE_SOLVER;

    @PropertyName ( "Diagram" )
    @PropertyDescription ( "Path to input diagram" )
    public DataElementPath getDiagramPath()
    {
        return diagramPath;
    }

    public void setDiagramPath(DataElementPath modelPath)
    {
        Object oldValue = this.diagramPath;
        this.diagramPath = modelPath;
        firePropertyChange( "diagramPath", oldValue, modelPath );
    }

    @PropertyName ( "Data table" )
    @PropertyDescription ( "Path to the table with initial FBC data (bounds, objective function coefficients)" )
    public DataElementPath getFbcDataTablePath()
    {
        return fbcDataTablePath;
    }

    public void setFbcDataTablePath(DataElementPath fbcDataTablePath)
    {
        Object oldValue = this.fbcDataTablePath;
        this.fbcDataTablePath = fbcDataTablePath;
        firePropertyChange( "fbcDataTablePath", oldValue, fbcDataTablePath );
    }


    @PropertyName ( "Output path" )
    @PropertyDescription ( "Path to table with fluxes values" )
    public DataElementPath getFbcResultPath()
    {
        return fbcResultPath;
    }

    public void setFbcResultPath(DataElementPath fbcResultPath)
    {
        Object oldValue = this.fbcResultPath;
        this.fbcResultPath = fbcResultPath;
        firePropertyChange( "fbcResultPath", oldValue, fbcResultPath );
    }

    public FbcModelCreator getCreator()
    {
        switch( solverType )
        {
            case FbcConstant.GUROBI_SOLVER:
                return new GurobiModelCreator();
            case FbcConstant.APACHE_SOLVER:
                return new ApacheModelCreator( maxIter );
            case FbcConstant.GLPK_SOLVER:
                try
                {
                    return new GLPKModelCreator();
                }
                catch( Exception e )
                {
                    throw new InternalException( e, "Can not load GLPK model creator" );
                }
        }
        throw new InternalException( "Wrong solver type" );
    }

    @PropertyName ( "Solver type" )
    @PropertyDescription ( "Type of the solver which will be used to find fluxes" )
    public String getSolverType()
    {
        return solverType;
    }

    public void setSolverType(String solverType)
    {
        Object oldValue = this.solverType;
        this.solverType = solverType;
        isGurobiSolver = FbcConstant.GUROBI_SOLVER.equals( solverType );
        firePropertyChange( "solverType", oldValue, solverType );
        firePropertyChange( "*", null, null );
    }

    @PropertyName ( "Optimization type" )
    @PropertyDescription ( "Type of objective function optimization which will be used (maximize or minimize)" )
    public String getTypeObjectiveFunction()
    {
        return typeObjectiveFunction;
    }

    public void setTypeObjectiveFunction(String typeObjectiveFunction)
    {
        Object oldValue = this.typeObjectiveFunction;
        this.typeObjectiveFunction = typeObjectiveFunction;
        firePropertyChange( "typeObjectiveFunction", oldValue, typeObjectiveFunction );
    }

    private int maxIter = 10000;
    @PropertyName ( "Max iter" )
    @PropertyDescription ( "Maximal iteration number" )
    public int getMaxIter()
    {
        return maxIter;
    }
    public void setMaxIter(int maxIter)
    {
        Object oldValue = this.maxIter;
        this.maxIter = maxIter;
        firePropertyChange( "maxIter", oldValue, maxIter );
    }

    private boolean isGurobiSolver = false;
    public boolean isGurobiSolver()
    {
        return isGurobiSolver;
    }
}
