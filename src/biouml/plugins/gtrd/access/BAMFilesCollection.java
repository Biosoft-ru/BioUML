package biouml.plugins.gtrd.access;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nonnull;

import biouml.plugins.ensembl.access.EnsemblDatabase;
import biouml.plugins.ensembl.access.EnsemblDatabaseSelector;
import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.standard.type.Species;
import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.BAMTrack;
import ru.biosoft.bsa.Track;

public class BAMFilesCollection extends AbstractDataCollection<BAMTrack>
{
    public static final String EXPERIMENTS_PATH_PROPERTY = "experimentsPath";


    private String folderPath;
    private @Nonnull List<String> nameList = new ArrayList<>();
    private Map<String, GenomeInfo> genomeByAlignsId = new HashMap<>();
    
    private Map<String, GenomeInfo> genomes = new HashMap<>();

    public BAMFilesCollection(DataCollection<?> parent, Properties properties) throws IOException
    {
        super( parent, properties );
        folderPath = properties.getProperty( DataCollectionConfigConstants.FILE_PATH_PROPERTY );
        if( folderPath == null )
            throw new IllegalArgumentException( "No " + DataCollectionConfigConstants.FILE_PATH_PROPERTY + " specified" );
        File folder = new File( folderPath );
        if( !folder.exists() || !folder.isDirectory() )
            throw new IllegalArgumentException( "Folder " + folderPath + " not exists" );
        
        Set<String> files = new HashSet<>();
        for( String file : folder.list() )
            if( file.endsWith( ".bam" ) )
                files.add( file );

        String expPath = properties.getProperty( EXPERIMENTS_PATH_PROPERTY, "databases/GTRD/Data/experiments" );
        DataCollection<ChIPseqExperiment> experimentsCollection = DataElementPath.create( expPath ).getDataCollection( ChIPseqExperiment.class );
        
        
        Path chrMappingFolder = Paths.get( properties.getProperty( "chrMappingFolder", "chr_mapping" ) );
        if(!chrMappingFolder.isAbsolute())
            chrMappingFolder = Paths.get( path ).resolve( chrMappingFolder );
        
        for( ChIPseqExperiment exp : experimentsCollection )
        {
            String bamFile = exp.getAlignment().getName();
            if(!files.contains( bamFile ))
                continue;
            
            Species species = exp.getSpecie();
            GenomeInfo genome = genomes.get( species.getLatinName() );
            if(genome == null)
            {
                EnsemblDatabase ensembl = EnsemblDatabaseSelector.getDefaultEnsembl( species );
                File chrMappingFile = chrMappingFolder.resolve( species.getLatinName().replace( ' ', '_' ).toLowerCase() + "_" + ensembl.getVersion() + ".txt" ).toFile();
                genome = new GenomeInfo( species.getLatinName(), ensembl.getVersion(), ensembl.getPrimarySequencesPath(), chrMappingFile );
                genomes.put( species.getLatinName(), genome );
                
            }
            genomeByAlignsId.put( bamFile, genome );
        }

        nameList = new ArrayList<>(genomeByAlignsId.keySet());
        Collections.sort( nameList );
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        return nameList;
    }

    @Override
    protected BAMTrack doGet(String name) throws Exception
    {
        GenomeInfo genome = genomeByAlignsId.get( name );
        if(genome == null)
            return null;

        Properties properties = new Properties();
        properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
        properties.setProperty( DataCollectionConfigConstants.FILE_PATH_PROPERTY, folderPath );
        properties.setProperty( DataCollectionConfigConstants.FILE_PROPERTY, name );
        properties.setProperty( BAMTrack.BAM_INDEX_FILE_PROPERTY, name + ".bai" );
        properties.setProperty( Track.SEQUENCES_COLLECTION_PROPERTY, genome.seqCollectionPath.toString() );

        return new GTRDBAMTrack( this, properties, genome.fromEnsembl, genome.toEnsembl );
    }

    @Override
    public @Nonnull Class<? extends DataElement> getDataElementType()
    {
        return BAMTrack.class;
    }
    
    private static class GenomeInfo
    {
        String species;
        String ensemblVersion;
        DataElementPath seqCollectionPath;
        
        Map<String, String> fromEnsembl = new HashMap<>();
        Map<String, String> toEnsembl = new HashMap<>();
        
        GenomeInfo(String species, String ensemblVersion, DataElementPath seqCollectionPath, File chrMappingFile) throws IOException
        {
            this.species = species;
            this.ensemblVersion = ensemblVersion;
            this.seqCollectionPath = seqCollectionPath;
            if(chrMappingFile != null)
                loadChrMapping( chrMappingFile );
        }

        private void loadChrMapping(File file) throws IOException
        {
            try (BufferedReader reader = new BufferedReader( new FileReader( file ) ))
            {
                String line;
                while( ( line = reader.readLine() ) != null )
                {
                    String[] parts = line.split( "\t", 2 );
                    toEnsembl.put( parts[0], parts[1] );
                }
            }
            toEnsembl.forEach( (k, v) -> fromEnsembl.put( v, k ) );
        }
    }
}
