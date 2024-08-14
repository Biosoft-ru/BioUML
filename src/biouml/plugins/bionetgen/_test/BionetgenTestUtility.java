package biouml.plugins.bionetgen._test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.StringTokenizer;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.Preferences;

import biouml.model.Diagram;
import biouml.plugins.bionetgen.bnglparser.BNGStart;
import biouml.plugins.bionetgen.bnglparser.BionetgenParser;
import biouml.plugins.bionetgen.diagram.BionetgenDiagramGenerator;
import ru.biosoft.access.core.CollectionFactory;

public class BionetgenTestUtility
{
    public static final String TEST_FILE_NAME = "testFile";
    private static BionetgenParser parser = new BionetgenParser();

    public static BNGStart readDiagram(String fileName, String modelName) throws Exception
    {
        try( FileInputStream is = new FileInputStream(fileName);
                InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8) )
        {
            BNGStart start = parser.parse(modelName, reader);
            if( parser.getStatus() != BionetgenParser.STATUS_OK )
            {
                throw new Exception("Parsing Exception in " + fileName + ": " + parser.getMessages());
            }
            return start;
        }
    }

    public static Diagram generateDiagram(String fileName, String modelName, boolean needLayout) throws Exception
    {
        BNGStart start = readDiagram(fileName, modelName);
        return BionetgenDiagramGenerator.generateDiagram( start, null, modelName, needLayout );
    }

    public static void initPreferences() throws Exception
    {
        CollectionFactory.createRepository("../data");
        Application.setPreferences(new Preferences());
    }

    public static HashMap<String, double[]> readResults(String modelName) throws IOException
    {
        List<String> names = new ArrayList<>();
        List<double[]> strValues = new ArrayList<>();
        HashMap<String, double[]> result = new LinkedHashMap<>();
        try( FileInputStream is = new FileInputStream(modelName);
                InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(reader) )
        {
            String line = br.readLine();
            StringTokenizer firstLine;
            if( line != null )
                firstLine = new StringTokenizer(line, " ");
            else
                throw new IllegalArgumentException("File mustn't be empty: empty file " + modelName);

            while( firstLine.hasMoreTokens() )
                names.add(firstLine.nextToken());
            int dim = names.size();

            while( ( line = br.readLine() ) != null )
            {
                StringTokenizer strTok = new StringTokenizer(line, " ");
                if( strTok.countTokens() != dim )
                    continue;
                double[] strData = new double[dim];
                for( int i = 0; i < dim; i++ )
                {
                    strData[i] = Double.parseDouble(strTok.nextToken());
                }
                strValues.add(strData);
            }

            for( int i = 0; i < dim; i++ )
            {
                double[] currentData = new double[strValues.size()];
                for( int j = 0; j < strValues.size(); j++ )
                {
                    currentData[j] = strValues.get(j)[i];
                }
                result.put(names.get(i), currentData);
            }
            return result;
        }
    }

    public static String readFile(String fileName)
    {
        try
        {
            File file = new File(fileName);
            return ApplicationUtils.readAsString(file);
        }
        catch( Exception e )
        {
            return ( "Can not read file '" + fileName + "', error: " + e.getMessage() );
        }
    }

    public static File createTestFile(String dir, String content) throws Exception
    {
        File file = new File(dir + TEST_FILE_NAME);
        File parentFile = file.getParentFile();
        if( !parentFile.exists() && !parentFile.mkdirs() )
            throw new Exception("Failed to cteate necessary directory");
        ApplicationUtils.writeString(file, content);
        return file;
    }

}
