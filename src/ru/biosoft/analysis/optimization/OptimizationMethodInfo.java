package ru.biosoft.analysis.optimization;

import com.developmentontheedge.beans.Option;


public class OptimizationMethodInfo extends Option
{
    public static final String DEVIATION = "deviation";
    public static final String PENALTY = "penalty";
    public static final String EVALUATIONS = "evaluations";

    private String deviation = "";
    public String getDeviation()
    {
        return this.deviation;
    }
    public void setDeviation(String deviation)
    {
        String oldValue = this.deviation;
        this.deviation = deviation;
        firePropertyChange(DEVIATION, oldValue, deviation);
    }

    private String penalty = "";
    public String getPenalty()
    {
        return this.penalty;
    }
    public void setPenalty(String penalty)
    {
        String oldValue = this.penalty;
        this.penalty = penalty;
        firePropertyChange(PENALTY, oldValue, penalty);
    }

    private String evaluations = "";
    public String getEvaluations()
    {
        return this.evaluations;
    }
    public void setEvaluations(String evaluations)
    {
        String oldValue = this.evaluations;
        this.evaluations = evaluations;
        firePropertyChange(EVALUATIONS, oldValue, evaluations);
    }
}
