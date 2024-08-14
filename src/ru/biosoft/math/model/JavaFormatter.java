package ru.biosoft.math.model;

import java.util.HashMap;
import java.util.Map;

public class JavaFormatter extends PiecewiseExtFormatter
{
    protected Map<String, Integer> historicalIndexes;
    public JavaFormatter(Map<String, Integer> historicalIndexes)
    {
        this.historicalIndexes = historicalIndexes;
    }
    
    private static final Map<String, String> badConstants = new HashMap<>();
    {
        {
            badConstants.put("exp", "exponentiale");
        }
    }

    private static final Map<String, String> badFuncNames = new HashMap<>();
    {
        {
            badFuncNames.put("abs", "Math.abs");
            badFuncNames.put("arccos", "Math.acos");
            badFuncNames.put("arcsin", "Math.asin");
            badFuncNames.put("arctan", "Math.atan");
            badFuncNames.put("cos", "Math.cos");
            badFuncNames.put("exp", "Math.exp");
            badFuncNames.put("ln", "Math.log");
            badFuncNames.put("sqrt", "Math.sqrt");
            badFuncNames.put("sin", "Math.sin");
            badFuncNames.put("tan", "Math.tan");
            badFuncNames.put("xor", "^");
            badFuncNames.put("floor", "Math.floor");
            badFuncNames.put("ceiling", "Math.ceil");
            badFuncNames.put("random", "Math.random");
            badFuncNames.put("uniform", "MathRoutines.uniform");
            badFuncNames.put("normal", "MathRoutines.normal");
            badFuncNames.put("logNormal", "MathRoutines.logNormal");
            badFuncNames.put("binomial", "MathRoutines.binomial");
            badFuncNames.put("factorial", "MathRoutines.factorial");
            badFuncNames.put("sinh", "Math.sinh");
            badFuncNames.put("cosh", "Math.cosh");
            badFuncNames.put("tanh", "Math.tanh");
            badFuncNames.put("coth", "MathRoutines.ctgh");
            badFuncNames.put("arccosh", "MathRoutines.ach");
            badFuncNames.put("arcsinh", "MathRoutines.ash");
            badFuncNames.put("arccoth", "MathRoutines.actgh");
            badFuncNames.put("arctanh", "MathRoutines.atgh");
            badFuncNames.put("sec", "MathRoutines.sec");
            badFuncNames.put("csc", "MathRoutines.csec");
            badFuncNames.put("csch", "MathRoutines.csech");
            badFuncNames.put("sech", "MathRoutines.sech");
            badFuncNames.put("arcsec", "MathRoutines.asec");
            badFuncNames.put("arccsc", "MathRoutines.acsec");
            badFuncNames.put("arcsech", "MathRoutines.asech");
            badFuncNames.put("arccsch", "MathRoutines.acsech");
            badFuncNames.put("arccot", "MathRoutines.actg");
            badFuncNames.put( "round", "Math.rint" );
        }
    }

    @Override
    protected String getFunctionName(String name)
    {
        String strTestName = badFuncNames.get(name);
        return ( strTestName == null ) ? name : strTestName;
    }

    @Override
    protected void processFunction(AstFunNode node)
    {
        Function function = node.getFunction();
        String functionName = function.getName();
        Node first = ( node.jjtGetNumChildren() > 0 ) ? node.jjtGetChild(0) : null;
        Node second = ( node.jjtGetNumChildren() > 1 ) ? node.jjtGetChild(1) : null;

        if( function.getPriority() == Function.POWER_PRIORITY && "^".equals(functionName) )
        {
            if( second instanceof AstConstant && ( (AstConstant)second ).getValue() instanceof Number )
            {
                double value = ( (Number) ( (AstConstant)second ).getValue() ).doubleValue();
                if( value == -1.0 )
                    append("(1.0/(", first, "))");
                else if( value == 0.0 )
                    append("1");
                else if( value == 1.0 )
                    append("(", first, ")");
                else if( value >= -1.0 && value < 1000 && Math.floor(value) == value )
                    append("MathRoutines.pow(", first, ", (int)", second, ")");
                else
                    append("Math.pow(", first, ",", second, ")");
            }
            else
            {
                append("Math.pow(", first, ",", second, ")");
            }
            return;
        }
        else if( function.getPriority() == Function.FUNCTION_PRIORITY )
        {
            if( "delay".equals(functionName) )
            {
                if( historicalIndexes != null )
                {
                    if( first instanceof AstVarNode )
                    {
                        Integer index = historicalIndexes.get( ( (AstVarNode)first ).getName());
                        if( index != null ) //TODO: check why this can be null
                            append("delay(" + index + ", time - (", second, "))");
                    }
                }
            }
            else if( "root".equals(functionName) )
            {
                append("(Math.pow(", first, ", 1.0/( (double)(", second, "))))");
            }
            else if( "ln".equals(functionName) ) //logarithm to the base E
            {
                append("Math.log(", first, ")");
            }
            else if( "log".equals(functionName) )
            {
                if( second == null )
                    append("Math.log10(", first, ")");
                else
                    append("(Math.log(", first, ")/Math.log(", second, "))");
            }
            else if( "cot".equals(functionName) )
            {
                append("(1/Math.tan(", first, "))");
            }
            else if( "mod".equals(functionName) )
            {
                append("((", first, ")%(", second, "))");
            }
            else if ("rem".equals(functionName))
            {
                append("((", first, ")%(", second, "))");
            }
            else if ("quotient".equals(functionName))
            {
                append("Math.floor(", first, "/", second, ")");
            }
            else
            {
                super.processFunction(node);
            }
            return;
        }
        super.processFunction(node);
    }
    
    @Override
    protected void processConstant(AstConstant node)
    {
        if( badConstants.containsKey(node.getName()) )
        {
            result.append(badConstants.get(node.getName()));
            return;
        }
        else
        {
            String formatted = processSpecialValues(node);
            if (formatted!=null)
            {
                result.append(formatted);
                return;
            }
        }
        super.processConstant(node);
    }
    
    /** Returns code representation of special java values (Infinity, NaN)*/
    protected String processSpecialValues(AstConstant node)
    {
        if( node.getValue() == null )
            return null;
        if( node.getValue().equals(Double.POSITIVE_INFINITY) )
            return "Double.POSITIVE_INFINITY";
        if( node.getValue().equals(Double.NEGATIVE_INFINITY) )
            return "Double.NEGATIVE_INFINITY";
        if( node.getValue().equals(Double.NaN) )
            return "Double.NaN";
        return null;
    }


    @Override
    protected void processFunctionDeclaration(AstFunctionDeclaration node)
    {
        int n = node.jjtGetNumChildren() - 1;
        append("    protected static double " + node.getName() + "(");
        for( int i = 0; i < n; i++ )
            append("double ", node.jjtGetChild(i), i < n - 1 ? ", " : "");
        append(")" + endl + "    {" + endl + "         return ", node.jjtGetChild(n), ";" + endl + "    }" + endl);
    }

    @Override
    protected void processIf(AstPiece node)
    {
        append("if (", node.getCondition(), ") {" + endl + "    " + auxVariableName + " = ", node.getValue(), ";" + endl + "}");
    }

    @Override
    protected void processElseIf(AstPiece node)
    {
        append("else if (", node.getCondition(), ") {" + endl + "    " + auxVariableName + " = ", node.getValue(), ";" + endl + "}");
    }
    
    @Override
    protected void processOtherwise(AstPiece node)
    {
        append("else {" + endl + "    " + auxVariableName + " = ", node.getValue(), ";" + endl + "}" + endl);
    }

    @Override
    protected void processPiecewiseEnd(Node node)
    {
    }

    @Override
    protected void processPiecewiseBegin(Node node)
    {
        append("double " + auxVariableName + " = 0;" + endl);
    }
}
