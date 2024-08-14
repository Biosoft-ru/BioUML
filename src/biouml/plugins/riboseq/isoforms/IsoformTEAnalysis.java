package biouml.plugins.riboseq.isoforms;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.riboseq.transcripts.Transcript;
import biouml.plugins.riboseq.transcripts.TranscriptSet;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;
import net.sf.samtools.SAMSequenceDictionary;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.BAMTrack;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.bean.BeanInfoEx2;

public class IsoformTEAnalysis extends AnalysisMethodSupport<IsoformTEAnalysis.Parameters>
{
    public IsoformTEAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        List<Transcript> transcripts = loadTranscripts();
        
        log.info( "Fetching transcript sequences" );
        TranscriptSequence[] transcriptSeqs = fetchTranscriptSequences( transcripts );
        TranscriptSequence[] cdsSeqs = getCDSSequences( transcripts, transcriptSeqs );

        log.info( "Sampling mRNA-seq" );
        double[][] mrnaTPMSamples = sampleTPM( parameters.getMrnaseqBAMFile(), transcriptSeqs );
        log.info( "Sampling ribo-seq" );
        double[][] riboTPMSamples = sampleTPM( parameters.getRiboseqBAMFile(), cdsSeqs );

        log.info("Computing TE");
        double[][] teSamples = computeTE( mrnaTPMSamples, riboTPMSamples );

        log.info( "Computing statistics" );
        double[][] teStats = computeStatistics( teSamples );
        double[][] riboStats = computeStatistics( riboTPMSamples );
        double[][] mrnaStats = computeStatistics( mrnaTPMSamples );

        log.info( "Writing results" );
        return makeOutputTable( transcriptSeqs, teStats, riboStats, mrnaStats );
    }
    
    private List<Transcript> loadTranscripts()
    {
        List<Transcript> transcripts = parameters.getTranscripts().createTranscriptLoader().loadTranscripts( log );
        DataCollection<AnnotatedSequence> chromosomes = parameters.getTranscripts().getChromosomes();
        Set<String> chrNames = new HashSet<>(chromosomes.getNameList());
        return StreamEx.of( transcripts ).filter( t -> t.isCoding() && chrNames.contains( t.getChromosome() ) ).toList();
    }
    
    private TranscriptSequence[] fetchTranscriptSequences(List<Transcript> transcripts) throws Exception
    {
        TranscriptSequence[] result = new TranscriptSequence[transcripts.size()];
        DataCollection<AnnotatedSequence> chromosomes = parameters.getTranscripts().getChromosomes();
        for( int i = 0; i < transcripts.size(); i++ )
        {
            Transcript t = transcripts.get( i );
            Sequence chrSequence = chromosomes.get( t.getChromosome() ).getSequence();
            Sequence tSequence = t.getSequence( chrSequence );

            byte[] codes = new byte[tSequence.getLength()];
            for( int j = 0; j < codes.length; j++ )
                codes[j] = tSequence.getLetterCodeAt( j + tSequence.getStart() );
            result[i] = new TranscriptSequence( t.getName(), codes );
        }   
        return result;
    }
    
    private TranscriptSequence[] getCDSSequences(List<Transcript> transcripts, TranscriptSequence[] transcriptSeqs)
    {
        TranscriptSequence[] cdsSeqs = new TranscriptSequence[transcripts.size()];
        for( int i = 0; i < transcripts.size(); i++ )
        {
            Transcript t = transcripts.get( i );
            Interval cds = t.getCDSLocations().get( 0 );
            int d = parameters.getCdsOverhangs();
            byte[] tSeq = transcriptSeqs[i].seq;
            cds = new Interval( Math.max( 0, cds.getFrom() - d ), Math.min( tSeq.length - 1, cds.getTo() + d ) );
            byte[] cdsCodes = new byte[cds.getLength()];
            System.arraycopy( tSeq, cds.getFrom(), cdsCodes, 0, cds.getLength() );
            cdsSeqs[i] = new TranscriptSequence( t.getName(), cdsCodes );
        }
        return cdsSeqs;
    }

    private double[][] sampleTPM(File bamFile, TranscriptSequence[] seqs) throws RepositoryException
    {
        Map<String, Integer> nameToId = new HashMap<>();
        for( int i = 0; i < seqs.length; i++ )
            nameToId.put( seqs[i].name, i );
        
        ReadsAndHits readsAndHits = parseBAMFile( bamFile, nameToId );
        
        EM em = new EM( readsAndHits.hits, readsAndHits.reads, seqs, true );
        //we just want to estimate model using EM
        em.setRoundsModelUpdate( 10 );
        em.setMinRound( 11 );
        em.setMaxRound( 11 );
        em.run();
        
        Gibbs gibbs = new Gibbs( readsAndHits.hits, seqs.length, readsAndHits.reads.getUnmappedCount(), em.getHitConProb(),
                em.getNoiseConProb() );
        int nSamples = parameters.getNSamples();
        double[][] thetaSamples = gibbs.sampleTheta( nSamples );

        double[][] tpmSamples = new double[seqs.length][nSamples];
        for( int i = 0; i < nSamples; i++ )
        {
            double[] tpm = ExpressionMeasures.calcTPM( thetaSamples[i], em.getModel().getLenDist(), seqs );
            for(int j = 0; j < seqs.length; j++)
                tpmSamples[j][i] = tpm[j];
        }
        return tpmSamples;
    }

    private double[][] computeTE(double[][] mrnaTPMSamples, double[][] riboTPMSamples)
    {
        int nSamples = mrnaTPMSamples[0].length;
        int nTranscripts = mrnaTPMSamples.length;
        double[][] teSamples = new double[nTranscripts][nSamples];
        for( int i = 0; i < nSamples; i++ )
        {
            double sum = 0;
            for( int j = 0; j < nTranscripts; j++ )
                sum += (teSamples[j][i] = riboTPMSamples[j][i] / mrnaTPMSamples[j][i]);
            for( int j = 0; j < nTranscripts; j++ )
                teSamples[j][i] /= sum;
        }
        return teSamples;
    }

    private TableDataCollection makeOutputTable(TranscriptSequence[] transcriptSeqs, double[][] teStats, double[][] riboStats, double[][] mrnaStats)
    {
        TableDataCollection outTable = TableDataCollectionUtils.createTableDataCollection( parameters.getOutputTable() );
        outTable.getColumnModel().addColumn( "Mean", DataType.Float );
        outTable.getColumnModel().addColumn( "Median", DataType.Float );
        outTable.getColumnModel().addColumn( "CI_LB", DataType.Float );
        outTable.getColumnModel().addColumn( "CI_UB", DataType.Float );
        
        outTable.getColumnModel().addColumn( "Ribo Mean", DataType.Float );
        outTable.getColumnModel().addColumn( "Ribo Median", DataType.Float );
        outTable.getColumnModel().addColumn( "Ribo CI_LB", DataType.Float );
        outTable.getColumnModel().addColumn( "Ribo CI_UB", DataType.Float );

        outTable.getColumnModel().addColumn( "mRNA Mean", DataType.Float );
        outTable.getColumnModel().addColumn( "mRNA Median", DataType.Float );
        outTable.getColumnModel().addColumn( "mRNA CI_LB", DataType.Float );
        outTable.getColumnModel().addColumn( "mRNA CI_UB", DataType.Float );
        
        for( int i = 0; i < transcriptSeqs.length; i++ )
        {
            double[] t = teStats[i];
            double[] r = riboStats[i];
            double[] m = mrnaStats[i];
            Object[] values = new Object[] {
                    t[0], t[1], t[2], t[3],
                    r[0], r[1], r[2], r[3],
                    m[0], m[1], m[2], m[3]
            };
            TableDataCollectionUtils.addRow( outTable, transcriptSeqs[i].name, values, true );
        }
        outTable.finalizeAddition();
        parameters.getOutputTable().save( outTable );
        return outTable;
    }
    
    private double[][] computeStatistics(double[][] samples)
    {
        int nTranscripts = samples.length;
        double[][] result = new double[nTranscripts][];
        for( int transcript = 0; transcript < nTranscripts; transcript++ )
        {
            double mean = 0;
            double[] tSamples = samples[transcript].clone();
            int nSamples = tSamples.length;
            for( int j = 0; j < tSamples.length; j++ )
                mean += tSamples[j];
            mean /= nSamples;

            Arrays.sort( tSamples );
            double median = tSamples[nSamples / 2];
            double lb95 = tSamples[nSamples * 25 / 1000];
            double ub95 = tSamples[nSamples * 975 / 1000];
            
            result[transcript] = new double[] {mean, median, lb95, ub95};
        }
        return result;
    }
    
    private static class ReadsAndHits
    {
        public final ReadContainer reads;
        public final HitContainer hits;
        public ReadsAndHits(ReadContainer reads, HitContainer hits)
        {
            this.reads = reads;
            this.hits = hits;
        }
    }
    private ReadsAndHits parseBAMFile(File file, Map<String, Integer> transcriptIds)
    {
        try (SAMFileReader bamReader = new SAMFileReader( file ))
        {
            SAMFileHeader bamHeader = bamReader.getFileHeader();
            //if( bamHeader.getSortOrder() != SortOrder.queryname )
              //  throw new IllegalArgumentException( "Input bam files should be sorted by query name" );

            SAMSequenceDictionary sequenceDictionary = bamHeader.getSequenceDictionary();
            int[] referenceMap = new int[sequenceDictionary.size()];
            for( int i = 0; i < referenceMap.length; i++ )
            {
                String transcriptName = sequenceDictionary.getSequence( i ).getSequenceName();
                if(transcriptIds.containsKey( transcriptName ))
                    referenceMap[i] = transcriptIds.get( transcriptName );
                else
                    referenceMap[i] = -1;
            }

            List<Read> unmapped = new ArrayList<>();
            List<Read> mapped = new ArrayList<>();
            HitContainer hits = new HitContainer();

            SAMRecordIterator it = bamReader.iterator();
            String lastName = null;
            while( it.hasNext() )
            {
                SAMRecord bamRecord = it.next();
                Read read = new Read( bamRecord.getReadString() );

                if( bamRecord.getReadUnmappedFlag() )
                {
                    unmapped.add( read );
                    continue;
                }

                int referenceId = referenceMap[bamRecord.getReferenceIndex()];
                if(referenceId == -1)
                    continue;
                
                String name = bamRecord.getReadName();
                if( !name.equals( lastName ) )
                    hits.finishReadBucket();

                boolean forwardStrand = !bamRecord.getReadNegativeStrandFlag();
                int pos = forwardStrand ? bamRecord.getAlignmentStart() - 1 : bamRecord.getAlignmentEnd() - 1;
                Hit hit = new Hit( pos, referenceId, forwardStrand );
                hits.addHit( hit );
                mapped.add( read );

            }
            hits.finishReadBucket();
            bamReader.close();

            ReadReader<Read> mappedReader = new ReadsInMemory<>( mapped.toArray( new Read[0] ) );
            ReadReader<Read> unmappedReader = new ReadsInMemory<>( unmapped.toArray( new Read[0] ) );
            ReadContainer reads = new ReadContainer( mappedReader, unmappedReader, ReadReader.EMPTY );

            return new ReadsAndHits( reads, hits );
        }
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        public Parameters()
        {
            setTranscripts( new TranscriptSet() );
            transcripts.setOnlyProteinCoding(true);
        }
        
        private DataElementPath riboseqAlignments;
        @PropertyName ( "Ribo-seq alignments" )
        @PropertyDescription ( "Alignments of ribo-seq reads to the set of coding sequences, should include unmapped reads and sorted by read name" )
        public DataElementPath getRiboseqAlignments()
        {
            return riboseqAlignments;
        }
        public void setRiboseqAlignments(DataElementPath riboseqAlignments)
        {
            Object oldValue = this.riboseqAlignments;
            this.riboseqAlignments = riboseqAlignments;
            firePropertyChange( "riboseqAlignments", oldValue, riboseqAlignments );
        }
        public File getRiboseqBAMFile()
        {
            return getRiboseqAlignments().getDataElement( BAMTrack.class ).getBAMFile();
        }

        private DataElementPath mrnaseqAlignments;
        @PropertyName ( "mRNA-seq alignments" )
        @PropertyDescription ( "Alignments of mRNA-seq reads to the set of transcripts, should include unmapped reads and sorted by read name" )
        public DataElementPath getMrnaseqAlignments()
        {
            return mrnaseqAlignments;
        }
        public void setMrnaseqAlignments(DataElementPath mrnaseqAlignments)
        {
            Object oldValue = this.mrnaseqAlignments;
            this.mrnaseqAlignments = mrnaseqAlignments;
            firePropertyChange( "mrnaseqAlignments", oldValue, mrnaseqAlignments );
        }
        public File getMrnaseqBAMFile()
        {
            return getMrnaseqAlignments().getDataElement( BAMTrack.class ).getBAMFile();
        }

        private TranscriptSet transcripts;
        @PropertyName ( "Transcript set" )
        @PropertyDescription ( "Transcript set" )
        public TranscriptSet getTranscripts()
        {
            return transcripts;
        }
        public void setTranscripts(TranscriptSet transcripts)
        {
            TranscriptSet oldValue = this.transcripts;
            this.transcripts = withPropagation( oldValue, transcripts );
            firePropertyChange( "transcripts", oldValue, transcripts );
        }

        private int cdsOverhangs = 15;
        public int getCdsOverhangs()
        {
            return cdsOverhangs;
        }
        public void setCdsOverhangs(int cdsOverhangs)
        {
            int oldValue = this.cdsOverhangs;
            this.cdsOverhangs = cdsOverhangs;
            firePropertyChange( "cdsOverhangs", oldValue, cdsOverhangs );
        }
        
        private int nSamples = 50000;
        @PropertyName("Number of samples")
        @PropertyDescription("Number of samples taken by gibbs sampler")
        public int getNSamples()
        {
            return nSamples;
        }
        public void setNSamples(int nSamples)
        {
            int oldValue = this.nSamples;
            this.nSamples = nSamples;
            firePropertyChange( "nSamples", oldValue, nSamples );
        }

        private DataElementPath outputTable;
        @PropertyName ( "Output table" )
        @PropertyDescription ( "Output table" )
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
            property( "riboseqAlignments" ).inputElement( BAMTrack.class ).add();
            property( "mrnaseqAlignments" ).inputElement( BAMTrack.class ).add();
            add( "transcripts" );
            add( "cdsOverhangs" );
            addExpert( "nSamples" );
            property( "outputTable" ).outputElement( TableDataCollection.class ).add();
        }
    }
}
