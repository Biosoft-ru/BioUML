
package biouml.plugins.bindingregions.utils;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.WritableTrack;
import ru.biosoft.util.JsonUtils;
import ru.biosoft.util.ObjectCache;
import biouml.plugins.bindingregions.utils.EnsemblUtils.Gene;
import biouml.plugins.gtrd.TrackSqlTransformer;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.eclipsesource.json.JsonObject;

/**
 * @author yura
 *
 */
public class CisModule implements Comparable<CisModule>
{
    public static final String NUMBER_OF_TF_CLASSES = "numberOfTfClasses";
    public static final String MINIMAL_NUMBER_OF_OVERLAPS = "minimalNumberOfOverlaps";
    public static final String DISTINCT_TFCLASSES_AND_NAMES = "distinctTfClassesAndNames";
    public static final String TF_CLASSES = "tfClasses";
    public static final String SEPARATOR_BETWEEN_TFCLASSES = ";";

    int startPosition;
    int finishPosition;
    List<String> tfClasses;

    public CisModule(int startPosition, int finishPosition, List<String> tfClasses)
    {
        this.startPosition = startPosition;
        this.finishPosition = finishPosition;
        this.tfClasses = tfClasses;
    }

    public int getStartPosition()
    {
        return startPosition;
    }

    public int getFinishPosition()
    {
        return finishPosition;
    }

    public int getMiddlePosition()
    {
        return (startPosition + finishPosition) / 2;
    }

    public List<String> getTfClasses()
    {
        return tfClasses;
    }
    
    public int getLength()
    {
        return finishPosition - startPosition + 1;
    }
    
    public int getNumberOfTfClasses()
    {
        return tfClasses.size();
    }
    
    public Map<String, Boolean> getIndicatorVectorOfTfClasses (List<String> distinctTfClasses)
    {
        Map<String, Boolean> result = new HashMap<>();
        for( String tfClass : distinctTfClasses )
            result.put(tfClass, false);
        for( String tfClass : tfClasses )
            result.put(tfClass, true);
        return result;
    }
    
    public List<String> getDifferentTfClasses()
    {
        return StreamEx.of(tfClasses).distinct().toList();
    }
    
    public int getNumberOfDistinctTfClasses()
    {
        return getDifferentTfClasses().size();
    }

    public boolean isBelongToCisModule(String tfClass)
    {
        return tfClasses.contains(tfClass);
    }
    
    @Override
    public int compareTo(CisModule o)
    {
        return startPosition - o.getStartPosition();
    }

    private static Map<String, String> refreshTfClassAndTfName(Map<String, List<CisModule>> allCisModules, Map<String, String> tfClassAndTfName)
    {
        return StreamEx.of( getDistinctTfClasses(allCisModules) ).toMap( tfClassAndTfName::get );
    }
    
    public static void writeCisModulesIntoTrack(Map<String, List<CisModule>> allCisModules, int minimalNumberOfOverlaps, String specie, String cellLine, String pathToSequenceCollection, Map<String, String> tfClassAndTfName, DataElementPath pathToOutputs, String nameOfTrack) throws Exception
    {
        if( allCisModules.isEmpty() ) return;
        Properties properties = new Properties();
        properties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, nameOfTrack);
        properties.setProperty(TrackSqlTransformer.SPECIE_PROPERTY, specie);
        properties.setProperty(Track.SEQUENCES_COLLECTION_PROPERTY, pathToSequenceCollection);
        properties.setProperty(TrackSqlTransformer.CELL_LINE_PROPERTY, cellLine);
        properties.setProperty(MINIMAL_NUMBER_OF_OVERLAPS, Integer.toString(minimalNumberOfOverlaps));
        if( tfClassAndTfName != null )
        {
            Map<String, String> refreshedTfClassAndTfName = refreshTfClassAndTfName(allCisModules, tfClassAndTfName);
            JsonObject object = JsonUtils.fromMap( refreshedTfClassAndTfName );
            properties.setProperty(DISTINCT_TFCLASSES_AND_NAMES, object.toString());
        }
        WritableTrack track = TrackUtils.createTrack(pathToOutputs.getDataCollection(), properties);
        for( Sequence sequence : EnsemblUtils.getSequences(DataElementPath.create(pathToSequenceCollection)) )
        {
            List<CisModule> cisModules = allCisModules.get(sequence.getName());
            if( cisModules == null ) continue;
            for( CisModule cisModule : cisModules )
            {
                int start = cisModule.getStartPosition();
                int length = cisModule.getLength();
                if( start <= 0 || length <= 0 ) continue;
                List<String> tfClasses = cisModule.getTfClasses();
                int numberOfTfClasses = tfClasses.size();
                String siteName = Integer.toString(numberOfTfClasses);
                Site site = new SiteImpl(null, siteName, siteName, Site.BASIS_PREDICTED, start, length, Site.PRECISION_NOT_KNOWN, Site.STRAND_BOTH, sequence, null);
                DynamicPropertySet dps = site.getProperties();
                dps.add(new DynamicProperty(NUMBER_OF_TF_CLASSES, Integer.class, numberOfTfClasses));
                StringBuilder s = new StringBuilder();
                for( String tfClass : tfClasses )
                {
                    if( s.length() > 0 )
                        s.append( SEPARATOR_BETWEEN_TFCLASSES );
                    s.append( tfClass );
                }
                dps.add(new DynamicProperty(TF_CLASSES, String.class, s.toString()));
                track.addSite(site);
            }
        }
        track.finalizeAddition();
        DataElementPath.create(track).save(track);
    }

    //////////////// O.K.
    public static List<CisModule> getCisModules1(String chromosome, Map<String, List<BindingRegion>> allBindingRegions, int minimalNumberOfOverlaps)
    {
        List<CisModule> result = new ArrayList<>();
        List<BindingRegion> bindingRegions = allBindingRegions.get(chromosome);
        for( int i = 0; i < bindingRegions.size() - 1; i++ )
        {
            BindingRegion br1 = bindingRegions.get(i);
            int finishPosition = br1.getFinishPosition();
            List<String> tfClasses = new ArrayList<>();
            tfClasses.add(br1.getTfClass());
            for( int ii = i + 1; ii < bindingRegions.size(); ii++ )
            {
                BindingRegion br2 = bindingRegions.get(ii);
                if( finishPosition < br2.getStartPosition() )
                {
                    i = ii - 1;
                    break;
                }
                else
                {
                    if( finishPosition < br2.getFinishPosition() )
                        finishPosition = br2.getFinishPosition();
                    tfClasses.add(br2.getTfClass());
                }
            }
            if( tfClasses.size() >= minimalNumberOfOverlaps )
                result.add(new CisModule(br1.getStartPosition(), finishPosition, tfClasses));
        }
        return result;
    }
    
    //////////////// O.K.
    public static Map<String, List<CisModule>> getCisModules1(Map<String, List<BindingRegion>> allBindingRegions, int minimalNumberOfOverlaps)
    {
        Map<String, List<CisModule>> result = new HashMap<>();
        Iterator<String> it = allBindingRegions.keySet().iterator();
        while( it.hasNext() )
        {
            String chromosome = it.next();
            List<CisModule> cisModules = getCisModules1(chromosome, allBindingRegions, minimalNumberOfOverlaps);
            if( ! cisModules.isEmpty() )
                result.put(chromosome, cisModules);
        }
        return result;
    }

    //////////////// O.K.
    public static List<CisModule> getCisModules2InOneChromosome(List<BindingRegion> bindingRegions, int minimalNumberOfOverlaps)
    {
        List<CisModule> result = new ArrayList<>();
        List<CisModule> preliminaryResult = new ArrayList<>();
        int startOfOverlaps = 0;
        int endOfOverlaps = 0;
        int indexOfNewBindingRegion = 0;
        List<BindingRegion> overlappedBindingRegions = new ArrayList<>();
        while( indexOfNewBindingRegion < bindingRegions.size() )
        {
            if( overlappedBindingRegions.size() == 0 )
            {
                BindingRegion bindingRegion = bindingRegions.get(indexOfNewBindingRegion);
                overlappedBindingRegions.add(bindingRegion);
                indexOfNewBindingRegion++;
                startOfOverlaps = bindingRegion.getStartPosition();
                endOfOverlaps = bindingRegion.getFinishPosition();
                if( indexOfNewBindingRegion < bindingRegions.size() )
                {
                    int candidateForEndOfOverlaps = bindingRegions.get(indexOfNewBindingRegion).getStartPosition();
                    if( candidateForEndOfOverlaps < endOfOverlaps )
                        endOfOverlaps = candidateForEndOfOverlaps;
                }
            }
            else
            {
                BindingRegion bindingRegion = bindingRegions.get(indexOfNewBindingRegion);
                int timeWaitingForBirth = bindingRegion.getStartPosition() - endOfOverlaps;
                int timeWaitingForDeath = Integer.MAX_VALUE;
                int indexForNewDeath = 0;
                for( int i = 0; i < overlappedBindingRegions.size(); i++ )
                {
                    BindingRegion br = overlappedBindingRegions.get(i);
                    int time = br.getFinishPosition() - endOfOverlaps;
                    if( time < timeWaitingForDeath )
                    {
                        timeWaitingForDeath = time;
                        indexForNewDeath = i;
                    }
                }
                if( timeWaitingForBirth <= timeWaitingForDeath )
                {
                    overlappedBindingRegions.add(bindingRegion);
                    indexOfNewBindingRegion++;
                }
                else
                    overlappedBindingRegions.remove(indexForNewDeath);
                startOfOverlaps = endOfOverlaps;
                endOfOverlaps = Integer.MAX_VALUE;
                if( indexOfNewBindingRegion < bindingRegions.size() )
                    endOfOverlaps = bindingRegions.get(indexOfNewBindingRegion).getStartPosition();
                endOfOverlaps = StreamEx.of( overlappedBindingRegions ).mapToInt( BindingRegion::getFinishPosition ).min()
                        .orElse( endOfOverlaps );
            }
            if( overlappedBindingRegions.size() >= minimalNumberOfOverlaps )
            {
                List<String> tfClasses = StreamEx.of( overlappedBindingRegions ).map( BindingRegion::getTfClass ).toList();
                CisModule cisModule = new CisModule(startOfOverlaps, endOfOverlaps, tfClasses);
                if( preliminaryResult.isEmpty() )
                    preliminaryResult.add(cisModule);
                else
                    if( preliminaryResult.get(preliminaryResult.size() - 1).getFinishPosition() == startOfOverlaps )
                        preliminaryResult.add(cisModule);
                    else
                    {
                        result.add(StreamEx.of( preliminaryResult ).maxByInt( CisModule::getNumberOfTfClasses ).get());
                        preliminaryResult.clear();
                    }
            }
            if( overlappedBindingRegions.size() == minimalNumberOfOverlaps - 1 && ! preliminaryResult.isEmpty() )
            {
                result.add(StreamEx.of( preliminaryResult ).maxByInt( CisModule::getNumberOfTfClasses ).get());
                preliminaryResult.clear();
            }
        }
        if( ! preliminaryResult.isEmpty() )
        {
            result.add(StreamEx.of( preliminaryResult ).maxByInt( CisModule::getNumberOfTfClasses ).get());
        }
        return result;
    }

    // This version of method 'getAllCisModules2' requires a lot of memory
    // Main analyzes avoid this version now
    public static Map<String, List<CisModule>> getAllCisModules2(Map<String, List<BindingRegion>> allBindingRegions, int minimalNumberOfOverlaps)
    {
        return EntryStream.of(allBindingRegions)
                .mapValues(val -> getCisModules2InOneChromosome(val, minimalNumberOfOverlaps))
                .removeValues(List::isEmpty).toMap();
    }
    
    // new version of 'getAllCisModules2' is preferable from point of view of memory
    public static Map<String, List<CisModule>> getAllCisModules2(Track trackWithBindingRegions, List<String> givenTfClasses, int minimalNumberOfOverlaps) throws Exception
    {
        Map<String, List<CisModule>> result = new HashMap<>();
        Set<String> chromosomes = getChromosomeNames(trackWithBindingRegions);
        for( String chromosome : chromosomes )
        {
            List<BindingRegion> brs = BindingRegion.readBindingRegionsFromTrack(trackWithBindingRegions, chromosome, givenTfClasses);
            Collections.sort(brs);
            List<CisModule> cisModules = getCisModules2InOneChromosome(brs, minimalNumberOfOverlaps);
            if( ! cisModules.isEmpty() )
                result.put(chromosome, cisModules);
        }
        return result;
    }
    
    private static Set<String> getChromosomeNames(Track track)
    {
        return track.getAllSites().stream().map( site -> site.getSequence().getName() ).collect( Collectors.toSet() );
    }

    public static Set<String> getDistinctTfClasses(Map<String, List<CisModule>> allCisModules)
    {
        return StreamEx.ofValues(allCisModules).flatMap( List::stream ).flatCollection( CisModule::getTfClasses ).toSet();
    }
    
    public static Map<String, List<CisModule>> readCisModulesInSqlTrack(Track track, int minSizeOfCisModules)
    {
        Map<String, List<CisModule>> result = new HashMap<>();
        ObjectCache<String> distinctTfClasses = new ObjectCache<>();
        for( Site site : track.getAllSites() )
        {
            String chromosome = site.getSequence().getName();
            int start = site.getFrom();
            int end = site.getTo();
            if( start <= 0 || start > end ) continue;
            String stringWithTfClasses = site.getProperties().getValueAsString(TF_CLASSES);
            String splittedTfClasses[] = stringWithTfClasses.split(SEPARATOR_BETWEEN_TFCLASSES);
            if( splittedTfClasses.length < minSizeOfCisModules ) continue;
            List<String> tfClasses = StreamEx.of(splittedTfClasses).map( distinctTfClasses::get ).toList();
            CisModule cisModule = new CisModule(start, end, tfClasses);
            result.computeIfAbsent( chromosome, k -> new ArrayList<>() ).add( cisModule );
        }
        return result;
    }
    
    public static TObjectIntMap<String> getFrequenciesOfTfClasses(Map<String, List<CisModule>> allCisModules)
    {
        TObjectIntMap<String> result = new TObjectIntHashMap<>();
        for( List<CisModule> cisModules : allCisModules.values() )
            for( CisModule cisModule : cisModules )
                for( String tfClass : cisModule.getTfClasses() )
                    result.adjustOrPutValue(tfClass, 1, 1);
        return result;
    }

    /***
     * 
     * @param allCisModules
     * @param tfClass1
     * @param tfClass2
     * @return array od dimension 4: n00, n01, n10, n11
     */
    public static int[] getContingencyTable(Map<String, List<CisModule>> allCisModules, String tfClass1, String tfClass2)
    {
        int[] result = new int[4];
        for( List<CisModule> cisModules : allCisModules.values() )
            for( CisModule cisModule : cisModules )
            {
                List<String> tfClasses = cisModule.getTfClasses();
                int i = 0;
                int j = 0;
                if( tfClasses.contains(tfClass1) )
                    i = 1;
                if( tfClasses.contains(tfClass2) )
                    j = 1;
                result[ 2 * i + j]++;
            }
        return result;
    }
    
    /***
     * Genes have to be sorted!
     * @param chromosomesAndGivenGenes
     * @param chromosomeAndCisModules
     * @param distanceThreshold
     * @return CisModules located near given genes
     */
    public static Map<String, List<CisModule>> getCisModulesNearGivenGenes(Map<String, List<Gene>> chromosomesAndGivenGenes, Map<String, List<CisModule>> chromosomeAndCisModules, int distanceThreshold)
    {
        Map<String, List<CisModule>> result = new HashMap<>();
        for( Entry<String, List<Gene>> entry : chromosomesAndGivenGenes.entrySet() )
        {
            String chromosome = entry.getKey();
            List<CisModule> list = new ArrayList<>();
            List<CisModule> cisModules = chromosomeAndCisModules.get(chromosome);
            for( Gene gene : entry.getValue() )
            {
                int[] geneBoundaries = gene.getStartAndEndOfGene();
                geneBoundaries[0] -= distanceThreshold;
                if( geneBoundaries[0] < 1) geneBoundaries[0] = 1;
                geneBoundaries[1] += distanceThreshold;
                for( CisModule cisModule : cisModules )
                {
                    if( cisModule.getStartPosition() > geneBoundaries[1] ) break;
                    if( cisModule.getFinishPosition() < geneBoundaries[0] ) continue;
                    list.add(cisModule);
                }
            }
            if( ! list.isEmpty() )
                result.put(chromosome, list);
        }
        return result;
    }
}