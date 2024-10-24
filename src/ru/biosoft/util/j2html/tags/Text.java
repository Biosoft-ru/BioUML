package ru.biosoft.util.j2html.tags;

import java.io.IOException;
import java.io.Writer;

import org.apache.commons.lang.StringEscapeUtils;

public class Text extends DomContent {

    private final String text;

    public Text(String text) {
        this.text = text;
    }
    
    @Override
    public void renderTo(Writer writer) throws IOException
    {
        StringEscapeUtils.escapeHtml( writer, text );
    }

    @Override
    public String render() {
        return StringEscapeUtils.escapeHtml( text );
    }
}
