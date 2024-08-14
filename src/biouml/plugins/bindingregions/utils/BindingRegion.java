
package biouml.plugins.bindingregions.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;

import biouml.plugins.bindingregions.utils.EnsemblUtils.Gap;
import biouml.plugins.gtrd.TrackSqlTransformer;
import biouml.standard.type.Species;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.MergedTrack;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.UnionTrack;
import ru.biosoft.bsa.WritableTrack;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.JsonUtils;
import ru.biosoft.util.ListUtil;
import ru.biosoft.util.ObjectCache;

/**
 * @author yura
 *
 */
public class BindingRegion implements Comparable<BindingRegion>
{
    public static final String NUMBER_OF_OVERLAPS = "numberOfOverlaps";
    public static final String NUMBER_OF_MERGED_TRACKS = "numberOfMergedTracks";
    public static final String ALL_TF_CLASSES = "allTfClasses";
    public static final String ALL_CELL_LINES = "allCellLines";
    public static final String NAMES_OF_OVERLAPPED_TRACKS = "namesOfOverlappedTracks";
    public static final String SEPARATOR_BETWEEN_TF_CLASS_AND_ITS_NAME = ": ";

    String tfClass;
    Interval interval;
    int numberOfOverlaps;

    public BindingRegion(String tfClass, Interval interval, int numberOfOverlaps)
    {
        this.tfClass = tfClass;
        this.interval = interval;
        this.numberOfOverlaps = numberOfOverlaps;
    }

    public BindingRegion(String tfClass, int startPosition, int finishPosition, int numberOfOverlaps)
    {
        this(tfClass, new Interval(startPosition, finishPosition), numberOfOverlaps);
    }

    public BindingRegion(String tfClass, Interval interval)
    {
        this(tfClass, interval, 0);
    }

    public String getTfClass()
    {
        return tfClass;
    }
    
    public Interval getInterval()
    {
        return interval;
    }

    public int getStartPosition()
    {
        return interval.getFrom();
    }

    public int getFinishPosition()
    {
        return interval.getTo();
    }

    public int getCenterPosition()
    {
        return interval.getCenter();
    }
    
    public int getNumberOfOverlap()
    {
        return numberOfOverlaps;
    }
    
    public int getLengthOfBindingRegion()
    {
        return interval.getLength();
    }

    public void increaseNumberOfOverlaps()
    {
        numberOfOverlaps++;
    }
    
    public int getStartPositionCorrectedOnGaps(List<Gap> gaps)
    {
        int correctedPosition = interval.getFrom();
        for( Gap gap : gaps )
        {
            correctedPosition = gap.correct(correctedPosition);
        }
        return correctedPosition;
    }
    
    public int getFinishPositionCorrectedOnGaps(List<Gap> gaps)
    {
        int correctedPosition = interval.getTo();
        for( Gap gap : gaps )
        {
            correctedPosition = gap.correct(correctedPosition);
        }
        return correctedPosition;
    }

    @Override
    public int compareTo(BindingRegion o)
    {
        return interval.compareTo(o.interval);
    }
    
    /***
     * Create merged binding regions for single tfClass for which there exists single track of Chip-Seq peaks.
     * This method may be applied when some peaks from the same track can be overlapped.
     * @param trackWithPeaks
     * @param chromosomes
     * @param tfClass
     * @return
     * @throws Exception
     */
    public static Map<String, List<BindingRegion>> getMergedBindingRegionsFromTrackWithPeaks(Track trackWithPeaks, Collection<String> chromosomes, String tfClass)
    {
        return getMergedBindingRegionsFromTracksWithPeaks(Collections.singletonList(trackWithPeaks), chromosomes, tfClass);
    }
    
    /***
     * Create merged binding regions for single tfClass for which there exist several tracks of Chip-Seq peaks
     * @param tracks
     * @param chromosomes
     * @param tfClass
     * @return
     * @throws Exception
     */
    public static Map<String, List<BindingRegion>> getMergedBindingRegionsFromTracksWithPeaks(List<Track> tracks, Collection<String> chromosomes, String tfClass)
    {
        UnionTrack unionTrack = new UnionTrack("none", null, tracks);
        MergedTrack mergedTrack = new MergedTrack(unionTrack);
        return StreamEx.of(chromosomes).mapToEntry(chromosome -> mergedTrack.getSites(chromosome, 0, Integer.MAX_VALUE).stream()
                    .filter(site -> site.getLength() > 0)
                .map( site -> new BindingRegion( tfClass, site.getInterval(), 0 ) ).collect( Collectors.toList() ) )
                .removeValues(List::isEmpty).toMap();
    }
    
    public static List<BindingRegion> getMergedBindingRegionsFromTracksWithPeaks(List<Track> tracks, String chromosome, String tfClass) throws Exception
    {
        UnionTrack unionTrack = new UnionTrack("none", null, tracks);
        MergedTrack mergedTrack = new MergedTrack(unionTrack);
        return mergedTrack.getSites(chromosome, 0, Integer.MAX_VALUE).stream()
                .filter(site -> site.getLength() > 0)
                .map( site -> new BindingRegion( tfClass, site.getInterval(), 0 ) ).collect( Collectors.toList() );
    }

    ///////////////////////////////////////////// O.K.
    public static Map<String, List<CisModule>> getAllBindingRegionsAsCisModules2(List<Track> tracks, Collection<String> chromosomeNames, int minimalNumberOfOverlaps) throws Exception
    {
        if( tracks == null || tracks.size() < minimalNumberOfOverlaps ) return null;
        Map<String, List<BindingRegion>> allBindingRegions = StreamEx.of(tracks)
                .flatMapToEntry(track -> getMergedBindingRegionsFromTrackWithPeaks(track, chromosomeNames, track.getName()))
                .flatMapValues(List::stream).grouping();
        ListUtil.sortAll(allBindingRegions);
        Map<String, List<CisModule>> allCisModules2 = CisModule.getAllCisModules2(allBindingRegions, minimalNumberOfOverlaps);
        return allCisModules2;
    }

    public static Track writeTrackWithBindingRegionsIdentifiedAsCisModules2(Map<String, List<CisModule>> allCisModules2, String tfClass, int numberOfUsedTracks, Species givenSpecie, DataElementPath pathToSequences, DataElementPath pathToOutputs, String nameOfTrack) throws Exception
    {
        if( allCisModules2 == null || allCisModules2.isEmpty()) return null;
        Properties properties = new Properties();
        properties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, nameOfTrack);
        properties.setProperty(TrackSqlTransformer.SPECIE_PROPERTY, givenSpecie.getLatinName());
        properties.setProperty(Track.SEQUENCES_COLLECTION_PROPERTY, pathToSequences.toString());
        properties.setProperty(TrackSqlTransformer.TF_CLASS_ID_PROPERTY, tfClass);
        properties.setProperty(NUMBER_OF_MERGED_TRACKS, Integer.toString(numberOfUsedTracks));
        WritableTrack result = TrackUtils.createTrack(pathToOutputs.getDataCollection(), properties);
        List<Sequence> sequences = EnsemblUtils.getSequences(pathToSequences);
        for( Sequence sequence : sequences )
        {
            String chromosome = sequence.getName();
            if( ! allCisModules2.containsKey(chromosome) ) continue;
            List<CisModule> cisModules2 = allCisModules2.get(chromosome);
            for( CisModule cisModule : cisModules2 )
            {
                int start = cisModule.getStartPosition();
                int length = cisModule.getFinishPosition() - start + 1;
                if( length <= 0 ) continue;
                int numberOfOverlaps = cisModule.getNumberOfTfClasses();
                Site site = new SiteImpl(null, tfClass, tfClass + "_" + numberOfOverlaps, Site.BASIS_PREDICTED, start, length, Site.PRECISION_NOT_KNOWN, Site.STRAND_BOTH, sequence, null);
                DynamicPropertySet dps = site.getProperties();
                dps.add(new DynamicProperty(NUMBER_OF_OVERLAPS, Integer.class, numberOfOverlaps));
                dps.add(new DynamicProperty(TrackSqlTransformer.TF_CLASS_ID_PROPERTY, String.class, tfClass));
                dps.add(new DynamicProperty(NUMBER_OF_MERGED_TRACKS, Integer.class, numberOfUsedTracks));
                List<String> namesOfOverlappedTracks = cisModule.getTfClasses();
                String s = String.join(";", namesOfOverlappedTracks);
                dps.add(new DynamicProperty(NAMES_OF_OVERLAPPED_TRACKS, String.class, s));
                result.addSite(site);
            }
        }
        result.finalizeAddition();
        CollectionFactoryUtils.save(result);
        return result;
    }

    ///////////////////////////////////////////// O.K.
    public static Map<String, List<BindingRegion>> readBindingRegionsInTable(DataElementPath dataElementPath, String nameOfTable) throws Exception
    {
        DataElementPath dep = dataElementPath.getChildPath(nameOfTable);
        Map<String, List<BindingRegion>> result = new HashMap<>();
        TableDataCollection table = dep.getDataElement(TableDataCollection.class);
        ObjectCache<String> tfClassNames = new ObjectCache<>();
        for( RowDataElement row : table )
        {
            Object[] objects = row.getValues();
            String chromosome = (String)objects[0];
            int startPosition = (Integer)objects[1];
            int finishPosition = (Integer)objects[2];
            if( startPosition > finishPosition ) continue;
            String tfClass = tfClassNames.get( (String)objects[3] );
            int numberOfOverlaps = (Integer)objects[4];
            BindingRegion bindingRegion = new BindingRegion(tfClass, startPosition, finishPosition, numberOfOverlaps);
            result.computeIfAbsent( chromosome, k -> new ArrayList<>() ).add(bindingRegion);
        }
        return result;
    }

    // 1. Basic 'readBindingRegionsFromTrack'
    public static List<BindingRegion> readBindingRegionsFromTrack(Track track, String chromosome, List<String> givenTfClasses)
    {
        List<BindingRegion> result = new ArrayList<>();
        String pathToSequences = ((DataCollection<?>)track).getInfo().getProperty(Track.SEQUENCES_COLLECTION_PROPERTY);
        String sequence = DataElementPath.create(pathToSequences).getChildPath(chromosome).toString();
        ObjectCache<String> distinctTfClasses = new ObjectCache<>();
//      for(Site site: track.getAllSites())
        for( Site site : track.getSites(sequence, 0, Integer.MAX_VALUE) )
        {
            if( site.getLength() <= 0 ) continue;
            DynamicPropertySet properties = site.getProperties();
            String tfClass = properties.getValueAsString(TrackSqlTransformer.TF_CLASS_ID_PROPERTY);
            if(tfClass == null)
                throw new IllegalArgumentException("Invalid track supplied");
            if( givenTfClasses != null && ! givenTfClasses.contains(tfClass) ) continue;
            tfClass = distinctTfClasses.get(tfClass);
            Integer numberOfOverlaps = (Integer)properties.getValue(NUMBER_OF_OVERLAPS);
            if( numberOfOverlaps == null )
                throw new IllegalArgumentException("Invalid track supplied");
            BindingRegion bindingRegion = new BindingRegion(tfClass, site.getInterval(), numberOfOverlaps);
            result.add(bindingRegion);
        }
        return result;
    }
    
    // 2. In future, it is necessary to replace it by basic version because it require a lot of memory
    public static Map<String, List<BindingRegion>> readBindingRegionsFromTrack(Track track, List<String> givenTfClasses) throws Exception
    {
        String pathToSequences = ((DataCollection<?>)track).getInfo().getProperty(Track.SEQUENCES_COLLECTION_PROPERTY);
        return EnsemblUtils.sequenceNames(DataElementPath.create(pathToSequences))
                .mapToEntry(chromosome -> readBindingRegionsFromTrack(track, chromosome, givenTfClasses))
                .nonNullValues().removeValues(List::isEmpty).toMap();
    }

    public static Map<String, List<BindingRegion>> readBindingRegionsFromTrack(Track track, String givenTfClass) throws Exception
    {
        return readBindingRegionsFromTrack(track, givenTfClass == null ? null : Collections.singletonList(givenTfClass));
    }

    // new version
    public static Map<String, List<BindingRegion>> readBindingRegionsFromTrack(Track track) throws Exception
    {
        return readBindingRegionsFromTrack(track, (List<String>)null);
    }

    ////////////////////////////////O.K.
    public static Map<String, List<BindingRegion>> readBindingRegionsFromTrack(DataElementPath pathToTrack, String givenTfClass) throws Exception
    {
        Track track = pathToTrack.getDataElement(Track.class);
        return readBindingRegionsFromTrack(track, givenTfClass);
    }

    ///////////////////////////////// O.K.
    public static Map<String, List<BindingRegion>> readBindingRegionsFromTrack(DataElementPath pathToTrack) throws Exception
    {
        return readBindingRegionsFromTrack(pathToTrack, null);
    }

    ///////////////////////////////// O.K.
    public static Set<String> getDistinctTfClasses(Map<String, List<BindingRegion>> allBindingRegions)
    {
        return StreamEx.ofValues(allBindingRegions).flatMap(List::stream).map(BindingRegion::getTfClass).toSet();
    }
    
    public static List<Integer> getLengthsOfBindingRegionsOfGivenTfClass(Map<String, List<BindingRegion>> allBindingRegions, String tfClass)
    {
        return StreamEx.ofValues(allBindingRegions).flatMap(List::stream).filter(br -> br.getTfClass().equals(tfClass))
                .map(BindingRegion::getLengthOfBindingRegion).toList();
    }
    
    /////////////////////////O.K.
    public static StreamEx<Sequence> sequencesForBindingRegions(Map<String, List<BindingRegion>> allbindingRegions, DataElementPath pathToSequences, int minimalLengthOfSequenceRegion)
    {
        return EntryStream.of( allbindingRegions ).flatMapKeyValue(
                (chr, brs) -> sequencesForBindingRegions( EnsemblUtils.getSequence( pathToSequences, chr ), brs,
                        minimalLengthOfSequenceRegion ) );
    }
    
    ///////////////////////O.K.
    public static StreamEx<Sequence> sequencesForBindingRegions(Sequence sequence, List<BindingRegion> bindingRegions, int minimalLengthOfSequenceRegion)
    {
        return StreamEx.of(bindingRegions).map(br -> getSequenceForBindingRegion(sequence, br, minimalLengthOfSequenceRegion));
    }

    ///////////////////O.K.
    public static List<Sequence> getLinearSequencesForBindingRegions(Map<String, List<BindingRegion>> bindingRegions, DataElementPath pathToSequences, int minimalLengthOfSequenceRegion)
    {
        return sequencesForBindingRegions( bindingRegions, pathToSequences, minimalLengthOfSequenceRegion )
                .map( seq -> new LinearSequence( seq ) ).map( Sequence.class::cast ).toList();
    }
    
    ////////////////////////////////////////////////// O.K.
    public static Sequence getSequenceForBindingRegion(Sequence sequence, BindingRegion bindingRegion, int minimalLengthOfSequenceRegion)
    {
        Interval interval = bindingRegion.getInterval();
        if(interval.getLength() < minimalLengthOfSequenceRegion)
            interval = interval.zoomToLength(minimalLengthOfSequenceRegion);
        return new SequenceRegion(sequence, interval.fit(sequence.getInterval()), false, false);
    }
    
    ////////////////////////////////////////////////// O.K.
    public static List<BindingRegion> selectBindingRegions(List<BindingRegion> bindingRegions, List<String> givenTfClasses)
    {
        return StreamEx.of(bindingRegions).filter(br -> givenTfClasses.contains(br.getTfClass())).toList();
    }
    
    public static Map<String, List<BindingRegion>> selectBindingRegionsWithoutAlu(Map<String, List<BindingRegion>> allBindingRegions, Track repeatTrack, DataElementPath pathToSequences) throws Exception
    {
        return EntryStream.of(allBindingRegions).mapToValue((chromosome, bindingRegions) -> {
            String sequenceName = pathToSequences.getChildPath(chromosome).toString();
            return StreamEx.of(bindingRegions)
                    .mapToEntry( br -> repeatTrack.getSites(sequenceName, br.getStartPosition(), br.getFinishPosition()) )
                    .filterValues(repeats -> repeats.stream().map(site -> site.getType().toUpperCase(Locale.ENGLISH))
                        .noneMatch(repeatName -> repeatName.indexOf("ALU") >= 0))
                    .keys().toList();
        }).removeValues(List::isEmpty).toMap();
    }
    
    public static Map<String, List<BindingRegion>> selectBindingRegionsWithoutAlu(Map<String, List<BindingRegion>> allBindingRegions, DataElementPath pathToSequences) throws Exception
    {
        DataElementPath dep = pathToSequences.getParentPath().getParentPath();
        DataElementPath pathToRepeatsTrack = dep.getChildPath("Tracks", "RepeatTrack");
        Track repeatTrack = pathToRepeatsTrack.getDataElement(Track.class);
        return selectBindingRegionsWithoutAlu(allBindingRegions, repeatTrack, pathToSequences);
    }
    
    /********************* JSON ***************************/
    
    /***
     * Example of creation of input string:
     * Map<String, String> map = HashMap<String, String>();
     * JsonObject object = JsonUtil.fromMap(map);
     * String string = object.toString();
     * 
     * @param string It was created by ByJSON
     * @return
     * @throws ParseException
     * @throws UnsupportedOperationException
     */
    public static String[] splitStringToArrayByJSON(String string) throws ParseException, UnsupportedOperationException
    {
        JsonObject object = JsonObject.readFrom( string );
        return JsonUtils.objectStream( object ).mapValues( JsonValue::asString ).join( SEPARATOR_BETWEEN_TF_CLASS_AND_ITS_NAME )
                .toArray( String[]::new );
    }
    
    /***
     * Read tfClassesAndTfNames in track
     * @param track
     * @return tfClassesAndTfNames
     */
    public static Map<String, String> getDistinctTfClassesAndNamesFromTrack(Track track) throws ParseException, UnsupportedOperationException
    {
        String tfClassesAndTfNames = ((DataCollection<?>)track).getInfo().getProperty(CisModule.DISTINCT_TFCLASSES_AND_NAMES);
        JsonObject object = JsonObject.readFrom( tfClassesAndTfNames );
        return JsonUtils.objectStream( object ).mapValues( JsonValue::asString ).toMap();
    }
    
    /***
     * 
     * @param tfClassAndTfName
     * @param tfClasses
     * @return array of pairs (tfClass: tfName) with specified separator defined in SEPARATOR_BETWEEN_TF_CLASS_AND_ITS_NAME
     */
    public static String[] getTfClassesAndNames(Map<String, String> tfClassAndTfName, String[] tfClasses)
    {
        return StreamEx.of( tfClasses ).mapToEntry( tfClassAndTfName::get ).nonNullValues()
                .join( SEPARATOR_BETWEEN_TF_CLASS_AND_ITS_NAME ).toArray( String[]::new );
    }
}