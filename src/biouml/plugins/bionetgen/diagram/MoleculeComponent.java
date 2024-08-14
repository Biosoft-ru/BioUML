package biouml.plugins.bionetgen.diagram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import ru.biosoft.exception.InternalException;
import ru.biosoft.util.ObjectCache;

public class MoleculeComponent implements Comparable<MoleculeComponent>, Cloneable
{
    private static final AtomicInteger hashCounter = new AtomicInteger();
    private int hashCode = hashCounter.incrementAndGet();
    private String name = "";
    private String label = "";
    private String state = "";
    private int edgeWildcard = 0;
    private List<String> edges = Collections.emptyList();
    private int localHashCode = 0;

    private BionetgenMolecule molecule;

    public MoleculeComponent(BionetgenMolecule molecule, String component) throws Exception
    {
        this.molecule = molecule;

        String[] nameParts = ParseUtils.parseName(component);
        if( nameParts == null )
            throw new IllegalArgumentException("Invalid component format: '" + component + "'");

        this.name = getStringCache().get(nameParts[0]);

        if( !nameParts[1].isEmpty() )
        {
            try
            {
                defineState(nameParts[1]);
            }
            catch( Exception e )
            {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    protected ObjectCache<String> getStringCache()
    {
        return molecule.getStringCache();
    }

    private void addEdgeInternal(String e)
    {
        if( edges.isEmpty() )
            edges = new ArrayList<>();
        edges.add(getStringCache().get(e));
    }

    private void defineState(String component) throws Exception
    {
        String[] stateParts = ParseUtils.parseState(component);

        if( stateParts == null )
            throw new IllegalArgumentException("Invalid component format: '" + this + component + "'");

        char c = stateParts[0].charAt(0);

        switch( c )
        {
            case '~':
                if( !state.isEmpty() )
                    throw new IllegalArgumentException("Multiple state definition: '" + this + component + "'");
                if( stateParts[1].startsWith("+") || stateParts[1].startsWith("?") )
                    throw new IllegalArgumentException("Invalid component format: '" + this + component + "'");

                setState(stateParts[1]);
                break;
            case '!':
                if( stateParts[1].equals("+") || stateParts[1].equals("?") )
                {
                    if( edgeWildcard != 0 )
                        throw new IllegalArgumentException("Multiple edge wildcard definition: '" + this + component + "'");

                    edgeWildcard = 1;
                    addEdgeInternal(stateParts[1]);
                }
                else
                {
                    addEdgeInternal(stateParts[1]);
                }
                break;
            case '%':
                if( !label.isEmpty() )
                    throw new IllegalArgumentException("Multiple label definition: '" + this + component + "'");
                if( stateParts[1].startsWith("+") || stateParts[1].startsWith("?") )
                    throw new IllegalArgumentException("Invalid component format: '" + this + component + "'");

                label = getStringCache().get(stateParts[1]);
                break;
            default:
                throw new Exception("Unknown matching error: '" + this + component + "'");
        }

        if( !stateParts[2].isEmpty() )
            this.defineState(stateParts[2]);
    }

    public void changeEdgeToWildcard(String edgeNumber)
    {
        int pos = edges.indexOf(edgeNumber);
        if( pos >= 0 )
        {
            edgeWildcard = 1;
            edges.set(pos, "+");
            resetHashCode();
        }
    }

    private void resetHashCode()
    {
        localHashCode = 0;
        molecule.resetHashCode();
    }

    public void removeEdgesNumbers()
    {
        resetHashCode();
        if( edgeWildcard == 0 )
        {
            edges = Collections.emptyList();
        }
        else if( edges.contains("+") )
        {
            edges = new ArrayList<>();
            edges.add("+");
        }
        else if( edges.contains("?") )
        {
            edges = new ArrayList<>();
            edges.add("?");
        }
        else
        {
            throw new RuntimeException("Conflict in edge wildcard in: '" + this + "'");
        }
    }

    public MoleculeComponent copyAndRemoveEdgesNumbers(BionetgenMolecule newParent)
    {
        MoleculeComponent copy;
        try
        {
            copy = (MoleculeComponent)clone();
        }
        catch( CloneNotSupportedException e )
        {
            throw new InternalException(e);
        }
        copy.molecule = newParent;
        copy.removeEdgesNumbers();
        return copy;
    }

    public void addEdge(String edge)
    {
        resetHashCode();
        if( edge.equals("+") || edge.equals("?") )
        {
            if( edgeWildcard == 0 )
            {
                edgeWildcard = 1;
                addEdgeInternal(edge);
            }
            else if( edges.contains("?") && edge.equals("+") )
            {
                edges.set(edges.indexOf("?"), "+");
            }
        }
        else if( !edges.contains(edge) )
            addEdgeInternal(edge);
    }

    public static boolean compareLocal(MoleculeComponent sc1, MoleculeComponent sc2)
    {
        if( sc1.localHashCode() != sc2.localHashCode() || !sc1.name.equals(sc2.name) || !sc1.state.equals(sc2.state)
                || sc1.edges.size() != sc2.edges.size() || sc1.edgeWildcard != sc2.edgeWildcard )
            return false;

        return true;
    }

    public int localHashCode()
    {
        if( localHashCode == 0 )
        {
            localHashCode = name.hashCode() * 1371 + state.hashCode() * 17537 + edges.size() * 37 + edgeWildcard;
        }
        return localHashCode;
    }

    public BionetgenMolecule getMolecule()
    {
        return molecule;
    }

    public List<String> getEdges()
    {
        return Collections.unmodifiableList(edges);
    }

    public String getState()
    {
        return state;
    }

    public void setState(String state)
    {
        if( !this.state.equals(state) )
        {
            this.state = getStringCache().get(state);
            resetHashCode();
        }
    }

    public String getName()
    {
        return name;
    }

    public String getLabel()
    {
        return label;
    }

    public int getEdgeWildcard()
    {
        return edgeWildcard;
    }


    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(name);
        if( !state.isEmpty() )
            sb.append('~').append(state);
        if( !label.isEmpty() )
            sb.append('%').append(label);
        for( String edge : edges )
        {
            sb.append('!').append(edge);
        }
        return sb.toString();
    }

    @Override
    public int compareTo(MoleculeComponent o)
    {
        int result = name.compareTo(o.name);
        if( result != 0 )
            return result;
        result = label.compareTo(o.label);
        if( result != 0 )
            return result;
        result = edgeWildcard - o.edgeWildcard;
        if( result != 0 )
            return result;
        result = state.compareTo(o.state);
        if( result != 0 )
            return result;
        int minEdges = Math.min(edges.size(), o.edges.size());
        for( int i = 0; i < minEdges; i++ )
        {
            String e1 = edges.get(i);
            String e2 = o.edges.get(i);
            result = e1.compareTo(e2);
            if( result != 0 )
                return result;
        }
        return edges.size() - o.edges.size();
    }

    @Override
    public int hashCode()
    {
        return hashCode;
    }
}