package biouml.plugins.sbml._test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.developmentontheedge.application.ApplicationUtils;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SbmlBiomodelsUploadTest extends TestCase
{
    private static final String csvOutDirectory = "Q:/biosoft/SBML/biomodels db/New_CSV_files/";

    private static final String proxyName = "developmentontheedge.com";
    private static final int proxyPort = 8080;

    private static final String startPage = "http://sys-bio.org/fbergman/compare/";
    private static final String linkFilter = "SBToolbox2.csv";

    /** Standart JUnit constructor */
    public SbmlBiomodelsUploadTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(SbmlBiomodelsUploadTest.class.getName());
        suite.addTest(new SbmlBiomodelsUploadTest("testUpload"));
        return suite;
    }

    public void testUpload() throws Exception
    {
        URL bhv = new URL("http", proxyName, proxyPort, startPage);
        StringBuilder sbuffer = new StringBuilder();
        try(BufferedReader br = new BufferedReader(new InputStreamReader(bhv.openStream(), StandardCharsets.UTF_8)))
        {
            String line;
            while( ( line = br.readLine() ) != null )
            {
                sbuffer.append(line);
            }
        }
        String firstPage = sbuffer.toString();
        int currentPos = 0;
        while( ( currentPos = firstPage.indexOf("<a href", currentPos) ) != -1 )
        {
            int start = firstPage.indexOf('"', currentPos + 1) + 1;
            int end = firstPage.indexOf('"', start);
            String linkName = firstPage.substring(start, end);
            String targetName = null;
            if( ( targetName = getLink(linkName) ) != null )
            {
                uploadFile(targetName);
            }
            currentPos = end + 1;
        }
    }

    private String getLink(String inputLink)
    {
        //use only relative links
        if( inputLink.startsWith("http://") )
        {
            return null;
        }
        //check filter
        if( inputLink.indexOf(linkFilter) != -1 )
        {
            return startPage + inputLink;
        }
        return null;
    }

    private void uploadFile(String link) throws Exception
    {
        String name = link.substring(link.lastIndexOf('/') + 1);

        System.out.print(name + "\t");

        URL bhv = new URL("http", proxyName, proxyPort, link);
        ApplicationUtils.copyStream(new FileOutputStream(new File(csvOutDirectory, name)), bhv.openStream());
        System.out.println("ok");
    }
}
