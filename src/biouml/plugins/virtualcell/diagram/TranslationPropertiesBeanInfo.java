package biouml.plugins.virtualcell.diagram;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class TranslationPropertiesBeanInfo extends BeanInfoEx2<TranslationProperties>
{
    public TranslationPropertiesBeanInfo()
    {
        super( TranslationProperties.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add( "name" );
        property( DataElementPathEditor.registerInput( "translationRates", beanClass, TableDataCollection.class, false ) ).add();
    }
}