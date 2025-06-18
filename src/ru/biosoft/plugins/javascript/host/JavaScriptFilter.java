package ru.biosoft.plugins.javascript.host;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.mozilla.javascript.Script;

import com.developmentontheedge.beans.model.ComponentFactory;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.filter.Filter;
import ru.biosoft.access.core.filter.FilteredDataCollection;
import ru.biosoft.plugins.javascript.JavaScriptHostObjectBase;
import ru.biosoft.util.TextUtil2;

public class JavaScriptFilter extends JavaScriptHostObjectBase
{
    public DataCollection byValue(Object source, String property, String values)
    {
        DataCollection dc = resolveDataCollection(source);
        if (dc == null)
        {
            setLastError(new Throwable("Cannot resolve source to ru.biosoft.access.core.DataCollection"));
            return null;
        }
        
        Set<String> valuesSet = new HashSet<>(Arrays.asList(values.split(",")));
        ByValueFilter filter = new ByValueFilter(property, valuesSet);
        
        return createFilteredDC(dc, filter);
    }
    
    public DataCollection byExpression(Object source, String expression)
    {
        DataCollection dc = resolveDataCollection(source);
        if (dc == null)
        {
            setLastError(new Throwable("Cannot resolve source to ru.biosoft.access.core.DataCollection"));
            return null;
        }
        
        Script script;
        
        if (!scriptText2CompiledScript.containsKey(expression))
        {
            try
            {
                script = getCurrentContext().compileString(expression, "", 1, null);
                scriptText2CompiledScript.put(expression, script);
            }
            catch (Throwable ex)
            {
                setLastError(ex);
                return null;
            }
        }
        else
        {
            script = scriptText2CompiledScript.get(expression);
        }
        
        ByExpressionFilter filter = new ByExpressionFilter(script);
        return createFilteredDC(dc, filter);
    }
    
    public DataCollection bySet(Object source, Object filterSource)
    {
        DataCollection dc = resolveDataCollection(source);
        if (dc == null)
        {
            setLastError(new Throwable("Cannot resolve source to ru.biosoft.access.core.DataCollection"));
            return null;
        }
        
        DataCollection filterDc = resolveDataCollection(filterSource);
        if (filterDc == null)
        {
            setLastError(new Throwable("Cannot resolve filterSource to ru.biosoft.access.core.DataCollection"));
            return null;
        }
        
        ByDataCollectionFilter filter = new ByDataCollectionFilter(filterDc);
        return createFilteredDC(dc, filter);
    }
    
    DataCollection createFilteredDC(DataCollection dc, Filter filter)
    {
        FilteredDataCollection fdc = new FilteredDataCollection(null, "",
                dc, filter, null, new Properties());
        
        return fdc;
    }
    
    private static class ByValueFilter implements Filter<DataElement>
    {
        String property;
        boolean isKey;
        Set<String> valuesSet;
        
        public ByValueFilter(String property, Set<String> valuesSet)
        {
            super();
            this.property = property;
            isKey = TextUtil2.isEmpty(property);
            this.valuesSet = valuesSet;
        }

        @Override
        public boolean isAcceptable(DataElement de)
        {
            String val;
            
            if (isKey)
            {
                val = de.getName();
            }
            else
            {
                val = ComponentFactory.getModel( de ).findProperty(this.property).getValue().toString();
            }
            
            return valuesSet.contains(val);
        }

        @Override
        public boolean isEnabled()
        {
            return true;
        }
    }
    
    private class ByExpressionFilter implements Filter<DataElement>
    {
        Script script;

        public ByExpressionFilter(Script script)
        {
            super();
            this.script = script;
        }

        @Override
        public boolean isAcceptable(DataElement de)
        {
            defineColumnVariables(de);
            Object result = script.exec(getCurrentContext(), scope);
            return Boolean.valueOf(result.toString());
        }

        @Override
        public boolean isEnabled()
        {
            return true;
        }
    }
    
    private static class ByDataCollectionFilter implements Filter<DataElement>
    {
        DataCollection filterDc;
        
        public ByDataCollectionFilter(DataCollection filterDc)
        {
            this.filterDc = filterDc;
        }

        @Override
        public boolean isAcceptable(DataElement de)
        {
            return this.filterDc.contains(de);
        }

        @Override
        public boolean isEnabled()
        {
            return true;
        }
    }
}