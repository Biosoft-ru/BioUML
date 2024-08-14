package biouml.plugins.hemodynamics;

import java.awt.Point;
import java.util.List;
import java.util.stream.Stream;

import ru.biosoft.graphics.editor.ViewEditorPane;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.InitialElementPropertiesSupport;
import biouml.model.Node;
import biouml.standard.type.Stub;
import one.util.streamex.StreamEx;

public class BifurcationProperties extends InitialElementPropertiesSupport
{
    private Vessel vessel;
    private String parentVesselName;
    private Diagram diagram;

    public BifurcationProperties(Diagram diagram)
    {
        this.diagram = diagram;
        parentVesselName = getAvailableParents().findFirst().orElse("<No parent vessels fond>");
        vessel = new Vessel(DefaultSemanticController.generateUniqueNodeName(diagram, "Vessel"), null, 10, 1, 1, 10E6);
    }

    @Override
    public DiagramElementGroup doCreateElements(Compartment c, Point location, ViewEditorPane viewPane) throws Exception
    {
        Node parentJunction;

        if( parentVesselName.equals("Heart") )
            parentJunction = new Node(diagram, "Heart", new Stub(null, "Heart", HemodynamicsType.HEART));
        else
        {
            Edge parentEdge = (Edge)diagram.get(parentVesselName);
            if( parentEdge == null || !Util.isVessel(parentEdge) )
                throw new IllegalArgumentException("Parent vessel is missing.");
            parentJunction = parentEdge.getOutput();
        }

        String nodeName = DefaultSemanticController.generateUniqueNodeName(diagram, "n");
        Node junction = new Node(diagram, nodeName, new Stub(null, nodeName, HemodynamicsType.BIFURCATION));
        junction.setTitle(vessel.getName());
        DiagramElementGroup elements = new DiagramElementGroup( junction );
        if( parentVesselName.equals( "Heart" ) )
            elements.add( parentJunction );
        Edge e = new Edge(diagram, new Stub(null, vessel.getName(), HemodynamicsType.VESSEL), parentJunction, junction);
        e.getAttributes().add(new DynamicProperty("Beta factor", Double.class, 1));
        e.getAttributes().add(new DynamicProperty("Area factor", Double.class, 1));
        e.getAttributes().add(new DynamicProperty("vessel", Vessel.class, vessel));
        elements.add( e );
        return elements;
    }

    public Stream<String> getAvailableParents()
    {
        List<Edge> vessels = diagram.stream().select(Edge.class).filter(e -> Util.isVessel(e)).toList();
        if( vessels.isEmpty() )
            return StreamEx.of("Heart");
        return vessels.stream().filter(e -> hasSlots(e)).map(e -> e.getName());

    }

    public static boolean hasSlots(Edge edge)
    {
        return edge.getOutput().edges().filter(e -> Util.isVessel(e)).count() < 3;
    }

    @PropertyName ( "Vessel properties" )
    @PropertyDescription ( "Vessel properties" )
    public Vessel getVessel()
    {
        return vessel;
    }
    public void setVessel(Vessel vessel)
    {
        this.vessel = vessel;
    }

    @PropertyName ( "Parent vessel" )
    @PropertyDescription ( "Parent vessel" )
    public String getParentVesselName()
    {
        return parentVesselName;
    }
    public void setParentVesselName(String parentVesselName)
    {
        this.parentVesselName = parentVesselName;
    }

}
