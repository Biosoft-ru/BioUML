package biouml.model.dynamics;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import one.util.streamex.StreamEx;

public class MathOperations
{
    private final HashMap<String, Method> operations = new HashMap<>();
    private final Set<String> comparisons = new TreeSet<>();

    public MathOperations()
    {
        init();
    }

    public Method getMethod(String name)
    {
        return operations.get(name);
    }

    public boolean contains(String name)
    {
        return operations.containsKey(name);
    }

    public boolean isComparison(String name)
    {
        return comparisons.contains(name);
    }

    private void init()
    {
        initComparisonSet();
        initMathLibraryOperations();
        initSimpleOperations();
    }

    private void initComparisonSet()
    {
        comparisons.add("=");
        comparisons.add(">");
        comparisons.add("<");
    }

    private void initMathLibraryOperations()
    {
        Method[] methods = Math.class.getMethods();

        for( Method method : methods )
        {
            if( checkParameterTypes(method.getParameterTypes()) )
            {
                String name = method.getName();
                if( name.equals("pow") )
                    name = "^";
                operations.put(name, method);
            }
        }
    }

    private boolean checkParameterTypes(Class<?>[] types)
    {
        return StreamEx.of(types).allMatch( double.class::equals );
    }

    private void initSimpleOperations()
    {
        try
        {
            Method method = MathOperations.class.getMethod("plus", new Class[] {double.class, double.class});
            operations.put("+", method);

            method = MathOperations.class.getMethod("minus", new Class[] {double.class, double.class});
            operations.put("-", method);

            method = MathOperations.class.getMethod("multiplication", new Class[] {double.class, double.class});
            operations.put("*", method);

            method = MathOperations.class.getMethod("division", new Class[] {double.class, double.class});
            operations.put("/", method);

            method = MathOperations.class.getMethod("unaryMinus", new Class[] {double.class});
            operations.put("u-", method);

            method = MathOperations.class.getMethod("equality", new Class[] {double.class, double.class});
            operations.put("=", method);

            method = MathOperations.class.getMethod("less", new Class[] {double.class, double.class});
            operations.put("<", method);

            method = MathOperations.class.getMethod("greater", new Class[] {double.class, double.class});
            operations.put(">", method);
            
            method = MathOperations.class.getMethod("delay", new Class[] {double.class, double.class});
            operations.put("delay", method);
           
            operations.put("log", MathOperations.class.getMethod("log", new Class[] {double.class, double.class}));
            operations.put("ln", MathOperations.class.getMethod("ln", new Class[] {double.class}));
        }
        catch( NoSuchMethodException exc )
        {
            throw new IllegalArgumentException(exc);
        }
    }

    public static double plus(double arg1, double arg2)
    {
        return arg1 + arg2;
    }

    public static double minus(double arg1, double arg2)
    {
        return arg1 - arg2;
    }

    public static double multiplication(double arg1, double arg2)
    {
        return arg1 * arg2;
    }

    public static double division(double arg1, double arg2)
    {
        return arg1 / arg2;
    }

    public static double unaryMinus(double arg1)
    {
        return -arg1;
    }

    public static boolean equality(double arg1, double arg2)
    {
        return arg1 == arg2;
    }

    public static boolean less(double arg1, double arg2)
    {
        return arg1 < arg2;
    }

    public static boolean greater(double arg1, double arg2)
    {
        return arg1 > arg2;
    }
    
    public static double delay(double arg1, double arg2)
    {
        return arg1;
    }
    
    public static double log(double arg1, double arg2)
    {
        return ln(arg2)/ln(arg1);
    }
    
    public static double ln(double arg)
    {
        return Math.log( arg );
    }
}
