package ru.biosoft.bsa.importer;

import java.io.File;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.PropertyDescriptorEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.Repository;
import ru.biosoft.access.security.Permission;
import ru.biosoft.bsa.BAMTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.SubFunctionJobControl;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.TextUtil;


public class SAMBAMTrackImporter implements DataElementImporter
{
    protected ImporterProperties properties = null;

    @Override
    public int accept(DataCollection<?> parent, File file)
    {
        if( parent == null || !parent.isMutable() || !DataCollectionUtils.isAcceptable( parent, getResultType() ) )
            return ACCEPT_UNSUPPORTED;
        if( file == null || file.getName().toLowerCase().endsWith(".sam") || file.getName().toLowerCase().endsWith(".bam") ||
        file.getName().toLowerCase().endsWith(".cram"))
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
            trackProperties.put(DataCollectionConfigConstants.CLASS_PROPERTY, BAMTrack.class.getName());
            trackProperties.put(DataCollectionConfigConstants.FILE_PROPERTY, name);
            if(properties.getSequenceCollectionPath() != null)
                trackProperties.put(Track.SEQUENCES_COLLECTION_PROPERTY, properties.getSequenceCollectionPath().toString());
            if( !TextUtil.isEmpty(properties.getGenomeId()) )
                trackProperties.put(Track.GENOME_ID_PROPERTY, properties.getGenomeId());

            BAMTrack track;
            DataCollection typeSpecificCollection = DataCollectionUtils.getTypeSpecificCollection(parent, BAMTrack.class);
            if(typeSpecificCollection instanceof Repository)
            {
                Repository parentRepository = (Repository)typeSpecificCollection;
                track = (BAMTrack)DataCollectionUtils.fetchPrimaryCollection(parentRepository.createDataCollection(name, trackProperties, null, null, null), Permission.WRITE);
            }
            else
            {
                File bamFolder = TempFiles.dir( ".bam_folder" );
                trackProperties.put( DataCollectionConfigConstants.FILE_PATH_PROPERTY, bamFolder.getAbsolutePath() );
                track = new BAMTrack( parent, trackProperties );
            }

            File indexFile = BAMTrack.getIndexFile(fileToImport);

            JobControl copyBAMJC = jobControl;
            if(jobControl != null && properties.getCreateIndex() && !indexFile.exists())
                copyBAMJC = new SubFunctionJobControl( jobControl, 0, 50 );
            ApplicationUtils.linkOrCopyFile(track.getBAMFile(), fileToImport, copyBAMJC);

            if(indexFile.exists())
                ApplicationUtils.linkOrCopyFile(track.getIndexFile(), indexFile, null);

            if( properties.getCreateIndex() && !indexFile.exists() )
                try
                {
                    track.createIndex(jobControl == null ? null : new SubFunctionJobControl( jobControl, 50, 100 ));
                }
                catch( Exception e )
                {
                    log.log(Level.WARNING, "Can not create index for " + DataElementPath.create(track), e);
                }

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
    public ImporterProperties getProperties(DataCollection<?> parent, File file, String elementName)
    {
        return getProperties();
    }

    protected synchronized ImporterProperties getProperties()
    {
        if( properties == null )
            properties = new ImporterProperties();
        return properties;
    }

    @Override
    public boolean init(Properties properties)
    {
        return true;
    }

    @SuppressWarnings ( "serial" )
    public static class ImporterProperties extends TrackImportProperties
    {
        private boolean createIndex = true;

        public boolean getCreateIndex()
        {
            return createIndex;
        }

        public void setCreateIndex(boolean createIndex)
        {
            boolean oldValue = this.createIndex;
            this.createIndex = createIndex;
            firePropertyChange("createIndex", oldValue, createIndex);
        }
    }

    public static class ImporterPropertiesBeanInfo extends TrackImportPropertiesBeanInfo
    {
        public ImporterPropertiesBeanInfo()
        {
            super(ImporterProperties.class);
        }

        public ImporterPropertiesBeanInfo(Class<? extends ImporterProperties> beanClass)
        {
            super( beanClass );
        }

        @Override
        public void initProperties() throws Exception
        {
            super.initProperties();
            add(new PropertyDescriptorEx("createIndex", beanClass), getResourceString("PN_SAMBAMTRACKIMPORT_CREATE_INDEX"), getResourceString("PD_SAMBAMTRACKIMPORT_CREATE_INDEX"));
        }
    }

    @Override
    public Class<? extends DataElement> getResultType()
    {
        return BAMTrack.class;
    }
}
