package biouml.plugins.simulation.ae;

public class NewtonSolverWrapper implements AeSolver
{
    private boolean isSuccess = false;

    public NewtonSolverWrapper()
    {
    }

    @Override
    public double[] solve(double[] initialGuess, AeModel model) throws Exception
    {
        isSuccess = true;
        try
        {
            NewtonSolver.solve( initialGuess, model );
        }
        catch( Exception e )
        {
            isSuccess = false;
            throw e;
        }
        return initialGuess;
    }

    @Override
    public boolean isSuccess()
    {
        return isSuccess;
    }

    @Override
    public String getMessage()
    {
        return isSuccess ? "" : "Newton solver can't find solution.";
    }
}
