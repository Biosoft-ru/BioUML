package biouml.plugins.gtrd.master.sites.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

import biouml.plugins.gtrd.master.sites.HistoryEntry;

public class HistoryEntrySerializer extends JacksonObjectSerializer<HistoryEntry>
{
    private static final String FIELD_FROM = "from";
    private static final String FIELD_TO = "to";
    private static final String FIELD_VERSION = "version";
    private static final String FIELD_RELEASE = "release";
    
    @Override
    public HistoryEntry read(JsonParser parser) throws IOException
    {
        result = new HistoryEntry();
        return super.read( parser );
    }

    @Override
    protected void readField(JsonParser parser) throws IOException
    {
        String name = parser.getCurrentName();
        switch(name)
        {
            case FIELD_FROM:
                result.setFrom( parser.getIntValue() );
                break;
            case FIELD_TO:
                result.setTo( parser.getIntValue() );
                break;
            case FIELD_VERSION:
                result.setVersion( parser.getIntValue() );
                break;
            case FIELD_RELEASE:
                result.setRelease( parser.getIntValue() );
                break;
            default:
                throw new JsonParseException( parser, "Unexpected field: " + name );
        }
    }

    @Override
    protected void writeFields(HistoryEntry obj, JsonGenerator jGenerator) throws IOException
    {
        jGenerator.writeNumberField( FIELD_VERSION, obj.getVersion() );
        jGenerator.writeNumberField( FIELD_RELEASE, obj.getVersion() );
        jGenerator.writeNumberField( FIELD_FROM, obj.getFrom() );
        jGenerator.writeNumberField( FIELD_TO, obj.getTo() );
    }
}
