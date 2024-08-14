package biouml.plugins.lucene;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EtchedBorder;

import org.apache.lucene.queryparser.classic.ParseException;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.gui.ExplorerPane;
import biouml.model.Module;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;
import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.swing.TabularPropertyInspector;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobProgressBar;

/**
 * Search pane
 */
@SuppressWarnings ( "serial" )
public abstract class LuceneSearchPane extends JTabbedPane
{

    protected MessageBundle messageBundle = new MessageBundle();

    protected Logger log;

    //lucene interface
    LuceneQuerySystem luceneFacade = null;

    //table for presenting of results
    TabularPropertyInspector tabularInspector = null;
    //explorer for exploring of selected item
    ExplorerPane explorerPane = null;
    //inspector for choosing of necessary collumns
    PropertyInspector columnsInspector = null;

    //search condition feald
    JTextField searchCondition = null;
    //avaible data collections list field
    JComboBox<String> dcList = null;
    //avaible properties for selected data collection
    JList<String> properties = null;
    //stub
    JobProgressBar jobBar = null;
    //start search button
    JButton startButton = null;
    //cancel button
    JButton cancelButton = null;
    //pane for search button, cancel button and jobBar
    JPanel buttonsPanel = null;

    //used formatter
    Formatter formatter = null;
    //used view
    boolean alternativeView = false;
    //if necessary variable row height
    boolean variableRowHeight = true;
    int defaultRowHeight = 10;

    boolean cleanPerformed = false;

    Module module;

    JFrame frame;

    boolean isCompactDialog;

    /**
     * Setting pane
     *
     * @param frame - base frame (for error dialogs)
     * @param module - module with <code>LuceneQuerySystem</code>
     * @param formatter - formatter, used by this search
     * @param table - table for presentation of results
     * @param explorer - explore pane for exploring of selected item
     * @return
     * @throws Exception
     */
    public LuceneSearchPane(JFrame frame, LuceneQuerySystem luceneFacade_, Module module_, Formatter formatter,
            TabularPropertyInspector table, ExplorerPane explorer, Logger log, boolean isCompactDialog) throws Exception
    {
        this.module = module_;
        this.frame = frame;
        this.log = log;
        this.luceneFacade = luceneFacade_;
        this.formatter = formatter;
        this.isCompactDialog = isCompactDialog;
        explorerPane = explorer;
        tabularInspector = table;

        createSettingsPane();

        defaultRowHeight = ( new JTextField("Yy") ).getMinimumSize().height;
        tabularInspector.setVariableRowHeight(variableRowHeight);

        tabularInspector.addListSelectionListener(e -> {
            if( cleanPerformed )
                return;
            LuceneSearchPane.this.prepareSelectItem();
            Object model = tabularInspector.getModelOfSelectedRow();
            if( model == null )
                return;
            if( model instanceof DynamicPropertySet )
            {
                DynamicPropertySet dps = (DynamicPropertySet)model;
                DynamicProperty name = dps.getProperty(messageBundle.getResourceString("COLUMN_ELEMENT_NAME"));
                if( name == null )
                    return;
                DynamicProperty fullName = dps.getProperty(messageBundle.getResourceString("COLUMN_FULL_NAME"));
                if( fullName == null )
                    return;
                //System.out.println ( fullName.getValue ( ).toString ( ) );
                DataElement de = null;
                try
                {
                    de = module.getKernel(fullName.getValue().toString());
                }
                catch( Throwable t )
                {
                }
                if( de == null )
                    return;
                explorerPane.explore(de, null);
                LuceneSearchPane.this.selectItem(de);
            }
        });
    }

    public void changeModule(LuceneQuerySystem luceneFacade, Module module)
    {
        this.luceneFacade = luceneFacade;
        this.module = module;
        //dcWithIndexPane.reinit ( getAllCollectinsWithBuildIndexes ( ), luceneFacade, true );
        dcList.removeAllItems();
        fillDCList();
        if( dcList.getItemCount() > 0 )
        {
            dcList.setSelectedIndex(0);
            startButton.setEnabled(true);
            return;
        }
        startButton.setEnabled(false);
    }

    private void createSettingsPane() throws Exception
    {
        properties = new JList<>();
        properties.setListData(new Vector<String>());
        //properties.setPreferredSize ( new Dimension ( 140, properties.getPreferredScrollableViewportSize ( ).height ) );
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.getViewport().setView(properties);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JPanel pane = new JPanel();

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        pane.setLayout(gridbag);

        JLabel text = new JLabel(messageBundle.getResourceString("LUCENE_DATA_COLLECTION_COOSER_NAME"));
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        gridbag.setConstraints(text, c);
        pane.add(text);
        dcList = new JComboBox<>();
        fillDCList();
        dcList.setActionCommand("select");
        dcList.addActionListener(e -> {
            if( "select".equals(e.getActionCommand()) )
                try
                {
                    String dc = dcList.getSelectedItem().toString();
                    Vector<String> prop = LuceneUtils.indexedFields( luceneFacade.getIndexes( dc ) ).toCollection( Vector::new );
                    //System.out.println ( "DC = " + dc + ", size = " + prop.size ( ) );
                    properties.setListData(prop);
                    properties.setPreferredSize(new Dimension(properties.getPreferredScrollableViewportSize().width, properties
                            .getPreferredScrollableViewportSize().height
                            / properties.getVisibleRowCount() * prop.size()));
                }
                catch( Throwable t )
                {
                }
        });
        if( dcList.getItemCount() > 0 )
            dcList.setSelectedIndex(0);
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(dcList, c);
        pane.add(dcList);

        if( !isCompactDialog )
        {
            text = new JLabel(" ");
            gridbag.setConstraints(text, c);
            pane.add(text);
        }

        text = new JLabel(messageBundle.getResourceString("LUCENE_SEARCH"));
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.RELATIVE;
        gridbag.setConstraints(text, c);
        pane.add(text);
        searchCondition = new JTextField("");
        searchCondition.setPreferredSize(new Dimension(140, searchCondition.getPreferredSize().height));
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(searchCondition, c);
        pane.add(searchCondition);

        if( !isCompactDialog )
        {
            text = new JLabel(" ");
            gridbag.setConstraints(text, c);
            pane.add(text);
        }

        text = new JLabel(messageBundle.getResourceString("LUCENE_ALTRNATIVE_VIEW_CONDITION"));
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.RELATIVE;
        gridbag.setConstraints(text, c);
        pane.add(text);
        JCheckBox alternativeViewBox = new JCheckBox();
        alternativeViewBox.setActionCommand("select");
        alternativeViewBox.addActionListener(e -> {
            if( "select".equals(e.getActionCommand()) )
                alternativeView = !alternativeView;
        });
        alternativeViewBox.setSelected(alternativeView);
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(alternativeViewBox, c);
        pane.add(alternativeViewBox);

        if( !isCompactDialog )
        {
            text = new JLabel(" ");
            gridbag.setConstraints(text, c);
            pane.add(text);
        }

        text = new JLabel(messageBundle.getResourceString("LUCENE_VARIABLE_ROW_HEIGHT"));
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.RELATIVE;
        gridbag.setConstraints(text, c);
        pane.add(text);
        JCheckBox variableRow = new JCheckBox();
        variableRow.setActionCommand("select");
        variableRow.addActionListener(e -> {
            if( "select".equals(e.getActionCommand()) )
            {
                variableRowHeight = !variableRowHeight;
                tabularInspector.setVariableRowHeight(variableRowHeight);
                tabularInspector.getTable().setRowHeight(defaultRowHeight);
                tabularInspector.updateUI();
            }
        });
        variableRow.setSelected(variableRowHeight);
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(variableRow, c);
        pane.add(variableRow);

        // create buttons panel
        buttonsPanel = new JPanel(new GridBagLayout());
        buttonsPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED));

        if( !isCompactDialog )
            startButton = new JButton(messageBundle.getResourceString("BUTTON_START"));
        else
            startButton = new JButton(messageBundle.getResourceString("BUTTON_SEARCH"));
        startButton.addActionListener(e -> startPressed());

        cancelButton = new JButton(messageBundle.getResourceString("BUTTON_CLOSE"));
        cancelButton.addActionListener(e -> cancelPressed());

        addButtons();

        JPanel searchPane = new JPanel(new BorderLayout());
        searchPane.add(pane, BorderLayout.CENTER);
        searchPane.add(buttonsPanel, BorderLayout.SOUTH);
        addTab(messageBundle.getResourceString("LUCENE_TAB_NAME"), searchPane);
        //tabbedPane.setSize ( new Dimension ( pane.getPreferredSize ( ).width, tabbedPane.getPreferredSize ( ).height ) );

        addTab(messageBundle.getResourceString("LUCENE_AVAIBLE_PROPERTIES_TAB_NAME"), scrollPane);

        columnsInspector = new PropertyInspector();
        //columnsInspector.setPreferredSize ( new Dimension ( 140, columnsInspector.getPreferredSize ( ).height ) );
        addTab(messageBundle.getResourceString("LUCENE_COLUMNS_TAB_NAME"), columnsInspector);
        /*
         dcWithIndexPane = new RebuildIndexPane ( getAllCollectinsWithBuildIndexes ( ), luceneFacade, true );
         dcInspector = dcWithIndexPane.reinit ( getAllCollectinsWithBuildIndexes ( ), luceneFacade, true );
         addTab ( messageBundle.getResourceString ( "LUCENE_INDEX_TAB_NAME" ), dcInspector );
         */
    }

    private void fillDCList()
    {
        List<String> namesCollection = new ArrayList<>();
        if( luceneFacade != null )
        {
            namesCollection.addAll(luceneFacade.getCollectionsNamesWithIndexes());
        }
        QuerySystem[] externalLuceneFacades = null;

        if( module != null )
        {
            externalLuceneFacades = module.getExternalLuceneFacades();
        }

        if( externalLuceneFacades != null )
        {
            for( QuerySystem lqs : externalLuceneFacades )
            {
                if( lqs instanceof LuceneQuerySystem )
                {
                    for( String obj : ( (LuceneQuerySystem)lqs ).getCollectionsNamesWithIndexes() )
                    {
                        if( !namesCollection.contains(obj) )
                            namesCollection.add(obj);
                    }
                }
            }
        }
        for( String name : namesCollection )
        {
            dcList.addItem(name);
        }
    }
    private void startPressed()
    {
        try
        {
            jobBar = new JobProgressBar();
            //jobBar.setPreferredSize ( new Dimension ( 140, jobBar.getPreferredSize ( ).height ) );
            final FunctionJobControl jobControl = new FunctionJobControl(null, null);
            jobControl.addListener(jobBar);
            buttonsPanel.removeAll();
            buttonsPanel.add(jobBar, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                    new Insets(10, 5, 10, 10), 0, 0));
            buttonsPanel.validate();
            buttonsPanel.repaint();

            ( new Thread()
            {
                @Override
                public void run()
                {
                    List<DynamicPropertySet> searchResults = new ArrayList<>();

                    try
                    {
                        int index = dcList.getSelectedIndex();
                        if( index >= 0 && index < dcList.getItemCount() )
                        {
                            String relativeName = dcList.getItemAt(index);

                            Module[] externalModules = module.getExternalModules(relativeName);

                            if( luceneFacade.testHaveIndex() )
                            {
                                DynamicPropertySet[] results1 = luceneFacade.search(relativeName, searchCondition.getText(), formatter,
                                        alternativeView);
                                if( results1 != null )
                                {
                                    searchResults.addAll( Arrays.asList( results1 ) );
                                }
                            }
                            else if( externalModules == null )
                            {
                                RebuildIndexDialog rebuildDialog = new RebuildIndexDialog(frame, messageBundle
                                        .getResourceString("LUCENE_REBUILD_INDEX_TITLE"), null, luceneFacade, module);
                                rebuildDialog.doModal();
                            }

                            if( externalModules != null )
                            {
                                for( Module eModule : externalModules )
                                {
                                    LuceneQuerySystem lqs = null;
                                    if( eModule.getInfo().getQuerySystem() instanceof LuceneQuerySystem )
                                    {
                                        lqs = (LuceneQuerySystem)eModule.getInfo().getQuerySystem();
                                    }
                                    if( lqs != null && lqs.testHaveIndex() )
                                    {
                                        DynamicPropertySet[] results2 = lqs.search(relativeName, searchCondition.getText(), formatter,
                                                alternativeView);
                                        if( results2 != null )
                                        {
                                            searchResults.addAll( Arrays.asList( results2 ) );
                                        }
                                    }
                                }
                            }

                            prepareSelectItem();
                            tabularInspector.explore(searchResults.toArray(new DynamicPropertySet[searchResults.size()]));
                            tabularInspector.getTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                            columnsInspector.explore(tabularInspector.getColumnModel());
                            afterSearch();
                        }
                    }
                    catch( ParseException pe )
                    {
                        JOptionPane.showMessageDialog(LuceneSearchPane.this, pe.getMessage(), messageBundle
                                .getResourceString("LUCENE_PARSE_EXCEPTION"), JOptionPane.ERROR_MESSAGE);
                    }
                    catch( Throwable t )
                    {
                        if( log != null )
                            log.log(Level.SEVERE, "Search error", t);
                    }
                    try
                    {
                        if( searchResults.isEmpty() )
                        {
                            cleanPerformed = true;
                            DynamicPropertySet[] dps = new DynamicPropertySet[1];
                            dps[0] = new DynamicPropertySetSupport();
                            dps[0].add( new DynamicProperty( messageBundle.getResourceString( "COLUMN_FIND_NOTHING" ), String.class, "" ) );
                            tabularInspector.explore( dps );
                            columnsInspector.explore( new DataCollectionList() );
                            afterSearch();
                            cleanPerformed = false;
                        }
                        addButtons();
                    }
                    catch( Throwable t )
                    {
                        if( log != null )
                            log.log(Level.SEVERE, "Search error", t);
                    }
                }
            } ).start();
        }
        catch( Throwable t )
        {
            if( log != null )
                log.log(Level.SEVERE, "Search error", t);
        }
    }
    private void addButtons()
    {
        buttonsPanel.removeAll();
        buttonsPanel.add(startButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(10, 10, 10, 5), 0, 0));
        startButton.setEnabled(dcList.getItemCount() > 0);
        if( !isCompactDialog )
        {
            buttonsPanel.add(cancelButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                    new Insets(10, 5, 10, 10), 0, 0));
            buttonsPanel.validate();
            buttonsPanel.repaint();
        }
    }

    protected abstract void cancelPressed();

    protected abstract void prepareSelectItem();
    protected abstract void selectItem(DataElement de);

    protected abstract void afterSearch();

}
