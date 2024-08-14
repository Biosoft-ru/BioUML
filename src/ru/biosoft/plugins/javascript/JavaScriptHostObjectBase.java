package ru.biosoft.plugins.javascript;

import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

public abstract class JavaScriptHostObjectBase
{
    protected Throwable lastError;
    protected Map<String, Script> scriptText2CompiledScript = new HashMap<>();
    
    protected Map<Long, Context> threadIdToContext = new HashMap<>();
    protected Scriptable scope;
    
    public String getLastError()
    {
        if (lastError != null)
            return lastError.getMessage();
        return null;
    }

    protected void setLastError(Throwable lastError)
    {
        this.lastError = lastError;
    }

    protected void beforeFunctionCall()
    {
        lastError = null;
    }
    
    protected Context getCurrentContext()
    {
        long currentThreadId = Thread.currentThread().getId();

        Context cx;

        if (!threadIdToContext.containsKey(currentThreadId))
        {
            cx = Context.enter();
            ImporterTopLevel g = new ImporterTopLevel();
            scope = cx.initStandardObjects(g);
            threadIdToContext.put(currentThreadId, cx);
        }
        else
        {
            cx = threadIdToContext.get(currentThreadId);
        }

        return cx;
    }
    
    protected void defineColumnVariables(DataElement rowDE)
    {
        JavaScriptUtils.defineColumnVariables(rowDE, scope, false);
    }
    
    protected DataCollection resolveDataCollection(Object source)
    {
        DataElement de = null;
        
        if (source instanceof String)
        {
            de = CollectionFactory.getDataElement((String)source);
        }
        else if (source instanceof DataElement)
        {
            de = (DataElement)source;
        }
        else
        {
            return null;
        }
        
        if (!(de instanceof DataCollection))
            return null;
        
        return (DataCollection)de;
    }
}
