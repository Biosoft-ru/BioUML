package ru.biosoft.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.InetSocketAddress;
import java.net.Proxy;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.exception.ExceptionRegistry;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;
import com.developmentontheedge.application.Application;

public class NetworkConfigurator
{
    protected static final Logger log = Logger.getLogger(NetworkConfigurator.class.getName());

    public static final String PROXY_PREFERENCES_PROXY = "Proxy";
    public static final String PROXY_PREFERENCES_PROXY_USE = "Use";
    public static final String PROXY_PREFERENCES_PROXY_HOST = "Host";
    public static final String PROXY_PREFERENCES_PROXY_PORT = "Port";
    public static final String PROXY_PREFERENCES_PROXY_USERNAME = "Username";
    public static final String PROXY_PREFERENCES_PROXY_PASSWORD = "Password";

    protected static Preferences getProxySettings() throws Exception
    {
        Preferences preferences = Application.getPreferences();
        if(preferences == null)
            return new Preferences();
        Preferences proxyPreferences = (Preferences)preferences.getValue(PROXY_PREFERENCES_PROXY);
        if( proxyPreferences == null )
        {
            proxyPreferences = new Preferences();
            preferences.add(new DynamicProperty(PROXY_PREFERENCES_PROXY, PROXY_PREFERENCES_PROXY, "Proxy preferences", Preferences.class,
                    proxyPreferences));
        }
        //update proxy as far as it changed
        proxyPreferences.removePropertyChangeListener(changeListener);
        proxyPreferences.addPropertyChangeListener(changeListener);
        return proxyPreferences;
    }
    public static String getHost() throws Exception
    {
        return (String)getProxySettings().getValue(PROXY_PREFERENCES_PROXY_HOST);
    }
    public static int getPort() throws Exception
    {
        Integer port = (Integer)getProxySettings().getValue(PROXY_PREFERENCES_PROXY_PORT);
        if( port == null )
            return 0;

        return port.intValue();
    }
    public static String getUsername() throws Exception
    {
        return (String)getProxySettings().getValue(PROXY_PREFERENCES_PROXY_USERNAME);
    }
    public static String getPassword() throws Exception
    {
        return (String)getProxySettings().getValue(PROXY_PREFERENCES_PROXY_PASSWORD);
    }
    public static boolean isProxyUsed() throws Exception
    {
        return getProxySettings().getBooleanValue(PROXY_PREFERENCES_PROXY_USE, false);
    }
    public static void changeProxySettings(Preferences proxySettings) throws Exception
    {
        Preferences preferences = Application.getPreferences();
        preferences.add(new DynamicProperty(PROXY_PREFERENCES_PROXY, PROXY_PREFERENCES_PROXY, "Proxy preferences", Preferences.class,
                proxySettings));
        setProxy();
    }

    public static Proxy getProxyObject() throws Exception
    {
        if(isProxyUsed())
            return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(getHost(), getPort()));
        return Proxy.NO_PROXY;
    }

    /**
     * Sets proxy related properties if needed
     */
    public static void setProxy()
    {
        try
        {
            boolean useProxy = isProxyUsed();
            String proxyHost = getHost();
            int proxyPort = getPort();
            String proxyUser = getUsername();
            String proxyPassword = getPassword();

            if( useProxy && proxyHost != null && !"".equals(proxyHost) && proxyPort != 0 )
            {
                System.setProperty("proxyHost", proxyHost);
                System.setProperty("proxyPort", String.valueOf(proxyPort));
                System.setProperty("proxySet", "true");

                System.setProperty("http.proxyHost", proxyHost);
                System.setProperty("http.proxyPort", String.valueOf(proxyPort));
                System.setProperty("http.proxySet", "true");

                System.setProperty("https.proxyHost", proxyHost);
                System.setProperty("https.proxyPort", String.valueOf(proxyPort));
                System.setProperty("https.proxySet", "true");

                if( proxyUser != null && !"".equals(proxyUser) )
                {
                    System.setProperty("proxyUserName", proxyUser);
                    System.setProperty("http.proxyUserName", proxyUser);
                    System.setProperty("https.proxyUserName", proxyUser);
                    if( proxyPassword != null && !"".equals(proxyPassword) )
                    {
                        System.setProperty("proxyPassword", proxyPassword);
                        System.setProperty("http.proxyPassword", proxyPassword);
                        System.setProperty("https.proxyPassword", proxyPassword);
                    }
                }
            }
            else
            {
                //clean proxy properties, because other applications can set incorrect default properties (like BE-based applications)
                System.getProperties().remove("proxyHost");
                System.getProperties().remove("proxyPort");
                System.setProperty("proxySet", "false");

                System.getProperties().remove("http.proxyHost");
                System.getProperties().remove("http.proxyPort");
                System.setProperty("http.proxySet", "false");

                System.getProperties().remove("https.proxyHost");
                System.getProperties().remove("https.proxyPort");
                System.setProperty("https.proxySet", "false");
            }
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Error occured while setting proxy: "+ExceptionRegistry.log(t));
        }
    }

    private static ChangeListener changeListener = new ChangeListener();
    protected static class ChangeListener implements PropertyChangeListener
    {
        @Override
        public void propertyChange(PropertyChangeEvent arg0)
        {
            //update proxy as far as it changed
            NetworkConfigurator.setProxy();
        }
    }

    private static void initHTTPS()
    {
        String handlers = System.getProperty( "java.protocol.handler.pkgs" );
        if( handlers == null )
        {
            // nothing specified yet (expected case)
            System.setProperty( "java.protocol.handler.pkgs", "javax.net.ssl" );
        }
        else
        {
            // something already there, put ourselves out front
            System.setProperty( "java.protocol.handler.pkgs", "javax.net.ssl|".concat( handlers ) );
        }
        HostnameVerifier hv = new HostnameVerifier()
        {
            @Override
            public boolean verify(String arg0, SSLSession arg1)
            {
                return true;
            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier( hv );
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
            SSLContext sc = SSLContext.getInstance( "SSL" );
            sc.init( null, trustAllCerts, new java.security.SecureRandom() );
            HttpsURLConnection.setDefaultSSLSocketFactory( sc.getSocketFactory() );
        }
        catch( Throwable e )
        {
            log.log( Level.WARNING, "setDefaultSSLSocketFactory", e );
        }
    }

    private static volatile boolean networkInitialized = false;

    public static void initNetworkConfiguration()
    {
        if(!networkInitialized)
        {
            synchronized(NetworkConfigurator.class)
            {
                if(!networkInitialized)
                {
                    setProxy();
                    initHTTPS();
                }
            }
        }
    }
}
