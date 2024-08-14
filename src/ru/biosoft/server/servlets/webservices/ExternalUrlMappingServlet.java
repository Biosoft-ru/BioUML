package ru.biosoft.server.servlets.webservices;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import one.util.streamex.StreamEx;
import ru.biosoft.access.ExternalURLMapper;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.server.AbstractServlet;
import ru.biosoft.util.TextUtil;

/**
 * @author anna
 *
 */
public class ExternalUrlMappingServlet extends AbstractServlet
{
    @Override
    public String service(String localAddress, Object session, Map params, OutputStream out, Map<String, String> header)
    {
        return "text/html";
    }

    public void service(String localAddress, Object session, Map params, OutputStream out, Object response)
    {
        try
        {
            BiosoftWebResponse resp = new BiosoftWebResponse( response, out );
            resp.setContentType( "text/html" );
            WebSession.getSession( session );
            String url;
            if( params.containsKey( "registration" ) )
            {
                url = SecurityManager.getSecurityProvider().getRegistrationURL();
            }
            else if( params.containsKey( "forgot_password" ) )
            {
                url = SecurityManager.getSecurityProvider().getForgetPasswordURL();
            }
            else if( params.containsKey( "login" ) )
            {
                String addParams;
                if(params.containsKey( "params" )) {
                    addParams = ( ( (Object[])params.get( "params" ) )[0] ).toString();
                } else {
                    addParams = "";
                }
                url = SecurityManager.getSecurityProvider().getLoginURL( addParams );
            }
            else if( params.containsKey( "logout" ) )
            {
                url = SecurityManager.getSecurityProvider().getLogoutURL();
            }
            else if( params.containsKey( "reinit" ) )
            {
                String addParams;
                if(params.containsKey( "params" )) {
                    addParams = ( ( (Object[])params.get( "params" ) )[0] ).toString();
                } else {
                    addParams = "";
                }
                url = SecurityManager.getSecurityProvider().getReinitURL( addParams );
            }
            else
            {
                DataElement de = CollectionFactory.getDataElement( ( ( (Object[])params.get( "de" ) )[0] ).toString() );
                url = ExternalURLMapper.getExternalUrl( de );
            }
            OutputStreamWriter ow = new OutputStreamWriter( out, "UTF8" );
            ow.write( "<html><head>" );
            if( url.isEmpty() )
            {
                ow.write( "<body>Error: cannot find appropriate external URL</body>" );
            }
            else if( url.startsWith( "http-post://" ) )
            {
                Map<String, List<String>> urlParameters = getUrlParameters( url );
                int questionPos = url.indexOf( "?" );
                url = "http://" + url.substring( "http-post://".length(), questionPos == -1 ? url.length() : questionPos );
                ow.write( "<body><form method=\"post\" id=\"form\" action=\"" + url + "\">" );
                for( Entry<String, List<String>> entry : urlParameters.entrySet() )
                {
                    for( String value : entry.getValue() )
                    {
                        ow.write( "<input type=\"hidden\" name=\"" + entry.getKey() + "\" value=\"" + value + "\">" );
                    }
                }
                ow.write( "</form><script>document.getElementById('form').submit();</script></body>" );
            }
            else
            {
                ow.write( "<meta http-equiv=\"refresh\" content=\"0;url=" + url + "\"/><body></body>" );
            }
            ow.write( "</html>" );
            ow.flush();
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE,  "Support servlet exception", e );
        }
    }

    public static Map<String, List<String>> getUrlParameters(String url)
    {
        String[] urlParts = url.split( "\\?" );
        if( urlParts.length > 1 )
        {
            String query = urlParts[1];
            return StreamEx.split( query, '&' ).map( param -> TextUtil.split( param, '=' ) )
                    .mapToEntry( pair -> TextUtil.decodeURL( pair[0] ), pair -> pair.length > 1 ? TextUtil.decodeURL( pair[1] ) : "" )
                    .grouping();
        }
        return Collections.emptyMap();
    }
}
