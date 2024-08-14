package biouml.plugins.cytoscape.cx;

import java.awt.Point;

import org.json.JSONObject;

import biouml.plugins.cytoscape.CytoscapeConstants;

public class CXNode extends CXElement
{
    private final String name;
    private final String represents;
    private Point location = new Point( 0, 0 );
    protected CXNode(long id, String name, String represents)
    {
        super( id );
        this.name = name;
        this.represents = represents;
    }

    public String getName()
    {
        return name;
    }
    public String getRepresents()
    {
        return represents;
    }

    public Point getLocation()
    {
        return location;
    }
    public void setLocation(int x, int y)
    {
        this.location = new Point( x, y );
    }

    public static CXNode fromJSON(JSONObject nodeObject) throws IllegalArgumentException
    {
        long id = getLongMandatoryValue( CytoscapeConstants.ELEMENT_ID_KEY, nodeObject, NODE_TYPE );
        String name = nodeObject.optString( CytoscapeConstants.NAME_KEY, "" );
        String represents = nodeObject.optString( CytoscapeConstants.NODE_REPRESENTS_KEY, "" );
        return new CXNode( id, name, represents );
    }
}