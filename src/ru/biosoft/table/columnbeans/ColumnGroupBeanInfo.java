package ru.biosoft.table.columnbeans;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.table.TableDataCollection;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class ColumnGroupBeanInfo extends BeanInfoEx
{
    public ColumnGroupBeanInfo()
    {
        super(ColumnGroup.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("PN_COLUMN_GROUP"));
        beanDescriptor.setShortDescription(getResourceString("PD_COLUMN_GROUP"));
    }

    @Override
    public void initProperties() throws Exception
    {
        add(DataElementPathEditor.registerInput("tablePath", beanClass, TableDataCollection.class, beanClass.getMethod("canBeNull")),
                getResourceString("PN_TABLE"), getResourceString("PD_TABLE"));
//        add(new PropertyDescriptorEx("group", beanClass), ColumnGroupSelector.class, getResourceString("PN_GROUP"), getResourceString("PD_GROUP"));
        PropertyDescriptorEx pde = new PropertyDescriptorEx("columns", beanClass, "getColumnsForEditor", "setColumns");
        pde.setChildDisplayName(beanClass.getMethod("calcColumnName", new Class[] {Integer.class, Object.class}));
        pde.setHideChildren( true );
        pde.setPropertyEditorClass(ColumnsEditor.class);
        add(pde, getResourceString("PN_COLUMNS"), getResourceString("PD_COLUMNS"));
        pde = new PropertyDescriptorEx("namesDescription", beanClass, "getNamesDescription", null);
        pde.setHidden(true);
        add(pde);
    }

//    public class ColumnGroupSelector extends GenericComboBoxEditor
//    {
//        @Override
//        public Object[] getAvailableValues()
//        {
//            try
//            {
//                return ((ColumnGroup)getBean()).getTable().getGroups().getNameList().toArray();
//            }
//            catch( Exception ex )
//            {
//                return new Object[] {};
//            }
//        }
//    }
}
