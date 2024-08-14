package biouml.plugins.simulation_test;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import ru.biosoft.util.TextUtil;

import com.developmentontheedge.application.ApplicationUtils;

public class BiomodelsPlotLinksLoader
{
    protected static final String fileName = "plot_links.txt";
    protected static final String linkTemplate = "http://www.ebi.ac.uk/compneur-srv/biomodels-main/simulation-result.do?uri=publ-model.do&mid=BIOMD0000000000";
    protected static final String nameTemplate = "BIOMD0000000000";

    private static final String proxyName = "developmentontheedge.com";
    private static final int proxyPort = 8080;

    private static BiomodelsPlotLinksLoader linksLoader;
    private static String targetDirectory;

    public static BiomodelsPlotLinksLoader getInstance(String directory)
    {
        if( linksLoader == null || !directory.equals(targetDirectory) )
        {
            targetDirectory = directory;
            linksLoader = new BiomodelsPlotLinksLoader();
        }
        return linksLoader;
    }

    protected Map<String, String> links;

    public BiomodelsPlotLinksLoader()
    {
        links = new HashMap<>();
        File file = new File(targetDirectory + fileName);
        try
        {
            if( file.exists() )
            {
                //just read values from file
                readLinksFromFile(file);
            }
            else
            {
                //try to find plot links
                findLinks();
                saveLinksToFile(file);
            }
        }
        catch( Exception e )
        {

        }
    }

    public String getPlotLinkForTest(String testName)
    {
        return links.get(testName);
    }

    protected void readLinksFromFile(File file) throws Exception
    {
        try(BufferedReader br = ApplicationUtils.utfReader( file ))
        {
            String line;
            while( ( line = br.readLine() ) != null )
            {
                String[] values = TextUtil.split( line, ' ' );
                if( values.length >= 2 )
                {
                    links.put(values[0].trim(), values[1].trim());
                }
            }
        }
    }

    protected void findLinks()
    {
        for( int i = 1; i <= 151; i++ )
        {
            try
            {
                String textNumber = String.valueOf(i);
                String startPage = linkTemplate.substring(0, linkTemplate.length() - textNumber.length()) + textNumber;

                URL bhv = new URL("http", proxyName, proxyPort, startPage);
                String page = ApplicationUtils.readAsString(bhv.openStream());
                int startPos = page.indexOf("<img src=\"");
                if( startPos > 0 )
                {
                    startPos += 10;
                    int endPos = page.indexOf("\"/>", startPos);
                    if( endPos > 0 )
                    {
                        String pictureLink = page.substring(startPos, endPos);
                        String name = nameTemplate.substring(0, nameTemplate.length() - textNumber.length()) + textNumber;
                        links.put(name, pictureLink);
                        System.out.println("Plot link found: " + name + " " + pictureLink);
                    }
                }
            }
            catch( Exception e )
            {
            }
        }
    }

    protected void saveLinksToFile(File file) throws Exception
    {
        try (PrintWriter pw = new PrintWriter( file ))
        {
            links.entrySet().forEach( entry -> pw.println( entry.getKey() + " " + entry.getValue() ) );
        }
    }
}
