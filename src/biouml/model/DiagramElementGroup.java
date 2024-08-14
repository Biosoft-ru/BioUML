package biouml.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import one.util.streamex.StreamEx;

public class DiagramElementGroup
{
    //TODO: remove EMPTY_EG
    public static final DiagramElementGroup EMPTY_EG = new DiagramElementGroup();
    private List<DiagramElement> elements = null;

    public DiagramElementGroup()
    {
        elements = new ArrayList<>();
    }

    public DiagramElementGroup(DiagramElement de)
    {
        elements = new ArrayList<>();
        if( de != null )
            elements.add( de );
    }

    public DiagramElementGroup(List<DiagramElement> deList)
    {
        elements = new ArrayList<>();
        elements.addAll( deList );
    }

    public DiagramElement get(int i)
    {
        if( elements != null && elements.size() > i )
            return elements.get( i );
        return null;
    }

    public DiagramElement getElement()
    {
        if( elements != null && elements.size() > 0 )
            return elements.get( 0 );
        return null;
    }

    public List<DiagramElement> getElements()
    {
        //TODO: null-check
        return elements;
    }

    public void addAll(List<DiagramElement> additional) throws Exception
    {
        if( elements == null )
            throw new Exception( "Can not add to empty diagram element group" );
        elements.addAll( additional );
    }

    public DiagramElement getElement(Predicate<? super DiagramElement> predicate)
    {
        if( elements == null )
            return null;
        return elements.stream().filter( predicate ).findFirst().orElse( null );
    }

    public StreamEx<Node> nodesStream()
    {
        if( elements == null )
            return StreamEx.empty();
        return StreamEx.of( elements ).select( Node.class );
    }

    public StreamEx<Edge> edgesStream()
    {
        if( elements == null )
            return StreamEx.empty();
        return StreamEx.of( elements ).select( Edge.class );
    }

    public void add(DiagramElement de) throws Exception
    {
        if( elements == null )
            throw new Exception( "Can not add to empty diagram element group" );
        elements.add( de );
    }

    public void putToCompartment()
    {
        if( elements == null || elements.size() == 0 )
            return;
        nodesStream().forEach( n -> n.getCompartment().put( n ) );
        edgesStream().forEach( e -> e.getCompartment().put( e ) );
    }

    public int size()
    {
        return elements.size();
    }

}
