package ru.biosoft.server.servlets.webservices.providers;

import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import org.apache.commons.lang.StringEscapeUtils;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.HtmlDataElement;
import ru.biosoft.access.ImageElement;
import ru.biosoft.access.core.TextDataElement;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.html.ZipHtmlDataCollection;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.BiosoftWebResponse;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.util.ImageGenerator;

/**
 * Provides the content of applicable elements.
 * @author lan
 */
public class ContentProvider extends WebProviderSupport
{
    @Override
    public void process(BiosoftWebRequest arguments, BiosoftWebResponse resp) throws Exception
    {
        OutputStream out = resp.getOutputStream();
        try
        {
            String path = arguments.getAction();
            if(path.endsWith("/")) path+="index.html";
            DataElement de = DataElementPath.create(path).getDataElement();
            String lowerCaseName = de.getName().toLowerCase();
            if(DataCollectionUtils.checkPrimaryElementType( de, ZipHtmlDataCollection.class ))
            {
                resp.setContentType("text/html");
                try (PrintWriter pw = new PrintWriter( out ))
                {
                    pw.println( "<html><meta http-equiv=\"refresh\" content=\"0;url=" + de.getName() + "/\"></html>" );
                }
            }
            else if( de instanceof HtmlDataElement )
            {
                resp.setContentType("text/html");
                out.write( ( (HtmlDataElement)de ).getContent().getBytes("utf-8"));
            }
            else if( de instanceof TextDataElement )
            {
                resp.setContentType(lowerCaseName.endsWith(".css") ? "text/css" : lowerCaseName.endsWith(".js") ? "application/javascript"
                        : "text/plain");
                out.write( ( (TextDataElement)de ).getContent().getBytes());
            }
            else if( de instanceof ImageElement )
            {
                resp.setContentType("image/png");
                ImageGenerator.encodeImage( ( (ImageElement)de ).getImage(null), "PNG", out);
            }
            else if( de instanceof FileDataElement )
            {
                resp.setContentType(lowerCaseName.endsWith(".pdf") ? "application/pdf" : lowerCaseName.endsWith(".gif") ? "image/gif" : "application/octet-stream");
                ApplicationUtils.copyStream(out, new FileInputStream( ( (FileDataElement)de ).getFile()));
            }
            else
                throw new WebException("EX_QUERY_UNSUPPORTED_ELEMENT", path);
            out.close();
        }
        catch( Exception e )
        {
            resp.setContentType("text/html");
            try (PrintWriter pw = new PrintWriter( out ))
            {
                pw.println( "<html><body><h1>Error</h1><p>"
                        + ( e instanceof WebException ? e.getMessage()
                                : StringEscapeUtils.escapeHtml( ExceptionRegistry.log( e ) ).replaceAll( "\n", "<br>" ) )
                        + "</p></body></html>" );
            }
        }
    }
}
