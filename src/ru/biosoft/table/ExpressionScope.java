package ru.biosoft.table;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;
import com.developmentontheedge.beans.model.ComponentFactory.Policy;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.plugins.javascript.JavaScriptUtils;
import ru.biosoft.util.BeanUtil;

/**
 * Scope used for expressions
 * @author lan
 *
 */
@SuppressWarnings ( "serial" )
public class ExpressionScope extends ImporterTopLevel
{
    private DataElement de;
    private ColumnModel cm;
    private final boolean addEvaluated;
    private final Map<String, String> nameMap = new HashMap<>();
    private final Set<String> names = new HashSet<>();
    private final RowScriptable buck;
    
    private Object doGet(String name)
    {
        if(!doHas(name)) return null;
        Object value = null;
        if(de instanceof RowDataElement)
        {
            RowDataElement row = (RowDataElement)de;
            if(name.equals(JavaScriptUtils.ID_ATTR))
                value = row.getName();
            else
            {
                int index = cm.getColumnIndex(name);
                TableColumn column = cm.getColumn(index);
                value = row.getValues(addEvaluated && !column.isExpressionEmpty())[index];
            }
        } else
        {
            try
            {
                if(name.equals(JavaScriptUtils.ID_ATTR))
                    name = "name";
                value = BeanUtil.getBeanPropertyValue(de, name);
            }
            catch( Exception e )
            {
            }
        }
        return value == null?"":Context.toObject(value, this);
    }
    
    private boolean doHas(String name)
    {
        if(name == null) return false;
        return names.contains(name);
    }

    private final class RowScriptable extends ScriptableObject
    {
        private static final long serialVersionUID = 1L;
        @Override
        public String getClassName()
        {
            return "Columns";
        }
        
        @Override
        public Object get(String name, Scriptable start)
        {
            Object result = doGet(name);
            if(result != null)
                return result;
            return super.get(name, start);
        }
        @Override
        public boolean has(String name, Scriptable start)
        {
            return doHas(name) || super.has(name, start);
        }
    }

    public ExpressionScope(DataElement de, boolean addEvaluated)
    {
        this.de = de;
        this.addEvaluated = addEvaluated;
        if(de instanceof RowDataElement)
        {
            RowDataElement row = (RowDataElement)de;
            cm = row.getOrigin().getColumnModel();
            for(TableColumn column : cm)
            {
                String varName = JavaScriptUtils.getValidName(column.getName());
                names.add(column.getName());
                nameMap.put(varName, column.getName());
            }
        } else
        {
            ComponentModel cm = ComponentFactory.getModel( de, Policy.DEFAULT, true );
            for (int i = 0; i < cm.getPropertyCount(); ++i)
            {
                Property prop = cm.getPropertyAt(i);
                String varName = JavaScriptUtils.getValidName(prop.getDisplayName());
                names.add(prop.getName());
                nameMap.put(varName, prop.getName());
            }
        }
        names.add(JavaScriptUtils.ID_ATTR);
        nameMap.put(JavaScriptUtils.ID_ATTR, JavaScriptUtils.ID_ATTR);
        this.buck = new RowScriptable();
    }
    
    public void setDataElement(DataElement de)
    {
        this.de = de;
    }

    @Override
    public Object get(String name, Scriptable start)
    {
        if(name.equals("$"))
            return buck;
        Object result = doGet(nameMap.get(name));
        if(result != null)
            return result;
        return super.get(name, start);
    }

    @Override
    public boolean has(String name, Scriptable start)
    {
        return name.equals("$") || doHas(nameMap.get(name)) || super.has(name, start);
    }
}
