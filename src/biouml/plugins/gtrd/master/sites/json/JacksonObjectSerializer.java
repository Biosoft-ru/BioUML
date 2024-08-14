package biouml.plugins.gtrd.master.sites.json;

import java.io.EOFException;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public abstract class JacksonObjectSerializer<T> extends JacksonSerializer<T>
{
    protected T result;
    
    public T read(JsonParser parser) throws IOException
    {
        JsonToken t = parser.getCurrentToken();
        if(t == null)
            throw new EOFException();
        if(t != JsonToken.START_OBJECT)
            throw new JsonParseException( parser, "should starts with {" );
        
        while((t = parser.nextToken()) == JsonToken.FIELD_NAME)
        {
            parser.nextToken();
            readField(parser);
        }
        
        t = parser.currentToken();
        if(t != JsonToken.END_OBJECT)
            throw new JsonParseException(parser, "expecting }");
        return result;
    }
    
    //should not move parser
    protected abstract void readField(JsonParser parser) throws IOException;
    
    public void write(T obj, JsonGenerator jGenerator) throws IOException
    {
        jGenerator.writeStartObject();
        writeFields( obj, jGenerator );
        jGenerator.writeEndObject();
    }
    
    protected abstract void writeFields(T obj, JsonGenerator jGenerator) throws IOException;
}
