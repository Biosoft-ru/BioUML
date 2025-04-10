package ru.biosoft.server.servlets.webservices.providers;

import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.HtmlDescribedElement;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.BiosoftWebResponse;
import ru.biosoft.templates.TemplateRegistry;
import ru.biosoft.templates.TemplateUtils;
import ru.biosoft.util.TextUtil2;

/**
 * @author lan
 *
 */
public class HtmlPageTemplateProvider extends WebProviderSupport
{
    @Override
    public void process(BiosoftWebRequest arguments, BiosoftWebResponse resp) throws Exception
    {
        resp.setContentType("text/html");
        String templateName = arguments.get("templateName");
        if( templateName == null )
        {
            templateName = "Default";
        }
        try (PrintWriter pw = new PrintWriter( resp.getOutputStream() ))
        {
            DataElement de = null;
            StringBuffer result;
            try
            {
                de = arguments.getDataElement();
                result = TemplateRegistry.mergeTemplate( de, templateName );
            }
            catch( Exception e )
            {
                result = new StringBuffer();
                result.append( "<div class='log_error'>" ).append( ExceptionRegistry.log( e ) ).append( "</div>" );
            }
            String html;
            String baseId = "";
            if( de instanceof HtmlDescribedElement )
            {
                baseId = ( (HtmlDescribedElement)de ).getBaseId() + "/";
            }
            html = result.toString();

            if( !html.startsWith( "<html>" ) )
                html = "<pre>" + html;//non html text should keep its formatting

            html = html.replaceAll( "<math", "<math displaystyle=\"true\"" );
            html = html.replaceAll( "href=\"de:([^\"]+)\"", "" );
            Pattern pattern = Pattern.compile( "<img([^>]*) src=\"([^\"]+)\"" );
            Matcher matcher = pattern.matcher( html );
            int start = 0;
            while( matcher.find( start ) )
            {
                html = html.substring( 0, matcher.start() ) + "<img" + matcher.group( 1 ) + " src=\"img?id="
                        + ( matcher.group( 2 ).contains( "://" ) ? "" : baseId )
                        + TextUtil2.encodeURL( StringEscapeUtils.unescapeHtml( matcher.group( 2 ) ) ) + "\""
                        + html.substring( matcher.end() );
                start = matcher.end();
                matcher = pattern.matcher( html );
            }
            html = html.replaceFirst( "<html>", "<html>" + TemplateUtils.getStyle() );
            pw.print( html );
        }
    }
}
