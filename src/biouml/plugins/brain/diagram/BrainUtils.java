package biouml.plugins.brain.diagram;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.DynamicProperty;
import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.Node;
import biouml.model.SubDiagram;
import biouml.model.SubDiagram.PortOrientation;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Function;
import biouml.model.dynamics.TableElement;
import biouml.model.dynamics.VariableRole;
import biouml.model.xml.XmlDiagramTypeConstants;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.Util;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;
import ru.biosoft.access.core.DataCollection;

public class BrainUtils
{
	/*
	 * method checks if node is connectivity matrix.
	 */
    public static boolean isConnectivityMatrix(Node node)
    {
       return BrainType.TYPE_CONNECTIVITY_MATRIX.equals(node.getKernel().getType()); 
    }
    
	/*
	 * method checks if node is delay matrix.
	 */
    public static boolean isDelayMatrix(Node node)
    {
       return BrainType.TYPE_DELAY_MATRIX.equals(node.getKernel().getType()); 
    }

    /*
     * method checks if node is regional model.
     */
    public static boolean isRegionalModel(Node node)
    {
       return BrainType.TYPE_REGIONAL_MODEL.equals(node.getKernel().getType()); 
    }
    
    /*
     * method checks if node is cellular model.
     */
    public static boolean isCellularModel(Node node)
    {
       return BrainType.TYPE_CELLULAR_MODEL.equals(node.getKernel().getType()); 
    }
    
    /*
     * method checks if node is receptor model.
     */
    public static boolean isReceptorModel(Node node)
    {
       return BrainType.TYPE_RECEPTOR_MODEL.equals(node.getKernel().getType()); 
    }
    
    /*
     * method returns list of nodes
     * in which each one has TYPE_CONNECTIVITY_MATRIX (TableElement role).
     */
    public static List<Node> getConnectivityMatrixNodes(Diagram src) 
    {
    	return src.stream(Node.class).filter(BrainUtils::isConnectivityMatrix).toList();
    }
    
    /*
     * method returns list of nodes
     * in which each one has TYPE_DELAY_MATRIX (TableElement role).
     */
    public static List<Node> getDelayMatrixNodes(Diagram src) 
    {
    	return src.stream(Node.class).filter(BrainUtils::isDelayMatrix).toList();
    }
    
    /*
     * method returns 2D array of doubles
     * created with the first connectivity matrix in the diagram
     * which contains connectivity strengths of the regional model (strengths of connections between regions).
     */
    public static double[][] getConnectivityMatrix(Diagram src)
    {
    	if (getConnectivityMatrixNodes(src).size() == 0)
    	{
    		return null;
    	}
    	
    	TableElement te = (TableElement)getConnectivityMatrixNodes(src).get(0).getRole();
    	
    	// Table size without names
	    int sizeRows = te.getTable().getSize();
	    int sizeColumns = te.getVariables().length;
	    double[][] connectivityMatrix = new double[sizeRows][sizeColumns];
	    
	    for (int i = 0; i < sizeRows; i++)
	    {
	    	Object[] rowValues = te.getTable().getAt(i).getValues();
	    	for (int j = 0; j < sizeColumns; j++) 
	      	{
	      		double cellValue = ((Number)rowValues[j]).doubleValue();
	      		connectivityMatrix[i][j] = cellValue;
	      	}
	    }
	      
	    return connectivityMatrix;
    }
    
    /*
     * method returns 2D array of doubles
     * created with the first delay matrix in the diagram
     * which contains time delays of the regional model (neural tracks lengths / transmission speed).
     */
    public static double[][] getDelayMatrix(Diagram src)
    {
    	if (getDelayMatrixNodes(src).size() == 0)
    	{
    		return null;
    	}
    	
    	TableElement te = (TableElement)getDelayMatrixNodes(src).get(0).getRole();
    	
    	// Table size without names
	    int sizeRows = te.getTable().getSize();
	    int sizeColumns = te.getVariables().length;
	    double[][] delayMatrix = new double[sizeRows][sizeColumns];
	    
	    for (int i = 0; i < sizeRows; i++)
	    {
	    	Object[] rowValues = te.getTable().getAt(i).getValues();
	    	for (int j = 0; j < sizeColumns; j++) 
	      	{
	      		double cellValue = ((Number)rowValues[j]).doubleValue();
	      		delayMatrix[i][j] = cellValue;
	      	}
	    }
	      
	    return delayMatrix;
    }
    
	/*
	 * method calculates matrix epsilon0 for Rossler model (neurons from different regions are trying to synchronize each other)
	 * 
	 * coupling term epsilon_ji = epsilonmin_ji + epsilon0_ji * eta
	 * 
	 * where:
	 * epsilonmin_ji = 0.0
	 * epsilon0_ji = 0.21, if neurons j and i from different regions
	 *               0.0, if neurons from same region
	 */
    public static double[][] getCouplingMatrix(double[][] connectivityMatrix) 
    {
    	double epsilon0 = 0.21;
    	
    	int sizeRows = connectivityMatrix.length;
    	int sizeColumns = connectivityMatrix[0].length;
    	
    	double[][] couplingMatrix = new double[sizeRows][sizeColumns];
    	
    	for (int i = 0; i < sizeColumns; i++) 
    	{
    		for (int j = 0; j < sizeRows; j++)
    		{
    			if (connectivityMatrix[j][i] != 0.0) 
    			{
    			    couplingMatrix[j][i] = 0.0;
    			}
    			else
    			{
    				couplingMatrix[j][i] = epsilon0;
    			}
    		}
    	}
    	
    	return couplingMatrix;
    }
    
    /*
     * method distributes the regions in the Rossler model into clusters, 
     * taking into account the connectivity matrix.
     */
    public static ArrayList<ArrayList<Integer>> getClusters(double[][] connectivityMatrix)
    {
    	int sizeRows = connectivityMatrix.length;
    	int sizeColumns = connectivityMatrix[0].length;
    	
    	boolean[] checked = new boolean[sizeRows];
    	Arrays.fill(checked, false);
    	
    	ArrayList<ArrayList<Integer>> clusters = new ArrayList<ArrayList<Integer>>();
    	
       	for (int i = 0; i < sizeRows; i++) 
    	{
       		if (checked[i]) 
       		{
       			continue;
       		}
       		
       		ArrayList<Integer> cluster = new ArrayList<Integer>();
       		cluster.add(i);
       		checked[i] = true;
       		
    		for (int j = 0; j < sizeColumns; j++)
    		{
    			if (connectivityMatrix[i][j] != 0.0 && j != i) 
    			{
    				cluster.add(j);
    				checked[j] = true;
    			}
    		}
    		
    		clusters.add(cluster);
    	}
       	
       	return clusters;
    }
    
    /*
     * method returns list of nodes
     * in which each one has TYPE_REGIONAL_MODEL (BrainRegionalModel role).
     */
    public static List<Node> getRegionalModelNodes(Diagram src)
    {
    	return src.stream(Node.class).filter(BrainUtils::isRegionalModel).toList();
    }
    
    /*
     * method returns list of nodes
     * in which each one has TYPE_CELLULAR_MODEL (BrainCellularModel role).
     */
    public static List<Node> getCellularModelNodes(Diagram src)
    {
    	return src.stream(Node.class).filter(BrainUtils::isCellularModel).toList();
    }
    
    /*
     * method returns list of nodes
     * in which each one has TYPE_RECEPTOR_MODEL (BrainCellularModel role).
     */
    public static List<Node> getReceptorModelNodes(Diagram src)
    {
    	return src.stream(Node.class).filter(BrainUtils::isReceptorModel).toList();
    }
    
    /*
     * method adds a mathematical equation to the diagram.
     */
    public static void createEquation(String name, String variable, String formula, String type,
    		@Nonnull Diagram diagram, Point point) throws Exception
    {
        String validName = DefaultSemanticController.generateUniqueNodeName(diagram, name);
        Node eqNode = new Node(diagram, new Stub(null, validName, Type.MATH_EQUATION));
        eqNode.setRole(new Equation(eqNode, type, variable, formula));
        eqNode.setShowTitle(false);
        eqNode.setLocation(point);
        diagram.put(eqNode);
    }
    
    /*
     * method adds an event to the diagram.
     */
    public static void createEvent(String name, String trigger, Assignment[] assignment, 
    		@Nonnull Diagram diagram, Point point) throws Exception
    {
        String validName = DefaultSemanticController.generateUniqueNodeName(diagram, name);
        Node evNode = new Node(diagram, new Stub(null, validName, Type.MATH_EVENT));
        evNode.setLocation(point);
        diagram.setNotificationEnabled(false);
        Event ev = new Event(evNode);
        ev.setTriggerInitialValue(true);
        ev.setTrigger(trigger);
        ev.setEventAssignment(assignment);
        diagram.setNotificationEnabled(true);
        evNode.setRole(ev);
        diagram.put(evNode);
    }
    
    /*
     * method adds a mathematical equation to the diagram.
     */
    public static void createFunction(String name, String formula,
    		@Nonnull Diagram diagram, Point point) throws Exception
    {    	
        String validName = DefaultSemanticController.generateUniqueNodeName(diagram, name);
        Node funcNode = new Node(diagram, new Stub(null, validName, Type.MATH_FUNCTION));
        funcNode.setLocation(point);
        funcNode.setShowTitle(false);
        diagram.setNotificationEnabled(false);
        Function func = new Function(funcNode);
        func.setName(name);
        func.setFormula(formula);
        diagram.setNotificationEnabled(true);
        funcNode.setRole(func);
        diagram.put(funcNode);
    }
    
    /*
     * method adds a note to the diagram.
     */
    public static void createNote(String title, Dimension size,
    		@Nonnull Diagram diagram, Point point) throws Exception
    {    	
        String validName = DefaultSemanticController.generateUniqueNodeName(diagram, "note");
        Node noteNode = new Node(diagram, new Stub.Note(null, validName));
        noteNode.setTitle(title);
        noteNode.setShowTitle(true);
        noteNode.setShapeSize(size);
        noteNode.setLocation(point);
        diagram.put(noteNode);
    }
    
    /*
     * method adds a public port to the diagram.
     */
    public static Node createPort(String variableName, String title, String type, 
    		@Nonnull Diagram diagram, Point point) throws Exception
    {
    	EModel emodel = diagram.getRole(EModel.class);
    	if (!emodel.containsVariable(variableName))
    	{
    		emodel.declareVariable(variableName, 0.0);
    	}
    	
        String validName = DefaultSemanticController.generateUniqueNodeName(diagram, "port");
        Node portNode = new Node(diagram, Stub.ConnectionPort.createPortByType(diagram, validName, type));
        portNode.setLocation(point);
        portNode.setTitle(title);
        portNode.getAttributes().add(new DynamicProperty(Stub.ConnectionPort.ACCESS_TYPE, String.class, Stub.ConnectionPort.PUBLIC));
        portNode.getAttributes().add(new DynamicProperty(PortOrientation.ORIENTATION_ATTR, PortOrientation.class, PortOrientation.RIGHT));
        portNode.getAttributes().add(new DynamicProperty(Stub.ConnectionPort.PORT_TYPE, String.class, type));
        portNode.getAttributes().add(new DynamicProperty(XmlDiagramTypeConstants.XML_TYPE, String.class, type));
        portNode.getAttributes().add(new DynamicProperty(Stub.ConnectionPort.VARIABLE_NAME_ATTR, String.class, variableName));
        diagram.put(portNode);
        return portNode;
    }
    
    /*
     * method adds a private port to the diagram.
     */
    public static Node createPrivatePort(String variableName, String title, String type, 
    		@Nonnull Diagram diagram, Point point) throws Exception
    {
    	EModel emodel = diagram.getRole(EModel.class);
    	if (!emodel.containsVariable(variableName))
    	{
    		emodel.declareVariable(variableName, 0.0);
    	}
    	
        String validName = DefaultSemanticController.generateUniqueNodeName(diagram, "port");
        Node portNode = new Node(diagram, Stub.ConnectionPort.createPortByType(diagram, validName, type));
        portNode.setLocation(point);
        portNode.setTitle(title);
        portNode.getAttributes().add(new DynamicProperty(Stub.ConnectionPort.ACCESS_TYPE, String.class, Stub.ConnectionPort.PRIVATE));
        portNode.getAttributes().add(new DynamicProperty(PortOrientation.ORIENTATION_ATTR, PortOrientation.class, PortOrientation.RIGHT));
        portNode.getAttributes().add(new DynamicProperty(Stub.ConnectionPort.PORT_TYPE, String.class, type));
        portNode.getAttributes().add(new DynamicProperty(XmlDiagramTypeConstants.XML_TYPE, String.class, type));
        portNode.getAttributes().add(new DynamicProperty(Stub.ConnectionPort.VARIABLE_NAME_ATTR, String.class, variableName));
        diagram.put(portNode);
        return portNode;
    }
    
    /*
     * method adds a compartment to the diagram and returns it.
     */
    public static Compartment createCompartment(String name, String title, double initialValue, String comment, Dimension size, 
    		@Nonnull Diagram diagram, Point point) throws Exception
    {
    	Base kernel = new biouml.standard.type.Compartment(null, name);
    	DiagramElement de = new Compartment(diagram, kernel);
    	VariableRole role = new VariableRole(de, initialValue);
    	role.setComment(comment);
    	de.setRole(role);
    	de.setTitle(title);
    	Node node = (Node)de;
    	node.setShapeSize(size);
    	node.setLocation(point);
    	diagram.put(de);
    	return (Compartment)node;
    }
    
    /*
     * method adds a complex to the parent and returns it.
     */
    public static Compartment createComplex(String name, String title, double initialValue, int quantityType, String comment, Dimension size, 
    		DataCollection<?> parent, Point point) throws Exception
    {
    	Compartment complex = new Compartment(parent, name, new Specie(parent, title, biouml.plugins.sbgn.Type.TYPE_COMPLEX));
    	VariableRole role = new VariableRole(complex, initialValue);
    	role.setInitialQuantityType(quantityType);
    	role.setQuantityType(quantityType);
    	role.setOutputQuantityType(quantityType);
    	role.setComment(comment);
    	complex.setRole(role);
    	complex.setLocation(point);
    	complex.setShapeSize(size);
    	((Compartment)parent).put(complex);
        return complex;
    }
    
    /*
     * method adds a macromolecule to the parent and returns it.
     */
    public static Compartment createEntity(String name, String title, Dimension size, 
    		DataCollection<?> parent, Point point) throws Exception
    {
        Compartment entity = new Compartment(parent, name, new Specie(parent, title, biouml.plugins.sbgn.Type.TYPE_MACROMOLECULE));
        //entity.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_MULTIMER_PD, Integer.class, 5));
        entity.setLocation(point);
        entity.setShapeSize(size);
        ((Compartment)parent).put(entity);
        return entity;
    }
    
    /*
     * method adds a reaction to the diagram according to the specieReference.
     */
    public static void createReaction(SpecieReference[] specieReference, String formula,
    		@Nonnull Diagram diagram, Point point) throws Exception
    {
        String reactionName = DefaultSemanticController.generateUniqueNodeName(diagram, "Reaction");
        String reactionTitle = "";
        String products = "";
        for (SpecieReference spRef : specieReference)
        {
        	if (spRef.getRole().equals(SpecieReference.PRODUCT))
        	{
        		products += products.isEmpty() ? spRef.getSpecieName() : "+" + spRef.getSpecieName();
        	}
        	else
        	{
        		reactionTitle += reactionTitle.isEmpty() ? spRef.getSpecieName() : "+" + spRef.getSpecieName();
        	}
        }
        reactionTitle += "->" + products;
        Reaction prototype = new Reaction(null, reactionName);
        prototype.setTitle(reactionTitle);
        prototype.setSpecieReferences(specieReference);
        prototype.setFormula(formula);
        DiagramElementGroup reactionElements = diagram.getType().getSemanticController().createInstance(diagram, Reaction.class, point, prototype);
        reactionElements.putToCompartment();
    }
    
    /*
     * method creates entities with names from entityNames 
     * and puts them into the complex, grouping by 3.
     */
    public static void addEntitiesToComplex(List<String> entityNames, Compartment complex, Diagram diagram, Dimension size) throws Exception
    {
    	if (entityNames.size() == 0)
    		return;
    	
    	Compartment entity1 = null;
    	Compartment entity2 = null;
    	Compartment entity3 = null;
    	
    	Point point = complex.getLocation();
    	int count = 0;
    	for (String entityName : entityNames)
    	{
    		switch (count % 3) 
    		{
    		    case 0:
    		    	point.translate(5, 10);
        			entity1 = BrainUtils.createEntity(DefaultSemanticController.generateUniqueNodeName(diagram, entityName), entityName, size, complex, point);
        			count += 1;
        			break;
    		    case 1:
    		    	point.translate(entity1.getShapeSize().width, 0);
    		    	entity2 = BrainUtils.createEntity(DefaultSemanticController.generateUniqueNodeName(diagram, entityName), entityName, size, complex, point);
    		    	count += 1;
    		    	break;
    		    case 2:
    		    	point.translate(-entity2.getShapeSize().width / 2, entity2.getShapeSize().height);
    		    	entity3 = BrainUtils.createEntity(DefaultSemanticController.generateUniqueNodeName(diagram, entityName), entityName, size, complex, point);
    		    	point = entity1.getLocation();
    		    	point.translate(0, entity1.getShapeSize().height + entity3.getShapeSize().height);
    		    	count += 1;
    		    	break;
    		}
    	}
    }
    
    /*
     * method finds any port in subDiagram by variableName and returns it.
     */
    public static Node findPort(SubDiagram subDiagram, String variableName) throws Exception
    {
    	return subDiagram.findPort(variableName).orElseThrow(() -> new Exception("Could not find port for '" + variableName + "' in " + subDiagram.getName()));
    }
    
    /*
     * method finds input port in subDiagram by variableName and returns it.
     */
    public static Node findInputPort(SubDiagram subDiagram, String variableName) throws Exception
    {
    	return subDiagram.stream(Node.class).findAny(de->Util.isInputPort(de) && variableName.equals(Util.getPortVariable(de)))
    	    .orElseThrow(() -> new Exception("Could not find port for '" + variableName + "' in " + subDiagram.getName()));
    }
    
    /*
     * method finds output port in subDiagram by variableName and returns it.
     */
    public static Node findOutputPort(SubDiagram subDiagram, String variableName) throws Exception
    {
    	return subDiagram.stream(Node.class).findAny(de->Util.isOutputPort(de) && variableName.equals(Util.getPortVariable(de)))
    	    .orElseThrow(() -> new Exception("Could not find port for '" + variableName + "' in " + subDiagram.getName()));
    }
    
    /*
     * method aligns input ports to the left edge of the subDiagram and output ports to the right edge.
     */
    public static void alignPorts(SubDiagram subDiagram)
    {
    	int xInputPortOffset = 5;
    	int xOutputPortOffset = 45;
    	int yPortOffset = 20;
    	
    	Point point = subDiagram.getLocation();
    	
    	Point pointInput = new Point(point);
    	pointInput.x += xInputPortOffset;
    	
    	Point pointOutput = new Point(point);
    	pointOutput.x += subDiagram.getShapeSize().width;
    	pointOutput.x -= xOutputPortOffset;
    	
    	for (Node node: subDiagram.getNodes()) 
    	{
    		if (Util.isInputPort(node)) 
    		{
    			node.setLocation(pointInput);
    			pointInput.y += yPortOffset;
    		}
    		else if (Util.isOutputPort(node))
    		{
    			node.setLocation(pointOutput);
    			pointOutput.y += yPortOffset;
    		}
    	}
    }
    
    /*
     * method creates directed connection on outerDiagram between outputVariableName from subDiagramFrom and inputVatiableName from subDiagramTo.
     */
    public static void createConnection(Diagram outerDiagram, SubDiagram subDiagramFrom, String outputVariableName, SubDiagram subDiagramTo, String inputVatiableName) throws Exception
    {
    	Node portFrom = BrainUtils.findOutputPort(subDiagramFrom, outputVariableName);
    	Node portTo = BrainUtils.findInputPort(subDiagramTo, inputVatiableName);
    	DiagramUtility.createConnection(outerDiagram, portFrom, portTo, true);
    }
    
    /*
     * method safely assigns an initial value to the variable on the diagram by name.
     */
    public static void setInitialValue(Diagram diagram, String variableName, Double value)
    {
        EModel emodel = diagram.getRole(EModel.class);
        
        if (emodel.getVariable(variableName) != null)
        {
            emodel.getVariable(variableName).setInitialValue(value);
        }
    }
    
    /*
     * method safely assigns an initial value and comment to the variable on the diagram by name.
     */
    public static void setInitialValue(Diagram diagram, String variableName, Double value, String comment)
    {
        EModel emodel = diagram.getRole(EModel.class);
        
        if (emodel.getVariable(variableName) != null)
        {
            emodel.getVariable(variableName).setInitialValue(value);
            emodel.getVariable(variableName).setComment(comment);
        }
    }
}
