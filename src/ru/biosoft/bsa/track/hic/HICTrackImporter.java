package ru.biosoft.bsa.track.hic;

import java.io.File;
import java.util.Properties;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.Repository;
import ru.biosoft.access.security.Permission;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.importer.TrackImportProperties;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.TextUtil2;

public class HICTrackImporter implements DataElementImporter
{
    private final TrackImportProperties properties = new TrackImportProperties();

    @Override
    public int accept(DataCollection<?> parent, File file)
    {
        if( parent == null || !parent.isMutable() || !DataCollectionUtils.isAcceptable( parent, getResultType() )
                || ! ( DataCollectionUtils.getTypeSpecificCollection( parent, getResultType() ) instanceof Repository ) )
            return ACCEPT_UNSUPPORTED;
        if( file == null || file.getName().toLowerCase().endsWith(".hic") )
            return ACCEPT_HIGHEST_PRIORITY;
        return ACCEPT_UNSUPPORTED;
    }

    @Override
    public DataElement doImport(@Nonnull DataCollection parent, @Nonnull File fileToImport, String elementName,
            FunctionJobControl jobControl, Logger log) throws Exception
    {
        if( jobControl != null )
            jobControl.functionStarted();

        String name = elementName;
        if( name == null || name.equals("") )
            name = fileToImport.getName();
        name = name.replaceAll("\\/", "");

        if( parent.contains(name) )
            parent.remove(name);

        try
        {
            Properties trackProperties = new ExProperties();
            trackProperties.put(DataCollectionConfigConstants.NAME_PROPERTY, name);
            trackProperties.put(DataCollectionConfigConstants.CLASS_PROPERTY, HICTrack.class.getName());
            if(properties.getSequenceCollectionPath() != null)
                trackProperties.put(Track.SEQUENCES_COLLECTION_PROPERTY, properties.getSequenceCollectionPath().toString());
            if( !TextUtil2.isEmpty(properties.getGenomeId()) )
                trackProperties.put(Track.GENOME_ID_PROPERTY, properties.getGenomeId());
            trackProperties.putAll( properties.getTrackProperties() );

            HICTrack track;
            DataCollection typeSpecificCollection = DataCollectionUtils.getTypeSpecificCollection(parent, HICTrack.class);
            if(typeSpecificCollection instanceof Repository)
            {
                Repository parentRepository = (Repository)typeSpecificCollection;
                track = (HICTrack)DataCollectionUtils.fetchPrimaryCollection(parentRepository.createDataCollection(name, trackProperties, null, null, null), Permission.WRITE);
            }
            else
            {
                File bamFolder = TempFiles.dir( ".bb_folder" );
                trackProperties.put( DataCollectionConfigConstants.FILE_PATH_PROPERTY, bamFolder.getAbsolutePath() );
                track = new HICTrack( parent, trackProperties );
            }

            ApplicationUtils.linkOrCopyFile(new File(track.getFilePath()), fileToImport, jobControl);
            parent.put(track);
        }
        catch( Exception e )
        {
            if( jobControl != null )
            {
                jobControl.functionTerminatedByError(e);
                return null;
            }
            else
                throw e;
        }
        if( jobControl != null )
        {
            jobControl.setPreparedness(100);
            jobControl.functionFinished();
        }
        return parent.get(name);
    }

    @Override
    public TrackImportProperties getProperties(DataCollection<?> parent, File file, String elementName)
    {
        return properties;
    }

    @Override
    public boolean init(Properties properties)
    {
        return true;
    }


    @Override
    public Class<? extends DataElement> getResultType()
    {
        return HICTrack.class;
    }
}
