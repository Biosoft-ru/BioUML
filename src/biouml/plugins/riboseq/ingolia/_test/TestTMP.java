package biouml.plugins.riboseq.ingolia._test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import biouml.plugins.riboseq.transcripts.EnsemblTranscriptsProvider;
import biouml.plugins.riboseq.transcripts.Transcript;
import biouml.plugins.server.SqlModule;
import junit.framework.TestCase;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Interval;

public class TestTMP extends TestCase
{
    /*public void testCDSCoords() throws Exception
    {
        CollectionFactory.createRepository( "../data" );
        SqlModule ensembl = DataElementPath.create( "databases/EnsemblMouse81_38" ).getDataElement( SqlModule.class );
        EnsemblTranscriptsProvider provider = new EnsemblTranscriptsProvider( ensembl );
        List<Transcript> transcripts = provider.getTranscripts();

        try (BufferedWriter writer = new BufferedWriter( new FileWriter( "cds.txt" ) ))
        {
            for( Transcript t : transcripts )
            {
                List<Interval> cdsSet = t.getCDSLocations();
                if(cdsSet.size() == 0)
                    continue;
                if(cdsSet.size() > 1)
                    throw new Exception();
                Interval cds = cdsSet.get( 0 );
                writer.write( t.getName() + "\t" + cds.getFrom() + "\t" + cds.getTo() );
            }
        }

    }*/
    
    public void testTranscriptClusters() throws Exception
    {
        CollectionFactory.createRepository( "../data" );
        SqlModule ensembl = DataElementPath.create( "databases/EnsemblMouse81_38" ).getDataElement( SqlModule.class );
        EnsemblTranscriptsProvider provider = new EnsemblTranscriptsProvider( ensembl );
        List<Transcript> transcripts = provider.getTranscripts();
        System.out.println( "Load " + transcripts.size() + " transcripts" );

        Map<String, List<IntervalData<Transcript>>> m = new HashMap<>();

        for(Transcript t : transcripts)
        {
            String chr = t.getChromosome() + (t.isOnPositiveStrand() ? "+" : "-");
            List<IntervalData<Transcript>> onChr = m.get( chr );
            if(onChr == null)
                m.put( chr, onChr = new ArrayList<>() );
            for(Interval exon : t.getExonLocations())
                onChr.add( new IntervalData<>( exon, t ) );
        }
        
        List<Set<Transcript>> exonClusters = new ArrayList<>();
        
        for(List<IntervalData<Transcript>> onChr : m.values())
        {
            Collections.sort( onChr );
            IntervalData<Transcript> cur = onChr.get( 0 );
            IntervalData<Set<Transcript>> cluster = mkCluster( cur );
            for(int i = 1; i < onChr.size(); i++)
            {
                cur = onChr.get( i );
                if(cur.getFrom() > cluster.getTo())
                {
                    exonClusters.add( cluster.getData() );
                    cluster = mkCluster( cur );
                }
                else
                {
                    cluster.getData().add( cur.getData() );
                    cluster = addToCluster( cluster, cur );
                }
            }
            exonClusters.add( cluster.getData() );
        }
        System.out.println( "Found " + exonClusters.size() + " exon clusters" );
        
        Map<Transcript, Set<Transcript>> t2s = new HashMap<>();
        for( Set<Transcript> s : exonClusters )
        {
            Set<Transcript> merged = new HashSet<>(s);
            for( Transcript t : s )
            {
                Set<Transcript> otherS = t2s.get( t );
                if(otherS != null)
                    merged.addAll( otherS );
            }
            for(Transcript t : merged)
                t2s.put( t, merged );
        }

        Set<Set<Transcript>> uClusters = Collections.newSetFromMap( new IdentityHashMap<>() );
        uClusters.addAll( t2s.values() );

        List<Set<Transcript>> clusters = new ArrayList<>(uClusters);
        
        System.out.println( "Found " + clusters.size() + " clusters" );
        
        
        int clusterId = 1;
        try (BufferedWriter writer = new BufferedWriter( new FileWriter( "clusters.txt" ) ))
        {
            for( Set<Transcript> cluster : clusters )
            {
                List<IntervalData<Transcript>> exons = new ArrayList<>();
                for( Transcript t : cluster )
                    for( Interval exon : t.getExonLocations() )
                        exons.add( new IntervalData<>( exon, t ) );
                Collections.sort( exons );
                int to = 0;
                int offsetTo = 0;
                Map<String, Integer> transcriptOffset = new HashMap<>();
                List<Row> rows = new ArrayList<>();
                boolean onPositiveStrand = true;
                for( IntervalData<Transcript> exon : exons )
                {
                    onPositiveStrand = exon.getData().isOnPositiveStrand();
                    String transcriptName = exon.getData().getName();
                    if( !transcriptOffset.containsKey( transcriptName ) )
                        transcriptOffset.put( transcriptName, 0 );
                    int tOffset = transcriptOffset.get( transcriptName );

                    int offset;

                    if( exon.getFrom() <= to )
                        offset = offsetTo - ( to - exon.getFrom() );
                    else
                        offset = offsetTo + 1;

                    if( exon.getTo() > to )
                    {
                        to = exon.getTo();
                        offsetTo = offset + exon.getLength() - 1;
                    }

                    Row row = new Row( clusterId, exon.getData() );
                    row.from = tOffset;
                    row.to = row.from + exon.getLength() - 1;
                    row.offset = offset;
                    rows.add( row );
                    transcriptOffset.put( transcriptName, tOffset + exon.getLength() );
                }
                if( !onPositiveStrand )
                    for( Row r : rows )
                        r.translateToReverse( offsetTo );
                for( Row r : rows )
                    writer.append( r.toString() ).append( '\n' );
                clusterId++;
            }

        }
    }
    
    private static IntervalData<Set<Transcript>> mkCluster(IntervalData<Transcript> a)
    {
        return new IntervalData<>( a, new HashSet<>( Collections.singleton( a.getData() ) )  );
    }
    private static IntervalData<Set<Transcript>> addToCluster(IntervalData<Set<Transcript>> cluster, IntervalData<Transcript> a)
    {
        return new IntervalData<>( cluster.getFrom(), Math.max( cluster.getTo(), a.getTo() ), cluster.getData() );
    }
    
    public static class IntervalData<T> extends Interval
    {
        private T data;

        public IntervalData(int from, int to, T data)
        {
            super( from, to );
            this.data = data;
        }
        
        public IntervalData(Interval interval, T data)
        {
            this( interval.getFrom(), interval.getTo(), data );
        }

        public T getData() { return data; }
    }
    
    public static class Row
    {
        public final int clusterId;
        public final Transcript transcript;
        public int from, to, offset;
        
        public Row(int clusterId, Transcript transcript)
        {
            this.clusterId = clusterId;
            this.transcript = transcript;
        }
        
        public void translateToReverse(int maxOffset)
        {
            int newTo = transcript.getLength() - from - 1;
            int newFrom = transcript.getLength() - to - 1;
            offset = maxOffset - offset + 1;
            offset -= (to - from);
            to = newTo;
            from = newFrom;
        }
        
        @Override
        public String toString()
        {
            return "" + clusterId + "\t" + transcript.getName() + "\t" + from + "\t" + to + "\t" + offset;
        }
    }
}
