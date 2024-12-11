package biouml.plugins.virtualcell.diagram;

import biouml.model.Diagram;
import ru.biosoft.access.repository.DataElementPathEditor;
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
        property( DataElementPathEditor.registerInput( "transcriptionFactors", beanClass, Diagram.class, false ) ).add();
    }
}