package ru.biosoft.access;

import ru.biosoft.access.core.DataCollection;

/**
 * Class duplicates moved class ru.biosoft.access.core.TextDataElement for
 * backward compatibility in already existing Data Collections.
 * 
 * Deprecated. Use moved class instead.
 */
@Deprecated
public class TextDataElement extends ru.biosoft.access.core.TextDataElement
{
    public TextDataElement(String name, DataCollection<?> origin)
    {
        super( name, origin );
    }

    public TextDataElement(String name, DataCollection<?> origin, String content)
    {
        super( name, origin, content );
    }
}
