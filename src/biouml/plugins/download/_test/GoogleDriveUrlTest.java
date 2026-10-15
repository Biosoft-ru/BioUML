package biouml.plugins.download._test;

import java.net.URL;

import junit.framework.TestCase;
import biouml.plugins.download.FileDownloader;

/**
 * A Google Drive share link is converted to the file's direct download link.
 */
public class GoogleDriveUrlTest extends TestCase
{
    private static final String DOWNLOAD = "https://drive.usercontent.google.com/download?id=1NvEMmaGnHV044a7HIbGe9BtJX3YPcoXH&export=download&confirm=t";

    public void testShareLinks() throws Exception
    {
        assertEquals( DOWNLOAD, convert( "https://drive.google.com/file/d/1NvEMmaGnHV044a7HIbGe9BtJX3YPcoXH/view?usp=sharing" ) );
        assertEquals( DOWNLOAD, convert( "https://drive.google.com/file/d/1NvEMmaGnHV044a7HIbGe9BtJX3YPcoXH" ) );
        assertEquals( DOWNLOAD, convert( "https://drive.google.com/open?id=1NvEMmaGnHV044a7HIbGe9BtJX3YPcoXH" ) );
        assertEquals( DOWNLOAD, convert( "https://drive.google.com/uc?export=download&id=1NvEMmaGnHV044a7HIbGe9BtJX3YPcoXH" ) );
    }

    public void testOtherLinksUnchanged() throws Exception
    {
        assertEquals( DOWNLOAD, convert( DOWNLOAD ) );
        assertEquals( "https://example.org/a.vcf", convert( "https://example.org/a.vcf" ) );
    }

    private static String convert(String url) throws Exception
    {
        return FileDownloader.convertURL( new URL( url ), null ).toExternalForm();
    }
}
