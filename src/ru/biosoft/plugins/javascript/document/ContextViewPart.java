package ru.biosoft.plugins.javascript.document;

import java.awt.BorderLayout;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.table.TableModel;

import ru.biosoft.gui.Document;
import ru.biosoft.gui.ViewPartSupport;
import ru.biosoft.plugins.javascript.JSElement;

public class ContextViewPart extends ViewPartSupport implements ChangeListener
{
    protected ContextTreeTable treeTable;
    protected WatchTable watchTable;

    public ContextViewPart()
    {
        treeTable = new ContextTreeTable(new VariableModel());
        JScrollPane scrollPane = new JScrollPane(treeTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        watchTable = new WatchTable();
        JScrollPane scrollPane2 = new JScrollPane(watchTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, scrollPane2);
        splitPane.setDividerLocation(400);
        add(splitPane, BorderLayout.CENTER);
    }

    @Override
    public JComponent getView()
    {
        return this;
    }

    @Override
    public void explore(Object model, Document document)
    {
        if( document instanceof JSDocument )
        {
            this.model = model;
            this.document = document;
            stateChanged();
            watchTable.setTableModel(new WatchTable.MyTableModel( ( (JSDocument)document ).getJSPanel()));
            ( (JSDocument)document ).getJSPanel().addChangeListener(this);
        }
    }
    @Override
    public boolean canExplore(Object model)
    {
        if( ( model instanceof JSElement ) )
            return true;
        return false;
    }

    @Override
    public Action[] getActions()
    {
        return new Action[0];
    }

    @Override
    public void stateChanged()
    {
        //context
        Dim.ContextData contextData = ( (JSDocument)document ).getJSPanel().getDim().currentContextData();
        if( contextData == null )
        {
            treeTable.resetTree(new VariableModel());
            return;
        }

        Dim.StackFrame frame = contextData.getFrame(0);
        Object thisObj = frame.thisObj();
        treeTable.resetTree(new VariableModel( ( (JSDocument)document ).getJSPanel().getDim(), thisObj));
        
        //watch
        TableModel model = watchTable.getModel();
        if( model != null )
        {
            ( (WatchTable.MyTableModel)model ).updateModel();
        }
    }
}
