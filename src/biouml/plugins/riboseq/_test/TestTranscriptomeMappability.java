package biouml.plugins.riboseq._test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import biouml.plugins.ensembl.access.EnsemblDatabase;
import biouml.plugins.riboseq.ingolia._test.AbstractRiboSeqTest;
import biouml.plugins.riboseq.mappability.TranscriptomeMinimalUniqueLength;
import biouml.plugins.riboseq.mappability.TranscriptomeMinimalUniqueLength.Parameters;
import biouml.plugins.riboseq.transcripts.EnsemblTranscriptsProvider;
import biouml.plugins.riboseq.transcripts.Transcript;
import biouml.plugins.riboseq.transcripts.TranscriptSet;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.IntArray;

public class TestTranscriptomeMappability extends AbstractRiboSeqTest
{
    public void test1() throws Exception
    {
        TranscriptomeMinimalUniqueLength analysis = new TranscriptomeMinimalUniqueLength( null, "test" );
        Parameters params = analysis.getParameters();
        
        TranscriptSet transcriptSet = new TranscriptSet();
        EnsemblDatabase ensembl = new EnsemblDatabase( "databases/EnsemblMouse81_38" );
        transcriptSet.setEnsembl( ensembl );

        params.setTranscriptSet( transcriptSet  );

        DataElementPath subsetPath = DataElementPath.create( "live/subset" );
        TableDataCollection subset = TableDataCollectionUtils.createTableDataCollection( subsetPath );
        String[] transcripts = {"ENSMUST00000070968", "ENSMUST00000186574"};
        for(String t : transcripts)
            TableDataCollectionUtils.addRow( subset, t, new Object[0] );
        subsetPath.save( subset );
        transcriptSet.setTranscriptSubset( subsetPath );
        
        DataElementPath resultPath = DataElementPath.create( "databases/riboseq_data/files/result.wig" );
        params.setResult( resultPath );
        
        analysis.justAnalyzeAndPut();

        FileDataElement result = resultPath.getDataElement( FileDataElement.class );
        Map<String, int[]> profiles = parseWigFile( result.getFile() );
        assertEquals( transcripts.length, profiles.size() );
        
        Map<String, byte[]> sequences = getTranscriptSequences( ensembl, transcripts );
        
        
        Map<String, Interval[]> uniqueRegions = new HashMap<>();
        uniqueRegions.put( transcripts[0], new Interval[]{new Interval(5151-2683,5151-1)});
        uniqueRegions.put( transcripts[1], new Interval[]{new Interval(0,14),new Interval(2834-351,2834-1)});
        
        for(String t : transcripts)
        {
            int[] p = profiles.get( t );
            assertNotNull( p );
            byte[] s = sequences.get( t );
            assertEquals( p.length, s.length );
            Interval[] ur = uniqueRegions.get( t );
            for(int i = 0; i < p.length; i++)
            {
                if( p[i] > 0 )
                {
                    int count1 = 0;
                    int count2 = 0;
                    for( String tt : transcripts )
                    {
                        byte[] ss = sequences.get( tt );
                        count1 += countOccurencies( s, i, p[i], ss );
                        count2 += countOccurencies( s, i, p[i]-1, ss );
                    }

                    boolean uniqueMatch = false;
                    Interval patInterval = new Interval(i,i+p[i]-1);
                    for(Interval u : ur)
                        if(u.intersects( patInterval ))
                            uniqueMatch = true;
                    if(uniqueMatch)
                        assertEquals( t + "[" + i + "]", 1, count1 );
                    else
                        assertEquals( t + "[" + i + "]", 2, count1 );
                    
                    if(p[i] > 1)
                    {
                        uniqueMatch = false;
                        patInterval = new Interval(i,i+p[i]-2);
                        for(Interval u : ur)
                            if(u.intersects( patInterval ))
                                uniqueMatch = true;
                        if(uniqueMatch)
                            assertTrue(t + "[" + i + "]", count2 > 1);
                        else
                            assertTrue(t + "[" + i + "]", count2 > 2);
                        
                    }
                }
                else
                {
                    //always match to unique region
                    int count = 0;
                    for(String tt : transcripts)
                    {
                        byte[] ss = sequences.get( tt );
                        count += countOccurencies( s, i, s.length-i, ss );
                    }
                    assertTrue(count > 1);
                }

            }
        }
        
    }
    
    boolean arraysEqual(byte[] a, int aOffset, byte[] b, int bOffset, int len)
    {
        for(int i = 0; i < len; i++)
            if(a[aOffset + i] != b[bOffset + i])
                return false;
        return true;
    }
    
    int countOccurencies(byte[] pat, int patOffset, int patLength, byte[] reference)
    {
        int res = 0;
        for(int i = 0; i < reference.length - patLength + 1; i++)
            if(arraysEqual(pat, patOffset, reference, i, patLength))
                res++;
        return res;
    }
    
    Map<String, int[]> parseWigFile(File file) throws Exception
    {
        Map<String, int[]> result = new HashMap<>();
        try( BufferedReader reader = new BufferedReader(
                new InputStreamReader( new GZIPInputStream( new FileInputStream( file ) ), StandardCharsets.UTF_8 ) ) )
        {
            String line = reader.readLine();
            while(line != null)
            {
                String[] fields = line.split( " " );
                assertEquals( 4, fields.length );
                assertEquals(fields[0], "fixedStep");
                assertEquals(fields[2], "start=1");
                assertEquals(fields[3], "step=1");
                assertTrue(fields[1].startsWith( "chrom=" ));
                String chrom = fields[1].substring( "chrom=".length(), fields[1].length() );
                
                IntArray values = new IntArray();
                while((line = reader.readLine()) != null)
                {
                    int value;
                    try
                    {
                        value = Integer.parseInt( line );
                    }
                    catch( NumberFormatException e )
                    {
                        break;
                    }
                    values.add( value );
                }
                values.compress();
                
                result.put( chrom, values.data() );
            }
        }
        return result;
    }
    
    Map<String, byte[]> getTranscriptSequences(EnsemblDatabase ensembl, String[] ids) throws Exception
    {
        Map<String, byte[]> result = new HashMap<>();
        EnsemblTranscriptsProvider provider = new EnsemblTranscriptsProvider( ensembl );
        provider.setSubset( Arrays.asList( ids ) );
        for(Transcript transcript : provider.getTranscripts())
        {
            Sequence chrSequence = ensembl.getPrimarySequencesPath().getChildPath( transcript.getChromosome() ).getDataElement( AnnotatedSequence.class ).getSequence();
            Sequence sequence = transcript.getSequence( chrSequence );
            byte[] seqData = new String(sequence.getBytes()).toUpperCase().getBytes();
            result.put( transcript.getName(), seqData );
        }
        return result;
    }
}
