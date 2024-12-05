package biouml.plugins.virtualcell.diagram;

import biouml.model.Diagram;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.util.bean.BeanInfoEx2;

public class ProcessPropertiesBeanInfo extends BeanInfoEx2<ProcessProperties>
{
    public ProcessPropertiesBeanInfo()
    {
        super( ProcessProperties.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add( "name" );
        property( DataElementPathEditor.registerInput( "diagramPath", beanClass, Diagram.class, false ) ).add();
    }
}