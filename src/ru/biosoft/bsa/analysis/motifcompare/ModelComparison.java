
package ru.biosoft.bsa.analysis.motifcompare;

public class ModelComparison
{
    double[][] thresholds;
    double[][] sensitivities;
    double[][] fprs;
    
    ModelComparison(double[][] thresholds, double[][] sensitivities, double[][] fprs)
    {
        super();
        this.thresholds = thresholds;
        this.sensitivities = sensitivities;
        this.fprs = fprs;
    }

    public int getModelCount()
    {
        return thresholds.length;
    }
    
    public double[] getThresholds(int modelIndex)
    {
        return thresholds[modelIndex];
    }
    
    public double[] getSensitivities(int modelIndex)
    {
        return sensitivities[modelIndex];
    }
    
    public double[] getFPRs(int modelIndex)
    {
        return fprs[modelIndex];
    }
}
