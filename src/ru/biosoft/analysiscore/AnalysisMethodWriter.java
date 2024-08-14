package ru.biosoft.analysiscore;

import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.json.JSONArray;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.Property;

import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisMethodElement;
import ru.biosoft.server.JSONUtils;
import ru.biosoft.util.FieldMap;


public class AnalysisMethodWriter extends AnalysisMethodConstants
{
    private Document doc;
    private OutputStream stream;
    private Logger log;

    public AnalysisMethodWriter(OutputStream stream)
    {
        log = Logger.getLogger( AnalysisMethodWriter.class.getName() );
        this.stream = stream;
    }

    public void write(AnalysisMethodElement methodElement) throws Exception
    {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        write( methodElement, transformerFactory.newTransformer() );
    }

    public void write(AnalysisMethodElement methodElement, Transformer transformer) throws Exception
    {
        if( methodElement == null )
        {
            String msg = "The null analysis method cannot be written.";
            Exception e = new NullPointerException( msg );
            log.log( Level.SEVERE, msg, e );
            throw e;
        }

        buildDocument( methodElement );
        DOMSource source = new DOMSource( doc );
        StreamResult result = new StreamResult( stream );
        transformer.setOutputProperty( javax.xml.transform.OutputKeys.MEDIA_TYPE, "text/xml" );
        transformer.setOutputProperty( javax.xml.transform.OutputKeys.INDENT, "yes" );
        transformer.transform( source, result );
    }

    protected Document buildDocument(AnalysisMethodElement methodElement) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        doc = builder.newDocument();
        doc.appendChild( createAnalysisElement( methodElement ) );
        return doc;
    }

    protected Element createAnalysisElement(AnalysisMethodElement methodElement)
    {
        Element element = doc.createElement( ANALYSIS_METHOD_ELEMENT );
        element.setAttribute( ID_ATTR, methodElement.getName() );

        AnalysisMethod method = methodElement.getAnalysisMethod();

        element.setAttribute( METHOD_NAME_ATTR, method.getName() );
        element.appendChild( createParametersElement( method ) );
        return element;
    }

    protected Element createParametersElement(AnalysisMethod method)
    {
        Element element = doc.createElement( ANALYSIS_METHOD_PARAMETERS_ELEMENT );
        try
        {
            JSONArray params = JSONUtils.getModelAsJSON( ComponentFactory.getModel( method.getParameters() ), FieldMap.ALL, Property.SHOW_EXPERT );
            element.setAttribute( JSON_VALUE_ATTR, params.toString() );
        }
        catch( Exception e )
        {
            log.log( Level.SEVERE, "AnalysisMethodWriter: error of the analysis parameters serialization.", e );
        }
        return element;
    }
}
