package biouml.plugins.gxl;

import java.io.File;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import java.util.logging.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataCollection;
import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.DiagramTypeSupport;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Stub;

/**
 * @todo Add support of "rel", "rel", "type", "attr" elements
 */
public class GxlReader extends GxlSupport
{
    protected Diagram diagram = null;
    protected DataCollection origin = null;

    @Override
    protected Logger initLog()
    {
        return Logger.getLogger(GxlReader.class.getName());
    }

    protected Diagram createDiagram(String name, DataCollection origin) throws Exception
    {
        return new Diagram(origin, new DiagramInfo(name), getDiagramType());
    }

    protected DiagramType getDiagramType()
    {
        return new DiagramTypeSupport();
    }

    private Diagram getDiagram(String name, DataCollection origin) throws Exception
    {
        if( diagram == null )
            diagram = createDiagram(name, origin);
        return diagram;
    }

    public Diagram readDiagram(File file, DataCollection origin)
    {
        this.origin = origin;

        SAXParserFactory factory = SAXParserFactory.newInstance();
        try
        {
            SAXParser saxParser = factory.newSAXParser();

            String str = ApplicationUtils.readAsString(file);

            diagram = getDiagram(file.getName(), origin);

            // dirty trick to escape DTD loading
            saxParser.parse(new java.io.StringBufferInputStream(str.replaceAll("<!DOCTYPE.*>", "")), getHandler());
        }
        catch( Throwable t )
        {
            error("GXL_PARSE_ERROR", new String[] {file.getName(), t.getMessage()});
        }
        return diagram;
    }

    /**
     * Returns SAX handler for given type of GXL
     * extension document. Override it to have extended behavior.
     */
    protected DefaultHandler getHandler()
    {
        return new GxlHandler();
    }

    /**
     * Define its descendants to have extended behavior in classes
     * derivated from GxlReader
     */
    protected class GxlHandler extends DefaultHandler implements GxlConstants
    {
        protected GxlParserContext context;
        public GxlHandler()
        {
            context = new GxlParserContext();
        }

        @Override
        public void startDocument() throws SAXException
        {

        }

        @Override
        public void startElement(String namespaceURI, String localName, // local name
                String qualifiedName, // qualified name
                Attributes attrs) throws SAXException
        {
            String elementName = localName;
            if( "".equals(elementName) )
                elementName = qualifiedName;

            if( elementName.equals(GXL_ELEM) )
                readGxl(attrs);
            else if( elementName.equals(GRAPH_ELEM) )
                readGraph(attrs);
            else if( elementName.equals(NODE_ELEM) )
                readNode(attrs);
            else if( elementName.equals(EDGE_ELEM) )
                readEdge(attrs);
            else if( elementName.equals(RELATION_ELEM) )
                readRelation(attrs);
            else if( elementName.equals(RELEND_ELEM) )
                readRelEnd(attrs);
            else if( elementName.equals(TYPE_ELEM) )
                readType(attrs);
            else if( elementName.equals(ATTR_ELEM) )
                readAttribute(attrs);
            else if( isValue(elementName) )
                readValue(attrs);
        }

        public void endElement(String namespaceURI, String localName, // local name
                String qualifiedName, // qualified name
                Attributes attrs)
        {
            String elementName = localName;
            if( "".equals(elementName) )
                elementName = qualifiedName;

            if( elementName.equals(GXL_ELEM) )
                leaveGxl(attrs);
            else if( elementName.equals(GRAPH_ELEM) )
                leaveGraph(attrs);
            else if( elementName.equals(NODE_ELEM) )
                leaveNode(attrs);
            else if( elementName.equals(EDGE_ELEM) )
                leaveEdge(attrs);
            else if( elementName.equals(RELATION_ELEM) )
                leaveRelation(attrs);
            else if( elementName.equals(RELEND_ELEM) )
                leaveRelEnd(attrs);
            else if( elementName.equals(TYPE_ELEM) )
                leaveType(attrs);
            else if( elementName.equals(ATTR_ELEM) )
                leaveAttribute(attrs);
            else if( isValue(elementName) )
                leaveValue(attrs);
        }

        protected void readGxl(Attributes attrs)
        {
            // do nothing
        }

        /**
         * Create new diagram for each specified graph
         */
        protected void readGraph(Attributes attrs)
        {
            String id = attrs.getValue(ID_ATTR);
            try
            {
                processGraphExtension(attrs);
            }
            catch( Exception e )
            {
                GxlReader.this.error("ERROR_GRAPH_PROCESSING", new String[] {id != null ? id : ""});
            }
        }

        /**
         * It is stub for further overriding
         */
        protected void processGraphExtension(Attributes attrs)
        {
        }

        protected void readNode(Attributes attrs)
        {
            String id = attrs.getValue(ID_ATTR);
            try
            {
                Node node = new Node(diagram, id, new Stub(diagram, id));
                context.enterNodeContext(node);
                processNodeExtension(node, attrs);
                diagram.put(node);
            }
            catch( Exception e )
            {
                GxlReader.this.error("ERROR_NODE_PROCESSING", new String[] {id != null ? id : ""});
            }
        }

        /**
         * It is stub for further overriding
         */
        protected void processNodeExtension(Node node, Attributes attrs)
        {
        }

        protected void readEdge(Attributes attrs)
        {
            String edgeId = attrs.getValue(ID_ATTR);
            try
            {
                String from = attrs.getValue(FROM_ATTR);
                String to = attrs.getValue(TO_ATTR);
                if( from == null )
                {
                    GxlReader.this.error("ERROR_NO_FROM_NODE", new String[] {edgeId});
                    return;
                }

                if( to == null )
                {
                    GxlReader.this.error("ERROR_NO_TO_NODE", new String[] {edgeId});
                    return;
                }

                // autogenerate id if it absent
                if( edgeId == null )
                    edgeId = from + "_to_" + to;

                Node fromNode = (Node)diagram.get(from);
                Node toNode = (Node)diagram.get(to);

                Edge edge = new Edge(diagram, edgeId, null, fromNode, toNode);
                context.enterEdgeContext(edge);
                processEdgeExtension(edge, attrs);
                diagram.put(edge);
            }
            catch( Exception e )
            {
                GxlReader.this.error("ERROR_EDGE_PROCESSING", new String[] {edgeId});
            }
        }

        /**
         * It is stub for further overriding
         */
        protected void processEdgeExtension(Edge edge, Attributes attrs)
        {
        }

        /**
         * @todo Do something with processRelationExtension(...)
         */
        protected void readRelation(Attributes attrs)
        {
            String id = attrs.getValue(ID_ATTR);
            GxlReader.this.warn("ERROR_RELATIONS_NOT_SUPPORTED", new String[] {id != null ? id : ""});
        }

        /**
         * @todo Do something with processRelendExtension(...)
         */
        protected void readRelEnd(Attributes attrs)
        {
            String target = attrs.getValue(TARGET_ATTR);
            GxlReader.this.warn("ERROR_RELEND_NOT_SUPPORTED", new String[] {target});
        }

        /**
         * @todo Do something with processTypeExtension(...)
         */
        protected void readType(Attributes attrs)
        {
            String id = attrs.getValue(ID_ATTR);
            GxlReader.this.warn("ERROR_TYPE_NOT_SUPPORTED", new String[] {id != null ? id : ""});
        }

        /**
         * @todo Do something with processAttributeExtension(...)
         */
        protected void readAttribute(Attributes attrs)
        {
            String id = attrs.getValue(ID_ATTR);
            GxlReader.this.warn("ERROR_ATTRIBUTE_NOT_SUPPORTED", new String[] {id != null ? id : ""});
        }

        /**
         * @todo Implement it together with atrribute processing
         */
        protected void readValue(Attributes attrs)
        {
        }


        protected void leaveGxl(Attributes attrs)
        {
        }
        protected void leaveGraph(Attributes attrs)
        {
        }

        protected void leaveNode(Attributes attrs)
        {
            context.leaveNodeContext();
        }

        protected void leaveEdge(Attributes attrs)
        {
            context.leaveEdgeContext();
        }

        protected void leaveRelation(Attributes attrs)
        {
        }
        protected void leaveRelEnd(Attributes attrs)
        {
        }
        protected void leaveType(Attributes attrs)
        {
        }
        protected void leaveAttribute(Attributes attrs)
        {
        }
        protected void leaveValue(Attributes attrs)
        {
        }


        protected boolean isValue(String name)
        {
            return name.equals(LOCATOR_ELEM) || name.equals(BOOL_ELEM) || name.equals(INT_ELEM) || name.equals(FLOAT_ELEM)
                    || name.equals(STRING_ELEM) || name.equals(ENUM_ELEM) || name.equals(SEQ_ELEM) || name.equals(SET_ELEM)
                    || name.equals(BAG_ELEM) || name.equals(TUP_ELEM) || isValueExtension(name);
        }

        /**
         * It may be overridden to support value extensions
         */
        protected boolean isValueExtension(String elementName)
        {
            return false;
        }

    }
}
