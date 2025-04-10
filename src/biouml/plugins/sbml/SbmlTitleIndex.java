package biouml.plugins.sbml;

import java.io.File;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.FileCollection;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.access.security.PrivilegedAction;
import ru.biosoft.access.security.SecurityManager;
import biouml.standard.type.access.TitleIndex;

@SuppressWarnings ( "serial" )
public class SbmlTitleIndex extends TitleIndex
{
    public SbmlTitleIndex(DataCollection<?> dc, String indexName) throws Exception
    {
        super(dc, indexName);
    }
    
    private static class SAXHandler extends DefaultHandler
    {
        private String name;
        
        /**
         * @return the name
         */
        public String getName()
        {
            return name;
        }

        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException
        {
            if(qName.equalsIgnoreCase("model"))
            {
                name = atts.getValue("name");
                if(name == null) name = atts.getValue("id");
                throw new SAXException();   // stop parser here
            }
        }
    }

    @Override
    protected void doInit() throws Exception
    {
        SecurityManager.runPrivileged(new PrivilegedAction()
        {
            @Override
            public Object run() throws Exception
            {
                FileCollection fileCollection = (FileCollection) ( (TransformedDataCollection<?, ?>)DataCollectionUtils
                        .fetchPrimaryCollectionPrivileged( dc ) ).getPrimaryCollection();
                for(FileDataElement element : fileCollection)
                {
                    File file = element.getFile();
                    SAXParserFactory factory = SAXParserFactory.newInstance();
                    SAXParser parser = factory.newSAXParser();
                    SAXHandler handler = new SAXHandler();
                    try
                    {
                        parser.parse(file, handler);
                    }
                    catch( SAXException e )
                    {
                    }
                    putInternal(element.getName(), handler.getName());
                }
                return null;
            }
        });
    }
}
