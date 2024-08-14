package ru.biosoft.analysis;

public class PolynomialRegressionAnalysisParameters extends MicroarrayAnalysisParameters
{
    private Integer regressionPower = 2;

    public PolynomialRegressionAnalysisParameters()
    {
        getExperimentData().setNumerical(true);
    }

    public Integer getRegressionPower()
    {
        return regressionPower;
    }

    public void setRegressionPower(Integer regressionPower)
    {
        Integer oldValue = this.regressionPower;
        this.regressionPower = regressionPower;
        firePropertyChange("regressionPower", oldValue, regressionPower);
    }
}
