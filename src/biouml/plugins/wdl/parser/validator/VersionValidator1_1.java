package biouml.plugins.wdl.parser.validator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Version validator (WDL 1.1) 
 * <br>
 * Contains utils for validation
 * @author Alisa
 *
 */
@SuppressWarnings ( "serial" )
public class VersionValidator1_1 extends VersionValidator
{

    private Map<String, List<String>> reqToType = new HashMap<String, List<String>>()
    {
        {
            put("container", Arrays.asList("String", "Array[String]"));
            put("docker", Arrays.asList("String", "Array[String]"));
            put("cpu", Arrays.asList("Int", "Float"));
            put("memory", Arrays.asList("Int", "String"));
            put("gpu", Arrays.asList("Boolean"));
            put("disks", Arrays.asList("Int", "String", "Array[String]"));
            put("maxRetries", Arrays.asList("Int"));
            put("returnCodes", Arrays.asList("Int", "Array[Int]", "*"));
        }
    };


    public Map<String, List<String>> hintToType = new HashMap<String, List<String>>()
    {
        {
            put("maxCpu", Arrays.asList("Int", "Float"));
            put("maxMemory", Arrays.asList("Int", "String"));
            put("shortTask", Arrays.asList("Boolean"));
            put("localizationOptional", Arrays.asList("Boolean"));
            put("inputs", Arrays.asList("object"));
            put("outputs", Arrays.asList("object"));
        }
    };


    public void checkRuntimeAttributes(String name, String type) throws Exception
    {
        List<String> types = reqToType.get(name);

        if( types == null )
        {
            types = hintToType.get(name);
            if( types != null )
                log.log(Level.WARNING, "The use of hint attributes in the runtime section is deprecated.");
            else
                return;
        }

        if( !types.contains(type) )
            throw new Exception("Types are incompatible!");

    }
}
