package biouml.plugins.gtrd.master.collections;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import biouml.plugins.ensembl.access.EnsemblDatabase;
import biouml.plugins.ensembl.access.EnsemblDatabaseSelector;
import biouml.plugins.gtrd.master.sites.PWMMotif;
import biouml.plugins.gtrd.master.sites.bedconv.BedEntryToMotif;
import biouml.standard.type.Species;
import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.bsa.ChrNameMapping;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.track.big.BigBedTrack;

public class MotifsCollection<T> extends AbstractDataCollection< BigBedTrack<PWMMotif> >
{
    private String folderPath;
    private @Nonnull List<String> nameList = new ArrayList<>();
    private String sequenceCollection;
    
    public MotifsCollection(DataCollection<?> parent, Properties properties) throws IOException
    {
        super( parent, properties );
        folderPath = properties.getProperty( DataCollectionConfigConstants.FILE_PATH_PROPERTY );
        if( folderPath == null )
            throw new IllegalArgumentException( "No " + DataCollectionConfigConstants.FILE_PATH_PROPERTY + " specified" );
        File folder = new File( folderPath );
        if( !folder.exists() || !folder.isDirectory() )
            throw new IllegalArgumentException( "Folder " + folderPath + " not exists" );
        
        String organism = properties.getProperty( "organism" );
        EnsemblDatabase ensembl = EnsemblDatabaseSelector.getDefaultEnsembl( Species.getSpecies( organism ) );
        sequenceCollection = ensembl.getPrimarySequencesPath().toString();
        
        for( String file : folder.list() )
            if( file.endsWith( ".bb" ) )
                nameList.add( file );
        Collections.sort( nameList );
    }
   
    @Override
    public @Nonnull List<String> getNameList()
    {
        return nameList;
    }

    @Override
    protected BigBedTrack<PWMMotif> doGet(String name) throws Exception
    {
        if(!name.endsWith( ".bb" ))
            return null;
        
        Properties properties = new Properties();
        properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
        properties.setProperty( DataCollectionConfigConstants.FILE_PATH_PROPERTY, folderPath );
       
        Path bbPath = Paths.get(folderPath, name);
        if(!Files.exists( bbPath ))
            return null;
        properties.setProperty( BigBedTrack.PROP_BIGBED_PATH, bbPath.toString() );
        
        properties.setProperty( Track.SEQUENCES_COLLECTION_PROPERTY, sequenceCollection );

        String mapping = getInfo().getProperty( ChrNameMapping.PROP_CHR_MAPPING );
        if(mapping != null)
            properties.setProperty( ChrNameMapping.PROP_CHR_MAPPING, mapping );
        
        properties.setProperty( BigBedTrack.PROP_CONVERTER_CLASS, BedEntryToMotif.class.getName() );
        properties.setProperty( BedEntryToMotif.SITE_MODEL_COLLECTION, getInfo().getProperty( BedEntryToMotif.SITE_MODEL_COLLECTION ) );
        
        return new BigBedTrack<>( this, properties);
    }
    
    @Override
    public @Nonnull Class<? extends DataElement> getDataElementType()
    {
        return BigBedTrack.class;
    }
}
