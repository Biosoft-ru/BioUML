package biouml.workbench.module.xml.editor;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.logging.Level;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.util.logging.Logger;
import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.swing.TabularPropertyInspector;
import com.developmentontheedge.beans.swing.table.AbstractRowModel;
import com.developmentontheedge.beans.swing.table.Column;
import com.developmentontheedge.beans.swing.table.ColumnModel;
import com.developmentontheedge.beans.swing.table.ColumnWithSort;

public class RichTableControl extends JPanel
{
    protected Logger log = Logger.getLogger(RichTableControl.class.getName());
    protected MessageBundle messageBundle = (MessageBundle)ResourceBundle.getBundle(MessageBundle.class.getName());

    protected TabularPropertyInspector table;
    protected PropertyInspector propertyInspector;
    protected JButton addButton;
    protected JButton removeButton;

    protected List objects;
    protected Object activeType;
    protected Class elementType;

    protected ColumnModel columnModel;

    public RichTableControl(List objects, Class elementType, String[] fields)
    {
        this.objects = objects;
        this.elementType = elementType;
        initColumnModel(fields);

        setLayout(new GridBagLayout());

        table = new TabularPropertyInspector();
        table.setPreferredSize(new Dimension(450, 200));
        table.addListSelectionListener(new ListSelectionListener()
        {
            @Override
            public void valueChanged(ListSelectionEvent e)
            {
                selectItem();
            }
        });
        JScrollPane typesScrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        refreshTabularInspector();
        add(typesScrollPane, new GridBagConstraints(0, 0, 3, 1, 1.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0,
                0, 0, 0), 0, 0));

        addButton = new JButton(messageBundle.getResourceString("TAB_ADD_BUTTON"));
        addButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                addAction();
            }
        });
        add(addButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));

        removeButton = new JButton(messageBundle.getResourceString("TAB_REMOVE_BUTTON"));
        removeButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                removeAction();
            }
        });
        add(removeButton, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0,
                0, 0), 0, 0));

        propertyInspector = new PropertyInspector();
        propertyInspector.setPreferredSize(new Dimension(450, 150));
        JScrollPane detailsScrollPane = new JScrollPane(propertyInspector, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(detailsScrollPane, new GridBagConstraints(0, 2, 3, 1, 1.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(
                0, 0, 0, 0), 0, 0));
    }

    protected void initColumnModel(String[] fields)
    {
        if( fields != null )
        {
            Column[] columns = new Column[fields.length];
            for( int i = 0; i < fields.length; i++ )
            {
                String displayName = "";
                try
                {
                    for( PropertyDescriptor pd : Introspector.getBeanInfo(elementType).getPropertyDescriptors() )
                    {
                        if( pd.getName().equals(fields[i]) )
                        {
                            displayName = pd.getDisplayName();
                        }
                    }
                }
                catch( Exception e )
                {
                }
                columns[i] = new ColumnWithSort(null, fields[i], displayName);
            }
            columnModel = new ColumnModel(columns);
        }
    }

    protected void refreshTabularInspector()
    {
        if( objects == null || objects.size() == 0 )
        {
            table.explore(new Object[0]);
        }
        else
        {
            if( columnModel == null )
            {
                table.explore(objects.toArray());
            }
            else
            {
                table.explore(new AbstractRowModel()
                {
                    @Override
                    public int size()
                    {
                        return objects.size();
                    }
                    @Override
                    public Object getBean(int index)
                    {
                        return objects.get(index);
                    }
                }, columnModel);
            }
        }
    }

    protected void selectItem()
    {
        Object obj = table.getModelOfSelectedRow();
        if( obj != null )
        {
            activeType = obj;
            propertyInspector.explore(obj);
            propertyInspector.setRootVisible(false);
            setEnable(propertyInspector, true);
        }
    }
    protected void removeAction()
    {
        if( activeType != null )
        {
            objects.remove(activeType);
            activeType = null;

            setEnable(propertyInspector, false);
            refreshTabularInspector();
        }
    }

    protected void addAction()
    {
        try
        {
            activeType = elementType.newInstance();
            objects.add(activeType);

            propertyInspector.explore(activeType);
            propertyInspector.setRootVisible(false);
            setEnable(propertyInspector, true);
            refreshTabularInspector();
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Can not create new element", t);
        }
    }

    private void setEnable(Component component, boolean enable)
    {
        if( component instanceof Container )
        {
            for( Component c : ( (Container)component ).getComponents() )
            {
                c.setEnabled(enable);
                setEnable(c, enable);
            }
        }
    }
}
