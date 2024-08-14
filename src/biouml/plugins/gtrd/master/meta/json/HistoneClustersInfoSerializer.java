package biouml.plugins.gtrd.master.meta.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

import biouml.plugins.gtrd.master.meta.BuildInfo.HistoneClustersInfo;
import biouml.plugins.gtrd.master.sites.json.JacksonObjectSerializer;

public class HistoneClustersInfoSerializer extends JacksonObjectSerializer<HistoneClustersInfo>
{

    public static final String FIELD_MODIFICATION = "modification";
    public static final String FIELD_PEAK_CALLER = "peakCaller";
    public static final String FIELD_CELL_ID = "cellId";
    public static final String FIELD_VERSION = "version";
    
    
    @Override
    public HistoneClustersInfo read(JsonParser parser) throws IOException
    {
        result = new HistoneClustersInfo( );
        return super.read( parser );
    }
    
    @Override
    protected void readField(JsonParser parser) throws IOException
    {
        String name = parser.getCurrentName();
        switch(name)
        {
            case FIELD_MODIFICATION:
                result.modification = parser.getText();
                break;
            case FIELD_PEAK_CALLER:
                result.peakCaller = parser.getText();
                break;
            case FIELD_CELL_ID:
                result.cellId = parser.getText();
                break;
            case FIELD_VERSION:
                result.version = parser.getIntValue();
                break;
            default:
                throw new JsonParseException( parser, "Unexpected field: " + name );
        }
    }

    @Override
    protected void writeFields(HistoneClustersInfo ci, JsonGenerator jGenerator) throws IOException
    {
        jGenerator.writeStringField( FIELD_MODIFICATION, ci.modification );
        jGenerator.writeStringField( FIELD_PEAK_CALLER, ci.peakCaller );
        jGenerator.writeStringField( FIELD_CELL_ID, ci.cellId );
        jGenerator.writeNumberField( FIELD_VERSION, ci.version );
    }

}
