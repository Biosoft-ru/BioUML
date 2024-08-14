package biouml.plugins.ensembl.analysis.mutationeffect;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.plugins.ensembl.access.EnsemblSequenceTransformer;
import biouml.plugins.ensembl.type.Exon;
import one.util.streamex.IntStreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AminoAcidsAlphabet;
import ru.biosoft.bsa.DiscontinuousCoordinateSystem;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.IntervalMap;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.VCFSqlTrack;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.util.Pair;
import ru.biosoft.util.TextUtil;

@ClassIcon ( "resources/mutation-effect.gif" )
public class Analysis extends AnalysisMethodSupport<Parameters>
{
    public Analysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new Parameters());
    }

    @Override
    public Track justAnalyzeAndPut() throws Exception
    {
        SqlTrack inputTrack = parameters.getInputTrack().getDataElement(SqlTrack.class);

        DataCollection<?> sequencesCollection = parameters.getGenome().getSequenceCollection();

        jobControl.pushProgress(0, 20);
        log.info("Loading translation information for " + sequencesCollection.getName());
        List<Translation> translations = loadTranslations(sequencesCollection);
        jobControl.popProgress();

        jobControl.pushProgress(20, 30);
        log.info("Indexing coding sequences");
        final Map<String, IntervalMap<Translation>> codingRegionsIndices = new HashMap<>();
        for( Translation tr : translations )
        {
            String chromosome = tr.chromosome.getName();
            IntervalMap<Translation> forChromosome = codingRegionsIndices.get(chromosome);
            if( forChromosome == null )
                codingRegionsIndices.put(chromosome, forChromosome = new IntervalMap<>());
            for( Interval cr : tr.getCodingIntervals() )
                forChromosome.add(cr.getFrom(), cr.getTo(), tr);
        }
        jobControl.popProgress();

        final SqlTrack result = SqlTrack.createTrack(parameters.getOutputTrack(), inputTrack, VCFSqlTrack.class);

        jobControl.pushProgress(30, 100);
        log.info("Determining mutation types");

        jobControl.forCollection(DataCollectionUtils.asCollection(inputTrack.getAllSites(), Site.class), element -> {
            try
            {
                Site newSite = new SiteImpl(result, null, element.getType(), element.getBasis(), element.getStart(),
                        element.getLength(), element.getPrecision(), element.getStrand(), element.getOriginalSequence(),
                        element.getComment(), (DynamicPropertySet)element.getProperties().clone());

                Set<String> annotations = new TreeSet<>();

                String chromosome = element.getOriginalSequence().getName();
                IntervalMap<Translation> index = codingRegionsIndices.get(chromosome);
                if( index != null )
                {
                    String refAllel = element.getProperties().getValueAsString( "RefAllele" );
                    String altAllelStr = element.getProperties().getValueAsString( "AltAllele" );
                    if(refAllel == null || altAllelStr == null)
                    {
                        log.log( Level.SEVERE,
                                "RefAllele or AltAllele properties not found, please use VCF (variant call format) file as input" );
                        return false;
                    }
                    String[] altAllels = TextUtil.split( altAllelStr, ',' );

                    int start = element.getStart();
                    Collection<Translation> translations1 = index.getIntervals(start);//index.getIntervals(start, start + refAllel.length() - 1);

                    for( String altAllel : altAllels )
                        for( Translation tr : translations1 )
                        {
                            Set<MutationType> mutTypes = getMutationEffectType(tr, start, refAllel, altAllel);
                            for(MutationType mutType : mutTypes)
                                annotations.add(mutType.toString());
                        }

                }
                if( annotations.isEmpty() )
                    annotations.add(MutationType.NOTHING.toString());
                newSite.getProperties().add(
                        new DynamicProperty("MutationEffect", String.class, String.join(", ", annotations)));
                result.addSite(newSite);
            }
            catch( Exception e )
            {
                throw ExceptionRegistry.translateException(e);
            }
            return true;
        });

        if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST || jobControl.getStatus() == JobControl.TERMINATED_BY_ERROR )
        {
            result.getOrigin().remove(result.getName());
            return null;
        }
        result.finalizeAddition();

        log.info("Track created (" + result.getAllSites().getSize() + " sites)");
        jobControl.popProgress();

        return result;
    }

    private static class Translation
    {
        private final Sequence chromosome;
        private final DiscontinuousCoordinateSystem coordinateSystem;

        /** How many nucleotides at the beginning of translation correspond to incomplete codon (0 or 1 or 2) */
        private int phase;

        /**
         * @param exons
         * @param startExonId           first coding exon (where translation begins)
         * @param offsetInFirstExon     one based offset in first coding exon of first translated nucleotide
         * @param endExonId             last coding exon (where translation ends)
         * @param offsetInLastExon      one based offset in last coding exon of last translated nucleotide
         */
        public Translation(List<Exon> exons, int startExonId, int offsetInFirstExon, int endExonId, int offsetInLastExon)
        {
            if( exons.isEmpty() )
                throw new IllegalArgumentException();

            chromosome = exons.get(0).getOriginalSequence();
            int strand = exons.get(0).getStrand();
            if( strand != StrandType.STRAND_PLUS && strand != StrandType.STRAND_MINUS )
                throw new IllegalArgumentException("Undefined strand");

            for( Exon e : exons )
                if( e.getOriginalSequence() != chromosome || e.getStrand() != strand )
                    throw new IllegalArgumentException("All exons in translation should have the same chromosome and strand");

            List<Interval> codingIntervals = new ArrayList<>();

            int startExonIdx = findExonIdx(exons, startExonId);
            int endExonIdx = findExonIdx(exons, endExonId);
            if( startExonIdx > endExonIdx )
                throw new IllegalArgumentException("Invalid exon order");

            for( int i = startExonIdx; i <= endExonIdx; i++ )
            {

                Exon e = exons.get(i);
                int cdsStart = e.getFrom();
                int cdsEnd = e.getTo();

                if( i == startExonIdx )
                {

                    if( strand == StrandType.STRAND_PLUS )
                        cdsStart = e.getFrom() + offsetInFirstExon - 1;
                    else
                        cdsEnd = e.getTo() - offsetInFirstExon + 1;
                }

                if( i == endExonIdx )
                {
                    if( strand == StrandType.STRAND_PLUS )
                        cdsEnd = e.getFrom() + offsetInLastExon - 1;
                    else
                        cdsStart = e.getTo() - offsetInLastExon + 1;
                }

                if( cdsStart > cdsEnd )
                    throw new IllegalArgumentException("Invalid exon (" + e.getId() + ") " + cdsStart + " > " + cdsEnd);

                codingIntervals.add(new Interval(cdsStart, cdsEnd));
            }

            coordinateSystem = new DiscontinuousCoordinateSystem(codingIntervals, strand == StrandType.STRAND_MINUS);

            Interval firstCDS = codingIntervals.get(0);
            Exon firstExon = exons.get(startExonIdx);

            if( firstExon.getStartPhase() != -1 && offsetInFirstExon == 1 )
                phase = ( 3 - firstExon.getStartPhase() ) % 3;
            else if( firstExon.getEndPhase() != -1 )
                phase = ( firstCDS.getLength() - firstExon.getEndPhase() + 3 ) % 3;

/*
            if( phase != coordinateSystem.getLength() % 3 )
                throw new IllegalArgumentException("Invalid coding sequence length");
*/
            
            initLastJunction(exons);
        }

        private static int findExonIdx(List<Exon> exons, int id)
        {
            return IntStreamEx.ofIndices( exons, exon -> exon.getId() == id ).findAny()
                    .orElseThrow( () -> new IllegalArgumentException( "Exon (" + id + ") not found" ) );
        }

        /**
         * @param position      position in chromosome
         * @return zero based offset in coding sequence that correspond to the given position in chromosome
         */
        private int getOffsetInCodingSequence(int position)
        {
            return coordinateSystem.translateCoordinate(position);
        }

        /**
         * Finds position of first nucleotide in codon
         * @param offset      zero based offset in coding sequence
         * @return zero based offset in coding sequence that correspond to the codon start,
         *         can be negative if first codon is incomplete
         */
        private int getCodonStartPosition(int offset)
        {
            if( offset < 0 || offset >= coordinateSystem.getLength() )
                throw new IndexOutOfBoundsException();
            int posInCodon = ( offset - phase + 3 ) % 3;
            return offset - posInCodon;
        }

        /**
         * position  chromosome position
         * @return Pair where first is
         *           3-letter string corresponding to codon in given chromosome position
         *           or 1,2-letter string if codon is incomplete
         *           or null if there is no codon at position
         *         and second is position offset in codon
         */
        public Pair<String, Integer> getCodon(int position)
        {
            int offsetInCDS;
            try
            {
                offsetInCDS = getOffsetInCodingSequence(position);
            }
            catch( IndexOutOfBoundsException e )
            {
                //position is not coding
                return null;
            }

            int codonPositionInCodingSequence = getCodonStartPosition(offsetInCDS);

            StringBuilder sb = new StringBuilder();

            for( int i = Math.max(0, codonPositionInCodingSequence); i < Math.min(codonPositionInCodingSequence + 3, coordinateSystem
                    .getLength()); i++ ) {
                byte letter = chromosome.getLetterAt(coordinateSystem.translateCoordinateBack(i));
                if(coordinateSystem.isReverse())
                    letter = chromosome.getAlphabet().letterComplementMatrix()[letter];
                sb.append((char)letter);
            }

            return new Pair<>(sb.toString(), offsetInCDS - codonPositionInCodingSequence);
        }

        public Collection<? extends Interval> getCodingIntervals()
        {
            return coordinateSystem.getIntervals();
        }

        public boolean isReverse()
        {
            return coordinateSystem.isReverse();
        }
        
        private DiscontinuousCoordinateSystem exonsCoordSystem;
        private int lastJunctionPosition;//position in transcript just before last junction (zero based)
        private void initLastJunction(List<Exon> exons)
        {
            if(exons.size() == 1)
                lastJunctionPosition = -1;
            List<Interval> exonIntervals = new ArrayList<>(exons.size());
            
            boolean reverse = false;
            for(Exon e : exons)
            {
                exonIntervals.add( e.getInterval() );
                reverse = e.getStrand()==StrandType.STRAND_MINUS;
            }
            exonsCoordSystem = new DiscontinuousCoordinateSystem( exonIntervals, reverse );
            
            
            Exon lastExon = exons.get( exons.size() - 1 );
            int chrPosAfterJunction = reverse ? lastExon.getTo() : lastExon.getFrom();
            
            lastJunctionPosition = exonsCoordSystem.translateCoordinate( chrPosAfterJunction ) - 1;
        }
        
        /**
         * @param position - chromosome position
         * @return 
         */
        public Integer getDistanceToLastJunction(int position)
        {
            if(exonsCoordSystem == null)
                return null;
            int transcriptOffset = exonsCoordSystem.translateCoordinate( position );
            return lastJunctionPosition - transcriptOffset;
        }
    }

    public Set<MutationType> getMutationEffectType(Translation translation, int start, String refAllele, String altAllele)
    {
        AminoAcidsAlphabet alphabet = AminoAcidsAlphabet.getInstance();

        refAllele = refAllele.toLowerCase();
        altAllele = altAllele.toLowerCase();
        
        
        Set<MutationType> result = EnumSet.noneOf( MutationType.class );

        if( refAllele.length() == altAllele.length() ) //substitution
        {
            if( refAllele.length() == 1 ) //SNV
            {
                Pair<String, Integer> codon = translation.getCodon(start);

                if( codon.getFirst().length() != 3 )
                    //incomplete codon
                    return result;

                String refCodon = codon.getFirst().toLowerCase();
                int variantPositionInCodon = codon.getSecond();

                char refLetter = refAllele.charAt(0);
                char altLetter = altAllele.charAt(0);
                if(translation.isReverse())
                {
                    byte[] complement = translation.chromosome.getAlphabet().letterComplementMatrix();
                    refLetter = (char)complement[(byte)refLetter];
                    altLetter = (char)complement[(byte)altLetter];
                }

                if( refLetter != refCodon.charAt(variantPositionInCodon) )
                    throw new IllegalArgumentException("Invalid reference allele");

                StringBuilder sb = new StringBuilder(refCodon);
                sb.setCharAt(variantPositionInCodon, altLetter);
                String altCodon = sb.toString();

                byte refAminoAcid = alphabet.getAminoAcidForTriplet(refCodon);
                byte altAminoAcid = alphabet.getAminoAcidForTriplet(altCodon);
                if( refAminoAcid == altAminoAcid )
                    result.add( MutationType.SYNONYNYMOUS_SNV );
                else if( refAminoAcid == -1 )
                    result.add( MutationType.STOP_LOSS );
                else if( altAminoAcid == -1 )
                {
                    result.add( MutationType.STOP_GAIN );
                    Integer distance = translation.getDistanceToLastJunction( start );
                    distance += variantPositionInCodon;//distance from first letter of codon
                    if(distance != null && distance >= 50)
                        result.add( MutationType.NONSENSE_MEDIATED_DECAY );
                }
                else
                    result.add( MutationType.NONSYNONYMOUS_SNV );
            }
            else
            //block substitution
            {

            }
        }
        else if( refAllele.length() < altAllele.length() ) //insertion
        {

        }
        else
        //deletion
        {

        }
        
        return result;
    }


    private List<Translation> loadTranslations(@Nonnull DataCollection<?> ensemblSequenceCollection) throws Exception
    {
        String constraints = EnsemblSequenceTransformer.getConstraints(ensemblSequenceCollection);
        Connection con = SqlConnectionPool.getConnection(ensemblSequenceCollection);

        List<Translation> result = new ArrayList<>();
        int invalidTranslations = 0;
        try( Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(
                        "SELECT translation_id, seq_start, seq_end, name, seq_region_start, seq_region_end, seq_region_strand, exon_id, start_exon_id, end_exon_id, phase, end_phase FROM translation JOIN exon_transcript USING(transcript_id) JOIN exon USING(exon_id) JOIN seq_region USING(seq_region_id) "
                                + constraints + " ORDER BY translation_id, exon_transcript.rank" ))
        {
            boolean hasNext = rs.next();
            while( hasNext )
            {
                List<Exon> exons = new ArrayList<>();
                int translationId;
                Sequence seq = null;
                int firstExonId, lastExonId;
                int offsetInFirstExon, offsetInLastExon;
                do
                {
                    translationId = rs.getInt(1);
                    offsetInFirstExon = rs.getInt(2);
                    offsetInLastExon = rs.getInt(3);
                    String chr = rs.getString(4);
                    int exonStart = rs.getInt(5);
                    int exonEnd = rs.getInt(6);
                    int strand = rs.getInt(7);
                    int exonId = rs.getInt(8);
                    firstExonId = rs.getInt(9);
                    lastExonId = rs.getInt(10);
                    int phase = rs.getInt(11);
                    int endPhase = rs.getInt(12);

                    if( exonStart > exonEnd )
                        throw new Exception("Invalid exon in " + translationId + " translation");

                    if( seq == null )
                        seq = ( (ru.biosoft.bsa.AnnotatedSequence)ensemblSequenceCollection.get(chr) ).getSequence();

                    exons.add( new Exon(null, exonId, seq, exonStart, exonEnd, strand == 1, phase, endPhase) );
                }
                while( ( hasNext = rs.next() ) && rs.getInt(1) == translationId );

                Translation t = null;
                try
                {
                    t = new Translation(exons, firstExonId, offsetInFirstExon, lastExonId, offsetInLastExon);
                }catch(IllegalArgumentException e)
                {
                    if(invalidTranslations < 10)
                        log.log( Level.WARNING, "Invalid translation", e );
                    invalidTranslations++;
                }

                if(t != null)
                    result.add(t);
            }
        }

        if(invalidTranslations > 0)
            log.warning( "Total invalid translations " + invalidTranslations );

        return result;
    }

}
