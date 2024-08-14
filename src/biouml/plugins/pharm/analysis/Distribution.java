package biouml.plugins.pharm.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import cern.jet.random.Normal;
import cern.jet.random.engine.MersenneTwister;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class Distribution extends Option
{
    String name;
    DistributionParameters parameters;
    
    public Distribution()
    {
        setName("Normal");
    }

    @PropertyName ( "Name" )
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        String oldValue = this.name;
        this.name = name;
        setParameters(nameToParameters.get(name));
        firePropertyChange("name", oldValue, name);
        firePropertyChange("*", null, null);
    }

    @PropertyName ( "Distribution" )
    public DistributionParameters getParameters()
    {
        return parameters;
    }
    public void setParameters(DistributionParameters parameters)
    {
        DistributionParameters oldValue = this.parameters;
        this.parameters = parameters;
        firePropertyChange("parameters", oldValue, parameters);
    }
    
    public static final Map<String, DistributionParameters> nameToParameters = new HashMap<String, DistributionParameters>()
    {
        {
            put("Normal", new NormalParameters());
            put("Multivariate Normal", new MultivariateNormalParameters());
            put("Uniform", new UniformParameters());
        }
    };

   

    public static interface DistributionParameters
    {
        public abstract double[] draw();
    }

    public static class NormalParameters implements DistributionParameters
    {
        String variableName;
        double mean = 0;
        double variance = 1;
        private MersenneTwister twister = new MersenneTwister();
        private Normal normal = new Normal(0, 1, twister);

        @PropertyName ( "Mean value" )
        @PropertyDescription ( "Mean value." )
        public double getMean()
        {
            return mean;
        }
        public void setMean(double mean)
        {
            this.mean = mean;
        }

        @PropertyName ( "Variance" )
        @PropertyDescription ( "Variance." )
        public double getVariance()
        {
            return variance;
        }
        public void setVariance(double variance)
        {
            this.variance = variance;
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
        @Override
        public double[] draw()
        {
            return new double[]{normal.nextDouble()};
        }
    }

    public static class NormalParametersBeanInfo extends BeanInfoEx2<NormalParameters>
    {
        public NormalParametersBeanInfo()
        {
            super(NormalParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
//            addWithTags("variableName", bean -> bean.get);
            add("mean");
            add("variance");
        }
    }

    public static class MultivariateNormalParameters implements DistributionParameters
    {
        String[] variableNames;
        int n;
        double mean = 0;
        TableDataCollection varianceTable;
        private MersenneTwister twister = new MersenneTwister(new Random().nextInt());
        
//        Normal Normal = new Normal();
        
        @PropertyName ( "Mean value" )
        @PropertyDescription ( "Mean value." )
        public double getMean()
        {
            return mean;
        }
        public void setMean(double mean)
        {
            this.mean = mean;
        }

        @PropertyName ( "Variance" )
        @PropertyDescription ( "Variance." )
        public TableDataCollection getVariance()
        {
            return varianceTable;
        }
        public void setVariance(TableDataCollection variance)
        {
            this.varianceTable = variance;
        }

        public String[] getVariableNames()
        {
            return variableNames;
        }
        public void setVariableNames(String[] variableNames)
        {
            this.variableNames = variableNames;
        }
        @Override
        public double[] draw()
        {
            if (n==2)
            {
                
            }
            return new double[]{twister.nextDouble()};
        }
    }

    public static class MultivariateNormalParametersBeanInfo extends BeanInfoEx2<MultivariateNormalParameters>
    {
        public MultivariateNormalParametersBeanInfo()
        {
            super(MultivariateNormalParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add("variableNames");
            add("mean");
            add("variance");
        }
    }

    public static class UniformParameters implements DistributionParameters
    {
        String variableName;
        double lowerBound = 0;
        double upperBound = 1;
        private MersenneTwister twister = new MersenneTwister();
        
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
        @Override
        public double[] draw()
        {
            return new double[]{twister.nextDouble()};
        }
    }

    public static class UniformParametersBeanInfo extends BeanInfoEx2<UniformParameters>
    {
        public UniformParametersBeanInfo()
        {
            super(UniformParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add("lowerBound");
            add("upperBound");

        }
    }
}
