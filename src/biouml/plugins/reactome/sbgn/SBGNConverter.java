package biouml.plugins.reactome.sbgn;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.graphics.ComplexTextView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.View;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramType;
import biouml.model.DiagramTypeConverterSupport;
import biouml.model.DiagramViewBuilder;
import biouml.model.DiagramViewOptions;
import biouml.model.Edge;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.VariableRole;
import biouml.model.xml.XmlDiagramSemanticController;
import biouml.model.xml.XmlDiagramType;
import biouml.model.xml.XmlDiagramTypeConstants;
import biouml.model.xml.XmlDiagramViewOptions;
import biouml.plugins.reactome.ReactomeProteinTableType;
import biouml.plugins.reactome.ReactomeSqlUtils;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.standard.type.Base;
import biouml.standard.type.BaseSupport;
import biouml.standard.type.Complex;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.application.ApplicationUtils;

/**
 * Pathway/SBGN converter
 */
public class SBGNConverter extends DiagramTypeConverterSupport
{
    private static final String SBGN_NOTATION_NAME = "sbgn_simulation.xml";
    private static final @Nonnull DataElementPath REACTOME_MODULE = DataElementPath.create( "databases/Reactome" );

    @Override
    public Diagram convert(Diagram diagram, Object type) throws Exception
    {
        return super.convert(diagram, SBGN_NOTATION_NAME);
    }

    @Override
    protected Diagram convert(DiagramType diagramType, Diagram diagram) throws Exception
    {
        DataCollection<?> dc = REACTOME_MODULE.optDataCollection();
        if( dc == null )
        {
            log.log(Level.SEVERE, "Can not find Reactome data collection. Diagram " + diagram.getCompletePath() + " is not converted");
            return diagram;
        }
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
        updateDiagramModel(sbgnDiagram, sbgnDiagram);
        sbgnDiagram.setNotificationEnabled(notificationEnabled);

        sbgnDiagram.getAttributes().add(new DynamicProperty(ReferenceTypeRegistry.REFERENCE_TYPE_PD, String.class, ReferenceTypeRegistry.getReferenceType(ReactomeProteinTableType.class).toString()));
        DynamicPropertySet options = ((XmlDiagramViewOptions)sbgnDiagram.getViewOptions()).getOptions();
        options.setValue( "customTitleFont", options.getValue( "nodeTitleFont" ) );
        return sbgnDiagram;
    }

    /**
     * Transforms diagram element to a list of SBGN diagram elements
     */
    @Override
    public DiagramElement[] convertDiagramElement(DiagramElement de, Diagram diagram) throws Exception
    {
        Base kernel = de.getKernel();
        if( kernel == null || kernel.getOrigin() == null  || !( kernel.getOrigin().getCompletePath().isDescendantOf(REACTOME_MODULE) ) )
            return null;

        DataCollection<?> parent = de.getOrigin();
        if( parent instanceof Compartment )
        {
            if( de instanceof Node )
            {
                List<DiagramElement> converted = getSBGNNodes((Compartment)parent, de.getName(), de.getKernel(),
                        ( (Node)de ).getLocation(), getElementTypeAttribute());
                DiagramViewBuilder viewBuilder = diagram.getType().getDiagramViewBuilder();
                DiagramViewOptions viewOptions = diagram.getViewOptions();
                Graphics graphics = ApplicationUtils.getGraphics();
                for( DiagramElement elem : converted )
                {
                    if( elem instanceof Compartment )
                    {
                        viewBuilder.createCompartmentView((Compartment)elem, viewOptions, graphics);
                    }
                    else if( elem instanceof Node )
                    {
                        viewBuilder.createNodeView((Node)elem, viewOptions, graphics);
                    }
                }
                arrangeElements(converted.iterator());
                return converted.toArray(new DiagramElement[converted.size()]);
            }
            else if( de instanceof Edge && kernel instanceof SpecieReference )
            {
                String role = ( (SpecieReference)kernel ).getRole();
                String edgeType = getEdgeType(role);
                String sbgnEdgeType = getSbgnEdgeType(role);
                if( !sbgnEdgeType.isEmpty() )
                    de.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_EDGE_TYPE_PD, String.class, sbgnEdgeType));
                de.getAttributes().add(new DynamicProperty(XmlDiagramTypeConstants.KERNEL_ROLE_ATTR_PD, String.class, role));
                setXmlType(de, edgeType);
                return new DiagramElement[] {de};
            }
        }
        return null;
    }


    private void createElements(Compartment baseCompartment, Compartment compartment) throws Exception
    {
        List<String> elementNames = baseCompartment.getNameList();
        for( String name : elementNames )
        {
            DiagramElement de = baseCompartment.get(name);
            if( de instanceof Compartment )
            {
                Compartment newCompartment = new Compartment(compartment, name, de.getKernel());
                setXmlType(newCompartment, "compartment");
                newCompartment.setLocation( ( (Compartment)de ).getLocation());
                newCompartment.setShapeSize( ( (Compartment)de ).getShapeSize());
                newCompartment.setTitle(de.getTitle());
                compartment.put(newCompartment);
                createElements((Compartment)de, newCompartment);
            }
            else if( de instanceof Node )
            {
                addNode(compartment, name, de.getKernel(), ( (Node)de ).getLocation(), getElementTypeAttribute());
            }
        }
    }

    private static Node addNode(Compartment compartment, String name, Base kernel, Point location, String typeAttr) throws Exception
    {
        List<DiagramElement> nodes = getSBGNNodes(compartment, name, kernel, location, typeAttr);
        if( nodes == null || nodes.size() == 0 || ! ( nodes.get(0) instanceof Node ) )
            return null;
        for( DiagramElement el : nodes )
        {
            compartment.put(el);
        }

        return (Node)nodes.get(0);
    }

    private static List<DiagramElement> getSBGNNodes(Compartment compartment, String name, Base kernel, Point location, String typeAttr)
            throws Exception
    {
        List<DiagramElement> nodes = new ArrayList<>();
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
            Stub kernelStub = new Stub(null, reaction.getName(), Type.TYPE_REACTION);
            Node reactionNode = createNodeClone(compartment, name, kernelStub, location, newXmlType);
            reactionNode.getAttributes().add(new DynamicProperty(DataCollectionConfigConstants.COMPLETE_NAME_PROPERTY, String.class, reaction.getCompletePath().toString()));
            reactionNode.setTitle(reaction.getTitle());
            if( newXmlType.equals("process") )
            {
                reactionNode.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_PROCESS_TYPE_PD, String.class, "simple"));
            }
            Equation rule = new Equation(reactionNode, Equation.TYPE_SCALAR, "$$rate_" + reactionNode.getName(), reaction.getFormula());
            reactionNode.setRole(rule);
            nodes.add(reactionNode);
            if( reactants == 0 )
            {
                String sourceName = name + "ReactantSource";
                Node input = new Node(compartment, sourceName, new Stub(null, sourceName, "source-sink"));
                nodes.add(input);
                Edge sourceEdge = new Edge(compartment, sourceName + "Edge", new Stub(null, sourceName + "Edge", "consumption"), input,
                        reactionNode);
                nodes.add(sourceEdge);
            }
            if( products == 0 )
            {
                String sourceName = name + "ProductSource";
                Node output = new Node(compartment, sourceName, new Stub(null, sourceName, "source-sink"));
                nodes.add(output);
                Edge sourceEdge = new Edge(compartment, sourceName + "Edge", new Stub(null, sourceName + "Edge", "production"),
                        reactionNode, output);
                nodes.add(sourceEdge);
            }
        }
        else
        {
            boolean isComplex = checkComplex(kernel);
            Compartment newNode = new Compartment(compartment, name, kernel);
            newNode.setLocation(location);
            VariableRole var = new VariableRole(newNode, 0);
            newNode.setRole(var);
            nodes.add(newNode);

            if( isComplex )
            {
                setXmlType(newNode, "complex");
                newNode.setTitle(getCorrectedTitle(newNode.getTitle()));

                List<ru.biosoft.access.core.DataElementPath> sts = getComplexComponents(kernel);
                if( sts != null && sts.size() < 5 )
                {
                    for( DataElementPath component : sts )
                    {
                        String childName = component.getName();
                        DataElement childDE = component.optDataElement();
                        String shildTitle = childName;
                        if( childDE instanceof BaseSupport )
                        {
                            shildTitle = ( (BaseSupport)childDE ).getTitle();
                            shildTitle = getCorrectedTitle(shildTitle);
                        }
                        Compartment child = new Compartment(newNode, new Stub(null, childName, "entity"));
                        child.setTitle(shildTitle);
                        child.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_ENTITY_TYPE_PD, String.class, typeAttr));
                        newNode.put(child);

                        child.setShapeSize(new Dimension(0, 0));
                    }
                }
            }
            else
            {
                setXmlType(newNode, "entity");
                newNode.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_ENTITY_TYPE_PD, String.class, typeAttr));
                newNode.setShapeSize(new Dimension(0, 0));
            }
        }
        return nodes;
    }

    private static List<ru.biosoft.access.core.DataElementPath> getComplexComponents(Base kernel)
    {
        DataCollection<?> dc = kernel.getOrigin();
        DataElementPath dataPath = REACTOME_MODULE.getChildPath(Module.DATA);
        if( kernel instanceof Complex )
        {
            String[] comp = ( (Complex)kernel ).getComponents();
            List<ru.biosoft.access.core.DataElementPath> components = new ArrayList<>();
            for( String c : comp )
            {
                components.add(REACTOME_MODULE.getRelativePath(c));
            }
            return components;
        }
        Connection connection = getConnection(dc);
        Object idObj = kernel.getAttributes().getValue("InnerID");
        if( idObj == null )
            return null;
        try(Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(
                    "SELECT identifier, _class, hasComponent_rank FROM " + ReactomeSqlUtils.databaseObjectTable
                            + " do JOIN StableIdentifier si ON (si.DB_ID=do.stableIdentifier) "
                            + "WHERE do.DB_ID IN (SELECT hasComponent FROM Complex_2_hasComponent chc WHERE chc.DB_ID="
                            + idObj.toString() + " ORDER BY hasComponent_rank)"))
        {
            List<ru.biosoft.access.core.DataElementPath> components = new ArrayList<>();
            while( rs.next() )
            {
                components.add(dataPath.getChildPath(ReactomeSqlUtils.getCollectionNameByClass(rs.getString(2)), rs.getString(1)));
            }
            return components;
        }
        catch( SQLException e )
        {
        }
        return null;
    }
    private static String getCorrectedTitle(String title)
    {
        //String newtitle = title.replaceAll("\\([\\w\\.]+\\)", "");
        return title;
    }

    private static Edge addEdge(@Nonnull Node node1, @Nonnull Node node2, String name, SpecieReference sr) throws Exception
    {
        String edgeType = getEdgeType(sr.getRole());
        Edge edge = name == null ? new Edge(sr, node1, node2) : new Edge(name, sr, node1, node2);
        String sbgnEdgeType = getSbgnEdgeType(sr.getRole());
        if( !sbgnEdgeType.isEmpty() )
            edge.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_EDGE_TYPE_PD, String.class, sbgnEdgeType));
        edge.getAttributes().add(new DynamicProperty(XmlDiagramTypeConstants.KERNEL_ROLE_ATTR_PD, String.class, sr.getRole()));
        setXmlType(edge, edgeType);
        edge.save();
        return edge;
    }

    private void createEdges(Compartment baseCompartment, Compartment compartment, Diagram sbgnDiagram) throws Exception
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
                        addEdge(node1, node2, edge.getName(), sr);
                    }
                }
            }
        }
    }

    private static Node createNodeClone(Compartment compartment, String name, Base kernel, Point location, String xmlType) throws Exception
    {
        Node newNode = new Node(compartment, name, kernel);
        setXmlType(newNode, xmlType);
        newNode.setLocation(location);
        return newNode;
    }

    private String getElementTypeAttribute()
    {
        return "macromolecule";
    }

    private static void setXmlType(DiagramElement de, String type) throws Exception
    {
        de.getAttributes().add(new DynamicProperty(XmlDiagramTypeConstants.XML_TYPE, String.class, type));
    }

    /**
     * Arrange diagram elements: correct positions of inner components, correct shape size etc.
     */
    private static void arrangeDiagram(Diagram diagram)
    {
        diagram.getType().getDiagramViewBuilder().createDiagramView(diagram, ApplicationUtils.getGraphics());
        arrangeElements(diagram.iterator());
        diagram.setView(null);
        diagram.getType().getDiagramViewBuilder().createDiagramView(diagram, ApplicationUtils.getGraphics());
    }

    /**
     * Arrange elements in compartment
     */
    private static void arrangeElements(Iterator<DiagramElement> iter)
    {
        while( iter.hasNext() )
        {
            Object element = iter.next();
            if( element instanceof Compartment )
            {
                Compartment node = (Compartment)element;
                Object xmlType = node.getAttributes().getValue(XmlDiagramTypeConstants.XML_TYPE);
                if( xmlType != null )
                {
                    if( "complex".equals(xmlType) )
                    {
                        arrangeComplex(node);
                    }
                    else if( "entity".equals(xmlType) )
                    {
                        arrangeEntity(node);
                    }
                    else
                    {
                        arrangeElements(node.iterator());
                    }
                }
                else
                {
                    arrangeElements(node.iterator());
                }
            }
            else if( element instanceof Node )
            {
                Node node = (Node)element;
                Object xmlType = node.getAttributes().getValue(XmlDiagramTypeConstants.XML_TYPE);
                if( xmlType != null )
                {
                    if( "process".equals(xmlType) || "association".equals(xmlType) || "dissociation".equals(xmlType) )
                    {
                        arrangeReaction(node);
                    }
                }
            }
        }
    }

    /**
     * Arrange complex node
     */
    private static void arrangeComplex(Compartment node)
    {
        int height = 15;
        int maxWidth = 60;
        for(DiagramElement de: node)
        {
            if( de instanceof Compartment )
            {
                Compartment child = (Compartment)de;
                arrangeEntity(child);
                child.setLocation(node.getLocation().x + 15, node.getLocation().y + height);
                Dimension size = child.getShapeSize();
                height += size.height;
                if( size.width > maxWidth )
                {
                    maxWidth = size.width;
                }
            }
        }
        for(DiagramElement de: node)
        {
            if( de instanceof Compartment )
            {
                Compartment child = (Compartment)de;
                Dimension size = child.getShapeSize();
                if( size.width < maxWidth )
                {
                    child.setShapeSize(new Dimension(maxWidth, size.height));
                }
            }
        }
        CompositeView view = (CompositeView)node.getView();
        //check if complex has title
        Rectangle labelBounds = new Rectangle(0, 0, 0, 0);
        for( View childView : view )
        {
            if( childView instanceof ComplexTextView )
            {
                labelBounds = childView.getBounds();
                break;
            }
        }
        node.setShapeSize(new Dimension(maxWidth + 30, height + labelBounds.height + 15));
    }

    /**
     * Arrange entity node
     */
    private static void arrangeEntity(Compartment node)
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
     * Arrange reaction nodes for layout issues
     */
    private static void arrangeReaction(Node node)
    {
        Rectangle bounds = node.getView().getBounds();
        node.setShapeSize(new Dimension(bounds.width, bounds.height));
    }

    private static boolean checkComplex(Base kernel)
    {
        String originName = kernel.getOrigin().getCompletePath().getName();
        if( originName.endsWith("Complex") )
            return true;
        return false;
    }

    private static Connection getConnection(DataCollection<?> dc)
    {
        return DataCollectionUtils.getSqlConnection(dc);
    }

    private static String getEdgeType(String role)
    {
        if( role.equals(SpecieReference.REACTANT) )
        {
            return "consumption";
        }
        else if( role.equals(SpecieReference.PRODUCT) )
        {
            return "production";
        }
        else if( role.equals(SpecieReference.MODIFIER) )
        {
            return "regulation";
        }
        return "";
    }

    private static String getSbgnEdgeType(String role)
    {
        if( role.equals(SpecieReference.MODIFIER) )
        {
            return "catalysis";
        }
        return "";
    }
}
