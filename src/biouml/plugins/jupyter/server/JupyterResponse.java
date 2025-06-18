package biouml.plugins.jupyter.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.eclipsesource.json.JsonObject;

import ru.biosoft.server.servlets.webservices.BiosoftWebResponse;
import ru.biosoft.server.servlets.webservices.BiosoftWebResponse.CookieTemplate;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.util.TextUtil2;

public class JupyterResponse extends JSONResponse
{
    private static final int MAX_COOKIE_AGE = 30 * 24 * 60 * 60; //30 days in seconds

    private static final String HTTP_ONLY = "HttpOnly";
    private static final String PATH = "Path";
    private static final String EXPIRES = "expires";

    private BiosoftWebResponse resp;
    public JupyterResponse(BiosoftWebResponse resp)
    {
        super( resp );
        this.resp = resp;
    }

    public void setHeader(String key, String value)
    {
        resp.setHeader( key, value );
    }

    public void setCookies(List<String> cookies)
    {
        if( cookies == null )
            return;
        List<CookieTemplate> templates = new ArrayList<>();
        for( String cookie : cookies )
        {
            CookieTemplate cookieTemplate = parseCookie( cookie );
            if( cookieTemplate != null )
                templates.add( cookieTemplate );
        }

        resp.setCookies( templates );
    }

    public void sendSimpleOK() throws IOException
    {
        JsonObject result = new JsonObject();
        result.add( "type", "ok" );
        sendJSON( result );
    }

    /**
     * Examples:
     * jupyterhub-session-id=4bb86963b3da4635acc774a6d5913a06; HttpOnly; Path=/
     * jupyterhub-hub-login="2|1:0|...some more info here..."; expires=Sat, 17 Aug 2019 10:05:51 GMT; HttpOnly; Path=/jupyter/hub/
     * @param cookie
     * @return
     */
    public static CookieTemplate parseCookie(String cookie)
    {
        String name = null;
        String value = null;
        String path = null;
        int maxAge = -1;
        boolean httpOnly = false;

        String[] cookieParts = TextUtil2.split( cookie, ';' );
        for( String cookiePart : cookieParts )
        {
            cookiePart = cookiePart.trim();
            int eqIndex = cookiePart.indexOf( "=" );
            if( eqIndex != -1 )
            {
                String propName = cookiePart.substring( 0, eqIndex );
                String propValue = cookiePart.substring( eqIndex + 1 );
                if( PATH.equalsIgnoreCase( propName ) )
                {
                    path = propValue;
                }
                else if( EXPIRES.equalsIgnoreCase( propName ) )
                {
                    maxAge = MAX_COOKIE_AGE;
                }
                else if( name == null && value == null )
                {
                    name = propName;
                    value = propValue;
                }
            }
            else if( HTTP_ONLY.equalsIgnoreCase( cookiePart ) )
            {
                httpOnly = true;
            }
        }
        if( name == null || value == null )
            return null;

        return new CookieTemplate( name, value, path, maxAge, httpOnly );
    }
}
