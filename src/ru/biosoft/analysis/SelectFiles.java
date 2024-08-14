package ru.biosoft.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.analysiscore.AnalysisMethodSupport;

public class SelectFiles extends AnalysisMethodSupport<SelectFilesParameters>
{
    public static final String FOLDER_NAME = "file_collection.files";

    public SelectFiles(DataCollection origin, String name)
    {
        super( origin, name, new SelectFilesParameters() );
    }

    @Override
    public Object justAnalyzeAndPut()
    {
        log.info( "Filter started" );
        DataCollection dc = parameters.getInputCollection().getDataCollection();

        int filesFound = 0;
        File simDir = null;
        try
        {
            DataCollection parent = parameters.getOutputCollection().getParentCollection();
            if( parent.contains( parameters.getOutputCollection().getName() ) )
                parent.remove( parameters.getOutputCollection().getName() );

            DataCollection result = (DataCollection)DataCollectionUtils.createSubCollection( parameters.getOutputCollection() );

            String pattern = parameters.getMask();
            ru.biosoft.access.core.DataElement[] elements = filter( dc, pattern );            //            

            File workDir = new File( parameters.getWorkDir() );
            if( !workDir.exists() )
                workDir.mkdirs();

            simDir = new File( workDir.getAbsolutePath(), "Selected" );
            if( simDir.exists() )
            {
                for( File file : simDir.listFiles() )
                    file.delete();
                simDir.delete();
            }
            simDir.mkdir();

            for( ru.biosoft.access.core.DataElement de : elements )
            {
                File file = null;
                try
                {
                    file = DataCollectionUtils.getChildFile( dc, de.getName() );
                }
                catch( Exception ex )
                {
                    log.info( "File not found for DataElemnent " + de.getName() + ". It will be skipped for analysis. Error: "
                            + ex.getMessage() );
                    continue;
                }

                if( file == null )
                {
                    log.info( "File not found for DataElemnent " + de.getName() + ". It will be skipped for analysis." );
                    continue;
                }

                File link = createSymbolicLink( file, simDir );
                FileDataElement resultElement = new FileDataElement( link.getName(), result, link );
                result.put( resultElement );
            }
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
        finally
        {
/*
            if( simDir != null )
            {
                for( File file: simDir.listFiles() )                
                    file.delete();                
                simDir.delete();
            }
*/
        }

        getJobControl().setPreparedness( 100 );
        return null;
    }

    private File createSymbolicLink(File file, File targetFolder) throws IOException
    {
        File f = new File( targetFolder.getAbsolutePath(), file.getName() );
        Path linkPath = Paths.get( f.getAbsolutePath() );
        return Files.createSymbolicLink( linkPath, Paths.get( file.getAbsolutePath() ) ).toFile();
    }

    private static DataElement[] filter(DataCollection<DataElement> dc, String pattern) throws IOException
    {
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher( "glob:" + pattern );
        return dc.stream().filter( de -> pathMatcher.matches( Paths.get( de.getName() ) ) ).toArray( ru.biosoft.access.core.DataElement[]::new );
    }


}