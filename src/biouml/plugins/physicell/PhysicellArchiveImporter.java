package biouml.plugins.physicell;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.archive.ArchiveEntry;
import ru.biosoft.util.archive.ArchiveFactory;
import ru.biosoft.util.archive.ArchiveFile;
import ru.biosoft.util.bean.BeanInfoEx2;

public class PhysicellArchiveImporter implements DataElementImporter
{
    private static final String EXTENSION = ".pcl";

    protected static final Logger log = Logger.getLogger(PhysicellArchiveImporter.class.getName());

    private static final String MY_FORMAT = "Archive (*.pcl)";

    private List<ImportedFile> files;
    private File mainFile = null;

    private ImportProperties properties;

    private static final String TYPE_SBML = "sbml";
    private static final String TYPE_SCRIPT = "script";
    private static final String TYPE_TABLE = "table";

    private Map<String, File> additionalFiles = new HashMap<>();

    @Override
    public int accept(DataCollection parent, File file)
    {
        if( parent == null || !DataCollectionUtils.isAcceptable(parent, getResultType()) || !parent.isMutable() )
            return ACCEPT_UNSUPPORTED;
        if( file == null )
            return ACCEPT_HIGH_PRIORITY;
        ArchiveFile archiveFile = ArchiveFactory.getArchiveFile(file);
        if( archiveFile != null )
        {
            archiveFile.close();
            if( file.getName().endsWith(EXTENSION) )
                return ACCEPT_HIGHEST_PRIORITY;
            return ACCEPT_BELOW_MEDIUM_PRIORITY;
        }
        return ACCEPT_UNSUPPORTED;
    }

    @Override
    public ImportProperties getProperties(DataCollection parent, File file, String elementName)
    {
        properties = new ImportProperties(DataElementPath.create(parent));
        return properties;
    }

    @PropertyName ( "Import properties" )
    @PropertyDescription ( "Import properties." )
    public static class ImportProperties extends Option
    {
        private DataElementPath path = null;
        private String diagramName = "Model";

        public ImportProperties(DataElementPath path)
        {
            this.path = path;
        }
        
        @PropertyName ( "Diagram name" )
        @PropertyDescription ( "Diagram name." )
        public String getDiagramName()
        {
            return diagramName;
        }
        public void setDiagramName(String diagramName)
        {
            this.diagramName = diagramName;
        }

        @PropertyName ( "Folder path" )
        @PropertyDescription ( "Folder path." )
        public DataElementPath getPath()
        {
            return path;
        }
        public void setPath(DataElementPath path)
        {
            this.path = path;
        }
    }

    public static class ImportPropertiesBeanInfo extends BeanInfoEx2<ImportProperties>
    {
        public ImportPropertiesBeanInfo()
        {
            super(ImportProperties.class, MessageBundle.class.getName());
        }
        @Override
        public void initProperties() throws Exception
        {
            add("path");
            add("diagramName");
        }
    }

    @Override
    public DataElement doImport(DataCollection<?> parent, File file, String elementName, FunctionJobControl jobControl, Logger log)
            throws Exception
    {
        if( jobControl != null )
            jobControl.functionStarted();

        DataElementPath path = properties.getPath();
        DataCollection<?> collection = null;
        try
        {
            if( !path.exists() )
            {
                collection = DataCollectionUtils.createSubCollection(path);
                if( collection == null )
                    throw new Exception("Unable to create " + path);
            }
            else
                collection = path.getDataCollection();

            files = prepareFiles(file);

            Map<String, String> newPaths = new HashMap<>();
            for( ImportedFile importedFile : files )
                newPaths.put(importedFile.shortName, DataElementPath.create(collection, importedFile.title).toString());

            log.info("Files to import in order: ");
            for( ImportedFile f : files )
                log.info(f.shortName);

            PhysicellImporter importer = new PhysicellImporter();
            PhysicellImportProperties props = (PhysicellImportProperties)importer.getProperties(collection, mainFile, elementName);
            props.setDiagramName(properties.getDiagramName());
            importer.setAdditionalFiles(additionalFiles);
            importer.doImport(collection, mainFile, elementName, jobControl, log);
        }
        catch( Exception e )
        {
            if( jobControl != null )
            {
                jobControl.functionTerminatedByError(e);
                return null;
            }
            throw e;
        }
        if( jobControl != null )
        {
            jobControl.setPreparedness(100);
            jobControl.functionFinished();
        }
        return collection;
    }

    private List<ImportedFile> prepareFiles(File file) throws Exception
    {
        List<ImportedFile> result = new ArrayList<>();
        ArchiveFile archiveFile = ArchiveFactory.getArchiveFile(file);
        if( archiveFile == null )
            throw new Exception("Specified file is not an archive");
        ArchiveEntry entry;
        while( ( entry = archiveFile.getNextEntry() ) != null )
        {
            String[] pathFields = entry.getName().split("[\\\\\\/]+");
            String entryName = pathFields[pathFields.length - 1];
            if( entryName.toLowerCase().equals("manifest.xml") )
                continue;

            String entryNameNoExt = entryName.replaceFirst("^(.+)\\..+", "$1");
            InputStream is = entry.getInputStream();
            File tmpFile = TempFiles.file("zipImport" + entryName, is);
            String type = detectType(file);
            additionalFiles.put(entry.getName(), tmpFile);
            if( entryName.equalsIgnoreCase("PhysiCell_settings.xml") )
            {
                mainFile = tmpFile;
                continue;
            }
            result.add(new ImportedFile(tmpFile, entry.getName(), entryNameNoExt, type));
        }
        return result;
    }

    private String detectType(File file)
    {
        String name = file.getName();
        String ext = name.substring(name.lastIndexOf(".") + 1);
        switch( ext )
        {
            case "txt":
            case "csv":
                return TYPE_TABLE;
            case "xml":
            case "sbml":
                return TYPE_SBML;
            case "java":
                return TYPE_SCRIPT;
            default:
                return TYPE_TABLE;
        }
    }

    @Override
    public boolean init(Properties properties)
    {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public Class<? extends DataElement> getResultType()
    {
        // TODO Auto-generated method stub
        return null;
    }

    private class ImportedFile
    {
        File f;
        String type;
        String shortName;
        String title;

        public ImportedFile(File file, String shortName, String title, String type)
        {
            this.f = file;
            this.shortName = shortName;
            this.title = title;
            this.type = type;
        }
    }

}