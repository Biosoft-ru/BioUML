package biouml.plugins.hemodynamics;

import biouml.plugins.hemodynamics.ControlPointProperties.ControlTypeEditor;
import ru.biosoft.util.bean.BeanInfoEx2;

public class ControlPointPropertiesBeanInfo extends BeanInfoEx2<ControlPointProperties>
{
    public ControlPointPropertiesBeanInfo()
    {
        super(ControlPointProperties.class);
    }
    
    @Override
    protected void initProperties() throws Exception
    {
        addWithTags("vesselName", bean -> bean.getAvailableVessels());
        add("variableType", ControlTypeEditor.class);
        add("segment");
        add("variableName");
    }
}