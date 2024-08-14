package biouml.plugins.gtrd.master.meta.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

import biouml.plugins.gtrd.master.meta.BuildInfo;
import biouml.plugins.gtrd.master.meta.BuildInfo.HistoneClustersInfo;
import biouml.plugins.gtrd.master.sites.json.JacksonObjectSerializer;
import biouml.plugins.gtrd.master.sites.json.ListSerializer;
import biouml.plugins.gtrd.master.sites.json.MapOfStringSetSerializer;
import biouml.plugins.gtrd.master.sites.json.StringSerializer;
import biouml.standard.type.Species;

public class BuildInfoSerializer extends JacksonObjectSerializer<BuildInfo>
{
    public static final String FIELD_CHIPSEQ = "chipseq";
    public static final String FIELD_CHIPEXO = "chipexo";
    public static final String FIELD_DNASE_PEAKS = "dnasePeaks";
    public static final String FIELD_DNASE_CLUSTERS = "dnaseClusters";
    public static final String FIELD_FOOTPRINTS = "footprints";
    public static final String FIELD_FOOTPRINT_CLUSTERS = "footprintClusters";
    public static final String FIELD_ATAC_CLUSTERS = "atacClusters";
    public static final String FIELD_FAIRE_CLUSTERS = "faireClusters";
    public static final String FIELD_MNASE_PEAKS = "mnasePeaks";
    public static final String FIELD_HISTONE_PEAKS = "histonePeaks";
    public static final String FIELD_HISTONE_CLUSTERS = "histoneClusters";
    public static final String FIELD_MOTIFS = "motifs";
    
    @Override
    public BuildInfo read(JsonParser parser) throws IOException
    {
        result = new BuildInfo();
        return super.read( parser );
    }
    
    private ListSerializer<String> strListSerializer = new ListSerializer<>( new StringSerializer() );
    private MapOfStringSetSerializer mosSerializer = new MapOfStringSetSerializer();
    private ListSerializer<HistoneClustersInfo> listOfHistoneClusters = new ListSerializer<>(new HistoneClustersInfoSerializer());
    
    @Override
    protected void readField(JsonParser parser) throws IOException
    {
        String name = parser.getCurrentName();
        switch(name)
        {
            case FIELD_CHIPSEQ:
                result.chipSeqPeaksByPeakCaller = mosSerializer.read( parser );
                break;
            case FIELD_CHIPEXO:
                result.chipExoPeaksByPeakCaller = mosSerializer.read( parser );
                break;
            case FIELD_DNASE_PEAKS:
                result.dnasePeaksByPeakCaller = mosSerializer.read( parser );
                break;
            case FIELD_DNASE_CLUSTERS:
                result.dnaseClustersByPeakCaller = mosSerializer.read( parser );
                break;
            case FIELD_FOOTPRINTS:
                result.footprints = mosSerializer.read( parser );
                break;
            case FIELD_FOOTPRINT_CLUSTERS:
                result.footprintClusters = mosSerializer.read( parser );
                break;
            case FIELD_ATAC_CLUSTERS:
                result.atacClusters = mosSerializer.read( parser );
                break;
            case FIELD_FAIRE_CLUSTERS:
                result.faireClusters = mosSerializer.read( parser );
                break;
            case FIELD_MNASE_PEAKS:
                result.mnasePeaks = mosSerializer.read( parser );
                break;
            case FIELD_HISTONE_PEAKS:
                result.histonePeaks = mosSerializer.read( parser );
                break;
            case FIELD_HISTONE_CLUSTERS:
                result.histoneClusters = new HashSet<>(listOfHistoneClusters.read( parser ));
                break;
            case FIELD_MOTIFS:
                strListSerializer.setReadTarget( new ArrayList<>() );
                result.motifs = new HashSet<>(strListSerializer.read( parser ));
                break;
            default:
                throw new JsonParseException( parser, "Unexpected field: " + name );
        }
    }
    
    
    private void writeMapOfSet(String name, Map<String, Set<String>> data, JsonGenerator jGenerator) throws IOException
    {
        if(data.isEmpty())
            return;
        jGenerator.writeFieldName( name );
        mosSerializer.write( data, jGenerator );
    }

    @Override
    protected void writeFields(BuildInfo info, JsonGenerator jGenerator) throws IOException
    {
        writeMapOfSet(FIELD_CHIPSEQ, info.chipSeqPeaksByPeakCaller, jGenerator);
        writeMapOfSet(FIELD_CHIPEXO, info.chipExoPeaksByPeakCaller, jGenerator);
        writeMapOfSet(FIELD_DNASE_PEAKS, info.dnasePeaksByPeakCaller, jGenerator);
        writeMapOfSet(FIELD_DNASE_CLUSTERS, info.dnaseClustersByPeakCaller, jGenerator);
        writeMapOfSet(FIELD_FOOTPRINTS, info.footprints, jGenerator);
        writeMapOfSet(FIELD_FOOTPRINT_CLUSTERS, info.footprintClusters, jGenerator);
        writeMapOfSet(FIELD_ATAC_CLUSTERS, info.atacClusters, jGenerator);
        writeMapOfSet(FIELD_FAIRE_CLUSTERS, info.faireClusters, jGenerator);
        writeMapOfSet(FIELD_MNASE_PEAKS, info.mnasePeaks, jGenerator);
        writeMapOfSet(FIELD_HISTONE_PEAKS, info.histonePeaks, jGenerator);

        if( !info.histoneClusters.isEmpty() )
        {
            
            jGenerator.writeFieldName( FIELD_HISTONE_CLUSTERS );
            listOfHistoneClusters.write( new ArrayList<>(info.histoneClusters), jGenerator );
        }
        
        if(!info.motifs.isEmpty())
        {
            
            jGenerator.writeFieldName( FIELD_MOTIFS );
            strListSerializer.write( new ArrayList<>(info.motifs), jGenerator );
        }
    }

}
