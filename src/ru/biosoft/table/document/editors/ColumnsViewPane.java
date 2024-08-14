package ru.biosoft.table.document.editors;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.gui.Document;
import ru.biosoft.table.MessageBundle;
import ru.biosoft.table.Sample;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.IconUtils;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.editors.CustomEditorSupport;
import com.developmentontheedge.beans.editors.StringTagEditorSupport;
import com.developmentontheedge.beans.swing.TabularPropertyInspector;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.dialog.TextButtonField;

@SuppressWarnings ( "serial" )
public class ColumnsViewPane extends JPanel
{
    static final Logger log = Logger.getLogger(ColumnsViewPane.class.getName());

    protected TableDataCollection tableData;
    protected TabularPropertyInspector table;
    protected JScrollPane scrollPane;

    protected Document document;

    public static final String RECALCULATE_DOCUMENT_ACTION = "ColumnsViewPane.RecalculateDocumentAction";
    public static final String ADD_COLUMN_ACTION = "ColumnsViewPane.AddColumnAction";
    public static final String REMOVE_COLUMN_ACTION = "ColumnsViewPane.RemoveColumnAction";

    protected Action[] actions;
    protected Action addColumnAction = new AddColumnAction(ADD_COLUMN_ACTION);
    protected Action removeColumnAction = new RemoveColumnAction(REMOVE_COLUMN_ACTION);

    protected TableElement selectedColumn;
    protected TableElement[] columns;

    public ColumnsViewPane()
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
            if( model instanceof TableElement )
            {
                selectedColumn = (TableElement)model;
                if( selectedColumn.getRow() >= 0 )
                {
                    removeColumnAction.setEnabled(true);
                }
                else
                {
                    removeColumnAction.setEnabled(false);
                }
            }
        });
        scrollPane = new JScrollPane(table);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane, new GridBagConstraints(0, 0, 2, 1, 1.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(10, 0,
                0, 0), 0, 0));

        removeColumnAction.setEnabled(false);
    }

    public void explore(TableDataCollection me, Document document)
    {
        this.tableData = me;
        this.document = document;

        selectedColumn = null;
        removeColumnAction.setEnabled(false);

        columns = new TableElement[me.getColumnModel().getColumnCount() + 1];
        columns[0] = new TableElement(me, -1, document);
        for( int i = 0; i < me.getColumnModel().getColumnCount(); i++ )
        {
            columns[i + 1] = new TableElement(me, i, document);
        }
        table.explore(columns);
    }

    protected void addColumnAction()
    {
        try
        {
            tableData.getColumnModel().addColumn(tableData.getColumnModel().generateUniqueColumnName(), String.class);
            selectedColumn = null;
            removeColumnAction.setEnabled(false);

            explore(tableData, document);
            this.document.update();
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "can't create new column", t);
        }
    }

    protected void removeColumnAction()
    {
        if( selectedColumn != null && selectedColumn.getRow() >= 0
                && JOptionPane.showConfirmDialog(this, "Do you really want to remove column with all data") == JOptionPane.OK_OPTION )
        {
            tableData.getColumnModel().removeColumn(selectedColumn.getRow());
            explore(tableData, document);
            this.document.update();
        }
    }

    public Action[] getActions()
    {
        if( actions == null )
        {
            ActionInitializer initializer = new ActionInitializer(MessageBundle.class);
            initializer.initAction(addColumnAction, ColumnsViewPane.ADD_COLUMN_ACTION);
            initializer.initAction(removeColumnAction, ColumnsViewPane.REMOVE_COLUMN_ACTION);

            actions = new Action[] {addColumnAction, removeColumnAction};
        }

        return actions;
    }

    //actions
    private class AddColumnAction extends AbstractAction
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

    private class RemoveColumnAction extends AbstractAction
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

    //table element bean
    public static class TableElement extends Option implements DataElement
    {
        protected TableDataCollection me;
        protected int row;
        protected boolean reallyVisible;
        protected Document document;
        public TableElement(TableDataCollection me, int row)
        {
            this(me, row, null);
        }
        public TableElement(TableDataCollection me, int row, Document document)
        {
            this.me = me;
            this.row = row;
            this.document = document;
        }

        public boolean isReallyVisible()
        {
            if( row == -1 )
            {
                return true;
            }

            reallyVisible = !me.getColumnModel().getColumn(row).isHidden();
            return reallyVisible;
        }

        public void setReallyVisible(boolean isVisible)
        {
            if( row != -1 )
            {
                reallyVisible = isVisible;
                me.getColumnModel().getColumn(row).setHidden( !reallyVisible);
            }
        }

        public int getRow()
        {
            return row;
        }

        public int getVisibleRow()
        {
            return row+1;
        }

        public String getColumnName()
        {
            if( row == -1 )
            {
                return "id";
            }
            return me.getColumnModel().getColumn(row).getName();
        }

        public void setColumnName(String columnName)
        {
            if( row != -1 )
            {
                me.getColumnModel().getColumn(row).setName(columnName);
                if( document != null )
                    document.update();
            }
        }

        public String getDescription()
        {
            if( row == -1 )
            {
                return "";
            }
            return me.getColumnModel().getColumn(row).getShortDescription();
        }

        public void setDescription(String description)
        {
            if( row != -1 )
            {
                me.getColumnModel().getColumn(row).setShortDescription(description);
            }
        }

        public String getType()
        {
            if( row == -1 )
            {
                return DataType.Text.toString();
            }
            return me.getColumnModel().getColumn(row).getType().toString();
        }

        public void setType(String typeName)
        {
            if( row != -1 )
            {
                DataType newType = DataType.fromString(typeName);
                me.getColumnModel().getColumn(row).setType(newType);
                if( document != null )
                    document.update();
            }
        }

        public String getNature()
        {
            if( row != -1 )
            {
                return me.getColumnModel().getColumn(row).getNature().toString();
            }

            return TableColumn.Nature.NONE.toString();
        }

        public void setNature(String strNewNature)
        {
            if( row != -1 )
            {
                TableColumn.Nature newNature = TableColumn.Nature.valueOf(strNewNature);

                TableColumn col = me.getColumnModel().getColumn(row);
                if( newNature != TableColumn.Nature.SAMPLE && col.getNature() == TableColumn.Nature.SAMPLE )
                {
                    try
                    {
                        me.getSamples().remove(col.getName());
                    }
                    catch( Exception e )
                    {
                        e.printStackTrace();
                    }
                }

                if( newNature == TableColumn.Nature.SAMPLE )
                {
                    Sample sample = new Sample(me.getSamples(), col.getName());
                    fillNewSampleAttributes(sample);
                    col.setSample(sample);
                    try
                    {
                        me.getSamples().put(sample);
                    }
                    catch( Throwable t )
                    {
                        log.log(Level.SEVERE, "can't add sample", t);
                    }
                }

                col.setNature(newNature);
            }
        }

        public String getExpression()
        {
            if( row != -1 )
            {
                TableColumn column = me.getColumnModel().getColumn(row);
                Object value = column.getExpression();
                return value == null ? "" : value.toString();
            }
            return "";
        }

        public void setExpression(String expression)
        {
            if( row != -1 )
            {
                TableColumn column = me.getColumnModel().getColumn(row);
                //boolean isNotificationEnabled = me.isNotificationEnabled();
                //me.setNotificationEnabled(false);
                column.setExpression(expression);
                //me.setNotificationEnabled(isNotificationEnabled);
            }
        }

        public Boolean isReadOnly()
        {
            return row == -1;
        }

        public boolean isExpressionDisabled()
        {
            return row == -1 || me.getColumnModel().getColumn(row).isExpressionLocked();
        }

        private void fillNewSampleAttributes(Sample sample)
        {
            if( me.getSamples().getSize() > 0 )
            {
                Sample template = me.getSamples().iterator().next();
                Iterator<String> iter = template.getAttributes().nameIterator();
                while( iter.hasNext() )
                {
                    String pName = iter.next();
                    try
                    {
                        sample.getAttributes().add(new DynamicProperty(pName, String.class));
                    }
                    catch( Throwable t )
                    {
                        log.log(Level.SEVERE, "can't get sample property", t);
                    }
                }
            }
        }
        public TableDataCollection getTable()
        {
            return me;
        }

        @Override
        public String getName()
        {
            return String.valueOf( row );
        }

        @Override
        public DataCollection getOrigin()
        {
            return me;
        }
    }

    public static class TableElementBeanInfo extends BeanInfoEx
    {
        public TableElementBeanInfo()
        {
            this(TableElement.class, "COLUMN_EDITOR", "ru.biosoft.table.MessageBundle");
        }

        protected TableElementBeanInfo(Class beanClass, String key, String messageBundle)
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
            initResources(MessageBundle.class.getName());

            PropertyDescriptorEx pde;

            pde = new PropertyDescriptorEx("row", beanClass, "getVisibleRow", null);
            add(pde, getResourceString("PN_COLUMN_EDITOR_ROW_NUMBER"), getResourceString("PD_COLUMN_EDITOR_ROW_NUMBER"));

            pde = new PropertyDescriptorEx("columnName", beanClass, "getColumnName", "setColumnName");
            pde.setReadOnly(beanClass.getMethod("isReadOnly"));
            add(pde, getResourceString("PN_COLUMN_EDITOR_COLUMN_NAME"), getResourceString("PD_COLUMN_EDITOR_COLUMN_NAME"));

            pde = new PropertyDescriptorEx("type", beanClass, "getType", "setType");
            pde.setReadOnly(beanClass.getMethod("isReadOnly"));
            add(pde, TypeEditor.class, getResourceString("PN_COLUMN_EDITOR_COLUMN_TYPE"), getResourceString("PD_COLUMN_EDITOR_COLUMN_TYPE"));

            pde = new PropertyDescriptorEx("description", beanClass);
            pde.setReadOnly(beanClass.getMethod("isReadOnly"));
            add(pde, getResourceString("PN_COLUMN_EDITOR_COLUMN_DESCRIPTION"), getResourceString("PD_COLUMN_EDITOR_COLUMN_DESCRIPTION"));

            pde = new PropertyDescriptorEx("expression", beanClass);
            pde.setReadOnly(beanClass.getMethod("isExpressionDisabled"));
            add(pde, ExpressionEditor.class, getResourceString("PN_COLUMN_EDITOR_COLUMN_EXPRESSION"),
                    getResourceString("PD_COLUMN_EDITOR_COLUMN_EXPRESSION"));

            /*pde = new PropertyDescriptorEx("nature", beanClass, "getNature", "setNature");
            pde.setReadOnly(beanClass.getMethod("isReadOnly"));
            add(pde, NatureEditor.class, getResourceString("PN_COLUMN_EDITOR_COLUMN_NATURE"),
                    getResourceString("PN_COLUMN_EDITOR_COLUMN_NATURE"));*/

            pde = new PropertyDescriptorEx("reallyVisible", beanClass);
            pde.setReadOnly(beanClass.getMethod("isReadOnly"));
            add(pde, getResourceString("PN_COLUMN_EDITOR_COLUMN_VISIBLE"), getResourceString("PD_COLUMN_EDITOR_COLUMN_VISIBLE"));
        }

        public static class TypeEditor extends StringTagEditorSupport
        {
            static String[] getDataTypeNames()
            {
                return DataType.names();
            }

            public TypeEditor()
            {
                super(getDataTypeNames());
            }
        }

        public static class NatureEditor extends StringTagEditorSupport
        {
            static String[] getDataTypeNames()
            {
                TableColumn.Nature[] natures = TableColumn.Nature.values();
                String[] natureStrs = new String[natures.length];
                for( int i = 0; i < natures.length; i++ )
                {
                    natureStrs[i] = natures[i].toString();
                }

                return natureStrs;
            }

            public NatureEditor()
            {
                super(getDataTypeNames());
            }
        }

        public static class ExpressionEditor extends CustomEditorSupport
        {
            protected Editor editor = new Editor();

            private void initComponent()
            {
                editor.setText( ( (TableElement)getBean() ).getExpression());
                ( editor.getTextField() ).addActionListener(e -> setValue(editor.getText()));
            }
            @Override
            public Component getCustomRenderer(Component parent, boolean isSelected, boolean hasFocus)
            {
                initComponent();
                return editor;
            }

            @Override
            public Component getCustomEditor(Component parent, boolean isSelected)
            {
                initComponent();
                return editor;
            }

            class Editor extends TextButtonField
            {
                Editor()
                {
                    super("");

                    URL url = getClass().getResource("resources/edit.gif");
                    button.setIcon( IconUtils.getImageIcon( url ) );
                    button.setPreferredSize(new Dimension(30, 20));

                    button.addActionListener(e -> {
                        TableDataCollection tableData = ( (TableElement)getBean() ).getTable();
                        ExpressionFilterDialog dialog = new ExpressionFilterDialog(tableData);
                        dialog.setValue(getValue().toString());
                        if( dialog.doModal() )
                        {
                            String expressionValue = dialog.getValue();
                            editor.setText(expressionValue);
                            setValue(expressionValue);
                        }
                    });
                }

                void setText(String name)
                {
                    textField.setText(name);
                }

                public String getText()
                {
                    return textField.getText();
                }
            }
        }
    }
}