package ru.biosoft.table.document.editors;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.editors.StringTagEditorSupport;

import ru.biosoft.table.MessageBundle;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.datatype.DataType;

public class TableElementBeanInfo extends BeanInfoEx
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
        add(pde, TableElementBeanInfo.TypeEditor.class, getResourceString("PN_COLUMN_EDITOR_COLUMN_TYPE"), getResourceString("PD_COLUMN_EDITOR_COLUMN_TYPE"));

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
}