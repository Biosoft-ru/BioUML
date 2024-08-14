package ru.biosoft.server.tomcat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DataElementServlet extends HttpServlet
{
    @Override
    public void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException
    {
        /*
        String path = request.getPathInfo();
        if( path.startsWith( "/" ) )
        {
            path = path.substring( 1 );
        }
        response.sendRedirect( "/bioumlweb/#de=" + path );
        */ 
        String path = request.getRequestURI();
        log( path );
        path = path.replace( "/anonde/", "/#anonymous=true&de=" );
        path = path.replace( "/de/", "/#de=" );
        response.sendRedirect( path );
    }
}
