package biouml.plugins.agentmodeling;

import java.util.HashMap;

import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.Simulator;
import biouml.plugins.simulation.SimulatorSupport;
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
    protected int divideIndex = -1;

    public MortalAgent(Model model, int deathIndex, Simulator simulator, Span span, String name, ResultListener ... listeners)
            throws Exception
    {
        super( model, simulator, span, name, listeners );
        this.deathIndex = deathIndex;
    }

    public void setDivideIndex(int index)
    {
        divideIndex = index;
    }

    public MortalAgent(SimulationEngine engine, String name) throws Exception
    {
        super( engine, name );
        deathIndex = engine.getVarIndexMapping().get( "Death" );
    }

    public MortalAgent(Model model, Simulator simulator, Span span, String name, ResultListener ... listeners) throws Exception
    {
        super( model, simulator, span, name, listeners );
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
            System.out.println( "Error during check if agent " + getName() + "is alive: " + e.getMessage() );
        }
    }

    @Override
    public boolean shouldDivide()
    {
        if( divideIndex == -1 )
            return false;
        try
        {
            double doubleVal = model.getCurrentValues()[divideIndex];
            return ( doubleVal == 1 );
        }
        catch( Exception ex )
        {

        }
        return false;
    }

    @Override
    public MortalAgent[] divide() throws Exception
    {
        double[] values = model.getCurrentValues();
        values[divideIndex] = 0;
        model.setCurrentValues( values );

        String newName = getName() + "_" + Math.floor( getScaledCurrentTime() );
        MortalAgent child = this.clone( newName );


        return new MortalAgent[] {child};
    }

    public MortalAgent clone(String newName)
    {
        try
        {
            Model newModel = this.model.clone();

            Simulator newSimulator = this.simulator.getClass().newInstance();

            ( (SimulatorSupport)newSimulator ).setFireInitialValues( false );

            Span newSpan = span.getRestrictedSpan( currentTime, completionTime );

            MortalAgent result = new MortalAgent( newModel, newSimulator, newSpan, newName,
                    this.listeners.toArray( new ResultListener[listeners.size()] ) );
            result.setDivideIndex( divideIndex );
            result.variableToIndex = new HashMap<>(variableToIndex);
            return result;

        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
        return null;
    }
}
