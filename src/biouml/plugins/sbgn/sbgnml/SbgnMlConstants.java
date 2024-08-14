package biouml.plugins.sbgn.sbgnml;

import java.util.HashSet;
import java.util.Set;

public class SbgnMlConstants
{
    public static final String SBGN_ML_NAMESPACE = "http://sbgn.org/libsbgn/0.2";
    public static final String XMLNS_ATTR = "xmlns";
    public static final String SBGN_ELEMENT = "sbgn";

    public static final String MAP_ELEMENT = "map";
    public static final String LANGUAGE_ATTR = "language";
    public static final String PROCESS_DESCRIPTION = "process description";

    public static final String ID_ATTR = "id";
    public static final String CLASS_ATTR = "class";

    public static final String GLYPH_ELEMENT = "glyph";
    public static final String COMPARTMENT_REF_ATTR = "compartmentRef";
    public static final String COMPARTMENT_CLASS = "compartment";
    public static final String LABEL_ATTR = "label";
    public static final String TEXT_ATTR = "text";
    public static final String CLONE_ATTR = "clone";
    public static final String ORIENTATION_ATTR = "orientation";

    public static final String VARIABLE_CLASS = "state variable";
    public static final String STATE_ATTR = "state";
    public static final String VARIABLE_ATTR = "variable";
    public static final String VALUE_ATTR = "value";

    public static final String TERMINAL_CLASS = "terminal";
    public static final String SUBMAP_CLASS = "submap";

    public static final String TAG_CLASS = "tag";
    public static final String SOURCE_SINK_CLASS = "source and sink";

    public static final String ARC_ELEMENT = "arc";
    public static final String TARGET_ATTR = "target";
    public static final String SOURCE_ATTR = "source";
    public static final String START_ATTR = "start";
    public static final String NEXT_ATTR = "next";
    public static final String END_ATTR = "end";
    public static final String POINT_ATTR = "point";

    public static final String BBOX_ATTR = "bbox";
    public static final String X_ATTR = "x";
    public static final String Y_ATTR = "y";
    public static final String HEIGHT_ATTR = "h";
    public static final String WIDTH_ATTR = "w";
    public static final String PORT_ELEMENT = "port";

    public static final Set<String> LOGICAL_CLASSES = new HashSet<String>()
    {
        {
            add("and");
            add("or");
            add("not");
        }
    };
}
