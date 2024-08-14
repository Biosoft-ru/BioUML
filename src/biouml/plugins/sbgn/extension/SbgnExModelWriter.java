package biouml.plugins.sbgn.extension;

import java.util.logging.Level;

import org.w3c.dom.Element;

import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbgn.SbgnModelWriter;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.Type;

public class SbgnExModelWriter extends SbgnModelWriter
{
    public SbgnExModelWriter(Diagram diagram, Diagram baseDiagram)
    {
        super( diagram, baseDiagram );
        rootElementTag = SbgnExExtension.SBGN_EX_ELEMENT;
    }

    @Override
    protected boolean hasCorrectDiagramType()
    {
        if( SbgnExDiagramType.class.equals( diagram.getType().getClass() ) )
            return true;

        log.log( Level.SEVERE, "Incorrect diagram type, should be SbgnExDiagramType" );
        return false;
    }

    @Override
    public void writeEdge(Element parent, Edge edge)
    {
        if( edge.getKernel() instanceof SemanticRelation || Type.TYPE_SEMANTIC_RELATION.equals( edge.getKernel().getType() ) )
        {
            Element edgeElement = doc.createElement( EDGE_ELEMENT );

            edgeElement.setAttribute( ID_ATTR, edge.getName() );
            edgeElement.setAttribute( TYPE_ATTR, edge.getKernel().getType() );
            edgeElement.setAttribute( FROM_ATTR, edge.getInput().getCompleteNameInDiagram() );
            edgeElement.setAttribute( TO_ATTR, edge.getOutput().getCompleteNameInDiagram() );
            edgeElement.setAttribute( TITLE_ATTR, edge.getTitle() );
            edgeElement.setAttribute( REF_ATTR, edge.getName() );

            initAttribute( edge, edgeElement, SBGNPropertyConstants.SBGN_EDGE_TYPE, EDGE_TYPE_ATTR );
            initAttribute( edge, edgeElement, TEXT_ATTR );

            writePath( edgeElement, edge );
            writeEdgePaint( edgeElement, edge );

            parent.appendChild( edgeElement );
        }
        else
            super.writeEdge( parent, edge );
    }

}
