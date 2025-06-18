package biouml.plugins.wdl.parser.validator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Version validator (WDL 1.2) 
 * <br>
 * Contains utils for validation
 * @author Alisa
 *
 */

@SuppressWarnings ( "serial" )
public class VersionValidator1_2 extends VersionValidator
{

    private Map<String, List<String>> reqToType = new HashMap<String, List<String>>()
    {
        {
            put("container", Arrays.asList("String", "Array[String]", "*"));
            put("docker", Arrays.asList("String", "Array[String]", "*"));
            put("cpu", Arrays.asList("Int", "Float"));
            put("memory", Arrays.asList("Int", "String"));
            put("gpu", Arrays.asList("Boolean"));
            put("fpga", Arrays.asList("Boolean"));
            put("disks", Arrays.asList("Int", "String", "Array[String]"));
            put("maxRetries", Arrays.asList("Int"));
            put("max_retries", Arrays.asList("Int"));
            put("returnCodes", Arrays.asList("Int", "Array[Int]", "*"));
            put("return_codes", Arrays.asList("Int", "Array[Int]", "*"));
        }
    };


    public Map<String, List<String>> hintToType = new HashMap<String, List<String>>()
    {
        {
            put("maxCpu", Arrays.asList("Int", "Float"));
            put("max_cpu", Arrays.asList("Int", "Float"));
            put("maxMemory", Arrays.asList("Int", "String"));
            put("max_memory", Arrays.asList("Int", "String"));
            put("disks", Arrays.asList("String", "Map[String,String]"));
            put("gpu", Arrays.asList("Int", "String"));
            put("fpga", Arrays.asList("Int", "String"));
            put("short_task", Arrays.asList("Boolean"));
            put("localizationOptional", Arrays.asList("Boolean"));
            put("localization_optional", Arrays.asList("Boolean"));
            put("inputs", Arrays.asList("object"));
            put("outputs", Arrays.asList("object"));
        }
    };


    public void checkRuntimeAttributes(String name, String type) throws Exception
    {
        List<String> types = reqToType.get(name);

        if( types == null )
            throw new Exception("Attribute " + name
                    + " is not supported. The requirements are limited to the attributes defined in the specification!");

        if( !types.contains(type) )
            throw new Exception("Types are incompatible!");

    }

    public void checkHintAttributes(String name, String type) throws Exception
    {
        List<String> types = reqToType.get(name);

        if( types == null )
            return;

        if( !types.contains(type) )
            throw new Exception("Types are incompatible!");

    }



}
