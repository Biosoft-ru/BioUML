package biouml.plugins.gtrd.master.collections;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import biouml.plugins.ensembl.access.EnsemblDatabase;
import biouml.plugins.ensembl.access.EnsemblDatabaseSelector;
import biouml.plugins.gtrd.CellLine;
import biouml.plugins.gtrd.master.meta.Metadata;
import biouml.plugins.gtrd.master.sites.bedconv.BedEntryToPeak;
import biouml.standard.type.Species;
import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.ChrNameMapping;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.track.big.BedEntryConverter;
import ru.biosoft.bsa.track.big.BigBedTrack;

public abstract class ClustersCollection<T> extends AbstractDataCollection< BigBedTrack<? extends T> >
{
    public static final String PROP_CELLS_PATH = "CellsPath";
    
    private String folderPath;
    private @Nonnull List<String> nameList = new ArrayList<>();
    
    private DataElementPath metadataPath;
    private DataElementPath cellsPath;
    
    public ClustersCollection(DataCollection<?> parent, Properties properties) throws IOException
    {
        super( parent, properties );
        folderPath = properties.getProperty( DataCollectionConfigConstants.FILE_PATH_PROPERTY );
        if( folderPath == null )
            throw new IllegalArgumentException( "No " + DataCollectionConfigConstants.FILE_PATH_PROPERTY + " specified" );
        File folder = new File( folderPath );
        if( !folder.exists() || !folder.isDirectory() )
            throw new IllegalArgumentException( "Folder " + folderPath + " not exists" );
        
        metadataPath = DataElementPath.create( properties.getProperty( BedEntryToPeak.PROP_METADATA_PATH ) );
        cellsPath = DataElementPath.create( properties.getProperty( PROP_CELLS_PATH, "databases/GTRD/Dictionaries/cells" ));
        
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

    //DNase-seq_from_cell_id_2865_MACS2.bb
    private static final Pattern NAME_PATTERN = Pattern.compile( "(.*)_from_cell_id_([0-9]+)_([^.]*)([.]v[0-9]+)?[.]bb" );
    
    @Override
    protected BigBedTrack<T> doGet(String name) throws Exception
    {
        Matcher matcher = NAME_PATTERN.matcher( name );
        if(!matcher.matches())
            return null;

        String expType = matcher.group( 1 );
        String cellId = matcher.group( 2 );
        String peakCaller = matcher.group( 3 ).toLowerCase();
        
        return doGet(name, expType, cellId, peakCaller);
    }
    
    protected BigBedTrack<T> doGet(String name, String expType, String cellId, String peakCaller) throws Exception
    {
        File file = new File(folderPath, name);
        if(!file.exists())
            return null;
        
        CellLine cell = findCell( cellId );
        if(cell == null)
        {
            log.warning( "Can not find " + cellId + " for " + name );
            return null;
        }
        
        Species organism = cell.getSpecies();
        EnsemblDatabase ensembl = EnsemblDatabaseSelector.getDefaultEnsembl( organism );
        
        Properties properties = new Properties();
        properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
        properties.setProperty( DataCollectionConfigConstants.FILE_PATH_PROPERTY, folderPath );
        Path bbPath = Paths.get(folderPath, name);
        properties.setProperty( BigBedTrack.PROP_BIGBED_PATH, bbPath.toString() );
        properties.setProperty( Track.SEQUENCES_COLLECTION_PROPERTY, ensembl.getPrimarySequencesPath().toString() );
        String mapping = getInfo().getProperty( ChrNameMapping.PROP_CHR_MAPPING );
        if(mapping != null)
            properties.setProperty( ChrNameMapping.PROP_CHR_MAPPING, mapping );
        
        BigBedTrack<T> result =  new BigBedTrack<>( this, properties, false );
        
        BedEntryConverter<T> converter = getConverter( expType, peakCaller, cell, result );
        result.setConverter( converter );
        
        result.open();
        return result;
    }
    

    protected abstract BedEntryConverter<T> getConverter(String expType, String peakCaller, CellLine cell, BigBedTrack<T> result);
    
    private CellLine findCell(String id)
    {
        if(metadataPath != null)
        {
            Metadata meta = metadataPath.getDataElement( Metadata.class );
            return meta.cells.get( id );
        }else if(cellsPath != null)
        {
            return cellsPath.getChildPath( id ).optDataElement( CellLine.class );
        }
        else
            return null;
    }

    @Override
    public @Nonnull Class<? extends DataElement> getDataElementType()
    {
        return BigBedTrack.class;
    }
}
