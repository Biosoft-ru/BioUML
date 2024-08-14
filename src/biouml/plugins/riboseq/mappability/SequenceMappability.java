package biouml.plugins.riboseq.mappability;


import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Track;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.bean.BeanInfoEx2;

public class SequenceMappability extends AnalysisMethodSupport<SequenceMappability.Parameters>
{
    public SequenceMappability(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        DataCollection<AnnotatedSequence> genome = parameters.getGenome().getSequenceCollection();
        Track track = parameters.getInputTrack().getDataElement( Track.class );
        SequenceSet seqs = new SequenceSet();
        seqs.load( track, genome );
        
        int[][] result = computeMappableLength( seqs.data, parameters.getMinReadLength(), parameters.getMaxReadLength(), parameters.getMaxAlignsPerRead() );
        
        TableDataCollection outTable = TableDataCollectionUtils.createTableDataCollection( parameters.getOutputTable() );
        for(int readLen = parameters.getMinReadLength(); readLen <= parameters.getMaxReadLength(); readLen++)
            outTable.getColumnModel().addColumn( "Len=" + readLen, DataType.Integer );
        for(int i = 0; i < seqs.names.size(); i++)
        {
            Object[] values = new Object[result.length];
            for(int j = 0; j < values.length; j++)
                values[j] = result[j][i];
            TableDataCollectionUtils.addRow( outTable, seqs.names.get( i ), values, true );
        }
        outTable.finalizeAddition();
        parameters.getOutputTable().save( outTable );
        
        return outTable;
    }
    
    public static int[][] computeMappableLength(byte[] seq, int minReadLen, int maxReadLen, int maxAlignsPerRead)
    {
        int[] seqId = new int[seq.length];
        int nSeqs = 0;
        for(int i = 0; i < seq.length; i++)
        {
            seqId[i] = nSeqs;
            byte c = seq[i];
            if(c == '\n') nSeqs++;
            if(c != 'A' && c != 'C' && c != 'G' && c != 'T' && c != '\n')
                seq[i] = '\n';
        }
        
        int[] sa = new int[seq.length];
        SuffixArrays.suffixsort( seq, sa, seq.length );
        int[] lcp = SuffixArrays.computeLCP( seq, sa );
                
        int[][] result = new int[maxReadLen - minReadLen + 1][nSeqs];
        
        for(int readLen = minReadLen; readLen <= maxReadLen; readLen++)
        {
            int[] mappableLength = result[readLen - minReadLen];
            int start = 0;
            while(start < seq.length)
            {
                int end = start + 1;
                while(end < seq.length && lcp[end] >= readLen)
                    end++;
                if(end - start <= maxAlignsPerRead)
                {
                    for(int j = start; j < end; j++)
                        mappableLength[seqId[sa[j]]]++;
                }
                start = end;
            }
            for(int i = 0;i < nSeqs; i++)
                mappableLength[i] = Math.max( 0, mappableLength[i] - readLen );
        }
        
        return result;
    }
    
    public static class Parameters extends AbstractAnalysisParameters
    {
        public Parameters()
        {
            setGenome( new BasicGenomeSelector() );
        }
        
        private DataElementPath inputTrack;
        public DataElementPath getInputTrack()
        {
            return inputTrack;
        }
        public void setInputTrack(DataElementPath inputTrack)
        {
            Object oldValue = this.inputTrack;
            this.inputTrack = inputTrack;
            if(inputTrack != null && !inputTrack.equals( oldValue ))
                genome.setFromTrack( inputTrack.getDataElement( Track.class ) );
            firePropertyChange( "inputTrack", oldValue, inputTrack );
        }
        
        private BasicGenomeSelector genome;
        public BasicGenomeSelector getGenome()
        {
            return genome;
        }
        public void setGenome(BasicGenomeSelector genome)
        {
            BasicGenomeSelector oldValue = this.genome;
            this.genome = withPropagation( oldValue, genome );
            firePropertyChange( "genome", oldValue, genome );
        }

        private int maxAlignsPerRead = 1;
        public int getMaxAlignsPerRead()
        {
            return maxAlignsPerRead;
        }
        public void setMaxAlignsPerRead(int maxAlignsPerRead)
        {
            int oldValue = this.maxAlignsPerRead;
            this.maxAlignsPerRead = maxAlignsPerRead;
            firePropertyChange( "maxAlignsPerRead", oldValue, maxAlignsPerRead );
        }

        private int minReadLength = 30;
        public int getMinReadLength()
        {
            return minReadLength;
        }
        public void setMinReadLength(int minReadLength)
        {
            int oldValue = this.minReadLength;
            this.minReadLength = minReadLength;
            firePropertyChange( "minReadLength", oldValue, minReadLength );
        }

        private int maxReadLength = 30;
        public int getMaxReadLength()
        {
            return maxReadLength;
        }
        public void setMaxReadLength(int maxReadLength)
        {
            int oldValue = this.maxReadLength;
            this.maxReadLength = maxReadLength;
            firePropertyChange( "maxReadLength", oldValue, maxReadLength );
        }

        private DataElementPath outputTable;
        public DataElementPath getOutputTable()
        {
            return outputTable;
        }
        public void setOutputTable(DataElementPath outputTable)
        {
            Object oldValue = this.outputTable;
            this.outputTable = outputTable;
            firePropertyChange( "outputTable", oldValue, outputTable );
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
            property("inputTrack").inputElement( Track.class ).add();
            property("genome").add();
            add("maxAlignsPerRead");
            add("minReadLength");
            add("maxReadLength");
            property("outputTable").outputElement( TableDataCollection.class ).add();
        }
        
    }
}
