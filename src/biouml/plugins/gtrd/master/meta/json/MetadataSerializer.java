package biouml.plugins.gtrd.master.meta.json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

import biouml.plugins.gtrd.ATACExperiment;
import biouml.plugins.gtrd.CellLine;
import biouml.plugins.gtrd.ChIPexoExperiment;
import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.DNaseExperiment;
import biouml.plugins.gtrd.Experiment;
import biouml.plugins.gtrd.FAIREExperiment;
import biouml.plugins.gtrd.HistonesExperiment;
import biouml.plugins.gtrd.MNaseExperiment;
import biouml.plugins.gtrd.master.meta.Metadata;
import biouml.plugins.gtrd.master.sites.json.JacksonObjectSerializer;
import biouml.plugins.gtrd.master.sites.json.ListSerializer;
import ru.biosoft.access.core.DataElementPathSet;

public class MetadataSerializer extends JacksonObjectSerializer<Metadata>
{
    public static final String FIELD_VERSION = "version";
    public static final String FIELD_CELLS = "cells";
    public static final String FIELD_CHIPSEQ = "chipseq";
    public static final String FIELD_CHIPEXO = "chipexo";
    public static final String FIELD_HISTONES = "histones";
    public static final String FIELD_DNASE = "dnase";
    public static final String FIELD_MNASE = "mnase";
    public static final String FIELD_ATAC = "atac";
    public static final String FIELD_FAIRE = "faire";
    public static final String FIELD_SITE_MODELS_PATH = "siteModelsPath";
    public static final String FIELD_TF = "tf";
    public static final String FIELD_BUILD_INFO = "buildInfo";
    
    
    private TFSerializer tfSerializer = new TFSerializer();
    
    private ListSerializer<ChIPseqExperiment> chipSeqSerializer = new ListSerializer<>( new ChIPSeqExperimentSerializer() );
    private ListSerializer<ChIPexoExperiment> chipExoSerializer = new ListSerializer<>( new ChIPExoExperimentSerializer() );
    private ListSerializer<HistonesExperiment> histonesSerializer = new ListSerializer<>( new HistoneExperimentSerializer() );
    private ListSerializer<DNaseExperiment> dnaseSerializer = new ListSerializer<>( new DNaseExperimentSerializer() );
    private ListSerializer<MNaseExperiment> mnaseSerializer = new ListSerializer<>( new MNaseExperimentSerializer() );
    private ListSerializer<ATACExperiment> atacSerializer = new ListSerializer<>( new ATACExperimentSerializer() );
    private ListSerializer<FAIREExperiment> faireSerializer = new ListSerializer<>( new FAIREExperimentSerializer() );
    
    private ListSerializer<CellLine> cellSerializer = new ListSerializer<>( new CellSerializer() );
    private BuildInfoSerializer buildInfoSerializer = new BuildInfoSerializer();
    
    
    public static Metadata readMetadata(Path metadataPath) throws IOException
    {
        MetadataSerializer serializer = new MetadataSerializer();
        String json = new String(Files.readAllBytes( metadataPath ), "utf8");//TODO: reading from URL
        return serializer.fromJSON( json );
    }

    public static void writeMetadata(Metadata metadata, Path metadataPath) throws IOException
    {
        MetadataSerializer serializer = new MetadataSerializer();
        String json = serializer.toJSON( metadata );
        Files.write( metadataPath, json.getBytes( "utf8" ) );
    }
    
    @Override
    public Metadata read(JsonParser parser) throws IOException
    {
        result = new Metadata();
        super.read( parser );
        linkCellsToExperiments();
        return result;
    }

    private void linkCellsToExperiments()
    {
        linkCellsToExperiments(result.chipSeqExperiments.values());
        linkCellsToExperiments(result.chipExoExperiments.values());
        linkCellsToExperiments(result.dnaseExperiments.values());
        linkCellsToExperiments(result.histoneExperiments.values());
        linkCellsToExperiments( result.mnaseExperiments.values() );
        linkCellsToExperiments( result.atacExperiments.values() );
        linkCellsToExperiments( result.faireExperiments.values() );
    }
    
    private void linkCellsToExperiments(Collection<? extends Experiment> c)
    {
        for(Experiment exp : c)
        {
            String cellId = exp.getCell().getName();
            CellLine cell = result.cells.get( cellId );
            if(cell != null)
            {
                exp.setCell( cell );
                exp.setSpecie( cell.getSpecies() );
            }
            //if not found leave stub cell
        }
    }

    @Override
    protected void readField(JsonParser parser) throws IOException
    {
        String name = parser.getCurrentName();
        switch(name)
        {
            case FIELD_VERSION:
                result.setVersion( parser.getIntValue() );
                break;
            case FIELD_TF:
                result.tf = tfSerializer.read( parser );
                break;
            case FIELD_CHIPSEQ:
                chipSeqSerializer.setReadTarget( new ArrayList<>() );
                for(ChIPseqExperiment exp : chipSeqSerializer.read( parser ))
                    result.chipSeqExperiments.put( exp.getName(), exp );
                break;
            case FIELD_CHIPEXO:
                chipExoSerializer.setReadTarget( new ArrayList<>() );
                for(ChIPexoExperiment exp : chipExoSerializer.read( parser ))
                    result.chipExoExperiments.put( exp.getName(), exp );
                break;
            case FIELD_HISTONES:
                histonesSerializer.setReadTarget( new ArrayList<>() );
                for(HistonesExperiment exp : histonesSerializer.read( parser ))
                    result.histoneExperiments.put( exp.getName(), exp );
                break;
            case FIELD_DNASE:
                dnaseSerializer.setReadTarget( new ArrayList<>() );
                for(DNaseExperiment exp : dnaseSerializer.read( parser ))
                    result.dnaseExperiments.put( exp.getName(), exp );
                break;
            case FIELD_MNASE:
                mnaseSerializer.setReadTarget( new ArrayList<>() );
                for(MNaseExperiment exp : mnaseSerializer.read( parser ))
                    result.mnaseExperiments.put( exp.getName(), exp );
                break;
            case FIELD_ATAC:
                atacSerializer.setReadTarget( new ArrayList<>() );
                for(ATACExperiment exp : atacSerializer.read( parser ))
                    result.atacExperiments.put( exp.getName(), exp );
                break;
            case FIELD_FAIRE:
                faireSerializer.setReadTarget( new ArrayList<>() );
                for(FAIREExperiment exp : faireSerializer.read( parser ))
                    result.faireExperiments.put( exp.getName(), exp );
                break;
            case FIELD_CELLS:
                cellSerializer.setReadTarget( new ArrayList<>() );
                for(CellLine exp : cellSerializer.read( parser ))
                    result.cells.put( exp.getName(), exp );
                break;
            case FIELD_SITE_MODELS_PATH:
                result.siteModels = new DataElementPathSet( parser.getText() );
                break;
            case FIELD_BUILD_INFO:
                result.buildInfo = buildInfoSerializer.read( parser );
                break;
            default:
                throw new JsonParseException( parser, "Unexpected field: " + name );
        }
    }
    
    @Override
    public void write(Metadata obj, JsonGenerator jGenerator) throws IOException
    {
        jGenerator.useDefaultPrettyPrinter();
        super.write( obj, jGenerator );
    }

    @Override
    protected void writeFields(Metadata meta, JsonGenerator jGenerator) throws IOException
    {
        jGenerator.writeNumberField( FIELD_VERSION, meta.getVersion() );
        
        jGenerator.writeFieldName( FIELD_TF );
        tfSerializer.write( meta.tf, jGenerator );
        
        if( !meta.chipSeqExperiments.isEmpty() )
        {
            jGenerator.writeFieldName( FIELD_CHIPSEQ );
            List<ChIPseqExperiment> chipSeqList = new ArrayList<>( meta.chipSeqExperiments.values() );
            Collections.sort( chipSeqList );
            chipSeqSerializer.write( chipSeqList, jGenerator );
        }

        if( !meta.chipExoExperiments.isEmpty() )
        {
            jGenerator.writeFieldName( FIELD_CHIPEXO );
            List<ChIPexoExperiment> chipExoList = new ArrayList<>( meta.chipExoExperiments.values() );
            Collections.sort( chipExoList );
            chipExoSerializer.write( chipExoList, jGenerator );
        }

        if( !meta.histoneExperiments.isEmpty() )
        {
            jGenerator.writeFieldName( FIELD_HISTONES );
            List<HistonesExperiment> histoneList = new ArrayList<>( meta.histoneExperiments.values() );
            Collections.sort( histoneList );
            histonesSerializer.write( histoneList, jGenerator );
        }

        if( !meta.dnaseExperiments.isEmpty() )
        {
            jGenerator.writeFieldName( FIELD_DNASE );
            List<DNaseExperiment> dnaseList = new ArrayList<>( meta.dnaseExperiments.values() );
            Collections.sort( dnaseList );
            dnaseSerializer.write( dnaseList, jGenerator );
        }

        if( !meta.mnaseExperiments.isEmpty() )
        {
            jGenerator.writeFieldName( FIELD_MNASE );
            List<MNaseExperiment> mnaseList = new ArrayList<>( meta.mnaseExperiments.values() );
            Collections.sort( mnaseList );
            mnaseSerializer.write( mnaseList, jGenerator );
        }
        
        if( !meta.atacExperiments.isEmpty() )
        {
            jGenerator.writeFieldName( FIELD_ATAC );
            List<ATACExperiment> atacList = new ArrayList<>( meta.atacExperiments.values() );
            Collections.sort( atacList );
            atacSerializer.write( atacList, jGenerator );
        }
        
        if( !meta.faireExperiments.isEmpty() )
        {
            jGenerator.writeFieldName( FIELD_FAIRE );
            List<FAIREExperiment> faireList = new ArrayList<>( meta.faireExperiments.values() );
            Collections.sort( faireList );
            faireSerializer.write( faireList, jGenerator );
        }
        
        List<CellLine> cells = new ArrayList<>(meta.cells.values());
        Collections.sort( cells );
        jGenerator.writeFieldName( FIELD_CELLS );
        cellSerializer.write( cells , jGenerator );
        
        if(meta.siteModels != null && !meta.siteModels.isEmpty())
            jGenerator.writeStringField( FIELD_SITE_MODELS_PATH, meta.siteModels.toString() );

        jGenerator.writeFieldName( FIELD_BUILD_INFO );
        buildInfoSerializer.write( meta.buildInfo, jGenerator );
    }
}
