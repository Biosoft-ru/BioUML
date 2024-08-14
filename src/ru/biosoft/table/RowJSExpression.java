package ru.biosoft.table;

import java.util.concurrent.atomic.AtomicReference;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

import ru.biosoft.access.BiosoftSecurityManager;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.plugins.javascript.JScriptContext;

/**
 * Represents JavaScript expression to be executed per each table row
 * @author lan
 *
 */
public class RowJSExpression
{
    public static final String ID_ATTR = "ID";

    private Script script;
    private ExpressionScope scope;
    
    // TODO: add these functions in more pretty way
    private static final String COMMON_FUNC = "function any(c,p){if(!c.iterator) return p(c.toString());var i = c.iterator();while(i.hasNext()){if(p(i.next().toString())) return true;} return false;};"+
        "function all(c,p){if(!c.iterator) return p(c.toString());var i = c.iterator();while(i.hasNext()){if(p(i.next().toString())) return true;} return false;};";

    public RowJSExpression(String filterStr, DataCollection dc) throws IllegalArgumentException
    {
        Context context = JScriptContext.getContext();
        try
        {
            script = context.compileString(COMMON_FUNC+filterStr, "", 1, null);
        }
        catch( Exception e )
        {
            throw new IllegalArgumentException("Expression compilation error: "+e.getMessage());
        }
        checkErrors(dc);
    }

    public Object evaluate(DataElement rowDataElement)
    {
        Context context = JScriptContext.getContext();
        scope.setDataElement(rowDataElement);
        AtomicReference<Object> result = new AtomicReference<>();
        BiosoftSecurityManager.runInSandbox( ()->{
            result.set( script.exec(context, scope) );
        } );
        return convertResult( result.get() );
    }

    private Object convertResult(Object result)
    {
        if(result instanceof NativeJavaObject) return ((NativeJavaObject)result).unwrap();
        if(result instanceof NativeJavaArray) return ((NativeJavaArray)result).unwrap();
        if(result instanceof Scriptable) return ((Scriptable)result).getDefaultValue(null);
        return result;
    }

    private void checkErrors(DataCollection<?> dc) throws IllegalArgumentException
    {
        DataElement de = dc.stream().findAny().orElse( null );
        if(de == null)
            return;
        scope = new ExpressionScope(de, true);
        Context context = JScriptContext.getContext();
        scope.initStandardObjects(context, false);
        try
        {
            BiosoftSecurityManager.runInSandbox( () -> script.exec(context, scope) );
        }
        catch( Exception e )
        {
            throw new IllegalArgumentException("Expression execution error: "+e.getMessage());
        }
    }
}
