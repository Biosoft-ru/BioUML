package ru.biosoft.access;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.SessionCache;
import ru.biosoft.util.ObjectExtensionRegistry;

/**
 * This registry allows to get specific bean by path. Replacement to some code in
 * ru.biosoft.server.servlets.webservices.providers.WebBeanProvider.getBean(String, boolean)
 * @author lan
 */
public class BeanRegistry
{
    private static final ObjectExtensionRegistry<BeanProvider> instance = new ObjectExtensionRegistry<>( "ru.biosoft.access.beans",
            "prefix", BeanProvider.class );
    
    public static <T extends BeanProvider> T getProvider(Class<T> clazz)
    {
        return instance.stream().select( clazz ).findAny().orElse( null );
    }
    
    static class SearchResult
    {
        BeanProvider provider;
        String providerPath;
        String remainingPath;
    }
    
    public static SearchResult findProvider(String path)
    {
        int pos = path.length();
        while(pos > 0)
        {
            String subPath = path.substring( 0, pos );
            BeanProvider provider = instance.getExtension( subPath );
            if(provider != null)
            {
                SearchResult sr = new SearchResult();
                sr.provider = provider;
                sr.providerPath = subPath;
                sr.remainingPath = pos >= path.length() ? "" : path.substring( pos + 1, path.length() );
                return sr;
            }
            pos = path.lastIndexOf( '/', pos-1 );
        }
        return null;
    }
    
    public static Object getBean(Class<? extends BeanProvider> clazz, String subPath) throws Exception
    {
        BeanProvider provider = instance.stream().select( clazz ).findAny().orElse( null );
        if(provider == null)
            return null;
        return provider.getBean( subPath );
    }
    
    private static Object getBean(BeanProvider provider, String subPath, String path, SessionCache cache)
    {
        Object bean = provider.getBean( subPath );
        if(bean != null && cache != null && provider instanceof CacheableBeanProvider)
        {
            cache.addObject( path, bean, true );
        }
        return bean;
    }
    
    public static Object getBean(String path, SessionCache cache)
    {
        //System.out.println( "getBean 1: " + path );
        Object bean = null;
        bean = CollectionFactory.getDataElement(path);
        if(bean != null)
        { 
            //System.out.println( "retBean 1: " + bean );
            return bean;
        }
        //System.out.println( "getBean 2: " + path );
        if(cache != null)
        {
            Object cachedObject = cache.getObject( path );
            if(cachedObject != null)
                return cachedObject;
        }
        BeanProvider provider = instance.getExtension(path);
        //System.out.println( "getBean 3: " + path );
        if(provider != null) return getBean(provider, null, path, cache);
        int pos = 0;
        //System.out.println( "getBean 4: " + path );
        while(true)
        {
            pos = path.indexOf('/', pos);
            if(pos == -1) break;
            String subPath = path.substring(0, pos);
            //System.out.println( "subPath: " + subPath );
            provider = instance.getExtension(subPath);
            if(provider != null)
            {
                //System.out.println( "provider: " + provider );
                return getBean(provider, path.substring(pos+1), path, cache);
            }
            pos++;
        }
        return null;
    }

    public static void saveBean(String path, Object bean, SessionCache cache) throws Exception
    {
        if( DataElementPath.create( path ).exists() && bean instanceof DataElement)
        {
            DataElement de = ((DataElement)bean);
            DataElementPath dePath = de.getCompletePath();
            if(dePath.equals( DataElementPath.create( path ) ) && de.getOrigin() != null)
                dePath.save( de );
        }
        else
        {
            SearchResult sr = findProvider( path );
            if(sr != null)
                sr.provider.saveBean( sr.remainingPath, bean );
        }
        
        if(cache != null)
            cache.addObject( path, bean, true );
    }
}
