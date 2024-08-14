package biouml.plugins.hemodynamics;

import java.awt.Point;
import java.beans.PropertyDescriptor;
import java.util.stream.Stream;

import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.util.DPSUtils;
import ru.biosoft.util.bean.StaticDescriptor;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.editors.StringTagEditor;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.InitialElementPropertiesSupport;
import biouml.model.Node;
import biouml.model.dynamics.VariableRole;
import biouml.standard.type.Stub;

public class ControlPointProperties extends InitialElementPropertiesSupport
{
    private static final String NO_VESSELS_FOUND = "<No vessels found>";
    private String vesselName;
    private String variableType;
    private String variableName;
    private int segment = 0;
    private Diagram diagram;
    public static final PropertyDescriptor CONTROL_TYPE_DESCRIPTOR = StaticDescriptor.create("type", ControlTypeEditor.class);
    public static final String PRESSURE_TYPE = "Pressure";
    public static final String FULL_PRESSURE_TYPE = "Full pressure";
    public static final String AREA_TYPE = "Area";
    public static final String FLOW_TYPE = "Flow";

    public static class ControlTypeEditor extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return new String[] {PRESSURE_TYPE, AREA_TYPE, FLOW_TYPE, FULL_PRESSURE_TYPE};
        }
    }

    public ControlPointProperties(Diagram diagram)
    {
        this.diagram = diagram;
        this.variableType = PRESSURE_TYPE;
        this.setVesselName(getAvailableVessels().findFirst().orElse(NO_VESSELS_FOUND));
    }

    @Override
    public DiagramElementGroup doCreateElements(Compartment c, Point location, ViewEditorPane viewPane) throws Exception
    {
        Edge edge = diagram.stream(Edge.class).filter(e -> Util.isVessel(e) && Util.getVessel(e).getTitle().equals(vesselName)).findAny()
                .orElse(null);
        if( edge == null )
            throw new IllegalArgumentException("Control point must be associated with vessel.");

        Node node = edge.getOutput();
        String name = DefaultSemanticController.generateUniqueNodeName(diagram, variableName);
        Node controlPoint = new Node(diagram, name, new Stub(null, name, HemodynamicsType.CONTROL_POINT));
        controlPoint.getAttributes().add(new DynamicProperty(CONTROL_TYPE_DESCRIPTOR, String.class, this.variableType));
        controlPoint.getAttributes().add(new DynamicProperty("segment", Integer.class, segment));
        controlPoint.getAttributes().add(DPSUtils.createReadOnly("vessel", String.class, vesselName));
        VariableRole role = new VariableRole(controlPoint);
        controlPoint.setRole(role);
        DiagramElementGroup elements = new DiagramElementGroup( controlPoint );
        Edge controlLink = createControlLink(diagram, node, controlPoint);
        elements.add( controlLink );
        return elements;
    }

    private Edge createControlLink(Compartment compartment, Node input, Node output)
    {
        String id = DefaultSemanticController.generateUniqueNodeName(compartment, "link");
        return new Edge(compartment, new Stub(null, id, "link"), input, output);
    }

    public Stream<String> getAvailableVessels()
    {
        return diagram.stream().select(Edge.class).filter(e -> Util.isVessel(e)).map(e -> Util.getVessel(e).getTitle());
    }

    @PropertyName ( "Vessel name" )
    @PropertyDescription ( "Vessel name" )
    public String getVesselName()
    {
        return vesselName;
    }
    public void setVesselName(String vesselName)
    {
        this.vesselName = vesselName;
        updateVariableName();
    }

    @PropertyName ( "Control point name" )
    @PropertyDescription ( "Control point name" )
    public String getVariableName()
    {
        return variableName;
    }
    public void setVariableName(String variableName)
    {
        this.variableName = variableName;
    }

    @PropertyName ( "Segment" )
    @PropertyDescription ( "Segment" )
    public int getSegment()
    {
        return this.segment;
    }
    public void setSegment(int segment)
    {
        this.segment = segment;
        updateVariableName();
    }

    @PropertyName ( "Type" )
    @PropertyDescription ( "Type" )
    public String getVariableType()
    {
        return this.variableType;
    }
    public void setVariableType(String type)
    {
        variableType = type;
        updateVariableName();
    }

    private void updateVariableName()
    {
        String name = vesselName + "_" + variableType + "_at_" + segment;
        name = name.replaceAll(" ", "_");
        if( !vesselName.equals(NO_VESSELS_FOUND) )
            setVariableName(DefaultSemanticController.generateUniqueNodeName(diagram, name));
    }

}
