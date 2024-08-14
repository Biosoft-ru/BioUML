package biouml.plugins.agentmodeling;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

import ru.biosoft.graphics.editor.ViewEditorPane;
import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import biouml.model.Node;
import biouml.standard.diagram.Util;
import biouml.standard.type.Base;
import biouml.standard.type.Type;
import biouml.standard.type.Stub.ConnectionPort;
import biouml.standard.type.Stub.InputConnectionPort;
import biouml.standard.type.Stub.OutputConnectionPort;

/**
 * Create connection dialog
 */
@SuppressWarnings ( "serial" )
public class SimplePortProperties implements InitialElementProperties
{
    @Override
    public DiagramElementGroup createElements(Compartment c, Point location, ViewEditorPane viewPane) throws Exception
    {
        String name = DefaultSemanticController.generateUniqueNodeName(c, varName + "_port");
        Node port = new Node(c, ConnectionPort.createPortByType(null, name, typeNameToType.get(portTypeShort)));
        port.setTitle(varName);
        port.setLocation(c.getLocation());
        Util.setPortVariable(port, varName);

        if (viewPane!= null)
            viewPane.startTransaction("Add");
        c.put(port);

        if (viewPane!= null)
            viewPane.completeTransaction();

        return new DiagramElementGroup( port );
    }

    public SimplePortProperties(Class<? extends Base> type)
    {
        portTypeShort = type == InputConnectionPort.class ? "input" : type == OutputConnectionPort.class ? "output" : "contact";
    }

    private String portTypeShort;
    private String varName = "";
    private String portName = "";

    private static Map<String, String> typeNameToType = new HashMap<String,String>()
    {
        {
            put( "input", Type.TYPE_INPUT_CONNECTION_PORT);
            put( "output", Type.TYPE_OUTPUT_CONNECTION_PORT );
            put( "contact", Type.TYPE_CONTACT_CONNECTION_PORT );
        }
    };

    public String[] getAvailablePortTypes()
    {
        return typeNameToType.keySet().toArray(new String[typeNameToType.size()]);
    }


    public void setPortType(String type)
    {
        portTypeShort = type;
    }

    public String getPortType()
    {
        return portTypeShort;
    }

    public String getVarName()
    {
        return varName;
    }
    public void setVarName(String varName)
    {
        this.varName = varName;
    }
    public String getPortName()
    {
        return portName;
    }
    public void setPortName(String portName)
    {
        this.portName = portName;
    }
}