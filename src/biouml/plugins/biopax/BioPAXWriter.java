// $ Id: $
package biouml.plugins.biopax;

import java.io.IOException;
import java.net.URI;

import org.coode.xml.XMLWriter;
import org.semanticweb.owl.model.OWLObject;
import org.semanticweb.owl.vocab.Namespaces;

public class BioPAXWriter
{

    private XMLWriter writer;

    BioPAXWriter(XMLWriter writer)
    {
        this.writer = writer;
    }

    public void writeStartElement(URI elementName)
    {
        try
        {
            // Sort out with namespace
            writer.writeStartElement(elementName.toString());
        }
        catch( IOException e )
        {
            throw new RuntimeException(e);
        }
    }

    public void writeParseTypeAttribute()
    {
        try
        {
            writer.writeAttribute(Namespaces.RDF + "parseType", "Collection");
        }
        catch( IOException e )
        {
            throw new RuntimeException(e);
        }
    }

    public void writeDatatypeAttribute(URI datatypeURI)
    {
        try
        {
            writer.writeAttribute(Namespaces.RDF + "datatype", datatypeURI.toString());
        }
        catch( IOException e )
        {
            throw new RuntimeException(e);
        }
    }

    public void writeTextContent(String text)
    {
        try
        {
            writer.writeTextContent(text);
        }
        catch( IOException e )
        {
            throw new RuntimeException(e);
        }
    }

    public void writeLangAttribute(String lang)
    {
        try
        {
            writer.writeAttribute("xml:lang", lang);
        }
        catch( IOException e )
        {
            throw new RuntimeException(e);
        }
    }


    public void writeEndElement()
    {
        try
        {
            writer.writeEndElement();
        }
        catch( IOException e )
        {
            throw new RuntimeException(e);
        }
    }

    public void writeAboutAttribute(URI value)
    {
        writeAttribute(Namespaces.RDF + "about", value.toString());
    }
    
    public void writeIDAttribute(URI value)
    {
        if(value.getFragment()!=null)
        {
            writeAttribute(Namespaces.RDF + "ID", value.getFragment());
        }
    }

    private void writeAttribute(String attributeName, String value)
    {
        try
        {
            if( value.startsWith(writer.getXMLBase()) )
            {
                writer.writeAttribute(attributeName, value.substring(writer.getXMLBase().length(), value.length()));
            }
            else
            {
                writer.writeAttribute(attributeName, value);
            }
        }
        catch( IOException e )
        {
            throw new RuntimeException(e);
        }
    }

    public void writeOWLObject(OWLObject owlObject)
    {

    }


    public void writeResourceAttribute(URI value)
    {
        writeAttribute(Namespaces.RDF + "resource", value.toString());
    }


    public void startDocument()
    {
        try
        {
            writer.startDocument(Namespaces.RDF + "RDF");
        }
        catch( IOException e )
        {
            throw new RuntimeException(e);
        }
    }

    public void endDocument()
    {
        try
        {
            writer.endDocument();
        }
        catch( IOException e )
        {
            throw new RuntimeException(e);
        }
    }


    public void writeComment(String comment)
    {
        try
        {
            writer.writeComment(comment);
        }
        catch( IOException e )
        {
            throw new RuntimeException(e);
        }
    }
}
