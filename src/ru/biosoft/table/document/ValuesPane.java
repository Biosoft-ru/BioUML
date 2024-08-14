package ru.biosoft.table.document;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Properties;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.ListSelectionListener;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.filter.Filter;
import ru.biosoft.access.core.filter.FilteredDataCollection;
import ru.biosoft.access.support.DataCollectionRowModelAdapter;
import ru.biosoft.gui.Document;
import ru.biosoft.table.TableDataCollection;

import com.developmentontheedge.beans.swing.PagedTabularPropertyInspector;
import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.swing.table.ColumnModel;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationFrame;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;

public class ValuesPane extends JPanel
{
    private static final int[] PAGE_SIZE = new int[] {50, 100, 200, 500, 1000};

    public static final String SERIES_ATTR = "series";
    public static final String ID_ATTR = "ID";

    protected DataCollection tableData;
    protected DataCollection filteredTableData;
    protected Filter rowFilter;
    protected PagedTabularPropertyInspector tabularFeatureInspector;
    protected Document document;
    protected DataCollectionRowModelAdapter rowModel;
    private FunctionJobControl jobControl = null;
    private ValueListThread currentThread;

    /**
     * Get column model for data collection
     */
    protected static ColumnModel getColumnModel(DataCollection dc)
    {
        ColumnModel columnModel;
        if( dc instanceof TableDataCollection )
        {
            columnModel = ( (TableDataCollection)dc ).getColumnModel().getSwingColumnModel();
        }
        else if( dc.getSize() > 0 )
        {
            columnModel = new ColumnModel(dc.iterator().next());
        }
        else
        {
            columnModel = new ColumnModel(dc.getDataElementType(), PropertyInspector.SHOW_USUAL);
        }
        return columnModel;
    }

    public ValuesPane(DataCollection tableData)
    {
        super();
        this.tableData = tableData;
        this.filteredTableData = null;

        setLayout(new GridBagLayout());

        tabularFeatureInspector = new PagedTabularPropertyInspector(PAGE_SIZE);
        //        tabularFeatureInspector.getTable().setRowSelectionAllowed(false);
        tabularFeatureInspector.getTable().setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tabularFeatureInspector.getTable().getTableHeader().setReorderingAllowed(false);
        tabularFeatureInspector.setVariableRowHeight(true);

        rowModel = new DataCollectionRowModelAdapter(tableData);
        ColumnModel columnModel = getColumnModel(tableData);
        tabularFeatureInspector.explore(rowModel, columnModel);

        add(tabularFeatureInspector, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.BOTH,
                new Insets(5, 0, 0, 0), 0, 0));
    }

    public void setDocument(Document document)
    {
        this.document = document;
    }

    public void setRowFilter(Filter rowFilter)
    {
        if( this.rowFilter != rowFilter )
        {
            this.rowFilter = rowFilter;
            filteredTableData = null;
            setValueList(tableData);
        }
    }

    public Filter getRowFilter()
    {
        return rowFilter;
    }

    public void setValueList(DataCollection tableData)
    {
        this.tableData = tableData;

        if( jobControl != null )
            jobControl.terminate();

        if( currentThread != null )
            currentThread.stopThread();
        
        if( rowFilter == null )
        {
            DataCollection filteredTable = getFilteredTable();
            if( rowModel != null )
            {
                rowModel.unregisterListener();
                rowModel = null;
            }
            rowModel = new DataCollectionRowModelAdapter(filteredTable);
            ColumnModel columnModel = getColumnModel(tableData);
            firePropertyChange("filteringStatus", JobControl.RUNNING, JobControl.COMPLETED);
            tabularFeatureInspector.explore(rowModel, columnModel);
        }
        else
        {
            currentThread = new ValueListThread(tableData, tabularFeatureInspector);
            currentThread.start();
        }
    }

    public synchronized DataCollection getFilteredTable()
    {
        if( rowFilter == null )
        {
            filteredTableData = tableData;
        }
        else
        {
            ApplicationFrame frame = Application.getApplicationFrame();
            if( frame != null )
            {
                jobControl = new FunctionJobControl(null);
                jobControl.addListener(frame.getStatusBar());
            }
            filteredTableData = new FilteredDataCollection(null, "", tableData, rowFilter, jobControl, new Properties());
        }
        return filteredTableData;
    }

    public DataCollection getCurrentFilteredTable()
    {
        if( filteredTableData != null )
            return filteredTableData;
        else
            return getFilteredTable();
    }

    public Object[] getSelectedRows()
    {
        int[] rowNumbers = tabularFeatureInspector.getTable().getSelectedRows();
        Object[] rows = new Object[rowNumbers.length];
        for( int i = 0; i < rowNumbers.length; i++ )
        {
            rows[i] = tabularFeatureInspector.getModelForRow(rowNumbers[i]);
        }
        return rows;
    }
    
    public void addSelectionListener(ListSelectionListener listener)
    {
        tabularFeatureInspector.getTable().getSelectionModel().addListSelectionListener(listener);
    }
    
    public void removeSelectionListener(ListSelectionListener listener)
    {
        tabularFeatureInspector.getTable().getSelectionModel().removeListSelectionListener(listener);
    }
    
    public void setSelectionMode(int selectionMode)
    {
        tabularFeatureInspector.getTable().setSelectionMode( selectionMode );
    }

    public class ValueListThread extends Thread
    {
        private DataCollection table;
        private PagedTabularPropertyInspector tabularFeatureInspector;
        private boolean isRunning = false;

        public ValueListThread(DataCollection dc, PagedTabularPropertyInspector tpi)
        {
            table = dc;
            tabularFeatureInspector = tpi;
        }

        @Override
        public void run()
        {
            isRunning = true;
            DataCollection filteredTable = getFilteredTable();
            if( isRunning )
            {
                if( rowModel != null )
                {
                    rowModel.unregisterListener();
                    rowModel = null;
                }
                rowModel = new DataCollectionRowModelAdapter(filteredTable);
                ColumnModel columnModel = getColumnModel(table);
                firePropertyChange("filteringStatus", JobControl.RUNNING, JobControl.COMPLETED);
                tabularFeatureInspector.explore(rowModel, columnModel);
            }
        }

        public void stopThread()
        {
            isRunning = false;
        }
    }

    public Object getDocument()
    {
        return document;
    }
}
