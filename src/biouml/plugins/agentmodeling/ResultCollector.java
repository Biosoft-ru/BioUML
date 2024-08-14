package biouml.plugins.agentmodeling;

import biouml.plugins.simulation.Span;
import biouml.standard.simulation.ResultListener;

/**
 * @author axec
 *
 */
@Deprecated
public class ResultCollector extends StatCollector
{
    private ResultListener[] listeners = null;
    private Span span;
    private int spanStep = 0;
    private AgentBasedModel model;

    public ResultCollector(ResultListener[] listeners, Span span, AgentBasedModel model) throws Exception
    {
        this.listeners = listeners;
        this.span = span;
        this.spanStep = 0;
        this.model = model;
    }
    
    @Override
    public void init(Span span, AgentBasedModel model) throws Exception
    {
        super.init(span, model);
        spanStep = 0;
    }

    @Override
    public void update(double time) throws Exception
    {
       
        if( listeners == null )
            return;
        double[] values = model.getCurrentValues();
        for( ResultListener listener : listeners )
            listener.add(time, values);
        
        spanStep++;
    }



    @Override
    public void finish()
    {
    }

    @Override
    public void update(double time, SimulationAgent agent) throws Exception
    {
        // TODO Auto-generated method stub
        
    }

}
