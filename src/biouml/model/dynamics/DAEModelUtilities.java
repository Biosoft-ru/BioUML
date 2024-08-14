package biouml.model.dynamics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.AstVarNode;
import ru.biosoft.math.model.Utils;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.SemanticController;
import biouml.model.dynamics.EModel.NodeFilter;
import biouml.model.dynamics.SimpleTableElement.VarColumn;
import biouml.standard.diagram.Util;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.standard.type.Type;
import one.util.streamex.EntryStream;

/**
 * Utility class, providing involved functionality for using EModel
 */
public class DAEModelUtilities
{
    static Logger log = Logger.getLogger(EModel.class.getName());

    boolean verifyDAE(EModel emodel)
    {
        return true;
    }

    public static class MathScalarEquationFilter extends NodeFilter
    {
        @Override
        protected boolean isNodeAcceptable(biouml.model.Node de)
        {
            Equation role = de.getRole(Equation.class);
            String type = de.getKernel().getType();
            return role.getType().equals(Equation.TYPE_SCALAR)
                    && ( type.equals(Type.MATH_EQUATION) || type.equals("equation") );
        }
    }

    public static void substituteAssignmentRules(Map<String, ru.biosoft.math.model.Node> macroMap, AstStart start) throws Exception
    {
        substituteSubtree(macroMap, start);
    }

    protected static void substituteSubtree(Map<String, ru.biosoft.math.model.Node> map, ru.biosoft.math.model.Node node)
    {
        if( node == null )
            return;

        if( node instanceof AstVarNode )
        {
            ru.biosoft.math.model.Node substNode = map.get( ( (AstVarNode)node ).getName());

            if( substNode != null && substNode.jjtGetNumChildren() > 0 )
            {
                ru.biosoft.math.model.Node parent = node.jjtGetParent();
                parent.jjtReplaceChild(node, substNode.jjtGetChild(0));
                substituteSubtree(map, substNode.jjtGetChild(0));
            }
        }
        else
        {
            Utils.children( node ).forEach( child -> substituteSubtree( map, child ) );
        }
    }


    /**
     * Reorders assignment rules according to variable dependencies.
     */
    public static void reorderAssignmentRules(EModel emodel, List<Equation> orderedEquations, List<Equation> cycledEquations, NodeFilter filter)
            throws Exception
    {
        reorderEquations( emodel.getEquations(filter).toSet(), orderedEquations, cycledEquations );
    }

    public static void reorderAssignmentRules(Set<Equation> equations, List<Equation> orderedEquations, List<Equation> cycledEquations)
            throws Exception
    {
        reorderEquations( equations, orderedEquations, cycledEquations );
    }

    protected static Map<String, EquationASTPair> getEquationsMap(Set<Equation> equations) throws Exception
    {
        Map<String, EquationASTPair> map = new HashMap<>();
        for( Equation equation : equations )
        {
            if( map.containsKey( equation.getVariable() ) )
            {
                log.log(Level.SEVERE,  "Duplicate assignment rules for variable " + equation.getVariable() );
                throw new Exception( "Duplicate assignment rules for variable " + equation.getVariable() );
            }

            AstStart start = equation.getMath();
            if( start != null)
                map.put( equation.getVariable(), new EquationASTPair( equation, start ) );
        }
        return map;
    }

    protected static void reorderEquations(Set<Equation> equations, List<Equation> orderedEquations, List<Equation> cycledEquations) throws Exception
    {

        Map<String, EquationASTPair> map = getEquationsMap(equations);
        Set<Map.Entry<String, EquationASTPair>> entrySet = map.entrySet();

        boolean independentEquationFound = true;
        //in this loop we try to find equation which does not depend on other equations and remove it from our map
        //then we reiterate the loop until we could not find such equation anymore, which means that all equations depends on each other
        while( independentEquationFound )
        {
            entrySet = map.entrySet();

            if( entrySet.isEmpty() )
                break;

            independentEquationFound = false;
            for( Map.Entry<String, EquationASTPair> entry : entrySet )
            {
                EquationASTPair pair = entry.getValue();
                if( !dependentFromOthers( pair, map ) )
                {
                    independentEquationFound = true;
                    orderedEquations.add( pair.equation );
                    map.remove( entry.getKey() );
                    break;
                }
            }
        }

        List<Equation> tail = new ArrayList<>();
        boolean unusedEquationFound = true;
        //at this step we try to find equations which calculates variables unused in other equations and remove them from cycled
        while( unusedEquationFound )
        {
            entrySet = map.entrySet();

            if( entrySet.isEmpty() )
                break;

            unusedEquationFound = false;
            for( Map.Entry<String, EquationASTPair> entry : entrySet )
            {
                EquationASTPair pair = entry.getValue();
                if( !othersDepend(pair, map) )
                {
                    unusedEquationFound = true;
                    tail.add(pair.equation);
                    map.remove(entry.getKey());
                    break;
                }
            }
        }
        Collections.reverse(tail);
        orderedEquations.addAll(tail);

        if (cycledEquations == null)
            return;
        for( EquationASTPair pair : map.values() )
        {
           cycledEquations.add( pair.equation );
        }
    }

    /**
     * Checks if equation in pair depends on any equations from map
     * i.e. have their variables in its right side
     */
    protected static boolean dependentFromOthers(EquationASTPair pair, Map<String, EquationASTPair> map)
    {
        return pair.variables.stream().anyMatch(s->map.containsKey(s));
    }

    /**
     * Checks if any of equations from map depends on equation in pair
     * i.e. have its variable in their right side
     */
    protected static boolean othersDepend(EquationASTPair pair, Map<String, EquationASTPair> map)
    {
        String variable = pair.equation.getVariable();
        return EntryStream.of(map).values().anyMatch(p->p.variables.contains(variable));
    }

//    protected static boolean dependentFromOthers(ru.biosoft.math.model.Node node, Map<String, EquationASTPair> map)
//    {
//
//        if( node instanceof ru.biosoft.math.model.AstVarNode )
//        {
//            if( map.containsKey( ( (ru.biosoft.math.model.AstVarNode)node ).getName() ) )
//                return true;
//        }
//        else
//        {
//            if( node == null )
//                return false;
//
//            return Utils.children( node ).anyMatch( child -> dependentFromOthers( child, map ) );
//        }
//        return false;
//    }

    protected static class EquationASTPair
    {
        public Equation equation;
        public AstStart ast;
        public Set<String> variables;

        EquationASTPair(Equation equation, AstStart ast)
        {
            this.equation = equation;
            this.ast = ast;
            this.variables = Utils.variables(ast).toSet();

            if( equation.getType().equals( Equation.TYPE_INITIAL_VALUE ) )
                this.variables.remove( equation.getVariable() );
        }
    }

    /**
     *  Check if diagram element de has at least one related variable
     *  (for example, used in kinetic law for Reaction)
     *  from input set required
     */
    public static boolean hasVariables(DiagramElement de, Set<String> required)
    {
        Base kernel = de.getKernel();
        if( kernel != null )
        {
            Role role = de.getRole();
            String type = kernel.getType();
            if( kernel instanceof Reaction )
            {
                String formula = ( (Reaction)kernel ).getKineticLaw().getFormula();
                if( Utils.variables( formula ).anyMatch( required::contains ) )
                    return true;

                else if( ( (Node)de ).edges().map( e -> e.getOtherEnd( (Node)de ) )
                        .filter( n -> ( n.getRole() != null && n.getRole() instanceof Variable ) )
                        .anyMatch( n -> required.contains( ( (Variable)n.getRole() ).getName() ) ) )
                    return true;
            }
            else if( Util.isPort( de ) )
            {
                if( required.contains( Util.getPortVariable( de ) ) )
                {
                    return true;
                }
            }
            else if( role != null && role instanceof ExpressionOwner && !type.equals( Type.MATH_FUNCTION ) )
            {
                for( String expression : ( (ExpressionOwner)role ).getExpressions() )
                {
                    if( Utils.variables( expression ).anyMatch( required::contains ) )
                        return true;
                }
            }
            else if( role instanceof Variable )
            {
                if( required.contains( ( (Variable)role ).getName() ) )
                    return true;
            }
            else if( de instanceof biouml.model.Edge )
            {
                return ( (biouml.model.Edge)de ).nodes().filter( n -> Util.isPort( n ) )
                        .anyMatch( n -> required.contains( Util.getPortVariable( n ) ) );
            }
            else if( Util.isBlock( de ) )
            {
                for( DiagramElement blockDe : (Compartment)de )
                {
                    if( hasVariables( blockDe, required ) )
                        return true;
                }
            }
        }
        return false;
    }
    
    public static boolean hasFunction(DiagramElement de, Set<String> required)
    {
        Base kernel = de.getKernel();
        if( kernel != null )
        {
            Role role = de.getRole();
            if( kernel instanceof Reaction )
            {
                String formula = ( (Reaction)kernel ).getKineticLaw().getFormula();
                return Utils.functions( formula ).anyMatch( required::contains );
            }

            if( role instanceof ExpressionOwner )
            {
                if( role instanceof Function )
                {
                    if( required.contains( ( (Function)role ).getName() ) )
                        return true;
                }
                for( String expression : ( (ExpressionOwner)role ).getExpressions() )
                {
                    if( Utils.functions( expression ).anyMatch( required::contains ) )
                        return true;
                }
            }
            else if( Util.isBlock( de ) )
            {
                for( DiagramElement blockDe : (Compartment)de )
                {
                    if( hasFunction( blockDe, required ) )
                        return true;
                }
            }
        }
        return false;
    }
    
    public static void processSimpleTableElement(@Nonnull Compartment compartment, Node node) throws Exception
    {
        SimpleTableElement tableElement = node.getRole( SimpleTableElement.class );
        TableDataCollection table = tableElement.getTable();
        if( table == null )
            throw new Exception( "Please specufy table data collection for element " + node.getTitle() );

        VarColumn argColumn = tableElement.getArgColumn();
        VarColumn[] columns = tableElement.getColumns();

        String argName = argColumn.getVariable();
        String argColumnName = argColumn.getColumn();
        double[] arg = TableDataCollectionUtils.getColumn( table, argColumnName );

        SemanticController controller = Diagram.getDiagram( compartment).getType().getSemanticController();
        
        for( int i = 0; i < columns.length; i++ )
        {
            VarColumn var = columns[i];
            String varName = var.getVariable();
            String columnName = var.getColumn();
            double[] values = TableDataCollectionUtils.getColumn( table, columnName );
            String rightHandSide = generatePiecewise( argName, arg, values );
            Equation eq = new Equation( null, Equation.TYPE_SCALAR, varName );
            eq.setFormula( rightHandSide );
            DiagramElement de = controller.createInstance( compartment, Equation.class, node.getLocation(), eq ).getElement();
            compartment.put( de );
        }
    }
    
    public static String generatePiecewise(String argName, double[] time, double[] values)
    {
        StringBuffer result = new StringBuffer();
        result.append( "piecewise( " );               
        double curValue = Double.NaN;        
        for (int i=0; i<time.length - 1; i++)
        {
            double nextValue = values[i];
            if (Double.isNaN( nextValue ))
                continue;            
            else if (!Double.isNaN( curValue ) && nextValue != curValue)// value changed from this time point            
                result.append( argName+" < "+time[i] + " => "+ curValue +"; " );
            curValue = nextValue;
        }
        result.append( curValue +" );");
        return result.toString();
    }
}