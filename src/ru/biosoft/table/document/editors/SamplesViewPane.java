package ru.biosoft.table.document.editors;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import java.util.logging.Logger;

import ru.biosoft.gui.Document;
import ru.biosoft.table.MessageBundle;
import ru.biosoft.table.Sample;
import ru.biosoft.table.TableDataCollection;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.application.action.ActionInitializer;

@SuppressWarnings ( "serial" )
public class SamplesViewPane extends JPanel
{
    protected Logger log = Logger.getLogger(SamplesViewPane.class.getName());

    protected TableDataCollection tableData;
    protected JTable table;
    protected JScrollPane scrollPane;

    private Document document;

    public static final String FIRST_COLUMN_NAME = "Series";

    public static final String ADD_COLUMN_ACTION = "SamplesViewPane.AddColumnAction";
    public static final String REMOVE_COLUMN_ACTION = "SamplesViewPane.RemoveColumnAction";

    protected Action[] actions;
    protected Action addColumnAction = new AddColumnAction(ADD_COLUMN_ACTION);
    protected Action removeColumnAction = new RemoveColumnAction(REMOVE_COLUMN_ACTION);

    public SamplesViewPane()
    {
        super();

        setLayout(new GridBagLayout());
        table = new JTable();
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        table.addPropertyChangeListener(evt -> {
            if( document != null )
                document.update();
        });

        scrollPane = new JScrollPane(table);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane, new GridBagConstraints(0, 0, 2, 1, 1.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(10, 0,
                0, 0), 0, 0));
    }

    public void explore(TableDataCollection me, Document document)
    {
        this.tableData = me;
        this.document = document;

        refreshData();
    }

    public void refreshData()
    {
        table.setModel(new SamplesTableModel());
    }

    protected void addColumnAction()
    {
        if( tableData.getSamples().getSize() > 0 )
        {
            String propertyName = JOptionPane.showInputDialog("Enter property name");
            for(Sample sample : tableData.getSamples())
            {
                try
                {
                    sample.getAttributes().add(new DynamicProperty(propertyName, String.class));
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE, "can't create new column", t);
                }
            }
            refreshData();
        }
        else
        {
            JOptionPane.showMessageDialog(null, "This table contains no SAMPLE columns");
        }
    }

    protected void removeColumnAction()
    {
        String propertyName = getPropertyList().get(table.getSelectedColumn() - 1);
        for(Sample sample : tableData.getSamples())
        {
            try
            {
                sample.getAttributes().remove(propertyName);
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "can't delete column", t);
            }
        }
        refreshData();
    }

    private class SamplesTableModel extends AbstractTableModel
    {
        @Override
        public int getColumnCount()
        {
            return 1 + getPropertyList().size();
        }

        @Override
        public int getRowCount()
        {
            return tableData.getSamples().getSize();
        }

        @Override
        public Object getValueAt(int row, int col)
        {
            if( col == 0 )
            {
                return tableData.getSamples().getNameList().get(row);
            }
            else
            {
                String pName = getPropertyList().get(col - 1);
                try
                {
                    Sample sample = tableData.getSamples().get((String)getValueAt(row, 0));
                    return sample.getAttributes().getValue(pName);
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE, "can't get sample value", t);
                }
            }
            return "";
        }

        @Override
        public String getColumnName(int col)
        {
            if( col == 0 )
            {
                return FIRST_COLUMN_NAME;
            }
            else
            {
                return getPropertyList().get(col - 1);
            }
        }

        @Override
        public boolean isCellEditable(int row, int col)
        {
            if( col == 0 )
                return false;
            return true;
        }

        @Override
        public void setValueAt(Object aValue, int row, int col)
        {
            if( col != 0 )
            {
                String pName = getPropertyList().get(col - 1);
                try
                {
                    Sample sample = tableData.getSamples().get((String)getValueAt(row, 0));
                    sample.getAttributes().setValue(pName, aValue);
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE, "can't set sample value", t);
                }
            }
        }
    }

    private List<String> getPropertyList()
    {
        List<String> result = new ArrayList<>();
        for(Sample sample : tableData.getSamples())
        {
            Iterator<String> iter2 = sample.getAttributes().nameIterator();
            while( iter2.hasNext() )
            {
                result.add(iter2.next());
            }
            break;
        }

        return result;
    }

    public Action[] getActions()
    {
        if( actions == null )
        {
            ActionInitializer initializer = new ActionInitializer(MessageBundle.class);
            initializer.initAction(addColumnAction, SamplesViewPane.ADD_COLUMN_ACTION);
            initializer.initAction(removeColumnAction, SamplesViewPane.REMOVE_COLUMN_ACTION);

            actions = new Action[] {addColumnAction, removeColumnAction};
        }

        return actions;
    }

    public class AddColumnAction extends AbstractAction
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

    public class RemoveColumnAction extends AbstractAction
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
