package biouml.plugins.sbgn;

/**
 * XML tag names and attributes constants for SBGN
 */
public class SBGNXmlConstants
{
    public static final String SBGN_ELEMENT = "sbgn";
    public static final String NOTATION_REF = "notation";

    public static final String NODES_ELEMENT = "nodes";
    public static final String EDGES_ELEMENT = "edges";
    public static final String STATES_ELEMENT = "states";
    public static final String STYLES_ELEMENT = "styles";

    public static final String VARCOLUMNS_ELEMENT = "variableColumns";
    public static final String VARCOLUMN_ELEMENT = "variableColumn";
    public static final String ARGCOLUMN_ELEMENT = "argumentColumn";    
    
    public static final String NODE_ELEMENT = "node";
    public static final String NODE_LAYOUT_ELEMENT = "nodeLayout";
    public static final String NODE_PAINT_ELEMENT = "nodePaint";
    public static final String NODE_TITLE_ELEMENT = "nodeTitle";
    public static final String EDGE_ELEMENT = "edge";
    public static final String PATH_ELEMENT = "path";
    public static final String SEGMENT_ELEMENT = "segment";
    public static final String EDGE_PAINT_ELEMENT = "edgePaint";

    public static final String ID_ATTR = "id";
    public static final String TITLE_ATTR = "title";
    public static final String SHOW_TITLE_ATTR = "showTitle";
    public static final String TYPE_ATTR = "type";
    public static final String PARENT_ATTR = "parent";
    public static final String REF_ATTR = "ref";
    public static final String CLONE_ATTR = "clone";
    public static final String MULTIMER_ATTR = "multimer";
    public static final String FROM_ATTR = "from";
    public static final String TO_ATTR = "to";
    public static final String MAIN_VAR_ATTR = "mainVar";
    public static final String CONVERSION_FACTOR_ATTR = "factor";
    public static final String FUNCTION_ATTR = "function";
    public static final String KERNEL_TYPE_ATTR = "kernelType";
    public static final String REACTION_TYPE_ATTR = "reactionType";
    public static final String EDGE_TYPE_ATTR = "edgeType";
    public static final String VALUE_ATTR = "value"; //to save variable value
    public static final String PORT_TYPE_ATTR = "portType";
    public static final String VISIBLE_ATTR = "visible";
    public static final String FIXED_ATTR = "fixed";
    public static final String OPERATOR_TYPE_ATTR = "operatorType";
    public static final String FIXED_IN_OUT_ATTR = "fixedInOut";
    
    public static final String X_ATTR = "x";
    public static final String Y_ATTR = "y";
    public static final String WIDTH_ATTR = "width";
    public static final String HEIGHT_ATTR = "height";
    
    public static final String COLOR_ATTR = "color";

    public static final String SEGMENT_TYPE_ATTR = "segmentType";
    public static final String SEGMENT_X_ATTR = "x0";
    public static final String SEGMENT_Y_ATTR = "y0";

    public static final String LINE_LINETO = "lineTo";
    public static final String LINE_QUADRIC = "quadTo";
    public static final String LINE_CUBIC = "cubicTo";
    
    public static final String PROPERTY_ELEMENT = "property";
    public static final String PROPERTY_NAME_ATTR = "name";
    public static final String PROPERTY_TYPE_ATTR = "type";
    public static final String PROPERTY_VALUE_ATTR = "value";
    
    public static final String TEXT_ATTR = "text"; //to save special edge titles
    
    public static final String BIOUML_XMLNS_ATTR                = "xmlns:biouml";
    public static final String BIOUML_XMLNS_VALUE               = "http://www.biouml.org/ns";
    public static final String VERTICAL = "vertical";
    public static final String HORIZONTAL = "horizontal";
}
