package biouml.plugins.gtrd.master.collections;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import biouml.plugins.ensembl.access.EnsemblDatabase;
import biouml.plugins.ensembl.access.EnsemblDatabaseSelector;
import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.Experiment;
import biouml.plugins.gtrd.master.meta.Metadata;
import biouml.plugins.gtrd.master.sites.Peak;
import biouml.plugins.gtrd.master.sites.bedconv.BedEntryToPeak;
import biouml.standard.type.Species;
import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.ChrNameMapping;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.track.big.BigBedTrack;

public abstract class PeaksCollection<E extends Experiment, P extends Peak<E>> extends AbstractDataCollection<BigBedTrack<P>>
{
    private String folderPath;
    
    private DataElementPath metadataPath;
    private DataElementPath experimentsPath;
    
    private @Nonnull List<String> nameList = new ArrayList<>();
    private Map<String, String> nameToFile = new HashMap<>();
    private Map<String, String> peakToExp = new HashMap<>();
    
    private String bigBedConverterClass;
    
    public PeaksCollection(DataCollection<?> parent, Properties properties) throws IOException
    {
        super( parent, properties );
        folderPath = properties.getProperty( DataCollectionConfigConstants.FILE_PATH_PROPERTY );
        if( folderPath == null )
            throw new IllegalArgumentException( "No " + DataCollectionConfigConstants.FILE_PATH_PROPERTY + " specified" );
        File folder = new File( folderPath );
        if( !folder.exists() || !folder.isDirectory() )
            throw new IllegalArgumentException( "Folder " + folderPath + " not exists" );
        
        metadataPath = DataElementPath.create( properties.getProperty( BedEntryToPeak.PROP_METADATA_PATH ) );
        experimentsPath = DataElementPath.create( properties.getProperty( BedEntryToPeak.PROP_EXPERIMENTS_PATH ));
        bigBedConverterClass = properties.getProperty( BigBedTrack.PROP_CONVERTER_CLASS );
        
        for( String file : folder.list() )
            if( file.endsWith( ".bb" ) )
            {
                String name = getNameForFile(file);
                if(name != null)
                    nameToFile.put(name, file);
            }
        nameList = new ArrayList<>(nameToFile.keySet());
        Collections.sort( nameList );
        
        initPeakToExp();
    }
    

    protected String getNameForFile(String fileName)
    {
        //default implementation: PEAKS000001_*.bb->PEAKS000001.bb
        int idx = fileName.indexOf( '_' );
        if(idx <= 0)
            return fileName;
        String id = fileName.substring( 0, idx );
        return id + ".bb";
    }
    
    protected String getPeakIdFromName(String name) {
        //default implementation: just remove extension
        int idx = name.indexOf( '.' );
        if(idx != -1)
            return name.substring( 0, idx );
        return null;
    }
   
    @Override
    public @Nonnull List<String> getNameList()
    {
        return nameList;
    }
    
    @Override
    protected BigBedTrack<P> doGet(String name) throws Exception
    {
        String fileName = nameToFile.get( name );
        if(fileName == null)
            return null;
        
        Path bbPath = Paths.get(folderPath, fileName);
        if(!Files.exists( bbPath ))
            return null;
        
        String peakId = getPeakIdFromName( name );
        if(peakId == null)
        {
            log.warning( "Can not deduce peak id for " + peakId );
            return null;
        }
        
        String expId = getExpIdForPeakId( peakId );
        if(expId == null)
        {
            log.warning( "Can not find experiment for " + peakId );
            return null;
        }
        Experiment exp = findExperiment( expId );
        
        Properties properties = createBBTrackProperties( name, exp);
        
        return new BigBedTrack<P>( this, properties );
    }


    protected Properties createBBTrackProperties(String name, Experiment exp)
    {
        Properties properties = new Properties();
        properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
        properties.setProperty( DataCollectionConfigConstants.FILE_PATH_PROPERTY, folderPath );
        Path bbPath = Paths.get(folderPath, nameToFile.get( name ));
        properties.setProperty( BigBedTrack.PROP_BIGBED_PATH, bbPath.toString() );
        properties.setProperty( BigBedTrack.PROP_CONVERTER_CLASS, bigBedConverterClass );

        String mapping = getInfo().getProperty( ChrNameMapping.PROP_CHR_MAPPING );
        if(mapping != null)
            properties.setProperty( ChrNameMapping.PROP_CHR_MAPPING, mapping );
        
        if(metadataPath != null)
            properties.setProperty( BedEntryToPeak.PROP_METADATA_PATH, metadataPath.toString() );
        if(experimentsPath != null)
            properties.setProperty( BedEntryToPeak.PROP_EXPERIMENTS_PATH, experimentsPath.toString() );
        properties.setProperty( BedEntryToPeak.PROP_EXPERIMENT_ID, exp.getName() );
        
        
        Species organism = exp.getSpecie();
        try
        {
            EnsemblDatabase ensembl = EnsemblDatabaseSelector.getDefaultEnsembl( organism );
            properties.setProperty( Track.SEQUENCES_COLLECTION_PROPERTY, ensembl.getPrimarySequencesPath().toString() );
        }
        catch(Exception e)
        {
            log.log( Level.WARNING, "Can not find chromosomes", e );
        }
        
        return properties;
    }
    
    private String getExpIdForPeakId(String peakId)
    {
        return peakToExp.get( peakId );
    }
    
    protected abstract Map<String, E> getExperimentsCollectionFromMetadata(Metadata meta);
    
    private Experiment findExperiment(String id)
    {
        if(metadataPath != null)
        {
            Metadata meta = metadataPath.getDataElement( Metadata.class );
            Map<String, E> exps = getExperimentsCollectionFromMetadata( meta );
            return exps.get( id );
        }else if(experimentsPath != null)
        {
            return experimentsPath.getChildPath( id ).optDataElement( Experiment.class );
        }
        else
            return null;
    }
    
    private void initPeakToExp()
    {
        Iterable<? extends Experiment> exps;
        if(metadataPath != null)
        {
            Metadata meta = metadataPath.getDataElement( Metadata.class );
            exps = getExperimentsCollectionFromMetadata( meta ).values();
        } else if(experimentsPath != null)
        {
            exps = experimentsPath.getDataCollection( Experiment.class );
        } else
            throw new IllegalArgumentException();
        for(Experiment exp : exps)
            if(exp.getPeakId() != null)
                peakToExp.put( exp.getPeakId(), exp.getName() );
        
    }

    @Override
    public @Nonnull Class<? extends DataElement> getDataElementType()
    {
        return BigBedTrack.class;
    }
    
    
}
