package ru.biosoft.analysiscore;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONArray;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisMethodElement;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.server.JSONUtils;
import ru.biosoft.util.XmlUtil;


public class AnalysisMethodReader extends AnalysisMethodConstants
{
    protected String name;
    protected InputStream stream;
    private Logger log;

    public AnalysisMethodReader(String name, InputStream stream)
    {
        log = Logger.getLogger( AnalysisMethodReader.class.getName() );
        this.name = name;
        this.stream = stream;
    }

    public AnalysisMethodElement read(DataCollection<?> origin) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = null;

        try
        {
            doc = builder.parse( stream );
        }
        catch( SAXException e )
        {
            log.log( Level.SEVERE, "Parse analysis method \"" + name + "\" error: " + e.getMessage() );
            return null;
        }
        catch( IOException e )
        {
            log.log( Level.SEVERE, "Read analysis method \"" + name + "\" error: " + e.getMessage() );
            return null;
        }
        return read( origin, doc );
    }

    public AnalysisMethodElement read(DataCollection<?> origin, Document document)
    {
        Element root = document.getDocumentElement();

        if( ANALYSIS_METHOD_ELEMENT.equals( root.getTagName() ) )
        {
            String methodElementName = root.getAttribute( ID_ATTR );
            AnalysisMethodElement methodElement = new AnalysisMethodElement( methodElementName, origin );

            String methodName = root.getAttribute( METHOD_NAME_ATTR );
            AnalysisMethod method = AnalysisMethodRegistry.getAnalysisMethod( methodName );

            Element parametersElement = getElement( root, ANALYSIS_METHOD_PARAMETERS_ELEMENT );
            JSONArray params = new JSONArray( parametersElement.getAttribute( JSON_VALUE_ATTR ) );
            JSONUtils.correctBeanOptions( method.getParameters(), params );

            methodElement.setAnalysisMethod( method );

            return methodElement;
        }
        return null;
    }

    protected Element getElement(Element element, String childName)
    {
        Element child = null;
        String elementName = element.getTagName();
        try
        {
            Element result = null;
            for( Element e : XmlUtil.elements( element, childName ) )
            {
                if( result == null )
                    result = e;
                else
                    log.warning( "Analysis method reader: can not process element " + childName );
            }
            return result;
        }
        catch( Throwable t )
        {
            log.log( Level.SEVERE, "Model" + elementName + ": can not read element " + childName + ", error: " + t.getMessage() );
        }
        return child;
    }
}