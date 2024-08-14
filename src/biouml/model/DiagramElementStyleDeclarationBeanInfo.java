package biouml.model;

import java.beans.IntrospectionException;

import ru.biosoft.util.bean.BeanInfoEx2;

public class DiagramElementStyleDeclarationBeanInfo extends BeanInfoEx2<DiagramElementStyleDeclaration>
{
    public DiagramElementStyleDeclarationBeanInfo()
    {
        super(DiagramElementStyleDeclaration.class);
    }

    @Override
    public void initProperties() throws IntrospectionException
    {
       add("name");
       add("style");
    }
}
