package biouml.plugins.ensembl.access;

import java.sql.Connection;
import java.util.Properties;

import biouml.plugins.ensembl.type.Gene;
import biouml.plugins.ensembl.type.Transcript;
import biouml.standard.type.Species;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.access.sql.SqlConnectionHolder;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.access.support.SerializableAsText;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.VariationElement;

public class EnsemblDatabase implements SqlConnectionHolder, SerializableAsText
{
    private DataElementPath path;
    private String version;
    private Species specie;
    private String genomeBuild;

    public EnsemblDatabase(DataElementPath path)
    {
        this.path = path;
        
        DataCollection<?> root = path.getDataElement( ru.biosoft.access.core.DataCollection.class );
        Properties properties = root.getInfo().getProperties();
        
        version = properties.getProperty( "version" );
        if(version == null)
            throw new DataElementReadException(path, "ensembl version");
        
        String speciesStr = properties.getProperty( "species" );
        if(speciesStr == null)
            throw new DataElementReadException( path, "ensembl specie" );
        specie = Species.getSpecies( speciesStr );
        if(specie == null)
            throw new DataElementReadException( Species.SPECIES_PATH, speciesStr );
        
        genomeBuild = properties.getProperty( "genomeBuild" );
    }
    
    public EnsemblDatabase(String pathStr)
    {
        this( DataElementPath.create( pathStr ) );
    }
    
    public DataElementPath getPath()
    {
        return path;
    }
    
    public String getVersion()
    {
        return version;
    }

    public Species getSpecie()
    {
        return specie;
    }

    public String getGenomeBuild()
    {
        return genomeBuild;
    }

    public DataElementPath getPrimarySequencesPath()
    {
        return TrackUtils.getPrimarySequencesPath( path );
    }
    
    public DataCollection<Gene> getGenesCollection()
    {
        return path.getChildPath("Data", "gene").getDataCollection(Gene.class);
    }
    
    public DataCollection<Transcript> getTranscriptsCollection()
    {
        return path.getChildPath("Data", "transcript").getDataCollection(Transcript.class);
    }
    
    public DataCollection<VariationElement> getVariationCollection()
    {
        return TrackUtils.getVariationCollection( path );
    }
    
    public Track getGenesTrack()
    {
        return path.getChildPath( "Tracks", "Genes" ).getDataElement( Track.class );
    }
    
    public Track getTranscriptsTrack()
    {
        return path.getChildPath( "Tracks", "Transcripts" ).getDataElement( Track.class );
    }
    
    public Track getVariationTrack()
    {
        return path.getChildPath( "Tracks", "Variations" ).getDataElement( Track.class );
    }
    
    @Override
    public Connection getConnection() throws BiosoftSQLException
    {
        Properties properties = path.getDataCollection().getInfo().getProperties();
        Connection persistentConnection = SqlConnectionPool.getPersistentConnection( properties );
        SqlUtil.checkConnection(persistentConnection);
        return persistentConnection;
    }

    @Override
    public String toString()
    {
        return specie.getCommonName() + version + (genomeBuild == null ? "" : " (" + genomeBuild + ")");
    }
    
    @Override
    public String getAsText()
    {
        return path.toString();
    }
}