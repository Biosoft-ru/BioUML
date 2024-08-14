package ru.biosoft.plugins.javascript.document;

import java.awt.Dimension;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.EventObject;

import javax.swing.JTree;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import one.util.streamex.IntStreamEx;

import com.developmentontheedge.beans.swing.treetable.JTreeTable;
import com.developmentontheedge.beans.swing.treetable.TreeTableModel;
import com.developmentontheedge.beans.swing.treetable.TreeTableModelAdapter;

public class ContextTreeTable extends JTreeTable
{
    /**
     * Creates a new MyTreeTable.
     */
    public ContextTreeTable(VariableModel model)
    {
        super(model);
    }

    /**
     * Initializes a tree for this tree table.
     */
    public JTree resetTree(TreeTableModel treeTableModel)
    {
        tree = new TreeTableCellRenderer(treeTableModel);

        // Install a tableModel representing the visible rows in the tree.
        super.setModel(new TreeTableModelAdapter(treeTableModel, tree));

        // Force the JTable and JTree to share their row selection models.
        //ListToTreeSelectionModelWrapper selectionWrapper = new ListToTreeSelectionModelWrapper();
        //tree.setSelectionModel(selectionWrapper);
        //setSelectionModel(selectionWrapper.getListSelectionModel());

        // Make the tree and table row heights the same.
        if( tree.getRowHeight() < 1 )
        {
            // Metal looks better like this.
            setRowHeight(18);
        }

        // Install the tree editor renderer and editor.
        setDefaultRenderer(TreeTableModel.class, tree);
        setDefaultEditor(TreeTableModel.class, new TreeTableCellEditor());
        setShowGrid(true);
        setIntercellSpacing(new Dimension(1, 1));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        DefaultTreeCellRenderer r = (DefaultTreeCellRenderer)tree.getCellRenderer();
        r.setOpenIcon(null);
        r.setClosedIcon(null);
        r.setLeafIcon(null);
        return tree;
    }

    /**
     * Returns whether the cell under the coordinates of the mouse
     * in the {@link EventObject} is editable.
     */
    public boolean isCellEditable(EventObject e)
    {
        if( e instanceof MouseEvent )
        {
            MouseEvent me = (MouseEvent)e;
            // If the modifiers are not 0 (or the left mouse button),
            // tree may try and toggle the selection, and table
            // will then try and toggle, resulting in the
            // selection remaining the same. To avoid this, we
            // only dispatch when the modifiers are 0 (or the left mouse
            // button).
            if( me.getModifiers() == 0
                    || ( ( me.getModifiers() & ( InputEvent.BUTTON1_MASK | 1024 ) ) != 0 && ( me.getModifiers() & ( InputEvent.SHIFT_MASK
                            | InputEvent.CTRL_MASK | InputEvent.ALT_MASK | InputEvent.BUTTON2_MASK | InputEvent.BUTTON3_MASK | 64 | //SHIFT_DOWN_MASK
                            128 | //CTRL_DOWN_MASK
                            512 | // ALT_DOWN_MASK
                            2048 | //BUTTON2_DOWN_MASK
                    4096 //BUTTON3_DOWN_MASK
                    ) ) == 0 ) )
            {
                int row = rowAtPoint(me.getPoint());
                for( int counter = getColumnCount() - 1; counter >= 0; counter-- )
                {
                    if( TreeTableModel.class == getColumnClass(counter) )
                    {
                        MouseEvent newME = new MouseEvent(ContextTreeTable.this.tree, me.getID(), me.getWhen(), me.getModifiers(), me.getX()
                                - getCellRect(row, counter, true).x, me.getY(), me.getClickCount(), me.isPopupTrigger());
                        ContextTreeTable.this.tree.dispatchEvent(newME);
                        break;
                    }
                }
            }
            if( me.getClickCount() >= 3 )
            {
                return true;
            }
            return false;
        }
        if( e == null )
        {
            return true;
        }
        return false;
    }
}

/**
 * Tree model for script object inspection.
 */
class VariableModel implements TreeTableModel
{

    /**
     * Serializable magic number.
     */
    private static final String[] cNames = {" Name", " Value"};

    /**
     * Tree column types.
     */
    private static final Class[] cTypes = {TreeTableModel.class, String.class};

    /**
     * Empty {@link VariableNode} array.
     */
    private static final VariableNode[] CHILDLESS = new VariableNode[0];

    /**
     * The debugger.
     */
    private Dim debugger;

    /**
     * The root node.
     */
    private VariableNode root;

    /**
     * Creates a new VariableModel.
     */
    public VariableModel()
    {
    }

    /**
     * Creates a new VariableModel.
     */
    public VariableModel(Dim debugger, Object scope)
    {
        this.debugger = debugger;
        this.root = new VariableNode(scope, "this");
    }

    // TreeTableModel

    /**
     * Returns the root node of the tree.
     */
    @Override
    public Object getRoot()
    {
        if( debugger == null )
        {
            return null;
        }
        return root;
    }

    /**
     * Returns the number of children of the given node.
     */
    @Override
    public int getChildCount(Object nodeObj)
    {
        if( debugger == null )
        {
            return 0;
        }
        VariableNode node = (VariableNode)nodeObj;
        return children(node).length;
    }

    /**
     * Returns a child of the given node.
     */
    @Override
    public Object getChild(Object nodeObj, int i)
    {
        if( debugger == null )
        {
            return null;
        }
        VariableNode node = (VariableNode)nodeObj;
        return children(node)[i];
    }

    /**
     * Returns whether the given node is a leaf node.
     */
    @Override
    public boolean isLeaf(Object nodeObj)
    {
        if( debugger == null )
        {
            return true;
        }
        VariableNode node = (VariableNode)nodeObj;
        return children(node).length == 0;
    }

    /**
     * Returns the index of a node under its parent.
     */
    @Override
    public int getIndexOfChild(Object parentObj, Object childObj)
    {
        if( debugger == null )
        {
            return -1;
        }
        VariableNode parent = (VariableNode)parentObj;
        VariableNode child = (VariableNode)childObj;
        VariableNode[] children = children(parent);
        return IntStreamEx.ofIndices( children, ch -> ch == child ).findFirst().orElse( -1 );
    }

    /**
     * Returns whether the given cell is editable.
     */
    @Override
    public boolean isCellEditable(Object node, int column)
    {
        return column == 0;
    }

    /**
     * Sets the value at the given cell.
     */
    @Override
    public void setValueAt(Object value, Object node, int column)
    {
    }

    /**
     * Adds a TreeModelListener to this tree.
     */
    @Override
    public void addTreeModelListener(TreeModelListener l)
    {
    }

    /**
     * Removes a TreeModelListener from this tree.
     */
    @Override
    public void removeTreeModelListener(TreeModelListener l)
    {
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue)
    {
    }

    // TreeTableNode

    /**
     * Returns the number of columns.
     */
    @Override
    public int getColumnCount()
    {
        return cNames.length;
    }

    /**
     * Returns the name of the given column.
     */
    @Override
    public String getColumnName(int column)
    {
        return cNames[column];
    }

    /**
     * Returns the type of value stored in the given column.
     */
    @Override
    public Class getColumnClass(int column)
    {
        return cTypes[column];
    }

    /**
     * Returns the value at the given cell.
     */
    @Override
    public Object getValueAt(Object nodeObj, int column)
    {
        if( debugger == null )
        {
            return null;
        }
        VariableNode node = (VariableNode)nodeObj;
        switch( column )
        {
            case 0: // Name
                return node.toString();
            case 1: // Value
                String result;
                try
                {
                    result = debugger.objectToString(getValue(node));
                }
                catch( RuntimeException exc )
                {
                    result = exc.getMessage();
                }
                StringBuffer buf = new StringBuffer();
                int len = result.length();
                for( int i = 0; i < len; i++ )
                {
                    char ch = result.charAt(i);
                    if( Character.isISOControl(ch) )
                    {
                        ch = ' ';
                    }
                    buf.append(ch);
                }
                return buf.toString();
        }
        return null;
    }

    /**
     * Returns an array of the children of the given node.
     */
    private VariableNode[] children(VariableNode node)
    {
        if( node.children != null )
        {
            return node.children;
        }

        VariableNode[] children;

        Object value = getValue(node);
        Object[] ids = debugger.getObjectIds(value);
        if( ids == null || ids.length == 0 )
        {
            children = CHILDLESS;
        }
        else
        {
            Arrays.sort(ids, (l, r) -> {
                if( l instanceof String )
                {
                    if( r instanceof Integer )
                    {
                        return -1;
                    }
                    return ( (String)l ).compareToIgnoreCase((String)r);
                }
                else
                {
                    if( r instanceof String )
                    {
                        return 1;
                    }
                    int lint = ( (Integer)l ).intValue();
                    int rint = ( (Integer)r ).intValue();
                    return lint - rint;
                }
            });
            children = new VariableNode[ids.length];
            for( int i = 0; i != ids.length; ++i )
            {
                children[i] = new VariableNode(value, ids[i]);
            }
        }
        node.children = children;
        return children;
    }

    /**
     * Returns the value of the given node.
     */
    public Object getValue(VariableNode node)
    {
        try
        {
            return debugger.getObjectProperty(node.object, node.id);
        }
        catch( Exception exc )
        {
            return "undefined";
        }
    }

    /**
     * A variable node in the tree.
     */
    private static class VariableNode
    {

        /**
         * The script object.
         */
        private final Object object;

        /**
         * The object name.  Either a String or an Integer.
         */
        private final Object id;

        /**
         * Array of child nodes.  This is filled with the properties of
         * the object.
         */
        private VariableNode[] children;

        /**
         * Creates a new VariableNode.
         */
        public VariableNode(Object object, Object id)
        {
            this.object = object;
            this.id = id;
        }

        /**
         * Returns a string representation of this node.
         */
        @Override
        public String toString()
        {
            return id instanceof String ? (String)id : "[" + ( (Integer)id ).intValue() + "]";
        }
    }
}