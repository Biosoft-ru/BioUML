package biouml.workbench.module.xml;

import java.io.OutputStream;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import biouml.model.CollectionDescription;
import biouml.workbench.module.xml.XmlModule.DiagramTypeDescription;
import biouml.workbench.module.xml.XmlModule.InternalType;
import biouml.workbench.module.xml.XmlModule.InternalType.IndexDescription;

import ru.biosoft.access.SqlDataCollection;

public class XmlModuleWriter extends XmlModuleSupport
{
    protected static final Logger log = Logger.getLogger(XmlModuleWriter.class.getName());

    private Document doc;

    /** Stream to store the XML . */
    protected OutputStream stream;

    protected XmlModule module;

    public XmlModuleWriter(OutputStream stream)
    {
        this.stream = stream;
    }

    public void write(XmlModule module) throws Exception
    {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        write(module, transformerFactory.newTransformer());
    }

    public void write(XmlModule module, Transformer transformer) throws Exception
    {
        if( module == null )
        {
            String msg = "XmlModuleWriter - module is null, can not be written.";
            Exception e = new NullPointerException(msg);
            log.log(Level.SEVERE, msg, e);
            throw e;
        }

        this.module = module;
        module.setReadingAvailable(false);

        buildDocument();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(stream);
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.MEDIA_TYPE, "text/xml");
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        transformer.transform(source, result);

        module.setReadingAvailable(true);
    }

    protected Document buildDocument() throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        doc = builder.newDocument();
        doc.appendChild(createDBModuleElement());
        return doc;
    }

    protected Element createDBModuleElement()
    {
        Element element = doc.createElement(DB_DATABASE_ELEMENT);
        element.setAttribute(NAME_ATTR, module.getName());
        element.setAttribute(TITLE_ATTR, module.getInfo().getDisplayName());
        element.setAttribute(DESCRIPTION_ATTR, module.getInfo().getDescription());
        element.setAttribute(VERSION_ATTR, module.getVersion());
        element.setAttribute(TYPE_ATTR, module.getModuleType());

        if( module.getModuleType().equals(XmlModuleConstants.TYPE_SQL) )
        {
            Properties properties = module.getDatabaseProperties();
            if( properties.containsKey(DATABASE_TYPE_ATTR) && properties.getProperty(DATABASE_TYPE_ATTR).length() > 0 )
            {
                element.setAttribute(DATABASE_TYPE_ATTR, properties.getProperty(DATABASE_TYPE_ATTR));
            }
            if( properties.containsKey(DATABASE_VERSION_ATTR) && properties.getProperty(DATABASE_VERSION_ATTR).length() > 0 )
            {
                element.setAttribute(DATABASE_VERSION_ATTR, properties.getProperty(DATABASE_VERSION_ATTR));
            }
            if( properties.containsKey(DATABASE_NAME_ATTR) && properties.getProperty(DATABASE_NAME_ATTR).length() > 0 )
            {
                element.setAttribute(DATABASE_NAME_ATTR, properties.getProperty(DATABASE_NAME_ATTR));
            }

            Element dbElement = doc.createElement(JDBC_CONNECTION_ELEMENT);
            if( properties.containsKey(SqlDataCollection.JDBC_DRIVER_PROPERTY) )
            {
                dbElement.setAttribute(JDBC_CONNECTION_DRIVER_ATTR, properties.getProperty(SqlDataCollection.JDBC_DRIVER_PROPERTY));
            }
            if( properties.containsKey(SqlDataCollection.JDBC_URL_PROPERTY) )
            {
                dbElement.setAttribute(JDBC_CONNECTION_URL_ATTR, properties.getProperty(SqlDataCollection.JDBC_URL_PROPERTY));
            }
            if( properties.containsKey(SqlDataCollection.JDBC_USER_PROPERTY) )
            {
                dbElement.setAttribute(JDBC_CONNECTION_USER_ATTR, properties.getProperty(SqlDataCollection.JDBC_USER_PROPERTY));
            }
            if( properties.containsKey(SqlDataCollection.JDBC_PASSWORD_PROPERTY) )
            {
                dbElement.setAttribute(JDBC_CONNECTION_PASSWORD_ATTR, properties.getProperty(SqlDataCollection.JDBC_PASSWORD_PROPERTY));
            }
            element.appendChild(dbElement);
        }

        createDependenciesElement(element);
        createTypesElement(element);

        return element;
    }

    protected void createDependenciesElement(Element element)
    {
        Element dElement = doc.createElement(DEPENDENCIES_ELEMENT);
        if( module.getExternalTypes() != null )
        {
            createExternalModulesElement(dElement);
        }
        if( module.getDiagramTypes() != null )
        {
            for( DiagramTypeDescription dt : module.getDiagramTypes() )
            {
                createDiagramTypeElement(dElement, dt);
            }
        }
        element.appendChild(dElement);
    }

    protected void createExternalModulesElement(Element element)
    {
        List<String> processedModules = new ArrayList<>();
        while( true )
        {
            String moduleName = null;
            for( CollectionDescription cd : module.getExternalTypes() )
            {
                if( !processedModules.contains(cd.getModuleName()) )
                {
                    moduleName = cd.getModuleName();
                    break;
                }
            }
            if( moduleName == null )
                break;

            processedModules.add(moduleName);

            Element externalModule = doc.createElement(EXTERNAL_DATABASE_ELEMENT);
            externalModule.setAttribute(EXTERNAL_DATABASE_NAME_ATTR, moduleName);

            for( CollectionDescription cd : module.getExternalTypes() )
            {
                if( cd.getModuleName().equals(moduleName) )
                {
                    creatExternalTypeElement(externalModule, cd);
                }
            }

            element.appendChild(externalModule);
        }
    }

    protected void creatExternalTypeElement(Element element, CollectionDescription cd)
    {
        Element etElement = doc.createElement(EXTERNAL_TYPE_ELEMENT);

        etElement.setAttribute(EXTERNAL_TYPE_NAME_ATTR, cd.getTypeName());
        etElement.setAttribute(EXTERNAL_TYPE_SECTION_ATTR, cd.getSectionName());

        String type = "true";
        if( !cd.isReadOnly() )
            type = "false";
        etElement.setAttribute(EXTERNAL_TYPE_READONLY_ATTR, type);

        element.appendChild(etElement);
    }

    protected void createDiagramTypeElement(Element element, DiagramTypeDescription dt)
    {
        Element dtElement = doc.createElement(GRAPHIC_NOTATION_ELEMENT);

        dtElement.setAttribute(GRAPHIC_NOTATION_NAME_ATTR, dt.getName());
        dtElement.setAttribute(GRAPHIC_NOTATION_TYPE_ATTR, dt.getType());

        if( dt.getClassName() != null && dt.getClassName().length() > 0 )
        {
            dtElement.setAttribute(GRAPHIC_NOTATION_CLASS_ATTR, dt.getClassName());
        }
        if( dt.getPath() != null && dt.getPath().length() > 0 )
        {
            dtElement.setAttribute(GRAPHIC_NOTATION_PATH_ATTR, dt.getPath());
        }

        element.appendChild(dtElement);
    }

    protected void createTypesElement(Element element)
    {
        Element typesElement = doc.createElement(TYPES_ELEMENT);
        if( module.getInternalTypes() != null )
        {
            for( InternalType it : module.getInternalTypes() )
            {
                createInternalType(typesElement, it);
            }
        }
        element.appendChild(typesElement);
    }

    protected void createInternalType(Element element, InternalType it)
    {
        Element itElement = doc.createElement(INTERNAL_TYPE_ELEMENT);

        itElement.setAttribute(INTERNAL_TYPE_NAME_ATTR, it.getName());
        itElement.setAttribute(INTERNAL_TYPE_SECTION_ATTR, it.getSection());
        itElement.setAttribute(INTERNAL_TYPE_CLASS_ATTR, it.getTypeClass());
        itElement.setAttribute(INTERNAL_TYPE_TRANSFORMER_ATTR, it.getTypeTransformer());

        if( it.getIdFormat() != null && it.getIdFormat().length() > 0 )
        {
            itElement.setAttribute(INTERNAL_TYPE_IDFORMAT_ATTR, it.getIdFormat());
        }

        createQuerySystem(itElement, it);

        element.appendChild(itElement);
    }

    protected void createQuerySystem(Element element, InternalType it)
    {
        Element qsElement = doc.createElement(QUERY_SYSTEM_ELEMENT);

        if( it.getQuerySystemClass() != null && it.getQuerySystemClass().length() > 0 )
        {
            qsElement.setAttribute(QUERY_SYSTEM_CLASS_ATTR, it.getQuerySystemClass());
        }
        if( it.getLuceneIndexes() != null && it.getLuceneIndexes().length() > 0 )
        {
            qsElement.setAttribute(QUERY_SYSTEM_LUCENE_ATTR, it.getLuceneIndexes());
        }

        if( it.getIndexes() != null )
        {
            for( IndexDescription id : it.getIndexes() )
            {
                createIndexDescription(qsElement, id);
            }
        }

        element.appendChild(qsElement);
    }

    protected void createIndexDescription(Element element, IndexDescription id)
    {
        Element idElement = doc.createElement(INDEX_ELEMENT);

        if( id.getName() != null )
        {
            idElement.setAttribute(INDEX_ELEMENT_NAME_ATTR, id.getName());
        }
        if( id.getIndexClass() != null )
        {
            idElement.setAttribute(INDEX_ELEMENT_CLASS_ATTR, id.getIndexClass());
        }
        if( id.getTable() != null && id.getTable().length() > 0 )
        {
            idElement.setAttribute(INDEX_ELEMENT_TABLE_ATTR, id.getTable());
        }

        element.appendChild(idElement);
    }
}
