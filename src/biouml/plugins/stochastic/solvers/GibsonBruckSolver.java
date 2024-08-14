package biouml.plugins.stochastic.solvers;

import com.developmentontheedge.beans.BeanInfoEx;

import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.Options;
import biouml.plugins.simulation.SimulatorInfo;
import ru.biosoft.jobcontrol.FunctionJobControl;
import biouml.plugins.simulation.Span;
import biouml.standard.simulation.ResultListener;

public class GibsonBruckSolver extends GillespieSolver
{
    protected double[] tauBeforeNull;
    protected double[] propensityBeforeNull;
    protected double[] tauM;
    protected int m;
    boolean firstRun = true;

    @Override
    public void init(Model model, double[] x0, Span tspan, ResultListener[] listeners, FunctionJobControl jobControl) throws Exception
    {
        super.init(model, x0, tspan, listeners, jobControl);
        propensities = this.model.getPropensities(x0);
        m = propensities.length;
        tauM = new double[m];
        tauBeforeNull = new double[m];
        propensityBeforeNull = new double[m];
        firstRun = true;
    }

    @Override
    protected void nextReaction() throws Exception
    {
        if( firstRun )
        {
            timeBeforeNextReaction = Double.POSITIVE_INFINITY;
            nextReactionIndex = -1;
            for( int j = 0; j < m; j++ )
            {
                tauM[j] = calculateTau(propensities[j]); //find reactions times
                if( tauM[j] < timeBeforeNextReaction )
                {
                    timeBeforeNextReaction = tauM[j];
                    nextReactionIndex = j;
                }
            }
            firstRun = false;
            return;
        }

        double[] oldPropensities = propensities.clone();
        model.updatePropensities(propensities, nextReactionIndex, x);

        int currentReactionIndex = nextReactionIndex;
        double timeBeforeCurrentReaction = timeBeforeNextReaction;

        timeBeforeNextReaction = Double.POSITIVE_INFINITY;
        nextReactionIndex = -1;

        for( int j = 0; j < m; j++ )
        {
            if( propensities[j] <= 0 )
            {
                if( oldPropensities[j] > 0 )
                {
                    tauBeforeNull[j] = tauM[j] - timeBeforeCurrentReaction;
                    propensityBeforeNull[j] = ( j == currentReactionIndex ) ? 0 : oldPropensities[j];

                }
                tauM[j] = Double.POSITIVE_INFINITY;
            }
            else if( oldPropensities[j] == 0 && propensities[j] > 0 )
            {
                tauM[j] = ( propensityBeforeNull[j] == 0 ) ? calculateTau(propensities[j]) : ( propensityBeforeNull[j] / propensities[j] )
                        * tauBeforeNull[j];
            }
            else
            {
                //if reaction was just fired - generating new random value
                //else recalculate time with random value from the previous step
                tauM[j] = ( j == currentReactionIndex ) ? calculateTau(propensities[j])
                        : ( oldPropensities[j] / propensities[j] ) * ( tauM[j] - timeBeforeCurrentReaction );
            }

            if( tauM[j] < timeBeforeNextReaction )
            {
                timeBeforeNextReaction = tauM[j];
                nextReactionIndex = j;
            }
        }
    }

    @Override
    protected double calculateTau(double propensity)
    {
        return propensity > 0 ? stochastic.getExponential(propensity) : Double.POSITIVE_INFINITY;
    }

    @Override
    public SimulatorInfo getInfo()
    {
        SimulatorInfo info = new SimulatorInfo();
        info.name = "Gibson and Bruck";
        info.eventsSupport = true;
        info.delaySupport = false;
        info.boundaryConditionSupport = false;
        return info;
    }

    @Override
    public Object getDefaultOptions()
    {
        return new GibsonBruckOptions();
    }

    @Override
    public void setOptions(Options options)
    {
        if( ! ( options instanceof GibsonBruckOptions ) )
            throw new IllegalArgumentException("Incorrect options class " + options.getClass()
                    + " Only GibsonBruckOptions are compatible with GibsonBruck solver");
        this.options = options;
    }

    //Options class
    public static class GibsonBruckOptions extends Options
    {
    }

    public static class GibsonBruckOptionsBeanInfo extends BeanInfoEx
    {
        public GibsonBruckOptionsBeanInfo()
        {
            super(GibsonBruckOptions.class);
        }
    }
}
