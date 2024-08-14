package biouml.plugins.riboseq.mappability;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.riboseq.transcripts.Transcript;
import biouml.plugins.riboseq.transcripts.TranscriptSet;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.FileImporter;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.bean.BeanInfoEx2;

public class TranscriptomeMinimalUniqueLength extends AnalysisMethodSupport<TranscriptomeMinimalUniqueLength.Parameters>
{
    public TranscriptomeMinimalUniqueLength(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        long time = System.currentTimeMillis();
        log.info( "Fetching transcripts" );
        List<Transcript> transcriptList = parameters.getTranscriptSet().createTranscriptLoader().loadTranscripts( log );
        log.info( transcriptList.size() + " transcripts loaded (" + ( System.currentTimeMillis() - time ) + "ms)" );

        time = System.currentTimeMillis();
        log.info( "Filtering transcripts" );
        transcriptList = filterTranscripts( transcriptList );
        log.info( transcriptList.size() + " transcripts remain (" + ( System.currentTimeMillis() - time ) + "ms)" );

        time = System.currentTimeMillis();
        log.info( "Loading transcript sequences" );
        Transcripts transcripts = new Transcripts( transcriptList );
        log.info( transcripts.totalBases + " total bases (" + ( System.currentTimeMillis() - time ) + "ms)" );

        time = System.currentTimeMillis();
        log.info( "Building suffix array" );
        int[] sa = buildSuffixArray( transcripts.text );
        log.info( "Done (" + ( System.currentTimeMillis() - time ) + "ms)" );

        time = System.currentTimeMillis();
        log.info( "Computing longest common prefix array" );
        int[] lcp = SuffixArrays.computeLCP( transcripts.text, sa );
        log.info( "Done (" + ( System.currentTimeMillis() - time ) + "ms)" );

        time = System.currentTimeMillis();
        log.info( "Recovering genomic positions for each suffix" );
        int[] g = getGenomicPositions( transcripts );
        log.info( "Done (" + ( System.currentTimeMillis() - time ) + "ms)" );

        time = System.currentTimeMillis();
        log.info( "Computing mappable read length for each position" );
        int[] mrl = computeMRL( sa, lcp, transcripts.text, g );
        lcp = null;
        g = null;
        log.info( "Done (" + ( System.currentTimeMillis() - time ) + "ms)" );

        time = System.currentTimeMillis();
        log.info( "Transforming to transcript coordinates" );
        int[][] result = transformToTranscriptCoordinates( transcripts, sa, mrl );
        log.info( "Done (" + ( System.currentTimeMillis() - time ) + "ms)" );

        time = System.currentTimeMillis();
        log.info( "Saving results" );
        FileDataElement resultingFile = saveResultsToWigFile( transcriptList, result );
        log.info( "Done (" + ( System.currentTimeMillis() - time ) + "ms)" );
        return resultingFile;
    }

    private FileDataElement saveResultsToWigFile(List<Transcript> transcriptList, int[][] result) throws Exception
    {
        try (
                TempFile wigFile = TempFiles.file( ".wig" );
                BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( wigFile ) ), StandardCharsets.UTF_8) ))
        {
            for( int i = 0; i < result.length; i++ )
            {
                Transcript t = transcriptList.get( i );
                writer.append( "fixedStep chrom=" ).append( t.getName() ).append( " start=1 step=1\n" );
                for( int j = 0; j < result[i].length; j++ )
                    writer.append( String.valueOf( result[i][j] ) ).append( '\n' );
            }

            FileImporter importer = new FileImporter();
            DataCollection<DataElement> parent = parameters.getResult().getParentCollection();
            String name = parameters.getResult().getName();
            importer.getProperties( parent, wigFile, name ).setPreserveExtension( false );
            return (FileDataElement)importer.doImport( parent, wigFile, name, null, log );
        }
    }

    private int[][] transformToTranscriptCoordinates(Transcripts transcripts, int[] sa, int[] mrl)
    {
        int[][] result = new int[transcripts.transcripts.size()][];
        for(int i = 0; i < result.length; i++)
            result[i] = new int[transcripts.transcripts.get( i ).getLength()];
        for(int i = 0; i < sa.length; i++)
        {
            if(transcripts.text[sa[i]] == '\n')
                continue;
            int tId = transcripts.getTranscriptIndex( sa[i] );
            int tOffset = sa[i] - transcripts.transcriptOffset[tId];
            result[tId][tOffset] = mrl[i];
        }
        return result;
    }

    private int[] computeMRL(int[] sa, int[] lcp, byte[] text, int[] g)
    {
        int[] mrl = new int[sa.length];
        int i = 0;
        while( i < sa.length )
        {
            int j = i + 1;
            while( j < sa.length && g[sa[i]] == g[sa[j]])
                j++;
            if( j == i + 1 )
            {
                int lcpk = Math.max( lcp[i], lcp[j] );
                mrl[i] = text[sa[i] + lcpk]=='\n' ? -1 : lcpk + 1;
            }
            else
            {
                int[] min1 = new int[j - i];
                min1[0] = lcp[i];
                for( int k = i + 1; k < j; k++ )
                    min1[k - i] = Math.min( min1[k - i - 1], lcp[k] );

                int[] min2 = new int[j - i];
                min2[min2.length - 1] = lcp[j];
                for( int k = j - 2; k >= i; k-- )
                    min2[k - i] = Math.min( min2[k - i + 1], lcp[k + 1] );

                for( int k = i; k < j; k++ )
                {
                    int lcpk = Math.max( min1[k - i], min2[k - i] );
                    mrl[k] = text[sa[k] + lcpk]=='\n' ? -1 : lcpk + 1;
                }
            }
            i = j;
        }
        return mrl;
    }
    
   

    private int[] getGenomicPositions(Transcripts transcripts)
    {
        SortedMap<String, Integer> chrOffset = new TreeMap<>();
        DataCollection<AnnotatedSequence> chrDC = parameters.getTranscriptSet().getChromosomes();
        int cumSum = 0;
        for(AnnotatedSequence as : chrDC)
        {
            chrOffset.put( as.getName(), cumSum);
            cumSum += as.getSequence().getLength();
        }

        int[] g = new int[transcripts.text.length];
        for(int i = 0; i < transcripts.transcripts.size(); i++)
        {
            Transcript t = transcripts.transcripts.get( i );
            int offset = transcripts.transcriptOffset[i];
            if(t.isOnPositiveStrand())
            {
                List<Interval> exons = t.getExonLocations();
                for(int k = 0; k < exons.size(); k++)
                {
                    Interval e = exons.get( k );
                    for(int j = 0; j < e.getLength(); j++)
                        g[offset++] = e.getFrom() + j + chrOffset.get( t.getChromosome() );
                } 
            }
            else
            {
                List<Interval> exons = t.getExonLocations();
                for(int k = exons.size() - 1; k >= 0; k--)
                {
                    Interval e = exons.get( k );
                    for(int j = 0; j < e.getLength(); j++)
                        g[offset++] = e.getTo() - j + chrOffset.get( t.getChromosome() );
                } 
            }
            g[offset] = -1;
        }
        return g;
    }
    
    private List<Transcript> filterTranscripts(List<Transcript> transcripts) throws Exception
    {
        DataCollection<AnnotatedSequence> chromosomes = parameters.getTranscriptSet().getChromosomes();
        Set<String> ignoredChromosomes = new HashSet<>();
        List<Transcript> result = new ArrayList<>();
        for(Transcript t : transcripts)
        {
            if(parameters.isProteinCoding() && !t.isCoding())
                continue;
            if(chromosomes.get( t.getChromosome() ) == null)
            {
                ignoredChromosomes.add( t.getChromosome() );
                continue;
            }
            result.add( t );
        }
        
        if(!ignoredChromosomes.isEmpty())
            log.warning( "Ignoring chromsomes " + String.join( ", ", ignoredChromosomes ) );
        return result;
    }
    
    private int[] buildSuffixArray(byte[] text) throws Exception
    {
        int[] sa = new int[text.length];
        SuffixArrays.suffixsort( text, sa, text.length );
        return sa;
    }
    
    public static class Parameters extends AbstractAnalysisParameters
    {
        private TranscriptSet transcriptSet;
        {            
            setTranscriptSet( new TranscriptSet() );
        }
        @PropertyName("Transcript set")
        public TranscriptSet getTranscriptSet()
        {
            return transcriptSet;
        }
        public void setTranscriptSet(TranscriptSet transcriptSet)
        {
            TranscriptSet oldValue = this.transcriptSet;
            this.transcriptSet = withPropagation( oldValue, transcriptSet );
            firePropertyChange( "transcriptSet", oldValue, transcriptSet );
        }

        private boolean proteinCoding;
        @PropertyName("Only protein coding")
        @PropertyDescription("Use only protein coding transcripts")
        public boolean isProteinCoding()
        {
            return proteinCoding;
        }
        public void setProteinCoding(boolean proteinCoding)
        {
            boolean oldValue = this.proteinCoding;
            this.proteinCoding = proteinCoding;
            firePropertyChange( "proteinCoding", oldValue, proteinCoding );
            transcriptSet.setOnlyProteinCoding( proteinCoding );
        }
        
        private DataElementPath result;
        @PropertyName("Result path")
        @PropertyDescription("Path to store result")
        public DataElementPath getResult()
        {
            return result;
        }
        public void setResult(DataElementPath result)
        {
            Object oldValue = this.result;
            this.result = result;
            firePropertyChange( "result", oldValue, result );
        }
    }
    
    public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add( "transcriptSet" );
            add("proteinCoding");
            property( "result" ).outputElement( FileDataElement.class ).add();
        }
    }
    
    
    private class Transcripts
    {
        List<Transcript> transcripts;
        byte[] text;
        int[] transcriptOffset;
        long totalBases;
        
        public Transcripts(List<Transcript> transcripts) throws Exception
        {
            this.transcripts = transcripts;
            init();
        }
        
        void init() throws Exception
        {
            for(Transcript t : transcripts)
                totalBases += t.getLength();
            
            if(totalBases + transcripts.size() >= Integer.MAX_VALUE)
                throw new Exception("Too large transcriptome");
            
            text = new byte[(int)totalBases + transcripts.size()];
            transcriptOffset = new int[transcripts.size()];
            
            DataCollection<AnnotatedSequence> chromosomes = parameters.getTranscriptSet().getChromosomes();
            int offset = 0;
            for( int i = 0; i < transcripts.size(); i++ )
            {
                Transcript t = transcripts.get( i );
                transcriptOffset[i] = offset;
                
                AnnotatedSequence chr = chromosomes.get( t.getChromosome() );
                byte[] seq = t.getSequence( chr.getSequence() ).getBytes();
                seq = new String(seq).toUpperCase().getBytes();
                
                System.arraycopy( seq, 0, text, offset, seq.length );
                offset += seq.length;
                text[offset++] = '\n';
            }
        }
        
        int getTranscriptIndex(int offset)
        {
            int searchRes = Arrays.binarySearch( transcriptOffset, offset );
            if(searchRes < 0)
                searchRes = -(searchRes + 2);
            return searchRes;
        }
        
    }
}
