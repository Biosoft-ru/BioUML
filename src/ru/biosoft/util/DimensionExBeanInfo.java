package ru.biosoft.util;
import ru.biosoft.util.bean.BeanInfoEx2;

public class DimensionExBeanInfo extends BeanInfoEx2<DimensionEx>
{
    public DimensionExBeanInfo()
    {
        super( DimensionEx.class );
        //setHideChildren(true);
        //setCompositeEditor("width;height", new java.awt.GridLayout(1, 2));
    }
 
    @Override
    protected void initProperties() throws Exception
    {
        add("width");
        add("height");
    }


}
