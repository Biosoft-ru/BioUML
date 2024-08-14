package ru.biosoft.access;

import java.net.URL;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.repository.IconFactory;

public interface HtmlDescribedElement extends DataElement
{
    /**
     * @return String containing HTML which describes element
     */
    public String getDescriptionHTML();
    
    /**
     * @return URL for base location of HTML returned by getDescriptionHTML so that images can load correctly
     */
    public URL getBase();

    /**
     * @return String in iconID-format (see {@link IconFactory}) for base location of HTML returned by getDescriptionHTML so that images can load correctly
     */
    public String getBaseId();
}
