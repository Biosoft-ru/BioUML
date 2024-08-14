package ru.biosoft.analysis;

import java.util.Random;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import cern.jet.random.engine.MersenneTwister;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.util.bean.BeanInfoEx2;

public class RandomNumberAnalysisParameters extends AbstractAnalysisParameters
{
    private DataElementPath output;
    private DiscreteUniformParameters[] randomNumbers = new DiscreteUniformParameters[] {};
    
    private DiscreteUniformParameters randomNumber1 = new DiscreteUniformParameters();
    private DiscreteUniformParameters randomNumber2 = new DiscreteUniformParameters();
    
    private int count = 1;
    private int varNumber = 1;
    
    
    public RandomNumberAnalysisParameters()
    {
        randomNumber1 = new DiscreteUniformParameters();
        randomNumber1.setVariableName("Value 1");
        randomNumber2 = new DiscreteUniformParameters();
        randomNumber2.setVariableName("Value 2");
    }
    
    @PropertyName("Number of variables")
    public int getVarNumber()
    {
        return varNumber;
    }
    public void setVarNumber(int varNumber)
    {
        this.varNumber = varNumber;
        DiscreteUniformParameters[] newDistribution = new DiscreteUniformParameters[varNumber];
        
        for (int i=0; i<varNumber; i++)
        {
            if (i<randomNumbers.length)
                newDistribution[i] = randomNumbers[i];
            else
                newDistribution[i] = new DiscreteUniformParameters();
        }
        this.setRandomNumbers(newDistribution);
    }

    private static Random r = new Random();
    
    @PropertyName ( "Output table" )
    public DataElementPath getOutput()
    {
        return output;
    }
    public void setOutput(DataElementPath output)
    {
        this.output = output;
    }

    @PropertyName ( "Random numbers" )
    public DiscreteUniformParameters[] getRandomNumbers()
    {
        return randomNumbers;
    }
    public void setRandomNumbers(DiscreteUniformParameters[] randomNumbers)
    {
        Object oldValue = this.randomNumbers;
        this.randomNumbers = randomNumbers;
        firePropertyChange("randomNumbers", oldValue, randomNumbers);
        firePropertyChange("*", null, null);
    }

    @PropertyName ( "Count" )
    public int getCount()
    {
        return count;
    }
    public void setCount(int count)
    {
        this.count = count;
    }
    
    @PropertyName("Random number 1")
    public DiscreteUniformParameters getRandomNumber1()
    {
        return randomNumber1;
    }
    public void setRandomNumber1(DiscreteUniformParameters randomNumber1)
    {
        this.randomNumber1 = randomNumber1;
    }
    
    @PropertyName("Random number 2")
    public DiscreteUniformParameters getRandomNumber2()
    {
        return randomNumber2;
    }
    public void setRandomNumber2(DiscreteUniformParameters randomNumber2)
    {
        this.randomNumber2 = randomNumber2;
    }
    

    public static abstract class DistributionParameters
    {
        private String variableName = "Random value";
        private int seed = -1;
        private boolean manualSeed = false;
        protected String type;

//        public DistributionParameters(String name)
//        {
//           this.variableName = name;
//        }
        
        public DistributionParameters()
        {
        }
        
        @PropertyName ( "Seed" )
        @PropertyDescription ( "Seed." )
        public int getSeed()
        {
            return seed;
        }
        public void setSeed(int seed)
        {
            this.seed = seed;
        }

        @PropertyName ( "Manual seed" )
        @PropertyDescription ( "Manual seed." )
        public boolean isManualSeed()
        {
            return manualSeed;
        }
        public void setManualSeed(boolean manualSeed)
        {
            this.manualSeed = manualSeed;
        }

        @PropertyName ( "Variable name" )
        @PropertyDescription ( "Variable name." )
        public String getVariableName()
        {
            return variableName;
        }
        public void setVariableName(String variableName)
        {
            this.variableName = variableName;
        }

        public boolean isAutoSeed()
        {
            return !manualSeed;
        }

        protected MersenneTwister initTwister()
        {
            return manualSeed ? new MersenneTwister(seed) : new MersenneTwister(r.nextInt());
        }
        
        protected Random initRandom()
        {
            Random random = new Random();
            random.setSeed(manualSeed? seed: r.nextInt());
            return random;
        }

        abstract int[] draw(int count); //TODO: change to Object[]
        
        abstract boolean isDiscrete();

    }

    public static class DistributionParametersBeanInfo extends BeanInfoEx2<DistributionParameters>
    {
        public DistributionParametersBeanInfo(Class<? extends DistributionParameters> c)
        {
            super(c);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add("variableName");
            add("manualSeed");
            addReadOnly("seed", "isAutoSeed");
        }
    }
    
    public static class DiscreteUniformParameters extends DistributionParameters
    {
        private int lowerBound = 0;
        private int upperBound = 10000;

        public DiscreteUniformParameters()
        {
        }
        
//        public DiscreteUniformParameters(String name)
//        {
//            super(name);
//        }
        
        @PropertyName ( "Lower boundary" )
        @PropertyDescription ( "Lower boundary." )
        public int getLowerBound()
        {
            return lowerBound;
        }
        public void setLowerBound(int lowerBound)
        {
            this.lowerBound = lowerBound;
        }

        @PropertyName ( "Upper boundary" )
        @PropertyDescription ( "Upper boundary." )
        public int getUpperBound()
        {
            return upperBound;
        }
        public void setUpperBound(int upperBound)
        {
            this.upperBound = upperBound;
        }

        @Override
        int[] draw(int count)
        {
            int[] result = new int[count];
            Random r = initRandom();
            for( int i = 0; i < count; i++ )
            {
                if( upperBound <= lowerBound )
                    result[i] = lowerBound;
                else
                    result[i] = lowerBound + r.nextInt(upperBound - lowerBound);
            }
            return result;
        }
        
        @Override
        boolean isDiscrete()
        {
            return true;
        }
    }
    
    public static class DiscreteUniformParametersBeanInfo extends DistributionParametersBeanInfo
    {
        public DiscreteUniformParametersBeanInfo()
        {
            super(DiscreteUniformParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            super.initProperties();
            add("lowerBound");
            add("upperBound");
        }
    }
    
    public static class UniformParameters extends DistributionParameters
    {
        private double lowerBound = 0;
        private double upperBound = 1;

        @PropertyName ( "Lower boundary" )
        @PropertyDescription ( "Lower boundary." )
        public double getLowerBound()
        {
            return lowerBound;
        }
        public void setLowerBound(double lowerBound)
        {
            this.lowerBound = lowerBound;
        }

        @PropertyName ( "Upper boundary" )
        @PropertyDescription ( "Upper boundary." )
        public double getUpperBound()
        {
            return upperBound;
        }
        public void setUpperBound(double upperBound)
        {
            this.upperBound = upperBound;
        }

        @Override
        int[] draw(int count)
        {
            int[] result = new int[count];
            MersenneTwister mersenneTwister = initTwister();
            for( int i = 0; i < count; i++ )
            {
                result[i] = (int)(lowerBound + ( upperBound - lowerBound ) * mersenneTwister.nextDouble());
            }
            return result;
        }
        
        @Override
        boolean isDiscrete()
        {
            return false;
        }
    }

    public static class UniformParametersBeanInfo extends DistributionParametersBeanInfo
    {
        public UniformParametersBeanInfo()
        {
            super(UniformParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            super.initProperties();
            add("lowerBound");
            add("upperBound");

        }
    }
}