package biouml.standard.diagram;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

import biouml.model.DiagramElement;
import biouml.model.Node;
import biouml.model.Role;

/**
 * Auxiliary diagram elements allowing to establishing connections between ports at the distance, decreasing numbers of edges intersections
 * Each bus role may correspond to several nodes (all of them have the same role).
 * For visual distinguishing between different clusters of nodes - each bus define its own color for node
 * @author axec
 *
 */
public class Bus implements Role
{
    private Set<Node> nodes = new HashSet<>();
    private String name;
    private boolean directed = false;
    private Color color = Color.RED;

    public Bus(String name, boolean directed)
    {
        this.name = name;
        this.directed = directed;
    }

    public String getName()
    {
        return name;
    }

    public Color getColor()
    {
        return color;
    }

    public void setColor(Color color)
    {
        this.color = color;
    }

    public boolean isDirected()
    {
        return directed;
    }

    public void addNode(Node n)
    {
        nodes.add( n );
    }

    @Override
    public DiagramElement getDiagramElement()
    {
        return nodes.isEmpty() ? null : nodes.iterator().next();
    }

    public Set<Node> getNodes()
    {
        return nodes;
    }

    @Override
    public Role clone(DiagramElement de)
    {
        Bus result = new Bus(de.getName(), directed);
        result.setColor( new Color(color.getRed(), color.getBlue(), color.getGreen(), color.getAlpha()) );
        return result;
    }
}
