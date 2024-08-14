package biouml.model.xml;

import java.beans.PropertyDescriptor;

import ru.biosoft.util.bean.StaticDescriptor;

public class XmlDiagramTypeConstants
{
    public static final String XML_TYPE = "xmlElementType";
    
    public static final String GRAPHIC_NOTATION_ELEMENT     = "graphic-notation";
    public static final String TITLE_ATTR                   = "title";
    public static final String VERSION_ATTR                 = "version";
    public static final String APPVERSION_ATTR              = "appVersion";
    public static final String PLUGINS_ATTR                 = "requiredPlugins";
    public static final String CLASS_ATTR                   = "class";
    
    public static final String PATH_LAYOUTER_ELEMENT        = "pathLayouter";
    public static final String PROPERTIES_ELEMENT           = "properties";
    public static final String PROPERTY_ELEMENT             = "property";
    public static final String NAME_ATTR                    = "name";
    public static final String TYPE_ATTR                    = "type";
    public static final String PROPERTIES_BEAN_ATTR         = "propertiesBean";
    public static final String KERNELTYPE_ATTR              = "kernelType";
    public static final String DESCRIPTION_ATTR             = "short-description";
    public static final String VALUE_ATTR                   = "value";
    public static final String IS_COMPARTMENT               = "isCompartment";
    public static final String IS_DEFAULT                   = "isDefault";
    public static final String NEED_LAYOUT                  = "needLayout";
    public static final String CREATE_BY_PROTOTYPE          = "createByPrototype";
    public static final String AUTO_LAYOUT                  = "autoLayout";

    public static final String TYPE_BOOLEAN                 = "boolean";
    public static final String TYPE_INT                     = "int";
    public static final String TYPE_DOUBLE                  = "double";
    public static final String TYPE_STRING                  = "String";
    public static final String TYPE_SIZE                    = "size";
    public static final String TYPE_PEN                     = "pen";
    public static final String TYPE_BRUSH                   = "brush";
    public static final String TYPE_ARRAY                   = "array";
    public static final String TYPE_COMPOSITE               = "composite";

    public static final String TAGS_ELEMENT                 = "tags";
    public static final String TAG_ELEMENT                  = "tag";

    public static final String NODE_TYPES_ELEMENT           = "nodes";
    public static final String NODE_TYPE_ELEMENT            = "node";
    public static final String ICON_ATTR                    = "icon";
    public static final String KERNEL_ATTR                  = "kernel";

    public static final String EDGE_TYPES_ELEMENT           = "edges";
    public static final String EDGE_TYPE_ELEMENT            = "edge";
    
    public static final String REACTION_TYPES_ELEMENT       = "reactions";
    public static final String REACTION_TYPE_ELEMENT        = "reaction";

    public static final String VIEW_OPTIONS_ELEMENT         = "viewOptions";

    public static final String VIEW_BUILDER_ELEMENT         = "viewBuilder";
    public static final String PROTOTYPE_ATTR               = "prototype";

    public static final String NODE_VIEW_ELEMENT            = "nodeView";
    public static final String EDGE_VIEW_ELEMENT            = "edgeView";

    public static final String SEMANTIC_CONTROLLER_ELEMENT  = "semanticController";
    public static final String CAN_ACCEPT_ELEMENT           = "canAccept";
    public static final String IS_RESIZABLE_ELEMENT         = "isResizable";
    public static final String MOVE_ELEMENT                 = "move";
    public static final String REMOVE_ELEMENT               = "remove";

    public static final String FUNCTION_ELEMENT             = "function";
    
    public static final String ICONS_ELEMENT                = "icons";
    public static final String ICON_ELEMENT                 = "icon";
    
    public static final String EXAMPLES_ELEMENT             = "examples";
    public static final String EXAMPLE_ELEMENT              = "example";
    
    public static final String KERNEL_ROLE_ATTR             = "kernelRole";
    
    public static final PropertyDescriptor KERNEL_ROLE_ATTR_PD = StaticDescriptor.create(KERNEL_ROLE_ATTR);
    public static final PropertyDescriptor XML_TYPE_PD = StaticDescriptor.createHidden(XML_TYPE);
}
