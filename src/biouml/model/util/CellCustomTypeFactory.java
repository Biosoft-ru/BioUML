package biouml.model.util;

import ru.biosoft.util.serialization.CustomTypeFactory;

public class CellCustomTypeFactory implements CustomTypeFactory
{
    String name;

    @Override
    public Object getFactoryInstance(Object o)
    {
        name = ( (biouml.standard.type.Cell)o ).getName();
        return this;
    }

    @Override
    public Object getOriginObject()
    {
        return new biouml.standard.type.Cell(null, name);
    }
}
