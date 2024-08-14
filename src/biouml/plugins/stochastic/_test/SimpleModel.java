
package biouml.plugins.stochastic._test;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import biouml.plugins.stochastic.StochasticModel;

public class SimpleModel extends StochasticModel
{
    @Override
    public void init(double[] initialValues, Map<String, Double> parameters)
    {
    }

    @Override
    public void doReaction(int i, double[] x, double k)
    {
        switch( i )
        {
            case 1:
                //reaction $$rate_RCT000006
                x[0] -= 1 * k;
                x[2] += 1 * k;
                break;
            case 0:
                //reaction $$rate_RCT000005
                x[0] += 1 * k;
                x[1] -= 1 * k;
                break;
        }
    }

    @Override
    public Set<Integer> getIndexesOfSubstrate(int i)
    {
        Set<Integer> result = new HashSet<>();
        switch( i )
        {
            case 1:
                //reaction $$rate_RCT000006
                result.add(0);
                break;
            case 0:
                //reaction $$rate_RCT000005
                result.add(1);
                break;
        }
        return result;
    }

    @Override
    public double[] getPropensities(double[] x)
    {
        final double[] propensities = new double[2];
        propensities[0] = 0.2 * x[1];
        propensities[1] = 0.2 * x[0];
        return propensities;
    }

    @Override
    public void updatePropensities(double[] propensities, int i, double[] x)
    {
        switch( i )
        {
            case 1:
                //reaction $$rate_RCT000006
                break;
            case 0:
                //reaction $$rate_RCT000005
                propensities[1] = 0.2 * x[0];
                break;
        }
    }

    @Override
    public double[] getInitialValues()
    {
        final double[] values = new double[3];
        values[0] = 1000.0; //  initial value of $SBS000001
        values[1] = 1000.0; //  initial value of $SBS000003
        values[2] = 1000.0; //  initial value of $SBS000002
        return values;
    }

    @Override
    public void init()
    {
        time = 0.0; // initial value of time
    }

    @Override
    public double[] extendResult(double time, double[] values)
    {
        this.time = time;
        double[] yv0 = new double[6];
        yv0[0] = values[0];
        yv0[1] = values[1];
        yv0[3] = values[2];
        yv0[5] = time;
        return yv0;
    }

    @Override
    public Map<Integer, Integer>[] getProductStochiometry()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<Integer, Integer>[] getReactantStochiometry()
    {
        // TODO Auto-generated method stub
        return null;
    }

}
