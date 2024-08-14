package biouml.plugins.riboseq.mappability;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.FileImporter;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.SequenceCollection;
import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.bean.BeanInfoEx2;

public class SequenceMinimalUniqueLength extends AnalysisMethodSupport<SequenceMinimalUniqueLength.Parameters>
{

    public SequenceMinimalUniqueLength(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        log.info( "Loading sequences..." );
        SequenceCollection genome = parameters.getGenome().getDataElement( SequenceCollection.class );
        SequenceSet seqs = new SequenceSet();
        seqs.load( genome );
        log.info( seqs.names.size() + " sequences loaded, total bases " + ( seqs.data.length - seqs.names.size() ) );

        long time = System.currentTimeMillis();
        
        log.info( "Preprocessing special symbols" );
        byte[] text = new byte[seqs.data.length];
        for(int i = 0; i < text.length; i++)
        {
            byte c = seqs.data[i];
            if(c != 'A' && c != 'C' && c != 'G' && c != 'T')
                c = '\n';
            text[i] = c;
        }
        log.info( "Done (" + ( System.currentTimeMillis() - time ) + "ms)" );
        
        time = System.currentTimeMillis();
        log.info( "Building suffix array" );
        int[] sa = new int[text.length];
        SuffixArrays.suffixsort( text, sa, text.length );
        log.info( "Done (" + ( System.currentTimeMillis() - time ) + "ms)" );

        time = System.currentTimeMillis();
        log.info( "Computing longest common prefix array" );
        int[] lcp = SuffixArrays.computeLCP( text, sa );
        log.info( "Done (" + ( System.currentTimeMillis() - time ) + "ms)" );

        time = System.currentTimeMillis();
        log.info( "Computing mappable read length for each position" );
        int[] mrl = computeMRL( text, sa, lcp );
        log.info( "Done (" + ( System.currentTimeMillis() - time ) + "ms)" );

        time = System.currentTimeMillis();
        log.info( "Writing results" );
        FileDataElement res = saveResultsToWigFile( seqs, mrl );
        log.info( "Done (" + ( System.currentTimeMillis() - time ) + "ms)" );

        return res;
    }

    public int[] computeMRL(byte[] text, int[] sa, int[] lcp)
    {
        int[] mrl = new int[text.length];
        for( int i = 0; i < text.length; i++ )
        {
            int lcpk = Math.max( lcp[i], lcp[i + 1] );
            mrl[sa[i]] = text[sa[i] + lcpk] == '\n' ? -1 : lcpk + 1;
        }
        return mrl;
    }

    private FileDataElement saveResultsToWigFile(SequenceSet seqs, int[] mrl) throws Exception
    {
        try (TempFile wigFile = TempFiles.file( ".wig" );
             BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( wigFile ) ), StandardCharsets.UTF_8) ))
        {
            int i = 0;
            for( String name : seqs.names )
            {
                writer.append( "fixedStep chrom=" ).append( name ).append( " start=1 step=1\n" );
                while( seqs.data[i] != '\n' )
                {
                    writer.append( String.valueOf( mrl[i] ) ).append( '\n' );
                    i++;
                }
                i++;
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
