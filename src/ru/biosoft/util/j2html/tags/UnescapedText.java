package ru.biosoft.util.j2html.tags;

import java.io.IOException;
import java.io.Writer;

public class UnescapedText extends DomContent
{
    private final String text;

    public UnescapedText(String text)
    {
        this.text = text;
    }

    @Override
    public void renderTo(Writer writer) throws IOException
    {
        writer.write( text );
    }

    @Override
    public String render()
    {
        return text;
    }

}
