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
        SdeModel sdeModel = (SdeModel)odeModel;
        
        double[] xPredictor = StdMet.copyArray(xNew);
        super.integrationStep(xPredictor, xOld, tOld, h);
        
        double[] dydt_deterministic = sdeModel.dy_dt_deterministic(tOld, xOld);
        double[] dydt_deterministic_predictor = sdeModel.dy_dt_deterministic(tOld + h, xPredictor);
        double[] dydt_deterministic_corrector = StdMet.copyArray(dydt_deterministic_predictor);
        StdMet.arraySum(dydt_deterministic_corrector, dydt_deterministic, dydt_deterministic_predictor);
        
        double[] dydt_stochastic = sdeModel.dy_dt_stochastic(tOld, xOld);
        double[] dydt_stochastic_predictor = sdeModel.dy_dt_stochastic(tOld + h, xPredictor);
        double[] dydt_stochastic_corrector = StdMet.copyArray(dydt_stochastic_predictor);
        StdMet.arraySum(dydt_stochastic_corrector, dydt_stochastic, dydt_stochastic_predictor);
        
        for (int i = 0; i < n; i++) 
        {
            xNew[i] = xOld[i] + 0.5 * dydt_deterministic_corrector[i] * h + 0.5 * dydt_stochastic_corrector[i] * Math.sqrt(h);
        }
    }
}
