package ru.biosoft.table.columnbeans;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.util.BeanUtil;

/**
 * Descriptor which takes information from the bean
 * @author lan
 */
public class BeanBasedDescriptor extends DescriptorSupport
{
    private String beanProperty;
    private DataCollection source;

    public BeanBasedDescriptor(String name, DataCollection origin, DataCollection source, String beanProperty)
    {
        super(name, origin);
        this.source = source;
        this.beanProperty = beanProperty;
    }

    @Override
    public Map<String, Object> getColumnValues(List<String> names) throws Exception
    {
        Map<String, Object> result = new HashMap<>();
        for(String name: names)
        {
            try
            {
                DataElement de = source.get(name);
                if(de == null) continue;
                Object value = BeanUtil.getBeanPropertyValue(de, beanProperty);
                if(value != null) result.put(name, value.toString());
            }
            catch( Exception e )
            {
            }
        }
        return result;
    }

    @Override
    public ReferenceType getInputReferenceType()
    {
        return ReferenceTypeRegistry.getElementReferenceType(source);
    }
}
