package ru.biosoft.table;

import java.beans.FeatureDescriptor;
import java.util.Enumeration;

import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;

import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.swing.table.Column;
import com.developmentontheedge.beans.swing.table.ColumnModel;

/**
 * Swing column model which creates ColumnEx columns and reads ReferenceTypes
 * @author lan
 */
public class BeanColumnModelEx extends ColumnModel
{
    private static final long serialVersionUID = 1L;

    /**
     * @param templateBeanClass
     * @param showMode
     */
    public BeanColumnModelEx(Class<?> templateBeanClass, int showMode)
    {
        super(templateBeanClass, showMode);
    }

    /**
     * @param fields
     */
    public BeanColumnModelEx(Column[] fields)
    {
        super(fields);
    }

    /**
     * @param templateBean
     * @param showMode
     */
    public BeanColumnModelEx(Object templateBean, int showMode)
    {
        super(templateBean, showMode);
    }

    /**
     * @param templateBean
     */
    public BeanColumnModelEx(Object templateBean)
    {
        super(templateBean);
    }

    /** Init name to display in the columns headers */
    @Override
    protected void initColumnNames( ComponentModel columnsModel, int showMode )
    {
        super.initColumnNames(columnsModel, showMode);
        Column[] columns = getColumns();
        for(int i=0; i<columns.length; i++)
        {
            ColumnEx newColumn = new ColumnEx(this, columns[i].getColumnKey(), columns[i].getName(), columns[i].getEnabled());
            try
            {
                FeatureDescriptor descriptor = columnsModel.getPropertyAt(i).getDescriptor();
                Enumeration<String> attributeNames = descriptor.attributeNames();
                while(attributeNames.hasMoreElements())
                {
                    try
                    {
                        String name = attributeNames.nextElement();
                        newColumn.setValue(name, descriptor.getValue(name).toString());
                    }
                    catch( Exception e )
                    {
                    }
                }
            }
            catch( Exception e )
            {
            }
            try
            {
                ReferenceType typeForDescriptor = ReferenceTypeRegistry.getTypeForDescriptor(columnsModel.findProperty(columns[i].getColumnKey()).getDescriptor());
                if( typeForDescriptor != null )
                    newColumn.setValue( ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY, typeForDescriptor.toString() );
            }
            catch( Exception e )
            {
            }
            columns[i] = newColumn;
        }
    }
}
