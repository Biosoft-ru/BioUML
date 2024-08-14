package ru.biosoft.server.tomcat;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import ru.biosoft.server.Response;

/**
 * This class is loaded by Tomcat class loaded which has no access to other BioUML plugins.
 * Please do not introduce illegal dependencies
 */
@SuppressWarnings ( "serial" )
public abstract class ConnectionServlet extends HttpServlet
{
    private static final String REMOTE_ADDRESS = "Remote-address";
    private static final String RANGE = "Range";

    protected Logger log;

    protected Class<?> request;
    protected Class<?> services;
    private Map extensionServletMap;

    public ConnectionServlet()
    {

    }
    
    protected void postInit()
    {
        try
        {
            Class<?> registry = services.getClassLoader().loadClass( "ru.biosoft.server.ServletRegistry" );
            Method getRegistry = registry.getMethod( "getRegistry", new Class[] {} );
            extensionServletMap = (Map)getRegistry.invoke( registry, new Object[] {} );
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Cannot initialize extensionServletMap for " + getClass(), t);
            extensionServletMap = Collections.emptyMap();
        }
    }
    
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
    {
        executeQueryWithExtensionServlet(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
    {
        if( !executeQueryWithExtensionServlet(req, resp) )
        {
            try
            {
                Map<Object, Object[]> args = req.getParameterMap();
                Map<Object, Object> arguments = new HashMap<>();
                for( Map.Entry<Object, Object[]> entry : args.entrySet() )
                {
                    arguments.put(entry.getKey(), entry.getValue()[0]);
                }
                if( arguments.size() == 0 )
                {
                    stub(resp.getOutputStream());
                    return;
                }
                OutputStream os = resp.getOutputStream();
                if( isGZIPSupported(req) && !req.getRequestURI().endsWith( ".gz" ) )
                {
                    resp.setHeader("Content-Encoding", "gzip");
                    os = new GZIPOutputStream(os);
                }

                try
                {
                    processRequest(arguments, os, req.getRequestURI(), req.getRemoteAddr());
                }
                finally
                {
                    os.close();
                }
            }
            catch( Throwable e )
            {
                log.log(Level.SEVERE, "Get exception", e);
            }
        }
    }

    protected static final Pattern RE_ACCEPT_ENCODING = Pattern.compile("([^,;]+)(?:;\\s*q\\s*=\\s*(\\d(?:\\.\\d+)?)\\s*)?,?");

    /**
     * True if client supports gzip encoding.
     */
    protected static boolean isGZIPSupported(HttpServletRequest request)
    {
        String codings = request.getHeader("Accept-Encoding");
        if( codings == null )
            return false;

        // search for gzip in Accept-Encoding
        Matcher m = RE_ACCEPT_ENCODING.matcher(codings);
        while( m.find() )
        {
            String coding = m.group(1).trim();
            String quality = m.group(2);

            // client supports it if gzip specified, and qvalue not specified or greater than zero
            if( "gzip".equals(coding) && ( quality == null || quality.length() < 1 || Float.parseFloat(quality) > 0 ) )
                return true;
        }

        return false;
    }

    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp)
    {
        executeQueryWithExtensionServlet(req, resp);
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
    {
        executeQueryWithExtensionServlet(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
    {
        if( !executeQueryWithExtensionServlet(req, resp) )
        {
            try
            {
                Method m = request.getMethod("getArguments", new Class[] {InputStream.class});
                Object argumentsObj = m.invoke(null, new Object[] {req.getInputStream()});
                Map arguments = (Map)argumentsObj;
                try (ServletOutputStream os = resp.getOutputStream())
                {
                    processRequest(arguments, os, req.getRequestURI(), req.getRemoteAddr());
                }
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Post exception", t);
            }
        }
    }
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
    {
        executeQueryWithExtensionServlet(req, resp);
    }

    @Override
    protected void doTrace(HttpServletRequest req, HttpServletResponse resp)
    {
        executeQueryWithExtensionServlet(req, resp);
    }

    protected void stub(OutputStream os)
    {
        try
        {
            os.write( ( "<html><body>Internal BioUML application server.</body></html>" ).getBytes(StandardCharsets.ISO_8859_1));
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Stub write exception", t);
        }
    }

    protected void processRequest(Map arguments, OutputStream os, String url, String remoteAddr)
    {
        try
        {
            Method m = request.getMethod("getService", new Class[] {Map.class});
            Object serviceName = m.invoke(null, new Object[] {arguments});
            m = services.getMethod("getService", new Class[] {String.class});
            Object service = m.invoke(null, new Object[] {serviceName});
            m = request.getMethod("getCommand", new Class[] {Map.class});
            Object command = m.invoke(null, new Object[] {arguments});

            if( service != null && command != null )
            {
                long startTime = System.currentTimeMillis();

                Class<?> responseClass = service.getClass().getClassLoader().loadClass(Response.class.getName());
                Constructor<?> responseConstructor = responseClass.getConstructor(new Class[] {OutputStream.class, String.class});
                Object response = responseConstructor.newInstance(new Object[] {os, url});
                m = service.getClass().getMethod("processRequest", new Class[] {Integer.class, Map.class, responseClass});
                m.invoke(service, new Object[] {command, arguments, response});

                //code for printing statistic
                //long endTime = System.currentTimeMillis();
                //log.info(remoteAddr + " \t" + service.getClass().getName() + ":" + command + " \ttime=" + ( endTime - startTime ));
                ////////////////////////////////////////
            }
            else
            {
                log.log(Level.SEVERE, "Unknown service: " + serviceName + "->" + command);
                Response response = new Response(os, url);
                response.error("Unknown service: " + serviceName + "->" + command);
            }
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Unknown request processing error", t);
            try
            {
                Response response = new Response(os, url);
                response.error("unknown error: " + t);
            }
            catch( Throwable tWrite )
            {
                log.log(Level.SEVERE, "Error wrting to ouput", tWrite);
            }
        }
    }

    protected Map<String, Object> getParameterMap(final HttpServletRequest req, final Object servlet)
    {
        Map<?, ?> urlParams = req.getParameterMap();
        final Map<String, Object> params = new HashMap<>();
        for( Entry<?, ?> entry : urlParams.entrySet() )
        {
            params.put(entry.getKey().toString(), entry.getValue());
        }
        params.put(REMOTE_ADDRESS, new String[] {req.getRemoteAddr()});
        if( req.getHeader( "X-Forwarded-For" ) != null )
        {
            params.put( "X-Forwarded-For", new String[] {req.getHeader( "X-Forwarded-For" )} );
        }
        if (req.getHeader(RANGE) != null)
            params.put(RANGE, new String[] { req.getHeader(RANGE) });
        if( ServletFileUpload.isMultipartContent(req) )
        {
            try
            {
                // Create a factory for disk-based file items
                FileItemFactory factory = new DiskFileItemFactory();

                // Create a new file upload handler
                ServletFileUpload upload = new ServletFileUpload(factory);

                try
                {
                    upload.setProgressListener(new UploadProgressListener(servlet, req, params));
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE,  "While doing setProgressListener", e );
                }

                // Parse the request
                @SuppressWarnings ( "unchecked" )
                List<FileItem> items = upload.parseRequest(req);
                // Process the uploaded items
                for(FileItem item : items)
                {
                    if( item.isFormField() )
                    {
                        String name = item.getFieldName();
                        String value = item.getString();
                        if( params.get(name) == null )
                        {
                            params.put(name, new String[] {value});
                        }
                        else
                        { // Duplicate parameter name: ignore

                        }
                    }
                    else
                    {
                        String name = item.getFieldName();
                        if( params.get(name) == null )
                        {
                            params.put(name, new FileItem[] {item});
                        }
                        //processUploadedFile(item);
                    }
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE,  "File upload error", e );
            }
        }
        return params;
    }

    protected boolean executeQueryWithExtensionServlet(HttpServletRequest req, HttpServletResponse resp)
    {
        try
        {
            req.setCharacterEncoding( "UTF-8" );
        }
        catch( UnsupportedEncodingException e )
        {
            log.log(Level.WARNING, "", e );
        }
        String path = req.getServletPath();
        if( path.length() == 0 || path.equals("/") )
        {
            return false;
        }

        String[] query = path.split( "/" );
        String extensionPrefix = query[1];

        try
        {
            Object servlet = extensionServletMap.get(extensionPrefix);
            if(servlet == null)
                return false;
            try
            {
                Method postService = servlet.getClass().getMethod( "service", new Class[] {InputStream.class, OutputStream.class} );
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                String error = (String)postService.invoke(servlet, req.getInputStream(), result);
                try (OutputStream os = resp.getOutputStream())
                {
                    resp.setContentType( "text/xml;charset=UTF-8" );
                    if( error != null )
                    {
                        resp.setStatus( 500 );
                        try (PrintWriter printWriter = new PrintWriter( new OutputStreamWriter( os, StandardCharsets.UTF_8 ) ))
                        {
                            printWriter.print( "<body><h1>Error</h1><p>" + error + "</p></body>" );
                        }
                    }
                    else
                    {
                        os.write( result.toByteArray() );
                    }
                }
                return true;
            }
            catch(NoSuchMethodException e)
            {
            }
            catch(Exception e)
            {
                if(!e.getClass().getSimpleName().equals("ClientAbortException"))
                    log.log(Level.SEVERE, "Extension servlet failed: " + extensionPrefix, e);
                return true;
            }
            try
            {
                Method service = servlet.getClass().getMethod("service",
                        new Class[] {String.class, Object.class, Map.class, OutputStream.class, Object.class});

                OutputStream os = resp.getOutputStream();
                if( isGZIPSupported(req)  && !req.getRequestURI().endsWith( ".gz" ))
                {
                    resp.setHeader("Content-Encoding", "gzip");
                    os = new GZIPOutputStream(os);
                }
                try
                {
                    service.invoke(servlet, new Object[] {path, req.getSession(), getParameterMap(req, servlet), os, resp});
                }
                finally
                {
                    os.close();
                }
                return true;
            }
            catch(NoSuchMethodException e)
            {
                Method service = servlet.getClass().getMethod("service",
                        new Class[] {String.class, Object.class, Map.class, OutputStream.class, Map.class});

                // TODO: remove extra buffer (will need to write all headers before)
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                Map<String, String> header = new HashMap<>();
                String contentType = (String)service.invoke(servlet, new Object[] {path, req.getSession(), getParameterMap(req, servlet),
                        result, header});

                if(contentType.equals("logout"))
                {
                    resp.setContentType("text/html");
                    Cookie cookie = new Cookie("JSESSIONID", "");
                    cookie.setPath("/biouml");
                    cookie.setMaxAge(0);
                    resp.addCookie(cookie);
                    cookie = new Cookie("JSESSIONID", "");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    resp.addCookie(cookie);
                } else
                {
                    resp.setContentType(contentType);
                }
                for( Map.Entry<String, String> entry : header.entrySet() )
                {
                    resp.setHeader(entry.getKey(), entry.getValue());
                }
                OutputStream os = resp.getOutputStream();
                if( isGZIPSupported(req) && !req.getRequestURI().endsWith( ".gz" ))
                {
                    resp.setHeader("Content-Encoding", "gzip");
                    os = new GZIPOutputStream(os);
                }
                try
                {
                    os.write(result.toByteArray());
                }
                finally
                {
                    os.close();
                }
                return true;
            }
        }
        catch( Exception e )
        {
            if(!e.getClass().getSimpleName().equals("ClientAbortException"))
                log.log(Level.SEVERE, "Extension servlet failed: " + extensionPrefix, e);
        }
        return true;
    }
}
