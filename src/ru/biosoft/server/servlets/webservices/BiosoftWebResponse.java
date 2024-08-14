package ru.biosoft.server.servlets.webservices;

import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class used to access HttpServletResponse which was loaded by another ClassLoader
 * @author lan
 */
public class BiosoftWebResponse
{
    private static final Logger log = Logger.getLogger( BiosoftWebResponse.class.getName() );

    private Object response;
    private OutputStream out;
    
    public BiosoftWebResponse(Object response, OutputStream out)
    {
        this.response = response;
        this.out = out;
    }
    
    public OutputStream getOutputStream()
    {
        return out;
    }
    
    public void setContentType(String contentType)
    {
        try
        {
            response.getClass().getMethod("setContentType", String.class).invoke(response, contentType);
        }
        catch( Exception e )
        {
        }
    }
    
    public void setHeader(String key, String value)
    {
        try
        {
            response.getClass().getMethod("setHeader", String.class, String.class).invoke(response, key, value);
        }
        catch( Exception e )
        {
        }
    }
    
    public void setCookies(List<CookieTemplate> cookieTemlates)
    {
        try
        {
            Class<?> cookieClass = response.getClass().getClassLoader().loadClass( "javax.servlet.http.Cookie" );
            Constructor<?> cookieConstructor = cookieClass.getConstructor( String.class, String.class );
            Method cookieSetPathMethod = getMethodFromClass( cookieClass, "setPath", String.class );
            Method cookieSetMaxAgeMethod = getMethodFromClass( cookieClass, "setMaxAge", int.class );
            Method cookieSetHttpOnly = getMethodFromClass( cookieClass, "setHttpOnly", boolean.class );
            Method addCookieMethod = response.getClass().getMethod( "addCookie", cookieClass );
            for( CookieTemplate template : cookieTemlates )
            {
                if( !template.isValid() )
                    continue;
                Object cookie = cookieConstructor.newInstance( template.name, template.value );
                if( cookieSetPathMethod != null )
                    cookieSetPathMethod.invoke( cookie, template.getPath() );
                if( cookieSetMaxAgeMethod != null )
                    cookieSetMaxAgeMethod.invoke( cookie, template.maxAge );
                if( cookieSetHttpOnly != null )
                    cookieSetHttpOnly.invoke( cookie, template.httpOnly );
                addCookieMethod.invoke( response, cookie );
            }
        }
        catch( Exception e )
        {
        }
    }

    public void setStatus( int status )
    {
        try
        {
            response.getClass().getMethod("setStatus", int.class).invoke(response, status);
        } catch (Exception e)
        {
        }
    }

    public void setContentLengthLong( long contentLength )
    {
        try
        {
            response.getClass().getMethod("setContentLengthLong", Long.class).invoke(response, contentLength);
        } catch (Exception e)
        {
        }

    }

    /**
     * @param classObj class to search for method in
     * @param methodName name of the requested method
     * @param parameterTypes signature parameters
     * @return requested <code>Method</code> or <code>null</code> if exception was thrown
     */
    private static Method getMethodFromClass(Class<?> classObj, String methodName, Class<?> ... parameterTypes)
    {
        try
        {
            return classObj.getMethod( methodName, parameterTypes );
        }
        catch( Exception e )
        {
            log.log( Level.SEVERE, constructMessage( classObj.getName(), methodName, parameterTypes ), e );
            return null;
        }
    }
    private static String constructMessage(String className, String methodName, Class<?> ... parameterTypes)
    {
        List<String> types = new ArrayList<>();
        for( Class<?> parameterType : parameterTypes )
            types.add( parameterType == null ? "" : parameterType.getName() );
        return "Can not find method " + methodName + "(" + String.join( ", ", types ) + ") in class " + className;
    }

    public void clearSession()
    {
        try
        {
            Class<?> cookieClass = response.getClass().getClassLoader().loadClass("javax.servlet.http.Cookie");
            Constructor<?> cookieConstructor = cookieClass.getConstructor(String.class, String.class);
            Method cookieSetPathMethod = cookieClass.getMethod("setPath", String.class);
            Method cookieSetMaxAgeMethod = cookieClass.getMethod( "setMaxAge", int.class );
            Method addCookieMethod = response.getClass().getMethod("addCookie", cookieClass);
            Object cookie = cookieConstructor.newInstance("JSESSIONID", "");
            cookieSetPathMethod.invoke(cookie, "/");
            cookieSetMaxAgeMethod.invoke(cookie, 0);
            addCookieMethod.invoke(response, cookie);
            cookie = cookieConstructor.newInstance("JSESSIONID", "");
            cookieSetPathMethod.invoke(cookie, "/biouml");
            cookieSetMaxAgeMethod.invoke(cookie, 0);
            addCookieMethod.invoke(response, cookie);
        }
        catch( Exception e )
        {
        }
    }

    public static class CookieTemplate
    {
        public final String name;
        public final String value;
        public final String path;
        public final int maxAge;
        public final boolean httpOnly;
        public CookieTemplate(String name, String value, String path, int maxAge, boolean httpOnly)
        {
            this.name = name;
            this.value = value;
            this.path = path;
            this.maxAge = maxAge;
            this.httpOnly = httpOnly;
        }
        public boolean isValid()
        {
            return name != null && value != null;
        }
        public String getPath()
        {
            return path == null ? "/" : path;
        }
    }


}
