package ru.biosoft.util.j2html.tags;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public abstract class DomContent
{
    public String render()
    {
        StringWriter writer = new StringWriter();
        try
        {
            renderTo( writer );
        }
        catch( IOException e )
        {
            throw new InternalError( e );
        }
        return writer.toString();
    }

    public abstract void renderTo(Writer writer) throws IOException;

    @Override
    public String toString()
    {
        return render();
    }

}
