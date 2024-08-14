package biouml.plugins.pharm.nlme;

import java.util.HashMap;
import java.util.Map;

import biouml.plugins.simulation.Model;

public class MixedEffectModel implements Model
{
    Model odeModel;
    protected double[][] doseVals;
    protected double[][] doseTimes;
    protected Map<String, Integer> varIndexMapping;
    protected int resultIndex;

    public MixedEffectModel(Model model, ExperimentInfo expInfo)
    {
        this.odeModel = model;
        expInfo.initDosing();
        doseTimes = expInfo.getDoseTimes();
        doseVals = expInfo.getDoseVals();
    }

    //override this method
    public Map<String, Double> getDoseParameters(int subject)
    {
        return new HashMap<>();
    }

    public Model getODEModel()
    {
        return odeModel;
    }

    public Map<String, Integer> getVarIndexMapping()
    {
        return varIndexMapping;
    }

    public int getResultIndex()
    {
        return resultIndex;
    }

    @Override
    public double[] getInitialValues() throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void init() throws Exception
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void init(double[] initialValues, Map<String, Double> parameters) throws Exception
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isInit()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public double[] getCurrentValues() throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setCurrentValues(double[] values) throws Exception
    {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public MixedEffectModel clone()
    {
        return this;
    }
    
    @Override
	public double[] getCurrentState() throws Exception 
	{
		return null;
	}
}
