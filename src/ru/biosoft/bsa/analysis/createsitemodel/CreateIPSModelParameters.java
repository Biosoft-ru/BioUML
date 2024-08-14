package ru.biosoft.bsa.analysis.createsitemodel;

import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.bsa.analysis.IPSSiteModel;

public class CreateIPSModelParameters extends CreateSiteModelParameters
{
    private DataElementPathSet frequencyMatrices;
    private double critIPS = IPSSiteModel.DEFAULT_CRIT_IPS;
    private int windowSize = IPSSiteModel.DEFAULT_WINDOW;
    private int distMin = IPSSiteModel.DEFAULT_DIST_MIN;

    public DataElementPathSet getFrequencyMatrices()
    {
        return frequencyMatrices;
    }

    public void setFrequencyMatrices(DataElementPathSet frequencyMatrices)
    {
        Object oldValue = this.frequencyMatrices;
        this.frequencyMatrices = frequencyMatrices;
        firePropertyChange("frequencyMatrices", oldValue, frequencyMatrices);
    }

    public double getCritIPS()
    {
        return critIPS;
    }

    public void setCritIPS(double critIPS)
    {
        Object oldValue = this.critIPS;
        this.critIPS = critIPS;
        firePropertyChange("critIPS", oldValue, critIPS);
    }

    public int getWindowSize()
    {
        return windowSize;
    }

    public void setWindowSize(int windowSize)
    {
        Object oldValue = this.windowSize;
        this.windowSize = windowSize;
        firePropertyChange("windowSize", oldValue, windowSize);
    }

    public int getDistMin()
    {
        return distMin;
    }

    public void setDistMin(int distMin)
    {
        Object oldValue = this.distMin;
        this.distMin = distMin;
        firePropertyChange("distMin", oldValue, distMin);
    }

}
