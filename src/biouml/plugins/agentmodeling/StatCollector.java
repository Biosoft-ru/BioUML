package biouml.plugins.agentmodeling;

import biouml.plugins.simulation.Span;

public abstract class StatCollector
{
    private boolean showPlot;
    protected AgentBasedModel model;

    public void init(Span span, AgentBasedModel model) throws Exception
    {
        this.model = model;
    }

    public abstract void finish();

    public abstract void update(double time) throws Exception;
    
    public abstract void update(double time, SimulationAgent agent) throws Exception;

    public boolean isShowPlot()
    {
        return showPlot;
    }
    public void setShowPlot(boolean showPlot)
    {
        this.showPlot = showPlot;
    }
}