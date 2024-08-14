package biouml.plugins.bionetgen.diagram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

import ru.biosoft.exception.InternalException;
import ru.biosoft.util.ObjectCache;
import ru.biosoft.util.TextUtil;

public class BionetgenSpeciesGraph implements Cloneable
{
    private final ObjectCache<String> strings = new ObjectCache<>();
    private String name;
    private String label;
    private String nameDelimiter = "";
    private List<BionetgenMolecule> moleculesList;

    private int edgeCount = 0;
    private Map<MoleculeComponent, Set<MoleculeComponent>> adjacency = new HashMap<>();

    private boolean considerStoich = false;
    private final Map<String, Integer> stoichMap = new HashMap<>();
    private int localHashCode;

    public Map<String, Integer> getStoich()
    {
        return stoichMap;
    }

    private static final Pattern HEADER_PATTERN = Pattern.compile("((\\w)*)((%(\\w)+)*)");

    protected ObjectCache<String> getStringCache()
    {
        return strings;
    }

    public void addEdge(MoleculeComponent p1, MoleculeComponent p2)
    {
        Set<MoleculeComponent> p1set = adjacency.get(p1);
        if( p1set == null )
        {
            p1set = new HashSet<>();
            adjacency.put(p1, p1set);
        }
        if( p1set.add(p2) )
            edgeCount++;
        Set<MoleculeComponent> p2set = adjacency.get(p2);
        if( p2set == null )
        {
            p2set = new HashSet<>();
            adjacency.put(p2, p2set);
        }
        if( p2set.add(p1) )
            edgeCount++;
    }

    public void deleteEdge(MoleculeComponent p1, MoleculeComponent p2)
    {
        Set<MoleculeComponent> p1set = adjacency.get(p1);
        if( p1set != null && p1set.remove(p2) )
            edgeCount--;
        Set<MoleculeComponent> p2set = adjacency.get(p2);
        if( p2set != null && p2set.remove(p1) )
            edgeCount--;
        p1.getMolecule().removeEdgesNumbers();
        p2.getMolecule().removeEdgesNumbers();
        updateEdges();
    }

    public void updateEdges()
    {
        updateEdges(1);
    }

    public BionetgenSpeciesGraph copyAndUpdate(int start)
    {
        BionetgenSpeciesGraph copy;
        try
        {
            copy = (BionetgenSpeciesGraph)clone();
        }
        catch( CloneNotSupportedException e )
        {
            throw new InternalException(e);
        }
        copy.moleculesList = new ArrayList<>(moleculesList.size());

        Map<MoleculeComponent, MoleculeComponent> old2new = new IdentityHashMap<>();
        for( BionetgenMolecule mol : moleculesList )
        {
            BionetgenMolecule newMol = mol.copyAndRemoveEdgesNumbers(copy);
            copy.moleculesList.add(newMol);
            List<MoleculeComponent> moleculeComponents = mol.getMoleculeComponents();
            List<MoleculeComponent> newMoleculeComponents = newMol.getMoleculeComponents();
            EntryStream.zip( moleculeComponents, newMoleculeComponents ).forKeyValue( old2new::put );
        }
        copy.adjacency = new HashMap<>();
        for( Entry<MoleculeComponent, Set<MoleculeComponent>> entry : adjacency.entrySet() )
        {
            for( MoleculeComponent mol : entry.getValue() )
            {
                copy.addEdge(old2new.get(entry.getKey()), old2new.get(mol));
            }
        }
        copy.updateEdges(start);
        return copy;
    }

    public void updateEdges(int start)
    {
        if( edgeCount == 0 )
            return;
        for( BionetgenMolecule mol : moleculesList )
        {
            mol.removeEdgesNumbers();
        }

        int currentEdgeNumber = start;
        Set<MoleculeComponent> ignoreList = new HashSet<>();

        for( BionetgenMolecule mol : moleculesList )
        {
            for( MoleculeComponent mc : mol.getMoleculeComponents() )
            {
                Set<MoleculeComponent> map = adjacency.get(mc);
                if( map != null && !ignoreList.contains(mc) )
                {
                    for( MoleculeComponent mc2 : map )
                    {
                        if( !ignoreList.contains(mc2) )
                        {
                            String edgeNumberString = String.valueOf(currentEdgeNumber);
                            mc.addEdge(edgeNumberString);
                            mc2.addEdge(edgeNumberString);
                            ignoreList.add(mc);
                            ++currentEdgeNumber;
                        }
                    }
                }
            }
        }
    }

    public BionetgenSpeciesGraph(String specie)
    {
        this(specie, false);
    }

    public BionetgenSpeciesGraph(String specie, boolean considerStoich)
    {
        this.considerStoich = considerStoich;
        name = "";
        label = "";
        moleculesList = new ArrayList<>();
        String sp;
        String header = "";
        boolean hadHeader = false;

        String[] parts = TextUtil.split(specie, ':');
        int partsSize = parts.length;
        if( partsSize == 1 )
            sp = specie;
        else if( parts[0].isEmpty() || parts[partsSize - 1].isEmpty() )
            throw new IllegalArgumentException("Improper syntax of Species Graph header: '" + specie + "'");
        else if( partsSize > 3 )
            throw new IllegalArgumentException("Improper syntax of Species Graph header: '" + specie + "'");
        else
        {
            if( partsSize == 3 )
            {
                if( !parts[1].isEmpty() )
                    throw new IllegalArgumentException("Improper syntax of Species Graph header: '" + specie + "'");
                header = parts[0];
                nameDelimiter = "::";
                sp = parts[2];
            }
            else
            {
                header = parts[0];
                nameDelimiter = ":";
                sp = parts[1];
            }
            hadHeader = true;
        }
        if( sp.contains("..") )
            throw new IllegalArgumentException("Improper syntax of Species Graph: two or more molecules separators ('.') in a row at '"
                    + sp + "'");

        Matcher matcher = HEADER_PATTERN.matcher(header);

        if( matcher.matches() && hadHeader )
        {
            name = matcher.group(1);
            if( !matcher.group(3).isEmpty() )
            {
                label = matcher.group(3).substring(1);
            }
        }

        String[] molecules = TextUtil.split(sp, '.');
        for( String molecule : molecules )
        {
            try
            {
                BionetgenMolecule bionetgenMolecule = new BionetgenMolecule(this, molecule);
                moleculesList.add(bionetgenMolecule);
                addMoleculeStoichiometry(bionetgenMolecule);
            }
            catch( Exception e )
            {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
        sortMolecules();

        defineEdges();

        updateEdges();

        sortMolecules();
    }

    public boolean deleteMolecule(BionetgenMolecule mol)
    {
        if( !mol.getSpeciesGraph().equals(this) )
            return false;

        for( MoleculeComponent mc : mol.getMoleculeComponents() )
        {
            Set<MoleculeComponent> set = adjacency.remove(mc);
            if( set != null )
                edgeCount -= set.size();
        }
        for( MoleculeComponent mc : mol.getMoleculeComponents() )
        {
            for( Set<MoleculeComponent> targets : adjacency.values() )
            {
                if( targets.remove(mc) )
                    edgeCount--;
            }
        }

        moleculesList.remove(mol);
        resetHashCode();

        removeMoleculeStoichiometry(mol);

        return true;
    }

    public void addMolecule(BionetgenMolecule mol, Map<MoleculeComponent, List<MoleculeComponent>> map)
    {
        if( mol.getSpeciesGraph() == null || !mol.getSpeciesGraph().equals(this) )
            throw new IllegalArgumentException("Specified molecule has wrong graph");
        if( moleculesList.contains(mol) )
            return;

        EntryStream.of(map).flatMapValues(List::stream).forKeyValue(this::addEdge);

        addMoleculeStoichiometry(mol);

        moleculesList.add(mol);
        updateEdges();
        sortMolecules();
        resetHashCode();
    }

    private void addMoleculeStoichiometry(BionetgenMolecule mol)
    {
        if( !considerStoich )
            return;
        Integer stoich;
        String molName = mol.getName();
        if( ( stoich = stoichMap.get(molName) ) != null )
            stoichMap.put(molName, stoich + 1);
        else
            stoichMap.put(molName, 1);
    }

    private void removeMoleculeStoichiometry(BionetgenMolecule mol)
    {
        if( !considerStoich )
            return;
        String molName = mol.getName();
        if( !stoichMap.containsKey(molName) )
            return;
        int molStoich = stoichMap.get(molName) - 1;
        if( molStoich == 0 )
            stoichMap.remove(molName);
        else
            stoichMap.put(molName, molStoich);
    }

    public boolean fulfillStoichiometry(Map<String, Integer> maxStoich)
    {
        if( !considerStoich )
            return maxStoich == null;
        for( Map.Entry<String, Integer> entry : maxStoich.entrySet() )
        {
            Integer stoich = stoichMap.get(entry.getKey());
            if( stoich != null && stoich > entry.getValue() )
            {
                return false;
            }
        }
        return true;
    }

    private void sortMolecules()
    {
        Collections.sort(moleculesList);
    }

    private void defineEdges()
    {
        List<MoleculeComponent> mcl = getMoleculeComponents();
        for( MoleculeComponent mc : mcl.toArray(new MoleculeComponent[mcl.size()]) )
        {
            for( String mcEdge : mc.getEdges() )
            {
                if( !mcEdge.equals("+") && !mcEdge.equals("?") )
                {
                    MoleculeComponent mcBind = findBind(mcEdge, mcl, mc);
                    if( mcBind != null )
                        addEdge(mc, mcBind);
                    else
                        mc.changeEdgeToWildcard(mcEdge);
                }
            }
        }
    }

    public static @CheckForNull MoleculeComponent findBind(String mcEdge, List<MoleculeComponent> mcl, MoleculeComponent ignored)
    {
        for( MoleculeComponent molComp : mcl )
        {
            if( molComp.equals(ignored) )
                continue;
            for( String molCompEdge : molComp.getEdges() )
            {
                if( molCompEdge.equals(mcEdge) )
                {
                    mcl.remove(molComp);
                    if( ( findBind(molCompEdge, mcl, ignored) == null ) )
                    {
                        mcl.add(molComp);
                        return molComp;
                    }
                    else
                    {
                        throw new IllegalArgumentException("Error: edge #" + mcEdge + " has more than 2 vertices in "
                                + molComp.getMolecule().getSpeciesGraph().toString());
                    }
                }
            }
        }
        return null;
    }

    /**
     * Tests if this graph is isomorphic to subgraphs of <b>bsg2</b>
     * @param bsg2 graph to find isomorphic subgraphs in
     * @return all maps of isomorphism between subgraphs of given <b>bsg2</b> and this graph
     */
    public @Nonnull List<BionetgenMap> isomorphicToSubgraphOf(BionetgenSpeciesGraph bsg2)
    {
        return this.isomorphicToSubgraphOf(bsg2, false);
    }

    /**
     * Tests if this graph is isomorphic to subgraph of <b>bsg2</b>
     * @param bsg2 graph to find isomorphic subgraph in
     * @param matchOnce <code>boolean</code> flag to indicate all maps of
     * isomorphism should be returned or only one
     * @return list of isomorphism maps (one map if <b>matchOnce</b> is <code>true</code>). List is empty
     * if no map has been found
     */
    public @Nonnull List<BionetgenMap> isomorphicToSubgraphOf(BionetgenSpeciesGraph bsg2, boolean matchOnce)
    {
        List<BionetgenMap> result = new ArrayList<>();

        List<BionetgenMolecule> molecules2 = bsg2.getMoleculesList();
        Map<MoleculeComponent, Set<MoleculeComponent>> adj2 = bsg2.getAdjacency();

        int size = moleculesList.size();
        if( molecules2.size() < size )
            return result;
        if( bsg2.edgeCount < edgeCount )
            return result;

        BitSet molUsed = new BitSet();
        int[] molPointers = new int[size];
        int[][] comPointers = new int[size][];
        int[][] comUseds = new int[size][];
        Arrays.fill(molPointers, -1);
        for( int i = 0; i < size; i++ )
        {
            comPointers[i] = new int[moleculesList.get(i).getMoleculeComponents().size()];
            Arrays.fill(comPointers[i], -1);
        }

        int molIter1 = 0;
        int molIter2;
        int comIter1 = -1;
        int comIter2;
        BionetgenMolecule molecule1 = moleculesList.get(molIter1);
        List<MoleculeComponent> mCs1 = molecule1.getMoleculeComponents();
        List<MoleculeComponent> mCs2 = null;

        MITER: while( true )
        {
            boolean moleculeMatch = false;
            for( molIter2 = Math.max(0, molPointers[molIter1]); molIter2 < molecules2.size(); molIter2++ )
            {
                BionetgenMolecule molecule2 = molecules2.get(molIter2);
                if( molUsed.get(molIter2) )
                    continue;
                if( !molecule1.getName().equals(molecule2.getName()) )
                    continue;
                if( !molecule1.getState().isEmpty() && !molecule1.getState().equals(molecule2.getState()) )
                    continue;
                molPointers[molIter1] = molIter2;
                mCs2 = molecule2.getMoleculeComponents();
                comUseds[molIter1] = new int[mCs2.size()];
                Arrays.fill(comUseds[molIter1], -1);
                Arrays.fill(comPointers[molIter1], -1);
                comIter1 = !mCs1.isEmpty() ? 0 : -1;
                moleculeMatch = true;
                break;
            }

            if( !moleculeMatch )
            {
                if( molIter1 == 0 )
                    break MITER;

                molPointers[molIter1] = -1;
                --molIter1;
                mCs1 = moleculesList.get(molIter1).getMoleculeComponents();
                comIter1 = mCs1.size() - 1;
                molIter2 = molPointers[molIter1];
                molUsed.clear(molPointers[molIter1]);
                molecule1 = moleculesList.get(molIter1);
                mCs2 = molecules2.get(molIter2).getMoleculeComponents();
                if( comIter1 >= 0 )
                {
                    ++comPointers[molIter1][comIter1];
                }
                else
                {
                    ++molPointers[molIter1];
                    continue MITER;
                }
            }

            int[] comPointer = comPointers[molIter1];
            int[] comUsed = comUseds[molIter1];
            int mCs1size = mCs1.size();
            int mCs2size = mCs2.size();

            CITER: while( true )
            {
                boolean compMatch;

                if( mCs1size > 0 )
                {
                    MoleculeComponent component1 = mCs1.get(comIter1);
                    compMatch = false;

                    for( comIter2 = Math.max(0, comPointer[comIter1]); comIter2 < mCs2size; comIter2++ )
                    {
                        if( comUsed[comIter2] != -1 )
                            continue;
                        MoleculeComponent component2 = mCs2.get(comIter2);

                        if( !component1.getName().equals(component2.getName()) )
                            continue;
                        if( !component1.getState().isEmpty() && !component1.getState().equals(component2.getState()) )
                            continue;

                        int difference = component2.getEdges().size() - component1.getEdges().size();
                        if( difference != 0 )
                        {
                            if( component1.getEdges().size() > 0 )
                            {
                                if( component1.getEdgeWildcard() == 0 )
                                    continue;
                                else if( component1.getEdges().get(0).equals("+") && difference < 0 )
                                    continue;
                                else if( difference < -1 )
                                    continue;
                            }
                            else
                                continue;
                        }

                        boolean edgeMatch = true;

                        Set<MoleculeComponent> set = adjacency.get(component1);
                        if( set != null )
                        {
                            Set<MoleculeComponent> set2 = adj2.get(component2);
                            if( set2 == null )
                                set2 = Collections.emptySet();
                            if( set2.size() < set.size() )
                                edgeMatch = false;
                            else
                            {
                                EDGE: for( MoleculeComponent q1 : set )
                                {
                                    int pos = moleculesList.indexOf(q1.getMolecule());
                                    if( molIter1 < pos )
                                        continue;
                                    if( molIter1 == pos && comIter1 <= mCs1.indexOf(q1) )
                                        continue;

                                    MoleculeComponent q2 = molecules2.get(molPointers[pos]).getMoleculeComponents()
                                            .get(comPointers[pos][q1.getMolecule().getMoleculeComponents().indexOf(q1)]);

                                    if( !set2.contains(q2) )
                                    {
                                        edgeMatch = false;
                                        break EDGE;
                                    }
                                }
                            }
                        }
                        if( !edgeMatch )
                            continue;

                        comPointer[comIter1] = comIter2;

                        if( comIter1 == mCs1size - 1 )
                        {
                            compMatch = true;
                            break;
                        }
                        else
                        {
                            comUsed[comIter2] = 1;
                            ++comIter1;
                            continue CITER;
                        }
                    }
                }
                else
                    compMatch = true;

                if( !compMatch )
                {
                    if( comIter1 <= 0 )
                    {
                        ++molPointers[molIter1];
                        continue MITER;
                    }

                    comPointer[comIter1] = -1;
                    comIter1--;
                    comUsed[comPointer[comIter1]] = -1;
                    comPointer[comIter1]++;
                    continue CITER;
                }

                if( molIter1 == size - 1 )
                {
                    //isomorphism mapping
                    Map<MoleculeComponent, MoleculeComponent> map = new HashMap<>();
                    Map<BionetgenMolecule, BionetgenMolecule> moleculesMap = new IdentityHashMap<>();

                    for( int i = 0; i < size; i++ )
                    {
                        int[] cptr = comPointers[i];
                        for( int k = 0; k < cptr.length; k++ )
                        {
                            map.put(moleculesList.get(i).getMoleculeComponents().get(k), molecules2.get(molPointers[i])
                                    .getMoleculeComponents().get(cptr[k]));
                        }
                        moleculesMap.put(moleculesList.get(i), molecules2.get(molPointers[i]));
                    }

                    result.add(new BionetgenMap(this, bsg2, map, moleculesMap));

                    if( matchOnce )
                        return result;

                    if( mCs1size > 0 )
                    {
                        ++comPointer[comIter1];
                        continue CITER;
                    }
                    else
                    {
                        ++molPointers[molIter1];
                        continue MITER;
                    }
                }

                break CITER;
            }

            molUsed.set(molIter2);
            molIter1++;
            molecule1 = moleculesList.get(molIter1);
            mCs1 = molecule1.getMoleculeComponents();

        }

        return result;
    }

    /**
     * Tests if this graph is isomorphic to <b>bsg2</b>
     * @param bsg2 graph to test for isomorphism with
     * @return <code>true</code> if isomorphic and <code>false</code> otherwise
     */
    public boolean isomorphicTo(BionetgenSpeciesGraph bsg2)
    {
        List<BionetgenMolecule> molecules2 = bsg2.getMoleculesList();
        Map<MoleculeComponent, Set<MoleculeComponent>> adj2 = bsg2.getAdjacency();

        int size = moleculesList.size();
        if( molecules2.size() != size )
            return false;
        if( bsg2.edgeCount != edgeCount )
            return false;
        if( bsg2.localHashCode() != localHashCode() )
            return false;

        BitSet molUsed = new BitSet();
        int[] molPointers = new int[size];
        int[][] comPointers = new int[size][];
        int[][] comUseds = new int[size][];
        Arrays.fill(molPointers, -1);
        for( int i = 0; i < size; i++ )
        {
            comPointers[i] = new int[moleculesList.get(i).getMoleculeComponents().size()];
            Arrays.fill(comPointers[i], -1);
        }

        int molIter1 = 0;
        int molIter2;
        int comIter1 = -1;
        int comIter2;
        BionetgenMolecule molecule1 = moleculesList.get(molIter1);
        List<MoleculeComponent> mCs1 = molecule1.getMoleculeComponents();
        List<MoleculeComponent> mCs2 = null;

        MITER: while( true )
        {
            boolean molMatch = false;

            for( molIter2 = Math.max(0, molPointers[molIter1]); molIter2 < molecules2.size(); molIter2++ )
            {
                if( molUsed.get(molIter2) )
                    continue;
                BionetgenMolecule molecule2 = molecules2.get(molIter2);
                if( molecule1.localHashCode() != molecule2.localHashCode() || !BionetgenMolecule.compareLocal(molecule1, molecule2) )
                    continue;
                molPointers[molIter1] = molIter2;
                mCs2 = molecule2.getMoleculeComponents();
                comUseds[molIter1] = new int[mCs2.size()];
                Arrays.fill(comPointers[molIter1], -1);
                Arrays.fill(comUseds[molIter1], -1);
                comIter1 = 0;
                molMatch = true;
                break;
            }

            if( !molMatch )
            {
                if( molIter1 == 0 )
                    break MITER;

                molPointers[molIter1] = -1;
                --molIter1;
                mCs1 = moleculesList.get(molIter1).getMoleculeComponents();
                comIter1 = mCs1.size() - 1;
                molIter2 = molPointers[molIter1];
                molUsed.clear(molPointers[molIter1]);
                molecule1 = moleculesList.get(molIter1);
                mCs2 = molecules2.get(molIter2).getMoleculeComponents();
                if( comIter1 >= 0 )
                {
                    ++comPointers[molIter1][comIter1];
                }
                else
                {
                    ++molPointers[molIter1];
                    continue MITER;
                }
            }

            int mCs1size = mCs1.size();
            int mCs2size = mCs2.size();
            if( mCs1size != 0 )
            {
                int[] comPointer = comPointers[molIter1];
                int[] comUsed = comUseds[molIter1];

                CITER: while( true )
                {
                    boolean compMatch = false;
                    MoleculeComponent component1 = mCs1.get(comIter1);

                    for( comIter2 = Math.max(0, comPointer[comIter1]); comIter2 < mCs2size; comIter2++ )
                    {
                        if( comUsed[comIter2] != -1 )
                            continue;
                        MoleculeComponent component2 = mCs2.get(comIter2);

                        if( !MoleculeComponent.compareLocal(component1, component2) )
                            continue;

                        boolean edgeMatch = true;

                        Set<MoleculeComponent> set = adjacency.get(component1);
                        if( set != null )
                        {
                            Set<MoleculeComponent> set2 = adj2.get(component2);
                            if( set2 == null )
                                set2 = Collections.emptySet();
                            if( set2.size() != set.size() )
                                edgeMatch = false;
                            else
                            {
                                EDGE: for( MoleculeComponent q1 : set )
                                {
                                    int pos = moleculesList.indexOf(q1.getMolecule());
                                    if( molIter1 < pos )
                                        continue;
                                    if( molIter1 == pos && comIter1 <= mCs1.indexOf(q1) )
                                        continue;

                                    MoleculeComponent q2 = molecules2.get(molPointers[pos]).getMoleculeComponents()
                                            .get(comPointers[pos][q1.getMolecule().getMoleculeComponents().indexOf(q1)]);

                                    if( !set2.contains(q2) )
                                    {
                                        edgeMatch = false;
                                        break EDGE;
                                    }
                                }
                            }
                        }
                        if( !edgeMatch )
                            continue;

                        comPointer[comIter1] = comIter2;

                        if( comIter1 == mCs1size - 1 )
                        {
                            compMatch = true;
                            break;
                        }
                        else
                        {
                            comUsed[comIter2] = 1;
                            ++comIter1;
                            continue CITER;
                        }
                    }

                    if( !compMatch )
                    {
                        if( comIter1 == 0 )
                        {
                            ++molPointers[molIter1];
                            continue MITER;
                        }

                        comPointer[comIter1] = -1;
                        comIter1--;
                        comUsed[comPointer[comIter1]] = -1;
                        comPointer[comIter1]++;
                        continue CITER;
                    }
                    break CITER;
                }
            }

            if( molIter1 == size - 1 )
                return true;

            molUsed.set(molIter2);
            molIter1++;
            molecule1 = moleculesList.get(molIter1);
            mCs1 = molecule1.getMoleculeComponents();
        }

        return false;
    }

    public List<BionetgenSpeciesGraph> getConnectedSubgraphs(boolean considerStoich)
    {
        List<BionetgenSpeciesGraph> result = new ArrayList<>();
        Set<BionetgenMolecule> alreadyAddedMols = new HashSet<>();

        for( BionetgenMolecule mol : moleculesList )
        {
            if( alreadyAddedMols.contains(mol) )
                continue;
            alreadyAddedMols.add(mol);
            Set<BionetgenMolecule> adding = getAllBound(mol, alreadyAddedMols);
            adding.add(mol);
            BionetgenSpeciesGraph graph = new BionetgenSpeciesGraph( StreamEx.of( adding ).joining( "." ), considerStoich );
            graph.sortMolecules();
            result.add(graph);
        }

        return result;
    }

    private Set<BionetgenMolecule> getAllBound(BionetgenMolecule mol, Set<BionetgenMolecule> ignoreList)
    {
        Set<BionetgenMolecule> result = new HashSet<>();
        Set<BionetgenMolecule> boundWith = new HashSet<>();

        for( MoleculeComponent mc : mol.getMoleculeComponents() )
        {
            Set<MoleculeComponent> set = adjacency.get(mc);
            if( set == null )
                continue;
            for( MoleculeComponent mc2 : set )
            {
                if( !ignoreList.contains(mc2.getMolecule()) )
                    boundWith.add(mc2.getMolecule());
            }
        }
        result.addAll(boundWith);
        ignoreList.addAll(boundWith);

        for( BionetgenMolecule molecule : boundWith )
        {
            result.addAll(getAllBound(molecule, ignoreList));
        }

        return result;
    }

    public List<MoleculeComponent> getMoleculeComponents()
    {
        List<MoleculeComponent> result = new ArrayList<>();
        for( BionetgenMolecule mol : this.moleculesList )
        {
            result.addAll(mol.getMoleculeComponents());
        }
        return result;
    }

    public String getName()
    {
        return name;
    }

    public String getLabel()
    {
        return label;
    }

    public int getEdgesNumber()
    {
        return edgeCount;
    }

    public List<BionetgenMolecule> getMoleculesList()
    {
        return moleculesList;
    }

    public String getMoleculesListAsString()
    {
        List<String> result = new ArrayList<>();
        for( BionetgenMolecule molecule : moleculesList )
        {
            result.add(molecule.toString());
        }
        return result.toString();
    }

    public Map<MoleculeComponent, Set<MoleculeComponent>> getAdjacency()
    {
        return adjacency;
    }

    @Override
    public String toString()
    {
        String graph = "";
        if( !name.isEmpty() )
            graph = name;
        if( !label.isEmpty() )
            graph += "%" + label;
        String mols = StreamEx.of( moleculesList ).joining( "." );
        if( !graph.isEmpty() )
        {
            if( moleculesList.size() > 0 )
                return graph + nameDelimiter + mols;
            return graph;
        }
        else if( moleculesList.size() > 0 )
        {
            return mols;
        }
        return "";
    }

    public int localHashCode()
    {
        if( localHashCode == 0 )
        {
            localHashCode = StreamEx.of( moleculesList ).mapToInt( BionetgenMolecule::localHashCode ).sorted()
                    .reduce( 1, (a, b) -> 31 * a + b );
        }
        return localHashCode;
    }

    void resetHashCode()
    {
        localHashCode = 0;
    }
}
