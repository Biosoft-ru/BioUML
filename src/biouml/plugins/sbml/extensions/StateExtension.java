package biouml.plugins.sbml.extensions;

import javax.annotation.Nonnull;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.util.DiagramXmlReader;
import biouml.model.util.DiagramXmlWriter;
import biouml.standard.state.StateXmlSerializer;

public class StateExtension extends SbmlExtensionSupport
{
    @Override
    public void readElement(Element element, DiagramElement specie, @Nonnull Diagram diagram)
    {
        DiagramXmlReader reader = new DiagramXmlReader(diagram.getName());
        reader.setDiagram( diagram );
        diagram.addState( StateXmlSerializer.readXmlElement(element, diagram, reader));
    }

    @Override
    public Element[] writeElement(DiagramElement specie, Document document)
    {
        //states are BiouML specific
        if (!modelWriter.isWriteBioUMLAnnotation())
        {
            return null;
        }
        
        if( ! ( specie instanceof Diagram ) )
        {
            return null;
        }

        Diagram diagram = (Diagram)specie;
        Element[] elements = diagram.states()
                .map( state -> StateXmlSerializer.getStateXmlElement( state, document, new DiagramXmlWriter( document, diagram ) ) )
                .toArray( Element[]::new );
        return elements.length == 0 ? null : elements;
    }
}
