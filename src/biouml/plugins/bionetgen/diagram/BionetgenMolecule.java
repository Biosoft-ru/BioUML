package biouml.plugins.bionetgen.diagram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckForNull;
import one.util.streamex.StreamEx;

import ru.biosoft.exception.InternalException;
import ru.biosoft.util.ObjectCache;
import ru.biosoft.util.TextUtil2;

public class BionetgenMolecule implements Comparable<BionetgenMolecule>, Cloneable
{
    private String name = "";
    private List<MoleculeComponent> moleculeComponents = Collections.emptyList();
    private String state = "";
    private int edgeWildcard = 0;
    private String label = "";
    private List<String> edges = Collections.emptyList();
    private boolean needBraces = false;
    int localHashCode = 0;

    private BionetgenSpeciesGraph speciesGraph;

    public BionetgenMolecule(BionetgenSpeciesGraph speciesGraph, String molecule)
    {
        this.speciesGraph = speciesGraph;
        String[] specieParts = TextUtil2.split(molecule, '(');
        String tail = "";

        if( specieParts.length > 2 )
            throw new IllegalArgumentException("Invalid molecule format (too many right parentheses): '" + molecule + "'");
        if( specieParts.length == 1 && specieParts[0].contains(")") )
            throw new IllegalArgumentException("Invalid molecule format (parentheses are not balanced): '" + molecule + "'");

        if( specieParts.length == 2 )
        {
            if( !specieParts[1].contains(")") )
                throw new IllegalArgumentException("Invalid molecule format (parentheses are not balanced): '" + molecule + "'");
            else if( specieParts[1].equals(")") )
            {
                needBraces = true;
            }
            else
            {
                String[] speciePart = TextUtil2.split(specieParts[1], ')');
                if( speciePart.length > 2 )
                    throw new IllegalArgumentException("Invalid molecule format (too many left parentheses): '" + molecule + "'");
                if( speciePart.length == 2 && !speciePart[1].isEmpty() )
                    tail = speciePart[1];
                String components[] = TextUtil2.split(speciePart[0], ',');
                for( String component : components )
                {
                    try
                    {
                        if( moleculeComponents.isEmpty() )
                            moleculeComponents = new ArrayList<>();
                        moleculeComponents.add(new MoleculeComponent(this, component));
                    }
                    catch( Exception e )
                    {
                        throw new IllegalArgumentException(e.getMessage());
                    }
                }
            }

        }

        String[] nameParts = ParseUtils.parseName(specieParts[0]);
        if( nameParts == null )
            throw new IllegalArgumentException("Invalid molecule format: '" + molecule + "'");

        this.name = getStringCache().get(nameParts[0]);

        if( !nameParts[1].isEmpty() )
        {
            try
            {
                defineMoleculeState(nameParts[1]);
            }
            catch( Exception e )
            {
                throw new IllegalArgumentException("Invalid molecule format: " + e.getMessage());
            }
        }
        if( !tail.isEmpty() )
        {
            try
            {
                defineMoleculeState(tail);
            }
            catch( Exception e )
            {
                throw new IllegalArgumentException("Invalid molecule format: " + e.getMessage());
            }
        }
        Collections.sort(moleculeComponents);
    }

    protected ObjectCache<String> getStringCache()
    {
        return speciesGraph.getStringCache();
    }

    private void addEdgeInternal(String e)
    {
        if( edges.isEmpty() )
        {
            edges = new ArrayList<>();
        }
        edges.add(getStringCache().get(e));
    }

    private void defineMoleculeState(String moleculeState) throws Exception
    {
        String[] stateParts = ParseUtils.parseState(moleculeState);

        if( stateParts == null )
            throw new IllegalArgumentException("Invalid molecule format: '" + this + moleculeState + "'");

        char c = stateParts[0].charAt(0);

        switch( c )
        {
            case '~':
                if( !state.isEmpty() )
                    throw new IllegalArgumentException("Multiple state definition: '" + this + moleculeState + "'");
                if( stateParts[1].startsWith("+") || stateParts[1].startsWith("?") )
                    throw new IllegalArgumentException("Invalid molecule format: '" + this + moleculeState + "'");

                setState(stateParts[1]);
                break;
            case '!':
                if( stateParts[1].equals("+") || stateParts[1].equals("?") )
                {
                    if( edgeWildcard != 0 )
                        throw new IllegalArgumentException("Multiple edge wildcard definition: '" + this + moleculeState + "'");

                    edgeWildcard = 1;
                    addEdgeInternal(stateParts[1]);
                }
                else
                    addEdgeInternal(stateParts[1]);
                break;
            case '%':
                if( !label.isEmpty() )
                    throw new IllegalArgumentException("Multiple label definition: '" + this + moleculeState + "'");
                if( stateParts[1].startsWith("+") || stateParts[1].startsWith("?") )
                    throw new IllegalArgumentException("Invalid molecule format: '" + this + moleculeState + "'");

                label = getStringCache().get(stateParts[1]);
                break;
            default:
                throw new Exception("Unknown matching error: '" + this + moleculeState + "'");
        }

        if( !stateParts[2].isEmpty() )
            this.defineMoleculeState(stateParts[2]);
    }

    public static boolean compareLocal(BionetgenMolecule bm1, BionetgenMolecule bm2)
    {
        if( !bm1.name.equals(bm2.name) || !bm1.state.equals(bm2.state) || bm1.moleculeComponents.size() != bm2.moleculeComponents.size() )
            return false;

        return true;
    }

    public int localHashCode()
    {
        if(localHashCode == 0)
        {
            int compHash = StreamEx.of( moleculeComponents ).mapToInt( MoleculeComponent::localHashCode ).sorted()
                    .reduce( 1, (a, b) -> a * 31 + b );
            localHashCode = name.hashCode() * 7317 + state.hashCode() * 31 + compHash;
        }
        return localHashCode;
    }

    public void removeEdgesNumbers()
    {
        for( MoleculeComponent mc : moleculeComponents )
        {
            mc.removeEdgesNumbers();
        }
    }

    public BionetgenMolecule copyAndRemoveEdgesNumbers(BionetgenSpeciesGraph newParent)
    {
        BionetgenMolecule copy;
        try
        {
            copy = (BionetgenMolecule)clone();
        }
        catch( CloneNotSupportedException e )
        {
            throw new InternalException(e);
        }
        copy.speciesGraph = newParent;
        copy.moleculeComponents = new ArrayList<>();
        for( MoleculeComponent mc : moleculeComponents )
        {
            copy.moleculeComponents.add(mc.copyAndRemoveEdgesNumbers(copy));
        }
        return copy;
    }

    public static @CheckForNull Map<MoleculeComponent, MoleculeComponent> findDifference(BionetgenMolecule mol1, BionetgenMolecule mol2)
    {
        Map<MoleculeComponent, MoleculeComponent> result = new HashMap<>();

        if( !compareLocal(mol1, mol2) )
            return null;

        List<MoleculeComponent> mcUsed = new ArrayList<>();
        for( MoleculeComponent mc1 : mol1.getMoleculeComponents() )
        {
            boolean found = false;
            for( MoleculeComponent mc2 : mol2.getMoleculeComponents() )
            {
                if( ( mcUsed.contains(mc2) ) || ( !mc1.getName().equals(mc2.getName()) )
                        || ( mc1.getEdgeWildcard() != mc2.getEdgeWildcard() ) )
                    continue;

                if( !MoleculeComponent.compareLocal(mc1, mc2) )
                {
                    result.put(mc1, mc2);
                }

                mcUsed.add(mc2);
                found = true;
                break;
            }

            if( !found )
                return null;
        }

        if( result.size() == 0 )
            return null;

        return result;
    }

    public String createLabelForMap()
    {
        StringBuilder sb = new StringBuilder(name);
        if( !label.isEmpty() )
            sb.append("%").append(label);
        for( MoleculeComponent mc : getMoleculeComponents() )
        {
            sb.append("_").append(mc.getName());
            if( !mc.getLabel().isEmpty() )
                sb.append("%").append(mc.getLabel());
            if( ( mc.getEdgeWildcard() != 0 ) && ( mc.toString().contains("!+") ) )
                sb.append("!+");
        }
        return sb.toString();
    }

    public String getName()
    {
        return name;
    }

    public String getState()
    {
        return state;
    }

    public void setState(String state)
    {
        this.state = getStringCache().get(state);
        localHashCode = 0;
    }

    public int getEdgeWildcard()
    {
        return edgeWildcard;
    }

    public String getLabel()
    {
        return label;
    }

    public List<MoleculeComponent> getMoleculeComponents()
    {
        return Collections.unmodifiableList(moleculeComponents);
    }

    public BionetgenSpeciesGraph getSpeciesGraph()
    {
        return speciesGraph;
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
        if( needBraces || !moleculeComponents.isEmpty() )
        {
            sb.append('(');
        }
        sb.append( StreamEx.of( moleculeComponents ).joining( "," ) );
        if( needBraces || !moleculeComponents.isEmpty() )
        {
            sb.append(')');
        }
        return sb.toString();
    }

    @Override
    public int compareTo(BionetgenMolecule o)
    {
        int result = name.compareTo(o.name);
        if( result != 0 )
            return result;
        result = state.compareTo(o.state);
        if( result != 0 )
            return result;
        result = label.compareTo(o.label);
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
        result = edges.size() - o.edges.size();
        if( result != 0 )
            return result;
        int minComponents = Math.min(moleculeComponents.size(), o.moleculeComponents.size());
        for( int i = 0; i < minComponents; i++ )
        {
            MoleculeComponent c1 = moleculeComponents.get(i);
            MoleculeComponent c2 = o.moleculeComponents.get(i);
            result = c1.compareTo(c2);
            if( result != 0 )
                return result;
        }
        return moleculeComponents.size() - o.moleculeComponents.size();
    }

    void resetHashCode()
    {
        localHashCode = 0;
        speciesGraph.resetHashCode();
    }

}