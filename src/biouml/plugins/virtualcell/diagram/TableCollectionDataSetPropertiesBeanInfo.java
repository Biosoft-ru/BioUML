package biouml.plugins.virtualcell.diagram;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class TableCollectionDataSetPropertiesBeanInfo extends BeanInfoEx2<TableCollectionDataSetProperties>
{
    public TableCollectionDataSetPropertiesBeanInfo()
    {
        super( TableCollectionDataSetProperties.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add( "name" );
        property( DataElementPathEditor.registerInput( "path", beanClass, TableDataCollection.class, false ) ).add();
    }
}