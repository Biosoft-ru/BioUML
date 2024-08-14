package ru.biosoft.util.serialization.xml;

import java.io.IOException;


import org.xml.sax.SAXException;

import ru.biosoft.util.serialization.SerializationException;
import ru.biosoft.util.serialization.Utils;

/**
 * Created by IntelliJ IDEA.
 * User: puz
 * Date: 10.05.2006
 * Time: 16:17:57
 */
public class TestUtils
{
    public static boolean verifyXMLSerialization( Object o ) throws SerializationException, IOException, SAXException
    {
        Object oo = Parser.fromXML( new XMLSerializer().serialize( o ) );
        return Utils.areEqual( oo, o );
    }
}
