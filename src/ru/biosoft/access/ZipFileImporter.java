package ru.biosoft.access;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;

import one.util.streamex.StreamEx;
import ru.biosoft.access.DataElementImporterRegistry.ImporterInfo;
import ru.biosoft.access.ImporterFormat.DefaultImporterFormatEditor;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.SubFunctionJobControl;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.LazyValue;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.archive.ArchiveEntry;
import ru.biosoft.util.archive.ArchiveFactory;
import ru.biosoft.util.archive.ArchiveFile;
import ru.biosoft.util.archive.ComplexArchiveFile;
import ru.biosoft.util.bean.BeanInfoEx2;

public class ZipFileImporter implements DataElementImporter
{
    private static final String MY_FORMAT = "ZIP-archive (*.zip)";
    private final ImporterProperties properties = new ImporterProperties();

    @Override
    public int accept(DataCollection parent, File file)
    {
        if(parent == null || !DataCollectionUtils.isAcceptable(parent, getResultType()) || !parent.isMutable()) return ACCEPT_UNSUPPORTED;
        if(file == null) return ACCEPT_HIGH_PRIORITY;
        ArchiveFile archiveFile = ArchiveFactory.getArchiveFile(file);
        if(archiveFile != null)
        {
            archiveFile.close();
            return ACCEPT_HIGH_PRIORITY;
        }
        return ACCEPT_UNSUPPORTED;
    }

    @Override
    public DataElement doImport(@Nonnull DataCollection parent, @Nonnull File file, String elementName, FunctionJobControl jobControl,
            Logger log) throws Exception
    {
        if( jobControl != null )
        {
            jobControl.functionStarted();
        }
        String name = elementName;
        if( name == null || name.equals("") )
            name = file.getName();
        name = name.replaceAll("\\/", "");
        DataCollection<?> collection = null;
        try
        {
            if( !properties.getImportFormat().equals("(auto)") )
            {
                ImporterInfo info = DataElementImporterRegistry.getImporterInfo(properties.getImportFormat());
                if( info == null )
                    throw new Exception("Specified import format for archive entries is not found: " + properties.getImportFormat());
            }
            boolean cleanup = properties.isCleanupFolder();
            DataElementPath rootPath = DataElementPath.create( parent, name );
            if( parent.contains(name) && !cleanup  && parent.get(name) instanceof FolderCollection)
            {
                collection = (DataCollection<?>)parent.get( name );
            }
            else if( parent.contains(name) && cleanup )
            {
                parent.remove(name);
                collection = DataCollectionUtils.createSubCollection(rootPath);
            }
            else
            {
                collection = DataCollectionUtils.createSubCollection(rootPath);
            }
            if(collection == null)
                throw new Exception("Unable to create "+rootPath);
            ArchiveFile archiveFile = ArchiveFactory.getArchiveFile(file);
            if(archiveFile == null) throw new Exception("Specified file is not an archive");
            archiveFile = new ComplexArchiveFile(archiveFile);
            long size = file.length();
            int i=0;
            ArchiveEntry entry;
            int numImported = 0;
            while((entry = archiveFile.getNextEntry()) != null)
            {
                if(jobControl != null)
                {
                    if(size > 0)
                        jobControl.setPreparedness((int) ( archiveFile.offset()*100./size ));
                    if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST)
                    {
                        archiveFile.close();
                        parent.remove(collection.getName());
                        return null;
                    }
                }
                i++;
                if(entry.isDirectory()) continue;
                String[] pathFields = entry.getName().split("[\\\\\\/]+");
                String entryName = pathFields[pathFields.length-1];
                DataElementPath entryPath;
                String entryNameNoExt = properties.isPreserveExtension() ? entryName : entryName.replaceFirst("^(.+)\\..+", "$1");
                if(properties.isPreserveArchiveStructure())
                {
                    entryPath = StreamEx.of( pathFields ).without( "." ).without( ".." ).remove( String::isEmpty )
                            .foldLeft( rootPath, DataElementPath::getChildPath );
                    DataCollectionUtils.createFoldersForPath(entryPath);
                } else
                {
                    entryPath = rootPath.getChildPath(entryName);
                }
                if(!properties.isPreserveExtension())
                    entryPath.getSiblingPath( entryNameNoExt );

                if(entryPath.exists())
                    continue;
                InputStream is = entry.getInputStream();
                DataCollection<? extends DataElement> parentCollection = entryPath.optParentCollection();
                if(parentCollection == null) continue;
                File tmpFile = TempFiles.file("zipImport"+entryName, is);
                try
                {
                    DataElementImporter importer;
                    if(properties.getImportFormat().equals("(auto)"))
                    {
                        ImporterInfo[] importers =  DataElementImporterRegistry.getAutoDetectImporter(tmpFile, parentCollection, false);
                        if(importers == null || importers.length == 0)
                            continue;
                        importer = importers[0].cloneImporter();
                    }
                    else
                        importer = DataElementImporterRegistry.getImporterInfo(properties.getImportFormat()).cloneImporter();
                    if(importer.accept(parentCollection, tmpFile) == ACCEPT_UNSUPPORTED)
                    {
                        log.warning("Importer doesn't accept "+entryName);
                        continue;
                    }
                    if(properties.getImporterProperties() != null)
                    {
                        Object importerProperties = importer.getProperties(parentCollection, tmpFile, entryNameNoExt);
                        if(importerProperties != null)
                            BeanUtil.copyBean(properties.getImporterProperties(), importerProperties);
                    }
                    FunctionJobControl subJobControl = null;
                    if(jobControl != null)
                      subJobControl = new SubFunctionJobControl( jobControl, jobControl.getPreparedness(), (int)(archiveFile.offset()*100./size) );
                    importer.doImport(parentCollection, tmpFile, entryNameNoExt, subJobControl, log);
                    numImported++;
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "Unable to import "+entryName+": "+e);
                    log.log(Level.FINE, "Unable to import "+entryName, e);
                }
                finally
                {
                    tmpFile.delete();
                }
            }
            if( numImported == 0 )
            {
                throw new Exception( "No importable or valid files were found in archive. Please check your input file."
                        + " If you wish to import to a folder already existing in your workspace or upload the same"
                        + " archive a second time removing existing contents, please mark the option \"Cleanup existing folder\"" );
            }
            parent.put(collection);
            archiveFile.close();
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

    @Override
    public boolean init(Properties properties)
    {
        return true;
    }

    @Override
    public Object getProperties(DataCollection parent, File file, String elementName)
    {
        return properties;
    }

    public static class ImporterProperties extends Option
    {
        private static final long serialVersionUID = 1L;
        private ImporterFormat importerInfo = ImporterFormat.FORMAT_AUTO;
        private Object importerProperties = null;
        private boolean cleanupFolder = false;
        private boolean preserveExtension = false;
        private boolean preserveArchiveStructure = false;

        public String getImportFormat()
        {
            return importerInfo.getFormat();
        }

        @PropertyName ( "Importer for files in archive" )
        @PropertyDescription ( "Import all elements in the specified format (elements not compatible with this format will be skipped)" )
        public ImporterFormat getImporterInfo()
        {
            return importerInfo;
        }
        public void setImporterInfo(ImporterFormat importerInfo)
        {
            Object oldValue = this.importerInfo;
            this.importerInfo = importerInfo;
            firePropertyChange( "importerInfo", oldValue, this.importerInfo );
            setImporterProperties( DataElementImporterRegistry.getImporterProperties( importerInfo.getFormat() ) );
        }

        @PropertyName("Importer properties")
        @PropertyDescription("Properties for importer")
        public Object getImporterProperties()
        {
            return importerProperties;
        }

        public void setImporterProperties(Object importerProperies)
        {
            Object oldValue = this.importerProperties;
            this.importerProperties = importerProperies;
            if(importerProperies != null)
            {
                ComponentModel model = ComponentFactory.getModel(this);
                ComponentFactory.recreateChildProperties(model);
            }
            firePropertyChange("importerProperties", oldValue, this.importerProperties);
        }

        public boolean isPropertiesHidden()
        {
            return importerProperties == null;
        }

        @PropertyName("Cleanup existing folder")
        @PropertyDescription ( "Files from your archive will be imported into a folder. "
                + "If the folder with the same name already exists, its content will be removed." )
        public boolean isCleanupFolder()
        {
            return cleanupFolder;
        }

        public void setCleanupFolder(boolean cleanupFolder)
        {
            Object oldValue = this.cleanupFolder;
            this.cleanupFolder = cleanupFolder;
            firePropertyChange("cleanupFolder", oldValue, cleanupFolder);
        }

        @PropertyName("Preserve extension")
        @PropertyDescription("Preserve extension of zip entries")
        public boolean isPreserveExtension()
        {
            return preserveExtension;
        }

        public void setPreserveExtension(boolean preserveExtension)
        {
            boolean oldValue = this.preserveExtension;
            this.preserveExtension = preserveExtension;
            firePropertyChange("preserveExtension", oldValue, preserveExtension);
        }

        @PropertyName("Preserve archive structure")
        @PropertyDescription("Whether to create subfolders existing in archive or extract everything in single folder")
        public boolean isPreserveArchiveStructure()
        {
            return preserveArchiveStructure;
        }

        public void setPreserveArchiveStructure(boolean preserveArchiveStructure)
        {
            Object oldValue = this.preserveArchiveStructure;
            this.preserveArchiveStructure = preserveArchiveStructure;
            firePropertyChange("preserveArchiveStructure", oldValue, preserveArchiveStructure);
        }
    }

    public static class ImporterPropertiesBeanInfo extends BeanInfoEx2<ImporterProperties>
    {
        public ImporterPropertiesBeanInfo()
        {
            super(ImporterProperties.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add("cleanupFolder");
            add("preserveExtension");
            add("preserveArchiveStructure");
            property( "importerInfo" ).editor( ImporterFormatEditor.class ).simple().structureChanging().add();
            addHidden("importerProperties", "isPropertiesHidden");
        }
    }
    public static class ImporterFormatEditor extends DefaultImporterFormatEditor
    {
        @Override
        protected LazyValue<ImporterFormat[]> getFormats()
        {
            return new LazyValue<>( () -> DataElementImporterRegistry.importers()
                    .filter( importer -> !MY_FORMAT.equals( importer.getFormat() ) ).sortedBy( ImporterInfo::getFormat )
                    .map( ImporterFormat::new ).prepend( ImporterFormat.FORMAT_AUTO ).toArray( ImporterFormat[]::new ) );
        }
    }

    @Override
    public Class<? extends DataElement> getResultType()
    {
        return FolderCollection.class;
    }
}
