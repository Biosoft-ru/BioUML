package ru.biosoft.util.serialization.xml;

import ru.biosoft.util.serialization.Serializer;

/**
 * Created by IntelliJ IDEA.
 * User: puz
 * Date: 06.05.2006
 * Time: 16:23:17
 */
public class XMLSerializer extends Serializer
{
    public XMLSerializer()
    {
        super( new XMLObjectSerializationHandler() );
    }
}
