package biouml.standard.diagram;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.accessibility.AccessibleContext;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.Index;
import ru.biosoft.access.support.NameListToComboBoxModelAdapter;
import ru.biosoft.access.support.NameListToListModelAdapter;
import ru.biosoft.gui.ExplorerPane;
import biouml.model.CollectionDescription;
import biouml.model.Module;
import biouml.standard.type.Base;
import biouml.standard.type.GenericEntity;
import biouml.standard.type.access.TitleIndex;

/**
 * Special pane for choosing of elements, whch support
 * choosing throw ID and (if exists) throw "title" Index
 * @see ru.biosoft.access.core.Index
 */
public class ElementChooserPane extends JPanel
{

    protected static final Logger log = Logger.getLogger(ElementChooserPane.class.getName());

    protected String TITLE_PROTOTYPE = "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM";

    private static MessageBundle resources = (MessageBundle)ResourceBundle.getBundle(MessageBundle.class.getName());

    private Class<? extends DataElement> type;
    private final Module module;

    protected JComboBox<String> cbModule;

    protected JComboBox<String> cbName;
    protected NameListToComboBoxModelAdapter cbNameModel;

    protected JTextField tfTitle;
    protected JComboBox<String> cbTitle;
    protected ComboBoxModel<String> cbTitleModel;

    protected ExplorerPane explorerPane;

    protected DataCollection kernelDC;
    protected DataCollection baseKernelDC;
    protected CollectionDescription[] extendedModules;

    protected ElementChooserPaneListener paneListener = null;

    protected TitleIndex index;

    protected boolean lockTitle = false;
    protected boolean lockID = false;

    // //////////////////////////////////////////////////////////////////////////
    // Constructor
    //
    /**
     * Create empty pane
     */
    public ElementChooserPane(Module module, ExplorerPane explorerPane) throws Exception
    {
        this.module = module;
        this.explorerPane = explorerPane;

        setLayout(new GridBagLayout());
    }

    public ElementChooserPane(Module module, ExplorerPane explorerPane, CollectionDescription[] extendedModules,
            ElementChooserPaneListener paneListener) throws Exception
    {
        this(module, explorerPane);
        this.extendedModules = extendedModules;
        this.paneListener = paneListener;
    }

    /**
     * You should call this function for correct work
     * @throws Exception
     */
    public void init(Class<? extends DataElement> type) throws Exception
    {
        close();
        this.type = type;

        baseKernelDC = module.getCategory(type);
        if( baseKernelDC == null && extendedModules == null )
        {
            add(new JLabel(resources.getResourceString("NEW_ELEMENT_DIALOG_ERROR")));
            repaint();
            return;
        }

        if( baseKernelDC != null )
        {
            kernelDC = baseKernelDC;
        }
        else if( extendedModules.length > 0 )
        {
            kernelDC = extendedModules[0].getDc();
        }

        refreshChooserPane();
    }

    protected void refreshChooserPane()
    {
        GridBagConstraints gbc;

        // create module ComboBox if necessary
        if( extendedModules != null )
        {
            if( cbModule == null )
            {
                cbModule = new JComboBox<>();

                gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.BOTH;
                gbc.weighty = 1.0;
                add(new JLabel(resources.getResourceString("DATABASE_COMBO_BOX")), gbc);

                gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.BOTH;
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                add(cbModule, gbc);

                fillModuleComboBox();

                cbModule.addActionListener(e -> {
                    String moduleName = (String)cbModule.getSelectedItem();
                    if( module.getName().equals(moduleName) )
                    {
                        kernelDC = baseKernelDC;
                    }
                    else if( extendedModules != null )
                    {
                        for( CollectionDescription et : extendedModules )
                        {
                            if( et.getModuleName().equals(moduleName) )
                            {
                                kernelDC = et.getDc();
                            }
                        }
                    }
                    changeModule(moduleName);
                });
            }
            cbModule.repaint();
        }
        // create name (id) box
        cbNameModel = new NameListToComboBoxModelAdapter(kernelDC);

        if( cbName == null )
        {
            cbName = new JComboBox<>(cbNameModel);

            String namePrototype = kernelDC.getInfo().getProperty(DataCollectionConfigConstants.ID_FORMAT);
            if( namePrototype != null )
                cbName.setPrototypeDisplayValue(namePrototype);
            else
                cbName.setPrototypeDisplayValue(TITLE_PROTOTYPE);

            add(new JLabel(resources.getResourceString("NAME_COMBO_BOX")));

            gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            add(cbName, gbc);

            cbName.addActionListener(e -> {
                if( !lockID )
                    if( tfTitle != null )
                    {
                        lockTitle = true;
                        tfTitle.setText("");
                        lockTitle = false;
                    }

                String name = (String)cbName.getSelectedItem();

                if( cbTitle != null )
                {
                    String title = ( (TitleIndex)cbTitle.getModel() ).get(name);
                    if( title != null )
                    {
                        cbTitle.setSelectedItem(title);
                    }
                    cbTitle.repaint();
                }

                try
                {
                    Object selectedKernel = kernelDC.get(name);
                    if( ElementChooserPane.this.explorerPane != null )
                        ElementChooserPane.this.explorerPane.explore(selectedKernel, null);
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE, "Can not get element '" + name + "', dc=" + kernelDC.getCompletePath() + ", error: " + t, t);
                }
            });
        }
        else
        {
            cbName.removeAllItems();
            cbName.setModel(cbNameModel);
        }

        if( cbTitle != null )
        {
            cbTitle.removeAllItems();
            cbTitle.setEnabled(false);
            tfTitle.setText("");
            tfTitle.setEnabled(false);
        }

        // if available - create title combo box
        if( kernelDC != null && kernelDC.getInfo().getQuerySystem() != null
                && kernelDC.getInfo().getQuerySystem().getIndex("title") != null )
        {
            Index tempIndex = kernelDC.getInfo().getQuerySystem().getIndex("title");
            if( tempIndex instanceof TitleIndex )
                index = (TitleIndex)tempIndex;
            //else
            //    index = new WrapperTitleIndex ( kernelDC, "title" );
            if( index != null )
            {
                cbTitleModel = index;
                cbTitleModel.setSelectedItem(null);

                if( cbTitle == null )
                {
                    cbTitle = new JComboBox<>(cbTitleModel);
                    cbTitle.setPrototypeDisplayValue(TITLE_PROTOTYPE);

                    cbTitle.addActionListener(e -> {
                        String title = (String)cbTitle.getSelectedItem();
                        String name = ( (TitleIndex)cbTitle.getModel() ).getIdByTitle(title);
                        if( name != null && !name.equals(cbName.getSelectedItem()) )
                        {
                            lockID = true;
                            cbName.setSelectedItem(name);
                            lockID = false;
                        }
                        cbName.repaint();
                    });

                    tfTitle = new JTextField();
                    tfTitle.getAccessibleContext().addPropertyChangeListener(arg0 -> {
                        if( arg0 != null )
                            if( AccessibleContext.ACCESSIBLE_TEXT_PROPERTY.equalsIgnoreCase(arg0.getPropertyName()) )
                            {
                                String text = tfTitle.getText();
                                index.setConstraint(text);
                                if( cbTitle.getItemCount() > 0 && !lockTitle )
                                {
                                    lockID = true;
                                    cbTitle.setSelectedIndex(0);
                                    lockID = false;
                                }
                                cbTitle.repaint();
                            }
                    });

                    gbc = new GridBagConstraints();
                    gbc.fill = GridBagConstraints.BOTH;
                    gbc.gridheight = 2;
                    gbc.weighty = 1.0;
                    add(new JLabel(resources.getResourceString("TITLE_COMBO_BOX")), gbc);

                    gbc = new GridBagConstraints();
                    gbc.fill = GridBagConstraints.BOTH;
                    gbc.gridwidth = GridBagConstraints.REMAINDER;
                    add(tfTitle, gbc);

                    gbc = new GridBagConstraints();
                    gbc.fill = GridBagConstraints.BOTH;
                    gbc.gridwidth = GridBagConstraints.REMAINDER;
                    add(cbTitle, gbc);
                }
                else
                {
                    cbTitle.setModel(cbTitleModel);
                }

                cbTitle.setEnabled(true);
                tfTitle.setEnabled(true);
            }
        }

        setControlsEnabled(cbName.getItemCount() > 0);

        if( cbName.getItemCount() > 0 )
            cbName.setSelectedIndex(0);
    }

    protected boolean moduleChanging = false;
    protected void changeModule(String moduleName)
    {
        if( !moduleChanging )
        {
            moduleChanging = true;
            refreshChooserPane();
            if( paneListener != null )
            {
                paneListener.moduleChanged(moduleName, kernelDC);
            }
            if( cbModule != null )
            {
                cbModule.setSelectedItem(moduleName);
            }
            moduleChanging = false;
        }
    }


    /**
     * This function can be colled outside for selected of
     * choosing element
     */
    public void selectItem(String name, String moduleName)
    {
        if( cbModule != null )
        {
            cbModule.setSelectedItem(moduleName);
        }
        if( cbName != null && cbName.getModel() != null )
        {
            if( ( (NameListToListModelAdapter)cbName.getModel() ).contain(name) )
                cbName.setSelectedItem(name);
        }
    }

    /**
     * @pending move resource strings to ru.biosoft.access.support.MessageBundle
     */
    public void addNew(GenericEntity entity)
    {
        try
        {
            if( entity != null && type != null )
            {
                if( tfTitle != null )
                    tfTitle.setText("");

                DataCollection category = module.getCategory(type);
                if( category != null )
                {
                    category.put(entity);
                    cbName.setSelectedItem(entity.getName());
                    cbName.repaint();
                    setControlsEnabled(cbName.getItemCount() > 0);
                }
            }
        }
        catch( Throwable e )
        {
            log.log(Level.SEVERE, "Error during creation new data element", e);
        }
    }

    protected void setControlsEnabled(boolean value)
    {
        cbName.setEnabled(value);
    }

    /**
     * Close this panel (should be colled afrer using)
     */
    public void close()
    {
        if( cbNameModel != null )
            cbNameModel.close();
    }

    /**
     * Get selected data element or <b>null</b>
     */
    public Base getKernel()
    {
        try
        {
            String name = getKernelName();
            if( name == null || type == null )
                return null;
            return (Base)module.getKernel(type, name);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Error during creation of a diagram element", e);
        }
        return null;
    }

    protected String getKernelName()
    {
        int index = cbName.getSelectedIndex();
        if( index >= 0 && index < cbName.getItemCount() )
            return "" + cbName.getItemAt(index);
        return null;
    }

    protected void fillModuleComboBox()
    {
        cbModule.removeAllItems();
        List<String> usedModuleNames = new ArrayList<>();
        if( baseKernelDC != null )
        {
            cbModule.addItem(module.getName());
        }
        if( extendedModules != null )
        {
            for( CollectionDescription et : extendedModules )
            {
                String moduleName = et.getModuleName();
                if( !usedModuleNames.contains(moduleName) )
                {
                    cbModule.addItem(moduleName);
                    usedModuleNames.add(moduleName);
                }
            }
        }
    }
}
