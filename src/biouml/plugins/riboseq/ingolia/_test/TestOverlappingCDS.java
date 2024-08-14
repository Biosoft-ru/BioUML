package biouml.plugins.riboseq.ingolia._test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.IntervalMap;
import biouml.plugins.riboseq.transcripts.EnsemblTranscriptsProvider;
import biouml.plugins.riboseq.transcripts.Transcript;
import biouml.plugins.server.SqlModule;

public class TestOverlappingCDS extends AbstractRiboSeqTest
{

    public void testSingleCDSPerTranscript()
    {
        testSingleCDSPerTranscript( getTranscripts( "databases/EnsemblMouse37" ) );
        testSingleCDSPerTranscript( getTranscripts( "databases/EnsemblMouse38" ) );
        testSingleCDSPerTranscript( getTranscripts( "databases/EnsemblHuman64_37" ) );
        testSingleCDSPerTranscript( getTranscripts( "databases/EnsemblHuman73_37" ) );
    }

    private void testSingleCDSPerTranscript(List<Transcript> transcripts)
    {
        for( Transcript t : transcripts )
            if( t.isCoding() )
                assertEquals( 1, t.getCDSLocations().size() );
    }

    public void testCountOverlappingStartSites()
    {
        testCountOverlappingStartSites( getTranscripts( "databases/EnsemblMouse37" ) );
        testCountOverlappingStartSites( getTranscripts( "databases/EnsemblHuman64_37" ) );
        testCountOverlappingStartSites( getTranscripts( "databases/EnsemblMouse38" ) );
        testCountOverlappingStartSites( getTranscripts( "databases/EnsemblHuman73_37" ) );
    }

    public void testCountOverlappingStartSites(List<Transcript> transcripts)
    {
        List<List<Interval>> transcriptWindows = new ArrayList<>(transcripts.size());
        int usedTranscripts = 0;
        for(Transcript t : transcripts)
        {
            if(!t.isCoding())
                continue;
            List<Interval> windows = new ArrayList<>();
            transcriptWindows.add( windows );
            boolean isUsed = false;
            for(Integer cdsStart : t.getStartInitializations())
            {
                //int windowStart = cdsStart - 12;
                //int windowEnd = cdsStart + 220;
                int windowStart = cdsStart - 7;
                int windowEnd = cdsStart + 40;
                if(windowStart < 0 || windowEnd >= t.getLength())
                    continue;
                isUsed = true;
                List<Interval> exons = t.getExonLocations();
                if(t.isOnPositiveStrand())
                {
                    int startExon = 0;
                    while(windowStart >= exons.get( startExon ).getLength())
                    {
                        windowStart -= exons.get( startExon ).getLength();
                        startExon++;
                    }
                    int endExon = 0;
                    while(windowEnd >= exons.get( endExon ).getLength())
                    {
                        windowEnd -= exons.get( endExon ).getLength();
                        endExon++;
                    }
                    for(int i = startExon; i <= endExon; i++)
                    {
                        Interval exon = exons.get( i );
                        int from = exon.getFrom();
                        int to = exon.getTo();
                        if(i == startExon)
                            from += windowStart;
                        if(i == endExon)
                            to = exon.getFrom() + windowEnd;
                        windows.add( new Interval( from, to ) );
                    }
                }else
                {
                    int startExon = exons.size() - 1;
                    while(windowStart >= exons.get( startExon ).getLength())
                    {
                        windowStart -= exons.get( startExon ).getLength();
                        startExon--;
                    }
                    int endExon = exons.size() - 1;
                    while(windowEnd >= exons.get( endExon ).getLength())
                    {
                        windowEnd -= exons.get( endExon ).getLength();
                        endExon--;
                    }
                    for(int i = endExon; i <= startExon; i++)
                    {
                        Interval exon = exons.get( i );
                        int from = exon.getFrom();
                        int to = exon.getTo();
                        if(i == endExon)
                            from = exon.getTo() - windowEnd;
                        if(i == startExon)
                            to = exon.getTo() - windowStart;
                        windows.add( new Interval( from, to ) );
                    }
                }
                
            }
            if(isUsed)
                usedTranscripts++;
        }
        
        boolean[] overlaps = new boolean[transcripts.size()];
        Map<String, IntervalMap<Integer>> indexByChr = new HashMap<>();
        for(int i = 0; i < transcripts.size(); i++)
        {
            Transcript transcript = transcripts.get( i );
            IntervalMap<Integer> index = indexByChr.get( transcript.getChromosome() );
            if(index == null)
                indexByChr.put( transcript.getChromosome(), index = new IntervalMap<>() );
            
            for(Interval window : transcriptWindows.get( i ))
            {
                Collection<Integer> overlapping = index.getIntervals( window.getFrom(), window.getTo() );
                if(!overlapping.isEmpty())
                {
                    overlaps[i] = true;
                    for(Integer j : overlapping)
                        overlaps[j] = true;
                }
            }
            
            for(Interval window : transcriptWindows.get( i ))
                index.add( window.getFrom(), window.getTo(), i );
        }
        
        System.out.println("Total transcripts: " + transcripts.size());
        System.out.println("Used transcripts: " + usedTranscripts);
        int overlapCount = 0;
        for(boolean b : overlaps) if(b) overlapCount++;
        System.out.println("Overlap count: " + overlapCount);
   
    }

    private List<Transcript> getTranscripts(String ensemblPathStr)
    {
        DataElementPath ensemblPath = DataElementPath.create( ensemblPathStr );
        EnsemblTranscriptsProvider provider = new EnsemblTranscriptsProvider( ensemblPath.getDataElement( SqlModule.class ) );
        return provider.getTranscripts();
    }
}
