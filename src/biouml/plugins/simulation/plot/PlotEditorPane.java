package biouml.plugins.simulation.plot;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSplitPane;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionListener;

import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.swing.TabularPropertyInspector;

import biouml.plugins.simulation.resources.MessageBundle;
import biouml.standard.diagram.Util;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.simulation.plot.Plot;
import biouml.standard.simulation.plot.Series;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathField;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;

@SuppressWarnings ( "serial" )
public class PlotEditorPane extends JPanel
{
    // Logging issues
    protected static final Logger log = Logger.getLogger(PlotEditorPane.class.getName());

    protected MessageBundle messageBundle = (MessageBundle)ResourceBundle.getBundle(MessageBundle.class.getName());

    protected PlotEx plotEx;
    protected PropertyInspector plotInspector = new PropertyInspector();
    protected TabularPropertyInspector tabSeriesInspector = new TabularPropertyInspector();

    protected DataElementPathField resultPath = null;
    protected JComboBox<String> xPath = new JComboBox<>();
    protected JComboBox<VariableItem> xVariable = new JComboBox<>();
    protected JComboBox<String> yPath = new JComboBox<>();
    protected JComboBox<VariableItem> yVariable = new JComboBox<>();
    protected DataElementPathField experimentPath = null;
    protected JComboBox<String> plotType = new JComboBox<>();

    protected JButton addButton = new JButton();
    protected JButton removeButton = new JButton();

    protected JRadioButton resultNameRB = new JRadioButton();
    protected JRadioButton experimentNameRB = new JRadioButton();
    
    public PlotEditorPane()
    {
        createInterfaceElements();
        initListeners();
    }
    
    public PlotEditorPane(PlotEx plotEx)
    {
        createInterfaceElements();
        initListeners();

        setPlot(plotEx, true);
    }
    
    public void setPlot(PlotEx plotEx)
    {
        setPlot(plotEx, false);
    }
    
    public void setPlot(PlotEx plotEx, boolean mode)
    {
        this.plotEx = plotEx;
        updateInterfaceElements(mode);
        registerListeners();
        
        if( plotEx.hasDefaultResult() )
        {
            resultPath.setPath(DataElementPath.create(plotEx.getDefaultSimulationResult()));
            resultNameRB.setSelected(true);
            updateVariableCombos();
        }
    }

    protected Border createBorder(String key)
    {
        Border loweredetched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        return BorderFactory.createTitledBorder(loweredetched, messageBundle.getResourceString(key));
    }

    protected void createInterfaceElements()
    {
        setLayout(new BorderLayout());
        
        JPanel upperPane = new JPanel();
        upperPane.setLayout(new GridBagLayout());
        upperPane.setBorder(new EmptyBorder(5, 5, 5, 5));

        //--- Add series pane  ---
        JPanel seriesPane = new JPanel(new GridBagLayout());
        seriesPane.setBorder(createBorder("PLOT_SERIES_BORDER_TITLE"));
        upperPane.add(seriesPane, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0, 0, 0,
                0), 0, 0));

        seriesPane.add(resultNameRB, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(1, 0, 0, 0), 0, 0));
        seriesPane.add(experimentNameRB, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(1, 0, 0, 0), 0, 0));

        seriesPane.add(new JLabel(messageBundle.getResourceString("PLOT_SIMULATION_RESULT")), new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(1, 2, 0, 0), 0, 0));
        seriesPane.add(new JLabel(messageBundle.getResourceString("PLOT_EXPERIMENTAL_DATA")), new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(1, 2, 0, 0), 0, 0));
        seriesPane.add(new JLabel(messageBundle.getResourceString("PLOT_X_VARIABLE")), new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(1, 2, 0, 0), 0, 0));
        seriesPane.add(new JLabel(messageBundle.getResourceString("PLOT_Y_VARIABLE")), new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(1, 2, 0, 0), 0, 0));

        resultPath = new DataElementPathField("resultPath", SimulationResult.class, messageBundle
                .getResourceString("CLICK_SIMULATION_RESULT"), null, true);
        seriesPane.add(resultPath, new GridBagConstraints(2, 1, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(2, 2, 0, 0), 0, 0));
        experimentPath = new DataElementPathField("experimentPath", TableDataCollection.class, messageBundle
                .getResourceString("CLICK_EXPERIMENT"), null, true);
        seriesPane.add(experimentPath, new GridBagConstraints(2, 2, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(1, 3, 0, 0), 0, 0));
        seriesPane.add(xPath, new GridBagConstraints(2, 3, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(1, 2, 0, 0), 0, 0));
        seriesPane.add(xVariable, new GridBagConstraints(3, 3, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(1, 2, 0, 0), 0, 0));
        seriesPane.add(yPath, new GridBagConstraints(2, 4, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(1, 2, 0, 0), 0, 0));
        seriesPane.add(yVariable, new GridBagConstraints(3, 4, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(1, 2, 0, 0), 0, 0));

        xPath.addItemListener( new ItemListener()
        {            
            @Override
            public void itemStateChanged(ItemEvent e)
            {
                if( resultNameRB.isSelected() && resultPath.getPath() != null )
                {
                    SimulationResult result = plotEx.getSimulationResult( resultPath.getPath() );
                    List<String> availableVariables = getVariablesForPath( e.getItem().toString(), result );
                    xVariable.removeAllItems();
                    for( String var : availableVariables )
                    {
                        VariableItem item = new VariableItem( var );

                        xVariable.addItem( item );
                        if( var.equals( "time" ) )
                            xVariable.setSelectedItem( item );
                    }
                }
            }
        } );
        
        yPath.addItemListener( new ItemListener()
        {            
            @Override
            public void itemStateChanged(ItemEvent e)
            {
                if( resultNameRB.isSelected() && resultPath.getPath() != null )
                {
                    yVariable.removeAllItems();
                    SimulationResult result = plotEx.getSimulationResult( resultPath.getPath() );
                    List<String> availableVariables = getVariablesForPath( e.getItem().toString(), result );
                    for( String var : availableVariables )
                        yVariable.addItem( new VariableItem( var ) );
                    yVariable.setSelectedIndex( 0 );
                }
            }
        } );
        
        // "Add" and "Remove" series buttons
        addButton.setText(messageBundle.getResourceString("PLOT_ADD_BUTTON"));
        upperPane.add(addButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5,
                5), 0, 0));

        removeButton.setText(messageBundle.getResourceString("PLOT_REMOVE_BUTTON"));
        upperPane.add(removeButton, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5,
                5, 5), 0, 0));

        addButton.setMinimumSize(removeButton.getMinimumSize());
        addButton.setPreferredSize(removeButton.getMinimumSize());
        
        //--Add plot property inspector panel--
        plotInspector.setPreferredSize(new Dimension(400, 320));
        plotInspector.setMinimumSize(new Dimension(300, 200));
        plotInspector.setBorder(createBorder("PLOT_INSPECTOR_BORDER_TITLE"));

        upperPane.add(plotInspector, new GridBagConstraints(1, 0, 1, 3, 1.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0, 0,
                0, 0), 0, 0));
        
        //--Panel of series tabular property inspector--
        tabSeriesInspector.setBorder(createBorder("PLOT_TAB_PROPERTIES_BORDER_TITLE"));
        tabSeriesInspector.setPreferredSize(new Dimension(400, 200));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, upperPane, tabSeriesInspector);
        splitPane.setDividerLocation(0.5);
        splitPane.setDividerSize(2);
        splitPane.setBorder(null);
        add(splitPane, BorderLayout.CENTER);
    }

    public void updateInterfaceElements(boolean extendedMode)
    {
        if( extendedMode )
            plotInspector.explore(plotEx);
        else
            plotInspector.explore(plotEx.getPlot());
        plotInspector.expandAll(true);
        plotInspector.setRootVisible(false);
        initTabSeriesInspector(plotEx.getPlot());
    }
    
    private ActionListener addButtonListener;
    private ActionListener removeButtonListener;
    private ActionListener pathListener;
    private DocumentListener documentListener;
    private ActionListener resultNameRBListener;
    private ActionListener experimentRBListener;
    private ListSelectionListener seriesSelectionListener;
    private PropertyChangeListener plotPropertyChangeListener;
    
    private void initListeners()
    {
        plotPropertyChangeListener = e -> {
            if( e.getPropertyName().equals("plot") )
            {
                updateSeries();
            }
        };
        
        addButtonListener = ae -> addSeries();
        
        removeButtonListener = ae -> {
            int index = tabSeriesInspector.getTable().getSelectedRow();
            removeSeries(index);
        };
        
        pathListener = ie -> updateVariableCombos();
        
        documentListener = new DocumentListener()
        {
            @Override
            public void changedUpdate(DocumentEvent e)
            {
            }
            @Override
            public void removeUpdate(DocumentEvent e)
            {
            }
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                updateVariableCombos();
            }
        };
        
        resultNameRBListener = e -> {
            experimentNameRB.setSelected(false);
            updateVariableCombos();
        };
        
        experimentRBListener = e -> {
            resultNameRB.setSelected(false);
            updateVariableCombos();
        };
        
        seriesSelectionListener = e -> {
            int index = tabSeriesInspector.getTable().getSelectedRow();
            removeButton.setEnabled(index >= 0);
        };
        
    }
    
    protected void registerListeners()
    {
        plotEx.removePropertyChangeListener(plotPropertyChangeListener);
        plotEx.addPropertyChangeListener(plotPropertyChangeListener);

        addButton.removeActionListener(addButtonListener);
        addButton.addActionListener(addButtonListener);

        removeButton.removeActionListener(removeButtonListener);
        removeButton.addActionListener(removeButtonListener);

        resultPath.removeActionListener(pathListener);
        resultPath.addActionListener(pathListener);
        
        resultPath.getDocument().removeDocumentListener(documentListener);
        resultPath.getDocument().addDocumentListener(documentListener);

        experimentPath.removeActionListener(pathListener);
        experimentPath.addActionListener(pathListener);
        
        experimentPath.getDocument().removeDocumentListener(documentListener);
        experimentPath.getDocument().addDocumentListener(documentListener);

        resultNameRB.removeActionListener(resultNameRBListener);
        resultNameRB.addActionListener(resultNameRBListener);

        experimentNameRB.removeActionListener(experimentRBListener);
        experimentNameRB.addActionListener(experimentRBListener);

        tabSeriesInspector.getTable().getSelectionModel().removeListSelectionListener(seriesSelectionListener);
        tabSeriesInspector.getTable().getSelectionModel().addListSelectionListener(seriesSelectionListener);
    }

    protected void updateSeries()
    {
        initTabSeriesInspector(plotEx.getPlot());
    }

    protected void initTabSeriesInspector(Plot plot)
    {
        if( plot == null )
        {
            return;
        }

        List<Series> series = plot.getSeries();

        if( series != null && series.size() > 0 )
        {
            tabSeriesInspector.explore(plot.getRowModel(), series.get(0), PropertyInspector.SHOW_USUAL | PropertyInspector.SHOW_PREFERRED);
        }
        else
        {
            tabSeriesInspector.explore(plot.getRowModel(), Plot.getDefaultSeries(), PropertyInspector.SHOW_USUAL
                    | PropertyInspector.SHOW_PREFERRED);
        }
    }

    protected void updateVariableCombos()
    {
        try
        {
            xVariable.removeAllItems();
            xPath.removeAllItems();
            yVariable.removeAllItems();
            yPath.removeAllItems();
            if( resultNameRB.isSelected() && resultPath.getPath() != null )
            {
                SimulationResult result = plotEx.getSimulationResult(resultPath.getPath());
                if( result == null )
                    return;

                Set<String> paths = result.getPaths();
                for( String path : paths )
                {
                    yPath.addItem( path );
                    xPath.addItem( path );
                }
                
                xPath.setSelectedItem( "" );
                yPath.setSelectedItem( "" );
            }
            else if( experimentNameRB.isSelected() && experimentPath.getPath() != null )
            {
                TableDataCollection experiment = plotEx.getExperiment(experimentPath.getPath());

                if( experiment != null && experiment.getColumnModel().getColumnCount() > 0 )
                {
                    for(TableColumn column : experiment.getColumnModel())
                    {
                        yVariable.addItem(new VariableItem(column.getName()));
                        xVariable.addItem(new VariableItem(column.getName()));
                    }
                }
            }
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Could not update variable combo box: " + ex.getMessage(), ex);
        }
    }

    protected void addSeries()
    {
        try
        {
            VariableItem xVar = (VariableItem)xVariable.getSelectedItem();
            VariableItem yVar = (VariableItem)yVariable.getSelectedItem();
            if( resultNameRB.isSelected() )
            {
                plotEx.getPlot()
                        .addSeries( Plot.getDefaultSeries( (String)xPath.getSelectedItem(), xVar.getName(), (String)yPath.getSelectedItem(),
                                yVar.getName(), resultPath.getPath().toString(), Series.SourceNature.SIMULATION_RESULT ) );
            }
            else if( experimentNameRB.isSelected() )
            {
                plotEx.getPlot().addSeries(
                        Plot.getDefaultSeries(xVar.getName(), yVar.getName(), experimentPath.getPath().toString(),
                                Series.SourceNature.EXPERIMENTAL_DATA));
            }
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Error occured while adding new series: " + ex);
        }
    }

    protected void removeSeries(int index)
    {
        if( index >= 0 && index < plotEx.getPlot().getRowModel().size() )
        {
            plotEx.getPlot().removeSeries(index);
        }
    }

    /**
     * Variable item for list
     */
    public static class VariableItem implements Comparable<VariableItem>
    {
        protected String path;
        protected String name;
        protected String title;

        public VariableItem(String path)
        {
            String[] components = Util.getMainPathComponents( path );
            this.path = components[0];
            this.name = components[1];
            this.title = name;
        }

        public String getName()
        {
            return name;
        }

        public void setTitle(String title)
        {
            this.title = title;
        }

        @Override
        public String toString()
        {
            return name;
        }

        @Override
        public int compareTo(VariableItem obj)
        {
            return name.compareTo(obj.name);
        }
    }
    
    public static List<String> getVariablesForPath(String path, SimulationResult result)
    {
        List<String> variables = getVariablesForPathmapping(path, result).get( path );
        if (variables == null)
            return new ArrayList<String>();
        return variables;
    }
    
    public static Map<String, List<String>> getVariablesForPathmapping(String path, SimulationResult result)//TODO: create this map in simulation result
    {
        Map<String, List<String>> map = new HashMap<>();
        Map<String, Integer> variables = result.getVariablePathMap();
        
        for (String varPath: variables.keySet())
        {
            String[] components = Util.getMainPathComponents( varPath );
            map.computeIfAbsent( components[0], k-> new ArrayList<>() ).add( components[1] );
        }
        for (List<String> array: map.values())
        {
            Collections.sort( array );
        }
        return map;
    }
}
