package ru.biosoft.treetable;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Collections;
import java.util.List;

import javax.swing.Action;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import com.developmentontheedge.beans.model.Property;
import com.developmentontheedge.beans.swing.ValueEditor;
import com.developmentontheedge.beans.swing.ValueRenderer;
import com.developmentontheedge.beans.swing.treetable.JTreeTable;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.subaction.DynamicActionFactory;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.GUI;

/**
 * Display tree classification with table in one document (tree table)
 */
public class TreeTableDocument extends Document
{
    private static boolean actionInitialized;
    private JTreeTable treeTable;

    public TreeTableDocument(TreeTableElement model)
    {
        super(model);

        final DataCollection root = model.getTree();
        treeTable = new JTreeTable(new TreeTableModel(model));
        treeTable.setShowGrid(true);
        treeTable.setRowHeight((int) ( treeTable.getRowHeight() * 1.5 ));
        treeTable.setIntercellSpacing(new Dimension(2, 2));
        treeTable.getTree().setCellRenderer(new DefaultTreeCellRenderer()
        {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row,
                    boolean hasFocus)
            {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

                DataElementPath path = (DataElementPath)value;
                String text = path.getName();
                setText(text);
                setOpaque( !sel);
                return this;
            }
        });

        treeTable.setDefaultRenderer(Property.class, new ValueRenderer());
        treeTable.setDefaultEditor(Property.class, new ValueEditor());


        treeTable.getTree().addTreeSelectionListener(e -> {
            if(treeTable.getTree().getSelectionPaths() == null || treeTable.getTree().getSelectionPaths().length == 0) return;
            TreePath treePath = treeTable.getTree().getSelectionPaths()[0];
            if( treePath != null )
            {
                Object obj = treePath.getLastPathComponent();
                if( obj instanceof DataElementPath )
                {
                    DataElementPath path = (DataElementPath)obj;
                    String pathStr = path.toString();
                    DataElement de = path.optDataElement();
                    if( de == null && path.isDescendantOf(DataElementPath.create(root)) )
                    {
                        if( path.getDepth() == 1 )
                            de = root;
                        else
                            de = CollectionFactory.getDataElement(
                                    pathStr.substring(pathStr.indexOf(DataElementPath.PATH_SEPARATOR) + 1), root);
                    }
                    if( de != null )
                    {
                        GUI.getManager().explore( de );
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(treeTable);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        viewPane = new ViewPane();
        viewPane.add(scrollPane);
    }

    @Override
    public String getDisplayName()
    {
        return ( (TreeTableElement)getModel() ).getName();
    }

    @Override
    public Action[] getActions(ActionType actionType)
    {
        //ActionManager actionManager = Application.getActionManager();
        if( !actionInitialized )
        {
            actionInitialized = true;
        }
        if( actionType == ActionType.TOOLBAR_ACTION )
        {
            List<Action> actions = DynamicActionFactory.getEnabledActions(getModel());
            return actions.toArray(new Action[actions.size()]);
        }
        return null;
    }
    
    @Override
    public List<DataElement> getSelectedItems()
    {
        TreePath[] selectionPaths = treeTable.getTree().getSelectionPaths();
        if(selectionPaths == null) return Collections.emptyList();
        return StreamEx.of( selectionPaths ).map( TreePath::getLastPathComponent ).select( DataElementPath.class )
                .map( DataElementPath::optDataElement ).nonNull().toList();
    }
}
