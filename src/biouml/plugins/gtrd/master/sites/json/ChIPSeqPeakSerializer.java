package biouml.plugins.gtrd.master.sites.json;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.master.sites.chipseq.ChIPSeqPeak;
import biouml.plugins.gtrd.master.sites.chipseq.GEMPeak;
import biouml.plugins.gtrd.master.sites.chipseq.MACS2ChIPSeqPeak;
import biouml.plugins.gtrd.master.sites.chipseq.PICSPeak;
import biouml.plugins.gtrd.master.sites.chipseq.SISSRSPeak;
import ru.biosoft.bsa.track.big.BigBedTrack;
import ru.biosoft.util.TextUtil2;

public class ChIPSeqPeakSerializer extends GenomeLocationSerializer<ChIPSeqPeak>
{
    public static final String FIELD_ID = "id";
    
    protected Map<String, ChIPseqExperiment> exps; 
    protected BigBedTrack<?> origin;
    
    public ChIPSeqPeakSerializer(BigBedTrack<?> origin, Map<String, ChIPseqExperiment> exps)
    {
        this.origin = origin;
        this.exps = exps;
    }
    
    @Override
    public void writeFields(ChIPSeqPeak peak, JsonGenerator jGenerator) throws IOException
    {
        super.writeFields( peak, jGenerator );//write genome location
        //write other peak properties
        if(peak.getClass().equals( MACS2ChIPSeqPeak.class ))
            writeMACS2( (MACS2ChIPSeqPeak)peak, jGenerator );
        else if(peak.getClass().equals( SISSRSPeak.class ))
            writeSISSRS( (SISSRSPeak)peak, jGenerator );
        else if(peak.getClass().equals( GEMPeak.class ))
            writeGEM( (GEMPeak)peak, jGenerator );
        else if(peak.getClass().equals( PICSPeak.class ))
            writePICS( (PICSPeak)peak, jGenerator );
        else
            throw new IllegalArgumentException("Unknown peak type: " + peak.getClass());
    }
    
    @Override
    public ChIPSeqPeak read(JsonParser parser) throws IOException
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
            if(result.getClass().equals( MACS2ChIPSeqPeak.class ))
                parseMACS2( parser );
            else if(result.getClass().equals( SISSRSPeak.class ))
                parseSISSRS( parser );
            else if(result.getClass().equals( GEMPeak.class ))
                parseGEM( parser );
            else if(result.getClass().equals( PICSPeak.class ))
                parsePICS( parser );
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
        String[] parts = TextUtil2.split( idString, '.' );
        
        String peakCaller = parts[2];
        switch(peakCaller)
        {
            case MACS2ChIPSeqPeak.PEAK_CALLER:result = new MACS2ChIPSeqPeak();break;
            case GEMPeak.PEAK_CALLER:result = new GEMPeak(); break;
            case SISSRSPeak.PEAK_CALLER: result = new SISSRSPeak(); break;
            case PICSPeak.PEAK_CALLER: result = new PICSPeak(); break;
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
        MACS2ChIPSeqPeak peak = (MACS2ChIPSeqPeak)result;
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
    private void writeMACS2(MACS2ChIPSeqPeak peak, JsonGenerator jGenerator) throws IOException
    {
        jGenerator.writeNumberField( FIELD_MACS2_FOLD_ENRICHMNET, peak.getFoldEnrichment() );
        jGenerator.writeNumberField( FIELD_MACS2_M_LOG10P_VALUE, peak.getMLog10PValue() );
        jGenerator.writeNumberField( FIELD_MACS2_M_LOG10Q_VALUE, peak.getMLog10QValue() );
        jGenerator.writeNumberField( FIELD_MACS2_SUMMIT, peak.getSummit() );
        jGenerator.writeNumberField( FIELD_MACS2_PILE_UP, peak.getPileup() );
    }


    private static final String FIELD_SISSRS_NUM_TAGS = "numTags",
            FIELD_SISSRS_FOLD = "fold",
            FIELD_SISSRS_PVALUE = "pValue";
    private void parseSISSRS(JsonParser parser) throws IOException
    {
        String name = parser.getCurrentName();
        SISSRSPeak peak = (SISSRSPeak)result;
        switch(name)
        {
            case FIELD_POS: parsePos(peak, parser); break;
            case FIELD_SISSRS_NUM_TAGS: peak.setNumTags( parser.getIntValue() ); break;
            case FIELD_SISSRS_PVALUE: peak.setPValue( (float)parser.getValueAsDouble() ); break;
            case FIELD_SISSRS_FOLD: peak.setFold( (float)parser.getValueAsDouble() ); break;
            default:
                throw new JsonParseException( parser, "Unexpected field: " + name );
        }
    }
    private void writeSISSRS(SISSRSPeak peak, JsonGenerator jGenerator) throws IOException
    {
        jGenerator.writeNumberField( FIELD_SISSRS_NUM_TAGS, peak.getNumTags() );
        if(peak.getExp().getControlId() != null)
        {
            jGenerator.writeNumberField( "fold", peak.getFold() );
            jGenerator.writeNumberField( "pValue", peak.getPValue() );
        }
    }

    
    private static final String FIELD_GEM_CONTROL = "control",
            FIELD_GEM_EXPECTED = "expected",
            FIELD_GEM_FOLD = "fold",
            FIELD_GEM_IP = "IP",
            FIELD_GEM_IP_VS_EMP = "IPvsEMP",
            FIELD_GEM_NOISE = "Noise",
            FIELD_GEM_P_MLOG10 = "P_mLog10",
            FIELD_GEM_P_POISS = "P_poiss",
            FIELD_GEM_Q_MLOG10 = "Q_mLog10";
    private void parseGEM(JsonParser parser) throws IOException
    {
        String name = parser.getCurrentName();
        GEMPeak peak = (GEMPeak)result;
        switch(name)
        {
            case FIELD_POS: parsePos(peak, parser); break;
            case FIELD_GEM_CONTROL: peak.setControl( (float)parser.getValueAsDouble() ); break;
            case FIELD_GEM_EXPECTED: peak.setExpected( (float)parser.getValueAsDouble() ); break;
            case FIELD_GEM_FOLD: peak.setFold( (float)parser.getValueAsDouble()); break;
            case FIELD_GEM_IP: peak.setIp( (float)parser.getValueAsDouble()); break;
            case FIELD_GEM_IP_VS_EMP: peak.setIpVsEmp( (float)parser.getValueAsDouble()); break;
            case FIELD_GEM_NOISE: peak.setNoise( (float)parser.getValueAsDouble() ); break;
            case FIELD_GEM_P_MLOG10: peak.setPMLog10( (float)parser.getValueAsDouble() ); break;
            case FIELD_GEM_P_POISS: peak.setPPoiss( (float)parser.getValueAsDouble() ); break;
            case FIELD_GEM_Q_MLOG10: peak.setQMLog10( (float)parser.getValueAsDouble() ); break;
            default:
                throw new JsonParseException( parser, "Unexpected field: " + name );
        }
        
    }
    private void writeGEM(GEMPeak peak, JsonGenerator jGenerator) throws IOException
    {
        jGenerator.writeNumberField( FIELD_GEM_CONTROL, peak.getControl() );
        jGenerator.writeNumberField( FIELD_GEM_EXPECTED, peak.getExpected() );
        jGenerator.writeNumberField( FIELD_GEM_FOLD, peak.getFold() );
        jGenerator.writeNumberField( FIELD_GEM_IP, peak.getIp() );
        jGenerator.writeNumberField( FIELD_GEM_IP_VS_EMP, peak.getIpVsEmp() );
        jGenerator.writeNumberField( FIELD_GEM_NOISE, peak.getNoise() );
        jGenerator.writeNumberField( FIELD_GEM_P_MLOG10, peak.getPMLog10() );
        jGenerator.writeNumberField( FIELD_GEM_P_POISS, peak.getPPoiss() );
        jGenerator.writeNumberField( FIELD_GEM_Q_MLOG10, peak.getQMLog10() );
    }

    
    private static final String FIELD_PICS_SCORE = "score";
    private void parsePICS(JsonParser parser) throws IOException
    {
        String name = parser.getCurrentName();
        PICSPeak peak = (PICSPeak)result;
        switch(name)
        {
            case FIELD_POS: parsePos(peak, parser); break;
            case FIELD_PICS_SCORE: peak.setPicsScore( (float)parser.getValueAsDouble() ); break;
            default:
                throw new JsonParseException( parser, "Unexpected field: " + name );
        }
    }
    private void writePICS(PICSPeak peak, JsonGenerator jGenerator) throws IOException
    {
        jGenerator.writeNumberField( FIELD_PICS_SCORE, peak.getPicsScore() );
    }


}
