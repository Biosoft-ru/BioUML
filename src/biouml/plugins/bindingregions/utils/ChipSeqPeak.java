
package biouml.plugins.bindingregions.utils;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import com.developmentontheedge.beans.DynamicPropertySet;

/**
 * @author yura
 *
 */
public class ChipSeqPeak
{
    public static final String FDR = "Peak FDR";
    public static final String FOLD_ENRICHMENT = "Fold enrichment";
    public static final String SCORE = "Peak score";
    public static final String SUMMIT = "Summit";
    public static final String TAGS = "Tag number";
    public static final String LENGTH = "Peak length";
    public static final String DISTANCE_BETWEEN_SUMMIT_AND_SITE = "Distance between summit and site";
    public static final String P_VALUE = "Peak p-value";
    public static final String NUMBER_OF_OVERLAPS = "Number of overlaps";

    public static final String FDR_PROPERTY = "fdr";
    public static final String FOLD_ENRICHMENT_PROPERTY = "fold_enrichemnt";
    public static final String FOLD_ENRICHMENT_PROPERTY_2 = "fold_enrichment";
    public static final String FOLD_ENRICHMENT_PROPERTY_3 = "fold";
    public static final String SUMMIT_PROPERTY = "summit";
    public static final String TAGS_PROPERTY = "tags";
    public static final String P_VALUE_PROPERTY = "p-value";
    
    public static final String PEAK_FINDER_MACS = "MACS";
    public static final String PEAK_FINDER_SISSRS = "SISSRs";

    String chromosome;
    Interval interval;
    float fdr = -1;
    float fold_enrichment = -1;
    float score = -1;
    float pValue = -1;
    int summit = -1;
    int tags = -1;
    int numberOfOverlaps = 0;
    
    public ChipSeqPeak(String chromosome, Interval interval, float fdr, float fold_enrichment, float score, float pValue, int summit, int tags, int numberOfOverlaps)
    {
        this.chromosome = chromosome;
        this.interval = interval;
        this.fdr = fdr;
        this.fold_enrichment = fold_enrichment;
        this.score = score;
        this.pValue = pValue;
        this.summit = summit;
        this.tags = tags;
        this.numberOfOverlaps = numberOfOverlaps;
    }
    
    public String getChromosome()
    {
        return chromosome;
    }
    
    public Interval getInterval()
    {
        return interval;
    }
    
    public int getStartPosition()
    {
        return getInterval().getFrom();
    }
    
    public int getFinishPosition()
    {
        return getInterval().getTo();
    }
    
    public int getLengthOfPeak()
    {
        return getInterval().getLength();
    }
    
    public float getFdr()
    {
        return fdr;
    }
    public float getFoldEnrichment()
    {
        return fold_enrichment;
    }
    
    public float getScore()
    {
        return score;
    }
    
    public float getPvalue()
    {
        return pValue;
    }

    public int getSummit()
    {
        return summit;
    }
    
    public int getTags()
    {
        return tags;
    }
    
    public int getNumberOfOverlaps()
    {
        return numberOfOverlaps;
    }
    
    public void increaseNumberOfOverlaps(List<Track> tracks)
    {
        numberOfOverlaps += countOverlaps(tracks);
    }
    
    /**
     * Returns the number of tracks from input list which have at least one site on given peak
     */
    public int countOverlaps(List<Track> tracks)
    {
        return (int)StreamEx.of( tracks )
                .map( t -> t.getSites( getChromosome(), getStartPosition(), getFinishPosition() ) )
                .filter( sites -> sites.getSize() > 0 ).count();
    }

    public static Map<String, List<ChipSeqPeak>> readChromosomeAndPeaks(Track track)
    {
        Map<String, List<ChipSeqPeak>> result = new HashMap<>();
        for( Site site : track.getAllSites() )
        {
            String chromosome = site.getSequence().getName();
            if( chromosome.equals("MT") || chromosome.equals("M") || chromosome.equals("EBV") || chromosome.contains("_") || site.getInterval().getLength() <= 0 || site.getStart() < 1 ) continue;
            DynamicPropertySet properties = site.getProperties();

//          Number x = (Number)properties.getValue(FDR_PROPERTY);
//          float fdr = x != null ? x.floatValue() : -1.0f;
//          Number x = Double.valueOf(properties.getValueAsString(FDR_PROPERTY));
            
            String tempStr = properties.getValueAsString( FDR_PROPERTY );
            float fdr = tempStr != null ? Float.parseFloat( tempStr ) : -1.0f;

            tempStr = properties.getValueAsString( FOLD_ENRICHMENT_PROPERTY );
            float fold_enrichment = tempStr != null ? Float.parseFloat( tempStr ) : -1.0f;
            if( fold_enrichment < 0.0 )
            {
                tempStr = properties.getValueAsString( FOLD_ENRICHMENT_PROPERTY_2 );
                fold_enrichment = tempStr != null ? Float.parseFloat( tempStr ) : -1.0f;
            }
            if( fold_enrichment < 0.0 )
            {
                tempStr = properties.getValueAsString( FOLD_ENRICHMENT_PROPERTY_3 );
                fold_enrichment = tempStr != null ? Float.parseFloat( tempStr ) : -1.0f;
            }

            tempStr = properties.getValueAsString( Site.SCORE_PROPERTY );
            float score = tempStr != null ? Float.parseFloat( tempStr ) : -1.0f;

            tempStr = properties.getValueAsString( SUMMIT_PROPERTY );
            int summit = tempStr != null ? Integer.parseInt( tempStr ) : -1;

            tempStr = properties.getValueAsString( TAGS_PROPERTY );
            int tags = tempStr != null ? Integer.parseInt( tempStr ) : -1;
            tempStr = properties.getValueAsString( P_VALUE_PROPERTY );
            float pValue = tempStr != null ? Float.parseFloat( tempStr ) : -1.0f;

            result.computeIfAbsent( chromosome, key -> new ArrayList<>() ).add(
                    new ChipSeqPeak( chromosome, site.getInterval(), fdr, fold_enrichment, score, pValue, summit, tags, 0 ) );
        }
        return result;
    }

    public static StreamEx<String> characteristics()
    {
        return StreamEx.of(FDR, FOLD_ENRICHMENT, SCORE, P_VALUE, SUMMIT, TAGS);
    }
    
    private double getValueOfGivenCharacteristic(String characteristicName)
    {
        switch( characteristicName )
        {
            case FDR                : return fdr;
            case FOLD_ENRICHMENT    : return fold_enrichment;
            case SCORE              : return score;
            case P_VALUE            : return pValue;
            case TAGS               : return tags;
            case SUMMIT             : return summit;
            case LENGTH             : return getLengthOfPeak();
            case NUMBER_OF_OVERLAPS : return numberOfOverlaps;
            default                 : return -1.0;
        }
    }

    public static double[] getValuesOfGivenCharacteristic(Map<String, List<ChipSeqPeak>> chromosomeAndPeaks, String characteristicName)
    {
        TDoubleList list = new TDoubleArrayList();
        for( List<ChipSeqPeak> peaks: chromosomeAndPeaks.values() )
            for( ChipSeqPeak peak : peaks )
            {
                double x = peak.getValueOfGivenCharacteristic(characteristicName);
                if( x < 0.0 ) return null;
                list.add(x);
            }
        if( list.isEmpty() ) return null;
        return list.toArray();
    }
    
    public static double[][] getValuesOfGivenCharacteristics(Map<String, List<ChipSeqPeak>> chromosomeAndPeaks, String[] characteristicNames)
    {
        double[][] matrix = StreamEx.of( characteristicNames ).map( chName -> getValuesOfGivenCharacteristic( chromosomeAndPeaks, chName ) )
                .toArray( double[][]::new );
        return MatrixUtils.getTransposedMatrix(matrix);
    }

    /***
     * Here a single set of peaks is considered
     * @param chromosomeAndPeaks
     * @return array with positive characteristic names
     */
    public static String[] getPositiveCharacteristicNames(Map<String, List<ChipSeqPeak>> chromosomeAndPeaks)
    {
        return characteristics().remove( negativeCharacteristicNames( chromosomeAndPeaks ).toSet()::contains ).toArray( String[]::new );
    }
    
    public static StreamEx<String> negativeCharacteristicNames(Map<String, List<ChipSeqPeak>> chromosomeAndPeaks)
    {
        return StreamEx.ofValues( chromosomeAndPeaks ).flatMap( List::stream )
                .flatMap( peak -> characteristics().filter( ch -> peak.getValueOfGivenCharacteristic( ch ) < 0 ) );
    }
    
    private Sequence getSequenceRegionForChipSeqPeak(Sequence fullChromosome, int minimalLengthOfSequenceRegion)
    {
        Interval interval = getInterval();
        if( interval.getLength() < minimalLengthOfSequenceRegion )
            interval = interval.zoomToLength(minimalLengthOfSequenceRegion).fit(fullChromosome.getInterval());
        return new SequenceRegion(fullChromosome, interval, false, false);
        // import ru.biosoft.bsa.analysis.SequenceAccessor.CachedSequenceRegion;
        // return new CachedSequenceRegion(fullChromosome, interval.getFrom(), interval.getLength(), false);
    }
    
    public static List<Sequence> getSequenceRegionsForChipSeqPeaks(Map<String, List<ChipSeqPeak>> chromosomeAndPeaks, DataElementPath pathToSequences, int minimalLengthOfSequenceRegion)
    {
        return EntryStream.of(chromosomeAndPeaks)
            .mapKeys( chr -> pathToSequences.getChildPath(chr).getDataElement(AnnotatedSequence.class).getSequence() )
            .flatMapValues( List::stream )
            .mapKeyValue( (sequence, peak) -> peak.getSequenceRegionForChipSeqPeak(sequence, minimalLengthOfSequenceRegion))
            .toList();
    }

    // main 2
    public static Sequence[] getLinearSequencesForChipSeqPeaks(Map<String, List<ChipSeqPeak>> chromosomeAndPeaks, DataElementPath pathToSequences, int minimalLengthOfSequenceRegion, AnalysisJobControl jobControl, int from, int to)
    {
        List<Sequence> result = new ArrayList<>();
        int index = 0, difference = to - from;
        int n = chromosomeAndPeaks.size();
        for( Entry<String, List<ChipSeqPeak>> entry : chromosomeAndPeaks.entrySet() )
        {
            Sequence sequence = pathToSequences.getChildPath(entry.getKey()).getDataElement(AnnotatedSequence.class).getSequence();
            List<ChipSeqPeak> peaks = entry.getValue();
            for( ChipSeqPeak peak : peaks )
                result.add(new LinearSequence(peak.getSequenceRegionForChipSeqPeak(sequence, minimalLengthOfSequenceRegion)));
            if( jobControl != null )
                jobControl.setPreparedness(from + ++index * difference / n);
        }
        return result.toArray(new Sequence[result.size()]);
    }
    
    // main 1
    /***
     * 
     * @param pathToChipSeqPeaksTrack
     * @param pathToSequences
     * @param isAroundSummit
     * @param minimalLengthOfSequenceRegion
     * @param jobControl
     * @param from
     * @param to
     * @return array Object[] : Object[0] = Sequence[] - LinearSequences;
     *               Object[1] = boolean 'isActuallyAroundSummit';
     */
    public static Object[] getLinearSequencesForChipSeqPeaks(DataElementPath pathToChipSeqPeaksTrack, DataElementPath pathToSequences, boolean isAroundSummit, int minimalLengthOfSequenceRegion, AnalysisJobControl jobControl, int from, int to)
    {
        Track track = pathToChipSeqPeaksTrack.getDataElement(Track.class);
        Map<String, List<ChipSeqPeak>> chromosomeAndPeaks = readChromosomeAndPeaks(track);
        boolean isActuallyAroundSummit = isAroundSummit && isSummitExist(chromosomeAndPeaks);
        if( isActuallyAroundSummit )
            chromosomeAndPeaks = getPeaksWithSummitsInCenter(chromosomeAndPeaks, minimalLengthOfSequenceRegion, EnsemblUtils.getChromosomeIntervals(pathToSequences));
        Sequence[] sequences = getLinearSequencesForChipSeqPeaks(chromosomeAndPeaks, pathToSequences, minimalLengthOfSequenceRegion, jobControl, from, to);
        return new Object[]{sequences, isActuallyAroundSummit};
    }

    private int getSummitCorrectedOnRegionLengthExtention(int minimalLengthOfSequenceRegion)
    {
        int length = getLengthOfPeak();
        if( length < minimalLengthOfSequenceRegion )
            return minimalLengthOfSequenceRegion / 2 - length / 2 + summit;
        return summit;
    }
    
    public static int[] getSummitsCorrectedOnRegionLengthExtention(Map<String, List<ChipSeqPeak>> chromosomeAndPeaks, int minimalLengthOfSequenceRegion)
    {
        return StreamEx.ofValues( chromosomeAndPeaks ).flatMap( List::stream )
                .mapToInt( peak -> peak.getSummitCorrectedOnRegionLengthExtention( minimalLengthOfSequenceRegion ) ).toArray();
    }

    public static boolean isSummitExist(String[] characteristicNames)
    {
        return characteristicNames != null && ArrayUtils.contains(characteristicNames, SUMMIT);
    }
    
    public static boolean isSummitExist(Map<String, List<ChipSeqPeak>> chromosomeAndPeaks)
    {
        return !negativeCharacteristicNames( chromosomeAndPeaks ).has( SUMMIT );
    }
    
    private static ChipSeqPeak[] sortPeaksByCharacteristic(final String characteristicName, Map<String, List<ChipSeqPeak>> chromosomeAndPeaks)
    {
        if( chromosomeAndPeaks == null ) return new ChipSeqPeak[0];
        return StreamEx.ofValues( chromosomeAndPeaks )
            .flatMap( List::stream )
            .filter( peak -> peak.getValueOfGivenCharacteristic(characteristicName) >= 0 )
            .sortedByDouble( peak -> peak.getValueOfGivenCharacteristic( characteristicName ) )
            .toArray( ChipSeqPeak[]::new );
    }
    
    public static Map<String, List<ChipSeqPeak>> getPeaksWithSummitsInCenter(Map<String, List<ChipSeqPeak>> chromosomeAndPeaks, int newPeakLength, Map<String, Interval> nameAndLengthOfChromosomes)
    {
        if( chromosomeAndPeaks == null) return null;
        Map<String, List<ChipSeqPeak>> result = new HashMap<>();
        for( Map.Entry<String, List<ChipSeqPeak>> entry : chromosomeAndPeaks.entrySet() )
        {
            String chromosome = entry.getKey();
            Interval chromosomeInterval = nameAndLengthOfChromosomes.get(chromosome);
            List<ChipSeqPeak> peaks = entry.getValue(), newPeaks = new ArrayList<>();
            for( ChipSeqPeak peak : peaks )
            {
                int summit = peak.getSummit();
                if( summit < 0 ) continue;
                Interval interval = new Interval(peak.getStartPosition() + summit).zoomToLength(newPeakLength).fit(chromosomeInterval);
                newPeaks.add(new ChipSeqPeak(chromosome, interval, peak.getFdr(), peak.getFoldEnrichment(), peak.getScore(), peak.getPvalue(), newPeakLength / 2, peak.getTags(), 0));
            }
            if( ! newPeaks.isEmpty() )
                result.put(chromosome, newPeaks);
            else result = null;
        }
        return result;
    }

    private static Map<String, List<ChipSeqPeak>> getMapFromArray(ChipSeqPeak[] peaks, int indexFrom, int indexTo)
    {
        return StreamEx.of(peaks, indexFrom, indexTo).groupingBy(ChipSeqPeak::getChromosome);
    }

    public static Map<String, Map<String, List<ChipSeqPeak>>> getGroupedPeaks(int numberOfGroups, String nameOfCharacteristicForGrouping, Map<String, List<ChipSeqPeak>> chromosomeAndPeaks)
    {
        Map<String, Map<String, List<ChipSeqPeak>>> result = new HashMap<>();
        ChipSeqPeak[] peaksSorted = sortPeaksByCharacteristic(nameOfCharacteristicForGrouping, chromosomeAndPeaks);
        if( peaksSorted.length < numberOfGroups ) return null;
        int size = peaksSorted.length / numberOfGroups;
        for( int i = 0; i < numberOfGroups; i++ )
        {
            int indexTo = peaksSorted.length;
            if( i != numberOfGroups - 1 )
                indexTo = (i + 1) * size;
            result.put("group_" + i, getMapFromArray(peaksSorted, i * size, indexTo));
        }
        return result;
    }

    public static Map<String, double[]> getNamesAndPredictionsWithChipSeqCharacteristics(Map<String, List<ChipSeqPeak>> chromosomeAndPeaks, DataElementPath pathToSequences, SiteModelsComparison siteModelsComparison, int minimalLengthOfSequenceRegion, boolean areBothStrands, AnalysisJobControl jobControl, int from, int to)
    {
        Map<String, double[]> result = new TreeMap<>();
        String[] characteristicNames = getPositiveCharacteristicNames(chromosomeAndPeaks);
        boolean isSummitExist = isSummitExist(characteristicNames);
        characteristicNames = (String[])ArrayUtils.removeElement(characteristicNames, SUMMIT);
        characteristicNames = (String[])ArrayUtils.add(characteristicNames, characteristicNames.length, LENGTH);
        for( String name : characteristicNames )
            result.put(name, getValuesOfGivenCharacteristic(chromosomeAndPeaks, name));
        int middle = (from + to) / 2, difference = to - middle;
        Sequence[] sequences = getLinearSequencesForChipSeqPeaks(chromosomeAndPeaks, pathToSequences, minimalLengthOfSequenceRegion, jobControl, from, middle);
        int[] summits = isSummitExist ? getSummitsCorrectedOnRegionLengthExtention(chromosomeAndPeaks, minimalLengthOfSequenceRegion) : null;
        String[] modelNames = siteModelsComparison.getModelsNames();
        for( String name : modelNames )
            result.put(name, new double[sequences.length]);
        String[] distanceNames = null;
        if( isSummitExist )
        {
            distanceNames = new String[modelNames.length];
            for( int j = 0; j < modelNames.length; j++ )
            {
                distanceNames[j] = DISTANCE_BETWEEN_SUMMIT_AND_SITE + "(" + modelNames[j] + ")";
                result.put(distanceNames[j], new double[sequences.length]);
            }
        }
        for( int i = 0; i < sequences.length; i++ )
        {
            Site[] bestSites = siteModelsComparison.findBestSite(sequences[i], areBothStrands);
            for( int j = 0; j < modelNames.length; j++)
            {
                double[] array = result.get(modelNames[j]);
                array[i] = bestSites[j].getScore();
                if( isSummitExist )
                {
                    array = result.get(distanceNames[j]);
                    array[i] = Math.abs(bestSites[j].getInterval().getCenter() - summits[i]);
                }
            }
            jobControl.setPreparedness(middle + (i + 1) * difference / sequences.length);
        }
        return result;
    }
    
    /***
     * For every peak from 'chromosomeAndPeaks' the number of its overlaps with peaks from tracks is calculated
     * @param chromosomeAndPeaks
     * @param tracks
     * @throws Exception
     */
    public static void calculateNumbersOfOverlaps(Map<String, List<ChipSeqPeak>> chromosomeAndPeaks, List<Track> tracks) throws Exception
    {
        StreamEx.ofValues(chromosomeAndPeaks).flatMap( List::stream ).forEach( peak -> peak.increaseNumberOfOverlaps( tracks ) );
    }
    
    /***
     * 
     * @param pathToChipSeqTrack
     * @param chromosomeAndPeaks
     * @param characteristicNames
     * @param dataMatrix
     * @return  array[]: array[0] = new version of chromosomeAndPeaks; array[1] = new version of dataMatrix
     * @throws Exception
     */
    public static Object[] insertNumbersOfOverlaps(DataElementPath pathToChipSeqTrack, Map<String, List<ChipSeqPeak>> chromosomeAndPeaks, String[] characteristicNames, double[][] dataMatrix) throws Exception
    {
        List<Track> tracks = TrackInfo.getTracksWithSameTfClass(pathToChipSeqTrack);
        if( tracks == null ) return new Object[]{characteristicNames, dataMatrix};
        characteristicNames = (String[])ArrayUtils.add(characteristicNames, characteristicNames.length, NUMBER_OF_OVERLAPS);
        calculateNumbersOfOverlaps(chromosomeAndPeaks, tracks);
        double[] values = getValuesOfGivenCharacteristic(chromosomeAndPeaks, NUMBER_OF_OVERLAPS);
        MatrixUtils.addColumnToMatrix(dataMatrix, values);
        return new Object[]{characteristicNames, dataMatrix};
    }

}