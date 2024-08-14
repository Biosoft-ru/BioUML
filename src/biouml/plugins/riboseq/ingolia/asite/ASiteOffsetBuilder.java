package biouml.plugins.riboseq.ingolia.asite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import biouml.plugins.riboseq.ingolia.AlignmentConverter;
import biouml.plugins.riboseq.ingolia.AlignmentOnTranscript;
import biouml.plugins.riboseq.transcripts.Transcript;
import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.bsa.BAMTrack;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.util.Maps;

public class ASiteOffsetBuilder
{
    public static final Map<Integer, Integer> STANDARD_A_SITE_OFFSET_TABLE;

    static
    {
        Map<Integer, Integer> table = new HashMap<>();
        table.put( 29, 15 );
        table.put( 30, 15 );
        table.put( 31, 16 );
        table.put( 32, 16 );
        table.put( 33, 16 );
        table.put( 34, 17 );
        table.put( 35, 17 );
        STANDARD_A_SITE_OFFSET_TABLE = Collections.unmodifiableMap( table );
    }

    private Map<Integer, Integer> aSiteOffsetMap = STANDARD_A_SITE_OFFSET_TABLE;
    private final ASiteOffsetBuilderParameters parameters;

    private Map<Integer, Map<Integer, Integer>> aSiteOffsetCounterMap;

    public ASiteOffsetBuilder(ASiteOffsetBuilderParameters parameters)
    {
        this.parameters = parameters;
    }

    public Map<Integer, Integer> getASiteOffsetTable()
    {
        return aSiteOffsetMap;
    }

    public void computeTableFromBamTracks(List<BAMTrack> bamTrackList, List<Transcript> transcriptList,
                                          AnalysisJobControl jobControl) throws Exception
    {
        final Map<Integer, List<Integer>> pSiteOffsetListMap = getPSiteOffsetListMap(
                transcriptList, bamTrackList, jobControl );
        final Map<Integer, TIntIntMap> pSiteCounterOffsetMap = getCounterOffsetMap( pSiteOffsetListMap );

        aSiteOffsetCounterMap = shiftToASiteFromPSite( pSiteCounterOffsetMap );
        aSiteOffsetMap = chooseRepresentativeOffset();
    }

    private Map<Integer, List<Integer>> getPSiteOffsetListMap(final List<Transcript> transcriptList, List<BAMTrack>
            bamTrackList, final AnalysisJobControl jobControl) throws Exception
    {
        final Map<Integer, List<Integer>> pSiteOffsetCountMap = new HashMap<>();
        jobControl.forCollection( bamTrackList, bamTrack -> {
            jobControl.forCollection( transcriptList, transcript -> {
                try
                {
                    final Map<Integer, List<Integer>> listMap = getTranscriptLengthSiteListMap(
                            transcript, bamTrack );
                    collectPSiteOffsetMap( listMap, pSiteOffsetCountMap );
                } catch( Exception e )
                {
                    throw ExceptionRegistry.translateException( e );
                }

                return true;
            } );

            return true;
        } );

        return pSiteOffsetCountMap;
    }

    private Map<Integer, List<Integer>> getTranscriptLengthSiteListMap(Transcript transcript, BAMTrack bamTrack) throws Exception
    {
        if( !transcript.isCoding() )
        {
            return Collections.emptyMap();
        }

        final AlignmentConverter alignmentConverter = new AlignmentConverter();
        alignmentConverter.setTranscriptOverhangs( parameters.getTranscriptOverhangs() );
        final List<AlignmentOnTranscript> transcriptAlignmentList = alignmentConverter.getTranscriptAlignments( transcript, bamTrack );

        final List<Integer> startInitializationList = transcript.getStartInitializations();

        Map<Integer, List<Integer>> result = new HashMap<>();
        for( Integer startInitialization : startInitializationList )
        {
            for( AlignmentOnTranscript transcriptAlignment : transcriptAlignmentList )
            {
                if( transcriptAlignment.isPositiveStrand() && transcriptAlignment.inside( startInitialization ) )
                {
                    if( parameters.isStrandSpecific() && !transcriptAlignment.isPositiveStrand() )
                    {
                        continue;
                    }

                    final int siteLength = transcriptAlignment.getLength();
                    final int transcriptAlignmentStart = transcriptAlignment.getFrom();
                    final int pSiteOffset = Math.abs( startInitialization - transcriptAlignmentStart );

                    result.computeIfAbsent( siteLength, k -> new ArrayList<>() ).add( pSiteOffset );
                }
            }
        }

        return result;
    }

    private void collectPSiteOffsetMap(Map<Integer, List<Integer>> pSiteOffsetMap, Map<Integer, List<Integer>> pSiteOffsetSummaryMap)
    {
        for( Map.Entry<Integer, List<Integer>> pSiteOffsetEntry : pSiteOffsetMap.entrySet() )
        {
            final Integer length = pSiteOffsetEntry.getKey();
            final List<Integer> offset = pSiteOffsetEntry.getValue();
            pSiteOffsetSummaryMap.computeIfAbsent( length, k -> new ArrayList<>() ).addAll( offset );
        }
    }

    private Map<Integer, TIntIntMap> getCounterOffsetMap(Map<Integer, List<Integer>> pSiteOffsetCountMap)
    {
        return Maps.transformValues( pSiteOffsetCountMap, offsetList -> {
            TIntIntMap offsetCounterMap = new TIntIntHashMap();
            for( Integer offset : offsetList )
            {
                offsetCounterMap.adjustOrPutValue( offset, 1, 1 );
            }
            return offsetCounterMap;
        });
    }

    // choose moda offset
    private Map<Integer, Integer> chooseRepresentativeOffset()
    {
        final Map<Integer, Integer> aSiteOffsetMap = new HashMap<>();
        for( Map.Entry<Integer, Map<Integer, Integer>> offsetEntry : aSiteOffsetCounterMap.entrySet() )
        {
            final Integer length = offsetEntry.getKey();
            final Map<Integer, Integer> offsetCounterMap = offsetEntry.getValue();

            Map.Entry<Integer, Integer> maxEntry = offsetCounterMap.entrySet().iterator().next();
            for( Map.Entry<Integer, Integer> entry : offsetCounterMap.entrySet() )
            {
                final Integer entryVal = entry.getValue();
                final Integer maxEntryVal = maxEntry.getValue();
                if( entryVal > maxEntryVal || (entryVal.equals( maxEntryVal ) && entry.getKey() < maxEntry.getKey()) )
                {
                    maxEntry = entry;
                }
            }

            aSiteOffsetMap.put( length, maxEntry.getKey() );
        }

        return aSiteOffsetMap;
    }

    private Map<Integer, Map<Integer, Integer>> shiftToASiteFromPSite(Map<Integer, TIntIntMap> pSiteOffsetCounterMap)
    {
        final Map<Integer, Map<Integer, Integer>> aSiteOffsetCounterMap = new HashMap<>();
        for( Map.Entry<Integer, TIntIntMap> entry : pSiteOffsetCounterMap.entrySet() )
        {
            final Map<Integer, Integer> offsetCounterMap = new HashMap<>();
            final TIntIntMap tpOffsetCounterMap = entry.getValue();
            final TIntIntIterator iterator = tpOffsetCounterMap.iterator();

            for( int j = tpOffsetCounterMap.size(); j-- > 0; )
            {
                iterator.advance();
                offsetCounterMap.put( iterator.key() + 3, iterator.value() );
            }

            aSiteOffsetCounterMap.put( entry.getKey(), offsetCounterMap );
        }
        return aSiteOffsetCounterMap;
    }

    public Map<Integer, Map<Integer, Integer>> getASiteOffsetCounterMap()
    {
        return aSiteOffsetCounterMap;
    }
}
