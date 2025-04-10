package ru.biosoft.access.repository;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.application.dialog.OkCancelDialog;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;

import one.util.streamex.IntStreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.journal.JournalRegistry;
import ru.biosoft.util.TextUtil2;

@SuppressWarnings ( "serial" )
public class DataElementPathDialog extends OkCancelDialog
{
    private JButton upButton;
    JList<String> elementList;
    JTextField elementName;
    private JScrollPane scrollPane;
    private DataElementPath value = null;
    private DataElementPathSet values = new DataElementPathSet();
    private DataCollection<?> currentCollection = null;
    private CollectionSelectPopup popup = null;
    private boolean elementMustExist = false;
    private boolean promptOverwrite = false;
    boolean multiSelect = false;
    private Class<? extends DataElement>[] elementClass = null;
    private Class<? extends DataElement> childClass = null;
    private Class<? extends ReferenceType> referenceType = null;
    boolean enabled;
    
    public DataElementPathDialog(String title)
    {
        super(Application.getApplicationFrame(), title);
        init();
    }
    
    public DataElementPathDialog()
    {
        this(null);
    }
    
    protected void init()
    {
        JPanel contentPane = new JPanel(new GridBagLayout());
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        contentPane.add(new JLabel("Collection: ", JLabel.RIGHT), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
        JButton openTreeButton = new JButton();
        openTreeButton.setHorizontalAlignment(JButton.LEFT);
        popup = new CollectionSelectPopup(openTreeButton, null, null);
        popup.setCollectionSelectedListener(value -> setValue(value));
        contentPane.add(openTreeButton, new GridBagConstraints(1, 0, 1, 1, 10.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
        upButton = new JButton("Up");
        contentPane.add(upButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
        upButton.addActionListener(evt -> setValue(value.getParentPath().getSiblingPath("")));
        elementList = new JList<>();
        elementList.setCellRenderer(new MyListCellRenderer());
        elementList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        elementList.setLayoutOrientation(JList.VERTICAL_WRAP);
        elementList.setVisibleRowCount(17);
        elementList.addListSelectionListener(evt -> {
            if(enabled)
            {
                if(multiSelect && elementList.getSelectedValuesList().size()!=1)
                {
                    elementName.setText(String.join(";", elementList.getSelectedValuesList()));
                } else
                {
                    elementName.setText("");
                    if(elementList.getSelectedValue() != null)
                    {
                        String selected = elementList.getSelectedValue();
                        if( isPathAcceptable(getValue().getSiblingPath(selected), childClass, elementClass, referenceType))
                        {
                            elementName.setText(selected);
                        }
                    }
                }
            }
        });
        elementList.addMouseListener(new MouseListener()
        {
            @Override
            public void mouseClicked(MouseEvent evt)
            {
                if(evt.getClickCount() > 1) {
                    if(elementList.getSelectedValue() != null)
                    {
                        DataElementPath newValue = value.getSiblingPath(elementList.getSelectedValue());
                        DataCollection<?> selectedDC = newValue.optDataCollection();
                        // This condition is to decide whether double-click should be considered as entering ru.biosoft.access.core.DataCollection
                        // or selecting element. It was built empirically based on different use-cases
                        if( ! ( selectedDC instanceof FolderCollection )
                                && ( ( ( childClass != null || elementClass != null ) && isPathAcceptable(newValue,
                                        childClass, elementClass, referenceType) ) || ( childClass == null && elementClass == null
                                        && referenceType == null && ( currentCollection.getInfo().isChildrenLeaf() || selectedDC == null ) ) ) )
                            okPressed();
                        else if(selectedDC != null)
                        {
                            setValue(newValue.getChildPath(""));
                            if(newValue.getName().equals(elementName.getText()))
                                elementName.setText("");
                        }
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent evt) {}
            @Override
            public void mousePressed(MouseEvent evt) {}
            @Override
            public void mouseExited(MouseEvent evt) {}
            @Override
            public void mouseEntered(MouseEvent evt) {}
        });
        scrollPane = new JScrollPane(elementList);
        scrollPane.setMinimumSize(new Dimension(1000,1000));
        scrollPane.setBorder(BorderFactory.createLoweredBevelBorder());
        scrollPane.addComponentListener(new ComponentListener()
        {
            @Override
            public void componentResized(ComponentEvent evt)
            {
                elementList.setVisibleRowCount(scrollPane.getViewport().getHeight()/elementList.getFixedCellHeight());
            }
            
            @Override
            public void componentMoved(ComponentEvent evt) {}
            @Override
            public void componentShown(ComponentEvent evt) {}
            @Override
            public void componentHidden(ComponentEvent evt) {}
        });
        contentPane.add(scrollPane, new GridBagConstraints(0, 1, 3, 1, 11.0, 6.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
        elementName = new JTextField();
        contentPane.add(new JLabel("Name: ", JLabel.RIGHT), new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
        contentPane.add(elementName, new GridBagConstraints(1, 2, 2, 1, 11.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
        setContent(contentPane);
        getContentPane().setPreferredSize(new Dimension(700,400));
    }

    protected boolean isPathAcceptable(DataElementPath path, Class<? extends DataElement> childClass,
            Class<? extends DataElement>[] elementClass, Class<? extends ReferenceType> referenceType)
    {
        if(elementClass == null || elementClass.length == 0)
            return DataCollectionUtils.isAcceptable(path, childClass, null, referenceType);

        boolean isAcceptable = false;
        for(Class<? extends DataElement> elClass : elementClass)
        {
            if(DataCollectionUtils.isAcceptable(path, childClass, elClass, referenceType))
                isAcceptable = true;
        }
        return isAcceptable;
    }

    protected boolean isParentPathAcceptable(DataElementPath path, Class<? extends DataElement>[] elementClass)
    {
        if(elementClass == null || elementClass.length == 0)
            return DataCollectionUtils.isAcceptable(path, null, null);

        boolean isAcceptable = false;
        for(Class<? extends DataElement> elClass : elementClass)
        {
            if(DataCollectionUtils.isAcceptable(path, elClass, null))
                isAcceptable = true;
        }
        return isAcceptable;
    }

    public DataElementPath getValue()
    {
        return value;
    }
    
    public DataElementPathSet getValues()
    {
        return values;
    }
    
    @Override
    public String getTitle()
    {
        String title = super.getTitle();
        if(title == null)
        {
            String value = "";
            if( getElementClass() != null )
            {
                for( Class<? extends DataElement> clazz : getElementClass() )
                {
                    value += DataCollectionUtils.getClassTitle( clazz ) + "; ";
                }
            }
            title = (isElementMustExist()?"Select: "+value+(this.multiSelect?"(one or several elements)":""):"Specify name: "+value);
        }
        return title;
    }

    protected void updateNameList()
    {
        List<String> names = currentCollection.getNameList();
        if( names.isEmpty() )
        {
            elementList.setListData(new String[] {"(none)"});
            elementList.setEnabled(false);
        } else
        {
            elementList.setListData(names.toArray(new String[names.size()]));
            elementList.setEnabled(true);
            if(multiSelect && !names.isEmpty())
            {
                DataElementPath parent = DataElementPath.create(currentCollection);
                int[] indices = IntStreamEx.ofIndices( names, name -> values.contains( parent.getChildPath( name ) ) ).toArray();
                elementList.setSelectedIndices(indices);
            } else
            {
                elementList.setSelectedValue(this.value.getName(), true);
            }
        }
        enabled = elementMustExist || isParentPathAcceptable(value.getParentPath(), elementClass);
        okButton.setEnabled(enabled);
    }

    public void setValue(DataElementPath value)
    {
        DataElementPath oldValue = this.value;
        this.value = value;
        currentCollection = null;
        if(this.value != null)
        {
            if(this.value.getName().equals("") && !elementMustExist)
                this.value = this.value.getSiblingPath(elementName.getText());
            currentCollection = this.value.optParentCollection();
            while(currentCollection == null && !this.value.toString().equals(DataElementPath.PATH_SEPARATOR) && !this.value.isEmpty())
            {
                this.value = this.value.getParentPath().getSiblingPath("");
                currentCollection = this.value.optParentCollection();
            }
        }
        if(currentCollection == null)
        {
            DataElementPath preferencesPath = getPathFromPreferences();
            if(preferencesPath != null)
            {
                currentCollection = preferencesPath.optDataCollection();
                this.value = DataElementPath.create(currentCollection, "");
            }
        }
        if(currentCollection == null)
        {
            DataElementPath projectPath = JournalRegistry.getProjectPath();
            if(projectPath != null)
            {
                currentCollection = projectPath.optDataCollection();
            }
            if(currentCollection == null)
                currentCollection = CollectionFactoryUtils.getUserProjectsPath().optDataCollection();
            if(currentCollection == null)
                currentCollection = CollectionFactory.getDataCollection("data");
            this.value = DataElementPath.create(currentCollection, "");
        }
        popup.selectItem(this.value.getParentPath().toString());
        updateNameList();
        upButton.setEnabled(!this.value.getParentPath().getParentPath().isEmpty());
        elementName.setText(this.value.getName());
        firePropertyChange("value", oldValue, this.value);
    }
    
    public void setValue(DataElementPathSet value)
    {
        DataElementPath masterValue = value == null?null:value.first();
        if(masterValue == null)
        {
            setValue(value==null?null:value.getPath().getChildPath(""));
        }
        else
        {
            setValue(masterValue);
        }
        values.clear();
        if(masterValue != null)
            values.add(masterValue);
        if(masterValue == null || !multiSelect) return;
        value.stream().filter( masterValue::isSibling ).forEach( values::add );
        String names = value.stream().map( DataElementPath::getName ).collect( Collectors.joining( ";" ) );
        updateNameList();
        elementName.setText(names);
    }

    @Override
    protected void okPressed()
    {
        String name = elementName.getText();
        DataElementPathSet selValues = new DataElementPathSet();
        if(name.equals(""))
        {
            selValues.add(value.getParentPath());
        } else if(name.contains(DataElementPath.PATH_SEPARATOR))
        {
            ApplicationUtils.errorBox("Error", "Invalid element name");
            return;
        } else
        {
            if(multiSelect)
            {
                selValues.clear();
                for(String val: TextUtil2.split(name, ';'))
                {
                    selValues.add(value.getSiblingPath(val));
                }
            } else
            {
                selValues.add(value.getSiblingPath(name));
            }
        }
        if(elementMustExist)
        {
            for(DataElementPath selValue: selValues)
            {
                if(!selValue.exists())
                {
                    ApplicationUtils.errorBox("Error", "This element does not exist: "+selValue.getName());
                    return;
                }
                if(!isPathAcceptable(selValue, childClass, elementClass, referenceType))
                {
                    ApplicationUtils.errorBox("Error", "This element has inacceptable type: "+selValue.getName());
                    return;
                }
            }
        }
        if(promptOverwrite)
        {
            for(DataElementPath selValue: selValues)
            {
                if( selValue.optDataElement() != null
                        && !ApplicationUtils.dialogAreYouSure(this, "This element already exists: " + selValue.getName()
                                + ". Do you want to overwrite it?") )
                    return;
            }
        }
        value = selValues.first();
        values = selValues;
        storePathToPreferences();
        super.okPressed();
    }

    public boolean isElementMustExist()
    {
        return elementMustExist;
    }

    public void setElementMustExist(boolean elementMustExist)
    {
        Object oldValue = this.elementMustExist;
        this.elementMustExist = elementMustExist;
        firePropertyChange("elementMustExists", oldValue, this.elementMustExist);
    }

    public boolean isPromptOverwrite()
    {
        return promptOverwrite;
    }

    public void setPromptOverwrite(boolean promptOverwrite)
    {
        Object oldValue = this.promptOverwrite;
        this.promptOverwrite = promptOverwrite;
        firePropertyChange("promptOverwrite", oldValue, this.promptOverwrite);
    }

    public boolean isMultiSelect()
    {
        return multiSelect;
    }

    /**
     * Sets whether multi select is allowed (currently works only when elementMustExist flag is set)
     */
    public void setMultiSelect(boolean multiSelect)
    {
        Object oldValue = this.multiSelect;
        this.multiSelect = multiSelect;
        elementList.setSelectionMode(multiSelect?ListSelectionModel.MULTIPLE_INTERVAL_SELECTION:ListSelectionModel.SINGLE_SELECTION);
        firePropertyChange("multiSelect", oldValue, this.multiSelect);
    }

    public Class<? extends DataElement>[] getElementClass()
    {
        return elementClass;
    }

    public void setElementClass(Class<? extends DataElement>... elementClass)
    {
        Object oldValue = this.elementClass;
        this.elementClass = elementClass;
        firePropertyChange("elementClass", oldValue, this.elementClass);
    }

    public Class<? extends DataElement> getChildClass()
    {
        return childClass;
    }

    public void setChildClass(Class<? extends DataElement> childClass)
    {
        Object oldValue = this.childClass;
        this.childClass = childClass;
        firePropertyChange("childClass", oldValue, this.childClass);
    }
    
    /**
     * @return the referenceType
     */
    public Class<? extends ReferenceType> getReferenceType()
    {
        return referenceType;
    }

    /**
     * @param referenceType the referenceType to set
     */
    public void setReferenceType(Class<? extends ReferenceType> referenceType)
    {
        Object oldValue = this.referenceType;
        this.referenceType = referenceType;
        firePropertyChange("referenceType", oldValue, referenceType);
    }
    
    /**
     * Preferences management
     * Try to set value from preferences using childClass
     * @author anna
     *
     */
    public static final String DATA_COLLECTION_PREFERENCE = "DataCollectionPaths";
    
    public static DataElementPath getDefaultPath(Class<? extends DataElement> clazz)
    {
        DataElementPath basePath = DataElementPathDialog.getPathFromPreferences( clazz );
        if(basePath == null)
        {
            DataElementPath projectPath = JournalRegistry.getProjectPath();
            if(projectPath == null)
                basePath = DataElementPath.create( "data" );
            else
                basePath = projectPath.getChildPath( "Data" );
        }
        return basePath;
    }

    public static DataElementPath getPathFromPreferences(Class<? extends DataElement> clazz)
    {
        Preferences preferences = Application.getPreferences();
        String key = DATA_COLLECTION_PREFERENCE + "/" + getPreferencesKey( clazz );
        String value = preferences.getStringValue(key, null);
        return DataElementPath.create(value);
    }

    public static void storePathToPreferences(Class<? extends DataElement> clazz, DataElementPath path)
    {
        String key = getPreferencesKey(clazz);
        Preferences preferences = Application.getPreferences();
        String pathValue = path.toString();
        
        try
        {
            Preferences properties = preferences.getPreferencesValue(DATA_COLLECTION_PREFERENCE);
            if( properties == null )
            {
                properties = new Preferences();
                preferences.add(new DynamicProperty(DATA_COLLECTION_PREFERENCE, Preferences.class, properties));
            }
            properties.add(new DynamicProperty(key, String.class, pathValue));
        }
        catch( Exception e )
        {
        }
    }

    private static String getPreferencesKey(Class<? extends DataElement> clazz)
    {
        return clazz == null ? "(default)" : clazz.getSimpleName();
    }

    private DataElementPath getPathFromPreferences()
    {
        return getPathFromPreferences( childClass == null ? (elementClass == null || elementClass.length == 0 ? null : elementClass[0]) : childClass );
    }

    private void storePathToPreferences()
    {
        Class<? extends DataElement> clazz = childClass == null ? (elementClass == null || elementClass.length == 0 ? null : elementClass[0]) : childClass;
        storePathToPreferences( clazz, value.getParentPath() );
    }

    private class MyListCellRenderer extends DefaultListCellRenderer
    {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            Component result = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if(result instanceof JLabel)
            {
                ((JLabel)result).setText(value.toString());
                DataElementPath path = getValue().getSiblingPath(value.toString());
                try
                {
                    if(list.isEnabled())
                        ((JLabel)result).setIcon(popup.getItemIcon(path));
                }
                catch( Exception e )
                {
                }
                ((JLabel)result).setEnabled(isPathAcceptable(path, childClass, elementClass, referenceType));
            }
            return result;
        }
    }
}
