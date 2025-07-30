package biouml.plugins.physicell;

import java.util.Arrays;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.Node;
import one.util.streamex.StreamEx;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.DistributedParameter;
import ru.biosoft.physicell.core.Distribution;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.util.bean.BeanInfoEx2;

public class InitialDistributionProperties extends Option
{
    public static String[] distributions = new String[] {"Uniform", "LogUniform", "Normal", "LogNormal", "Log10Normal"};
    private Node node;
    private CellDefinitionProperties cellDefinition;
    private MulticellEModel model = null;
    private String[] availableParameters = new String[0];

    private ParameterDistribution[] parameterDistributions = new ParameterDistribution[0];

    public InitialDistributionProperties()
    {
    }

    public InitialDistributionProperties(Node de)
    {
        setDiagramElement( de );
    }

    @PropertyName ( "Distributions" )
    public ParameterDistribution[] getParameterDistributions()
    {
        return parameterDistributions;
    }
    public void setParameterDistributions(ParameterDistribution[] parameterDistributions)
    {
        this.parameterDistributions = parameterDistributions;
        for( ParameterDistribution pDistribution : parameterDistributions )
        {
            pDistribution.setParent( this );
            pDistribution.setAvailableParameters( availableParameters );
        }
    }

    public void setDiagramElement(Node node)
    {
        this.node = node;
        if( node != null )
        {
            cellDefinition = node.getRole( CellDefinitionProperties.class );
            model = Diagram.getDiagram( node ).getRole( MulticellEModel.class );
            availableParameters = RuleProperties.getAvailableDistributed( model, cellDefinition ).toArray( String[]::new );
            for( ParameterDistribution pDistribution : parameterDistributions )
                pDistribution.setAvailableParameters( availableParameters );
        }
    }

    public void update()
    {
        if( node != null )
            this.setDiagramElement( node );
    }

    public void createInitialDistribution(CellDefinition cd, Model model) throws Exception
    {
        cd.distribution.distributions = new DistributedParameter[parameterDistributions.length];
        for( int i = 0; i < parameterDistributions.length; i++ )
        {
            DistributedParameter sDistribution = new DistributedParameter();
            sDistribution.parameter = parameterDistributions[i].parameter;
            sDistribution.distribution = new Distribution( parameterDistributions[i].distribution );
            DistributionProperties properties = parameterDistributions[i].properties;
            sDistribution.distribution.addParameter( "min", properties.getMin() );
            sDistribution.distribution.addParameter( "max", properties.getMax() );
            if( properties instanceof NormalProperties )
            {
                sDistribution.distribution.addParameter( "mu", ( (NormalProperties)properties ).getMu() );
                sDistribution.distribution.addParameter( "sigma", ( (NormalProperties)properties ).getSigma() );
            }
            cd.distribution.distributions[i] = sDistribution;
        }
    }

    public void init(CellDefinition cd)
    {
        DistributedParameter[] distributions = cd.distribution.distributions;
        this.parameterDistributions = new ParameterDistribution[distributions.length];
        for( int i = 0; i < distributions.length; i++ )
        {
            DistributedParameter distribution = distributions[i];
            String distributionName = distribution.distribution.getName();
            String parameter = distribution.parameter;

            parameterDistributions[i] = new ParameterDistribution();
            parameterDistributions[i].setDistribution( distributionName );
            parameterDistributions[i].setParameter( parameter );
            DistributionProperties properties = parameterDistributions[i].getProperties();
            properties.setMin( distribution.distribution.getValue( "min" ) );
            properties.setMax( distribution.distribution.getValue( "max" ) );
            if( properties instanceof NormalProperties )
            {
                ( (NormalProperties)properties ).setMu( distribution.distribution.getValue( "mu" ) );
                ( (NormalProperties)properties ).setSigma( distribution.distribution.getValue( "sigma" ) );
            }
        }
    }

    public InitialDistributionProperties clone(Node node)
    {
        InitialDistributionProperties result = new InitialDistributionProperties();
        result.setParameterDistributions(
                Arrays.stream( parameterDistributions ).map( pd -> pd.clone() ).toArray( ParameterDistribution[]::new ) );
        result.setDiagramElement( node );
        return result;
    }

    public static class ParameterDistribution extends Option
    {
        private String parameter = "volume";
        private String distribution = "Uniform";
        private DistributionProperties properties;
        private String[] availableParameters = new String[0];

        public ParameterDistribution()
        {
            properties = new UniformProperties();
            properties.setParent( this );
        }
        
        public void setAvailableParameters(String[] availableParameters)
        {
            this.availableParameters = availableParameters;
        }

        public String[] getAvailableParameters()
        {
            return availableParameters;
        }

        @PropertyName ( "Model parameter" )
        public String getParameter()
        {
            return parameter;
        }
        public void setParameter(String parameter)
        {
            this.parameter = parameter;
        }

        @PropertyName ( "Distribution" )
        public String getDistribution()
        {
            return distribution;
        }
        public void setDistribution(String distribution)
        {
            String oldValue = this.distribution;
            this.distribution = distribution;
            if( distribution.equals( "Uniform" ) || distribution.equals( "LogUniform" ) )
                setProperties( new UniformProperties() );
            else
                setProperties( new NormalProperties() );
            firePropertyChange( "distribution", oldValue, distribution );
//            firePropertyChange( "*", null, null );
        }

        @PropertyName ( "Distribution properties" )
        public DistributionProperties getProperties()
        {
            return properties;
        }
        public void setProperties(DistributionProperties properties)
        {
            DistributionProperties oldValue = this.properties;
            this.properties = properties;
            this.properties.setParent( this );
            firePropertyChange( "properties", oldValue, properties );
            firePropertyChange( "*", null, null );
        }

        @Override
        public ParameterDistribution clone()
        {
            ParameterDistribution result = new ParameterDistribution();
            result.setParameter( parameter );
            result.setAvailableParameters( availableParameters );
            result.setDistribution( distribution );
            result.setProperties( properties.clone() );
            return result;
        }
    }

    public static class ParameterDistributionBeanInfo extends BeanInfoEx2<ParameterDistribution>
    {
        public ParameterDistributionBeanInfo()
        {
            super( ParameterDistribution.class );
        }

        @Override
        public void initProperties()
        {
            property( "parameter" ).tags( bean -> StreamEx.of( bean.getAvailableParameters() ) ).add();
            property( "distribution" ).tags( distributions ).structureChanging().add();
            add( "properties" );
        }
    }

    public static class DistributionProperties extends Option
    {
        private double min = 0;
        private double max = 1;

        @PropertyName ( "Min" )
        public double getMin()
        {
            return min;
        }
        public void setMin(double min)
        {
            this.min = min;
        }

        @PropertyName ( "Max" )
        public double getMax()
        {
            return max;
        }
        public void setMax(double max)
        {
            this.max = max;
        }

        @Override
        public DistributionProperties clone()
        {
            return new DistributionProperties();
        }
    }

    public static class DistributionPropertiesBeanInfo extends BeanInfoEx
    {
        public DistributionPropertiesBeanInfo(Class<?> beanClass)
        {
            super( beanClass, true );
        }

        public DistributionPropertiesBeanInfo()
        {
            super( DistributionProperties.class, true );
        }

        @Override
        public void initProperties()
        {
            add( "min" );
            add( "max" );
        }
    }
    public static class UniformProperties extends DistributionProperties
    {


        @Override
        public UniformProperties clone()
        {
            UniformProperties result = new UniformProperties();
            result.setMax( getMax() );
            result.setMin( getMin() );
            return result;
        }
    }

    public static class UniformPropertiesBeanInfo extends DistributionPropertiesBeanInfo
    {
        public UniformPropertiesBeanInfo()
        {
            super( UniformProperties.class );
        }

        @Override
        public void initProperties()
        {
            add( "min" );
            add( "max" );
        }
    }

    public static class NormalProperties extends DistributionProperties
    {
        private double mu = 0;
        private double sigma = 1;
        //        private double min = -9E9;
        //        private double max = 9E9;
        //        
        //        @PropertyName("Min")
        //        public double getMin()
        //        {
        //            return min;
        //        }
        //        public void setMin(double min)
        //        {
        //            this.min = min;
        //        }
        //        
        //        @PropertyName("Max")
        //        public double getMax()
        //        {
        //            return max;
        //        }
        //        public void setMax(double max)
        //        {
        //            this.max = max;
        //        }

        @PropertyName ( "Mu" )
        public double getMu()
        {
            return mu;
        }
        public void setMu(double mu)
        {
            this.mu = mu;
        }

        @PropertyName ( "Sigma" )
        public double getSigma()
        {
            return sigma;
        }
        public void setSigma(double sigma)
        {
            this.sigma = sigma;
        }

        @Override
        public NormalProperties clone()
        {
            NormalProperties result = new NormalProperties();
            result.setMax( getMax() );
            result.setMin( getMin() );
            result.setMu( mu );
            result.setSigma( sigma );
            return result;
        }
    }

    public static class NormalPropertiesBeanInfo extends DistributionPropertiesBeanInfo
    {
        public NormalPropertiesBeanInfo()
        {
            super( NormalProperties.class );
        }

        @Override
        public void initProperties()
        {
            super.initProperties();
            add( "mu" );
            add( "sigma" );
        }
    }
}