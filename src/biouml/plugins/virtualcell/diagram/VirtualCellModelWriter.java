package biouml.plugins.virtualcell.diagram;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import biouml.model.Diagram;
import biouml.model.Role;
import biouml.model.util.ModelXmlWriter;

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
        
        for( TranslationProperties processProperties : diagram.recursiveStream().map( de -> de.getRole() ).select( TranslationProperties.class ) )
        {
            Element child = doc.createElement( "translation" );
            child.setAttribute( "name", processProperties.getName() );
            if( processProperties.getTranslationRates() != null )
                child.setAttribute( "translation_rates", processProperties.getTranslationRates().toString() );
            element.appendChild( child );
        }
        
        for( ProteinDegradationProperties processProperties : diagram.recursiveStream().map( de -> de.getRole() ).select( ProteinDegradationProperties.class ) )
        {
            Element child = doc.createElement( "protein_degradation" );
            child.setAttribute( "name", processProperties.getName() );
            if( processProperties.getDegradationRates() != null )
                child.setAttribute( "degradation_rates", processProperties.getDegradationRates().toString() );
            element.appendChild( child );
        }
        
        for( PopulationProperties processProperties : diagram.recursiveStream().map( de -> de.getRole() ).select( PopulationProperties.class ) )
        {
            Element child = doc.createElement( "population" );
            child.setAttribute( "name", processProperties.getName() );
            if( processProperties.getCoeffs() != null )
                child.setAttribute( "coefficients", processProperties.getCoeffs().toString() );
            element.appendChild( child );
        }
        
        if( element.hasChildNodes() )
            parent.appendChild( element );
    }

    private void createPools(Diagram diagram, Element parent)
    {
        Element element = doc.createElement( "pools" );
        for( TableCollectionPoolProperties properties : diagram.recursiveStream().map( de -> de.getRole() )
                .select( TableCollectionPoolProperties.class ) )
        {
            Element child = doc.createElement( "pool" );
            child.setAttribute( "name", properties.getName() );
            if( properties.getPath() != null )
                child.setAttribute( "path", properties.getPath().toString() );
            element.appendChild( child );
            if (properties.isShouldBeSaved())
            {
                child.setAttribute( "should_be_saved", "true" );
                child.setAttribute( "save_step", String.valueOf(properties.getSaveStep()) );
            }
        }
        if( element.hasChildNodes() )
            parent.appendChild( element );
    }
}