package biouml.plugins.gtrd.master.sites.json;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

import biouml.plugins.gtrd.CellLine;
import biouml.plugins.gtrd.master.sites.histones.HistonesCluster;
import biouml.plugins.gtrd.master.sites.histones.MACS2HistonesCluster;
import biouml.plugins.gtrd.master.utils.StringPool;
import ru.biosoft.bsa.track.big.BigBedTrack;
import ru.biosoft.util.TextUtil2;

public class HistonesClusterSerializer extends GenomeLocationSerializer<HistonesCluster>
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
    public HistonesClusterSerializer(BigBedTrack<?> origin, Map<String, CellLine> cells)
    {
        this.origin = origin;
        this.cells = cells;
    }
    
    @Override
    public HistonesCluster read(JsonParser parser) throws IOException
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
            if(result.getClass().equals( MACS2HistonesCluster.class ))
                parseMACS2( parser );
            else
                throw new AssertionError();
            
        }
    }
    
    protected void parseId(JsonParser parser) throws IOException
    {
        String name = parser.getCurrentName();
        if(!name.equals( FIELD_ID ))
            throw new JsonParseException(parser, "id should be the first field, but get " + name);
        String text = parser.getValueAsString(); //hc.H3KME3.CELL001234.macs2.3343 or hc.H2A.Zac.CELL000113.macs2.1
        if(!text.startsWith( "hc." ))
            throw new IllegalArgumentException();
        
        int idx =  text.lastIndexOf( '.' );
        if(idx == -1)
            throw new IllegalArgumentException();
        int id = Integer.parseInt(text.substring( idx + 1 ));
        
        int idx2 = text.lastIndexOf( '.', idx - 1 );
        String peakCaller = text.substring( idx2+1, idx );
        peakCaller = peakCaller.toLowerCase();
        
        idx = idx2;
        idx2 = text.lastIndexOf( '.', idx - 1 );
        if(idx2 == -1)
            throw new IllegalArgumentException();
        String cellId = text.substring( idx2+1, idx );
        cellId = cellId.substring( "CELL".length() );
        cellId = String.valueOf( Integer.parseInt( cellId ) );
        
        String target = text.substring( "hc.".length(), idx2 );
        target = StringPool.get( target );
        
        switch(peakCaller)
        {
            case MACS2HistonesCluster.PEAK_CALLER: result = new MACS2HistonesCluster();break;
            default:
                throw new JsonParseException(parser, "Unknown peak caller: " + peakCaller);
        }
        
        result.setOrigin( origin );
        
        result.setTarget( target);
        
        CellLine cell = cells.get( cellId );
        result.setCell( cell );
        
        result.setId( id );
    }
    
    protected void parseMACS2(JsonParser parser) throws IOException
    {
        String name = parser.getCurrentName();
        MACS2HistonesCluster cluster = (MACS2HistonesCluster)result;
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
    
    @Override
    public void writeFields(HistonesCluster cluster, JsonGenerator jGenerator) throws IOException
    {
        super.writeFields( cluster, jGenerator );//write genome location
        //write other peak properties
        if(cluster.getClass().equals( MACS2HistonesCluster.class ))
            writeMACS2( (MACS2HistonesCluster)cluster, jGenerator );
        else
            throw new IllegalArgumentException("Unknown peak type: " + cluster.getClass());
    }
    
    protected void writeMACS2(MACS2HistonesCluster obj, JsonGenerator jGenerator) throws IOException
    {
        jGenerator.writeNumberField( FIELD_PEAK_COUNT, obj.getPeakCount() );
        if(obj.hasSummit())
        {
            jGenerator.writeNumberField( FIELD_MEAN_ABS_SUMMIT, obj.getMeanAbsSummit() );
            jGenerator.writeNumberField( FIELD_MEDIAN_ABS_SUMMIT, obj.getMedianAbsSummit() );
        }
        jGenerator.writeNumberField( FIELD_MEAN_PILEUP, obj.getMeanPileup() );
        jGenerator.writeNumberField( FIELD_MEDIAN_PILEUP, obj.getMedianPileup() );
        jGenerator.writeNumberField( FIELD_MEAN_FOLD_ENRICHMENT, obj.getMeanFoldEnrichment() );
        jGenerator.writeNumberField( FIELD_MEDIAN_FOLD_ENRICHMENT, obj.getMedianFoldEnrichment() );
    }
}
