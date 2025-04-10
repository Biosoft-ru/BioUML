
package ru.biosoft.access;

import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.TextDataElement;

import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author anna
 *
 */
@ClassIcon ( "resources/html.gif" )
@PropertyName("html")
public class HtmlDataElement extends TextDataElement
{

    public HtmlDataElement(String name, DataCollection origin, String content)
    {
        super(name, origin, content);
    }

    @Override
    public String getContentType()
    {
        return "text/html";
    }
}
