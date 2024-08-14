package biouml.plugins.research.workflow.yaml;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import one.util.streamex.StreamEx;

import org.yaml.snakeyaml.Yaml;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.table.TableDataCollection;
import biouml.plugins.research.workflow.items.DataElementType;
import biouml.plugins.research.workflow.items.VariableType;
import biouml.plugins.research.workflow.items.WorkflowParameter;
import biouml.plugins.research.workflow.items.WorkflowParameterBeanInfo.DropDownOptionsSelector;

/**
 * Validates YAML representation of workflow
 */
public class YamlValidator
{
    public boolean validate(String text)
    {
        Yaml parser = new Yaml();
        Object root;
        try
        {
            root = parser.load( text );
        }
        catch( Exception e )
        {
            return false;
        }
        if( root == null )
            return false;
        if( ! ( root instanceof Map ) )
            return false;

        Map<?, ?> rootMap = (Map<?, ?>)root;
        return validate( rootMap );
    }

    public boolean validate(Map<?, ?> rootMap)
    {
        Set<String> allowedKeys = new HashSet<>();
        allowedKeys.add( "name" );
        allowedKeys.add( "description" );
        allowedKeys.add( "parameters" );
        allowedKeys.add( "expressions" );
        allowedKeys.add( "tasks" );
        allowedKeys.add( "dependencies" );
        allowedKeys.add( "cycles" );
        for( Object key : rootMap.keySet() )
        {
            if( ! ( key instanceof String ) )
                return false;
            if( !allowedKeys.contains( key ) )
                return false;
        }

        if( rootMap.get( "name" ) == null )
            return false;

        Object description = rootMap.get( "description" );
        if( description != null && ! ( description instanceof String ) )
            return false;

        if( rootMap.containsKey( "parameters" ) )
        {
            Object parameters = rootMap.get( "parameters" );
            if( !validateParameters( parameters ) )
                return false;
        }

        if( rootMap.containsKey( "expressions" ) )
        {
            Object expressions = rootMap.get( "expressions" );
            if( !validateExpressions( expressions ) )
                return false;
        }

        if( rootMap.containsKey( "tasks" ) )
        {
            Object tasks = rootMap.get( "tasks" );
            if( !validateTasks( tasks ) )
                return false;
        }

        return true;
    }

    private boolean validateTasks(Object obj)
    {
        if( ! ( obj instanceof Map ) )
            return false;
        Map<?,?> tasks = (Map<?,?>)obj;
        for( Map.Entry<?, ?> e : tasks.entrySet() )
        {
            if(!(e.getKey() instanceof String ))
                return false;
            //TODO:validate analysis name
            if( !validateTask( e.getValue() ) )
                return false;
        }
        return true;
    }

    private boolean validateTask(Object taskObj)
    {
        if( ! ( taskObj instanceof Map ) )
            return false;
        Map<?, ?> analysis = (Map<?, ?>)taskObj;
        Set<String> allowedKeys = new HashSet<>();
        allowedKeys.add( "in" );
        allowedKeys.add( "out" );
        allowedKeys.add( "param" );
        
        //TODO: validate analysis parameters
        return StreamEx.ofKeys( analysis ).allMatch( allowedKeys::contains );
    }

    private boolean validateExpressions(Object obj)
    {
        if( ! ( obj instanceof Map ) )
            return false;
        Map<?, ?> expressions = (Map<?, ?>)obj;
        for( Map.Entry<?, ?> e : expressions.entrySet() )
        {
            if( ! ( e.getKey() instanceof String ) )
                return false;
            if( !validateExpression( e.getValue() ) )
                return false;
        }
        return true;
    }

    private boolean validateExpression(Object obj)
    {
        if( ! ( obj instanceof Map ) )
            return false;
        Map<?, ?> map = (Map<?, ?>)obj;

        Set<String> allowedKeys = new HashSet<>();
        
        Object type = map.get( "type" );
        if( type != null )
        {
            if( ! ( type instanceof String ) )
                return false;
            VariableType variableType = VariableType.getTypeOrNull( (String)type );
            if( variableType == null )
                return false;
            allowedKeys.add( "type" );
        }

        Object expression = map.get( "expression" );
        if( expression != null )
        {
            if( ! ( expression instanceof String ) )
                return false;
            allowedKeys.add( "expression" );
        }

        return StreamEx.ofKeys( map ).allMatch( allowedKeys::contains );
    }

    private boolean validateParameters(Object obj)
    {
        if( ! ( obj instanceof Map ) )
            return false;
        Map<?, ?> parametersMap = (Map<?, ?>)obj;
        for( Map.Entry<?, ?> e : parametersMap.entrySet() )
        {
            if( ! ( e.getKey() instanceof String ) )
                return false;
            if( !validateParameter( e.getValue() ) )
                return false;
        }
        return true;
    }

    private boolean validateParameter(Object obj)
    {
        if( ! ( obj instanceof Map ) )
            return false;
        Map<?, ?> paramMap = (Map<?, ?>)obj;

        Set<String> allowedKeys = new HashSet<>();

        allowedKeys.add( "description" );
        Object description = paramMap.get( "description" );
        if( description != null && ! ( description instanceof String ) )
            return false;

        allowedKeys.add( "defaultValue" );
        Object defaultValue = paramMap.get( "defaultValue" );
        if( defaultValue != null && ! ( defaultValue instanceof String ) )
            return false;

        VariableType variableType = VariableType.getType( "Data element" );
        Object type = paramMap.get( "type" );
        if( type != null )
        {
            if( ! ( type instanceof String ) )
                return false;
            variableType = VariableType.getTypeOrNull( (String)type );
            if( variableType == null )
                return false;
            allowedKeys.add( "type" );
        }
        else
            type = "Data element";

        if( variableType.getTypeClass().equals( DataElementPath.class ) || variableType.getTypeClass().equals( DataElementPathSet.class ) )
        {
            allowedKeys.add( "role" );
            if( paramMap.containsKey( "role" ) )
            {
                Object role = paramMap.get( "role" );
                if( ! ( role instanceof String ) )
                    return false;
                if( !Arrays.asList( WorkflowParameter.ROLES ).contains( role ) )
                    return false;
            }
            allowedKeys.add( "elementType" );
            if( paramMap.containsKey( "elementType" ) )
            {
                Object elementType = paramMap.get( "elementType" );
                if( ! ( elementType instanceof String ) )
                    return false;
                DataElementType deType = DataElementType.getTypeOrNull( (String)elementType );
                if( deType == null )
                    return false;
                if( deType.getTypeClass().equals( TableDataCollection.class ) )
                {
                    allowedKeys.add( "referenceType" );
                    if( paramMap.containsKey( "referenceType" ) )
                    {
                        Object referenceType = paramMap.get( "referenceType" );
                        if( ! ( referenceType instanceof String ) )
                            return false;
                        /*String[] possibleValues = new ReferenceTypeSelector().getTags();
                        if( !Arrays.asList( possibleValues ).contains( referenceType ) )
                            return false;*/
                    }
                }
            }
        }

        if( type.equals( "String" ) || type.equals( "Multiple strings" ) )
        {
            allowedKeys.add( "dropDownOptions" );
            if( paramMap.containsKey( "dropDownOptions" ) )
            {
                Object dropDownOptions = paramMap.get( "dropDownOptions" );
                String[] possibleValues = new DropDownOptionsSelector().getTags();
                if( !Arrays.asList( possibleValues ).contains( dropDownOptions ) )
                    return false;
            }
        }

        return StreamEx.ofKeys( paramMap ).allMatch( allowedKeys::contains );
    }
}
