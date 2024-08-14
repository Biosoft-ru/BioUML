package ru.biosoft.access;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.osgi.framework.Bundle;

/**
 * Wraps OSGi bundle to {@link ClassLoader} interface
 */
public class BundleDelegatingClassLoader extends ClassLoader
{
    private final ClassLoader bridge;

    private final Bundle backingBundle;

    public BundleDelegatingClassLoader(Bundle bundle, ClassLoader bridgeLoader)
    {
        super(null);
        this.backingBundle = bundle;
        this.bridge = bridgeLoader;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException
    {
        try
        {
            return this.backingBundle.loadClass(name);
        }
        catch( ClassNotFoundException cnfe )
        {
            throw new ClassNotFoundException(name + " not found from bundle [" + backingBundle.getSymbolicName() + "]", cnfe);
        }
        catch( NoClassDefFoundError ncdfe )
        {
            NoClassDefFoundError e = new NoClassDefFoundError(name + " not found from bundle [" + backingBundle + "]");
            e.initCause(ncdfe);
            throw e;
        }
    }

    @Override
    protected URL findResource(String name)
    {
        URL url = this.backingBundle.getResource(name);
        return url;
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException
    {
        return this.backingBundle.getResources(name);
    }

    @Override
    public URL getResource(String name)
    {
        URL resource = findResource(name);
        if( bridge != null && resource == null )
        {
            resource = bridge.getResource(name);
        }
        return resource;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
        Class<?> clazz = null;
        try
        {
            clazz = findClass(name);
        }
        catch( ClassNotFoundException cnfe )
        {
            if( bridge != null )
                clazz = bridge.loadClass(name);
            else
                throw cnfe;
        }
        if( resolve )
        {
            resolveClass(clazz);
        }
        return clazz;
    }

    @Override
    public String toString()
    {
        return "BundleDelegatingClassLoader for [" + backingBundle + "]";
    }
}
