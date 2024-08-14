package ru.biosoft.plugins.javascript;

import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollectionUtils;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentFactory.Policy;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;

public class JavaScriptUtils
{
    public static final String ID_ATTR = "ID";

    private static final class RowScriptable extends ScriptableObject
    {
        private static final long serialVersionUID = 1L;
        @Override
        public String getClassName()
        {
            return "Columns";
        }
    }

    public static String getValidName (String name)
    {
        String validName = Pattern.compile("[\\W]+").matcher(name).replaceAll("_");
        if(Pattern.compile("^\\d").matcher(validName).find())
        {
            validName = "_" + validName;
        }
        return validName;
    }
    
    private final static class BuckScriptable extends ScriptableObject
    {
        private static final long serialVersionUID = 1L;
        @Override
        public String getClassName()
        {
            return "WorkflowVariables";
        }
    }
    
    public static Scriptable defineVariables(ScriptableObject scope, Map<String, Object> variables)
    {
        Scriptable buck = new BuckScriptable();
        scope.defineProperty( "$", buck, ScriptableObject.DONTENUM );
        for( Entry<String, Object> var : variables.entrySet() )
        {
            Object val = var.getValue() == null ? "" : Context.toObject(var.getValue(), scope);
            scope.put(JavaScriptUtils.getValidName(var.getKey()), scope, val);
            buck.put(var.getKey(), buck, val);
        }
        return buck;
    }
    
    public static void defineColumnVariables(DataElement rowDE, Scriptable scope, boolean addEvaluated)
    {
        Scriptable buck = new RowScriptable();
        scope.put("$", scope, buck);
        if(rowDE instanceof RowDataElement)
        {
            RowDataElement rde = (RowDataElement)rowDE;
            scope.put(ID_ATTR, scope, rde.getName());
            buck.put(ID_ATTR, buck, rde.getName());
            Object[] values = rde.getValues(addEvaluated);
            String[] columns = TableDataCollectionUtils.getColumnNames(rde.getOrigin());
            for(int i=0; i<columns.length; i++)
            {
                String varName = JavaScriptUtils.getValidName(columns[i]);
                Object val = values[i] == null?"":Context.toObject(values[i], scope);
                scope.put(varName, scope, val);
                buck.put(columns[i], buck, val);
            }
            return;
        }
        ComponentModel cm = ComponentFactory.getModel( rowDE, Policy.DEFAULT, true );
        for (int i = 0; i < cm.getPropertyCount(); ++i)
        {
            Property prop = cm.getPropertyAt(i);
            String varName = JavaScriptUtils.getValidName(prop.getDisplayName());
            Scriptable varValue = prop.getValue() == null?null:Context.toObject(prop.getValue(), scope);
            scope.put(varName, scope, varValue);
            buck.put(prop.getName(), buck, varValue);
            if(prop.getName().equals("name")) //ID column name corresponds to name property of RowDataElement
            {
                scope.put(ID_ATTR, scope, varValue);
                buck.put(ID_ATTR, buck, varValue);
            }
        }
    }
}
