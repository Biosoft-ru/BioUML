package biouml.plugins.antimony;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.yaml.snakeyaml.Yaml;

import biouml.plugins.antimony.astparser_v2.AstImportAnnotation;
import biouml.plugins.antimony.astparser_v2.AstMap;
import biouml.plugins.antimony.astparser_v2.AstSingleProperty;
import biouml.plugins.antimony.astparser_v2.Node;
import biouml.plugins.antimony.astparser_v2.SimpleNode;
import ru.biosoft.access.TextDataElement;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;

/**
 * Importer of antimony annotation in yaml format
 */

public class AntimonyAnnotationImporter
{
    static Map<String, Map<String, Object>> annotations = new HashMap<String, Map<String, Object>>();

    @SuppressWarnings ( "unchecked" )
    private static Map<String, Object> getAnnotationMap(String source)
    {
        String yamlText = "";

        if( source.startsWith("http://") || source.startsWith("https://") )
        {
        }
        else
        {
            System.out.println("Try to load YAML file from " + source.toString() );
            TextDataElement tde = (TextDataElement)CollectionFactory.getDataElement(source.toString());
       
            yamlText = tde.getContent();
        }

        Yaml parser = new Yaml();

        Object root;
        try
        {
            root = parser.load(yamlText);
        }
        catch( Exception e )
        {
            return null;
        }

        if( ! ( root instanceof Map ) )
            return null;

        Map<?, ?> rootMap = (Map<?, ?>)root;
        if( !validate(rootMap) )
            return null;
        return (Map<String, Object>)rootMap;
    }

    private static boolean validate(Map<?, ?> rootMap)
    {
        Set<String> allowedKeys = new HashSet<>();
        allowedKeys.add("name");
        allowedKeys.add("description");
        allowedKeys.add("properties");
        for( Object key : rootMap.keySet() )
        {
            if( ! ( key instanceof String ) )
                return false;
            if( !allowedKeys.contains(key) )
                return false;
        }

        if( rootMap.containsKey("description") )
        {
            Object description = rootMap.get("description");
            if( ! ( description instanceof String ) )
                return false;
        }

        Object properties = rootMap.get("properties");
        if( properties != null && !validateProperties(properties) )
            return false;

        return true;
    }

    private static boolean validateProperties(Object obj)
    {
        if( ! ( obj instanceof List ) )
            return false;
        List<?> properties = (List<?>)obj;
        for( Object e : properties )
        {
            if( ! ( e instanceof Map ) )
                return false;
            Map<?, ?> property = (Map<?, ?>)e;
            for( Object key : property.keySet() )
            {
                if( ! ( key instanceof String ) )
                    return false;
                if( !validateProperty(property.get(key)) )
                    return false;
            }

        }
        return true;
    }

    private static boolean validateProperty(Object obj)
    {
        if( ! ( obj instanceof Map ) )
            return false;

        Map<?, ?> property = (Map<?, ?>)obj;

        Set<String> allowedKeys = new HashSet<>();
        allowedKeys.add("type");
        allowedKeys.add("values");
        allowedKeys.add("description");

        for( Object key : property.keySet() )
        {
            if( ! ( key instanceof String ) )
                return false;
            if( !allowedKeys.contains(key) )
                return false;
        }
        Set<String> allowedTypes = new HashSet<>();
        allowedTypes.add("int");
        allowedTypes.add("float");
        allowedTypes.add("string");
        allowedTypes.add("enum");
        allowedTypes.add("array");
        allowedTypes.add("enum");
        allowedTypes.add("map");
        allowedTypes.add("any");

        if( property.get("type") == null && allowedTypes.contains(property.get("type")) )
            return false;


        if( property.containsKey("values") )
        {
            Object values = property.get("values");
            if( !validateValues(values, property.get("type").toString()) )
                return false;
        }

        if( property.containsKey("description") )
        {
            Object description = property.get("description");
            if( ! ( description instanceof String ) )
                return false;
        }

        return true;
    }

    private static boolean validateValues(Object obj, String type)
    {
        if( "enum".equals(type) )
        {
            if( ! ( obj instanceof List ) )
                return false;
            List<?> values = (List<?>)obj;

            for( Object value : values )
            {
                if( ! ( value instanceof String ) )
                    return false;
            }
        }
        else if( "map".equals(type) )
        {
            if( ! ( obj instanceof Map ) )
                return false;

            Map<?, ?> values = (Map<?, ?>)obj;
            for( Object key : values.keySet() )
            {
                if( ! ( key instanceof String ) )
                    return false;
                if( !validateProperty(values.get(key)) )
                    return false;
            }
        }

        return true;
    }

    @SuppressWarnings ( "unchecked" )
    public static void setAnnotation(String path, String annotationType) throws Exception
    {
        if( annotations.containsKey(annotationType) )
            return;

        Map<String, Object> annotationMap = getAnnotationMap(path.toString());

        if( annotationMap == null )
            throw new Exception("Annotation is invalid");

        String annotationName = annotationMap.get("name").toString();

        for( Map.Entry<String, Object> entry : annotationMap.entrySet() )
        {
            if( "properties".equals(entry.getKey()) )
            {
                Map<String, Object> properties = new HashMap<String, Object>();
                for( Map<String, Object> prop : (List<Map<String, Object>>)entry.getValue() )
                {
                    for( String key : prop.keySet() )
                    {
                        Map<String, Object> propAttributes = new HashMap<String, Object>();
                        Map<String, Object> attributes = (Map<String, Object>)prop.get(key);
                        for( Map.Entry<String, Object> attr : attributes.entrySet() )
                        {
                            if( "type".equals(attr.getKey()) )
                                propAttributes.put("type", attr.getValue());
                            else if( "values".equals(attr.getKey()) )
                                propAttributes.put("values", attr.getValue());

                        }
                        properties.put(key, propAttributes);
                    }
                }
                annotations.put(annotationName, properties);
            }
        }

    }

    public static boolean isPropertyImported(String attributeToCheck)
    {
        String notationType;
        if( attributeToCheck.startsWith("sbgn") )
            notationType = "sbgn";
        else if( attributeToCheck.startsWith("glycan") )
            notationType = "glycan";
        else if( attributeToCheck.startsWith("smiles") )
            notationType = "smiles";
        else if( attributeToCheck.startsWith("biouml") )
            notationType = "biouml";
        else
            return false;

        if( !annotations.containsKey(notationType) )
            return false;

        Map<String, Object> properties = annotations.get(notationType);

        String attributeName = attributeToCheck;
        if( attributeName.endsWith("EdgeType") )
            attributeName = "edgeType";
        else if( attributeName.endsWith("ReactionType") )
            attributeName = "reactionType";
        else if( attributeName.endsWith("Type") )
            attributeName = "type";
        else if( attributeName.endsWith("Structure") )
            attributeName = "structure";
        else if( attributeName.endsWith("multimer") )
            attributeName = "multimer";
        else if( attributeName.endsWith("title") )
            attributeName = "title";
        else if( attributeName.contains("clone") )
            attributeName = "clone";
        else if( attributeName.endsWith("Bus") )
            attributeName = "bus";

        if( !properties.containsKey(attributeName) )
            return false;

        return true;
    }

    @SuppressWarnings ( "unchecked" )
    public static void validatePropertyValue(AstSingleProperty sp, String notationType) throws Exception
    {
        Map<String, Object> properties = annotations.get(notationType);

        String propertyName = sp.getPropertyName();

        if( !properties.containsKey(propertyName) )
            throw new Exception("Property \"" + propertyName + "\" was not imported");

        String propertyType = ( (Map<String, Object>)properties.get(propertyName) ).get("type").toString();

        if( "enum".equals(propertyType) )
        {
            List<String> list = (List<String>) ( (Map<String, Object>)properties.get(propertyName) ).get("values");
            if( sp.getPropertyValue() instanceof String && !list.contains(sp.getPropertyValue().toString()) )
                throw new Exception("Property value \"" + sp.getPropertyValue() + "\" is invalid");
            return;
        }
        else if( "map".equals(propertyType) )
        {
            Map<String, Object> m = (Map<String, Object>) ( (Map<String, Object>)properties.get(propertyName) ).get("values");
            for( Node child : sp.getChildren() )
            {
                if( child instanceof AstMap )
                {
                    for( Node node : ( (SimpleNode)child ).getChildren() )
                    {
                        if( node instanceof AstSingleProperty && !m.containsKey( ( (AstSingleProperty)node ).getPropertyName()) )
                            throw new Exception("Key \"" + ( (AstSingleProperty)node ).getPropertyName() + "\" of property \""
                                    + ( (AstSingleProperty)sp ).getPropertyName() + "\" is invalid");
                    }
                    return;
                }
            }
        }


    }

    static DataElementPath diagramPath;


    static void setDiagramPath(DataElementPath path)
    {
        diagramPath = path;
    }


    public static void addAnnotation(AstImportAnnotation node) throws Exception
    {
        String path;
        if( node.getPath().contains("/") )
            path = node.getPath();
        else
            path = diagramPath.getChildPath(node.getPath()).toString();
        setAnnotation(path, node.getAnnotationType());
    }

    /****
     * Used for antimony parser
     * @param name
     * @return
     * @throws Exception 
     */
    public static void checkPropertyImport(String notationType, String propertyName) throws Exception
    {
        if( !annotations.containsKey(notationType) )
            throw new Exception("Notation type \"" + notationType + "\" was not imported");

        Map<String, Object> properties = annotations.get(notationType);

        if( !properties.containsKey(propertyName) )
            throw new Exception("Property \"" + propertyName + "\" was not imported");
    }


    public static boolean isPropertyImported(String notationType, String propertyName)
    {
        if( !isAnnotationImported(notationType) )
            return false;

        Map<String, Object> properties = annotations.get(notationType);

        if( !properties.containsKey(propertyName) )
            return false;

        return true;
    }

    public static boolean isAnnotationImported(String notationType)
    {
        return annotations.containsKey(notationType);
    }

}