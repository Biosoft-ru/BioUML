package biouml.plugins.sedml;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.archive.ArchiveFactory;
import ru.biosoft.util.archive.ArchiveFile;
import biouml.plugins.sbml.SbmlImporter;

import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.SubFunctionJobControl;

public class SedxImporter implements DataElementImporter
{

    private static String METADATA = "metadata";
    private static String MANIFEST = "manifest.xml";
    
    @Override
    public int accept(DataCollection<?> parent, File file)
    {
        if( parent == null || !DataCollectionUtils.isAcceptable( parent, getResultType() ) || !parent.isMutable() )
            return ACCEPT_UNSUPPORTED;
        if( file == null )
            return ACCEPT_MEDIUM_PRIORITY;
        ArchiveFile archiveFile = ArchiveFactory.getArchiveFile( file );
        if( archiveFile != null )
        {
            archiveFile.close();
            if( file.getName().endsWith( ".sedx" ) )
                return ACCEPT_HIGHEST_PRIORITY;
            return ACCEPT_MEDIUM_PRIORITY;//should be below then in general zip file
        }
        return ACCEPT_UNSUPPORTED;
    }

    @Override
    public DataElement doImport(@Nonnull DataCollection<?> parent, @Nonnull File file, String elementName, FunctionJobControl jobControl,
            Logger log) throws Exception
    {
        if( jobControl != null )
            jobControl.functionStarted();
        DataElementPath rootPath = DataElementPath.create( parent, elementName );
        if( rootPath.exists() )
            rootPath.remove();
        DataCollection<?> root = DataCollectionUtils.createSubCollection( rootPath );

        try (ZipFile zipFile = new ZipFile( file ))
        {
            Map<String, File> sedmlFiles = new HashMap<>();
            Map<String, File> sbmlFiles = new HashMap<>();
            for( ZipEntry entry : StreamEx.of( zipFile.stream() ).remove( ZipEntry::isDirectory ) )
            {
                String[] pathFields = entry.getName().split( "[\\\\\\/]+" );
                String entryName = pathFields[pathFields.length - 1];
                TempFile entryFile = TempFiles.file( "sedxImport" + entryName, zipFile.getInputStream( entry ) );
                DataElementImporter importer = new SedmlImporter();
                if( importer.accept( root, entryFile ) != ACCEPT_UNSUPPORTED )
                    sedmlFiles.put( entryName, entryFile );
                else
                {
                    if (entryName.startsWith(METADATA) || entryName.equalsIgnoreCase(MANIFEST))
                        continue;
                    importer = new SbmlImporter();
                    if( importer.accept( root, entryFile ) == ACCEPT_UNSUPPORTED )
                    {
                        log.info("file +" +entryName+" skipped as unsupported.");
                        continue;
                    }
                        //throw new Exception( "Can not import " + entryName + ", only sbml and sedml files allowed" );
                    sbmlFiles.put( entryName, entryFile );
                }
            }

            int i = 0;
            for( Map.Entry<String, File> e : sbmlFiles.entrySet() )
            {
                String sbmlName = e.getKey();
                sbmlName = sbmlName.replaceFirst( "\\.\\w+$", "" );
                File sbmlFile = e.getValue();
                SubFunctionJobControl subJC = null;
                if( jobControl != null )
                    subJC = new SubFunctionJobControl( jobControl, i * 50 / sbmlFiles.size(), ( i + 1 ) * 50 / sbmlFiles.size() );
                SbmlImporter importer = new SbmlImporter();
                importer.getProperties( root, sbmlFile, sbmlName );
                importer.doImport( root, sbmlFile, sbmlName, subJC, log );
                sbmlFile.delete();
                i++;
            }

            i = 0;
            for( Map.Entry<String, File> e : sedmlFiles.entrySet() )
            {
                String sedmlName = e.getKey();
                File sedmlFile = e.getValue();
                SubFunctionJobControl subJC = null;
                if( jobControl != null )
                    subJC = new SubFunctionJobControl( jobControl, 50 + i * 50 / sedmlFiles.size(), 50 + ( i + 1 ) * 50 / sedmlFiles.size() );
                SedmlImporter importer = new SedmlImporter();
                importer.getProperties( root, sedmlFile, sedmlName ).setModelCollectionPath( rootPath );
                importer.doImport( root, sedmlFile, sedmlName, subJC, log );
                sedmlFile.delete();
                i++;
            }
        }

        if( jobControl != null )
            jobControl.functionFinished();

        return root;
    }

    @Override
    public boolean init(Properties properties)
    {
        return true;
    }

    @Override
    public Object getProperties(DataCollection<?> parent, File file, String elementName)
    {
        return null;
    }

    @Override
    public Class<? extends DataElement> getResultType()
    {
        return FolderCollection.class;
    }

}
