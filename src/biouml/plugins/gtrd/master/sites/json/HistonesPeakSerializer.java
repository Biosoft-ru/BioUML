package biouml.plugins.gtrd.master.sites.json;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

import biouml.plugins.gtrd.HistonesExperiment;
import biouml.plugins.gtrd.master.sites.histones.HistonesPeak;
import biouml.plugins.gtrd.master.sites.histones.MACS2HistonesPeak;
import ru.biosoft.bsa.track.big.BigBedTrack;
import ru.biosoft.util.TextUtil2;

public class HistonesPeakSerializer extends GenomeLocationSerializer<HistonesPeak>
{
    public static final String FIELD_ID = "id";
    
    protected Map<String, HistonesExperiment> exps; 
    protected BigBedTrack<?> origin;
    
    public HistonesPeakSerializer(BigBedTrack<?> origin, Map<String, HistonesExperiment> exps)
    {
        this.origin = origin;
        this.exps = exps;
    }
    
    @Override
    public void writeFields(HistonesPeak peak, JsonGenerator jGenerator) throws IOException
    {
        super.writeFields( peak, jGenerator );//write genome location
        //write other peak properties
        if(peak.getClass().equals( MACS2HistonesPeak.class ))
            writeMACS2( (MACS2HistonesPeak)peak, jGenerator );
        else
            throw new IllegalArgumentException("Unknown peak type: " + peak.getClass());
    }
    
    @Override
    public HistonesPeak read(JsonParser parser) throws IOException
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
            if(result.getClass().equals( MACS2HistonesPeak.class ))
                parseMACS2( parser );
            else
                throw new AssertionError();
        }
    }
    
    protected void parseId(JsonParser parser) throws IOException
    {
        String name = parser.getCurrentName();
        if(!name.equals( FIELD_ID ))
            throw new JsonParseException(parser, "id should be the first field");

        String idString = parser.getValueAsString();//p.EXP003098.macs2.10719
        String[] parts = TextUtil2.split(idString, '.');
        
        String peakCaller = parts[2];
        switch(peakCaller)
        {
            case MACS2HistonesPeak.PEAK_CALLER:result = new MACS2HistonesPeak();break;
            default:
                throw new JsonParseException(parser, "Unknown peak caller: " + peakCaller);
        }
        
        result.setOrigin( origin );
        result.setId( Integer.parseInt( parts[3] ) );
        
        String expId = parts[1];
        result.setExp( exps.get( expId ) );
    }
    
    private static final String FIELD_MACS2_PILE_UP = "pileUp";
    private static final String FIELD_MACS2_SUMMIT = "summit";
    private static final String FIELD_MACS2_M_LOG10Q_VALUE = "mLog10QValue";
    private static final String FIELD_MACS2_M_LOG10P_VALUE = "mLog10PValue";
    private static final String FIELD_MACS2_FOLD_ENRICHMNET = "foldEnrichmnet";
    private void parseMACS2(JsonParser parser) throws IOException
    {
        String name = parser.getCurrentName();
        MACS2HistonesPeak peak = (MACS2HistonesPeak)result;
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
    private void writeMACS2(MACS2HistonesPeak peak, JsonGenerator jGenerator) throws IOException
    {
        jGenerator.writeNumberField( FIELD_MACS2_FOLD_ENRICHMNET, peak.getFoldEnrichment() );
        jGenerator.writeNumberField( FIELD_MACS2_M_LOG10P_VALUE, peak.getMLog10PValue() );
        jGenerator.writeNumberField( FIELD_MACS2_M_LOG10Q_VALUE, peak.getMLog10QValue() );
        if(peak.hasSummit())
          jGenerator.writeNumberField( FIELD_MACS2_SUMMIT, peak.getSummit() );
        jGenerator.writeNumberField( FIELD_MACS2_PILE_UP, peak.getPileup() );
    }
}
