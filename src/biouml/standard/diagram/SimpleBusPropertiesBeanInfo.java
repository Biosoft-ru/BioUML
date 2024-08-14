package biouml.standard.diagram;

import ru.biosoft.util.bean.BeanInfoEx2;

public class SimpleBusPropertiesBeanInfo extends BeanInfoEx2<SimpleBusProperties>
{
    public SimpleBusPropertiesBeanInfo()
    {
        super( SimpleBusProperties.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add("newBus");
        addHidden("name", "isExistingBus");
        addHidden("directed", "isExistingBus");
        addHidden("color", "isExistingBus");
        property("existingName").hidden("isNewBus").tags( bean->bean.getExistingBuses() ).add();   
        
    }
}