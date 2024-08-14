package biouml.workbench;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Test connection to web site
 */
public class ProxyTester
{
    /**
     * Test connection using HTTP protocol
     * 
     * @param host server address(for example, "http://biosoft.ru")
     * @param timeout timeout in milliseconds
     */
    public static long ping(String host, int timeout)
    {
        URLConnection conn = null;
        try
        {
            long start = System.currentTimeMillis();
            conn = new URL(host).openConnection();
            conn.setUseCaches(false);
            conn.setConnectTimeout(timeout);
            try(InputStream is = conn.getInputStream())
            {
                is.read(new byte[1]);
            }
            long end = System.currentTimeMillis();
            return end - start;
        }
        catch( Exception e )
        {
            return -1;
        }
    }

    /**
     * Check connection in separate thread
     * 
     * @param host server address
     * @param timeout timeout in milliseconds
     */
    public static void testServer(final String host, final int timeout, final TestListener listener)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                listener.testFinished(ping(host, timeout));
            }
        }).start();
    }
    /**
     * Callback interface
     */
    public static interface TestListener
    {
        public void testFinished(long result);
    }
}