package ru.biosoft.analysis;

import ru.biosoft.util.bean.BeanInfoEx2;

public class VennManyParametersBeanInfo extends BeanInfoEx2<VennManyParameters>
{
    public VennManyParametersBeanInfo()
    {
        super(VennManyParameters.class);
    }
    @Override
    public void initProperties() throws Exception
    {
        add("cases");
        add("counts");
        add("outVenn");
    }
}
