package ru.biosoft.bsa;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

import ru.biosoft.bsa.gui.MessageBundle;

public class SqlTrackBeanInfo extends BeanInfoEx
{
    public SqlTrackBeanInfo()
    {
        super(SqlTrack.class, MessageBundle.class.getName());
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        
        add( "genomeSelector" );
        
        add(new PropertyDescriptorEx("sqlTable", beanClass, "getTableId", null));
        add("description");
    }
}
