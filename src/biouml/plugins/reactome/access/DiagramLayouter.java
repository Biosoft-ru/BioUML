package biouml.plugins.reactome.access;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.ArrayUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.graph.Path;
import ru.biosoft.graphics.Brush;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.XmlStream;
import ru.biosoft.util.XmlUtil;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementStyle;
import biouml.model.Edge;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.xml.XmlDiagramTypeConstants;
import biouml.plugins.reactome.ReactomeDiagramReference;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.standard.type.Stub;

import com.developmentontheedge.beans.DynamicProperty;

/**
 * @author anna
 *
 */
public class DiagramLayouter
{
    private final @Nonnull Diagram diagram;
    private final Connection connection;
    private final String table;
    private Map<String, NodeLayout> id2layout;
    private List<String> processNodes;
    private Map<String, String> component2compartment;
    private Map<String, List<String>> innerId2id;
    private Map<String, List<EdgeLayout>> reactionComponents;
    private List<EdgeLayout> noteEdges;
    
    private static final Map<String, String> edgeTypeMap = new HashMap<>();
    
    static
    {
        edgeTypeMap.put("Catalyst", "catalysis");
        edgeTypeMap.put("Inhibitor", "inhibition");
        edgeTypeMap.put("Activator", "stimulation");
    }
    
    private static final double SCALE_X = 1.2;
    private static final double SCALE_Y = 1.2;

    public DiagramLayouter(@Nonnull Diagram diagram, Connection connection, String table)
    {
        this.diagram = diagram;
        this.connection = connection;
        this.table = table;
    }
    
    public Diagram applyLayout() throws Exception
    {
        id2layout = new HashMap<>();
        component2compartment = new HashMap<>();
        innerId2id = new HashMap<>();
        reactionComponents = new HashMap<>();
        noteEdges = new ArrayList<>();
        processNodes = new ArrayList<>();
        readLayout();
        return rebuildDiagramStructure();
    }

    private void readLayout() throws Exception
    {
        ResultSet rs = null;
        Statement st = null;
        byte[] data;
        try
        {
            st = connection.createStatement();
            rs = st.executeQuery("SELECT storedATXML from " + table + " WHERE DB_ID=" + diagram.getName());
            if( !rs.next() )
            {
                throw new Exception("No layout for pathway "+diagram.getCompletePath());
            }
            data = rs.getBytes(1);
        }
        finally
        {
            SqlUtil.close(st, rs);
        }
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new ByteArrayInputStream(data));
        doc.getDocumentElement().normalize();
        Element process = doc.getDocumentElement();
        if(process == null)
            throw new Exception("Unable to find tag 'Process'");
        Element nodes = XmlUtil.getChildElement(process, "Nodes");
        if(nodes != null)
        {
            XmlStream.elements( nodes ).forEach( this::readElement );
        }

        Element edges = XmlUtil.getChildElement(process, "Edges");
        if(edges != null)
        {
            for( Element elem : XmlUtil.elements(edges) )
            {
                readElement(elem);
                readReactionComponents(elem);
                if(elem.getTagName().equals("org.gk.render.EntitySetAndMemberLink")
                        || elem.getTagName().equals("org.gk.render.FlowLine"))
                {
                    Element input = (Element)elem.getElementsByTagName("Input").item(0);
                    if(input == null)
                        continue;
                    String inputId = input.getAttribute("id");
                    if(inputId == null)
                        continue;
                    Element output = (Element)elem.getElementsByTagName("Output").item(0);
                    if(output == null)
                        continue;
                    String outputId = output.getAttribute("id");
                    if(outputId == null)
                        continue;
                    Point[] points = strToPointArray(elem.getAttribute("points"));
                    noteEdges.add(new EdgeLayout(inputId, outputId, points, null));
                }
            }
        }
    }
    
    private void readReactionComponents(Element elem)
    {
        String tag = elem.getTagName();
        if(tag.equals("org.gk.render.RenderableReaction"))
        {
            List<EdgeLayout> components = new ArrayList<>();
            Point[] position = strToPointArray(elem.getAttribute("position"));
            Point[] points = strToPointArray(elem.getAttribute("points"));
            int middlePoint = ArrayUtils.indexOf(points, position[0]);
            if(middlePoint < 0) middlePoint = points.length/2;
            Point[] inputPoints = (Point[])ArrayUtils.subarray(points, 0, middlePoint+1);
            Point[] outputPoints = (Point[])ArrayUtils.subarray(points, middlePoint, points.length);
            ArrayUtils.reverse(outputPoints);
            for(EdgeLayout layout : readComponentsGroup(elem, "Inputs", "Input", false))
            {
                if(points.length >= 2)
                    layout.addAfter(inputPoints);
                components.add(layout);
            }
            for(EdgeLayout layout : readComponentsGroup(elem, "Outputs", "Output", true))
            {
                if(points.length >= 3)
                    layout.addAfter(outputPoints);
                layout.revert();
                components.add(layout);
            }
            for(EdgeLayout layout : readComponentsGroup(elem, "Catalysts", "Catalyst", false))
            {
                if(points.length >= 2)
                    layout.addAfter(position);
                components.add(layout);
            }
            for(EdgeLayout layout : readComponentsGroup(elem, "Inhibitors", "Inhibitors", false))
            {
                if(points.length >= 2)
                    layout.addAfter(position);
                components.add(layout);
            }
            for(EdgeLayout layout : readComponentsGroup(elem, "Activators", "Activator", false))
            {
                if(points.length >= 2)
                    layout.addAfter(position);
                components.add(layout);
            }
            reactionComponents.put(elem.getAttribute("id"), components);
        }
    }

    private List<EdgeLayout> readComponentsGroup(Element elem, String groupName, String elementName, boolean output)
    {
        List<EdgeLayout> result = new ArrayList<>();
        Element group = XmlUtil.getChildElement(elem, groupName);
        if(group != null)
        {
            for(Element child : XmlUtil.elements(group, elementName))
            {
                EdgeLayout layout;
                if( output )
                    layout = new EdgeLayout(elem.getAttribute("id"), child.getAttribute("id"), strToPointArray(child.getAttribute("points")), elementName);
                else
                    layout = new EdgeLayout(child.getAttribute("id"), elem.getAttribute("id"), strToPointArray(child.getAttribute("points")), elementName);
                
                result.add(layout);
            }
        }
        return result;
    }

    private void readElement(Element elem)
    {
        String tag = elem.getTagName();
        if( tag.equals("org.gk.render.RenderableCompartment") )
        {
            String id = elem.getAttribute("id");
            Element componentsElement = XmlUtil.getChildElement(elem, "Components");
            if( componentsElement != null )
            {
                for(Element component : XmlUtil.elements(componentsElement, "Component"))
                {
                    component2compartment.put(component.getAttribute("id"), id);
                }
            }
        }
        if( tag.startsWith("org.gk.render.Renderable") || tag.equals("org.gk.render.ProcessNode"))
        {
            String id = elem.getAttribute("id");
            if(tag.equals("org.gk.render.ProcessNode"))
                processNodes.add(id);
            NodeLayout layout = new NodeLayout(elem, tag.equals("org.gk.render.RenderableCompartment"));
            id2layout.put(id, layout);
            innerId2id.computeIfAbsent( layout.getId(), innerId -> new ArrayList<>() ).add( id );
        }
    }
    
    private static String getInnerId(DiagramElement de)
    {
        if(de.getKernel() == null)
            return null;
        Object innerId = de.getKernel().getAttributes().getValue("InnerID");
        if(innerId == null)
            return null;
        return innerId.toString();
    }

    private Diagram rebuildDiagramStructure() throws Exception
    {
        Diagram newDiagram = diagram.getType().createDiagram(diagram.getOrigin(), diagram.getName(), diagram.getKernel());
        newDiagram.setTitle(diagram.getTitle());
        Map<String, Compartment> compartments = new HashMap<>();
        newDiagram.setNotificationEnabled(false);
        Map<String, Node> nodes = new HashMap<>();
        /*
         * First create all the compartments
         * We create them in cycle, because if compartment is located in another compartment,
         * it should be created after parent one. Thus we postpone compartment to further iteration
         * if its parent is not yet created
         */
        boolean newCompartments;
        do
        {
            newCompartments = false;
            for( String compartmentId : component2compartment.values() )
            {
                Compartment compartment = compartments.get(compartmentId);
                if( compartment != null )
                    continue;
                Compartment parent;
                String parentId = component2compartment.get(compartmentId);
                if( parentId == null )
                    parent = newDiagram;
                else
                    parent = compartments.get(parentId);
                if( parent == null )
                    continue;
                NodeLayout nodeLayout = id2layout.get(compartmentId);
                if(nodeLayout == null)
                    continue;
                Base kernel = Module.getModulePath(diagram).getChildPath("Data", "Compartment", nodeLayout.getId()).getDataElement(Base.class);
                compartment = new Compartment(parent, kernel);
                compartment.setLocation(nodeLayout.getPosition());
                compartment.setShapeSize(nodeLayout.getShapeSize());
                compartment.setFixed(true);
                compartment.setPredefinedStyle(DiagramElementStyle.STYLE_NOT_SELECTED);
                compartment.getCustomStyle().setBrush(new Brush(nodeLayout.getColor()));
                if(nodeLayout.getDisplayName() != null)
                    compartment.setTitle(nodeLayout.displayName);
                parent.put(compartment);
                compartments.put(compartmentId, compartment);
                newCompartments = true;
            }
        } while( newCompartments );
        /*
         * Next we create nodes cloning the original ones. Note that single node may be cloned several times
         * if clones appear in reactome diagram
         */
        for(Node node : diagram.getNodes())
        {
            String reactId = getInnerId(node);
            if( reactId == null )
                continue;
            List<String> ids = innerId2id.get(reactId);
            if(ids == null)
                continue;
            for(int i=0; i<ids.size(); i++)
            {
                String id = ids.get(i);
                NodeLayout nodeLayout = id2layout.get(id);
                if(nodeLayout == null)
                    continue;
                String compartmentId = component2compartment.get(id);
                Compartment origin;
                if(compartmentId != null)
                {
                    origin = compartments.get(compartmentId);
                } else
                {
                    origin = newDiagram;
                }
                Node newNode = node.clone(origin, node.getName()+(i==0?"":"_"+i));
                if(newNode.getKernel() instanceof Reaction) // Reaction node has no bounds specified, but it's size is defined in notation as 15x15
                    newNode.setLocation(new Point(nodeLayout.getPosition().x-7,nodeLayout.getPosition().y-7));
                else
                    newNode.setLocation(nodeLayout.getPosition());
                newNode.setShapeSize(nodeLayout.getShapeSize());
                newNode.setFixed(true);
                if("process".equals(newNode.getAttributes().getValue(XmlDiagramTypeConstants.XML_TYPE)))
                    newNode.getAttributes().add(new DynamicProperty(XmlDiagramTypeConstants.XML_TYPE, String.class, nodeLayout.getReactionNodeType()));
                if(nodeLayout.getProcessType() != null)
                    newNode.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_PROCESS_TYPE_PD, String.class, nodeLayout.getProcessType()));
                String title = nodeLayout.getDisplayName();
                if(title == null)
                    title = newNode.getKernel().getTitle().replaceFirst("\\s*\\[.+\\]", "");
                newNode.setTitle(title);
                newNode.save();
                nodes.put(id, newNode);
            }
        }
        /*
         * Create process nodes if any
         */
        for(String id: processNodes)
        {
            NodeLayout nodeLayout = id2layout.get(id);
            if(nodeLayout == null)
                continue;
            String compartmentId = component2compartment.get(id);
            Compartment origin;
            if(compartmentId != null)
            {
                origin = compartments.get(compartmentId);
            } else
            {
                origin = newDiagram;
            }
            ReactomeDiagramReference kernel = Module.getModulePath( diagram ).getChildPath( "Data", "Pathway", nodeLayout.getId() )
                    .getDataElement( ReactomeDiagramReference.class );
            Node newNode = new Node(origin, kernel);
            newNode.setLocation(nodeLayout.getPosition());
            newNode.setShapeSize(nodeLayout.getShapeSize());
            newNode.setFixed(true);
            if(nodeLayout.getDisplayName() != null)
                newNode.setTitle(nodeLayout.getDisplayName());
            newNode.save();
            nodes.put(id, newNode);
        }
        /*
         * Finally we create edges. The tricky part here is to determine which clone this edge
         * should be connected to.
         */
        for(DiagramElement de : diagram)
        {
            if(de instanceof Edge)
            {
                Edge edge = ((Edge)de);
                List<String> inputIds = innerId2id.get(getInnerId(edge.getInput()));
                List<String> outputIds = innerId2id.get(getInnerId(edge.getOutput()));
                if(inputIds == null || outputIds == null)
                    continue;
                String inputId = inputIds.get(0);
                String outputId = outputIds.get(0);
                Path path = null;
                String type = null;
                List<EdgeLayout> components = reactionComponents.get(inputId);
                if(components != null)
                {
                    for(EdgeLayout component : components)
                    {
                        if(outputIds.contains(component.getOutputId()))
                        {
                            outputId = component.getOutputId();
                            path = component.getPath();
                            adjustPathEnd(path, true);
                            break;
                        }
                    }
                } else
                {
                    components = reactionComponents.get(outputId);
                    if(components != null)
                    {
                        for(EdgeLayout component : components)
                        {
                            if(inputIds.contains(component.getInputId()))
                            {
                                inputId = component.getInputId();
                                path = component.getPath();
                                type = component.getType();
                                adjustPathEnd(path, false);
                                break;
                            }
                        }
                    }
                }
                if(path == null)
                    continue;
                Edge newEdge = new Edge(edge.getName(), edge.getKernel(), nodes.get(inputId), nodes.get(outputId));
                newEdge.setPath(path);
                for(DynamicProperty dp : edge.getAttributes())
                    newEdge.getAttributes().add(dp);
                if(type != null)
                    newEdge.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_EDGE_TYPE_PD, String.class, type));
                newEdge.save();
                //newDiagram.getType().getSemanticController().recalculateEdgePath(newEdge);
            }
        }
        for(EdgeLayout layout : noteEdges)
        {
            Node input = nodes.get(layout.getInputId());
            Node output = nodes.get(layout.getOutputId());
            if(input == null || output == null)
                continue;
            Edge newEdge = new Edge(new Stub(null, "link", "notelink"), input, output);
            newEdge.setPath(layout.getPath());
            newEdge.save();
        }
        newDiagram.setNotificationEnabled(true);
        return newDiagram;
    }

    private void adjustPathEnd(Path path, boolean start)
    {
        int idx1 = start ? 0 : path.npoints-1;
        int idx2 = start ? 1 : path.npoints-2;
        double dx = path.xpoints[idx2]-path.xpoints[idx1];
        double dy = path.ypoints[idx2]-path.ypoints[idx1];
        double len = Math.sqrt(dx*dx+dy*dy);
        if(len < 8)
        {
            path.xpoints[idx1] = path.xpoints[idx2];
            path.ypoints[idx1] = path.ypoints[idx2];
        } else
        {
            dx = dx*8/len;
            dy = dy*8/len;
            path.xpoints[idx1]+=dx;
            path.ypoints[idx1]+=dy;
        }
    }
    
    private static class EdgeLayout
    {
        private Point[] points;
        private final String inputId;
        private final String outputId;
        private final String type;
        
        public Path getPath()
        {
            int[] x = new int[points.length];
            int[] y = new int[points.length];
            for(int i=0; i<points.length; i++)
            {
                x[i] = (int) ( points[i].x*SCALE_X );
                y[i] = (int) ( points[i].y*SCALE_Y );
            }
            return new Path(x, y, points.length);
        }

        public EdgeLayout(String inputId, String outputId, Point[] points, String type)
        {
            this.inputId = inputId;
            this.outputId = outputId;
            this.points = points;
            this.type = type;
        }

        public void addAfter(Point[] newPoints)
        {
            points = (Point[])ArrayUtils.addAll(points, newPoints);
        }
        
        public void revert()
        {
            ArrayUtils.reverse(points);
        }

        public String getInputId()
        {
            return inputId;
        }

        public String getOutputId()
        {
            return outputId;
        }

        public String getType()
        {
            return edgeTypeMap.get(type);
        }
    }

    private static class NodeLayout
    {
        private final String id;
        private Point position;
        private Dimension shapeSize;
        private Color color;
        private final boolean isCompartment;
        private final String reactionType;
        private String displayName;

        public boolean isCompartment()
        {
            return isCompartment;
        }

        public String getId()
        {
            return id;
        }

        public Point getPosition()
        {
            return position;
        }

        public Dimension getShapeSize()
        {
            return shapeSize;
        }
        
        public Color getColor()
        {
            return color;
        }

        public NodeLayout(Element elem, boolean isCompartment)
        {
            String pos = elem.getAttribute("position");
            String bounds = elem.getAttribute("bounds");
            String bgColor = elem.getAttribute("bgColor");
            Element properties = XmlUtil.getChildElement(elem, "Properties");
            if(properties != null)
            {
                Element displayName = XmlUtil.getChildElement(properties, "displayName");
                if(displayName != null)
                {
                    this.displayName = XmlUtil.getTextContent(displayName);
                }
            }
            reactionType = elem.getAttribute("reactionType");
            this.id = elem.getAttribute("reactomeId");
            this.isCompartment = isCompartment;

            if( !bounds.isEmpty() )
            {
                int[] boundCoords = strToIntArray(bounds);
                shapeSize = new Dimension((int) ( boundCoords[2]*SCALE_X ), (int) ( boundCoords[3]*SCALE_Y ));
                position = new Point((int) ( boundCoords[0]*SCALE_X ), (int) ( boundCoords[1]*SCALE_Y ));
            }
            else
            {
                int[] coords = strToIntArray(pos);
                position = new Point((int) ( coords[0]*SCALE_X ), (int) ( coords[1]*SCALE_Y ));
            }
            if( !bgColor.isEmpty() )
            {
                int[] colorComponents = strToIntArray(bgColor);
                this.color = new Color(colorComponents[0], colorComponents[1], colorComponents[2]);
            }
        }

        public String getReactionNodeType()
        {
            if("Association".equals(reactionType))
                return "association";
            if("Dissociation".equals(reactionType))
                return "dissociation";
            return "process";
        }
        
        public String getProcessType()
        {
            if("Omitted Process".equals(reactionType))
                return "omitted process";
            return null;
        }

        public String getDisplayName()
        {
            return displayName;
        }
    }
    
    static Point[] strToPointArray(String str)
    {
        if(str.isEmpty())
            return new Point[0];
        return StreamEx.split( str, ", " ).map( DiagramLayouter::strToIntArray ).map( ints -> new Point( ints[0], ints[1] ) )
                .toArray( Point[]::new );
    }

    static int[] strToIntArray(String str)
    {
        String[] strs = TextUtil.split(str, ' ');
        int[] ints = new int[strs.length];
        for( int i = 0; i < strs.length; i++ )
        {
            try
            {
                ints[i] = Integer.parseInt(strs[i]);
            }
            catch( NumberFormatException e )
            {
                ints[i] = 0;
            }
        }
        return ints;
    }
}
