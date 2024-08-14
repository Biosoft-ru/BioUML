package biouml.plugins.gtrd.master.collections;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import biouml.plugins.gtrd.master.MasterTrack;
import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.bsa.track.big.BigBedTrack;

public class MasterTrackCollection extends AbstractDataCollection<MasterTrack>
{

    private String folderPath;
    private List<String> nameList;
    
    public MasterTrackCollection(DataCollection<?> parent, Properties properties)
    {
        super( parent, properties );
        folderPath = properties.getProperty( DataCollectionConfigConstants.FILE_PATH_PROPERTY );
        if( folderPath == null )
            throw new IllegalArgumentException( "No " + DataCollectionConfigConstants.FILE_PATH_PROPERTY + " specified" );
        File folder = new File( folderPath );
        if( !folder.exists() || !folder.isDirectory() )
            throw new IllegalArgumentException( "Folder " + folderPath + " not exists" );
        
        nameList = new ArrayList<>();
        for( String file : folder.list() )
            if( file.endsWith( ".bb" ) )
                nameList.add( file );
        Collections.sort( nameList );
    }

    @Override
    public List<String> getNameList()
    {
        return nameList;
    }
    
    @Override
    protected MasterTrack doGet(String name) throws Exception
    {
        Path bbPath = Paths.get(folderPath, name);
        if(!Files.exists( bbPath ))
            return null;
        
        Properties properties = new Properties();//allow to pass properties from parent
        properties.putAll( getInfo().getProperties() );
        
        properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
        properties.setProperty( BigBedTrack.PROP_BIGBED_PATH, bbPath.toString() );
        
        return new MasterTrack( this, properties );
    }
    
    @Override
    public @Nonnull Class<? extends DataElement> getDataElementType()
    {
        return MasterTrack.class;
    }
    
}
