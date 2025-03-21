package biouml.plugins.virtualcell.diagram;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class PopulationPropertiesBeanInfo extends BeanInfoEx2<PopulationProperties>
{
    public PopulationPropertiesBeanInfo()
    {
        super( PopulationProperties.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add( "name" );
        property( DataElementPathEditor.registerInput( "coeffs", beanClass, TableDataCollection.class, false ) ).add();
    }
}