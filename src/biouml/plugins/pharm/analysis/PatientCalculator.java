package biouml.plugins.pharm.analysis;

public interface PatientCalculator
{
    abstract public Patient calculate(double[] input) throws Exception;
}