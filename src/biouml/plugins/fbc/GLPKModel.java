package biouml.plugins.fbc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gnu.glpk.GLPK;
import org.gnu.glpk.GLPKConstants;
import org.gnu.glpk.GlpkException;
import org.gnu.glpk.glp_prob;
import org.gnu.glpk.glp_smcp;

import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;

public class GLPKModel implements FbcModel
{
    protected Logger log = Logger.getLogger( GLPKModel.class.getName() );
    public glp_prob problem;
    private String[] reactionNames;
    private final HashMap<String, Double> optimalValues;

    public GLPKModel(glp_prob lp, String[] reactionNames)
    {
        problem = lp;
        this.reactionNames = reactionNames;
        optimalValues = new HashMap<>();
    }

    public void setLogLevel(Level level)
    {
        log.setLevel( level );
    }

    @Override
    public void optimize()
    {
        glp_smcp methodParams = new glp_smcp();
        try
        {
            //initialize simplex method control parameters
            GLPK.glp_init_smcp( methodParams );

            int ret = GLPK.glp_simplex( problem, methodParams );
            logSolutionStatusInfo( GLPK.glp_get_status( problem ) );
            if( ret == 0 )
            {
                fillOptimalValues();
            }
            else
            {
                String errorMsg = getSimplexError( ret );
                log.severe( errorMsg );
                //logSolutionWithErrors();
                for( int i = 0; i < reactionNames.length; i++ )
                    optimalValues.put( reactionNames[i], Double.NaN );
            }
        }
        catch( GlpkException e )
        {
            log.severe( "GLPK error during solving the problem: " + e.getMessage() );
        }

    }

    /**
     * write problem with current values and errors to log file
     */
    private void logSolutionWithErrors()
    {
        try (TempFile resultFile = TempFiles.file( "glpk " + GLPK.glp_get_prob_name( problem ) ))
        {
            GLPK.glp_print_sol( problem, resultFile.getAbsolutePath() );
            StringBuffer buf = new StringBuffer( "GLPK problem status " + GLPK.glp_get_status( problem ) );
            BufferedReader in = new BufferedReader( new FileReader( resultFile ) );
            String line = in.readLine();
            while( line != null )
            {
                buf.append( line );
                line = in.readLine();
            }
            in.close();
            log.info( buf.toString() );
        }
        catch( Exception e )
        {
        }
    }

    private String getSimplexError(int errorCode)
    {
        String msg = "Unknown GLPK simplex method error";
        if( errorCode == GLPKConstants.GLP_EBADB )
            msg = "Unable to start the search, because the initial basis specified in the problem object is invalid—the number of basic (auxiliary and structural) variables is not the same as the number of rows in the problem object.";
        else if( errorCode == GLPKConstants.GLP_ESING )
            msg = "Unable to start the search, because the basis matrix corresponding to the initial basis is singular within the working precision.";
        else if( errorCode == GLPKConstants.GLP_ECOND )
            msg = "Unable to start the search, because the basis matrix corresponding to the initial basis is ill-conditioned, i.e. its condition number is too large.";
        else if( errorCode == GLPKConstants.GLP_EBOUND )
            msg = "Unable to start the search, because some double-bounded (auxiliary or structural) variables have incorrect bounds.";
        else if( errorCode == GLPKConstants.GLP_EFAIL )
            msg = "The search was prematurely terminated due to the solver failure.";
        else if( errorCode == GLPKConstants.GLP_EOBJLL )
            msg = "The search was prematurely terminated, because the objective function being maximized has reached its lower limit and continues decreasing (the dual simplex only).";
        else if( errorCode == GLPKConstants.GLP_EOBJUL )
            msg = "The search was prematurely terminated, because the objective function being minimized has reached its upper limit and continues increasing (the dual simplex only).";
        else if( errorCode == GLPKConstants.GLP_EITLIM )
            msg = "The search was prematurely terminated, because the simplex iteration limit has been exceeded.";
        else if( errorCode == GLPKConstants.GLP_ETMLIM )
            msg = "The search was prematurely terminated, because the time limit has been exceeded.";
        else if( errorCode == GLPKConstants.GLP_ENOPFS )
            msg = "The LP problem instance has no primal feasible solution (only if the LP presolver is used).";
        else if( errorCode == GLPKConstants.GLP_ENODFS )
            msg = "The LP problem instance has no dual feasible solution (only if the LP presolver is used).";
        return "GLPK: " + msg;
    }

    private void logSolutionStatusInfo(int status)
    {
        String msg = "GLPK: Optimal solution found";
        if( status == GLPKConstants.GLP_FEAS )
            msg = "GLPK: Solution is feasible";
        else if( status == GLPKConstants.GLP_INFEAS )
            msg = "GLPK: Solution is infeasible";
        else if( status == GLPKConstants.GLP_NOFEAS )
            msg = "GLPK: No feasible solution for given problem.";
        else if( status == GLPKConstants.GLP_UNDEF )
            msg = "GLPK: No feasible solution for given problem.";
        log.warning( msg );
    }

    /**
     * write simplex solution
     * testing method
     */
    private static void write_lp_solution(glp_prob lp)
    {
        int i;
        int n;
        String name;
        double val;
        name = GLPK.glp_get_obj_name( lp );
        val = GLPK.glp_get_obj_val( lp );
        System.out.print( name );
        System.out.print( " = " );
        System.out.println( val );
        n = GLPK.glp_get_num_cols( lp );
        for( i = 1; i <= n; i++ )
        {
            name = GLPK.glp_get_col_name( lp, i );
            val = GLPK.glp_get_col_prim( lp, i );
            System.out.print( name );
            System.out.print( " = " );
            System.out.println( val );
        }
    }

    private void fillOptimalValues()
    {
        int n = GLPK.glp_get_num_cols( problem );
        for( int i = 1; i <= n; i++ )
        {
            String name = GLPK.glp_get_col_name( problem, i );
            optimalValues.put( name, GLPK.glp_get_col_prim( problem, i ) );
        }
    }

    @Override
    public double getOptimValue(String reactionName)
    {
        return optimalValues.get( reactionName );
    }

    @Override
    public double getValueObjFunc()
    {
        return GLPK.glp_get_obj_val( problem );
    }

    @Override
    public String[] getReactionNames()
    {
        return reactionNames;
    }

    @Override
    public GLPKModel clone()
    {
        glp_prob newProblem = GLPK.glp_create_prob();
        GLPK.glp_copy_prob( newProblem, problem, 1 );
        return new GLPKModel( newProblem, reactionNames.clone() );
    }

    public void setLowerBound(double lb, String reactionName)
    {
        int index = -1;
        for( int i = 0; i < reactionNames.length; i++ )
        {
            if( reactionName.equals( reactionNames[i] ) )
                index = i + 1;
        }
        setLowerBound( lb, index );
    }

    public double getLowerBound(String reactionName)
    {
        int index = -1;
        for( int i = 0; i < reactionNames.length; i++ )
        {
            if( reactionName.equals( reactionNames[i] ) )
                index = i + 1;
        }
        return getLowerBound( index );
    }

    public double getLowerBound(int index)
    {
        return GLPK.glp_get_col_lb( problem, index );
    }

    public void setLowerBound(double lb, int index)
    {
        double ub = GLPK.glp_get_col_ub( problem, index );
        int type = GLPK.glp_get_col_type( problem, index );
        GLPK.glp_set_col_bnds( problem, index, type, lb, ub );
    }
}