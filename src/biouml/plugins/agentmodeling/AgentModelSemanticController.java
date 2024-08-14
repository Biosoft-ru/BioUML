package biouml.plugins.agentmodeling;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.PropertyDescriptorEx;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.SubDiagram;
import biouml.plugins.agentmodeling.ScriptAgent.ScriptTypeEditor;
import biouml.standard.diagram.CompositeSemanticController;
import biouml.standard.diagram.Util;
import biouml.standard.type.Stub;
import biouml.standard.type.Stub.ConnectionPort;
import biouml.standard.type.Type;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.util.DPSUtils;

public class AgentModelSemanticController extends CompositeSemanticController
{

    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment parent, Object type, Point pt, ViewEditorPane viewEditor)
    {
        Class<?> typeClass = (Class<?>)type;
        boolean isNotificationEnabled = parent.isNotificationEnabled();
        parent.setNotificationEnabled(isNotificationEnabled);
        try
        {
            if( typeClass == Stub.AveragerElement.class )
            {
                Compartment compartment = new Compartment(parent,
                        new Stub.AveragerElement(parent, generateUniqueNodeName(parent, Type.TYPE_AVERAGER)));
                compartment.getAttributes()
                        .add(DPSUtils.createHiddenReadOnlyTransient(Node.INNER_NODES_PORT_FINDER_ATTR, Boolean.class, true));
                compartment.setShapeSize(new Dimension(100, 80));
                addDynamicProperties(compartment);
                compartment.getAttributes().add(new DynamicProperty(AveragerAgent.STEPS_FOR_AVERAGE, Integer.class, 10));

                Node inputConnectionPort = new Node(compartment, new Stub.InputConnectionPort(compartment, "in"));
                inputConnectionPort.setLocation(pt.getLocation());
                inputConnectionPort.getAttributes()
                        .add(DPSUtils.createReadOnly(Stub.ConnectionPort.VARIABLE_NAME_ATTR, String.class, "variable"));
                compartment.put(inputConnectionPort);

                Node outputConnectionPort = new Node(compartment, new Stub.OutputConnectionPort(compartment, "out"));
                outputConnectionPort.setLocation(pt.getLocation());
                outputConnectionPort.getAttributes()
                        .add(DPSUtils.createReadOnly(Stub.ConnectionPort.VARIABLE_NAME_ATTR, String.class, "variable"));
                compartment.put(outputConnectionPort);
                return new DiagramElementGroup(compartment);
            }
            else if( typeClass == Stub.SwitchElement.class )
            {
                Node switchElement = (Node)super.createInstance(parent, type, pt, viewEditor).getElement();
                addDynamicProperties(switchElement);
                return new DiagramElementGroup(switchElement);
            }
            else if( typeClass == ScriptAgent.class )
            {
                String name = DefaultSemanticController.generateUniqueNodeName(parent, ScriptAgent.SCRIPT_AGENT, false);
                Compartment de = new Compartment(parent, new Stub(null, name, ScriptAgent.SCRIPT_AGENT));
                de.getAttributes().add(new DynamicProperty(ScriptAgent.SCRIPT_INITIAL, String.class, ""));
                de.getAttributes().add(new DynamicProperty(ScriptAgent.SCRIPT, String.class, ""));
                de.getAttributes().add(new DynamicProperty(ScriptAgent.SCRIPT_RESULT, String.class, ""));
                addDynamicProperties(de);

                PropertyDescriptorEx pde = new PropertyDescriptorEx(ScriptAgent.SCRIPT_TYPE);
                pde.setPropertyEditorClass(ScriptTypeEditor.class);
                de.getAttributes().add(new DynamicProperty(pde, String.class, ScriptAgent.JAVA_SCRIPT_TYPE));
                de.getAttributes().add(DPSUtils.createHiddenReadOnlyTransient(Node.INNER_NODES_PORT_FINDER_ATTR, Boolean.class, true));
                return new DiagramElementGroup(de);
            }
            else if( typeClass == PythonAgent.class )
            {
                String name = DefaultSemanticController.generateUniqueNodeName(parent, PythonAgent.PYTHON_AGENT, false);
                Compartment de = new Compartment(parent, new Stub(null, name, PythonAgent.PYTHON_AGENT));
                de.getAttributes().add(new DynamicProperty(ScriptAgent.SCRIPT, String.class, ""));
                addDynamicProperties(de);
                return new DiagramElementGroup(de);
            }
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Error during element creation", ex);
            return DiagramElementGroup.EMPTY_EG;
        }
        finally
        {
            parent.setNotificationEnabled(isNotificationEnabled);
        }
        return super.createInstance(parent, type, pt, viewEditor);
    }

    public static void addDynamicProperties(DiagramElement de)
    {
        addIfAbsent(de.getAttributes(), new DynamicProperty(Util.INITIAL_TIME, Double.class, 0.0));
        addIfAbsent(de.getAttributes(), new DynamicProperty(Util.COMPLETION_TIME, Double.class, 100.0));
        addIfAbsent(de.getAttributes(), new DynamicProperty(Util.TIME_INCREMENT, Double.class, 1.0));
        addIfAbsent(de.getAttributes(), new DynamicProperty(Util.TIME_SCALE, Double.class, 1.0));
    }

    public static void addIfAbsent(DynamicPropertySet dps, DynamicProperty dp)
    {
        if (dps.getProperty(dp.getName()) == null)
            dps.add(dp);
    }

    @Override
    public boolean canAccept(Compartment compartment, DiagramElement de)
    {
        if( isDynamicModule(compartment) || isDynamicModule(de.getCompartment()))
            return false;

        if(  Util.isAverager( de ) )
            return true;

        if( compartment.getKernel() != null && compartment.getKernel().getType().equals( ScriptAgent.SCRIPT_AGENT ) && de instanceof Node
                && Util.isPort( de ) )
            return true;

        if(de.getKernel() != null && de.getKernel().getType().equals(ScriptAgent.SCRIPT_AGENT) || de.getKernel().getType().equals(PythonAgent.PYTHON_AGENT))
            return true;

        return super.canAccept(compartment, de);
    }

    /**
     * Returns true if <b>de</b> is a module on a modular diagram and <b>de</b> represents dynamic behavior
     * @param de
     * @return
     */
    public static boolean isDynamicModule(DiagramElement de)
    {
        if( ! ( de instanceof Node ) )
            return false;
        return Util.isAverager( de ) || Util.isSwitch( de ) || Util.isSubDiagram( de );
    }

    @Override
    public Object getPropertiesByType(Compartment compartment, Object type, Point point)
    {
        if( type instanceof Class && ConnectionPort.class.isAssignableFrom((Class<?>)type) && compartment.getKernel() != null
                && compartment.getKernel().getType().equals(ScriptAgent.SCRIPT_AGENT) )
            return new SimplePortProperties((Class<? extends ConnectionPort>)type);
        else if( type instanceof Class && SubDiagram.class.isAssignableFrom((Class<?>)type) )
            return new AgentSubDiagramProperties(Diagram.getDiagram(compartment));
        return super.getPropertiesByType(compartment, type, point);
    }

    @Override
    public Dimension move(DiagramElement de, Compartment newParent, Dimension offset, Rectangle oldBounds) throws Exception
    {
        if( isDynamicModule(newParent) )
        {
            Compartment compartment = (Compartment)de.getOrigin();
            boolean keepOrientation = offset.width == 0 && offset.height == 0;
            movePortToEdge( (Node)de, compartment, offset, keepOrientation );

            for( Edge edge : ( (Node)de ).getEdges() )
                this.recalculateEdgePath(edge);

            return offset;
        }

        if( isDynamicModule(de) && de instanceof Compartment)
        {
            Compartment compartment = (Compartment)de;
            Point location = compartment.getLocation();
            location.translate(offset.width, offset.height);
            boolean notification = compartment.isNotificationEnabled();
            compartment.setNotificationEnabled(false);//to avoid updating subDiagram
            compartment.setLocation(location);
            compartment.setNotificationEnabled(notification);

            for( Node node : compartment.getNodes() )
            {
                Point nodeLocation = node.getLocation();
                nodeLocation.translate(offset.width, offset.height);
                node.setLocation(nodeLocation);
            }

            for( Edge edge : getEdges(compartment) )
                recalculateEdgePath(edge);
            return offset;
        }
        return super.move(de, newParent, offset, oldBounds);
    }


}
