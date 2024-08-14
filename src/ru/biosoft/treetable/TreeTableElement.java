package ru.biosoft.treetable;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.ScriptableObject;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.plugins.javascript.JScriptContext;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * Model for {@link TreeTableDocument}
 */
@ClassIcon("resources/treetable.gif")
@PropertyName("tree-table")
public class TreeTableElement extends DataElementSupport
{
    protected static final Logger log = Logger.getLogger(TreeTableElement.class.getName());

    protected DataElementPath treePath;
    protected String tableScript;
    protected boolean hideBranchesAbsentInTable;

    public TreeTableElement(String name, DataCollection<?> origin)
    {
        super(name, origin);
    }

    public DataElementPath getTreePath()
    {
        return treePath;
    }

    public void setTreePath(DataElementPath treePath)
    {
        this.treePath = treePath;
    }

    public String getTableScript()
    {
        return tableScript;
    }

    public void setTableScript(String tableScript)
    {
        this.tableScript = tableScript;
    }

    /**
     * Get model tree as {@link ru.biosoft.access.core.DataCollection} element
     */
    public DataCollection getTree()
    {
        return treePath.getDataCollection();
    }

    /**
     * Get model table as {@link ru.biosoft.access.core.DataCollection} element
     */
    private DataCollection table = null;
    public synchronized DataCollection getTable()
    {
        if(table == null)
        {
            try
            {
                Context context = JScriptContext.getContext();
                ScriptableObject scope = JScriptContext.getScope();
                Object obj = context.evaluateString(scope, tableScript, "", 0, null);
                table = (DataCollection) ( (NativeJavaObject)obj ).unwrap();
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Incorrect table script", e);
            }
        }
        return table;
    }
    
    public boolean isHideBranchesAbsentInTable()
    {
        return hideBranchesAbsentInTable;
    }

    public void setHideBranchesAbsentInTable(boolean hideBranchesAbsentInTable)
    {
        this.hideBranchesAbsentInTable = hideBranchesAbsentInTable;
    }
}
