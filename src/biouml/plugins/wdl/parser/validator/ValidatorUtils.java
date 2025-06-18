package biouml.plugins.wdl.parser.validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidatorUtils
{
    static String getWorkflowName(String name) throws Exception
    {
        Pattern pattern = Pattern.compile("(?<=^|/)([a-zA-Z0-9_]+)(?=.wdl$|$)");
        Matcher matcher = pattern.matcher(name);

        if( matcher.find() )
            return matcher.group();

        throw new Exception("Invalid URI of imported workflow: " + name);
    }


}
