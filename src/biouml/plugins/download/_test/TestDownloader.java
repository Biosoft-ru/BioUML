package biouml.plugins.download._test;

import java.net.URL;
import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;
import biouml.plugins.download.FileDownloader;
import junit.framework.TestCase;

import com.developmentontheedge.application.ApplicationUtils;
import ru.biosoft.jobcontrol.FunctionJobControl;

/**
 * @author lan
 *
 */
public class TestDownloader extends TestCase
{
    public void testConvertURL() throws Exception
    {
        URL url = new URL( "http://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSE31193" );
        URL expected = new URL( "http://www.ncbi.nlm.nih.gov/geosuppl/?acc=GSE31193" );
        assertEquals( expected, FileDownloader.convertURL( url, null ) );
        StringBuilder sb = new StringBuilder();
        assertEquals( expected, FileDownloader.convertURL( url, sb ) );
        assertEquals( "GEO page URL detected; converted to:\n" + expected, sb.toString() );

        URL plain = new URL( "http://example.com/" );
        assertEquals( plain, FileDownloader.convertURL( plain, null ) );
        sb = new StringBuilder();
        assertEquals( plain, FileDownloader.convertURL( plain, sb ) );
        assertEquals( "", sb.toString() );
    }

    public void testFileDownloader() throws Exception
    {
        try (TempFile input = TempFiles.file( "temp.txt", "test" ); TempFile output = TempFiles.file( "temp.download.txt" ))
        {
            FunctionJobControl fjc = new FunctionJobControl( null );
            FileDownloader.downloadFile( input.toURI().toURL(), output, fjc );
            assertEquals( 100, fjc.getPreparedness() );
            assertEquals( "test", ApplicationUtils.readAsString( output ) );
        }
    }
}
