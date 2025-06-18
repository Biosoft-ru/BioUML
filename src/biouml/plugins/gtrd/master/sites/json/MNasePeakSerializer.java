package biouml.plugins.gtrd.master.sites.json;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

import biouml.plugins.gtrd.MNaseExperiment;
import biouml.plugins.gtrd.master.sites.mnase.Danpos2MNasePeak;
import biouml.plugins.gtrd.master.sites.mnase.MNasePeak;
import ru.biosoft.bsa.track.big.BigBedTrack;
import ru.biosoft.util.TextUtil2;

public class MNasePeakSerializer extends GenomeLocationSerializer<MNasePeak>
{
    public static final String FIELD_ID = "id";
    
    protected BigBedTrack<?> origin;
    protected Map<String, MNaseExperiment> exps; 
    public MNasePeakSerializer(BigBedTrack<?> origin, Map<String, MNaseExperiment> exps)
    {
        this.origin = origin;
        this.exps = exps;
    }
    
    @Override
    public void writeFields(MNasePeak peak, JsonGenerator jGenerator) throws IOException
    {
        super.writeFields( peak, jGenerator );//write genome location
        //write other peak properties
        writeDanpos2( (Danpos2MNasePeak)peak, jGenerator );
    }
    
    @Override
    public MNasePeak read(JsonParser parser) throws IOException
    {
        result = new Danpos2MNasePeak();
        return super.read( parser );
    }
    
    private static final String FIELD_DANPOS2_FUZZINESS_SCORE = "fuzzinessScore";
    private static final String FIELD_DANPOS2_SUMMIT = "summit";
    private static final String FIELD_DANPOS2_SUMMIT_VALUE = "summitValue";
    
    @Override
    protected void readField(JsonParser parser) throws IOException
    {
        String name = parser.getCurrentName();
        Danpos2MNasePeak peak = (Danpos2MNasePeak)result;
        switch(name)
        {
            case FIELD_ID: parseId(parser.getText(), parser); break;
            case FIELD_POS: parsePos(peak, parser); break;
            case FIELD_DANPOS2_FUZZINESS_SCORE: peak.setFuzzinessScore( (float)parser.getValueAsDouble() ); break;
            case FIELD_DANPOS2_SUMMIT: peak.setSummit( parser.getIntValue() ); break;
            case FIELD_DANPOS2_SUMMIT_VALUE: peak.setSummitValue(  parser.getIntValue() ); break;
            default:
                throw new JsonParseException( parser, "Unexpected field: " + name );
        }
    }
    private void writeDanpos2(Danpos2MNasePeak peak, JsonGenerator jGenerator) throws IOException
    {
        jGenerator.writeNumberField( FIELD_DANPOS2_FUZZINESS_SCORE, peak.getFuzzinessScore() );
        jGenerator.writeNumberField( FIELD_DANPOS2_SUMMIT, peak.getSummit() );
        jGenerator.writeNumberField( FIELD_DANPOS2_SUMMIT_VALUE, peak.getSummitValue() );
    }

    private void parseId(String idString, JsonParser parser) throws IOException
    {
        String[] parts = TextUtil2.split(idString, '.');
        
        String peakCaller = parts[2];
        if(!peakCaller.equals( Danpos2MNasePeak.PEAK_CALLER ))
            throw new JsonParseException(parser, "Unknown peak caller: " + peakCaller);
        
        result.setOrigin( origin );
        result.setId( Integer.parseInt( parts[3] ) );
        
        String expId = parts[1];
        result.setExp( exps.get( expId ) );
    }
}
