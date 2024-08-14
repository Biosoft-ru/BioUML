package biouml.plugins.research.workflow.yaml;

import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class YamlParser
{
    @SuppressWarnings ( "unchecked" )
    public Map<String, Object> parseYaml(String text)
    {
        Yaml parser = new Yaml();

        Object root;
        try
        {
            root = parser.load( text );
        }
        catch( Exception e )
        {
            return null;
        }
        if( root == null )
            return null;
        if( ! ( root instanceof Map ) )
            return null;

        Map<?, ?> rootMap = (Map<?, ?>)root;
        YamlValidator validator = new YamlValidator();
        if( !validator.validate( rootMap ) )
            return null;
        return (Map<String, Object>)rootMap;
    }
}
