package biouml.plugins.virtualcell.diagram;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class ProteinDegradationPropertiesBeanInfo extends BeanInfoEx2<ProteinDegradationProperties>
{
    public ProteinDegradationPropertiesBeanInfo()
    {
        super( ProteinDegradationProperties.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add( "name" );
        property( DataElementPathEditor.registerInput( "degradationRates", beanClass, TableDataCollection.class, false ) ).add();
    }
}