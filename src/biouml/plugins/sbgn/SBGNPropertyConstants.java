package biouml.plugins.sbgn;

import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.developmentontheedge.beans.editors.StringTagEditor;
import com.developmentontheedge.beans.editors.StringTagEditorSupport;

import biouml.standard.diagram.PortProperties;
import biouml.standard.type.Stub.ConnectionPort;
import ru.biosoft.util.bean.StaticDescriptor;

/**
 * Names and property descriptors for SBGN properties
 * @author lan
 */
public class SBGNPropertyConstants
{
    protected static final Set<String> entityTypes = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList("macromolecule", "unspecified", "simple chemical", "nucleic acid feature", "perturbing agent", "complex")));

    static final Set<String> mathTypesFull = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList(Type.TYPE_EVENT, Type.TYPE_EQUATION, Type.TYPE_FUNCTION, Type.TYPE_CONSTRAINT)));

    public static final String STYLE_ATTR = "Style";
    public static final String SUPER_TYPE = "Super type";

    public static final String SBGN_EDGE_TYPE = "sbgn:edgeType";
    public static final String SBGN_ENTITY_TYPE = "sbgn:entityType";
    public static final String SBGN_PROCESS_TYPE = "sbgn:processType";
    public static final String SBGN_MULTIMER = "sbgn:multimer";
    public static final String SBGN_TITLE = "sbgn:title";
    public static final String SBGN_CLONE_MARKER = "sbgn:cloneMarker";
    public static final String SBGN_LOGICAL_OPERATOR = "sbgn:logicalOperator";
    public static final String SBGN_REACTION_TYPE = "sbgn:reactionType";

    public static final String TYPE_ATTR = "compartmentType"; //type attribute (for compartments: SQUARE, OVAL)

    public static final PropertyDescriptor SBGN_LOGICAL_OPERATOR_PD = StaticDescriptor.create(SBGN_LOGICAL_OPERATOR,
            OperatorTypeEditor.class);
    public static final PropertyDescriptor SBGN_EDGE_TYPE_PD = StaticDescriptor.create(SBGN_EDGE_TYPE, EdgeTypeEditor.class);
    public static final PropertyDescriptor SBGN_ENTITY_TYPE_PD = StaticDescriptor.create(SBGN_ENTITY_TYPE, EntityTypeEditor.class);
    public static final PropertyDescriptor SBGN_ENTITY_COMPLEX_TYPE_PD = StaticDescriptor.create(SBGN_ENTITY_TYPE,
            EntityComplexTypeEditor.class);

    public static final PropertyDescriptor SBGN_PROCESS_TYPE_PD = StaticDescriptor.create(SBGN_PROCESS_TYPE);
    public static final PropertyDescriptor SBGN_MULTIMER_PD = StaticDescriptor.create(SBGN_MULTIMER);
    public static final PropertyDescriptor SBGN_CLONE_MARKER_PD = StaticDescriptor.create(SBGN_CLONE_MARKER);
    public static final PropertyDescriptor SBGN_REACTION_TYPE_PD = StaticDescriptor.create(SBGN_REACTION_TYPE,
            ReactionTypeTypeEditor.class);
    public static final PropertyDescriptor SBGN_COMPARTMENT_TYPE_PD = StaticDescriptor.create(TYPE_ATTR, CompartmentTypeEditor.class);

    public static final PropertyDescriptor SBGN_PORT_TYPE_PD = StaticDescriptor.create(ConnectionPort.PORT_TYPE, PortTypeEditor.class);
    public static final PropertyDescriptor SBGN_ACCESS_TYPE_PD = StaticDescriptor.create(ConnectionPort.ACCESS_TYPE,
            AccessTypeEditor.class);

    public static final String NAME_POINT_ATTR = "NamePoint";
    public static final String LINE_PEN_ATTR = "linePen";
    public static final String BRUSH_ATTR = "paint";
    public static final String LINE_IN_PEN_ATTR = "linePenIn";
    public static final String CLOSEUP_ATTR = "closeup"; //compartment type closeup info attribute
    public static final String TITLE_FONT_ATTR = "titleFont";

    public static final String LINE_OUT_PEN_ATTR = "linePenOut";
    public static final String SBGN_ATTRIBUTE_NAME = "sbgn_diagram";
    public static final String ORIENTATION = "orientation";

    public static final String META_ID = "metaid";

    public static final String BACKGROUND_VISIBLE_ATTR = "background_visible";
    public static final String BACKGROUND_COLOR_ATTR = "background_color";

    public static class EdgeTypeEditor extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return new String[] {"modulation", "stimulation", "catalysis", "inhibition", "necessary stimulation"};
        }
    }

    public static class OperatorTypeEditor extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return LogicalOperatorProperties.operatorTypes;
        }
    }

    public static class PortTypeEditor extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return PortProperties.getAvailablePortTypes();
        }
    }

    public static class AccessTypeEditor extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return PortProperties.getAvailableAccessTypes();
        }
    }

    public static class CompartmentTypeEditor extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return new String[] {"square", "oval"};
        }
    }

    public static class EntityTypeEditor extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return new String[] {"macromolecule", "simple chemical", "unspecified", "nucleic acid feature", "perturbing agent"};
        }
    }

    public static class ReactionTypeTypeEditor extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return new String[] {"process", "omitted process", "dissociation", "association", "uncertain process"};
        }
    }

    public static class EntityComplexTypeEditor extends StringTagEditorSupport
    {
        public EntityComplexTypeEditor()
        {
            super(entityTypes.toArray(new String[entityTypes.size()]));
        }
    }

}
