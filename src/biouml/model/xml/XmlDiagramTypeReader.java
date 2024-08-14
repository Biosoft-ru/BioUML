package biouml.model.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.exception.LoggedClassNotFoundException;
import ru.biosoft.exception.LoggedClassCastException;
import ru.biosoft.graph.Layouter;
import ru.biosoft.util.XmlStream;
import ru.biosoft.util.XmlUtil;
import biouml.model.DiagramViewBuilder;
import biouml.model.SemanticController;
import biouml.model.util.DiagramXmlReader;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

/**
 */
public class XmlDiagramTypeReader extends XmlDiagramTypeSupport
{
    protected static final Logger log = Logger.getLogger(XmlDiagramTypeReader.class.getName());

    String version = XmlDiagramTypeWriter.VERSION;

    // //////////////////////////////////////////////////////////////////////////
    // Constructors and public methods
    //

    protected InputStream stream;
    protected String name;
    private final Map<String, String> iconName2Type = new HashMap<>();

    /**
     * This constructor is used to read diagram from stream and can be used to
     * read diagram from relational database (TEXT or BLOB).
     *
     * @param name diagramType name
     * @param stream stream that contains diagram XML
     */
    public XmlDiagramTypeReader(String name, InputStream stream)
    {
        this.name = name;
        this.stream = stream;
    }

    public XmlDiagramType read(DataCollection<?> origin) throws Exception
    {
        diagramType = null;

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = null;

        // TODO - replace StringBuffer
        StringBuffer buffer = new StringBuffer();
        byte[] b = new byte[65536];
        int length = stream.read(b);
        while( length > 0 )
        {
            buffer.append(new String(b, 0, length, StandardCharsets.UTF_8));
            length = stream.read(b);
        }
        String xml = buffer.toString();
        xml = xml.replaceAll("[\\x01\\x02\\x03\\x04\\x05\\x06\\x0b\\x0c\\x0f\\x12\\x14\\x16\\x92\\x1a\\x1c\\x1e\\xff]", "?");
        stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

        try
        {
            doc = builder.parse(stream);
        }
        catch( SAXException e )
        {
            log.log(Level.SEVERE, "Parse diagram type \"" + name + "\" error: " + e.getMessage());
            return null;
        }
        catch( IOException e )
        {
            log.log(Level.SEVERE, "Read diagram type \"" + name + "\" error: " + e.getMessage());
            // <![CDATA[ ]]> section is invalid - remove it
            int start = xml.indexOf("<![CDATA[");
            String newXml = xml;
            if( start >= 0 )
            {
                int finish = xml.indexOf("]]>");
                if( finish > start + 9 )
                {
                    newXml = xml.substring(0, start + 9) + "Invalid diagram" + xml.substring(finish, xml.length());
                }
            }
            stream = new ByteArrayInputStream(newXml.getBytes(StandardCharsets.UTF_8));

            try
            {
                doc = builder.parse(stream);
            }
            catch( SAXException | IOException e2 )
            {
                return null;
            }

            return read(origin, doc);
        }

        return read(origin, doc);
    }

    protected XmlDiagramType read(DataCollection origin, Document doc) throws Exception
    {
        Element root = doc.getDocumentElement();
        if( root.hasAttribute(VERSION_ATTR) )
            version = root.getAttribute(VERSION_ATTR);

        diagramType = new XmlDiagramType(origin, name);

        if( root.hasAttribute(PLUGINS_ATTR) )
            diagramType.setRequiredPlugins(root.getAttribute(PLUGINS_ATTR));

        Element e = XmlUtil.getChildElement( root, PATH_LAYOUTER_ELEMENT );
        if(e != null)
        {
            try
            {
                Class<? extends Layouter> layouterClass = ClassLoading.loadSubClass( e.getAttribute( CLASS_ATTR ), Layouter.class );
                diagramType.setPathLayouter( layouterClass );
            }
            catch( LoggedClassNotFoundException | LoggedClassCastException ex )
            {
                error("ERROR_PATH_LAYOUTER", new String[] {name, ex.getMessage()}, ex);
            }
        }

        String title = root.getAttribute(TITLE_ATTR);
        if( title != null && title.trim().length()>0 )
        {
            diagramType.setTitle(title);
        }

        String description = root.getAttribute(DESCRIPTION_ATTR);
        if( description != null && description.trim().length()>0 )
        {
            diagramType.setDescription(description);
        }

        readProperties(root);
        readExamples(root);

        readNodeTypes(root);
        readEdgeTypes(root);
        readReactionTypes(root);

        readViewBuilder(root);
        readViewOptions(root);
        readSemanticController(root);
        readIcons(root);

        return diagramType;
    }

    ///////////////////////////////////////////////////////////////////
    // Properties issues
    //

    public void readProperties(Element root)
    {
        Element propertiesElement = getElement(root, PROPERTIES_ELEMENT);
        if( propertiesElement == null )
        {
            error("ERROR_PROPERTIES_ABSENT", new String[] {name});
            return;
        }

        DynamicPropertySet propertiesMap = diagramType.getProperties();
        DiagramXmlReader.fillProperties(propertiesElement, propertiesMap, null);
    }

    ///////////////////////////////////////////////////////////////////
    // Node and edge types
    //

    public void readNodeTypes(Element root)
    {
        Element nodeTypesElement = getElement(root, NODE_TYPES_ELEMENT);
        if( nodeTypesElement == null )
        {
            error("ERROR_NODE_TYPES_ABSENT", new String[] {name});
            return;
        }

        NodeList list = nodeTypesElement.getChildNodes();
        DynamicPropertySet propertiesMap = diagramType.getProperties();
        List<String> types = new ArrayList<>();

        for(Element child : XmlUtil.elements(list))
        {
            try
            {
                types.add(readNodeType(child, propertiesMap));
            }
            catch( Throwable t )
            {
                error("ERROR_NODE_TYPES", new String[] {name, t.getMessage()}, t);
            }
        }

        diagramType.setNodeTypes(types.toArray(new String[types.size()]));
    }

    public String readNodeType(Element nodeElement, DynamicPropertySet propertySet) throws Exception
    {
        String type;

        if( !nodeElement.getNodeName().equals(NODE_TYPE_ELEMENT) )
        {
            error("ERROR_UNEXPECTED_ELEMENT", new String[] {name, nodeElement.getNodeName(), NODE_TYPES_ELEMENT, NODE_TYPE_ELEMENT});
            return null;
        }

        if( nodeElement.hasAttribute(TYPE_ATTR) )
            type = nodeElement.getAttribute(TYPE_ATTR);
        else
        {
            error("ERROR_NODE_TYPE_NOT_SPECIFIED", new String[] {this.name, nodeElement.toString()});
            return null;
        }

        if( nodeElement.hasAttribute(PROPERTIES_BEAN_ATTR) )
        {
            try
            {
                diagramType.addPropertiesBeanClass(type, ClassLoading.loadClass( nodeElement.getAttribute(PROPERTIES_BEAN_ATTR) ));
            }
            catch( Exception e )
            {
            }
        }

        Class kernelType = null;
        if( nodeElement.hasAttribute(KERNELTYPE_ATTR) )
        {
            String className = nodeElement.getAttribute(KERNELTYPE_ATTR);
            try
            {
                kernelType = ClassLoading.loadClass( className, null );
            }
            catch( LoggedClassNotFoundException e )
            {
                kernelType = null;
            }
        }

        if( nodeElement.hasAttribute(ICON_ATTR) )
        {
            String icon = nodeElement.getAttribute(ICON_ATTR);
            iconName2Type.put(icon, type);
        }

        DynamicPropertySet dps = new DynamicPropertySetAsMap();
        DiagramXmlReader.fillProperties(nodeElement, dps, propertySet);

        if( nodeElement.hasAttribute(IS_COMPARTMENT) )
        {
            if( Boolean.parseBoolean(nodeElement.getAttribute(IS_COMPARTMENT)) )
            {
                diagramType.addCompartment(type);
            }
        }

        if( nodeElement.hasAttribute(DataCollectionConfigConstants.ID_FORMAT) )
        {
            String idFormat = nodeElement.getAttribute(DataCollectionConfigConstants.ID_FORMAT);
            if( idFormat.length() > 0 )
            {
                diagramType.setIdFormat(type, idFormat);
            }
        }

        diagramType.addType(type, dps, kernelType);
        if( nodeElement.hasAttribute(IS_DEFAULT) )
        {
            if( Boolean.parseBoolean(nodeElement.getAttribute(IS_DEFAULT)) )
            {
                diagramType.setDefaultTypeName(type);
            }
        }

        if( nodeElement.hasAttribute(NEED_LAYOUT) )
        {
            if( Boolean.parseBoolean(nodeElement.getAttribute(NEED_LAYOUT)) )
            {
                diagramType.addNeedLayout(type);
            }
        }

        if( nodeElement.hasAttribute(CREATE_BY_PROTOTYPE) )
        {
            if( Boolean.parseBoolean(nodeElement.getAttribute(CREATE_BY_PROTOTYPE)) )
            {
                diagramType.addCreateByPrototype( type);
            }
        }

        return type;
    }

    public void readEdgeTypes(Element root)
    {
        Element edgeTypesElement = getElement(root, EDGE_TYPES_ELEMENT);
        if( edgeTypesElement == null )
        {
            error("ERROR_EDGE_TYPES_ABSENT", new String[] {name});
            return;
        }

        NodeList list = edgeTypesElement.getChildNodes();
        DynamicPropertySet propertiesMap = diagramType.getProperties();
        List<String> types = new ArrayList<>();

        for(Element child : XmlUtil.elements(list))
        {
            try
            {
                types.add(readEdgeType(child, propertiesMap));
            }
            catch( Throwable t )
            {
                error("ERROR_EDGE_TYPES", new String[] {name, t.getMessage()}, t);
            }
        }

        diagramType.setEdgeTypes(types.toArray(new String[types.size()]));
    }

    public String readEdgeType(Element edgeElement, DynamicPropertySet propertySet) throws Exception
    {
        String type;

        if( !edgeElement.getNodeName().equals(EDGE_TYPE_ELEMENT) )
        {
            error("ERROR_UNEXPECTED_ELEMENT", new String[] {name, edgeElement.getNodeName(), EDGE_TYPES_ELEMENT, EDGE_TYPE_ELEMENT});
            return null;
        }

        if( edgeElement.hasAttribute(TYPE_ATTR) )
            type = edgeElement.getAttribute(TYPE_ATTR);
        else
        {
            error("ERROR_EDGE_TYPE_NOT_SPECIFIED", new String[] {this.name, edgeElement.toString()});
            return null;
        }

        Class kernelType = null;
        if( edgeElement.hasAttribute(KERNELTYPE_ATTR) )
        {
            String className = edgeElement.getAttribute(KERNELTYPE_ATTR);
            try
            {
                kernelType = ClassLoading.loadClass( className, null );
            }
            catch( LoggedClassNotFoundException e )
            {
                kernelType = null;
            }
        }

        if( edgeElement.hasAttribute(ICON_ATTR) )
        {
            String icon = edgeElement.getAttribute(ICON_ATTR);
            iconName2Type.put(icon, type);
        }

        DynamicPropertySet dps = new DynamicPropertySetAsMap();
        DiagramXmlReader.fillProperties(edgeElement, dps, propertySet);
        diagramType.addType(type, dps, kernelType);

        return type;
    }

    public void readReactionTypes(Element root)
    {
        Element reactionTypesElement = getElement(root, REACTION_TYPES_ELEMENT);
        if( reactionTypesElement == null )
        {
            error("ERROR_REACTION_TYPES_ABSENT", new String[] {name});
            return;
        }

        NodeList list = reactionTypesElement.getChildNodes();
        DynamicPropertySet propertiesMap = diagramType.getProperties();
        List<String> types = new ArrayList<>();

        for(Element child : XmlUtil.elements(list))
        {
            try
            {
                types.add(readReactionType(child, propertiesMap));
            }
            catch( Throwable t )
            {
                error("ERROR_REACTION_TYPES", new String[] {name, t.getMessage()}, t);
            }
        }

        diagramType.setReactionTypes(types.toArray(new String[types.size()]));
    }

    public String readReactionType(Element nodeElement, DynamicPropertySet propertySet) throws Exception
    {
        String type;

        if( !nodeElement.getNodeName().equals(REACTION_TYPE_ELEMENT) )
        {
            error("ERROR_UNEXPECTED_ELEMENT", new String[] {name, nodeElement.getNodeName(), REACTION_TYPES_ELEMENT, REACTION_TYPE_ELEMENT});
            return null;
        }

        if( nodeElement.hasAttribute(TYPE_ATTR) )
            type = nodeElement.getAttribute(TYPE_ATTR);
        else
        {
            error("ERROR_NODE_TYPE_NOT_SPECIFIED", new String[] {this.name, nodeElement.toString()});
            return null;
        }

        Class kernelType = null;
        if( nodeElement.hasAttribute(KERNELTYPE_ATTR) )
        {
            String className = nodeElement.getAttribute(KERNELTYPE_ATTR);
            try
            {
                kernelType = ClassLoading.loadClass( className, null );
            }
            catch( LoggedClassNotFoundException e )
            {
                kernelType = null;
            }
        }

        if( nodeElement.hasAttribute(ICON_ATTR) )
        {
            String icon = nodeElement.getAttribute(ICON_ATTR);
            iconName2Type.put(icon, type);
        }

        DynamicPropertySet dps = new DynamicPropertySetAsMap();
        DiagramXmlReader.fillProperties(nodeElement, dps, propertySet);
        diagramType.addType(type, dps, kernelType);

        return type;
    }

    ///////////////////////////////////////////////////////////////////
    // SemanticController
    //

    ///////////////////////////////////////////////////////////////////
    // DiagramViewOptions
    public void readViewOptions(Element root)
    {
        Element optionsElement = getElement(root, VIEW_OPTIONS_ELEMENT);
        if( optionsElement == null )
        {
            error("ERROR_VIEW_OPTIONS_ABSENT", new String[] {name});
            return;
        }

        // read options
        DiagramXmlReader.fillProperties(optionsElement, diagramType.getXmlDiagramViewBuilder().getViewOptions(), null);

    }

    public void readIcons(Element root)
    {
        Element iconsElement = getElement(root, ICONS_ELEMENT);
        if( iconsElement != null )
        {
            NodeList list = iconsElement.getElementsByTagName(ICON_ELEMENT);
            for(Element icon : XmlUtil.elements(list))
            {
                try
                {
                    String name = icon.getAttribute(NAME_ATTR);
                    if( !name.isEmpty() )
                    {
                        String elementType = getElementType(name);
                        String base64 = icon.getFirstChild().getNodeValue();
                        Object kernelType = diagramType.getKernelType(elementType);
                        Icon imageIcon = getIconFromBase64(base64);
                        diagramType.getXmlDiagramViewBuilder().setIcon(elementType, imageIcon);
                        diagramType.getXmlDiagramViewBuilder().setIcon(kernelType, imageIcon);
                    }
                    else
                    {
                        error("ERROR_NO_ICON_NAME", new String[] {});
                    }
                }
                catch( Throwable t )
                {
                    error("ERROR_EDGE_TYPES", new String[] {name, t.getMessage()}, t);
                }
            }
        }
    }

    public void readExamples(Element root)
    {
        Element examplesElement = getElement(root, EXAMPLES_ELEMENT);
        if( examplesElement == null )
        {
            error("ERROR_EXAMPLES_ABSENT", new String[] {name});
            return;
        }

        NodeList list = examplesElement.getElementsByTagName(EXAMPLE_ELEMENT);
        for(Element example : XmlUtil.elements(list))
        {
            try
            {
                String name = example.getAttribute(NAME_ATTR);
                if( !name.isEmpty() )
                {
                    readExample(name, getElement(example, DiagramXmlReader.DIAGRAM_ELEMENT));
                }
                else
                {
                    error("ERROR_NO_EXAMPLE_NAME", new String[] {name});
                }
            }
            catch( Throwable t )
            {
                error("ERROR_READ_EXAMPLE", new String[] {name, t.getMessage()}, t);
            }
        }
    }

    protected void readExample(String name, Element element)
    {
        diagramType.addExampleElement(name, element);
//        Diagram diagram = DiagramXmlReader.parseDiagram(name, element, null, diagramType);
//        diagramType.addExample(diagram);
    }

    private String getElementType(String name)
    {
        return iconName2Type.get(name);
    }

    public static Icon getIconFromBase64(String base64)
    {
        return new ImageIcon( Base64.getDecoder().decode( base64 ) );
    }


    ///////////////////////////////////////////////////////////////////
    // DaiagramViewBuilder
    //

    public void readViewBuilder(Element root)
    {
        XmlDiagramViewBuilder dvb = new XmlDiagramViewBuilder(diagramType);
        diagramType.xmlDiagramViewBuilder = dvb;

        Element builderElement = getElement(root, VIEW_BUILDER_ELEMENT);
        if( builderElement == null )
        {
            error("ERROR_VIEW_BUILDER_ABSENT", new String[] {name});
            return;
        }

        // get prototype
        if( builderElement.hasAttribute(PROTOTYPE_ATTR) )
        {
            String prototype = builderElement.getAttribute(PROTOTYPE_ATTR);

            try
            {
                dvb.setBaseViewBuilder( ClassLoading.loadSubClass( prototype, diagramType.getRequiredPlugins(), DiagramViewBuilder.class )
                        .newInstance() );
            }
            catch( Throwable t )
            {
                error("ERROR_VIEW_BUILDER_PROTOTYPE", new String[] {name, t.getMessage()}, t);
            }
        }

        // read JavaScript functions
        NodeList list = builderElement.getChildNodes();
        for(Element child : XmlUtil.elements(list))
        {
            try
            {
                readViewFunction(child, dvb);
            }
            catch( Throwable t )
            {
                error("ERROR_VIEW_BUILDER_FUNCTION", new String[] {name, t.getMessage()}, t);
            }
        }
    }

    protected void readViewFunction(Element viewElement, XmlDiagramViewBuilder dvb) throws Exception
    {
        if( !viewElement.getNodeName().equals(NODE_VIEW_ELEMENT) && !viewElement.getNodeName().equals(EDGE_VIEW_ELEMENT) )
        {
            error("ERROR_UNEXPECTED_ELEMENT", new String[] {name, viewElement.getNodeName(), VIEW_BUILDER_ELEMENT,
                    ( NODE_VIEW_ELEMENT + ", " + EDGE_VIEW_ELEMENT )});
            return;
        }

        if( !viewElement.hasAttribute(TYPE_ATTR) )
        {
            error("ERROR_VIEW_TYPE_NOT_SPECIFIED", new String[] {this.name, viewElement.toString()});
            return;
        }

        String type = viewElement.getAttribute(TYPE_ATTR);
        String script = getCDATA(viewElement);

        if( script == null || script.length() == 0 )
        {
            error("ERROR_VIEW_SCRIPT_NOT_SPECIFIED", new String[] {this.name, type});
            return;
        }

        dvb.addFunction(type, script);
    }

    public void readSemanticController(Element root)
    {
        XmlDiagramSemanticController sc = new XmlDiagramSemanticController(diagramType);
        diagramType.setSemanticController(sc);

        Element controllerElement = getElement(root, SEMANTIC_CONTROLLER_ELEMENT);
        if( controllerElement == null )
        {
            error("ERROR_SEMANTIC_CONTROLLER_ABSENT", new String[] {name});
            return;
        }

        // get prototype
        if( controllerElement.hasAttribute(PROTOTYPE_ATTR) )
        {
            String prototype = controllerElement.getAttribute(PROTOTYPE_ATTR);

            try
            {
                sc.setPrototype(( ClassLoading.loadSubClass( prototype, diagramType.getRequiredPlugins(), SemanticController.class ) ).newInstance());
            }
            catch( Throwable t )
            {
                error("ERROR_SEMANTIC_CONTROLLER_PROTOTYPE", new String[] {name, t.getMessage()}, t);
            }
        }

        // read JavaScript functions
        NodeList list = controllerElement.getChildNodes();
        for(Element child : XmlUtil.elements(list))
        {
            try
            {
                readSemanticControllerFunction(child, sc);
            }
            catch( Throwable t )
            {
                error("ERROR_SEMANTIC_CONTROLLER_FUNCTION", new String[] {name, t.getMessage()}, t);
            }
        }
    }

    protected void readSemanticControllerFunction(Element viewElement, XmlDiagramSemanticController sc) throws Exception
    {
        if( !viewElement.getNodeName().equals(CAN_ACCEPT_ELEMENT) && !viewElement.getNodeName().equals(IS_RESIZABLE_ELEMENT)
                && !viewElement.getNodeName().equals(MOVE_ELEMENT) && !viewElement.getNodeName().equals(REMOVE_ELEMENT) )
        {
            error("ERROR_UNEXPECTED_ELEMENT", new String[] {name, viewElement.getNodeName(), VIEW_BUILDER_ELEMENT,
                    ( NODE_VIEW_ELEMENT + ", " + EDGE_VIEW_ELEMENT )});
            return;
        }

        String script = getCDATA(viewElement);

        if( script == null || script.length() == 0 )
        {
            error("ERROR_CONTROLLER_SCRIPT_NOT_SPECIFIED", new String[] {this.name, ""});
            return;
        }

        if( viewElement.getNodeName().equals(CAN_ACCEPT_ELEMENT) )
        {
            sc.setCanAcceptFunction(script);
        }
        else if( viewElement.getNodeName().equals(IS_RESIZABLE_ELEMENT) )
        {
            sc.setIsResizableFunction(script);
        }
        else if( viewElement.getNodeName().equals(MOVE_ELEMENT) )
        {
            sc.setMoveFunction(script);
        }
        else if( viewElement.getNodeName().equals(REMOVE_ELEMENT) )
        {
            sc.setRemoveFunction(script);
        }
    }

    protected String getCDATA(Element element)
    {
        return XmlStream.nodes( element ).findFirst( child -> child.getNodeName().equals( "#cdata-section" ) )
                .map( child -> child.getNodeValue().trim() ).orElse( null );
    }
}
