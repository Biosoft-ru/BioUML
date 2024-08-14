package biouml.plugins.stochastic.solvers;

import java.util.Map;

import one.util.streamex.IntStreamEx;

import com.developmentontheedge.beans.BeanInfoEx;

import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.Options;
import biouml.plugins.simulation.SimulatorInfo;

import ru.biosoft.jobcontrol.FunctionJobControl;

import biouml.plugins.simulation.Span;
import biouml.plugins.stochastic.StochasticModel;
import biouml.plugins.stochastic.Util;
import biouml.standard.simulation.ResultListener;

public class TauLeapingSolver extends GillespieEfficientSolver
{
    //parameters
    static final long CRITICAL_CONSTANT = 10;
    static final double ERROR_CONTROL_PARAMETER = 0.03;
    static final double CONDITION_FOR_GILLESPIE = 10;
    int gillespieRuns = 100;
    static final double CONSTANT_FOR_DIFFERENIATION = 1E-5;

    int[][] specieReactionChange;
    protected Map<Integer, Integer>[] reactantHistos;
    protected Map<Integer, Integer>[] productHistos;
    protected int criticalReactionNumber;

    protected boolean[] criticalReaction;

    private int m;

    //debug issues
    public int totalGillespieRuns;
    public int notGillespieRuns;


    @Override
    public boolean doStep()
    {
        try
        {
            if( fireInitialValues || eventAtSpanPoint )
                fireSolutionUpdate(span.getTimeStart(), x);
            
            if( terminated || jobControl != null && jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST )
                return false;
            
            fireInitialValues = false;
            eventAtSpanPoint = false;

            double timeLimit = span.getTime(nextSpanIndex);

            xOld = x.clone();

            while( time < timeLimit && running )
            {
                double[] propensities = model.getPropensities(x);

                initCriticalSets(propensities);
                double propensitySum = Util.sum(propensities);
                double tauNonCriticals = calculateTauNonCriticals(propensities, propensitySum);

                boolean flag = true;
                while( flag )
                {
                    if( tauNonCriticals < CONDITION_FOR_GILLESPIE / propensitySum )
                        return doGillespie();

                    notGillespieRuns++;
                    xOld = x.clone();
                    nextReaction(propensities, timeLimit, tauNonCriticals);

                    //firing critical reaction
                    if( nextReactionIndex != -1 )
                        model.doReaction(nextReactionIndex, x, 1);

                    //firing non critical reactions
                    for( int j = 0; j < m; j++ )
                    {
                        if( !criticalReaction[j] )
                        {
                            double k = 0;
                            double mean = propensities[j] * timeBeforeNextReaction;
                            if( mean == 0 )
                                continue;
                            try
                            {

                            if( mean > 1E8 )
                            {
                                k = stochastic.getNormal( mean, Math.sqrt( mean ) );
                                k = Math.rint( k );
                            }
                            else
                                k = stochastic.getPoisson( mean );
                            }
                            catch( Exception ex )
                            {
                                log.info( "Can't calculate possion random variable, mean is too large: " + mean );
                            }
                            model.doReaction( j, x, k );
                        }
                    }

                    flag = false;
                    for( double xElemet : x )
                    {
                        if( xElemet < 0 )
                        {
                            //rollback
                            flag = true;
                            tauNonCriticals /= 2;
                            x = xOld.clone();
                            break;
                        }
                    }
                }

                if( modelHasEvents )
                {
                    double[] eventsOnStart = model.checkEvent(time, xOld);

                    int eventFlag = checkEvent(eventsOnStart, time, time + timeBeforeNextReaction, timeLimit);

                    if( eventFlag == SUCCESS )
                    {
                        return nextSpanIndex < span.getLength();
                    }
                    else if( eventFlag == EVENT_OCCURED )
                    {
                        return false;
                    }
                }
                time += timeBeforeNextReaction;
            }

            profile.setX(x);
            profile.setTime(timeLimit);
            fireSolutionUpdate(timeLimit, x);
            nextSpanIndex++;
            return nextSpanIndex < span.getLength();

        }
        catch( Exception ex )
        {
            if( jobControl != null )
                jobControl.terminate();
            ex.printStackTrace();
            profile.setErrorMessage(ex.getLocalizedMessage());
            profile.setUnstable(true);
            log.info(ex.getMessage());
            return false;
        }
    }

    private boolean doGillespie() throws Exception
    {
        boolean done = true;

        for( int i = 0; i < gillespieRuns && done; i++ )
        {
            totalGillespieRuns++;
            done = super.doStep();
        }
        return done;
    }

    protected void nextReaction(double[] propensities, double timeLimit, double tauNonCriticals)
    {
        nextReactionIndex = -1;
        double propensityCriticalSum = 0;
        for( int i = 0; i < m; i++ )
        {
            if( criticalReaction[i] )
                propensityCriticalSum += propensities[i];
        }
        double tauCriticals = stochastic.getExponential(propensityCriticalSum);
        double restTime = timeLimit - time;
        if( ( restTime < tauNonCriticals ) && ( restTime < tauCriticals ) )
        {
            timeBeforeNextReaction = restTime;
        }
        else if( tauNonCriticals < tauCriticals )
        {
            timeBeforeNextReaction = tauNonCriticals;
        }
        else
        {
            timeBeforeNextReaction = tauCriticals;
            nextReactionIndex = identifyTheOnlyCriticalReaction(propensities, propensityCriticalSum);


            if( debug )
                System.out.println("Critical reaction " + nextReactionIndex);
        }
        //System.out.println("tau= " + tau + " Kmy = [" + kMy[0] + ",  " + kMy[1] + "]");
    }

    protected double calculateTauNonCriticals(double[] propensities, double propensitySum) throws Exception
    {
        double result = Double.POSITIVE_INFINITY;
        if( criticalReactionNumber == m )
            return result;

        double[][] propensitiesDir = getPropensityDir(propensities, x, model);

        for( int j = 0; j < m; j++ )
        {
            double mu = 0;
            double sigma = 0;
            for( int jdash = 0; jdash < m; jdash++ )
            {
                if( criticalReaction[jdash] )
                    continue;
                double sum = 0;
                for( int i = 0; i < n; i++ )
                    sum += propensitiesDir[i][j] * specieReactionChange[i][jdash];

                mu += sum * propensities[jdash];
                sigma += sum * sum * propensities[jdash];
            }

            double temp = ERROR_CONTROL_PARAMETER * propensitySum;
            result = Math.min(result, temp / Math.abs(mu));
            result = Math.min(result, temp * temp / sigma);
        }
        return result;
    }
    @Override
    public Object getDefaultOptions()
    {
        return new TauLeapingOptions();
    }

    @Override
    public SimulatorInfo getInfo()
    {
        SimulatorInfo info = new SimulatorInfo();
        info.name = "Tau-leaping";
        info.eventsSupport = true;
        info.delaySupport = false;
        info.boundaryConditionSupport = true;
        return info;
    }

    @Override
    public void setOptions(Options options)
    {
        if( ! ( options instanceof TauLeapingOptions ) )
            throw new IllegalArgumentException("Incorrect options class " + options.getClass()
                    + " Only TauLeapingOptions are compatible with TauLeaping solver");
        this.options = options;

    }

    @Override
    public void init(Model model, double[] x0, Span tspan, ResultListener[] listeners, FunctionJobControl jobControl) throws Exception
    {
        super.init(model, x0, tspan, listeners, jobControl);
        reactantHistos = ( (StochasticModel)model ).getReactantStochiometry();
        productHistos = ( (StochasticModel)model ).getProductStochiometry();
        double[] propensities = ( (StochasticModel)model ).getPropensities(x0);
        m = propensities.length;
        initSpecieReactionChange(propensities, (StochasticModel)model);
        totalGillespieRuns = 0;
        notGillespieRuns = 0;
    }

    private void initSpecieReactionChange(double[] propensities, StochasticModel model)
    {
        specieReactionChange = new int[n][m];

        IntStreamEx.range( m ).forEach( j -> {
            reactantHistos[j].forEach( (i, v) -> specieReactionChange[i][j] -= v );
            productHistos[j].forEach( (i, v) -> specieReactionChange[i][j] = v );
        } );
    }

    private void initCriticalSets(double[] propensities)
    {
        criticalReaction = new boolean[m];
        criticalReactionNumber = 0;

        for( int i = 0; i < m; i++ )
        {
            int firings = Integer.MAX_VALUE;

            Map<Integer, Integer> reactantHisto = reactantHistos[i];
            for( Map.Entry<Integer, Integer> entry : reactantHisto.entrySet() )
                firings = Math.min(firings, (int)Math.floor(x[entry.getKey()] / entry.getValue()));

            if( propensities[i] > 0 && firings < CRITICAL_CONSTANT )
            {
                criticalReaction[i] = true;
                criticalReactionNumber++;
            }
        }
    }

    private double[][] getPropensityDir(double[] propensities, double[] x, StochasticModel model) throws Exception
    {
        double[][] result = new double[n][m];
        for( int i = 0; i < n; i++ )
        {
            double[] xDisturbed = x.clone();
            xDisturbed[i] += CONSTANT_FOR_DIFFERENIATION;
            double[] propensitiesDisturb = model.getPropensities(xDisturbed);
            for( int j = 0; j < m; j++ )
                result[i][j] = ( propensitiesDisturb[j] - propensities[j] ) / CONSTANT_FOR_DIFFERENIATION;
        }
        return result;
    }

    private int identifyTheOnlyCriticalReaction(double[] propensities, double propensitySum)
    {
        double r2 = stochastic.getUniform();
        double test = r2 * propensitySum;
        double sum = 0;
        for( int i = 0; i < m; i++ )
        {
            if( !criticalReaction[i] )
                continue;
            sum += propensities[i];
            if( sum >= test )
                return i;
        }
        return -1;
    }

    //Options class
    public static class TauLeapingOptions extends Options
    {
    }

    public static class TauLeapingOptionsBeanInfo extends BeanInfoEx
    {
        public TauLeapingOptionsBeanInfo()
        {
            super(TauLeapingOptions.class);
        }
    }
}
