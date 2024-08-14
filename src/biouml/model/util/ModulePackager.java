package biouml.model.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.JDialog;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Module;
import biouml.model.ProtectedModule;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.CreateDataCollectionController;
import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.Repository;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.util.ExProperties;

/** @todo comments */
public class ModulePackager
{
    public static final String MF_DATABASE_NAME = "Module-Name";
    public static final String MF_DATABASE_VERSION = "Module-Version";
    public static final String MF_DATABASE_DESCRIPTION = "Module-Description";
    public static final String MF_DATABASE_ACTIVATOR_CLASS = "Module-Activator-Class";
    public static final String MF_DATABASE_TYPE_CLASS = "Module-Type-Class";
    public static final String MF_DATABASE_PARENT = "Module-Parent-Name";

    public static final String BMD_FILE_EXTENTION = ".bmd";

    public static final String INFO_FILENAME = "info";
    public static final String LICENSE_FILENAME = "license";

    private static final String MANIFEST_DIR = "META-INF/";
    private static final String MANIFEST_FILE = MANIFEST_DIR + "manifest.mf";

    protected static final Logger log = Logger.getLogger(ModulePackager.class.getName());
    protected static final MessageBundle messageBundle = new MessageBundle();

    /**
     * Export the specified module into distributive file.
     * @pending it is suggested that moduleName, moduleVersion, moduleDescriptor and fileName are not nulls.
     * @todo comments
     */
    public static void exportModule(DataCollection module, String moduleName, String moduleVersion, String moduleDescription,
            String filename, FunctionJobControl jobControl, Set<String> excludedNames)
    {

        if( excludedNames == null )
        {
            excludedNames = new HashSet<>();
        }
        excludedNames.add("CVS");
        excludedNames.add(ModulePackager.INFO_FILENAME);

        String exportModuleMessage = MessageFormat.format(messageBundle.getResourceString("EXPORT_DATABASE_MESSAGE"), new Object[] {
                module.getName(), filename});
        log.info(exportModuleMessage);
        if( jobControl != null )
            jobControl.functionStarted();

        JarOutputStream jarOutputStream = null;
        DataElementPath oldPath = DataElementPath.create(module);
        File moduleDir = ModulePackager.getModuleDir(module.getCompletePath());
        String nextConfig = null;
        try
        {
            Manifest manifest = ModulePackager.getModuleManifest(module.getCompletePath());
            Attributes attr = null;
            if( manifest != null )
            {
                attr = manifest.getMainAttributes();
                attr.remove(ModulePackager.MF_DATABASE_ACTIVATOR_CLASS);
            }
            else
            {
                manifest = new Manifest();
                attr = manifest.getMainAttributes();
                attr.put(Attributes.Name.MANIFEST_VERSION, "1.0");
            }

            attr.putValue(ModulePackager.MF_DATABASE_NAME, moduleName);
            attr.putValue(ModulePackager.MF_DATABASE_VERSION, moduleVersion);
            attr.putValue(ModulePackager.MF_DATABASE_DESCRIPTION, moduleDescription);
            attr.putValue(ModulePackager.MF_DATABASE_PARENT, oldPath.getParentPath().toString());
            if( module instanceof Module )
                attr.putValue(ModulePackager.MF_DATABASE_TYPE_CLASS, ( (Module)module ).getType().getClass().getName());

            File outputFile = new File(filename);
            if( !outputFile.getName().toLowerCase().endsWith(BMD_FILE_EXTENTION) )
                outputFile = new File(outputFile.getPath() + BMD_FILE_EXTENTION);

            FileOutputStream out = new FileOutputStream(outputFile);
            jarOutputStream = new JarOutputStream(out, manifest);
            jarOutputStream.setMethod(ZipOutputStream.DEFLATED);

            File configFile = new File(moduleDir, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE);
            Properties properties = new ExProperties(configFile);
            nextConfig = properties.getProperty(DataCollectionConfigConstants.NEXT_CONFIG);
            if( nextConfig != null && !module.getName().equals(moduleName) )
                setProperty(moduleDir, nextConfig, DataCollectionConfigConstants.NAME_PROPERTY, moduleName);
        

            if( !module.getName().equals(moduleName) )
            {
                setProperty(moduleDir, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE, DataCollectionConfigConstants.NAME_PROPERTY, moduleName);
                File defaultRepository = new File(moduleDir, DataCollectionConfigConstants.DEFAULT_REPOSITORY);
                if( defaultRepository.exists() )
                    setProperty(moduleDir, DataCollectionConfigConstants.DEFAULT_REPOSITORY, DataCollectionConfigConstants.NAME_PROPERTY, moduleName);
            }

            int excludedPrefixPathSize = moduleDir.getPath().length() + 1;
            List<File> files = getFiles(moduleDir, excludedNames);

            int count = 0;
            for(File file: files)
            {
                String fileStr = MessageFormat.format(messageBundle.getResourceString("ADDING_FILE"), new Object[] {file.getName()});
                log.info(fileStr);
                addToJar(jarOutputStream, excludedPrefixPathSize, file);

                count++;
                if( jobControl != null )
                    jobControl.setPreparedness(100 * count / files.size());
            }

            String message = messageBundle.getResourceString("DATABASE_EXPORTED_SUCCESSFULLY");
            log.info(message);
            if( jobControl != null )
                jobControl.functionFinished(message);
        }
        catch( Throwable t )
        {
            String message = MessageFormat.format(messageBundle.getResourceString("DATABASE_EXPORT_ERROR"), new Object[] {t.getMessage(),
                    module.getName()});
            log.log(Level.SEVERE, message, t);

            if( jobControl != null )
                jobControl.functionTerminatedByError(t);
        }
        finally
        {
            try
            {
                if( jarOutputStream != null )
                {
                    jarOutputStream.close();
                }

                if( !module.getName().equals(moduleName) )
                {
                    //restores the config files
                    setProperty(moduleDir, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE, DataCollectionConfigConstants.NAME_PROPERTY, module.getName());
                    File defaultRepository = new File(moduleDir, DataCollectionConfigConstants.DEFAULT_REPOSITORY);
                    if( defaultRepository.exists() )
                        setProperty(moduleDir, DataCollectionConfigConstants.DEFAULT_REPOSITORY, DataCollectionConfigConstants.NAME_PROPERTY, module.getName());
                    if( nextConfig != null )
                        setProperty(moduleDir, nextConfig, DataCollectionConfigConstants.NAME_PROPERTY, module.getName());
                }
            }
            catch( IOException e )
            {
                String message = MessageFormat.format(messageBundle.getResourceString("DATABASE_EXPORT_ERROR"), new Object[] {
                        e.getMessage(), module.getName()});
                log.log(Level.SEVERE, message, e);
            }
        }
    }

    public static void setProperty(File moduleDir, String filename, String key, String value) throws IOException
    {
        File file = new File(moduleDir, filename);
        Properties properties = new ExProperties(file);
        properties.setProperty(key, value);
        ExProperties.store(properties, file);
    }

    static Module module = null;
    /**
     * Imports BioUML module from  module distributive.

     * @moduleFile module distributive file
     * @controller controls creating of data collection: <ul>
     * <li> responds whether all previous files should be replaced by new
     * if imported data collection is already exists </li>
     * <li>provides {@link JobControl}</li></ul>
     *
     * <h2>Implementation notes</h2>
     *
     * <h3>Imported data collection is already exists</h3>
     * Here we have only two possibilities: cancel or remove old collection and create new one.
     * We can not replace old data collection files because they can be locked by data collection.
     * <p>Valid responses of controller are {link CreateDataCollectionController.CANCEL} or
     * {link CreateDataCollectionController.REMOVE_ALL}, otherwise function will throw
     * <code>IllegalArgumentException</code>.
     * <p>If controller is null and data collection is already exists then function will
     * throw IOException.
     */

    public static void importModule(JDialog parentWindow, JarFile moduleFile, Repository repository,
            CreateDataCollectionController controller) throws Exception
    {
        FunctionJobControl jobControl = null;
        if( controller != null )
            jobControl = controller.getJobControl();
        importModule(parentWindow, moduleFile, repository, controller, jobControl);
    }
    public static void importModule(JDialog parentWindow, JarFile moduleFile, Repository repository,
            CreateDataCollectionController controller, FunctionJobControl jobControl) throws Exception
    {
        String importingModuleFromMessage = MessageFormat.format(messageBundle.getResourceString("IMPORTING_DATABASE_FROM"),
                new Object[] {moduleFile.getName()});
        log.info(importingModuleFromMessage);

        if( jobControl != null )
            jobControl.functionStarted();

        try
        {
            // check whether such module already exists
            String moduleName = getModuleAttribute(moduleFile, ModulePackager.MF_DATABASE_NAME);
            File moduleDir = getModuleDir(DataElementPath.create(getModuleAttribute(moduleFile, ModulePackager.MF_DATABASE_PARENT)), moduleName);
            if( ( (DataCollection<?>)repository ).contains( moduleName ) )
            {
                String message = MessageFormat.format(messageBundle.getResourceString("MESSAGE_DATABASE_EXIST"), new Object[] {moduleName});
                log.info(message);

                if( controller == null )
                    throw new IOException(message + " Controller is null.");

                int response = controller.fileAlreadyExists(moduleDir);
                if( response == CreateDataCollectionController.CANCEL )
                {
                    message = messageBundle.getResourceString("DATABASE_IMPORT_CANCELED");
                    log.info(message);
                    if( jobControl != null )
                        jobControl.functionFinished(message);

                    return;
                }

                if( response != CreateDataCollectionController.REMOVE_ALL && response != CreateDataCollectionController.OVERWRITE_ALL )
                {
                    throw new IllegalArgumentException("Old files can not be replaced by new in module " + moduleName);
                }
                CollectionFactoryUtils.getDatabases().remove(moduleName);
                log.info(messageBundle.getResourceString("OLD_DATABASE_VERSION_REMOVED"));
            }

            if( !moduleDir.exists() )
            {
                moduleDir.mkdir();
            }

            // create dirs first
            Enumeration<JarEntry> entries = moduleFile.entries();
            while( entries.hasMoreElements() )
            {
                JarEntry jarEntry = entries.nextElement();
                if( jarEntry.isDirectory() )
                {
                    String dirName = jarEntry.getName();
                    File dir = new File(moduleDir, dirName);
                    if( !dir.exists() && !dirName.equalsIgnoreCase(MANIFEST_DIR) )
                    {
                        String message = MessageFormat.format(messageBundle.getResourceString("CREATING_ITEM"),
                                new Object[] {dir.getName()});
                        log.info(message);
                        dir.mkdirs();
                    }
                }
            }

            // copy files
            int count = 0;
            StringBuilder classpath = new StringBuilder();
            entries = moduleFile.entries();
            while( entries.hasMoreElements() )
            {
                JarEntry jarEntry = entries.nextElement();
                String entryName = jarEntry.getName();
                if( !jarEntry.isDirectory() && !entryName.equalsIgnoreCase(MANIFEST_FILE) )
                {
                    String message = MessageFormat.format(messageBundle.getResourceString("EXTRACTING_ITEM"), new Object[] {entryName});
                    log.info(message);
                    File file = extractFile(moduleDir, entryName, moduleFile, jarEntry);

                    //put jar files to CollectionFactory classpath
                    if( file.getName().endsWith(".jar") )
                    {
                        if( classpath.length() > 0 )
                        {
                            classpath.append(File.pathSeparator);
                        }
                        classpath.append(file.getPath());
                    }
                }

                count++;
                if( jobControl != null )
                    jobControl.setPreparedness(100 * count / moduleFile.size());
            }

            // writing module info stored in the manifest to the file databases/module_name/info
            Manifest manifest = moduleFile.getManifest();
            if( manifest != null )
            {
                File moduleInfoFile = new File(moduleDir, INFO_FILENAME);
                if( !moduleInfoFile.exists() )
                {
                    try (OutputStream out = new BufferedOutputStream( new FileOutputStream( moduleInfoFile ) ))
                    {
                        manifest.write( out );
                    }
                }
            }

            // create module activator
            String moduleActivatorClassName = ModulePackager.getModuleAttribute(moduleFile, ModulePackager.MF_DATABASE_ACTIVATOR_CLASS);
            ModuleActivator moduleActivator = null;
            int whenCreateDataCollection = ModuleActivator.CREATE_DATA_COLLECTION_BEFORE;
            if( moduleActivatorClassName != null )
            {
                moduleActivator = ClassLoading.loadSubClass( moduleActivatorClassName, ModuleActivator.class ).newInstance();
                whenCreateDataCollection = moduleActivator.whenCreateDataCollection();
            }

            if( moduleActivator != null && whenCreateDataCollection == ModuleActivator.CREATE_DATA_COLLECTION_AFTER )
            {
                activateModule(parentWindow, moduleActivator, repository, moduleFile, classpath.toString(), controller);
            }

            // do not create module if ModulePackager.MF_DATABASE_CREATOR_CLASS is not found
            // just copy the content of BMD file
            if( moduleActivator == null || whenCreateDataCollection != ModuleActivator.CREATE_DATA_COLLECTION_NEVER )
            {
                File defaultConfig = new File(moduleDir, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE);
                Properties properties = new ExProperties(defaultConfig);
                String nextConfig = properties.getProperty(DataCollectionConfigConstants.NEXT_CONFIG);
                if( nextConfig != null )
                {
                    String className = properties.getProperty(DataCollectionConfigConstants.CLASS_PROPERTY);
                    if( ProtectedModule.class.getName().equals(className) )
                    {
                        File nextConfigFile = new File(moduleDir, nextConfig);
                        properties = new ExProperties(nextConfigFile);
                        nextConfigFile.delete();
                    }
                }

                CollectionFactoryUtils.createSubDirCollection(repository, moduleName, properties);
            }

            if( moduleActivator != null && whenCreateDataCollection != ModuleActivator.CREATE_DATA_COLLECTION_AFTER )
            {
                activateModule(parentWindow, moduleActivator, repository, moduleFile, classpath.toString(), controller);
            }
            String message = messageBundle.getResourceString("DATABASE_IMPORTED_SUCCESSFULLY");
            log.info(message);
            if( jobControl != null )
                jobControl.functionFinished(message);
        }
        catch( Exception t )
        {

            // if error occur we should remove the module
            /*
             if (module == null)
             {
             ApplicationUtils.removeDir(moduleDir);
             Logger.error(log, "Module " + module.getName() + " was not created");
             }
             */

            if( jobControl != null )
                jobControl.functionTerminatedByError(t);
            throw t;
        }
    }

    /**
     * @todo implement
     */
    private static void activateModule(JDialog parentWindow, ModuleActivator moduleActivator, Repository repository, JarFile moduleFile,
            String classpath, CreateDataCollectionController controller) throws Exception
    {
        module = moduleActivator.createModule(parentWindow, repository, moduleFile, classpath, log, controller);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Utility functions
    //

    /**
     * @pending hack version.
     */
    public static File getModuleDir(DataElementPath modulePath)
    {
        File moduleDir = null;
        DataCollection<?> module = modulePath.optDataCollection();
        if( module != null )
        {
            if( module instanceof Module )
            {
                moduleDir = ( (Module)module ).getPath();
            }
            else if( module instanceof LocalRepository )
            {
                moduleDir = ( (LocalRepository)module ).getRootDirectory();
            }
        }
        else
        {
            moduleDir = getModuleDir(modulePath.getParentPath(), modulePath.getName());
        }
        return moduleDir;
    }

    public static File getModuleDir(DataElementPath parentPath, String moduleName)
    {
        LocalRepository parent = parentPath.optDataElement(LocalRepository.class);
        if( parent != null )
        {
            File root = parent.getRootDirectory();
            return new File(root, moduleName);
        }
        //try old style
        File rootModulesDir = ( (LocalRepository)CollectionFactoryUtils.getDatabases() ).getRootDirectory();
        if( !rootModulesDir.exists() )
        {
            rootModulesDir.mkdir();
        }
        return new File(rootModulesDir, moduleName);
    }

    public static Manifest getModuleManifest(DataElementPath modulePath)
    {
        Manifest manifest = null;
        File moduleDir = ModulePackager.getModuleDir(modulePath);
        File infoFile = new File(moduleDir, ModulePackager.INFO_FILENAME); // this is the manifest
        if( infoFile.exists() && infoFile.isFile() )
        {
            try (BufferedInputStream in = new BufferedInputStream( new FileInputStream( infoFile ) ))
            {
                manifest = new Manifest( in );
            }
            catch( Throwable t )
            {
                String message = MessageFormat.format( messageBundle.getResourceString( "CANNOT_GET_MANIFEST" ), new Object[] {modulePath} );
                log.log(Level.SEVERE,  message, t );
            }
        }

        return manifest;
    }

    public static List<File> getFiles(File dir, Set<String> excludedNames)
    {
        List<File> list = new ArrayList<>();
        if( dir.isDirectory() )
        {
            File[] files = dir.listFiles();
            if( files != null )
            {
                for( File file : files )
                {
                    if( excludedNames == null || !excludedNames.contains(file.getName()) )
                    {
                        list.add(file);
                        if( file.isDirectory() )
                            list.addAll(getFiles(file, excludedNames));
                    }
                }
            }
        }

        return list;
    }

    public static void addToJar(JarOutputStream jarOutputStream, int excludedPrefixPathSize, File file) throws IOException
    {
        String fileName = file.getPath();
        if( file.isDirectory() )
        {
            fileName += "/";
        }
        String zipEnrtyName = fileName.substring(excludedPrefixPathSize);
        ZipEntry zipentry = new ZipEntry(zipEnrtyName);
        zipentry.setSize(file.length());
        zipentry.setTime(file.lastModified());
        jarOutputStream.putNextEntry(zipentry);
        if( file.isFile() )
        {
            byte[] rgb = new byte[1000];
            int n;
            CRC32 crc32 = new CRC32(); // Calculate the CRC-32 value
            try (FileInputStream fileinputstream = new FileInputStream( file ))
            {
                while( ( n = fileinputstream.read(rgb) ) > -1 )
                {
                    crc32.update(rgb, 0, n);
                }
            }
            // Create a zip entry.
            // Add the zip entry and associated data.
            zipentry.setCrc(crc32.getValue());
            try (FileInputStream fileinputstream = new FileInputStream( file ))
            {
                while( ( n = fileinputstream.read(rgb) ) > -1 )
                {
                    jarOutputStream.write(rgb, 0, n);
                }
            }
        }
        jarOutputStream.closeEntry();
    }

    public static File extractFile(File dstDir, String fileName, JarFile moduleFile, JarEntry jarEntry) throws IOException
    {
        fileName = fileName.replace('\\', '/');
        File file = new File(dstDir, fileName);
        ApplicationUtils.copyStream(new FileOutputStream(file), moduleFile.getInputStream(jarEntry));
        return file;
    }

    public static String getModuleAttribute(JarFile jarFile, String attribute)
    {
        String moduleAttribute = null;
        try
        {
            Manifest manifest = jarFile.getManifest();
            if( manifest != null )
            {
                Attributes attributes = manifest.getMainAttributes();
                if( attributes != null )
                    moduleAttribute = attributes.getValue(attribute);
            }
        }
        catch( Throwable t )
        {
            String message = MessageFormat.format(messageBundle.getResourceString("CANNOT_GET_MANIFEST_ATTRIBUTE"),
                    new Object[] {attribute});
            log.log(Level.SEVERE, message, t);
        }

        return moduleAttribute;
    }

}
