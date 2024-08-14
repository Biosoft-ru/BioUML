package biouml.model.dynamics.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.exception.InternalException;
import ru.biosoft.math.model.AstFunNode;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.AstVarNode;
import ru.biosoft.math.model.LinearFormatter;
import ru.biosoft.math.model.Node;
import ru.biosoft.math.model.Utils;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Role;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.ExpressionOwner;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.model.dynamics.plot.Curve;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.type.Base;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub.ConnectionPort;
import biouml.standard.type.Unit;
import one.util.streamex.StreamEx;

/**
 * Utility class, providing complex functionality for EModel
 */
public class EModelHelper
{
    static Logger log = Logger.getLogger( EModelHelper.class.getName() );

    private final EModel model;

    public EModelHelper(EModel model)
    {
        this.model = model;
    }

    /**
     * Returns all variables used in given parsed tree
     */
    public static List<String> getUsedVariables(AstStart start)
    {
        return Utils.getVariables( start );
    }

    /**
     * Reorders assignment rules according to variable dependencies.
     */
    public List<Equation> reorderAssignmentRules() throws InternalException
    {
        Map<String, EquationASTPair> varASTMap = model.getDiagramElement().recursiveStream()
            .map( DiagramElement::getRole ).select( Equation.class )
            .mapToEntry( equation -> new EquationASTPair( equation, equation.getMath() ) )
            .filterValues( pair -> pair.ast != null )
            .mapKeys( Equation::getVariable )
            .toMap( (p1, p2) -> {
                throw new InternalException( "Duplicate assignment rules for variable " + p1.equation.getVariable() );
            });
        return reorderEquations( varASTMap );
    }

    public void substituteAssignmentRules(Map<String, ru.biosoft.math.model.Node> macroMap, AstStart start) throws Exception
    {
        substituteSubtree( macroMap, start );
    }

    protected void substituteSubtree(Map<String, ru.biosoft.math.model.Node> map, ru.biosoft.math.model.Node node)
    {
        if( node == null )
            return;

        if( node instanceof AstVarNode )
        {
            ru.biosoft.math.model.Node substNode = map.get( ( (AstVarNode)node ).getName() );

            if( substNode != null && substNode.jjtGetNumChildren() > 0 )
            {
                ru.biosoft.math.model.Node parent = node.jjtGetParent();
                parent.jjtReplaceChild( node, substNode.jjtGetChild( 0 ) );
                substituteSubtree( map, substNode.jjtGetChild( 0 ) );
            }
        }
        else
        {
            Utils.children( node ).forEach( child -> substituteSubtree( map, child ) );
        }
    }

    protected static List<Equation> reorderEquations(Map<String, EquationASTPair> map)
    {
        boolean cycleFound = false;
        List<Equation> reorderedEquationList = new ArrayList<>();
        while( !cycleFound )
        {
            Set<Map.Entry<String, EquationASTPair>> entrySet = map.entrySet();
            if( entrySet.isEmpty() )
                break;

            boolean independentEquationFound = false;
            for( Map.Entry<String, EquationASTPair> entry : entrySet )
            {
                if( !dependsFromOthers( entry.getValue().ast, map ) )
                {
                    independentEquationFound = true;
                    reorderedEquationList.add( entry.getValue().equation );
                    map.remove( entry.getKey() );
                    break;
                }
            }
            cycleFound = !independentEquationFound;
        }

        if( cycleFound )
        {
            log.log(Level.SEVERE,  "Cyclic dependences between assignment rules found." );
            return null;
        }

        return reorderedEquationList;
    }

    public Variable[] getInvolvedVariables(String equationType)
    {
        return model.getEquations()
            .filter( equation -> equationType == null || equationType.equals( equation.getType() ) )
            .flatMap( equation -> Utils.variables( equation.getMath() ).append( equation.getVariable() ) )
            .distinct().nonNull()
            .map( name -> new Variable(name, model, model.getVariables()) )
            .toArray( Variable[]::new );
    }

    public void renameUnit(String oldName, String newName)
    {
        if( model.getUnits().containsKey( newName ) )
            return;

        if( model.getUnits().containsKey(oldName) )
        {
            Unit oldUnit = model.getUnits().get(oldName); 
            model.addUnit( oldUnit.clone(null, newName));
            model.removeUnit(oldName);
        }
        else
        {
            model.addUnit( new Unit( null, newName ) );
        }

        for( Variable var : model.getVariables() )
        {
            if( var.getUnits().equals( oldName ) )
                var.setUnits(newName);
        }
    }
    
    public StreamEx<ExpressionOwner> getExpressionOwners()
    {
        return model.getDiagramElement().recursiveStream().map(de->de.getRole()).select(ExpressionOwner.class);
    }

    public void replaceFunction(String oldName, String newName)
    {
        ru.biosoft.math.model.Function replacement = model.getFunction(newName);

        for( ExpressionOwner owner : getExpressionOwners() )
        {
            List<AstStart> astStart = model.readMath(owner.getExpressions(), (Role)owner);
            for( AstStart start : astStart )
                renameFunctionInAST(start, oldName, replacement);
            
            List<String> formatted = new LinearFormatter().format(astStart);
            owner.setExpressions(formatted.toArray(new String[formatted.size()]));
        }
    }

    public void renameVariableRole(String oldName, String newName) throws Exception
    {
        Variable var = model.getVariable( oldName );
        if( ! ( var instanceof VariableRole ) )
            throw new IllegalArgumentException(
                    "Entity with name " + oldName + " was not found in model " + model.getParent().getName() + "!" );

        if( model.containsVariable( newName ) )
            throw new IllegalArgumentException(
                    "Variable with name " + newName + " already exists in model " + model.getParent().getName() + "!" );

        VariableRole varRole = (VariableRole)var;
        biouml.model.Node de = (biouml.model.Node)varRole.getDiagramElement();

        Base base = de.getKernel();
        Base newKernel = null;
        try
        {
            if( base instanceof biouml.standard.type.Compartment )
                newKernel = new biouml.standard.type.Compartment( base.getOrigin(), newName );
            else
                newKernel = base.getClass().getConstructor( ru.biosoft.access.core.DataCollection.class, String.class, String.class )
                        .newInstance( base.getOrigin(),    newName, base.getType() );
        }
        catch( Exception ex )
        {

        }

        biouml.model.Node newNode = (biouml.model.Node)de.clone( de.getCompartment(), newName, newKernel );
        newNode.setTitle( de.getTitle().equals( de.getName() ) ? newName : de.getTitle() );
        VariableRole newRole = (VariableRole)de.getRole( VariableRole.class ).clone( newNode, VariableRole.createName(newNode, false)); 
        newNode.setRole( newRole  );
        String newVarName = newNode.getRole( VariableRole.class ).getName();
        de.getCompartment().put( newNode );

        renameVariable( model.getDiagramElement(), oldName, newVarName );
        for( Edge e : de.getEdges() )
        {
            if( e.getInput().equals( de ) )
                e.setInput( newNode );
            else
                e.setOutput( newNode );

            newNode.addEdge( e );
            de.removeEdge( e );
            if( e.getKernel() instanceof SpecieReference )
            {
                ( (SpecieReference)e.getKernel() ).setSpecie( newNode.getCompleteNameInDiagram() );
                Equation eq = (Equation)e.getRole();              
                renameVariable( eq, oldName, newVarName );
            }
        }        

        for( biouml.model.Node node : newNode.recursiveStream().without( newNode ).select( biouml.model.Node.class )
                .filter( n -> n.getRole() instanceof VariableRole ) )
        {
            VariableRole innerRole = node.getRole( VariableRole.class );
            node.setRole( innerRole.clone( node, VariableRole.createName( node, false ) ) );
            EModelHelper helper = new EModelHelper( model );
            model.getVariableRoles().put( (VariableRole)node.getRole() );
            helper.renameVariable( innerRole.getName(), node.getRole( VariableRole.class ).getName(), true ); //TODO: rename variables in batch (or do not rename at all)

        }
        model.getDiagramElement().getType().getSemanticController().remove( de );
    }
    
    public void renameVariable(String oldName, String newName)
    {
        renameVariable(oldName, newName , false);
    }
    
    /**
     * Method finds all inclusions of variable with name <b>oldName</b> in EModel and replaces them with <b>newName</b>.
     * replaceByCoincide defines what to do with variable with name <b>newName</b> if such variable exist in the model.<br>
     * 
     * As a result both variables <b>oldName</b> and <b>newName</b> will be merged into one.<br>
     * Argument <b>replaceByCoincide</b> defines properties of that new variable.<br>
     * if ( <b>replaceByCoincide</b> == true) then new variable will inherit properties from <b>newName</b><br>
     * else from <b>oldName</b>
     */
    public void renameVariable(String oldName, String newName, boolean replaceByCoincide)
    {
        if( oldName.equals( newName ) )
            return;
        try
        {
            Variable oldVar = model.getVariable( oldName );

            Variable coincideVariable = model.getVariable( newName );
            if( !replaceByCoincide )
            {
                if( ! ( coincideVariable instanceof VariableRole ) )
                {
                    if( coincideVariable != null )
                    {
                        coincideVariable.setParent( null );
                        model.getVariables().remove( newName );
                    }
                    Variable newVar = oldVar.clone( newName );
                    model.put( newVar );
                    newVar.setParent( model );
                    newVar.setTitle( oldVar.getTitle().equals( oldVar.getName() ) ? newName : oldVar.getTitle() ); //keep title if differs form name
                }
            }
            renameVariable( model.getDiagramElement(), oldName, newName );
          
            if( oldVar != null && ! ( oldVar instanceof VariableRole ) )
            {
                oldVar.setParent( null );
                model.getVariables().remove( oldName );
            }
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE,  "Problems occured during variable " + oldName + " renaming to " + newName
                    + " old variable was not deleted from the model because of " + ex );
        }
    }
    private void renameVariable(Compartment compartment, String oldName, String newName)
    {
        if( oldName.equals( newName ) )
            return;

        for( DiagramElement de : compartment )
        {
            //boolean notificationEnabled = de.isNotificationEnabled();
            //de.setNotificationEnabled( false );
            if( de instanceof Compartment )
            {
                renameVariable( (Compartment)de, oldName, newName );
            }
            if( de.getKernel() != null )
            {
                renameVariable( de, de.getKernel(), oldName, newName );
            }
            if( de.getRole() != null )
            {
                renameVariable( de.getRole(), oldName, newName );
            }
            //de.setNotificationEnabled( notificationEnabled );
        }
        
        PlotsInfo plots = DiagramUtility.getPlotsInfo( Diagram.getDiagram( compartment ) );
        if( plots != null )
        {
            for( PlotInfo plot : plots.getActivePlots() )
            {
                for( Curve c : plot.getYVariables() )
                {
                    String name = c.getName();
                    if( name.equals( oldName ) )
                        c.setName( newName );
                }
                if( plot.getXVariable().getName().equals( oldName ) )
                    plot.getXVariable().setName( newName );
            }
        }
    }

    private void renameVariable(DiagramElement de, Base kernel, String oldName, String newName)
    {
        if( kernel instanceof ConnectionPort )
        {
            String ownerNodeName = de.getAttributes().getValue( ConnectionPort.VARIABLE_NAME_ATTR ).toString();
            if( ownerNodeName.equals( oldName ) )
                de.getAttributes().setValue( ConnectionPort.VARIABLE_NAME_ATTR, newName );
        }
    }

    public void renameVariable(Role role, String oldName, String newName)
    {
        if( role instanceof ExpressionOwner )
        {
            ExpressionOwner expressionOwner = (ExpressionOwner)role;
            List<AstStart> astStart = model.readMath( expressionOwner.getExpressions(), role );
            for( AstStart start : astStart )
                renameVariableInAST( start, oldName, newName );
            List<String> formatted = new LinearFormatter().format( astStart );
            expressionOwner.setExpressions( formatted.toArray( new String[formatted.size()] ) );
        }
    }

    /**
     * Method finds all inclusions of variable with name <b>oldName</b> in node and replaces them with <b>newName</b>
     * @param node
     * @param oldName
     * @param newName
     */
    private void renameVariableInAST(ru.biosoft.math.model.Node node, String oldName, String newName)
    {
        if( node == null )
            return;

        Utils.deepChildren( node ).select( AstVarNode.class ).findAny( var -> var.getName().equals( oldName ) ).ifPresent( var -> {
            var.setName( newName );
            var.setTitle( newName );
        } );
    }
    
    private void renameFunctionInAST(ru.biosoft.math.model.Node node, String oldName, ru.biosoft.math.model.Function replacement)
    {
        if( node == null )
            return;
        
        Utils.deepChildren( node ).select( AstFunNode.class ).filter( f -> f.getFunction().getName().equals( oldName ) ).forEach( f ->
            f.setFunction(replacement) );
    }


    /**
     * Resolves all variable names in EModel
     * 
     * @param model
     *            EModel passed
     * @param compartment
     *            current compartment
     * @param map
     *            output mapping between qualified name and Variable in original
     *            model
     */
    public Map<String, String> resolveVariables()
    {
        return model.getDiagramElement().recursiveStream()
            .mapToEntry( DiagramElement::getRole, Function.identity() )
            .flatMapKeys( this::variableNames )
            .mapToValue( (name, de) -> model.getQualifiedName( name, de ) )
            .toMap();
    }
    
    private Stream<String> variableNames(Role role)
    {
        if(role instanceof VariableRole)
        {
            return Stream.of( ( (VariableRole)role ).getName() );
        }
        if(role instanceof ExpressionOwner)
        {
            ExpressionOwner expressionOwner = (ExpressionOwner)role;
            return Stream.of(expressionOwner.getExpressions())
                    .map( expression -> model.readMath( expression, role ) )
                    .flatMap( Utils::variables );
        }
        return null;
    }

    protected static boolean dependsFromOthers(ru.biosoft.math.model.Node node, Map<String, EquationASTPair> map)
    {
        if(node == null)
            return false;
        return Utils.variables( node ).anyMatch( map::containsKey );
    }

    protected static class EquationASTPair
    {
        public Equation equation;

        public AstStart ast;

        EquationASTPair(Equation equation, AstStart ast)
        {
            this.equation = equation;
            this.ast = ast;
        }
    }

    public static boolean variableIsUsed(String variable, Node node)
    {
        if( node == null )
            return false;
        
        return Utils.variables( node ).has( variable );
    }


    /**
     * Returns array of model parameters in alphabetic order
     * @param emodel
     * @return
     */
    public static String[] getParameters(EModel emodel)
    {
        return emodel.getVariables().names().sorted().toArray( String[]::new );
    }

    /**
     * Generates unique variable name for <b>emodel</b>
     * Result is <b>base</b>i
     * @param emodel
     * @param base
     * @return
     */
    public static String generateUniqueVariableName(EModel emodel, String base)
    {
        String result = base;
        int counter = 1;
        while( emodel.containsVariable( result ) )
            result = base + counter++;
        return result;
    }
    
    //TODO: cache this map
    public static Map<String, List<Equation>> findDefiningEquations(EModel emodel)
    {
        Map<String, List<Equation>> varToEquations = new HashMap<>();
        for( Equation eq : emodel.getEquations() )
        {
            if( Equation.TYPE_ALGEBRAIC.equals( eq.getType() ) || Equation.TYPE_SCALAR_INTERNAL.equals(eq.getType()))
                continue;

            String varName = eq.getVariable();
            Variable variable = emodel.getVariable( varName );

            if( variable.isConstant() && !Equation.TYPE_INITIAL_ASSIGNMENT.equals( eq.getType() ) )
                continue;

            if( variable instanceof VariableRole && ( (VariableRole)variable ).isBoundaryCondition()
                    && Equation.TYPE_RATE.equals( eq.getType() ) && eq.getParent() instanceof Edge )
                continue;

            varToEquations.computeIfAbsent( varName, k -> new ArrayList<>() ).add( eq );
        }
        return varToEquations;
    }

}