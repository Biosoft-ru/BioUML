package biouml.plugins.virtualcell.diagram;

import biouml.model.Diagram;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class MetabolismPropertiesBeanInfo extends BeanInfoEx2<MetabolismProperties>
{
    public MetabolismPropertiesBeanInfo()
    {
        super( MetabolismProperties.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add( "name" );
        property( DataElementPathEditor.registerInput( "diagramPath", beanClass, Diagram.class, false ) ).add();
        property( DataElementPathEditor.registerInput( "tablePath", beanClass, TableDataCollection.class, false ) ).add();
    }
}