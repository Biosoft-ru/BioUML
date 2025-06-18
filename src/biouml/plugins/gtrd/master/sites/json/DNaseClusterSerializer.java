package biouml.plugins.gtrd.master.sites.json;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

import biouml.plugins.gtrd.CellLine;
import biouml.plugins.gtrd.master.sites.dnase.DNaseCluster;
import biouml.plugins.gtrd.master.sites.dnase.DNaseCluster.Design;
import biouml.plugins.gtrd.master.sites.dnase.FootprintCluster;
import biouml.plugins.gtrd.master.sites.dnase.Hotspot2DNaseCluster;
import biouml.plugins.gtrd.master.sites.dnase.MACS2DNaseCluster;
import biouml.plugins.gtrd.master.sites.dnase.WellingtonHotspot2FootprintCluster;
import biouml.plugins.gtrd.master.sites.dnase.WellingtonMACS2FootprintCluster;
import ru.biosoft.bsa.track.big.BigBedTrack;
import ru.biosoft.util.TextUtil2;

public class DNaseClusterSerializer extends GenomeLocationSerializer<DNaseCluster>
{
    private static final String FIELD_ID = "id";
    private static final String FIELD_POS = "pos";
    private static final String FIELD_PEAK_COUNT = "peakCount";
    private static final String FIELD_MEAN_ABS_SUMMIT = "meanAbsSummit";
    private static final String FIELD_MEDIAN_ABS_SUMMIT = "medianAbsSummit";
    private static final String FIELD_MEAN_PILEUP = "meanPileup";
    private static final String FIELD_MEDIAN_PILEUP = "medianPileup";
    private static final String FIELD_MEAN_FOLD_ENRICHMENT = "meanFoldEnrichment";
    private static final String FIELD_MEDIAN_FOLD_ENRICHMENT = "medianFoldEnrichment";

    
    private BigBedTrack<?> origin;
    private Map<String, CellLine> cells;
    public DNaseClusterSerializer(BigBedTrack<?> origin, Map<String, CellLine> cells)
    {
        this.origin = origin;
        this.cells = cells;
    }
    
    @Override
    public DNaseCluster read(JsonParser parser) throws IOException
    {
        result = null;
        return super.read( parser );
    }

    @Override
    protected void readField(JsonParser parser) throws IOException
    {
        if(result == null)
            parseId( parser );
        else
        {
            if(result.getClass().equals( MACS2DNaseCluster.class ))
                parseMACS2( parser );
            else if(result.getClass().equals( Hotspot2DNaseCluster.class ) || result instanceof FootprintCluster)
                parseWithoutExtraFields( parser );
            else
                throw new AssertionError();
            
        }
    }
    
    protected void parseId(JsonParser parser) throws IOException
    {
        String name = parser.getCurrentName();
        if(!name.equals( FIELD_ID ))
            throw new JsonParseException(parser, "id should be the first field, but get " + name);
        String text = parser.getValueAsString(); //dc.CEL001234.macs2.3343
        String[] parts = TextUtil2.split(text, '.');
        
        String peakCaller = parts[2].toLowerCase();
        switch(peakCaller)
        {
            case MACS2DNaseCluster.PEAK_CALLER: result = new MACS2DNaseCluster();break;
            case Hotspot2DNaseCluster.PEAK_CALLER: result = new Hotspot2DNaseCluster(); break;
            case WellingtonMACS2FootprintCluster.PEAK_CALLER: result = new WellingtonMACS2FootprintCluster(); break;
            case WellingtonHotspot2FootprintCluster.PEAK_CALLER: result = new WellingtonHotspot2FootprintCluster(); break;
            default:
                throw new JsonParseException(parser, "Unknown peak caller: " + peakCaller);
        }
        
        result.setOrigin( origin );
        
        Design design = Design.getByIdPrefix( parts[0] );
        result.setDesign( design );
        
        String cellId = parts[1];
        cellId = cellId.substring( "CELL".length() );
        cellId = String.valueOf( Integer.parseInt( cellId ) );
        CellLine cell = cells.get( cellId );
        result.setCell( cell );
        
        result.setId( Integer.parseInt( parts[3] ) );
    }
    
    protected void parseMACS2(JsonParser parser) throws IOException
    {
        String name = parser.getCurrentName();
        MACS2DNaseCluster cluster = (MACS2DNaseCluster)result;
        switch(name)
        {
            case FIELD_POS:
                parsePos(cluster, parser);
                break;
            case FIELD_PEAK_COUNT:
                cluster.setPeakCount( parser.getIntValue() );
                break;
            case FIELD_MEAN_ABS_SUMMIT:
                cluster.setMeanAbsSummit( (float)parser.getValueAsDouble() );
                break;
            case FIELD_MEDIAN_ABS_SUMMIT:
                cluster.setMedianAbsSummit( (float)parser.getValueAsDouble() );
                break;
            case FIELD_MEAN_PILEUP:
                cluster.setMeanPileup( (float)parser.getValueAsDouble() );
                break;
            case FIELD_MEDIAN_PILEUP:
                cluster.setMedianPileup( (float)parser.getValueAsDouble() );
                break;
            case FIELD_MEAN_FOLD_ENRICHMENT:
                cluster.setMeanFoldEnrichment( (float)parser.getValueAsDouble() );
                break;
            case FIELD_MEDIAN_FOLD_ENRICHMENT:
                cluster.setMedianFoldEnrichment( (float)parser.getValueAsDouble() );
                break;
            default:
                throw new JsonParseException( parser, "Unexpected field: " + name );
        }    
    }
    
    private void parseWithoutExtraFields(JsonParser parser) throws IOException
    {
        String name = parser.getCurrentName();
        switch(name)
        {
            case FIELD_POS:
                parsePos(result, parser);
                break;
            case FIELD_PEAK_COUNT:
                result.setPeakCount( parser.getIntValue() );
                break;
            default:
                throw new JsonParseException( parser, "Unexpected field: " + name );
        }            
    }

    @Override
    public void writeFields(DNaseCluster cluster, JsonGenerator jGenerator) throws IOException
    {
        super.writeFields( cluster, jGenerator );//write genome location
        //write other peak properties
        if(cluster.getClass().equals( MACS2DNaseCluster.class ))
            writeMACS2( (MACS2DNaseCluster)cluster, jGenerator );
        else if(cluster.getClass().equals( Hotspot2DNaseCluster.class ) || cluster instanceof FootprintCluster)
            writeWithoutExtraFields( cluster, jGenerator );
        else
            throw new IllegalArgumentException("Unknown peak type: " + cluster.getClass());
    }
    
    protected void writeMACS2(MACS2DNaseCluster obj, JsonGenerator jGenerator) throws IOException
    {
        writeWithoutExtraFields( obj, jGenerator );
        jGenerator.writeNumberField( FIELD_MEAN_ABS_SUMMIT, obj.getMeanAbsSummit() );
        jGenerator.writeNumberField( FIELD_MEDIAN_ABS_SUMMIT, obj.getMedianAbsSummit() );
        jGenerator.writeNumberField( FIELD_MEAN_PILEUP, obj.getMeanPileup() );
        jGenerator.writeNumberField( FIELD_MEDIAN_PILEUP, obj.getMedianPileup() );
        jGenerator.writeNumberField( FIELD_MEAN_FOLD_ENRICHMENT, obj.getMeanFoldEnrichment() );
        jGenerator.writeNumberField( FIELD_MEDIAN_FOLD_ENRICHMENT, obj.getMedianFoldEnrichment() );
    }
    
    protected void writeWithoutExtraFields(DNaseCluster obj, JsonGenerator jGenerator) throws IOException
    {
        jGenerator.writeNumberField( FIELD_PEAK_COUNT, obj.getPeakCount() );
    }
    
}
