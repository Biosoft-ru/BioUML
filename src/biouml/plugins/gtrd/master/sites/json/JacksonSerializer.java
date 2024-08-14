package biouml.plugins.gtrd.master.sites.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

public abstract class JacksonSerializer<T>
{
    //The parser already points to the first token, after read it should point to the last token consumed
    public abstract T read(JsonParser parser) throws IOException;
    public abstract void write(T obj, JsonGenerator jGenerator) throws IOException;
    

    protected JsonFactory jsonFactory = new JsonFactory();
    
    public String toJSON(T obj) throws IOException
    {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        JsonGenerator jGenerator = jsonFactory.createGenerator( stream, JsonEncoding.UTF8 );
        write(obj, jGenerator);
        jGenerator.close();
        return new String( stream.toByteArray(), "UTF-8" );
    }
    
    public String toPrettyJSON(T obj) throws IOException
    {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        JsonGenerator jGenerator = jsonFactory.createGenerator( stream, JsonEncoding.UTF8 );
        jGenerator.useDefaultPrettyPrinter();
        write(obj, jGenerator);
        jGenerator.close();
        return new String( stream.toByteArray(), "UTF-8" );
    }
    
    public T fromJSON(String json) throws IOException
    {
        JsonParser jp = jsonFactory.createParser(json);
        jp.nextToken();//move parser to the first token
        return read(jp);
    }
}
