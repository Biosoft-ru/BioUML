package biouml.standard.diagram;

import java.awt.Point;
import java.util.List;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementPropertiesSupport;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.standard.type.Base;
import biouml.standard.type.Stub;
import biouml.standard.type.Stub.ConnectionPort;
import biouml.standard.type.Stub.InputConnectionPort;
import biouml.standard.type.Stub.OutputConnectionPort;
import one.util.streamex.StreamEx;
import ru.biosoft.graphics.editor.ViewEditorPane;

@SuppressWarnings ( "serial" )
@PropertyName ( "Port properties" )
@PropertyDescription ( "Port properties." )
public class PortProperties extends InitialElementPropertiesSupport
{
    protected String[] availableParameters;
    protected String[] availablePorts;
    protected String[] availableModules;
    private String name;
    private String title;

    private String portType;
    private String accessType = ConnectionPort.PUBLIC;
    private String varName = "";

    /** name of the base port which is propagated by this */
    private String portName = "";
    private final Diagram diagram;
    private Compartment module;
    private boolean isTypeFixed = false;
    private List<Node> existingPorts;

    @Override
    public DiagramElementGroup doCreateElements(Compartment c, Point location, ViewEditorPane viewPane) throws Exception
    {
        return new DiagramElementGroup(DiagramUtility.createPortNode(c, this, viewPane, location));
    }

    public PortProperties(Diagram diagram, Class<? extends Base> type)
    {
        this(diagram, type == InputConnectionPort.class ? "input" : type == OutputConnectionPort.class ? "output" : "contact");
        if( type == Stub.ConnectionPort.class )
            isTypeFixed = false;
    }

    public PortProperties(Diagram diagram, String type)
    {
        this.diagram = diagram;
        existingPorts = Util.getPorts(diagram).toList();

        //refresh modules
        availableModules = getModuleNames(diagram);
        module = ( availableModules.length > 0 ) ? (Compartment)diagram.get(availableModules[0]) : null;
        isTypeFixed = true;
        setPortType(type);
        generatePortName();
    }


    @PropertyName ( "Port type" )
    @PropertyDescription ( "Port type." )
    public String getPortType()
    {
        return portType;
    }
    public void setPortType(String type)
    {
        if( type == null || type.equals(portType) )
            return;
        Object oldValue = this.portType;
        portType = type;

        findAvailablePorts();

        this.portName = ( availablePorts.length > 0 ) ? availablePorts[0] : "";

        this.availableParameters = getAvailableParameters(diagram);

        //refresh variable name if needed
        if( alreadyHasPort(varName, portType, accessType) || varName.isEmpty() )
            varName = availableParameters.length > 0 ? availableParameters[0] : "";
        generatePortName();
        firePropertyChange("type", oldValue, type);
    }

    private void findAvailablePorts()
    {
        if( module != null )
        {
            if( accessType.equals(ConnectionPort.PROPAGATED) )
                availablePorts = getPropagatablePorts(module, getPortClass());
            else if( accessType.equals(ConnectionPort.PRIVATE) )
                availablePorts = getPrivateConnectablePorts(module, getPortClass());
            else
                availablePorts = new String[0];
        }
        else
            availablePorts = new String[0];
        this.portName = ( availablePorts.length > 0 ) ? availablePorts[0] : "";
    }

    public Class<? extends Base> getPortClass()
    {
        return ConnectionPort.typeNameToType.get(portType);
    }

    @PropertyName ( "Title" )
    public String getTitle()
    {
        return title;
    }
    public void setTitle(String title)
    {
        this.title = title;
    }

    @PropertyName ( "Name" )
    @PropertyDescription ( "Port name." )
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }

    @PropertyName ( "Module name" )
    @PropertyDescription ( "Module for port propagation." )
    public String getModuleName()
    {
        return module != null ? module.getName() : "";
    }
    public void setModuleName(String moduleName)
    {
        String oldValue = getModuleName();
        this.module = (Compartment)diagram.findDiagramElement(moduleName);
        if( module != null )
        {
            findAvailablePorts();
            generatePortName();
            this.firePropertyChange("*", oldValue, moduleName);
        }
    }
    public Compartment getModule()
    {
        return module;
    }

    @PropertyName ( "Access type" )
    @PropertyDescription ( "Port access type." )
    public String getAccessType()
    {
        return accessType;
    }
    public void setAccessType(String accessType)
    {
        if( accessType.equals(this.accessType) )
            return;
        String oldValue = this.accessType;
        this.accessType = accessType;

        if( accessType.equals(ConnectionPort.PRIVATE) )
        {
            this.availableModules = StreamEx.of(Util.getModules(diagram)).map(Node::getCompleteNameInDiagram).prepend("")
                    .toArray(String[]::new);
            module = null;
        }

        findAvailablePorts();

        this.availableParameters = getAvailableParameters(diagram);

        //refresh variable name if needed
        if( alreadyHasPort(varName, portType, accessType) )
            varName = availableParameters.length > 0 ? availableParameters[0] : "";

        generatePortName();
        this.firePropertyChange("*", oldValue, accessType);
        this.firePropertyChange("*", null, null);
    }

    @PropertyName ( "Variable name" )
    @PropertyDescription ( "Variable asssociated with port." )
    public String getVarName()
    {
        return varName;
    }
    public void setVarName(String varName)
    {
        String oldValue = this.varName;
        this.varName = varName;
        generatePortName();
        this.firePropertyChange("varName", oldValue, varName);
    }

    @PropertyName ( "Base port name" )
    @PropertyDescription ( "Base port for propagation." )
    public String getBasePortName()
    {
        return portName;
    }
    public void setBasePortName(String portName)
    {
        Object oldValue = portName;
        this.portName = portName;
        generatePortName();
        this.firePropertyChange("portName", oldValue, portName);
    }

    private void generatePortName()
    {
        String oldValue = name;
        if( !isPropagatedPort() )
        {
            String result = varName;
            result = result.substring(result.lastIndexOf(".") + 1);
            if( result.startsWith("$$") )
                result = result.substring(7);
            else if( result.startsWith("$") )
                result = result.substring(1);
            this.name = result.concat("_port");
            this.title = result;
        }
        else if( !getBasePortName().isEmpty() )
        {
            if( accessType.equals(ConnectionPort.PROPAGATED) )
                name = portName.concat("_propagated");
            else if( accessType.equals(ConnectionPort.PRIVATE) )
                name = varName + "_private";
            this.title = getModule().get(getBasePortName()).getTitle();
        }
        this.name = DefaultSemanticController.generateUniqueNodeName(diagram, name);
        this.firePropertyChange("*", oldValue, name);
    }

    /** whether diagram where we want add port to is composite or not*/
    public boolean isDiagramFlat()
    {
        return !DiagramUtility.isComposite(diagram);
    }

    public boolean isPortTypeFixed()
    {
        return isTypeFixed;
    }

    public boolean isNotPropagatedPort()
    {
        return !isPropagatedPort() && !isPrivatePort();
    }


    public boolean isPropagatedPort()
    {
        return ConnectionPort.PROPAGATED.equals(accessType);
    }

    public boolean isPrivatePort()
    {
        return ConnectionPort.PRIVATE.equals(accessType);
    }

    public boolean isPublicPort()
    {
        return ConnectionPort.PUBLIC.equals(accessType);
    }

    public static String[] getParameters(Diagram d)
    {
        return d.getRole(EModel.class).getVariables().stream().map(Variable::getName).filter(n -> ! ( n.equals("time") )).sorted()
                .toArray(String[]::new);
    }

    public String[] getAvailableParameters(Diagram d)
    {
        if( isPrivatePort() )
            return d.getRole(EModel.class).getVariables().stream().map(Variable::getName).filter(n -> ! ( n.equals("time") )).sorted()
                    .toArray(String[]::new);
        else
            return d.getRole(EModel.class).getVariables().stream().map(Variable::getName).filter(n -> ! ( n.equals("time") ))
                    .filter(n -> !alreadyHasPort(n, portType, accessType)).sorted().toArray(String[]::new);
    }

    /**
     * Returns true if this variable on the top level already have port with given type (input, output or contact) and access type (private, public or propagated) 
     */
    public boolean alreadyHasPort(String name, String type, String accessType)
    {
        return StreamEx.of(existingPorts).anyMatch(
                p -> Util.getPortVariable(p).equals(name) && Util.getAccessType(p).equals(accessType) && Util.getPortType(p).equals(type));
    }

    /**
     * Returns names of  all ports from given module (compartment) which are available for propagation AND have given type (input, output or contact).
     * Port is not available for propagation if 
     *  1. it is already propagated
     *  2. it is input and it is already fulfilled (have incoming directed connection)
     */
    public String[] getPropagatablePorts(Compartment compartment, Class<? extends Base> type)
    {
        return compartment.stream(Node.class).filter(node -> node.getKernel() != null && node.getKernel().getClass().equals(type)
                && !isAlreadyPropagated(node) && !isInputOccupied(node)).map(Node::getName).toArray(String[]::new);
    }

    /**
     * Returns names of  all ports from given module (compartment) which are available for propagation AND have given type (input, output or contact).
     * Port is not available for propagation if 
     *  1. it is already propagated
     *  2. it is input and it is already fulfilled (have incoming directed connection)
     */
    public String[] getPrivateConnectablePorts(Compartment compartment, Class<? extends Base> type)
    {
        return compartment
                .stream(Node.class).filter(node -> node.getKernel() != null
                        && node.getKernel().getClass().equals(ConnectionPort.getOppositeClass(type)) && !isInputOccupied(node))
                .map(Node::getName).toArray(String[]::new);
    }

    /**
     * Returns true if this is an input port with incoming directed connection (fulfilled)
     */
    public static boolean isInputOccupied(Node node)
    {
        return Util.isInputPort(node) && node.edges().anyMatch(e -> Util.isDirectedConnection(e));
    }

    /**
     * Returns true if this is a port inside module and it has propagation on the top level
     */
    public boolean isAlreadyPropagated(Node node)
    {
        return node.edges().anyMatch(e -> Util.isConnection(e) && Util.isPropagatedPort(e.getOtherEnd(node)));
    }

    /**
     * Returns list of names of all modules in the diagram
     */
    public static String[] getModuleNames(Diagram d)
    {
        return StreamEx.of(Util.getModules(d)).map(Node::getCompleteNameInDiagram).toArray(String[]::new);
    }

    /**
     * Returns String description of all available port types (input, output and contact) 
     */
    public static String[] getAvailablePortTypes()
    {
        return ConnectionPort.typeNameToType.keySet().toArray(new String[ConnectionPort.typeNameToType.size()]);
    }

    /**
     * Returns String description of all available port access types (public, private and propagated) 
     */
    public static String[] getAvailableAccessTypes()
    {
        return new String[] {ConnectionPort.PUBLIC, ConnectionPort.PRIVATE, ConnectionPort.PROPAGATED};
    }


    public String[] getAvailableModules()
    {
        return availableModules;
    }

    public String[] getAvailablePorts()
    {
        return availablePorts;
    }

    public String[] getAvailableParameters()
    {
        return availableParameters;
    }
}
