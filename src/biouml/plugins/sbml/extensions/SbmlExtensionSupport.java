package biouml.plugins.sbml.extensions;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import biouml.plugins.sbml.SbmlModelReader;
import biouml.plugins.sbml.SbmlModelWriter;

public abstract class SbmlExtensionSupport implements SbmlExtension
{
    protected SbmlModelReader modelReader;
    protected SbmlModelWriter modelWriter;

    public static Element getElement(Element element, String name)
    {
        NodeList list = element.getChildNodes();
        for( int i = 0; i < list.getLength(); i++ )
        {
            org.w3c.dom.Node node = list.item(i);
            if( ( node instanceof Element ) && ( node.getNodeName().equals(name) ) )
            {
                return (Element)node;
            }
        }
        return null;
    }

    public static String getTextContent(Element root)
    {
        String result = null;
        NodeList list = root.getChildNodes();
        for( int i = 0; i < list.getLength(); i++ )
        {
            org.w3c.dom.Node child = list.item(i);
            if( child instanceof Text )
            {
                result = ( (Text)child ).getData();
                break;
            }
        }
        return result;
    }

    @Override
    public void setSbmlModelReader(SbmlModelReader reader)
    {
        this.modelReader = reader;
    }

    @Override
    public void setSbmlModelWriter(SbmlModelWriter writer)
    {
        this.modelWriter = writer;
    }
}
