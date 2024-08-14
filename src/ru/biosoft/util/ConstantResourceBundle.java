package ru.biosoft.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.ListResourceBundle;

/**
 * @author lan
 *
 */
public abstract class ConstantResourceBundle extends ListResourceBundle
{
    private Object[][] contents = null;

    @Override
    protected Object[][] getContents()
    {
        if(contents == null)
        {
            List<Object[]> result = new ArrayList<>();
            for(Field field: getClass().getFields())
            {
                String value;
                String key = field.getName();
                try
                {
                    value = (String)field.get(null);
                }
                catch(Exception e)
                {
                    continue;
                }
                result.add(new Object[] {key, value});
            }
            contents = result.toArray(new Object[result.size()][]);
        }
        return contents;
    }
}
