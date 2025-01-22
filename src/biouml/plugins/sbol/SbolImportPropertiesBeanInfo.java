package biouml.plugins.sbol;

import ru.biosoft.util.bean.BeanInfoEx2;

public class SbolImportPropertiesBeanInfo extends BeanInfoEx2<SbolImportProperties>
{
    public SbolImportPropertiesBeanInfo()
    {
        super(SbolImportProperties.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add( "diagramName" );
    }
}