package biouml.plugins.gtrd.master.sites.json;

import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

public class CachedSerializer<T> extends JacksonSerializer<T>
{
    private JacksonSerializer<T> primary;
    
    private Map<T, T> cache = new WeakHashMap<>();
    
    public CachedSerializer(JacksonSerializer<T> primary)
    {
        this.primary = primary;
    }
    
    public T read(JsonParser parser) throws IOException
    {
        T val = primary.read( parser );
        T cached = cache.putIfAbsent( val, val );
        return cached == null ? val : cached;
    }
    
    public void write(T obj, JsonGenerator jGenerator) throws IOException
    {
        primary.write( obj, jGenerator );
    }
}
