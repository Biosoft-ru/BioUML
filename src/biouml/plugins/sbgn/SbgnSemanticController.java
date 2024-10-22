package biouml.plugins.sbgn;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.InitialElementProperties;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.SubDiagram;
import biouml.model.SubDiagram.PortOrientation;
import biouml.model.dynamics.Constraint;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Function;
import biouml.model.dynamics.SimpleTableElement;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.sbml.SbmlConstants;
import biouml.plugins.sbml.SbmlSupport;
import biouml.standard.diagram.CompositeSemanticController;
import biouml.standard.diagram.CreateEdgeAction;
import biouml.standard.diagram.CreatorElementWithName;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.PortLinkCreator;
import biouml.standard.diagram.PortProperties;
import biouml.standard.diagram.SimpleBusProperties;
import biouml.standard.diagram.SimpleTableElementProperties;
import biouml.standard.diagram.SubDiagramProperties;
import biouml.standard.diagram.Util;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Stub.ConnectionPort;
import biouml.standard.type.Stub.Note;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.util.DPSUtils;
import ru.biosoft.util.PropertiesDialog;

/**@author Ilya*/
public class SbgnSemanticController extends CompositeSemanticController implements CreatorElementWithName
{
    @SuppressWarnings ( "serial" )
    protected static final Map<Class<?>, String> classToType = new HashMap<Class<?>, String>()
    {
        {
            put(Equation.class, Type.TYPE_EQUATION);
            put(Function.class, Type.TYPE_FUNCTION);
            put(Event.class, Type.TYPE_EVENT);
            put(Constraint.class, Type.TYPE_CONSTRAINT);
            put(LogicalOperatorProperties.class, Type.TYPE_LOGICAL);
        }
    };

    @SuppressWarnings ( "serial" )
    protected static final Map<String, String> typeToName = new HashMap<String, String>()
    {
        {
            put(Type.TYPE_EVENT, "event");
            put(Type.TYPE_FUNCTION, "f");
            put(Type.TYPE_EQUATION, "equation");
            put(Type.TYPE_CONSTRAINT, "constraint");
            put(Type.TYPE_VARIABLE, "value");
            put(Type.TYPE_UNIT_OF_INFORMATION, "p");
        }
    };

    /**list of types for which SBO term should not be applied**/
    @SuppressWarnings ( "serial" )
    protected static final Set<String> nonSBOElements = new HashSet<String>()
    {
        {
            add(Type.TYPE_NOTE);
            add(Type.TYPE_NOTELINK);
            add(Type.TYPE_LOGICAL);
            add(Type.TYPE_REGULATION);
            add(Type.TYPE_VARIABLE);
            add(Type.TYPE_UNIT_OF_INFORMATION);
            add(Type.TYPE_TABLE);
            add(biouml.standard.type.Type.TYPE_CONNECTION_BUS);
        }
    };

    @Override
    public DiagramElementGroup createInstance(@Nonnull
    Compartment parent, Object type, Point point, ViewEditorPane viewEditor)
    {
        boolean isNotificationEnabled = parent.isNotificationEnabled();
        parent.setNotificationEnabled(false);

        try
        {
            if( type.equals(Type.TYPE_PORTLINK) )
            {
                new CreateEdgeAction().createEdge(point, viewEditor, new PortLinkCreator());
                return null;
            }
            else if( !createByParent(type) )
            {
                Object properties = getPropertiesByType(parent, type, point);
                PropertiesDialog dialog = new PropertiesDialog(Application.getApplicationFrame(), "New element", properties);
                if( dialog.doModal() )
                {
                    if( properties instanceof InitialElementProperties )
                        ( (InitialElementProperties)properties ).createElements(parent, point, viewEditor);
                    return DiagramElementGroup.EMPTY_EG;
                }
            }
            else
            {
                return super.createInstance(parent, type, point, viewEditor);
            }
        }
        catch( Throwable t )
        {
            throw ExceptionRegistry.translateException(t);
        }
        finally
        {
            parent.setNotificationEnabled(isNotificationEnabled);
        }
        return DiagramElementGroup.EMPTY_EG;
    }

    @Override
    public DiagramElementGroup createInstance(@Nonnull
    Compartment compartment, Object type, Point point, Object properties)
    {
        String name = type.equals(Reaction.class) ? generateUniqueName(compartment, "reaction")
                : type.toString().substring(type.toString().lastIndexOf(".") + 1);
        if( properties instanceof Reaction )
            name = generateUniqueName(compartment, ( (Reaction)properties ).getName());
        else if( properties instanceof PortProperties )
            name = generateUniqueName(compartment, ( (PortProperties)properties ).getName());
        else if( classToType.get(type) != null )
            name = generateUniqueName(compartment, typeToName.get(classToType.get(type)));
        return createInstance(compartment, type, name, point, properties);
    }

    @Override
    public DiagramElementGroup createInstance(@Nonnull
    Compartment compartment, Object type, String name, Point point, Object properties)
    {
        try
        {
            if( type.equals(Specie.class) )
            {
                return new DiagramElementGroup(createDiagramElement(Type.TYPE_MACROMOLECULE, name, compartment));
            }
            else if( type.equals(biouml.standard.type.Compartment.class) )
            {
                return new DiagramElementGroup(createDiagramElement(Type.TYPE_COMPARTMENT, name, compartment));
            }
            else if( type.equals(Reaction.class) )
            {
                if( properties instanceof Reaction )
                {
                    Reaction oldReaction = (Reaction)properties;
                    List<SpecieReference> components = Arrays.asList(oldReaction.getSpecieReferences());
                    SbgnReactionProperties reactionProperties = new SbgnReactionProperties(name, oldReaction.getKineticLaw(), components);
                    reactionProperties.setReactionTitle(oldReaction.getTitle());
                    DiagramElementGroup elements = reactionProperties.createElements(compartment, point, null);
                    reactionProperties.putReaction();
                    return elements;
                    //TODO: put results outside this method
                    //reactionProperties.putResults(elements);
                    //return elements.stream().filter(Util::isReaction).findAny().orElse(null); //return reaction here TODO: maybe return all elements
                }
                else if( properties instanceof SbgnReactionProperties )
                {
                    SbgnReactionProperties reactionProperties = (SbgnReactionProperties)properties;
                    DiagramElementGroup elements = reactionProperties.createElements(compartment, point, null);
                    reactionProperties.putReaction();
                    return elements;
                }
            }
            else if( type.equals(LogicalOperatorProperties.class) )
            {
                LogicalOperatorProperties logOP = (LogicalOperatorProperties)properties;
                DiagramElementGroup elements = logOP.doCreateElements(compartment, point, null);
                return elements;
            }
            else if( type.equals(SimpleTableElementProperties.class) )
            {
                SimpleTableElementProperties steb = (SimpleTableElementProperties)properties;
                DiagramElementGroup elements = steb.doCreateElements(compartment, point, null);
                return elements;
            }
            else if( type.equals(PhenotypeProperties.class) )
            {
                PhenotypeProperties phenProps = (PhenotypeProperties)properties;
                DiagramElementGroup elements = phenProps.doCreateElements(compartment, point, null);
                return elements;
            }
            else if( type.equals(EquivalenceOperatorProperties.class) )
            {
                EquivalenceOperatorProperties eop = (EquivalenceOperatorProperties)properties;
                DiagramElementGroup elements = eop.doCreateElements(compartment, point, null);
                return elements;
            }
            else if( type.equals(Note.class) )
            {
                return ( (InitialElementProperties)properties ).createElements(compartment, point, null);
            }
            else if( properties instanceof SimpleBusProperties )
            {
                SimpleBusProperties sbp = (SimpleBusProperties)properties;
                DiagramElementGroup elements = sbp.createElements(compartment, point, null);
                return elements;

            }
            else if( type.equals(ConnectionPort.class) )
            {
                if( properties instanceof PortProperties )
                {
                    PortProperties portProperties = (PortProperties)properties;

                    SbgnPortProperties sbgnPortProperties = new SbgnPortProperties(Diagram.getDiagram(compartment), null);
                    sbgnPortProperties.setVarName(portProperties.getVarName());
                    sbgnPortProperties.setName(portProperties.getName());
                    sbgnPortProperties.setPortType(portProperties.getPortType());
                    //TODO: put results outside this method
                    DiagramElementGroup elements = sbgnPortProperties.createElements(compartment, point, null);
                    return elements;
                }
            }
            else if( type.equals(SubDiagram.class) && properties.equals(SubDiagramProperties.class) )
            {
                SubDiagramProperties subDiagramProperties = (SubDiagramProperties)properties;
                SbgnSubDiagramProperties sbgnProperties = new SbgnSubDiagramProperties(Diagram.getDiagram(compartment));
                sbgnProperties.setExternal(subDiagramProperties.isExternal());
                sbgnProperties.setModelDefinitionName(subDiagramProperties.getModelDefinitionName());
                sbgnProperties.setName(subDiagramProperties.getName());
                sbgnProperties.setDiagramPath(subDiagramProperties.getDiagramPath());
                DiagramElementGroup elements = sbgnProperties.createElements(compartment, point, null);
                return elements;
            }
            else if( classToType.get(type) != null )
            {
                Node node = new Node(compartment, new Stub(null, name, classToType.get(type)));
                Role role = (Role) ( ! ( properties instanceof Role )
                        ? ( (Class<?>)type ).getConstructor(DiagramElement.class).newInstance(node)
                        : (Role) ( (Role)properties ).clone(node) );
                node.setRole(role.clone(node));
                if( point != null )
                    node.setLocation(point);
                setNeccessaryAttributes(node);
                return new DiagramElementGroup(node);
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "While creating instance of type " + type.toString(), e);
        }
        return DiagramElementGroup.EMPTY_EG;
    }

    @Override
    public boolean canAccept(Compartment parent, DiagramElement de)
    {
        String parentType = parent.getKernel().getType();
        String deType = de.getKernel().getType();
        Compartment currentParent = de.getCompartment();

        //variable and info unit can be only inside specie or compartment and can not be moved from one node to another
        if( deType.equals(Type.TYPE_VARIABLE) || deType.equals(Type.TYPE_UNIT_OF_INFORMATION) )
            return ( parent.getKernel() instanceof Specie || parent.getKernel() instanceof biouml.standard.type.Compartment )
                    && currentParent.equals(parent);

        //only specie can be inside complex and it can not be moved from one complex to another
        if( parentType.equalsIgnoreCase(Type.TYPE_COMPLEX) || currentParent.getKernel().getType().equals(Type.TYPE_COMPLEX) )
            return de.getKernel() instanceof Specie && currentParent.equals(parent);

        //can not move equations, functions and events to compartment
        if( parentType.equals(Type.TYPE_COMPARTMENT) )
            return !SBGNPropertyConstants.mathTypesFull.contains(deType);

        if( parent.getKernel() instanceof Specie )
            return false;

        return true;
    }

    @Override
    public Dimension move(DiagramElement de, Compartment newParent, Dimension offset, Rectangle oldBounds) throws Exception
    {
        if( de.getKernel().getType().equals(Type.TYPE_UNIT_OF_INFORMATION) || de.getKernel().getType().equals(Type.TYPE_VARIABLE) )
        {
            Node node = (Node)de;
            Compartment parent = (Compartment)node.getOrigin();
            if( newParent != parent )
                newParent = parent;
            Point oldLocation = node.getLocation();
            Point newLocation = new Point(oldLocation);
            newLocation.translate(offset.width, offset.height);
            SbgnUtil.moveToEdge(node, new Rectangle(newParent.getLocation(), newParent.getShapeSize()), newLocation);
            node.setLocation(newLocation);
            return new Dimension(oldLocation.x - newLocation.x, oldLocation.y - newLocation.y);
        }
        return super.move(de, newParent, offset, oldBounds);
    }

    @Override
    public boolean remove(DiagramElement de) throws Exception
    {
        if( de instanceof Diagram )
            return false;

        SbgnDiagramViewOptions options = (SbgnDiagramViewOptions)Diagram.getDiagram(de).getViewOptions();
        //remove ports
        if( de instanceof Node && de.getKernel() instanceof Specie || de.getKernel() instanceof biouml.standard.type.Compartment )
        {
            Node node = (Node)de;
            for( Edge e : node.edges().filter(e -> Util.isPortLink(e)) )
                remove(e.getOtherEnd(node));
        }

        if( de.getKernel() instanceof Specie && !options.isAutoDeleteReactions() )
        {
            Node node = (Node)de;
            for( Edge e : node.getEdges() )
            {
                Node reactionNode = e.getOtherEnd((Node)de);
                if( Util.isReaction(reactionNode) )
                {
                    Reaction reaction = (Reaction)reactionNode.getKernel();
                    reaction.remove(e.getKernel().getName()); //remove specie reference
                    reactionNode.removeEdge(e);
                    if( StreamEx.of(reaction.getSpecieReferences()).filter(sr -> sr.isReactantOrProduct()).count() == 0 )//TODO: if only have source sink also delete reaction
                        remove(reactionNode);
                    else
                        SbgnUtil.generateSourceSink(reactionNode, true); //otherwise add source sinks
                }
            }
            removeWithEdges(node);
        }
        else if( de.getKernel() instanceof Reaction )
        {
            Node node = (Node)de;
            List<Node> nodesToRemove = node.edges().map(e -> e.getOtherEnd(node)).filter(n -> SbgnUtil.isLogical(n) || Util.isSourceSink(n))
                    .append(node).toList();
            for( Node nodeToRemove : nodesToRemove )
                removeWithEdges(nodeToRemove);
        }
        else if( SbgnUtil.isLogical(de) )
        {
            for( SpecieReference sr : ( (Node)de ).edges().map(Edge::getKernel).select(SpecieReference.class)
                    .filter(sr -> sr.getOrigin() instanceof Reaction) )
                sr.getOrigin().remove(sr.getName());
            removeWithEdges((Node)de);
        }
        else if( Util.isSourceSink(de) )
        {
            if( !options.isAddSourceSink() )
                removeWithEdges((Node)de);
            else
            {
                for( Edge e : ( (Node)de ).getEdges() )
                {
                    if( e.getKernel() instanceof Stub && ( Type.TYPE_CONSUMPTION.equals(e.getKernel().getType())
                            || Type.TYPE_PRODUCTION.equals(e.getKernel().getType()) ) )
                        return false;
                }
            }
        }
        else if( de instanceof Edge && Util.isPortLink((Edge)de) )
        {
            de.getOrigin().remove(de.getName());
            return true;
        }
        return super.remove(de);
    }

    private static void removeWithEdges(Node node) throws Exception
    {
        node.getOrigin().remove(node.getName());
        Edge[] edges = node.getEdges();
        for( Edge e : edges )
        {
            node.removeEdge(e);
            e.getOrigin().remove(e.getName());
        }
    }

    /**Decides if diagram element of this type should be created by superclass */
    public boolean createByParent(Object type)
    {
        return type instanceof Class;
    }

    public static void setNeccessaryAttributes(DiagramElement de)
    {
        String type = de.getKernel().getType();
        DynamicPropertySet dps = SbgnSemanticController.getDPSByType(type);

        dps.remove(SBGNPropertyConstants.SBGN_ENTITY_TYPE);

        if( de.getKernel() instanceof ConnectionPort )
        {
            dps.add(DPSUtils.createTransient(SbmlConstants.SBO_TERM_ATTR, String.class, SbgnUtil.generateSBOTerm(de)));
            dps.add(DPSUtils.createReadOnly(ConnectionPort.VARIABLE_NAME_ATTR, String.class, ""));
            dps.add(new DynamicProperty(ConnectionPort.PORT_ORIENTATION, PortOrientation.class, PortOrientation.RIGHT));
            dps.add(new DynamicProperty(SBGNPropertyConstants.SBGN_ACCESS_TYPE_PD, String.class, ConnectionPort.PUBLIC));
        }

        if( Type.TYPE_REGULATION.equals(de.getKernel().getType()) )
            dps.add(new DynamicProperty(SBGNPropertyConstants.SBGN_EDGE_TYPE_PD, String.class, Type.TYPE_CATALYSIS));

        if( de.getKernel() instanceof Specie && de.getCompartment().getKernel() instanceof Specie )
            dps.remove(Util.COMPLEX_STRUCTURE);

        for( DynamicProperty dp : dps )
        {
            DynamicProperty oldDp = de.getAttributes().getProperty(dp.getName());
            if( oldDp == null || !oldDp.getType().equals(dp.getType()) )//some sophisticated creation procedures may have already set some properties
                de.getAttributes().add(dp);
        }
    }

    @Override
    public Object getPropertiesByType(Compartment compartment, Object type, Point point)
    {
        if( type instanceof Class )
        {
            if( Reaction.class.isAssignableFrom((Class<?>)type) )
                return new SbgnReactionProperties();
            else if( SubDiagram.class.isAssignableFrom((Class<?>)type) )
                return new SbgnSubDiagramProperties(Diagram.getDiagram(compartment));
        }

        if( ! ( type instanceof String ) )
            return super.getPropertiesByType(compartment, type, point);

        String name = generateUniqueName(Diagram.getDiagram(compartment), getNameByType((String)type), false);
        if( Type.TYPE_PHENOTYPE.equals(type) )
            return new PhenotypeProperties(Diagram.getDiagram(compartment), name);
        else if( Type.TYPE_PORT.equals(type) )
            return new SbgnPortProperties(Diagram.getDiagram(compartment), ConnectionPort.class);
        else if( Type.TYPE_LOGICAL.equals(type) )
            return new LogicalOperatorProperties(Diagram.getDiagram(compartment), name);
        else if( Type.TYPE_EQUIVALENCE.equals(type) )
            return new EquivalenceOperatorProperties(Diagram.getDiagram(compartment), name);
        else if( Type.TYPE_TABLE.equals(type) )
            return new SimpleTableElementProperties(name);
        return new SbgnElementProperties((String)type, name);
    }

    protected static String getNameByType(String type)
    {
        return SbmlSupport.castStringToSId( ( typeToName.containsKey(type) ) ? typeToName.get(type) : type);
    }

    public static Base createKernelByType(String type, String name)
    {
        if( SBGNPropertyConstants.entityTypes.contains(type) || Type.TYPE_ENTITY.equals(type) )
            return new Specie(null, name, type,
                    SBGNPropertyConstants.entityTypes.toArray(new String[SBGNPropertyConstants.entityTypes.size()]));
        if( Type.TYPE_COMPARTMENT.equals(type) )
            return new biouml.standard.type.Compartment(null, name);
        if( type.equals(biouml.standard.type.Type.TYPE_REACTION) )
            return new Reaction(null, name);
        if( ConnectionPort.portFullTypes.contains(type) )
            return ConnectionPort.createPortByType(null, name, type);
        if( Type.TYPE_NOTE.equals(type) )
            return new Stub.Note(null, name);
        if( Type.TYPE_NOTELINK.equals(type) )
            return new Stub.NoteLink(null, name);
        return new Stub(null, name, type);
    }

    /**creates dynamic property set with all attributes required for diagram element of given type*/
    public static DynamicPropertySet getDPSByType(String type)
    {
        DynamicPropertySet dps = new DynamicPropertySetAsMap();

        if( type.equals(Type.TYPE_ENTITY) ) //entity is generic term, select default
            type = Type.TYPE_MACROMOLECULE;

        if( ! ( nonSBOElements.contains(type) ) )
            dps.add(DPSUtils.createTransient(SbmlConstants.SBO_TERM_ATTR, String.class, ""));

        if( SBGNPropertyConstants.entityTypes.contains(type) )
        {
            dps.add(new DynamicProperty(SBGNPropertyConstants.SBGN_ENTITY_COMPLEX_TYPE_PD, String.class, type));
            dps.add(new DynamicProperty(SBGNPropertyConstants.SBGN_MULTIMER, Integer.class, 0));
            dps.add(DPSUtils.createTransient(Util.COMPLEX_STRUCTURE, String.class, ""));
            return dps;
        }

        switch( type )
        {
            case biouml.standard.type.Type.TYPE_REACTION:
            {
                dps.add(new DynamicProperty(SBGNPropertyConstants.SBGN_REACTION_TYPE_PD, String.class, Type.TYPE_PROCESS));
                dps.add(new DynamicProperty(SBGNPropertyConstants.ORIENTATION, PortOrientation.class, PortOrientation.RIGHT));
                break;
            }
            case Type.TYPE_SUBDIAGRAM:
            {
                dps.add(new DynamicProperty(Util.EXTENT_FACTOR, String.class, ""));
                dps.add(new DynamicProperty(Util.TIME_SCALE, String.class, ""));
                break;
            }
            case Type.TYPE_LOGICAL:
            {
                dps.add(new DynamicProperty(SBGNPropertyConstants.SBGN_LOGICAL_OPERATOR_PD, String.class, "And"));
                dps.add(new DynamicProperty(PortOrientation.ORIENTATION_ATTR, PortOrientation.class, PortOrientation.RIGHT));
                break;
            }
            case Type.TYPE_EQUIVALENCE:
            {
                dps.add(new DynamicProperty(PortOrientation.ORIENTATION_ATTR, PortOrientation.class, PortOrientation.RIGHT));
                break;
            }
            case Type.TYPE_REGULATION:
            {
                dps.add(new DynamicProperty(SBGNPropertyConstants.SBGN_EDGE_TYPE_PD, String.class, Type.TYPE_CATALYSIS));
                break;
            }
            default:
                break;
        }
        return dps;
    }

    protected static DiagramElement createDiagramElement(String type, String name, Compartment compartment)
    {
        Base kernel = createKernelByType(type, name);
        DiagramElement de;
        if( kernel instanceof Specie )
        {
            de = new Compartment(compartment, kernel);
            if( !compartment.getKernel().getType().equals(Type.TYPE_COMPLEX) )//if parent is complex then this is not a substantive species 
            {
                de.setRole(new VariableRole(de, 0.0));
                ( (Specie)de.getKernel() ).setParent(de);
            }
        }
        else if( Type.TYPE_PHENOTYPE.equals(kernel.getType()) )
        {
            de = new Compartment(compartment, kernel);
        }
        else if( kernel instanceof biouml.standard.type.Compartment )
        {
            de = new Compartment(compartment, kernel);
            ( (biouml.standard.type.Compartment)de.getKernel() ).setParent(de);
            de.setRole(new VariableRole(de, 1.0)); //compartments have "1" initial size to avoid division by zero when concentrations are calculated
        }
        else if( kernel instanceof Reaction )
        {
            de = new Node(compartment, kernel);
            DiagramUtility.generateReactionRole(Diagram.getDiagram(compartment), de);
            ( (Reaction)kernel ).setParent(de);
            ( (Node)de ).setShowTitle(false);
        }
        else
        {
            de = new Node(compartment, kernel);
        }

        if( type.equals(Type.TYPE_EVENT) )
            de.setRole(new Event(de));
        else if( type.equals(Type.TYPE_EQUATION) )
        {
            de.setRole(new Equation(de, Equation.TYPE_SCALAR, "unknown", "0"));
            ( (Node)de ).setShowTitle(false);
        }
        else if( type.equals(Type.TYPE_FUNCTION) )
        {
            de.setRole(new Function(de));
            ( (Node)de ).setShowTitle(false);
        }
        else if( type.equals(Type.TYPE_CONSTRAINT) )
        {
            de.setRole(new Constraint(de));
            ( (Node)de ).setShowTitle(false);
        }
        else if( Type.TYPE_TABLE.equals(kernel.getType()) )
        {
            de.setRole(new SimpleTableElement(de));
        }

        ( (Node)de ).setShapeSize(new Dimension(0, 0));
        setNeccessaryAttributes(de);
        return de;
    }

    @Override
    public boolean isResizable(DiagramElement diagramElement)
    {
        if( diagramElement.getKernel() != null && ( Type.TYPE_UNIT_OF_INFORMATION.equals(diagramElement.getKernel().getType())
                || Type.TYPE_VARIABLE.equals(diagramElement.getKernel().getType()) ) )
            return true;

        if( diagramElement.getKernel() instanceof Reaction )
            return ( (SbgnDiagramViewOptions)Diagram.getDiagram(diagramElement).getViewOptions() ).isOrientedReactions();

        if( SbgnUtil.isLogical(diagramElement) )
            return true;

        return super.isResizable(diagramElement);
    }

    public static boolean hasCloneMarker(String type)
    {
        return Type.TYPE_MACROMOLECULE.equals(type) || Type.TYPE_NUCLEIC_ACID_FEATURE.equals(type) || Type.TYPE_COMPLEX.equals(type);
    }

    @Override
    public Dimension resize(DiagramElement de, Dimension sizeChange)
    {
        if( Type.TYPE_SIMPLE_CHEMICAL.equals(de.getKernel().getType()) ) //width should not exceed width for simple chemical
        {
            Dimension s = ( (Node)de ).getShapeSize();
            int w = s.width;
            int h = s.height;
            int newW = w + sizeChange.width;
            int newH = h + sizeChange.height;

            if( newH > newW )
            {
                newH = Math.min(newW, newH);
                return new Dimension(newW - w, newH - h);
            }
            return sizeChange;
        }
        return super.resize(de, sizeChange);
    }

    @Override
    public Edge createEdge(Node fromNode, Node toNode, String edgeType, Compartment compartment)
    {
        Edge edge = super.createEdge(fromNode, toNode, edgeType, compartment);
        if( edge != null )
            setNeccessaryAttributes(edge);
        Node reactionNode = ( fromNode.getKernel() instanceof Reaction ) ? fromNode : toNode;
        Node sourceSinkNode = SbgnUtil.getRedundantSourceSink(reactionNode, edgeType);
        if( sourceSinkNode != null )
            try
            {
                removeWithEdges(sourceSinkNode);
            }
            catch( Exception e )
            {
            }
        return edge;
    }

    private boolean isTypeAcceptable(Object type)
    {
        if( type.equals(Specie.class) || type.equals(biouml.standard.type.Compartment.class) || type.equals(Reaction.class)
                || type.equals(ConnectionPort.class) || classToType.get(type) != null )
            return true;
        return false;
    }

    @Override
    public DiagramElementGroup createInstanceFromElement(Compartment compartment, DataElement element, Point point,
            ViewEditorPane viewEditor) throws Exception
    {
        DiagramElementGroup elements = null;
        String name = generateUniqueName(compartment, element.getName());

        Base kernel = null;
        if( element instanceof DiagramElement )
            kernel = ( (DiagramElement)element ).getKernel();
        else if( element instanceof Base )
            kernel = (Base)element;

        if( kernel != null )
        {
            Object type;
            if( kernel instanceof Stub )
                type = kernel.getType();
            else
                type = kernel.getClass();
            if( isTypeAcceptable(type) )
            {
                elements = createInstance(compartment, type, name, point, kernel);
                DiagramElement de = elements.getElement();
                if( de == null )
                    throw new Exception("Can not create instance of type " + type.toString());
                de.setTitle(kernel.getTitle());
                for( DynamicProperty dp : kernel.getAttributes() )
                    de.getKernel().getAttributes().add(new DynamicProperty(dp.getDescriptor(), dp.getType(), dp.getValue()));
                if( element instanceof DiagramElement )
                {
                    for( DynamicProperty dp : ( (DiagramElement)element ).getAttributes() )
                        de.getAttributes().add(new DynamicProperty(dp.getDescriptor(), dp.getType(), dp.getValue()));
                }
            }
        }
        if( elements == null )
            elements = super.createInstanceFromElement(compartment, element, point, viewEditor);
        return elements;
    }

    @Override
    public String validateName(String name)
    {
        String result = SbmlSupport.castStringToSId( name );
        if( name.startsWith( "$$" ) )
            result = "$$" + result;
        return name;
    }

    @Override
    public Node cloneNode(Node node, String newName, Point location)
    {
        Node newNode = super.cloneNode(node, newName, location);

        if( newNode.getKernel() instanceof Specie )
        {
            if( !node.getAttributes().hasProperty(SBGNPropertyConstants.SBGN_CLONE_MARKER) )
                setCloneMarker(node, node.getTitle());
            setCloneMarker(newNode, node.getTitle());
        }
        return newNode;
    }

    public static void setCloneMarker(Node node, String marker)
    {
        node.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_CLONE_MARKER, String.class, marker));
    }
}
