package biouml.plugins.riboseq;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.riboseq.ingolia.AlignmentConverter;
import biouml.plugins.riboseq.ingolia.AlignmentOnTranscript;
import biouml.plugins.riboseq.transcripts.Transcript;
import biouml.plugins.riboseq.transcripts.TranscriptSet;
import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileHeader.SortOrder;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMSequenceRecord;
import net.sf.samtools.util.SequenceUtil;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.BAMTrack;
import ru.biosoft.bsa.ChrIntervalMap;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceCollection;
import ru.biosoft.bsa.SequenceImporter;
import ru.biosoft.bsa.importer.SAMBAMTrackImporter;
import ru.biosoft.bsa.importer.SAMBAMTrackImporter.ImporterProperties;
import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.bean.BeanInfoEx2;

public class GenomicAlignmentsToTranscriptomic extends AnalysisMethodSupport<GenomicAlignmentsToTranscriptomic.Parameters>
{
    public GenomicAlignmentsToTranscriptomic(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        List<Transcript> transcripts = parameters.getTranscriptSet().createTranscriptLoader().loadTranscripts( log );
        Map<String, Integer> chrLen = parameters.getTranscriptSet().getChromosomes().stream()
                .collect( Collectors.toMap( DataElement::getName, s -> s.getSequence().getLength() ) );
        transcripts = preprocessTranscripts( transcripts, chrLen );
        writeSequences(transcripts);
        ChrIntervalMap<Transcript> transcriptIndex = new ChrIntervalMap<>();
        for(Transcript t : transcripts)
            transcriptIndex.add( t.getChromosome(), t.getLocation().getFrom(), t.getLocation().getTo(), t );
        
        SAMFileWriterFactory factory = new SAMFileWriterFactory();
        SAMFileHeader header = new SAMFileHeader();
        header.setSortOrder( SortOrder.unsorted );
        for( Transcript t : transcripts )
        {
            if(parameters.isConvertToCDSOffsets() && !t.isCoding())
                continue;
            int len = t.getLength();
            if(parameters.isConvertToCDSOffsets())
                len = t.getCDSLocations().get( 0 ).getLength();
            header.addSequence( new SAMSequenceRecord( t.getName(), len ) );
        }
        BAMTrack inTrack = parameters.getInputBamTrack().getDataElement( BAMTrack.class );

        try (TempFile outBamFile = TempFiles.file( ".bam" ); SAMFileReader bamReader = new SAMFileReader( inTrack.getBAMFile() ))
        {
            SAMFileWriter bamWriter = factory.makeBAMWriter( header, false, outBamFile );
            try
            {
                for( SAMRecord r : bamReader )
                {
                    if(r.getReadUnmappedFlag())
                        continue;
                    Collection<Transcript> overlappingTranscripts = transcriptIndex.getIntervals( r.getReferenceName(),
                            r.getAlignmentStart() - 1, r.getAlignmentEnd() - 1 );
                    for( Transcript t : overlappingTranscripts )
                    {
                        SAMRecord transcriptAlignment = convertAlignment( r, t, header );
                        if( transcriptAlignment != null )
                            bamWriter.addAlignment( transcriptAlignment );
                    }
                }
            }
            finally
            {
                bamWriter.close();
            }

            DataElementPath outPath = parameters.getOutputBamTrack();
            SAMBAMTrackImporter importer = new SAMBAMTrackImporter();
            ImporterProperties properties = importer.getProperties( outPath.getParentCollection(), outBamFile, outPath.getName() );
            properties.setCreateIndex( false );
            return importer.doImport( outPath.getParentCollection(), outBamFile, outPath.getName(), null, log );
        }
    }

    private void writeSequences(List<Transcript> transcripts) throws Exception
    {
        DataElementPath outPath = parameters.getOutputSequences();
        if(outPath == null)
            return;
        DataCollection<AnnotatedSequence> chromosomes = parameters.getTranscriptSet().getChromosomes();
        try(TempFile outFasta = TempFiles.file( ".fa" );
             BufferedWriter writer = new BufferedWriter( new FileWriter( outFasta ) ))
        {
            for(Transcript t : transcripts)
            {
                writer.append( '>' ).append( t.getName() ).append( '\n' );
                Sequence chrSeq = chromosomes.get( t.getChromosome() ).getSequence();
                String tSeq = new String( t.getSequence( chrSeq ).getBytes() );
                if(parameters.isConvertToCDSOffsets())
                {
                    Interval cds = t.getCDSLocations().get( 0 );
                    tSeq = tSeq.substring( cds.getFrom(), cds.getTo() + 1 );
                }
                writer.append( tSeq ).append( '\n' );
            }
            writer.flush();
            SequenceImporter importer = new SequenceImporter();
            Properties properties = new Properties();
            properties.setProperty( DataElementImporter.SUFFIX, "fasta" );
            importer.init( properties  );
            importer.doImport( outPath.getParentCollection(), outFasta, outPath.getName(), null, log );
        }
    }

    private List<Transcript> preprocessTranscripts(List<Transcript> transcripts, Map<String, Integer> chrLen)
    {
        return StreamEx.of( transcripts ).filter( t->chrLen.containsKey( t.getChromosome() ) )
                .filter( t->!parameters.isConvertToCDSOffsets() || t.isCoding() ).map( t->{
            if(!t.isCoding()) return t;
            Interval cds = t.getCDSLocations().get( 0 );
            int len = chrLen.get( t.getChromosome() );
            
            int upstream = 0;
            if(cds.getFrom() < parameters.getCdsOverhangs())
                upstream = parameters.getCdsOverhangs() - cds.getFrom();
            if(t.isOnPositiveStrand() && t.getLocation().getFrom() - upstream < 0)
                upstream = t.getLocation().getFrom();
            if(!t.isOnPositiveStrand() && t.getLocation().getTo() + upstream > len - 1)
                upstream = len - t.getLocation().getTo() - 1;
            
            int downstream = 0;
            if(t.getLength() - cds.getTo() - 1 < parameters.getCdsOverhangs())
                downstream = parameters.getCdsOverhangs() - (t.getLength() - cds.getTo() - 1);
            if(t.isOnPositiveStrand() && t.getLocation().getTo() + downstream > len - 1)
                downstream = len - t.getLocation().getTo() - 1;
            if(!t.isOnPositiveStrand() && t.getLocation().getFrom() - downstream < 0)
                downstream = t.getLocation().getFrom();
            
            List<Interval> exons = t.isOnPositiveStrand() ? growIntervals( t.getExonLocations(), upstream, downstream )
                    : growIntervals( t.getExonLocations(), downstream, upstream );
            
            cds = cds.shift( upstream );
            int cFrom = Math.max( 0, cds.getFrom() - parameters.getCdsOverhangs() );
            int cTo = Math.min( t.getLength() + downstream + upstream - 1, cds.getTo() + parameters.getCdsOverhangs() );
            cds = new Interval( cFrom, cTo );
            
            int tFrom = exons.get( 0 ).getFrom();
            int tTo = exons.get( exons.size() - 1 ).getTo();
            Interval loc = new Interval(tFrom, tTo);
            
            return new Transcript( t.getName(), t.getChromosome(), loc, t.isOnPositiveStrand(), exons, Collections.singletonList( cds ) );
        } ).toList();
    }
    
    private static List<Interval> growIntervals(List<Interval> exons, int upstream, int downstream)
    {
        exons = new ArrayList<>( exons );
        Interval e = exons.get( 0 );
        exons.set( 0, new Interval(e.getFrom() - upstream, e.getTo()) );
        
        e = exons.get( exons.size() - 1 );
        e = new Interval(e.getFrom(), e.getTo() + downstream);
        exons.set( exons.size() - 1, e );
        
        return exons;
    }

    private SAMRecord convertAlignment(SAMRecord r, Transcript t, SAMFileHeader header)
    {
        if(parameters.isConvertToCDSOffsets() && !t.isCoding())
            return null;
        SAMRecord res = new SAMRecord( header );
        res.setReferenceName( t.getName() );
        boolean sameStrand = r.getReadNegativeStrandFlag() != t.isOnPositiveStrand();
        res.setReadNegativeStrandFlag( !sameStrand );
        byte[] bases = r.getReadBases().clone();
        byte[] quals = r.getBaseQualities().clone();
        if(!t.isOnPositiveStrand())
        {
            SequenceUtil.reverseComplement( bases );
            SequenceUtil.reverseQualities( quals );
        }
        res.setReadBases( bases );
        res.setBaseQualities( quals );
        
        res.setReadName( r.getReadName() );
        res.setMappingQuality( r.getMappingQuality() );
        res.setReadUnmappedFlag( r.getReadUnmappedFlag() );
        if(r.getReadUnmappedFlag())
            return res;

        
        AlignmentOnTranscript alignOnTranscript = AlignmentConverter.bamSiteToAlignment( r.getAlignmentStart() - 1, !r.getReadNegativeStrandFlag(), r.getCigar(), t, 0 );
        if(alignOnTranscript == null)
            return null;
        
        if(parameters.isConvertToCDSOffsets())
        {
            Interval cds = t.getCDSLocations().get( 0 );
            if(alignOnTranscript.getFrom() < cds.getFrom() || alignOnTranscript.getTo() > cds.getTo())
                return null;
            res.setAlignmentStart( alignOnTranscript.getFrom() - cds.getFrom() + 1);
        }
        else
            res.setAlignmentStart( alignOnTranscript.getFrom() +  1 );
        
        List<CigarElement> es = new ArrayList<>();
        for(CigarElement e : r.getCigar().getCigarElements())
            if(e.getOperator() != CigarOperator.N)
                es.add( e );
        es = mergeCigar(es);
        res.setCigar( new Cigar( es ) );
        
        return res;
    }

    private List<CigarElement> mergeCigar(List<CigarElement> cigar)
    {
        List<CigarElement> res = new ArrayList<>();
        for(int i = 0;i < cigar.size(); i++)
        {
            CigarOperator op = cigar.get( i ).getOperator();
            int len = cigar.get( i ).getLength();
            while(i+1 < cigar.size() && op == cigar.get( i+1 ).getOperator())
                len += cigar.get( ++i ).getLength();
            res.add( new CigarElement( len, op ) );
        }
        return res;
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath inputBamTrack;
        @PropertyName("Input bam track")
        public DataElementPath getInputBamTrack()
        {
            return inputBamTrack;
        }
        public void setInputBamTrack(DataElementPath inputBamTrack)
        {
            Object oldValue = this.inputBamTrack;
            this.inputBamTrack = inputBamTrack;
            firePropertyChange( "inputBamTrack", oldValue, inputBamTrack );
        }
        
        private TranscriptSet transcriptSet;
        {
            setTranscriptSet( new TranscriptSet() );
        }
        @PropertyName("Transcripts")
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
        
        private boolean convertToCDSOffsets;
        @PropertyName("Convert to CDS offsets")
        public boolean isConvertToCDSOffsets()
        {
            return convertToCDSOffsets;
        }
        public void setConvertToCDSOffsets(boolean convertToCDSOffsets)
        {
            boolean oldValue = this.convertToCDSOffsets;
            this.convertToCDSOffsets = convertToCDSOffsets;
            transcriptSet.setOnlyProteinCoding( convertToCDSOffsets );
            firePropertyChange( "convertToCDSOffsets", oldValue, convertToCDSOffsets );
        }
        
        private int cdsOverhangs;
        @PropertyName("CDS overhangs")
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
        
        private DataElementPath outputBamTrack;
        @PropertyName("Output bam track")
        public DataElementPath getOutputBamTrack()
        {
            return outputBamTrack;
        }
        public void setOutputBamTrack(DataElementPath outputBamTrack)
        {
            Object oldValue = this.outputBamTrack;
            this.outputBamTrack = outputBamTrack;
            firePropertyChange( "outputBamTrack", oldValue, outputBamTrack );
        }
        
        private DataElementPath outputSequences;
        @PropertyName("Output sequences")
        public DataElementPath getOutputSequences()
        {
            return outputSequences;
        }
        public void setOutputSequences(DataElementPath outputSequences)
        {
            Object oldValue = this.outputSequences;
            this.outputSequences = outputSequences;
            firePropertyChange( "outputSequences", oldValue, outputSequences );
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
            property("inputBamTrack").inputElement( BAMTrack.class ).add();
            add("transcriptSet");
            add("convertToCDSOffsets");
            add( "cdsOverhangs" );
            property("outputBamTrack").outputElement( BAMTrack.class ).add();
            property("outputSequences").outputElement( SequenceCollection.class ).canBeNull().add();
        }
    }
}
