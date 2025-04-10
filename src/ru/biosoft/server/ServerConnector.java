package ru.biosoft.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.exception.BiosoftNetworkException;
import ru.biosoft.access.security.CodePrivilege;
import ru.biosoft.access.security.CodePrivilegeType;
import ru.biosoft.util.TextUtil2;

import com.developmentontheedge.application.ApplicationUtils;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

@CodePrivilege ( CodePrivilegeType.SYSTEM )
public class ServerConnector
{
    private final String baseURL;
    private String sessionCookie;
    private static boolean isHTTSinit = false;

    protected static void initHTTPS()
    {
        if( !isHTTSinit )
        {
            isHTTSinit = true;
            String handlers = System.getProperty("java.protocol.handler.pkgs");
            if( handlers == null )
            {
                // nothing specified yet (expected case)
                System.setProperty("java.protocol.handler.pkgs", "javax.net.ssl");
            }
            else
            {
                // something already there, put ourselves out front
                System.setProperty("java.protocol.handler.pkgs", "javax.net.ssl|".concat(handlers));
            }
            HostnameVerifier hv = (arg0, arg1) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(hv);
    
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager()
            {
                @Override
                public X509Certificate[] getAcceptedIssuers()
                {
                    return null;
                }
    
                @Override
                public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException
                {
                }
    
                @Override
                public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException
                {
                }
            }};
    
            // Install the all-trusting trust manager
            try
            {
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            }
            catch( Exception e )
            {
            }
        }
    }

    public ServerConnector(String baseURL)
    {
        this(baseURL, null);
    }
    
    public ServerConnector(String baseURL, String sessionId)
    {
        initHTTPS();
        if(!baseURL.endsWith("/")) baseURL+="/";
        if(!baseURL.toLowerCase().startsWith("http")) baseURL = "http://"+baseURL;
        if(!baseURL.endsWith("biouml/"))
            baseURL = baseURL+"biouml/";
        this.baseURL = baseURL;
        if(sessionId != null)
            this.sessionCookie = "JSESSIONID="+sessionId;
    }
    
    public InputStream queryServer(String action, Map<String, String> parameters) throws BiosoftNetworkException
    {
        try
        {
            String url = baseURL + action;
            HttpURLConnection urlc = (HttpURLConnection)new URL(url).openConnection();
            
            urlc.setUseCaches(false); // Don't look at possibly cached data
            if( sessionCookie != null )
            {
                //set cookie for session support
                urlc.setRequestProperty("Cookie", sessionCookie);
            }
            urlc.setRequestMethod("POST");
            urlc.setDoOutput(true);
            if( parameters != null )
            {
                String paramStr = EntryStream.of( parameters ).mapKeys( TextUtil2::encodeURL ).mapValues( TextUtil2::encodeURL ).join( "=" )
                        .joining( "&" );
                try( OutputStreamWriter wr = new OutputStreamWriter( urlc.getOutputStream() ) )
                {
                    wr.write( paramStr );
                    wr.flush();
                }
            }
    
            List<String> cookies = urlc.getHeaderFields().get("Set-Cookie");
            if( cookies != null )
            {
                sessionCookie = StreamEx.of(cookies).map( cookie -> TextUtil2.split( cookie, ';' )[0] ).joining( "; " );
            }
            return urlc.getInputStream();
        }
        catch( Exception e )
        {
            throw new BiosoftNetworkException(e, baseURL);
        }
    }

    public JsonObject queryJSON(String action, Map<String, String> parameters) throws BiosoftNetworkException
    {
        InputStream stream = queryServer(action, parameters);
        String response;
        try
        {
            response = ApplicationUtils.readAsString(stream);
        }
        catch( IOException e )
        {
            throw new BiosoftNetworkException(e, baseURL);
        }
        JsonObject jsonResponse = null;
        try
        {
            jsonResponse = JsonObject.readFrom( response );
        }
        catch( ParseException | UnsupportedOperationException e )
        {
            throw new BiosoftNetworkException(e, baseURL);
        }
        int type = jsonResponse.getInt("type", -1);
        if( type != 0 && type != 2 )
        {
            throw new BiosoftNetworkException(baseURL, jsonResponse);
        }
        return jsonResponse;
    }
    
    public void login(String userName, String password) throws BiosoftNetworkException
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("username", userName);
        parameters.put("password", password);
        queryJSON("web/login", parameters);
    }
    
    public void logout() throws BiosoftNetworkException
    {
        queryJSON("web/logout", Collections.<String, String>emptyMap());
    }
    
    public InputStream getContentStream(DataElementPath path) throws BiosoftNetworkException
    {
        return queryServer("web/content/"+path, null);
    }
    
    public String getContent(DataElementPath path) throws BiosoftNetworkException
    {
        try
        {
            return ApplicationUtils.readAsString(getContentStream(path));
        }
        catch( IOException e )
        {
            throw new BiosoftNetworkException(e, baseURL);
        }
    }
}
