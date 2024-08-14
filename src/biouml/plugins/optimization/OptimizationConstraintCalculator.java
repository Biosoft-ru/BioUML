package biouml.plugins.optimization;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.logging.Logger;

import ru.biosoft.math.model.AstConstant;
import ru.biosoft.math.model.AstFunNode;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.AstVarNode;
import ru.biosoft.math.model.Function;
import ru.biosoft.math.model.Node;
import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.MathCalculator;
import biouml.model.dynamics.MathContext;
import biouml.standard.diagram.Util;
import biouml.standard.simulation.SimulationResult;

public class OptimizationConstraintCalculator extends MathCalculator
{
    protected static final Logger log = Logger.getLogger(OptimizationConstraintCalculator.class.getName());

    /*
     * The constraints checking must be performed before a constraint inaccuracy calculation.
     */
    public double getConstraintInaccuracy(int constraintNumber, SimulationResult sr, double startTime, double endTime)
    {
        AstStart start = parsedFormulas.get(constraintNumber);

        Map<String, Integer> variablesMap = sr.getVariableMap();

        AstFunNode child = (AstFunNode)start.jjtGetChild(0);
        Function function = child.getFunction(); // The function of the root node is comparison: "=", ">"...
        String name = function.getName();

        double inaccuracy = 0;
        double[] times = sr.getTimes();

        int steps = 0;

        for( int i = 0; i < times.length; ++i )
        {
            if( times[i] >= startTime && times[i] <= endTime )
            {
                MathContext variableValues = new MathContext();
                if( variablesMap != null )
                {
                    for(String var : variablesMap.keySet())
                    {
                        int columnNumber = variablesMap.get(var);
                        String varName = checkVariable(constraintNumber, var);
                        if(varName != null)
                            variableValues.put(varName, sr.getValues()[i][columnNumber]);
                    }
                }

                double leftSideValue = processNode(child.jjtGetChild(0), variableValues);
                double rightSideValue = processNode(child.jjtGetChild(1), variableValues);

                try
                {
                    Method comparison = mathOperations.getMethod(name);
                    if( !(Boolean)comparison.invoke(null, leftSideValue, rightSideValue) )
                    {
                        inaccuracy += Math.abs(leftSideValue - rightSideValue);
                    }
                }
                catch( IllegalAccessException | InvocationTargetException exc )
                {
                    throw new AssertionError(exc);
                }

                steps++;
            }
        }

        if( steps != 0 )
            inaccuracy /= steps;

        return inaccuracy;
    }

    public double getConstraintInaccuracy(int constraintNumber, Map<String, Double> steadyState)
    {
        MathContext variableValues = new MathContext();
        if(steadyState != null)
        {
            for(String var : steadyState.keySet())
            {
                String varName = checkVariable(constraintNumber, var);
                if( varName != null )
                    variableValues.put(varName, steadyState.get(var));
            }
        }

        AstStart start = parsedFormulas.get(constraintNumber);

        AstFunNode child = (AstFunNode)start.jjtGetChild(0);
        Function function = child.getFunction(); // The function of the root node is comparison: "=", ">"...
        String name = function.getName();

        double inaccuracy = 0;

        double leftSideValue = processNode(child.jjtGetChild(0), variableValues);
        double rightSideValue = processNode(child.jjtGetChild(1), variableValues);

        try
        {
            Method comparison = mathOperations.getMethod(name);
            if( !(Boolean)comparison.invoke(null, leftSideValue, rightSideValue) )
            {
                inaccuracy += Math.abs(leftSideValue - rightSideValue);
            }
        }
        catch( IllegalAccessException | InvocationTargetException exc )
        {
            throw new AssertionError(exc);
        }
        return inaccuracy;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Constraint checking
    //
    private final HashMap<Integer, AstStart> parsedFormulas = new HashMap<>();
    private final HashMap<Integer, String> paths = new HashMap<>();

    public void parseConstraints(List<OptimizationConstraint> constraints, Diagram diagram)
    {
        if( constraints == null || constraints.size() == 0 )
            return;

        for( OptimizationConstraint constr : constraints )
        {
            String formula = constr.getFormula();
            int constraintNumber = constraints.indexOf(constr);
            AstStart start = readMath(formula);

            if( start != null && checkComparisonOperation(start, constraintNumber) )
            {
                Node child = start.jjtGetChild(0);

                for( int i = 0; i < child.jjtGetNumChildren(); ++i )
                {
                    if( !checkNode(child.jjtGetChild(i), diagram, constr.getSubdiagramPath(), constraintNumber) )
                    {
                        throw new IllegalArgumentException("Wrong constraint in line " + constraints.indexOf(constr));
                    }
                }

                parsedFormulas.put(constraintNumber, start);
                paths.put(constraintNumber, constr.getSubdiagramPath());
            }
            else
            {
                throw new IllegalArgumentException("Wrong constraint in line " + constraints.indexOf(constr));
            }
        }
    }

    private boolean checkComparisonOperation(Node node, int constraintNumber)
    {
        if( node != null && node.jjtGetNumChildren() == 1 )
        {
            Node child = node.jjtGetChild(0);
            if( child instanceof AstFunNode )
            {
                Function function = ( (AstFunNode)child ).getFunction();
                String name = function.getName();

                if( child.jjtGetNumChildren() == 2 )
                {
                    if( mathOperations.isComparison(name) )
                    {
                        return true;
                    }
                }
            }
        }
        log.log(Level.SEVERE, MessageBundle.format("ERROR_WRONG_CONSTRAINT", new Object[] {constraintNumber}));
        return false;
    }

    private boolean checkNode(Node node, Diagram diagram, String subdiagramPath, int constraintNumber)
    {
        if( node instanceof AstConstant )
            return checkConstant((AstConstant)node, constraintNumber);
        else if( node instanceof AstVarNode )
            return checkVariable((AstVarNode)node, diagram, subdiagramPath, constraintNumber);
        else if( node instanceof AstFunNode )
            return checkFunction((AstFunNode)node, diagram, subdiagramPath, constraintNumber);

        log.log(Level.SEVERE, MessageBundle.format("ERROR_WRONG_CONSTRAINT", new Object[] {constraintNumber}));
        return false;
    }

    private boolean checkConstant(AstConstant node, int constraintNumber)
    {
        Object value = node.getValue();

        if( value instanceof Double || value instanceof Integer )
            return true;

        log.log(Level.SEVERE, MessageBundle.format("WRONG_TYPE_OF_CONSTANT", new Object[] {value, constraintNumber}));
        return false;
    }

    private boolean checkVariable(AstVarNode node, Diagram diagram, String subdiagramPath, int constraintNumber)
    {
        try
        {
            Diagram targetDiagram = Util.getInnerDiagram(diagram, subdiagramPath);
            if(targetDiagram.getRole() instanceof EModel)
                if(targetDiagram.getRole(EModel.class).getVariable(node.getName()) != null)
                    return true;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, MessageBundle.format("ERROR_VARIABLE_GETTING", new Object[] {node.getName()}));
        }

        log.log(Level.SEVERE, MessageBundle.format("ERROR_WRONG_CONSTRAINT_UNKNOWN_VAR", new Object[] {constraintNumber, node.getName()}));
        return false;
    }

    private boolean checkFunction(AstFunNode node, Diagram diagram, String subdiagramPath, int constraintNumber)
    {
        for( int i = 0; i < node.jjtGetNumChildren(); ++i )
            if(!checkNode(node.jjtGetChild(i), diagram, subdiagramPath, constraintNumber))
                return false;

        String name = node.getFunction().getName();
        if( mathOperations.isComparison(name) )
        {
            log.log(Level.SEVERE, MessageBundle.format("ERROR_WRONG_CONSTRAINT", new Object[] {constraintNumber}));
            return false;
        }

        if( mathOperations.contains(name) )
            return true;
        else
        {
            log.log(Level.SEVERE, MessageBundle.format("ERROR_WRONG_CONSTRAINT_UNKNOWN_FUNC", new Object[] {name, constraintNumber}));
            return false;
        }
    }

    private String checkVariable(int constraintNumber, String fullVarName)
    {
        if(paths.get(constraintNumber).isEmpty())
        {
            if(!fullVarName.contains( "__" ) && !fullVarName.contains( "/" ))
                return fullVarName;
        }
        else
        {
            String[] varTokens = fullVarName.split("/");
            String[] pathTokens = paths.get( constraintNumber ).split("/");
            if(varTokens.length == pathTokens.length + 1)
            {
                boolean suitable = true;
                for(int i = 0; i < pathTokens.length; ++i)
                    if(!pathTokens[i].equals(varTokens[i]))
                        suitable = false;
                if(suitable)
                    return varTokens[varTokens.length - 1];
            }
        }
        return null;
    }
}
