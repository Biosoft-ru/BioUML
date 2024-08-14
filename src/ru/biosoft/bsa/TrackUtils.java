package ru.biosoft.bsa;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import biouml.standard.type.Gene;
import biouml.standard.type.Species;
import biouml.standard.type.Transcript;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementNotFoundException;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.journal.ProjectUtils;

/**
 * @author lan
 *
 */
public class TrackUtils
{
    public static final String ENSEMBL_PATH_PROPERTY = "ensemblPath";
    public static final String TRACK_IMPLEMENTATION_PROPERTY = "trackImplementation";

    public static @Nonnull WritableTrack createTrack(DataCollection<?> origin, Properties properties) throws Exception
    {
        Class<? extends WritableTrack> clazz = SqlTrack.class;
        try
        {
        	String className = origin.getInfo().getProperty( TRACK_IMPLEMENTATION_PROPERTY );
        	if(className != null)
        	{
        		clazz = (Class<? extends WritableTrack>)ClassLoading.loadClass( className );
        	}
        }
        catch (Exception e)
        {
        }

        return createTrack( origin, properties, clazz );
    }
    
    public static @Nonnull WritableTrack createTrack(DataCollection<?> origin, Properties properties, Class<? extends WritableTrack> clazz)
            throws Exception
    {
        if( origin instanceof VectorDataCollection )
        {
            return new TrackImpl( origin, properties );
        }
        Constructor<?> constructor = clazz.getConstructor( ru.biosoft.access.core.DataCollection.class, Properties.class );
        return (WritableTrack)constructor.newInstance( origin, properties );
    }

    public static void addTrackProperty(Track track, String key, String value)
    {
        Properties properties = track.cast( ru.biosoft.access.core.DataCollection.class ).getInfo().getProperties();
        if( value != null )
            properties.setProperty( key, value );
        else
            properties.remove( key );
        CollectionFactoryUtils.save( track );
    }
    
    public static @Nonnull DataElementPath getTrackSequencesPath(Track track)
    {
        try
        {
            if(track instanceof TrackOnSequences)
                return track.getCompletePath();
            String sequences = ( track instanceof DataCollection ) ? ( (DataCollection<?>)track ).getInfo().getProperty(
                    Track.SEQUENCES_COLLECTION_PROPERTY) : DataElementPath.create(track).getDescriptor()
                    .getValue(Track.SEQUENCES_COLLECTION_PROPERTY);
            DataElementPath path = DataElementPath.create(sequences);
            if(path == null)
                throw new DataElementReadException(track, Track.SEQUENCES_COLLECTION_PROPERTY);
            return path;
        }
        catch( Exception e )
        {
            throw new DataElementReadException(e, track, Track.SEQUENCES_COLLECTION_PROPERTY);
        }
    }
    
    public static String getGenomeId(Track track)
    {
        if( track instanceof DataCollection )
            return ( (DataCollection<?>)track ).getInfo().getProperty( Track.GENOME_ID_PROPERTY );
        return DataElementPath.create( track ).getDescriptor().getValue( Track.GENOME_ID_PROPERTY );
    }
    
    public static @Nonnull DataElementPath getEnsemblPath(Species species) throws LoggedException
    {
        return getEnsemblPath( species, null );
    }

    public static @Nonnull DataElementPath getEnsemblPath(Species species, DataElementPath destPath) throws LoggedException
    {
        DataElementPath projectPath = ProjectUtils.getProjectPath( destPath );
        DataElementPath path = ProjectUtils.getPreferredDatabasePath( "Ensembl (" + species.getLatinName() + ")", projectPath );
        if(path != null)
        {
            return path;
        }
        try
        {
            String pathStr = species.getAttributes().getValueAsString( ENSEMBL_PATH_PROPERTY );
            if( pathStr == null )
                    throw new NullPointerException();
            return DataElementPath.create( pathStr );

        }
        catch( Exception e )
        {
            throw new DataElementReadException(DataElementPath.create(species), ENSEMBL_PATH_PROPERTY);
        }
    }

    public static @Nonnull DataElementPath getEnsemblPath(String speciesName) throws LoggedException
    {
        Species species = Species.getSpecies(speciesName);
        if(species == null)
            throw new DataElementNotFoundException(Species.SPECIES_PATH.getChildPath(speciesName));
        return getEnsemblPath(species);
    }
    
    public static @Nonnull DataElementPath getEnsemblPath(String speciesName, DataElementPath destPath) throws LoggedException
    {
        Species species = Species.getSpecies( speciesName );
        if( species == null )
            throw new DataElementNotFoundException( Species.SPECIES_PATH.getChildPath( speciesName ) );
        return getEnsemblPath( species, destPath );
    }

    public static @Nonnull DataCollection<Gene> getGenesCollection(@Nonnull DataElementPath ensemblPath) throws LoggedException
    {
        return ensemblPath.getChildPath("Data", "gene").getDataCollection(Gene.class);
    }
    
    public static @Nonnull DataCollection<Gene> getGenesCollection(Species species) throws LoggedException
    {
        return getGenesCollection( species, null );
    }
    
    public static @Nonnull DataCollection<Gene> getGenesCollection(Species species, DataElementPath destPath) throws LoggedException
    {
        return getGenesCollection( getEnsemblPath( species, destPath ) );
    }

    public static @Nonnull DataCollection<Transcript> getTranscriptsCollection(@Nonnull DataElementPath ensemblPath) throws LoggedException
    {
        return ensemblPath.getChildPath("Data", "transcript").getDataCollection(Transcript.class);
    }
    
    public static @Nonnull DataCollection<Transcript> getTranscriptsCollection(Species species) throws LoggedException
    {
        return getTranscriptsCollection( species, null );
    }
    
    public static @Nonnull DataCollection<Transcript> getTranscriptsCollection(Species species, DataElementPath destPath)
            throws LoggedException
    {
        return getTranscriptsCollection( getEnsemblPath( species, destPath ) );
    }

    public static DataCollection<VariationElement> getVariationCollection(@Nonnull DataElementPath ensemblPath)
    {
        return ensemblPath.getChildPath( "Data", "variation").getDataCollection( VariationElement.class );
    }

    public static DataElementPath getPrimarySequencesPath(@Nonnull DataElementPath databasePath) throws LoggedException
    {
        String sequencesName = databasePath.getDataCollection().getInfo().getProperty("sequences");
        if(sequencesName != null)
        {
            DataElementPath path = databasePath.getChildPath("Sequences", sequencesName);
            if(path.exists())
                return path;
        }
        
        DataElementPathSet children = databasePath.getChildPath("Sequences").getChildren();
        if(children.isEmpty())
            throw new DataElementReadException(databasePath, "sequences");
        DataElementPath path = children.first();
        DataCollection<? extends DataElement> dc = path.getDataElement(DataCollection.class);
        if(dc.getDataElementType().equals(AnnotatedSequence.class))
            return path;
        throw new DataElementReadException(path, "sequences");
    }

    public static List<String> getTrackSitesProperties(Track track)
    {
        List<String> result = new ArrayList<>();
        DataCollection<Site> allSites = track.getAllSites();
        if( !allSites.isEmpty() )
        {
            Iterator<String> nameIterator = allSites.iterator().next().getProperties().nameIterator();
            while( nameIterator.hasNext() )
            {
                result.add( nameIterator.next() );
            }
        }
        return result;
    }
}
