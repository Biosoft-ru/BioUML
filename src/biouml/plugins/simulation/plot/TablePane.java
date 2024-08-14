package biouml.plugins.simulation.plot;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import java.util.logging.Logger;
import org.jfree.chart.event.PlotChangeEvent;
import org.jfree.chart.event.PlotChangeListener;

import ru.biosoft.access.core.DataElementPath;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.simulation.plot.Plot;
import biouml.standard.simulation.plot.Series;

import com.developmentontheedge.beans.swing.table.RowModelEvent;
import com.developmentontheedge.beans.swing.table.RowModelListener;

@SuppressWarnings ( "serial" )
public class TablePane extends JPanel implements PropertyChangeListener, RowModelListener, PlotChangeListener
{
    protected static final Logger log = Logger.getLogger(TablePane.class.getName());
    protected JTable table;
    private JComboBox<ColumnNameAndPosition> allColumnsList;
    private JComboBox<ColumnNameAndPosition> visibleColumnsList;
    private SimulationResult seriesResult = null;
    private ArrayList<String> columnNames = new ArrayList<>();
    private JTextField lowerLimit;
    private JTextField upperLimit;
    private JTextField step;
    private boolean timeFilterInitialized = false;

    public TablePane(int ix, int iy)
    {
        super(new BorderLayout());

        try
        {
            JPanel filters = new JPanel();
            filters.setLayout(new BorderLayout());
            //--Columns filter--
            JPanel columnsFilterPanel = new JPanel(new GridLayout(2, 3));

            JLabel acll = new JLabel("All columns:");
            columnsFilterPanel.add(acll);

            allColumnsList = new JComboBox<>(new ColumnNameAndPosition[0]);
            columnsFilterPanel.add(allColumnsList);
            allColumnsList.setPreferredSize(new Dimension(150, 20));

            JButton dropToRightListButton = new JButton(new AbstractAction("Add")
            {
                @Override
                public void actionPerformed(ActionEvent arg0)
                {
                    int index = allColumnsList.getSelectedIndex();
                    if( 0 > index )
                        return;
                    ColumnNameAndPosition item = allColumnsList.getItemAt(index);
                    for( int i = 0; i < visibleColumnsList.getItemCount(); i++ )
                    {
                        if( item.equals(visibleColumnsList.getItemAt(i)) )
                        {
                            return;
                        }
                    }
                    visibleColumnsList.addItem(item);
                    drawSeries();
                }
            });
            columnsFilterPanel.add(dropToRightListButton);
            dropToRightListButton.setPreferredSize(new Dimension(30, 20));

            JLabel vcll = new JLabel("Visible columns:");
            columnsFilterPanel.add(vcll);

            visibleColumnsList = new JComboBox<>(new ColumnNameAndPosition[0]);
            columnsFilterPanel.add(visibleColumnsList);
            visibleColumnsList.setPreferredSize(new Dimension(150, 20));

            JButton removeButton = new JButton(new AbstractAction("Remove")
            {
                @Override
                public void actionPerformed(ActionEvent arg0)
                {
                    int index = visibleColumnsList.getSelectedIndex();
                    if( 0 > index )
                        return;
                    visibleColumnsList.removeItemAt(index);
                    drawSeries();
                }
            });
            columnsFilterPanel.add(removeButton);
            dropToRightListButton.setPreferredSize(new Dimension(50, 20));
            filters.add(columnsFilterPanel, BorderLayout.NORTH);

            //--Time filter--
            JPanel timeFilterPanel = new JPanel(new GridLayout(2, 4));

            JLabel lowerLimitLabel = new JLabel("Lower time limit:");
            timeFilterPanel.add(lowerLimitLabel);

            lowerLimit = new JTextField("0.0");
            timeFilterPanel.add(lowerLimit);
            lowerLimit.setPreferredSize(new Dimension(100, 20));
            

            JLabel upperLimitLabel = new JLabel("Upper time limit:");
            timeFilterPanel.add(upperLimitLabel);

            upperLimit = new JTextField("1.0");
            timeFilterPanel.add(upperLimit);
            upperLimit.setPreferredSize(new Dimension(100, 20));
            

            JLabel stepLabel = new JLabel("Time step:");
            timeFilterPanel.add(stepLabel);

            step = new JTextField("0.1");
            timeFilterPanel.add(step);
            step.setPreferredSize(new Dimension(100, 20));

            JButton applyFilter = new JButton(new AbstractAction("Apply time filter")
            {
                @Override
                public void actionPerformed(ActionEvent arg0)
                {
                    updateContents();
                }
            });
            timeFilterPanel.add(applyFilter);
            filters.add(timeFilterPanel, BorderLayout.SOUTH);
            filters.setBorder(new EmptyBorder(0, 5, 0, 0));
            
            add(filters, BorderLayout.NORTH);
            
            //Table
            table = new JTable(new String[][] {{""}}, new String[] {""});
            //table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            JScrollPane tablePane = new JScrollPane(table);
            tablePane.setPreferredSize(new Dimension(ix, iy));
            tablePane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            tablePane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            add(tablePane, BorderLayout.CENTER);
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Error occured while creating table panel: " + ex);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Properties
    //

    protected Plot plot;
    public Plot getPlot()
    {
        return plot;
    }

    public void setPlot(Plot plot)
    {
        Plot oldPlot = this.plot;
        this.plot = plot;
        if( oldPlot != null )
        {
            oldPlot.removePropertyChangeListener(this);
            oldPlot.getRowModel().removeRowModelListener(this);
        }
        if( this.plot != null )
        {
            this.plot.addPropertyChangeListener(this);
            this.plot.getRowModel().addRowModelListener(this);
        }

        seriesChanged = true;
    }

    private static class ColumnNameAndPosition
    {
        private String name;
        private int position;
        public ColumnNameAndPosition(String name, int position)
        {
            this.name = name;
            this.position = position;
        }
        public String getName()
        {
            return name;
        }
        public int getPosition()
        {
            return position;
        }
        @Override
        public String toString()
        {
            return name;
        }
        @Override
        public boolean equals(Object object)
        {
            if( object instanceof ColumnNameAndPosition )
            {
                ColumnNameAndPosition o = (ColumnNameAndPosition)object;
                if( null == name )
                    return null == o.getName();
                return name.equals(o.getName());
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            throw new UnsupportedOperationException();
        }
    }

    public void updateContents()
    {
        List<Series> series = plot.getSeries();
        if( series == null )
            return;

        Iterator<Series> iter = series.iterator();
        seriesResult = new SimulationResult(null, "");
        columnNames.clear();
        columnNames.add("Time");
        boolean firstTime = true;
        int rowCount = 0;
        int currentColumn = 0;
        allColumnsList.removeAllItems();
        visibleColumnsList.removeAllItems();
        allColumnsList.addItem(new ColumnNameAndPosition("Time", -1));
        visibleColumnsList.addItem(new ColumnNameAndPosition("Time", -1));
        if( iter != null )
        {
            while( iter.hasNext() )
            {
                Series s = iter.next();
                if( !s.getSourceNature().equals(Series.SourceNature.EXPERIMENTAL_DATA) )
                {
                    if( "time".equalsIgnoreCase(s.getYVar()) )
                        continue;
                    String columnName = s.getSource() + " " + s.getYVar();
                    columnNames.add(columnName);
                    allColumnsList.addItem(new ColumnNameAndPosition(columnName, currentColumn));
                    visibleColumnsList.addItem(new ColumnNameAndPosition(columnName, currentColumn));
                    try
                    {
                        if( s.getSourceNature().equals(Series.SourceNature.SIMULATION_RESULT) )
                        {
                            SimulationResult sResult = DataElementPath.create(s.getSource()).getDataElement(SimulationResult.class);
                            if( timeFilterInitialized )
                            {
                                double lowLim = Double.parseDouble(lowerLimit.getText());
                                double upLim = Double.parseDouble(upperLimit.getText());
                                double st = Double.parseDouble(step.getText());
                                sResult = sResult.approximate(lowLim, upLim, st);
                            }
                            double times[] = sResult.getTimes();
                            if( firstTime )
                            {
                                rowCount = sResult.getTimes().length;
                                seriesResult.setTimes(times);
                                try
                                {
                                    if( !timeFilterInitialized )
                                    {
                                        lowerLimit.setText(String.valueOf(times[0]));
                                        upperLimit.setText(String.valueOf(times[times.length - 1]));
                                        step.setText(String.valueOf(times[1] - times[0]));
                                        timeFilterInitialized = true;
                                    }
                                }
                                catch( Exception e )
                                {
                                }
                                double values[][] = new double[rowCount][series.size()];
                                seriesResult.setValues(values);
                                firstTime = false;
                            }
                            double values[][] = seriesResult.getValues();
                            double sValues[][] = sResult.getValues();
                            Map<String, Integer> vMap = sResult.getVariablePathMap();
                            String path = s.getYPath().isEmpty()? s.getYVar(): s.getYPath()+"/"+s.getYVar();
                            
//                            Map<String, Integer> vMap = sResult.getVariableMap();
                            Integer cc = vMap.get(path);
                            if( null != cc )
                            {
                                for( int i = 0; i < rowCount; i++ )
                                {
                                    values[i][currentColumn] = sValues[i][cc];
                                }
                            }
                            else
                            {
                                for( int i = 0; i < rowCount; i++ )
                                {
                                    values[i][currentColumn] = times[i];
                                }
                            }
                        }
                    }
                    catch( Exception e )
                    {
                        log.log(Level.SEVERE, "No source " + s.getSource() + " found in repository.");
                    }
                    currentColumn++;
                }
            }
        }
        try
        {
            allColumnsList.setSelectedIndex(0);
            visibleColumnsList.setSelectedIndex(0);
        }
        catch( Exception e )
        {
        }
        if( seriesChanged )
        {
            seriesChanged = false;
        }
        drawSeries();
    }

    private void drawSeries()
    {
        int colQ = visibleColumnsList.getItemCount();
        boolean timeColumnVisible = false;
        for( int i = 0; i < colQ; i++ )
            if( -1 == visibleColumnsList.getItemAt(i).getPosition() )
                timeColumnVisible = true;
        int requiredColumns[] = new int[timeColumnVisible ? colQ - 1 : colQ];
        int j = 0;
        for( int i = 0; i < colQ; i++ )
        {
            int pos = visibleColumnsList.getItemAt(i).getPosition();
            if( -1 == pos )
                continue;
            requiredColumns[j] = pos;
            j++;
        }
        redrawSeries(seriesResult, columnNames, requiredColumns, timeColumnVisible);
    }

    private void redrawSeries(final SimulationResult seriesResult, final ArrayList<String> columnNames, final int[] requiredColumns,
            final boolean timeColumnVisible)
    {
        try
        {
            table.setModel(new AbstractTableModel()
            {
                @Override
                public String getColumnName(int y)
                {
                    try
                    {
                        if( timeColumnVisible )
                        {
                            if( 0 == y )
                                return columnNames.get(0);
                            return columnNames.get(requiredColumns[y - 1] + 1);
                        }
                        return columnNames.get(requiredColumns[y] + 1);
                    }
                    catch( Exception e )
                    {
                    }
                    return "";
                }

                @Override
                public Object getValueAt(int x, int y)
                {
                    try
                    {
                        if( timeColumnVisible )
                        {
                            if( 0 == y )
                                return String.valueOf(seriesResult.getTimes()[x]);
                            return String.valueOf(seriesResult.getValues()[x][requiredColumns[y - 1]]);
                        }
                        return String.valueOf(seriesResult.getValues()[x][requiredColumns[y]]);
                    }
                    catch( Exception e )
                    {
                    }
                    return "";
                }

                @Override
                public int getRowCount()
                {
                    try
                    {
                        if( 0 >= seriesResult.getTimes().length )
                            return 0;
                        return seriesResult.getTimes().length;
                    }
                    catch( Exception e )
                    {
                    }
                    return 0;
                }

                @Override
                public int getColumnCount()
                {
                    try
                    {
                        if( 0 >= requiredColumns.length )
                            return timeColumnVisible ? 1 : 0;
                        return requiredColumns.length + ( timeColumnVisible ? 1 : 0 );
                    }
                    catch( Exception e )
                    {
                    }
                    return 0;
                }
            });
            for( int i = 0; i < table.getColumnCount(); i++ )
            {
                TableColumn tc = table.getColumn(table.getColumnName(i));
                tc.setPreferredWidth(String.valueOf(tc.getHeaderValue()).length() * 10);
            }
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Error occured while filling in chart data for series : \n" + ex);
        }
    }

    boolean seriesChanged = true;

    @Override
    public void propertyChange(PropertyChangeEvent arg0)
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void tableChanged(RowModelEvent e)
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void plotChanged(PlotChangeEvent arg0)
    {
        // TODO Auto-generated method stub
    }
}
