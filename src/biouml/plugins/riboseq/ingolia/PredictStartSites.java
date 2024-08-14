package biouml.plugins.riboseq.ingolia;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import one.util.streamex.StreamEx;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

import biouml.plugins.riboseq.ingolia.svmlight.SVMLightPredict;
import biouml.plugins.riboseq.transcripts.Transcript;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.bsa.AminoAcidsAlphabet;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.DiscontinuousCoordinateSystem;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.bean.StaticDescriptor;

public class PredictStartSites extends AnalysisMethodSupport<PredictStartSitesParameters>
{
    private static final String READS_NUMBER = "Reads number";
    private static final String PROTEIN = "Protein";
    private static final String TYPE = "Type";
    private static final String OFFSET_FROM_KNOWN_CDS_END = "Offset from known CDS end";
    private static final String OFFSET_FROM_KNOWN_CDS_START = "Offset from known CDS start";
    private static final String CDS_LENGTH = "CDS length";
    private static final String INIT_CODON_SCORE = "Init codon score";
    private static final String INIT_CODON = "Init codon";
    private static final String INIT_CODON_OFFSET = "Init codon offset";
    private static final String SUMMIT_SCORE = "Summit score";
    private static final String SUMMIT_OFFSET = "Summit offset";
    private static final String PEAK_SEQUENCE = "Peak sequence";
    private static final String PEAK_TO = "Peak to";
    private static final String PEAK_FROM = "Peak from";
    private static final String TRANSCRIPT_NAME = "Transcript name";

    private static PropertyDescriptor READS_NUMBER_PD = StaticDescriptor.create( READS_NUMBER );
    private static PropertyDescriptor PROTEIN_PD = StaticDescriptor.create( PROTEIN );
    private static PropertyDescriptor TYPE_PD = StaticDescriptor.create( TYPE );
    private static PropertyDescriptor OFFSET_FROM_KNOWN_CDS_END_PD = StaticDescriptor.create( OFFSET_FROM_KNOWN_CDS_END );
    private static PropertyDescriptor OFFSET_FROM_KNOWN_CDS_START_PD = StaticDescriptor.create( OFFSET_FROM_KNOWN_CDS_START );
    private static PropertyDescriptor CDS_LENGTH_PD = StaticDescriptor.create( CDS_LENGTH );
    private static PropertyDescriptor INIT_CODON_SCORE_PD = StaticDescriptor.create( INIT_CODON_SCORE );
    private static PropertyDescriptor INIT_CODON_PD = StaticDescriptor.create( INIT_CODON );
    private static PropertyDescriptor INIT_CODON_OFFSET_PD = StaticDescriptor.create( INIT_CODON_OFFSET );
    private static PropertyDescriptor SUMMIT_SCORE_PD = StaticDescriptor.create( SUMMIT_SCORE );
    private static PropertyDescriptor SUMMIT_OFFSET_PD = StaticDescriptor.create( SUMMIT_OFFSET );
    private static PropertyDescriptor PEAK_SEQUENCE_PD = StaticDescriptor.create( PEAK_SEQUENCE );
    private static PropertyDescriptor PEAK_TO_PD = StaticDescriptor.create( PEAK_TO );
    private static PropertyDescriptor PEAK_FROM_PD = StaticDescriptor.create( PEAK_FROM );
    private static PropertyDescriptor TRANSCRIPT_NAME_PD = StaticDescriptor.create( TRANSCRIPT_NAME );

    public PredictStartSites(DataCollection<?> origin, String name)
    {
        super( origin, name, new PredictStartSitesParameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        jobControl.pushProgress( 0, 10 );
        List<Transcript> transcripts = parameters.getTranscriptSet().createTranscriptLoader().loadTranscripts( log );
        jobControl.popProgress();

        jobControl.pushProgress( 10, 90 );
        final List<PredictedStartSite> startSites = new ArrayList<>();
        jobControl.forCollection( transcripts, t -> {
            try
            {
                List<PredictedStartSite> startSitesOfTranscript = predictStartSites( t );
                startSites.addAll( startSitesOfTranscript );
            }
            catch( Exception e )
            {
                throw ExceptionRegistry.translateException( e );
            }
            return true;
        } );
        jobControl.popProgress();

        jobControl.pushProgress( 90, 95 );
        TableDataCollection summaryTable = makeSummaryTable( startSites );
        jobControl.popProgress();

        jobControl.pushProgress( 95, 100 );
        SqlTrack outputTrack = makeOutputTrack( startSites );
        jobControl.popProgress();


        return new Object[] {summaryTable, outputTrack};
    }

    private SqlTrack makeOutputTrack(List<PredictedStartSite> startSites) throws Exception
    {
        SqlTrack result = SqlTrack.createTrack( parameters.getOutputTrack(), null, parameters.getTranscriptSet().getChromosomes().getCompletePath() );
        for(PredictedStartSite p : startSites)
            result.addSite( createSite(p) );
        result.finalizeAddition();
        return result;
    }



    private Site createSite(PredictedStartSite p) throws Exception
    {
        Transcript t = p.getTranscript();
        DynamicPropertySetAsMap properties = new DynamicPropertySetAsMap();
        properties.add( new DynamicProperty( TRANSCRIPT_NAME_PD, String.class, p.getTranscript().getName() ) );
        properties.add( new DynamicProperty( PEAK_FROM_PD, Integer.class, p.getPeak().getFrom() ) );
        properties.add( new DynamicProperty( PEAK_TO_PD, Integer.class, p.getPeak().getTo() ) );
        properties.add( new DynamicProperty( PEAK_SEQUENCE_PD, String.class, p.getPeakSequence() ) );
        properties.add( new DynamicProperty( SUMMIT_OFFSET_PD, Integer.class, p.getSummitOffset()) );
        properties.add( new DynamicProperty( SUMMIT_SCORE_PD, Double.class, p.getSummitScore() ) );
        if(p.getInitCodonOffset() != null)
            properties.add( new DynamicProperty( INIT_CODON_OFFSET_PD, Integer.class, p.getInitCodonOffset() ) );
        if(p.getInitCodon() != null)
            properties.add( new DynamicProperty( INIT_CODON_PD, String.class, p.getInitCodon() ) );
        if(p.getInitCodonScore() != null)
            properties.add( new DynamicProperty( INIT_CODON_SCORE_PD, Double.class, p.getInitCodonScore() ) );
        if(p.getCDSLength() != null)
            properties.add( new DynamicProperty( CDS_LENGTH_PD, Integer.class, p.getCDSLength() ) );
        if(p.getOffsetFromKnownCDSStart() != null)
            properties.add( new DynamicProperty( OFFSET_FROM_KNOWN_CDS_START_PD, Integer.class, p.getOffsetFromKnownCDSStart() ) );
        if(p.getOffsetFromKnownCDSEnd() != null)
            properties.add( new DynamicProperty( OFFSET_FROM_KNOWN_CDS_END_PD, Integer.class, p.getOffsetFromKnownCDSEnd() ) );
        if(p.getType() != null)
            properties.add( new DynamicProperty( TYPE_PD, String.class, p.getType().toString() ) );
        if(p.getProteinSequence() != null)
            properties.add( new DynamicProperty( PROTEIN_PD, String.class, p.getProteinSequence() ) );
        properties.add( new DynamicProperty( READS_NUMBER_PD, Integer.class, p.getReadsNumber() ) );
        Sequence sequence = parameters.getTranscriptSet().getChromosomes().get( t.getChromosome() ).getSequence();
        DiscontinuousCoordinateSystem coordSystem = new DiscontinuousCoordinateSystem( StreamEx.of( t.getExonLocations() ).map( i->i.shift( sequence.getStart() ) ).toList(), !t.isOnPositiveStrand() );
        int start = coordSystem.translateCoordinateBack( p.getSummitOffset() );
        return new SiteImpl( null, t.getChromosome(), SiteType.TYPE_MISC_SIGNAL, Basis.BASIS_PREDICTED, start, 1, Precision.PRECISION_EXACTLY, t.isOnPositiveStrand() ? StrandType.STRAND_PLUS : StrandType.STRAND_MINUS, sequence, properties );
    }

    private List<PredictedStartSite> predictStartSites(Transcript t) throws Exception
    {
        final ObservationListBuilder builder = parameters.createObservationListBuilder();
        final ObservationList observations = builder.buildAllTranscriptObservations( t, parameters.getBAMTracks() );
        if(observations.getObservations().isEmpty())
            return Collections.emptyList();

        final double[] scores = getScores( observations );

        final List<YesPosition> yesPositions = createYesPositions( observations, scores );
        if( yesPositions.isEmpty() )
            return Collections.emptyList();

        final List<Interval> clusters = createClusters( yesPositions );

        final int[][] sampleProfiles = builder.getSampleProfiles();
        return createPredictedStartSites( t, yesPositions, clusters, sampleProfiles );
    }

    private double[] getScores(ObservationList observations) throws Exception
    {
        SVMLightPredict svmLight = new SVMLightPredict();
        File modelFile = parameters.getModelFile().getDataElement( FileDataElement.class ).getFile();
        return svmLight.predict( observations, modelFile, log );
    }

    private List<YesPosition> createYesPositions(ObservationList observations, double[] scores)
    {
        List<YesPosition> yesPositions = new ArrayList<>();
        final List<Observation> observationList = observations.getObservations();
        for( int i = 0; i < observationList.size(); i++ )
            if( scores[i] > 0 )
            {
                Observation observation = observationList.get( i );
                final YesPosition yesPosition = new YesPosition( observation.getPosition(), scores[i] );
                yesPositions.add( yesPosition );
            }
        return yesPositions;
    }

    private List<Interval> createClusters(List<YesPosition> yesPositions)
    {
        List<Interval> clusters = new ArrayList<>();
        int curFrom = 0;
        for( int i = 1; i < yesPositions.size(); i++ )
            if( yesPositions.get( i ).pos != yesPositions.get( i - 1 ).pos + 1 )
            {
                clusters.add( new Interval( curFrom, i - 1 ) );
                curFrom = i;
            }
        clusters.add( new Interval( curFrom, yesPositions.size() - 1 ) );
        return clusters;
    }

    private List<PredictedStartSite> createPredictedStartSites(Transcript t, List<YesPosition> yesPositions,
                                                               List<Interval> clusters, int[][] sampleProfiles)
            throws Exception
    {
        List<PredictedStartSite> result = new ArrayList<>();
        for( Interval cluster : clusters )
        {
            PredictedStartSite startSite = createPredictedStartSite( t, yesPositions, sampleProfiles, cluster );
            result.add( startSite );
        }
        return result;
    }

    private PredictedStartSite createPredictedStartSite(Transcript t, List<YesPosition> yesPositions, int[][] sampleProfiles, Interval cluster) throws Exception
    {
        PredictedStartSite startSite = new PredictedStartSite();
        startSite.setTranscript( t );
        addPeak( yesPositions, cluster, startSite );
        addSummit( startSite, yesPositions, cluster );
        annotateStartSite( startSite, t );
        addInitCodonScore( startSite, yesPositions, cluster );
        addReadsNumber( startSite, sampleProfiles );

        return startSite;
    }

    private void addSummit(PredictedStartSite startSite, List<YesPosition> yesPositions, Interval cluster)
    {
        YesPosition summit = StreamEx.of( yesPositions.subList( cluster.getFrom(), cluster.getTo() + 1 ) )
                .maxByDouble( p->p.score ).orElseThrow( ()->new RuntimeException("Empty cluster") );
        startSite.setSummitOffset( summit.pos );
        startSite.setSummitScore( summit.score );
    }

    private void addPeak(List<YesPosition> yesPositions, Interval cluster, PredictedStartSite startSite)
    {
        final int clusterFrom = cluster.getFrom();
        final int clusterTo = cluster.getTo();
        final int peakFrom = yesPositions.get( clusterFrom ).pos;
        final int peakTo = yesPositions.get( clusterTo ).pos;
        final Interval peak = new Interval( peakFrom, peakTo );
        startSite.setPeak( peak );
    }

    private void addInitCodonScore(PredictedStartSite startSite, List<YesPosition> yesPositions, Interval cluster)
    {
        if( startSite.getInitCodon() != null )
        {
            double initCodonScore = StreamEx.of( yesPositions.subList( cluster.getFrom(), cluster.getTo() + 1 ) )
                .findAny( p->p.pos == startSite.getInitCodonOffset() )
                .orElseThrow( ()->new RuntimeException("Invalid init codon offset") )
                .score;
            startSite.setInitCodonScore( initCodonScore );
        }
    }

    private void addReadsNumber(PredictedStartSite startSite, int[][] sampleProfiles)
    {
        final StartSiteReadsCounter readsCounter = new StartSiteReadsCounter( startSite, sampleProfiles );
        final int readsNumber = readsCounter.countReads();
        startSite.setReadsNumber( readsNumber );
    }

    private void annotateStartSite(PredictedStartSite startSite, Transcript t) throws Exception
    {
        Sequence processedSequence = getTranscriptSequence( t );
        addPeakSequence( startSite, processedSequence );
        addInitCodon( startSite, processedSequence );
        addCDSLength( startSite, processedSequence );
        addKnownCDSOffset( startSite, t );
        addStartSiteType( startSite, t );
        addProtein( startSite, processedSequence );
    }

    private void addProtein(PredictedStartSite startSite, Sequence processedSequence)
    {
        if( startSite.getInitCodonOffset() == null || startSite.getCDSLength() == null )
            return;
        AminoAcidsAlphabet alphabet = AminoAcidsAlphabet.getInstance();
        char[] protein = new char[startSite.getCDSLength() / 3 - 1];
        for( int i = 0; i < protein.length; i++ )
        {
            String codon = getCodon( processedSequence, startSite.getInitCodonOffset() + i * 3 );
            protein[i] = (char)alphabet.getAminoAcidForTriplet( codon );
        }
        startSite.setProteinSequence( new String( protein ).toUpperCase() );
    }

    private void addStartSiteType(PredictedStartSite startSite, Transcript t)
    {
        if(startSite.getInitCodonOffset() == null || startSite.getCDSLength() == null)
            return;
        int myCDSFrom = startSite.getInitCodonOffset();
        int myCDSTo = myCDSFrom + startSite.getCDSLength() - 1;
        StartSiteType bestType = StartSiteType.NOVEL;
        for(Interval knownCDS : t.getCDSLocations())
        {
            StartSiteType type;
            if( myCDSFrom == knownCDS.getFrom() )
                type = StartSiteType.CANONICAL;
            else if( myCDSFrom < knownCDS.getFrom() )
            {
                if(myCDSTo < knownCDS.getFrom())
                    type = StartSiteType.UPSTREAM;
                else if( (knownCDS.getFrom() - myCDSFrom) % 3 == 0)
                    type = StartSiteType.EXTENSION;
                else
                    type = StartSiteType.UPSTREAM_OVERLAPPING;
            }
            else
            {
                if(myCDSFrom > knownCDS.getTo())
                    type = StartSiteType.DOWNSTREAM;
                else if( (myCDSFrom - knownCDS.getFrom()) % 3 == 0)
                    type = StartSiteType.TRUNCATION;
                else
                    type = StartSiteType.INTERNAL_OUT_OF_FRAME;
            }
            if(type.ordinal() < bestType.ordinal())
                bestType = type;
        }
        startSite.setType( bestType );
    }

    private void addKnownCDSOffset(PredictedStartSite startSite, Transcript t)
    {
        if( startSite.getInitCodonOffset() == null )
            return;
        int min = Integer.MAX_VALUE;
        for( Interval cds : t.getCDSLocations() )
        {
            int distance = startSite.getInitCodonOffset() - cds.getFrom();
            if( Math.abs( distance ) < Math.abs( min ) )
                min = distance;
        }
        if( min != Integer.MAX_VALUE )
            startSite.setOffsetFromKnownCDSStart( min );

        if( startSite.getCDSLength() == null )
            return;
        int cdsEnd = startSite.getInitCodonOffset() + startSite.getCDSLength() - 1;
        min = Integer.MAX_VALUE;
        for( Interval cds : t.getCDSLocations() )
        {
            int distance = cdsEnd - cds.getTo();
            if( Math.abs( distance ) < Math.abs( min ) )
                min = distance;
        }
        if( min != Integer.MAX_VALUE )
            startSite.setOffsetFromKnownCDSEnd( min );
    }

    private void addCDSLength(PredictedStartSite startSite, Sequence processedSequence)
    {
        if(startSite.getInitCodonOffset() == null)
            return;
        for(int i = startSite.getInitCodonOffset(); i < processedSequence.getLength() - 2; i += 3)
        {
            String codon = getCodon( processedSequence, i );
            if(codon.equals( "TAG" ) || codon.equals( "TGA" ) || codon.equals( "TAA" ))
            {
                startSite.setCDSLength( i - startSite.getInitCodonOffset() + 3 );
                return;
            }
        }
    }

    private void addInitCodon(PredictedStartSite startSite, Sequence processedSequence)
    {
        Set<String> nonCanonicalInitCodons = new HashSet<>();
        nonCanonicalInitCodons.add( "CTG" );
        nonCanonicalInitCodons.add( "GTG" );
        nonCanonicalInitCodons.add( "TTG" );
        nonCanonicalInitCodons.add( "ACG" );
        for( int i = startSite.getPeak().getFrom(); i <= startSite.getPeak().getTo(); i++ )
        {
            String codon = getCodon( processedSequence, i );
            if( "ATG".equals( codon ) )
            {
                startSite.setInitCodonOffset( i );
                startSite.setInitCodon( "ATG" );
                return;
            }
        }

        for( int i = startSite.getPeak().getFrom(); i <= startSite.getPeak().getTo(); i++ )
        {
            String codon = getCodon( processedSequence, i );
            if( nonCanonicalInitCodons.contains( codon ) )
            {
                startSite.setInitCodonOffset( i );
                startSite.setInitCodon( codon );
                return;
            }
        }

        startSite.setInitCodonOffset( startSite.getSummitOffset() );
        String codon = getCodon( processedSequence, startSite.getSummitOffset() );
        startSite.setInitCodon( codon );
    }

    private String getCodon(Sequence s, int position)
    {
        char[] codon = new char[3];
        for(int i = 0; i < 3; i++)
            codon[i] = (char)s.getLetterAt( position + i );
        return new String( codon ).toUpperCase();
    }

    private void addPeakSequence(PredictedStartSite startSite, Sequence processedSequence) throws Exception
    {
        Interval peakInterval = startSite.getPeak();
        char[] peakSequence = new char[peakInterval.getLength() + 4];
        for( int i = peakInterval.getFrom() - 2; i <= peakInterval.getTo() + 2; i++ )
        {
            char letter = processedSequence.getInterval().inside( i ) ? (char)processedSequence.getLetterAt( i ) : 'n';
            if(peakInterval.inside( i ))
                letter = Character.toUpperCase( letter );
            else
                letter = Character.toLowerCase( letter );
            peakSequence[i - peakInterval.getFrom() + 2] = letter;
        }
        startSite.setPeakSequence( new String( peakSequence ) );
    }

    private Sequence getTranscriptSequence(Transcript t) throws Exception
    {
        Sequence chrSequence = getChromosomeSequence( t.getChromosome() );
        return t.getSequence( chrSequence );
    }


    private final Set<String> notFoundChromosomes = new HashSet<>();
    private Sequence getChromosomeSequence(String ensemblChrName) throws Exception
    {
        DataCollection<AnnotatedSequence> chromosomes = parameters.getTranscriptSet().getChromosomes();
        AnnotatedSequence chromosome = chromosomes.get( ensemblChrName );
        if( chromosome != null )
            return chromosome.getSequence();

        String ucscChrName;
        if( ensemblChrName.equals( "MT" ) )
            ucscChrName = "chrM";
        else
            ucscChrName = "chr" + ensemblChrName;
        chromosome = chromosomes.get( ucscChrName );
        if( chromosome != null )
            return chromosome.getSequence();

        if( !notFoundChromosomes.contains( ensemblChrName ) )
        {
            log.warning( "Chromosome " + ensemblChrName + " not found" );
            notFoundChromosomes.add( ensemblChrName );
        }
        return null;
    }

    private TableDataCollection makeSummaryTable(List<PredictedStartSite> startSites)
    {
        TableDataCollection result = TableDataCollectionUtils.createTableDataCollection( parameters.getSummaryTable() );

        ColumnModel cm = result.getColumnModel();
        cm.addColumn( TRANSCRIPT_NAME, DataType.Text );
        cm.addColumn( PEAK_FROM, DataType.Integer );
        cm.addColumn( PEAK_TO, DataType.Integer );
        cm.addColumn( PEAK_SEQUENCE, DataType.Text );
        cm.addColumn( SUMMIT_OFFSET, DataType.Integer );
        cm.addColumn( SUMMIT_SCORE, Double.class );
        cm.addColumn( INIT_CODON_OFFSET, DataType.Integer );
        cm.addColumn( INIT_CODON, DataType.Text );
        cm.addColumn( INIT_CODON_SCORE, Double.class );
        cm.addColumn( CDS_LENGTH, DataType.Integer );
        cm.addColumn( OFFSET_FROM_KNOWN_CDS_START, DataType.Integer );
        cm.addColumn( OFFSET_FROM_KNOWN_CDS_END, DataType.Integer );
        cm.addColumn( TYPE, DataType.Text );
        cm.addColumn( PROTEIN, DataType.Text );
        cm.addColumn( READS_NUMBER, DataType.Integer );

        int id = 1;
        for( PredictedStartSite s : startSites )
        {
            Object[] values = new Object[] {
                    s.getTranscript().getName(),
                    s.getPeak().getFrom(),
                    s.getPeak().getTo(),
                    s.getPeakSequence(),
                    s.getSummitOffset(),
                    s.getSummitScore(),
                    s.getInitCodonOffset(),
                    s.getInitCodon(),
                    s.getInitCodonScore(),
                    s.getCDSLength(),
                    s.getOffsetFromKnownCDSStart(),
                    s.getOffsetFromKnownCDSEnd(),
                    s.getType() == null ? null : s.getType().toString(),
                    s.getProteinSequence(),
                    s.getReadsNumber()
                    };
            TableDataCollectionUtils.addRow( result, String.valueOf( id++ ), values, true );
        }

        result.finalizeAddition();
        parameters.getSummaryTable().save( result );

        return result;
    }

}
