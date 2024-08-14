package biouml.plugins.agentmodeling;

import java.lang.reflect.Field;
import java.util.logging.Level;

import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.Simulator;
import biouml.plugins.simulation.SimulatorSupport;
import biouml.plugins.simulation.Span;
import biouml.standard.simulation.ResultListener;

public class CellAgent extends ModelAgent
{
    Field divisionField;
    Field currentStateTimeField;

    double divisionProbability = 0.1;

    boolean canDivide = true;

    public CellAgent(Model model, Simulator simulator, Span span, String name, ResultListener ... listeners) throws Exception
    {
        super(model, simulator, span, name, listeners);

        try
        {
            divisionField = model.getClass().getDeclaredField("division");
            divisionField.setAccessible(true);

            currentStateTimeField = model.getClass().getDeclaredField("currentStateTime");
            currentStateTimeField.setAccessible(true);

            Field canNotDivideField = model.getClass().getDeclaredField("arrest");
            canNotDivideField.setAccessible(true);

            //            if( Math.random() < divisionProbability )
            //            {
            //                canDivide = false;
            canNotDivideField.setDouble(model, 0.0);
            //            }
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Can not init division flag in the agent " + name);
        }
    }

    @Override
    public boolean shouldDivide()
    {
        try
        {
            if( !canDivide )
                return false;

            double doubleVal = divisionField.getDouble(model);
            return ( doubleVal == 1 );
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return false;
    }

    @Override
    public CellAgent[] divide() throws Exception
    {
        divisionField.setDouble(model, 0.0);

        String newName = getName()+"_"+Math.floor(getScaledCurrentTime());
        CellAgent child = this.clone(newName);

        //        a little random in child's initial state
        double currentStateTime = currentStateTimeField.getDouble(child.model);
        currentStateTimeField.setDouble(child.model, currentStateTime - 2 * Math.random());

        return new CellAgent[] {child};
    }

    public CellAgent clone(String newName)
    {
        try
        {
            Model newModel = this.model.clone();

            Simulator newSimulator = this.simulator.getClass().newInstance();

            ( (SimulatorSupport)newSimulator ).setFireInitialValues(false);

            Span newSpan = span.getRestrictedSpan(currentTime, completionTime);

            CellAgent result = new CellAgent(newModel, newSimulator, newSpan, newName, this.listeners.toArray(new ResultListener[listeners
                    .size()]));

            //            result.setInitialValues(y);

            return result;

        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
        return null;
    }
}
