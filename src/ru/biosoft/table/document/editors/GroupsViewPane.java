package ru.biosoft.table.document.editors;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.gui.Document;
import ru.biosoft.table.MessageBundle;
import ru.biosoft.table.SampleGroup;
import ru.biosoft.table.TableDataCollection;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.swing.TabularPropertyInspector;
import com.developmentontheedge.application.action.ActionInitializer;

@SuppressWarnings ( "serial" )
public class GroupsViewPane extends JPanel
{
    protected static final Logger log = Logger.getLogger(GroupsViewPane.class.getName());

    // tableData
    protected TableDataCollection tableData;
    protected TabularPropertyInspector table;
    protected JScrollPane scrollPane;

    private Document document;

    public static final String FIRST_COLUMN_NAME = "Group";
    public static final String SECOND_COLUMN_NAME = "Description";
    public static final String THIRD_COLUMN_NAME = "Samples";

    public static final String ADD_COLUMN_ACTION = "GroupsViewPane.AddColumnAction";
    public static final String REMOVE_COLUMN_ACTION = "GroupsViewPane.RemoveColumnAction";

    protected Action[] actions;
    protected Action addColumnAction = new AddColumnAction(ADD_COLUMN_ACTION);
    protected Action removeColumnAction = new RemoveColumnAction(REMOVE_COLUMN_ACTION);

    protected GroupElement selectedElement;

    public GroupsViewPane()
    {
        super();

        setLayout(new GridBagLayout());

        table = new TabularPropertyInspector();
        table.getTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSortEnabled(false);
        table.addPropertyChangeListener(evt -> {
            if( document != null )
                document.update();
        });
        table.addListSelectionListener(event -> {
            Object model = table.getModelOfSelectedRow();
            if( model instanceof GroupElement )
            {
                selectedElement = (GroupElement)model;
                removeColumnAction.setEnabled(true);
            }
        });

        scrollPane = new JScrollPane(table);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane, new GridBagConstraints(0, 0, 2, 1, 1.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(10, 0,
                0, 0), 0, 0));

        selectedElement = null;
        removeColumnAction.setEnabled(false);
    }

    public void explore(TableDataCollection me, Document document)
    {
        this.tableData = me;
        this.document = document;

        selectedElement = null;
        removeColumnAction.setEnabled(false);

        GroupElement[] elements = new GroupElement[me.getGroups().getSize()];
        for( int i = 0; i < me.getGroups().getSize(); i++ )
        {
            elements[i] = new GroupElement(me, i);
        }
        table.explore(elements);
    }

    protected void addColumnAction()
    {
        try
        {
            selectedElement = null;
            removeColumnAction.setEnabled(false);

            DataCollection<SampleGroup> groups = tableData.getGroups();
            groups.put(new SampleGroup(groups, "New group"));
            explore(tableData, document);
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "can't create new group", t);
        }
    }
    protected void removeColumnAction()
    {
        if( selectedElement != null
                && JOptionPane.showConfirmDialog(this, "Do you really want to remove group '" + selectedElement.getGroup() + "'") == JOptionPane.OK_OPTION )
        {
            try
            {
                tableData.getGroups().remove(selectedElement.getGroup());
                explore(tableData, document);

                selectedElement = null;
                removeColumnAction.setEnabled(false);
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "can't delete group", t);
            }
        }
    }

    public Action[] getActions()
    {
        if( actions == null )
        {
            ActionInitializer initializer = new ActionInitializer(MessageBundle.class);
            initializer.initAction(addColumnAction, GroupsViewPane.ADD_COLUMN_ACTION);
            initializer.initAction(removeColumnAction, GroupsViewPane.REMOVE_COLUMN_ACTION);

            actions = new Action[] {addColumnAction, removeColumnAction};
        }

        return actions;
    }

    /////////////////////
    //table element bean
    /////////////////////
    public static class GroupElement extends Option
    {
        protected TableDataCollection me;
        protected int row;
        protected String name;

        public GroupElement(TableDataCollection me, int row)
        {
            this.me = me;
            this.row = row;
            name = me.getGroups().getNameList().get(row);
        }

        public String getGroup()
        {
            return name;
        }

        public void setGroup(String group)
        {
            try
            {
                DataCollection<SampleGroup> groups = me.getGroups();
                SampleGroup newGroup = new SampleGroup(groups, group);
                SampleGroup oldGroup = groups.get(name);
                newGroup.setDescription(oldGroup.getDescription());
                newGroup.setPattern(oldGroup.getPattern());
                groups.remove(name);
                groups.put(newGroup);
                name = group;
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "can't change group name", t);
            }
        }

        public String getDescription()
        {
            try
            {
                return me.getGroups().get(name).getDescription();
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "can't get group description", t);
            }
            return null;
        }

        public void setDescription(String description)
        {
            try
            {
                me.getGroups().get(name).setDescription(description);
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "can't change description", t);
            }
        }

        public String getSamples()
        {
            try
            {
                return me.getGroups().get(name).getPattern();
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "can't get group pattern", t);
            }
            return null;
        }

        public void setSamples(String samples)
        {
            try
            {
                if( samples != null && samples.matches("(((\\d+)|(\\d+-\\d+))[,;])*") )
                {
                    me.getGroups().get(name).setPattern(samples);
                }
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "can't change samples", t);
            }
        }
    }

    public static class GroupElementBeanInfo extends BeanInfoEx
    {
        public GroupElementBeanInfo()
        {
            this(GroupElement.class, "GROUP_EDITOR", "ru.biosoft.table.MessageBundle");
        }

        protected GroupElementBeanInfo(Class<? extends GroupElement> beanClass, String key, String messageBundle)
        {
            super(beanClass, messageBundle);
            if( key != null && messageBundle != null )
            {
                beanDescriptor.setDisplayName(getResourceString("CN_" + key));
                beanDescriptor.setShortDescription(getResourceString("CD_" + key));
            }
        }

        @Override
        public void initProperties() throws Exception
        {
            initResources("ru.biosoft.table.MessageBundle");

            PropertyDescriptorEx pde;

            pde = new PropertyDescriptorEx("group", beanClass, "getGroup", "setGroup");
            add(pde, getResourceString("PN_GROUP_EDITOR_GROUP"), getResourceString("PD_GROUP_EDITOR_GROUP"));

            pde = new PropertyDescriptorEx("description", beanClass, "getDescription", "setDescription");
            add(pde, getResourceString("PN_GROUP_EDITOR_DESCRIPTION"), getResourceString("PD_GROUP_EDITOR_DESCRIPTION"));

            pde = new PropertyDescriptorEx("samples", beanClass, "getSamples", "setSamples");
            add(pde, getResourceString("PN_GROUP_EDITOR_SAMPLES"), getResourceString("PD_GROUP_EDITOR_SAMPLES"));
        }
    }


    ////////////////////////
    // actions
    ///////////////////////
    public class AddColumnAction extends AbstractAction
    {
        public AddColumnAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            addColumnAction();
        }
    }

    public class RemoveColumnAction extends AbstractAction
    {
        public RemoveColumnAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            removeColumnAction();
        }
    }
}
