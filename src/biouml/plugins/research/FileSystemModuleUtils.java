package biouml.plugins.research;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import biouml.model.Module;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.Repository;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.support.BeanInfoEntryTransformer;
import ru.biosoft.fs.FileSystemCollection;
import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.util.ExProperties;

/**
 * Builder for research modules
 */
public class FileSystemModuleUtils
{
    private static String DESCRIPTION_FILE = "description.html";

    /**
     * Create research in {@link Repository}
     */
    public static DataCollection<?> createResearch(File target, Repository parent, String name) throws Exception
    {
        // Create Module data collection (root)
        Properties primary = new ExProperties();
        primary.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, name);
        primary.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName());

        Properties transformed = new ExProperties();
        transformed.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, Module.class.getName());
        transformed.setProperty(Module.TYPE_PROPERTY, ResearchModuleType.class.getName());
        transformed.setProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY, "biouml.plugins.research");
        transformed.setProperty(DataCollectionConfigConstants.REMOVE_CHILDREN, "true");
        transformed.setProperty(DataCollectionConfigConstants.DESCRIPTION_PROPERTY, DESCRIPTION_FILE);

        Module research = (Module)CollectionFactoryUtils.createDerivedCollection(parent, name, primary, transformed, name);
        new File(research.getPath(), DESCRIPTION_FILE).createNewFile();
        LocalRepository researchLR = (LocalRepository)research.getPrimaryCollection();
        
        primary = new ExProperties();
        primary.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, Module.DATA);
        primary.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, FileSystemCollection.class.getName());
        primary.setProperty(FileSystemCollection.FILE_SYSTEM_PROPERTY, "local");
        primary.setProperty(FileSystemCollection.FILE_SYSTEM_PROPERTIES_PREFIX+DataCollectionConfigConstants.FILE_PATH_PROPERTY, target.getCanonicalPath());
        researchLR.createDataCollection( Module.DATA, primary, null, null, null );

        primary = new ExProperties();
        primary.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, "tmp");
        primary.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, FileSystemCollection.class.getName());
        primary.setProperty(FileSystemCollection.FILE_SYSTEM_PROPERTY, "local");
        researchLR.createDataCollection( "tmp", primary, "tmp", null, null );
        Path dataDir = researchLR.getRootDirectory().toPath().resolve( "tmp" ).resolve( Module.DATA );
        if(!Files.isDirectory( dataDir ))
            Files.createDirectory( dataDir );

        // create journal collection
        CollectionFactoryUtils.createTransformedCollection(researchLR, "Journal", BeanInfoEntryTransformer.class, TaskInfo.class, null, null,
                ".dat", "ID", "ID", "//", null);
        DataCollection<?> journalDC = researchLR.get("Journal");
        journalDC.getInfo().writeProperty(DataCollectionConfigConstants.IS_LEAF, "true");
        journalDC.getInfo().writeProperty(DataCollectionConfigConstants.NODE_IMAGE, ClassLoading.getResourceLocation( FileSystemModuleUtils.class, "resources/journal.gif" ));

        return research;
    }
    
    /**
     * 
     * @param module
     * @return target directory if module is FileSystem module; null otherwise
     */
    public static File getTargetDirectory(Module module)
    {
        try
        {
            DataElement dataDE = module.get( Module.DATA );
            if(dataDE instanceof FileSystemCollection)
            {
                String filePath = ( (FileSystemCollection)dataDE ).getInfo().getProperty(
                        FileSystemCollection.FILE_SYSTEM_PROPERTIES_PREFIX + DataCollectionConfigConstants.FILE_PATH_PROPERTY );
                if(filePath == null)
                {
                    return null;
                }
                return new File(filePath);
            }
        }
        catch( Exception e )
        {
        }
        return null;
    }
    
    public static File canonicalize(File file)
    {
        try
        {
            return file.getCanonicalFile();
        }
        catch( IOException e )
        {
            // Ignore
            return file;
        }
    }
    
    @SuppressWarnings ( "unchecked" )
    public static Module getModuleByTarget(File target, DataCollection<?> modulesCollection)
    {
        if(target == null || modulesCollection == null)
        {
            return null;
        }
        File dir = canonicalize( target );
        return ( (DataCollection<Module>)modulesCollection ).stream( Module.class )
                .filter( module -> dir.equals( getTargetDirectory( module ) ) ).findFirst().orElse( null );
    }
}
