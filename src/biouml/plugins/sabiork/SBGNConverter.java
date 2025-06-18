package biouml.plugins.sabiork;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.annotation.Nonnull;

import ru.biosoft.graphics.ComplexTextView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.View;
import ru.biosoft.util.TextUtil2;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramType;
import biouml.model.DiagramTypeConverterSupport;
import biouml.model.DiagramViewBuilder;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.xml.XmlDiagramSemanticController;
import biouml.model.xml.XmlDiagramType;
import biouml.model.xml.XmlDiagramTypeConstants;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.standard.type.Base;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.application.ApplicationUtils;

/**
 * Quick solution for SABIO-RK to SBGN converter
 * TODO: realize it better
 */
public class SBGNConverter extends DiagramTypeConverterSupport
{
    public static final String SBGN_NOTATION_NAME = "sbgn_simulation.xml";
    public static final String SBGN_BASE_DIAGRAM = "baseDiagram";

    @Override
    public Diagram convert(Diagram diagram, Object type) throws Exception
    {
        return super.convert(diagram, SBGN_NOTATION_NAME);
    }

    @Override
    protected Diagram convert(DiagramType diagramType, Diagram diagram) throws Exception
    {
        XmlDiagramType xmlDiagramType = (XmlDiagramType)diagramType;
        ( (XmlDiagramSemanticController)xmlDiagramType.getSemanticController() ).setPrototype(diagram.getType().getSemanticController());
        DiagramViewBuilder viewBuilder = xmlDiagramType.getDiagramViewBuilder();
        viewBuilder.setBaseViewBuilder(diagram.getType().getDiagramViewBuilder());
        viewBuilder.setBaseViewOptions(diagram.getViewOptions());
        Diagram sbgnDiagram = xmlDiagramType.createDiagram(diagram.getOrigin(), diagram.getName(), null);
        sbgnDiagram.setTitle(diagram.getTitle());
        ( (DiagramInfo)sbgnDiagram.getKernel() ).setDescription( ( (DiagramInfo)diagram.getKernel() ).getDescription());
        ( (DiagramInfo)sbgnDiagram.getKernel() ).setDatabaseReferences( ( (DiagramInfo)diagram.getKernel() ).getDatabaseReferences());

        boolean notificationEnabled = sbgnDiagram.isNotificationEnabled();
        sbgnDiagram.setNotificationEnabled(false);

        createElements(diagram, sbgnDiagram);
        createEdges(diagram, sbgnDiagram, sbgnDiagram);

        arrangeDiagram(sbgnDiagram);

        sbgnDiagram.setNotificationEnabled(notificationEnabled);

        return sbgnDiagram;
    }
    protected void createElements(Compartment baseCompartment, Compartment compartment) throws Exception
    {
        List<String> elementNames = baseCompartment.getNameList();
        for( String name : elementNames )
        {
            DiagramElement de = baseCompartment.get(name);
            if( de instanceof Node )
            {
                Base kernel = de.getKernel();
                if( kernel instanceof Reaction )
                {
                    Reaction reaction = (Reaction)kernel;
                    int reactants = 0;
                    int products = 0;
                    int modifiers = 0;
                    for( SpecieReference sr : reaction.getSpecieReferences() )
                    {
                        if( sr.getRole().equals(SpecieReference.REACTANT) )
                        {
                            reactants++;
                        }
                        else if( sr.getRole().equals(SpecieReference.PRODUCT) )
                        {
                            products++;
                        }
                        else if( sr.getRole().equals(SpecieReference.MODIFIER) )
                        {
                            modifiers++;
                        }
                    }
                    String newXmlType;
                    if( reactants > 1 && products == 1 )
                    {
                        newXmlType = "association";
                    }
                    else if( reactants == 1 && products > 1 )
                    {
                        newXmlType = "dissociation";
                    }
                    else
                    {
                        newXmlType = "process";
                    }
                    Node reactionNode = createNodeClone(compartment, (Node)de, name, newXmlType);
                    if( newXmlType.equals("process") )
                    {
                        reactionNode.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_PROCESS_TYPE_PD, String.class, "simple"));
                    }
                    if( reactants == 0 )
                    {
                        String sourceName = de.getName() + "ReactantSource";
                        Node input = new Node(compartment, sourceName, new Stub(null, sourceName, "source-sink"));
                        compartment.put(input);
                        Edge sourceEdge = new Edge(compartment, sourceName + "Edge", new Stub(null, sourceName + "Edge", "consumption"),
                                input, reactionNode);
                        compartment.put(sourceEdge);
                    }
                    if( products == 0 )
                    {
                        String sourceName = de.getName() + "ProductSource";
                        Node output = new Node(compartment, sourceName, new Stub(null, sourceName, "source-sink"));
                        compartment.put(output);
                        Edge sourceEdge = new Edge(compartment, sourceName + "Edge", new Stub(null, sourceName + "Edge", "production"),
                                reactionNode, output);
                        compartment.put(sourceEdge);
                    }
                }
                else
                {
                    Compartment newNode = new Compartment(compartment, name, de.getKernel());
                    newNode.setLocation( ( (Node)de ).getLocation());
                    Map<String, NodeElements> nodeInfoElements = NodeElements.getNodeInfo(de.getTitle());
                    compartment.put(newNode);

                    Map.Entry<String, NodeElements> nodeInfoEntry = nodeInfoElements.entrySet().iterator().next();
                    newNode.setTitle(nodeInfoEntry.getKey());
                    fillNodeInfo(newNode, nodeInfoEntry.getValue());
                    setXmlType(newNode, "entity");
                    newNode.getAttributes().add(
                            new DynamicProperty(SBGNPropertyConstants.SBGN_ENTITY_TYPE_PD, String.class, "macromolecule"));
                    newNode.setShapeSize(new Dimension(0, 0));
                }
            }
        }
    }
    protected void fillNodeInfo(Compartment node, NodeElements nodeInfo) throws Exception
    {
        for( String variable : nodeInfo.getVariables() )
        {
            Node varNode = new Node(node, new Stub(null, variable, "variable"));
            varNode.getAttributes().add(new DynamicProperty("value", String.class, variable));
            node.put(varNode);
        }
        if( nodeInfo.getMultimer() > 1 )
        {
            node.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_MULTIMER_PD, Integer.class, nodeInfo.getMultimer()));
        }
    }

    protected void createEdges(Compartment baseCompartment, Compartment compartment, Diagram sbgnDiagram) throws Exception
    {
        List<String> elementNames = baseCompartment.getNameList();
        for( String name : elementNames )
        {
            DiagramElement de = baseCompartment.get(name);
            if( de instanceof Compartment )
            {
                createEdges((Compartment)de, (Compartment)compartment.get(name), sbgnDiagram);
            }
            else if( de instanceof Edge )
            {
                Edge edge = (Edge)de;
                if( edge.getKernel() instanceof SpecieReference )
                {
                    Node node1 = sbgnDiagram.findNode(edge.getInput().getCompleteNameInDiagram());
                    Node node2 = sbgnDiagram.findNode(edge.getOutput().getCompleteNameInDiagram());
                    if( node1 != null && node2 != null )
                    {
                        SpecieReference sr = (SpecieReference)edge.getKernel();
                        Edge newEdge = null;
                        if( sr.getRole().equals(SpecieReference.REACTANT) )
                        {
                            newEdge = new Edge(edge.getName(), new Stub(null, sr.getName(), "consumption"), node2, node1);
                        }
                        else if( sr.getRole().equals(SpecieReference.PRODUCT) )
                        {
                            newEdge = new Edge(edge.getName(), new Stub(null, sr.getName(), "production"), node1, node2);
                        }
                        else if( sr.getRole().equals(SpecieReference.MODIFIER) )
                        {
                            newEdge = new Edge(edge.getName(), new Stub(null, sr.getName(), "regulation"), node2, node1);
                            newEdge.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_EDGE_TYPE_PD, String.class, "catalysis"));
                        }
                        if( newEdge != null )
                        {
                            newEdge.save();
                        }
                    }
                }
            }
        }
    }
    protected @Nonnull Node createNodeClone(Compartment compartment, Node base, String name, String xmlType) throws Exception
    {
        Node newNode = new Node(compartment, name, base.getKernel());
        setXmlType(newNode, xmlType);
        newNode.setLocation(base.getLocation());
        compartment.put(newNode);
        return newNode;
    }

    protected void setXmlType(DiagramElement de, String type) throws Exception
    {
        de.getAttributes().add(new DynamicProperty(XmlDiagramTypeConstants.XML_TYPE, String.class, type));
    }

    /**
     * Arrange all diagram elements
     */
    protected void arrangeDiagram(Diagram diagram)
    {
        diagram.getType().getDiagramViewBuilder().createDiagramView(diagram, ApplicationUtils.getGraphics());
        arrangeElements(diagram);
    }

    /**
     * Arrange elements in compartment
     */
    protected void arrangeElements(Compartment compartment)
    {
        for(DiagramElement de : compartment)
        {
            if( de instanceof Compartment )
            {
                Compartment node = (Compartment)de;
                Object xmlType = node.getAttributes().getValue(XmlDiagramTypeConstants.XML_TYPE);
                if( xmlType != null )
                {
                    if( "entity".equals(xmlType) )
                    {
                        arrangeEntity(node);
                        arrangeStates(node);
                    }
                    else
                    {
                        arrangeElements(node);
                    }
                }
                else
                {
                    arrangeElements(node);
                }
            }
        }
    }

    /**
     * Arrange entity node
     */
    protected void arrangeEntity(Compartment node)
    {
        CompositeView view = (CompositeView)node.getView();
        Rectangle labelBounds = new Rectangle(0, 0, 30, 20);
        int maxLabelWidth = 0;
        int stateCount = 0;
        for( View childView : view )
        {
            if( childView instanceof ComplexTextView )
            {
                labelBounds = childView.getBounds();
            }
            else if( childView instanceof CompositeView )
            {
                Rectangle bounds = childView.getBounds();
                if( bounds.width > maxLabelWidth )
                {
                    maxLabelWidth = bounds.width;
                }
                stateCount++;
            }
        }
        int height = labelBounds.height;
        if( height < ( stateCount * 20 ) )
        {
            height = stateCount * 20;
        }
        int labelBonus = 0;
        if( maxLabelWidth > 10 )
        {
            labelBonus = maxLabelWidth - 10;
        }
        node.setShapeSize(new Dimension(Math.max(60, labelBounds.width + labelBonus + 20), Math.max(30, height + 20)));
    }

    /**
     * Arrange complex node
     */
    protected void arrangeComplex(Compartment node)
    {
        int height = 15;
        int maxWidth = 60;
        int variableBonus = 0;
        for(DiagramElement childObj : node)
        {
            if( childObj instanceof Compartment )
            {
                Compartment child = (Compartment)childObj;
                arrangeEntity(child);
                child.setLocation(15, height);
                Dimension size = child.getShapeSize();
                height += size.height;
                if( size.width > maxWidth )
                {
                    maxWidth = size.width;
                }
                for(DiagramElement state : child )
                {
                    if( state instanceof Node )
                    {
                        int width = ( (Node)state ).getView().getBounds().width / 2;
                        if( variableBonus < width )
                        {
                            variableBonus = width;
                        }
                    }
                }
            }
        }
        for(DiagramElement childObj : node)
        {
            if( childObj instanceof Compartment )
            {
                Compartment child = (Compartment)childObj;
                Dimension size = child.getShapeSize();
                if( size.width < maxWidth )
                {
                    child.setShapeSize(new Dimension(maxWidth, size.height));
                }
                arrangeStates(child);
            }
        }
        if( variableBonus > 5 )
        {
            variableBonus -= 5;
        }
        node.setShapeSize(new Dimension(maxWidth + variableBonus + 30, height + 15));
    }

    /**
     * Arrange states
     */
    protected void arrangeStates(Compartment node)
    {
        int i = 0;
        for(DiagramElement childObj : node)
        {
            if( childObj instanceof Node )
            {
                Node state = (Node)childObj;
                int nodeTop = node.getLocation().y;
                int nodeRight = node.getLocation().x + node.getShapeSize().width;
                state.setLocation(nodeRight, nodeTop + 20 + ( 20 * i ));
                i++;
            }
        }
    }

    public static class NodeElements
    {
        protected String title;
        protected String[] variables;
        protected int multimer;

        public static Map<String, NodeElements> getNodeInfo(String nodeTitle)
        {
            Map<String, NodeElements> result = new HashMap<>();
            String[] subElements = TextUtil2.split( nodeTitle, ':' );
            for( String subElement : subElements )
            {
                NodeElements element = new NodeElements();
                StringTokenizer st = new StringTokenizer(subElement, "{");
                element.setTitle(st.nextToken());
                List<String> variablesList = new ArrayList<>();
                while( st.hasMoreTokens() )
                {
                    String token = st.nextToken();
                    int ind = token.indexOf('}');
                    if( ind != -1 )
                    {
                        variablesList.add(token.substring(0, ind));
                        if( ind < token.length() - 1 )
                        {
                            try
                            {
                                element.setMultimer(Integer.parseInt(token.substring(ind + 1)));
                            }
                            catch( NumberFormatException e )
                            {
                            }
                        }
                    }
                }
                element.setVariables(variablesList.toArray(new String[variablesList.size()]));
                result.put(element.getTitle(), element);
            }
            return result;
        }
        public String getTitle()
        {
            return title;
        }

        public void setTitle(String title)
        {
            this.title = title;
        }

        public void setVariables(String[] variables)
        {
            this.variables = variables;
        }

        public void setMultimer(int multimer)
        {
            this.multimer = multimer;
        }

        public String[] getVariables()
        {
            return variables;
        }

        public int getMultimer()
        {
            return multimer;
        }
    }
}
