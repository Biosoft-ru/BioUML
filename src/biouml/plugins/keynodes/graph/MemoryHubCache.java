package biouml.plugins.keynodes.graph;

import java.util.Objects;
import java.util.function.Function;

import javax.annotation.Nonnull;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.biohub.TargetOptions.CollectionRecord;
import ru.biosoft.exception.InternalException;
import ru.biosoft.exception.MissingParameterException;
import ru.biosoft.util.Cache;
import biouml.plugins.keynodes.graph.GraphDecoratorRegistry.GraphDecoratorRecord;

public class MemoryHubCache<N>
{
    private Function<MultiStringKey, HubGraph<N>> cache;

    /**
     * Creates a cache for {@link HubGraph}s including decorated hubs
     * @param rootHubCreator function which can create hub by specified rootName string (usually species name)
     * @param converter converter to convert hub node to {@link Element} and vice versa
     */
    public MemoryHubCache(Function<String, HubGraph<N>> rootHubCreator, ElementConverter<N> converter)
    {
        cache = Cache.soft( name -> {
            if(name.getParent().isRoot())
            {
                return rootHubCreator.apply( name.getLast() );
            } else
            {
                CollectionRecord decoratorOptions = GraphDecoratorRecord.createInstance( name.getLast() );
                return GraphDecoratorRegistry.decorate( cache.apply( name.getParent() ), converter, decoratorOptions );
            }
        } );
    }

    /**
     * Returns a HubGraph by specified rootName with no additional options
     * @param rootName implementation-dependent string identifying the graph (usually a species name)
     * @return HubGraph
     */
    public @Nonnull HubGraph<N> get(String rootName)
    {
        return get( rootName, new TargetOptions(new String[0], true) );
    }

    /**
     * Returns a HubGraph by specified rootName with additional decorators
     * @param rootName implementation-dependent string identifying the graph (usually a species name)
     * @param options {@link TargetOptions} requested by client
     * @return HubGraph
     */
    public @Nonnull HubGraph<N> get(String rootName, TargetOptions options)
    {
        if(rootName == null)
            throw new MissingParameterException( "rootName" );
        MultiStringKey key = options.collections().select( GraphDecoratorRecord.class ).map( GraphDecoratorRecord::getAsText )
                .foldLeft( MultiStringKey.of( rootName ), MultiStringKey::add );
        HubGraph<N> hub = cache.apply( key );
        if(hub == null)
            throw new InternalException( "Unable to fetch BioHubGraph (key = "+key+")" );
        return hub;
    }

    static final class MultiStringKey
    {
        public static final MultiStringKey ROOT = new MultiStringKey();

        private final MultiStringKey parent;
        private final String last;
        private final int hashCode;

        private MultiStringKey()
        {
            this.parent = null;
            this.last = null;
            this.hashCode = 0;
        }

        private MultiStringKey(MultiStringKey parent, String last)
        {
            Objects.requireNonNull( parent );
            Objects.requireNonNull( last );
            this.parent = parent;
            this.last = last;
            this.hashCode = parent.hashCode * 31 + last.hashCode();
        }

        public MultiStringKey add(String str)
        {
            return new MultiStringKey( this, str );
        }

        public boolean isRoot()
        {
            return last == null;
        }

        public MultiStringKey getParent()
        {
            return parent;
        }

        public String getLast()
        {
            return last;
        }

        @Override
        public int hashCode()
        {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj)
        {
            if( this == obj )
                return true;
            if( obj == null )
                return false;
            if( getClass() != obj.getClass() )
                return false;
            MultiStringKey other = (MultiStringKey)obj;
            if( hashCode != other.hashCode )
                return false;
            if( last == null )
            {
                if( other.last != null )
                    return false;
            }
            else if( !last.equals( other.last ) )
                return false;
            if( parent == null )
            {
                if( other.parent != null )
                    return false;
            }
            else if( !parent.equals( other.parent ) )
                return false;
            return true;
        }

        public static MultiStringKey of(String string)
        {
            return ROOT.add( string );
        }

        @Override
        public String toString()
        {
            if(parent == null)
                return "";
            if(parent.parent == null)
                return last;
            return parent.toString() + ":" + last;
        }
    }
}
