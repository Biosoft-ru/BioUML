package biouml.plugins.gtrd.master.sites.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

public class MapOfStringSetSerializer extends JacksonObjectSerializer<Map<String, Set<String>>>
{

    private ListSerializer<String> strListSerializer = new ListSerializer<>( new StringSerializer() );
    
    @Override
    public Map<String, Set<String>> read(JsonParser parser) throws IOException
    {
        result = new HashMap<>();
        return super.read( parser );
    }
    
    @Override
    protected void readField(JsonParser parser) throws IOException
    {
        String key = parser.getCurrentName();
        strListSerializer.setReadTarget( new ArrayList<>() );
        List<String> list = strListSerializer.read( parser );
        result.put(key, new HashSet<>(list));
        
    }

    @Override
    protected void writeFields(Map<String, Set<String>> data, JsonGenerator jGenerator) throws IOException
    {
        for( String peakCaller : data.keySet() )
        {
            Set<String> idSet = data.get( peakCaller );
            if( idSet.isEmpty() )
                continue;
            jGenerator.writeFieldName( peakCaller );
            jGenerator.writeStartArray( idSet.size() );
            for( String id : idSet )
                jGenerator.writeString( id );
            jGenerator.writeEndArray();
        }
    }

}
