package biouml.plugins.cytoscape.cx;

import org.json.JSONObject;

import biouml.plugins.cytoscape.CytoscapeConstants;

public class CXEdge extends CXElement
{
    private final long source;
    private final long target;
    private final String interaction;
    private CXEdge(long id, long source, long target, String interaction)
    {
        super( id );
        this.source = source;
        this.target = target;
        this.interaction = interaction;
    }

    public long getSource()
    {
        return source;
    }
    public long getTarget()
    {
        return target;
    }
    public String getInteraction()
    {
        return interaction;
    }

    public static CXEdge fromJSON(JSONObject edgeObject) throws IllegalArgumentException
    {
        long id = getLongMandatoryValue( CytoscapeConstants.ELEMENT_ID_KEY, edgeObject, EDGE_TYPE );
        long source = getLongMandatoryValue( CytoscapeConstants.EDGE_SOURCE_KEY, edgeObject, EDGE_TYPE );
        long target = getLongMandatoryValue( CytoscapeConstants.EDGE_TARGET_KEY, edgeObject, EDGE_TYPE );
        String interaction = edgeObject.optString( CytoscapeConstants.EDGE_TYPE_KEY, "" );
        return new CXEdge( id, source, target, interaction );
    }

    @Override
    public String getBioPAXType()
    {
        String biopaxType = getAttributes().getValueAsString( CytoscapeConstants.BIOPAX_TYPE );
        if( biopaxType == null )
            biopaxType = interaction;
        return biopaxType;
    }
}