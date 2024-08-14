package ru.biosoft.analysis.optimization.methods;

import java.util.Properties;
import java.util.Random;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysis.optimization.OptimizationMethod;
import ru.biosoft.analysis.optimization.OptimizationMethodParameters;
import ru.biosoft.analysis.optimization.OptimizationMethodParametersBeanInfo;
import ru.biosoft.analysis.optimization.OptimizationProblem;


/*
 * This algorithm was created based on the paper
 * Thomas P. Runarsson and Xin Yao
 * Stochastic Ranking for Constrained Evolutionary Optimization.
 * (IEEE Transactions on Evolutionary Computation, vol. 4, #3, September 2000)
 */
public class SRESOptMethod extends OptimizationMethod<SRESOptMethod.SRESOptMethodParameters>
{
    public SRESOptMethod(DataCollection<?> origin, String name)
    {
        super(origin, name, null);
        parameters = new SRESOptMethodParameters();
        individualsNumber = 7 * parameters.getSurvivalSize();
    }

    @PropertyName("Method parameters")
    @PropertyDescription("Method parameters.")
    public class SRESOptMethodParameters extends OptimizationMethodParameters
    {
        private int numOfIterations;
        private int survivalSize;

        public SRESOptMethodParameters()
        {
            this.numOfIterations = 500;
            this.survivalSize = 20;
            random = new Random();
        }

        @PropertyName ( "Number of iterations" )
        @PropertyDescription ( "The number of iterations after which the optimization process will be stopped." )
        public int getNumOfIterations()
        {
            return this.numOfIterations;
        }
        public void setNumOfIterations(int numOfIterations)
        {
            int oldValue = this.numOfIterations;
            this.numOfIterations = numOfIterations;
            firePropertyChange("numOfIterations", oldValue, numOfIterations);
        }

        @PropertyName (  "Survival size" )
        @PropertyDescription (  "The number of the best solutions survived in the generated population (size of which is 7 times larger) at each iteration of the optimization method." )
        public int getSurvivalSize()
        {
            return this.survivalSize;
        }
        public void setSurvivalSize(int survivalSize)
        {
            int oldValue = this.survivalSize;
            this.survivalSize = survivalSize;
            individualsNumber = 7 * survivalSize;
            firePropertyChange("survivalSize", oldValue, survivalSize);
        }

        @Override
        public void read(Properties properties, String prefix)
        {
            super.read(properties, prefix);
            try
            {
                numOfIterations = Integer.parseInt(properties.getProperty(prefix + "numOfIterations"));
            }
            catch( Exception e )
            {
            }
            try
            {
                survivalSize = Integer.parseInt(properties.getProperty(prefix + "survivalSize"));
            }
            catch( Exception e )
            {
            }
        }

        @Override
        public void write(Properties properties, String prefix)
        {
            super.write(properties, prefix);
            properties.put(prefix + "numOfIterations", Integer.toString(numOfIterations));
            properties.put(prefix + "survivalSize", Integer.toString(survivalSize));
        }
    }

    public static class SRESOptMethodParametersBeanInfo extends OptimizationMethodParametersBeanInfo
    {
        public SRESOptMethodParametersBeanInfo()
        {
            super(SRESOptMethodParameters.class);
        }

        @Override
        public void initProperties() throws Exception
        {
            super.initProperties();
            add("numOfIterations");
            add("survivalSize");
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Optimization problem
    //
    private double[][] population;
    private double[][] sigmas;

    //learning rates
    private double tau;
    private double tauPrime;

    @Override
    public void setOptimizationProblem(OptimizationProblem problem)
    {
        super.setOptimizationProblem(problem);

        if( problem != null )
        {
            population = new double[individualsNumber][n];
            sigmas = new double[individualsNumber][n];
            stepsNumber = parameters.getNumOfIterations();
            tau = 1 / Math.sqrt(2 * Math.sqrt(n));
            tauPrime = 1 / Math.sqrt(2 * n);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Realization of the method
    //

    @Override
    public double getDeviation()
    {
        return distances[0];
    }

    @Override
    public double getPenalty()
    {
        return penalties[0];
    }

    @Override
    public double[] getIntermediateSolution()
    {
        return population[0].clone();
    }

    @Override
    public double[] getSolution()
    {
        Random random = getParameters().initRandom();
        init(random);
        calculateDistances(population);

        int i;
        for( i = 0; i < parameters.getNumOfIterations() && go; i++ )
        {
            sort(random);
            generate(random);
            calculateDistances(population);

            displayInfo();
            incPreparedness(i + 1);
        }

        if( i == 0 )
            sort(random);

        return population[0];
    }

    private void init(Random random)
    {
        for( int i = 0; i < individualsNumber; i++ )
        {
            for( int j = 0; j < n; j++ )
            {
                double lowerBound = params.get(j).getLowerBound();
                double upperBound = params.get(j).getUpperBound();

                if( i == 0 )
                    population[i][j] = params.get(j).getValue();
                else
                    population[i][j] = lowerBound + random.nextDouble() * ( upperBound - lowerBound );

                sigmas[i][j] = ( upperBound - lowerBound ) / Math.sqrt(n);
            }
        }
    }

    private void sort(Random random)
    {
        double p_f = 0.475;

        for( int i = 0; i < individualsNumber - 1; i++ )
        {
            boolean wasSwapped = false;

            for( int j = 0; j < ( individualsNumber - 1 - i ) && go; j++ )
            {
                if( ( ( penalties[j] == 0 ) && ( penalties[j + 1] == 0 ) ) || ( random.nextDouble() < p_f ) )
                {
                    if( distances[j] > distances[j + 1] )
                    {
                        wasSwapped = true;
                        swap(population, j);
                        swap(sigmas, j);
                        swap(distances, j);
                        swap(penalties, j);
                    }
                }
                else
                {
                    if( penalties[j] > penalties[j + 1] )
                    {
                        wasSwapped = true;
                        swap(population, j);
                        swap(sigmas, j);
                        swap(distances, j);
                        swap(penalties, j);
                    }
                }
            }

            if( !wasSwapped )
            {
                break;
            }
        }
    }

    private void swap(double[][] matrix, int j)
    {
        for( int i = 0; i < n; ++i )
        {
            double oneValue = matrix[j][i];
            matrix[j][i] = matrix[j + 1][i];
            matrix[j + 1][i] = oneValue;
        }
    }

    private void swap(double[] vector, int j)
    {
        double oneValue = vector[j];
        vector[j] = vector[j + 1];
        vector[j + 1] = oneValue;
    }

    private void generate(Random random)
    {
        double[][] newPopulation = new double[individualsNumber][n];
        double[][] newSigmas = new double[individualsNumber][n];

        int timer;
        int i = 0;

        double u = random.nextGaussian();

        for( int h = 0; h < individualsNumber; h++ )
        {
            if( i == parameters.getSurvivalSize() )
                i = 0;

            for( int j = 0; j < n; j++ )
            {
                int k_j = 0;
                if( parameters.getSurvivalSize() > 1 )
                    k_j = random.nextInt(parameters.getSurvivalSize() - 1);

                double factor = ( sigmas[i][j] + Math.min(sigmas[k_j][j], sigmas[i][j]) ) / 2;
                newSigmas[h][j] = factor * Math.exp(tauPrime * u + tau * random.nextGaussian());

                if( h == 0 )
                {
                    newPopulation[h][j] = population[i][j];
                }
                else
                {
                    double lowerBound = params.get(j).getLowerBound();
                    double upperBound = params.get(j).getUpperBound();

                    for( timer = 0; timer < 10; timer++ )
                    {
                        newPopulation[h][j] = population[i][j] + newSigmas[h][j] * random.nextGaussian();

                        if( ( newPopulation[h][j] < upperBound ) && ( newPopulation[h][j] > lowerBound ) )
                            break;
                    }

                    if( timer == 10 )
                        newPopulation[h][j] = population[h][j];
                }
            }

            i++;
        }

        for( int h = 0; h < individualsNumber; ++h )
        {
            for( int j = 0; j < n; ++j )
            {
                population[h][j] = newPopulation[h][j];
                sigmas[h][j] = newSigmas[h][j];
            }
        }
    }
}
