package biouml.plugins.gtrd.master.sites.json;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

import biouml.plugins.gtrd.ChIPexoExperiment;
import biouml.plugins.gtrd.master.sites.chipexo.ChIPexoGEMPeak;
import biouml.plugins.gtrd.master.sites.chipexo.ChIPexoPeak;
import biouml.plugins.gtrd.master.sites.chipexo.ChIPexoPeakzillaPeak;
import ru.biosoft.bsa.track.big.BigBedTrack;
import ru.biosoft.util.TextUtil2;

public class ChIPexoPeakSerializer extends GenomeLocationSerializer<ChIPexoPeak>
{
    protected Map<String, ChIPexoExperiment> exps;
    protected BigBedTrack<?> origin;
    public ChIPexoPeakSerializer( BigBedTrack<?> origin, Map<String, ChIPexoExperiment> exps)
    {
        this.origin = origin;
        this.exps = exps;
    }
    
    @Override
    public void writeFields(ChIPexoPeak peak, JsonGenerator jGenerator) throws IOException
    {
        super.writeFields( peak, jGenerator );//write genome location
        //write other peak properties
        if(peak.getClass().equals( ChIPexoPeakzillaPeak.class ))
            writePeakzilla( (ChIPexoPeakzillaPeak)peak, jGenerator );
        else if(peak.getClass().equals( ChIPexoGEMPeak.class ))
            writeGEM( (ChIPexoGEMPeak)peak, jGenerator );
        else
            throw new IllegalArgumentException("Unknown peak type: " + peak.getClass());
    }
    
    @Override
    public ChIPexoPeak read(JsonParser parser) throws IOException
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
            if(result.getClass().equals( ChIPexoPeakzillaPeak.class ))
                parsePeakzilla( parser );
            else if(result.getClass().equals( ChIPexoGEMPeak.class ))
                parseGEM( parser );
            else
                throw new AssertionError();
        }
    }
    
    protected void parseId(JsonParser parser) throws IOException
    {
        String name = parser.getCurrentName();
        if(!name.equals( FIELD_ID ))
            throw new JsonParseException(parser, "id should be the first field");

        String idString = parser.getValueAsString();//p.EEXP003098.macs2.10719
        String[] parts = TextUtil2.split(idString, '.');
        
        String peakCaller = parts[2];
        switch(peakCaller)
        {
            case ChIPexoPeakzillaPeak.PEAK_CALLER:result = new ChIPexoPeakzillaPeak(); break;
            case ChIPexoGEMPeak.PEAK_CALLER:result = new ChIPexoGEMPeak(); break;
            default:
                throw new JsonParseException(parser, "Unknown peak caller: " + peakCaller);
        }
        
        result.setOrigin( origin );
        result.setId( Integer.parseInt( parts[3] ) );
        
        String expId = parts[1];
        result.setExp( exps.get( expId ) );
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
        ChIPexoGEMPeak peak = (ChIPexoGEMPeak)result;
        switch(name)
        {
            case FIELD_POS: parsePos(peak, parser); break;
            case FIELD_GEM_CONTROL: peak.setControl( (float)parser.getValueAsDouble() ); break;
            case FIELD_GEM_EXPECTED: peak.setExpected( (float)parser.getValueAsDouble() ); break;
            case FIELD_GEM_FOLD: peak.setFold( (float)parser.getValueAsDouble() ); break;
            case FIELD_GEM_IP: peak.setIp( (float)parser.getValueAsDouble() ); break;
            case FIELD_GEM_IP_VS_EMP: peak.setIpVsEmp( (float)parser.getValueAsDouble() ); break;
            case FIELD_GEM_NOISE: peak.setNoise( (float)parser.getValueAsDouble() ); break;
            case FIELD_GEM_P_MLOG10: peak.setPMLog10( (float)parser.getValueAsDouble() ); break;
            case FIELD_GEM_P_POISS: peak.setPPoiss( (float)parser.getValueAsDouble() ); break;
            case FIELD_GEM_Q_MLOG10: peak.setQMLog10( (float)parser.getValueAsDouble() ); break;
            default:
                throw new JsonParseException( parser, "Unexpected field: " + name );
        }
        
    }
    private void writeGEM(ChIPexoGEMPeak peak, JsonGenerator jGenerator) throws IOException
    {
        jGenerator.writeNumberField( FIELD_GEM_CONTROL, peak.getControl() );
        jGenerator.writeNumberField( FIELD_GEM_EXPECTED, peak.getExpected() );
        jGenerator.writeNumberField( FIELD_GEM_FOLD, peak.getFold() );
        jGenerator.writeNumberField( FIELD_GEM_IP, peak.getId() );
        jGenerator.writeNumberField( FIELD_GEM_IP_VS_EMP, peak.getIpVsEmp() );
        jGenerator.writeNumberField( FIELD_GEM_NOISE, peak.getNoise() );
        jGenerator.writeNumberField( FIELD_GEM_P_MLOG10, peak.getPMLog10() );
        jGenerator.writeNumberField( FIELD_GEM_P_POISS, peak.getPPoiss() );
        jGenerator.writeNumberField( FIELD_GEM_Q_MLOG10, peak.getQMLog10() );
    }
    
    
    private static final String FIELD_PEAKZILLA_SUMMIT = "summit";
    private static final String FIELD_PEAKZILLA_SCORE = "score";
    private static final String FIELD_PEAKZILLA_FOLD_ENRICHMENT = "foldEnrichment";
    private static final String FIELD_PEAKZILLA_FDR = "fdr";
    private static final String FIELD_PEAKZILLA_DISTRIBUTION_SCORE = "distributionScore";
    private static final String FIELD_PEAKZILLA_CONTROL = "control";
    private static final String FIELD_PEAKZILLA_CHIP = "chip";
    private void parsePeakzilla(JsonParser parser) throws IOException
    {
        String name = parser.getCurrentName();
        ChIPexoPeakzillaPeak peak = (ChIPexoPeakzillaPeak)result;
        switch(name)
        {
            case FIELD_POS: parsePos(peak, parser); break;
            case FIELD_PEAKZILLA_SUMMIT: peak.setSummit( parser.getIntValue() ); break;
            case FIELD_PEAKZILLA_SCORE: peak.setPeakZillaScore( (float)parser.getValueAsDouble() ); break;
            case FIELD_PEAKZILLA_FOLD_ENRICHMENT: peak.setFoldEnrichment( (float)parser.getValueAsDouble() ); break;
            case FIELD_PEAKZILLA_FDR: peak.setFdr( (float)parser.getValueAsDouble() ); break;
            case FIELD_PEAKZILLA_DISTRIBUTION_SCORE: peak.setDistributionScore( (float)parser.getValueAsDouble() ); break;
            case FIELD_PEAKZILLA_CONTROL: peak.setControl( (float)parser.getValueAsDouble() ); break;
            case FIELD_PEAKZILLA_CHIP: peak.setChip( (float)parser.getValueAsDouble() ); break;
            default:
                throw new JsonParseException( parser, "Unexpected field: " + name );
        }
        
    }
    private void writePeakzilla(ChIPexoPeakzillaPeak peak, JsonGenerator jGenerator) throws IOException
    {
        jGenerator.writeNumberField( FIELD_PEAKZILLA_CHIP, peak.getChip() );
        jGenerator.writeNumberField( FIELD_PEAKZILLA_CONTROL, peak.getControl() );
        jGenerator.writeNumberField( FIELD_PEAKZILLA_DISTRIBUTION_SCORE, peak.getDistributionScore() );
        jGenerator.writeNumberField( FIELD_PEAKZILLA_FDR, peak.getFdr() );
        jGenerator.writeNumberField( FIELD_PEAKZILLA_FOLD_ENRICHMENT, peak.getFoldEnrichment() );
        jGenerator.writeNumberField( FIELD_PEAKZILLA_SCORE, peak.getPeakZillaScore() );
        jGenerator.writeNumberField( FIELD_PEAKZILLA_SUMMIT, peak.getSummit() );
    }

}
