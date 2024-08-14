package biouml.plugins.cytoscape.cx;

import org.json.JSONException;
import org.json.JSONObject;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

import biouml.plugins.cytoscape.CytoscapeConstants;
import ru.biosoft.util.TextUtil;

public abstract class CXElement
{
    static final String NODE_TYPE = "node";
    static final String EDGE_TYPE = "edge";
    public static final String ATTRIBUTE_TYPE = "attribute";
    public static final String LAYOUT_TYPE = "layout element";

    private final long id;
    private final DynamicPropertySet attributes = new DynamicPropertySetAsMap();
    protected CXElement(long id)
    {
        this.id = id;
    }

    public long getID()
    {
        return id;
    }

    public DynamicPropertySet getAttributes()
    {
        return attributes;
    }
    public void addAttribute(DynamicProperty dp)
    {
        attributes.add( dp );
    }

    public String getBioPAXType()
    {
        return getAttributes().getValueAsString( CytoscapeConstants.BIOPAX_TYPE );
    }

    public static DynamicProperty readAttributeFromJSON(JSONObject obj)
    {
        String attrName = getStringMandatoryValue( CytoscapeConstants.NAME_KEY, obj, ATTRIBUTE_TYPE );
        String value = obj.optString( CytoscapeConstants.ATTRIBUTE_VALUE_KEY, null );
        if( value == null )
            throw new IllegalArgumentException( getErrorMsg( ATTRIBUTE_TYPE, CytoscapeConstants.ATTRIBUTE_VALUE_KEY ) );
        String typeStr = obj.optString( CytoscapeConstants.ATTRIBUTE_DATATYPE_KEY, "" );
        Class<?> type = CytoscapeConstants.getClassForCXType( typeStr );
        Object processedValue = TextUtil.fromString( type, obj.get( CytoscapeConstants.ATTRIBUTE_VALUE_KEY ).toString() );
        return new DynamicProperty( attrName, type, processedValue );
    }

    private static String getErrorMsg(String object, String key)
    {
        return "Inconsistent " + object + ": mandatory property '" + key + "' is missed or incorrect.";
    }

    public static double getDoubleMandatoryValue(String key, JSONObject obj, String objectType) throws IllegalArgumentException
    {
        try
        {
            return obj.getDouble( key );
        }
        catch( JSONException e )
        {
            throw new IllegalArgumentException( getErrorMsg( objectType, key ), e );
        }
    }

    public static long getLongMandatoryValue(String key, JSONObject obj, String objectType) throws IllegalArgumentException
    {
        try
        {
            return obj.getLong( key );
        }
        catch( JSONException e )
        {
            throw new IllegalArgumentException( getErrorMsg( objectType, key ), e );
        }
    }

    public static String getStringMandatoryValue(String key, JSONObject obj, String objectType) throws IllegalArgumentException
    {
        try
        {
            return obj.getString( key );
        }
        catch( JSONException e )
        {
            throw new IllegalArgumentException( getErrorMsg( objectType, key ), e );
        }
    }
}
