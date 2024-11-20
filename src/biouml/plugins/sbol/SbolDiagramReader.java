package biouml.plugins.sbol;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sbolstandard.core2.Component;
import org.sbolstandard.core2.ComponentDefinition;
import org.sbolstandard.core2.ModuleDefinition;
import org.sbolstandard.core2.RestrictionType;
import org.sbolstandard.core2.SBOLDocument;
import org.sbolstandard.core2.SBOLReader;
import org.sbolstandard.core2.SBOLValidationException;
import org.sbolstandard.core2.SequenceConstraint;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Node;
import biouml.standard.type.DiagramInfo;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.exception.BiosoftParseException;

public class SbolDiagramReader
{
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
            result.getAttributes().add(new DynamicProperty(SbolUtil.SBOL_DOCUMENT_PROPERTY, SBOLDocument.class, doc));

        }
        result.setNotificationEnabled(true);
        return result;
    }

    private static void fillDiagramByDocument(SBOLDocument doc, Diagram diagram)
    {
        Map<String, SbolBase> kernels = new HashMap<>();
        Set<ModuleDefinition> mds = doc.getRootModuleDefinitions();
        Set<ComponentDefinition> components = doc.getComponentDefinitions();
        for ( ComponentDefinition cd : components )
        {
            SbolBase base = SbolUtil.getKernelByComponentDefinition(cd);
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
        parseComponentDefinitions(components, diagram, kernels);
        arrangeDiagram(diagram, doc, kernels);
    }

    private static void parseComponentDefinitions(Set<ComponentDefinition> cds, Diagram diagram, Map<String, SbolBase> kernels)
    {
        Set<URI> subComponents = new HashSet<>();
        cds.stream().forEach(cd -> {
            cd.getComponents().stream().forEach(c -> {
                subComponents.add(c.getDefinition().getPersistentIdentity());
                return;
            });
            return;
        });
        for ( ComponentDefinition cd : cds )
        {
            if ( subComponents.contains(cd.getPersistentIdentity()) )
                continue;
            parseComponentDefinition(cd, diagram, kernels);
        }
    }



    private static void parseComponentDefinition(ComponentDefinition cd, Diagram diagram, Map<String, SbolBase> kernels)
    {
        Set<Component> components = cd.getComponents();
        if ( !components.isEmpty() )
        {
            //Fill as compartment
            Compartment compartment = new Compartment(diagram, kernels.get(cd.getPersistentIdentity().toString()));
            compartment.setShapeSize(new Dimension(xSize * components.size() + 10, ySize));

            Collection<URI> ordered = orderComponents(cd.getSequenceConstraints());
            //TODO: check if required
            Iterator<URI> iter = ordered.iterator();
            while ( iter.hasNext() )
            //for ( Component component : components )
            {
                Component component = cd.getComponent(iter.next());
                ComponentDefinition cdNode = component.getDefinition();
                SbolBase base = kernels.get(cdNode.getPersistentIdentity().toString());
                if ( base != null )
                {
                    Node node = new Node(diagram, base);
                    node.setUseCustomImage(true);

                    String icon = SbolUtil.getSbolImagePath(cdNode);
                    node.getAttributes()
                            .add(new DynamicProperty("node-image", URL.class, SbolDiagramReader.class.getResource("resources/" + icon + ".png")));
                    //composite node
                    if ( !cdNode.getComponents().isEmpty() )
                    {
                        node.getAttributes().add(new DynamicProperty("isComposite", Boolean.class, true));
                    }
                    compartment.put(node);
                }
            }
            diagram.put(compartment);
        }
        //TODO: if only DNA chain is saved without any elements
        //TODO: check what types can be compartments (linear, circular DNA, RNA) 
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

    private static int xSize = 48, ySize = 52;

    private static void arrangeElements(Diagram diagram, Set<ComponentDefinition> cds, Map<String, SbolBase> kernels)
    {
        int lY = 10;
        for ( ComponentDefinition cd : cds )
        {
            Set<Component> components = cd.getComponents();
            if ( components.isEmpty() )
                continue;
            DiagramElement de = diagram.findDiagramElement(kernels.get(cd.getPersistentIdentity().toString()).getName());
            if ( de == null || !(de instanceof Compartment) )
                continue;
            int lX = 0;
            Compartment compartment = (Compartment) de;
            Point location = new Point(lX, lY);
            compartment.setLocation(location);

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
                    Point nodeLocation = new Point(lX + xSize * (i++), lY);
                    node.setLocation(nodeLocation);
                }
            }
            lY += compartment.getView().getBounds().height + 20;
        }
    }

}
