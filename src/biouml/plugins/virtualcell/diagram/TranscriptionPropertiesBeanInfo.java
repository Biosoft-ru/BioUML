package biouml.plugins.virtualcell.diagram;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class TranscriptionPropertiesBeanInfo extends BeanInfoEx2<TranscriptionProperties>
{
    public TranscriptionPropertiesBeanInfo()
    {
        super( TranscriptionProperties.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add( "name" );
        property( DataElementPathEditor.registerInput( "transcriptionFactors", beanClass, TableDataCollection.class, false ) ).add();
    }
}