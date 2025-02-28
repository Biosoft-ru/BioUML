package ru.biosoft.table.document.editors;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.gui.Document;
import ru.biosoft.table.MessageBundle;
import ru.biosoft.table.TableDataCollection;
import com.developmentontheedge.beans.swing.TabularPropertyInspector;
import com.developmentontheedge.application.action.ActionInitializer;

@SuppressWarnings ( "serial" )
public class ColumnsViewPane extends JPanel
{
    static final Logger log = Logger.getLogger(ColumnsViewPane.class.getName());

    protected TableDataCollection tableData;
    protected TabularPropertyInspector table;
    protected JScrollPane scrollPane;

    protected Document document;

    public static final String RECALCULATE_DOCUMENT_ACTION = "ColumnsViewPane.RecalculateDocumentAction";
    public static final String ADD_COLUMN_ACTION = "ColumnsViewPane.AddColumnAction";
    public static final String REMOVE_COLUMN_ACTION = "ColumnsViewPane.RemoveColumnAction";

    protected Action[] actions;
    protected Action addColumnAction = new AddColumnAction(ADD_COLUMN_ACTION);
    protected Action removeColumnAction = new RemoveColumnAction(REMOVE_COLUMN_ACTION);

    protected TableElement selectedColumn;
    protected TableElement[] columns;

    public ColumnsViewPane()
    {
        super();

        setLayout(new GridBagLayout());

        table = new TabularPropertyInspector();
        table.getTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSortEnabled(false);
        table.addPropertyChangeListener(evt -> {
            if( document != null )
                document.update();
        });
        table.addListSelectionListener(event -> {
            Object model = table.getModelOfSelectedRow();
            if( model instanceof TableElement )
            {
                selectedColumn = (TableElement)model;
                if( selectedColumn.getRow() >= 0 )
                {
                    removeColumnAction.setEnabled(true);
                }
                else
                {
                    removeColumnAction.setEnabled(false);
                }
            }
        });
        scrollPane = new JScrollPane(table);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane, new GridBagConstraints(0, 0, 2, 1, 1.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(10, 0,
                0, 0), 0, 0));

        removeColumnAction.setEnabled(false);
    }

    public void explore(TableDataCollection me, Document document)
    {
        this.tableData = me;
        this.document = document;

        selectedColumn = null;
        removeColumnAction.setEnabled(false);

        columns = new TableElement[me.getColumnModel().getColumnCount() + 1];
        columns[0] = new TableElement(me, -1, document);
        for( int i = 0; i < me.getColumnModel().getColumnCount(); i++ )
        {
            columns[i + 1] = new TableElement(me, i, document);
        }
        table.explore(columns);
    }

    protected void addColumnAction()
    {
        try
        {
            tableData.getColumnModel().addColumn(tableData.getColumnModel().generateUniqueColumnName(), String.class);
            selectedColumn = null;
            removeColumnAction.setEnabled(false);

            explore(tableData, document);
            this.document.update();
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "can't create new column", t);
        }
    }

    protected void removeColumnAction()
    {
        if( selectedColumn != null && selectedColumn.getRow() >= 0
                && JOptionPane.showConfirmDialog(this, "Do you really want to remove column with all data") == JOptionPane.OK_OPTION )
        {
            tableData.getColumnModel().removeColumn(selectedColumn.getRow());
            explore(tableData, document);
            this.document.update();
        }
    }

    public Action[] getActions()
    {
        if( actions == null )
        {
            ActionInitializer initializer = new ActionInitializer(MessageBundle.class);
            initializer.initAction(addColumnAction, ColumnsViewPane.ADD_COLUMN_ACTION);
            initializer.initAction(removeColumnAction, ColumnsViewPane.REMOVE_COLUMN_ACTION);

            actions = new Action[] {addColumnAction, removeColumnAction};
        }

        return actions;
    }

    //actions
    private class AddColumnAction extends AbstractAction
    {
        public AddColumnAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            addColumnAction();
        }
    }

    private class RemoveColumnAction extends AbstractAction
    {
        public RemoveColumnAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            removeColumnAction();
        }
    }
}