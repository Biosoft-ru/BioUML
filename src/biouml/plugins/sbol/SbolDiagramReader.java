package biouml.plugins.sbol;

import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.sbolstandard.core2.Component;
import org.sbolstandard.core2.ComponentDefinition;
import org.sbolstandard.core2.FunctionalComponent;
import org.sbolstandard.core2.ModuleDefinition;
import org.sbolstandard.core2.SBOLDocument;
import org.sbolstandard.core2.SBOLReader;
import org.sbolstandard.core2.SBOLValidationException;
import org.sbolstandard.examples.Sbol2Terms.component;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.Diagram;
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
            SbolBase base = new SbolBase(cd);
            kernels.put(cd.getName(), base);
        }
        for ( ModuleDefinition md : mds )
        {
            Set<FunctionalComponent> fcs = md.getFunctionalComponents();
            for ( FunctionalComponent fc : fcs )
            {
                ComponentDefinition cd = fc.getDefinition();
                parseComponentDefinition(cd, diagram, kernels);
            }
        }
        //parseComponentDefinitions(components, diagram, kernels);
    }

    private static void parseComponentDefinitions(Set<ComponentDefinition> cds, Diagram diagram, Map<String, SbolBase> kernels)
    {
        for ( ComponentDefinition cd : cds )
        {
            parseComponentDefinition(cd, diagram, kernels);
        }
    }

    private static void parseComponentDefinition(ComponentDefinition cd, Diagram diagram, Map<String, SbolBase> kernels)
    {
        Set<Component> components = cd.getComponents();
        if ( !components.isEmpty() )
        {
            //Fill as compartment
            Compartment compartment = new Compartment(diagram, kernels.get(cd.getName()));
            compartment.setShapeSize(new Dimension(48 * components.size() + 10, 48 + 10));
            int lX = 0, lY = 20;
            Point location = new Point(lX, lY);
            compartment.setLocation(location);
            //            compartment.setUseCustomImage(true);
            //            String icon = SbolUtil.getSbolImagePath(cd);
            //            compartment.getAttributes()
            //                    .add(new DynamicProperty("node-image", URL.class, SbolDiagramReader.class.getResource("resources/" + icon + ".png")));
            int i = 0;
            for ( Component component : components )
            {

                ComponentDefinition cdNode = component.getDefinition();
                SbolBase base = kernels.get(cdNode.getName());
                if ( base != null )
                {
                    Node node = new Node(diagram, base);
                    node.setUseCustomImage(true);
                    Point nodeLocation = new Point(lX + 48 * (i++), lY + 5);
                    node.setLocation(nodeLocation);
                    String icon = SbolUtil.getSbolImagePath(cdNode);
                    node.getAttributes()
                            .add(new DynamicProperty("node-image", URL.class, SbolDiagramReader.class.getResource("resources/" + icon + ".png")));
                    compartment.put(node);
                }
            }
            diagram.put(compartment);
        }
        //TODO: if only DNA chain is saved without any elements
        //TODO: check what types can be compartments (linear, circular DNA, RNA) 
    }

    private static void fillCompartment()
    {

    }

}
