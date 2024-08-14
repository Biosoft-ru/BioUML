package biouml.model;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class ModelDefinitionBeanInfo extends CompartmentBeanInfo
{
    public ModelDefinitionBeanInfo()
    {
        super(ModelDefinition.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        PropertyDescriptorEx pde = new PropertyDescriptorEx("diagramPath", beanClass, "getDiagramPath", null);
        add(0, pde, "Diagram path", "Diagram path.");
    }
}
