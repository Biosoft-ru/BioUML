package biouml.plugins.bionetgen.diagram;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import one.util.streamex.StreamEx;

public class BionetgenMap
{
    private BionetgenSpeciesGraph source = null;
    private BionetgenSpeciesGraph target = null;
    private Map<MoleculeComponent, MoleculeComponent> mapForward = null;
    private Map<BionetgenMolecule, BionetgenMolecule> moleculesMap = null;

    public BionetgenMap(BionetgenSpeciesGraph bsgSource, BionetgenSpeciesGraph bsgTarget, Map<MoleculeComponent, MoleculeComponent> map,
            Map<BionetgenMolecule, BionetgenMolecule> molMap)
    {
        source = bsgSource;
        target = bsgTarget;
        mapForward = map;
        moleculesMap = molMap;
    }

    public BionetgenSpeciesGraph getSource()
    {
        return source;
    }

    public BionetgenSpeciesGraph getTarget()
    {
        return target;
    }

    public Map<MoleculeComponent, MoleculeComponent> getMapForward()
    {
        return mapForward;
    }

    public Map<BionetgenMolecule, BionetgenMolecule> getMoleculesMap()
    {
        return moleculesMap;
    }

    @Override
    public String toString()
    {
        Set<String> result = new TreeSet<>();

        for( Map.Entry<MoleculeComponent, MoleculeComponent> entry : mapForward.entrySet() )
        {
            result.add(entry.getKey().getMolecule().toString().concat("->").concat(entry.getValue().getMolecule().toString()));
        }

        if( mapForward.size() == 0 && moleculesMap != null )
        {
            for( Map.Entry<BionetgenMolecule, BionetgenMolecule> entry : moleculesMap.entrySet() )
                result.add(entry.getKey().toString() + "->" + entry.getValue().toString());
        }

        return result.toString();
    }

    public static List<List<BionetgenMap>> getDisjointMap(List<List<BionetgenMap>> allMaps, List<MoleculeComponent> reactionCenter)
    {
        allMaps = mapsWithDifCenter(allMaps, reactionCenter);
        PermutationList<BionetgenMap> pml = new PermutationList<>( allMaps );
        List<List<BionetgenMap>> result = new ArrayList<>();

        MAPS: for( List<BionetgenMap> mapList : pml )
        {
            List<BionetgenMap> localResult = new ArrayList<>();
            localResult.add(mapList.get(0));
            int size = mapList.size();
            if( size == 1 )
            {
                result.add(localResult);
                continue MAPS;
            }
            for( int i = 1; i < size; i++ )
            {
                if( areDisjointList(localResult, mapList.get(i)) )
                {
                    localResult.add(mapList.get(i));
                }
                else
                    continue MAPS;
            }
            if( withoutRepits(localResult, result) )
                result.add(localResult);
        }
        return result;
    }

    private static List<List<BionetgenMap>> mapsWithDifCenter(List<List<BionetgenMap>> allMaps, List<MoleculeComponent> reactionCenter)
    {
        if( reactionCenter == null || reactionCenter.isEmpty() )
            return allMaps;

        List<List<BionetgenMap>> mapLists = new ArrayList<>();
        for( List<BionetgenMap> currentMaps : allMaps )
        {
            List<BionetgenMap> currentMapList = new ArrayList<>();
            currentMapList.add(currentMaps.get(0));
            for( int j = 1; j < currentMaps.size(); j++ )
            {
                BionetgenMap currentMap = currentMaps.get(j);
                if( !StreamEx.of( currentMapList ).allMatch( otherMap -> hasDifferentCenter( currentMap, otherMap, reactionCenter ) ) )
                    continue;
                currentMapList.add(currentMap);
            }
            mapLists.add(currentMapList);
        }
        return mapLists;
    }

    private static boolean hasDifferentCenter(BionetgenMap map1, BionetgenMap map2, List<MoleculeComponent> center)
    {
        Map<MoleculeComponent, MoleculeComponent> fMap1 = map1.getMapForward();
        Map<MoleculeComponent, MoleculeComponent> fMap2 = map2.getMapForward();
        return StreamEx.of( center ).anyMatch( centerComp -> fMap1.get( centerComp ) != fMap2.get( centerComp ) );
    }

    private static boolean withoutRepits(List<BionetgenMap> addingMapsList, List<List<BionetgenMap>> resultMapsLists)
    {
        LISTS: for( List<BionetgenMap> mapsList : resultMapsLists )
        {
            int size = mapsList.size();
            if( size != addingMapsList.size() )
                continue LISTS;
            for( int i = 0; i < size; i++ )
            {
                if( areDifferent(mapsList.get(i), addingMapsList.get(i)) )
                    continue LISTS;
            }
            return false;
        }
        return true;
    }

    private static boolean areDifferent(BionetgenMap map1, BionetgenMap map2)
    {
        Map<MoleculeComponent, MoleculeComponent> mapForward1 = map1.getMapForward();
        if( mapForward1.size() != map2.getMapForward().size() )
            return true;

        Map<BionetgenMolecule, BionetgenMolecule> moleculesMap1 = map1.getMoleculesMap();
        Map<BionetgenMolecule, BionetgenMolecule> moleculesMap2 = map2.getMoleculesMap();
        if( moleculesMap1 != null && moleculesMap2 != null )
        {
            if( moleculesMap1.size() != moleculesMap2.size() )
                return true;
            for( Map.Entry<BionetgenMolecule, BionetgenMolecule> entry : moleculesMap1.entrySet() )
            {
                if( moleculesMap2.get(entry.getKey()) != entry.getValue() )
                    return true;
            }
            return false;
        }
        for( Map.Entry<MoleculeComponent, MoleculeComponent> entry : mapForward1.entrySet() )
        {
            if( map2.getMapForward().get(entry.getKey()).getMolecule() != entry.getValue().getMolecule() )
                return true;
        }
        return false;
    }

    private static boolean areDisjointList(List<BionetgenMap> listMap, BionetgenMap map2)
    {
        return !StreamEx.of( listMap ).anyMatch( map1 -> !areDisjoint( map1, map2 ) );
    }

    private static boolean areDisjoint(BionetgenMap map1, BionetgenMap map2)
    {
        Map<MoleculeComponent, MoleculeComponent> mapForward1 = map1.getMapForward();
        Map<MoleculeComponent, MoleculeComponent> mapForward2 = map2.getMapForward();
        int mapForward1Size = mapForward1.size();
        if( mapForward1Size != mapForward2.size() )
            return true;
        for( Map.Entry<MoleculeComponent, MoleculeComponent> entry : mapForward2.entrySet() )
        {
            if( mapForward1.containsValue(entry.getValue()) )
                return false;
        }
        Map<BionetgenMolecule, BionetgenMolecule> moleculesMap1 = map1.getMoleculesMap();
        Map<BionetgenMolecule, BionetgenMolecule> moleculesMap2 = map2.getMoleculesMap();
        if( mapForward1Size == 0 && moleculesMap1 != null && moleculesMap2 != null )
        {
            if( moleculesMap1.size() != moleculesMap2.size() )
                return true;
            for( Map.Entry<BionetgenMolecule, BionetgenMolecule> entry : moleculesMap2.entrySet() )
            {
                if( moleculesMap1.containsValue(entry.getValue()) )
                    return false;
            }
        }
        return true;
    }
}
