package biouml.plugins.physicell.web;

import java.util.stream.Stream;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.plugins.physicell.CellDefinitionProperties;
import biouml.plugins.physicell.MulticellEModel;
import biouml.plugins.physicell.RuleProperties;
import biouml.plugins.physicell.SubstrateProperties;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.providers.WebDiagramsProvider;
import ru.biosoft.table.access.TableResolver;
import ru.biosoft.util.bean.BeanInfoEx2;

public class PhysicellWebTableResolver extends TableResolver
{
    public static final String TYPE_PARAMETER = "tabletype";
    protected String type;

    public PhysicellWebTableResolver(BiosoftWebRequest arguments) throws WebException
    {
        this.type = arguments.getString(TYPE_PARAMETER);
    }

    @Override
    public DataCollection<?> getTable(DataElement de) throws Exception
    {
        if ( "substrates".equals(type) && de instanceof Diagram )
        {
            Diagram diagram = de.cast(Diagram.class);
            MulticellEModel model = diagram.getRole(MulticellEModel.class);
            VectorDataCollection<SubstratePropertiesWrapper> result = new VectorDataCollection<>("Substrates", SubstratePropertiesWrapper.class, null);
            model.getSubstrates().forEach(sp -> result.put(new SubstratePropertiesWrapper(sp)));
            return result;
        }
        else if ( "rules".equals(type) )
        {
            DataElementPath fullPath = de.getCompletePath();
            String elemName = fullPath.getName();
            try
            {
                Diagram diagram = WebDiagramsProvider.getDiagram(fullPath.getParentPath().toString(), false);
                DiagramElement elem = diagram.get(elemName);
                CellDefinitionProperties props = (CellDefinitionProperties) elem.getRole();
                VectorDataCollection<RulesPropertiesWrapper> result = new VectorDataCollection<>("Rules", RulesPropertiesWrapper.class, null);
                RuleProperties[] rps = props.getRulesProperties().getRules();
                for ( int i = 0; i < rps.length; i++ )
                {
                    result.put(new RulesPropertiesWrapper(String.valueOf(i), rps[i]));
                }
                return result;
            }
            catch (Exception e)
            {

            }

        }
        return null;
    }

    public static class RulesPropertiesWrapper implements DataElement
    {
        private RuleProperties rp = null;
        private String name;

        public RulesPropertiesWrapper()
        {
            this.rp = new RuleProperties();
            name = "-1";
        }

        public RulesPropertiesWrapper(String name, RuleProperties rp)
        {
            this.rp = rp;
            this.name = name;
        }

        @PropertyName ( "Signal" )
        public String getSignal()
        {
            return rp.getSignal();
        }
        public void setSignal(String signal)
        {
            this.rp.setSignal( signal );
        }

        @PropertyName ( "Response" )
        public String getDirection()
        {
            return rp.getDirection();
        }
        public void setDirection(String direction)
        {
            this.rp.setDirection( direction );
        }

        @PropertyName ( "Behavior" )
        public String getBehavior()
        {
            return rp.getBehavior();
        }
        public void setBehavior(String behavior)
        {
            this.rp.setBehavior( behavior );
        }

        @PropertyName ( "Half max" )
        public double getHalfMax()
        {
            return rp.getHalfMax();
        }
        public void setHalfMax(double halfMax)
        {
            this.rp.setHalfMax( halfMax );
        }

        @PropertyName ( "Hill power" )
        public double getHillPower()
        {
            return rp.getHillPower();
        }
        public void setHillPower(double hillPower)
        {
            this.rp.setHillPower( hillPower );
        }

        @PropertyName ( "Apply to dead" )
        public boolean isApplyToDead()
        {
            return rp.isApplyToDead();
        }
        public void setApplyToDead(boolean applyToDead)
        {
            this.rp.setApplyToDead( applyToDead );
        }

        @PropertyName ( "Saturation value" )
        public double getSaturationValue()
        {
            return rp.getSaturationValue();
        }
        public void setSaturationValue(double saturationVale)
        {
            this.rp.setSaturationValue( saturationVale );
        }

        public String[] getAvailableDirections()
        {
            return rp.getAvailableDirections();
        }

        public Stream<String> getAvailableSignals()
        {
            return rp.getAvailableSignals();
        }

        public Stream<String> getAvailableBehaviors()
        {
            return rp.getAvailableBehaviors();
        }

        @Override
        public String getName()
        {
            return name;
        }

        @Override
        public DataCollection<?> getOrigin()
        {
            return null;
        }
    }

    public static class RulesPropertiesWrapperBeanInfo extends BeanInfoEx2<RulesPropertiesWrapper>
    {
        public RulesPropertiesWrapperBeanInfo()
        {
            super( RulesPropertiesWrapper.class );
        }

        @Override
        public void initProperties() throws Exception
        {
            property( "signal" ).tags( bean -> bean.getAvailableSignals() ).add();
            property( "direction" ).tags( bean -> Stream.of( bean.getAvailableDirections() ) ).add();
            property( "behavior" ).tags( bean -> bean.getAvailableBehaviors() ).add();
            //        add( "baseValue" );
            add( "saturationValue" );
            add( "halfMax" );
            add( "hillPower" );
            add( "applyToDead" );
        }
    }

    public static class SubstratePropertiesWrapper implements DataElement
    {
        private SubstrateProperties sp = null;

        public SubstratePropertiesWrapper()
        {
            this.sp = new SubstrateProperties("Empty");
        }

        public SubstratePropertiesWrapper(SubstrateProperties sp)
        {
            this.sp = sp;
        }

        @Override
        public String getName()
        {

            return sp.getName();
        }

        public void setName(String name)
        {
            sp.setName(name);
        }

        @Override
        public DataCollection<?> getOrigin()
        {
            return null;
        }

        @PropertyName("Initial condition")
        public double getInitialCondition()
        {
            return sp.getInitialCondition();
        }

        public void setInitialCondition(double initialCondition)
        {
            sp.setInitialCondition(initialCondition);
        }

        @PropertyName("Decay rate")
        public double getDecayRate()
        {
            return sp.getDecayRate();
        }

        public void setDecayRate(double decayRate)
        {
            sp.setDecayRate(decayRate);
        }

        @PropertyName("Diffusion coefficient")
        public double getDiffusionCoefficient()
        {
            return sp.getDiffusionCoefficient();
        }

        public void setDiffusionCoefficient(double diffusionCoefficient)
        {
            sp.setDiffusionCoefficient(diffusionCoefficient);
        }

        @PropertyName("Dirichlet condition")
        public boolean isDirichletCondition()
        {
            return sp.isDirichletCondition();
        }

        public void setDirichletCondition(boolean dirichletCondition)
        {
            sp.setDirichletCondition(dirichletCondition);
        }

        @PropertyName("Dirichlet value")
        public double getDirichletValue()
        {
            return sp.getDirichletValue();
        }

        public void setDirichletValue(double dirichletValue)
        {
            sp.setDirichletValue(dirichletValue);
        }

        public boolean isCompleted()
        {
            return sp.isCompleted();
        }

    }

    public static class SubstratePropertiesWrapperBeanInfo extends BeanInfoEx2<SubstratePropertiesWrapper>
    {
        public SubstratePropertiesWrapperBeanInfo()
        {
            super(SubstratePropertiesWrapper.class);
        }

        @Override
        public void initProperties() throws Exception
        {
            addReadOnly("name", "isCompleted");
            add("initialCondition");
            add("decayRate");
            add("diffusionCoefficient");
            add("dirichletCondition");
            add("dirichletValue");
        }
    }

}