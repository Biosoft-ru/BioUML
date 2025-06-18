package biouml.plugins.research.workflow.items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import ru.biosoft.util.TextUtil2;
import biouml.model.Compartment;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;

public class VariablesTreeModel implements TreeModel
{
    private Compartment compartment;
    private SortedMap<String, WorkflowVariable> vars;
    private List<String> varNames = new ArrayList<>();

    public VariablesTreeModel(Compartment d)
    {
        this.compartment = d;
        initVariables();
    }
    
    private void initVariables()
    {
        vars = new TreeMap<>(WorkflowItemFactory.getVariables(compartment));
        varNames.addAll(vars.keySet());
    }
    
    public Property getProperty(String path)
    {
        String bean, property;
        int pos = path.indexOf("/");
        if( pos > -1 )
        {
            bean = path.substring(0, pos);
            property = path.substring(pos + 1);
        }
        else
        {
            bean = path;
            property = null;
        }
        WorkflowVariable var = vars.get(bean);
        Object value = null;
        try
        {
            value = var.getValue();
        }
        catch( Exception e )
        {
        }
        if( value == null )
            return null;
        ComponentModel model = ComponentFactory.getModel(value);
        return property == null ? model : model.findProperty(property);
    }
    
    @Override
    public void addTreeModelListener(TreeModelListener l)
    {
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l)
    {
    }

    @Override
    public Object getChild(Object parent, int index)
    {
        if(parent.equals("")) return varNames.get(index);
        Property property = getProperty(parent.toString()).getPropertyAt(index);
        return property == null?null:parent+"/"+property.getName();
    }
    @Override
    public int getChildCount(Object parent)
    {
        if(parent.equals("")) return vars.size();
        Property property = getProperty(parent.toString());
        return property == null?0:property.getPropertyCount();
    }

    @Override
    public int getIndexOfChild(Object parent, Object child)
    {
        if(parent == null || child == null) return -1;
        if(parent.equals(""))
        {
            int index = Collections.binarySearch(varNames, child.toString());
            return index >=0 ?index:-1;
        }
        Property property = getProperty(parent.toString());
        if(property == null) return -1;
        String[] fields = TextUtil2.split( child.toString(), '/' );
        String childName = fields[fields.length-1];
        for(int i=0; i<property.getPropertyCount(); i++)
        {
            if(property.getPropertyAt(i).getName().equals(childName)) return i;
        }
        return -1;
    }

    @Override
    public Object getRoot()
    {
        return "";
    }

    @Override
    public boolean isLeaf(Object obj)
    {
        if(obj.equals("")) return false;
        Property property = getProperty(obj.toString());
        return property == null?true:property.getPropertyCount() == 0;
    }

    @Override
    public void valueForPathChanged(TreePath arg0, Object arg1)
    {
    }
}