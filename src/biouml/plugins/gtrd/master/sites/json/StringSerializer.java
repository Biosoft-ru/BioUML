package biouml.plugins.gtrd.master.sites.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

public class StringSerializer extends JacksonSerializer<String>
{

    @Override
    public String read(JsonParser parser) throws IOException
    {
        return parser.getText();
    }

    @Override
    public void write(String str, JsonGenerator jGenerator) throws IOException
    {
        jGenerator.writeString( str );
    }

}
