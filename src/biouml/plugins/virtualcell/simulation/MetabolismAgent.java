package biouml.plugins.virtualcell.simulation;

import biouml.plugins.fbc.FbcModel;
import biouml.plugins.simulation.Span;

public class MetabolismAgent extends ProcessAgent
{
    private double[] rates;
    private FbcModel model;

    public MetabolismAgent(String name, Span span)
    {
        super( name, span );
    }

    @Override
    public void doStep()
    {
        model.optimize();
        for( String key : nameToIndex.keySet() )
        {
            double val = model.getOptimValue( key );
            int index = nameToIndex.get( key );
            rates[index] = val;
        }
    }
    
    public void setModel(FbcModel model)
    {
        this.model = model;
    }

    @Override
    public void setValue(String variable, String name, double value)
    {

    }

    @Override
    public double getValue(String variable, String name)
    {
        int index = nameToIndex.get( name );
        return rates[index];
    }

    @Override
    public void initPoolVariables(MapPool pool)
    {
        super.initPoolVariables( pool );
        rates = new double[nameToIndex.size()];
    }
}