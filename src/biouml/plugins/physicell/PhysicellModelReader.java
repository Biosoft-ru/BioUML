package biouml.plugins.physicell;

import org.w3c.dom.Element;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.util.ModelXmlReader;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.util.XmlUtil;

public class PhysicellModelReader extends ModelXmlReader
{
    public PhysicellModelReader(Diagram diagram)
    {
        super( diagram );
    }

    @Override
    public MulticellEModel readModel(Element element)
    {
        MulticellEModel model = null;
        try
        {
            String className = element.getAttribute( MODEL_CLASS_ATTR );
            Class<? extends MulticellEModel> clazz = ClassLoading.loadSubClass( className, MulticellEModel.class );
            model = clazz.getConstructor( DiagramElement.class ).newInstance( diagram );
            readDomain( element, model );
            readUserParameters( element, model );
            readInitialCondiiton( element, model );
            readReportProperties(element, model);
        }
        catch( Throwable t )
        {
            error( "ERROR_EXECUTABLE_MODEL", new String[] {diagram.getName(), t.getMessage()}, t );
        }
        if( model != null )
            model.setPropagationEnabled( true );
        return model;
    }

    protected void readReportProperties(Element modelElement, MulticellEModel model)
    {
        ReportProperties report = model.getReportProperties();
        Element reportElement = XmlUtil.findElementByTagName( modelElement, "report" );
        if( reportElement == null )
            return;
        String customReport = reportElement.getAttribute( "customReport" );
        if( !customReport.isEmpty() )
        {
            report.setCustomReport( true );
            report.setReportPath( DataElementPath.create( customReport ) );
        }
        String customVisualizer = reportElement.getAttribute( "customVisualizer" );
        if( !customVisualizer.isEmpty() )
        {
            report.setCustomVisualizer( true );
            report.setVisualizerPath( DataElementPath.create( customVisualizer ) );
        }
    }

    protected void readInitialCondiiton(Element modelElement, MulticellEModel model)
    {
        InitialCondition condition = model.getInitialCondition();
        Element conditionElement = XmlUtil.findElementByTagName( modelElement, "initialCondition" );
        if( conditionElement == null )
            return;
        condition.setCustomCondition( true );
        String customCode = conditionElement.getAttribute( "customCode" );
        condition.setCustomConditionCode( DataElementPath.create( customCode ) );
        String customTable = conditionElement.getAttribute( "customTable" );
        condition.setCustomConditionTable( DataElementPath.create( customTable ) );
    }

    protected void readDomain(Element modelElement, MulticellEModel model)
    {
        DomainOptions domain = model.getDomain();
        Element domainElement = XmlUtil.findElementByTagName( modelElement, "domain" );
        domain.setXFrom( getDouble( domainElement, "xFrom", -100 ) );
        domain.setXTo( getDouble( domainElement, "xTo", 100 ) );
        domain.setXStep( getDouble( domainElement, "xStep", 20 ) );
        domain.setYFrom( getDouble( domainElement, "yFrom", -100 ) );
        domain.setYTo( getDouble( domainElement, "yTo", 100 ) );
        domain.setYStep( getDouble( domainElement, "yStep", 20 ) );
        domain.setZFrom( getDouble( domainElement, "zFrom", -10 ) );
        domain.setZTo( getDouble( domainElement, "zTo", 10 ) );
        domain.setZStep( getDouble( domainElement, "zStep", 20 ) );
        domain.setUse2D( getBoolean( domainElement, "use2D", true ) );
    }

    protected void readUserParameters(Element modelElement, MulticellEModel model)
    {
        Element parametersElement = XmlUtil.findElementByTagName( modelElement, "userParameters" );
        if( parametersElement == null )
            return;
        for( Element parameterElement : XmlUtil.elements( parametersElement, "userParameter" ) )
        {
            UserParameter p = new UserParameter();
            p.setName( parameterElement.getAttribute( "name" ) );
            p.setUnits( parameterElement.getAttribute( "units" ) );
            p.setType( parameterElement.getAttribute( "type" ) );
            p.setValue( parameterElement.getAttribute( "value" ) );
            model.addUserParameter( p );
        }
    }

    private double getDouble(Element el, String attr, double defaultValue)
    {
        try
        {
            return Double.parseDouble( el.getAttribute( attr ) );
        }
        catch( Exception ex )
        {
            return defaultValue;
        }
    }

    private boolean getBoolean(Element el, String attr, boolean defaultValue)
    {
        try
        {
            return Boolean.parseBoolean( el.getAttribute( attr ) );
        }
        catch( Exception ex )
        {
            return defaultValue;
        }
    }
}