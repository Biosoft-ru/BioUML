package biouml.plugins.virtualcell.diagram;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import biouml.model.Diagram;
import biouml.model.Role;
import biouml.model.util.ModelXmlWriter;
import ru.biosoft.util.ColorUtils;

public class VirtualCellModelWriter extends ModelXmlWriter
{

    @Override
    public Element createModel(Diagram diagram, Document document)
    {
        doc = document;
        this.diagram = diagram;

        if( diagram == null )
            return null;

        Role role = diagram.getRole();

        Element element = doc.createElement( EXECUTABLE_MODEL_ELEMENT );
        element.setAttribute( MODEL_CLASS_ATTR, role.getClass().getName() );

        createProcesses( diagram, element );
        createPools( diagram, element );
//        createConnections( diagram, element );
        return element;
    }

    private void createProcesses(Diagram diagram, Element parent)
    {
        Element element = doc.createElement( "processes" );
        for( ProcessProperties processProperties : diagram.recursiveStream().map( de -> de.getRole() ).select( ProcessProperties.class ) )
        {
            Element child = doc.createElement( "process" );
            child.setAttribute( "name", processProperties.getName() );
            if( processProperties.getDiagramPath() != null )
                child.setAttribute( "path", processProperties.getDiagramPath().toString() );
            element.appendChild( child );
        }
        if( element.hasChildNodes() )
            parent.appendChild( element );
    }

    private void createPools(Diagram diagram, Element parent)
    {
        Element element = doc.createElement( "pools" );
        for( TableCollectionDataSetProperties properties : diagram.recursiveStream().map( de -> de.getRole() )
                .select( TableCollectionDataSetProperties.class ) )
        {
            Element child = doc.createElement( "pool" );
            child.setAttribute( "name", properties.getName() );
            if( properties.getPath() != null )
                child.setAttribute( "path", properties.getPath().toString() );
            element.appendChild( child );
        }
        if( element.hasChildNodes() )
            parent.appendChild( element );
    }

//    private void createConnections(Diagram diagram, Element parent)
//    {
//        if( report.isDefaultReport() && report.isDefaultVisualizer() )
//            return;
//        Element element = doc.createElement( "report" );
//        if( report.isCustomReport() && report.getReportPath() != null )
//            element.setAttribute( "customReport", report.getReportPath().toString() );
//        if( report.isCustomVisualizer() && report.getVisualizerPath() != null )
//            element.setAttribute( "customVisualizer", report.getVisualizerPath().toString() );
//        parent.appendChild( element );
//    }
}