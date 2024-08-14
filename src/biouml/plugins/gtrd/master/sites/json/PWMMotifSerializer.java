package biouml.plugins.gtrd.master.sites.json;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

import biouml.plugins.gtrd.master.sites.PWMMotif;
import biouml.plugins.gtrd.master.utils.StringPool;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.bsa.track.big.BigBedTrack;

public class PWMMotifSerializer extends JacksonObjectSerializer<PWMMotif>
{
    private static final String FIELD_ID = "id";
    private static final String FIELD_POS = "pos";
    private static final String FIELD_SITE_MODEL = "siteModel";
    private static final String FIELD_SCORE = "score";
    
    private BigBedTrack<?> origin;
    private Map<String, DataElementPath> siteModels = new HashMap<>();
    public PWMMotifSerializer(BigBedTrack<?> origin, DataElementPathSet siteModels)
    {
        this.origin = origin;
        for(DataElementPath path : siteModels)
            this.siteModels.put(path.getName(), path);
    }
    
    @Override
    public PWMMotif read(JsonParser parser) throws IOException
    {
        result = new PWMMotif();
        result.setOrigin( origin );
        return super.read( parser );
    }

    @Override
    protected void readField(JsonParser parser) throws IOException
    {
        String name = parser.getCurrentName();
        switch(name)
        {
            case FIELD_ID:
                result.setId( parser.getIntValue() );
                break;
            case FIELD_POS:
                parsePos(parser);
                break;
            case FIELD_SITE_MODEL:
                parseSiteModel( parser );
                break;
            case FIELD_SCORE:
                result.setScore( (float)parser.getValueAsDouble() );
                break;
            default:
                throw new JsonParseException( parser, "Unexpected field: " + name );
        }
    }

    public void parseSiteModel(JsonParser parser) throws IOException
    {
        String modelName = parser.getValueAsString();
        DataElementPath modelPath = siteModels.get( modelName );
        if(modelPath == null)
            modelPath = DataElementPath.create( modelName );
        result.setSiteModelPath( modelPath );
    }

    @Override
    protected void writeFields(PWMMotif obj, JsonGenerator jGenerator) throws IOException
    {
        jGenerator.writeNumberField( FIELD_ID, obj.getId() );
        jGenerator.writeStringField( FIELD_POS, obj.getChr() + ":" + (obj.getFrom()-1) + "-" + obj.getTo() + ":" + (obj.isForwardStrand()?"+":"-") );
        jGenerator.writeStringField( FIELD_SITE_MODEL, obj.getSiteModelPath().getName() );
        jGenerator.writeNumberField( FIELD_SCORE, obj.getScore() );
    }

    public void parsePos(JsonParser parser) throws IOException
    {
        String text = parser.getValueAsString();
        int colonIdx = text.indexOf( ':' );
        if(colonIdx == -1)
            throw new JsonParseException( parser, "wrong genome position: " + text );
        result.setChr( StringPool.get(text.substring( 0, colonIdx )) );
        
        int dashIndex = text.indexOf( '-', colonIdx + 1 );
        if(dashIndex == -1)
            throw new JsonParseException( parser, "wrong genome position: " + text );
        result.setFrom( 1 + Integer.parseInt( text.substring( colonIdx+1, dashIndex ) ) );
        
        int colon2Idx = text.indexOf( ':', dashIndex + 1 );
        if(colon2Idx == -1)
            throw new JsonParseException( parser, "wrong genome position: " + text );
        result.setTo( Integer.parseInt( text.substring( dashIndex + 1, colon2Idx ) ) );
        
        if(colon2Idx+1 >= text.length())
            throw new JsonParseException( parser, "wrong genome position: " + text );
        result.setForwardStrand( '+' == text.charAt( colon2Idx + 1 ) );
    }
}
