package biouml.model.dynamics;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import java.util.logging.Logger;

import biouml.model.MessageBundle;
import ru.biosoft.math.model.AstConstant;
import ru.biosoft.math.model.AstFunNode;
import ru.biosoft.math.model.AstFunctionDeclaration;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.AstVarNode;
import ru.biosoft.math.model.Node;
import ru.biosoft.math.model.Utils;
import ru.biosoft.math.parser.Parser;

public class MathCalculator
{
    protected static final Logger log = Logger.getLogger(MathCalculator.class.getName());
    protected static final MathOperations mathOperations = new MathOperations();
    private Map<String, AstStart> formulas;
    private List<AstFunctionDeclaration> functionDeclarations;
    private EModel emodel;
    private Map<String, AstStart> equations;
    public MathCalculator(EModel emodel)
    {
        this.emodel = emodel;
        formulas = new HashMap<>();
    }
    
    public MathCalculator()
    {
        formulas = new HashMap<>();
    }

    /**
     * The method calculates mathematical formulas based on the values specified in the variableValues map.
     * All calculated formulas are stored in the {@link #formulas} repository.
     * 
     * If the formula contains variables which have to be calculated by some additional scalar equations {@link Equation},
     * these equations must be loaded into the {@link #equations} repository by the method {@link #addEquations}
     * before starting calculations.
     * 
     * If the formula used additional functions {@link Function} (other than simple functions, such as +, -, etc.), they
     * must be loaded into the {@link #functionDeclarations} repository by the method {@link #addFunctionDeclarations}.
     * 
     * To clear all repositories use the method {@link #clearRepositories}.
     * 
     * @param formula formula to calculate.
     * 
     * @param variableValues a context containing variables and their values
     * It must contain all the variables and parameters that may appear in the formula.
     */
    public double[] calculateMath(String formula, MathContext variableValues)
    {
        AstStart start = formulas.get(formula);
        if( start == null )
        {
            start = readMath(formula);
            formulas.put(formula, start);
        }
        return calculateMath(start, variableValues);
    }
    
    public double[] calculateMath(AstStart start, MathContext variableValues)
    {
        return start == null ? null : Utils.children(start).mapToDouble(child -> processNode(child, variableValues)).toArray();
    }

    protected double processNode(Node node, MathContext variableValues)
    {
        if( node instanceof AstConstant )
            return processConstant((AstConstant)node);
        else if( node instanceof AstVarNode )
            return processVariable((AstVarNode)node, variableValues);
        else if( node instanceof AstFunNode )
            return processFunction((AstFunNode)node, variableValues);
        return 0;
    }

    protected double processConstant(AstConstant node)
    {
        Object value = node.getValue();
        return ( value instanceof Number )? ((Number)value).doubleValue(): 0;
    }

    protected double processVariable(AstVarNode node, MathContext variableValues)
    {
        String nodeName = node.getName().replace("\"", "");

        //Check whether this variable has to be calculated by any additional equation.
        if( equations != null && equations.containsKey(nodeName) )
            return calculateMath(equations.get(nodeName), variableValues)[0];

        if(!variableValues.contains(nodeName))
        {
            if( emodel != null )
            {
                Variable var = emodel.getVariable(nodeName);
                if( var != null )
                    return var.getInitialValue();
            }
            log.log(Level.SEVERE, "Unknown value for variable " + nodeName);
            return 0;
        }
        return variableValues.get(nodeName, 0);
    }

    protected double processFunction(AstFunNode node, MathContext variableValues)
    {
        String name = node.getFunction().getName();
        double[] args = new double[node.jjtGetNumChildren()];
        for( int i = 0; i < args.length; ++i )
            args[i] = processNode(node.jjtGetChild(i), variableValues);

        try
        {
            //Check whether this is a simple function, such as +, -, *, etc.
            Method method = mathOperations.getMethod(name);
            if( method != null )
                return (Double)method.invoke(null, (Object[])ArrayUtils.toObject(args));

            //Try to find this function declaration.
            if( functionDeclarations != null )
            {
                for( AstFunctionDeclaration declaration : functionDeclarations )
                {
                    if( declaration.getName().equals(name) && declaration.getNumberOfParameters() == args.length )
                    {
                        MathContext params = new MathContext(variableValues);
                        for( int i = 0; i < declaration.jjtGetNumChildren() - 1; ++i )
                        {
                            Node functionParam = declaration.jjtGetChild(i);
                            if( functionParam instanceof AstVarNode )
                                params.put( ( (AstVarNode)functionParam ).getName(), args[i]);
                        }
                        return processNode(declaration.jjtGetChild(declaration.jjtGetNumChildren()-1), params);
                    }
                }
            }
            log.log(Level.SEVERE, "Unknown function: '" + name + "'.");
            return 0;
        }
        catch( IllegalAccessException | InvocationTargetException exc )
        {
            throw new AssertionError(exc);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Formulas parsing
    //
    public static AstStart readMath(String math)
    {
        AstStart start = null;

        if( math != null && math.length() > 0 )
        {
            Parser parser = new Parser();

            try
            {
                int status = parser.parse(math);

                if( status > ru.biosoft.math.model.Parser.STATUS_OK )
                    MessageBundle.error(log, "ERROR_MATH_PARSING", new Object[] {math, Utils.formatErrors(parser)});

                if( status < ru.biosoft.math.model.Parser.STATUS_FATAL_ERROR )
                    start = parser.getStartNode();
            }
            catch( Throwable t )
            {
                MessageBundle.error(log, "ERROR_MATH_PARSING", new Object[] {math, t.toString()});
            }
        }
        return start;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Calculator repositories
    //
    public void addFunctionDeclarations(Function[] functions)
    {
        if( functions != null && functions.length > 0 )
        {
            if( functionDeclarations == null )
                functionDeclarations = new ArrayList<>();

            for (Function function : functions)
            {
                AstStart math = readMath(function.getFormula());
                if( math.jjtGetChild(0) instanceof AstFunctionDeclaration )
                    functionDeclarations.add((AstFunctionDeclaration)math.jjtGetChild(0));
            }
        }
    }
    
    /**
     * Method adds functions from emodel - their formulas will be parsed by this emodel
    */
    public void setEModel(EModel emodel)
    {
        this.emodel = emodel;
        if( functionDeclarations == null )
            functionDeclarations = new ArrayList<>();

        for( Function function : emodel.getFunctions() )
        {
            AstStart math = emodel.readMath(function.getFormula(), function);
            if( math.jjtGetChild(0) instanceof AstFunctionDeclaration )
                functionDeclarations.add((AstFunctionDeclaration)math.jjtGetChild(0));
        }
    }

    public void addEquations(List<Equation> equations) throws Exception
    {
        if( equations != null && equations.size() > 0 )
        {
            if( this.equations == null )
                this.equations = new HashMap<>();

            for( Equation equation : equations )
            {
                if( equation.getType().equals(Equation.TYPE_SCALAR) || equation.getType().equals(Equation.TYPE_INITIAL_ASSIGNMENT)
                        || equation.getType().equals( Equation.TYPE_SCALAR_INTERNAL ) )
                {
                    AstStart math = readMath( equation.getFormula() );
                    if( math != null )
                        this.equations.put(equation.getVariable(), math);
                }
                else
                {
                    throw new Exception("Unsupported type of equation: " + equation.getType());
                }
            }
        }
    }

    public void clearRepositories()
    {
        formulas = new HashMap<>();

        if( functionDeclarations != null )
            functionDeclarations = new ArrayList<>();

        if( equations != null )
            equations = new HashMap<>();
    }
}
