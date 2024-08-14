package biouml.plugins.gtrd.master.sites.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

import biouml.plugins.gtrd.master.MasterTrack;
import biouml.plugins.gtrd.master.meta.Metadata;
import biouml.plugins.gtrd.master.sites.HistoryEntry;
import biouml.plugins.gtrd.master.sites.MasterSite;
import biouml.plugins.gtrd.master.sites.MasterSite.Status;
import biouml.plugins.gtrd.master.sites.PWMMotif;
import biouml.plugins.gtrd.master.sites.chipexo.ChIPexoPeak;
import biouml.plugins.gtrd.master.sites.chipseq.ChIPSeqPeak;
import biouml.plugins.gtrd.master.sites.dnase.DNaseCluster;
import biouml.plugins.gtrd.master.sites.dnase.DNaseFootprint;
import biouml.plugins.gtrd.master.sites.dnase.DNasePeak;
import biouml.plugins.gtrd.master.sites.dnase.FootprintCluster;
import biouml.plugins.gtrd.master.sites.histones.HistonesCluster;
import biouml.plugins.gtrd.master.sites.histones.HistonesPeak;
import biouml.plugins.gtrd.master.sites.mnase.MNasePeak;

public class MasterSiteSerializer extends GenomeLocationSerializer<MasterSite>
{
    private static final String FIELD_ID = "id";
    private static final String FIELD_VERSION = "version";
    private static final String FIELD_SUMMIT = "summit";
    private static final String FIELD_RELIABILITY_LEVEL = "reliabilityLevel";
    private static final String FIELD_RELIABILITY_SCORE = "reliabilityScore";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_HISTORY = "history";
    
    private static final String FIELD_PEAKS = "peaks";
    private static final String FIELD_CHIP_EXO_PEAKS = "chipExoPeaks";
    private static final String FIELD_HISTONES_PEAKS = "histonesPeaks";
    private static final String FIELD_HISTONES_CLUSTERS = "histonesClusters";
    private static final String FIELD_MOTIFS = "motifs";
    private static final String FIELD_DNASE_PEAKS = "dnasePeaks";
    private static final String FIELD_DNASE_CLUSTERS = "dnaseClusters";
    private static final String FIELD_ATAC_CLUSTERS = "atacClusters";
    private static final String FIELD_FAIRE_CLUSTERS = "faireClusters";
    private static final String FIELD_DNASE_FOOTPRINTS = "dnaseFootprints";
    private static final String FIELD_FOOTPRINT_CLUSTERS = "footprintClusters";
    private static final String FIELD_MNASE_PEAKS = "mnasePeaks";
    
    private MasterTrack origin;
    
    private ListSerializer<ChIPSeqPeak> chipSeqListSerializer;
    private ListSerializer<ChIPexoPeak> chipExoListSerializer;
    private ListSerializer<HistonesPeak> histonesListSerializer;
    private ListSerializer<HistonesCluster> histonesClusterListSerializer;
    private ListSerializer<PWMMotif> pwmListSerializer;
    private ListSerializer<DNasePeak> dnaseListSerializer;
    private ListSerializer<DNaseCluster> dnaseClusterListSerializer;
    private ListSerializer<DNaseCluster> atacClusterListSerializer;
    private ListSerializer<DNaseCluster> faireClusterListSerializer;
    private ListSerializer<DNaseFootprint> footprintListSerializer;
    private ListSerializer<FootprintCluster> footprintClusterListSerializer;
    private ListSerializer<MNasePeak> mnaseListSerializer;
    private ListSerializer<HistoryEntry> historyListSerializer;

    public MasterSiteSerializer(MasterTrack origin)
    {
        this(origin, origin.getMetadata());
    }
    
    public MasterSiteSerializer(MasterTrack origin, Metadata metadata)
    {
        this.origin = origin;
        
        ChIPSeqPeakSerializer chipSeqSerializer = new ChIPSeqPeakSerializer(origin, metadata.chipSeqExperiments);
        ChIPexoPeakSerializer chipExoSerializer = new ChIPexoPeakSerializer(origin, metadata.chipExoExperiments);
        HistonesPeakSerializer histonesSerializer = new HistonesPeakSerializer( origin, metadata.histoneExperiments );
        HistonesClusterSerializer histonesClusterSerializer = new HistonesClusterSerializer( origin, metadata.cells );
        PWMMotifSerializer pwmMotifSerializer = new PWMMotifSerializer(origin, metadata.siteModels);
        DNasePeakSerializer dnasePeakSerializer = new DNasePeakSerializer(origin, metadata.dnaseExperiments);
        DNaseClusterSerializer dnaseClusterSerializer = new DNaseClusterSerializer(origin, metadata.cells);
        DNaseClusterSerializer atacClusterSerializer = new DNaseClusterSerializer(origin, metadata.cells);
        DNaseClusterSerializer faireClusterSerializer = new DNaseClusterSerializer(origin, metadata.cells);
        FootprintSerializer footprintSerializer = new FootprintSerializer(origin, metadata.dnaseExperiments);
        JacksonObjectSerializer footprintClusterSerializer = new DNaseClusterSerializer(origin, metadata.cells);
        MNasePeakSerializer mnasePeakSerializer = new MNasePeakSerializer(origin, metadata.mnaseExperiments );
        HistoryEntrySerializer historySerializer = new HistoryEntrySerializer();
        
        chipSeqListSerializer = new ListSerializer<ChIPSeqPeak>( chipSeqSerializer );
        chipExoListSerializer = new ListSerializer<ChIPexoPeak>( chipExoSerializer );
        histonesListSerializer = new ListSerializer<HistonesPeak>( new CachedSerializer<>( histonesSerializer ) );
        histonesClusterListSerializer = new ListSerializer<HistonesCluster>( new CachedSerializer<>( histonesClusterSerializer ) );
        pwmListSerializer = new ListSerializer<>(pwmMotifSerializer);
        dnaseListSerializer = new ListSerializer<>(new CachedSerializer<>( dnasePeakSerializer ));
        dnaseClusterListSerializer = new ListSerializer<>(new CachedSerializer<>( dnaseClusterSerializer ));
        atacClusterListSerializer = new ListSerializer<>(new CachedSerializer<>( atacClusterSerializer) );
        faireClusterListSerializer = new ListSerializer<>(new CachedSerializer<>(faireClusterSerializer) );
        footprintListSerializer = new ListSerializer<>(footprintSerializer);
        footprintClusterListSerializer = new ListSerializer<FootprintCluster>(footprintClusterSerializer);
        mnaseListSerializer = new ListSerializer<>(mnasePeakSerializer);
        historyListSerializer = new ListSerializer<HistoryEntry>(historySerializer);
    }

    
    protected void writeFields(MasterSite ms, JsonGenerator jGenerator) throws IOException
    {
        jGenerator.writeNumberField( FIELD_ID, ms.getId() );
        
        //we don't wont to write genome location here since it is stored in the BedEntry
        //super.writeFields( ms, jGenerator );
        
        jGenerator.writeNumberField( FIELD_SUMMIT, ms.getSummit() );
        jGenerator.writeNumberField( FIELD_VERSION, ms.getVersion() );
        jGenerator.writeStringField( FIELD_RELIABILITY_LEVEL, ms.getReliabilityLevel() );
        jGenerator.writeNumberField( FIELD_RELIABILITY_SCORE, ms.getReliabilityScore() );
        
        jGenerator.writeStringField( FIELD_STATUS, ms.getStatus().toString() );
        if(!ms.getHistory().isEmpty())
        {
            jGenerator.writeFieldName( FIELD_HISTORY );
            historyListSerializer.write(ms.getHistory(), jGenerator );
        }


        jGenerator.writeFieldName( FIELD_PEAKS );
        chipSeqListSerializer.write( ms.getChipSeqPeaks(), jGenerator );
        
        if(!ms.getChipExoPeaks().isEmpty())
        {
            jGenerator.writeFieldName( FIELD_CHIP_EXO_PEAKS );
            chipExoListSerializer.write( ms.getChipExoPeaks(), jGenerator );
        }
        
        if(!ms.getHistonesPeaks().isEmpty())
        {
            jGenerator.writeFieldName( FIELD_HISTONES_PEAKS );
            histonesListSerializer.write( ms.getHistonesPeaks(), jGenerator );
        }
        
        if(!ms.getHistonesClusters().isEmpty())
        {
            jGenerator.writeFieldName( FIELD_HISTONES_CLUSTERS );
            histonesClusterListSerializer.write( ms.getHistonesClusters(), jGenerator );
        }
        
        if(!ms.getMotifs().isEmpty())
        {
            jGenerator.writeFieldName( FIELD_MOTIFS );
            pwmListSerializer.write( ms.getMotifs(), jGenerator );
        }
        
        if(!ms.getDnasePeaks().isEmpty())
        {
            jGenerator.writeFieldName( FIELD_DNASE_PEAKS );
            dnaseListSerializer.write( ms.getDnasePeaks(), jGenerator );
        }
        
        if(!ms.getDnaseClusters().isEmpty())
        {
            jGenerator.writeFieldName( FIELD_DNASE_CLUSTERS );
            dnaseClusterListSerializer.write( ms.getDnaseClusters(), jGenerator );
        }
        
        if(!ms.getDnaseFootprints().isEmpty())
        {
            jGenerator.writeFieldName( FIELD_DNASE_FOOTPRINTS );
            footprintListSerializer.write( ms.getDnaseFootprints(), jGenerator );
        }
        
        if(!ms.getFootprintClusters().isEmpty())
        {
            jGenerator.writeFieldName( FIELD_FOOTPRINT_CLUSTERS );
            footprintClusterListSerializer.write( ms.getFootprintClusters(), jGenerator );
        }
        
        if(!ms.getAtacClusters().isEmpty())
        {
            jGenerator.writeFieldName( FIELD_ATAC_CLUSTERS );
            atacClusterListSerializer.write( ms.getAtacClusters(), jGenerator );
        }
        
        if(!ms.getFaireClusters().isEmpty())
        {
            jGenerator.writeFieldName( FIELD_FAIRE_CLUSTERS );
            faireClusterListSerializer.write( ms.getFaireClusters(), jGenerator );
        }
        
        if(!ms.getMnasePeaks().isEmpty())
        {
            jGenerator.writeFieldName( FIELD_MNASE_PEAKS );
            mnaseListSerializer.write( ms.getMnasePeaks(), jGenerator );
        }
    }

    @Override
    public MasterSite read(JsonParser parser) throws IOException
    {
        result = new MasterSite();
        result.setOrigin( origin );
        dnaseListSerializer.setReadTarget( result.getDnasePeaks() );
        dnaseClusterListSerializer.setReadTarget( result.getDnaseClusters() );
        atacClusterListSerializer.setReadTarget( result.getAtacClusters() );
        faireClusterListSerializer.setReadTarget( result.getFaireClusters() );
        footprintListSerializer.setReadTarget( result.getDnaseFootprints() );
        footprintClusterListSerializer.setReadTarget( result.getFootprintClusters() );
        pwmListSerializer.setReadTarget( result.getMotifs() );
        chipSeqListSerializer.setReadTarget( result.getChipSeqPeaks() );
        chipExoListSerializer.setReadTarget( result.getChipExoPeaks() );
        histonesListSerializer.setReadTarget( result.getHistonesPeaks() );
        histonesClusterListSerializer.setReadTarget( result.getHistonesClusters() );
        mnaseListSerializer.setReadTarget( result.getMnasePeaks() );
        return super.read( parser );
    }
    
    protected void readField(JsonParser parser) throws IOException
    {
        String name = parser.getCurrentName();
        switch(name)
        {
            case FIELD_POS: parsePos(result, parser); break;
            case FIELD_ID:  result.setId( parser.getIntValue() );  break;
            case FIELD_VERSION:  result.setVersion( parser.getIntValue() );  break;
            case FIELD_SUMMIT:  result.setSummit( parser.getIntValue() );  break;
            case FIELD_RELIABILITY_LEVEL:  result.setReliabilityLevel( parser.getValueAsString() );  break;
            case FIELD_RELIABILITY_SCORE:  result.setReliabilityScore( (float)parser.getValueAsDouble() );  break;
            case FIELD_STATUS:  result.setStatus( Status.valueOf( parser.getText() ) );  break;
            case FIELD_HISTORY: historyListSerializer.read( parser ); break;
            case FIELD_PEAKS: chipSeqListSerializer.read( parser ); break;
            case FIELD_CHIP_EXO_PEAKS: chipExoListSerializer.read( parser ); break;
            case FIELD_HISTONES_PEAKS: histonesListSerializer.read( parser ); break;
            case FIELD_HISTONES_CLUSTERS: histonesClusterListSerializer.read( parser ); break;
            case FIELD_MOTIFS: pwmListSerializer.read( parser ); break;
            case FIELD_DNASE_PEAKS: dnaseListSerializer.read( parser ); break;
            case FIELD_DNASE_CLUSTERS: dnaseClusterListSerializer.read( parser ); break;
            case FIELD_ATAC_CLUSTERS: atacClusterListSerializer.read( parser ); break;
            case FIELD_FAIRE_CLUSTERS: faireClusterListSerializer.read( parser ); break;
            case FIELD_DNASE_FOOTPRINTS: footprintListSerializer.read( parser ); break;
            case FIELD_FOOTPRINT_CLUSTERS: footprintClusterListSerializer.read( parser ); break;
            case FIELD_MNASE_PEAKS: mnaseListSerializer.read( parser ); break;
            default:
                throw new JsonParseException( parser, "Unexpected field: " + name );
        }
    }

}
