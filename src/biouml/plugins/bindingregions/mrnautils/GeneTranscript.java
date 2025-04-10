
package biouml.plugins.bindingregions.mrnautils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.ArrayUtils;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.analysis.IPSSiteModel;
import ru.biosoft.util.TextUtil2;
import biouml.plugins.bindingregions.utils.EnsemblUtils;
import biouml.plugins.bindingregions.utils.LinearRegression;
import biouml.plugins.bindingregions.utils.MatrixUtils;
import biouml.plugins.bindingregions.utils.Olig;
import biouml.plugins.bindingregions.utils.EnsemblUtils.Gene;
import biouml.plugins.bindingregions.utils.SitePrediction;
import biouml.standard.type.Transcript;

/**
 * @author yura
 *
 */

public class GeneTranscript
{
    static final byte[] ATG = new byte[]{'a', 't', 'g'};
    private static final byte[][] STOP_CODONS = new byte[][]{{'t', 'a', 'g'}, {'t', 'a', 'a'}, {'t', 'g', 'a'}};
    private static final String TRANSCRIPT_LENGTH = "length of transcript";
    private static final String LG_TRANSCRIPT_LENGTH = "lg(length of transcript)";
    private static final String LEADER_LENGTH = "5'UTR length";
    private static final String LG_LEADER_LENGTH = "lg(5'UTR length)";
    private static final String TRAILER_LENGTH = "3'UTR length";
    private static final String LG_TRAILER_LENGTH = "lg(3'UTR length)";
    private static final String LG_CDS_LENGTH = "lg(CDS length)";
    private static final String CDS_LENGTH = "CDS length";
    private static final String CG_CONTENT_IN_LIDER = "(C,G)-content in 5'UTR";
    private static final String CG_CONTENT_IN_TRANSCRIPT = "(C,G)-content in full transcript";
    private static final String CG_CONTENT_IN_TRAILER = "(C,G)-content in 3'UTR";
    private static final String CG_CONTENT_IN_CDS = "(C,G)-content in CDS";
    private static final String AUG_CONCENTRATION = "concentration of AUGs in 5'UTR";
    private static final String POSITION_MINUS_3 = "A or G on position -3";
    private static final String POSITION_PLUS_4 = "G on position +4";
    private static final String UPSTREAM_COMPLEMENTARITY_INDEX = "complementarity index on [-15, 1]-positions";
    private static final String DOWNSTREAM_COMPLEMENTARITY_INDEX = "complementarity index on [1, 20]-positions";
    private static final String COMPLEMENTARITY_INDEX2_FOR_TRANSCRIPT = "correlation-based complementarity index for full transcript";
    private static final String COMPLEMENTARITY_INDEX2_FOR_LIDER = "correlation-based complementarity index for 5'UTR";
    private static final String COMPLEMENTARITY_INDEX2_FOR_CDS = "correlation-based complementarity index for CDS";
    private static final String COMPLEMENTARITY_INDEX2_FOR_TRAILER = "correlation-based complementarity index for 3'UTR";
    public static final String OLIG_SET1 = "set of pre-specified oligonucleotides on [-100, 0]-positions";
    public static final String OLIG_SET2 = "set of pre-specified oligonucleotides on [3, 103]-positions";
    static final String[] OLIGS1 = {"gccgcc", "cgccgc", "ccgcca", "ccgccg", "ccgcgc", "cccgcc", "ctccgc", "cgcgcc"};
    static final String[] OLIGS2 = {"aagaag", "gaagaa", "caagaa", "agaagc", "ccaaga", "aagcag", "agaaga", "aagaaa", "aaaaag", "aagaac"};
    private static final String OLIG_CCGCCA = Olig.OLIGONUCLEOTIDE + "ccgcca_-100_0";
    private static final String OLIG_CAAGAA = Olig.OLIGONUCLEOTIDE + "caagaa_3_103";
    private static final String HAIRPIN_CONCENTRATION_IN_TRANSCRIPT = "concentration of hairpins in full transcript";
    private static final String HAIRPIN_CONCENTRATION_IN_LIDER = "concentration of hairpins in 5'UTR";
    private static final String HAIRPIN_CONCENTRATION_IN_CDS = "concentration of hairpins in CDS";
    private static final String HAIRPIN_CONCENTRATION_IN_TRAILER = "concentration of hairpins in3'UTR";
     
    public static final String MATRICES_SCORES = "Scores of matrices";
    public static final String FEATURE_MATRIX = "Matrix";
    public static final String MATRIX_FOR_START_CODON = FEATURE_MATRIX + " for start codons ";
    public static final String MATRIX_FOR_STOP_CODON = FEATURE_MATRIX + " for stop codons ";
    
    private static final int MINIMAL_UTR_LENGTH = 10;
    public static final String CANONICAL_START_CODON = "Only canonical start codons";
    public static final String NONCANONICAL_START_CODON = "Only non-canonical start codons";
    public static final String EACH_START_CODON = "All start codons";
    
    private final String transcriptName;
    private final String chromosome;
    private final Interval interval;  // positions 'from' and 'to' of transcript on chromosome
    private final String geneID;
    private final String geneType;
    private final int strand;
    private Interval cdsFromAndTo;  // 'from' = relative 1-st position of coding region (within mRNA); 'from' >= 0; 'to' = relative last position of coding region; 'to' > 'from'
    private final Interval[] exonPositions; // chromosomal positions of exons;
    /// private Interval[] exonPositions : !!!!!!!!!! in the case of dataset = INGOLIA_GSE30839_DATA_SET exonPositions = new Interval[]{1, processed transcriptLength};
    /// private Interval[] exonPositions : !!!!!!!!!! where processed transcriptLength is given from 'pathToSequences = pathToData.getChildPath("old_seq_ingolia")';
    private Map<String, Double> nameAndValueOfRiboSeqFeature;

    public GeneTranscript(String transcriptName, String chromosome, Interval interval, String geneID, String geneType, int strand, Interval cdsFromAndTo, Interval[] exonPositions, Map<String, Double> nameAndValueOfRiboSeqFeature)
    {
        this.transcriptName = transcriptName;
        this.chromosome = chromosome;
        this.interval = interval;
        this.geneID = geneID;
        this.geneType = geneType;
        this.strand = strand;
        this.cdsFromAndTo = cdsFromAndTo;
        this.exonPositions = exonPositions;
        this.nameAndValueOfRiboSeqFeature = nameAndValueOfRiboSeqFeature;
    }
    
    public void setCdsFromAndTo (Interval cdsFromAndTo)
    {
        this.cdsFromAndTo = cdsFromAndTo;
    }
    
    public void setNameAndValueOfRiboSeqFeature(Map<String, Double> nameAndValueOfRiboSeqFeature)
    {
        this.nameAndValueOfRiboSeqFeature = nameAndValueOfRiboSeqFeature;
    }
    
    public Map<String, Double> getNameAndValueOfRiboSeqFeature()
    {
        return nameAndValueOfRiboSeqFeature;
    }

    public static String[] getMrnaFeatureNames()
    {
        return new String[]{MATRICES_SCORES, CG_CONTENT_IN_LIDER, CG_CONTENT_IN_TRANSCRIPT, CG_CONTENT_IN_TRAILER, AUG_CONCENTRATION, POSITION_MINUS_3, POSITION_PLUS_4, UPSTREAM_COMPLEMENTARITY_INDEX, DOWNSTREAM_COMPLEMENTARITY_INDEX, TRANSCRIPT_LENGTH, LG_TRANSCRIPT_LENGTH, LEADER_LENGTH, LG_LEADER_LENGTH, TRAILER_LENGTH, LG_TRAILER_LENGTH, CDS_LENGTH, LG_CDS_LENGTH, OLIG_CCGCCA, OLIG_CAAGAA, CG_CONTENT_IN_CDS, COMPLEMENTARITY_INDEX2_FOR_TRANSCRIPT, COMPLEMENTARITY_INDEX2_FOR_LIDER, COMPLEMENTARITY_INDEX2_FOR_CDS, COMPLEMENTARITY_INDEX2_FOR_TRAILER, HAIRPIN_CONCENTRATION_IN_TRANSCRIPT, HAIRPIN_CONCENTRATION_IN_LIDER, HAIRPIN_CONCENTRATION_IN_CDS, HAIRPIN_CONCENTRATION_IN_TRAILER};
    }
    
    public static boolean isMrnaFeatureName(String featureName)
    {
        String[] mrnaFeatureNames = getMrnaFeatureNames();
        return ArrayUtils.contains(mrnaFeatureNames, featureName) || featureName.contains(Olig.OLIGONUCLEOTIDE) || featureName.contains(FEATURE_MATRIX);
    }
    
    public String getTranscriptName()
    {
        return transcriptName;
    }
    
    public String getChromosome()
    {
        return chromosome;
    }
    
    public Interval getInterval()
    {
        return interval;
    }
    
    public String getGeneID()
    {
        return geneID;
    }
    
    public String getGeneType()
    {
        return geneType;
    }
    
    public int getStrand()
    {
        return strand;
    }
    
    public Integer getChromosomalCDSstartPosition()
    {
        if( cdsFromAndTo == null || exonPositions == null || strand == 0 ) return null;
        int exonNumber = 0, pos = cdsFromAndTo.getFrom();
        for( Interval exon : exonPositions )
            if( pos >= exon.getLength() )
            {
                exonNumber++;
                pos -= exon.getLength();
            }
            else break;
        return strand == 2 ? exonPositions[exonNumber].getFrom() + pos : exonPositions[exonNumber].getTo() - pos;
    }
    
    public Double getLiderLength()
    {
        if( cdsFromAndTo == null ) return null;
        double length = cdsFromAndTo.getFrom();
        if( length < MINIMAL_UTR_LENGTH ) return null;
        else return length;
    }
    
    public Double getLiderLengthLG()
    {
        Double length = getLiderLength();
        if( length == null ) return null;
        else return Math.log10(length);
    }
    
    public Interval getCdsInterval()
    {
        return cdsFromAndTo;
    }
    
    public Interval[] getExonPositions()
    {
        return exonPositions;
    }

    public byte[] getTranscriptSequence(DataElementPath pathToSequences) throws Exception
    {
        byte[] result = null;
        if( transcriptName.contains(":") )
            result = EnsemblUtils.getSequenceFromFastaTrack(pathToSequences, TextUtil2.split(transcriptName, ':')[0]);
        else
        {
            Sequence fullChromosome = pathToSequences.getChildPath(chromosome).getDataElement(AnnotatedSequence.class).getSequence();
            for( Interval exonInterval : exonPositions )
            {
                Sequence sequence = new SequenceRegion(fullChromosome, exonInterval, false, false);
                if( strand == 3 )
                    sequence = SequenceRegion.getReversedSequence(sequence);
                result = ArrayUtils.addAll(result, sequence.getBytes());
            }
            result = new String(result).toLowerCase().getBytes();
        }
        return result;
    }
    
    public Double getTranscriptLength()
    {
        if( exonPositions == null ) return null;
        Double result = 0.0;
        for( Interval interval : exonPositions )
            result += interval.getLength();
        return result <= 0.0 ? null : result;
    }
    
    public Double geTranscriptLengthLG()
    {
        Double length = getTranscriptLength();
        return length == null || length < 1 ? null : Math.log10(length);
    }

    private Double getTrailerLength()
    {
        Double length = getTranscriptLength();
        if( length == null || cdsFromAndTo == null ) return null;
        Double trailerLength = length - cdsFromAndTo.getTo() - 1;
        return trailerLength < MINIMAL_UTR_LENGTH ? null : trailerLength;
    }
    
    /////////////////////OOOOOOO///////////
    private Double getTrailerLengthLG()
    {
        Double length = getTrailerLength();
        return length == null || length <= 0 ? null : Math.log10(length);
    }
    
    /////// is necessary now?
    public byte[] getCdsSequence(DataElementPath pathToSequences) throws Exception
    {
        if( cdsFromAndTo == null ) return null;
        byte[] seq = new byte[cdsFromAndTo.getLength()];
        byte[] transcriptSeq = getTranscriptSequence(pathToSequences);
        int cdsFrom = cdsFromAndTo.getFrom();
        for( int i = 0; i < cdsFromAndTo.getLength(); i++ )
            seq[i] = transcriptSeq[i + cdsFrom];
        return seq;
    }
    
    public Double getCdsLength()
    {
        return cdsFromAndTo == null ? null : (double)cdsFromAndTo.getLength();
    }
    
    public Double getCdsLengthLG()
    {
        Double length = getCdsLength();
        return length == null || length <= 0 ? null : Math.log10(length);
    }

    /////////////////////OOOOOOOOOOOOO/////////////////
    private Double getGCcontentInTrailer(byte[] transcriptSequence)
    {
        if( cdsFromAndTo == null ) return null;
        int result = 0;
        for( int i = cdsFromAndTo.getTo() + 1; i < getTranscriptLength(); i++ )
            if( transcriptSequence[i] == 'c' || transcriptSequence[i] == 'g' || transcriptSequence[i] == 'C' || transcriptSequence[i] == 'G')
                result++;
        Double trailerLength = getTrailerLength();
        return trailerLength == null || trailerLength < MINIMAL_UTR_LENGTH ? null : result / trailerLength;
    }
    
    //////////////////////OOOOOOOOOOOOO//////////////////
    private Double getGCcontentInTranscript(byte[] transcriptSequence)
    {
        double result = 0;
        if( transcriptSequence == null || transcriptSequence.length == 0 ) return null;
        for( byte seqElement : transcriptSequence )
            if( seqElement == 'c' || seqElement == 'g' || seqElement == 'C' || seqElement == 'G')
                result++;
        return result / transcriptSequence.length;
    }

    //////OOOOO//////
    private Double getGCcontentInLider(byte[] transcriptSequence)
    {
        if( cdsFromAndTo == null || transcriptSequence == null || cdsFromAndTo.getFrom() < MINIMAL_UTR_LENGTH ) return null;
        int result = 0;
        for( int j  = 0; j < cdsFromAndTo.getFrom(); j++ )
            if( transcriptSequence[j] == 'c' || transcriptSequence[j] == 'g' || transcriptSequence[j] == 'C' || transcriptSequence[j] == 'G')
                result++;
        return (double)result / cdsFromAndTo.getFrom();
    }
    
    private Double getConcentrationOfHairpins(byte[] transcriptSequence, int startPosition, int fragmentLength, int minSteamLength, int maxLoopLength)
    {
        List<Interval[]> list = getHairpins(transcriptSequence, startPosition, fragmentLength, minSteamLength, maxLoopLength);
        return (double)list.size() / (double)fragmentLength;
    }

    private Double getConcentrationOfHairpins(String typeOfTranscriptFragment, byte[] transcriptSequence, int minSteamLength, int maxLoopLength)
    {
        if( transcriptSequence == null ) return null;
        int startPosition = 0, fragmentLength = 0;
        switch( typeOfTranscriptFragment )
        {
            case HAIRPIN_CONCENTRATION_IN_TRANSCRIPT : fragmentLength = transcriptSequence.length; break;
            case HAIRPIN_CONCENTRATION_IN_LIDER      : getLiderLength();
                                                       if( cdsFromAndTo == null || cdsFromAndTo.getFrom() < MINIMAL_UTR_LENGTH ) return null;
                                                       fragmentLength = cdsFromAndTo.getFrom(); break;
            case HAIRPIN_CONCENTRATION_IN_CDS        : if( cdsFromAndTo == null ) return null;
                                                       startPosition = cdsFromAndTo.getFrom();
                                                       fragmentLength = cdsFromAndTo.getLength(); break;
            case HAIRPIN_CONCENTRATION_IN_TRAILER    : if( cdsFromAndTo == null ) return null;
                                                       startPosition = cdsFromAndTo.getTo() + 1;
                                                       fragmentLength = transcriptSequence.length - startPosition;
                                                       if( fragmentLength < MINIMAL_UTR_LENGTH ) return null; break;
            default                                  : return null;
        }
        return getConcentrationOfHairpins(transcriptSequence, startPosition, fragmentLength, minSteamLength, maxLoopLength);
    }
    
    private Double getGCcontentInCDS(byte[] transcriptSequence)
    {
        if( cdsFromAndTo == null || transcriptSequence == null ) return null;
        int result = 0;
        for( int j  = cdsFromAndTo.getFrom(); j <= cdsFromAndTo.getTo(); j++ )
            if( transcriptSequence[j] == 'c' || transcriptSequence[j] == 'g' || transcriptSequence[j] == 'C' || transcriptSequence[j] == 'G')
                result++;
        return result / ( cdsFromAndTo.getTo() - cdsFromAndTo.getFrom() + 1.0 );
    }
    
    // it is public temporary
    public int getNumberOfAUGinLider(byte[] transcriptSequence)
    {
        int result = 0;
        for( int i = 0; i < cdsFromAndTo.getFrom(); i++ )
            if( Olig.isGivenOlig(transcriptSequence, i, ATG) )
                result++;
        return result;
    }

    private Double getConcentrationOfAUGinLider(byte[] transcriptSequence)
    {
        if( cdsFromAndTo == null || cdsFromAndTo.getFrom() < MINIMAL_UTR_LENGTH ) return null;
        return getNumberOfAUGinLider( transcriptSequence ) / ( cdsFromAndTo.getFrom() - 2.0 );
    }

    ///////////////////////////////OOOO////////////////////////////////////
    private Boolean isAorGonPositionMinus3(byte[] transcriptSequence)
    {
        if( cdsFromAndTo == null || cdsFromAndTo.getFrom() < 3) return null;
        if( cdsFromAndTo.getFrom() >= 3 )
        {
            byte symbol = transcriptSequence[cdsFromAndTo.getFrom() - 3];
            if( symbol == 'a' || symbol == 'A' || symbol == 'g' || symbol == 'G')
                return true;
        }
        return false;
    }
    
    ///////////////////////////////OOOO////////////////////////////////////
    Boolean isGonPositionPlus4(byte[] transcriptSequence)
    {
        if( cdsFromAndTo == null || cdsFromAndTo.getFrom() + 3 >= transcriptSequence.length) return null;
        if( cdsFromAndTo.getFrom() + 3 < transcriptSequence.length )
            if( transcriptSequence[cdsFromAndTo.getFrom() + 3] == 'g')
                return true;
        return false;
    }
    
    ///////////////////////////////OOOO////////////////////////////////////
    private Double getComplementaryIndex(byte[] transcriptSequence, int fromRelativeToStartCodonPositions, int toRelativeToStartCodonPositions, int oligLength)
    {
        Double l = getTrailerLength();
        if( cdsFromAndTo == null || cdsFromAndTo.getFrom() < MINIMAL_UTR_LENGTH || l == null || l < MINIMAL_UTR_LENGTH ) return null;
        int from = cdsFromAndTo.getFrom() + fromRelativeToStartCodonPositions;
        int to = cdsFromAndTo.getFrom() + toRelativeToStartCodonPositions;
        return from < 0 || to > transcriptSequence.length - 1 ? null : Olig.getComplementaryIndex(transcriptSequence, from, to, oligLength);
    }

    private Double getCorrelationBasedComplementaryIndex(byte[] transcriptSequence, String transcriptFragmentType, int oligLength) throws Exception
    {
        Double l = getTrailerLength();
        if( cdsFromAndTo == null || cdsFromAndTo.getFrom() < MINIMAL_UTR_LENGTH || l == null || l < MINIMAL_UTR_LENGTH ) return null;
        int positionFrom = 0, positionTo = 0;
        switch( transcriptFragmentType )
        {
            case COMPLEMENTARITY_INDEX2_FOR_TRANSCRIPT : positionTo = transcriptSequence.length - 1; break;
            case COMPLEMENTARITY_INDEX2_FOR_LIDER      : positionTo = cdsFromAndTo.getFrom() - 1; break;
            case COMPLEMENTARITY_INDEX2_FOR_CDS        : positionFrom = cdsFromAndTo.getFrom();
                                                         positionTo = cdsFromAndTo.getTo(); break;
            case COMPLEMENTARITY_INDEX2_FOR_TRAILER    : positionFrom = cdsFromAndTo.getTo() + 1;
                                                         positionTo = transcriptSequence.length - 1; break;
            default                                    : return null;
        }
        return Olig.getCorrelationBasedComplementaryIndex(transcriptSequence, positionFrom, positionTo, oligLength);
    }

    
    //////////////////////////OOOOOOOO///////////////////
    private Boolean isGivenOligNearStartCodon(byte[] transcriptSequence, int fromRelativeToStartCodonPositions, int toRelativeToStartCodonPositions, byte[] olig)
    {
        if( cdsFromAndTo == null ) return null;
        int from = cdsFromAndTo.getFrom() + fromRelativeToStartCodonPositions;
        int to = cdsFromAndTo.getFrom() + toRelativeToStartCodonPositions;
        if( to > transcriptSequence.length - 1 || from < 0 ) return null;
        for( int j = from; j <= to - olig.length + 1; j++ )
            if( Olig.isGivenOlig(transcriptSequence, j, olig) )
                return true;
         return false;
    }
    
    public Double getMrnaFeatureValue(String mrnaFeatureName, byte[] transcriptSequence, Map<String, IPSSiteModel> nameAndSiteModel) throws Exception
    {
        if( transcriptSequence == null ) return null;
        if( mrnaFeatureName.contains(Olig.OLIGONUCLEOTIDE) )
        {
            String[] array = TextUtil2.split( mrnaFeatureName, '_');
            String olig = array[1];
            int pos1 = Integer.parseInt(array[2]), pos2 = Integer.parseInt(array[3]);
            Boolean existance = isGivenOligNearStartCodon(transcriptSequence, pos1, pos2, olig.toLowerCase().getBytes());
            if( existance == null ) return null;
            return existance ? 1.0 : 0.0;
        }
        if( mrnaFeatureName.contains(FEATURE_MATRIX) )
        {
            if( cdsFromAndTo == null ) return null;
            String matrixName = null;
            if( mrnaFeatureName.contains(MATRIX_FOR_START_CODON) )
                matrixName = (TextUtil2.splitPos( mrnaFeatureName, MATRIX_FOR_START_CODON.length()))[1];
            else if( mrnaFeatureName.contains(MATRIX_FOR_STOP_CODON) )
                matrixName = (TextUtil2.splitPos( mrnaFeatureName, MATRIX_FOR_STOP_CODON.length()))[1];
            else return null;
            IPSSiteModel siteModel = nameAndSiteModel.get(matrixName);
            int window = siteModel.getWindow();
            int startPosition = mrnaFeatureName.contains(MATRIX_FOR_START_CODON) ? cdsFromAndTo.getFrom() - window / 2 - 1 : cdsFromAndTo.getTo() - window / 2 - 3 ;
            byte[] region = Olig.getSubByteArray(transcriptSequence, startPosition, window + 2);
            if( region == null ) return null;
            Sequence sequence = new LinearSequence(getTranscriptName(), region, siteModel.getAlphabet());
            Site bestSite = SitePrediction.findBestSite(siteModel, sequence, false);
            return bestSite.getScore();
        }
        switch ( mrnaFeatureName )
        {
            case LinearRegression.INTERCEPT            : return 1.0;
            case TRANSCRIPT_LENGTH                     : return getTranscriptLength();
            case LG_TRANSCRIPT_LENGTH                  : return geTranscriptLengthLG();
            case LEADER_LENGTH                         : return getLiderLength();
            case LG_LEADER_LENGTH                      : return getLiderLengthLG();
            case TRAILER_LENGTH                        : return getTrailerLength();
            case LG_TRAILER_LENGTH                     : return getTrailerLengthLG();
            case CDS_LENGTH                            : return getCdsLength();
            case LG_CDS_LENGTH                         : return getCdsLengthLG();
            case CG_CONTENT_IN_LIDER                   : return getGCcontentInLider(transcriptSequence);
            case CG_CONTENT_IN_TRANSCRIPT              : return getGCcontentInTranscript(transcriptSequence);
            case CG_CONTENT_IN_TRAILER                 : return getGCcontentInTrailer(transcriptSequence);
            case CG_CONTENT_IN_CDS                     : return getGCcontentInCDS(transcriptSequence);
            case AUG_CONCENTRATION                     : return getConcentrationOfAUGinLider(transcriptSequence);
            case POSITION_MINUS_3                      : Boolean index = isAorGonPositionMinus3(transcriptSequence);
                                                         if( index == null ) return null;
                                                         return index ? 1.0 : 0.0;
            case POSITION_PLUS_4                       : Boolean pos4Index = isGonPositionPlus4(transcriptSequence);
                                                         if( pos4Index == null ) return null;
                                                         return pos4Index ? 1.0 : 0.0;
            case UPSTREAM_COMPLEMENTARITY_INDEX        : return getComplementaryIndex(transcriptSequence, -15, 0, 3);
            case DOWNSTREAM_COMPLEMENTARITY_INDEX      : return getComplementaryIndex(transcriptSequence, 0, 20, 3);
            case COMPLEMENTARITY_INDEX2_FOR_TRANSCRIPT :
            case COMPLEMENTARITY_INDEX2_FOR_LIDER      :
            case COMPLEMENTARITY_INDEX2_FOR_CDS        :
            case COMPLEMENTARITY_INDEX2_FOR_TRAILER    : return getCorrelationBasedComplementaryIndex(transcriptSequence, mrnaFeatureName, 3);
            case HAIRPIN_CONCENTRATION_IN_TRANSCRIPT   :
            case HAIRPIN_CONCENTRATION_IN_LIDER        :
            case HAIRPIN_CONCENTRATION_IN_CDS          :
            case HAIRPIN_CONCENTRATION_IN_TRAILER      : return getConcentrationOfHairpins(mrnaFeatureName, transcriptSequence, 4, 3);
            default                                    : return null;
        }
    }
    
    public boolean isStartCodonCanonical(DataElementPath pathToSequences) throws Exception
    {
        byte[] transcriptSequence = getTranscriptSequence(pathToSequences);
        return Olig.isGivenOlig(transcriptSequence, cdsFromAndTo.getFrom(), ATG);
    }

    ////////////////////////////////////new tested ///////////////////////////////////////////////////////////////////////
    //O.K.2\\
    /***
     * 
     * @param transcriptSequence
     * @param startCodonPosition
     * @return CDS as Interval(initial startCodonPosition, last stopCodon position)
     */
    public static Interval getCDS(byte[] transcriptSequence, Integer startCodonPosition)
    {
        if( startCodonPosition == null || transcriptSequence == null || startCodonPosition > transcriptSequence.length - 2 ) return null;
        for( int i = startCodonPosition + 3; i < transcriptSequence.length - 2; i += 3 )
        {
            if( Olig.isGivenOlig(transcriptSequence, i, STOP_CODONS[0]) || Olig.isGivenOlig(transcriptSequence, i, STOP_CODONS[1]) || Olig.isGivenOlig(transcriptSequence, i, STOP_CODONS[2]) )
                return new Interval(startCodonPosition, i + 2);
        }
        return null;
    }
    
    public static Interval getCDSWithGivenOrderOfStartCodon(byte[] transcriptSequence, int orderOfStartCodon)
    {
        int indexOfStartCodon = 0;
        if( transcriptSequence == null || transcriptSequence.length < 6 ) return null;
        for( int i = 0; i < transcriptSequence.length - 6; i ++ )
        {
            if( ! Olig.isGivenOlig(transcriptSequence, i, ATG) ) continue;
            if( ++indexOfStartCodon >= orderOfStartCodon ) return getCDS(transcriptSequence, i);
        }
        return null;
    }
    
    // it is not tested; it will be used for identification of all potential CDSs in single lincRNA
    public static List<Interval> getAllCDSs(byte[] transcriptSequence)
    {
        if( transcriptSequence == null || transcriptSequence.length < 6 ) return null;
        List<Interval> result = new ArrayList<>();
        for( int i = 0; i < transcriptSequence.length - 6; i ++ )
            if( Olig.isGivenOlig(transcriptSequence, i, ATG) )
            {
                Interval interval = getCDS(transcriptSequence, i);
                if( interval != null )
                    result.add(interval);
            }
        if( result.isEmpty() ) return null;
        return result;
    }
    
    private static String[] insertOligsIntoFeatureNames(String[] featureNames, String nameOfOligSet, String[] oligs, int pos1, int pos2)
    {
        String[] newFeaturenames = featureNames;
        if( ArrayUtils.contains(newFeaturenames, nameOfOligSet) )
        {
            newFeaturenames = (String[])ArrayUtils.removeElement(newFeaturenames, nameOfOligSet);
            for( String olig : oligs )
                newFeaturenames = (String[])ArrayUtils.add(newFeaturenames, newFeaturenames.length, Olig.OLIGONUCLEOTIDE + olig.toLowerCase() + "_" + pos1 + "_" + pos2);
        }
        return newFeaturenames;
    }

    public static String[] insertOligsIntoFeatureNames(String[] featureNames)
    {
        String[] newFeaturenames = featureNames;
        if( ! ArrayUtils.contains(featureNames, OLIG_SET1) && ! ArrayUtils.contains(featureNames, OLIG_SET2) ) return newFeaturenames;
        if( ArrayUtils.contains(newFeaturenames, OLIG_SET1) )
            newFeaturenames = insertOligsIntoFeatureNames(newFeaturenames, OLIG_SET1, OLIGS1, -100, -1);
        if( ArrayUtils.contains(newFeaturenames, OLIG_SET2) )
            newFeaturenames = insertOligsIntoFeatureNames(newFeaturenames, OLIG_SET2, OLIGS2, 3, 103);
        return newFeaturenames;
    }

    // under construction
    /***
     * TODO: currently this function identifies only perfect hairpins;
     * in future to adjust it to 'indels' (insertions or deletions)
     * 
     * @param sequence
     * @param startPosition : start position of sequence fragment
     * @param fragmentLength : length of sequence fragment
     * @param minSteamLength
     * @param maxLoopLength
     * @return list of hairpins; each hairpin is represented by Interval[] intervals;
     * where intervals[0] is 1-st part of steam, intervals[1] is 2-nd part of steam and
     * the loop lenth = intervals[1].getFrom() - intervals[0].getTo() - 1;
     */
    // look for hairpins in fixed fragment of (mRNA or transcript) sequence
    private static List<Interval[]> getHairpins(byte[] sequence, int startPosition, int fragmentLength, int minSteamLength, int maxLoopLength)
    {
        List<Interval[]> result = new ArrayList<>();
        // 1. scores := dote matrix
        int[][] scores = MatrixUtils.getLowerTriangularIntegerMatrix(fragmentLength);
        for( int i = 1; i < fragmentLength; i++ )
            for( int j = 0; j < i; j++ )
                if( Olig.areTwoSequenceElementsComplement(sequence[i + startPosition], sequence[j + startPosition]) )
                    scores[i][j] = 1;
        // 2. scores := scores for perfect steams
        for( int j = 1; j < fragmentLength - 1; j++ )
        {
            int steamLength = 0, iIndex = j, jIndex = j;
            while( ++iIndex < fragmentLength && --jIndex >= 0 )
            {
                if( scores[iIndex][jIndex] > 0 )
                    steamLength++;
                else if( steamLength > 0 )
                {
                    for( int i = 1; i <= steamLength; i++ )
                        scores[iIndex - i][jIndex + i] = steamLength;
                    steamLength = 0;
                }
                if( steamLength > 0 ) if( iIndex == fragmentLength - 1 || jIndex == 0 )
                    for( int i = 0; i < steamLength; i++ )
                        scores[iIndex - i][jIndex + i] = steamLength;
            }
        }
        // 3. TODO: in future, array scores[][] has to be adjusted to 'indels' (insertions or deletions)
        // 4. to form perfect hairpins; TODO: in future to adjust to 'indels'
        for( int j = 1; j < fragmentLength - 1; j++ )
        {
            int iIndex = j, jIndex = j;
            while( ++iIndex < fragmentLength && --jIndex >= 0 )
            {
                if( scores[iIndex][jIndex] == 0 ) continue;
                int steamLength = scores[iIndex][jIndex];
                if( steamLength >= minSteamLength && iIndex - jIndex - 1 <= maxLoopLength )
                {
                 Interval[] intervals = new Interval[]{new Interval(startPosition + jIndex - steamLength + 1, startPosition + jIndex), new Interval(startPosition + iIndex, startPosition + iIndex + steamLength - 1)};
                 result.add(intervals);
                }
                iIndex += steamLength - 1;
                jIndex -= steamLength - 1;
            }
        }
        return result;
    }
    
    //TODO: it is temporary presented here;
    /***
    int minSteamLength = 5, maxLoopLength = 1000000000;
    searchForPerfectHairpins(prs, minSteamLength, maxLoopLength);
    ***/
    private void searchForPerfectHairpins(ParticularRiboSeq prs, int minSteamLength, int maxLoopLength) throws Exception
    {
        List<GeneTranscript> transcripts = prs.getGeneTranscriptList();
        DataElementPath pathToSequences = prs.getPathToSequences();
        for(GeneTranscript gt : transcripts )
        {
            String name = gt.getTranscriptName();
            byte[] sequence = gt.getTranscriptSequence(pathToSequences);
            Interval cdsPositions = gt.getCdsInterval();
            log.info("transcript name = " + name + " length = " + sequence.length + " CDS positions : " + cdsPositions.getFrom() + ", " + cdsPositions.getTo());
            Map<String, Double> namesAndValuesOfRiboSeqFeature = gt.getNameAndValueOfRiboSeqFeature();
            for( Entry<String, Double> entry : namesAndValuesOfRiboSeqFeature.entrySet() )
                log.info("RiboSeqFeature : " + entry.getKey() + " = " + entry.getValue());
            List<Interval[]> hairpins = getHairpins(sequence, 0, sequence.length, minSteamLength, maxLoopLength);
            for( Interval[] hairpin : hairpins )
            {
               int steamFrom1 = hairpin[0].getFrom(), steamTo1 = hairpin[0].getTo();
               int steamFrom2 = hairpin[1].getFrom(), steamTo2 = hairpin[1].getTo();
               int loopLength = steamFrom2 - steamTo1 - 1;
               String seq1 = Olig.getStringFromByteInerval(hairpin[0], sequence);
               String seq2 = Olig.getStringFromByteInerval(hairpin[1], sequence);
               log.info("steam length = " + hairpin[0].getLength() + " loopLength = " + loopLength + " steam 1 : " + steamFrom1 + ", " + steamTo1 + " steam 2 : " + steamFrom2 + ", " + steamTo2 + " steam = " + seq1 + " " + seq2);
            }
        }
    }

    public static String[] getNamesOfProteinCodingTranscriptsInEnsembl(DataElementPath pathToSequences, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        List<String> result = new ArrayList<>();
        DataCollection<Transcript> dc = pathToSequences.getRelativePath("../../Data/transcript").getDataCollection(Transcript.class);
        List<String> nameList = dc.getNameList();
        int difference = to - from, iJobControl = 0, n = dc.getSize();
        for( String name : nameList )
        {
            if( jobControl != null )
                jobControl.setPreparedness(from + ++iJobControl * difference / n);
            Transcript transcript = dc.get(name);
            if( transcript == null ) continue;
            String geneType = (String)transcript.getAttributes().getValue("biotype");
            if( geneType.equals(Gene.PROTEIN_CODING) )
                result.add(name);
        }
        return result.toArray(new String[0]);
    }
    
    // details about parsing all 'transcript's : see in biouml.plugins.ensembl.access.TranscriptTransformer.java
    public static List<GeneTranscript> readTranscriptsInEnsembl(String[] transcriptNames, DataElementPath pathToSequences, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        List<GeneTranscript> result = new ArrayList<>();
        DataCollection<Transcript> dc = pathToSequences.getRelativePath("../../Data/transcript").getDataCollection(Transcript.class);
        String[] names = transcriptNames != null ? transcriptNames : dc.getNameList().toArray(new String[0]);
        int difference = to - from, iJobControl = 0, n = dc.getSize();
        for( String name : names )
        {
            if( jobControl != null )
                jobControl.setPreparedness(from + ++iJobControl * difference / n);
            Transcript transcript = dc.get(name);
            if( transcript == null ) continue;
            String transcriptName = transcript.getName();
            String chromosome = (String)transcript.getAttributes().getValue("chr");
            String geneId = (String)transcript.getAttributes().getValue("gene");
            String geneType = (String)transcript.getAttributes().getValue("biotype");
            String stringStrand = (String)transcript.getAttributes().getValue("strand");
            int intStrand = stringStrand.equals("+") ? 2 : 3;
            int start = ((Number)transcript.getAttributes().getValue("start")).intValue();
            int end = ((Number)transcript.getAttributes().getValue("end")).intValue();
            String[] exons = (String[])transcript.getAttributes().getValue("exons");
            String cds = (String)transcript.getAttributes().getValue("cds");
            
            // Parsing strings such as "ENSE00002024145:11394774-11401737;2"
            Interval[] exonIntervals = new Interval[exons.length];
            String[] exonNames = new String[exons.length];
            for( String exon : exons )
            {
                String[] array = TextUtil2.split( exon, ';');
                int exonIndex = Integer.parseInt(array[1]) - 1;
                array = TextUtil2.split( array[0], ':');
                exonNames[exonIndex] = array[0];
                array = TextUtil2.split( array[1], '-');
                exonIntervals[exonIndex] = new Interval(Integer.parseInt(array[0]), Integer.parseInt(array[1]));
            }
            
            // Parsing string such as "ENSE00002024145:109 ENSE00002024145:1032"
            Interval cdsInterval = null;
            if( cds != null )
            {
                String array[] = TextUtil2.split( cds, ' ');
                String subarray1[] = TextUtil2.split( array[0], ':'), subarray2[] = TextUtil2.split( array[1], ':');
                String name1 = subarray1[0], name2 = subarray2[0];
                int pos1 = Integer.parseInt(subarray1[1]), pos2 = Integer.parseInt(subarray2[1]);
                for( int i = 0; i < exonIntervals.length; i++ )
                    if( ! name1.equals(exonNames[i]) )
                        pos1 += exonIntervals[i].getLength();
                    else break;
                for( int i = 0; i < exonIntervals.length; i++ )
                    if( ! name2.equals(exonNames[i]) )
                        pos2 += exonIntervals[i].getLength();
                    else break;
                cdsInterval = new Interval(--pos1, --pos2);
            }
            result.add(new GeneTranscript(transcriptName, chromosome, new Interval(start, end), geneId, geneType, intStrand, cdsInterval, exonIntervals, null));
        }
        return result;
    }
    
    public static List<GeneTranscript> removeNonProteinCodingTranscriptsWithShortLiders(List<GeneTranscript> geneTranscriptList, AnalysisJobControl jobControl, int from, int to)
    {
        int difference = to - from;
        List<GeneTranscript> newGeneTranscriptList = new ArrayList<>();
        for( int i = 0; i < geneTranscriptList.size(); i++ )
        {
            if( jobControl != null )
                jobControl.setPreparedness(from + (i + 1) * difference / geneTranscriptList.size());
            GeneTranscript gt = geneTranscriptList.get(i);
            if( gt.getGeneType().equals(Gene.PROTEIN_CODING) && gt.getCdsInterval() != null && gt.getLiderLength() != null )
                newGeneTranscriptList.add(gt);
        }
        return newGeneTranscriptList;
    }

  //temp
    private static Logger log = Logger.getLogger(GeneTranscript.class.getName());
}
