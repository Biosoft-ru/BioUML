package biouml.plugins.keynodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;

import biouml.plugins.keynodes.KeyNodeAnalysisParameters.GraphDecoratorEntry;
import biouml.plugins.keynodes.biohub.KeyNodesHub;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.BioHubRegistry.BioHubInfo;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.biohub.TargetOptions.CollectionRecord;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.analysis.type.ProteinTableType;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.analysiscore.AnalysisParametersFactory;
import ru.biosoft.jobcontrol.Iteration;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.Maps;

public class KeyNodeAnalysis extends AnalysisMethodSupport<KeyNodeAnalysisParameters> implements PathGenerator
{
    public static final int NUM_SAMPLES = 1000;
    public static final int MAX_RANK = 50000;

    public KeyNodeAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, JavaScriptKeyNodes.class, new KeyNodeAnalysisParameters());
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        checkNotEmpty("bioHub");
        checkRange("maxRadius", 0, 20);
        KeyNodeAnalysisParameters parameters = getParameters();

        TableDataCollection input = parameters.getSource();
        int total = input.getSize();
        if( total == 0 )
            throw new IllegalArgumentException("Molecule set is empty or was loaded with errors");
        BioHubInfo bhi = parameters.getBioHub();
        if( bhi == null )
        {
            throw new IllegalArgumentException( "No biohub selected" );
        }
        KeyNodesHub<?> bioHub = parameters.getKeyNodesHub();
        if( bioHub == null )
        {
            throw new IllegalArgumentException( "Select custom biohub collection" );
        }
        ReferenceType inputType = ReferenceTypeRegistry.optReferenceType( input.getReferenceType() );
        ReferenceType[] types;
        if( inputType == null || ( types = bioHub.getSupportedMatching( inputType ) ) == null || types.length == 0 )
        {
            ReferenceType[] supportedTypes = bioHub.getSupportedInputTypes();
            String supportedStr = Stream.of( supportedTypes ).map( ReferenceType::getDisplayName ).map( n -> '\t' + n + '\n' )
                    .collect( Collectors.joining() );
            throw new IllegalArgumentException("Search collection " + bioHub.getName() + " does not support objects of given type. \nAcceptable "
                    + ( supportedTypes.length > 1 ? "types are\n" : "type is\n" ) + supportedStr + "Try to convert table first.");
        }
        String weightColumn = parameters.getWeightColumn();
        if( weightColumn != null && !weightColumn.equals("") && !weightColumn.equals(ColumnNameSelector.NONE_COLUMN) )
        {
            if( !input.getColumnModel().getColumn(weightColumn).getType().isNumeric() )
            {
                throw new IllegalArgumentException("Specified column is not numerical");
            }
        }
        for(GraphDecoratorEntry decoratorParameters : parameters.getDecorators())
        {
            if(!decoratorParameters.isAcceptable(bioHub))
            {
                throw new IllegalArgumentException( "Decorator " + decoratorParameters.getDecoratorName() + " is not acceptable for hub "
                        + bioHub.getName() );
            }
        }
    }

    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        KeyNodeAnalysisParameters parameters = getParameters();

        // Update parameters just for the case
        for(GraphDecoratorEntry decoratorEntry : parameters.getDecorators())
        {
            decoratorEntry.getParameters().setKeyNodeParameters( parameters );
        }

        TableDataCollection result = getKeyNodes();
        if( jobControl != null )
            log.log(Level.FINE, "Elapsed time " + jobControl.getElapsedTime());
        if( result == null )
        {
            log.info("Result was not created");
            return null;
        }
        this.writeProperties(result);
        AnalysisParametersFactory.writePersistent( result, this );
        result.getInfo().setNodeImageLocation(getClass(), "resources/keynodes.gif");
        result.getInfo().getProperties().setProperty(DataCollectionUtils.SPECIES_PROPERTY, parameters.getSpecies().getLatinName());

        KeyNodesHub<?> bioHub = parameters.getKeyNodesHub();
        TableDataCollection input = parameters.getSource();
        ReferenceType inputType = ReferenceTypeRegistry.optReferenceType(input.getReferenceType());
        ReferenceType[] types = bioHub.getSupportedMatching(inputType);
        if( types != null && types.length == 1 )
            ReferenceTypeRegistry.setCollectionReferenceType(result, types[0]);
        else
            ReferenceTypeRegistry.setCollectionReferenceType(result, ProteinTableType.class);

        CollectionFactoryUtils.save(result);
        log.info("DataCollection " + result.getName() + " created");
        return result;
    }

    private TableDataCollection getKeyNodes() throws Exception
    {
        final KeyNodesHub<?> bioHub = parameters.getKeyNodesHub();
        try
        {
            boolean isFDR = parameters.isCalculatingFDR();

            TableDataCollection input = parameters.getSource();

            jobControl.pushProgress(0, isFDR?40:96);

            final TargetOptions dbOptions = createTargetOptions( parameters );
            int size = input.getSize();
            if( parameters.isInputSizeLimited() && size > parameters.getInputSizeLimit() )
            {
                log.info( "Too many input " + bioHub.getElementTypeName() + "s. First " + parameters.getInputSizeLimit() + " "
                        + bioHub.getElementTypeName() + "s were selected for further analysis." );
                size = parameters.getInputSizeLimit();
            }

            jobControl.pushProgress(0, 20);
            final Set<String> targetNames = getInputList(input, dbOptions, size);
            if(targetNames.size() == 0)
            {
                log.info("No " + bioHub.getElementTypeName() + "s from input list can be taken for the analysis");
                return null;
            }
            else
            {
                log.info( targetNames.size() + " " + parameters.getKeyNodesHub().getElementTypeName()
                        + "s from input list are taken for the analysis" );
            }
            if(jobControl.isStopped()) return null;
            jobControl.popProgress();

            jobControl.pushProgress(20, 50);
            final Map<String, Float> res = getTargets(targetNames, dbOptions);
            final int totalFound = res.size();
            if( totalFound == 0 )
            {
                log.info("No "+(parameters.getSearchDirection()==BioHub.DIRECTION_UP?"regulator":"effector")+" node candidates found.");
                return null;
            }
            else
            {
                log.info(totalFound + " "+(parameters.getSearchDirection()==BioHub.DIRECTION_UP?"regulator":"effector")+" node candidates found");
            }
            jobControl.popProgress();
            if(jobControl.isStopped()) return null;

            log.info("Calculating scores...");
            jobControl.pushProgress(50, 100);
            final List<KeyNodeStats> values = Collections.synchronizedList(new ArrayList<KeyNodeStats>());
            final TObjectIntMap<String> isoformFactor = parameters.isIsoformFactor()?getIsoformsStatistics(targetNames):null;
            TaskPool.getInstance().iterate(res.entrySet(), entry -> {
                KeyNodeStats keyNodeStats = calculateKeyNodeStats( targetNames, entry.getKey(), entry.getValue(), dbOptions,
                        isoformFactor );
                values.add(keyNodeStats);
                return true;
            }, jobControl);
            if(jobControl.isStopped()) return null;
            calculateScores(values);
            log.info("Filtering by score cutoff...");
            int nValues = values.size();
            filterScores(values);
            log.info(values.size()+" of "+nValues+" molecules passed the filter");
            jobControl.popProgress();
            if(jobControl.isStopped()) return null;

            jobControl.popProgress();

            int[] ranksSum = null;
            if( isFDR )
            {
                log.info("Calculating FDR...");
                jobControl.pushProgress(40, 96);
                String[] names = new String[values.size()];
                float[] scores = new float[values.size()];
                for(int i=0; i<values.size(); i++)
                {
                    names[i] = values.get(i).getAccession();
                    scores[i] = values.get(i).getScore();
                }
                double[][] fdr = getFDR(names, scores, targetNames.size(), dbOptions);
                if(jobControl.isStopped()) return null;
                if( fdr == null )
                {
                    log.info("Can not calculate, saving result without FDR...");
                    isFDR = false;
                }
                log.info("Filtering by FDR/Z-Score cutoffs...");
                nValues = values.size();
                filterByFDR(fdr, values);
                log.info(values.size()+" of "+nValues+" molecules passed the filter");
                ranksSum = getRanksSum(values);
                jobControl.popProgress();
            }
            if(jobControl.isStopped()) return null;

            log.info("Generating output...");
            TableDataCollection result = TableDataCollectionUtils.createTableDataCollection(parameters.getOutputTable());
            ColumnModel columnModel = result.getColumnModel();
            columnModel.addColumn("Master molecule name", String.class);
            columnModel.addColumn("Maximal radius", Double.class);
            columnModel.addColumn("Reached from set", Integer.class);
            columnModel.addColumn("Reachable total", Integer.class);
            columnModel.addColumn("Score", Double.class);
            if( isFDR )
            {
                columnModel.addColumn("FDR", Double.class);
                columnModel.addColumn("Z-Score", Double.class);
                columnModel.addColumn("Ranks sum", Integer.class);
            }
            int fdrCol = 5;
            columnModel.addColumn("Hits", StringSet.class).setHidden(true);
            columnModel.addColumn("Hit names", StringSet.class);

            for( int i = 0; i < values.size(); i++ )
            {
                KeyNodeStats info = values.get(i);
                Object[] rowValues = new Object[] {
                        info.getTitle(),
                        info.getRadius(),
                        info.getNumReached(),
                        info.getNumReachable(),
                        info.getScore(),
                        info.getHits(),
                        getHitsTitles(info.getHits(), bioHub)
                };
                if( isFDR )
                {
                    Object[] valuesFdr = new Object[rowValues.length + 3];
                    System.arraycopy(rowValues, 0, valuesFdr, 0, fdrCol);
                    valuesFdr[fdrCol] = info.getFdrPvalue();
                    valuesFdr[fdrCol + 1] = info.getZScore();
                    valuesFdr[fdrCol + 2] = ranksSum[i];
                    System.arraycopy(rowValues, fdrCol, valuesFdr, fdrCol+3, rowValues.length - fdrCol);
                    TableDataCollectionUtils.addRow(result, info.getAccession(), valuesFdr, true);
                }
                else
                {
                    TableDataCollectionUtils.addRow(result, info.getAccession(), rowValues, true);
                }
            }
            result.finalizeAddition();
            if(isFDR)
            {
                TableDataCollectionUtils.setSortOrder(result, "Ranks sum", true);
            } else
            {
                TableDataCollectionUtils.setSortOrder(result, "Score", false);
            }
            return result;
        }
        finally
        {
            mol2iso = new HashMap<>();    // Clear to spare space
        }
    }

    /**
     * @param values KeyNodeStats
     * @return vector of sums of Z-Score and Score ranks
     */
    protected static int[] getRanksSum(List<KeyNodeStats> values)
    {
        class Pair implements Comparable<Pair>
        {
            double key;
            int value;

            public Pair(double key, int value)
            {
                this.key = key;
                this.value = value;
            }

            @Override
            public int compareTo(Pair o)
            {
                return Double.compare( o.key, key );
            }
        }
        int[] result = new int[values.size()];
        Pair[] scoreRanks = EntryStream.of( values ).mapKeyValue( (i, val) -> new Pair( val.getScore(), i ) ).sorted()
                .toArray( Pair[]::new );
        Pair[] zScoreRanks = EntryStream.of( values ).mapKeyValue( (i, val) -> new Pair( val.getZScore(), i ) ).sorted()
                .toArray( Pair[]::new );
        for(int i=0; i<values.size(); i++)
        {
            result[scoreRanks[i].value] += i;
            result[zScoreRanks[i].value] += i;
        }
        return result;
    }

    /**
     * @param fdr
     * @param values
     */
    protected void filterByFDR(double[][] fdr, List<KeyNodeStats> values)
    {
        for(int i=values.size()-1; i>=0; i--)
        {
            if(fdr[i][0] > parameters.getFDRcutoff() || fdr[i][1] < parameters.getZScoreCutoff())
            {
                values.remove(i);
            } else
            {
                values.get(i).setFdrPvalue((float)fdr[i][0]);
                values.get(i).setZScore((float)fdr[i][1]);
            }
        }
    }

    /**
     * Filters scores by scoreCutoff
     * @param values
     */
    protected void filterScores(List<KeyNodeStats> values)
    {
        values.removeIf( stat -> stat.getScore() < parameters.getScoreCutoff() );
    }

    /**
     * Calculate key node scores based on stats
     * @param values
     */
    public void calculateScores(List<KeyNodeStats> values)
    {
        calculateScores( values, parameters.getMaxRadius(), parameters.getPenalty() );
    }
    public static void calculateScores(List<KeyNodeStats> values, int maxRadius, double penalty)
    {
        float[] maxN = new float[maxRadius];
        float[] maxM = new float[maxRadius];

        for( KeyNodeStats stats : values )
        {
            for( int k = 0; k < maxRadius; k++ )
            {
                if( maxN[k] < stats.getScoreN()[k] ) maxN[k] = stats.getScoreN()[k];
                if( maxM[k] < stats.getScoreM()[k] ) maxM[k] = stats.getScoreM()[k];
            }
        }

        for( KeyNodeStats stats : values )
        {
            float score = 0;
            for( int k = 0; k < maxRadius; k++ )
            {
                if( maxN[k] != 0 && maxM[k] != 0 )
                    score += stats.getScoreM()[k] / ( 1 + penalty * stats.getScoreN()[k] / maxN[k] ) / maxM[k];
            }
            stats.setScore( score / maxRadius );
        }
    }

    public static StringSet getHitsTitles(StringSet hits, KeyNodesHub<?> bioHub)
    {
        return hits.stream().map( Element::new ).map( bioHub::getElementTitle ).sorted( String::compareTo )
                .collect( Collectors.toCollection( StringSet::new ) );
    }

    public double[][] getFDR(String[] names, float[] scores2, int inputSetSize, TargetOptions dbOptions)
            throws Exception
    {
        log.info("Generating "+NUM_SAMPLES+" random sets...");
        jobControl.pushProgress(0, 10);
        Map<String, int[]> randomSet = getRandomSet(inputSetSize, NUM_SAMPLES, parameters.getKeyNodesHub(), dbOptions);
        jobControl.popProgress();
        if(jobControl.isStopped() || randomSet.size() == 0) return null;

        List<TObjectIntMap<String>> isoformFactors = null;
        if( parameters.isIsoformFactor() )
        {
            jobControl.pushProgress(10, 50);
            log.info("Gathering isoform statistics...");
            isoformFactors = gatherIsoformStatistics(randomSet);
            jobControl.popProgress();
        }
        if(jobControl.isStopped()) return null;

        jobControl.pushProgress(parameters.isIsoformFactor()?50:10, 100);

        log.info("Running search from random sets...");
        jobControl.pushProgress(0, 40);
        String[] resNames = getTargets(randomSet.keySet(), dbOptions).keySet().stream().toArray( String[]::new );
        if(jobControl.isStopped()) return null;
        TObjectIntMap<String> resNamesMap = new TObjectIntHashMap<>();
        for( int i = 0; i < resNames.length; i++ )
        {
            resNamesMap.put( resNames[i], i );
        }
        jobControl.popProgress();
        if(jobControl.isStopped()) return null;

        jobControl.pushProgress(40, 98);
        log.info( "Calculating ranks..." );
        int[][] ranks = calculateRankMatrix(resNames, calculateScores(resNames, randomSet, dbOptions, isoformFactors));
        jobControl.popProgress();
        double[][] fdr = new double[names.length][2];
        Integer[] index = new Integer[names.length];
        int maxRank = Math.min(MAX_RANK, names.length);
        Arrays.setAll( index, j -> j );
        Arrays.sort( index, new ScoresComparator(scores2) );
        for( int rank = 1; rank <= maxRank; rank++ )
        {
            int curIndex = index[rank - 1];
            String name = names[curIndex];
            if(resNamesMap.containsKey(name))
            {
                int[] curRanks = ranks[resNamesMap.get(name)];
                fdr[curIndex] = calculateFDR(rank, curRanks);
            }
            else
            {
                fdr[curIndex] = new double[] {0.0, 1.0};
            }
        }
        for( int j = maxRank; j < names.length; j++ )
        {
            int curIndex = index[j];
            fdr[curIndex] = new double[] {1.0, 1.0};
        }
        jobControl.popProgress();
        return fdr;
    }

    private ConcurrentMap<String, Float> getTargets(Set<String> inputSet, final TargetOptions dbOptions) throws Exception
    {
        return getTargets( inputSet, dbOptions, parameters.getKeyNodesHub(), getRelationTypes(), parameters.getMaxRadius(),
                parameters.getSearchDirection(), jobControl );
    }

    public static ConcurrentMap<String, Float> getTargets(Set<String> inputSet, final TargetOptions dbOptions, KeyNodesHub<?> bioHub,
            final String[] relTypes, int maxRadius, int searchDirection, @CheckForNull JobControl jobControl) throws Exception
    {
        final ConcurrentMap<String, Float> targets = new ConcurrentHashMap<>();
        Iteration<String> iteration = name -> {
            Element[] nodes = bioHub.getReference( new Element( "stub/%//" + name ), dbOptions, relTypes, maxRadius, searchDirection );
            if( nodes != null )
            {
                for( Element node : nodes )
                    targets.merge( node.getAccession(), node.getLinkedLength(), Float::max );
            }
            return true;
        };

        if( jobControl != null )
            TaskPool.getInstance().iterate( inputSet, iteration, jobControl );
        else
            TaskPool.getInstance().iterate( inputSet, iteration );
        return targets;
    }

    public List<TObjectIntMap<String>> gatherIsoformStatistics(Map<String, int[]> randomSet)
    {
        List<List<String>> sets = new ArrayList<>();
        for( int i = 0; i < NUM_SAMPLES; i++ )
            sets.add(new ArrayList<>());
        EntryStream.of( randomSet ).flatMapValues( vals -> IntStream.of( vals ).boxed() )
                .forKeyValue( (acc, n) -> sets.get( n ).add( acc ) );
        final List<TObjectIntMap<String>> isoformFactors = new ArrayList<>();
        jobControl.forCollection(sets, set -> {
            isoformFactors.add(getIsoformsStatistics(set));
            return true;
        });
        return isoformFactors;
    }

    private double[] calculateFDR(int rank, int[] ranks)
    {
        int ng = 0;
        double E_X_square = 0;
        double E_X = 0;
        double total = ranks.length;
        for( int rank2 : ranks )
        {
            if( rank2 <= rank )
                ng++;
            E_X += rank2 / total;
            E_X_square += rank2 * rank2 / total;
        }
        double[] fdr = new double[2];

        fdr[0] = ng / total;
        if( E_X_square - E_X * E_X > 0 )
            fdr[1] = ( E_X - rank ) / Math.sqrt(E_X_square - E_X * E_X);
        else
            fdr[1] = ( MAX_RANK - rank * total ) / (MAX_RANK * Math.sqrt( total - 1 ));
        return fdr;
    }

    // For FDR: calculated twice to spare space
    private void calcNM(String keyNode, Map<String, int[]> randomSet, TargetOptions dbOptions, List<TObjectIntMap<String>> isoformFactors, int[] scoreN, float[][] scoreM)
    {
        int direction = parameters.getReverseDirection();
        int maxRadius = parameters.getMaxRadius();
        BioHub bioHub = parameters.getKeyNodesHub();
        Element keyNodeElement = new Element("stub/%//" + keyNode);
        String[] relTypes = getRelationTypes();

        int[] ind = randomSet.get(keyNode);
        if( ind != null )
        {
            for( int k : ind )
            {
                /*double weight = 1;
                int isoformFactor = isoformFactors == null?0:isoformFactors.get(k).get(keyNode);
                if( isoformFactor != 0 )
                    weight /= isoformFactor;*/
                for(int j=0; j<maxRadius; j++)
                {
                    scoreM[j][k] = 1;
                }
            }
        }

        Element[] reached = bioHub.getReference(keyNodeElement, dbOptions, relTypes, maxRadius, direction);
        if( reached == null ) reached = new Element[0];
        for( Element element : reached )
        {
            int reachableRadius = Math.max(0, (int)(Math.floor(element.getLinkedLength()-KeyNodeAnalysis.WEIGHT_PRECISION)));
            for(int j=reachableRadius; j<maxRadius; j++) scoreN[j]++;
            ind = randomSet.get(element.getAccession());
            if( ind != null )
            {
                for( int k : ind )
                {
                    double weight = 1;
                    int isoformFactor = isoformFactors == null?0:isoformFactors.get(k).get(element.getAccession());
                    if( isoformFactor != 0 )
                        weight /= isoformFactor;
                    for( int j = reachableRadius; j < maxRadius; j++ )
                        scoreM[j][k] += weight;
                }
            }
        }
    }

    public float[][] calculateScores(final String[] names, final Map<String, int[]> randomSet, final TargetOptions dbOptions, final List<TObjectIntMap<String>> isoformFactors) throws Exception
    {
        final int maxRadius = parameters.getMaxRadius();
        final double penalty = parameters.getPenalty();

        final int[] maxN = new int[maxRadius];
        final float[][] maxM = new float[maxRadius][NUM_SAMPLES];
        log.info("Gathering maximums...");
        final AtomicInteger cntResult = new AtomicInteger();
        TaskPool.getInstance().executeMultiple(() -> {
            while(true)
            {
                int curKeyNode = cntResult.getAndIncrement();
                if(curKeyNode >= names.length) break;
                int[] scoreN = new int[maxRadius];
                float[][] scoreM = new float[maxRadius][NUM_SAMPLES];
                calcNM(names[curKeyNode], randomSet, dbOptions, isoformFactors, scoreN, scoreM);
                synchronized(maxN)
                {
                    for(int j=0; j<maxRadius; j++)
                    {
                        if(maxN[j] < scoreN[j]) maxN[j] = scoreN[j];
                        for(int i=0; i<NUM_SAMPLES; i++)
                        {
                            if(maxM[j][i] < scoreM[j][i]) maxM[j][i] = scoreM[j][i];
                        }
                    }
                }
                jobControl.setPreparedness(curKeyNode*50/names.length);
            }
        });

        cntResult.set(0);
        log.info("Calculating scores...");
        final float[][] score = new float[NUM_SAMPLES][names.length];
        TaskPool.getInstance().executeMultiple(() -> {
            while(true)
            {
                int curKeyNode = cntResult.getAndIncrement();
                if(curKeyNode >= names.length) break;
                int[] scoreN = new int[maxRadius];
                float[][] scoreM = new float[maxRadius][NUM_SAMPLES];
                calcNM(names[curKeyNode], randomSet, dbOptions, isoformFactors, scoreN, scoreM);
                for(int i=0; i<NUM_SAMPLES; i++)
                {
                    float curScore = 0;
                    for(int j=0; j<maxRadius; j++)
                    {
                        if(maxN[j] != 0 && maxM[j][i] != 0)
                            curScore += scoreM[j][i]/(1+penalty*scoreN[j]/maxN[j])/maxM[j][i];
                    }
                    score[i][curKeyNode] = curScore / maxRadius;
                }
                jobControl.setPreparedness(50+curKeyNode*50/names.length);
            }
        });
        return score;
    }

    private static int[][] calculateRankMatrix(String[] names, float[][] score)
    {
        int total = names.length;
        int[][] ranks = new int[total][NUM_SAMPLES];
        Integer[] index = new Integer[total];
        int maxRank = total > MAX_RANK ? MAX_RANK : total;
        Arrays.setAll( index, j -> j );
        for(int i=0; i<NUM_SAMPLES; i++)
        {
            Integer[] curIndex = new Integer[total];
            System.arraycopy(index, 0, curIndex, 0, total);
            Arrays.sort(curIndex, new ScoresComparator(score[i]));
            for( int rank = 1; rank <= maxRank; rank++ )
            {
                ranks[curIndex[rank - 1]][i] = rank;
            }
            for( int j = maxRank; j < total; j++ )
            {
                ranks[curIndex[j]][i] = MAX_RANK;
            }
        }
        return ranks;
    }

    public KeyNodeStats calculateKeyNodeStats(Set<String> targets, String keyNode, float radius, TargetOptions dbOptions,
            TObjectIntMap<String> isoformFactor)
    {
        int direction = parameters.getReverseDirection();
        int maxRadius = parameters.getMaxRadius();
        KeyNodesHub<?> bioHub = parameters.getKeyNodesHub();
        String weightColumn = parameters.getWeightColumn();
        TableDataCollection source = parameters.getSource();
        return calculateKeyNodeStats( targets, keyNode, radius, bioHub, maxRadius, direction, dbOptions, getRelationTypes(), isoformFactor,
                weightColumn, source );
    }
    public static KeyNodeStats calculateKeyNodeStats(Set<String> targets, String keyNode, float radius, KeyNodesHub<?> bioHub,
            int maxRadius, int reverseDirection, TargetOptions dbOptions, final String[] relTypes,
            TObjectIntMap<String> isoformFactor, String weightColumn, TableDataCollection source)
    {
        int columnIndex = -1;
        if( source != null && weightColumn != null && !weightColumn.isEmpty() && !weightColumn.equals( ColumnNameSelector.NONE_COLUMN ) )
        {
            columnIndex = source.getColumnModel().optColumnIndex( weightColumn );
        }

        Element keyNodeElement = new Element( "stub/%//" + keyNode );
        Set<String> hits = new TreeSet<>();
        int reachedFromSet = 0;
        int reachedTotal = 0;
        float[] scoreM = new float[maxRadius];
        float[] scoreN = new float[maxRadius];
        if( targets.contains( keyNode ) )
        {
            reachedFromSet = 1;
            hits.add( keyNode );
            reachedTotal = 1;

            float weight = 1;
            if( columnIndex != -1 )
            {
                weight = Float.parseFloat( TableDataCollectionUtils.getRowValues( source, keyNode )[columnIndex].toString() );
            }
            if( isoformFactor != null )
                weight /= isoformFactor.get( keyNode );
            for( int j = 0; j < maxRadius; j++ )
            {
                scoreM[j] = weight;
                scoreN[j] = 1;
            }
        }

        Element[] reached = bioHub.getReference( keyNodeElement, dbOptions, relTypes, maxRadius, reverseDirection );
        if( reached != null && reached.length > 0 )
        {
            reachedTotal += reached.length;
            for( Element element : reached )
            {
                String elTargetAcc = element.getAccession();
                if( elTargetAcc.equals( keyNode ) )
                {
                    reachedTotal--;
                    continue;
                }
                int reachableRadius = Math.max( 0, (int) ( Math.floor( element.getLinkedLength() - KeyNodeAnalysis.WEIGHT_PRECISION ) ) );
                for( int j = reachableRadius; j < maxRadius; j++ )
                    scoreN[j]++;
                if( targets.contains( elTargetAcc ) )
                {
                    double weight = 1;
                    if( columnIndex != -1 )
                        weight = Double.parseDouble( TableDataCollectionUtils.getRowValues( source, elTargetAcc )[columnIndex].toString() );
                    if( isoformFactor != null )
                        weight /= isoformFactor.get( elTargetAcc );
                    for( int j = reachableRadius; j < maxRadius; j++ )
                        scoreM[j] += weight;

                    reachedFromSet++;
                    hits.add( elTargetAcc );
                }
            }
        }
        return new KeyNodeStats( keyNode, bioHub.getElementTitle( keyNodeElement ), radius, reachedFromSet, reachedTotal,
                new StringSet( hits ), scoreM, scoreN );
    }

    Map<String, String> mol2iso = new HashMap<>();
    /**
     * Precision of weight sum when comparing to radius
     */
    public static final double WEIGHT_PRECISION = 0.00005;
    public TObjectIntMap<String> getIsoformsStatistics(Collection<String> molecules)
    {
        return getIsoformsStatistics( molecules, parameters.getKeyNodesHub(), mol2iso );
    }
    public static TObjectIntMap<String> getIsoformsStatistics(Collection<String> molecules, KeyNodesHub<?> bioHub,
            Map<String, String> mol2isoCache)
    {
        TObjectIntMap<String> iso2num = new TObjectIntHashMap<>();
        TObjectIntMap<String> mol2num = new TObjectIntHashMap<>();
        for( String molName : molecules )
        {
            String parentIsoform = mol2isoCache.computeIfAbsent( molName, k -> bioHub.getParentIsoform( k ) );
            iso2num.adjustOrPutValue( parentIsoform, 1, 1 );
        }
        for( String mol : molecules )
        {
            String iso = mol2isoCache.get( mol );
            mol2num.put( mol, iso == null ? 1 : iso2num.get( iso ) );
        }
        return mol2num;
    }

    private Set<String> getInputList(DataCollection<?> input, TargetOptions dbOptions, int inputSizeLimit)
    {
        final String[] relTypes = getRelationTypes();
        Set<String> names = parameters.getKeyNodesHub().getNames( dbOptions, relTypes );
        return input.names().filter( names::contains ).limit( inputSizeLimit ).collect( Collectors.toSet() );
    }

    public Map<String, int[]> getRandomSet(int sampleSize, int sampleNum, KeyNodesHub<?> bioHub, TargetOptions dbOptions)
    {
        SplittableRandom random = parameters.getSeed() == 0 ? new SplittableRandom() : new SplittableRandom( parameters.getSeed() );
        final String[] relTypes = getRelationTypes();
        Map<String, TIntList> randSet = new HashMap<>();
        for( int j = 0; j < sampleNum; j++ )
        {
            Element[] sample = bioHub.getRandomSample( sampleSize, dbOptions, relTypes, random::nextInt );
            if(sample.length == 0)
            {
                log.info("Can not get random set, FDR calculation will be skipped");
                return Collections.emptyMap();
            }

            if( sample.length < sampleSize ) //Not enough
            {
                log.info("Not enough elements taken in random set, FDR values might be incorrect!");
            }
            for( Element element : sample )
            {
                randSet.computeIfAbsent( element.getAccession(), k -> new TIntArrayList(1) ).add( j );
            }
        }
        return Maps.transformValues( randSet, TIntList::toArray );
    }

    protected String[] getRelationTypes()
    {
        if( getKeyNodesHub().isRelationSignSupported() )
            return new String[] {parameters.getSpecies().getLatinName(), parameters.getRelationSign()};
        else
            return new String[] {parameters.getSpecies().getLatinName()};
    }

    public static TargetOptions createTargetOptions(KeyNodeAnalysisParameters parameters)
    {
        return new TargetOptions( StreamEx.of( parameters.getDecorators() ).map( GraphDecoratorEntry::createCollectionRecord )
                .prepend( new CollectionRecord( KeyNodesHub.KEY_NODES_HUB, true ) ).toArray( CollectionRecord[]::new ) );
    }

    public static TargetOptions getDBOptions()
    {
        return new TargetOptions( new CollectionRecord( KeyNodesHub.KEY_NODES_HUB, true ) );
    }

    public static class KeyNodeStats
    {
        float radius;
        int numReached;
        int numReachable;
        StringSet hits;
        String title;
        String accession;
        /**
         * M[k] vector - number of hits reached for radius k
         */
        private final float[] scoreM;
        /**
         * N[k] vector - total number of molecules reachable for radius k
         */
        private final float[] scoreN;
        private float score;

        private float fdrPvalue;
        private float ZScore;

        KeyNodeStats(String accession, String title, float radius, int reached, int reachable, StringSet hits, float[] scoreM,
                float[] scoreN)
        {
            this.title = title;
            this.accession = accession;
            this.radius = radius;
            numReached = reached;
            numReachable = reachable;
            this.hits = hits;
            this.scoreN = scoreN;
            this.scoreM = scoreM;
        }

        public int getNumReached()
        {
            return numReached;
        }

        public float getRadius()
        {
            return radius;
        }

        public int getNumReachable()
        {
            return numReachable;
        }

        public StringSet getHits()
        {
            return hits;
        }

        public float[] getScoreM()
        {
            return scoreM;
        }

        public float[] getScoreN()
        {
            return scoreN;
        }

        public String getTitle()
        {
            return title;
        }

        public String getAccession()
        {
            return accession;
        }

        public float getScore()
        {
            return score;
        }

        public void setScore(float score)
        {
            this.score = score;
        }

        public float getFdrPvalue()
        {
            return fdrPvalue;
        }

        public void setFdrPvalue(float fdrPvalue)
        {
            this.fdrPvalue = fdrPvalue;
        }

        public float getZScore()
        {
            return ZScore;
        }

        public void setZScore(float zScore)
        {
            ZScore = zScore;
        }
    }

    private static class ScoresComparator implements Comparator<Integer>
    {
        float[] base;
        public ScoresComparator(float[] score)
        {
            this.base = score;
        }

        @Override
        public int compare(Integer a, Integer b)
        {
            return Float.compare( base[b], base[a] );
        }
    }

    @Override
    public List<Element[]> generatePaths(String startElement, StringSet hits)
    {
        Element element = new Element( "stub/%//" + startElement );
        Element[] hitsElements = hits.stream().map( hit -> new Element( "stub/%//" + hit ) ).toArray( Element[]::new );
        return parameters.getKeyNodesHub().getMinimalPaths( element, hitsElements, createTargetOptions( parameters ), getRelationTypes(),
                parameters.getMaxRadius(), parameters.getReverseDirection() );
    }

    @Override
    public StringSet getKeysFromName(String name)
    {
        return StreamEx.of( name ).toCollection( StringSet::new );
    }

    @Override
    public List<Element> getAllReactions(String startElement, StringSet hits)
    {
        Element element = new Element( "stub/%//" + startElement );
        Element[] hitsElements = StreamEx.of( hits ).map( n -> new Element( "stub/%//" + n ) ).toArray( Element[]::new );
        return parameters.getKeyNodesHub().getAllReactions( element, hitsElements, createTargetOptions( parameters ), getRelationTypes(),
                parameters.getMaxRadius(), parameters.getReverseDirection() );
    }

    @Override
    public KeyNodesHub<?> getKeyNodesHub()
    {
        return parameters.getKeyNodesHub();
    }
}
