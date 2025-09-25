package biouml.plugins.wdl;

import com.developmentontheedge.beans.BeanInfoEx;

public class ImportPropertiesBeanInfo extends BeanInfoEx
{
    public ImportPropertiesBeanInfo()
    {
        super( ImportProperties.class );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add( "source" );
        add( "alias" );
    }
}