package ru.biosoft.analysis;

public class CRClusterAnalysisParameters extends MicroarrayAnalysisParameters
{
    private Integer chainCount = 20;
    private Integer cycleCount = 20;
    private Boolean invert = true;
    private Double cutoff = 0.9;

    public CRClusterAnalysisParameters()
    {
        getExperimentData().setNumerical(true);
    }
    public Integer getChainsCount()
    {
        return chainCount;
    }
    public void setChainsCount(Integer count)
    {
        Integer oldValue = this.chainCount;
        chainCount = count;
        firePropertyChange("chainCount", oldValue, chainCount);
    }

    public Integer getCycleCount()
    {
        return cycleCount;
    }
    public void setCycleCount(Integer count)
    {
        Integer oldValue = this.cycleCount;
        cycleCount = count;
        firePropertyChange("cycleCount", oldValue, cycleCount);
    }

    public Boolean isInvert()
    {
        return invert;
    }
    public void setInvert(Boolean invert)
    {
        Boolean oldValue = this.invert;
        this.invert = invert;
        firePropertyChange("invert", oldValue, invert);
    }

    public Double getCutoff()
    {
        return cutoff;
    }
    public void setCutoff(Double cutoff)
    {
        Double oldValue = this.cutoff;
        this.cutoff = cutoff;
        firePropertyChange("cutoff", oldValue, cutoff);
    }
}
