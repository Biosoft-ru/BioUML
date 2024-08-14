package biouml.plugins.agentmodeling;

import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.Simulator;
import biouml.plugins.simulation.Span;
import biouml.standard.simulation.ResultListener;

/**
 * Model agent with special "Death" variable
 * @author Ilya
 *
 */
public class MortalAgent extends ModelAgent
{
    protected int deathIndex;
    
    public MortalAgent(Model model, int deathIndex, Simulator simulator, Span span, String name, ResultListener ... listeners) throws Exception
    {
        super(model, simulator, span, name, listeners);
        this.deathIndex = deathIndex;
    }
    
    public MortalAgent(SimulationEngine engine, String name) throws Exception
    {
        super(engine, name);
        deathIndex = engine.getVarIndexMapping().get("Death");
    }
    
    @Override
    public void iterate()
    {
//        double t = ((EventLoopSimulator)simulator).t;
        super.iterate();
        try
        {
            this.isAlive = isAlive && model.getCurrentValues()[deathIndex] == 0;
//            if (!isAlive)
//          System.out.println("Elasped time "+((EventLoopSimulator)simulator).t);  
//            if (!isAlive)
//                System.out.println(getName()+": dead, + time  ="+ this.getScaledCurrentTime());
        }
        catch( Exception e )
        {
            System.out.println("Error during check if agent " + getName() + "is alive: " + e.getMessage());
        }
    }
}
