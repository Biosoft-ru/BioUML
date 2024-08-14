package biouml.model.xml;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import biouml.model.Diagram;
import biouml.model.util.DiagramXmlWriter;

import com.developmentontheedge.beans.DynamicPropertySet;

public class XmlDiagramTypeWriter extends XmlDiagramTypeSupport
{
    public static final String VERSION = "0.8.0";
    public static final String APPVERSION = "0.8.0";

    protected static final Logger log = Logger.getLogger(XmlDiagramTypeReader.class.getName());

    private Document doc;

    /** Stream to store the XML . */
    protected OutputStream stream;

    private final Map<Object, String> elementType2Icon = new HashMap<>();
    private int counter = 0;

    public XmlDiagramTypeWriter(OutputStream stream)
    {
        this.stream = stream;
    }

    public void write(XmlDiagramType diagramType) throws Exception
    {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        write(diagramType, transformerFactory.newTransformer());
    }

    public void write(XmlDiagramType diagramType, Transformer transformer) throws Exception
    {
        if( diagramType == null )
        {
            String msg = "XmlDiagramTypeWriter - diagramType is null, can not be written.";
            Exception e = new NullPointerException(msg);
            log.log(Level.SEVERE, msg, e);
            throw e;
        }

        this.diagramType = diagramType;

        buildDocument();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(stream);
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.MEDIA_TYPE, "text/xml");
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        transformer.transform(source, result);
    }

    protected Document buildDocument() throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        doc = builder.newDocument();
        doc.appendChild(createGraphicNotationElement());
        return doc;
    }

    protected Element createGraphicNotationElement()
    {
        Element element = doc.createElement(GRAPHIC_NOTATION_ELEMENT);
        element.setAttribute(VERSION_ATTR, VERSION);
        element.setAttribute(APPVERSION_ATTR, APPVERSION);
        element.setAttribute(TITLE_ATTR, diagramType.getTitle());

        String plugins = diagramType.getRequiredPlugins();
        if( plugins != null )
            element.setAttribute(PLUGINS_ATTR, plugins);


        element.appendChild(createPropertiesElement());
        element.appendChild(createNodesElement());
        element.appendChild(createEdgesElement());
        element.appendChild(createReactionsElement());
        element.appendChild(createViewOptionsElement());
        element.appendChild(createViewBuilderElement());
        element.appendChild(createIconsElement());
        element.appendChild(createSemanticControllerElement());
        element.appendChild(createExamplesElement());

        return element;
    }


    private Node createIconsElement()
    {
        Element iconsElement = doc.createElement(ICONS_ELEMENT);
        XmlDiagramViewBuilder diagramViewBuilder = diagramType.getXmlDiagramViewBuilder();
        for( Object edgeType : diagramType.getEdgeTypes() )
        {
            Icon icon = diagramViewBuilder.getIcon(edgeType);
            String base64 = encodeBase64(icon);
            if( base64 == null )
            {
                error("ERROR_UNSERIALIZABLE_ICON_TYPE", new String[] {edgeType.toString()});
            }
            else
            {
                Element iconElement = doc.createElement(ICON_ELEMENT);
                iconElement.setAttribute(NAME_ATTR, getIconName(edgeType));
                iconElement.setTextContent(base64);
                iconsElement.appendChild(iconElement);
            }
        }
        for( Object nodeType : diagramType.getNodeTypes() )
        {
            Icon icon = diagramViewBuilder.getIcon(nodeType);
            String base64 = encodeBase64(icon);
            if( base64 == null )
            {
                error("ERROR_UNSERIALIZABLE_ICON_TYPE", new String[] {nodeType.toString()});
            }
            else
            {
                Element iconElement = doc.createElement(ICON_ELEMENT);
                iconElement.setAttribute(NAME_ATTR, getIconName(nodeType));
                iconElement.setTextContent(base64);
                iconsElement.appendChild(iconElement);
            }
        }
        return iconsElement;
    }

    private String getIconName(Object type)
    {
        return elementType2Icon.get(diagramType.getKernelTypeName(type));
    }

    private String setIconNameForType(String type)
    {
        String iconName = "icon" + ( counter++ );
        elementType2Icon.put(type, iconName);
        return iconName;
    }

    private Node createViewBuilderElement()
    {
        Element viewBuilderElement = doc.createElement(VIEW_BUILDER_ELEMENT);
        XmlDiagramViewBuilder diagramViewBuilder = diagramType.getXmlDiagramViewBuilder();
        String prototype = diagramViewBuilder.getBaseViewBuilder().getClass().getName();
        viewBuilderElement.setAttribute(PROTOTYPE_ATTR, prototype);

        for( Map.Entry<String, String> e : diagramViewBuilder.getFunctionCodes().entrySet() )
        {
            Element nodeViewElement = doc.createElement(NODE_VIEW_ELEMENT);
            nodeViewElement.setAttribute(TYPE_ATTR, e.getKey());
            nodeViewElement.appendChild(doc.createCDATASection(e.getValue()));
            viewBuilderElement.appendChild(nodeViewElement);
        }

        return viewBuilderElement;
    }

    private Node createViewOptionsElement()
    {
        Element viewOptionsElement = doc.createElement(VIEW_OPTIONS_ELEMENT);
        DynamicPropertySet options = diagramType.getXmlDiagramViewBuilder().getViewOptions();
        DiagramXmlWriter.serializeDPS(doc, viewOptionsElement, options, null);
        return viewOptionsElement;
    }

    private Node createSemanticControllerElement()
    {
        Element semanticControllerElement = doc.createElement(SEMANTIC_CONTROLLER_ELEMENT);
        XmlDiagramSemanticController diagramSemanticController = (XmlDiagramSemanticController)diagramType.getSemanticController();
        String prototype = diagramSemanticController.getPrototype().getClass().getName();
        semanticControllerElement.setAttribute(PROTOTYPE_ATTR, prototype);

        if( diagramSemanticController.getCanAcceptCode() != null )
        {
            Element element = doc.createElement(CAN_ACCEPT_ELEMENT);
            element.appendChild(doc.createCDATASection(diagramSemanticController.getCanAcceptCode()));
            semanticControllerElement.appendChild(element);
        }
        if( diagramSemanticController.getIsResizableCode() != null )
        {
            Element element = doc.createElement(IS_RESIZABLE_ELEMENT);
            element.appendChild(doc.createCDATASection(diagramSemanticController.getIsResizableCode()));
            semanticControllerElement.appendChild(element);
        }
        if( diagramSemanticController.getMoveCode() != null )
        {
            Element element = doc.createElement(MOVE_ELEMENT);
            element.appendChild(doc.createCDATASection(diagramSemanticController.getMoveCode()));
            semanticControllerElement.appendChild(element);
        }
        if( diagramSemanticController.getRemoveCode() != null )
        {
            Element element = doc.createElement(REMOVE_ELEMENT);
            element.appendChild(doc.createCDATASection(diagramSemanticController.getRemoveCode()));
            semanticControllerElement.appendChild(element);
        }
        return semanticControllerElement;
    }

    private Node createEdgesElement()
    {
        Element edgeTypesElement = doc.createElement(EDGE_TYPES_ELEMENT);
        for( String edgeType : diagramType.getEdges() )
        {
            String iconName = setIconNameForType(edgeType);
            Element edgeElement = doc.createElement(EDGE_TYPE_ELEMENT);
            edgeElement.setAttribute(ICON_ATTR, iconName);
            edgeElement.setAttribute(TYPE_ATTR, edgeType);
            Object kernelType = diagramType.getKernelType(edgeType);
            if( kernelType instanceof Class )
            {
                edgeElement.setAttribute(KERNELTYPE_ATTR, ( (Class<?>)kernelType ).getName());
            }
            DynamicPropertySet typeDPS = diagramType.getType(edgeType);
            if( typeDPS != null )
            {
                DiagramXmlWriter.serializeDPS(doc, edgeElement, typeDPS, diagramType.getProperties());
            }
            edgeTypesElement.appendChild(edgeElement);
        }
        return edgeTypesElement;
    }

    private Node createNodesElement()
    {
        Element nodeTypesElement = doc.createElement(NODE_TYPES_ELEMENT);
        if( diagramType.getNodes() != null )
        {
            for( String nodeType : diagramType.getNodes() )
            {
                String iconName = setIconNameForType(nodeType);
                Element nodeElement = doc.createElement(NODE_TYPE_ELEMENT);
                nodeElement.setAttribute(ICON_ATTR, iconName);
                nodeElement.setAttribute(TYPE_ATTR, nodeType);
                if( diagramType.checkCompartment(nodeType) )
                {
                    nodeElement.setAttribute(IS_COMPARTMENT, "true");
                }
                String idFormat = diagramType.getIdFormat(nodeType);
                if( idFormat != null )
                {
                    nodeElement.setAttribute(DataCollectionConfigConstants.ID_FORMAT, idFormat);
                }
                Object kernelType = diagramType.getKernelType(nodeType);
                if( kernelType instanceof Class )
                {
                    nodeElement.setAttribute(KERNELTYPE_ATTR, ( (Class<?>)kernelType ).getName());
                }
                if((diagramType.getDefaultTypeName() != null) && (diagramType.getDefaultTypeName().equals(nodeType)))
                {
                    nodeElement.setAttribute(IS_DEFAULT, "true");
                }
                if( diagramType.checkNeedLayout(nodeType) )
                {
                    nodeElement.setAttribute(NEED_LAYOUT, "true");
                }
                Object bean = diagramType.getPropertiesBean(nodeType);
                if(bean != null)
                {
                    nodeElement.setAttribute(PROPERTIES_BEAN_ATTR, bean.getClass().getName());
                }
                DynamicPropertySet typeDPS = diagramType.getType(nodeType);
                if( typeDPS != null )
                {
                    DiagramXmlWriter.serializeDPS(doc, nodeElement, typeDPS, diagramType.getProperties());
                }
                nodeTypesElement.appendChild(nodeElement);
            }
        }
        return nodeTypesElement;
    }

    private Node createReactionsElement()
    {
        Element nodeTypesElement = doc.createElement(REACTION_TYPES_ELEMENT);
        if( diagramType.getReactionTypes() != null )
        {
            for( String nodeType : diagramType.getReactionTypes() )
            {
                String iconName = setIconNameForType(nodeType);
                Element nodeElement = doc.createElement(REACTION_TYPE_ELEMENT);
                nodeElement.setAttribute(ICON_ATTR, iconName);
                nodeElement.setAttribute(TYPE_ATTR, nodeType);
                Object kernelType = diagramType.getKernelType(nodeType);
                if( kernelType instanceof Class )
                {
                    nodeElement.setAttribute(KERNELTYPE_ATTR, ( (Class<?>)kernelType ).getName());
                }
                DynamicPropertySet typeDPS = diagramType.getType(nodeType);
                if( typeDPS != null )
                {
                    DiagramXmlWriter.serializeDPS(doc, nodeElement, typeDPS, diagramType.getProperties());
                }
                nodeTypesElement.appendChild(nodeElement);
            }
        }
        return nodeTypesElement;
    }

    private Node createExamplesElement()
    {
        Element examplesElement = doc.createElement(EXAMPLES_ELEMENT);

        Iterator<?> iter = diagramType.getExampleNameList().iterator();

        while( iter.hasNext() )
        {
            Diagram diagram = diagramType.getExample((String)iter.next());
            Element exampleElement = doc.createElement(EXAMPLE_ELEMENT);
            exampleElement.setAttribute(NAME_ATTR, diagram.getName());
            exampleElement.appendChild(DiagramXmlWriter.serializeDiagram(doc, diagram));
            examplesElement.appendChild(exampleElement);
        }

        return examplesElement;
    }

    private Node createPropertiesElement()
    {
        Element properties = doc.createElement(PROPERTIES_ELEMENT);
        DiagramXmlWriter.serializeDPS(doc, properties, diagramType.getProperties(), null);
        return properties;
    }

    public static String encodeBase64(Icon icon)
    {
        if( icon instanceof ImageIcon )
        {
            ByteArrayOutputStream baos = null;
            try
            {
                BufferedImage bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = bi.createGraphics();
                g2.drawImage( ( (ImageIcon)icon ).getImage(), 0, 0, null);
                baos = new ByteArrayOutputStream();
                ImageIO.write(bi, "png", baos);
            }
            catch( Exception e )
            {
                return null;
            }
            return Base64.getEncoder().encodeToString( baos.toByteArray() );
        }
        return null;
    }

}
