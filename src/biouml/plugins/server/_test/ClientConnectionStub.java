package biouml.plugins.server._test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import biouml.plugins.server.access.AccessService;
import ru.biosoft.server.ClientConnection;
import ru.biosoft.server.Request;
import ru.biosoft.server.Response;
import ru.biosoft.server.Service;

/**
 * Stub class for {@link ClientConnection} for testing.
 * Calls {@link Service} directly
 */
public class ClientConnectionStub extends ClientConnection
{
    protected ByteArrayOutputStream outputStream;

    public ClientConnectionStub(String url)
    {
        this(url, 0);
    }

    public ClientConnectionStub(String host, Integer port)
    {
        super(host, port);
    }

    @Override
    public InputStream getIs() throws IOException
    {
        //read parameters
        if( outputStream == null )
        {
            throw new IOException("Parameters stream is null");
        }
        Map<String, Object> arguments = null;
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
        try
        {
            Object argumentsObj = ois.readObject();
            if( argumentsObj instanceof Map )
            {
                arguments = (Map)argumentsObj;
            }
        }
        catch( Exception e )
        {
            throw new IOException(e.getMessage());
        }
        outputStream = null;
        if( arguments == null )
        {
            throw new IOException("Parameters not defined");
        }

        //process service request
        Service service = getService(arguments);
        Integer command = Request.getCommand(arguments);
        if( service == null || command == null )
        {
            throw new IOException("Service not found");
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        service.processRequest(command, arguments, new Response(baos, "localhhost"));
        return new ByteArrayInputStream(baos.toByteArray());
    }

    @Override
    protected OutputStream getOs() throws IOException
    {
        if( outputStream != null )
        {
            throw new IOException("Server busy");
        }
        outputStream = new ByteArrayOutputStream();
        return outputStream;
    }

    @Override
    public synchronized String toString()
    {
        return "Stub connection (for test only)";
    }

    protected Map<String, Service> serviceMap = new HashMap<>();

    protected Service getService(Map<String, Object> arguments)
    {
        String name = Request.getService(arguments);
        Service result = serviceMap.get(name);
        if( result == null )
        {
            result = initService(name);
            if( result != null )
            {
                serviceMap.put(name, result);
            }
        }
        return result;
    }

    protected Service initService(String name)
    {
        Service result = null;
        try
        {
            if( name.equals("access.service") )
            {
                result = new AccessService();
            }
            //TODO: support other services
        }
        catch( Exception e )
        {
        }
        return result;
    }
}
