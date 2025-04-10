package ru.biosoft.server.servlets.webservices._test;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Random;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import ru.biosoft.access.core.TextDataElement;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.exception.BiosoftNetworkException;
import ru.biosoft.server.ServerConnector;
import ru.biosoft.server.servlets.webservices.HttpServer;
import ru.biosoft.util.JsonUtils;

/**
 * @author lan
 *
 */
public class TestHTTPServer extends AbstractBioUMLTest
{
    private HttpServer httpServer;

    public void testHTTPServer() throws Exception
    {
        VectorDataCollection<DataElement> vdc = new VectorDataCollection<>("test");
        String content = "Example text";
        vdc.put(new TextDataElement("textDocument", vdc, content));
        CollectionFactory.registerRoot(vdc);
        String sessionId = "testSession";
        String url = startServer(sessionId);
        ServerConnector connector = new ServerConnector(url+"wrong_url", "wrong session");
        boolean catched = false;
        try
        {
            connector.getContent(DataElementPath.create("test/textDocument"));
        }
        catch( BiosoftNetworkException e )
        {
            assertTrue(e.getCause().getMessage().contains("403"));
            catched = true;
        }
        assertTrue(catched);
        connector = new ServerConnector(url+"wrong_url", sessionId);
        catched = false;
        try
        {
            connector.getContent(DataElementPath.create("test/textDocument"));
        }
        catch( BiosoftNetworkException e )
        {
            assertTrue(e.getCause() instanceof FileNotFoundException);
            catched = true;
        }
        assertTrue(catched);
        connector = new ServerConnector(url, sessionId);
        String validContent = connector.getContent(DataElementPath.create("test/textDocument"));
        assertEquals(content, validContent);
        
        JsonObject json = connector.queryJSON("web/export", Collections.singletonMap("action", "list"));
        JsonArray jsonArray = json.get("values").asArray();
        assertFalse(jsonArray.isEmpty());
        assertTrue( jsonArray.toString(), JsonUtils.arrayOfStrings( jsonArray ).anyMatch( "Generic file"::equals ) );
        
        stopServer();
    }

    private void stopServer()
    {
        httpServer.stopServer();
    }

    private String startServer(String sessionId) throws Exception
    {
        Random random = new Random();
        int minPort = 20000;
        int maxPort = 25000;
        int port;
        int tryNum = 0;
        while(true)
        {
            try
            {
                port = random.nextInt(maxPort-minPort)+minPort;
                httpServer = new HttpServer(port, sessionId);
                httpServer.startServer();
                break;
            }
            catch( Exception e )
            {
                if(tryNum++ > 100)
                {
                    throw new Exception("Unable to start server");
                }
            }
        }
        return "http://localhost:"+port+"/biouml";
    }
}
