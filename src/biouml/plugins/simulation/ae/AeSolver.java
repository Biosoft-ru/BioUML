package biouml.plugins.simulation.ae;

public interface AeSolver
{
    public double[] solve(double[] initialGuess, AeModel model) throws Exception;
    
    public boolean isSuccess();
    
    public String getMessage();
}
