package biouml.plugins.wdl.parser.validator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Version validator (WDL 1.0) 
 * <br>
 * Contains utils for validation
 * @author Alisa
 *
 */

// TODO: initialize during parsing
public class VersionValidator1_0 extends VersionValidator
{
    @SuppressWarnings ( "serial" )
    private Map<String, List<String>> reqToType = new HashMap<String, List<String>>()
    {
        {
            put("docker", Arrays.asList("String", "Array[String]"));
            put("memory", Arrays.asList("Int", "String"));
        }
    };

    public void checkRuntimeAttributes(String name, String type) throws Exception
    {
        List<String> types = reqToType.get(name);

        if( types == null )
            return;

        if( !types.contains(type) )
            throw new Exception("Types are incompatible!");
    }



}
