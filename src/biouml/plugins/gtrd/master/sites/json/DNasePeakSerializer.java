package biouml.plugins.gtrd.master.sites.json;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

import biouml.plugins.gtrd.DNaseExperiment;
import biouml.plugins.gtrd.master.sites.dnase.DNasePeak;
import biouml.plugins.gtrd.master.sites.dnase.Hotspot2DNasePeak;
import biouml.plugins.gtrd.master.sites.dnase.MACS2DNasePeak;
import biouml.plugins.gtrd.master.utils.StringPool;
import ru.biosoft.bsa.track.big.BigBedTrack;
import ru.biosoft.util.TextUtil2;

public class DNasePeakSerializer extends GenomeLocationSerializer<DNasePeak>
{
    public static final String FIELD_ID = "id";
    
    protected BigBedTrack<?> origin;
    protected Map<String, DNaseExperiment> exps; 
    public DNasePeakSerializer(BigBedTrack<?> origin, Map<String, DNaseExperiment> exps)
    {
        this.origin = origin;
        this.exps = exps;
    }
    
    @Override
    public void writeFields(DNasePeak peak, JsonGenerator jGenerator) throws IOException
    {
        super.writeFields( peak, jGenerator );//write genome location
        //write other peak properties
        if(peak.getClass().equals( MACS2DNasePeak.class ))
            writeMACS2( (MACS2DNasePeak)peak, jGenerator );
        else if(peak.getClass().equals( Hotspot2DNasePeak.class ))
            writeHotspot2( (Hotspot2DNasePeak)peak, jGenerator );
        else
            throw new IllegalArgumentException("Unknown peak type: " + peak.getClass());
    }
    
    @Override
    public DNasePeak read(JsonParser parser) throws IOException
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
        {
            if(result.getClass().equals( MACS2DNasePeak.class ))
                parseMACS2( parser );
            else if(result.getClass().equals( Hotspot2DNasePeak.class ))
                parseHotspot2( parser );
            else
                throw new AssertionError();
        }
    }
    
    protected void parseId(JsonParser parser) throws IOException
    {
        String name = parser.getCurrentName();
        if(!name.equals( FIELD_ID ))
            throw new JsonParseException(parser, "id should be the first field, but get " + name);

        String idString = parser.getValueAsString();//p.EXP003098.macs2.10719
        String[] parts = TextUtil2.split(idString, '.');
        
        String peakCaller = parts[2].toLowerCase();
        switch(peakCaller)
        {
            case MACS2DNasePeak.PEAK_CALLER: result = new MACS2DNasePeak();break;
            case Hotspot2DNasePeak.PEAK_CALLER: result = new Hotspot2DNasePeak(); break;
            default:
                throw new JsonParseException(parser, "Unknown peak caller: " + peakCaller);
        }
        
        result.setOrigin( origin );
        result.setId( Integer.parseInt( parts[3] ) );
        
        String[] parts2 = TextUtil2.split(parts[1], '_' );
        String expId = parts2[0];
        result.setExp( exps.get( expId ) );
        
        result.setReplicate( Integer.parseInt( parts2[1] ) );
        
    }
    
    private static final String FIELD_MACS2_PILE_UP = "pileUp";
    private static final String FIELD_MACS2_SUMMIT = "summit";
    private static final String FIELD_MACS2_M_LOG10Q_VALUE = "mLog10QValue";
    private static final String FIELD_MACS2_M_LOG10P_VALUE = "mLog10PValue";
    private static final String FIELD_MACS2_FOLD_ENRICHMNET = "foldEnrichmnet";
    private void parseMACS2(JsonParser parser) throws IOException
    {
        String name = parser.getCurrentName();
        MACS2DNasePeak peak = (MACS2DNasePeak)result;
        switch(name)
        {
            case FIELD_POS: parsePos(peak, parser); break;
            case FIELD_MACS2_FOLD_ENRICHMNET: peak.setFoldEnrichment( (float)parser.getValueAsDouble() ); break;
            case FIELD_MACS2_M_LOG10P_VALUE: peak.setMLog10PValue( (float)parser.getValueAsDouble() ); break;
            case FIELD_MACS2_M_LOG10Q_VALUE: peak.setMLog10QValue( (float)parser.getValueAsDouble() ); break;
            case FIELD_MACS2_SUMMIT: peak.setSummit( parser.getIntValue() ); break;
            case FIELD_MACS2_PILE_UP: peak.setPileup( parser.getNumberValue().floatValue() ); break;
            default:
                throw new JsonParseException( parser, "Unexpected field: " + name );
        }
    }
    private void writeMACS2(MACS2DNasePeak peak, JsonGenerator jGenerator) throws IOException
    {
        jGenerator.writeNumberField( FIELD_MACS2_FOLD_ENRICHMNET, peak.getFoldEnrichment() );
        jGenerator.writeNumberField( FIELD_MACS2_M_LOG10P_VALUE, peak.getMLog10PValue() );
        jGenerator.writeNumberField( FIELD_MACS2_M_LOG10Q_VALUE, peak.getMLog10QValue() );
        jGenerator.writeNumberField( FIELD_MACS2_SUMMIT, peak.getSummit() );
        jGenerator.writeNumberField( FIELD_MACS2_PILE_UP, peak.getPileup() );
    }


    private static final String FIELD_HOTSPOT2_SCORE1 = "score1",
            FIELD_HOTSPOT2_SCORE2 = "score2";
    private void parseHotspot2(JsonParser parser) throws IOException
    {
        String name = parser.getCurrentName();
        Hotspot2DNasePeak peak = (Hotspot2DNasePeak)result;
        switch(name)
        {
            case FIELD_POS: parsePos(peak, parser); break;
            case FIELD_HOTSPOT2_SCORE1: peak.setScore1( (float)parser.getValueAsDouble() ); break;
            case FIELD_HOTSPOT2_SCORE2: peak.setScore2( (float)parser.getValueAsDouble() ); break;
            default:
                throw new JsonParseException( parser, "Unexpected field: " + name );
        }
    }
    private void writeHotspot2(Hotspot2DNasePeak peak, JsonGenerator jGenerator) throws IOException
    {
        jGenerator.writeNumberField( FIELD_HOTSPOT2_SCORE1, peak.getScore1() );
        jGenerator.writeNumberField( FIELD_HOTSPOT2_SCORE2, peak.getScore2() );
    }
}
