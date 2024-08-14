package biouml.plugins.stochastic.solvers;

import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.Options;
import biouml.plugins.simulation.SimulatorInfo;
import ru.biosoft.jobcontrol.FunctionJobControl;
import biouml.plugins.simulation.Span;
import biouml.plugins.stochastic.StochasticSimulator;
import biouml.plugins.stochastic.Util;
import biouml.standard.simulation.ResultListener;

import java.util.Set;

import ru.biosoft.util.bean.BeanInfoEx2;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class MaxTSSolver extends StochasticSimulator
{

    protected double maxStep;
    private double xBound;
    private double propencityFractionBound;

    public double[] tBeforeXBecomesNull = null;
    public double[] aBeforeXBecomesNull = null;

    protected boolean[] isFastReaction; //fast-true, slow-false
    protected int numberOfSlowReactions;
    //    Set<Integer> slowReactions;
    double[] tauM;
    double[] propensities;

    int[] reactionFirings;

    protected int nextSlowReactionIndex;
    protected double timeBeforeSlowReaction;
    public double tau;

    @Override
    public int routine(double timeLimit) throws Exception
    {
        eventAtSpanPoint = false;
        if( time == Double.POSITIVE_INFINITY || Double.isNaN(time) )
        {
            xOld = x.clone();
        }
        while( time < timeLimit && running )
        {
            //fire slow reactions
            double[] eventsOnStart = ( modelHasEvents ) ? model.checkEvent(time, x) : null;
            nextReaction(timeLimit);
            if( tau == Double.POSITIVE_INFINITY )
            {
                time = Double.POSITIVE_INFINITY;
                profile.setX(x);
                profile.setTime(timeLimit);
                return SUCCESS;
            }

            //fire fast reactions
            boolean flag = false;
            xOld = x.clone();
            while( !flag && running )
            {
                for( int i = 0; i < isFastReaction.length; i++ )
                    if( isFastReaction[i] )
                        model.doReaction(i, x, stochastic.getPoisson(propensities[i] * tau));

                flag = true;
                for( double val : x )
                {
                    if( val < 0 ) //rollback
                    {
                        x = xOld.clone();
                        tau *= 0.5;
                        flag = false;
                        break;
                    }
                }
            }


            int eventFlag = checkEvent(eventsOnStart, time, time + tau, timeLimit);

            if( eventFlag == EVENT_OCCURED || eventFlag == SUCCESS )
            {
                return eventFlag; //return
            }
            time += tau;
        }
        profile.setX(x);
        profile.setTime(timeLimit);
        return SUCCESS;
    }

    protected boolean firstRun;

    protected void nextReaction(double timeLimit) throws Exception
    {
        if( firstRun )
        {
            timeBeforeSlowReaction = Double.POSITIVE_INFINITY;
            nextSlowReactionIndex = -1;
            for( int j = 0; j < propensities.length; j++ )
            {
                tauM[j] = calculateTau(propensities[j]); //find reactions times
                if( tauM[j] < timeBeforeSlowReaction )
                {
                    timeBeforeSlowReaction = tauM[j];
                    nextSlowReactionIndex = j;
                }
            }
            firstRun = false;
            return;
        }

        double[] oldPropensities = new double[propensities.length];
        boolean[] oldFR = new boolean[propensities.length];
        oldPropensities = propensities.clone();
        System.arraycopy(isFastReaction, 0, oldFR, 0, propensities.length);
        propensities = model.getPropensities(x);
        fastSlowReactionsPartition(propensities, x);

        //        if( slowReactions.size()==0) return;
        if( numberOfSlowReactions != 0 )
        {
            for( int i = 0; i < propensities.length; i++ )
            {
                double propensity = propensities[i];

                if( oldFR[i] && !isFastReaction[i] )
                    tauM[i] = calculateTau(propensity);

                else if( !oldFR[i] && !isFastReaction[i] )
                {
                    double tauMOld = tauM[nextSlowReactionIndex];
                    if( propensity == 0 && oldPropensities[i] > 0 )
                    {
                        tBeforeXBecomesNull[i] = tauM[i] - tauMOld;
                        aBeforeXBecomesNull[i] = i == nextSlowReactionIndex ? 0 : oldPropensities[i];

                        tauM[i] = Double.POSITIVE_INFINITY;
                    }
                    else
                    {
                        if( oldPropensities[i] == 0 && propensity > 0 )
                        {
                            if( aBeforeXBecomesNull[i] == 0 )
                                tauM[i] = calculateTau(propensity);
                            else
                                tauM[i] = ( aBeforeXBecomesNull[i] / propensity ) * ( tBeforeXBecomesNull[i] );
                        }
                        else
                        {
                            if( i != nextSlowReactionIndex )
                            {
                                tauM[i] = ( oldPropensities[i] / propensity ) * ( tauM[i] - tauMOld );
                            }
                        }
                    }
                }
            }
            //        if (nextSlowReactionIndex==-1) return;
            if( nextSlowReactionIndex != -1 )
            {
                if( !isFastReaction[nextSlowReactionIndex] )
                    tauM[nextSlowReactionIndex] = calculateTau(propensities[nextSlowReactionIndex]);
            }
            nextSlowReactionIndex = Util.indexOfMin(tauM, isFastReaction);
            timeBeforeSlowReaction = tauM[nextSlowReactionIndex];

        }

        else
        {
            nextSlowReactionIndex = -1;
            timeBeforeSlowReaction = Double.POSITIVE_INFINITY;
        }

        tau = ( numberOfSlowReactions == propensities.length ) ? timeBeforeSlowReaction : Math.min(maxStep, timeBeforeSlowReaction);

        if( tau == Double.POSITIVE_INFINITY )
            return;
        else if( tau > timeLimit - time )
        {
            tau = timeLimit - time;
        }
        else if( timeBeforeSlowReaction <= tau )
        {
            model.doReaction(nextSlowReactionIndex, x, 1);
        }

    }

    protected double calculateTau(double propensity)
    {
        return propensity > 0 ? stochastic.getExponential(propensity) : Double.POSITIVE_INFINITY;
    }

    protected boolean reactionIsFast(int i)
    {
        Set<Integer> vSet = model.getIndexesOfSubstrate(i);
        if( x[Util.indexOfMin(x, vSet)] > xBound && propensities[i] / Util.sum(propensities) > propencityFractionBound / x.length )
            return true;
        return false;
    }

    protected void fastSlowReactionsPartition(double[] propensities, double[] x)
    {
        isFastReaction = new boolean[propensities.length];
        numberOfSlowReactions = 0;
        for( int i = 0; i < propensities.length; i++ )
        {
            if( reactionIsFast(i) )
                isFastReaction[i] = true;
            else
            {
                isFastReaction[i] = false;
                numberOfSlowReactions++;
            }
        }
    }


    @Override
    public void init(Model model, double[] x0, Span tspan, ResultListener[] listeners, FunctionJobControl jobControl) throws Exception
    {
        super.init(model, x0, tspan, listeners, jobControl);
        this.maxStep = ( (MaxTSOptions)options ).maximalTimeStep;
        this.propencityFractionBound = ( (MaxTSOptions)options ).propencityFractionBoundForPartition;
        this.xBound = ( (MaxTSOptions)options ).xBoundForPartition;


        propensities = this.model.getPropensities(x);
        fastSlowReactionsPartition(propensities, x);

        tauM = new double[propensities.length];
        for( int j = 0; j < propensities.length; j++ )
        {
            tauM[j] = Util.calculateTau(propensities[j]);//-Math.log(Math.random()) / var[j] ;
        }
        tBeforeXBecomesNull = new double[propensities.length];
        aBeforeXBecomesNull = new double[propensities.length];
        nextSlowReactionIndex = Util.indexOfMin(tauM, isFastReaction);

    }

    @Override
    public SimulatorInfo getInfo()
    {
        SimulatorInfo info = new SimulatorInfo();
        info.name = "Max Time Step";
        info.eventsSupport = true;
        info.delaySupport = false;
        info.boundaryConditionSupport = false;
        return info;
    }

    @Override
    public Object getDefaultOptions()
    {
        return new MaxTSOptions();
    }

    @Override
    public void setOptions(Options options)
    {
        if( ! ( options instanceof MaxTSOptions ) )
            throw new IllegalArgumentException("Incorrect options class " + options.getClass()
                    + " Only MaxTSOptions are compatible with MaxTSSimulator");
        this.options = options;
    }

    //Options class
    public static class MaxTSOptions extends Options
    {
        double maximalTimeStep = 0.001;
        double propencityFractionBoundForPartition = 0.0001;
        double xBoundForPartition = 100;
 
        @PropertyName( "Maximal time step")
        @PropertyDescription("Maximal time step.")
        public double getMaximalTimeStep()
        {
            return maximalTimeStep;
        }

        public void setMaximalTimeStep(double k)
        {
            double oldValue = this.maximalTimeStep;
            this.maximalTimeStep = k;
            firePropertyChange( "maximalTimeStep", oldValue, k );
        }

        @PropertyName( "Propensity threshold")
        @PropertyDescription("Propensity  threshold slow reactions.")
        public double getPropencityFractionBoundForPartition()
        {
            return propencityFractionBoundForPartition;
        }

        public void setPropencityFractionBoundForPartition(double r)
        {
            double oldValue = this.propencityFractionBoundForPartition;
            this.propencityFractionBoundForPartition = r;
            firePropertyChange( "propencityFractionBoundForPartition", oldValue, r );
        }

        @PropertyName( "Substrate threshold")
        @PropertyDescription("Substrate threshold for slow reactions.")
        public double getXBoundForPartition()
        {
            return xBoundForPartition;
        }

        public void setXBoundForPartition(double n)
        {
            double oldValue = this.xBoundForPartition;
            this.xBoundForPartition = n;
            firePropertyChange( "xBoundForPartition", oldValue, n );
        }
    }

    public static class MaxTSOptionsBeanInfo extends BeanInfoEx2<MaxTSOptions>
    {
        public MaxTSOptionsBeanInfo()
        {
            super(MaxTSOptions.class);
        }

        @Override
        public void initProperties() throws Exception
        {
            add("maximalTimeStep");
            add("xBoundForPartition");
            add("propencityFractionBoundForPartition");
        }
    }
}