package biouml.plugins.jupyter.auth;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.message.BasicNameValuePair;

import biouml.plugins.jupyter.configuration.JupyterConfiguration;

public class BioumlJupyterAccessor implements JupyterAccessor
{
    private static final Logger log = Logger.getLogger( BioumlJupyterAccessor.class.getName() );

    protected final String loginUrl;
    public BioumlJupyterAccessor(JupyterConfiguration configuration)
    {
        this.loginUrl = configuration.getLoginUrl();
    }

    @Override
    public List<String> getAuthCookies(String user, String password)
    {
        List<String> authCookies = new ArrayList<>();

        try
        {
            List<BasicNameValuePair> formParams = new ArrayList<>();
            formParams.add( new BasicNameValuePair( "username", user ) );
            formParams.add( new BasicNameValuePair( "password", password ) );

            HttpResponse response = Request.Post( loginUrl )
                    .bodyForm( formParams, Charset.forName( "UTF-8" ) )
                    .execute()
                    .returnResponse();
            Header[] headers = response.getHeaders( "Set-Cookie" );
            if( headers == null )
                return authCookies;

            for( Header header : headers )
            {
                String value = header.getValue();
                if( value != null )
                    authCookies.add( value );
            }

            log.info( "Auth cookies from '" + loginUrl + "' for user '" + user + "' = \n" + authCookies );
        }
        catch( Exception e )
        {
            log.log( Level.SEVERE, "Error during login user '" + user + "' to jupyter", e );
        }
        return authCookies;
    }

}
