package biouml.plugins.sbgn.extension;

import java.util.logging.Level;
import org.w3c.dom.Element;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.util.XmlUtil;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramType;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbgn.SbgnModelReader;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.Type;

import com.developmentontheedge.beans.DynamicProperty;

public class SbgnExModelReader extends SbgnModelReader
{
    public SbgnExModelReader(DataCollection<?> origin, String name, Diagram baseDiagram)
    {
        super( origin, name, baseDiagram );
    }

    @Override
    protected boolean isValidRoot(Element root)
    {
        return root.getTagName().equals( SbgnExExtension.SBGN_EX_ELEMENT );
    }
    @Override
    protected DiagramType getDiagramType()
    {
        return new SbgnExDiagramType();
    }

    @Override
    public Edge readEdge(Element element, Diagram diagram) throws Exception
    {
        String type = element.getAttribute( TYPE_ATTR );
      
        if( type.equals(Type.TYPE_SEMANTIC_RELATION))
        {
            Edge newEdge = null;
            String id = element.getAttribute(ID_ATTR);
            String title = element.getAttribute(TITLE_ATTR);
            String from = element.getAttribute( FROM_ATTR );
            String to = element.getAttribute( TO_ATTR );
            if( id.isEmpty() || type.isEmpty() || from.isEmpty() || to.isEmpty() )
            {
                log.log(Level.SEVERE,  "Incorrect node attributes: id=\"" + id + "\" type=\"" + type + "\" from=\"" + from + "\" to=\"" + to + "\"" );
                return null;
            }
            Node input = diagram.findNode( from );
            Node output = diagram.findNode( to );
            if( input == null || output == null )
            {
                log.log(Level.SEVERE,  "Can not find input or output node: input=\"" + from + "\" output=\"" + to + "\"" );
                return null;
            }

            String ref = element.getAttribute( REF_ATTR );
            if( !ref.isEmpty() && baseDiagram != null )
            {
                DiagramElement de = baseDiagram.findDiagramElement( ref );
                if( de == null )
                    de = baseDiagram.findDiagramElement( ref.replaceAll( "_", "." ) ); //try oldStyle

                if( de == null && ref.contains( ":" ) )
                    de = baseDiagram.findDiagramElement( ref.replaceFirst( ": .+ to .+$", "" ) );

                if( de != null && de.getKernel() != null )
                {
                    newEdge = new Edge( id, de.getKernel(), input, output );
                    if( de.getRole() != null )
                        newEdge.setRole( de.getRole().clone( newEdge ) );
                    copyAttributes( de.getAttributes(), newEdge.getAttributes() ); 
                }
            }

            if( newEdge == null )
                newEdge = new Edge( id, new SemanticRelation( diagram, id ), input, output );

            String edgeType = element.getAttribute( EDGE_TYPE_ATTR );
            if( !edgeType.isEmpty() )
                newEdge.getAttributes()
                        .add( new DynamicProperty( SBGNPropertyConstants.SBGN_EDGE_TYPE_PD, String.class, edgeType ) );
            
            Element path = XmlUtil.findElementByTagName( element, PATH_ELEMENT );
            if( path != null )
                readEdgePath( path, newEdge );

            Element paint = XmlUtil.findElementByTagName( element, EDGE_PAINT_ELEMENT );
            if( paint != null )
                readEdgePaint( paint, newEdge, diagram );

            newEdge.setTitle( title );

            SbgnExSemanticController.setNeccessaryAttributes(newEdge);

            return newEdge;
        }
        else
        {
            return super.readEdge( element, diagram );
        }
    }
}
