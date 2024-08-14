package ru.biosoft.access.search;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.filter.FilteredDataCollection;
import ru.biosoft.gui.ExplorerPane;
import ru.biosoft.util.ApplicationUtils;

import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.swing.TabularPropertyInspector;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationFrame;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobProgressBar;

public class DataSearch extends JFrame implements PropertyChangeListener
{
    protected DataCollection primaryCollection = null;

    protected TabularPropertyInspector tabularInspector = null;
    protected PropertyInspector filterInspector = null;
    protected PropertyInspector columnsInspector = null;
    protected ExplorerPane explorerPane = null;

    public DataSearch(String title, DataCollection collection)
    {
        super(title);

        this.primaryCollection = collection;

        JPanel settingsPane = createSettingsPane();
        explorerPane = new ExplorerPane();
        JSplitPane verticalSplitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                                     settingsPane, explorerPane);
        verticalSplitter.setDividerSize(4);

        tabularInspector = new TabularPropertyInspector();
        JSplitPane horizontalSplitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                                     tabularInspector, verticalSplitter);

        verticalSplitter.setDividerLocation(300);
        horizontalSplitter.setDividerLocation(700);
        verticalSplitter.setDividerSize(4);
        horizontalSplitter.setDividerSize(4);

        setContentPane(horizontalSplitter);

        tabularInspector.addListSelectionListener(new ListSelectionListener()
            {
                @Override
                public void valueChanged(ListSelectionEvent e)
                {
                    Object model = tabularInspector.getModelOfSelectedRow();
                    explorerPane.explore(model, null); // pending document
                }
            });
    }

    @Override
    public void show()
    {
        pack();
        Dimension parentSize = null;
        ApplicationFrame applicationFrame = Application.getApplicationFrame();
        if (applicationFrame!=null)
        {
            parentSize = applicationFrame.getSize();
        }
        else
        {
            parentSize = Toolkit.getDefaultToolkit().getScreenSize();
        }
        setSize(parentSize.width-50, parentSize.height-50);
        ApplicationUtils.moveToCenter(this);
        super.show();
    }

    @Override
    public void propertyChange( PropertyChangeEvent evt )
    {
        String name = evt.getPropertyName();
        if( evt.getSource()==filteringSettings )
        {
            if( name.equals("filter") )
            {
                FilteringSettings newSettings = new FilteringSettings();
                newSettings.setCollectionName( filteringSettings.getCollectionName() );
                newSettings.setType( filteringSettings.getType() );
                filteringSettings.removePropertyChangeListener( this );
                filteringSettings = newSettings;
                filterInspector.explore( filteringSettings );
                filteringSettings.addPropertyChangeListener( this );
            }
        }
    }

    protected JPanel createSettingsPane()
    {
        MessageBundle messageBundle = new MessageBundle();
        filterInspector = new PropertyInspector();
        /** @todo Correct this code */

        filteringSettings = new FilteringSettings();
        filteringSettings.setCollectionName(primaryCollection.getName());
        filteringSettings.setType(primaryCollection.getDataElementType());
        filteringSettings.addPropertyChangeListener(this);
        filterInspector.explore(filteringSettings);

        // create settings panel
        columnsInspector = new PropertyInspector();

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab(messageBundle.getResourceString("DATA_SEARCH_FILTER_TAB_NAME"),
                          filterInspector);

        tabbedPane.addTab(messageBundle.getResourceString("DATA_SEARCH_COLUMNS_TAB_NAME"),
                          columnsInspector);

        JPanel settingsPanel = new JPanel(new BorderLayout());
        settingsPanel.add(tabbedPane, BorderLayout.CENTER);

        // create buttons panel
        buttonsPanel = new JPanel(new GridBagLayout());
        buttonsPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED));

        startButton = new JButton(messageBundle.getResourceString("BUTTON_START"));
        startButton.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    startPressed();
                }
            });

        cancelButton = new JButton(messageBundle.getResourceString("BUTTON_CLOSE"));
        cancelButton.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    cancelPressed();
                }
            });
        addButtons();

        // create main panel
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(settingsPanel, BorderLayout.CENTER);
        panel.add(buttonsPanel, BorderLayout.SOUTH);

        return panel;
    }

    protected void startPressed()
    {
        try
        {
            jobBar = new JobProgressBar();
            final FunctionJobControl jobControl = new FunctionJobControl( null );
            jobControl.addListener( jobBar );
            buttonsPanel.removeAll();
            buttonsPanel.add(jobBar,
                             new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                             GridBagConstraints.NONE,
                             new Insets(10, 5, 10, 10), 0, 0));
            buttonsPanel.validate();
            buttonsPanel.repaint();

            new Thread(
            new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        Properties properties = new Properties();
                        properties.put( DataCollectionConfigConstants.JOB_CONTROL_PROPERTY,jobControl );
                        properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY,primaryCollection.getName() );
                                properties.put( FilteredDataCollection.FILTER_PROPERTY, filteringSettings.getFilter() );
                        FilteredDataCollection fdc = new FilteredDataCollection( primaryCollection,properties );
                        tabularInspector.explore( fdc.iterator() );
                        columnsInspector.explore(tabularInspector.getColumnModel());
                        addButtons();
                    }
                    catch( Throwable t )
                    {
                        t.printStackTrace();
                    }
                }
            }).start();
        }
        catch( Throwable t )
        {
            /** @todo Out to log */
            t.printStackTrace();
//            log.log(Level.SEVERE, t.getMessage(), t);
        }
    }

    private void addButtons()
    {
        buttonsPanel.removeAll();
        buttonsPanel.add(startButton,
                         new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                         GridBagConstraints.NONE,
                         new Insets(10, 10, 10, 5), 0, 0));
        buttonsPanel.add(cancelButton,
                         new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                         GridBagConstraints.NONE,
                         new Insets(10, 5, 10, 10), 0, 0));
        buttonsPanel.validate();
        buttonsPanel.repaint();
    }

    protected void cancelPressed()
    {
        hide();
    }

    FilteringSettings filteringSettings = null;
    JPanel         buttonsPanel = null;
    JobProgressBar jobBar       = null;
    JButton        startButton  = null;
    JButton        cancelButton = null;
}
