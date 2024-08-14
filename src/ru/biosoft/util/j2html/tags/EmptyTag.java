package ru.biosoft.util.j2html.tags;

import java.io.IOException;
import java.io.Writer;

public class EmptyTag extends Tag<EmptyTag> {

    public EmptyTag(String tagName) {
        super(tagName);
    }

    @Override
    public void renderTo(Writer writer) throws IOException
    {
        renderOpenTag( writer );
    }
}
