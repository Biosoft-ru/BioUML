package biouml.plugins.research.workflow.yaml;

import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class YamlParser
{
    // Cache the Yaml instance — SnakeYAML construction is expensive
    // (sets up parser, constructor, emitter, etc.) and the instance is thread-safe for load() calls.
    private static final Yaml CACHED_YAML = new Yaml();

    @SuppressWarnings ( "unchecked" )
    public Map<String, Object> parseYaml(String text)
    {
        Object root;
        try
        {
            root = CACHED_YAML.load( text );
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
