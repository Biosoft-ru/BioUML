package biouml.plugins.gtrd.master.meta.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

import biouml.plugins.gtrd.ExternalReference;
import biouml.plugins.gtrd.master.sites.json.JacksonObjectSerializer;

public class ExternalRefSerializer extends JacksonObjectSerializer<ExternalReference>
{
    public static final String FIELD_EXTERNAL_DB = "db";
    public static final String FIELD_EXTERNAL_ID = "id";

    @Override
    public ExternalReference read(JsonParser parser) throws IOException
    {
        result = new ExternalReference();
        return super.read( parser );
    }
    
    @Override
    protected void readField(JsonParser parser) throws IOException
    {
        String name = parser.getCurrentName();
        switch(name)
        {
            case FIELD_EXTERNAL_DB:
                result.setExternalDB( parser.getText() );
                break;
            case FIELD_EXTERNAL_ID:
                result.setId( parser.getText() );
                break;
            default:
                throw new JsonParseException( parser, "Unexpected field: " + name );
        }
    }

    @Override
    protected void writeFields(ExternalReference ref, JsonGenerator jGenerator) throws IOException
    {
        jGenerator.writeStringField( FIELD_EXTERNAL_DB, ref.getExternalDB() );
        jGenerator.writeStringField( FIELD_EXTERNAL_ID, ref.getId() );        
    }


}
