package biouml.plugins.riboseq.mappability;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.riboseq.mappability.SuffixArraysLong.ByteArray;
import biouml.plugins.riboseq.mappability.SuffixArraysLong.LongArray;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.FileImporter;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceCollection;
import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.bean.BeanInfoEx2;

/** Same as SequenceMinimalUniqueLength but for sequences larger then Integer.MAX_VALUE */
public class LargeSequenceMinimalUniqueLength extends AnalysisMethodSupport<LargeSequenceMinimalUniqueLength.Parameters>
{

    public LargeSequenceMinimalUniqueLength(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        log.info( "Loading sequences..." );
        SequenceCollection genome = parameters.getGenome().getDataElement( SequenceCollection.class );
        long totalBases = 0;
        for(AnnotatedSequence chr : genome)
            totalBases += chr.getSequence().getLength();
        long textLength = (totalBases + genome.getSize())*2;
        ByteArray text = new ByteArray(textLength);
        String[] chrNames = new String[genome.getSize()*2];
        long[] chrOffsets = new long[genome.getSize()*2];
        long pos = 0;
        long chrOffset = 0;
        int chrIdx = 0;
        for(AnnotatedSequence chr : genome)
        {
            Sequence seq = chr.getSequence();
            for(int i = 0; i < seq.getLength(); i++)
            {
                byte nt = seq.getLetterAt( i + seq.getStart() );
                nt = (byte)Character.toUpperCase( nt );
                if(nt != 'A' && nt != 'C' && nt != 'G' && nt != 'T')
                    nt = '\n';
                text.set( pos++, nt );
            }
            text.set( pos++, '\n' );
            
            chrNames[chrIdx] = chr.getName() + "+";
            chrOffsets[chrIdx] = chrOffset;
            chrIdx++;
            chrOffset = pos;

            for(int i = 0; i < seq.getLength(); i++)
            {
                byte nt = (byte)text.get( chrOffset - i - 2 );
                if(nt != '\n')
                {
                    nt = seq.getAlphabet().letterComplementMatrix()[nt];
                    nt = (byte)Character.toUpperCase( nt );
                }
                text.set( pos++, nt );
            }
            text.set( pos++, '\n' );
            
            chrNames[chrIdx] = chr.getName() + "-";
            chrOffsets[chrIdx] = chrOffset;
            chrIdx++;
            chrOffset = pos;
        }
        log.info( chrNames.length + " sequences loaded, total bases " + totalBases );

        long time = System.currentTimeMillis();
        
        time = System.currentTimeMillis();
        log.info( "Building suffix array" );
        LongArray sa = new LongArray( textLength );
        SuffixArraysLong.suffixsort( text, sa, textLength );
        if( !SuffixArraysLong.check( text, sa, textLength ) )
            throw new Exception("Suffix array check not passed");
        log.info( "Done (" + ( System.currentTimeMillis() - time ) + "ms)" );

        time = System.currentTimeMillis();
        log.info( "Computing longest common prefix array" );
        LongArray lcp = SuffixArraysLong.computeLCP( text, sa, textLength );
        log.info( "Done (" + ( System.currentTimeMillis() - time ) + "ms)" );

        time = System.currentTimeMillis();
        log.info( "Computing mappable read length for each position" );
        LongArray mrl = computeMRL( text, sa, lcp, textLength );
        log.info( "Done (" + ( System.currentTimeMillis() - time ) + "ms)" );

        time = System.currentTimeMillis();
        log.info( "Writing results" );
        FileDataElement res = saveResultsToWigFile( chrNames, chrOffsets, mrl, textLength );
        log.info( "Done (" + ( System.currentTimeMillis() - time ) + "ms)" );

        return res;
    }

    public LongArray computeMRL(ByteArray text, LongArray sa, LongArray lcp, long n)
    {
        LongArray mrl = new LongArray(n);
        for( long i = 0; i < n; i++ )
        {
            long lcpk = Math.max( lcp.get( i ), lcp.get( i + 1 ) );
            mrl.set( sa.get( i ), text.get( sa.get(i) + lcpk ) == '\n' ? -1 : lcpk + 1 );
        }
        return mrl;
    }

    private FileDataElement saveResultsToWigFile(String[] chrNames, long[] chrOffsets, LongArray mrl, long n) throws Exception
    {
        try (TempFile wigFile = TempFiles.file( ".wig" );
             BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( wigFile ) ), StandardCharsets.UTF_8) ))
        {
            for( int i = 0; i < chrNames.length; i++ )
            {
                writer.append( "fixedStep chrom=" ).append( chrNames[i]).append( " start=1 step=1\n" );
                long end = i < chrOffsets.length - 1 ? chrOffsets[i+1] : n;
                for(long pos = chrOffsets[i]; pos < end - 1; pos++)
                {
                    writer.append( String.valueOf( mrl.get( pos ) ) ).append( '\n' );
                }
            }
            writer.close();

            FileImporter importer = new FileImporter();
            DataCollection<DataElement> parent = parameters.getResult().getParentCollection();
            String name = parameters.getResult().getName();
            importer.getProperties( parent, wigFile, name ).setPreserveExtension( false );
            return (FileDataElement)importer.doImport( parent, wigFile, name, null, log );
        }
    }


    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath genome;
        @PropertyName("Genome")
        @PropertyDescription("Collection of genomic sequences")
        public DataElementPath getGenome()
        {
            return genome;
        }
        public void setGenome(DataElementPath genome)
        {
            Object oldValue = this.genome;
            this.genome = genome;
            firePropertyChange( "genome", oldValue, genome );
        }

        private DataElementPath result;
        @PropertyName ( "Output wig file" )
        @PropertyDescription ( "Wig file with minimal unique length values" )
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
            property( "genome" ).inputElement( SequenceCollection.class ).add();
            property( "result" ).outputElement( FileDataElement.class ).add();
        }
    }
}
