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
import ru.biosoft.analysis.Stat;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Interval;
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

        log.info( "Computing site gain/loss scores for control track" );
        jobControl.pushProgress( 25, 50 );
        Track noTrack = parameters.getNoVCFTrack().getDataElement( Track.class );
        Scores noScores = computeMaxGainLossDeltas( noTrack, chromosomes, siteModels, maxModelLength );
        jobControl.popProgress();

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
            filterVCF( yesTrack, chromosomes, siteModels, maxModelLength, model2index, noScores, filteredYesTrack );
            filteredYesTrack.finalizeAddition();
            CollectionFactoryUtils.save( filteredYesTrack );
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
    private Scores computeMaxGainLossDeltas(Track track, Map<String, Sequence> chromosomes, SiteModel[] siteModels, int maxModelLength)
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
        float[] gains = new float[siteModels.length];
        float[] losses = new float[siteModels.length];

        int i = 0;
        for( Map.Entry<String, List<Variation>> e : variationsByChr.entrySet() )
        {
            String chrName = e.getKey();
            Sequence seq = chromosomes.get( chrName );
            for( Variation var : e.getValue() )
            {
                jobControl.setPreparedness( (int) ( (double)i * 100 / siteCount ) );
                computeMaxGainLossDeltasForSite( var, seq, siteModels, maxModelLength, losses, gains );
                for( int j = 0; j < siteModels.length; j++ )
                {
                    result.losses[j][i] = losses[j];
                    result.gains[j][i] = gains[j];
                }
                i++;
            }
        }

        return result;
    }
    private void filterVCF(Track track, Map<String, Sequence> chromosomes, SiteModel[] siteModels, int maxModelLength,
            Map<String, Integer> siteModelToIndex, Scores bgScores, SqlTrack resultTrack)
    {
        //TODO: here we run again computeMaxGainLossDeltasForSite
        //Think about caching variation-site-gain-loss data
        DataCollection<Site> sites = track.getAllSites();

        SiteModel[] siteModelsTop = StreamEx.of( siteModels ).filter( sm -> siteModelToIndex.containsKey( sm.getName() ) )
                .toArray( SiteModel[]::new );
        int siteCount = 0;
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

            siteCount++;
        }

        float[] gains = new float[siteModelsTop.length];
        float[] losses = new float[siteModelsTop.length];

        int i = 0;
        for(Map.Entry<String, List<Variation>> e : variationsByChr.entrySet())
        {
            String chrName = e.getKey();
            Sequence seq = chromosomes.get( chrName );
            for(Variation var : e.getValue())
            {
                jobControl.setPreparedness( (int)((double)i * 100 / siteCount) );
                computeMaxGainLossDeltasForSite( var, seq, siteModelsTop, maxModelLength, losses, gains );
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
                double weight = numGreater > 0 ? -Math.log10( (double)numGreater / values.length )
                        : -Math.log10( 1.0 / ( 2.0 * values.length ) );
                try
                {
                    Site site = sites.get( var.id );
                    if( site != null )
                    {
                        Site si = ( (SiteImpl)site ).clone( resultTrack );
                        si.getProperties().add( new DynamicProperty( CHANGING_PROPERTY, Float.class, weight ) );
                        resultTrack.addSite( si );
                    }
                }
                catch( Exception e1 )
                {
                }
                i++;
            }
        }
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


    private void computeMaxGainLossDeltasForSite(Variation var, Sequence chrSeq, SiteModel[] siteModels, int maxModelLength, float[] resultLosses,
            float[] resultGains)
    {
        Interval interval = var.grow( maxModelLength ).intersect( chrSeq.getInterval() );
        if( interval == null )
            throw new RuntimeException( "Variation out of chromosome bounds: " + chrSeq.getName() + ":" + var );
        CachedSequenceRegion reference = new CachedSequenceRegion( chrSeq, interval.getFrom(), interval.getLength(), false );
        CachedSequenceRegion referenceRC = reference.getReverseComplement();

        int relativeFrom = reference.translatePositionBack( var.getFrom() );
        Variation relativeVar = new Variation(var.id, var.name, relativeFrom, relativeFrom + var.getLength() - 1, var.ref, var.alt );

        Sequence alternative = Variation.applyVariations( reference, new Variation[] {relativeVar} );
        Sequence alternativeRC = SequenceRegion.getReversedSequence( alternative );

        CoordinateMapping ref2alt = new MappingByVCF( reference.getInterval(), new Variation[] {relativeVar} );
        CoordinateMapping ref2altRC = new ReverseStrandMapping( ref2alt, alternative.getInterval() );
        InverseMapping alt2ref = new InverseMapping( alternative.getInterval(), ref2alt );
        InverseMapping alt2refRC = new InverseMapping( alternativeRC.getInterval(), ref2altRC );

        for( int i = 0; i < siteModels.length; i++ )
        {
            SiteModel model = siteModels[i];
            float loss = maxSiteLoss( reference, alternative, ref2alt, model );
            float lossRC = maxSiteLoss( referenceRC, alternativeRC, ref2altRC, model );
            resultLosses[i] = Math.max( loss, lossRC );

            float gain = maxSiteLoss( alternative, reference, alt2ref, model );
            float gainRC = maxSiteLoss( alternativeRC, referenceRC, alt2refRC, model );
            resultGains[i] = Math.max( gain, gainRC );
        }
    }

    private float maxSiteLoss(Sequence reference, Sequence alternative, CoordinateMapping mapping, SiteModel siteModel)
    {
        float res = 0;
        for( int pos = reference.getStart(); pos < reference.getStart() + reference.getLength() - siteModel.getLength() + 1; pos++ )
        {
            double refScore = siteModel.getScore( reference, pos );
            Collection<Integer> altPositions = mapping.mapInterval( new Interval( pos, pos + siteModel.getLength() - 1 ) );
            if( altPositions.isEmpty() )
            {
                float delta = (float) ( refScore - siteModel.getMinScore() );
                if( delta > res )
                    res = delta;
                continue;
            }
            for( Integer altPos : altPositions )
            {
                double altScore = siteModel.getScore( alternative, altPos );
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
