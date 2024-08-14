package biouml.plugins.stochastic.solvers;

import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulatorInfo;
import ru.biosoft.jobcontrol.FunctionJobControl;
import biouml.plugins.simulation.Span;
import biouml.plugins.stochastic.Util;
import biouml.standard.simulation.ResultListener;

public class GillespieEfficientSolver extends GillespieSolver
{

    @Override
    public void init(Model model, double[] x0, Span tspan, ResultListener[] listeners, FunctionJobControl jobControl) throws Exception
    {
        super.init(model, x0, tspan, listeners, jobControl);
        propensities = this.model.getPropensities(x0);
        propensitiesSum = Util.sum(propensities);
        nextReactionIndex = -1;
    }

    private double propensitiesSum;

    @Override
    protected void nextReaction() throws Exception
    {
        model.updatePropensities(propensities, nextReactionIndex, x);
        propensitiesSum = Util.sum(propensities);

        timeBeforeNextReaction = calculateTau(propensitiesSum);

        if( timeBeforeNextReaction == Double.POSITIVE_INFINITY )
        {
            nextReactionIndex = -1;
            return;
        }

        propensitiesSum *= stochastic.getUniform();

        double sum = 0;

        for( int j = 0; j < propensities.length; j++ )
        {
            sum += propensities[j];
            if( propensitiesSum <= sum )
            {
                nextReactionIndex = j;
                return;
            }
        }

        nextReactionIndex = -1;
    }

    @Override
    public SimulatorInfo getInfo()
    {
        SimulatorInfo info = new SimulatorInfo();
        info.name = "Gillespie efficient";
        info.eventsSupport = true;
        info.delaySupport = false;
        info.boundaryConditionSupport = false;
        return info;
    }
}
