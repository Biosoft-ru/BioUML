package ru.biosoft.server;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.CollectionFactory;

/**
 * Abstract implementation for {@link ServletExtension}
 */
public abstract class AbstractServlet implements ServletExtension
{
    protected static final Logger log = Logger.getLogger( AbstractServlet.class.getName() );

    protected Map<String, String> convertParams(Map<?, ?> params)
    {
        Map<String, String> result = new HashMap<>();
        for(Entry<?, ?> entry: params.entrySet())
        {
            result.put(entry.getKey().toString(), Array.get(entry.getValue(), 0).toString());
        }
        return result;
    }

    public void uploadListener(Map<Object, Object> arguments, Object sessionObj, Long read, Long total)
    {
    }

    @Override
    public void init(String[] args) throws Exception
    {
        for( String arg : args )
        {
            try
            {
                CollectionFactory.createRepository(arg);
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Can not initialise repository, path=" + arg, t);
            }
        }
    }
}
