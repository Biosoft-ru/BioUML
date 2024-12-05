package biouml.plugins.virtualcell.simulation;

import biouml.plugins.simulation.Span;

public class PopulationAgent extends ProcessAgent
{
    private double[] coefficients;
    private double[] molecules;
    private double[] population;
    
    public PopulationAgent(String name, Span span)
    {
        super( name, span );
    }

    @Override
    public void doStep()
    {
        for( int i = 0; i < molecules.length; i++ )
        {
            population[i] = molecules[i] * coefficients[i];
        }
    }

    @Override
    public void setValue(String variable, String name, double value)
    {

        int index = nameToIndex.get( name );
        if( variable.equals( "Protein" ) )
            molecules[index] = value;
        else
            coefficients[index] = value;
    }

    @Override
    public double getValue(String variable, String name)
    {
        int index = nameToIndex.get( name );
        return population[index];
    }

    @Override
    public void initPoolVariables(MapPool pool)
    {
        super.initPoolVariables( pool );
        coefficients = new double[nameToIndex.size()];
        molecules = new double[nameToIndex.size()];
        population = new double[nameToIndex.size()];
    }
}