package ru.biosoft.util.serialization.xml;

import java.io.IOException;
import java.io.StringReader;

import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Created by IntelliJ IDEA.
 * User: puz
 * Date: 08.05.2006
 * Time: 16:38:36
 */
public class Parser
{
    public static Object fromXML( String xml ) throws IOException, SAXException
    {
        return fromXML( new InputSource( new StringReader( xml ) ) );
    }

    public static Object fromXML( InputSource is ) throws IOException, SAXException
    {
        ParserImpl handler = new ParserImpl();
        XMLReader parser = XMLReaderFactory.createXMLReader();
        parser.setContentHandler( handler );
        parser.setEntityResolver( new EntityResolver()
        {
            @Override
            public InputSource resolveEntity( String publicId, String systemId ) throws SAXException, IOException
            {
                return null;
            }
        } );
        parser.parse( is );
        return handler.getValue();
    }
}
