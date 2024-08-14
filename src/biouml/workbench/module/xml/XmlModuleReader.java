package biouml.workbench.module.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.util.XmlStream;
import biouml.model.CollectionDescription;
import biouml.workbench.module.xml.XmlModule.DiagramTypeDescription;
import biouml.workbench.module.xml.XmlModule.InternalType;
import biouml.workbench.module.xml.XmlModule.InternalType.IndexDescription;

/**
 */
public class XmlModuleReader extends XmlModuleSupport
{
    protected static final Logger log = Logger.getLogger(XmlModuleReader.class.getName());
    public static final String VERSION = "0.8.0";

    protected String version = VERSION;

    protected InputStream stream;
    protected String name;

    /**
     * This constructor is used to read diagram from stream and can be used to
     * read diagram from relational database (TEXT or BLOB).
     * 
     * @param name moduleType name
     * @param stream stream that contains module type XML
     */
    public XmlModuleReader(String name, InputStream stream)
    {
        this.name = name;
        this.stream = stream;
    }

    public void read(XmlModule xmlModule) throws Exception
    {
        this.xmlModule = xmlModule;

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = null;

        // TODO - replace StringBuffer
        StringBuffer buffer = new StringBuffer();
        byte[] b = new byte[65536];
        int length = stream.read(b);
        while( length > 0 )
        {
            buffer.append(new String(b, 0, length, StandardCharsets.UTF_8));
            length = stream.read(b);
        }
        String xml = buffer.toString();
        xml = xml.replaceAll("[\\x01\\x02\\x03\\x04\\x05\\x06\\x0b\\x0c\\x0f\\x12\\x14\\x16\\x92\\x1a\\x1c\\x1e\\xff]", "?");
        stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

        try
        {
            doc = builder.parse(stream);
        }
        catch( SAXException e )
        {
            log.log(Level.SEVERE, "Parse module type \"" + name + "\" error: " + e.getMessage());
            return;
        }
        catch( IOException e )
        {
            log.log(Level.SEVERE, "Read module type \"" + name + "\" error: " + e.getMessage());
            // <![CDATA[ ]]> section is invalide - remove it
            int start = xml.indexOf("<![CDATA[");
            String newXml = xml;
            if( start >= 0 )
            {
                int finish = xml.indexOf("]]>");
                if( finish > start + 9 )
                {
                    newXml = xml.substring(0, start + 9) + "Invalid module type" + xml.substring(finish, xml.length());
                }
            }
            stream = new ByteArrayInputStream(newXml.getBytes(StandardCharsets.UTF_8));

            try
            {
                doc = builder.parse(stream);
            }
            catch( SAXException | IOException e2 )
            {
                return;
            }

            read(doc);
            return;
        }

        read(doc);
    }

    protected void read(Document doc) throws Exception
    {
        Element root = doc.getDocumentElement();
        if( root.hasAttribute(VERSION_ATTR) )
            version = root.getAttribute(VERSION_ATTR);

        if( root.hasAttribute(TITLE_ATTR) )
        {
            xmlModule.getInfo().setDisplayName(root.getAttribute(TITLE_ATTR));
        }
        if( root.hasAttribute(TYPE_ATTR) )
        {
            xmlModule.setModuleType(root.getAttribute(TYPE_ATTR));
        }

        readJdbcConnection(root);

        xmlModule.initSections();

        readProperties(root);
        readDependencies(root);
        readTypes(root);
    }

    protected void readJdbcConnection(Element root)
    {
        Properties databaseProperties = new Properties();

        if( root.hasAttribute(DATABASE_TYPE_ATTR) )
            databaseProperties.put(DATABASE_TYPE_ATTR, root.getAttribute(DATABASE_TYPE_ATTR));

        if( root.hasAttribute(DATABASE_NAME_ATTR) )
            databaseProperties.put(DATABASE_NAME_ATTR, root.getAttribute(DATABASE_NAME_ATTR));

        if( root.hasAttribute(DATABASE_VERSION_ATTR) )
            databaseProperties.put(DATABASE_VERSION_ATTR, root.getAttribute(DATABASE_VERSION_ATTR));

        Element connectionElement = getElement(root, JDBC_CONNECTION_ELEMENT);
        if( connectionElement == null )
        {
            return;
        }
        if( connectionElement.hasAttribute(JDBC_CONNECTION_DRIVER_ATTR) )
        {
            databaseProperties.put(SqlDataCollection.JDBC_DRIVER_PROPERTY, connectionElement.getAttribute(JDBC_CONNECTION_DRIVER_ATTR));
        }
        if( connectionElement.hasAttribute(JDBC_CONNECTION_URL_ATTR) )
        {
            databaseProperties.put(SqlDataCollection.JDBC_URL_PROPERTY, connectionElement.getAttribute(JDBC_CONNECTION_URL_ATTR));
        }
        if( connectionElement.hasAttribute(JDBC_CONNECTION_USER_ATTR) )
        {
            databaseProperties.put(SqlDataCollection.JDBC_USER_PROPERTY, connectionElement.getAttribute(JDBC_CONNECTION_USER_ATTR));
        }
        if( connectionElement.hasAttribute(JDBC_CONNECTION_PASSWORD_ATTR) )
        {
            databaseProperties.put(SqlDataCollection.JDBC_PASSWORD_PROPERTY, connectionElement.getAttribute(JDBC_CONNECTION_PASSWORD_ATTR));
        }

        xmlModule.setDatabaseProperties(databaseProperties);
    }

    protected void readProperties(Element root)
    {
        //TODO:
    }

    protected void readDependencies(Element root)
    {
        Element dependenciesElement = getElement(root, DEPENDENCIES_ELEMENT);
        if( dependenciesElement == null )
        {
            return;
        }

        NodeList list = dependenciesElement.getElementsByTagName(EXTERNAL_DATABASE_ELEMENT);
        for( Element iTypeElement : XmlStream.elements( list ) )
        {
            try
            {
                readExternalModule(iTypeElement);
            }
            catch( Throwable t )
            {
                error("ERROR_READ_EXTERNAL_DATABASE", new String[] {name, t.getMessage()}, t);
            }
        }

        list = dependenciesElement.getElementsByTagName(GRAPHIC_NOTATION_ELEMENT);
        for( Element iTypeElement : XmlStream.elements( list ) )
        {
            try
            {
                readGraphicNotation(iTypeElement);
            }
            catch( Throwable t )
            {
                error("ERROR_READ_GRAPHIC_NOTATION", new String[] {name, t.getMessage()}, t);
            }
        }
    }

    protected void readExternalModule(Element root)
    {
        if( root.hasAttribute(EXTERNAL_DATABASE_NAME_ATTR) )
        {
            String moduleName = root.getAttribute(EXTERNAL_DATABASE_NAME_ATTR);
            NodeList list = root.getElementsByTagName(EXTERNAL_TYPE_ELEMENT);
            for( Element eTypeElement : XmlStream.elements( list ) )
            {
                try
                {
                    readExternalType(eTypeElement, moduleName);
                }
                catch( Throwable t )
                {
                    error("ERROR_READ_EXTERNAL_TYPE", new String[] {name, t.getMessage()}, t);
                }
            }
        }
        else
        {
            error("REQUIRED_ATTR_MISSING", new String[] {name, "name", "externalModule"});
        }
    }

    protected void readGraphicNotation(Element root)
    {
        DiagramTypeDescription dtd = new DiagramTypeDescription();

        if( root.hasAttribute(GRAPHIC_NOTATION_NAME_ATTR) )
        {
            dtd.setName(root.getAttribute(GRAPHIC_NOTATION_NAME_ATTR));
        }
        else
        {
            error("REQUIRED_ATTR_MISSING", new String[] {name, "name", "graphicNotation"});
        }
        if( root.hasAttribute(GRAPHIC_NOTATION_TYPE_ATTR) )
        {
            dtd.setType(root.getAttribute(GRAPHIC_NOTATION_TYPE_ATTR));
        }
        else
        {
            error("REQUIRED_ATTR_MISSING", new String[] {name, "type", "graphicNotation"});
        }

        if( root.hasAttribute(GRAPHIC_NOTATION_CLASS_ATTR) )
        {
            dtd.setClassName(root.getAttribute(GRAPHIC_NOTATION_CLASS_ATTR));
        }
        if( root.hasAttribute(GRAPHIC_NOTATION_PATH_ATTR) )
        {
            dtd.setPath(root.getAttribute(GRAPHIC_NOTATION_PATH_ATTR));
        }

        xmlModule.addDiagramType(dtd);
    }

    protected void readExternalType(Element root, String moduleName)
    {
        String typeName = null;
        if( root.hasAttribute(EXTERNAL_TYPE_NAME_ATTR) )
        {
            typeName = root.getAttribute(EXTERNAL_TYPE_NAME_ATTR);
        }
        else
        {
            error("REQUIRED_ATTR_MISSING", new String[] {name, "name", "externalType"});
        }
        String sectionName = null;
        if( root.hasAttribute(EXTERNAL_TYPE_SECTION_ATTR) )
        {
            sectionName = root.getAttribute(EXTERNAL_TYPE_SECTION_ATTR);
        }
        else
        {
            error("REQUIRED_ATTR_MISSING", new String[] {name, "name", "externalType"});
        }
        if( typeName != null && sectionName != null )
        {
            CollectionDescription eType = new CollectionDescription();
            eType.setModuleName(moduleName);
            eType.setTypeName(typeName);
            eType.setSectionName(sectionName);

            boolean readOnly = true;
            if( root.hasAttribute(EXTERNAL_TYPE_READONLY_ATTR) )
            {
                readOnly = Boolean.parseBoolean(root.getAttribute(EXTERNAL_TYPE_READONLY_ATTR));
            }
            eType.setReadOnly(readOnly);

            xmlModule.addExternalType(eType);
        }
    }

    protected void readTypes(Element root)
    {
        Element examplesElement = getElement(root, TYPES_ELEMENT);
        if( examplesElement == null )
        {
            return;
        }

        NodeList list = examplesElement.getElementsByTagName(INTERNAL_TYPE_ELEMENT);
        for( Element iTypeElement : XmlStream.elements( list ) )
        {
            try
            {
                readInternalType(iTypeElement);
            }
            catch( Throwable t )
            {
                error("ERROR_READ_INTERNAL_TYPE", new String[] {name, t.getMessage()}, t);
            }
        }
    }

    protected void readInternalType(Element root)
    {
        InternalType internalType = new InternalType();

        if( root.hasAttribute(INTERNAL_TYPE_SECTION_ATTR) )
        {
            internalType.setSection(root.getAttribute(INTERNAL_TYPE_SECTION_ATTR));
        }
        else
        {
            error("REQUIRED_ATTR_MISSING", new String[] {name, "section", "internalType"});
        }

        if( root.hasAttribute(INTERNAL_TYPE_NAME_ATTR) )
        {
            internalType.setName(root.getAttribute(INTERNAL_TYPE_NAME_ATTR));
        }
        else
        {
            error("REQUIRED_ATTR_MISSING", new String[] {name, "name", "internalType"});
        }

        if( root.hasAttribute(INTERNAL_TYPE_CLASS_ATTR) )
        {
            String className = root.getAttribute(INTERNAL_TYPE_CLASS_ATTR);
            internalType.setTypeClass(className);
        }
        else
        {
            error("REQUIRED_ATTR_MISSING", new String[] {name, "class", "internalType"});
        }

        if( root.hasAttribute(INTERNAL_TYPE_TRANSFORMER_ATTR) )
        {
            String className = root.getAttribute(INTERNAL_TYPE_TRANSFORMER_ATTR);
            internalType.setTypeTransformer(className);
        }
        else
        {
            error("REQUIRED_ATTR_MISSING", new String[] {name, "transformer", "internalType"});
        }

        if( root.hasAttribute(INTERNAL_TYPE_IDFORMAT_ATTR) )
        {
            String idFormat = root.getAttribute(INTERNAL_TYPE_IDFORMAT_ATTR);
            internalType.setIdFormat(idFormat);
        }

        NodeList list = root.getElementsByTagName(QUERY_SYSTEM_ELEMENT);
        if( list.getLength() > 0 )
        {
            try
            {
                Element queryElement = (Element)list.item(0);
                readQuerySystem(queryElement, internalType);
            }
            catch( Throwable t )
            {
                error("ERROR_READ_QUERY_SYSTEM", new String[] {name, t.getMessage()}, t);
            }
        }

        xmlModule.addInternalType(internalType);
    }

    protected void readQuerySystem(Element root, InternalType iType)
    {
        if( root.hasAttribute(QUERY_SYSTEM_CLASS_ATTR) )
        {
            iType.setQuerySystemClass(root.getAttribute(QUERY_SYSTEM_CLASS_ATTR));
        }
        else
        {
            //error("REQUIRED_ATTR_MISSING", new String[] {name, "class", "querySystem"});
        }
        if( root.hasAttribute(QUERY_SYSTEM_LUCENE_ATTR) )
        {
            iType.setLuceneIndexes(root.getAttribute(QUERY_SYSTEM_LUCENE_ATTR));
        }

        NodeList list = root.getElementsByTagName(INDEX_ELEMENT);
        for( Element indexElement : XmlStream.elements( list ) )
        {
            try
            {
                readIndex(indexElement, iType);
            }
            catch( Throwable t )
            {
                error("ERROR_READ_INTERNAL_TYPE", new String[] {name, t.getMessage()}, t);
            }
        }
    }

    protected void readIndex(Element root, InternalType iType)
    {
        IndexDescription indexDescription = new IndexDescription();
        if( root.hasAttribute(INDEX_ELEMENT_NAME_ATTR) )
        {
            indexDescription.setName(root.getAttribute(INDEX_ELEMENT_NAME_ATTR));
        }
        else
        {
            error("REQUIRED_ATTR_MISSING", new String[] {name, "name", "index"});
        }
        if( root.hasAttribute(INDEX_ELEMENT_CLASS_ATTR) )
        {
            indexDescription.setIndexClass(root.getAttribute(INDEX_ELEMENT_CLASS_ATTR));
        }
        else
        {
            error("REQUIRED_ATTR_MISSING", new String[] {name, "class", "index"});
        }
        if( root.hasAttribute(INDEX_ELEMENT_TABLE_ATTR) )
        {
            indexDescription.setTable(root.getAttribute(INDEX_ELEMENT_TABLE_ATTR));
        }
        iType.getIndexes().add(indexDescription);
    }
}
