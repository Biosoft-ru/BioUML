package biouml.plugins.brain.sde;

import biouml.plugins.simulation.ode.StdMet;

/**
 * Heun method for SDE solving.
 */
public class HeunStochastic extends EulerStochastic
{
    @Override
    public void integrationStep(double[] xNew, double[] xOld, double tOld, double h) throws Exception
    {   
    	SdeModel sdeModel = (SdeModel) odeModel;

    	double[] xPredictor = new double[n];
    	super.integrationStep(xPredictor, xOld, tOld, h);

    	double[] a0 = sdeModel.dy_dt_deterministic(tOld, xOld);

    	sdeModel.setStochasticGenerationEnabled(false);
    	double[] a1 = sdeModel.dy_dt_deterministic(tOld + h, xPredictor);
    	sdeModel.setStochasticGenerationEnabled(true);

    	double[] b0 = sdeModel.dy_dt_stochastic(tOld, xOld);
    	double[] b1 = sdeModel.dy_dt_stochastic(tOld, xPredictor);
    	
        for (int i = 0; i < n; i++)
        {
            xNew[i] = xOld[i]
                    + 0.5 * (a0[i] + a1[i]) * h
                    + 0.5 * (b0[i] + b1[i]) * Math.sqrt(h);
        } 
    }
}
