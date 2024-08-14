package biouml.plugins.cytoscape;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

public class CytoscapeConstants
{
    public static final String INTERACTION_ATTRIBUTE = "cx:interaction";
    public static final String BIOPAX_TYPE = "BIOPAX_TYPE";

    //verification constants
    public static final String NUMBER_VERIFICATION = "numberVerification";
    public static final String LONG_NUMBER_KEY = "longNumber";
    public static final long LONG_NUMBER_VALUE = 281474976710655L;

    //supported aspects
    public static final String NETWORK_ATTRIBUTES = "networkAttributes";
    public static final String NODES = "nodes";
    public static final String EDGES = "edges";
    public static final String NODE_ATTRIBUTES = "nodeAttributes";
    public static final String EDGE_ATTRIBUTES = "edgeAttributes";
    public static final String CARTESIAN_LAYOUT = "cartesianLayout";

    //CX elements properties
    public static final String ELEMENT_ID_KEY = "@id";
    public static final String NODE_REPRESENTS_KEY = "r";
    public static final String EDGE_SOURCE_KEY = "s";
    public static final String EDGE_TARGET_KEY = "t";
    public static final String EDGE_TYPE_KEY = "i";

    //name key constant belongs to both CX element & attribute
    public static final String NAME_KEY = "n";

    //CX attributes properties
    public static final String ATTRIBUTE_ELEMENT_ID_KEY = "po";
    public static final String ATTRIBUTE_VALUE_KEY = "v";
    public static final String ATTRIBUTE_DATATYPE_KEY = "d";

    //Layout properties
    public static final String LAYOUT_NODE_KEY = "node";
    public static final String LAYOUT_X_KEY = "x";
    public static final String LAYOUT_Y_KEY = "y";
    public static final String LAYOUT_VIEW_KEY = "view";

    //mapper for CX types to java classes
    @SuppressWarnings ( "serial" )
    private static final Map<String, Class<?>> CX_TYPE_MAPPER = new HashMap<String, Class<?>>()
    {
        {
            put( "boolean", Boolean.class );
            put( "byte", Byte.class );
            put( "char", Character.class );
            put( "double", Double.class );
            put( "float", Float.class );
            put( "integer", Integer.class );
            put( "long", Long.class );
            put( "short", Short.class );
            put( "string", String.class );
            put( "list_of_boolean", Boolean[].class );
            put( "list_of_byte", Byte[].class );
            put( "list_of_char", Character[].class );
            put( "list_of_double", Double[].class );
            put( "list_of_float", Float[].class );
            put( "list_of_integer", Integer[].class );
            put( "list_of_long", Long[].class );
            put( "list_of_short", Short[].class );
            put( "list_of_string", String[].class );
        }
    };

    public static Class<?> getClassForCXType(String typeStr)
    {
        return CX_TYPE_MAPPER.getOrDefault( typeStr, String.class );
    }

    //mapper for java classes to CX types
    @SuppressWarnings ( "serial" )
    private static final Map<Class<?>, String> CLASS_CX_MAPPER = new HashMap<Class<?>, String>()
    {
        {
            put( Boolean.class, "boolean" );
            put( Byte.class, "byte" );
            put( Character.class, "char" );
            put( Double.class, "double" );
            put( Float.class, "float" );
            put( Integer.class, "integer" );
            put( Long.class, "long" );
            put( Short.class, "short" );
            put( String.class, "string" );
            put( Boolean[].class, "list_of_boolean" );
            put( Byte[].class, "list_of_byte" );
            put( Character[].class, "list_of_char" );
            put( Double[].class, "list_of_double" );
            put( Float[].class, "list_of_float" );
            put( Integer[].class, "list_of_integer" );
            put( Long[].class, "list_of_long" );
            put( Short[].class, "list_of_short" );
            put( String[].class, "list_of_string" );
        }
    };

    public static String getCXTypeForClass(Class<?> clazz)
    {
        return CLASS_CX_MAPPER.getOrDefault( clazz, null );
    }

    public static final @Nonnull String CX_EDGE_PRODUCT = "right";
    public static final @Nonnull String CX_EDGE_REACTANT = "left";
    public static final @Nonnull String CX_EDGE_MODIFIER_INHIBITION = "INHIBITION";
    public static final @Nonnull String CX_EDGE_MODIFIER_ACTIVATION = "ACTIVATION";
    private static final Set<String> CX_EDGE_TYPES = Stream
            .of( CX_EDGE_PRODUCT, CX_EDGE_REACTANT, CX_EDGE_MODIFIER_INHIBITION, CX_EDGE_MODIFIER_ACTIVATION )
            .collect( Collectors.toSet() );
    public static boolean isCXReactionEdge(String typeStr)
    {
        return typeStr != null && CX_EDGE_TYPES.contains( typeStr );
    }
}
