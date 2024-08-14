package biouml.plugins.microarray;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import biouml.model.Diagram;
import biouml.model.DiagramFilter;
import biouml.standard.filter.HighlightAction;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.graphics.Brush;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;

@SuppressWarnings ( "serial" )
public class FilterTab extends JPanel
{
    protected JTable filterTable = new JTable();

    protected Diagram diagram;
    protected MicroarrayFilter currentFilter;
    protected MicroarrayFilter newFilter;

    protected JComboBox<String> filterComboBox = new JComboBox<>();
    protected JComboBox<String> experimentComboBox = new JComboBox<>();
    protected JComboBox<String> columnsComboBox = new JComboBox<>();
    protected JComboBox<String> actionsComboBox = new JComboBox<>();

    protected JButton addAction = new JButton("Add");

    public static final String EXPERIMENT_ID = "Experiment";
    public static final String COLUMN = "Column";
    public static final String ACTION = "Action";
    public static final String COMMENT = "Comment";

    public static final String NEW_FILTER_NAME = "<new filter>";

    public FilterTab()
    {
        GridBagLayout gridbag = new GridBagLayout();
        setLayout(gridbag);

        add(new JLabel(" Filter: "), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));

        filterComboBox.addItemListener(e -> changeFilter());
        add(filterComboBox, new GridBagConstraints(1, 0, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0,
                0, 0), 0, 0));

        add(new JLabel(" Experiment: "), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

        refreshExperiments();
        experimentComboBox.addItemListener(e -> refreshColumns());
        add(experimentComboBox, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));

        add(new JLabel(" Column: "), new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));

        refreshColumns();
        add(columnsComboBox, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,
                0, 0, 0), 0, 0));

        add(new JLabel(" Action: "), new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));

        actionsComboBox.addItem("Highlight");
        add(actionsComboBox, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));

        addAction.addActionListener(e -> addAction());
        add(addAction, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));

        filterTable.setModel(new FilterTableModel());
        filterTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane scrollPane = new JScrollPane(filterTable);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, new GridBagConstraints(0, 3, 4, 1, 1.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0, 0, 0,
                0), 0, 0));
    }

    public void refreshDiagram(Diagram diagram)
    {
        this.diagram = diagram;
        refreshFilters();
    }

    protected void refreshFilters()
    {
        newFilter = null;
        filterComboBox.removeAllItems();
        if( diagram.getFilterList().length != 0 )
        {
            currentFilter = null;
            for( DiagramFilter filter : diagram.getFilterList() )
            {
                if( filter instanceof MicroarrayFilter )
                {
                    if( currentFilter == null )
                    {
                        currentFilter = (MicroarrayFilter)filter;
                    }
                    filterComboBox.addItem( ( (MicroarrayFilter)filter ).getName());
                }
            }
        }
        else
        {
            newFilter = new MicroarrayFilter(NEW_FILTER_NAME, diagram);
            currentFilter = newFilter;
            filterComboBox.addItem(currentFilter.getName());
        }
        filterTable.setModel(new FilterTableModel());
    }

    protected void refreshExperiments()
    {
        experimentComboBox.removeAllItems();
        DataCollection<?> microarrays = CollectionFactory.getDataCollection("data/microarray");
        if( microarrays != null )
        {
            microarrays.names().forEach( experimentComboBox::addItem );
        }
    }

    protected void refreshColumns()
    {
        columnsComboBox.removeAllItems();
        DataCollection<?> microarrays = CollectionFactory.getDataCollection("data/microarray");
        if( microarrays == null )
            return;
        TableDataCollection me;
        try
        {
            me = (TableDataCollection)microarrays.get((String)experimentComboBox.getSelectedItem());
        }
        catch( Throwable e )
        {
            return;
        }
        if( me == null )
            return;
        
        for (TableColumn col : me.getColumnModel())
        {
            columnsComboBox.addItem(col.getName());
        }
    }

    protected void addAction()
    {
        currentFilter.addElement((String)experimentComboBox.getSelectedItem(), (String)columnsComboBox.getSelectedItem(),
                new HighlightAction(new Brush(new Color(0, 0, 0))), "");
        filterTable.setModel(new FilterTableModel());
    }

    public void removeAction()
    {
        int row = filterTable.getSelectedRow();
        if( row != -1 )
        {
            currentFilter.removeElement(row);
            filterTable.setModel(new FilterTableModel());
        }
    }

    public void newFilterAction()
    {
        newFilter = new MicroarrayFilter(NEW_FILTER_NAME, diagram);
        currentFilter = newFilter;
        filterComboBox.removeAllItems();
        filterComboBox.addItem(currentFilter.getName());
        if( diagram.getFilterList().length != 0 )
        {
            for( DiagramFilter filter : diagram.getFilterList() )
            {
                if( filter instanceof MicroarrayFilter )
                {
                    filterComboBox.addItem( ( (MicroarrayFilter)filter ).getName());
                }
            }
        }
        filterTable.setModel(new FilterTableModel());
    }

    public void saveFilterAction()
    {
        String name = JOptionPane.showInputDialog("Filter name:");
        if( name != null && name.length() > 0 && !name.equals(NEW_FILTER_NAME) )
        {
            if( !currentFilter.getName().equals(NEW_FILTER_NAME) )
            {
                currentFilter = (MicroarrayFilter)currentFilter.clone();
            }
            currentFilter.setName(name);
            DiagramFilter[] filters = diagram.getFilterList();
            DiagramFilter[] newFilters = new DiagramFilter[filters.length + 1];
            System.arraycopy(filters, 0, newFilters, 0, filters.length);
            newFilters[filters.length] = currentFilter;
            diagram.setFilterList(newFilters);
            refreshFilters();
        }
    }

    public void changeFilter()
    {
        String filterName = (String)filterComboBox.getSelectedItem();
        if( filterName != null && filterName.equals(NEW_FILTER_NAME) )
        {
            currentFilter = newFilter;
        }
        else
        {
            for( DiagramFilter filter : diagram.getFilterList() )
            {
                if( filter instanceof MicroarrayFilter && ( (MicroarrayFilter)filter ).getName().equals(filterName) )
                {
                    currentFilter = (MicroarrayFilter)filter;
                    break;
                }
            }
        }
        filterTable.setModel(new FilterTableModel());
    }

    public MicroarrayFilter getCurrentFilter()
    {
        return currentFilter;
    }

    protected String getActionNameByClass(Class<?> actionClass)
    {
        if( actionClass.getName().equals(HighlightAction.class.getName()) )
        {
            return "Highlight";
        }
        return "";
    }

    private class FilterTableModel extends AbstractTableModel
    {
        @Override
        public int getColumnCount()
        {
            return 4;
        }
        @Override
        public int getRowCount()
        {
            if( currentFilter != null )
            {
                return currentFilter.getElements().length;
            }
            return 0;
        }
        @Override
        public Object getValueAt(int row, int col)
        {
            if( currentFilter == null )
            {
                return "";
            }
            if( col == 0 )
            {
                return currentFilter.getElements()[row].getExperimentID();
            }
            else if( col == 1 )
            {
                return currentFilter.getElements()[row].getColumn();
            }
            else if( col == 2 )
            {
                return getActionNameByClass(currentFilter.getElements()[row].getAction().getClass());
            }
            else if( col == 3 )
            {
                return currentFilter.getElements()[row].getComment();
            }
            return "";
        }

        @Override
        public void setValueAt(Object value, int row, int col)
        {
            if( col == 3 )
            {
                currentFilter.getElements()[row].setComment((String)value);
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            if( columnIndex == 3 )
            {
                return true;
            }
            return false;
        }

        @Override
        public String getColumnName(int col)
        {
            if( col == 0 )
            {
                return EXPERIMENT_ID;
            }
            else if( col == 1 )
            {
                return COLUMN;
            }
            else if( col == 2 )
            {
                return ACTION;
            }
            else if( col == 3 )
            {
                return COMMENT;
            }
            return "";
        }
    }
}
