package biouml.plugins.microarray;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import one.util.streamex.StreamEx;

import biouml.model.Diagram;
import biouml.model.DiagramFilter;

public class FilterListTab extends JPanel
{
    protected JTable filterTable = new JTable();

    protected Diagram diagram;

    public static final String FILTER = "Filter";
    public static final String COMMENT = "Comment";

    public FilterListTab()
    {
        GridBagLayout gridbag = new GridBagLayout();
        setLayout(gridbag);

        filterTable.setModel(new FilterListTableModel());
        filterTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane scrollPane = new JScrollPane(filterTable);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0, 0, 0,
                0), 0, 0));
    }

    public void refreshDiagram(Diagram diagram)
    {
        this.diagram = diagram;
        filterTable.setModel(new FilterListTableModel());
    }

    public void removeFilterAction()
    {
        int row = filterTable.getSelectedRow();
        if( row != -1 )
        {
            DiagramFilter filter = getMicroarrayFilters().get(row);
            DiagramFilter[] filters = diagram.getFilterList();
            DiagramFilter[] newFilters = StreamEx.of( filters ).without( filter ).toArray( DiagramFilter[]::new );
            if(newFilters.length != filters.length)
            {
                diagram.setFilterList(newFilters);
                filterTable.setModel(new FilterListTableModel());
            }
        }
    }

    public MicroarrayFilter getCurrentFilter()
    {
        int row = filterTable.getSelectedRow();
        if( row != -1 )
        {
            return getMicroarrayFilters().get(row);
        }
        return null;
    }

    private class FilterListTableModel extends AbstractTableModel
    {
        @Override
        public int getColumnCount()
        {
            return 2;
        }
        @Override
        public int getRowCount()
        {
            if( diagram != null )
            {
                return getMicroarrayFilters().size();
            }
            return 0;
        }
        @Override
        public Object getValueAt(int row, int col)
        {
            if( diagram == null )
            {
                return "";
            }
            if( col == 0 )
            {
                return getMicroarrayFilters().get(row).getName();
            }
            else if( col == 1 )
            {
                return getMicroarrayFilters().get(row).getComment();
            }
            return "";
        }

        @Override
        public void setValueAt(Object value, int row, int col)
        {
            if( col == 1 )
            {
                getMicroarrayFilters().get(row).setComment((String)value);
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            if( columnIndex == 1 )
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
                return FILTER;
            }
            else if( col == 1 )
            {
                return COMMENT;
            }
            return "";
        }
    }
    private List<MicroarrayFilter> getMicroarrayFilters()
    {
        List<MicroarrayFilter> result = new ArrayList<>();
        for( DiagramFilter filter : diagram.getFilterList() )
        {
            if( filter instanceof MicroarrayFilter )
            {
                result.add((MicroarrayFilter)filter);
            }
        }
        return result;
    }
}
