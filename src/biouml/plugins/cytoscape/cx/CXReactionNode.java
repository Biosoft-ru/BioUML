package biouml.plugins.cytoscape.cx;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.beans.DynamicProperty;

public class CXReactionNode extends CXNode
{
    private List<CXEdge> cxEdges = new ArrayList<>();

    public CXReactionNode(CXNode template)
    {
        super( template.getID(), template.getName(), template.getRepresents() );
        Point location = template.getLocation();
        setLocation( location.x, location.y );
        for( DynamicProperty dp : template.getAttributes() )
            addAttribute( dp );
    }

    public List<CXEdge> getCXEdges()
    {
        return cxEdges;
    }

    public void addEdge(CXEdge cxEdge)
    {
        cxEdges.add( cxEdge );
    }
}