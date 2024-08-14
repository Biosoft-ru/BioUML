package biouml.plugins.gtrd.master.collections;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.bsa.ChrNameMapping;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.track.big.BigTrack;
import ru.biosoft.bsa.track.big.BigWigTrack;

public class BigWigTracksCollection extends AbstractDataCollection<BigWigTrack>
{
    private String folderPath;
    private List<String> nameList = new ArrayList<>();
    
    public BigWigTracksCollection(DataCollection<?> parent, Properties properties)
    {
        super( parent, properties );
        folderPath = properties.getProperty( DataCollectionConfigConstants.FILE_PATH_PROPERTY );
        if( folderPath == null )
            throw new IllegalArgumentException( "No " + DataCollectionConfigConstants.FILE_PATH_PROPERTY + " specified" );
        File folder = new File( folderPath );
        if( !folder.exists() || !folder.isDirectory() )
            throw new IllegalArgumentException( "Folder " + folderPath + " not exists" );
        
        nameList.addAll( Arrays.asList( folder.list() ) );
        Collections.sort( nameList );
    }
    
    @Override
    public List<String> getNameList()
    {
        return nameList;
    }

    @Override
    protected BigWigTrack doGet(String name) throws Exception
    {
        Properties properties = new Properties();
        properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
        properties.setProperty( BigTrack.PROP_BIGBED_PATH, new File(folderPath, name).getAbsolutePath() );
        
        String seqBase = getInfo().getProperty( Track.SEQUENCES_COLLECTION_PROPERTY );
        if(seqBase != null)
            properties.setProperty( Track.SEQUENCES_COLLECTION_PROPERTY, seqBase );
        
        String chrMapping = getInfo().getProperty( ChrNameMapping.PROP_CHR_MAPPING );
        if(chrMapping != null)
            properties.setProperty( ChrNameMapping.PROP_CHR_MAPPING, chrMapping );
        
        return new BigWigTrack( this, properties );
    }
    
    @Override
    public @Nonnull Class<? extends DataElement> getDataElementType()
    {
        return BigWigTrack.class;
    }
}
