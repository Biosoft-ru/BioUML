package biouml.plugins.sbgn;

import java.beans.IntrospectionException;

import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

public class EquivalenceOperatorPropertiesBeanInfo extends BeanInfoEx2<EquivalenceOperatorProperties>
{
    public EquivalenceOperatorPropertiesBeanInfo()
    {
        super(EquivalenceOperatorProperties.class);
    }

    @Override
    public void initProperties() throws IntrospectionException
    {
        add("name");
        add( "nodeNames", SubTypeEntitiesditor.class );
        addWithTags( "mainNodeName",  b ->b.getAvailableSuperEntities() );
    }  
    
    public static class SubTypeEntitiesditor extends GenericMultiSelectEditor
    {
        @Override
        public String[] getAvailableValues()
        {
            return ((EquivalenceOperatorProperties)getBean()).getAvailableSubEntities();
        }
    }
}