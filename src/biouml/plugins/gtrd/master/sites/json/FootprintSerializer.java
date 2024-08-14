package biouml.plugins.gtrd.master.sites.json;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

import biouml.plugins.gtrd.DNaseExperiment;
import biouml.plugins.gtrd.master.sites.dnase.DNaseFootprint;
import biouml.plugins.gtrd.master.sites.dnase.WellingtonFootprint;
import biouml.plugins.gtrd.master.sites.dnase.WellingtonHotspot2Footprint;
import biouml.plugins.gtrd.master.sites.dnase.WellingtonMACS2Footprint;
import ru.biosoft.bsa.track.big.BigBedTrack;
import ru.biosoft.util.TextUtil;

public class FootprintSerializer extends GenomeLocationSerializer<DNaseFootprint>
{
    public static final String FIELD_ID = "id";
    
    protected BigBedTrack<?> origin;
    protected Map<String, DNaseExperiment> exps; 
    public FootprintSerializer(BigBedTrack<?> origin, Map<String, DNaseExperiment> exps)
    {
        this.origin = origin;
        this.exps = exps;
    }
    
    @Override
    public void writeFields(DNaseFootprint peak, JsonGenerator jGenerator) throws IOException
    {
        super.writeFields( peak, jGenerator );//write genome location
        //write other peak properties
        if(peak instanceof WellingtonFootprint)
            writeWellington( (WellingtonFootprint)peak, jGenerator );
        else
            throw new IllegalArgumentException("Unknown peak type: " + peak.getClass());
    }
    
    @Override
    public DNaseFootprint read(JsonParser parser) throws IOException
    {
        result = null;
        return super.read( parser );
    }
    
    @Override
    public void readField(JsonParser parser) throws IOException
    {
        if(result == null)
            parseId( parser );
        else
            parseWellington( parser );
    }
    
    protected void parseId(JsonParser parser) throws IOException
    {
        String name = parser.getCurrentName();
        if(!name.equals( FIELD_ID ))
            throw new JsonParseException(parser, "id should be the first field");

        String idString = parser.getValueAsString();//p.DEXP003098_1.wellington_macs2.10719
        String[] parts = TextUtil.split(idString, '.');
        
        String peakCaller = parts[2];
        switch(peakCaller)
        {
            case WellingtonMACS2Footprint.PEAK_CALLER: result = new WellingtonMACS2Footprint();break;
            case WellingtonHotspot2Footprint.PEAK_CALLER: result = new WellingtonHotspot2Footprint(); break;
            default:
                throw new JsonParseException(parser, "Unknown peak caller: " + peakCaller);
        }
        
        result.setId( Integer.parseInt( parts[3] ) );
        
        String[] parts2 = TextUtil.split(parts[1], '_');
        String expId = parts2[0];
        result.setExp( exps.get( expId ) );
        
        result.setReplicate( Integer.parseInt( parts2[1] ) );
    }
    
    private static final String FIELD_WELLINGTON_SCORE = "score";
    private void parseWellington(JsonParser parser) throws IOException
    {
        String name = parser.getCurrentName();
        WellingtonFootprint peak = (WellingtonFootprint)result;
        switch(name)
        {
            case FIELD_POS: parsePos(peak, parser); break;
            case FIELD_WELLINGTON_SCORE: peak.setWellingtonScore(  (float)parser.getValueAsDouble() ); break;
            default:
                throw new JsonParseException( parser, "Unexpected field: " + name );
        }
    }
    private void writeWellington(WellingtonFootprint peak, JsonGenerator jGenerator) throws IOException
    {
        jGenerator.writeNumberField( FIELD_WELLINGTON_SCORE, peak.getWellingtonScore() );
    }

}
