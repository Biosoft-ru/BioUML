package biouml.standard.diagram;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.exception.InternalException;
import ru.biosoft.math.model.AstFunNode;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.AstVarNode;
import ru.biosoft.math.model.DefaultParserContext;
import ru.biosoft.math.model.Function;
import ru.biosoft.math.model.LinearFormatter;
import ru.biosoft.math.model.Node;
import ru.biosoft.math.model.PredefinedFunction;
import ru.biosoft.math.model.UndeclaredFunction;
import ru.biosoft.math.model.Utils;
import ru.biosoft.math.parser.Parser;
import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.DiagramType;
import biouml.model.Edge;
import biouml.model.ModelDefinition;
import biouml.model.Role;
import biouml.model.SemanticController;
import biouml.model.SubDiagram;
import biouml.model.dynamics.Connection;
import biouml.model.dynamics.Connection.Port;
import biouml.model.dynamics.DirectedConnection;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.ExpressionOwner;
import biouml.model.dynamics.MultipleConnection;
import biouml.model.dynamics.UndirectedConnection;
import biouml.model.dynamics.UndirectedConnection.MainVariableType;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.simulation.Preprocessor;
import biouml.standard.state.State;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Stub.ConnectionPort;
import biouml.standard.type.Type;

import com.developmentontheedge.beans.DynamicProperty;
import ru.biosoft.util.Pair;

/**
 * Preprocessor for composite ODE models. Transforms composite hierarchical model to flat model
 * Composite model contains submodels and connections between them. Connections links variables from different submodels.
 * There are two types of them:
 * <ul>
 * <li>directed - means that variable from one submodel is replaced by variable from another
 * <li>undirected - means that two variables should be merged into ont
 * </ul>
 * We assume here that one variable may have only one incoming connection
 * @Pending: switcher processing
 */
public class CompositeModelPreprocessor extends Preprocessor
{
    public static String VAR_PATH_DELIMITER = "/";
    
    public static final int AUTO_DETECT = 0;
    public static final int SBML_STYLE = 1;
    public static final int BIOUML_STYLE = 2;

    private boolean layout = false;
    private int nameStyle = AUTO_DETECT;

    /**Equation nodes that were already processed during processRateEqutions and should be skipped in processNodes*/
    private Set<biouml.model.Node> processedEquations;

    /**Should be used to establish connections between variable name in its own subdiagram and variable name in resultant plain diagram
     * It should be constructed recursively using a hierarchy of subdiagrams*/
    private Map<String, String> varPathMapping = new HashMap<>(); 
    
    /**Variable name + SubDiagram name -> new name in flat mode*/
    private HashMap<Pair<String, String>, String> substitutionMap;

    /**Function name + SubDiagram name -> new name in flat mode*/
    private HashMap<Pair<String, String>, String> functionSubstitutionMap;
    
    /**Mapping: subDiagram and variableName is mapped to new variableName in plain diagram, this new name is unique*/
    private HashMap<Pair<String, Diagram>, String> oldVariablesToNew;

    /**variable unique name to conversion factor*/
    private HashMap<String, String> conversionFactors;

    private HashMap<biouml.model.Node, biouml.model.Node> oldNodesToNew;
    
    /**Variables connected with directed connections*/
    private HashMap<String, AstStart> dConnected;

    private HashMap<biouml.model.Node, Point> subDiagramShift;

    /**Variables connected with undirected connections*/
    private HashMap<String, Set<String>> uConnected;
    private HashMap<String, Variable> uConnectedInfo;
    
    private HashMap<String, biouml.model.Node> newVarsToOldNodes;

    private HashMap<String, Variable> newVarsToOld;
    
    private HashMap<String, String> variablesInfo;

    private Diagram compositeDiagram;

    /**result (plain) diagram*/
    private Diagram diagram;

    /**result emodel*/
    private EModel eModel;

    private DiagramType type;

    /**if true then we should treat current composite diagram as contained inside subdiagram in another composite diagram*/
    protected boolean asSubDiagram = false;
    HashSet<String> variableIDS;

    public void setNameStyle(int style)
    {
        nameStyle = style;
    }

    public String getNewFunctionName(String oldName, String subDiagramName)
    {
        return functionSubstitutionMap.get(new Pair<>(oldName, subDiagramName));
    }
    
    public String getNewVariableName(String oldName, String subDiagramName)
    {
        return substitutionMap.get(new Pair<>(oldName, subDiagramName));
    }
    
    /**
     * returns mapping: complete name of subdiagram -> name of variable in subdiagram -> variable code name generated by the preprocessor
     */
    public Map<String, Map<String, String>> getCodeVariableNames()
    {
        if( substitutionMap != null )
        {
            Map<String, Map<String, String>> codeVariableNames = new HashMap<>();

            for( Map.Entry<Pair<String, String>, String> entry : substitutionMap.entrySet() )
            {
                String key = entry.getKey().getSecond();
                codeVariableNames.computeIfAbsent(key, k -> new HashMap<>()).put(entry.getKey().getFirst(), entry.getValue());
            }
            return codeVariableNames;
        }
        return null;
    }

    public Map<String, String> getVarPathToOldPath()
    {
        Map<String, String> result = new HashMap<>();
        for( Entry<Pair<String, String>, String> e : substitutionMap.entrySet() )
        {
            String newName = e.getValue();
            String oldPath = e.getKey().getSecond() + "\\" + e.getKey().getFirst();
            result.put(newName, oldPath);
        }
        return result;
    }

    /**Returns name which represents given element in the frames of composite diagram as a whole*/
    private String getUniqueVariableName(Diagram parent, String oldName)
    {
        return oldVariablesToNew.get(new Pair<>(oldName, parent));
    }

    public Diagram preprocess(Diagram compositeDiagram, DataCollection<?> origin) throws Exception
    {
        return preprocess(compositeDiagram, origin, null);
    }

    @Override
    public Diagram preprocess(Diagram compositeDiagram) throws Exception
    {
        return preprocess(compositeDiagram, null);
    }

    public Diagram preprocess(Diagram inputDiagram, DataCollection<?> origin, String newName) throws Exception
    {
        if( !accept(inputDiagram) )
            return inputDiagram;

        if( !DiagramUtility.isComposite(inputDiagram) )
            return asSubDiagram ? inputDiagram : inputDiagram.clone(origin, inputDiagram.getName());

        compositeDiagram = asSubDiagram ? inputDiagram : inputDiagram.clone(origin, inputDiagram.getName());

        if( nameStyle == AUTO_DETECT )
        {
            if( inputDiagram.getType().getClass().getName().endsWith("SbgnCompositeDiagramType") //dirty hack TODO: fix
                    || inputDiagram.getType().getClass().getName().endsWith("SbmlCompositeDiagramType") )
                nameStyle = SBML_STYLE;
            else
                nameStyle = BIOUML_STYLE;
        }

        compositeDiagram.getRole(EModel.class).setAutodetectTypes(false);

        long time = System.currentTimeMillis();
        log.info("Preprocessing diagram " + compositeDiagram.getName());

        init(compositeDiagram, origin, newName);
        diagram.setNotificationEnabled(false);
        boolean cdNotif = compositeDiagram.isNotificationEnabled();
        compositeDiagram.setNotificationEnabled(false);

        DiagramUtility.processBuses( compositeDiagram );

        //recursive processing of subDiagram
        processCompositeSubDiagrams(compositeDiagram);
        processSwitches(compositeDiagram);
        
        if( layout )
        {
            CompositeModelUtility.createView(compositeDiagram);
            subDiagramShift = CompositeModelUtility.generateShiftMap( compositeDiagram );
        }
        readVariableRoles( compositeDiagram);
        readVariables(compositeDiagram);
        readConnections(compositeDiagram);        
        processVariables(compositeDiagram, diagram);
        processVariableRoles(compositeDiagram, diagram, true);
        processVariableRoles(compositeDiagram, diagram, false);

        readFunctions(compositeDiagram, diagram);
        
        processRateEquations(compositeDiagram);
        readNodes(compositeDiagram, diagram);
        readClones(compositeDiagram, diagram);
        readReactions(compositeDiagram);
        readEdges(compositeDiagram);
        readPorts(compositeDiagram, diagram);

        for( String varName : eModel.getParameters().stream().map( v -> v.getName() ).filter( n -> dConnected.containsKey( n ) )
                .collect( Collectors.toList() ) )
            eModel.getVariables().remove(varName);

        processConstants(compositeDiagram);

        log.info("Done in " + ( System.currentTimeMillis() - time ) + " mseconds");

        DynamicProperty dp = compositeDiagram.getAttributes().getProperty("Plots");
        if( dp != null )
            diagram.getAttributes().add(new DynamicProperty(dp.getName(), dp.getType(), dp.getValue()));

        compositeDiagram.getRole(EModel.class).setAutodetectTypes(true);
        compositeDiagram.setNotificationEnabled(cdNotif);
        diagram.setNotificationEnabled(true);
        return diagram;
    }
    
    private void processVariables(Diagram compositeDiagram, Diagram diagram)
    {
        EModel emodel = diagram.getRole( EModel.class );
        for( Diagram dgr : SubDiagram.diagrams(compositeDiagram) )
        {
            boolean notif = dgr.isNotificationEnabled();
            dgr.setNotificationEnabled( false );
            for( Variable var : dgr.getRole(EModel.class).getVariables() )
            {
                if( var instanceof VariableRole || var.getName().startsWith( "$$" ))
                    continue;

                SubDiagram subDiagram = SubDiagram.getParentSubDiagram(dgr);
                Diagram diagramKey =  subDiagram != null ? subDiagram.getDiagram() : compositeDiagram;
                String uniqueName = getUniqueVariableName( diagramKey, var.getName() );        
                
                if (dConnected.containsKey( uniqueName ))
                   continue;
                
                String masterVariable = getMasterVariable( uniqueName );
                
                //add only main variables - replaced are not added to the model
                if (masterVariable.equals( uniqueName ))
                {
                    Variable realVar = uConnectedInfo.get( uniqueName );
                    if( realVar != null )
                        emodel.put( realVar.clone( uniqueName ) );
                    else
                        emodel.put(var.clone(uniqueName));
                }

                String key = subDiagram != null ? subDiagram.getCompleteNameInDiagram() : "";
                substitutionMap.put(new Pair<>(var.getName(), key), uniqueName);
            }
            dgr.setNotificationEnabled( notif );
        }
    }

    /**
     * Tries to define type of a plain version of diagram
     * TODO: create more complicated algorithm which will compare subdiagrams and tries to find one with most wide diagram type, so others could be transformed to it.
     * Usually it should be SbgnDiagramType
     * 
     * Currently if we have sbml and math diagrams and algorithm will pick MathDiagramType - species could not be transformed to it.
     */
    private static DiagramType defineType(Diagram diagram)
    {
        DiagramType type = diagram.getType();
        List<SubDiagram> subDiagrams = Util.getSubDiagrams( diagram );
        for (SubDiagram subDiagram: subDiagrams)
        {
           DiagramType innerType = subDiagram.getDiagram().getType();
           if (!(innerType instanceof CompositeDiagramType))
               return innerType;
        }
        return type;
    }

    private Diagram init(Diagram compositeDiagram, DataCollection<?> origin, String newName) throws Exception
    {
        type = defineType(compositeDiagram);
        conversionFactors = new HashMap<>();
        variableIDS = new HashSet<>();
        dConnected = new HashMap<>();
        uConnected = new HashMap<>();
        uConnectedInfo =new HashMap<>();
        
        newVarsToOld = new HashMap<>();
        oldVariablesToNew = new HashMap<>();
        oldNodesToNew = new HashMap<>();
        functionSubstitutionMap = new HashMap<>();
        substitutionMap = new HashMap<>();
        varPathMapping = new HashMap<>();
        variablesInfo = new HashMap<>();
        newVarsToOldNodes = new HashMap<>();
        processedEquations = new HashSet<>();
                
        if( newName == null )
            newName = compositeDiagram.getName() + "_plain";

        diagram = type.createDiagram(origin, newName, null);
        eModel = diagram.getRole(EModel.class);

        return diagram;
    }

    /**
     * Reads variables from all subDiagrams of <b>compositeDiagram</b> and fills map <b>oldVariablesToNew</b>
     * @param compositeDiagram
     */
    private void readVariables(Diagram compositeDiagram) throws Exception
    {
        for( Diagram dgr : SubDiagram.diagrams(compositeDiagram) )
        {
            for( Variable var : dgr.getRole(EModel.class).getVariables() )
            {
                if( var instanceof VariableRole )
                    continue;

                String oldVarName = var.getName();
                String newVarName = generateUniqueVariableName(oldVarName, dgr);
                oldVariablesToNew.put(new Pair<>(oldVarName, dgr), newVarName);
                newVarsToOld.put( newVarName, var);
            }
        }
    }

    private void readFunctions(Diagram compositeDiagram, Diagram diagram)
    {
        EModel newEmodel = diagram.getRole( EModel.class );
        Set<String> newFunctions = new HashSet<>();
        //first round - generate unique names
        for( Diagram dgr : SubDiagram.diagrams( compositeDiagram ) )
        {
            for( biouml.model.dynamics.Function f : dgr.getRole( EModel.class ).getFunctions() )
            {
                String functionName = f.getName();
                SubDiagram subDiagram = SubDiagram.getParentSubDiagram( dgr );
                String key = subDiagram != null ? subDiagram.getCompleteNameInDiagram() : "";                               
                String newFunctionName = generateUniqueFunctionName( functionName , subDiagram, false, newFunctions );
                newFunctions.add( newFunctionName );
                functionSubstitutionMap.put( new Pair<>( functionName, key ), newFunctionName );
            }
        }
        
        //second round add functions
        for( Diagram dgr : SubDiagram.diagrams( compositeDiagram ) )
        {
            for( biouml.model.dynamics.Function f : dgr.getRole( EModel.class ).getFunctions() )
            {        
                String functionName = f.getName();
                SubDiagram subDiagram = SubDiagram.getParentSubDiagram( dgr );
                String key = subDiagram != null ? subDiagram.getCompleteNameInDiagram() : "";         
                
                biouml.model.Node oldNode = (biouml.model.Node)f.getDiagramElement();
                String newFunctionName = functionSubstitutionMap.get( new Pair<>(functionName, key) );
                String rightSide = f.getRightHandSide(); 
                String processed = this.processFunction( rightSide, dgr  );
                biouml.model.dynamics.Function newFunction = new biouml.model.dynamics.Function( null, newFunctionName, f.getArguments(),
                        processed );
                DiagramElementGroup group = diagram.getType().getSemanticController().createInstance( diagram,
                        biouml.model.dynamics.Function.class, oldNode.getLocation(), newFunction );
                group.putToCompartment();
                group.nodesStream().map( n -> n.getRole() ).select( biouml.model.dynamics.Function.class ).forEach( fun -> newEmodel
                        .readMath( fun.getFormula(), fun, eModel.getVariableResolver( EModel.VARIABLE_NAME_BY_ID ), true ) );
            }          
        }
    }
    
    private static String generateUniqueFunctionName(String name, SubDiagram subDiagram, boolean sbmlStyle, Set<String> names)
    {
        String result =  (sbmlStyle && subDiagram != null)? subDiagram.getName() + "__" + name: name;        
        int i = 1;
        while( names.contains( result ))
            result = name + "_" + i++;
        return result;
    }
    

    /**
     * Reading of all variable roles in the composite diagram and all simple (not-composite) diagrams in it<br>
     * Generating 3 maps:
     * <b>oldVariablesToNew</b> containing mapping (old variable role name , diagram) -> new variable role name
     * <b>newVarsToOldNodes</b> containing mapping new variable role name -> old node
     * <b>substitutionMap</b> containing mapping (old variable role name , diagram) -> new variable role name ?? the same as oldVariablesToNew ??
     * @param diagram
     * @throws Exception
     */
    private void readVariableRoles(Diagram diagram)
    {
        SubDiagram.diagrams(diagram).forEach(dgr -> readVariableRoles(dgr, ""));
    }

    /**
     * Reads all variable roles from <b>compartment</b> generates new unique name for its future copy in plain diagram
     * (We need to know unique name for variable role to create correct node for it)
     * @param compartment - parent of old variable roles
     * @param subDiagram - subdiagram of old variable roles (compartment should belong to that diagram)
     * @param newCompartmentPath - path to the copy of compartment in plain diagram
     */
    public void readVariableRoles(Compartment compartment, String newCompartmentPath)
    {
        Diagram diagram = CompositeModelUtility.getDiagram(compartment);

        for( biouml.model.Node node : compartment.stream( biouml.model.Node.class ).filter( n->n.getRole() instanceof VariableRole) )
        {
            Role role = node.getRole();
            String oldVarName = ( (VariableRole)role ).getName();
            String newVarName = generateUniqueVariableName( oldVarName, diagram );

            if( role.getDiagramElement().equals( node ) )
            {
                if( ! ( compartment instanceof Diagram ) )
                    newVarName = newVarName.replace( compartment.getCompleteNameInDiagram(), newCompartmentPath );
                oldVariablesToNew.put( new Pair<>( oldVarName, diagram ), newVarName );
                newVarsToOldNodes.put( newVarName, (biouml.model.Node)role.getDiagramElement() );
                newVarsToOld.put( newVarName, (VariableRole)role );
//                SubDiagram subDiagram = SubDiagram.getParentSubDiagram( diagram );
//                substitutionMap.put( new Pair<>( oldVarName, subDiagram != null ? subDiagram.getCompleteNameInDiagram() : "" ),
//                        newVarName );
            }

            if( node instanceof Compartment )
                readVariableRoles( (Compartment)node, generateCompartmentName( newVarName ) );
        }
    }


    /**
     * Reads connections and fills maps <b>dConnected</b> and <b>uConnected</b>
     * which holds information about connected variables in terms of their unique names from <b>oldVariablesToNew</b>
    */
    private void readConnections(Diagram compositeDiagram) throws Exception
    {
        for( Connection c : compositeDiagram.recursiveStream().select(Edge.class).map(Edge::getRole).select(Connection.class)
                .flatCollection(c -> MultipleConnection.getBasicConnections(c).toSet()) )
            readConnection(c);
              
        transformUndirectedConnections();
        fixMixedConnections();
        
        adjustUniqueNames();
        adjustDirectedConnections();
        
        //processing directed connections
        for( AstStart function : dConnected.values() )
            extendFunction(function);        
    }

    /**
     * Method reads connection and fills maps <b>dConnected</b> and <b>uConnected</b>
     * which holds information about connected variables in terms of their unique names from <b>oldVariablesToNew</b>
     * @param connection
     */
    private void readConnection(Connection connection) throws Exception
    {
        Edge edge = (Edge)connection.getDiagramElement();

        if( Util.isConstant(edge.getInput()) || Util.isPlot(edge.getInput()) || Util.isPlot(edge.getOutput()) )
            return;

        if( Util.isPropagatedPort(edge.getInput()) || Util.isPropagatedPort(edge.getOutput()) )
            return;

        Diagram inDiagram = CompositeModelUtility.getDiagram(edge.getInput());
        Diagram outDiagram = CompositeModelUtility.getDiagram(edge.getOutput());

        String inOldVariableName = connection.getInputPort().getVariableName();
        String outOldVariableName = connection.getOutputPort().getVariableName();

        EModel outModel = outDiagram.getRole(EModel.class);
        EModel inModel = inDiagram.getRole(EModel.class);
        Variable outOldVariable = outModel.getVariable(outOldVariableName);
        Variable inOldVariable = inModel.getVariable(inOldVariableName);

        if( outOldVariable == null && Util.isPort(edge.getOutput()) )
        {
            outOldVariableName = Util.getPortVariable(edge.getOutput());
            outOldVariable = outModel.getVariable(outOldVariableName);
        }

        if( outOldVariable == null )
            throw new Exception("Connection " + connection.getDiagramElement().getName() + " is not correct: variable " + outOldVariableName
                    + " can not be found in module " + outDiagram.getName());

        if( inOldVariable == null && Util.isPort(edge.getInput()) )
        {
            inOldVariableName = Util.getPortVariable(edge.getInput());
            inOldVariable = inModel.getVariable(inOldVariableName);
        }

        if( inOldVariable == null )
            throw new Exception("Connection " + connection.getDiagramElement().getName() + " is not correct: variable " + inOldVariableName
                    + " can not be found in module " + inDiagram.getName());

        String inVariableName = getUniqueVariableName(inDiagram, inOldVariableName);
        String outVariableName = getUniqueVariableName(outDiagram, outOldVariableName);

        if( connection instanceof DirectedConnection )
        {
            //if function is defined we use it instead of variable name
            String function = ( (DirectedConnection)connection ).getFunction();
            if( function == null || function.equals("") )
            {
                function = inOldVariableName;
                SubDiagram subDiagram =  SubDiagram.getParentSubDiagram(outDiagram);
                String key = subDiagram != null ? subDiagram.getCompleteNameInDiagram() : "";
                substitutionMap.put(new Pair<>(outOldVariableName, key), inVariableName);
            }

            AstStart functionNode = (AstStart)parse(function);
            renameVariables(functionNode, inDiagram);

            //            eModel.getVariables().remove( outVariableName );
            if( dConnected.containsKey(outVariableName) )
                throw new Exception("Multiple directed connections to the same variable " + outOldVariableName);

            dConnected.put(outVariableName, functionNode);
        }
        //reading of undirected connection
        else
        {
            String auxVariableName = null;
            String mainVariableName = null;
            Double initialValue = null;
            MainVariableType type = ( (UndirectedConnection)connection ).getMainVariableType();

            if( type != MainVariableType.NOT_SELECTED )
            {
                if( type == MainVariableType.INPUT )
                {
                    mainVariableName = inVariableName;
                    auxVariableName = outVariableName;
                }
                else
                {
                    mainVariableName = outVariableName;
                    auxVariableName = inVariableName;
                }
                String factor = ( (UndirectedConnection)connection ).getConversionFactor();
                if( !factor.isEmpty() )
                    conversionFactors.put(mainVariableName, factor);
            }
            else
            {
                initialValue = ( (UndirectedConnection)connection ).getInitialValue();

                //Main variable priority:
                //1. Nodes with variable roles
                //3. Parameters without nodes
                //***************************
                //Initial value priority:
                //Connection value
                if( inOldVariable instanceof VariableRole && outOldVariable instanceof VariableRole )
                {
                    boolean outIsMain;
                   
                    outIsMain = ( inVariableName.length() > outVariableName.length() );


                    if( outIsMain )
                    {
                        mainVariableName = outVariableName;
                        auxVariableName = inVariableName;
                        initialValue = inOldVariable.getInitialValue();
                    }
                    else
                    {
                        mainVariableName = inVariableName;
                        auxVariableName = outVariableName;
                        initialValue = outOldVariable.getInitialValue();
                    }

                }
                else
                {
                    initialValue = ( (UndirectedConnection)connection ).getInitialValue();
                    if( inOldVariable instanceof VariableRole )
                    {
                        mainVariableName = inVariableName;
                        auxVariableName = outVariableName;
                    }
                    else
                    {
                        mainVariableName = outVariableName;
                        auxVariableName = inVariableName;
                    }
                }
            }
            registerUndirectedConnection(mainVariableName, auxVariableName, initialValue);
        }
    }
    
    /**
     * Method renames all local variables in expression according to their new names in plain diagram
     * @param function
     * @param subDiagram
     */
    private void renameVariables(Node expression, Diagram subDiagram)
    {
        Utils.deepChildren(expression).select(AstVarNode.class).forEach(var -> {
            String newName = getUniqueVariableName(subDiagram, var.getName());
            var.setName(newName);
            var.setTitle(newName);
        });
    }

    private void processCompositeSubDiagrams(Diagram diagram) throws Exception
    {
        for( SubDiagram subDiagram : diagram.recursiveStream().select(SubDiagram.class) )
        {
            Diagram associatedDiagram = subDiagram.getDiagram();
            if( DiagramUtility.containModules(associatedDiagram) ) //transform only if it actually (not potentially) contain modules
            {
                boolean notif = associatedDiagram.isNotificationEnabled();
                associatedDiagram.setNotificationEnabled( false );
                subDiagram = processCompositeSubDiagram(diagram, subDiagram);
                associatedDiagram.setNotificationEnabled( notif );
            }
        }
    }

    private void readReactions(Diagram diagram) throws Exception
    {
        for( Diagram dgr : SubDiagram.diagrams(diagram) )
            processReactions(dgr, dgr, this.diagram);
    }

    private void readEdges(Diagram diagram) throws Exception
    {
        for( Diagram dgr : SubDiagram.diagrams(diagram) )
            readEdges(dgr, this.diagram);
    }


    public SubDiagram processCompositeSubDiagram(Diagram compositeDiagram, SubDiagram subDiagram) throws Exception
    {
        CompositeModelPreprocessor preprocessor = new CompositeModelPreprocessor();
        preprocessor.asSubDiagram = true; 
        Diagram innerDiagram = subDiagram.getDiagram();
        innerDiagram.getAttributes().remove(SubDiagram.RELATIVE_SUBDIAGRAM); //we should consider his diagram as a top level for this preprocessing
        Diagram processedSubDiagram = preprocessor.preprocess(innerDiagram, null);
        SubDiagram newSubDiagram = new SubDiagram(compositeDiagram, processedSubDiagram, subDiagram.getName());

        for( DynamicProperty dp : subDiagram.getAttributes() )
            newSubDiagram.getAttributes().add(dp);

        newSubDiagram.setDiagram(processedSubDiagram);
        
        varPathMapping.putAll(preprocessor.getVarPathMapping(subDiagram.getName()));

        boolean in = false;

        //rename variable in all correspondent connections
        for( Edge edge : Util.getEdges(subDiagram) )
        {
            if( edge.getInput().equals(subDiagram) )
            {
                in = true;
                edge.setInput(newSubDiagram);
                newSubDiagram.addEdge(edge);
            }
            else if( edge.getOutput().equals(subDiagram) )
            {
                edge.setOutput(newSubDiagram);
                newSubDiagram.addEdge(edge);
            }
            else if( edge.getInput().getParent().equals(subDiagram) )
            {
                in = true;
                String portName = edge.getInput().getName(); //in subdiagram port can be only on the top level, because no other compartment can not be in subdiagram element
                biouml.model.Node newPort = newSubDiagram.findNode(portName);
                edge.setInput(newPort);
                newPort.addEdge(edge);
            }
            else if( edge.getOutput().getParent().equals(subDiagram) )
            {
                String portName = edge.getOutput().getName();
                biouml.model.Node newPort = newSubDiagram.findNode(portName);
                edge.setOutput(newPort);
                newPort.addEdge(edge);
            }

            if( edge.getRole() instanceof Connection )
            {
                Connection connection = edge.getRole(Connection.class);
                Port port;
                String varName;
                if( in )
                {
                    varName = Util.getPortVariable(edge.getInput());
                    port = connection.getInputPort();

                }
                else
                {
                    varName = Util.getPortVariable(edge.getOutput());
                    port = connection.getOutputPort();
                }
                port.setVariableName(varName);
                port.setVariableTitle(varName);
            }
        }
        boolean notif = compositeDiagram.isNotificationEnabled();
        compositeDiagram.setNotificationEnabled(false);
        State state = compositeDiagram.getCurrentState();
        compositeDiagram.restore();

        compositeDiagram.remove(subDiagram.getName());
        newSubDiagram.save();

        if( state != null )
            compositeDiagram.setStateEditingMode(state);

        compositeDiagram.setNotificationEnabled(notif);
        return newSubDiagram;
    }

    /**
    * Copy all content from subdiagram to the plain diagram
    * @param source
    * @param target
    * @throws Exception
    */
    public void readEdges(Compartment compartment, Compartment newCompartment) throws Exception
    {
        for( DiagramElement de : compartment )
        {
            try
            {
                if( de instanceof Compartment )
                {
                    readEdges((Compartment)de, (Compartment)oldNodesToNew.get(de));
                }
                else if( de instanceof biouml.model.Edge )
                {
                    biouml.model.Edge edge = (biouml.model.Edge)de;

                    //specie references are already added while reactions processing
                    if( edge.getKernel() instanceof SpecieReference )
                        continue;

                    //source sink edges should not be copied. They are created anew by semantic controller when new reaction is created
                    if ( edge.getKernel() instanceof Stub && Util.isSourceSink(edge.getInput()) || Util.isSourceSink(edge.getOutput()))
                        continue;

                    biouml.model.Node inputNode = oldNodesToNew.get(edge.getInput());
                    biouml.model.Node outputNode = oldNodesToNew.get(edge.getOutput());

                    //for some reason (eq: it is connection port) we may not add node to plain diagram
                    if( inputNode == null || outputNode == null )
                        continue;

                    biouml.model.Edge newEdge = CompositeModelUtility.clone(edge, newCompartment, edge.getKernel(), inputNode, outputNode);
                    newCompartment.put(newEdge);
                }
            }
            catch( Exception ex )
            {
                throw new Exception("Error during edge" + de.getName() + " processing: " + ex.getMessage());
            }
        }
    }

    private biouml.model.Node readSpecies(biouml.model.Node node, Compartment newCompartment, biouml.model.Node mainNode, boolean readCompartments) throws Exception
    {
        Diagram subDiagram = CompositeModelUtility.getDiagram(node);
        VariableRole oldVariable = node.getRole(VariableRole.class);
        String uniqueVariableName = getUniqueVariableName(subDiagram, oldVariable.getName());
                
        String newNodeName = generateNodeName(uniqueVariableName);
        
        biouml.model.Node newNode = null;
        if( mainNode != null && !mainNode.equals( node ) )
        {
            newNodeName = DefaultSemanticController.generateUniqueName( newCompartment, newNodeName );
            newNode = Diagram.getDiagram( mainNode ).getType().getSemanticController().cloneNode( mainNode, newNodeName, node.getLocation() );
        }
        else
        {
            Base oldKernel = node.getKernel();
            Base newKernel = CompositeModelUtility.cloneKernel(oldKernel, null, newNodeName);
            newNode = node.clone(newCompartment, newNodeName, newKernel);
            if (node instanceof Compartment)
            ((Compartment)newNode).clear();
            
            VariableRole varRole = null;
            
            //this variable is replaced by another
            if (uConnectedInfo.containsKey( uniqueVariableName ))            
                varRole = ((VariableRole)uConnectedInfo.get( uniqueVariableName )).clone( newNode, uniqueVariableName );            
            else
                varRole =  newNode.getRole( VariableRole.class ).clone( newNode, uniqueVariableName);//VariableRole.createName( newNode, false ) );
            
            newNode.setRole( varRole );
            eModel.put((VariableRole)newNode.getRole());
        }
        setLocation(newNode, subDiagram);
        newNode.setLocation(getNewLocation(newNode, subDiagram));

        if( dConnected.containsKey(uniqueVariableName) )
        {
            newNode.getRole(VariableRole.class).setBoundaryCondition(true);
            createConnectionEquation(newNode, dConnected.get(uniqueVariableName));
        }
        
        if (Type.TYPE_COMPARTMENT.equals( node.getKernel().getType()))
        processVariableRoles( (Compartment)node, (Compartment)newNode, readCompartments );
   
        newNode.setOrigin( newCompartment );
        newCompartment.put( newNode );
        oldNodesToNew.put( node, newNode );
        
        SubDiagram parentSubDiagram = SubDiagram.getParentSubDiagram(subDiagram);
        String key = parentSubDiagram != null ? parentSubDiagram.getCompleteNameInDiagram() : "";
        substitutionMap.put(new Pair<>(oldVariable.getName(), key), ((VariableRole)newNode.getRole()).getName());
        
        return newNode;
    }

    private void setLocation(biouml.model.Node node, Diagram subDiagram)
    {
        node.recursiveStream().select(biouml.model.Node.class).forEach(n -> n.setLocation(getNewLocation(n, subDiagram)));
    }

    private Compartment readCompartment(Compartment compartment, Compartment newCompartment) throws Exception
    {
        Diagram subDiagram = CompositeModelUtility.getDiagram(compartment);
        VariableRole oldVariable = compartment.getRole(VariableRole.class);
        String uniqueVariableName = getUniqueVariableName(subDiagram, oldVariable.getName());

        newCompartment.clear();
        
        newCompartment.getCompartment().put( newCompartment );

        if( !getMasterVariable(uniqueVariableName).equals(uniqueVariableName) )
        {
            String uniqueName = getUniqueVariableName(subDiagram, oldVariable.getName());
            eModel.getVariables().remove(uniqueName);
            return null;
        }
        if( compartment.getView() == null )
            CompositeModelUtility.createView(compartment);
        Rectangle rectangle = compartment.getView().getBounds();
        rectangle.setLocation(newCompartment.getLocation());

        for( String auxVariableName : getAuxVariables(uniqueVariableName) )
        {
            biouml.model.Node oldAuxNode = newVarsToOldNodes.get(auxVariableName);
            if( oldAuxNode.equals(compartment) )
                continue;
            readNodes((Compartment)oldAuxNode, newCompartment);

            if( oldAuxNode.getView() == null )
                CompositeModelUtility.createView(oldAuxNode);
            Rectangle r1 = oldAuxNode.getView().getBounds();
            r1.setLocation(getNewLocation(oldAuxNode, subDiagram));

            rectangle = rectangle.union(r1);

            //remap auxiliary old compartment to newly created main compartment
            oldNodesToNew.put(oldAuxNode, newCompartment);
        }
        newCompartment.setShapeSize(rectangle.getSize());
        newCompartment.setLocation(rectangle.getLocation());
        readNodes(compartment, newCompartment);
        return newCompartment;
    }

    public static SubDiagram processSwitch(Compartment switchNode) throws Exception
    {
        String condition = switchNode.getAttributes().getProperty(Util.CONDITION).getValue().toString();
        Diagram switchDiagram = new Diagram(null, new Stub(null, switchNode.getName()), new PathwaySimulationDiagramType());
        switchDiagram.setRole(new EModel(switchDiagram));
        String formula = "piecewise( " + condition + "=> experiment; default )";
        Equation eq = new Equation(null, Equation.TYPE_SCALAR, "out", formula);
        DiagramElement de = switchDiagram.getType().getSemanticController()
                .createInstance( switchDiagram, Equation.class, new Point( 0, 0 ), eq ).getElement();
        switchDiagram.put(de);

        HashMap<String, Edge> portNameToEdge = new HashMap<>();

        for( biouml.model.Node portNode : switchNode.getNodes() )
        {
            if( Util.isInputPort(portNode) || Util.isOutputPort(portNode) )
            {
                switchDiagram.put(portNode.clone(switchDiagram, portNode.getName()));
                portNode.edges().filter(e -> Util.isDirectedConnection(e)).forEach(e -> portNameToEdge.put(portNode.getName(), e));
            }
        }
        SubDiagram subDiagram = new SubDiagram(switchNode, switchDiagram, switchNode.getName());
        subDiagram.setLocation(switchNode.getLocation());
        subDiagram.setShapeSize(switchNode.getShapeSize());
        subDiagram.updatePorts();

        //redirecting edges to subDiagram ports
        for( biouml.model.Node portNode : subDiagram.getNodes() )
        {
            String originalPortName = portNode.getAttributes().getValue(SubDiagram.ORIGINAL_PORT_ATTR).toString();
            Edge e = portNameToEdge.get(originalPortName);

            if( e == null )
                continue;

            portNode.addEdge(e);

            if( e.getInput().getName().equals(originalPortName) )
                e.setInput(portNode);
            else
                e.setOutput(portNode);
        }
        return subDiagram;
    }

    private static void processSwitches(Diagram compositeDiagram) throws Exception
    {
        for( Compartment node : compositeDiagram.recursiveStream().select(Compartment.class).filter(Util::isSwitch) )
            compositeDiagram.put(processSwitch(node));
    }

    private void processConstants(Diagram compositeDiagram) throws Exception
    {
        for( biouml.model.Node node : compositeDiagram.recursiveStream().select(biouml.model.Node.class).filter(Util::isConstant) )
        {
            double value = Double.parseDouble(node.getAttributes().getProperty(Util.INITIAL_VALUE).getValue().toString());
            for( Edge edge : node.edges().filter(Util::isDirectedConnection) )
            {
                biouml.model.Node otherNode = edge.getOtherEnd(node);
                String variableName = Util.getPortVariable(otherNode); //only port can be here
                Diagram d = CompositeModelUtility.getDiagram(otherNode);
                SubDiagram subDiagram = SubDiagram.getParentSubDiagram(d);
                Diagram keyDiagram = subDiagram != null? subDiagram.getDiagram(): compositeDiagram; 
//                String key = subDiagram != null ? subDiagram.getCompleteNameInDiagram() : "";
                String newVariableName = getUniqueVariableName( keyDiagram, variableName );//getNewVariableName(variableName, key);
                Variable var = eModel.getVariable(newVariableName);
                var.setInitialValue(value);
                var.setConstant(true);
            }
        }
    }

    /**
     * Restore name of node by name of its variable
     * @param variableName
     * @return
     */
    private String generateCompartmentName(String variableName)
    {
        String compartmentName = variableName;
        if( compartmentName.startsWith("$") )
            compartmentName = compartmentName.substring(1);
        return compartmentName.replace("\"", "");
    }

    private String generateNodeName(String variableName)
    {
        String nodeName = variableName;
        if( nodeName.startsWith("$") )
            nodeName = nodeName.substring(1);
        if( nodeName.contains(".") )
            nodeName = nodeName.substring(nodeName.lastIndexOf(".") + 1);
        return nodeName.replace("\"", "");
    }

    /**
     * Process undirected connections between species, set clone attributes and new initial values<br>
     * At this point, all master nodes should already exist in plain diagram!
     * @param compartment - parent for considered species
     * @param newCompartment - new parent in plain diagram
     * @throws Exception
     */
    public void readClones(Compartment compartment, Compartment newCompartment) throws Exception
    {
        Set<VariableRole> toRemove = new HashSet<>();
        for( VariableRole var : eModel.getVariableRoles() )
        {
            try
            {
                String name = var.getName();
                DiagramElement de = var.getDiagramElement();

                String mainVarName = this.getMasterVariable(name);
                if( !name.equals(mainVarName) )
                {
                    Variable mainVariable = eModel.getVariable(mainVarName);
                    if( ! ( mainVariable instanceof VariableRole ) )
                        throw new InternalException("Variable " + mainVarName + " is not VariableRole");

                    DiagramElement mainDe = ( (VariableRole)mainVariable ).getDiagramElement();
                    de.setRole((VariableRole)mainVariable);
                    de.setTitle(mainDe.getTitle());
                    ( (VariableRole)mainVariable ).addAssociatedElement(de);
                    de.getAttributes().add(new DynamicProperty("sbgn:cloneMarker", String.class, mainDe.getCompleteNameInDiagram()));
                    toRemove.add(var);
                }
            }
            catch( Exception ex )
            {
                throw new Exception("Error during " + var.getName() + " processing: " + ex.getMessage());
            }
        }
        for( VariableRole var : toRemove )
            eModel.getVariables().remove(var.getName());
    }

    protected void readConnectedEquations(Compartment compartment, HashMap<String, List<Equation>> varToRateEqs)
    {
        for( biouml.model.Node node : compartment.getNodes() )
        {
            if( node.getRole() instanceof Equation )
            {
                Equation eq = node.getRole(Equation.class);
                if( eq.getType().equals(Equation.TYPE_RATE) )
                {
                    String uniqueName = getUniqueVariableName(Diagram.getDiagram(node), eq.getVariable());
                    String masterVariable = this.getMasterVariable(uniqueName);
                    varToRateEqs.computeIfAbsent(masterVariable, k -> new ArrayList<>()).add(eq);
                }
            }
            else if( node instanceof SubDiagram )
            {
                readConnectedEquations( ( (SubDiagram)node ).getDiagram(), varToRateEqs);
            }
            else if( node instanceof Compartment )
            {
                readConnectedEquations((Compartment)node, varToRateEqs);
            }
        }
    }

    public void processRateEquations(Compartment compartment)
    {
        HashMap<String, List<Equation>> varToRateEqs = new HashMap<>();
        readConnectedEquations(compartment, varToRateEqs);

        for( Map.Entry<String, List<Equation>> entry : varToRateEqs.entrySet() )
        {
            try
            {
                String masterVariable = entry.getKey();
                if( entry.getValue().size() <= 1 )
                    continue;
                biouml.model.Node node = null;
                StringBuilder formulaBuilder = new StringBuilder();

                for( Equation eq : entry.getValue() )
                {
                    Diagram diagram = Diagram.getDiagram(eq.getDiagramElement());

                    String eqFormula = eq.getFormula();
                    if( formulaBuilder.length() > 0 )
                        formulaBuilder.append("+");

                    eqFormula = processExpression(eqFormula, diagram, true);
                    SubDiagram subDiagram = SubDiagram.getParentSubDiagram(diagram);
                    if( subDiagram != null )
                    {
                        DynamicProperty timeScale = subDiagram.getAttributes().getProperty(Util.TIME_SCALE);
                        if( timeScale != null && !timeScale.getValue().toString().isEmpty() )
                            eqFormula = "(" + eqFormula + ")/" + timeScale.getValue().toString();
                    }
                    formulaBuilder.append(eqFormula);

                    String uniqueVariable = this.getUniqueVariableName(diagram, eq.getVariable());
                    if( uniqueVariable.equals(masterVariable) )
                        node = (biouml.model.Node)eq.getDiagramElement();

                    processedEquations.add((biouml.model.Node)eq.getDiagramElement());
                }

                if( node == null )
                    node = (biouml.model.Node)varToRateEqs.get(masterVariable).get(0).getDiagramElement();

                if( this.conversionFactors.containsKey(masterVariable) )
                {
                    String conversionFactor = conversionFactors.get(masterVariable);
                    formulaBuilder.insert(0, '(');
                    formulaBuilder.append(")*").append(conversionFactor);
                    //formula = "(" + formulaBuilder.toString() + ")*" + conversionFactor;
                }

                Equation eq = new Equation(null, Equation.TYPE_RATE, masterVariable, formulaBuilder.toString());
                Point location = getNewLocation(node, Diagram.getDiagram(node));
                DiagramElement de = diagram.getType().getSemanticController().createInstance( diagram, Equation.class, location, eq )
                        .getElement();
                de.save();
            }
            catch( Exception ex )
            {
                log.log(Level.SEVERE, "Error during rate equations processing");
            }
        }
    }

    public void readPorts(Compartment compartment, Compartment newCompartment) throws Exception
    {
        Diagram subDiagram = CompositeModelUtility.getDiagram(compartment);

        for( biouml.model.Node node : compartment.getNodes() )
        {
            if( Util.isPort(node) && node.getParent().equals(compositeDiagram) )
            {
                biouml.model.Node newNode = null;
                String oldVariable = Util.getPortVariable(node);

                if( Util.isPropagatedPort(node) )
                {
                    newNode = node.clone(newCompartment, generateUniqueNodeName(node.getName(), newCompartment));
                    if( !Diagram.getDiagram(node).equals(compositeDiagram) )
                        continue;

                    for( Edge edge : node.getEdges() )
                    {
                        if( edge.getKernel() instanceof Stub.DirectedConnection || edge.getKernel() instanceof Stub.UndirectedConnection )
                        {
                            biouml.model.Node anotherNode = edge.getOtherEnd(node);
                            if( Util.isPort(anotherNode) )
                            {
                                oldVariable = Util.getPortVariable(anotherNode);
                                SubDiagram portSubDiagram = (SubDiagram)anotherNode.getParent();
                                oldVariable = getUniqueVariableName(portSubDiagram.getDiagram(), oldVariable);
                            }
                        }
                    }
                }
                else if( Util.isPublicPort(node) )
                {
                    newNode = node.clone(newCompartment, generateUniqueNodeName(node.getName(), newCompartment));
                    oldVariable = getUniqueVariableName(Diagram.getDiagram(node), Util.getPortVariable(node));
                }
                else
                {
                    continue;
                }

                String newVariable = getMasterVariable(oldVariable);
                //some sort of hack to consider direcetd connections TODO: refactor
                if( dConnected.containsKey(oldVariable) )
                {
                    AstStart start = dConnected.get(oldVariable);
                    if( start.jjtGetNumChildren() == 1 && start.jjtGetChild(0) instanceof AstVarNode )
                    {
                        AstVarNode varNode = (AstVarNode)start.jjtGetChild(0);
                        newVariable = varNode.getName();
                    }
                }
                Util.setPortVariable(newNode, newVariable);
                Util.setPublic( newNode );
//                newNode.getAttributes().remove(ConnectionPort.ACCESS_TYPE);
                newNode.setLocation(getNewLocation(newNode, subDiagram));
                newNode.save();
                continue;
            }
        }
    }
    
    public void processCompartments(Compartment compartment, Compartment newCompartment) throws Exception
    {
        if (compartment instanceof Diagram)
        {
            for( Diagram dgr : compartment.recursiveStream().select(SubDiagram.class).map(SubDiagram::getDiagram) )
                processCompartments(dgr, newCompartment);
        }
        for( biouml.model.Node node : compartment.stream( Compartment.class ).filter( c -> Type.TYPE_COMPARTMENT.equals(c.getKernel().getType())))
        {
            
        }
    }

    /**
     * Reads nodes from <b>compartment</b> (which belongs to <b>subDiagram</b>, creates their clones and puts them to <b>newCompartment</b> (in plain diagram)
     * @throws Exception
     */
    public void processVariableRoles(Compartment compartment, Compartment newCompartment, boolean processCompartments) throws Exception
    {
        if (compartment instanceof Diagram)
        {
            for( Diagram dgr : compartment.recursiveStream().select(SubDiagram.class).map(SubDiagram::getDiagram) )
                processVariableRoles(dgr, newCompartment, processCompartments);
        }
 
        for( biouml.model.Node node : compartment.stream( biouml.model.Node.class ).filter( c -> c.getRole() instanceof VariableRole ) )
        {
            boolean isCompartment = Type.TYPE_COMPARTMENT.equals( node.getKernel().getType() );

            if( isCompartment )
            {
                if( !processCompartments ) //we read species, compartment was read already
                {
                    Compartment newNode = (Compartment)oldNodesToNew.get( node );
                    processVariableRoles( (Compartment)node, (Compartment)newNode, processCompartments );
                    continue;
                }
            }
            else if( processCompartments )
            {
                continue;
            }
            
            if (oldNodesToNew.containsKey( node ))
                continue;

            VariableRole varRole = node.getRole( VariableRole.class );
            biouml.model.Node mainNode = (biouml.model.Node)varRole.getDiagramElement();
            biouml.model.Node newMainNode = null;

            if( !mainNode.equals( node ) && !oldNodesToNew.keySet().contains( mainNode ) ) //this is clone, we need to process main node first 
                newMainNode = readSpecies( mainNode, newCompartment, null, processCompartments );

            //now we should check is this variable is replaced through connections
            Diagram subDiagram = CompositeModelUtility.getDiagram( node );
            VariableRole oldVariable = node.getRole( VariableRole.class );
            String uniqueVariableName = getUniqueVariableName( subDiagram, oldVariable.getName() );
            String masterVariableName = getMasterVariable( uniqueVariableName );

            //this variable is connected - we need to read main variable first
            if( !uniqueVariableName.equals( masterVariableName ) )
            {
                biouml.model.Node oldNode = newVarsToOldNodes.get( masterVariableName );
                if( oldNode != null )
                {
                    newMainNode = oldNodesToNew.get( oldNode );

                    biouml.model.Node newMainCompartment = oldNodesToNew.get( oldNode.getCompartment() );
                    if( newMainCompartment == null )
                        newMainCompartment = diagram;

                    //check if this node is not processed already
                    if( newMainNode == null )
                        newMainNode = readSpecies( oldNode, (Compartment)newMainCompartment, null, processCompartments );
                }
            }
            readSpecies( node, newCompartment, newMainNode, processCompartments );
        }
    }
    
    /**
     * Reads nodes from <b>compartment</b> (which belongs to <b>subDiagram</b>, creates their clones and puts them to <b>newCompartment</b> (in plain diagram)
     * @throws Exception
     */
    public void readNodes(Compartment compartment, Compartment newCompartment) throws Exception
    {
        Diagram subDiagram = CompositeModelUtility.getDiagram( compartment );
        for( biouml.model.Node node : compartment.getNodes() )
        {
            readNode( node, subDiagram, compartment, newCompartment );
        }
    }

    public void readNode(biouml.model.Node node, Diagram subDiagram, Compartment compartment, Compartment newCompartment)  throws Exception
    {
        try
        {
            if( node instanceof SubDiagram )
            {
                readNodes( ( (SubDiagram)node ).getDiagram(), this.diagram);
                return;
            }

            if( ( node.getKernel() instanceof Reaction ) || Util.isPlot( node ) || Util.isConstant( node ) || Util.isSwitch( node )
                    || Util.isPort( node ) || node instanceof ModelDefinition || Util.isSourceSink( node )
                    || node.getRole() instanceof biouml.model.dynamics.Function || Util.isVariable( node ) )
                return;

            if( processedEquations.contains( node ) || oldNodesToNew.keySet().contains( node ) )
                return;

            biouml.model.Node newNode;

            String newName = generateUniqueNodeName( node.getName(), newCompartment );
            newNode = node.clone( newCompartment, newName );
            newNode.setLocation( getNewLocation( newNode, subDiagram ) );

            if( newNode instanceof Compartment )
            {
                ( (Compartment)newNode ).clear();
                readNodes( (Compartment)node, (Compartment)newNode );
            }

            Role role = node.getRole();
            if( role instanceof Equation )
            {
                //case when equation which defines some variable replaced by input directed connection formula
                Equation equation = (Equation)role;
                String variableName = equation.getVariable();
                String uniqueName = getUniqueVariableName( subDiagram, variableName );
                if( dConnected.containsKey( uniqueName ) )
                    return;

                processExpressionOwner( (ExpressionOwner)role, subDiagram, (ExpressionOwner)newNode.getRole() );
                String formula = newNode.getRole( Equation.class ).getFormula();
                String mainVariable = getMasterVariable( uniqueName );
                if( conversionFactors.containsKey( mainVariable ) )
                    formula = "(" + formula + ")*" + conversionFactors.get( mainVariable );

                if( equation.getType().equals( Equation.TYPE_RATE ) )
                {
                    SubDiagram sDiagram = SubDiagram.getParentSubDiagram( subDiagram );
                    if( sDiagram != null )
                    {
                        DynamicProperty timeScale = sDiagram.getAttributes().getProperty( Util.TIME_SCALE );
                        if( timeScale != null && !timeScale.getValue().toString().isEmpty() )
                            formula = "(" + formula + ")/" + timeScale.getValue().toString();
                    }
                }
                newNode.getRole( Equation.class ).setFormula( formula );
            }
            else if( role instanceof ExpressionOwner )
            {
                processExpressionOwner((ExpressionOwner)role, subDiagram, (ExpressionOwner)newNode.getRole());
            }
            newCompartment.put(newNode);
            oldNodesToNew.put(node, newNode);
        }
        catch( Exception ex )
        {
            throw new Exception("Error during " + node.getName() + " processing: " + ex.getMessage(), ex);
        }
    }  
    
    private Point getNewLocation(biouml.model.Node node, Diagram diagram)
    {
        if (!layout)
            return node.getLocation();
        if( node.getView() == null )
            CompositeModelUtility.createView(node);
        Point location = new Point(node.getView().getBounds().getLocation());
        if( subDiagramShift != null && subDiagramShift.containsKey(diagram) )
        {
            Point diagramLocation = subDiagramShift.get(diagram);
            location.translate(diagramLocation.x, diagramLocation.y);
        }
        else if( subDiagramShift != null && subDiagramShift.containsKey(node) )
        {
            location = subDiagramShift.get(node);
        }
        return location;
    }

    private void processReactions(Compartment compartment, Diagram subDiagram, Compartment newCompartment) throws Exception
    {
        for( biouml.model.Node node : compartment.getNodes() )
        {
            try
            {
                if( node.getKernel() instanceof ConnectionPort || node instanceof SubDiagram)
                    continue;       
                else if( node instanceof Compartment && !((Compartment)node).isEmpty() )
                {
                    processReactions((Compartment)node, subDiagram, (Compartment)oldNodesToNew.get(node));
                }
                else if( node.getKernel() instanceof Reaction )
                {
                    String variable = node.getRole(Equation.class).getVariable();
                    String uniqueVariableName = getUniqueVariableName(subDiagram, variable);
                    if( !getMasterVariable(uniqueVariableName).equals(uniqueVariableName) )
                        continue;

                    Reaction reaction = (Reaction)node.getKernel();
                    List<SpecieReference> components = new ArrayList<>();

                    HashMap<biouml.model.Node, biouml.model.Node> masterSpeciesToCloned = new HashMap<>();

                    for( SpecieReference reference : reaction.getSpecieReferences() )
                    {
                        String specie = DiagramUtility.toDiagramPath(reference.getSpecie());
                        biouml.model.Node newSpecieNode = oldNodesToNew.get(subDiagram.findNode(specie));
                        biouml.model.Node masterNode = (biouml.model.Node)newSpecieNode.getRole().getDiagramElement();
                        if( !masterNode.equals(newSpecieNode) )
                        {
                            masterSpeciesToCloned.put(masterNode, newSpecieNode);
                            newSpecieNode = masterNode;
                        }
                        SpecieReference newReference = reference.clone(newCompartment, newSpecieNode.getCompleteNameInDiagram());
                        newReference.setSpecie(newSpecieNode.getCompleteNameInDiagram());
                        components.add(newReference);
                    }

                    Reaction newReaction = reaction.clone(null, reaction.getName());
                    newReaction.setSpecieReferences(components.toArray(new SpecieReference[components.size()]));
                    biouml.model.Node newNode = null;
                    String reactionName = generateUniqueVariableName(node.getName(), subDiagram);

                    SemanticController controller = diagram.getType().getSemanticController();
                    DiagramElementGroup reactionElements;
                    if( controller instanceof CreatorElementWithName )
                    {
                        reactionElements = ( (CreatorElementWithName)controller ).createInstance( newCompartment, Reaction.class,
                                reactionName, node.getLocation(), newReaction );
                    }
                    else //TODO: create reactions in the same way for all semantic controllers
                    {
                        ReactionInitialProperties properties = new ReactionInitialProperties();
                        properties.setKineticlaw(reaction.getKineticLaw());
                        properties.setSpecieReferences(components);
                        reactionElements = properties.createElements( newCompartment, node.getLocation(), null );
                    }
                    newNode = (biouml.model.Node)reactionElements.getElement( Util::isReaction );
                    for( biouml.model.Node n : reactionElements.nodesStream() )
                        n.getCompartment().put( n );
                    for( Edge e : reactionElements.edgesStream() )
                        e.getCompartment().put( e );

                    newReaction = (Reaction)newNode.getKernel();
                    newReaction.setFormula(processExpression(reaction.getFormula(), subDiagram, true));

                    //workaround for specie references correcting:
                    //They must contain reference to main variable, which is automatically generated during createReactionNode method call,
                    //However they should be redirected to auxiliary nodes with clone attribute.
                    for( Edge edge : newNode.getEdges() )
                    {
                        if( ! ( edge.getKernel() instanceof SpecieReference ) || ! ( edge.getRole() instanceof Equation ) )
                            continue;
                        Equation edgeEq = edge.getRole(Equation.class);
                        edgeEq.getFormula();
                        //                        edgeEq.setFormula( sp.isProduct() ? reactionRate : "-" + reactionRate );

                        biouml.model.Node input = edge.getInput();
                        biouml.model.Node output = edge.getOutput();

                        if( masterSpeciesToCloned.containsKey(input) )
                        {
                            edge.setInput(masterSpeciesToCloned.get(input));
                            masterSpeciesToCloned.get(input).addEdge(edge);
                        }
                        else if( masterSpeciesToCloned.containsKey(output) )
                        {
                            edge.setOutput(masterSpeciesToCloned.get(output));
                            masterSpeciesToCloned.get(output).addEdge(edge);
                        }
                    }

                    //find old edges and copy attributes
                    for( Edge e : node.getEdges() )
                    {
                        biouml.model.Node specieNode = node.equals(e.getInput()) ? e.getOutput() : e.getInput();
                        biouml.model.Node newSpecieNode = oldNodesToNew.get(specieNode);

                        //source sink and edges for new reaction are already created by semantic controller. Probabaly we should copy layout and styles but this is not urgent
                        if( Util.isSourceSink(specieNode) )
                            continue;

                        for( Edge newEdge : newSpecieNode.getEdges() )
                        {
                            if( newSpecieNode.equals(newEdge.getInput()) && newNode.equals(newEdge.getOutput())
                                    || newSpecieNode.equals(newEdge.getOutput()) && newNode.equals(newEdge.getInput()) )
                            {
                                CompositeModelUtility.copyAttributes(e, newEdge);
                                newEdge.setTitle(e.getTitle());
                                newEdge.setComment(e.getComment());
                                break;
                            }
                        }
                    }

                    newReaction.setDatabaseReferences(reaction.getDatabaseReferences());
                    newReaction.setLiteratureReferences(reaction.getLiteratureReferences());
                    newNode.setLocation(getNewLocation(newNode, subDiagram));
                    CompositeModelUtility.copyAttributes(node, newNode);

                    //                    processExpressionOwner( (Equation)newNode.getRole(), subDiagram, (Equation)newNode.getRole() );
                    SubDiagram sDiagram =  SubDiagram.getParentSubDiagram(Diagram.getDiagram(compartment));
                    if( sDiagram != null )
                    {
                        DynamicProperty timeFactor = sDiagram.getAttributes().getProperty(Util.TIME_SCALE);
                        DynamicProperty extentFactor = sDiagram.getAttributes().getProperty(Util.EXTENT_FACTOR);
                        String factor = "1";

                        if( extentFactor != null && !extentFactor.getValue().toString().isEmpty() )
                            factor = extentFactor.getValue().toString();

                        if( timeFactor != null && !timeFactor.getValue().toString().isEmpty() )
                            factor += "/" + timeFactor.getValue().toString();

                        if( !factor.equals("1") )
                            newReaction.setFormula(newReaction.getFormula() + "*(" + factor + ")");
                    }
                    oldNodesToNew.put(node, newNode);
                    //                    reactions.add( newReaction );
                }
            }
            catch( Exception ex )
            {
                throw new Exception("Error during reaction " + node.getName() + " processing: " + ex.getMessage());
            }
        }
    }

    /**
     * Creates auxiliary equation for directed connections
     * @param node
     * @param formula
     * @throws Exception
     */
    private void createConnectionEquation(biouml.model.Node node, AstStart formula) throws Exception
    {
        String name = node.getRole(VariableRole.class).getName();
        Equation eq = new Equation(null, Equation.TYPE_SCALAR, name, new LinearFormatter().format(formula)[1]);
        DiagramElement de = diagram.getType().getSemanticController().createInstance( diagram, Equation.class, new Point( 0, 0 ), eq )
                .getElement();
        diagram.put(de); // equations should be on the top level
    }

    /**
     * Method extends
     * @param function
     * @return
     */
    private void extendFunction(Node function)
    {
        int n = function.jjtGetNumChildren();
        for( int i = 0; i < n; i++ )
        {
            Node child = function.jjtGetChild(i);
            if( child instanceof AstVarNode )
            {
                String key = ( (AstVarNode)child ).getName();

                //if this variable has incident directed connections - we need to extend it and replace by expression
                if( dConnected.containsKey(key) )
                {
                    Node inputConnection = dConnected.get(key);
                    extendFunction(inputConnection);

                    if( inputConnection.jjtGetNumChildren() == 1 )
                        inputConnection = inputConnection.jjtGetChild(0);
                    function.jjtReplaceChild(child, inputConnection);
                }
            }
        }
    }

    private Node parse(String str)
    {
        Parser parser = new Parser();
        parser.parse(str);
        return parser.getStartNode();
    }

    private boolean isConnected(String varName)
    {
        return StreamEx.ofValues( uConnected ).anyMatch( set -> set.contains( varName ) );
    }
    
    //Access methods for main variable <----> auxiliary variables mapping
    private String getMasterVariable(String varName)
    {
        return StreamEx.ofKeys(uConnected, set -> set.contains(varName)).findAny().orElse(varName);
    }

    private Set<String> getAuxVariables(String mainVariableName)
    {
        return uConnected.getOrDefault(mainVariableName, Collections.emptySet());
    }

    /**
     * According to SBML specification
     * New names of variables should be taken from top level elements if said elements participate in replacement chain
     * Variable role still should be taken from "main" variable
     */
    private void transformUndirectedConnections()
    {
        Map<String, String> toReplace = new HashMap<>();
        for (Entry<String, Set<String>> e: uConnected.entrySet())
        {
            String main = e.getKey();
            String newVarName = null;
            for( String varName : e.getValue() )
            {
//                Variable oldVar = newVarsToOld.get( varName );
//                Diagram parent = getParentDiagram( oldVar );
                if( getParentDiagram( newVarsToOld.get( varName ) ).equals( compositeDiagram ) )
                {
                    toReplace.put( main, varName );
                    newVarName = varName;
                }
            }

            if( newVarName != null )
                uConnectedInfo.put( newVarName, newVarsToOld.get( main ) );
        }

        for( Entry<String, String> e : toReplace.entrySet() )
        {
            Set<String> value = uConnected.get( e.getKey() );
            uConnected.remove( e.getKey() );
            uConnected.put( e.getValue(), value );
        }
    }
    
    /**
     * Fix situation when species is connected to parameter and parameter is main
     */
    private void fixMixedConnections()
    {
        Map<String, String> toReplace = new HashMap<>();
        for (Entry<String, Set<String>> e: uConnected.entrySet())
        {
            String main = e.getKey();
            boolean mainIsSpecie = newVarsToOld.get( main ) instanceof VariableRole;
            if (mainIsSpecie)
                continue;
            String newVarName = null;
            for( String varName : e.getValue() )
            {
                boolean auxIsSpecie = newVarsToOld.get( varName ) instanceof VariableRole;
                if(auxIsSpecie )
                {
                    toReplace.put( main, varName );
                    newVarName = varName;
                    break;
                }
            }

            if( newVarName != null )
                uConnectedInfo.put( main, newVarsToOld.get( newVarName ) );
        }

        for( Entry<String, String> e : toReplace.entrySet() )
        {
            Set<String> value = uConnected.get( e.getKey() );
            uConnected.remove( e.getKey() );
            uConnected.put( e.getValue(), value );
        }
    }
    
    private void adjustDirectedConnections()
    {
        for (Entry<String, AstStart> e: dConnected.entrySet())
        {
            adjustDirectedConnection(e.getValue());
        }
    }

    private void adjustDirectedConnection(Node node)
    {
        for( int i = 0; i < node.jjtGetNumChildren(); i++ )
        {
            Node child = node.jjtGetChild( i );

            if( child instanceof AstVarNode )
            {
                String variableName = ( (AstVarNode)child ).getName();
                String[] parts = variableName.split( "\\." );
                String[] newParts = new String[parts.length];
                boolean replaced = false;
                for (int j=0; j< parts.length; j++)
                {
                    String substitution =  this.getMasterVariable( parts[j] );//  substitutionMap.get( new Pair( parts[i], key.getSecond() ) );
                    if( !parts[j].equals( substitution )  )
                    {
                        newParts[j] = substitution;
                        replaced = true;
                    }
                    else
                        newParts[j] = parts[j];
                }
                
                if (replaced)
                {
                    String newVal = StreamEx.of( newParts ).joining( "." );
                    AstStart newChild = (AstStart)parse( newVal );
                    node.jjtReplaceChild( node.jjtGetChild( i ), newChild.jjtGetChild( 0 ) );
                }                                
            }
            else
            {
                adjustDirectedConnection( child );
            }
        }
    }
    
    private Diagram getParentDiagram(Variable var)
    {
        if( var instanceof VariableRole )
            return Diagram.getDiagram( ( (VariableRole)var ).getDiagramElement() );
        else
            return ( (EModel)var.getParent() ).getDiagramElement();
    }
    
    private void registerUndirectedConnection(String mainVariableName, String auxVariableName, Double initialValue)
    {
        HashSet<String> newSet = new HashSet<>();
        newSet.add(mainVariableName);
        newSet.add(auxVariableName);

        Set<String> keySet = new HashSet<>();
        keySet.addAll(uConnected.keySet());
        for( String mainVar : keySet )
        {
            Set<String> set = uConnected.get(mainVar);
            //means one of those variables have another undirected connection
            if( set.contains(mainVariableName) || set.contains(auxVariableName) )
            {
                newSet.addAll(set);
                uConnected.remove(mainVar);

                if( !mainVar.equals(auxVariableName) )
                    mainVariableName = mainVar;
                break;
            }
        }
        uConnected.put(mainVariableName, newSet);

        String varInfo = null;

        if (variablesInfo.containsKey(mainVariableName))
            varInfo = variablesInfo.get(mainVariableName);

        if( nameStyle == SBML_STYLE )
        {
            if( !mainVariableName.contains("__") )
                varInfo = mainVariableName;
            else if( varInfo == null || varInfo.contains("__") )
                varInfo = auxVariableName;
        }
        else
            varInfo = mainVariableName;

        variablesInfo.put(mainVariableName, varInfo);
    }

    /**
     * Method generates new unique name in plain diagram for variable
     * Name is based on old variable name, if his name already exist we add "_i"
     * @param oldName
     * @return
     */
    public static String generateUniqueNodeName(String oldName, Compartment newCompartment)
    {
        String newName = oldName;
        int counter = 1;
        while( newCompartment.contains(newName) )
            newName = oldName + "_" + counter++;
        return newName;
    }

    /**
     * Method generates new unique name in plain diagram for variable
     * Name is based on old variable name, if this name already exist we add "(i)"
     * @param oldName
     * @return
     */
    private @Nonnull String generateUniqueVariableName(@Nonnull String oldName, Diagram diagram)
    {
        if( oldName.equals("time") )
            return "time";

        //special case of reaction variable
        if( nameStyle == SBML_STYLE )
        {
            boolean isRate = false;
            if( oldName.startsWith("$$rate_") )
            {
                isRate = true;
                oldName = oldName.substring(7);
            }
            SubDiagram subDiagram =  SubDiagram.getParentSubDiagram(diagram);
            if( subDiagram == null )
                return ( isRate ) ? "$$rate_" + oldName : oldName;

            String path = "";
            String baseId = oldName;
            if( oldName.contains(".") )
            {
                path = oldName.substring(0, oldName.lastIndexOf("."));
                baseId = oldName.substring(oldName.lastIndexOf(".") + 1);
                return path + "." + subDiagram.getName() + "__" + baseId;
            }

            if( isRate )
                return "$$rate_" + subDiagram.getName() + "__" + baseId;
            else if( baseId.startsWith("$") )
                return "$" + subDiagram.getName() + "__" + baseId.substring(1);
            return subDiagram.getName().replace(" ", "_") + "__" + baseId;
        }

        if( oldName.startsWith("$$") )
            return oldName;

        String newName = oldName.replace("\"", "");
        if( oldName.startsWith("$") )
            newName = newName.substring(1);

        String newBaseID = newName.substring(newName.lastIndexOf(".") + 1);

        String newID = newBaseID;
        int counter = 1;

        while( true )
        {
            if( counter != 1 )
                newID = newBaseID + counter;

            if( variableIDS.contains(newID) )
                counter++;
            else
            {
                variableIDS.add(newID);

                if( counter != 1 )
                    newName = newName + counter;

                if( !VariableRole.IDENTIFIER.matcher(newName).matches() )
                    newName = "\"" + newName + "\"";

                if( oldName.startsWith("$") )
                    newName = VariableRole.PREFIX + newName;

                return newName;
            }
        }
    }

    private void processExpressionOwner(ExpressionOwner oldOwner, Diagram subDiagram, ExpressionOwner newOwner)
    {
        if( oldOwner instanceof Equation && ! ( (Equation)oldOwner ).getType().equals(Equation.TYPE_ALGEBRAIC) )
        {
            String[] expressions = oldOwner.getExpressions();
            String[] newExpressions = new String[2];
            newExpressions[0] = processExpression(expressions[0], subDiagram, false);
            newExpressions[1] = processExpression(expressions[1], subDiagram, true);
            newOwner.setExpressions(newExpressions);
        }
        else if( !(oldOwner instanceof biouml.model.dynamics.Function ))
        {
            newOwner.setExpressions(
                    StreamEx.of(oldOwner.getExpressions()).map(expr -> processExpression(expr, subDiagram, true)).toArray(String[]::new));
        }

        //event delays should be additionally multiplied by time conversion factor
        if( newOwner instanceof Event )
        {
            String delay = ( (Event)newOwner ).getDelay();
            if( delay != null && !delay.isEmpty() && !delay.equals("0") )
            {
                SubDiagram sDiagram = SubDiagram.getParentSubDiagram(subDiagram);
                if( sDiagram != null )
                {
                    DynamicProperty dp = sDiagram.getAttributes().getProperty(Util.TIME_SCALE);
                    if( dp != null && !dp.getValue().toString().isEmpty() )
                        ( (Event)newOwner ).setDelay("(" + delay + ")*" + dp.getValue().toString());
                }
            }
        }
    }

    private String processExpression(String oldExpression, Diagram subDiagram, boolean rightSide)
    {
        if( oldExpression == null || oldExpression.isEmpty() )
            return oldExpression;

        Node expression = parse(oldExpression).jjtGetChild(0);
        Node newExpression = processExpression(expression, subDiagram, rightSide);
        AstStart start = new AstStart(0);
        start.jjtAddChild(newExpression, 0);
        return new LinearFormatter().format(start)[1];
    }

    
    private String processFunction(String oldExpression, Diagram subDiagram)
    {
        if( oldExpression == null || oldExpression.isEmpty() )
            return oldExpression;

        Node expression = parse(oldExpression).jjtGetChild(0);
        Node newExpression = processFunction(expression, subDiagram);
        AstStart start = new AstStart(0);
        start.jjtAddChild(newExpression, 0);
        return new LinearFormatter().format(start)[1];
    }
    
    private Node processFunction(Node expression, Diagram subDiagram)
    {
        Node newExpression = Utils.cloneAST(expression);
        if( newExpression instanceof AstFunNode )
        {
            String funName = ( (AstFunNode)newExpression ).getFunction().getName();
            SubDiagram sDiagram = SubDiagram.getParentSubDiagram(subDiagram);
            String key = sDiagram != null ? sDiagram.getCompleteNameInDiagram() : "";
            String newName = this.getNewFunctionName(funName, key);
            if( newName != null )
            {
                Function oldFunction = ( (AstFunNode)newExpression ).getFunction();
                ( (AstFunNode)newExpression ).setFunction(new UndeclaredFunction(newName, oldFunction.getPriority()));
            }
        }
        
        for( int i = 0; i < newExpression.jjtGetNumChildren(); i++ )
        {
            Node child = newExpression.jjtGetChild(i);
            newExpression.jjtReplaceChild(child, processFunction(child, subDiagram));
        }
        return newExpression;
    }
    
    /**
     * Method replace all variables with functions or another variables according to directed and undirected connections respectively
     * i.e.
     * <ul>
     * <li> if we have directed connection (function F --> variable V) we replace all inclusions of variable V with F</li>
     * <li> if we have undirected connection (variable V1 <---> variable V2 ) we replace all inclusions of variable V2 with variable V1</li>
     * </ul>
     * @param function
     * @param subDiagram
     */
    private Node processExpression(Node expression, Diagram subDiagram, boolean rightSide)
    {
        Node newExpression = Utils.cloneAST(expression);

        if( newExpression instanceof AstVarNode )
            return processVarNode((AstVarNode)expression, subDiagram, rightSide);

        if( newExpression instanceof AstFunNode )
        {
            String funName = ( (AstFunNode)newExpression ).getFunction().getName();
            if( funName.equals("=") && newExpression.jjtGetChild(0) instanceof AstVarNode )
            {
                Node varNode = newExpression.jjtGetChild(0);
                String varName = ( (AstVarNode)varNode ).getName();
                String mainVariable = getMasterVariable(getUniqueVariableName(subDiagram, varName));
                String conversionFactor = conversionFactors.get(mainVariable);
                Node newVariable = processVarNode((AstVarNode)newExpression.jjtGetChild(0), subDiagram, false);
                Node newFormula = processExpression(newExpression.jjtGetChild(1), subDiagram, rightSide);
                if( conversionFactor != null && !conversionFactor.isEmpty() )
                {
                    Node factor = parse(conversionFactor).jjtGetChild(0);
                    newFormula = Utils.applyFunction(newFormula, factor,
                            new PredefinedFunction(DefaultParserContext.TIMES, Function.TIMES_PRIORITY, -1));
                }
                newExpression.jjtReplaceChild(newExpression.jjtGetChild(1), newFormula);
                newExpression.jjtReplaceChild(newExpression.jjtGetChild(0), newVariable);
                return newExpression;
            }
            else if( funName.equals("delay") )
            {
                for( int i = 0; i < newExpression.jjtGetNumChildren(); i++ )
                {
                    Node child = newExpression.jjtGetChild(i);
                    newExpression.jjtReplaceChild(child, processExpression(child, subDiagram, rightSide));
                }

                SubDiagram sDiagram = SubDiagram.getParentSubDiagram(subDiagram);
                if( sDiagram != null )
                {
                    DynamicProperty timeFactor = sDiagram.getAttributes().getProperty(Util.TIME_SCALE);

                    if( timeFactor != null && !timeFactor.getValue().toString().isEmpty() )
                    {
                        Node formula = Utils.cloneAST(newExpression.jjtGetChild(1));
                        Node factor = parse(timeFactor.getValue().toString()).jjtGetChild(0);
                        formula = Utils.applyFunction(formula, factor,
                                new PredefinedFunction(DefaultParserContext.TIMES, Function.TIMES_PRIORITY, -1));
                        newExpression.jjtReplaceChild(newExpression.jjtGetChild(1), formula);
                    }
                }
                return newExpression;
            }
            else
            {
                SubDiagram sDiagram = SubDiagram.getParentSubDiagram(subDiagram);
                String key = sDiagram != null ? sDiagram.getCompleteNameInDiagram() : "";
                String newName = this.getNewFunctionName(funName, key);
                if( newName != null )
                {
                    Function oldFunction = ( (AstFunNode)newExpression ).getFunction();
                    ( (AstFunNode)newExpression ).setFunction(new UndeclaredFunction(newName, oldFunction.getPriority()));
                }
            }
        }

        for( int i = 0; i < newExpression.jjtGetNumChildren(); i++ )
        {
            Node child = newExpression.jjtGetChild(i);
            newExpression.jjtReplaceChild(child, processExpression(child, subDiagram, rightSide));
        }
        return newExpression;
    }

    protected Node processVarNode(AstVarNode node, Diagram diagram, boolean rightSide)
    {
        Node result = node.cloneAST();
        String varName = diagram.getRole(EModel.class).getVariable(node.getName()).getName(); //normalize name
        String uniqueName = getUniqueVariableName(diagram, varName);

        if( uniqueName == null )
            return null; //something goes wrong

        result = parse(uniqueName).jjtGetChild(0);

        SubDiagram subDiagram = SubDiagram.getParentSubDiagram(diagram);

        if( varName.startsWith("$$") )
        {
            if( !rightSide || subDiagram == null )
                return result;

            DynamicProperty timeFactor = subDiagram.getAttributes().getProperty(Util.TIME_SCALE);
            DynamicProperty extentFactor = subDiagram.getAttributes().getProperty(Util.EXTENT_FACTOR);
            String factor = "1";

            if( timeFactor != null && !timeFactor.getValue().toString().isEmpty() )
                factor = timeFactor.getValue().toString();

            if( extentFactor != null && !extentFactor.getValue().toString().isEmpty() )
                factor += "/" + extentFactor.getValue().toString();

            if( factor != "1" )
            {
                Node nodeFactor = parse(factor).jjtGetChild(0);
                result = Utils.applyFunction(result, nodeFactor,
                        new PredefinedFunction(DefaultParserContext.TIMES, Function.TIMES_PRIORITY, -1));
            }
        }

        //apply subdiagram time factor
        if( varName.equals("time") && subDiagram != null )
        {
            DynamicProperty dp = subDiagram.getAttributes().getProperty(Util.TIME_SCALE);
            if( dp != null && !dp.getValue().toString().isEmpty() )
            {
                Node nodeFactor = parse(dp.getValue().toString()).jjtGetChild(0);
                return Utils.applyFunction(result, nodeFactor,
                        new PredefinedFunction(DefaultParserContext.DIVIDE, Function.TIMES_PRIORITY, -1));
            }
        }

        //check directed connection - we replace variable by expression
        if( dConnected.containsKey(uniqueName) )
            return dConnected.get(uniqueName).jjtGetChild(0);

        //check undirected connection - we replace variable by another variable
        String mainVariable = getMasterVariable(uniqueName);
        if( mainVariable != null && ! ( mainVariable.equals(uniqueName) ) )
        {
            result = parse(mainVariable).jjtGetChild(0);
            if( rightSide )
            {
                String convFactor = conversionFactors.get(mainVariable);
                if( convFactor != null && !convFactor.isEmpty() )
                {
                    Node nodeFactor = parse(convFactor).jjtGetChild(0);
                    result = Utils.applyFunction(result, nodeFactor,
                            new PredefinedFunction(DefaultParserContext.DIVIDE, Function.TIMES_PRIORITY, -1));
                }
            }
        }
        return result;
    }

    /**
     * Method applies undirected connections to directed:<br>
     * it processes situation when one variable have undirected connection and outgoing directed connection:<br>
     * A <--> B --> C<br>
     * Where A is main variable, so B will be replaced by A when undirected connection will be applied<br>
     * Connections will be transformed like this:<br>
     * B <--> A --> C
     * Note: situation A <--> B <-- C should be excluded on the level of semantic controller as well as A --> B <-- C
     */
    private void processDirectedConnections()
    {
        for( AstStart formula : dConnected.values() )
            applyUndirectedConnection(formula);
    }

    private void applyUndirectedConnection(Node expression)
    {
        for( int i = 0; i < expression.jjtGetNumChildren(); i++ )
        {
            Node child = expression.jjtGetChild(i);

            if( child instanceof AstVarNode )
            {
                String variableName = ( (AstVarNode)child ).getName();
                String mainVar = getMasterVariable(variableName);
                AstStart newChild = (AstStart)parse(mainVar);
                expression.jjtReplaceChild(expression.jjtGetChild(i), newChild.jjtGetChild(0));
            }
            else
            {
                applyUndirectedConnection(child);
            }
        }
    }

    
    public void adjustUniqueNames()
    {
        Map<Pair<String, Diagram>, String> replacements = new HashMap<>();
        //second iteration
        for (Entry<Pair<String, Diagram>, String> e: oldVariablesToNew.entrySet())
        {
            
            Pair<String, Diagram> key = e.getKey();
            String value = e.getValue();
            String[] parts = value.split( "\\." );
            String[] newParts = new String[parts.length];
            
            if (isConnected(value))
                continue;
            
            newParts[parts.length-1] = parts[parts.length-1];
            boolean replaced = false;
            for (int i=0; i< parts.length - 1; i++)
            {
                String substitution =  this.getMasterVariable( parts[i] );//  substitutionMap.get( new Pair( parts[i], key.getSecond() ) );
                if( !parts[i].equals( substitution )  )
                {
                    newParts[i] = substitution;
                    replaced = true;
                }
                else
                    newParts[i] = parts[i];
            }
            if (replaced)
            {
                String newValue = StreamEx.of( newParts ).joining( "." );
                replacements.put( key, newValue );
            }
        }

        oldVariablesToNew.putAll( replacements );
    }
    
    /**
     * Generates mapping between path of variable in the mopdular model and its name in the final flat result
     */
    public Map<String, String> getVarPathMapping(String parentSubDiagram)
    {
//        adjustSubstitutions();
        //first step: we create mapping for current preprocessing step: between paths with 1-level deep and variables in flat model
        //E.g.
        //Submodel1/param -> param_flat
        //Submodel1/$species -> $species_flat
        //$top_species -> $top_species_flat
        Map<String, String> newPathMapping = new HashMap<>();
        EntryStream.of(substitutionMap).forEach(e -> {
            Pair<String, String> key = e.getKey();

            String newName = e.getValue();
            String subDiagram = key.getSecond();
            String path = subDiagram.isEmpty() ? key.getFirst() : subDiagram.concat(VAR_PATH_DELIMITER).concat(key.getFirst());
            newPathMapping.put(path, newName);
        });

        //second step we extend previously created mapping for the case when these variables initially were deeper than 1 level and were preprocessed by processCompositeSubDiagram method
        //E.g. there was mapping "Submodel2/param -> param" where Submodel2 was inside Submodel1 in that case we combine it with nePathMapping created on step 1:
        //Submodel1/Submodel2/param -> param_flat
        //Submodel1/Submodel2/$species -> $species_flat
        Map<String, String> extendedPathMapping = new HashMap<>();
        for( Entry<String, String> e : varPathMapping.entrySet() )
        {
            String value = e.getValue();
            extendedPathMapping.put(e.getKey(), newPathMapping.get(value)); //is it possible that new mapping does not have this key?
        }

        //Third step: all mappings which can not be extended by step 2 (they are one or zero level deep at this point) are simply added to resultant mapping:
        //$top_species -> $top_species_flat
        //Submodel0/species -> $species_flat
        for( Entry<String, String> e : newPathMapping.entrySet() )
        {
            if( !varPathMapping.containsValue(e.getKey()) )
            {
                extendedPathMapping.put(e.getKey(), e.getValue());
            }

        }

        //Fourth step: if diagram we processing right now is inside subdiagram itself - then we should add this subdiagram name to both sides of created 
        //mappings to facilitate mapping extension on the next processing step:
        //CurSubmodel/Submodel1/Submodel2/param -> CurSubmodel/param_flat
        //CurSubmodel/Submodel1/Submodel2/$species -> CurSubmodel/$species_flat
        if( !parentSubDiagram.isEmpty() )
        {
            Map<String, String> result = new HashMap<>();
            for( Entry<String, String> e : extendedPathMapping.entrySet() )
            {
                String newName = parentSubDiagram.concat(VAR_PATH_DELIMITER).concat(e.getValue());
                String oldPath = e.getKey();
                String path = parentSubDiagram.concat(VAR_PATH_DELIMITER).concat(oldPath);
                result.put(path, newName);
            }
            return result;
        }

        return extendedPathMapping;
    }
}