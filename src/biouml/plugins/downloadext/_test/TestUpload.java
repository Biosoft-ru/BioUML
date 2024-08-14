package biouml.plugins.downloadext._test;

import java.io.File;
import java.util.Properties;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.plugins.downloadext.FTPUploadAnalysis;
import biouml.plugins.downloadext.FTPUploadAnalysisParameters;
import ru.biosoft.access.FileCollection;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.ImporterFormat;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.util.TempFiles;

public class TestUpload extends AbstractBioUMLTest
{
    public void testUploadAnalysis() throws Exception
    {
        File input = TempFiles.file( "temp.txt", "test" );
        File output = new File( input + ".dir" );
        output.mkdirs();

        Properties properties = new Properties();
        properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, "test" );
        properties.setProperty( DataCollectionConfigConstants.FILE_PATH_PROPERTY, output.getAbsolutePath() );
        CollectionFactory.registerRoot( new FileCollection( null, properties ) );

        FTPUploadAnalysis analysis = new FTPUploadAnalysis( null, "" );
        FTPUploadAnalysisParameters parameters = analysis.getParameters();
        parameters.setResultPath( DataElementPath.create( "test/output" ) );
        parameters.setImporterFormat( new ImporterFormat( "blahblah" ) );
        Exception ex = null;
        try
        {
            analysis.validateParameters();
        }
        catch( Exception e )
        {
            ex = e;
        }
        assertNotNull( ex );
        parameters.setImporterFormat( new ImporterFormat( "Generic file" ) );
        parameters.setFileURL( "blah://url" );
        ex = null;
        try
        {
            analysis.validateParameters();
        }
        catch( Exception e )
        {
            ex = e;
        }
        assertNotNull( ex );
        parameters.setFileURL( input.toURI().toURL().toExternalForm() );
        analysis.validateParameters();
        DataElement fileDataElement = analysis.justAnalyzeAndPut();

        assertNotNull( fileDataElement );
        assertTrue( fileDataElement instanceof FileDataElement );
        assertEquals( new File( output, "output.txt" ), ( (FileDataElement)fileDataElement ).getFile() );
        assertEquals( "test", ApplicationUtils.readAsString( ( (FileDataElement)fileDataElement ).getFile() ) );
        CollectionFactory.unregisterAllRoot();
        input.delete();
        ApplicationUtils.removeDir( output );
    }
}
