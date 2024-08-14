package ru.biosoft.server.tomcat;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class CacheControlFilter implements Filter
{

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        HttpServletResponse resp = (HttpServletResponse)response;
        resp.setHeader( "Cache-Control", "no-cache" );
        chain.doFilter( request, response );
    }

    @Override
    public void destroy()
    {
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException
    {
    }

}