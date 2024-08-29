package biouml.plugins.physicell;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import biouml.model.Diagram;
import biouml.model.Role;
import biouml.model.util.ModelXmlWriter;

public class PhysicellModelWriter extends ModelXmlWriter
{

    @Override
    public Element createModel(Diagram diagram, Document document)
    {
        doc = document;
        this.diagram = diagram;

        if( diagram == null )
            return null;

        Role role = diagram.getRole();
        if( ! ( role instanceof MulticellEModel ) )
        {
            role = new MulticellEModel( diagram );
            diagram.setRole( role );
        }
        MulticellEModel model = (MulticellEModel)role;

        Element element = doc.createElement( EXECUTABLE_MODEL_ELEMENT );
        element.setAttribute( MODEL_CLASS_ATTR, role.getClass().getName() );

        createOptions( model.getOptions(), element );
        createDomain( model.getDomain(), element );
        createUserParameterList( model.getUserParmeters(), element );
        createInitialCondition( model.getInitialCondition(), element );
        createReport(model.getReportProperties(), element);
        setComment( element, model.getComment() );
        return element;
    }

    private void createReport(ReportProperties report, Element parent)
    {
        if (report.isDefaultReport() && report.isDefaultVisualizer())
            return;
        Element element = doc.createElement( "report" );        
        if( report.isCustomReport() && report.getReportPath() != null )
            element.setAttribute( "customReport", report.getReportPath().toString() );
        if( report.isCustomVisualizer() && report.getVisualizerPath() != null )
            element.setAttribute( "customVisualizer", report.getVisualizerPath().toString() );
        parent.appendChild( element );
    }
    
    private void createInitialCondition(InitialCondition initialCondition, Element parent)
    {
        if( initialCondition.isDefaultCondition() )
            return;
        Element element = doc.createElement( "initialCondition" );
        if( initialCondition.getCustomConditionCode() != null )
            element.setAttribute( "customCode", initialCondition.getCustomConditionCode().toString() );
        if( initialCondition.getCustomConditionTable() != null )
            element.setAttribute( "customTable", initialCondition.getCustomConditionTable().toString() );
        parent.appendChild( element );
    }

    private void createUserParameterList(UserParameters parameters, Element parent)
    {
        Element element = doc.createElement( "userParameters" );
        for( UserParameter parameter : parameters.getParameters() )
        {
            Element parameterElement = doc.createElement( "userParameter" );
            parameterElement.setAttribute( "name", parameter.getName() );
            parameterElement.setAttribute( "value", parameter.getValue() );
            element.appendChild( parameterElement );
        }
        parent.appendChild( element );
    }


    private void createOptions(ModelOptions options, Element parent)
    {
        Element element = doc.createElement( "options" );
        element.setAttribute( "disableAutomatedAdhesion", String.valueOf( options.isDisableAutomatedAdhesions() ) );
        parent.appendChild( element );
    }
    
    private void createDomain(DomainOptions options, Element parent)
    {
        Element element = doc.createElement( "domain" );
        element.setAttribute( "xFrom", String.valueOf( options.getXFrom() ) );
        element.setAttribute( "xStep", String.valueOf( options.getXStep() ) );
        element.setAttribute( "xTo", String.valueOf( options.getXTo() ) );

        element.setAttribute( "yFrom", String.valueOf( options.getYFrom() ) );
        element.setAttribute( "yStep", String.valueOf( options.getYStep() ) );
        element.setAttribute( "yTo", String.valueOf( options.getYTo() ) );

        element.setAttribute( "zStep", String.valueOf( options.getZStep() ) );
        element.setAttribute( "zFrom", String.valueOf( options.getZFrom() ) );
        element.setAttribute( "zTo", String.valueOf( options.getZTo() ) );

        element.setAttribute( "use2D", String.valueOf( options.isUse2D() ) );
        parent.appendChild( element );
    }
}