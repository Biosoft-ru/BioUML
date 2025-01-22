package ru.biosoft.server.custombean;

import one.util.streamex.IntStreamEx;

import ru.biosoft.util.bean.BeanInfoEx2;

public class ColorFontWrapperBeanInfo extends BeanInfoEx2<ColorFontWrapper>
{
    public ColorFontWrapperBeanInfo()
    {
        super(ColorFontWrapper.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        addWithTags("family", "SansSerif", "Serif", "Monospaced", "Arial", "Verdana", "Liberation Sans");
        addWithTags( "size",
                bean -> IntStreamEx.of( 6, 7, 8, 9, 10, 11, 12, 14, 16, 18, 20, 24, 28, 32, 36, 40, 48 ).mapToObj( String::valueOf ) );
        add("color");
    }
}
