package ru.biosoft.math.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class DefaultParserContext implements ParserContext
{
    public static final String OR       = "||";
    public static final String AND      = "&&";
    public static final String NOT      = "!";
    public static final String XOR      = "xor";

    public static final String GT       = ">";
    public static final String LT       = "<";
    public static final String GEQ      = ">=";
    public static final String LEQ      = "<=";
    public static final String EQ       = "==";
    public static final String NEQ      = "!=";

    public static final String PLUS     = "+";
    public static final String MINUS    = "-";
    public static final String UMINUS   = "u-";
    public static final String TIMES    = "*";
    public static final String DIVIDE   = "/";
    public static final String MOD   = "mod";
    public static final String POWER    = "^";
    public static final String ROOT     = "root";
    public static final String ABS      = "abs";
    public static final String EXP      = "exp";
    public static final String LN       = "ln";
    public static final String LOG      = "log";
    
    public static final String SQRT      = "sqrt";
    
    public static final String MAX      = "max";
    public static final String MIN      = "min";

    public static final String SIN      = "sin";
    public static final String COS      = "cos";
    public static final String TAN      = "tan";
    public static final String COT      = "cot";
    public static final String ASIN     = "arcsin";
    public static final String ACOS     = "arccos";
    public static final String ATAN     = "arctan";


    public static final String ARCCOSH  = "arccosh";
    public static final String ARCSINH  = "arcsinh";
    public static final String ARCCOT   = "arccot";
    public static final String ARCCOTH  = "arccoth";
    public static final String ARCSCS   = "arccsc";
    public static final String ARCCSCH  = "arccsch";
    public static final String ARCSEC   = "arcsec";
    public static final String ARCSECH  = "arcsech";
    public static final String ARCTANH  = "arctanh";
    public static final String COSH     = "cosh";
    public static final String COTH     = "coth";
    public static final String CSC      = "csc";
    public static final String CSCH     = "csch";
    public static final String SEC      = "sec";
    public static final String SECH     = "sech";
    public static final String SINH     = "sinh";
    public static final String TANH     = "tanh";

    public static final String DIFF     = "diff";
    public static final String ASSIGNMENT = "=";

    public static final String FLOOR     = "floor";
    public static final String CEIL      = "ceiling";
    public static final String FACTORIAL = "factorial";
    public static final String DELAY = "delay";
    public static final String RATE_OF = "rateOf";
    
    public static final String REM = "rem";
    public static final String QUOTIENT = "quotient";
    
    public static final String CONST_TRUE  = "true";
    public static final String CONST_FALSE = "false";
    public static final String CONST_PI    = "pi";
    public static final String CONST_E     = "exp";
    public static final String CONST_EXPONENTIALE     = "exponentiale";
    public static final String CONST_AVOGADRO     = "avogadro";
    public static final String CONST_INFINITY     = "Infinity";//"Double.POSITIVE_INFINITY";
    //public static final String CONST_NEGATIVE_INFINITY     = "Double.NEGATIVE_INFINITY";
    public static final String CONST_NaN     = "NaN";//"Double.NaN";
    public static final double CONST_AVOGADRO_VALUE = 6.02214179E23;
    
    private static final Set<String> logicalOperators = new HashSet<String>()
    {
        {
            add(OR);
            add(XOR);
            add(AND);
            add(NOT);
        }
    };

    private static final Set<String> relationalOperators = new HashSet<String>()
    {
        {
            add(GT);
            add(LT);
            add(EQ);
            add(GEQ);
            add(LEQ);
            add(NEQ);
        }
    };
    
    ParserContext parent = null;
    
    protected HashMap<String, Object> variablesMap = new HashMap<>();
    protected HashMap<String, String> substitutionMap = new HashMap<>();
    protected HashMap<String, Object> constantsMap = new HashMap<>();
    protected HashMap<String, Function> functionsMap = new HashMap<>();
  
    public DefaultParserContext()
    {
        declareStandardConstants(this);
        declareStandardOperators(this);
    }

    public ParserContext getParentContext()
    {
        return parent;
    }
    public void setParentContext(ParserContext parent)
    {
        this.parent = parent;
    }

    @Override
    public boolean containsConstant(String name)
    {
        if( constantsMap.containsKey(name) )
            return true;

        if( parent != null )
            return parent.containsConstant(name);

        return false;
    }

    @Override
    public Object getConstantValue(String name)
    {
        Object value = constantsMap.get(name);
        if( value == null && parent != null )
            value = parent.getConstantValue(name);

        return value;
    }

    @Override
    public void declareConstant(String name, Object value)
    {
        constantsMap.put(name, value);
    }
    
    @Override
    public void removeConstant(String name)
    {
        constantsMap.remove(name);
    }

    /**
     * Declares standard constants:
     * true, false, pi, ...
     *
     * @todo implement properly
     */
    public static void declareStandardConstants(ParserContext context)
    {
        context.declareConstant(CONST_TRUE, true);
        context.declareConstant(CONST_FALSE, false);
        context.declareConstant(CONST_PI, Math.PI);
        context.declareConstant(CONST_E, Math.E);
        context.declareConstant(CONST_EXPONENTIALE, Math.E);
        context.declareConstant(CONST_AVOGADRO, CONST_AVOGADRO_VALUE);
        context.declareConstant(CONST_INFINITY, Double.POSITIVE_INFINITY);
        context.declareConstant(CONST_NaN, Double.NaN);
        context.declareConstant("__INITIAL_VALUE__", 0.0);   //TODO: remove     
    }

    ///////////////////////////////////////////////////////////////////
    // Variable issues
    //

   
    @Override
    public boolean containsVariable(String name)
    {
        if( variablesMap.containsKey(name) )
            return true;

        if( parent != null )
            return parent.containsVariable(name);

        return false;
    }

    @Override
    public Object getVariableValue(String name)
    {
        Object value = variablesMap.get(name);

        if( value == null && parent != null )
            value = parent.getVariableValue(name);

        return value;
    }

    @Override
    public void declareVariable(String name, Object value)
    {
        variablesMap.put(name, value);
    }
    
    public Set<String> getVariableNames()
    {
        return variablesMap.keySet();
    }

    ///////////////////////////////////////////////////////////////////
    // Function issues
    //
    @Override
    public Function getFunction(String name)
    {
        if( name.startsWith("\"") )
            name = name.substring(1, name.length()-1 );

        // use substitution
        if( substitutionMap.containsKey(name) )
            name = substitutionMap.get(name);

        Function function = functionsMap.get(name);

        if (function == null && parent != null)
            function = parent.getFunction(name);

        return function;
    }

    @Override
    public void declareFunction(Function function)
    {
        functionsMap.put(function.getName(), function);
    }

    public void declareFunctionNameSubstitution(String name, String replaceBy)
    {
        substitutionMap.put(name, replaceBy);
    }  
    
    public static boolean isLogicalConstant(String name)
    {
        return "true".equals(name) || "false".equals(name);
    }
    
    public static boolean isLogicalOperator(Function f)
    {
        return logicalOperators.contains(f.getName());
    }
    
    public static boolean isRelationalOperator(Function f)
    {
        return relationalOperators.contains(f.getName());
    }
    
    /**
     * Declares standard operators:
     *
     * @todo implement properly
     * @todo declare operator names as constants
     */
    public static void declareStandardOperators(ParserContext context)
    {
        // Logical operators
        context.declareFunction(new PredefinedFunction(OR, Function.LOGICAL_PRIORITY, -1));
        context.declareFunction(new PredefinedFunction(AND, Function.LOGICAL_PRIORITY, -1));
        context.declareFunction(new PredefinedFunction(NOT, Function.UNARY_PRIORITY, 1));
        context.declareFunction(new PredefinedFunction(XOR, Function.LOGICAL_PRIORITY, -1));

        // Relational operators
        context.declareFunction(new PredefinedFunction(GT, Function.RELATIONAL_PRIORITY, -1));
        context.declareFunction(new PredefinedFunction(LT, Function.RELATIONAL_PRIORITY, -1));
        context.declareFunction(new PredefinedFunction(EQ, Function.RELATIONAL_PRIORITY, -1));
        context.declareFunction(new PredefinedFunction(GEQ, Function.RELATIONAL_PRIORITY, -1));
        context.declareFunction(new PredefinedFunction(LEQ, Function.RELATIONAL_PRIORITY, -1));
        context.declareFunction(new PredefinedFunction(NEQ, Function.RELATIONAL_PRIORITY, 2));

        // Arithmetic operators
        context.declareFunction(new PredefinedFunction(PLUS, Function.PLUS_PRIORITY, -1));
        context.declareFunction(new PredefinedFunction(MINUS, Function.PLUS_PRIORITY, 2));
        context.declareFunction(new PredefinedFunction(UMINUS, Function.UNARY_PRIORITY, 1));
        context.declareFunction(new PredefinedFunction(TIMES, Function.TIMES_PRIORITY, -1));
        context.declareFunction(new PredefinedFunction(DIVIDE, Function.TIMES_PRIORITY, 2));
        context.declareFunction(new PredefinedFunction(MOD, Function.FUNCTION_PRIORITY, 2));
        context.declareFunction(new PredefinedFunction(POWER, Function.POWER_PRIORITY, 2));
        context.declareFunction(new PredefinedFunction(ROOT, Function.FUNCTION_PRIORITY, -1));
        context.declareFunction(new PredefinedFunction(ABS, Function.FUNCTION_PRIORITY, 1));
        context.declareFunction(new PredefinedFunction(EXP, Function.FUNCTION_PRIORITY, 1));
        context.declareFunction(new PredefinedFunction(LN, Function.FUNCTION_PRIORITY, 1));
        context.declareFunction(new PredefinedFunction(LOG, Function.FUNCTION_PRIORITY, -1));
        
        context.declareFunction(new PredefinedFunction(SQRT, Function.FUNCTION_PRIORITY, 1));
        
        context.declareFunction(new PredefinedFunction(MAX, Function.FUNCTION_PRIORITY, -1));
        context.declareFunction(new PredefinedFunction(MIN, Function.FUNCTION_PRIORITY, -1));

        context.declareFunction(new PredefinedFunction(DIFF, Function.FUNCTION_PRIORITY, -1));
        context.declareFunction(new PredefinedFunction(ASSIGNMENT, Function.ASSIGNMENT_PRIORITY, 2));

        context.declareFunction(new PredefinedFunction(SIN, Function.FUNCTION_PRIORITY, 1));
        context.declareFunction(new PredefinedFunction(COS, Function.FUNCTION_PRIORITY, 1));
        context.declareFunction(new PredefinedFunction(TAN, Function.FUNCTION_PRIORITY, 1));
        context.declareFunction(new PredefinedFunction(COT, Function.FUNCTION_PRIORITY, 1));
        context.declareFunction(new PredefinedFunction(ASIN, Function.FUNCTION_PRIORITY, 1));
        context.declareFunction(new PredefinedFunction(ACOS, Function.FUNCTION_PRIORITY, 1));
        context.declareFunction(new PredefinedFunction(ATAN, Function.FUNCTION_PRIORITY, 1));

        context.declareFunction(new PredefinedFunction( ARCCOSH, Function.FUNCTION_PRIORITY, 1));
        context.declareFunction(new PredefinedFunction( ARCCOT, Function.FUNCTION_PRIORITY, 1));
        context.declareFunction(new PredefinedFunction( ARCCOTH, Function.FUNCTION_PRIORITY, 1));
        context.declareFunction(new PredefinedFunction( ARCSCS, Function.FUNCTION_PRIORITY, 1));
        context.declareFunction(new PredefinedFunction( ARCCSCH, Function.FUNCTION_PRIORITY, 1));
        context.declareFunction(new PredefinedFunction( ARCSEC, Function.FUNCTION_PRIORITY, 1));
        context.declareFunction(new PredefinedFunction( ARCSECH, Function.FUNCTION_PRIORITY, 1));
        context.declareFunction(new PredefinedFunction( ARCTANH, Function.FUNCTION_PRIORITY, 1));
        context.declareFunction(new PredefinedFunction( COSH, Function.FUNCTION_PRIORITY, 1));
        context.declareFunction(new PredefinedFunction( COTH, Function.FUNCTION_PRIORITY, 1));
        context.declareFunction(new PredefinedFunction( CSC, Function.FUNCTION_PRIORITY, 1));
        context.declareFunction(new PredefinedFunction( CSCH, Function.FUNCTION_PRIORITY, 1));
        context.declareFunction(new PredefinedFunction( SEC, Function.FUNCTION_PRIORITY, 1));
        context.declareFunction(new PredefinedFunction( SECH, Function.FUNCTION_PRIORITY, 1));
        context.declareFunction(new PredefinedFunction( SINH, Function.FUNCTION_PRIORITY, 1));
        context.declareFunction(new PredefinedFunction( TANH, Function.FUNCTION_PRIORITY, 1));
        context.declareFunction(new PredefinedFunction( ARCSINH, Function.FUNCTION_PRIORITY, 1));

        context.declareFunction(new PredefinedFunction( FLOOR, Function.FUNCTION_PRIORITY, 1));
        context.declareFunction(new PredefinedFunction( CEIL, Function.FUNCTION_PRIORITY, 1));
        context.declareFunction(new PredefinedFunction( FACTORIAL, Function.FUNCTION_PRIORITY, 1));
        context.declareFunction(new PredefinedFunction( DELAY, Function.FUNCTION_PRIORITY, 2));
        context.declareFunction(new PredefinedFunction( RATE_OF, Function.FUNCTION_PRIORITY, 1));
        
        context.declareFunction(new PredefinedFunction( REM, Function.FUNCTION_PRIORITY, 2));
        context.declareFunction(new PredefinedFunction( QUOTIENT, Function.FUNCTION_PRIORITY, 2));
        
        context.declareFunction(new PredefinedFunction( "NUMERIC_LT", Function.FUNCTION_PRIORITY, 2));
        context.declareFunction(new PredefinedFunction( "NUMERIC_GT", Function.FUNCTION_PRIORITY, 2));
        context.declareFunction(new PredefinedFunction( "NUMERIC_EQ", Function.FUNCTION_PRIORITY, 2));
        context.declareFunction(new PredefinedFunction( "NUMERIC_LEQ", Function.FUNCTION_PRIORITY, 2));
        context.declareFunction(new PredefinedFunction( "NUMERIC_GEQ", Function.FUNCTION_PRIORITY, 2));
        context.declareFunction(new PredefinedFunction( "NUMERIC_NEQ", Function.FUNCTION_PRIORITY, 2));
        
        context.declareFunction(new PredefinedFunction( "NUMERIC_AND", Function.FUNCTION_PRIORITY, 2));
        context.declareFunction(new PredefinedFunction( "NUMERIC_OR", Function.FUNCTION_PRIORITY, 2));
        context.declareFunction(new PredefinedFunction( "NUMERIC_XOR", Function.FUNCTION_PRIORITY, 2));
        context.declareFunction(new PredefinedFunction( "NUMERIC_NOT", Function.FUNCTION_PRIORITY, 1));
        
    }
    
    @Override
    public boolean canDeclare(String value)
    {
        return true;
    }
}