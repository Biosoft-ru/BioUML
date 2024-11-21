package biouml.plugins.virtualcell.simulation;

import biouml.plugins.simulation.Span;
import biouml.plugins.virtualcell.core.Pool;


public class TranslationAgent extends ProcessAgent
{
    private double[] rates;
    private double[] molecules;
    private double[] rna;

    public TranslationAgent(String name, Span span)
    {
        super( name, span );
    }

    @Override
    public void doStep()
    {
        for( int i = 0; i < molecules.length; i++ )
        {
            molecules[i] = molecules[i] + rates[i] * rna[i] * delta;
        }
    }

    @Override
    public void setValue(String variable, String name, double value)
    {

        int index = nameToIndex.get( name );
        if( name.equals( "RNA" ) )
            rna[index] = value;
        else
            rates[index] = value;
    }

    @Override
    public double getValue(String variable, String name)
    {
        int index = nameToIndex.get( name );
        return molecules[index];
    }

    @Override
    public void initPoolVariables(Pool pool)
    {
        super.initPoolVariables( pool );
        rates = new double[nameToIndex.size()];
        molecules = new double[nameToIndex.size()];
    }

}