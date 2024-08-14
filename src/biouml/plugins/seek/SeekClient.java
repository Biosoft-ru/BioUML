package biouml.plugins.seek;

import java.util.ArrayList;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

class SeekClient
{
    static String obtainSessionId(String seekUrl, String login, String password)
    {
        if( seekUrl == null )
        {
            return null;
        }
        String sessionId = null;
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build())
        {
            // String baseUrlString = "http://test.genexplain.com/seek/";
            HttpPost httpPost = new HttpPost( seekUrl + "/session" );
            ArrayList<NameValuePair> params = new ArrayList<>();
            params.add( new BasicNameValuePair( "login", login ) );
            params.add( new BasicNameValuePair( "password", password ) );
            httpPost.setEntity( new UrlEncodedFormEntity( params ) );

            CloseableHttpResponse response = httpClient.execute( httpPost );
            Header cookie = response.getFirstHeader( "Set-Cookie" );
            if( response.getFirstHeader( "location" ) != null && seekUrl.equals( response.getFirstHeader( "location" ).getValue() ) )
            {
                sessionId = cookie.getElements()[0].getValue();// _session_id
            }
        }
        catch( Exception e )
        {
        }
        return sessionId;
        // TODO alert on catch
    }



    static String downloadUrl(String seekUrl, String dfId)
    {
        return seekUrl + "/data_files/" + dfId + "/download";
    }
}
