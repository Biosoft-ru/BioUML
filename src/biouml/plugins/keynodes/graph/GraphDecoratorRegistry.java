package biouml.plugins.keynodes.graph;

import one.util.streamex.StreamEx;

import biouml.plugins.keynodes.biohub.KeyNodesHub;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.TargetOptions.CollectionRecord;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.exception.ParameterNotAcceptableException;
import ru.biosoft.util.ObjectExtensionRegistry;
import ru.biosoft.util.TextUtil2;

public class GraphDecoratorRegistry
{
    public static final String DECORATOR_PREFIX = "KeyNodeDecorator";
    public static final String DECORATOR_PROPERTY = "DecoratorParameters";

    @SuppressWarnings ( "rawtypes" )
    private static final ObjectExtensionRegistry<GraphDecorator> decorators = new ObjectExtensionRegistry<>(
            "biouml.plugins.keynodes.keyNodesDecorator", "name", GraphDecorator.class );

    public static CollectionRecord createCollectionRecord(String decoratorName, GraphDecoratorParameters parameters)
    {
        GraphDecorator<?> decorator = decorators.getExtension( decoratorName );
        if(decorator == null)
        {
            throw new ParameterNotAcceptableException( "decoratorName", decoratorName );
        }
        if(decorator.getParametersClass() != parameters.getClass())
        {
            throw new ParameterNotAcceptableException( "parameters", parameters.getClass().getName() );
        }
        return new GraphDecoratorRecord( decoratorName, parameters );
    }

    @SuppressWarnings ( "unchecked" )
    public static <N> HubGraph<N> decorate(HubGraph<N> orig, ElementConverter<N> converter, CollectionRecord cr)
    {
        if(!(cr instanceof GraphDecoratorRecord))
            return orig;
        GraphDecoratorRecord gdr = (GraphDecoratorRecord)cr;
        @SuppressWarnings ( "rawtypes" )
        GraphDecorator decorator = decorators.getExtension( gdr.getDecoratorName() );
        if(decorator == null)
        {
            throw new ParameterNotAcceptableException( "decorator", cr.getPath().toString() );
        }
        GraphDecoratorParameters params = gdr.getParameters();
        return decorator.decorate( orig, converter, params );
    }

    public static StreamEx<String> decorators(KeyNodesHub<?> hub)
    {
        return decorators.names().filter( name -> decorators.getExtension( name ).isAcceptable( hub ) );
    }

    public static GraphDecoratorParameters createParameters(String decoratorName)
    {
        GraphDecorator<?> decorator = decorators.getExtension( decoratorName );
        if(decorator == null)
        {
            throw new ParameterNotAcceptableException( "decoratorName", decoratorName );
        }
        try
        {
            return decorator.getParametersClass().newInstance();
        }
        catch( InstantiationException | IllegalAccessException e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

    public static class GraphDecoratorRecord extends CollectionRecord
    {
        private final GraphDecoratorParameters params;
        private final String name;

        public GraphDecoratorRecord(String decoratorName, GraphDecoratorParameters params)
        {
            super( DataElementPath.create(DECORATOR_PREFIX, decoratorName), true );
            this.params = params;
            this.name = decoratorName;
        }

        public String getDecoratorName()
        {
            return name;
        }

        @Override
        public String getAsText()
        {
            return new JsonObject().add("name", name).add( "parameters", TextUtil2.toString( params ) ).toString();
        }

        public GraphDecoratorParameters getParameters()
        {
            return params;
        }

        public static CollectionRecord createInstance(String serialized)
        {
            try
            {
                JsonObject obj = JsonObject.readFrom( serialized );
                String name = obj.get( "name" ).asString();
                GraphDecorator<?> decorator = decorators.getExtension( name );
                if(decorator == null)
                    throw new ParameterNotAcceptableException( "name", name );
                GraphDecoratorRecord result = new GraphDecoratorRecord( name, (GraphDecoratorParameters)TextUtil2.fromString(
                        decorator.getParametersClass(), obj.get( "parameters" ).asString() ) );
                return result;
            }
            catch( ParseException | UnsupportedOperationException | ParameterNotAcceptableException e )
            {
                throw new ParameterNotAcceptableException( e, "serialized", serialized );
            }
        }
    }

    public static GraphDecorator<?> getDecorator(String decoratorName)
    {
        return decorators.getExtension( decoratorName );
    }
}
