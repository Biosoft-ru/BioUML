package ru.biosoft.access.support;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Method;

import javax.accessibility.AccessibleContext;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import one.util.streamex.EntryStream;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.application.action.ActionInitializer;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.Index;

/**
 * This pane allows a user to select several {@link ru.biosoft.access.core.DataElement} names from
 * {@link ru.biosoft.access.core.DataCollection} name list.
 *
 * This pane consists from three panels.
 * First panel contains {@link JList} containing names of all <code>ru.biosoft.access.core.DataElement</code>s from the
 * <code>DataCollection</code>.
 * Second panel contains buttons to add/remove selected components.
 * Third panel contains {@link JList} with names of selected <code>ru.biosoft.access.core.DataElement</code>s.
 *
 * @pending high use <code>JList.setPrototypeCellValue</code> to optimize showing of big lists.
 * @pending use MessageBundle for label and button titles
 * @pending use actions for button initialization
 */
@SuppressWarnings ( "serial" )
public class DataCollectionMultyChoicePane extends JPanel
{
    protected static final Logger log = Logger.getLogger(DataCollectionMultyChoicePane.class.getName());

    /** DataCollection from which data element names are selected. */
    protected DataCollection dataCollection;

    /** Indicates whether selected values should be sorted. */
    protected boolean isSorted;

    protected String[] selectedValues;

    protected ListModel<String> possibleValuesModel;
    protected DefaultListModel<String> selectedValuesModel;

    protected Index queryIndex;

    ////////////////////////////////////////
    // Actions
    //
    public static final int TOOLBAR_BUTTON_SIZE = 20;

    public static final String ADD = "Add";
    public static final String REMOVE = "Remove";
    public static final String REMOVE_ALL = "Remove all";
    public static final String NEW_ELEMENT = "New";

    protected Action addAction = new AddAction(ADD);
    protected Action removeAction = new RemoveAction(REMOVE);
    protected Action removeAllAction = new RemoveAllAction(REMOVE_ALL);
    protected Action newElementAction = new NewElementAction(NEW_ELEMENT);

    ////////////////////////////////////////
    // Visual components
    //
    protected JList<String> listPossibleValues = new JList<>();
    protected JList<String> listSelectedValues = new JList<>();

    protected JTextField queryField = new JTextField();

    protected JButton bAdd;
    protected JButton bRemove;
    protected JButton bRemoveAll;
    protected JButton bNew;

    ////////////////////////////////////////
    // Constructors
    //
    public DataCollectionMultyChoicePane(DataCollection<?> dataCollection, String[] selectedValues, boolean isSorted)
    {
        this(dataCollection, null, selectedValues, isSorted);
    }

    public DataCollectionMultyChoicePane(DataCollection<?> dataCollection, String property, String[] selectedValues, boolean isSorted)
    {
        this.dataCollection = dataCollection;
        this.selectedValues = selectedValues.clone();
        this.isSorted = isSorted;

        if( property != null && dataCollection.getInfo().getQuerySystem() != null
                && dataCollection.getInfo().getQuerySystem().getIndex(property) != null )
        {
            queryIndex = dataCollection.getInfo().getQuerySystem().getIndex(property);
        }

        init();
        paint();
    }

    ////////////////////////////////////////
    // Methods
    //
    private void init()
    {
        if( queryIndex != null )
        {
            if( queryIndex instanceof ListModel )
                possibleValuesModel = (ListModel<String>)queryIndex;
        }

        if( possibleValuesModel == null )
            possibleValuesModel = new NameListToListModelAdapter(dataCollection);
        listPossibleValues.setModel(possibleValuesModel);
        listPossibleValues.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        selectedValuesModel = new DefaultListModel<>();
        setSelectedValues(selectedValues);
        listSelectedValues.setModel(selectedValuesModel);
        listSelectedValues.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        initToolbar();
        initListeners();
    }

    protected JToolBar toolbar;
    private void initToolbar()
    {
        toolbar = new JToolBar(SwingConstants.VERTICAL);
        toolbar.setFloatable(false);

        initButtons();

        toolbar.add(bAdd);
        toolbar.add(new JToolBar.Separator(new Dimension(0, TOOLBAR_BUTTON_SIZE / 2)));
        toolbar.add(bRemove);
        toolbar.add(new JToolBar.Separator(new Dimension(0, TOOLBAR_BUTTON_SIZE / 2)));
        toolbar.add(bRemoveAll);
        toolbar.add(new JToolBar.Separator(new Dimension(0, TOOLBAR_BUTTON_SIZE / 2)));
        toolbar.add(bNew);
    }

    private void initButtons()
    {
        initActions();

        bAdd = new JButton(addAction);
        bRemove = new JButton(removeAction);
        bRemoveAll = new JButton(removeAllAction);
        bNew = new JButton(newElementAction);

        configureButton(bAdd, addAction);
        configureButton(bRemove, removeAction);
        configureButton(bRemoveAll, removeAllAction);
        configureButton(bNew, newElementAction);
    }

    private void configureButton(AbstractButton button, Action action)
    {
        button.setAlignmentY(0.5f);

        Dimension btnSize = new Dimension(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE);
        button.setSize(btnSize);
        button.setPreferredSize(btnSize);
        button.setMinimumSize(btnSize);
        button.setMaximumSize(btnSize);

        if( button.getIcon() != null )
            button.setText(null);
        else
            button.setText((String)action.getValue(Action.NAME));
    }

    private void initListeners()
    {
        listPossibleValues.addMouseListener(new MouseListener()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if( e.getClickCount() > 1 )
                    addElement(listPossibleValues.getSelectedValue());
            }

            @Override
            public void mouseEntered(MouseEvent e)
            {
            }
            @Override
            public void mouseExited(MouseEvent e)
            {
            }
            @Override
            public void mousePressed(MouseEvent e)
            {
            }
            @Override
            public void mouseReleased(MouseEvent e)
            {
            }
        });

        listSelectedValues.addMouseListener(new MouseListener()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if( e.getClickCount() > 1 )
                    removePublication(listSelectedValues.getSelectedValue());
            }

            @Override
            public void mouseEntered(MouseEvent e)
            {
            }
            @Override
            public void mouseExited(MouseEvent e)
            {
            }
            @Override
            public void mousePressed(MouseEvent e)
            {
            }
            @Override
            public void mouseReleased(MouseEvent e)
            {
            }
        });

        queryField.getAccessibleContext().addPropertyChangeListener(arg0 -> {
            if( arg0 != null && AccessibleContext.ACCESSIBLE_TEXT_PROPERTY.equalsIgnoreCase(arg0.getPropertyName())
                    && queryIndex != null )
            {
                try
                {
                    Method method = queryIndex.getClass().getMethod("setConstraint", new Class[] {String.class});
                    if( method != null && listPossibleValues.getVisibleRowCount() > 0 )
                    {
                        method.invoke(queryIndex, new Object[] {queryField.getText()});
                        listPossibleValues.repaint();
                    }
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE, "Query system error", t);
                }
            }
        });
    }

    public void initActions()
    {
        ActionInitializer initializer = new ActionInitializer(MessageBundle.class);
        initializer.initAction(addAction, ADD);
        initializer.initAction(removeAction, REMOVE);
        initializer.initAction(removeAllAction, REMOVE_ALL);
        initializer.initAction(newElementAction, NEW_ELEMENT);
    }

    /** Returns selected values. */
    public String[] getSelectedValues()
    {
        String[] values = new String[selectedValuesModel.size()];
        for( int i = 0; i < selectedValuesModel.size(); i++ )
            values[i] = selectedValuesModel.get(i);
        return values;
    }

    /** Sets selected values. */
    public void setSelectedValues(String[] values)
    {
        if( values != null )
        {
            EntryStream.of(values).forKeyValue( selectedValuesModel::add );
        }
    }

    protected JLabel lPossibleValues = new JLabel("Possible values:");
    protected JLabel lSelectedValues = new JLabel("Selected values:");
    private void paint()
    {
        Insets insets = new Insets(5, 5, 5, 5);

        JScrollPane pPossibleValues = new JScrollPane(listPossibleValues);
        JScrollPane pSelectedValues = new JScrollPane(listSelectedValues);

        setLayout(new GridBagLayout());

        add(lPossibleValues, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insets, 0, 0));
        add(pPossibleValues, new GridBagConstraints(0, 1, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 0, 0));
        add(toolbar, new GridBagConstraints(1, 1, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 0, 0));
        add(lSelectedValues, new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insets, 0, 0));
        add(pSelectedValues, new GridBagConstraints(2, 1, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 0, 0));
        if( queryIndex != null )//field is unuseful without query index
        {
            add(queryField, new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Actions
    //
    protected class AddAction extends AbstractAction
    {
        public AddAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            addElement(listPossibleValues.getSelectedValue());
        }
    }

    protected class RemoveAction extends AbstractAction
    {
        public RemoveAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            removePublication(listSelectedValues.getSelectedValue());
        }
    }

    protected class RemoveAllAction extends AbstractAction
    {
        public RemoveAllAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            selectedValuesModel.removeAllElements();
        }
    }

    protected class NewElementAction extends AbstractAction
    {
        public NewElementAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            newPossibleElement();
        }
    }

    /**
     * Add new element to the list.
     *
     * @param value name of added data element.
     *
     * @pending optimization
     */
    private void addElement(String value)
    {
        if( value == null || selectedValuesModel.contains(value) )
            return;

        int index;
        if( !isSorted )
            index = selectedValuesModel.size();
        else
        {
            index = 0;
            while( index < selectedValuesModel.size() )
            {
                String s = selectedValuesModel.get(index);
                if( s.compareTo(value) > 0 )
                    break;

                index++;
            }
        }
        selectedValuesModel.add(index, value);
    }

    /**
     * Removes selected publication.
     *
     * @param value the data element name to be removed.
     */
    private void removePublication(String value)
    {
        if( value != null )
            selectedValuesModel.removeElement(value);
    }

    /**
     * Creates new element.
     */
    private void newPossibleElement()
    {
        NewDataElementDialog dialog = new NewDataElementDialog(DataCollectionMultyChoicePane.this, "New data element dialog",
                dataCollection);
        if( dialog.doModal() )
        {
            DataElement de = dialog.getNewDataElement();
            if( de != null )
            {
                try
                {
                    dataCollection.put(de);
                    listPossibleValues.setSelectedValue(de.getName(), true);
                }
                catch( Throwable t )
                {
                    String msg = "Can not create new data element: " + t;
                    log.log(Level.SEVERE, msg, t);
                    JOptionPane.showMessageDialog(DataCollectionMultyChoicePane.this, msg, "", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}