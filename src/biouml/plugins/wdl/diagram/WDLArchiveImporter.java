package biouml.plugins.wdl.diagram;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.plugins.wdl.FileScriptLoader;
import biouml.plugins.wdl.ScriptLoader;
import biouml.plugins.wdl.model.ScriptInfo;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.util.archive.ArchiveFactory;
import ru.biosoft.util.archive.ArchiveFile;
import ru.biosoft.util.bean.BeanInfoEx2;

public class WDLArchiveImporter implements DataElementImporter
{
    private WDLArchiveImportProperties properties = null;
    @Override
    public int accept(DataCollection<?> parent, File file)
    {
        if( parent == null || !DataCollectionUtils.isAcceptable( parent, getResultType() ) || !parent.isMutable() )
            return ACCEPT_UNSUPPORTED;
        if( file == null )
            return ACCEPT_HIGH_PRIORITY;

        ArchiveFile archiveFile = ArchiveFactory.getArchiveFile( file );
        if( archiveFile != null )
        {
            archiveFile.close();
            return ACCEPT_MEDIUM_PRIORITY;
        }
        return ACCEPT_UNSUPPORTED;
    }

    @Override
    public DataElement doImport(DataCollection<?> parent, File file, String elementName, FunctionJobControl jobControl, Logger log) throws Exception
    {
        String folderName = elementName;
        if( folderName == null || folderName.equals( "" ) )
            folderName = file.getName();
        folderName = folderName.replaceAll( "\\/", "" );
        // Convert non-ASCII characters to ASCII to prevent jstree selector errors
        folderName = TextUtil2.toASCII( folderName );
        boolean cleanup = properties.isCleanupFolder();
        DataElementPath rootPath = DataElementPath.create( parent, folderName );
        DataCollection<?> collection = null;
        if( parent.contains( folderName ) && !cleanup && parent.get( folderName ) instanceof FolderCollection )
        {
            collection = (DataCollection<?>) parent.get( folderName );
        }
        else if( parent.contains( folderName ) && cleanup )
        {
            parent.remove( folderName );
            collection = DataCollectionUtils.createSubCollection( rootPath );
        }
        else
        {
            collection = DataCollectionUtils.createSubCollection( rootPath );
        }
        if( collection == null )
            throw new Exception( "Unable to create " + rootPath );

        File archivedFolder = TempFiles.dir( elementName + "_unpacked" );
        try
        {
            ArchiveFactory.unpack( file, archivedFolder, null );
            if( jobControl != null )
                jobControl.setPreparedness( 20 );
            String location = properties.getMainFile();
            File mainFile = new File( archivedFolder, location );

            String diagramName = properties.getDiagramName();
            if( diagramName == null || diagramName.isEmpty() )
            {
                diagramName = FilenameUtils.removeExtension( mainFile.getName() );
            }

            String text = ApplicationUtils.readAsString( mainFile );

            WDLImporter importer = new WDLImporter();
            importer.setScriptLoader( new FileScriptLoader( ScriptLoader.WDL_TYPE, mainFile.getParentFile() ) );
            ScriptInfo info = importer.readScript( diagramName, text );
            DiagramGenerator generator = new DiagramGenerator();
            Diagram main = generator.generateDiagram( info, collection, diagramName );

            Map<String, Diagram> diagrams = generator.getAllImports();

            if( jobControl != null )
                jobControl.setPreparedness( 90 );

            for ( Diagram diagram : diagrams.values() )
                CollectionFactoryUtils.save( diagram );

            if( jobControl != null )
                jobControl.functionFinished();
            return main;
        }
        catch (Exception e)
        {
            if( jobControl != null )
                jobControl.functionTerminatedByError( e );
            throw e;
        }
    }

    private static List<String> collectWdlFiles(Path base, Path dir) throws IOException
    {
        List<String> result = new ArrayList<>();
        Files.walkFileTree( base, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
            {
                if( dir.getFileName().toString().equals( ".git" ) )
                {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                if( file.getFileName().toString().endsWith( ".wdl" ) )
                {
                    result.add( dir.relativize( file ).toString() );
                }
                return FileVisitResult.CONTINUE;
            }
        } );
        return result;
    }

    @Override
    public boolean init(Properties properties)
    {
        return true;
    }

    @Override
    public Object getProperties(DataCollection<?> parent, File file, String elementName)
    {
        if( properties == null )
            properties = new WDLArchiveImportProperties( null );

        if( file != null )
        {
            properties = new WDLArchiveImportProperties( file );
        }
        if( elementName != null )
            properties.setDiagramName( elementName );

        return properties;
    }

    @Override
    public Class<? extends DataElement> getResultType()
    {
        return Diagram.class;
    }

    public static class WDLArchiveImportProperties extends Option
    {
        private String diagramName;
        private String mainFile;
        private List<String> files;
        private boolean cleanupFolder = false;

        public WDLArchiveImportProperties(File file)
        {
            if( file != null )
            {
                try
                {
                    File archivedFolder = TempFiles.dir( file.getName() + "_unpacked" );
                    ArchiveFactory.unpack( file, archivedFolder, null );
                    Path dir = archivedFolder.toPath();
                    files = collectWdlFiles( dir, dir );
                }
                catch (Exception e)
                {
                    // TODO: handle exception
                }
            }
            else
                files = new ArrayList<>();
        }

        @PropertyName("Diagram name")
        public String getDiagramName()
        {
            return diagramName;
        }

        public void setDiagramName(String diagramName)
        {
            this.diagramName = diagramName;
        }

        @PropertyName("Main WDL file")
        public String getMainFile()
        {
            return mainFile;
        }

        public void setMainFile(String mainFile)
        {
            this.mainFile = mainFile;
            if( this.mainFile != null )
            {
                setDiagramName( FilenameUtils.removeExtension( new File( this.mainFile ).getName() ) );
            }

        }

        public Stream<String> getFiles()
        {
            return files.stream();
        }

        @PropertyName("Cleanup existing folder")
        @PropertyDescription("Files from your archive will be imported into a folder. " + "If the folder with the same name already exists, its content will be removed.")
        public boolean isCleanupFolder()
        {
            return cleanupFolder;
        }

        public void setCleanupFolder(boolean cleanupFolder)
        {
            Object oldValue = this.cleanupFolder;
            this.cleanupFolder = cleanupFolder;
            firePropertyChange( "cleanupFolder", oldValue, cleanupFolder );
        }
    }

    public static class WDLArchiveImportPropertiesBeanInfo extends BeanInfoEx2<WDLArchiveImportProperties>
    {
        public WDLArchiveImportPropertiesBeanInfo()
        {
            super( WDLArchiveImportProperties.class );
        }

        @Override
        public void initProperties() throws Exception
        {
            addWithTags( "mainFile", bean -> bean.getFiles() );
            add( "diagramName" );
            add( "cleanupFolder" );
        }
    }

}
