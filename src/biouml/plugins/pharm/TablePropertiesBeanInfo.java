package biouml.plugins.pharm;

import com.developmentontheedge.beans.BeanInfoEx;

public class TablePropertiesBeanInfo extends BeanInfoEx
{
    public TablePropertiesBeanInfo()
    {
        super( TableProperties.class, true );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add("name");
        add("tablePath");
        add("formula");
    }
}