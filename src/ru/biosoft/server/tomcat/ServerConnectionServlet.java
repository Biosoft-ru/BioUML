package ru.biosoft.server.tomcat;

import javax.servlet.ServletException;

import ru.biosoft.server.Request;
import ru.biosoft.server.ServiceRegistry;

public class ServerConnectionServlet extends ConnectionServlet
{
    public ServerConnectionServlet ( )
    {
    }
    
    @Override
    public void init ( ) throws ServletException
    {
        super.init ( );
        request = Request.class;
        services = ServiceRegistry.class;
        postInit();
    }
}