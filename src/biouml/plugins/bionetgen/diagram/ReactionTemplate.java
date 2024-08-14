package biouml.plugins.bionetgen.diagram;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

public class ReactionTemplate
{
    private final List<BionetgenSpeciesGraph> reactants = new ArrayList<>();
    private final List<BionetgenSpeciesGraph> products = new ArrayList<>();
    private final String name;
    private String forwardRate;
    private String rateLawType;
    private final Map<BionetgenMolecule, BionetgenMolecule> patternMap = new IdentityHashMap<>();
    private final List<BionetgenMolecule> deletingMolecules = new ArrayList<>();

    /*
     * next four variables contains number of reactant/product
     * and list of including/excluding species in it
     */
    private final Map<BionetgenSpeciesGraph, List<List<BionetgenSpeciesGraph>>> includeReactants = new IdentityHashMap<>();
    private final Map<BionetgenSpeciesGraph, List<List<BionetgenSpeciesGraph>>> excludeReactants = new IdentityHashMap<>();
    private final Map<BionetgenSpeciesGraph, List<List<BionetgenSpeciesGraph>>> includeProducts = new IdentityHashMap<>();
    private final Map<BionetgenSpeciesGraph, List<List<BionetgenSpeciesGraph>>> excludeProducts = new IdentityHashMap<>();

    private List<MoleculeComponent> reactionCenter = null;
    private TDoubleList multipliersList = null;
    private Map<MoleculeComponent, List<MoleculeComponent>> newMolsAdjacency = null;
    private double isomorphicMultiplier = 1.0;

    public ReactionTemplate(Reaction reaction)
    {
        name = reaction.getName();
        String s;
        if( ( s = reaction.getAttributes().getValueAsString(BionetgenConstants.RATE_LAW_TYPE_ATTR) ) != null )
        {
            rateLawType = s;
        }
        else
        {
            rateLawType = BionetgenConstants.DEFAULT;
        }
        if( ( s = reaction.getAttributes().getValueAsString(BionetgenConstants.FORWARD_RATE_ATTR) ) != null )
        {
            forwardRate = s;
        }

        reaction.stream().sorted(
                Comparator.comparingInt( (SpecieReference o) -> (Integer)o.getAttributes().getValue( BionetgenConstants.REACTANT_NUMBER_ATTR ) ) )
                .forEach( sr -> {
                    if( sr.getRole().equals( SpecieReference.REACTANT ) )
                        reactants.add( new BionetgenSpeciesGraph( sr.getSpecie() ) );
                    else
                        products.add( new BionetgenSpeciesGraph( sr.getSpecie() ) );
                } );

        Object additionValue = reaction.getAttributes().getValue(BionetgenConstants.ADDITION_ATTR);
        if( additionValue != null )
            setAddition((String[])additionValue);

        findMaps();

        reactionCenter = getReactionCenter();

        List<BionetgenSpeciesGraph> recordered = new ArrayList<>();
        int size = reactants.size();
        if( size > 1 )
        {
            for( int i = 0; i < size - 1; i++ )
            {
                int multiple = 1;
                if( recordered.contains(reactants.get(i)) )
                    continue;
                for( int j = i + 1; j < size; j++ )
                {
                    if( recordered.contains(reactants.get(j)) )
                        continue;

                    if( reactants.get(i).isomorphicTo(reactants.get(j)) )
                    {
                        multiple *= ( multiple + 1 );
                        recordered.add(reactants.get(j));
                    }
                }
                isomorphicMultiplier /= multiple;
            }
        }
        size = products.size();
        recordered = new ArrayList<>();
        if( size > 1 )
        {
            for( int i = 0; i < size - 1; i++ )
            {
                int multiple = 1;
                if( recordered.contains(products.get(i)) )
                    continue;
                for( int j = i + 1; j < size; j++ )
                {
                    if( recordered.contains(products.get(j)) )
                        continue;

                    if( products.get(i).isomorphicTo(products.get(j)) )
                    {
                        multiple *= ( multiple + 1 );
                        recordered.add(products.get(j));
                    }
                }
                isomorphicMultiplier /= multiple;
            }
        }
    }

    public String getName()
    {
        return name;
    }

    public String getForwardRate()
    {
        return forwardRate;
    }

    public String getRateLawType()
    {
        return rateLawType;
    }

    public double getLastMultiplier(int productsListIndex)
    {
        return multipliersList.get(productsListIndex);
    }

    public List<List<BionetgenSpeciesGraph>> getReactantSets(Collection<BionetgenSpeciesGraph> candidates)
    {
        List<List<BionetgenSpeciesGraph>> reactantCandidates = new ArrayList<>();
        int size = reactants.size();
        for( int i = 0; i < size; i++ )
            reactantCandidates.add(new ArrayList<BionetgenSpeciesGraph>());
        for( BionetgenSpeciesGraph bsg : candidates )
        {
            for( int i = 0; i < size; i++ )
            {
                BionetgenSpeciesGraph currentReactant = reactants.get(i);
                if( ( currentReactant.isomorphicToSubgraphOf(bsg, true).size() != 0 ) && checkReactantAddition(bsg, currentReactant) )
                    reactantCandidates.get(i).add(bsg);
            }
        }
        return reactantCandidates;
    }

    public List<List<BionetgenSpeciesGraph>> executeReaction(Collection<BionetgenSpeciesGraph> currentReactants,
            Map<String, Integer> maxStoichiometry) throws Exception
    {
        multipliersList = new TDoubleArrayList();

        boolean considerStoich = maxStoichiometry != null;

        List<List<BionetgenSpeciesGraph>> productGraphs = new ArrayList<>();
        List<List<BionetgenMap>> allMaps = new ArrayList<>();
        List<String> temp = new ArrayList<>();
        int j = 0;
        for( BionetgenSpeciesGraph reactant : currentReactants )
        {
            BionetgenSpeciesGraph updated = reactant.copyAndUpdate(j + 1);
            j += ( updated.getEdgesNumber() + 1 );
            temp.add(updated.toString());
        }
        BionetgenSpeciesGraph currentGraph;
        if( temp.size() == 1 )
            currentGraph = new BionetgenSpeciesGraph(temp.get(0));
        else
            currentGraph = new BionetgenSpeciesGraph( String.join( ".", temp ) );

        for( BionetgenSpeciesGraph react : reactants )
        {
            allMaps.add(react.isomorphicToSubgraphOf(currentGraph));
        }

        if( rateLawType.equals(BionetgenConstants.DEFAULT) )
            allMaps = BionetgenMap.getDisjointMap(allMaps, reactionCenter);
        else
            allMaps = BionetgenMap.getDisjointMap(allMaps, null);

        for( List<BionetgenMap> map : allMaps )
        {
            BionetgenSpeciesGraph currentProductGraph = new BionetgenSpeciesGraph(currentGraph.toString());
            BionetgenMap copyMap = currentGraph.isomorphicToSubgraphOf(currentProductGraph, true).get(0);
            newMolsAdjacency = new IdentityHashMap<>();

            //molecules deleting
            deleteMolecules(map, currentProductGraph, copyMap);

            for( BionetgenSpeciesGraph product : products )
            {
                for( BionetgenMolecule prodMol : product.getMoleculesList() )
                {
                    BionetgenMolecule reactMol = patternMap.get(prodMol);

                    if( ( reactMol != null ) && ( reactMol.getMoleculeComponents().size() == 0 ) )
                        continue;

                    //new molecule adding
                    if( reactMol == null )
                    {
                        addNewMolecule(map, currentProductGraph, copyMap, prodMol);
                        continue;
                    }

                    if( prodMol.getMoleculeComponents().size() == 0 )
                        continue;

                    Map<MoleculeComponent, MoleculeComponent> differenceMap = BionetgenMolecule.findDifference(reactMol, prodMol);

                    if( differenceMap == null )
                        continue;

                    int reactGraphIndex = reactants.indexOf(reactMol.getSpeciesGraph());
                    MOLCOMPS: for( Map.Entry<MoleculeComponent, MoleculeComponent> entry : differenceMap.entrySet() )
                    {
                        //State changing
                        if( !entry.getKey().getState().equals(entry.getValue().getState()) )
                        {
                            copyMap.getMapForward().get(map.get(reactGraphIndex).getMapForward().get(entry.getKey()))
                                    .setState(entry.getValue().getState());
                        }
                        //Deleting edges
                        for( String reactEdge : entry.getKey().getEdges() )
                        {
                            if( reactEdge.equals("+") || reactEdge.equals("?") )
                                continue;
                            boolean isFound = false;
                            MoleculeComponent reactBoundWith = BionetgenSpeciesGraph.findBind(reactEdge, reactMol.getSpeciesGraph()
                                    .getMoleculeComponents(), entry.getKey());
                            if( reactBoundWith == null )
                                continue;
                            for( String prodEdge : entry.getValue().getEdges() )
                            {
                                if( prodEdge.equals("+") || prodEdge.equals("?") )
                                    continue;
                                MoleculeComponent prodBoundWith = BionetgenSpeciesGraph.findBind(prodEdge, product.getMoleculeComponents(),
                                        entry.getValue());
                                if( prodBoundWith == null )
                                    continue;
                                if( patternMap.get(prodBoundWith.getMolecule()).equals(reactBoundWith.getMolecule()) )
                                {
                                    isFound = true;
                                    break;
                                }
                            }

                            if( !isFound )
                            {
                                MoleculeComponent mc1 = map.get(reactGraphIndex).getMapForward().get(entry.getKey());
                                MoleculeComponent mc2 = map.get(reactGraphIndex).getMapForward().get(reactBoundWith);
                                if( mc1 == null || mc2 == null )
                                    continue MOLCOMPS;
                                currentProductGraph.deleteEdge(copyMap.getMapForward().get(mc1), copyMap.getMapForward().get(mc2));
                            }
                        }

                        //Adding new edges
                        for( String prodEdge : entry.getValue().getEdges() )
                        {
                            if( prodEdge.equals("+") || prodEdge.equals("?") )
                                continue;
                            boolean isFound = false;
                            MoleculeComponent prodBoundWith = BionetgenSpeciesGraph.findBind(prodEdge, product.getMoleculeComponents(),
                                    entry.getValue());
                            if( prodBoundWith == null )
                                continue;
                            for( String reactEdge : entry.getKey().getEdges() )
                            {
                                if( reactEdge.equals("+") || reactEdge.equals("?") )
                                    continue;
                                MoleculeComponent reactBoundWith = BionetgenSpeciesGraph.findBind(reactEdge, reactMol.getSpeciesGraph()
                                        .getMoleculeComponents(), entry.getKey());
                                if( reactBoundWith == null )
                                    continue;
                                if( patternMap.get(prodBoundWith.getMolecule()).equals(reactBoundWith.getMolecule()) )
                                {
                                    isFound = true;
                                    break;
                                }
                            }

                            if( !isFound )
                            {
                                MoleculeComponent mc1 = map.get(reactGraphIndex).getMapForward().get(entry.getKey());
                                BionetgenMolecule reactBoundWithMol = patternMap.get(prodBoundWith.getMolecule());
                                if( reactBoundWithMol == null )
                                    continue;
                                Map<MoleculeComponent, MoleculeComponent> tempDifferenceMap = BionetgenMolecule.findDifference(
                                        reactBoundWithMol, prodBoundWith.getMolecule());

                                if( tempDifferenceMap == null )
                                    throw new Exception("Invalid reaction template: trying to make " + prodMol.toString() + " from "
                                            + reactMol.toString() + " in " + this.getName());

                                MoleculeComponent addingComp = StreamEx.ofKeys( tempDifferenceMap, prodBoundWith::equals ).findAny()
                                        .orElse( null );
                                if( addingComp == null )
                                    continue MOLCOMPS;

                                Map<MoleculeComponent, MoleculeComponent> mcs = map.get(
                                        reactants.indexOf(addingComp.getMolecule().getSpeciesGraph())).getMapForward();
                                MoleculeComponent mc2 = mcs.get(addingComp);
                                currentProductGraph.addEdge(copyMap.getMapForward().get(mc1), copyMap.getMapForward().get(mc2));
                            }
                        }

                    }

                }
            }
            currentProductGraph.updateEdges();
            double moleculesSymmetryMultiplier = findMoleculesSymmetryMultiplier(map);
            List<BionetgenSpeciesGraph> currentProductGraphs = normalizeCurrentProductsOrder(currentProductGraph
                    .getConnectedSubgraphs(considerStoich));
            if( currentProductGraphs != null && checkStoichiometry(maxStoichiometry, currentProductGraphs)
                    && isomorphismCheck(productGraphs, currentProductGraphs, moleculesSymmetryMultiplier) )
            {
                productGraphs.add(currentProductGraphs);
                multipliersList.add(moleculesSymmetryMultiplier);
            }
        }

        productGraphs = BionetgenDiagramDeployer.withoutPermutations(productGraphs, multipliersList);
        for( int i = 0; i < multipliersList.size(); i++ )
        {
            if( reactionCenter.isEmpty() )
            {
                multipliersList.set(i, 1.0);
                continue;
            }
            else if( multipliersList.get(i) != 1 )
            {
                double tempMultiple = multipliersList.get(i) * isomorphicMultiplier;
                multipliersList.set(i, tempMultiple);
            }
        }
        return productGraphs;
    }

    private void addNewMolecule(List<BionetgenMap> map, BionetgenSpeciesGraph currentProductGraph, BionetgenMap copyMap,
            BionetgenMolecule prodMol)
    {
        BionetgenMolecule newMolecule = new BionetgenMolecule(currentProductGraph, prodMol.toString());
        Map<MoleculeComponent, List<MoleculeComponent>> addingMap = new HashMap<>();
        for( MoleculeComponent mc : newMolecule.getMoleculeComponents() )
        {
            List<MoleculeComponent> newBinds = new ArrayList<>();
            MoleculeComponent prodMolComp = prodMol.getMoleculeComponents().get(mc.getMolecule().getMoleculeComponents().indexOf(mc));
            for( String edge : mc.getEdges() )
            {
                if( edge.equals("+") || edge.equals("?") )
                    continue;
                MoleculeComponent mcBindWith = BionetgenSpeciesGraph.findBind(edge, prodMol.getSpeciesGraph().getMoleculeComponents(),
                        prodMolComp);
                if( mcBindWith == null )
                    continue;
                BionetgenMolecule reactantTargetMol = patternMap.get(mcBindWith.getMolecule());
                if( reactantTargetMol == null )
                {
                    List<MoleculeComponent> list = newMolsAdjacency.get(mcBindWith);
                    if( list == null )
                    {
                        list = new ArrayList<>();
                        newMolsAdjacency.put(mcBindWith, list);
                    }
                    list.add(mc);
                    continue;
                }
                MoleculeComponent targetMC = reactantTargetMol.getMoleculeComponents().get(
                        mcBindWith.getMolecule().getMoleculeComponents().indexOf(mcBindWith));
                newBinds.add(copyMap.getMapForward().get(
                        map.get(reactants.indexOf(targetMC.getMolecule().getSpeciesGraph())).getMapForward().get(targetMC)));
            }
            mc.removeEdgesNumbers();
            if( newMolsAdjacency.containsKey(prodMolComp) )
                newBinds.addAll(newMolsAdjacency.get(prodMolComp));
            addingMap.put(mc, newBinds);
        }
        currentProductGraph.addMolecule(newMolecule, addingMap);
    }

    private void deleteMolecules(List<BionetgenMap> map, BionetgenSpeciesGraph currentProductGraph, BionetgenMap copyMap)
    {
        for( BionetgenMolecule delMol : deletingMolecules )
        {
            if( delMol.getMoleculeComponents().size() == 0 )
            {
                List<BionetgenMolecule> delMols = StreamEx.of( currentProductGraph.getMoleculesList() )
                        .filter( mol -> BionetgenMolecule.compareLocal( mol, delMol ) ).toList();
                StreamEx.of( delMols ).forEach( currentProductGraph::deleteMolecule );
            }
            else
            {
                MoleculeComponent currentDelComp = map.get(reactants.indexOf(delMol.getSpeciesGraph())).getMapForward()
                        .get(delMol.getMoleculeComponents().get(0));
                currentProductGraph.deleteMolecule(copyMap.getMapForward().get(currentDelComp).getMolecule());
            }
        }
    }

    private @CheckForNull List<BionetgenSpeciesGraph> normalizeCurrentProductsOrder(List<BionetgenSpeciesGraph> currentProducts)
    {
        int size = currentProducts.size();
        if( size != products.size() )
            return null;

        List<List<Integer>> allNumbers = new ArrayList<>();
        for( int i = 0; i < size; i++ )
        {
            allNumbers.add(new ArrayList<Integer>());
            BionetgenSpeciesGraph currentProductGraph = currentProducts.get(i);
            boolean added = false;
            for( int j = 0; j < size; j++ )
            {
                BionetgenSpeciesGraph product = products.get(j);
                if( product.isomorphicToSubgraphOf(currentProductGraph, true).size() != 0
                        && checkProductAddition(currentProductGraph, product) )
                {
                    allNumbers.get(i).add(j);
                    added = true;
                }
            }
            if( !added )
                return null;
        }

        return reorder( currentProducts, withoutRepits( new PermutationList<>( allNumbers ) ) );
    }

    private static @CheckForNull List<Integer> withoutRepits(@Nonnull List<List<Integer>> suspects)
    {
        int index = -1;
        SUSP: for( int k = 0; k < suspects.size(); k++ )
        {
            List<Integer> suspect = suspects.get(k);
            int suspectSize = suspect.size();
            for( int i = 0; i < suspectSize - 1; i++ )
            {
                for( int j = i + 1; j < suspectSize; j++ )
                {
                    if( suspect.get(i).equals(suspect.get(j)) )
                        continue SUSP;
                }
            }
            index = k;
        }
        if( index == -1 )
            return null;
        else
            return suspects.get(index);
    }

    private static @CheckForNull List<BionetgenSpeciesGraph> reorder(@Nonnull List<BionetgenSpeciesGraph> cP, List<Integer> values)
    {
        List<BionetgenSpeciesGraph> result = new ArrayList<>();
        int size = cP.size();
        if( values == null || size != values.size() )
            return null;

        for( int i = 0; i < size; i++ )
        {
            for( int j = 0; j < size; j++ )
            {
                if( values.get(j) == i )
                {
                    result.add(cP.get(j));
                    break;
                }
            }
        }

        if( result.size() != size )
            return null;

        return result;
    }

    private boolean isomorphismCheck(List<List<BionetgenSpeciesGraph>> graphLists, List<BionetgenSpeciesGraph> addedGraphs,
            double additionalMultiplier)
    {
        LISTS: for( int j = 0; j < graphLists.size(); j++ )
        {
            List<BionetgenSpeciesGraph> stringGraphList = graphLists.get(j);
            int size = stringGraphList.size();
            for( int i = 0; i < size; i++ )
            {
                if( !stringGraphList.get(i).isomorphicTo(addedGraphs.get(i)) )
                    continue LISTS;
            }
            double multiplier = multipliersList.get(j) + additionalMultiplier;
            multipliersList.set(j, multiplier);
            return false;
        }
        return true;
    }

    private void findMaps()
    {
        List<BionetgenMolecule> reactantsPatternGraph = new ArrayList<>();
        List<BionetgenMolecule> productsPatternGraph = new ArrayList<>();
        List<BionetgenMolecule> ignoreProducts = new ArrayList<>();

        for( BionetgenSpeciesGraph bsg : reactants )
        {
            for( BionetgenMolecule mol : bsg.getMoleculesList() )
            {
                reactantsPatternGraph.add(mol);
            }
        }
        for( BionetgenSpeciesGraph bsg : products )
        {
            for( BionetgenMolecule mol : bsg.getMoleculesList() )
            {
                productsPatternGraph.add(mol);
            }
        }

        List<String> reactantsLabelList = createLabelForMapList(reactantsPatternGraph);
        List<String> productsLabelList = createLabelForMapList(productsPatternGraph);
        int productPatternGraphSize = productsPatternGraph.size();
        for( int i = 0; i < reactantsPatternGraph.size(); i++ )
        {
            BionetgenMolecule reactMol = reactantsPatternGraph.get(i);
            boolean put = false;
            for( int j = 0; j < productPatternGraphSize; j++ )
            {
                BionetgenMolecule prodMol = productsPatternGraph.get(j);
                if( ignoreProducts.contains(prodMol) )
                    continue;
                if( reactantsLabelList.get(i).equals(productsLabelList.get(j)) )
                {
                    patternMap.put(prodMol, reactMol);
                    ignoreProducts.add(prodMol);
                    put = true;
                    break;
                }
            }
            if( !put )
                deletingMolecules.add(reactMol);
        }
    }

    private List<String> createLabelForMapList(List<BionetgenMolecule> mols)
    {
        return StreamEx.of( mols ).map( BionetgenMolecule::createLabelForMap ).toList();
    }

    @Override
    public String toString()
    {
        return StreamEx.of( reactants ).joining( " + " ) + " -> " + StreamEx.of( products ).joining( " + " );
    }

    private static final Pattern INCLUDE_PATTERN = Pattern.compile("include_(reactants|products)\\(([0-9])+(,(.)+)+\\)");
    private static final Pattern EXCLUDE_PATTERN = Pattern.compile("exclude_(reactants|products)\\(([0-9])+(,(.)+)+\\)");

    private void setAddition(String[] additions)
    {
        boolean isReverse = name.startsWith("rev");
        for( String addition : additions )
        {
            Matcher includeMatcher = INCLUDE_PATTERN.matcher(addition);

            if( !includeMatcher.matches() )
            {
                Matcher excludeMatcher = EXCLUDE_PATTERN.matcher(addition);
                if( !excludeMatcher.matches() )
                    throw new IllegalArgumentException("Invalid addition format: " + addition);

                int index = Integer.parseInt(excludeMatcher.group(2)) - 1;
                List<BionetgenSpeciesGraph> speciesList = getAdditionGraphs(
                        addition.substring(addition.indexOf(',') + 1, addition.length() - 1), ',', 0);

                setExcludeMaps(excludeMatcher, index, speciesList, isReverse);
                continue;
            }

            int index = Integer.parseInt(includeMatcher.group(2)) - 1;
            List<BionetgenSpeciesGraph> speciesList = getAdditionGraphs(
                    addition.substring(addition.indexOf(',') + 1, addition.length() - 1), ',', 0);

            setIncludeMaps(includeMatcher, index, speciesList, isReverse);
        }
    }

    private @Nonnull List<BionetgenSpeciesGraph> getAdditionGraphs(String str, char separator, int startIndex)
    {
        List<BionetgenSpeciesGraph> result = new ArrayList<>();
        String tempStr = str;
        int newStartIndex = -1;
        try
        {
            newStartIndex = str.indexOf(separator, startIndex);
            if( newStartIndex != -1 )
            {
                tempStr = str.substring(0, newStartIndex);
            }
            BionetgenSpeciesGraph bsg = new BionetgenSpeciesGraph(tempStr);
            result.add(bsg);
            if( newStartIndex != -1 )
            {
                result.addAll(getAdditionGraphs(str.substring(newStartIndex + 1), separator, 0));
            }
        }
        catch( Exception e )
        {
            if( newStartIndex != -1 )
            {
                result.addAll(getAdditionGraphs(str, separator, newStartIndex + 1));
            }
            if( result.isEmpty() )
                throw new IllegalArgumentException("Reaction template '" + name + "'(Rule#" + name.substring(name.indexOf('j'))
                        + ") contains invalid addition.\n" + e.getMessage() + " in the graph string: '" + str + "'.");
        }
        return result;
    }

    private static final String REACTANTS = "reactants";
    private static final String PRODUCTS = "products";

    private void setIncludeMaps(Matcher includeMatcher, int index, List<BionetgenSpeciesGraph> speciesList, boolean reverse)
    {
        String group = reverse ? PRODUCTS : REACTANTS;
        if( includeMatcher.group(1).equals(group) )
        {
            if( includeReactants.containsKey(reactants.get(index)) )
            {
                includeReactants.get(reactants.get(index)).add(speciesList);
            }
            else
            {
                includeReactants.put(reactants.get(index), new ArrayList<List<BionetgenSpeciesGraph>>());
                includeReactants.get(reactants.get(index)).add(speciesList);
            }
        }
        else
        {
            if( includeProducts.containsKey(products.get(index)) )
            {
                includeProducts.get(products.get(index)).add(speciesList);
            }
            else
            {
                includeProducts.put(products.get(index), new ArrayList<List<BionetgenSpeciesGraph>>());
                includeProducts.get(products.get(index)).add(speciesList);
            }
        }
    }

    private void setExcludeMaps(Matcher excludeMatcher, int index, List<BionetgenSpeciesGraph> speciesList, boolean reverse)
    {
        String group = reverse ? PRODUCTS : REACTANTS;
        if( excludeMatcher.group(1).equals(group) )
        {
            if( excludeReactants.containsKey(reactants.get(index)) )
            {
                excludeReactants.get(reactants.get(index)).add(speciesList);
            }
            else
            {
                excludeReactants.put(reactants.get(index), new ArrayList<List<BionetgenSpeciesGraph>>());
                excludeReactants.get(reactants.get(index)).add(speciesList);
            }
        }
        else
        {
            if( excludeProducts.containsKey(products.get(index)) )
            {
                excludeProducts.get(products.get(index)).add(speciesList);
            }
            else
            {
                excludeProducts.put(products.get(index), new ArrayList<List<BionetgenSpeciesGraph>>());
                excludeProducts.get(products.get(index)).add(speciesList);
            }
        }
    }

    private boolean checkProductAddition(BionetgenSpeciesGraph bsg, BionetgenSpeciesGraph product)
    {
        return checkProductInclude(bsg, product) && checkProductExclude(bsg, product);
    }

    private boolean checkProductExclude(BionetgenSpeciesGraph bsg, BionetgenSpeciesGraph product)
    {
        if( !excludeProducts.containsKey(product) )
        {
            return true;
        }
        ELISTS: for( List<BionetgenSpeciesGraph> excludeOrList : excludeProducts.get(product) )
        {
            for( BionetgenSpeciesGraph excludeBSG : excludeOrList )
            {
                if( excludeBSG.isomorphicToSubgraphOf(bsg, true).size() == 0 )
                    continue ELISTS;
            }
            return false;
        }
        return true;
    }

    private boolean checkProductInclude(BionetgenSpeciesGraph bsg, BionetgenSpeciesGraph product)
    {
        if( !includeProducts.containsKey(product) )
        {
            return true;
        }
        ILISTS: for( List<BionetgenSpeciesGraph> includeOrList : includeProducts.get(product) )
        {
            for( BionetgenSpeciesGraph includeBSG : includeOrList )
            {
                if( includeBSG.isomorphicToSubgraphOf(bsg, true).size() != 0 )
                    continue ILISTS;
            }
            return false;
        }
        return true;
    }

    private boolean checkReactantAddition(BionetgenSpeciesGraph bsg, BionetgenSpeciesGraph reactant)
    {
        return checkReactantInclude(bsg, reactant) && checkReactantExclude(bsg, reactant);
    }

    private boolean checkReactantInclude(BionetgenSpeciesGraph bsg, BionetgenSpeciesGraph reactant)
    {
        if( !includeReactants.containsKey(reactant) )
        {
            return true;
        }
        ILISTS: for( List<BionetgenSpeciesGraph> includeOrList : includeReactants.get(reactant) )
        {
            for( BionetgenSpeciesGraph includeBSG : includeOrList )
            {
                if( includeBSG.isomorphicToSubgraphOf(bsg, true).size() != 0 )
                    continue ILISTS;
            }
            return false;
        }
        return true;
    }

    private boolean checkReactantExclude(BionetgenSpeciesGraph bsg, BionetgenSpeciesGraph reactant)
    {
        if( !excludeReactants.containsKey(reactant) )
        {
            return true;
        }
        ELISTS: for( List<BionetgenSpeciesGraph> excludeOrList : excludeReactants.get(reactant) )
        {
            for( BionetgenSpeciesGraph excludeBSG : excludeOrList )
            {
                if( excludeBSG.isomorphicToSubgraphOf(bsg, true).size() == 0 )
                    continue ELISTS;
            }
            return false;
        }
        return true;
    }


    private int findMoleculesSymmetryMultiplier(List<BionetgenMap> maps)
    {
        int result = 1;
        for( MoleculeComponent centerComp : reactionCenter )
        {
            if( centerComp.getEdges().size() != 0 )
                continue;
            BionetgenMolecule currentCenterMol = maps.get(reactants.indexOf(centerComp.getMolecule().getSpeciesGraph())).getMapForward()
                    .get(centerComp).getMolecule();
            result *= (int)StreamEx.of( currentCenterMol.getMoleculeComponents() )
                    .filter( mc -> MoleculeComponent.compareLocal( centerComp, mc ) ).count();
        }
        return result;
    }

    private List<MoleculeComponent> getReactionCenter()
    {
        return EntryStream.of( patternMap )
                .map( entryPattern -> BionetgenMolecule.findDifference( entryPattern.getValue(), entryPattern.getKey() ) ).nonNull()
                .flatCollection( Map::keySet ).toList();
    }

    public static boolean checkStoichiometry(Map<String, Integer> maxStoich, List<BionetgenSpeciesGraph> currentProducts)
    {
        return maxStoich == null || !StreamEx.of( currentProducts ).anyMatch( p -> !p.fulfillStoichiometry( maxStoich ) );
    }
}