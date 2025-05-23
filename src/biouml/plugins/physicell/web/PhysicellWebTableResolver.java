package biouml.plugins.physicell.web;

import java.awt.Color;
import java.util.stream.Stream;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.plugins.physicell.CellDefinitionProperties;
import biouml.plugins.physicell.CellDefinitionVisualizerProperties;
import biouml.plugins.physicell.ColorScheme;
import biouml.plugins.physicell.EventProperties;
import biouml.plugins.physicell.MulticellEModel;
import biouml.plugins.physicell.RuleProperties;
import biouml.plugins.physicell.SubstrateProperties;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.ColorEditor;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.PenEditor;
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
        this.type = arguments.getString( TYPE_PARAMETER );
    }

    @Override
    public DataCollection<?> getTable(DataElement de) throws Exception
    {
        if( "substrates".equals( type ) && de instanceof Diagram )
        {
            Diagram diagram = de.cast( Diagram.class );
            MulticellEModel model = diagram.getRole( MulticellEModel.class );
            VectorDataCollection<SubstratePropertiesWrapper> result = new VectorDataCollection<>( "Substrates",
                    SubstratePropertiesWrapper.class, null );
            model.getSubstrates().forEach( sp -> result.put( new SubstratePropertiesWrapper( sp ) ) );
            return result;
        }
        else if( "cell_types".equals( type ) && de instanceof Diagram )
        {
            Diagram diagram = de.cast( Diagram.class );
            MulticellEModel model = diagram.getRole( MulticellEModel.class );
            VectorDataCollection<CellDefinitionWrapper> result = new VectorDataCollection<>( "Cell Types", CellDefinitionWrapper.class,
                    null );
            model.getCellDefinitions().forEach( cdp -> result.put( new CellDefinitionWrapper( cdp ) ) );
            return result;
        }
        else if( "events".equals( type ) && de instanceof Diagram )
        {
            Diagram diagram = de.cast( Diagram.class );
            MulticellEModel model = diagram.getRole( MulticellEModel.class );
            VectorDataCollection<EventWrapper> result = new VectorDataCollection<>( "Events", EventWrapper.class, null );
            model.getEvents().forEach( e -> result.put( new EventWrapper( e ) ) );
            return result;
        }
        else if( "color_schemes".equals( type ) && de instanceof Diagram )
        {
            Diagram diagram = de.cast( Diagram.class );
            MulticellEModel model = diagram.getRole( MulticellEModel.class );
            VectorDataCollection<ColorSchemeWrapper> result = new VectorDataCollection<>( "Color schemes", ColorSchemeWrapper.class, null );
            Stream.of( model.getColorSchemes() ).forEach( cs -> result.put( new ColorSchemeWrapper( cs ) ) );
            return result;
        }
        else if( "visualizers".equals( type ) && de instanceof Diagram )
        {
            Diagram diagram = de.cast( Diagram.class );
            MulticellEModel model = diagram.getRole( MulticellEModel.class );
            VectorDataCollection<VisualizerPropertiesWrapper> result = new VectorDataCollection<>( "Visualizers",
                    VisualizerPropertiesWrapper.class, null );
            CellDefinitionVisualizerProperties[] properties = model.getVisualizerProperties().getProperties();
            for( int i = 0; i < properties.length; i++ )
            {
                result.put( new VisualizerPropertiesWrapper( String.valueOf( i ), properties[i] ) );
            }
            return result;
        }
        else if( "rules".equals( type ) )
        {
            DataElementPath fullPath = de.getCompletePath();
            String elemName = fullPath.getName();
            try
            {
                Diagram diagram = WebDiagramsProvider.getDiagram( fullPath.getParentPath().toString(), false );
                DiagramElement elem = diagram.get( elemName );
                CellDefinitionProperties props = (CellDefinitionProperties)elem.getRole();
                VectorDataCollection<RulesPropertiesWrapper> result = new VectorDataCollection<>( "Rules", RulesPropertiesWrapper.class,
                        null );
                RuleProperties[] rps = props.getRulesProperties().getRules();
                for( int i = 0; i < rps.length; i++ )
                {
                    result.put( new RulesPropertiesWrapper( String.valueOf( i ), rps[i] ) );
                }
                return result;
            }
            catch( Exception e )
            {

            }

        }
        return null;
    }

    public static class CellDefinitionWrapper implements DataElement
    {
        private CellDefinitionProperties cdp;
        Pen penOuter;
        Pen penInner;

        public CellDefinitionWrapper(CellDefinitionProperties cdp)
        {
            this.cdp = cdp;
        }

        @PropertyName ( "Name" )
        public String getName()
        {
            return cdp.getName();
        }
        public void setName(String name)
        {
            //           cdp.setName( name );;
        }

        @PropertyName ( "Color outer" )
        public Pen getPenOuter()
        {
            return new Pen( 1, cdp.getColor() == null ? Color.black : cdp.getColor().getColor() );
        }

        public void setPenOuter(Pen pen)
        {
            this.penOuter = pen;
            if( pen != null )
                cdp.setColor( new Brush( pen.getColor() ) );
        }

        @PropertyName ( "Color inner" )
        public Pen getPenInner()
        {
            return new Pen( 1, cdp.getColor() == null ? Color.black : cdp.getColor().getColor() );
        }

        public void setPenInner(Pen pen)
        {
            this.penInner = pen;
            if( pen != null )
                cdp.setColor( new Brush( pen.getColor() ) );
        }

        @PropertyName ( "Initial number" )
        public int getInitialNumber()
        {
            return cdp.getInitialNumber();
        }

        public void setInitialNumber(int initialNumber)
        {
            cdp.setInitialNumber( initialNumber );
        }

        @PropertyName ( "Comment" )
        public String getComment()
        {
            return cdp.getComment();
        }

        public void setComment(String comment)
        {
            cdp.setComment( comment );
        }

        @Override
        public DataCollection<?> getOrigin()
        {
            // TODO Auto-generated method stub
            return null;
        }

    }

    public static class CellDefinitionWrapperBeanInfo extends BeanInfoEx2<CellDefinitionWrapper>
    {
        public CellDefinitionWrapperBeanInfo()
        {
            super( CellDefinitionWrapper.class );
        }

        @Override
        public void initProperties() throws Exception
        {
            addReadOnly( "name" );
            add( "initialNumber" );
            add("penOuter");
            add("penInner");
            add( "comment" );
        }
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
            this.sp = new SubstrateProperties( "Empty" );
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
            sp.setName( name );
        }

        @Override
        public DataCollection<?> getOrigin()
        {
            return null;
        }

        @PropertyName ( "Initial condition" )
        public double getInitialCondition()
        {
            return sp.getInitialCondition();
        }

        public void setInitialCondition(double initialCondition)
        {
            sp.setInitialCondition( initialCondition );
        }

        @PropertyName ( "Decay rate" )
        public double getDecayRate()
        {
            return sp.getDecayRate();
        }

        public void setDecayRate(double decayRate)
        {
            sp.setDecayRate( decayRate );
        }

        @PropertyName ( "Diffusion coefficient" )
        public double getDiffusionCoefficient()
        {
            return sp.getDiffusionCoefficient();
        }

        public void setDiffusionCoefficient(double diffusionCoefficient)
        {
            sp.setDiffusionCoefficient( diffusionCoefficient );
        }

        @PropertyName ( "Dirichlet condition" )
        public boolean isDirichletCondition()
        {
            return sp.isDirichletCondition();
        }

        @PropertyName ( "X min" )
        public double getXMin()
        {
            return sp.getXMin();
        }
        public void setXMin(double xMin)
        {
            this.sp.setXMin( xMin );
        }

        @PropertyName ( "X max" )
        public double getXMax()
        {
            return sp.getXMax();
        }
        public void setXMax(double xMax)
        {
            this.sp.setXMax( xMax );
        }

        @PropertyName ( "Y min" )
        public double getYMin()
        {
            return sp.getYMin();
        }
        public void setYMin(double yMin)
        {
            this.sp.setYMin( yMin );
        }

        @PropertyName ( "Y max" )
        public double getYMax()
        {
            return sp.getYMax();
        }
        public void setYMax(double yMax)
        {
            this.sp.setYMax( yMax );
        }

        @PropertyName ( "Z min" )
        public double getZMin()
        {
            return sp.getZMin();
        }
        public void setZMin(double zMin)
        {
            this.sp.setZMin( zMin );
        }

        @PropertyName ( "Z max" )
        public double getZMax()
        {
            return sp.getZMax();
        }
        public void setZMax(double zMax)
        {
            this.sp.setZMax( zMax );
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
            super( SubstratePropertiesWrapper.class );
        }

        @Override
        public void initProperties() throws Exception
        {
            addReadOnly( "name", "isCompleted" );
            add( "initialCondition" );
            add( "decayRate" );
            add( "diffusionCoefficient" );
            add( "xMin" );
            add( "xMax" );
            add( "yMin" );
            add( "yMax" );
            add( "zMin" );
            add( "zMax" );
        }
    }

    public static class EventWrapper implements DataElement
    {
        private EventProperties ep;

        public EventWrapper(EventProperties ep)
        {
            this.ep = ep;
        }

        @PropertyName ( "Name" )
        public String getName()
        {
            return ep.getName();
        }
        public void setName(String name)
        {
            //           cdp.setName( name );;
        }

        @PropertyName ( "Execution time" )
        public double getExecutionTime()
        {
            return ep.getExecutionTime();
        }

        public void setExecutionTime(double time)
        {
            ep.setExecutionTime( time );
        }

        @PropertyName ( "Description" )
        public String getDescription()
        {
            return ep.getComment();
        }

        public void setDescription(String description)
        {
            ep.setComment( description );
        }

        @PropertyName ( "Custom code" )
        public DataElementPath getCustomCode()
        {
            return ep.getExecutionCodePath();
        }

        public void setCustomCode(DataElementPath path)
        {
            ep.setExecutionCodePath( path );
        }

        @Override
        public DataCollection<?> getOrigin()
        {
            // TODO Auto-generated method stub
            return null;
        }

    }

    public static class EventWrapperBeanInfo extends BeanInfoEx2<EventWrapper>
    {
        public EventWrapperBeanInfo()
        {
            super( EventWrapper.class );
        }

        @Override
        public void initProperties() throws Exception
        {
            addReadOnly( "name" );
            add( "executionTime" );
            add( "description" );
            add( "customCode" );
        }
    }

    public static class ColorSchemeWrapper implements DataElement
    {
        private ColorScheme cs;

        public ColorSchemeWrapper(ColorScheme cs)
        {
            this.cs = cs;
        }

        @PropertyName ( "Name" )
        public String getName()
        {
            return cs.getName();
        }
        public void setName(String name)
        {
            cs.setName( name );
        }

        @PropertyName ( "Has border" )
        public boolean isBorder()
        {
            return cs.isBorder();
        }
        public void setBorder(boolean border)
        {
            cs.setBorder( border );
        }

        @PropertyName ( "Has core" )
        public boolean isCore()
        {
            return cs.isCore();
        }
        public void setCore(boolean core)
        {
            cs.setCore( core );
        }

        @PropertyName ( "Color" )
        public Color getColor()
        {
            return cs.getColor();
        }

        public void setColor(Color color)
        {
            cs.setColor(color);
        }

        @PropertyName ( "Border Color" )
        public Color getBorderColor()
        {
            return cs.getBorderColor();
        }

        public void setBorderColor(Color color)
        {
            cs.setBorderColor(color);
        }

        @PropertyName ( "Core Color" )
        public Color getCoreColor()
        {
            return cs.getCoreColor();
        }

        public void setCoreColor(Color color)
        {
            cs.setCoreColor(color);
        }

        @PropertyName ( "Core Border Color" )
        public Color getCoreBorderColor()
        {
            return cs.getCoreBorderColor();
        }

        public void setCoreBorderColor(Color color)
        {
            cs.setCoreBorderColor(color);
        }

        @Override
        public DataCollection<?> getOrigin()
        {
            // TODO Auto-generated method stub
            return null;
        }

    }

    public static class ColorSchemeWrapperBeanInfo extends BeanInfoEx2<ColorSchemeWrapper>
    {
        public ColorSchemeWrapperBeanInfo()
        {
            super( ColorSchemeWrapper.class );
        }

        @Override
        public void initProperties() throws Exception
        {
            add( "name" );
            add("color", ColorEditor.class);
            add( "border");
            add("borderColor", ColorEditor.class);
            add( "core" );
            add("coreColor", ColorEditor.class);
            add("coreBorderColor", ColorEditor.class);
            //            property( "borderColor" ).readOnly( "noBorder" ).add();
            //            add( "core" );
            //            property( "coreColor" ).readOnly( "noCore" ).add();
            //            property( "coreBorderColor" ).readOnly( "noCore" ).add();
        }
    }

    public static class VisualizerPropertiesWrapper implements DataElement
    {
        private CellDefinitionVisualizerProperties properties;
        private String name;
        
        public VisualizerPropertiesWrapper(String name, CellDefinitionVisualizerProperties properties)
        {
            this.properties = properties;
            this.name = name;
        }

        @PropertyName ( "Cell Type" )
        public String getCellType()
        {
            return properties.getCellType();
        }
        public void setCellType(String cellType)
        {
            this.properties.setCellType( cellType );
        }

        @PropertyName ( "Color type" )
        public String getType()
        {
            return properties.getType();
        }
        public void setType(String type)
        {
            properties.setType( type );
        }

        @PropertyName ( "Signal" )
        public String getSignal()
        {
            return properties.getSignal();
        }
        public void setSignal(String signal)
        {
            properties.setSignal( signal );
        }

        @PropertyName ( "Min value" )
        public double getMin()
        {
            return properties.getMin();
        }
        public void setMin(double min)
        {
            properties.setMin( min );
        }

        @PropertyName ( "Max value" )
        public double getMax()
        {
            return properties.getMax();
        }
        public void setMax(double max)
        {
            this.properties.setMax( max );
        }

        @PropertyName ( "Color 1" )
        public String getColor1()
        {
            return properties.getColor1();
        }
        public void setColor1(String color1)
        {
            this.properties.setColor1( color1 );
        }

        @PropertyName ( "Color 2" )
        public String getColor2()
        {
            return properties.getColor2();
        }
        public void setColor2(String color2)
        {
            this.properties.setColor2( color2 );
        }

        @PropertyName ( "Priority" )
        public double getPriority()
        {
            return properties.getPriority();
        }
        public void setPriority(double priority)
        {
            this.properties.setPriority( priority );
        }

        public boolean isOneColor()
        {
            return properties.isOneColor();
        }

        public StreamEx<String> getCellTypes()
        {
            return properties.getCellTypes();
        }

        public StreamEx<String> getSignals()
        {
            return properties.getSignals();
        }

        public StreamEx<String> getTypes()
        {
            return properties.getTypes();
        }

        public StreamEx<String> getColorSchemes()
        {
            return properties.getColorSchemes();
        }

        @Override
        public String getName()
        {
            return name;
        }

        @Override
        public DataCollection<?> getOrigin()
        {
            // TODO Auto-generated method stub
            return null;
        }
    }

    public static class VisualizerPropertiesWrapperBeanInfo extends BeanInfoEx2<VisualizerPropertiesWrapper>
    {
        public VisualizerPropertiesWrapperBeanInfo()
        {
            super( VisualizerPropertiesWrapper.class );
        }

        @Override
        public void initProperties()
        {
            property( "cellType" ).tags( bean -> bean.getCellTypes() ).add();
            add( "priority" );
            property( "type" ).tags( bean -> bean.getTypes() ).add();
            property( "signal" ).tags( bean -> bean.getSignals() ).add();
            property( "color1" ).tags( bean -> bean.getColorSchemes() ).add();
            property( "color2" ).tags( bean -> bean.getColorSchemes() ).add();
            add( "min" );
            add( "max" );
        }

    }
}
