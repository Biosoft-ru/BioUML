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
import biouml.plugins.sbgn.Type;
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
            fillDiagramByDocument(doc, result);
            DynamicProperty dp = new DynamicProperty(SbolUtil.SBOL_DOCUMENT_PROPERTY, SBOLDocument.class, doc);
            dp.setHidden(true);
            result.getAttributes().add(dp);
        }
        result.setNotificationEnabled(true);
        return result;
    }

    private static void fillDiagramByDocument(SBOLDocument doc, Diagram diagram)
    {
        Map<String, SbolBase> kernels = new HashMap<>();
        Set<ModuleDefinition> mds = doc.getRootModuleDefinitions();
        Set<ComponentDefinition> components = doc.getComponentDefinitions();
        Set<ComponentDefinition> topLevels = getTopLevelComponentDefinitions(doc);
        for ( ComponentDefinition cd : components )
        {
            SbolBase base = SbolUtil.getKernelByComponentDefinition(cd, topLevels.contains(cd));
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

    private static void parseInteractions(Set<ModuleDefinition> mds, Diagram diagram, Map<String, SbolBase> kernels)
    {
        for ( ModuleDefinition md : mds )
        {
            Set<Interaction> interactions = md.getInteractions();

            for ( Interaction interaction : interactions )
            {
                Node from = null, to = null;
                String type = SbolUtil.TYPE_PROCESS;
                if ( interaction.getTypes().contains(SystemsBiologyOntology.INHIBITION) )
                {
                    //inhibition (direct edge)
                    //sbolcanvas allow only INHIBITOR role refinement
                    from = getParticipantNode(
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
                    to = getParticipantNode(Set.of(SystemsBiologyOntology.INHIBITED), interaction.getParticipations(), diagram, kernels);
                    type = SbolUtil.TYPE_INHIBITION;
                }
                else if ( interaction.getTypes().contains(SystemsBiologyOntology.STIMULATION) )
                {
                    from = getParticipantNode(
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
                    to = getParticipantNode(Set.of(SystemsBiologyOntology.STIMULATED), interaction.getParticipations(), diagram, kernels);
                    type = SbolUtil.TYPE_STIMULATION;
                }
                else if ( interaction.getTypes().contains(SystemsBiologyOntology.BIOCHEMICAL_REACTION) )
                {
                    from = getParticipantNode(
                            Set.of(
                                SystemsBiologyOntology.REACTANT,
                                SystemsBiologyOntology.MODIFIER
                            ), 
                            interaction.getParticipations(), diagram, kernels);
                    
                    to = getParticipantNode(
                            Set.of(
                                    SystemsBiologyOntology.PRODUCT,
                                    SystemsBiologyOntology.MODIFIED
                                ), 
                            interaction.getParticipations(), diagram, kernels);
                    type = SbolUtil.TYPE_BIOCHEMICAL_REACTION;
                }
                else if ( interaction.getTypes().contains(SystemsBiologyOntology.NON_COVALENT_BINDING) )
                {
                    from = getParticipantNode(
                            Set.of(SystemsBiologyOntology.REACTANT, SystemsBiologyOntology.INTERACTOR, SystemsBiologyOntology.SUBSTRATE, SystemsBiologyOntology.SIDE_SUBSTRATE),
                            interaction.getParticipations(), diagram, kernels);
                    to = getParticipantNode(Set.of(SystemsBiologyOntology.PRODUCT), interaction.getParticipations(), diagram, kernels);
                    type = SbolUtil.TYPE_NON_COVALENT_BINDING;
                }
                else if ( interaction.getTypes().contains(SystemsBiologyOntology.CONTROL) )
                {
                    from = getParticipantNode(Set.of(SystemsBiologyOntology.MODIFIER), interaction.getParticipations(), diagram, kernels);
                    to = getParticipantNode(Set.of(SystemsBiologyOntology.MODIFIED), interaction.getParticipations(), diagram, kernels);
                    type = SbolUtil.TYPE_CONTROL;
                }
                else if ( interaction.getTypes().contains(SystemsBiologyOntology.DEGRADATION) )
                {
                    //inhibition (direct edge)
                    if ( interaction.getParticipations().size() != 1 )
                        log.info("Control interation has wrong number of components, " + interaction.getParticipations().size());
                    from = getParticipantNode(Set.of(SystemsBiologyOntology.REACTANT), interaction.getParticipations(), diagram, kernels);
                    if ( from != null )
                    {
                        String name = DefaultSemanticController.generateUniqueNodeName(diagram, "Degradation product");
                        to = new Node(diagram, new Stub(null, name, Type.TYPE_SOURCE_SINK));
                        diagram.put(to);
                    }
                    type = SbolUtil.TYPE_DEGRADATION;
                }
                else if ( interaction.getTypes().contains(SystemsBiologyOntology.GENETIC_PRODUCTION) )
                {
                    from = getParticipantNode(Set.of(SystemsBiologyOntology.PROMOTER, SystemsBiologyOntology.TEMPLATE), interaction.getParticipations(), diagram, kernels);
                    to = getParticipantNode(Set.of(SystemsBiologyOntology.PRODUCT), interaction.getParticipations(), diagram, kernels);

                    type = SbolUtil.TYPE_GENETIC_PRODUCTION;
                }
                else if ( interaction.getTypes().contains(SystemsBiologyOntology.PROCESS) )
                {
                    from = getParticipantNode(
                            Set.of(SystemsBiologyOntology.REACTANT, SystemsBiologyOntology.INTERACTOR, SystemsBiologyOntology.SUBSTRATE, SystemsBiologyOntology.SIDE_SUBSTRATE),
                            interaction.getParticipations(), diagram, kernels);
                    to = getParticipantNode(Set.of(SystemsBiologyOntology.PRODUCT), interaction.getParticipations(), diagram, kernels);
                    type = SbolUtil.TYPE_PROCESS;
                }

                if ( from != null && to != null )
                {
                    Edge result = new Edge(new Stub(null, from.getName() + " -> " + to.getName(),type), from, to);
                    result.getOrigin().put(result);
                    //diagram.put(result);
                }
            }

        }

    }

    private static Node getParticipantNode(Set<URI> types, Set<Participation> participants, Diagram diagram, Map<String, SbolBase> kernels)
    {
        for ( Participation pt : participants )
        {
            if ( pt.getRoles().stream().anyMatch(types::contains) )
            {
                DiagramElement de = diagram
                        .findDiagramElement(kernels.get(pt.getParticipantDefinition().getPersistentIdentity().toString()).getName());
                if ( de != null && de instanceof Node )
                    return (Node) de;
            }
        }
        return null;
    }

    private static void parseComponentDefinitions(Set<ComponentDefinition> cds, Diagram diagram, Map<String, SbolBase> kernels)
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


    private static void parseComponentDefinition(ComponentDefinition cd, Diagram diagram, Map<String, SbolBase> kernels)
    {
        Set<Component> components = cd.getComponents();
        if ( !components.isEmpty() )
        {
            //Fill as compartment
            Compartment compartment = new Compartment(diagram, kernels.get(cd.getPersistentIdentity().toString()));
            compartment.setShapeSize(new Dimension(xSize * components.size() + 10, ySize + 30));
            compartment.getAttributes().add(DPSUtils.createHiddenReadOnly(Node.INNER_NODES_PORT_FINDER_ATTR, Boolean.class, true));
            Collection<URI> ordered = orderComponents(cd.getSequenceConstraints());
            Iterator<URI> iter = ordered.iterator();
            while ( iter.hasNext() )
            //for ( Component component : components )
            {
                Component component = cd.getComponent(iter.next());
                ComponentDefinition cdNode = component.getDefinition();
                SbolBase base = kernels.get(cdNode.getPersistentIdentity().toString());
                if ( base != null )
                {
                    Node node = new Node(compartment, base);
                    node.setUseCustomImage(true);

                    String icon = SbolUtil.getSbolImagePath(cdNode);
                    node.getAttributes()
                            .add(new DynamicProperty("node-image", String.class, icon));
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
                }
            }
            diagram.put(compartment);
        }
        else
        {
            SbolBase base = kernels.get(cd.getPersistentIdentity().toString());
            if ( base != null )
            {
                Node node = new Node(diagram, base);
                node.setUseCustomImage(true);

                String icon = SbolUtil.getSbolImagePath(cd);
                node.getAttributes().add(new DynamicProperty("node-image", String.class, icon));
                node.setShapeSize(new Dimension(xSize, ySize));
                diagram.put(node);
            }
        }
    }

    //The position of the subject Component MUST precede that of the object Component.
    private static Collection<URI> orderComponents(Set<SequenceConstraint> scs)
    {
        Map<URI, URI> precedes = new HashMap<>();
        Set<URI> objects = new HashSet<>();
        Set<URI> subjects = new HashSet<>();
        for ( SequenceConstraint sc : scs )
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
            return precedes.keySet();
        List<URI> res = new ArrayList<>();
        URI start = subjects.iterator().next();
        while ( start != null )
        {
            res.add(start);
            start = precedes.get(start);
        }
        return res;
    }

    public static void arrangeDiagram(Diagram diagram, SBOLDocument doc, Map<String, SbolBase> kernels)
    {
        Graphics g = com.developmentontheedge.application.ApplicationUtils.getGraphics();
        diagram.setView(null);
        diagram.getType().getDiagramViewBuilder().createDiagramView(diagram, g);
        arrangeElements(diagram, doc.getComponentDefinitions(), kernels);
        diagram.setView(null);
    }

    private static int xSize = 45, ySize = 45;

    private static void arrangeElements(Diagram diagram, Set<ComponentDefinition> cds, Map<String, SbolBase> kernels)
    {
        int lY = 10;
        //Arrange DNA molecules
        for ( ComponentDefinition cd : cds )
        {
            Set<Component> components = cd.getComponents();
            if ( components.isEmpty() )
                continue;
            DiagramElement de = diagram.findDiagramElement(kernels.get(cd.getPersistentIdentity().toString()).getName());
            if ( de == null || !(de instanceof Compartment) )
                continue;
            int lX = 5;
            Compartment compartment = (Compartment) de;
            Point location = new Point(lX, lY);
            compartment.setLocation(location);
            //compartment.setFixed(true);

            Collection<URI> ordered = orderComponents(cd.getSequenceConstraints());
            Iterator<URI> iter = ordered.iterator();
            int i = 0;
            while ( iter.hasNext() )
            {

                Component component = cd.getComponent(iter.next());
                ComponentDefinition cdNode = component.getDefinition();
                Node node = compartment.findNode(kernels.get(cdNode.getPersistentIdentity().toString()).getName());
                if ( node != null )
                {
                    Point nodeLocation = new Point(lX + xSize * (i++), lY + 20);
                    node.setLocation(nodeLocation);
                }
            }
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
        //        for ( DiagramElement de : diagram )
        //        {
            //            if( de instanceof Edge && !de.isFixed() )
            //                diagram.getType().getSemanticController().recalculateEdgePath((Edge)de);
            //de.setFixed(false);
            //        }
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
