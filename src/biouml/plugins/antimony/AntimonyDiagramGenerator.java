package biouml.plugins.antimony;

import java.awt.Point;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.Option;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.DiagramType;
import biouml.model.Edge;
import biouml.model.InitialElementProperties;
import biouml.model.ModelDefinition;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.model.SubDiagram;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.Connection;
import biouml.model.dynamics.Constraint;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Function;
import biouml.model.dynamics.SimpleTableElement;
import biouml.model.dynamics.SimpleTableElement.VarColumn;
import biouml.model.dynamics.UndirectedConnection;
import biouml.model.dynamics.UndirectedConnection.MainVariableType;
import biouml.model.dynamics.UnitCalculator;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.antimony.astparser_v2.AntimonyNotationParser;
import biouml.plugins.antimony.astparser_v2.AstAssert;
import biouml.plugins.antimony.astparser_v2.AstAt;
import biouml.plugins.antimony.astparser_v2.AstConnectionConversionFactor;
import biouml.plugins.antimony.astparser_v2.AstConversionFactor;
import biouml.plugins.antimony.astparser_v2.AstDelete;
import biouml.plugins.antimony.astparser_v2.AstEOL;
import biouml.plugins.antimony.astparser_v2.AstEquation;
import biouml.plugins.antimony.astparser_v2.AstFunction;
import biouml.plugins.antimony.astparser_v2.AstGlobal;
import biouml.plugins.antimony.astparser_v2.AstHas;
import biouml.plugins.antimony.astparser_v2.AstImport;
import biouml.plugins.antimony.astparser_v2.AstIn;
import biouml.plugins.antimony.astparser_v2.AstIs;
import biouml.plugins.antimony.astparser_v2.AstList;
import biouml.plugins.antimony.astparser_v2.AstLocateFunction;
import biouml.plugins.antimony.astparser_v2.AstModel;
import biouml.plugins.antimony.astparser_v2.AstPersistent;
import biouml.plugins.antimony.astparser_v2.AstPriority;
import biouml.plugins.antimony.astparser_v2.AstProduct;
import biouml.plugins.antimony.astparser_v2.AstProperty;
import biouml.plugins.antimony.astparser_v2.AstReactant;
import biouml.plugins.antimony.astparser_v2.AstReactionTitle;
import biouml.plugins.antimony.astparser_v2.AstReactionType;
import biouml.plugins.antimony.astparser_v2.AstRegularFormulaElement;
import biouml.plugins.antimony.astparser_v2.AstRelationshipType;
import biouml.plugins.antimony.astparser_v2.AstSemicolon;
import biouml.plugins.antimony.astparser_v2.AstSingleProperty;
import biouml.plugins.antimony.astparser_v2.AstSpecialFormula;
import biouml.plugins.antimony.astparser_v2.AstStart;
import biouml.plugins.antimony.astparser_v2.AstSubSymbol;
import biouml.plugins.antimony.astparser_v2.AstSymbol;
import biouml.plugins.antimony.astparser_v2.AstSymbolType;
import biouml.plugins.antimony.astparser_v2.AstText;
import biouml.plugins.antimony.astparser_v2.AstTriggerInitialValue;
import biouml.plugins.antimony.astparser_v2.AstUnit;
import biouml.plugins.antimony.astparser_v2.AstUnitFormula;
import biouml.plugins.antimony.astparser_v2.AstUseValuesFromTriggerTime;
import biouml.plugins.antimony.astparser_v2.AstVarOrConst;
import biouml.plugins.antimony.astparser_v2.ParseException;
import biouml.plugins.antimony.astparser_v2.SimpleNode;
import biouml.plugins.sbgn.EquivalenceOperatorProperties;
import biouml.plugins.sbgn.LogicalOperatorProperties;
import biouml.plugins.sbgn.PhenotypeProperties;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbgn.SbgnComplexStructureManager;
import biouml.plugins.sbgn.SbgnCompositeDiagramType;
import biouml.plugins.sbgn.SbgnDiagramType;
import biouml.plugins.sbgn.SbgnSemanticController;
import biouml.plugins.sbgn.SbgnUtil;
import biouml.plugins.sbgn.Type;
import biouml.plugins.sbml.SbmlDiagramType_L3v1;
import biouml.standard.diagram.CreatorElementWithName;
import biouml.standard.diagram.DiagramGenerator;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.PortProperties;
import biouml.standard.diagram.SimpleBusProperties;
import biouml.standard.diagram.SimpleTableElementProperties;
import biouml.standard.diagram.Util;
import biouml.standard.state.State;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Reaction;
import biouml.standard.type.Referrer;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Stub.ConnectionPort;
import biouml.standard.type.Stub.Note;
import biouml.standard.type.Unit;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.math.model.LinearFormatter;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.DPSUtils;
import ru.biosoft.util.TextUtil;

public class AntimonyDiagramGenerator
{
    private final static String REACTION = "reaction";
    private final static String PORT = "port";
    private final static String NO_NAME = "antimonyNoName";
    private final static String SUBDIAGRAM_PREFIX = "subdiagram_";

    protected Logger log = Logger.getLogger(AntimonyDiagramGenerator.class.getName());

    protected Map<String, NodePrototype> nodePrototypes = new HashMap<>();
    protected Map<String, EdgePrototype> edgePrototypes = new HashMap<>();
    private Map<String, List<NodePrototype>> busNodePrototypes = new HashMap<String, List<NodePrototype>>();

    private Diagram newDiagram;
    private DiagramGenerator generator;
    private CreatorElementWithName controller;
    protected int antimonyNoName = 0;


    public AntimonyDiagramGenerator()
    {
    }

    public AntimonyDiagramGenerator(Map<String, PrototypeModelDefinition> importedDiagrams)
    {
        this.importedDiagrams = importedDiagrams;
    }

    public Diagram generateDiagram(AstStart start, Diagram prototypeDiagram) throws Exception
    {
        //importAnnotations(start, prototypeDiagram);


        AstModel model = start.getMainModel();
        if( model.isOutsideModel() )
            return generateDiagram(prototypeDiagram, model, model.getAstFunctions(), null, model.getAstUnits());
        else if( model.isMainModel() )
            return generateDiagram(prototypeDiagram, model, start.getAstFunctions(), start.getSimpleModels(), start.getAstUnits());
        return generateDiagram(prototypeDiagram, model, start.getAstFunctions(), null, start.getAstUnits());
    }

    //    private void importAnnotations(biouml.plugins.antimony.astparser_v2.Node ast, Diagram prototypeDiagram) throws Exception
    //    {
    //        if( ast instanceof AstStart || ast instanceof AstModel )
    //        {
    //            for( int i = 0; i < ast.jjtGetNumChildren(); i++ )
    //            {
    //                if( ast.jjtGetChild(i) instanceof AstImportAnnotation )
    //                    addAnnotation((AstImportAnnotation)ast.jjtGetChild(i), prototypeDiagram.getOrigin());
    //                else if( ast.jjtGetChild(i) instanceof AstModel )
    //                    importAnnotations(ast.jjtGetChild(i), prototypeDiagram);
    //            }
    //        }
    //    }

    private Diagram createDiagram(Diagram prototypeDiagram, AstModel model) throws Exception
    {
        String antimonyTitle = ( model.getNameSymbol() != null ) ? model.getNameSymbol().getName() : prototypeDiagram.getTitle();
        if( prototypeDiagram != null )
        {
            DiagramType type = prototypeDiagram.getType().clone();
            if( type instanceof SbgnDiagramType && model.isOutsideModel() )
                type = new SbgnCompositeDiagramType();

            Diagram result = type.createDiagram(prototypeDiagram.getOrigin(), prototypeDiagram.getKernel().getName(),
                    prototypeDiagram.getKernel());
            prototypeDiagram.getAttributes().forEach(dp -> result.getAttributes().add(dp));
            result.setPathLayouter(prototypeDiagram.getPathLayouter());//TODO: clone
            result.setTitle(antimonyTitle);
            return result;
        }

        Diagram result = new SbmlDiagramType_L3v1().createDiagram(null, antimonyTitle, new DiagramInfo(antimonyTitle));
        result.setTitle(antimonyTitle);
        return result;
    }

    private Diagram generateDiagram(Diagram prototypeDiagram, AstModel model, List<AstFunction> functions, List<AstModel> models,
            List<AstUnit> units) throws Exception
    {
        newDiagram = createDiagram(prototypeDiagram, model);
        generator = new DiagramGenerator(newDiagram.getName());
        controller = (CreatorElementWithName)newDiagram.getType().getSemanticController();

        addSimpleModels(models); // add models which are defined outside of this model
        addPublicPortPrototypes(model);

        // fill prototypes
        for( int i = 0; i < model.jjtGetNumChildren(); i++ )
        {
            try
            {
                addAntimonyElement(model.jjtGetChild(i));
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Can't add model element(" + model.jjtGetChild(i) + "): " + t);
                throw new Exception("Can't add " + model.jjtGetChild(i) + " in diagram: " + t.getMessage());
            }
        }

        fillDiagram(units, functions, model);
        if( prototypeDiagram != null )
        {
            adjustNodeParents(newDiagram, prototypeDiagram);
            adjustVariables(newDiagram, prototypeDiagram);
        }
        newDiagram = (Diagram)validateDe(newDiagram);
        return newDiagram;
    }

    private void addPublicPortPrototypes(AstModel model)
    {
        for( AstSymbol symbol : model.getParameters() )
        {
            getNodePrototype(symbol, null, null); // add variable
            NodePrototype portPrototype = getNodePrototype(symbol, ConnectionPort.PUBLIC, null); // add public port prototype
            portPrototype.portSymbol = symbol;
        }
    }

    private void addPublicPorts()
    {
        for( NodePrototype prototype : nodePrototypes.values() )
        {
            if( prototype.portSymbol != null && prototype.portSymbol.getChainNames().length == 1 )
                addPort(prototype, prototype.portSymbol.getPortType());
        }
    }

    private Node addPort(NodePrototype portPrototype, String portType)
    {
        String nameVariable = portPrototype.portSymbol.getName();
        String accessType = portPrototype.type;
        PortProperties properties = new PortProperties(newDiagram, Stub.ConnectionPort.class);
        try
        {
            Variable var = AntimonyUtility.findVariable(nameVariable, newDiagram.getRole(EModel.class));

            if( var != null && var instanceof VariableRole )
                nameVariable = var.getName();


            properties.setVarName(nameVariable);
            properties.setPortType(portType);
            properties.setAccessType(accessType);

            if( portPrototype != null )
            {
                if( portPrototype.title != null )
                    properties.setTitle(portPrototype.title);
            }


            Node oldPort = findSuchPort(properties, newDiagram);
            if( oldPort == null )
            {
                Node node = (Node)properties.createElements(newDiagram, new Point(), null).get(0);
                node = (Node)validateDe(node);
                if( Util.isPublicPort(node) )
                    AntimonyAstCreator.link(node, portPrototype.portSymbol);

                linkWithAst(portPrototype, node.getAttributes());

                return node;
            }
            linkWithAst(portPrototype, oldPort.getAttributes());
            return oldPort;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can't add port " + nameVariable + "_port");
            return null;
        }
    }

    private Node addPortChain(NodePrototype portPrototype, String portType) throws Exception
    {
        String[] subName = portPrototype.portSymbol.getChainNames();

        if( subName.length == 1 )
            return addPort(portPrototype, portPrototype.portSymbol.getPortType());
        //if subName.length == 2 then exist public port
        else if( subName.length > 2 )
        {
            AstSymbol symbol = portPrototype.portSymbol;

            Diagram diagram = newDiagram;
            Stack<Node> subDiagrams = new Stack<>();
            Node currentSubDiagram = diagram.findNode(subName[0]);
            try
            {
                diagram = ( (SubDiagram)currentSubDiagram ).getDiagram();
                for( int i = 1; i < subName.length - 1; i++ )
                {
                    currentSubDiagram = diagram.findNode(subName[i]);
                    subDiagrams.add(currentSubDiagram);
                    diagram = ( (SubDiagram)currentSubDiagram ).getDiagram();
                }
            }
            catch( ClassCastException ex )
            {
                throw new ClassCastException("Inncorrect chain of subdiagram");
            }
            Node basePort = StreamEx.of(AntimonyUtility.getPortNodes((SubDiagram)currentSubDiagram, false))
                    .findAny(portNode -> isCorrectBasePort(symbol.getNameInSubDiagram(), portNode)).orElse(null);
            while( !subDiagrams.isEmpty() )
            {
                Node subDiagram = subDiagrams.pop();
                diagram = (Diagram)subDiagram.getParent();
                basePort = addPropagatedPort(diagram, subDiagram.getName(), basePort);
            }

            Compartment firstSubDiagram = (Compartment)newDiagram.findNode(subName[0]);

            Node propPort = firstSubDiagram.findNode(basePort.getName());
            for( NodePrototype p : nodePrototypes.get(subName[0]).elementTitles )
            {
                if( ConnectionPort.PROPAGATED.equals(p.type) && Util.getPortVariable(propPort).equals("$" + p.name) )
                    propPort.setTitle(p.title);
            }

            return firstSubDiagram.findNode(basePort.getName());
        }
        return null;
    }
    private boolean isCorrectBasePort(String portName, Node suspectPortNode)
    {
        return portName.equals(AntimonyUtility.getAntimonyValidName(Util.getPortVariable(suspectPortNode)));
    }
    private Node addPropagatedPort(Diagram diagram, String subDiagramName, Node basePort) throws Exception
    {
        PortProperties properties = new PortProperties(diagram, Stub.ConnectionPort.class);
        try
        {

            String portType = Util.getPortType(basePort);
            properties.setPortType(portType);
            properties.setAccessType(ConnectionPort.PROPAGATED);
            properties.setBasePortName(basePort.getName());
            properties.setModuleName(subDiagramName);

            NodePrototype portPrototype = getNodePrototype(properties.getVarName().substring(1) + ":propagated port");

            if( portPrototype != null )
            {
                if( portPrototype.title != null )
                    properties.setTitle(portPrototype.title);
            }

            Node oldPort = findSuchPort(properties, diagram);
            if( oldPort == null )
            {
                Node node = (Node)properties.createElements(diagram, new Point(), null).get(0);
                node = (Node)validateDe(node);

                if( portPrototype != null )
                    linkWithAst(portPrototype, node.getAttributes());

                return node;
            }
            linkWithAst(portPrototype, oldPort.getAttributes());

            return oldPort;
        }
        catch( Exception e )
        {
            throw new Exception("Can't add propagated port " + basePort.getName() + " in " + subDiagramName);
        }
    }

    private Node findSuchPort(PortProperties properties, Diagram diagram) throws Exception
    {
        Collection<Node> ports = AntimonyUtility.getPortNodes(diagram, false);
        if( properties.isPropagatedPort() )
        {
            String moduleName = properties.getModuleName();
            if( moduleName == null || moduleName.isEmpty() )
                throw new Exception("Incorrect propagated port attribute baseModuleName is missing");

            String portName = properties.getBasePortName();
            if( portName == null || portName.isEmpty() )
                throw new Exception("Incorrect propagated port attribute basePortName is missing");

            for( Node port : ports )
            {
                DynamicProperty dp = port.getAttributes().getProperty(ConnectionPort.BASE_MODULE_NAME_ATTR);
                DynamicProperty dp2 = port.getAttributes().getProperty(ConnectionPort.BASE_PORT_NAME_ATTR);
                if( dp != null && dp2 != null && moduleName.equals(dp.getValue()) && portName.equals(dp2.getValue())
                        && !AntimonyUtility.haveSameAccessType(properties, port) )
                    return port;
            }
            return null;
        }

        String variableName = properties.getVarName();
        if( variableName == null || variableName.isEmpty() )
            throw new Exception("Incorrect port attribute variableName is missing");

        for( Node port : ports )
        {
            DynamicProperty dp = port.getAttributes().getProperty(ConnectionPort.VARIABLE_NAME_ATTR);
            if( dp != null && variableName.equals(dp.getValue()) && AntimonyUtility.haveSameAccessType(properties, port) )
                return port;
        }
        //if new port
        return null;
    }

    //copying features that can not be expressed with antimony
    private void adjustVariables(Diagram newDiagram, Diagram prototypeDiagram)
    {
        if( prototypeDiagram == null )
            return;

        EModel newEmodel = newDiagram.getRole(EModel.class);
        EModel oldEmodel = prototypeDiagram.getRole(EModel.class);

        for( Variable var : oldEmodel.getVariables() )
        {
            Variable newVar = newEmodel.getVariable(var.getName());
            if( newVar == null )
                continue;

            newVar.setComment(var.getComment());

            if( newVar instanceof VariableRole && var instanceof VariableRole )
            {
                VariableRole newRole = (VariableRole)newVar;
                VariableRole role = (VariableRole)var;
                newRole.setBoundaryCondition(role.isBoundaryCondition());
                //                newRole.setQuantityType(role.getQuantityType());
                //                newRole.setInitialQuantityType(role.getQuantityType());
                newRole.setOutputQuantityType(role.getOutputQuantityType());
                newRole.setConversionFactor(role.getConversionFactor());
            }
        }
    }


    private void adjustNodeParents(Diagram newDiagram, Diagram oldDiagram)
    {
        for( DiagramElement de : newDiagram )
        {
            if( de.getRole() instanceof Variable )
                continue;

            try
            {
                DiagramElement oldDe = oldDiagram.findDiagramElement(de.getName());

                if( oldDe != null && ( ( oldDe.getKernel() == null && de.getKernel() == null )
                        || oldDe.getKernel().getType().equals(de.getKernel().getType()) ) )
                {
                    Option oldParent = oldDe.getParent();
                    if( oldParent instanceof DiagramElement && ! ( oldParent instanceof Diagram || oldParent instanceof SubDiagram ) )
                    {
                        String oldParentName = ( (DiagramElement)oldParent ).getCompleteNameInDiagram();

                        DiagramElement newParent = newDiagram.findDiagramElement(oldParentName);
                        if( newParent instanceof Compartment )
                        {
                            de.setParent(newParent);
                            ( (Compartment)newParent ).put(de);
                        }
                    }
                }
            }
            catch( Exception ex )
            {
                log.warning("Can't save parent for " + de.getName() + ": " + ex.getMessage());
            }
        }
    }

    private void fillDiagram(List<AstUnit> units, List<AstFunction> functions, AstModel model) throws Exception
    {
        //first iteration: add units
        if( units != null )
            for( AstUnit astUnit : units )
            {
                try
                {
                    addUnit(astUnit);
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE, "Can't add unit(" + astUnit.getName() + "): " + t);
                    throw new Exception("Can't add " + astUnit + " in diagram: " + t.getMessage());
                }
            }

        //second iteration: add functions
        if( functions != null )
            for( AstFunction astFunction : functions )
            {
                try
                {
                    addFunction(astFunction);
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE, "Can't add function(" + astFunction.getFunctionName() + "): " + t);
                    throw new Exception("Can't add " + astFunction + " in diagram: " + t.getMessage());
                }
            }


        //third iteration: add all variables, species and compartments which may be addressed by other diagarm elements
        for( NodePrototype prototype : nodePrototypes.values() )
        {
            if( prototype.type == null || AntimonyUtility.isVariableRole(prototype.type) )//null means parameter
                try
                {
                    addNodeInDiagram(prototype);
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE, "Can't add " + prototype.name + " in diagram: " + t);
                    throw new Exception("Can't add " + prototype.name + " in diagram: " + t.getMessage());
                }
        }

        //fourth iteration: add all reactions which may be addressed by logical nodes
        for( NodePrototype prototype : nodePrototypes.values() )
        {
            if( prototype.type != null && prototype.type.equals(REACTION) )
                try
                {
                    addNodeInDiagram(prototype);
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE, "Can't add " + prototype.name + " in diagram: " + t);
                    throw new Exception("Can't add " + prototype.name + " in diagram: " + t.getMessage());
                }
        }

        //add all the rest except notes (they can be linked to any node)
        for( NodePrototype prototype : nodePrototypes.values() )
        {
            if( prototype.type != null && !prototype.type.equals(AstProperty.NOTE) )
                try
                {
                    addNodeInDiagram(prototype);
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE, "Can't add " + prototype.name + " in diagram: " + t);
                    throw new Exception("Can't add " + prototype.name + " in diagram: " + t.getMessage());
                }
        }

        //add notes 
        for( NodePrototype prototype : nodePrototypes.values() )
        {
            if( prototype.type != null && prototype.type.equals(AstProperty.NOTE) )
                try
                {
                    addNodeInDiagram(prototype);
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE, "Can't add " + prototype.name + " in diagram: " + t);
                    throw new Exception("Can't add " + prototype.name + " in diagram: " + t.getMessage());
                }
        }

        //add buses
        for( String key : busNodePrototypes.keySet() )
        {
            try
            {
                addBusInDiagram(key, busNodePrototypes.get(key));
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Can't add " + key + " in diagram: " + t);
                throw new Exception("Can't add " + key + " in diagram: " + t.getMessage());
            }
        }

        for( EdgePrototype prototype : edgePrototypes.values() )
        {
            try
            {
                addEdgeInDiagram(prototype);
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Can't add " + prototype.name + " in diagram: " + t);
                throw new Exception("Can't add " + prototype.name + " in diagram: " + t.getMessage());
            }
        }

        addPublicPorts();
        addConnection();
        removeDeletedElement();
    }

    private void addBusInDiagram(String busName, List<NodePrototype> associatedNodes) throws Exception
    {

        for( int i = 0; i < associatedNodes.size(); i++ )
        {
            NodePrototype np = associatedNodes.get(i);
            SimpleBusProperties sbp = new SimpleBusProperties(newDiagram);
            sbp.setName(busName);
            if( i > 0 )
                sbp.setNewBus(false);

            if( np.directed )
                sbp.setDirected(np.directed);

            DiagramElementGroup elements = controller.createInstance(newDiagram, SimpleBusProperties.class, np.name, null, sbp);
            Node busNode = (Node)elements.get(0);

            if( np.title != null )
                busNode.setTitle(np.title);


            linkWithAst(np, busNode.getAttributes());
            elements.putToCompartment();

            for( AstSymbol portSymbol : np.ports )
            {
                Node portNode = newDiagram.findNode(portSymbol.getName());

                if( portNode == null )
                    throw new Exception("Port node with name \"" + portSymbol.getName() + "\" does not exist!");

                String edgeName = DefaultSemanticController.generateUniqueNodeName(newDiagram, "connection");
                Edge newEdge;
                Connection role;

                if( np.directed )
                {
                    if( Util.isInputPort(portNode) )
                        newEdge = new Edge(newDiagram, new Stub.DirectedConnection(null, edgeName), busNode, portNode);
                    else
                        newEdge = new Edge(newDiagram, new Stub.DirectedConnection(null, edgeName), portNode, busNode);

                    role = new biouml.model.dynamics.DirectedConnection(newEdge);
                }
                else
                {
                    newEdge = new Edge(newDiagram, new Stub.UndirectedConnection(null, edgeName), busNode, portNode);
                    role = new biouml.model.dynamics.UndirectedConnection(newEdge);

                    role.setInputPort(new Connection.Port(Util.getPortVariable(busNode), busNode.getTitle()));
                    role.setOutputPort(new Connection.Port(Util.getPortVariable(portNode), portNode.getTitle()));

                }

                newEdge.setRole(role);

                newDiagram.put(newEdge);
                linkWithAst(portSymbol, newEdge.getAttributes());
            }
        }



        //        Node node = (Node)elements.getElement();
        //
        //        
        //
        //        for( DiagramElement de : elements.getElements() )
        //            linkWithAst(prototype.astNodes, de.getAttributes());

    }

    private void addEdgeInDiagram(EdgePrototype prototype)
    {
        Node node = (Node)Diagram.findNode(newDiagram, prototype.inputName);

        if( node == null )
            return;

        for( Edge edge : node.getEdges() )
        {
            if( edge.getOutput().getName().equals(prototype.outputName) )
            {
                edge.setTitle(prototype.name);
                if( edge.getKernel() instanceof SpecieReference && ! ( (SpecieReference)edge.getKernel() ).isReactantOrProduct() )
                {
                    if( prototype.modAction != null )
                        ( (SpecieReference)edge.getKernel() ).setModifierAction(prototype.modAction);
                }
                else
                {
                    edge.getAttributes()
                            .add(new DynamicProperty(SBGNPropertyConstants.SBGN_EDGE_TYPE_PD, String.class, prototype.modAction));
                }
                linkWithAst(prototype.astNodes, edge.getAttributes());
            }
        }

    }

    private void removeDeletedElement() throws Exception
    {
        for( AstDelete astDelete : deletedElements )
        {
            String[] chainNames = astDelete.getAstSymbol().getChainNames();
            if( chainNames.length > 1 )
            {
                Node currentSubDiagram = newDiagram.findNode(chainNames[0]);
                try
                {
                    Diagram diagram = ( (SubDiagram)currentSubDiagram ).getDiagram();
                    for( int i = 1; i < chainNames.length - 1; i++ )
                    {
                        currentSubDiagram = diagram.findNode(chainNames[i]);
                        diagram = ( (SubDiagram)currentSubDiagram ).getDiagram();
                    }
                    diagram.remove(chainNames[chainNames.length - 1]);
                    linkWithAst(astDelete, currentSubDiagram.getAttributes());
                }
                catch( ClassCastException ex )
                {
                    throw new ClassCastException("Inncorrect chain of subdiagram");
                }
            }
            else
                throw new Exception("Incorrect element name. Can delete element only from subDiagram");
        }

    }

    private void addConnection() throws Exception
    {
        for( LinkedPair pair : synchronizedPairs )
        {
            // add private ports for case synchronization
            DynamicProperty conversionFactor = null;

            // find port
            Node firstPortNode = pair.getFirstPort();
            Node secondPortNode = pair.getSecondPort();

            NodePrototype portPrototype = pair.firstPortPrototype;
            NodePrototype secondPortPrototype = pair.secondPortPrototype;
            if( firstPortNode == null )
            {
                if( secondPortNode == null )
                    secondPortNode = addPortChain(secondPortPrototype, null);
                firstPortNode = addPortChain(portPrototype, AntimonyUtility.getOppositePortType(secondPortNode));
            }
            else if( secondPortNode == null )
                secondPortNode = addPortChain(secondPortPrototype, AntimonyUtility.getOppositePortType(firstPortNode));

            if( firstPortNode == null || secondPortNode == null )
                throw new Exception("Incorrect port connection!");

            AstSymbol symbol = portPrototype.portSymbol;
            if( symbol != null )
            {
                for( int i = 0; i < symbol.jjtGetNumChildren(); i++ )
                {
                    if( symbol.jjtGetChild(i) instanceof AstConnectionConversionFactor )
                    {
                        AstConnectionConversionFactor factor = (AstConnectionConversionFactor)symbol.jjtGetChild(i);
                        String value = factor.getValue();
                        conversionFactor = new DynamicProperty("conversionFactor", String.class, value);
                        break;
                    }
                }
            }

            //create connect
            String edgeName = DefaultSemanticController.generateUniqueNodeName(newDiagram, "connection");
            Node inNode = Util.isInputPort(secondPortNode) ? secondPortNode : firstPortNode;
            Node outNode = secondPortNode.equals(inNode) ? firstPortNode : secondPortNode;

            Edge newEdge;
            Connection role;
            if( Util.isInputPort(firstPortNode) || Util.isInputPort(secondPortNode) )
            {
                newEdge = new Edge(newDiagram, new Stub.DirectedConnection(null, edgeName), inNode, outNode);
                role = new biouml.model.dynamics.DirectedConnection(newEdge);
            }
            else
            {
                newEdge = new Edge(newDiagram, new Stub.UndirectedConnection(null, edgeName), inNode, outNode);
                role = new biouml.model.dynamics.UndirectedConnection(newEdge);
            }
            role.setInputPort(new Connection.Port(Util.getPortVariable(inNode), inNode.getTitle()));
            role.setOutputPort(new Connection.Port(Util.getPortVariable(outNode), outNode.getTitle()));
            newEdge.setRole(role);

            newEdge = (Edge)validateDe(newEdge);
            if( conversionFactor != null )
                newEdge.getAttributes().add(conversionFactor);

            //define main variable
            if( role instanceof UndirectedConnection )
            {
                boolean isOutPortMain = secondPortNode.equals(outNode);
                ( (UndirectedConnection)role ).setMainVariableType(isOutPortMain ? MainVariableType.OUTPUT : MainVariableType.INPUT);
                symbol = isOutPortMain ? secondPortPrototype.portSymbol : symbol;
            }
            newDiagram.put(newEdge);
            if( symbol != null )
                linkWithAst(symbol, newEdge.getAttributes());
        }
    }


    private void addNodeInDiagram(NodePrototype prototype) throws Exception
    {
        if( prototype.parent != null && !nodePrototypes.get(prototype.parent.name).existInDiagram )
            addNodeInDiagram(nodePrototypes.get(prototype.parent.name));

        if( !prototype.existInDiagram )
        {
            prototype.existInDiagram = true;
            if( prototype.type == null )
            {
                addPrototypeVariableInDiagram(prototype);
                return;
            }

            Node node = null;

            if( prototype.type.equals(REACTION) )
            {
                addPrototypeReactionInDiagram(prototype);
                return;
            }

            Compartment parent = prototype.parent != null ? (Compartment)newDiagram.findNode(prototype.parent.name) : newDiagram;
            if( prototype.type.equals(AstSymbolType.COMPARTMENT) )
            {
                node = addPrototypeCompartmentInDiagram(prototype, parent);
            }
            else if( isEquation(prototype) )
            {
                node = addPrototypeEquationInDiagram(prototype, parent);
            }
            else if( isLogical(prototype) )
            {
                node = addPrototypeLogicalInDiagram(prototype, parent);
                return;
            }
            else if( prototype.type.equals(AstSymbol.EVENT) )
            {
                node = addPrototypeEventInDiagram(prototype, parent);
            }
            else if( prototype.type.equals(AstSymbol.SUBTYPE) )
            {
                node = addPrototypeEquivalenceInDiagram(prototype, parent);
            }
            else if( prototype.type.equals(AstSymbol.CONSTRAINT) )
            {
                node = addPrototypeConstraintInDiagram(prototype, parent);
            }
            else if( prototype.type.equals(Type.TYPE_PHENOTYPE) )
            {
                node = addPrototypePhenotypeInDiagram(prototype, parent);
            }
            else if( prototype.type.equals(AstSymbolType.SPECIES) || prototype.type.equals(AstSymbolType.GENE) )
            {
                node = addPrototypeSpecieInDiagram(prototype, parent);
                return;
            }
            else if( prototype.type.equals(AstProperty.NOTE) )
            {
                node = addPrototypeNoteInDiagram(prototype, parent);
            }
            else if( prototype.type.equals(AstProperty.TABLE) )
            {
                node = addPrototypeTableInDiagram(prototype, parent);
            }
            else if( prototype.type.equals(AstSymbol.SUBDIAGRAM) )
            {
                addPrototypeSubdiagramInDiagram(prototype); // prototype is added in diagram right away
            }
            if( node != null )
            {

                if( prototype.title != null )
                    node.setTitle(prototype.title);

                parent.put(node);
                linkWithAst(prototype, node.getAttributes());

            }
        }
    }

    private Node addPrototypeTableInDiagram(NodePrototype prototype, Compartment parent)
    {
        SimpleTableElementProperties steb = new SimpleTableElementProperties(prototype.name);
        SimpleTableElement elem = steb.getElement();
        if( prototype.path != null )
        {
            DataElement de = CollectionFactory.getDataElement(prototype.path);
            if( de instanceof TableDataCollection )
                elem.setTable((TableDataCollection)de);
        }

        List<String> availableCols = Arrays.asList(elem.getAvailableColumns());
        if( prototype.argColumn != null )
        {
            VarColumn col = elem.getArgColumn();

            if( prototype.argColumn.get("name") != null && availableCols.contains(prototype.argColumn.get("name")) )
                col.setColumn(prototype.argColumn.get("name"));

            if( prototype.argColumn.get("variable") != null )
                col.setVariable(prototype.argColumn.get("variable"));
        }

        if( prototype.columns != null )
        {
            List<VarColumn> cols = new ArrayList<VarColumn>();
            for( Map<String, String> colProps : prototype.columns )
            {
                VarColumn col = new VarColumn();

                if( colProps.get("name") != null && availableCols.contains(colProps.get("name")) )
                    col.setColumn(colProps.get("name"));

                if( colProps.get("variable") != null )
                    col.setVariable(colProps.get("variable"));

                cols.add(col);
            }
            elem.setColumns(cols.toArray(new VarColumn[0]));

        }

        DiagramElementGroup elements = controller.createInstance(parent, SimpleTableElementProperties.class, prototype.name, null, steb);

        Node node = (Node)elements.getElement();
        if( node == null )
            return null;

        elements.putToCompartment();

        return node;
    }

    private Node addPrototypeNoteInDiagram(NodePrototype prototype, Compartment parent) throws Exception
    {
        InitialElementProperties iep = (InitialElementProperties)controller.getPropertiesByType(parent, "note", null);
        DiagramElementGroup elements = controller.createInstance(parent, Note.class, prototype.name, new Point(0, 0), iep);
        Node note = (Node)elements.getElement();

        if( note == null )
            return null;


        for( AstSymbol symbol : prototype.relatedNodes )
        {
            Node node = (Node)newDiagram.findNode(symbol.getName());
            if( node == null )
                continue;

            Edge edge = new Edge(new Stub.NoteLink(null, symbol.getName()), note, node);
            elements.add(edge);
            linkWithAst(symbol, node.getAttributes());
            linkWithAst(symbol, edge.getAttributes());
        }
        elements.putToCompartment();
        return note;
    }

    private Node addPrototypeEquivalenceInDiagram(NodePrototype prototype, Compartment parent)
    {
        EquivalenceOperatorProperties eop = new EquivalenceOperatorProperties(newDiagram, prototype.name);
        eop.setNodeNames(prototype.nodes.toArray(new String[0]));
        eop.setMainNodeName(prototype.applicationName);

        DiagramElementGroup elements = controller.createInstance(parent, EquivalenceOperatorProperties.class, prototype.name, null, eop);

        Node node = (Node)elements.getElement();
        if( node == null )
            return null;

        elements.putToCompartment();

        return node;
    }

    private Node addPrototypePhenotypeInDiagram(NodePrototype prototype, Compartment parent)
    {
        PhenotypeProperties phenProps = new PhenotypeProperties(newDiagram, prototype.name);
        phenProps.setNodeNames(prototype.modifier.keySet().toArray(new String[0]));

        DiagramElementGroup elements = controller.createInstance(parent, PhenotypeProperties.class, prototype.name, null, phenProps);

        Node node = (Node)elements.getElement();
        if( node == null )
            return null;

        if( prototype.title != null && !prototype.title.isEmpty() )
            node.setTitle(prototype.title);

        elements.putToCompartment();

        for( DiagramElement de : elements.getElements() )
            linkWithAst(prototype.astNodes, de.getAttributes());

        return node;
    }

    private Node addPrototypeLogicalInDiagram(NodePrototype prototype, Compartment parent) throws Exception
    {
        LogicalOperatorProperties lop = new LogicalOperatorProperties(newDiagram, prototype.name);
        lop.setReactionName(prototype.applicationName);
        lop.getProperties().add(new DynamicProperty(SBGNPropertyConstants.SBGN_LOGICAL_OPERATOR_PD, String.class, prototype.type));
        lop.setNodeNames(prototype.nodes.toArray(new String[0]));

        DiagramElementGroup elements = controller.createInstance(parent, LogicalOperatorProperties.class, prototype.name, null, lop);

        Node node = (Node)elements.getElement();
        if( node == null )
            return null;


        elements.putToCompartment();

        for( DiagramElement de : elements.getElements() )
            linkWithAst(prototype.astNodes, de.getAttributes());

        return node;

    }

    private Node addPrototypeConstraintInDiagram(NodePrototype prototype, Compartment parent) throws Exception
    {
        Node node = (Node)controller.createInstance(parent, Constraint.class, prototype.name, null, new Constraint(parent)).getElement();
        if( node == null )
            return null;
        node = (Node)validateDe(node);

        String validFormula = formulaValidate(prototype.formula, node);
        node.getRole(Constraint.class).setFormula(validFormula);
        node.getRole(Constraint.class).setMessage(prototype.message);

        return node;
    }

    private Node addPrototypeSubdiagramInDiagram(NodePrototype prototype) throws Exception
    {
        String subDiagramName = prototype.name.substring(SUBDIAGRAM_PREFIX.length());

        PrototypeModelDefinition prototypeModelDefinition = importedDiagrams.get(prototype.modelDefinitionName);
        if( prototypeModelDefinition == null )
        {
            throw new Exception("Can't find model definition " + prototype.modelDefinitionName);
        }
        Diagram diagram = prototypeModelDefinition.diagram;
        SubDiagram subDiagram = new SubDiagram(newDiagram, diagram, subDiagramName);
        subDiagram.setTitle(subDiagramName);

        subDiagram = (SubDiagram)validateDe(subDiagram);
        for( DynamicProperty dp : prototype.subdiagramProperties )
            subDiagram.getAttributes().add(dp);
        newDiagram.put(subDiagram);
        subDiagram.updatePorts();
        if( prototype.title != null )
            subDiagram.setTitle(prototype.title);
        State state = new State(subDiagram.getDiagram(), subDiagram.getName() + "_state");
        subDiagram.getDiagram().addState(state);
        subDiagram.setState(state);
        linkWithAst(prototype, subDiagram.getAttributes());

        if( prototype.subdiagramParameters != null )
            for( int i = 0; i < prototype.subdiagramParameters.size(); i++ )
            {
                NodePrototype portPrototype = prototype.subdiagramParameters.get(i);
                String typePortParameter;
                try
                {
                    AstSymbol modelDefParameter = prototypeModelDefinition.parameters.get(i);
                    typePortParameter = AntimonyUtility.getOppositePortType(modelDefParameter.getPortType());
                    addPort(portPrototype, typePortParameter);
                }
                catch( Exception e )
                {
                    throw new Exception(prototype.modelDefinitionName + " doesn't have parameter number " + ( i + 1 ));
                }
            }

        Collection<Node> ports = AntimonyUtility.getPortNodes(subDiagram, false);
        for( NodePrototype portPrototype : prototype.elementTitles )
        {
            Node port = ports.stream().filter(
                    p -> Util.getAccessType(p).equals(portPrototype.type) && Util.getPortVariable(p).equals("$" + portPrototype.name))
                    .findFirst().orElse(null);

            if( port != null )
            {
                port.setTitle(portPrototype.title);
                portPrototype.astNodes.forEach(a -> AntimonyAstCreator.link(port, a));
            }
        }
        return subDiagram;
    }

    @SuppressWarnings ( "unchecked" )
    private Node addPrototypeSpecieInDiagram(NodePrototype prototype, Compartment parent) throws Exception
    {
        boolean isGene = prototype.type.equals(AstSymbolType.GENE);
        Node node = (Node)controller.createInstance(parent, Specie.class, prototype.name, null, null).getElement();
        if( node == null )
            return null;
        node = (Node)validateSpecieNode(node, prototype.astNodes,
                TextUtil.nullToEmpty((String)prototype.dynamicProperty.get(AntimonyConstants.SBGN_STRUCTURE)),
                TextUtil.nullToEmpty((String)prototype.dynamicProperty.get(AntimonyConstants.SBGN_TYPE)));

        if( SbgnUtil.isComplex(node) )
            validateComplexComponents(prototype, (Compartment)node);

        //add dynamic property
        for( String property : StreamEx.of(prototype.dynamicProperty.keySet()).without(AntimonyConstants.SBGN_STRUCTURE)
                .without(AntimonyConstants.SBGN_TYPE).without(SBGNPropertyConstants.SBGN_CLONE_MARKER).toSet() )
        {
            putDynamicProperty(node, property, (String)prototype.dynamicProperty.get(property));
        }

        if( isGene )
            ( (Specie)node.getKernel() ).setType(Type.TYPE_NUCLEIC_ACID_FEATURE);
        node.getRole(VariableRole.class).setInitialValue(Double.parseDouble(prototype.initialValue));
        node.getRole(VariableRole.class).setConstant(prototype.isConst);

        if( prototype.databaseReferences != null )
            addDatabaseReferences(node, prototype.databaseReferences);

        // create a unit if it does not exist
        EModel model = newDiagram.getRole(EModel.class);
        if( !model.getUnits().containsKey(prototype.unit) && !prototype.unit.isEmpty()
                && !Unit.getBaseUnitsList().contains(prototype.unit) )
        {
            Unit new_unit = new Unit(null, prototype.unit);
            model.addUnit(new_unit);
        }
        node.getRole(VariableRole.class).setUnits(prototype.unit);
        node.getRole(VariableRole.class).setQuantityType(prototype.quantityType);
        node.getRole(VariableRole.class).setInitialQuantityType(prototype.initialQuantityType);

        if( prototype.title != null )
            node.setTitle(prototype.title);

        parent.put(node);

        if( prototype.dynamicProperty.get(SBGNPropertyConstants.SBGN_CLONE_MARKER) != null )

        {
            List<Map<String, Object>> clones = (List<Map<String, Object>>)prototype.dynamicProperty
                    .get(SBGNPropertyConstants.SBGN_CLONE_MARKER);

            Iterator<Map<String, Object>> iter = clones.iterator();

            Map<String, Object> propForPrimary = iter.next();
            node.setTitle((String)propForPrimary.get("title"));
            parent.put(node);

            while( iter.hasNext() )
            {
                Map<String, Object> prop = iter.next();

                Point newLocation = node.getLocation();
                newLocation.translate(5, 5);

                Node newNode = controller.cloneNode(node, DefaultSemanticController.generateUniqueNodeName(parent, node.getName()),
                        newLocation);
                newNode.setTitle((String)prop.get("title"));
                newNode.save();
                linkWithAst((AstProperty)prop.get("astNode"), newNode.getAttributes());
                if( prop.get("reactions") instanceof List )
                    linkWithAst(filterReactionAstNodes(prototype.astNodes, (List<String>)prop.get("reactions")), newNode.getAttributes());
                else
                    linkWithAst(filterReactionAstNodes(prototype.astNodes, new ArrayList<String>()), newNode.getAttributes());

            }
            linkWithAst((AstProperty)propForPrimary.get("astNode"), node.getAttributes());
            if( propForPrimary.get("reactions") instanceof List )
                linkWithAst(filterReactionAstNodes(prototype.astNodes, (List<String>)propForPrimary.get("reactions")),
                        node.getAttributes());
            else
                linkWithAst(filterReactionAstNodes(prototype.astNodes, new ArrayList<String>()), node.getAttributes());
        }
        else
            linkWithAst(prototype.astNodes, node.getAttributes());

        return node;
    }


    private void validateComplexComponents(NodePrototype prototype, Compartment complex) throws Exception
    {
        for( AstProperty property : prototype.complexComponentsSbgnTypes )
        {
            List<String> names = property.getChainNames();
            String sbgnType = null;
            for( biouml.plugins.antimony.astparser_v2.Node node : property.getChildren() )
                if( node instanceof AstSingleProperty )
                {
                    AstSingleProperty sprop = (AstSingleProperty)node;
                    if( sprop.getPropertyName().equals("type") )
                        sbgnType = sprop.getPropertyValue().toString();
                }

            if( sbgnType == null )
                return;

            Node n = complex;
            Compartment parent = complex;
            for( int ind = 0; ind < names.size() - 1; ind++ )
            {

                String name = names.get(ind);
                if( ! ( name.matches("__sub(_)*([0-9]+)__") || ( n != null && name.equals(n.getName()) ) ) )
                    throw new Exception("Invalid component " + name);

                if( n != null && name.equals(n.getName()) )
                {
                    linkWithAst(property, n.getAttributes());

                    if( ind == names.size() - 1 )
                        break;

                    if( n instanceof Compartment )
                    {
                        parent = (Compartment)n;
                        n = (Node)parent.get(names.get(ind + 1));
                    }
                    else
                        throw new Exception("Invalid component " + name);

                }
                else
                {

                    int num = Integer.parseInt(name.replaceAll("[^0-9]+", ""));
                    int i = 1;
                    int numNodes = parent.getNodes().length;
                    for( DiagramElement de : parent.getNodes() )
                    {
                        if( de instanceof Node && SbgnUtil.isComplex((Node)de) )
                        {
                            if( num == i )
                            {
                                parent = (Compartment)de;
                                n = (Node)parent.get(names.get(ind + 1));
                                linkWithAst(property, parent.getAttributes());
                                break;
                            }
                            else
                                i++;
                        }

                    }
                    if( i > numNodes )
                        throw new Exception("Invalid subcomplex " + name);

                }

            }

            ( (Specie)n.getKernel() ).setType(n.getKernel().getType().equals(AntimonyConstants.STANDARD_GENE) ? AntimonyConstants.SBGN_GENE
                    : sbgnType.isEmpty() ? Type.TYPE_MACROMOLECULE : sbgnType);
            linkWithAst(property, n.getAttributes());
        }
    }

    private Node validateSpecieNode(Node node, Set<biouml.plugins.antimony.astparser_v2.Node> astNodes, String sbgnStructure,
            String sbgnType) throws Exception
    {
        if( node.getKernel() != null && node.getKernel() instanceof Specie )
        {
            node = (Node)validateDe(node, sbgnStructure, sbgnType);

            String currentType = node.getKernel().getType();
            sbgnType = sbgnType.isEmpty() ? Type.TYPE_MACROMOLECULE : sbgnType;

            if( !currentType.equals(sbgnType) && astNodes != null )
            {
                boolean toAddSingleProperty = true;
                for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
                {
                    if( astNode instanceof AstProperty )
                    {
                        AstProperty property = (AstProperty)astNode;
                        if( !property.getNotationType().equals("sbgn") )
                            continue;

                        for( biouml.plugins.antimony.astparser_v2.Node child : property.getChildren() )
                        {
                            if( child instanceof AstSingleProperty )
                            {

                                AstSingleProperty sprop = (AstSingleProperty)child;
                                if( sprop.getPropertyName().equals("type") )
                                {
                                    sprop.setPropertyValue(currentType);
                                    return node;
                                }
                            }
                        }

                        if( toAddSingleProperty )
                            property.addSingleProperty("type", currentType);


                    }
                }

            }
        }
        return node;
    }

    private Set<biouml.plugins.antimony.astparser_v2.Node> filterReactionAstNodes(Set<biouml.plugins.antimony.astparser_v2.Node> astNodes,
            List<String> list)
    {
        Set<biouml.plugins.antimony.astparser_v2.Node> set = new HashSet<>();
        set.addAll(astNodes);
        for( biouml.plugins.antimony.astparser_v2.Node node : astNodes )
        {
            if( node.jjtGetParent() instanceof AstReactant || node.jjtGetParent() instanceof AstProduct )
            {
                SimpleNode currentNode = (SimpleNode)node.jjtGetParent();
                while( ! ( currentNode instanceof AstStart ) )
                {
                    currentNode = (SimpleNode)currentNode.jjtGetParent();
                    if( AntimonyUtility.isReactionAstSymbol(currentNode) && !list.contains( ( (AstSymbol)currentNode ).getName()) )
                    {
                        set.remove(node);
                        break;
                    }
                }

            }
        }
        return set;
    }

    private void putDynamicProperty(Node node, String property, String value)
    {
        DynamicPropertySet attributes = node.getAttributes();
        DynamicProperty dp = attributes.getProperty(property);
        if( dp == null )
            attributes.add(new DynamicProperty(property, String.class, value));
        else
            dp.setValue(TextUtil.fromString(dp.getType(), value));

    }

    private Node addPrototypeEventInDiagram(NodePrototype prototype, Compartment parent) throws Exception
    {
        String name = prototype.name;
        if( prototype.name.startsWith(NO_NAME) )
            name = AntimonyUtility.generateName(newDiagram, new DecimalFormat("_E0"));
        Assignment[] assignment = new Assignment[0];
        Node node = (Node)controller.createInstance(parent, Event.class, name, null, new Event(null, null, null, assignment)).getElement();
        if( node == null )
            return null;
        node = (Node)validateDe(node);
        assignment = new Assignment[prototype.assignmentsVariable.size()];

        for( int i = 0; i < assignment.length; i++ )
        {
            NodePrototype varPrototype = nodePrototypes.get(prototype.assignmentsVariable.get(i));
            String validVar;
            if( varPrototype != null && varPrototype.type != null && ( AntimonyUtility.isVariableRole(varPrototype.type) ) )
                validVar = "$" + prototype.assignmentsVariable.get(i);
            else
                validVar = prototype.assignmentsVariable.get(i);
            assignment[i] = new Assignment(null, formulaValidate(prototype.assignmentsEquation.get(i), node), (Option)node.getRole());
            assignment[i].setVariable(validVar);
        }
        Event event = node.getRole(Event.class);
        event.setEventAssignment(assignment);
        event.setTrigger(formulaValidate(prototype.trigger, node));
        event.setPriority(formulaValidate(prototype.priority, node));
        event.setTriggerInitialValue(prototype.triggerInitialValues);
        event.setTriggerPersistent(prototype.persistent);
        event.setUseValuesFromTriggerTime(prototype.useValuesFromTriggerTime);

        if( prototype.delay != null )
            event.setDelay(formulaValidate(prototype.delay, node));
        return node;
    }

    private Node addPrototypeEquationInDiagram(NodePrototype prototype, Compartment parent) throws Exception
    {
        NodePrototype varPrototype = nodePrototypes.get(prototype.variable);
        if( varPrototype != null && varPrototype.type != null && ( AntimonyUtility.isVariableRole(varPrototype.type) ) )
            prototype.variable = "$" + prototype.variable;

        String name = DefaultSemanticController.generateUniqueNodeName(newDiagram, prototype.name);
        Node node = (Node)controller.createInstance(parent, Equation.class, name, null, new Equation(null, prototype.type, null, null))
                .getElement();
        if( node == null )
            return null;
        node = (Node)validateDe(node);
        String validFormula = formulaValidate(prototype.equation, node);
        node.getRole(Equation.class).setFormula(validFormula);
        if( node.getRole(Equation.class).getType() != Equation.TYPE_ALGEBRAIC )
            node.getRole(Equation.class).setVariable(prototype.variable);
        return node;
    }

    private Node addPrototypeCompartmentInDiagram(NodePrototype prototype, Compartment parent) throws Exception
    {
        Node node = (Node)controller.createInstance(parent, biouml.standard.type.Compartment.class, prototype.name, null, null)
                .getElement();
        if( node == null )
            return null;

        node = (Node)validateDe(node);

        if( prototype.databaseReferences != null )
            addDatabaseReferences(node, prototype.databaseReferences);

        node.getRole(VariableRole.class).setInitialValue(Double.parseDouble(prototype.initialValue));
        node.getRole(VariableRole.class).setConstant(prototype.isConst);
        return node;
    }

    private Compartment reactionParent;
    private Node addPrototypeReactionInDiagram(NodePrototype prototype) throws Exception
    {
        String reactionName = prototype.name;
        if( prototype.name.startsWith(NO_NAME) )
            reactionName = AntimonyUtility.generateName(newDiagram, new DecimalFormat("_J0"));
        Reaction reaction = new Reaction(null, reactionName);
        for( String refName : prototype.reactants.keySet() )
            reaction.put(
                    addSpecieReference(reactionName, refName, generator, SpecieReference.REACTANT, prototype.reactants.get(refName), null));

        for( String refName : prototype.products.keySet() )
            reaction.put(
                    addSpecieReference(reactionName, refName, generator, SpecieReference.PRODUCT, prototype.products.get(refName), null));

        for( String refName : prototype.modifier.keySet() )
            reaction.put(addSpecieReference(reactionName, refName, generator, SpecieReference.MODIFIER, prototype.modifier.get(refName)[0],
                    prototype.modifier.get(refName)[1]));

        //TODO: put all elements from DiagramElementGroup
        DiagramElementGroup reactionElements = controller.createInstance(reactionParent, Reaction.class, reactionName, new Point(0, 0),
                reaction);

        Node reactionNode = (Node)reactionElements.getElement(Util::isReaction);
        if( reactionNode == null )
            return null;

        if( prototype.title != null && !prototype.title.isEmpty() )
            reactionNode.setTitle(prototype.title);

        if( prototype.databaseReferences != null )
            addDatabaseReferences(reactionNode, prototype.databaseReferences);

        // put reaction to diagram now
        reactionElements.putToCompartment();

        if( prototype.reactionFormula != null )
        {
            String validFormula = formulaValidate(prototype.reactionFormula, reactionNode);
            ( (Reaction)reactionNode.getKernel() ).setFormula(validFormula);
        }
        if( prototype.isReversible != null )
            ( (Reaction)reactionNode.getKernel() ).setReversible(prototype.isReversible);

        if( prototype.dynamicProperty.get(SBGNPropertyConstants.SBGN_REACTION_TYPE) != null )
            reactionNode.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_REACTION_TYPE, String.class,
                    prototype.dynamicProperty.get(SBGNPropertyConstants.SBGN_REACTION_TYPE)));
        reactionParent = null;
        linkWithAst(prototype, reactionNode.getAttributes());
        return reactionNode;
    }

    private void addDatabaseReferences(Node node, Map<String, List<String>> databaseReferences)
    {
        if( ! ( node.getKernel() instanceof Referrer ) )
            return;
        List<DatabaseReference> drs = new ArrayList<DatabaseReference>();
        for( Map.Entry<String, List<String>> entry : databaseReferences.entrySet() )
        {
            for( String URI : entry.getValue() )
            {
                if( !URI.startsWith("http://identifiers.org/") )
                    continue;
                String[] dbname_id = URI.substring(23).split("/");

                DatabaseReference dr = new DatabaseReference(dbname_id[0], dbname_id[1]);
                dr.setRelationshipType(entry.getKey());
                drs.add(dr);
            }
        }
        ( (Referrer)node.getKernel() ).setDatabaseReferences(drs.toArray(new DatabaseReference[0]));
    }

    private void addPrototypeVariableInDiagram(NodePrototype prototype)
    {
        EModel model = newDiagram.getRole(EModel.class);
        Variable var = new Variable(prototype.name, model, model.getVariables());
        var.setInitialValue(Double.parseDouble(prototype.initialValue));
        var.setConstant(prototype.isConst);

        if( !model.getUnits().containsKey(prototype.unit) && !prototype.unit.isEmpty()
                && !Unit.getBaseUnitsList().contains(prototype.unit) )
        {
            Unit new_unit = new Unit(null, prototype.unit);
            model.addUnit(new_unit);
        }
        var.setUnits(prototype.unit);
        model.put(var);
        linkWithAst(prototype, var.getAttributes());
    }

    protected String formulaValidate(AstEquation equation, Node node) throws Exception
    {
        if( equation == null )
            return "";
        ru.biosoft.math.model.AstStart astStart = newDiagram.getRole(EModel.class).readMath(formatEquationFormula(equation), node.getRole(),
                EModel.VARIABLE_NAME_BY_ID);
        if( astStart == null )
            throw new Exception("Incorrect formula");
        return new LinearFormatter().format(astStart)[1];
    }
    private String formatEquationFormula(@Nonnull
    AstEquation equation) throws Exception
    {
        StringBuffer formula = new StringBuffer("");
        for( int i = 0; i < equation.jjtGetNumChildren(); i++ )
        {
            biouml.plugins.antimony.astparser_v2.Node child = equation.jjtGetChild(i);
            if( child instanceof AstSpecialFormula )
            {
                formula.append(formatEquationFormula( ( (AstSpecialFormula)child ).convertToEquation()));
                continue;
            }
            String nameFormulaElement = child.toString();
            if( child instanceof AstRegularFormulaElement && ! ( (AstRegularFormulaElement)child ).isNumber() )
            {
                Node associatedNode = newDiagram.findNode(nameFormulaElement);
                if( associatedNode != null && associatedNode.getRole() instanceof VariableRole )
                    formula.append("$");
            }
            formula.append(nameFormulaElement);
        }
        return formula.toString();
    }

    @SuppressWarnings ( "unchecked" )
    protected SpecieReference addSpecieReference(String reactionName, String refName, DiagramGenerator generator, String refType,
            String stoichiometry, String modifireType) throws Exception
    {
        NodePrototype refPrototype = nodePrototypes.get(refName);
        if( !refPrototype.existInDiagram )
            addNodeInDiagram(refPrototype);

        if( refPrototype.dynamicProperty.get(SBGNPropertyConstants.SBGN_CLONE_MARKER) != null )
            refName = getCloneName(refName, reactionName,
                    (List<Map<String, Object>>)refPrototype.dynamicProperty.get(SBGNPropertyConstants.SBGN_CLONE_MARKER));

        Node specie = newDiagram.findNode(refName);

        if( reactionParent != null )
            reactionParent = findCommonOrigin(reactionParent, specie);
        else
            reactionParent = (Compartment)specie.getOrigin();
        SpecieReference specieReference = generator.createSpeciesReference(specie, refType);
        if( refType.equals(SpecieReference.MODIFIER) && AstSymbol.REACTION_INHIBITOR.equals(modifireType) )
        {
            specieReference.setModifierAction(SpecieReference.ACTION_INHIBITION);
        }
        else if( refType.equals(SpecieReference.MODIFIER) && AstSymbol.REACTION_ACTIVATOR.equals(modifireType) )
        {
            specieReference.setModifierAction(SpecieReference.ACTION_CATALYSIS);
        }

        specieReference.setStoichiometry(stoichiometry);
        return specieReference;
    }

    @SuppressWarnings ( "unchecked" )
    private String getCloneName(String refName, String reactionName, List<Map<String, Object>> listOfCloneProps)
    {
        Node specie = newDiagram.findNode(refName);
        DiagramElement[] des = specie.getRole(VariableRole.class).getAssociatedElements();

        if( des == null )
            return refName;

        for( DiagramElement de : des )
        {
            DynamicProperty dp = de.getAttributes().getProperty(AntimonyConstants.ANTIMONY_LINK);
            if( dp == null )
                return refName;
            Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = (Set<biouml.plugins.antimony.astparser_v2.Node>)dp.getValue();

            if( astNodes == null )
                return refName;

            for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
            {
                if( astNode instanceof AstProperty )
                {
                    for( biouml.plugins.antimony.astparser_v2.Node child : ( (SimpleNode)astNode ).getChildren() )
                    {
                        if( child instanceof AstSingleProperty
                                && ( (AstSingleProperty)child ).getPropertyName().equals(AntimonyConstants.SBGN_CLONE) )
                        {
                            for( biouml.plugins.antimony.astparser_v2.Node elem : ( (AstSingleProperty)child ).getValueNode()
                                    .getChildren() )
                            {
                                if( elem instanceof AstSingleProperty && ( (AstSingleProperty)elem ).getPropertyName().equals("reactions") )
                                {
                                    for( biouml.plugins.antimony.astparser_v2.Node node : ( (AstSingleProperty)elem ).getValueNode()
                                            .getChildren() )
                                    {
                                        if( node instanceof AstSymbol && ( (AstSymbol)node ).getName().equals(reactionName) )
                                        {
                                            getNodePrototype((AstSymbol)node, REACTION, false); // it links symbol to node prototype
                                            return de.getName();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return refName;
    }

    private @Nonnull Compartment findCommonOrigin(@Nonnull
    Compartment first, DiagramElement second)
    {
        if( second.getOrigin() == null )
            return newDiagram;
        DataElementPath secondPath = second.getOrigin().getCompletePath();
        while( true )
        {
            DataElementPath path = first.getCompletePath();
            if( secondPath.isDescendantOf(path) )
                return first;

            if( first.getOrigin() instanceof Compartment )
                first = (Compartment)first.getOrigin();
            else
                return newDiagram;
        }
    }

    private void addAntimonyElement(biouml.plugins.antimony.astparser_v2.Node currentNode) throws Exception
    {
        if( currentNode instanceof AstEOL )
            return;
        else if( currentNode instanceof AstSemicolon )
            return;
        else if( currentNode instanceof AstGlobal )
        {
            //TODO: add global variable
        }
        else if( currentNode instanceof AstProperty )
        {
            addProperty((AstProperty)currentNode);
        }
        else if( AntimonyUtility.isAstSymbol(currentNode) )
        {
            AstSymbol symbol = (AstSymbol)currentNode;
            if( symbol.getDeclarationType() == null )
                return;
            else if( AntimonyUtility.isReactionAstSymbol(symbol) )
            {
                addReactionProperties(symbol);
            }
            else if( AntimonyUtility.isModifierAstSymbol(symbol) )
            {
                addModifier(symbol);
            }
            else if( AntimonyUtility.isDatabaseReferenceAstSymbol(symbol) )
            {
                addDatabaseReference(symbol);
            }
            else if( symbol.getDeclarationType().equals(AstSymbol.PARENT_DECLARATION) )
            {
                NodePrototype nodePrototype;
                if( symbol.isConstantPrefix() )
                    nodePrototype = getNodePrototype(symbol, null, true);
                else
                    nodePrototype = getNodePrototype(symbol, null, null);
                addParent(symbol, nodePrototype);
            }
            else if( symbol.getDeclarationType().equals(AstSymbol.DISPLAY_NAME) )
            {
                NodePrototype nodePrototype = getNodePrototype(symbol, null, null);
                addDisplayName(symbol, nodePrototype);
            }
            else if( AntimonyUtility.isEquationAstSymbol(symbol) )
            {
                addEquation(symbol);
            }
            else if( AntimonyUtility.isEventAstSymbol(symbol) )
            {
                addEvent(symbol);
            }
            else if( AntimonyUtility.isConstraintAstSymbol(symbol) )
            {
                addConstraint(symbol);
            }
            else if( AntimonyUtility.isSubtypeAstSymbol(symbol) )
            {
                addSubtype(symbol);
            }
            else if( AntimonyUtility.isSubdiagramAstSymbol(symbol) )
            {
                addSubdiagram(symbol);
                setCompositeType();
            }
            else if( AntimonyUtility.isSynchronizationsSymbol(symbol) )
            {
                addSynchronizationsLink(symbol);
            }
            else if( AntimonyUtility.isHasUnitAstSymbol(symbol) )
            {
                addUnitAssignment(symbol);
            }
            else if( AntimonyUtility.isFunctionReturnAssignmentAstSymbol(symbol) )
            {
                addAntimonyVariable(symbol);
            }
        }
        else if( currentNode instanceof AstVarOrConst )
        {
            AstSymbolType symbolType = ( (AstVarOrConst)currentNode ).getSymbolType();
            if( symbolType != null )
                addAllSymbols(symbolType, symbolType.getType(), ( (AstVarOrConst)currentNode ).isConst(), false);
            else
                addAllSymbols(currentNode, null, ( (AstVarOrConst)currentNode ).isConst(), false);
        }
        else if( AntimonyUtility.isAstSymbolType(currentNode) )
        {
            AstSymbolType symbolType = (AstSymbolType)currentNode;
            addAllSymbols(symbolType, symbolType.getType(), null, false);
        }
        else if( AntimonyUtility.isAstSubstanceOnly(currentNode) )
        {
            AstSymbolType symbolType = (AstSymbolType)currentNode.jjtGetChild(0);
            addAllSymbols(symbolType, symbolType.getType(), null, true);
        }
        else if( currentNode instanceof AstModel )
        {
            addModel((AstModel)currentNode);
            setCompositeType();
        }
        else if( currentNode instanceof AstImport )
        {
            addModel((AstImport)currentNode);
            setCompositeType();
        }
        else if( currentNode instanceof AstDelete )
        {
            saveDeletedElement((AstDelete)currentNode);
            setCompositeType();
        }
    }

    private void addAntimonyVariable(AstSymbol symbol) throws Exception
    {
        for( biouml.plugins.antimony.astparser_v2.Node child : symbol.getChildren() )
        {
            if( child instanceof AstLocateFunction )
            {
                AstLocateFunction node = (AstLocateFunction)child;
                String type = node.getProperty("type");

                for( biouml.plugins.antimony.astparser_v2.Node n : node.getChildren() )
                {
                    if( n instanceof AstSymbol && ( (AstSymbol)n ).getName().equals(node.getProperty("name")) )
                    {
                        if( type.contains(PORT) )
                        {
                            String normalizedType;
                            try
                            {
                                normalizedType = getValidPortType(type);
                                getNodePrototype((AstSymbol)n, normalizedType, false);
                                NodePrototype portPrototype = getNodePrototype(node.getProperty("name") + ":" + normalizedType);
                                portPrototype.astNodes.add(symbol);
                                nodePrototypes.put(symbol.getName(), portPrototype);
                            }
                            catch( Exception e )
                            {
                                log.log(Level.INFO, e.getMessage());
                            }
                        }
                        break;
                    }
                }
            }
        }


    }

    //    private void addAnnotation(AstImportAnnotation node, DataCollection<?> dataCollection) throws Exception
    //    {
    //        String path;
    //        if( node.getPath().contains("/") )
    //            path = node.getPath();
    //        else
    //            path = dataCollection.getCompletePath().getChildPath(node.getPath()).toString();
    //        AntimonyAnnotationImporter.setAnnotation(path, node.getAnnotationType());
    //    }

    private void addDatabaseReference(AstSymbol symbol)
    {
        NodePrototype nodePrototype = getNodePrototype(symbol.getName());
        if( nodePrototype != null )
        {
            String keyName = null;

            for( int i = 0; i < symbol.jjtGetNumChildren(); i++ )
            {
                biouml.plugins.antimony.astparser_v2.Node child = symbol.jjtGetChild(i);
                if( child instanceof AstRelationshipType )
                {
                    keyName = ( (AstRelationshipType)child ).getName();
                    if( nodePrototype.databaseReferences.get(keyName) == null )
                        nodePrototype.databaseReferences.put(keyName, new ArrayList<String>());
                }
                else if( child instanceof AstText )
                    nodePrototype.databaseReferences.get(keyName).add( ( (AstText)child ).getText().replaceAll("\"", ""));
            }
            nodePrototype.astNodes.add(symbol);
        }

    }

    private void addSubtype(AstSymbol symbol)
    {
        String superName = symbol.jjtGetChild(0).jjtGetChild(0).toAntimonyString();
        String subName = symbol.toAntimonyString();
        String name = superName + "_equivalent";

        NodePrototype nodePrototype = getNodePrototype(name);
        if( nodePrototype == null )
        {
            nodePrototype = new NodePrototype(name);

            nodePrototypes.put(name, nodePrototype);
            nodePrototype.type = AstSymbol.SUBTYPE;
            nodePrototype.nodes = new ArrayList<>();
        }

        nodePrototype.astNodes.add(symbol);
        nodePrototype.nodes.add(subName);
        nodePrototype.applicationName = superName;

        NodePrototype subPrototype = getNodePrototype(subName);
        subPrototype.astNodes.add(symbol);
        NodePrototype superPrototype = getNodePrototype(superName);
        superPrototype.astNodes.add(symbol);
    }

    private void addConstraint(AstSymbol symbol)
    {
        NodePrototype nodePrototype = new NodePrototype(symbol.getName());

        nodePrototype.type = AstSymbol.CONSTRAINT;
        for( biouml.plugins.antimony.astparser_v2.Node node : symbol.getAllChildren() )
        {
            if( node instanceof AstText )
                nodePrototype.message = ( (AstText)node ).getText();
            else if( node instanceof AstAssert )
            {
                if( node.jjtGetChild(0) instanceof AstEquation )
                    nodePrototype.formula = (AstEquation)node.jjtGetChild(0);
            }
        }

        nodePrototype.astNodes.add(symbol);
        nodePrototypes.put(nodePrototype.name, nodePrototype);
    }

    private void addUnitAssignment(AstSymbol unitAssignNode)
    {
        NodePrototype nodePrototype = getNodePrototype(unitAssignNode.getName());
        if( nodePrototype != null )
        {
            String unitName = "";

            for( int i = 0; i < unitAssignNode.jjtGetNumChildren(); i++ )
                if( unitAssignNode.jjtGetChild(i) instanceof AstHas )
                    unitName = ( (AstHas)unitAssignNode.jjtGetChild(i) ).getUnitName();

            nodePrototype.unit = unitName;

            nodePrototype.astNodes.add(unitAssignNode);
        }

    }

    private void addUnit(AstUnit unitNode) throws Exception
    {
        EModel emodel = newDiagram.getRole(EModel.class);
        AstUnitFormula equation = unitNode.getUnitFormula();

        Unit unit = new Unit(null, unitNode.getName());

        if( equation != null )
        {
            ru.biosoft.math.unitparser.AstStart astStart = UnitCalculator.readMath(equation.getFormula());
            if( astStart == null )
                throw new Exception("Incorrect formula: " + equation.getFormula());
            unit.setBaseUnits(UnitCalculator.getBaseUnits(astStart));
        }


        linkWithAst(unitNode, unit.getAttributes());
        emodel.addUnit(unit);
    }

    @SuppressWarnings ( "unchecked" )
    private void addProperty(AstProperty propertyNode) throws Exception
    {
        String notationType = propertyNode.getNotationType();

        if( !AntimonyAnnotationImporter.annotations.containsKey(notationType) )
            return;


        NodePrototype nodePrototype = null;
        boolean isNote = AstProperty.NOTE.equals(propertyNode.getDeclarationType());
        boolean isTable = AstProperty.TABLE.equals(propertyNode.getDeclarationType());
        List<String> names = propertyNode.getChainNames();


        if( propertyNode.hasImplicitName() )
            nodePrototype = locateNodePrototype(propertyNode);
        else if( isNote || isTable )
        {
            String name = propertyNode.getChainNames().get(0);
            nodePrototype = new NodePrototype(name);
            nodePrototype.type = propertyNode.getDeclarationType();
            nodePrototypes.put(name, nodePrototype);
        }
        else if( names.size() == 1 ) //if it's not a subentity 
            nodePrototype = getNodePrototype(names.get(0));


        Map<String, Object> properties = AntimonyAnnotationImporter.annotations.get(notationType);

        if( nodePrototype != null )
        {
            String type = nodePrototype.type;

            String singlePropName = "";
            switch( notationType )
            {
                case ( "sbgn" ):
                {
                    switch( type )
                    {
                        case AstSymbolType.SPECIES:
                        case AstSymbolType.GENE:
                        case REACTION:
                        {
                            singlePropName = notationType;
                            break;
                        }
                    }
                    break;
                }
                case ( "glycan" ):
                {
                    singlePropName = "glycan";
                    break;
                }
                case ( "smiles" ):
                {
                    singlePropName = "smiles";
                    break;
                }
                case ( "biouml" ):
                    break;
                // not implemented yet
                case ( "rdf" ):
                case ( "layout" ):
                    break;
                default:
                    throw new Exception("Notation type \"" + type + "\" does not exist");
            }

            if( isNote )
            {
                if( propertyNode.getSinglePropety("text") != null )
                    nodePrototype.title = (String)propertyNode.getSinglePropety("text").getPropertyValue();

                if( propertyNode.getSinglePropety("nodes") != null )
                {
                    for( biouml.plugins.antimony.astparser_v2.Node n : ( (AstList)propertyNode.getSinglePropety("nodes").getValueNode() )
                            .getChildren() )
                        if( n instanceof AstSymbol )
                            nodePrototype.relatedNodes.add((AstSymbol)n);
                }

                nodePrototype.astNodes.add(propertyNode);
            }
            else if( isTable )
            {
                // TODO: validate path
                if( propertyNode.getSinglePropety("path") != null )
                    nodePrototype.path = (String)propertyNode.getSinglePropety("path").getPropertyValue();


                if( propertyNode.getSinglePropety("argColumn") != null )
                {
                    nodePrototype.argColumn = new HashMap<String, String>();
                    Map<String, Object> vals = (Map<String, Object>)propertyNode.getSinglePropety("argColumn").getPropertyValue();
                    if( vals.get("variable") instanceof String )
                        nodePrototype.argColumn.put("variable", (String)vals.get("variable"));

                    if( vals.get("name") instanceof String )
                        nodePrototype.argColumn.put("name", (String)vals.get("name"));
                }

                if( propertyNode.getSinglePropety("columns") != null )
                {
                    nodePrototype.columns = new HashSet<Map<String, String>>();
                    Set<Object> colsSet = (Set<Object>)propertyNode.getSinglePropety("columns").getPropertyValue();

                    for( Object obj : colsSet )
                    {
                        if( ! ( obj instanceof Map ) )
                            continue;

                        nodePrototype.columns.add((Map<String, String>)obj);
                    }
                }
                nodePrototype.astNodes.add(propertyNode);
            }
            else
            {
                for( biouml.plugins.antimony.astparser_v2.Node node : propertyNode.getChildren() )
                    if( node instanceof AstSingleProperty )
                    {
                        AstSingleProperty sprop = (AstSingleProperty)node;
                        String propertyName = sprop.getPropertyName();

                        if( !properties.containsKey(propertyName) )
                            continue;

                        AntimonyAnnotationImporter.validatePropertyValue(sprop, notationType);

                        if( Type.TYPE_PHENOTYPE.equals(sprop.getPropertyValue()) )
                        {
                            nodePrototype.type = Type.TYPE_PHENOTYPE;
                            nodePrototype.astNodes.add(propertyNode);
                            continue;
                        }

                        switch( propertyName )
                        {
                            case ( "multimer" ):
                            {
                                propertyName = singlePropName + ":multimer";
                                break;
                            }
                            case ( "clone" ):
                            {
                                propertyName = singlePropName + ":cloneMarker";
                                break;
                            }
                            case ( "reactionType" ):
                            {
                                propertyName = singlePropName + ":reactionType";
                                break;
                            }
                            default:
                                propertyName = singlePropName.isEmpty() ? sprop.getPropertyName() : singlePropName
                                        + sprop.getPropertyName().substring(0, 1).toUpperCase() + sprop.getPropertyName().substring(1);
                        }

                        if( SBGNPropertyConstants.SBGN_CLONE_MARKER.equals(propertyName) )
                        {
                            if( nodePrototype.dynamicProperty.get(propertyName) == null )
                                nodePrototype.dynamicProperty.put(propertyName, new ArrayList<Map<String, Object>>());

                            HashMap<String, Object> newValue = (HashMap<String, Object>)sprop.getPropertyValue();
                            newValue.put("astNode", propertyNode);
                            ( (List<HashMap<String, Object>>)nodePrototype.dynamicProperty.get(propertyName) ).add(newValue);
                        }
                        else if( "title".equals(propertyName) )
                        {
                            nodePrototype.title = sprop.getPropertyValue().toString();
                            nodePrototype.astNodes.add(propertyNode);
                        }
                        else
                        {
                            nodePrototype.dynamicProperty.put(propertyName, sprop.getPropertyValue());
                            nodePrototype.astNodes.add(propertyNode);
                        }

                    }
            }

        }
        else if( getEdgePrototype(names.get(0)) != null )
        {
            EdgePrototype edgePrototype = getEdgePrototype(names.get(0));
            if( propertyNode.getNotationType().equals("sbgn") )
                for( biouml.plugins.antimony.astparser_v2.Node node : propertyNode.getChildren() )
                    if( node instanceof AstSingleProperty )
                    {
                        AstSingleProperty sprop = (AstSingleProperty)node;
                        if( sprop.getPropertyName().equals("edgeType") )
                            edgePrototype.modAction = sprop.getPropertyValue().toString();
                        edgePrototype.astNodes.add(propertyNode);

                        nodePrototype = getNodePrototype(edgePrototype.inputName);
                        if( nodePrototype != null && isLogical(nodePrototype) )
                            nodePrototype.astNodes.add(propertyNode);
                    }

        }
        else if( names.size() > 1 )
        {
            nodePrototype = getNodePrototype(names.get(0));
            if( nodePrototype == null )
                return;

            if( propertyNode.getNotationType().equals("sbgn") )
                for( biouml.plugins.antimony.astparser_v2.Node node : propertyNode.getChildren() )
                    if( node instanceof AstSingleProperty && ( (AstSingleProperty)node ).getPropertyName().equals("type") )
                    {
                        nodePrototype.complexComponentsSbgnTypes.add(propertyNode);
                    }
        }
        else
        {
            if( propertyNode.getNotationType().equals("biouml") )
            {
                for( biouml.plugins.antimony.astparser_v2.Node node : propertyNode.getChildren() )
                    if( node instanceof AstSingleProperty )
                    {
                        AstSingleProperty sprop = (AstSingleProperty)node;
                        String propertyName = sprop.getPropertyName();

                        if( !properties.containsKey(propertyName) )
                            throw new Exception("Property \"" + propertyName + "\" was not imported");

                        AntimonyAnnotationImporter.validatePropertyValue(sprop, notationType);

                        if( "bus".equals(propertyName) )
                        {
                            String name = propertyNode.getChainNames().get(0);
                            if( busNodePrototypes.get(name) == null )
                                busNodePrototypes.put(name, new ArrayList<NodePrototype>());

                            nodePrototype = getNodePrototype(name, "bus");
                            nodePrototype.astNodes.add(propertyNode);

                            HashMap<String, Object> propertyMap = (HashMap<String, Object>)sprop.getPropertyValue();
                            if( propertyMap.get("title") != null )
                                nodePrototype.title = (String)propertyMap.get("title");

                            if( propertyMap.get("ports") != null )
                            {
                                SimpleNode mapNode = sprop.getValueNode();
                                for( int i = 0; i < mapNode.jjtGetNumChildren(); i++ )
                                {
                                    if( mapNode.jjtGetChild(i) instanceof AstSingleProperty
                                            && ( (AstSingleProperty)mapNode.jjtGetChild(i) ).getPropertyName().equals("ports") )
                                    {
                                        SimpleNode listNode = ( (AstSingleProperty)mapNode.jjtGetChild(i) ).getValueNode();
                                        for( biouml.plugins.antimony.astparser_v2.Node sym : listNode.getChildren() )
                                        {
                                            if( sym instanceof AstSymbol )
                                                nodePrototype.ports.add((AstSymbol)sym);
                                        }
                                    }
                                }

                            }

                            if( propertyMap.get("directed") != null )
                                nodePrototype.directed = propertyMap.get("directed").toString().equals("true");

                            busNodePrototypes.get(name).add(nodePrototype);
                        }
                    }


            }
        }

    }


    Set<AstDelete> deletedElements = new HashSet<>();
    private void saveDeletedElement(AstDelete currentNode)
    {
        deletedElements.add(currentNode);
    }

    private void setCompositeType()
    {
        SemanticController sc = newDiagram.getType().getSemanticController();
        if( sc instanceof SbgnSemanticController )
        {
            if( !DiagramUtility.isComposite(newDiagram) )
            {
                newDiagram.setType(new SbgnCompositeDiagramType());
                controller = (CreatorElementWithName)newDiagram.getType().getSemanticController();
            }
        }
    }

    private final Set<LinkedPair> synchronizedPairs = new HashSet<>();

    private void addSynchronizationsLink(AstSymbol symbol)
    {
        try
        {
            NodePrototype firstPortPrototype = getSynchronizationPort(symbol);

            for( int i = 0; i < symbol.jjtGetNumChildren(); i++ )
                if( symbol.jjtGetChild(i) instanceof AstIs )
                {
                    SimpleNode is = (SimpleNode)symbol.jjtGetChild(i);
                    AstSymbol secondSymbol = ( (AstIs)is ).getSynchronizedElement();
                    NodePrototype secondPortPrototype = getSynchronizationPort(secondSymbol);
                    LinkedPair pair = new LinkedPair(firstPortPrototype, secondPortPrototype);
                    synchronizedPairs.add(pair);
                    break;
                }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can't add synchronization link: " + e.getMessage());
        }
    }

    /**
     * Returns a port prototype (and creates if it does not exist)
     * @param symbol
     * @throws Exception
     */
    private NodePrototype getSynchronizationPort(AstSymbol symbol)
    {
        int length = symbol.getChainNames().length;
        String type = ( length == 2 ) ? ConnectionPort.PUBLIC : ( length == 1 ) ? ConnectionPort.PRIVATE : ConnectionPort.PROPAGATED;

        if( length >= 1 )
        {
            NodePrototype portPrototype = getNodePrototype(symbol, type, null);
            portPrototype.portSymbol = symbol;
            return portPrototype;
        }
        else
        {
            log.log(Level.SEVERE, "Can't add " + type + " " + symbol.getName() + " since variable does not exist.");
        }
        return null;

    }
    private void addSubdiagram(AstSymbol symbol) throws Exception
    {
        NodePrototype nodePrototype = new NodePrototype(SUBDIAGRAM_PREFIX + symbol.getName());
        nodePrototype.type = AstSymbol.SUBDIAGRAM;
        nodePrototype.subdiagramParameters = new ArrayList<>();
        nodePrototype.elementTitles = new ArrayList<NodePrototype>();
        for( int j = 0; j < symbol.jjtGetNumChildren(); j++ )
            if( symbol.jjtGetChild(j) instanceof AstSymbol )
            {
                AstSymbol diagramSymbol = (AstSymbol)symbol.jjtGetChild(j);
                nodePrototype.modelDefinitionName = diagramSymbol.getName();
                PrototypeModelDefinition prototypeModelDefinition = importedDiagrams.get(nodePrototype.modelDefinitionName);
                if( prototypeModelDefinition == null )
                {
                    throw new Exception("Can't find model definition " + nodePrototype.modelDefinitionName);
                }
                for( int i = 0; i < diagramSymbol.jjtGetNumChildren(); i++ )
                    if( diagramSymbol.jjtGetChild(i) instanceof AstSymbol )
                    {
                        AstSymbol parameterSymbol = (AstSymbol)diagramSymbol.jjtGetChild(i);
                        // add variable and private port prototype for parameter  
                        getNodePrototype(parameterSymbol, null, null);
                        NodePrototype portPrototype = getNodePrototype(parameterSymbol, ConnectionPort.PRIVATE, null);
                        portPrototype.portSymbol = parameterSymbol;
                        nodePrototype.subdiagramParameters.add(portPrototype);

                        try
                        {
                            AstSymbol modelDefParameter = prototypeModelDefinition.parameters
                                    .get(nodePrototype.subdiagramParameters.size() - 1);
                            AstSubSymbol modelDefSymbol = new AstSubSymbol(AntimonyNotationParser.JJTSUBSYMBOL);
                            modelDefSymbol.setName(symbol.getName() + "." + modelDefParameter.getName());
                            NodePrototype modelDefPortPrototype = getSynchronizationPort(modelDefSymbol);
                            LinkedPair pair = new LinkedPair(modelDefPortPrototype, portPrototype);
                            synchronizedPairs.add(pair);
                        }
                        catch( Exception e )
                        {
                            throw new Exception(nodePrototype.modelDefinitionName + " doesn't have parameter number " + ( i + 1 ));
                        }
                    }
            }
            else if( symbol.jjtGetChild(j) instanceof AstConversionFactor )
            {
                AstConversionFactor astFactor = (AstConversionFactor)symbol.jjtGetChild(j);
                String factor = astFactor.getFactor();
                DynamicProperty dp;
                String propertyName = null;
                if( AstConversionFactor.EXTENT_CONVERSION_FACTOR.equals(factor) )
                    propertyName = Util.EXTENT_FACTOR;
                else if( AstConversionFactor.TIME_CONVERSION_FACTOR.equals(factor) )
                    propertyName = Util.TIME_SCALE;

                dp = new DynamicProperty(propertyName, String.class, astFactor.getValue());
                nodePrototype.subdiagramProperties.add(dp);
            }

        nodePrototype.astNodes.add(symbol);
        nodePrototypes.put(symbol.getName(), nodePrototype);
    }
    private Map<String, PrototypeModelDefinition> importedDiagrams = new HashMap<>();

    private void addModel(AstModel model) throws Exception
    {
        try
        {
            Diagram prototypeDiagram = newDiagram.getType().clone().createDiagram(null, model.getNameSymbol().getName(),
                    new DiagramInfo(model.getNameSymbol().getName()));

            Diagram diagramModel = new AntimonyDiagramGenerator(importedDiagrams).generateDiagram(prototypeDiagram, model, null, null,
                    null);

            diagramModel = (Diagram)validateDe(diagramModel);
            //add parameters
            ArrayList<AstSymbol> parameters = model.getParameters();

            PrototypeModelDefinition prototypeModelDefinition = new PrototypeModelDefinition(diagramModel, parameters);
            importedDiagrams.put(diagramModel.getName(), prototypeModelDefinition);

            //add model definition
            ModelDefinition modelDefinition = new ModelDefinition(newDiagram, diagramModel, diagramModel.getName());
            modelDefinition = (ModelDefinition)validateDe(modelDefinition);

            newDiagram.put(modelDefinition);
            linkWithAst(model, modelDefinition.getAttributes());
        }
        catch( Exception ex )
        {
            throw new Exception("Can't add module " + model.getNameSymbol().getName() + ", " + ex.getMessage());
        }
    }

    private void addModel(AstImport astImport)
    {
        try
        {
            DataElementPath path;
            String diagramName;
            if( astImport.getPath().contains(DataElementPath.PATH_SEPARATOR) )
            {
                path = DataElementPath.create(astImport.getPath()).getParentPath();
                String[] pathComponents = DataElementPath.split(astImport.getPath(), DataElementPath.PATH_SEPARATOR_CHAR);
                diagramName = pathComponents[pathComponents.length - 1];
            }
            else
            {
                path = newDiagram.getOrigin().getCompletePath();
                diagramName = astImport.getPath();
            }

            DataCollection<?> elementDC = CollectionFactory.getDataCollection(path.toString());
            DataElement diagramModel = elementDC.get(diagramName);
            PrototypeModelDefinition prototypeModelDefinition = new PrototypeModelDefinition((Diagram)diagramModel, null);
            importedDiagrams.put(diagramModel.getName(), prototypeModelDefinition);
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Can add model from " + astImport.getPath() + ": " + ex.getMessage());
        }
    }

    private void addSimpleModels(List<AstModel> simpleModels) throws Exception
    {
        if( simpleModels == null )
            return;
        setCompositeType();
        for( AstModel model : simpleModels )
            addModel(model);
    }

    private void addDisplayName(AstSymbol symbol, NodePrototype nodePrototype)
    {
        if( symbol.getAllChildren() != null )
            for( biouml.plugins.antimony.astparser_v2.Node childSymbol : symbol.getAllChildren() )
                if( childSymbol instanceof AstIs )
                    nodePrototype.title = ( (AstIs)childSymbol ).getDisplayName();
    }

    private void addAllSymbols(biouml.plugins.antimony.astparser_v2.Node node, String type, Boolean isConst, boolean isSubstanceOnly)
    {
        for( int i = 0; i < node.jjtGetNumChildren(); i++ )
            if( AntimonyUtility.isAstSymbol(node.jjtGetChild(i)) )
            {
                AstSymbol symbol = (AstSymbol)node.jjtGetChild(i);
                NodePrototype nodePrototype;
                if( isConst != null )
                    nodePrototype = getNodePrototype(symbol, type, isConst);
                else if( symbol.isConstantPrefix() )
                    nodePrototype = getNodePrototype(symbol, type, true);
                else
                    nodePrototype = getNodePrototype(symbol, type, null);
                addParent(symbol, nodePrototype);

                if( isSubstanceOnly )
                    nodePrototype.quantityType = VariableRole.AMOUNT_TYPE;
            }
    }

    protected void addParent(AstSymbol symbol, NodePrototype nodePrototype)
    {
        if( symbol.getAllChildren() != null )
            for( biouml.plugins.antimony.astparser_v2.Node childSymbol : symbol.getAllChildren() )
                if( childSymbol instanceof AstIn )
                {
                    AstSymbol parentSymbol = ( (AstIn)childSymbol ).getOrigin();
                    NodePrototype parentNodePrototype = getNodePrototype(parentSymbol, AstSymbolType.COMPARTMENT, null);
                    nodePrototype.parent = parentNodePrototype;
                    break;
                }
    }

    protected NodePrototype getNodePrototype(AstSymbol symbol, String type, Boolean isConst)
    {
        String name = symbol.getName();

        NodePrototype nodePrototype = getNodePrototype(name, type);

        if( isConst != null && !nodePrototype.isConst )
            nodePrototype.isConst = isConst;

        nodePrototype.astNodes.add(symbol);
        return nodePrototype;
    }

    private NodePrototype getNodePrototype(String name, String type)
    {
        NodePrototype nodePrototype = null;
        if( name != null )
        {
            if( type != null && ( type.equals(ConnectionPort.PRIVATE) || type.equals(ConnectionPort.PUBLIC)
                    || type.equals(ConnectionPort.PROPAGATED) ) )
            {
                String portName = name + ":" + type;
                if( getNodePrototype(portName) != null )
                    return getNodePrototype(portName);

                nodePrototype = new NodePrototype(name);
                nodePrototypes.put(portName, nodePrototype);
            }
            else if( type != null && type.equals("bus") )
            {
                nodePrototype = getNodePrototype(name);
                int i = 1;
                while( nodePrototype != null )
                {
                    name = name + "_" + i;
                    i++;
                    nodePrototype = getNodePrototype(name);
                }
                nodePrototype = new NodePrototype(name);
            }
            else
                nodePrototype = getNodePrototype(name);
        }
        else
        {
            nodePrototype = new NodePrototype(NO_NAME + ( antimonyNoName ));
            nodePrototypes.put(NO_NAME + ( antimonyNoName ), nodePrototype);
            antimonyNoName++;
        }

        if( nodePrototype == null )
        {
            nodePrototype = new NodePrototype(name);
            nodePrototypes.put(name, nodePrototype);
        }
        else
            checkType(nodePrototype, type);

        if( type != null && ( nodePrototype.type == null || nodePrototype.type.equals(AstSymbolType.SPECIES) ) )
            nodePrototype.type = type;

        return nodePrototype;
    }

    private NodePrototype locateNodePrototype(SimpleNode symbol)
    {
        if( symbol.getChildren() == null )
            return null;

        String[] subNames = null;
        for( biouml.plugins.antimony.astparser_v2.Node child : symbol.getChildren() )
        {
            if( child instanceof AstLocateFunction )
            {
                try
                {
                    AstLocateFunction node = (AstLocateFunction)child;
                    if( node.getProperty("type").contains(PORT) )
                    {
                        String type = getValidPortType(node.getProperty("type"));
                        subNames = node.getProperty("name").split("\\.");
                        if( subNames.length >= 2 )
                        {
                            // nodePrototype is inside subdiagram
                            NodePrototype subportPrototype = new NodePrototype(subNames[subNames.length - 1]);
                            subportPrototype.type = type;
                            subportPrototype.astNodes.add(symbol);
                            nodePrototypes.get(subNames[0]).elementTitles.add(subportPrototype);
                            return subportPrototype;
                        }
                        else
                        {
                            getNodePrototype(node.getProperty("name"), type);
                            return getNodePrototype(node.getProperty("name") + ":" + type);
                        }
                    }
                }
                catch( Exception e )
                {
                    log.log(Level.INFO, e.getMessage());
                }
            }
        }


        return null;
    }

    private String getValidPortType(String type) throws Exception
    {
        String[] values = type.split("\\s+", 0);
        String normalizedType = ConnectionPort.PUBLIC;
        if( values.length == 2 && values[1].equals(PORT) )
        {
            if( values[0].equals(ConnectionPort.PUBLIC) )
                return normalizedType;

            else if( values[0].equals(ConnectionPort.PRIVATE) || values[0].equals(ConnectionPort.PROPAGATED) )
                return values[0];

        }
        else if( values.length == 1 && values[0].equals(PORT) )
            return normalizedType;

        throw new Exception("Port type " + type + " does not exist");
    }

    private void checkType(NodePrototype prototype, String type)
    {
        if( prototype.type == null || type == null )
            return;
        if( prototype.type.equals(AstSymbolType.SPECIES) && type.equals(AstSymbolType.GENE) )
            return;
        if( prototype.type.equals(AstSymbolType.GENE) && type.equals(AstSymbolType.SPECIES) )
            return;
        else if( !prototype.type.equals(type) )
        {
            log.log(Level.SEVERE, prototype.name + "have different types");
            throw new IllegalArgumentException(prototype.name + " have different types");
        }
    }

    private void addEquation(AstSymbol symbol)
    {
        StreamEx.of(symbol.getAllChildren()).select(AstEquation.class).forEach(childSymbol -> {
            // if this is initial value
            if( AstSymbol.INIT.equals(symbol.getDeclarationType()) )
            {
                NodePrototype nodePrototype = getNodePrototype(symbol, null, symbol.isConstantPrefix() ? true : null);
                if( childSymbol.isNumber() || childSymbol.jjtGetNumChildren() == 0 )
                {
                    String number = childSymbol.getFormula();
                    nodePrototype.initialValue = number.isEmpty() ? "0" : number;
                }
                else if( childSymbol.isNumberWithUnit() )
                {
                    String unitName = childSymbol.jjtGetChild(1).toString();

                    nodePrototype.initialValue = childSymbol.jjtGetChild(0).toString();
                    nodePrototype.unit = unitName;

                    Unit unit = new Unit(null, unitName);
                    EModel emodel = newDiagram.getRole(EModel.class);

                    emodel.addUnit(unit);
                    linkWithAst(symbol, unit.getAttributes());
                }

                if( childSymbol.hasAmountQuantityType() )
                {
                    String unitName = childSymbol.jjtGetChild(childSymbol.jjtGetNumChildren() - 1).toString();
                    try
                    {
                        if( nodePrototype.parent != null )
                        {
                            if( nodePrototype.parent.name.equals(unitName) )
                                nodePrototype.initialQuantityType = VariableRole.AMOUNT_TYPE;
                            else
                                throw new Exception("Compartment name does not match parent's name!");
                        }
                    }
                    catch( Exception e )
                    {
                        log.log(Level.SEVERE, e.getMessage());
                    }
                }
            }
            // if this is initial assignment
            else
            {
                NodePrototype nodePrototype = new NodePrototype(symbol.getName() + "_" + symbol.getDeclarationType());
                nodePrototype.variable = symbol.getName();
                nodePrototype.equation = childSymbol;
                if( AstSymbol.INIT.equals(symbol.getDeclarationType()) )
                    nodePrototype.type = Equation.TYPE_INITIAL_ASSIGNMENT;
                else if( AstSymbol.RULE.equals(symbol.getDeclarationType()) )
                    nodePrototype.type = Equation.TYPE_SCALAR;
                else if( AstSymbol.RATE.equals(symbol.getDeclarationType()) )
                    nodePrototype.type = Equation.TYPE_RATE;
                else if( AstSymbol.ALGEBRAIC.equals(symbol.getDeclarationType()) )
                    nodePrototype.type = Equation.TYPE_ALGEBRAIC;
                nodePrototype.astNodes.add(symbol);
                nodePrototypes.put(nodePrototype.name, nodePrototype);
            }
        });
    }

    private void addEvent(AstSymbol symbol) throws ParseException
    {
        NodePrototype nodePrototype;
        if( symbol.getName() != null )
            nodePrototype = new NodePrototype(symbol.getName());
        else
            nodePrototype = new NodePrototype(NO_NAME + ( antimonyNoName++ ));
        nodePrototype.assignmentsVariable = new ArrayList<>();
        nodePrototype.assignmentsEquation = new ArrayList<>();
        nodePrototype.type = AstSymbol.EVENT;
        for( int i = 0; i < symbol.jjtGetNumChildren(); i++ )
        {
            if( symbol.jjtGetChild(i) instanceof AstAt )
            {
                nodePrototype.trigger = ( (AstAt)symbol.jjtGetChild(i) ).getTrigger();
                nodePrototype.delay = ( (AstAt)symbol.jjtGetChild(i) ).getDelay();
            }
            if( symbol.jjtGetChild(i) instanceof AstPriority )
            {
                nodePrototype.priority = ( (AstPriority)symbol.jjtGetChild(i) ).getEquation();
            }
            if( symbol.jjtGetChild(i) instanceof AstTriggerInitialValue )
            {
                nodePrototype.triggerInitialValues = ( (AstTriggerInitialValue)symbol.jjtGetChild(i) ).getValue();
            }
            if( symbol.jjtGetChild(i) instanceof AstPersistent )
            {
                nodePrototype.persistent = ( (AstPersistent)symbol.jjtGetChild(i) ).getValue();
            }
            if( symbol.jjtGetChild(i) instanceof AstUseValuesFromTriggerTime )
            {
                nodePrototype.useValuesFromTriggerTime = ( (AstUseValuesFromTriggerTime)symbol.jjtGetChild(i) ).getValue();
            }
            else if( AntimonyUtility.isAstSymbol(symbol.jjtGetChild(i)) )
            {
                AstSymbol assignament = (AstSymbol)symbol.jjtGetChild(i);
                nodePrototype.assignmentsVariable.add(assignament.getName());
                nodePrototype.assignmentsEquation.add((AstEquation)assignament.jjtGetChild(1));
            }
        }
        nodePrototype.astNodes.add(symbol);
        nodePrototypes.put(nodePrototype.name, nodePrototype);
    }

    private void addReactionProperties(AstSymbol symbol)
    {
        NodePrototype nodePrototype = getNodePrototype(symbol, REACTION, null);
        nodePrototype.reactants = new HashMap<>();
        nodePrototype.products = new HashMap<>();
        nodePrototype.modifier = new HashMap<>();
        for( int i = 0; i < symbol.jjtGetNumChildren(); i++ )
        {
            biouml.plugins.antimony.astparser_v2.Node child = symbol.jjtGetChild(i);
            if( child instanceof AstReactionTitle )
            {
                for( int j = 0; j < child.jjtGetNumChildren(); j++ )
                {
                    if( child.jjtGetChild(j) instanceof AstReactant )
                    {
                        AstReactant reactant = ( (AstReactant)child.jjtGetChild(j) );
                        NodePrototype reactantPrototype = getNodePrototype(reactant.getSymbol(), AstSymbolType.SPECIES, null);
                        nodePrototype.reactants.put(reactantPrototype.name, reactant.getStoichiometry());
                    }
                    else if( child.jjtGetChild(j) instanceof AstReactionType )
                    {
                        nodePrototype.isReversible = ( (AstReactionType)child.jjtGetChild(j) ).isReversible();
                    }
                    else if( child.jjtGetChild(j) instanceof AstProduct )
                    {
                        AstProduct product = ( (AstProduct)child.jjtGetChild(j) );
                        NodePrototype productPrototype = getNodePrototype(product.getSymbol(), AstSymbolType.SPECIES, null);
                        nodePrototype.products.put(productPrototype.name, product.getStoichiometry());
                    }

                }
            }
            else if( child instanceof AstEquation && ! ( (AstEquation)child ).getFormula().isEmpty() )
            {
                nodePrototype.reactionFormula = (AstEquation)child;
            }

        }
    }

    private void addModifier(AstSymbol symbol)
    {
        Map<String, String> modifiers = new HashMap<>();
        List<NodePrototype> reactions = new ArrayList<>();
        NodePrototype logicalPrototype = null;
        boolean isLogical = false;

        EdgePrototype edgePrototype = new EdgePrototype(symbol.getName());
        for( int i = 0; i < symbol.jjtGetNumChildren(); i++ )
        {
            biouml.plugins.antimony.astparser_v2.Node child = symbol.jjtGetChild(i);
            if( child instanceof AstReactionTitle )
            {
                AstReactionTitle rt = (AstReactionTitle)child;
                if( !rt.getType().equals(AstReactionTitle.STANDARD_REACTION) )
                {
                    logicalPrototype = new NodePrototype(symbol.getName() + "_" + rt.getType());
                    logicalPrototype.type = rt.getType();
                    isLogical = true;
                    logicalPrototype.astNodes.add(symbol);
                    nodePrototypes.put(logicalPrototype.name, logicalPrototype);
                }

                for( int j = 0; j < child.jjtGetNumChildren(); j++ )
                {
                    if( child.jjtGetChild(j) instanceof AstReactant )
                    {
                        AstReactant modifier = ( (AstReactant)child.jjtGetChild(j) );
                        NodePrototype modifierPrototype = getNodePrototype(modifier.getSymbol(), AstSymbolType.SPECIES, null);
                        modifiers.put(modifierPrototype.name, modifier.getStoichiometry());

                        if( isLogical )
                            edgePrototype.inputName = logicalPrototype.name;
                        else
                            edgePrototype.inputName = modifierPrototype.name;
                        edgePrototype.astNodes.add(symbol);
                        edgePrototypes.put(edgePrototype.name, edgePrototype);
                    }
                    else if( child.jjtGetChild(j) instanceof AstProduct )
                    {
                        AstProduct reaction = ( (AstProduct)child.jjtGetChild(j) );
                        NodePrototype reactionPrototype = getNodePrototype(reaction.getSymbol(), REACTION, null);
                        reactions.add(reactionPrototype);
                        reactionPrototype.astNodes.add(symbol);
                        if( isLogical )
                        {
                            logicalPrototype.applicationName = reactionPrototype.name;
                            logicalPrototype.nodes = new ArrayList<>();
                            for( Map.Entry<String, String> entry : modifiers.entrySet() )
                                logicalPrototype.nodes.add(entry.getKey());
                        }
                        else
                            for( Map.Entry<String, String> entry : modifiers.entrySet() )
                                reactionPrototype.modifier.put(entry.getKey(),
                                        new String[] {entry.getValue(), symbol.getDeclarationType()});

                        edgePrototype.outputName = reactionPrototype.name;
                    }
                }
            }
            else if( child instanceof AstEquation && ! ( (AstEquation)child ).getFormula().isEmpty() )
            {
                for( NodePrototype nodePrototype : reactions )
                    nodePrototype.reactionFormula = (AstEquation)child;
            }
        }
    }

    private boolean isEquation(NodePrototype prototype)
    {
        return ( prototype.type.equals(Equation.TYPE_INITIAL_ASSIGNMENT) || prototype.type.equals(Equation.TYPE_RATE)
                || prototype.type.equals(Equation.TYPE_SCALAR) || prototype.type.equals(Equation.TYPE_ALGEBRAIC) );
    }

    private boolean isLogical(NodePrototype prototype)
    {
        return ( prototype.type.equals(AstReactionTitle.MOD_AND_REACTION) || prototype.type.equals(AstReactionTitle.MOD_NOT_REACTION)
                || prototype.type.equals(AstReactionTitle.MOD_OR_REACTION) );
    }

    private NodePrototype getNodePrototype(String name)
    {
        return nodePrototypes.get(name);
    }

    private EdgePrototype getEdgePrototype(String name)
    {
        return edgePrototypes.get(name);
    }

    protected void addFunction(AstFunction astFunction) throws Exception
    {
        String name = astFunction.getFunctionName();
        Node node = (Node)controller.createInstance(newDiagram, Function.class, name, null, new Function(null)).getElement();
        if( node == null )
            return;
        node = (Node)validateDe(node);
        String args = StreamEx.of(astFunction.getParameters()).map(s -> s.getName()).joining(",");
        StringBuilder functionFormula = new StringBuilder("function ").append(name).append("(").append(args).append(") = ")
                .append(astFunction.getEquation().getFormula());
        EModel emodel = newDiagram.getRole(EModel.class);
        ru.biosoft.math.model.AstStart astStart = emodel.readMath(functionFormula.toString(), node.getRole(), EModel.VARIABLE_NAME_BY_ID);
        if( astStart == null )
            throw new Exception("Incorrect formula");
        node.getRole(Function.class).setFormula(new LinearFormatter().format(astStart)[1]);
        newDiagram.put(node);
        linkWithAst(astFunction, node.getAttributes());
    }

    protected void linkWithAst(Set<biouml.plugins.antimony.astparser_v2.Node> set, DynamicPropertySet dps)
    {
        DynamicProperty dp = dps.getProperty(AntimonyConstants.ANTIMONY_LINK);
        if( dp != null )
            set.addAll((Set)dp.getValue());
        dps.add(DPSUtils.createHiddenReadOnlyTransient(AntimonyConstants.ANTIMONY_LINK, Set.class, set));
    }

    protected void linkWithAst(biouml.plugins.antimony.astparser_v2.Node astNode, DynamicPropertySet dps)
    {
        Set<biouml.plugins.antimony.astparser_v2.Node> set = new HashSet<>();
        set.add(astNode);
        linkWithAst(set, dps);
    }

    protected void linkWithAst(NodePrototype prototype, DynamicPropertySet dps)
    {
        linkWithAst(prototype.astNodes, dps);
    }

    private DiagramElement validateDe(DiagramElement de) throws Exception
    {
        return validateDe(de, "", "");
    }
    private DiagramElement validateDe(DiagramElement de, @Nonnull
    String sbgnViewTitle, @Nonnull
    String sbgnType) throws Exception
    {
        if( newDiagram.getType() instanceof SbgnDiagramType )
        {
            if( !sbgnViewTitle.isEmpty() )
            {
                de.getAttributes().setValue(Util.COMPLEX_STRUCTURE, sbgnViewTitle);
                SbgnComplexStructureManager.annotateSpecies((Compartment)de);
                SbgnComplexStructureManager.arrangeComplexView((Compartment)de, null);
                de.getAttributes().add(DPSUtils.createHiddenReadOnlyTransient("AutogeneratedView", Boolean.class, true));
            }
            if( de instanceof Node && SbgnUtil.isMacromoleculeEntity((Node)de) && !Type.TYPE_COMPLEX.equals(sbgnType) )
            {
                ( (Specie)de.getKernel() ).setType(de.getKernel().getType().equals(AntimonyConstants.STANDARD_GENE)
                        ? AntimonyConstants.SBGN_GENE : sbgnType.isEmpty() ? Type.TYPE_MACROMOLECULE : sbgnType);
            }


        }
        return de;
    }


    private static class Prototype
    {
        //common property
        protected final String name;
        protected String type;
        protected NodePrototype parent;
        protected final Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = new HashSet<>();
        protected boolean existInDiagram;
        protected String title;
        protected final Map<String, Object> dynamicProperty = new HashMap<>();

        protected Prototype(String name)
        {
            this.name = name;
        }

    }
    private static class NodePrototype extends Prototype
    {

        private NodePrototype(String name)
        {
            super(name);
        }

        private Map<String, List<String>> databaseReferences = new HashMap<String, List<String>>();;

        // Table properties
        public Map<String, String> argColumn;
        public String path;
        public Set<Map<String, String>> columns;

        // Variable property
        private String initialValue = "0";
        private boolean isConst;
        private String unit = Unit.UNDEFINED;
        public int quantityType = VariableRole.CONCENTRATION_TYPE;
        public int initialQuantityType = VariableRole.AMOUNT_TYPE;

        // Complex components sbgnType properties: list of parent names
        private List<AstProperty> complexComponentsSbgnTypes = new ArrayList<AstProperty>();

        // reaction property
        //map(name specie, stoichiometry)
        private Map<String, String> reactants;
        private Map<String, String> products;
        //map(name specie, stoichiometry/type)
        private Map<String, String[]> modifier;
        private AstEquation reactionFormula;
        private Boolean isReversible = false;

        // equation property
        private String variable;
        private AstEquation equation;

        //note property
        private String text;
        private List<AstSymbol> relatedNodes = new ArrayList<AstSymbol>();

        //event property
        private AstEquation trigger;
        private AstEquation delay;
        private AstEquation priority;
        private boolean triggerInitialValues = false;
        private boolean persistent = false;
        private boolean useValuesFromTriggerTime = true;
        private ArrayList<String> assignmentsVariable;
        private ArrayList<AstEquation> assignmentsEquation;

        // logical and equivalence node properties 
        private String applicationName;
        private List<String> nodes;

        // constraint property
        private String message;
        private AstEquation formula;

        //subdiagram property
        private String modelDefinitionName;
        private List<NodePrototype> subdiagramParameters;
        private List<NodePrototype> elementTitles;
        private final Set<DynamicProperty> subdiagramProperties = new HashSet<>();

        // port property
        private AstSymbol portSymbol;

        // bus properties
        private List<AstSymbol> ports = new ArrayList<AstSymbol>();
        private boolean directed;
    }

    private static class EdgePrototype extends Prototype
    {

        //private String role; modifier, reactant, product
        private String inputName;
        private String outputName;

        private EdgePrototype(String name)
        {
            super(name);
        }


        // reaction property
        private String modAction;
        //private String participation;

    }

    private static class PrototypeModelDefinition
    {
        Diagram diagram;
        ArrayList<AstSymbol> parameters;

        public PrototypeModelDefinition(Diagram diagram, ArrayList<AstSymbol> parameters)
        {
            this.diagram = diagram;
            this.parameters = parameters;
        }
    }

    private class LinkedPair
    {
        private final NodePrototype firstPortPrototype;
        private final NodePrototype secondPortPrototype;

        private LinkedPair(NodePrototype firstPortPrototype, NodePrototype secondPortPrototype)
        {
            this.firstPortPrototype = firstPortPrototype;
            this.secondPortPrototype = secondPortPrototype;
        }

        private Node getFirstPort() throws Exception
        {
            return getPort(firstPortPrototype);
        }

        private Node getSecondPort() throws Exception
        {
            return getPort(secondPortPrototype);
        }
    }

    private Node getPort(NodePrototype portPrototype) throws Exception
    {
        AstSymbol symbol = portPrototype.portSymbol;
        String[] chainNames = symbol.getChainNames();
        String variableName = chainNames[chainNames.length - 1];

        Diagram diagram = newDiagram;
        // port from diagram
        if( chainNames.length == 1 )
        {
            List<Node> ports = AntimonyUtility.getPortNodes(diagram, false);
            for( Node port : ports )
            {
                String nameVariableInPort = AntimonyUtility.getAntimonyValidName(Util.getPortVariable(port));
                if( variableName.equals(nameVariableInPort) )
                    if( Util.isPrivatePort(port) )
                        return port;
            }
        }
        else if( chainNames.length == 2 )
        {
            // port from SubDiagram
            Node subDiagram = diagram.findNode(chainNames[0]);
            if( ! ( subDiagram instanceof SubDiagram ) )
                throw new Exception("Can't find subdiagram " + chainNames[0]);

            List<Node> ports = AntimonyUtility.getPortNodes((Compartment)subDiagram, false);
            for( Node port : ports )
            {
                String nameVariableInPort = AntimonyUtility.getAntimonyValidName(Util.getPortVariable(port));

                if( variableName.equals(nameVariableInPort) )
                {

                    if( portPrototype.title != null )
                        port.setTitle(portPrototype.title);

                    return port;
                }
            }
            throw new Exception("Can't find port for " + variableName + " in " + subDiagram.getName());
        }
        return null;
    }

    public Logger getLogger()
    {
        return log;
    }
}
