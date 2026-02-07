package biouml.plugins.wdl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.developmentontheedge.application.ApplicationUtils;



public class DummyParamsGenerator
{
    public static void main(String[] args) throws IOException
    {
        File scriptFile = new File("test.nf");
        File configFile = new File("dummy.config");

        generateDummyConfig(scriptFile, configFile);

        System.out.println(ApplicationUtils.readAsString(configFile));
    }

    /**
     * Generate a dummy config file for dry-run/DAG generation
     */
    public static void generateDummyConfig(File scriptFile, File outputConfig) throws IOException
    {
        Set<String> params = extractParametersWithTypes(scriptFile);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputConfig)))
        {
            for( String paramName : params )
            {
                String dummyValue = generateDummyValue(paramName);
                bw.append("\nparams." + paramName + " = '" + dummyValue + "'");
            }
        }
    }

    private static Set<String> extractParametersWithTypes(File scriptFile) throws IOException
    {
        String content = ApplicationUtils.readAsString(scriptFile);
        Set<String> params = new HashSet<>();

        Pattern pattern = Pattern.compile("params\\.([a-zA-Z_][a-zA-Z0-9_]*)");
        Matcher matcher = pattern.matcher(content);

        while( matcher.find() )
        {
            String paramName = matcher.group(1);
            params.add(paramName);
        }

        return params;
    }

    private static String generateDummyValue(String paramName)
    {
        return "dummy_" + paramName;
    }
}
