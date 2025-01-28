package biouml.plugins.sbol;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.sbolstandard.core2.Component;
import org.sbolstandard.core2.ComponentDefinition;
import org.sbolstandard.core2.Interaction;
import org.sbolstandard.core2.Location;
import org.sbolstandard.core2.ModuleDefinition;
import org.sbolstandard.core2.OrientationType;
import org.sbolstandard.core2.Participation;
import org.sbolstandard.core2.RestrictionType;
import org.sbolstandard.core2.SBOLDocument;
import org.sbolstandard.core2.SBOLReader;
import org.sbolstandard.core2.SBOLValidationException;
import org.sbolstandard.core2.SequenceAnnotation;
import org.sbolstandard.core2.SequenceConstraint;
import org.sbolstandard.core2.SystemsBiologyOntology;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.util.ImageGenerator;
import biouml.standard.type.Base;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Stub;
import biouml.workbench.graph.DiagramToGraphTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.exception.BiosoftParseException;
import ru.biosoft.graph.CompartmentCrossCostGridLayouter;
import ru.biosoft.graph.Graph;
import ru.biosoft.graph.Layouter;
import ru.biosoft.graph.OrthogonalPathLayouter;
import ru.biosoft.graph.PathwayLayouter;
import ru.biosoft.util.DPSUtils;

public class SbolDiagramReader
{

    protected static final Logger log = Logger.getLogger(SbolDiagramReader.class.getName());
    private static final SystemsBiologyOntology sbo = new SystemsBiologyOntology();
    
    public static Diagram readDiagram(File file, String name, DataCollection<?> origin) throws Exception
    {
        Diagram result = new SbolDiagramType().createDiagram(origin, name, new DiagramInfo(name));
        result.setNotificationEnabled(false);
        SBOLDocument doc = null;
        try
        {
            doc = SBOLReader.read(file);
        }
        catch (SBOLValidationException e)
        {
            throw new BiosoftParseException(e, file.getName());
        }
        if ( doc != null )
        {
            doc.setDefaultURIprefix( "https://biouml.org" );
            DynamicProperty dp = DPSUtils.createHiddenReadOnly( SbolUtil.SBOL_DOCUMENT_PROPERTY, SBOLDocument.class, doc);
            result.getAttributes().add(dp);
            fillDiagramByDocument(doc, result);
        }
        result.setNotificationEnabled(true);
        return result;
    }

    private static void fillDiagramByDocument(SBOLDocument doc, Diagram diagram)  throws Exception
    {
        Map<String, Base> kernels = new HashMap<>();
        Set<ModuleDefinition> mds = doc.getRootModuleDefinitions();
        Set<ComponentDefinition> components = doc.getComponentDefinitions();
        Set<ComponentDefinition> topLevels = getTopLevelComponentDefinitions(doc);
        for ( ComponentDefinition cd : components )
        {
            Base base = SbolUtil.getKernelByComponentDefinition(cd, topLevels.contains(cd));
            kernels.put(cd.getPersistentIdentity().toString(), base);
        }
        //        for ( ModuleDefinition md : mds )
        //        {
        //            Set<FunctionalComponent> fcs = md.getFunctionalComponents();
        //            for ( FunctionalComponent fc : fcs )
        //            {
        //                ComponentDefinition cd = fc.getDefinition();
        //                parseComponentDefinition(cd, diagram, kernels);
        //            }
        //        }

        parseComponentDefinitions(topLevels, diagram, kernels);
        parseInteractions(mds, diagram, kernels);
        arrangeDiagram(diagram, doc, kernels);
    }

    private static void parseInteractions(Set<ModuleDefinition> mds, Diagram diagram, Map<String, Base> kernels)
    {
        for ( ModuleDefinition md : mds )
        {
            Set<Interaction> interactions = md.getInteractions();

            for ( Interaction interaction : interactions )
            {
                Map<Node, Participation> from = new HashMap<>(), to = new HashMap<>();
                String type = SbolConstants.PROCESS;
                URI uri = interaction.getIdentity();
                URI typeUri = SystemsBiologyOntology.PROCESS;
                if ( interaction.getTypes().contains(SystemsBiologyOntology.INHIBITION) )
                {
                    //sbolcanvas allow only INHIBITOR role refinement
                    from = getParticipantNodes(
                            Set.of(SystemsBiologyOntology.INHIBITOR, 
                                    SystemsBiologyOntology.COMPETITIVE_INHIBITOR,
                                    SystemsBiologyOntology.NON_COMPETITIVE_INHIBITOR,
                                    sbo.getURIbyId("SBO:0000536"), //partial_inhibitor
                                    sbo.getURIbyId("SBO:0000537"), //complete_inhibitor
                                    SystemsBiologyOntology.SILENCER, 
                                    sbo.getURIbyId("SBO:0000639"), //allosteric_inhibitor, see http://identifiers.org/SBO:0000639
                                    sbo.getURIbyId("SBO:0000638"), //irreversible_inhibitor
                                    sbo.getURIbyId("SBO:0000640"), //uncompetitive_inhibitor
                                    SystemsBiologyOntology.PROMOTER //According to documentation SystemsBiologyOntology.PROMOTER could be a participant of the INHIBITION reaction (not stated if it is in or out)
                                    ),
                            interaction.getParticipations(), diagram, kernels);
                    to = getParticipantNodes(Set.of(SystemsBiologyOntology.INHIBITED), interaction.getParticipations(), diagram, kernels);
                    type = SbolConstants.INHIBITION;
                    typeUri = SystemsBiologyOntology.INHIBITION;
                }
                else if ( interaction.getTypes().contains(SystemsBiologyOntology.STIMULATION) )
                {
                    from = getParticipantNodes(
                            Set.of(SystemsBiologyOntology.STIMULATOR,
                                    SystemsBiologyOntology.CATALYST,
                                    SystemsBiologyOntology.ENZYMATIC_CATALYST,
                                    //sbo.getURIbyId("SBO:0000671"), //non-enzymatic catalyst !!!not present in current ontology version 2.1
                                    SystemsBiologyOntology.ESSENTIAL_ACTIVATOR,
                                    SystemsBiologyOntology.BINDING_ACTIVATOR,
                                    SystemsBiologyOntology.CATALYTIC_ACTIVATOR,
                                    SystemsBiologyOntology.SPECIFIC_ACTIVATOR,
                                    SystemsBiologyOntology.NON_ESSENTIAL_ACTIVATOR,
                                    SystemsBiologyOntology.POTENTIATOR,
                                    sbo.getURIbyId("SBO:0000636"), //allosteric activator
                                    sbo.getURIbyId("SBO:0000637"), //non-allosteric activator
                                    SystemsBiologyOntology.PROMOTER //According to documentation SystemsBiologyOntology.PROMOTER could be a participant of the STIMULATION reaction (not stated if it is in or out)
                            ), interaction.getParticipations(), diagram, kernels);
                    to = getParticipantNodes(Set.of(SystemsBiologyOntology.STIMULATED), interaction.getParticipations(), diagram, kernels);
                    type = SbolConstants.STIMULATION;
                    typeUri = SystemsBiologyOntology.STIMULATION;
                }
                else if ( interaction.getTypes().contains(SystemsBiologyOntology.DEGRADATION) )
                {
                    if ( interaction.getParticipations().size() != 1 )
                        log.info("Control interation has wrong number of components, " + interaction.getParticipations().size());
                    from = getParticipantNodes(Set.of(SystemsBiologyOntology.REACTANT), interaction.getParticipations(), diagram, kernels);
                    if ( from != null )
                    {
                        String name = DefaultSemanticController.generateUniqueNodeName(diagram, interaction.getParticipations().iterator().next().getParticipantDefinition().getDisplayId()+"_degradation_product");
                        Node degradationNode = new Node(diagram, new Stub(null, name, SbolUtil.TYPE_DEGRADATION_PRODUCT));
                        to.put(degradationNode, null);
                        diagram.put(degradationNode);
                    }
                    type = SbolConstants.DEGRADATION;
                    typeUri = SystemsBiologyOntology.DEGRADATION;
                }
                else if ( interaction.getTypes().contains(SystemsBiologyOntology.CONTROL) )
                {
                    from = getParticipantNodes(Set.of(SystemsBiologyOntology.MODIFIER), interaction.getParticipations(), diagram, kernels);
                    to = getParticipantNodes(Set.of(SystemsBiologyOntology.MODIFIED), interaction.getParticipations(), diagram, kernels);
                    type = SbolConstants.CONTROL;
                }
                else if ( interaction.getTypes().contains(SystemsBiologyOntology.BIOCHEMICAL_REACTION) )
                {
                    from = getParticipantNodes(
                            Set.of(
                                SystemsBiologyOntology.REACTANT,
                                SystemsBiologyOntology.MODIFIER
                            ), 
                            interaction.getParticipations(), diagram, kernels);
                    
                    to = getParticipantNodes(
                            Set.of(
                                    SystemsBiologyOntology.PRODUCT,
                                    SystemsBiologyOntology.MODIFIED
                                ), 
                            interaction.getParticipations(), diagram, kernels);
                    type = SbolConstants.BIOCHEMICAL_REACTION;
                    typeUri = SystemsBiologyOntology.BIOCHEMICAL_REACTION;
                }
                else if ( interaction.getTypes().contains(SystemsBiologyOntology.NON_COVALENT_BINDING) )
                {
                    from = getParticipantNodes(
                            Set.of(SystemsBiologyOntology.REACTANT, SystemsBiologyOntology.INTERACTOR, SystemsBiologyOntology.SUBSTRATE, SystemsBiologyOntology.SIDE_SUBSTRATE),
                            interaction.getParticipations(), diagram, kernels);
                    to = getParticipantNodes(Set.of(SystemsBiologyOntology.PRODUCT), interaction.getParticipations(), diagram, kernels);
                    type = SbolConstants.ASSOCIATION;
                    typeUri = SystemsBiologyOntology.NON_COVALENT_BINDING;
                }
                else if ( interaction.getTypes().contains(SystemsBiologyOntology.GENETIC_PRODUCTION) )
                {
                    from = getParticipantNodes(Set.of(SystemsBiologyOntology.PROMOTER, SystemsBiologyOntology.TEMPLATE), interaction.getParticipations(), diagram, kernels);
                    to = getParticipantNodes(Set.of(SystemsBiologyOntology.PRODUCT, SystemsBiologyOntology.SIDE_PRODUCT), interaction.getParticipations(), diagram, kernels);

                    type = SbolConstants.GENETIC_PRODUCTION;
                    typeUri = SystemsBiologyOntology.GENETIC_PRODUCTION;
                }
                else if ( interaction.getTypes().contains(SystemsBiologyOntology.PROCESS) )
                {
                    from = getParticipantNodes(
                            Set.of(SystemsBiologyOntology.REACTANT, SystemsBiologyOntology.INTERACTOR, SystemsBiologyOntology.SUBSTRATE, SystemsBiologyOntology.SIDE_SUBSTRATE),
                            interaction.getParticipations(), diagram, kernels);
                    to = getParticipantNodes(Set.of(SystemsBiologyOntology.PRODUCT), interaction.getParticipations(), diagram, kernels);
                    type = SbolConstants.PROCESS;
                    typeUri = SystemsBiologyOntology.PROCESS;
                }

                if ( from != null && to != null && from.size() > 0 && to.size() > 0 )
                {
                    Iterator<Entry<Node, Participation>> fromIter = from.entrySet().iterator();
                    Iterator<Entry<Node, Participation>> toIter = to.entrySet().iterator();

                    //                    if ( from.size() == 1 && to.size() == 1 ) //only one node from each side, can do without reaction node
                    //                    {
                    //                        Node fromNode = fromIter.next();
                    //                        Node toNode = toIter.next();
                    //                        Edge result = new Edge(new Stub(null, fromNode.getName() + " -> " + toNode.getName(), type), fromNode, toNode);
                    //                        result.getAttributes().add(new DynamicProperty("interactionURI", String.class, uri.toString()));
                    //                        result.getOrigin().put(result);
                    //                        //diagram.put(result);
                    //                    }
                    //                    else
                    //                    {
                    InteractionProperties reaction = new InteractionProperties(interaction);
                    reaction.setType(SbolUtil.getInteractionStringType(typeUri));
                    Node interactionNode = new Node(diagram, reaction);
                    interactionNode.getAttributes().add(DPSUtils.createHiddenReadOnly(SbolConstants.NODE_IMAGE, String.class, SbolUtil.getSbolImagePath(interaction)));
                    interactionNode.setUseCustomImage(true);
                    interactionNode.setShapeSize(new Dimension(15, 15));
                    diagram.put(interactionNode);
                    while ( fromIter.hasNext() )
                    {
                        Entry<Node, Participation> fromEntry = fromIter.next();
                        ParticipationProperties kernel = new ParticipationProperties(fromEntry.getValue());
                        Edge result = new Edge(kernel, fromEntry.getKey(), interactionNode);
                        result.getOrigin().put(result);
                    }
                    while ( toIter.hasNext() )
                    {
                        Entry<Node, Participation> toEntry = toIter.next();
                        ParticipationProperties kernel = new ParticipationProperties(toEntry.getValue());
                        if (toEntry.getValue() == null)
                        {
                            kernel.setType( SbolConstants.PRODUCT );
                        }
                        Edge result = new Edge(kernel, interactionNode, toEntry.getKey());
                        result.getOrigin().put(result);
                    }
                    //}
                }
                else
                {
                    //only interaction node should be placed
                    InteractionProperties reaction = new InteractionProperties(interaction);
                    reaction.setType( type );
                    Node interactionNode = new Node(diagram, reaction);

                    interactionNode.getAttributes().add(DPSUtils.createHiddenReadOnly(SbolConstants.NODE_IMAGE, String.class, SbolUtil.getSbolImagePath(interaction) ));
                    interactionNode.setShapeSize(new Dimension(15, 15));
                    interactionNode.setUseCustomImage(true);
                    diagram.put(interactionNode);
                }
            }

        }

    }

    private static Map<Node, Participation> getParticipantNodes(Set<URI> types, Set<Participation> participants, Diagram diagram, Map<String, Base> kernels)
    {
        Map<Node, Participation> result = new HashMap<>();
        participants.stream().filter(pt -> {
            return pt.getRoles().stream().anyMatch(types::contains);
        }).forEach(pt -> {
            DiagramElement de = diagram.findDiagramElement(kernels.get(pt.getParticipantDefinition().getPersistentIdentity().toString()).getName());
            if ( de != null && de instanceof Node )
                result.put((Node) de, pt);
        });
        //        (pt -> {
        //            DiagramElement de = diagram.findDiagramElement(kernels.get(pt.getParticipantDefinition().getPersistentIdentity().toString()).getName());
        //            if ( de != null && de instanceof Node )
        //                return (Node) de;
        //            else
        //                return null;
        //        }).filter(Objects::nonNull).collect(Collectors.toSet());
        return result;
    }

    private static void parseComponentDefinitions(Set<ComponentDefinition> cds, Diagram diagram, Map<String, Base> kernels)  throws SBOLValidationException
    {
        for ( ComponentDefinition cd : cds )
        {
            parseComponentDefinition(cd, diagram, kernels);
        }
    }

    private static Set<ComponentDefinition> getTopLevelComponentDefinitions(SBOLDocument doc)
    {
        Set<URI> subComponents = new HashSet<>();
        doc.getComponentDefinitions().stream().forEach(cd -> {
            cd.getComponents().stream().forEach(c -> {
                subComponents.add(c.getDefinition().getPersistentIdentity());
                return;
            });
            return;
        });
        return doc.getComponentDefinitions().stream().filter(cd -> !subComponents.contains(cd.getPersistentIdentity()))
                .collect(Collectors.toSet());
    }

 
    private static void parseComponentDefinition(ComponentDefinition cd, Diagram diagram, Map<String, Base> kernels) throws SBOLValidationException
    {
        Set<Component> components = cd.getComponents();
        if ( !components.isEmpty() )
        {
            Compartment compartment = new Compartment(diagram, kernels.get(cd.getPersistentIdentity().toString()));
            compartment.getAttributes().add(DPSUtils.createHiddenReadOnly(Node.INNER_NODES_PORT_FINDER_ATTR, Boolean.class, true));
            Iterator<Component> iter = components.iterator();
            boolean isCircular = false;
            boolean isWithChromLocus = false;
            while ( iter.hasNext() )
            {
                Component component = iter.next();
                ComponentDefinition cdNode = component.getDefinition();
                if ( cdNode.getRoles().contains(SbolUtil.ROLE_CIRCULAR) )
                {
                    isCircular = true;
                    continue;
                }
                if ( cdNode.getRoles().contains(SbolUtil.ROLE_CHROMOSOMAL_LOCUS) )
                {
                    isWithChromLocus = true;
                    continue;
                }
                Base base = kernels.get(cdNode.getPersistentIdentity().toString());
                if ( base != null )
                {
                    Compartment node = new Compartment(compartment, base);
                    node.setUseCustomImage(true);

                    String icon = SbolUtil.getSbolImagePath(cdNode);
                    node.getAttributes()
                            .add(DPSUtils.createHiddenReadOnly(SbolConstants.NODE_IMAGE, String.class, icon));

                    //composite node
                    //TODO: create new diagram for this node and store in attributes
                    if ( !cdNode.getComponents().isEmpty() )
                    {
                        node.getAttributes().add(new DynamicProperty("isComposite", Boolean.class, true));
                    }
                    SequenceAnnotation sa = cd.getSequenceAnnotation(component);
                    if ( sa != null )
                    {
                        for ( Location location : sa.getLocations() )
                        {
                            if ( location.getOrientation().equals(OrientationType.REVERSECOMPLEMENT) )
                                node.getAttributes().add(new DynamicProperty("isReverse", Boolean.class, true));
                        }
                    }
                    node.setShapeSize(new Dimension(xSize, ySize));
                    //node.setLocation(new Point(20, 20));
                    compartment.put(node);
                    //                    if ( cdNode.getRoles().contains(SbolUtil.ROLE_CIRCULAR) )
                    //                    {
                    //                        isCircular = true;
                    //                        //Circular plasmid component occurs only once as the end backbone component, but node should be added twice with normal and reverse orientation
                    //                        Node circularEnd = node.clone(compartment, node.getName() + "_start", new Stub(null, node.getName() + "_start", SbolUtil.TYPE_CIRCULAR_START));
                    //                        circularEnd.setShowTitle(false);
                    //                        compartment.put(circularEnd);
                    //
                    //                        node.setShowTitle(false);
                    //                        node.getAttributes().add(new DynamicProperty("isReverse", Boolean.class, true));
                    //                    }

                    //                    if ( cdNode.getRoles().contains(SbolUtil.ROLE_CHROMOSOMAL_LOCUS) )
                    //                    {
                    //                        node.setShowTitle(false);
                    //                        if ( cd.getSequenceConstraints().stream().noneMatch(con -> con.getObject().equals(cdNode)) )
                    //                                node.getAttributes().add(new DynamicProperty("isReverse", Boolean.class, true));
                    //                    }

                }

            }
            compartment.getAttributes().add(new DynamicProperty("isCircular", Boolean.class, isCircular));
            compartment.getAttributes().add(new DynamicProperty("isWithChromLocus", Boolean.class, isWithChromLocus));
            int extWidth = 0;//isCircular || isWithChromLocus ? 2 * xSize : 0;
            compartment.setShapeSize(new Dimension(xSize * compartment.getSize() + extWidth + 10, ySize + 20));

            diagram.put(compartment);
        }
        else
        {
            Base base = kernels.get(cd.getPersistentIdentity().toString());
            if ( base != null )
            {
                Node node = new Node(diagram, base);
                node.setUseCustomImage(true);
                String icon = SbolUtil.getSbolImagePath(cd);
                node.getAttributes().add(DPSUtils.createHiddenReadOnly(SbolConstants.NODE_IMAGE, String.class, icon));
                node.setShapeSize(new Dimension(xSize, ySize));
                diagram.put(node);
            }
        }
    }

    //The position of the subject Component MUST precede that of the object Component.
    private static Collection<Component> orderComponents(ComponentDefinition cd)
    {
        Map<URI, URI> precedes = new HashMap<>();
        Set<URI> objects = new HashSet<>();
        Set<URI> subjects = new HashSet<>();
        for ( SequenceConstraint sc : cd.getSequenceConstraints() )
        {
            if ( RestrictionType.PRECEDES.equals(sc.getRestriction()) )
            {
                precedes.put(sc.getSubjectURI(), sc.getObjectURI());
                objects.add(sc.getObjectURI());
                subjects.add(sc.getSubjectURI());
            }
        }
        subjects.removeAll(objects);
        if ( subjects.size() != 1 )
            //TODO: some parallel chains occured or circular structure found
            return cd.getComponents();
        List<Component> res = new ArrayList<>();
        URI start = subjects.iterator().next();
        while ( start != null )
        {
            res.add(cd.getComponent(start));
            start = precedes.get(start);
        }
        return res;
    }

    public static void arrangeDiagram(Diagram diagram, SBOLDocument doc, Map<String, Base> kernels) throws Exception
    {
        if( SbolUtil.hasLayout( diagram ) )
            SbolUtil.readLayout( diagram );
        else
        {
            Graphics g = com.developmentontheedge.application.ApplicationUtils.getGraphics();
            diagram.setView( null );
            diagram.getType().getDiagramViewBuilder().createDiagramView( diagram, g );
            arrangeElements( diagram, doc.getComponentDefinitions(), kernels );
            diagram.setView( null );
        }

    }

    private static int xSize = 45, ySize = 45;

    private static void arrangeElements(Diagram diagram, Set<ComponentDefinition> cds, Map<String, Base> kernels)
    {
        int lY = 10;
        //Arrange DNA molecules
        for ( ComponentDefinition cd : cds )
        {
            Set<Component> components = cd.getComponents();
            if ( components.isEmpty() )
                continue;
            DiagramElement de = diagram.findDiagramElement(kernels.get(cd.getPersistentIdentity().toString()).getName());
            if ( de == null || !(de instanceof Compartment && de.getKernel() instanceof Backbone) )
                continue;
            Compartment compartment = (Compartment) de;

            int lX = 5;
            int nodePos = lX + 5;
            //+ ((compartment.getAttributes().getValue("isCircular").equals(true) || compartment.getAttributes().getValue("isWithChromLocus").equals(true)) ? xSize : 0);
            Point location = new Point(lX, lY);
            compartment.setLocation(location);

            int i = 0;

            Set<String> nodeNames = new HashSet<>(compartment.getNameList());

            //Add starting extra nodes
            
            //            Node circularStart = compartment.stream(Node.class).findFirst(n -> SbolUtil.TYPE_CIRCULAR_START.equals(n.getKernel().getType())).orElse(null);
            //            if ( circularStart != null )
            //            {
            //                Point nodeLocation = new Point(0, lY);
            //                circularStart.setLocation(nodeLocation);
            //                nodeNames.remove(circularStart.getName());
            //            }
            //            boolean hasCircular = circularStart != null ;
            //            Node circularEnd = null;

            Collection<Component> ordered = orderComponents(cd);
            Iterator<Component> iter = ordered.iterator();
            while ( iter.hasNext() )
            {

                Component component = iter.next();
                ComponentDefinition cdNode = component.getDefinition();
                Node node = compartment.findNode(kernels.get(cdNode.getPersistentIdentity().toString()).getName());
                if ( node != null )
                {
                    //                    if ( hasCircular && SbolUtil.TYPE_CIRCULAR_END.equals(node.getKernel().getType()) )
                    //                    {
                    //                        circularEnd = node;
                    //                        nodeNames.remove(node.getName());
                    //                        continue; //will be processed last 
                    //                    }
                    Point nodeLocation = new Point(nodePos + xSize * (i++), lY + 10);
                    node.setLocation(nodeLocation);
                    nodeNames.remove(node.getName());
                }
            }

            //Add not ordered components
            Set<Component> nonOrdered = new HashSet<>(components);
            nonOrdered.removeAll(ordered);
            for ( Component component : nonOrdered )
            {
                ComponentDefinition cdNode = component.getDefinition();
                Node node = compartment.findNode(kernels.get(cdNode.getPersistentIdentity().toString()).getName());
                if ( node != null )
                {
                    Point nodeLocation = new Point(lX + xSize * (i++), lY + 10);
                    node.setLocation(nodeLocation);
                    nodeNames.remove(node.getName());
                }
            }

            //Add other extra nodes
            for ( String nodeName : nodeNames )
            {
                DiagramElement node = compartment.get(nodeName);
                if ( node != null && node instanceof Node )
                {
                    Point nodeLocation = new Point(lX + xSize * (i++), lY + 10);
                    ((Node) node).setLocation(nodeLocation);
                }
            }

            //            //Add circular end
            //            if ( circularEnd != null )
            //            {
            //                Point nodeLocation = new Point(lX + xSize * (i++), lY);
            //                circularEnd.setLocation(nodeLocation);
            //            }
            lY += compartment.getView().getBounds().height + 50;
        }
        int xSingleNodes = 5;
        for ( ComponentDefinition cd : cds )
        {
            Set<Component> components = cd.getComponents();
            if ( components.isEmpty() )
            {
                DiagramElement de = diagram.findDiagramElement(kernels.get(cd.getPersistentIdentity().toString()).getName());
                if ( de == null || !(de instanceof Node) || de.getParent() != diagram )
                    continue;
                ((Node) de).setLocation(new Point(xSingleNodes, lY));
                xSingleNodes += xSize;
            }
        }

        layoutDiagram(diagram, getLayouter());
        for ( DiagramElement de : diagram )
        {
            if ( de instanceof Edge && !de.isFixed() )
                diagram.getType().getSemanticController().recalculateEdgePath((Edge) de);
            if ( de instanceof Compartment && de.getKernel() instanceof Backbone )
            {
                Compartment compartment = (Compartment) de;
                compartment.stream(Edge.class).forEach(edge -> {if(!edge.isFixed())
                    {
                        edge.setPath(null);
                        diagram.getType().getSemanticController().recalculateEdgePath(edge);
                    }
                    return;
                });
            }
            //de.setFixed(false);
        }
        //Arrange other non-DNA nodes (proteins, small molecules, etc) with layouter


        //        for ( ComponentDefinition cd : cds )
        //        {
        //            Set<Component> components = cd.getComponents();
        //            if ( components.isEmpty() )
        //                continue;
        //        }

    }

    private static void layoutDiagram(Diagram diagram, Layouter layouter)
    {
        // Generate diagram view as it may update sizes and visibility
        ImageGenerator.generateDiagramView(diagram, ApplicationUtils.getGraphics());
        PathwayLayouter pathwayLayouter = new PathwayLayouter(layouter);
        Graph graph = DiagramToGraphTransformer.generateGraph(diagram, null);
        pathwayLayouter.doLayout(graph, null);
        DiagramToGraphTransformer.applyLayout(graph, diagram);
    }

    private static Layouter getLayouter()
    {
        CompartmentCrossCostGridLayouter layouter = new CompartmentCrossCostGridLayouter();
        layouter.setGridX(50);
        layouter.setGridY(50);
        layouter.setNe(5);

        OrthogonalPathLayouter orthogonalPathLayouter = new OrthogonalPathLayouter();
        orthogonalPathLayouter.setSmoothEdges(false);
        orthogonalPathLayouter.setGridX(10);
        orthogonalPathLayouter.setGridY(10);
        orthogonalPathLayouter.setIterationMax(10);
        orthogonalPathLayouter.setIterationK(10);
        layouter.getPathLayouterWrapper().setPathLayouter(orthogonalPathLayouter);

        return layouter;
    }

}
