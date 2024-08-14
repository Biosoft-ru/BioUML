package biouml.plugins.gtrd.master.sites.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class ListSerializer<T> extends JacksonSerializer<List<T>>
{
    private List<T> result = new ArrayList<>();
    private JacksonSerializer<T> elemSerializer;
    public ListSerializer(JacksonSerializer<T> elemSerializer)
    {
        this.elemSerializer = elemSerializer;
    }
    
    public void setReadTarget(List<T> target)
    {
        result = target;
    }

    @Override
    public List<T> read(JsonParser parser) throws IOException
    {
        if(parser.getCurrentToken() != JsonToken.START_ARRAY)
            throw new JsonParseException( parser, "Expecting [" );
        
        while(true)
        {
            JsonToken t = parser.nextToken();
            if(t == JsonToken.END_ARRAY)
                break;
            if(t == null)
                throw new JsonParseException(parser, "Expecting ]");
            T elem = elemSerializer.read( parser );
            result.add( elem );
        }
        return result;
    }

    @Override
    public void write(List<T> list, JsonGenerator jGenerator) throws IOException
    {
        jGenerator.writeStartArray();
        for(T elem : list)
            elemSerializer.write( elem, jGenerator );
        jGenerator.writeEndArray();
    }

}
