package ru.biosoft.graphics;

import static ru.biosoft.util.j2html.TagCreator.div;

import java.awt.Color;

import ru.biosoft.util.ControlCodeGenerator;
import ru.biosoft.util.Util;
import ru.biosoft.util.j2html.tags.Tag;

public class ColorControlCodeGenerator implements ControlCodeGenerator
{

    @Override 
    public Tag<?> getControlCode(Object value) throws Exception
    {
        Color color = ((Color) value);
        long uid = Util.getUniqueId();
        String viewerId = "viewer_" + uid;
        return div()
                .attr("style", "width:100px; height:12px; border:1px solid black; background-color:rgb(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ");")
                .withId(viewerId);
    }

    @Override public Class<?> getSupportedItemType()
    {
        return Color.class;
    }

}
