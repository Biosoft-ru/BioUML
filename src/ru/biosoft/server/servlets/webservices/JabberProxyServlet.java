package ru.biosoft.server.servlets.webservices;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.util.NetworkConfigurator;

/**
 * @author lan
 *
 */
public class JabberProxyServlet
{
    String jabberServer;

    public void init(String[] args)
    {
        jabberServer = Application.getGlobalValue("JabberServer");
    }
    
    public String service(InputStream input, OutputStream output)
    {
        if(jabberServer == null || jabberServer.isEmpty())
        {
            return "No jabber server specified";
        }
        URL url;
        try
        {
            url = new URL(jabberServer);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection(NetworkConfigurator.getProxyObject());
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            ApplicationUtils.copyStream(connection.getOutputStream(), input);
            ApplicationUtils.copyStream(output, connection.getInputStream());
            connection.disconnect();
        }
        catch( Exception e )
        {
            return e.getMessage();
        }
        return null;
    }
}
