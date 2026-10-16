package ru.biosoft.bsa;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

    /** Child property of a generic folder that holds the omics type of an element (see the web import). */
    public static final String OMICS_TYPE_PROPERTY = "omicsType";

    private static final String[][] GENOME_BUILD_ALIASES = {
            // order matters: longer / more specific patterns first
            {"grch38", "hg38"}, {"hg38", "hg38"}, {"hs38", "hg38"},
            {"grch37", "hg19"}, {"hg19", "hg19"}, {"hs37", "hg19"}, {"b37", "hg19"}, {"g1k_v37", "hg19"},
            {"ncbi36", "hg18"}, {"hg18", "hg18"}, {"ncbi35", "hg17"}, {"hg17", "hg17"}, {"ncbi34", "hg16"}, {"hg16", "hg16"},
            {"grcm39", "mm39"}, {"mm39", "mm39"}, {"grcm38", "mm10"}, {"mm10", "mm10"}, {"ncbim37", "mm9"}, {"mm9", "mm9"},
            {"rnor_6", "rn6"}, {"rn6", "rn6"}, {"mratbn7", "rn7"}, {"rn7", "rn7"}};

    /**
     * Genome build id (hg19, hg38, mm10, ...) named by a free-text reference, e.g. a VCF {@code ##reference}
     * value such as "GRCh37", "hg19" or "file:///ref/human_g1k_v37.fasta". Returns null if none is recognized.
     */
    public static String normalizeGenomeBuild(String reference)
    {
        if( reference == null )
            return null;
        String lower = reference.toLowerCase( java.util.Locale.ENGLISH );
        for( String[] alias : GENOME_BUILD_ALIASES )
        {
            // whole token only: "hg19" must not match inside "hg190", nor "b37" inside "ab37"
            for( int pos = lower.indexOf( alias[0] ); pos >= 0; pos = lower.indexOf( alias[0], pos + 1 ) )
            {
                int end = pos + alias[0].length();
                if( ( pos == 0 || !Character.isLetterOrDigit( lower.charAt( pos - 1 ) ) )
                        && ( end == lower.length() || !Character.isDigit( lower.charAt( end ) ) ) )
                    return alias[1];
            }
        }
        return null;
    }

    /** Length of chromosome 1 in each build: it tells the build of a VCF whose contig lines give lengths. */
    private static final Map<String, String> CHR1_LENGTH_BUILDS = new HashMap<>();
    static
    {
        CHR1_LENGTH_BUILDS.put( "248956422", "hg38" );
        CHR1_LENGTH_BUILDS.put( "249250621", "hg19" );
        CHR1_LENGTH_BUILDS.put( "247249719", "hg18" );
        CHR1_LENGTH_BUILDS.put( "195154279", "mm39" );
        CHR1_LENGTH_BUILDS.put( "195471971", "mm10" );
        CHR1_LENGTH_BUILDS.put( "197195432", "mm9" );
    }

    /**
     * Genome build of a VCF, normalized by {@link #normalizeGenomeBuild}; null if the header tells none. The
     * length of chromosome 1 in the {@code ##contig} lines wins: the lines describe the coordinates, while
     * {@code ##reference} may be stale — Picard's LiftoverVcf rewrites the contig lines for the target build and
     * keeps the source's {@code ##reference=hg19}. Then {@code ##reference=} or {@code ##assembly=}, then the
     * {@code assembly} of a contig line.
     */
    public static String detectVcfGenomeBuild(java.io.File file)
    {
        try (java.io.BufferedReader reader = new java.io.BufferedReader( new java.io.InputStreamReader(
                new java.io.FileInputStream( file ), java.nio.charset.StandardCharsets.UTF_8 ) ))
        {
            String line;
            String fromReference = null, fromContig = null;
            while( ( line = reader.readLine() ) != null && line.startsWith( "##" ) )
            {
                String lower = line.toLowerCase( java.util.Locale.ENGLISH );
                if( lower.startsWith( "##reference=" ) || lower.startsWith( "##assembly=" ) )
                {
                    if( fromReference == null )
                        fromReference = normalizeGenomeBuild( line.substring( line.indexOf( '=' ) + 1 ) );
                }
                else if( lower.startsWith( "##contig=<" ) )
                {
                    String id = contigField( lower, "id" );
                    if( "1".equals( id ) || "chr1".equals( id ) )
                    {
                        String build = CHR1_LENGTH_BUILDS.get( contigField( lower, "length" ) );
                        if( build != null )
                            return build;
                    }
                    String assembly = contigField( lower, "assembly" );
                    if( fromContig == null && assembly != null )
                        fromContig = normalizeGenomeBuild( assembly );
                }
            }
            return fromReference != null ? fromReference : fromContig;
        }
        catch( Exception e )
        {
            return null;
        }
    }

    private static final Map<String, java.util.regex.Pattern> CONTIG_FIELDS = new HashMap<>();
    static
    {
        for( String field : new String[] {"id", "length", "assembly"} )
            CONTIG_FIELDS.put( field, java.util.regex.Pattern.compile( "[<,]" + field + "=([^,>]*)" ) );
    }

    /** The value of a field (id, length or assembly) of a lower-cased {@code ##contig=<ID=chr1,length=...>} line, or null. */
    private static String contigField(String line, String field)
    {
        java.util.regex.Matcher m = CONTIG_FIELDS.get( field ).matcher( line );
        return m.find() ? m.group( 1 ).trim() : null;
    }

    /** Omics type ("Genomics", ...) recorded for an element in its generic parent folder, or null. */
    public static String getOmicsType(DataElement de)
    {
        try
        {
            ru.biosoft.access.generic.GenericDataCollection parent = getGenericParent( de );
            return parent == null ? null : parent.getChildProperty( de.getName(), OMICS_TYPE_PROPERTY );
        }
        catch( Exception e )
        {
            return null;
        }
    }

    /** Record the omics type of an element in its generic parent folder (no-op for other parents). */
    public static void setOmicsType(DataElement de, String omicsType)
    {
        try
        {
            ru.biosoft.access.generic.GenericDataCollection parent = getGenericParent( de );
            if( parent != null && omicsType != null )
                parent.setChildProperty( de.getName(), OMICS_TYPE_PROPERTY, omicsType );
        }
        catch( Exception e )
        {
        }
    }

    /** Give {@code to} the omics type of {@code from}, when {@code from} has one and {@code to} has none. */
    public static void copyOmicsType(DataElement from, DataElement to)
    {
        String omicsType = getOmicsType( from );
        if( omicsType != null && getOmicsType( to ) == null )
            setOmicsType( to, omicsType );
    }

    private static ru.biosoft.access.generic.GenericDataCollection getGenericParent(DataElement de) throws Exception
    {
        DataCollection<?> origin = de == null ? null : de.getOrigin();
        if( origin == null )
            return null;
        Object primary = ru.biosoft.access.security.SecurityManager
                .runPrivileged( () -> ru.biosoft.access.DataCollectionUtils.fetchPrimaryCollectionPrivileged( origin ) );
        return primary instanceof ru.biosoft.access.generic.GenericDataCollection ? (ru.biosoft.access.generic.GenericDataCollection)primary : null;
    }
}
