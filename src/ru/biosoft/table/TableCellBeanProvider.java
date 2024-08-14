package ru.biosoft.table;

import com.developmentontheedge.beans.model.Property;

import ru.biosoft.access.BeanProvider;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;

/**
 * @author lan
 *
 */
public class TableCellBeanProvider implements BeanProvider
{

    @Override
    public Object getBean(String completeName)
    {
        DataElementPath path = DataElementPath.create(completeName);
        DataElement de = path.getParentPath().optDataElement();
        if( de instanceof RowDataElement )
        {
            int index = -1;
            try
            {
                index = Integer.parseInt(path.getName());
            }
            catch( NumberFormatException e )
            {
            }
            if( index >= 0 )
            {
                Object[] values = ( (RowDataElement)de ).getValues();
                Object value = null;
                if( values != null && values.length > index )
                {
                    value = values[index];
                    if( value instanceof Property )
                        value = ( (Property)value ).getValue();
                }
                return value;
            }
        }
        return null;
    }
}
