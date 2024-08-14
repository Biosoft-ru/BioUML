package biouml.plugins.pharm;

import ru.biosoft.util.bean.BeanInfoEx2;
import biouml.model.Diagram;

public class StructuralModelPropertiesBeanInfo extends BeanInfoEx2<StructuralModelProperties>
{
    public StructuralModelPropertiesBeanInfo()
    {
        super(StructuralModelProperties.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("name");
        property( "diagramPath" ).inputElement( Diagram.class ).add();
    }
}
