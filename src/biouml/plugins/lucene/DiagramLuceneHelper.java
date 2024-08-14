package biouml.plugins.lucene;

import java.util.Arrays;
import java.util.Vector;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Reaction;
import biouml.standard.type.Referrer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

/**
 * Lucene helper implementation for diagrams
 */
public class DiagramLuceneHelper implements LuceneHelper
{
    protected DataCollection dc;

    public DiagramLuceneHelper(DataCollection dc)
    {
        this.dc = dc;
    }

    public static final String NAME = "name";
    public static final String TITLE = "title";
    public static final String DESCRIPTION = "description";
    public static final String STATISTICS = "statistics";
    public static final String COMPONENTS = "components";
    public static final String CHILD_NAMES = "__childNames";
    public static final String DB_REFS = "__dbRefs";

    @Override
    public Vector<String> getPropertiesNames()
    {
        Vector<String> v = new Vector<>();
        v.add(NAME);
        v.add(TITLE);
        v.add(DESCRIPTION);
        v.add(STATISTICS);
        v.add(COMPONENTS);
        v.add(CHILD_NAMES);
        v.add(DB_REFS);

        return v;
    }

    @Override
    public String getBeanValue(DataElement de, String name)
    {
        if( de instanceof Diagram )
        {
            Diagram diagram = (Diagram)de;
            if( NAME.equals(name) )
            {
                return diagram.getName();
            }
            else if( TITLE.equals(name) )
            {
                return diagram.getTitle();
            }
            else if( DESCRIPTION.equals(name) )
            {
                return ((DiagramInfo)diagram.getKernel()).getDescription();
            }
            else if( STATISTICS.equals(name) )
            {
                return diagram.recursiveStream().foldLeft( new Statistic(), Statistic::collect ).toString();
            }
            else if( COMPONENTS.equals(name) )
            {
                return diagram.recursiveStream().select( Node.class ).map( Node::getKernel ).select( Reaction.class )
                        .map( Reaction::getTitle ).distinct().joining( ", " );
            }
            else if( CHILD_NAMES.equals(name) )
            {
                return diagram.stream().flatMap( DiagramElement::recursiveStream ).map( DiagramElement::getName ).distinct().joining( ", " );
            }
            else if( DB_REFS.equals(name) )
            {
                return diagram.recursiveStream().map( DiagramElement::getKernel ).select( Referrer.class )
                        .map( Referrer::getDatabaseReferences ).nonNull().flatMap( Arrays::stream ).map( Object::toString ).distinct()
                        .joining( ", " );
            }
        }
        return null;
    }

    protected static class Statistic
    {
        public int componentCount;
        public int reactionCount;
        public int edgesCount;
        
        public Statistic collect(DiagramElement element)
        {
            if( element instanceof Node )
            {
                if( element.getKernel() instanceof Reaction )
                {
                    reactionCount++;
                }
                else
                {
                    componentCount++;
                }
            }
            else if( element instanceof Edge )
            {
                edgesCount++;
            }
            return this;
        }
        
        @Override
        public String toString() {
            return "components:" + componentCount + ", reactions:" + reactionCount + ", edges:" + edgesCount;
        }
    }
}
