package biouml.plugins.microarray.editors;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.developmentontheedge.application.PanelInfo;
import com.developmentontheedge.application.PanelManager;
import com.developmentontheedge.application.action.ActionInitializer;

import biouml.model.Diagram;
import biouml.model.Module;
import biouml.plugins.microarray.MessageBundle;
import biouml.standard.type.DatabaseInfo;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathDialog;
import ru.biosoft.analysis.FilterTable;
import ru.biosoft.analysis.FilterTableParameters;
import ru.biosoft.analysiscore.AnalysisParametersFactory;
import ru.biosoft.gui.Document;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.RowFilter;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.access.TableRowsExporter;
import ru.biosoft.table.document.TableDocument;
import ru.biosoft.table.document.ValuesPane;
import ru.biosoft.table.document.editors.ColumnsFilterPanel;
import ru.biosoft.tasks.TaskManager;

@SuppressWarnings ( "serial" )
public class FilterViewPane extends JPanel
{
    protected Logger log = Logger.getLogger(FilterViewPane.class.getName());

    public static final String ID_ATTR = "ID";
    private static final String NONE = "none";

    private JComboBox<String> modulesList;
    private JComboBox<String> diagramsList;
    private JComboBox<String> geneHubList;
    //private JList columnsList;
    //private JTextArea filterString;
    private final ColumnsFilterPanel columnsFilterPanel;

    protected TableDataCollection tableData;

    protected TableDocument tableDocument;

    protected ResourceBundle resources;

    public static final String APPLY_FILTER_ACTION = "FilterViewPane.ApplyFilterAction";
    public static final String REMOVE_FILTER_ACTION = "FilterViewPane.RemoveFilterAction";
    public static final String EXPORT_ACTION = "FilterViewPane.ExportAction";

    protected Action applyFilterAction;
    protected Action removeFilterAction;
    protected Action exportAction;
    protected Action[] actions;

    protected PropertyChangeListener filterStatusChangeListener;

    public FilterViewPane()
    {
        resources = ResourceBundle.getBundle(MessageBundle.class.getName());

        filterStatusChangeListener = new FilterStatusChangeListener();

        this.setLayout(new BorderLayout());

        PanelManager panelManager = new PanelManager();

        applyFilterAction = new ApplyFiltersAction(APPLY_FILTER_ACTION);
        removeFilterAction = new RemoveFiltersAction(REMOVE_FILTER_ACTION);
        exportAction = new ExportAction(EXPORT_ACTION);

        columnsFilterPanel = new ColumnsFilterPanel();

        PanelInfo filterInfo = new PanelInfo("Filter by column", columnsFilterPanel, true, null);
        panelManager.addPanel(filterInfo, null, PanelInfo.RIGHT, 500);

        JPanel diagramFilterPanel = new JPanel();
        creatDiagramFilterPanel(diagramFilterPanel);

        filterInfo = new PanelInfo("Filter by diagram", diagramFilterPanel, true, null);
        panelManager.addPanel(filterInfo, "Filter by column", PanelInfo.RIGHT, 500);

        add(panelManager, BorderLayout.CENTER);
    }

    private void creatDiagramFilterPanel(JPanel diagramFilterPanel)
    {
        diagramFilterPanel.setLayout(new GridBagLayout());
        JLabel moduleLabel = new JLabel("Module:");

        moduleLabel.setVerticalAlignment(JLabel.CENTER);
        diagramFilterPanel.add(moduleLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.NONE,
                new Insets(5, 10, 0, 0), 0, 0));
        JLabel diagramLabel = new JLabel("Diagram:");

        diagramLabel.setVerticalAlignment(JLabel.CENTER);
        diagramFilterPanel.add(diagramLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.NORTH,
                GridBagConstraints.NONE, new Insets(5, 10, 0, 0), 0, 0));
        JLabel geneHubLabel = new JLabel("GeneHub:");

        geneHubLabel.setVerticalAlignment(JLabel.CENTER);
        diagramFilterPanel.add(geneHubLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.NORTH,
                GridBagConstraints.NONE, new Insets(5, 10, 0, 0), 0, 0));
        JLabel emptyLabel = new JLabel("");

        emptyLabel.setVerticalAlignment(JLabel.CENTER);
        diagramFilterPanel.add(emptyLabel, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.NONE,
                new Insets(5, 10, 0, 0), 0, 0));
        diagramsList = new JComboBox<>();

        diagramFilterPanel.add(diagramsList, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.NORTH,
                GridBagConstraints.HORIZONTAL, new Insets(5, 10, 0, 10), 0, 0));
        modulesList = new JComboBox<>();

        diagramFilterPanel.add(modulesList, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.NORTH,
                GridBagConstraints.HORIZONTAL, new Insets(5, 10, 0, 10), 0, 0));
        geneHubList = new JComboBox<>();
        geneHubList.addItem(NONE);

        diagramFilterPanel.add(geneHubList, new GridBagConstraints(1, 2, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH,
                GridBagConstraints.HORIZONTAL, new Insets(5, 10, 0, 10), 0, 0));
    }

    protected Map<String, BioHubRegistry.BioHubInfo> bioHubMap = null;

    private void initDiagramFilterPanel()
    {
        modulesList.removeAllItems();
        initModulesList(modulesList, CollectionFactoryUtils.getDatabases());
        if( 0 < modulesList.getItemCount() )
        {
            modulesList.setSelectedIndex(0);
        }

        geneHubList.removeAllItems();
        geneHubList.addItem(NONE);

        bioHubMap = BioHubRegistry.getBioHubs();
        for(String hubName: bioHubMap.keySet())
        {
            geneHubList.addItem(hubName);
        }

        if( 0 < geneHubList.getItemCount() )
        {
            geneHubList.setSelectedIndex(0);
        }
    }

    public void explore(TableDataCollection me, Document document)
    {
        tableData = me;

        tableDocument = (TableDocument)document;
        tableDocument.removePropertyChangeListener(filterStatusChangeListener);
        tableDocument.addPropertyChangeListener(filterStatusChangeListener);

        //initFilterPanel();
        columnsFilterPanel.initPanel(me);
        initDiagramFilterPanel();
    }
    protected void applyFilters()
    {
        if( tableDocument != null )
        {
            tableDocument.setRowFilter(getRowFilter());
        }
    }

    private RowFilter getRowFilter()
    {
        Module module = null;
        Diagram diagram = null;
        int index = modulesList.getSelectedIndex();
        if( index >= 0 )
        {
            module = resolveModule(String.valueOf(modulesList.getItemAt(index)));
        }
        if( module != null )
        {
            index = diagramsList.getSelectedIndex();
            if( index >= 0 )
            {
                diagram = resolveDiagram(String.valueOf(diagramsList.getItemAt(index)), module);
            }
        }

        BioHub bioHub = null;
        index = geneHubList.getSelectedIndex();
        if( 0 <= index )
        {
            bioHub = resolveBioHub(String.valueOf(geneHubList.getItemAt(index)));
        }

        String filterStr = columnsFilterPanel.getText();
        try
        {
            return new RowFilter(filterStr, tableData, diagram, module, bioHub);
        }
        catch( IllegalArgumentException e )
        {
            String dialogTitle = resources.getString("FILTER_ERROR_TITLE");
            String message = e.getMessage();
            JOptionPane.showMessageDialog(this, message, dialogTitle, JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    protected void removeFilters()
    {
        if( tableDocument != null )
        {
            tableDocument.setRowFilter(null);
        }
    }

    protected void export()
    {
        if( tableDocument != null )
        {
            DataElementPathDialog dialog = new DataElementPathDialog("Select result table");
            dialog.setElementClass(TableDataCollection.class);

            dialog.setValue(DataElementPath.create(tableDocument.getTableData()));
            dialog.setPromptOverwrite(true);
            if( dialog.doModal() )
            {
                try
                {
                    FunctionJobControl stub = new FunctionJobControl( log );
                    FilterTable analysis = new FilterTable(null, FilterTable.ANALYSIS_NAME);
                    FilterTableParameters parameters = analysis.getParameters();
                    analysis.setLogger(log);
                    parameters.setInputPath(DataElementPath.create((DataCollection<?>)tableDocument.getModel()));
                    parameters.setOutputPath(dialog.getValue());
                    parameters.setFilterExpression(((RowFilter)tableDocument.getRowFilter()).getFilterExpression());
                    try
                    {
                        TaskManager.getInstance().addAnalysisTask( analysis, stub, false );
                    }
                    catch( Exception e )
                    {
                        log.log(Level.SEVERE, "Error while creating history entry for filter: ", e);
                    }
                    stub.functionStarted();
                    TableRowsExporter.exportTable( dialog.getValue(), tableData, DataCollectionUtils.asCollection(
                            (DataCollection<RowDataElement>)tableDocument.getCurrentFilteredCollection(), RowDataElement.class ), stub );
                    AnalysisParametersFactory.write( dialog.getValue().getDataCollection(), analysis );
                    stub.functionFinished();
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "Can not export table", e);
                }
            }
        }
    }

    private void initModulesList(final JComboBox<String> modulesList, DataCollection<?> repository)
    {
        modulesList.addItem(NONE);
        if( repository != null )
        {
            for(DataElement de: repository)
            {
                if( de instanceof Module )
                {
                    modulesList.addItem(de.getName());
                }
            }
        }
        modulesList.addActionListener(e -> {
            int idx = modulesList.getSelectedIndex();
            if( idx >= 0 )
            {
                initDiagramsList(String.valueOf(modulesList.getItemAt(idx)));
            }
        });
    }

    protected void initDiagramsList(String string)
    {
        diagramsList.removeAllItems();
        diagramsList.addItem(NONE);
        if( NONE.equals(string) )
        {
            return;
        }
        Module module = resolveModule(string);
        if( null != module )
        {
            DataCollection<?> diagrams = null;
            try
            {
                diagrams = module.getDiagrams();
            }
            catch( Exception e )
            {
                e.printStackTrace();
            }
            if( null != diagrams )
            {
                diagrams.names().forEach( diagramsList::addItem );
            }
        }
    }

    public Action[] getActions()
    {
        if( actions == null )
        {
            ActionInitializer initializer = new ActionInitializer(MessageBundle.class);
            initializer.initAction(applyFilterAction, APPLY_FILTER_ACTION);
            initializer.initAction(removeFilterAction, REMOVE_FILTER_ACTION);
            initializer.initAction(exportAction, EXPORT_ACTION);

            actions = new Action[] {applyFilterAction, removeFilterAction, exportAction};
        }

        return actions;
    }

    protected Map<String, DatabaseInfo> databaseInfoMap = new HashMap<>();

    private Diagram resolveDiagram(String diagramName, Module module)
    {
        if( null == module || NONE.equals(diagramName) )
        {
            return null;
        }
        Diagram diagram = null;
        try
        {
            diagram = module.getDiagram(diagramName);
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
        return diagram;
    }

    private Module resolveModule(String moduleName)
    {
        if( NONE.equals(moduleName) )
        {
            return null;
        }
        Module module = null;
        try
        {
            module = (Module)CollectionFactoryUtils.getDatabases().get(moduleName);
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
        return module;
    }

    private BioHub resolveBioHub(String geneHubName)
    {
        BioHub bioHub = null;
        if( null != geneHubName && !NONE.equals(geneHubName) )
        {
            BioHubRegistry.BioHubInfo bioHubInfo = bioHubMap.get(geneHubName);
            if( null != bioHubInfo )
            {
                bioHub = bioHubInfo.getBioHub();
            }
        }
        return bioHub;
    }

    private class ApplyFiltersAction extends AbstractAction
    {
        public ApplyFiltersAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            exportAction.setEnabled(false);
            applyFilters();
        }
    }

    private class RemoveFiltersAction extends AbstractAction
    {
        public RemoveFiltersAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            exportAction.setEnabled(false);
            columnsFilterPanel.setText("");
            removeFilters();
        }
    }

    private class ExportAction extends AbstractAction
    {
        public ExportAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            export();
        }
    }

    private class FilterStatusChangeListener implements PropertyChangeListener
    {

        @Override
        public void propertyChange(PropertyChangeEvent arg0)
        {
            if( arg0.getPropertyName().equals("filteringStatus") )
            {
                Object source = arg0.getSource();
                if( source instanceof ValuesPane && tableDocument.equals( ( (ValuesPane)source ).getDocument()) )
                {
                    if( arg0.getNewValue().equals(JobControl.COMPLETED) )
                    {
                        exportAction.setEnabled(true);
                    }
                }
            }

        }

    }

}
