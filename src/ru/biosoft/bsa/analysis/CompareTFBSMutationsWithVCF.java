package ru.biosoft.bsa.analysis;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.task.ExceptionalConsumer;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.analysis.Stat;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SiteModelCollection;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.VCFSqlTrack;
import ru.biosoft.bsa.analysis.SequenceAccessor.CachedSequenceRegion;
import ru.biosoft.bsa.analysis.maos.Variation;
import ru.biosoft.bsa.analysis.maos.coord_mapping.CoordinateMapping;
import ru.biosoft.bsa.analysis.maos.coord_mapping.InverseMapping;
import ru.biosoft.bsa.analysis.maos.coord_mapping.MappingByVCF;
import ru.biosoft.bsa.analysis.maos.coord_mapping.ReverseStrandMapping;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.bean.BeanInfoEx2;

@ClassIcon("resources/CompareTFBSVariation.gif")
public class CompareTFBSMutationsWithVCF extends AnalysisMethodSupport<CompareTFBSMutationsWithVCF.Parameters>
{
    public static final String CHANGING_PROPERTY = "TFBS_changing";
    public static final String EXPECTED_RATIO_COLUMN = "Expected ratio";
    public static final String RATIO_COLUMN = "Ratio";
    private static final String PVALUE_COLUMN = "P-value";
    private static final String CUTOFF_COLUMN = "Cutoff";
    private static final float MIN_DELTA = 0.1f;


    public CompareTFBSMutationsWithVCF(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        SiteModel[] siteModels = parameters.getSiteModels().getDataElement( SiteModelCollection.class ).stream()
                .toArray( SiteModel[]::new );
        int maxModelLength = StreamEx.of( siteModels ).mapToInt( SiteModel::getLength ).max().orElseThrow( () -> {
            throw new RuntimeException( "No site models found" );
        } );
        Map<String, Sequence> chromosomes = parameters.getGenome().getSequenceCollection().stream()
                .collect( Collectors.toMap( DataElement::getName, AnnotatedSequence::getSequence ) );

        log.info( "Computing site gain/loss scores for experimental track" );
        jobControl.pushProgress( 0, 25 );
        Track yesTrack = parameters.getYesVCFTrack().getDataElement( Track.class );
        Scores yesScores = computeMaxGainLossDeltas( yesTrack, chromosomes, siteModels, maxModelLength );
        jobControl.popProgress();
        if( yesScores == null )
            return null; // cancelled

        log.info( "Computing site gain/loss scores for control track" );
        jobControl.pushProgress( 25, 50 );
        Track noTrack = parameters.getNoVCFTrack().getDataElement( Track.class );
        Scores noScores = computeMaxGainLossDeltas( noTrack, chromosomes, siteModels, maxModelLength );
        jobControl.popProgress();
        if( noScores == null )
            return null; // cancelled

        log.info( "Finding site loss over/under representation" );
        jobControl.pushProgress( 50, 75 );
        TableDataCollection siteLossTable = createTable( parameters.getSiteLossesTable() );
        findDifferentiatingModels( yesScores.losses, noScores.losses, siteModels, siteLossTable );
        siteLossTable.finalizeAddition();
        parameters.getSiteLossesTable().save( siteLossTable );
        jobControl.popProgress();

        log.info( "Finding site gain over/under representation" );
        int percent = parameters.isFilterVCF() ? 85 : 100;
        jobControl.pushProgress( 75, percent );
        TableDataCollection siteGainTable = createTable( parameters.getSiteGainsTable() );
        findDifferentiatingModels( yesScores.gains, noScores.gains, siteModels, siteGainTable );
        parameters.getSiteGainsTable().save( siteGainTable );
        siteGainTable.finalizeAddition();

        List<Object> results = new ArrayList<>();
        results.add( siteGainTable );
        results.add( siteLossTable );

        if(parameters.isFilterVCF())
        {
            log.info( "Filtering input yes track by top site gain/loss" );
            jobControl.pushProgress( 85, 100 );
            int maxcnt = parameters.getTopCount();
            ColumnModel columnModel = siteGainTable.getColumnModel();
            int indRatio = columnModel.getColumnIndex( RATIO_COLUMN );
            int indExpRatio = columnModel.getColumnIndex( EXPECTED_RATIO_COLUMN );
            boolean isOverrepresented = parameters.isUseOverrepresented();
            Set<String> bestmodels = new HashSet<>();
            Map<String, Integer> model2index = new HashMap<>();
            int cnt = 0;
            for( RowDataElement rde : siteGainTable )
            {
                Object[] values = rde.getValues();
                if( !isOverrepresented || (Double)values[indRatio] > (Double)values[indExpRatio] )
                {
                    bestmodels.add( rde.getName() );
                    cnt++;
                }
                if( cnt == maxcnt )
                    break;
            }
            cnt = 0;
            for( RowDataElement rde : siteLossTable )
            {
                Object[] values = rde.getValues();
                if( !isOverrepresented || (Double)values[indRatio] > (Double)values[indExpRatio] )
                {
                    bestmodels.add( rde.getName() );
                    cnt++;
                }

                if( cnt == maxcnt )
                    break;
            }

            for( int i = 0; i < siteModels.length; i++ )
            {
                SiteModel sm = siteModels[i];
                if( bestmodels.contains( sm.getName() ) )
                    model2index.put( sm.getName(), i );
            }
            DataElementPath trackPath = parameters.getFilteredVCFTrack();
            SqlTrack filteredYesTrack = SqlTrack.createTrack( trackPath, yesTrack, yesTrack.getClass() );
            boolean completed = false;
            try
            {
                if( !filterVCF( yesTrack, chromosomes, siteModels, maxModelLength, model2index, noScores, filteredYesTrack ) )
                    return null; // cancelled
                filteredYesTrack.finalizeAddition();
                CollectionFactoryUtils.save( filteredYesTrack );
                completed = true;
            }
            finally
            {
                // cancelled or failed: remove the track not finished, as ProcessTrack and FilterTrackAnalysis do on cancel
                if( !completed )
                {
                    try
                    {
                        filteredYesTrack.getOrigin().remove( filteredYesTrack.getName() );
                    }
                    catch( Exception e )
                    {
                        // keep the exception that brought us here, if any
                        log.log( Level.WARNING, "Cannot remove the unfinished track " + trackPath, e );
                    }
                }
            }
            results.add( filteredYesTrack );
        }
        return results.toArray( new Object[0] );
    }

    private TableDataCollection createTable(DataElementPath path)
    {
        TableDataCollection result = TableDataCollectionUtils.createTableDataCollection( path );
        ColumnModel cm = result.getColumnModel();
        cm.addColumn( RATIO_COLUMN, DataType.Float );
        cm.addColumn( EXPECTED_RATIO_COLUMN, DataType.Float );
        cm.addColumn( PVALUE_COLUMN, DataType.Float );
        cm.addColumn( CUTOFF_COLUMN, DataType.Float );
        cm.addColumn( "yesCount", DataType.Integer );
        cm.addColumn( "noCount", DataType.Integer );
        TableDataCollectionUtils.setSortOrder( result, "P-value", true );
        return result;
    }

    private static class Scores
    {
        float[][] losses;
        float[][] gains;
        Scores(int siteModelCount, int siteCount)
        {
            losses = new float[siteModelCount][siteCount];
            gains = new float[siteModelCount][siteCount];
        }
    }
    /** @return the scores, or null if cancelled */
    private Scores computeMaxGainLossDeltas(Track track, Map<String, Sequence> chromosomes, SiteModel[] siteModels, int maxModelLength)
            throws Exception
    {
        DataCollection<Site> sites = track.getAllSites();

        int siteCount = 0;
        Set<String> missingChromosomes = new HashSet<>();
        Map<String, List<Variation>> variationsByChr = new HashMap<>();
        int skipped = 0;
        int mismatch = 0;
        for( Site s : sites )
        {
            String chrName = s.getOriginalSequence().getName();
            Sequence seq = chromosomes.get( chrName );
            if( seq == null )
            {
                if( !missingChromosomes.contains( chrName ) )
                {
                    log.warning( "Ignoring sites from chromosome " + chrName );
                    missingChromosomes.add( chrName );
                }
                continue;
            }
            Variation var = null;
            try
            {
                var = Variation.createFromSite( s );
                if( !CompareTFBSMutations.isCompatible( var, seq ) )
                {
                    mismatch++;
                    var = null;
                }
            }
            catch( IllegalArgumentException e )
            {
                skipped++;
                //ignore errors parsing vcf site
            }
            if( var == null )
                continue;
            variationsByChr.computeIfAbsent( chrName, k -> new ArrayList<>() ).add( var );

            siteCount++;
        }

        if( skipped + mismatch > 0 )
            log.warning( ( skipped + mismatch ) + " vcf sites were skipped" );
        if( mismatch > sites.getSize() / 2 )
            log.warning( mismatch + " variations were not matched to reference sequences. Probably, incompatible genome version." );

        if( siteCount > parameters.getInputSizeLimit() )
        {
            variationsByChr = subSample( variationsByChr, parameters.getInputSizeLimit() );
            siteCount = variationsByChr.values().stream().mapToInt( List::size ).sum();
            log.info( "Subsampled to " + siteCount + " sites" );
        }

        Scores result = new Scores( siteModels.length, siteCount );
        if( !computeDeltas( variationsByChr, chromosomes, siteModels, maxModelLength, result.losses, result.gains, jobControl ) )
            return null; // cancelled: the scores are incomplete
        return result;
    }

    /** A variation to score, with its chromosome; index is its place in the former loop over the variations. */
    private static class ScoredVariation
    {
        final int index;
        final Variation var;
        final Sequence chromosome;
        final SequenceAccessor accessor;

        ScoredVariation(int index, Variation var, Sequence chromosome, SequenceAccessor accessor)
        {
            this.index = index;
            this.var = var;
            this.chromosome = chromosome;
            this.accessor = accessor;
        }
    }

    /** The variations of the map, numbered in the map's order, each with its chromosome and the accessor to read it. */
    private static List<ScoredVariation> numbered(Map<String, List<Variation>> variationsByChr, Map<String, Sequence> chromosomes)
    {
        List<ScoredVariation> result = new ArrayList<>();
        for( Map.Entry<String, List<Variation>> e : variationsByChr.entrySet() )
        {
            Sequence chromosome = chromosomes.get( e.getKey() );
            // a chromosome is read by several threads: its regions are copied through one synchronized accessor, as
            // SiteSearchAnalysis does; the copies are private to the thread
            SequenceAccessor accessor = new SequenceAccessor( chromosome );
            for( Variation var : e.getValue() )
                result.add( new ScoredVariation( result.size(), var, chromosome, accessor ) );
        }
        return result;
    }

    /**
     * Runs the body for every variation in parallel (TaskPool). Each variation is independent and writes only its own
     * index of the results, so they do not depend on the threads. Stops at a cancel request.
     *
     * <p>The site models are shared by the threads, as in SiteSearchAnalysis, which scores with them in parallel too.
     * Their scoring is safe for that: getScore, getMinScore, getLength and getAlphabet read the model; the state of a
     * call is per thread (WeightMatrixModel: lazily built weights under double-checked locking, its letter cache in a
     * ThreadLocal; IPSSiteModel, LogIPSSiteModel: ThreadLocal), and MatchSiteModel and the log/log-odds/custom weight
     * models compute from final data. A new model class used here must keep that.</p>
     * @param jobControl gets the progress, 0-100%, forward only; may be null
     * @return false if cancelled
     */
    private static boolean forEachVariation(List<ScoredVariation> variations, ExceptionalConsumer<ScoredVariation> body,
            JobControl jobControl) throws Exception
    {
        Progress progress = new Progress( jobControl, variations.size() );
        try
        {
            // not given the jobControl: iterate would report the progress from several threads, out of order
            TaskPool.getInstance().iterate( variations, variation -> {
                if( jobControl != null && jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
                    return false;
                try
                {
                    body.accept( variation );
                }
                catch( Throwable t )
                {
                    throw ExceptionRegistry.translateException( t );
                }
                progress.add();
                return true;
            } );
        }
        catch( ExecutionException e )
        {
            throw ExceptionRegistry.translateException( e );
        }
        return jobControl == null || jobControl.getStatus() != JobControl.TERMINATED_BY_REQUEST;
    }

    /**
     * Progress of work done in parallel, forward only. Counting is lock-free; only a new percent is published, in order,
     * under the lock (at most 100 times).
     */
    private static class Progress
    {
        private final JobControl jobControl;
        private final long total;
        private final AtomicLong done = new AtomicLong();
        private volatile int reported = -1;

        Progress(JobControl jobControl, long total)
        {
            this.jobControl = jobControl;
            this.total = Math.max( 1, total );
        }

        void add()
        {
            int percent = (int) ( done.incrementAndGet() * 100 / total );
            if( jobControl == null || percent <= reported )
                return;
            synchronized( this )
            {
                if( percent > reported )
                {
                    reported = percent;
                    jobControl.setPreparedness( percent );
                }
            }
        }
    }

    /**
     * The greatest site loss and gain of each model at each variation: losses[model][i] and gains[model][i], i in the
     * order of the variations in the map (by chromosome, then as listed). Public for tests.
     * @param jobControl gets the progress and may cancel; may be null
     * @return false if cancelled (the scores are then incomplete)
     */
    public static boolean computeDeltas(Map<String, List<Variation>> variationsByChr, Map<String, Sequence> chromosomes,
            SiteModel[] siteModels, int maxModelLength, float[][] losses, float[][] gains, JobControl jobControl) throws Exception
    {
        return forEachVariation( numbered( variationsByChr, chromosomes ), v -> {
            float[] varLosses = new float[siteModels.length], varGains = new float[siteModels.length];
            computeMaxGainLossDeltasForSite( v.var, v.chromosome, v.accessor, siteModels, maxModelLength, varLosses, varGains );
            for( int j = 0; j < siteModels.length; j++ )
            {
                losses[j][v.index] = varLosses[j];
                gains[j][v.index] = varGains[j];
            }
        }, jobControl );
    }
    /** @return false if cancelled: then nothing was written */
    private boolean filterVCF(Track track, Map<String, Sequence> chromosomes, SiteModel[] siteModels, int maxModelLength,
            Map<String, Integer> siteModelToIndex, Scores bgScores, SqlTrack resultTrack)
            throws Exception
    {
        //TODO: here we run again computeMaxGainLossDeltasForSite
        //Think about caching variation-site-gain-loss data
        DataCollection<Site> sites = track.getAllSites();

        SiteModel[] siteModelsTop = StreamEx.of( siteModels ).filter( sm -> siteModelToIndex.containsKey( sm.getName() ) )
                .toArray( SiteModel[]::new );
        Set<String> missingChromosomes = new HashSet<>();
        Map<String, List<Variation>> variationsByChr = new HashMap<>();
        for( Site s : sites )
        {
            String chrName = s.getOriginalSequence().getName();
            Sequence seq = chromosomes.get( chrName );
            if(seq == null)
            {
                if(!missingChromosomes.contains( chrName ))
                {
                    missingChromosomes.add( chrName );
                }
                continue;
            }
            Variation var = null;
            try {
                var = Variation.createFromSite( s );
                if( !CompareTFBSMutations.isCompatible( var, seq ) )
                    var = null;
            } catch(IllegalArgumentException e)
            {
            }
            if(var == null)
                continue;
            variationsByChr
                .computeIfAbsent( chrName, k->new ArrayList<>() )
                .add( var );

        }

        // the weights are computed in parallel, then the sites are written in the former order
        List<ScoredVariation> variations = numbered( variationsByChr, chromosomes );
        double[] weights = new double[variations.size()];
        if( !forEachVariation( variations, v -> {
            float[] gains = new float[siteModelsTop.length];
            float[] losses = new float[siteModelsTop.length];
            computeMaxGainLossDeltasForSite( v.var, v.chromosome, v.accessor, siteModelsTop, maxModelLength, losses, gains );
            int igain = 0, iloss = 0;
            for( int j = 1; j < siteModelsTop.length; j++ )
            {
                if( gains[j] > gains[igain] )
                    igain = j;
                if( losses[j] > losses[iloss] )
                    iloss = j;
            }
            float[] values;
            float cur;
            if( gains[igain] > losses[iloss] )
            {
                cur = gains[igain];
                SiteModel sm = siteModelsTop[igain];
                int smi = siteModelToIndex.get( sm.getName() );
                values = bgScores.gains[smi];
                //count in bg gains
            }
            else
            {
                cur = losses[iloss];
                //count in bg losses
                SiteModel sm = siteModelsTop[iloss];
                int smi = siteModelToIndex.get( sm.getName() );
                values = bgScores.losses[smi];
            }
            int numGreater = 0;
            for( int j = 0; j < values.length; j++ )
                if( values[j] >= cur )
                    numGreater++;
            weights[v.index] = numGreater > 0 ? -Math.log10( (double)numGreater / values.length )
                    : -Math.log10( 1.0 / ( 2.0 * values.length ) );
        }, jobControl ) )
            return false;
        int failed = 0, notFound = 0;
        Exception firstFailure = null;
        String firstFailed = null;
        for( ScoredVariation v : variations )
        {
            if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
                return false; // the caller then neither finalizes nor saves the track
            try
            {
                Site site = sites.get( v.var.id );
                if( site != null )
                {
                    Site si = ( (SiteImpl)site ).clone( resultTrack );
                    si.getProperties().add( new DynamicProperty( CHANGING_PROPERTY, Float.class, weights[v.index] ) );
                    resultTrack.addSite( si );
                }
                else
                    notFound++;
            }
            catch( Exception e )
            {
                // a site that cannot be written is left out, as before, but no longer silently
                if( failed++ == 0 )
                {
                    firstFailure = e;
                    firstFailed = v.var.id;
                }
            }
        }
        if( notFound > 0 )
            log.warning( notFound + " variation(s) were not found in the input track and are not in the filtered track" );
        if( failed > 0 )
            log.log( Level.WARNING, failed + " site(s) could not be written to the filtered track; the first, " + firstFailed + ":",
                    firstFailure );
        return jobControl.getStatus() != JobControl.TERMINATED_BY_REQUEST;
    }

    private Map<String, List<Variation>> subSample(Map<String, List<Variation>> variations, int maxSize)
    {
        int remaining = variations.values().stream().mapToInt(List::size).sum();
        if(remaining <= maxSize)
            return variations;
        int required = maxSize;
        Map<String, List<Variation>> result = new HashMap<>();
        Random rnd = new Random( maxSize );
        for(Map.Entry<String, List<Variation>> e : variations.entrySet())
        {
            String chr = e.getKey();
            List<Variation> varList = e.getValue();
            for(Variation var : varList)
            {
                if( rnd.nextInt( remaining ) < required )
                {
                    result.computeIfAbsent( chr, k->new ArrayList<>() ).add( var );
                    required--;
                }
                remaining--;
            }
        }
        return result;
    }


    private static void computeMaxGainLossDeltasForSite(Variation var, Sequence chrSeq, SequenceAccessor chrAccessor, SiteModel[] siteModels,
            int maxModelLength, float[] resultLosses, float[] resultGains)
    {
        Interval interval = var.grow( maxModelLength ).intersect( chrSeq.getInterval() );
        if( interval == null )
            throw new RuntimeException( "Variation out of chromosome bounds: " + chrSeq.getName() + ":" + var );
        // copied through the accessor (synchronized): the chromosome is shared by the threads, the copy is not
        CachedSequenceRegion reference = chrAccessor.getSubSequence( interval.getFrom(), interval.getLength(), false );
        CachedSequenceRegion referenceRC = reference.getReverseComplement();

        int relativeFrom = reference.translatePositionBack( var.getFrom() );
        Variation relativeVar = new Variation(var.id, var.name, relativeFrom, relativeFrom + var.getLength() - 1, var.ref, var.alt );

        Sequence alternative = Variation.applyVariations( reference, new Variation[] {relativeVar} );
        Sequence alternativeRC = SequenceRegion.getReversedSequence( alternative );

        CoordinateMapping ref2alt = new MappingByVCF( reference.getInterval(), new Variation[] {relativeVar} );
        CoordinateMapping ref2altRC = new ReverseStrandMapping( ref2alt, alternative.getInterval() );
        InverseMapping alt2ref = new InverseMapping( alternative.getInterval(), ref2alt );
        InverseMapping alt2refRC = new InverseMapping( alternativeRC.getInterval(), ref2altRC );

        // This is the hot path of the analysis (every variation x every model). The loss pass scores every reference
        // window and the alternative windows they map to; the gain pass scores every alternative window and the
        // reference windows they map to -- i.e. the same scores again. So each window of the four sequences is scored
        // once per model into a buffer and both passes read from it. The window -> mapped-windows lists depend only on
        // the mapping and the model length, so they are computed once per distinct length rather than once per model.
        // getScore and getMinScore are pure, so the deltas are bit-identical to scoring on demand.
        double[] refScores = new double[reference.getLength()];
        double[] refRCScores = new double[referenceRC.getLength()];
        double[] altScores = new double[alternative.getLength()];
        double[] altRCScores = new double[alternativeRC.getLength()];
        int[][][] ref2altWindows = new int[maxModelLength + 1][][];
        int[][][] ref2altRCWindows = new int[maxModelLength + 1][][];
        int[][][] alt2refWindows = new int[maxModelLength + 1][][];
        int[][][] alt2refRCWindows = new int[maxModelLength + 1][][];

        // The four sequences are three different classes (cached region, reverse-complement region, linear sequence),
        // so the per-letter getLetterAt in the scoring loop is megamorphic and costs an itable dispatch plus position
        // translation per letter -- more than half of the scoring time. Plain array-backed copies (same start, length,
        // alphabet and letters) keep that call site monomorphic. Used only for weight-matrix models with a one-letter
        // code, which read nothing outside the scored window, so their scores are identical. The copies are made only
        // when at least one model can use them (with only a few other models they measurably cost more than they save).
        boolean anyFlat = false;
        for( SiteModel model : siteModels )
            anyFlat |= model instanceof WeightMatrixModel && model.getAlphabet().codeLength() == 1;
        Sequence referenceFlat = anyFlat ? flatten( reference ) : reference;
        Sequence referenceRCFlat = anyFlat ? flatten( referenceRC ) : referenceRC;
        Sequence alternativeFlat = anyFlat ? flatten( alternative ) : alternative;
        Sequence alternativeRCFlat = anyFlat ? flatten( alternativeRC ) : alternativeRC;

        for( int i = 0; i < siteModels.length; i++ )
        {
            SiteModel model = siteModels[i];
            int length = model.getLength();
            boolean flat = model instanceof WeightMatrixModel && model.getAlphabet().codeLength() == 1;
            int refCount = scoreWindows( model, flat ? referenceFlat : reference, refScores );
            int refRCCount = scoreWindows( model, flat ? referenceRCFlat : referenceRC, refRCScores );
            int altCount = scoreWindows( model, flat ? alternativeFlat : alternative, altScores );
            int altRCCount = scoreWindows( model, flat ? alternativeRCFlat : alternativeRC, altRCScores );

            if( ref2altWindows[length] == null )
            {
                ref2altWindows[length] = mapWindows( reference, ref2alt, alternative, length );
                ref2altRCWindows[length] = mapWindows( referenceRC, ref2altRC, alternativeRC, length );
                alt2refWindows[length] = mapWindows( alternative, alt2ref, reference, length );
                alt2refRCWindows[length] = mapWindows( alternativeRC, alt2refRC, referenceRC, length );
            }

            float loss = maxSiteLoss( refScores, refCount, alternative, altScores, altCount, ref2altWindows[length], model );
            float lossRC = maxSiteLoss( refRCScores, refRCCount, alternativeRC, altRCScores, altRCCount, ref2altRCWindows[length],
                    model );
            resultLosses[i] = Math.max( loss, lossRC );

            float gain = maxSiteLoss( altScores, altCount, reference, refScores, refCount, alt2refWindows[length], model );
            float gainRC = maxSiteLoss( altRCScores, altRCCount, referenceRC, refRCScores, refRCCount, alt2refRCWindows[length],
                    model );
            resultGains[i] = Math.max( gain, gainRC );
        }
    }

    /** The windows of a window mapped to no window (or only to windows outside the target). */
    public static final int[] NO_WINDOWS = new int[0];

    /** An array-backed copy of the sequence with the same start, length, alphabet and letters. */
    private static Sequence flatten(Sequence sequence)
    {
        int start = sequence.getStart();
        byte[] letters = new byte[sequence.getLength()];
        for( int k = 0; k < letters.length; k++ )
            letters[k] = sequence.getLetterAt( start + k );
        return new LinearSequence( sequence.getName(), start, letters, sequence.getAlphabet() );
    }

    /**
     * Scores every window of the sequence: scores[k] is the score at position sequence.getStart() + k.
     * @return the number of windows
     */
    private static int scoreWindows(SiteModel siteModel, Sequence sequence, double[] scores)
    {
        int start = sequence.getStart();
        int count = Math.max( 0, sequence.getLength() - siteModel.getLength() + 1 );
        for( int k = 0; k < count; k++ )
            scores[k] = siteModel.getScore( sequence, start + k );
        return count;
    }

    /**
     * For each window of the source, the starts of the windows of the target it maps to.
     *
     * <p>Only windows lying wholly within the target are kept. A window that crosses an indel maps to candidate starts
     * shifted by the indel, and these can reach past the target. Away from the chromosome ends they never do: the scored
     * region extends the longest model length on each side of the variation. Where the region is cut at a chromosome
     * end, they do, and scoring them read past the sequence (ArrayIndexOutOfBoundsException). Such a window would
     * extend past the chromosome, so it is no site. A source window none of whose windows remains counts as one with no
     * mapped window: its site is lost.</p>
     */
    public static int[][] mapWindows(Sequence source, CoordinateMapping mapping, Sequence target, int length) // public for tests
    {
        int start = source.getStart();
        int count = Math.max( 0, source.getLength() - length + 1 );
        int targetFrom = target.getStart(), targetLastStart = target.getStart() + target.getLength() - length;
        int[][] result = new int[count][];
        for( int k = 0; k < count; k++ )
        {
            Collection<Integer> mapped = mapping.mapInterval( new Interval( start + k, start + k + length - 1 ) );
            int[] positions = new int[mapped.size()];
            int j = 0;
            for( Integer pos : mapped )
                if( pos >= targetFrom && pos <= targetLastStart )
                    positions[j++] = pos;
            result[k] = j == 0 ? NO_WINDOWS : j == positions.length ? positions : Arrays.copyOf( positions, j );
        }
        return result;
    }

    private static float maxSiteLoss(double[] refScores, int refCount, Sequence alternative, double[] altScores, int altCount,
            int[][] windows, SiteModel siteModel)
    {
        float res = 0;
        // getMinScore loops over the whole weight matrix, and it is needed only for a window with no mapped window
        // (an indel longer than the model), which most variations never have -- so compute it on first use only.
        double minScore = Double.NaN;
        int altStart = alternative.getStart();
        for( int k = 0; k < refCount; k++ )
        {
            double refScore = refScores[k];
            int[] altPositions = windows[k];
            if( altPositions.length == 0 )
            {
                if( Double.isNaN( minScore ) )
                    minScore = siteModel.getMinScore();
                float delta = (float) ( refScore - minScore );
                if( delta > res )
                    res = delta;
                continue;
            }
            for( int altPos : altPositions )
            {
                int idx = altPos - altStart;
                // A mapped window can start outside the alternative's window range (e.g. a deletion at the edge of
                // the region); score it on demand exactly as before instead of reading the buffer.
                double altScore = idx >= 0 && idx < altCount ? altScores[idx] : siteModel.getScore( alternative, altPos );
                float delta = (float) (refScore - altScore);
                if( delta > res )
                    res = delta;
            }
        }
        return res;
    }

    private void findDifferentiatingModels(float[][] yes, float[][] no, SiteModel[] siteModels, TableDataCollection result)
    {
        for( int i = 0; i < siteModels.length; i++ )
        {
            double minRatio = 1;
            double pvalue = 1;
            double bestCutoff = 0;
            boolean overRepresented = true;
            int yesCount = 0;
            int noCount = 0;

            float[] yesDeltas = yes[i];
            float[] noDeltas = no[i];
            double binomialProb = (double)yesDeltas.length / ( yesDeltas.length + noDeltas.length );

            Arrays.sort( yesDeltas );
            Arrays.sort( noDeltas );
            int iYes = yesDeltas.length - 1, iNo = noDeltas.length - 1;
            while( iYes >= 0 || iNo >= 0 )
            {
                float cutoff = -Float.MAX_VALUE;
                if( iYes >= 0 )
                    cutoff = yesDeltas[iYes];
                if( iNo >= 0 && noDeltas[iNo] > cutoff )
                    cutoff = noDeltas[iNo];

                if( cutoff < MIN_DELTA )
                    break;

                while( iYes >= 0 && yesDeltas[iYes] == cutoff )
                    iYes--;

                while( iNo >= 0 && noDeltas[iNo] == cutoff )
                    iNo--;

                int nYes = yesDeltas.length - iYes - 1;
                int nNo = noDeltas.length - iNo - 1;

                double ratio = (double)nYes / ( nYes + nNo );
                double[] pvalues = Stat.cumulativeBinomialFast( nYes + nNo, nYes, binomialProb );

                if( pvalues[0] <= parameters.getPValueCutoff() && ratio <= minRatio )
                {
                    minRatio = ratio;
                    overRepresented = false;
                    pvalue = pvalues[0];
                    bestCutoff = cutoff;
                    yesCount = nYes;
                    noCount = nNo;
                }
                if( pvalues[1] <= parameters.getPValueCutoff() && ( 1 - ratio ) <= minRatio )
                {
                    minRatio = 1 - ratio;
                    overRepresented = true;
                    pvalue = pvalues[1];
                    bestCutoff = cutoff;
                    yesCount = nYes;
                    noCount = nNo;
                }
            }

            if( overRepresented )
                minRatio = 1 - minRatio;

            if( pvalue <= parameters.getPValueCutoff() )
                TableDataCollectionUtils.addRow( result, siteModels[i].getName(),
                        new Object[] {minRatio, binomialProb, pvalue, bestCutoff, yesCount, noCount}, true );

        }
    }



    public static class Parameters extends CompareTFBSMutations.Parameters
    {
        {
            setGenome( new BasicGenomeSelector() );
        }

        //        private DataElementPath yesVCFTrack;
        //        @PropertyName ( "Experiment VCF track" )
        //        public DataElementPath getYesVCFTrack()
        //        {
        //            return yesVCFTrack;
        //        }
        //        public void setYesVCFTrack(DataElementPath yesVCFTrack)
        //        {
        //            Object oldValue = this.yesVCFTrack;
        //            if( !Objects.equals( oldValue, yesVCFTrack ) )
        //            {
        //                Track t = yesVCFTrack.optDataElement( Track.class );
        //                if( t != null )
        //                    setGenome( new BasicGenomeSelector( t ) );
        //            }
        //            this.yesVCFTrack = yesVCFTrack;
        //            firePropertyChange( "yesVCFTrack", oldValue, yesVCFTrack );
        //        }
        //
        //        private DataElementPath noVCFTrack;
        //        @PropertyName ( "Control VCF track" )
        //        public DataElementPath getNoVCFTrack()
        //        {
        //            return noVCFTrack;
        //        }
        //        public void setNoVCFTrack(DataElementPath noVCFTrack)
        //        {
        //            Object oldValue = this.noVCFTrack;
        //            this.noVCFTrack = noVCFTrack;
        //            firePropertyChange( "noVCFTrack", oldValue, noVCFTrack );
        //        }
        //
        //        private BasicGenomeSelector genome;
        //        @PropertyName ( "Genome" )
        //        @PropertyDescription ( "Reference genome" )
        //        public BasicGenomeSelector getGenome()
        //        {
        //            return genome;
        //        }
        //        public void setGenome(BasicGenomeSelector genome)
        //        {
        //            BasicGenomeSelector oldValue = this.genome;
        //            this.genome = withPropagation( oldValue, genome );
        //            firePropertyChange( "genome", oldValue, genome );
        //        }
        //
        //        private DataElementPath siteModels;
        //        @PropertyName ( "Profile" )
        //        @PropertyDescription ( "Predefined set of site models" )
        //        public DataElementPath getSiteModels()
        //        {
        //            return siteModels;
        //        }
        //        public void setSiteModels(DataElementPath siteModels)
        //        {
        //            Object oldValue = this.siteModels;
        //            this.siteModels = siteModels;
        //            firePropertyChange( "siteModels", oldValue, siteModels );
        //        }
        //
        //        private double pValueCutoff = 0.05;
        //        @PropertyName ( "P-value cutoff" )
        //        public double getPValueCutoff()
        //        {
        //            return pValueCutoff;
        //        }
        //        public void setPValueCutoff(double pValueCutoff)
        //        {
        //            double oldValue = this.pValueCutoff;
        //            this.pValueCutoff = pValueCutoff;
        //            firePropertyChange( "pValueCutoff", oldValue, pValueCutoff );
        //        }
        //
        //        private int inputSizeLimit = 100000;
        //        @PropertyName("Input size limit")
        //        @PropertyDescription("If the input VCF larger than this limit, the VCF track will be randomly subsampled to this size.")
        //        public int getInputSizeLimit()
        //        {
        //            return inputSizeLimit;
        //        }
        //        public void setInputSizeLimit(int inputSizeLimit)
        //        {
        //            int oldValue = this.inputSizeLimit;
        //            this.inputSizeLimit = inputSizeLimit;
        //            firePropertyChange( "inputSizeLimit", oldValue, inputSizeLimit );
        //        }
        //
        //        private DataElementPath siteGainsTable;
        //        @PropertyName ( "Site gains table" )
        //        @PropertyDescription ( "Table with transcription factors which change frequency of site gains between experiment and control" )
        //        public DataElementPath getSiteGainsTable()
        //        {
        //            return siteGainsTable;
        //        }
        //        public void setSiteGainsTable(DataElementPath siteGainsTable)
        //        {
        //            Object oldValue = this.siteGainsTable;
        //            this.siteGainsTable = siteGainsTable;
        //            firePropertyChange( "siteGainsTable", oldValue, siteGainsTable );
        //        }
        //
        //        private DataElementPath siteLossesTable;
        //        @PropertyName ( "Site losses table" )
        //        @PropertyDescription ( "Table with transcription factors which change frequency of site losses between experiment and control" )
        //        public DataElementPath getSiteLossesTable()
        //        {
        //            return siteLossesTable;
        //        }
        //        public void setSiteLossesTable(DataElementPath siteLossesTable)
        //        {
        //            Object oldValue = this.siteLossesTable;
        //            this.siteLossesTable = siteLossesTable;
        //            firePropertyChange( "siteLossesTable", oldValue, siteLossesTable );
        //        }

        private boolean filterVCF;
        @PropertyName ( "Filter input for top sites" )
        @PropertyDescription ( "Create filtered track with variations influencing top site models" )
        public boolean isFilterVCF()
        {
            return filterVCF;
        }
        public void setFilterVCF(boolean filterVCF)
        {
            Object oldValue = this.filterVCF;
            this.filterVCF = filterVCF;
            firePropertyChange( "filterVCF", oldValue, filterVCF );
        }
        public boolean isFilterVCFHidden()
        {
            return !isFilterVCF();
        }

        private int topCount;
        @PropertyName ( "Number of top sites" )
        @PropertyDescription ( "Take top site models from gains and losses tables" )
        public int getTopCount()
        {
            return topCount;
        }
        public void setTopCount(int topCount)
        {
            Object oldValue = this.topCount;
            this.topCount = topCount;
            firePropertyChange( "topCount", oldValue, topCount );
        }

        private DataElementPath filteredVCFTrack;
        @PropertyName ( "Filtered VCF track" )
        @PropertyDescription ( "Path to filtered Yes track" )
        public DataElementPath getFilteredVCFTrack()
        {
            return filteredVCFTrack;
        }
        public void setFilteredVCFTrack(DataElementPath filteredVCFTrack)
        {
            Object oldValue = this.filteredVCFTrack;
            this.filteredVCFTrack = filteredVCFTrack;
            firePropertyChange( "filteredVCFTrack", oldValue, filteredVCFTrack );
        }

        private boolean useOverrepresented = true;
        @PropertyName ( "Overrepresented models" )
        public boolean isUseOverrepresented()
        {
            return useOverrepresented;
        }
        public void setUseOverrepresented(boolean useOverrepresented)
        {
            Object oldValue = this.useOverrepresented;
            this.useOverrepresented = useOverrepresented;
            firePropertyChange( "this.useOverrepresented", oldValue, useOverrepresented );
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
            property( "yesVCFTrack" ).inputElement( VCFSqlTrack.class ).add();
            property( "noVCFTrack" ).inputElement( VCFSqlTrack.class ).add();
            add( "genome" );
            property( "siteModels" ).inputElement( SiteModelCollection.class ).add();
            add( "pValueCutoff" );
            add( "inputSizeLimit" );
            property( "siteGainsTable" ).outputElement( TableDataCollection.class ).auto( "$yesVCFTrack$ site gains" ).add();
            property( "siteLossesTable" ).outputElement( TableDataCollection.class ).auto( "$yesVCFTrack$ site losses" ).add();
            addExpert( "filterVCF" );
            addHidden( "topCount", "isFilterVCFHidden" );
            addHidden( "useOverrepresented", "isFilterVCFHidden" );
            property( "filteredVCFTrack" ).hidden( "isFilterVCFHidden" ).outputElement( VCFSqlTrack.class )
                    .auto( "$yesVCFTrack$ significant" )
                    .add();
        }

    }
}
