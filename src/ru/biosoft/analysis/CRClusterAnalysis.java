package ru.biosoft.analysis;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Vector;

import one.util.streamex.DoubleStreamEx;
import one.util.streamex.StreamEx;

import org.apache.commons.text.StringEscapeUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.columnbeans.ColumnGroup;

@ClassIcon("resources/CRC-analysis.gif")
public class CRClusterAnalysis extends MicroarrayAnalysis<CRClusterAnalysisParameters>
{
    public static final int FACTOR_0 = 1;
    public static final int MAX_LENGTH = 150;

    Vector<Cluster> clusters;
    Cluster[] member;
    boolean[] sign;
    double[] average;
    int[] shift;
    double[][] expre;

    public CRClusterAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new CRClusterAnalysisParameters());
    }

    @Override
    public void validateParameters()
    {
        ColumnGroup experimentData = parameters.getExperimentData();
        if( experimentData == null || experimentData.getColumns().length == 0 )
            throw new IllegalArgumentException("Please specify experiment columns");
        if( experimentData.getTable() == null )
            throw new IllegalArgumentException("Please specify experiment table");

        DataElementPath path = parameters.getOutputTablePath();
        if( path == null || path.optParentCollection() == null || path.getParentCollection().getName().isEmpty() )
            throw new IllegalArgumentException("Please specify output collection");

        if( path.getName().isEmpty() )
            throw new IllegalArgumentException("Please specify output name");
        if(experimentData.getTablePath().equals(path))
            throw new IllegalArgumentException("Output is the same as the input. Please specify different output name.");
    }

    @Override
    public String generateJavaScript(Object parametersObject)
    {
        try
        {
            CRClusterAnalysisParameters parameters = (CRClusterAnalysisParameters)parametersObject;

            StringBuffer getSourceScript = new StringBuffer();
            String[] params = {"null", "", "0.9", "20", "10", "false", "-Infinity", "Infinity", ""};

            if( parameters.getExperiment() != null )
            {
                getSourceScript.append("var experiment = data.get('"
                        + StringEscapeUtils.escapeEcmaScript(parameters.getExperiment().getCompletePath().toString()) + "');\n");
                params[0] = "experiment";
            }
            if( parameters.getExperimentData().getColumns() != null )
                params[1] = "'" + StringEscapeUtils.escapeEcmaScript(parameters.getExperimentData().getNamesDescription()) + "'";

            if( parameters.getCutoff() != null )
                params[2] = parameters.getCutoff().toString();
            if( parameters.getChainsCount() != null )
                params[3] = parameters.getChainsCount().toString();
            if( parameters.getCycleCount() != null )
                params[4] = parameters.getCycleCount().toString();
            if( parameters.isInvert() != null )
                params[5] = parameters.isInvert().toString();
            if( parameters.getThresholdDown() != null )
                params[6] = parameters.getThresholdDown().toString();
            if( parameters.getThresholdUp() != null )
                params[7] = parameters.getThresholdUp().toString();
            if( parameters.getOutputTablePath() != null )
                params[8] = "'" + StringEscapeUtils.escapeEcmaScript(parameters.getOutputTablePath().toString()) + "'";

            String putTableScript = "data.save(result,'" + parameters.getOutputCollection().getCompletePath().toString() + "/');";

            return getSourceScript.append("var result = microarray.crc(" + String.join(", ", params) + ");\n").append(putTableScript)
                    .toString();
        }
        catch( Exception ex )
        {
            return "Error during java script generating" + ex.getMessage();
        }
    }

    @Override
    public int getStepCount()
    {
        int lenght = ( parameters.getExperimentData() != null ) ? parameters.getExperiment().getSize() : 0;
        return parameters.getChainsCount() * parameters.getCycleCount() * lenght;
    }

    private static class Cluster
    {
        Deque<Integer> geneID = new ArrayDeque<>();
        Vector<Double> ysum = new Vector<>();
        Vector<Double> ysum2 = new Vector<>();
    }

    @Override
    public TableDataCollection getAnalyzedData() throws Exception
    {
        validateParameters();
        boolean invert = parameters.isInvert();
        int maxShift = 0;//parameters.getMaxShift();
        int chains = parameters.getChainsCount();
        double probcutoff = parameters.getCutoff();
        int cycleCount = parameters.getCycleCount();

        //  read in data
        log.info("Read in data");
        TableDataCollection table = parameters.getExperimentData().getTable();
        String[] columnNames = parameters.getExperimentData().getNames();
        int[] columnIndices = TableDataCollectionUtils.getColumnIndexes(table, columnNames);

        expre = TableDataCollectionUtils.getMatrix(table, columnIndices, parameters.getThresholdDown(), parameters.getThresholdUp());
        int genesNumber = expre.length;
        int exprPointsNumber = expre[0].length;

        int[] finalShift = new int[genesNumber];
        int[] midShift = new int[genesNumber];
        boolean[] finalSign = new boolean[genesNumber];
        boolean[] midSign = new boolean[genesNumber];
        clusters = new Vector<>();
        member = new Cluster[genesNumber];
        shift = new int[genesNumber];
        sign = new boolean[genesNumber];

        if( invert )
        {
            Arrays.fill( sign, true );
        }

        int count = 0;
        for(int j = 0; j < genesNumber; j++ )
        {
            boolean allmiss = DoubleStreamEx.of( expre[j] ).allMatch( Double::isNaN );
            if( allmiss )
            {
                log.info("the " + j + "th gene is completely missing");
                count++;
                continue;
            }
        }
        genesNumber -= count;
        int initialClustersNumber = (int)Math.sqrt(genesNumber);
        //  calculate average for each gene, prepare to try inverted expression profiles
        if( invert )
        {
            double avg = StreamEx.of( expre ).flatMapToDouble( Arrays::stream ).remove( Double::isNaN ).average().orElse( Double.NaN );
            average = DoubleStreamEx.constant( avg, genesNumber ).toArray();
        }
        int length = exprPointsNumber - maxShift;
        // calculate mean and variance, use them to choose parameters for priors
        double sum = 0;
        double sum2 = 0;
        count = 0;
        boolean[] missing = new boolean[genesNumber];
        for(int j = 0; j < genesNumber; j++ )
        {
            for(int k = 0; k < exprPointsNumber; k++ )
            {
                double val = expre[j][k];
 
                if( !Double.isNaN(val) )
                {
                    missing[j] = false;
                    sum = sum + val;
                    sum2 = sum2 + val * val;
                }
                else
                {
                    count++;
                    missing[j] = true;
                }
            }
        }
        int  misscount = count;
        double mean = sum / ( genesNumber * exprPointsNumber - count );
        double var = ( sum2 - sum * mean ) / ( (double)genesNumber * exprPointsNumber - count - 1 );
        double priorbeta0 = mean;
//        if( ( invert ) || ( maxShift > 0 ) )//added 01/02/06
//        {
            double priora = 0.5;
            double priorb = var;
//        }
        if( !invert  &&  maxShift < 0 )
        {
            priora *= 2;
            priorb *= 2;
        }

        log.info("Start clustering.");
        for(int j = 0; j < initialClustersNumber; j++ )
        {
            Cluster tempClust = new Cluster();
            tempClust.geneID.add(j);
            for(int k = 0; k < length; k++ )//column
            {
                double val = expre[j][k];
                if( !Double.isNaN(val) )
                {
                    tempClust.ysum.add(val);
                    tempClust.ysum2.add(val * val);
                }
                else
                {
                    tempClust.ysum.add(0.0);
                    tempClust.ysum2.add(0.0);
                }
            }
            clusters.add(tempClust);
        }

        Cluster cluster = clusters.firstElement();
        // sequentially add the remaining genes one by one to these clusters.
        for(int j = initialClustersNumber; j < genesNumber; j++ )
        {
            if( ( j % initialClustersNumber ) == 0 )// set allClusts pointer to the beginning
                cluster = clusters.firstElement();
            else
            {
                //add genes to existing clusters
                int index = clusters.indexOf(cluster);
                index++;
                cluster = clusters.elementAt(index);
            }
            cluster.geneID.add(j);
            for(int k = 0; k < length; k++ )
            {
                double val = expre[j][k];
                if( !Double.isNaN(val) )
                {
                    cluster.ysum.set(k, cluster.ysum.get(k) + val);
                     cluster.ysum2.set(k, cluster.ysum2.get(k) + val * val);
                }
            }
        }
        // assign members
        for( Cluster temp : clusters )
        {
            for( int geneID : temp.geneID )
                member[geneID] = temp;
        }

        List<Cluster> finalClust = new ArrayList<>();
        List<Cluster> midClust = new ArrayList<>();
        
        // initial runs to determine likely number of clusters.
        double maxlike = clust(genesNumber, length, maxShift, finalClust, finalShift, finalSign, priorbeta0, priora, priorb, cycleCount, invert);
        // fine search for mostly likely clustering using multiple chains
        for(int j = 0; j < chains && go; j++ )
        {
            randomInitialClust(genesNumber, length, maxShift, initialClustersNumber, invert);
            double loglike = clust(genesNumber, length, maxShift, midClust, midShift, midSign, priorbeta0, priora, priorb, cycleCount, invert);
            if( maxlike < loglike )
            {
                maxlike = loglike;
                finalClust.clear();
                finalClust.addAll(midClust);
                finalShift = new int[genesNumber];
                System.arraycopy(midShift, 0, finalShift, 0, genesNumber);
                if( invert )
                {
                    finalSign = new boolean[genesNumber];
                    System.arraycopy(midSign, 0, finalSign, 0, genesNumber);
                }
            }
            midClust.clear();
        }
        if( !go )
            return null;
        double[] postProba = postProb(genesNumber, length, maxShift, finalClust, finalShift, finalSign, priorbeta0, priora, priorb, invert);

        int[] clusterAssignments = output(genesNumber, exprPointsNumber, length, maxlike, finalClust, finalShift, finalSign, postProba, priorbeta0, priora,
                priorb, invert, probcutoff, chains, cycleCount, misscount);

        return assignClusters(clusterAssignments, table, parameters.getExperimentData().getNames(), finalShift, finalSign, postProba);
    }

    void randomInitialClust(int row, int length, int maxShift, int updatedClustNumber, boolean invert)
    {
        int initialClust;
        int order;

        //  initial shift position is in the middle
        Vector<Integer>[] geneList = new Vector[updatedClustNumber];
        for( int i = 0; i < updatedClustNumber; i++ )
            geneList[i] = new Vector<>();
        member = new Cluster[row];
        shift = new int[row];
        //  initial shift position is in the middle
        if( invert )
        {
            sign = new boolean[row];
            Arrays.fill( sign, true );
        }
        for( int j = 0; j < row; j++ )
        {
            initialClust = (int) (  Math.random() * updatedClustNumber );
            geneList[initialClust].add(j);
        }
        for( int j = 0; j < updatedClustNumber; j++ )
        {
            int size = geneList[j].size();
            Cluster tempClust = new Cluster();
            for( int m = 0; m < length; m++ )
            {
                tempClust.ysum.add(0.0);
                tempClust.ysum2.add(0.0);
            }
            for( int k = 0; k < size; k++ )
            {
                order = geneList[j].get(k);
                tempClust.geneID.add(order);
                for( int m = 0; m < length; m++ )
                {
                    double val = expre[order][m];
                    if( !Double.isNaN(val) )
                    {
                        tempClust.ysum.set(m, tempClust.ysum.get(m) + val);
                        tempClust.ysum2.set(m, tempClust.ysum2.get(m) + val * val);
                    }
                }
            }
            clusters.add(tempClust);
        }
        for( int j = 0; j < updatedClustNumber; j++ )
            geneList[j].clear();
        // assign members
        for( Cluster cluster : clusters )
        {
            for( int geneID : cluster.geneID )
                member[geneID] = cluster;
        }
    }

    int[] output(int row, int column, int length, double loglike, List<Cluster> finalClust, int[] finalShift, boolean[] finalSign,
            double[] postProba, double priorbeta0, double priora, double priorb, boolean invert, double probcutoff, int CHAINS, int ROUND,
            int misscount) throws Exception
    {

         log.info("There are total of of " + finalClust.size() + " clusters");
        int clusterNumber = 0;
        int passedGenes = 0;
        int[] clusterAssignment = new int[row];

        for( Cluster cluster : finalClust )
        {
            clusterNumber++;
          
            for( int geneID : cluster.geneID )
            {
                if( postProba[geneID] > probcutoff )
                {
                    clusterAssignment[geneID] = clusterNumber;
                    passedGenes++;
                }
            }
        }
        log.info("Number of genes above threshold = " + passedGenes);
        return clusterAssignment;
    }


    double[] postProb(int row, int length, int maxShift, List<Cluster> finalClust, int[] finalShift, boolean[] finalSign,
            double priorbeta0, double priora, double priorb, boolean invert)
    {
        double[] ysum = new double[length];
        double[] ysum2 = new double[length];
        double[] sum1 = new double[length];
        double[] sum2 = new double[length];
        double ratio;
        double sum = 0;
        double win = 0;
        double numerator = 0;

        double[] postProba = new double[row];
        for( Cluster finalCluster : finalClust )
        {
            for( int k = 0; k < length; k++ )
            {
                sum1[k] = finalCluster.ysum.get(k);
                sum2[k] = finalCluster.ysum2.get(k);
            }
            int nsize = finalCluster.geneID.size();

            for( int geneIDIter : finalCluster.geneID )
            {
               int start = finalShift[geneIDIter];
                if( ( invert ) && ( !finalSign[geneIDIter] ) )
                {
                    for( int k = 0; k < length; k++ )
                    {
                        double val = expre[geneIDIter][k + start];
                        if( !Double.isNaN(val) )
                        {
                            double newVal = 2 * average[geneIDIter] + val;
                            ysum[k] = sum1[k] - newVal;
                            ysum2[k] = sum2[k] - newVal * newVal;
                        }
                        else
                        {
                            ysum[k] = sum1[k];
                            ysum2[k] = sum2[k];
                        }

                        if( ysum2[k] < 0 )
                        {
                            System.out.print("");
                        }
                    }

                    numerator = Math.exp(bayesratioinvert(geneIDIter, start, length, ysum, ysum2, nsize, priorbeta0, priora, priorb));
                }
                else
                {
                    for( int k = 0; k < length; k++ )
                    {
                        double val = expre[geneIDIter][k + start];
                        if( !Double.isNaN(val) )
                        {
                            ysum[k] = sum1[k] - val;
                            ysum2[k] = sum2[k] - val * val;
                        }
                        else
                        {
                            ysum[k] = sum1[k];
                            ysum2[k] = sum2[k];
                        }

                        if( ysum2[k] < 0 )
                        {
                            System.out.print("");
                        }
                    }
                    numerator = Math.exp(bayesratio(geneIDIter, start, length, ysum, ysum2, nsize, priorbeta0, priora, priorb));
                }
                sum = 0;
                for( Cluster cluster : finalClust )
                {
                    if( cluster != finalCluster )
                    {
                        for( int k = 0; k < length; k++ )
                        {
                            ysum[k] = cluster.ysum.get(k);
                            ysum2[k] = cluster.ysum2.get(k);
                        }
                        int nsizenew = cluster.geneID.size() + 1;
                        for( int k = 0; k < maxShift + 1; k++ )//(2*maxShift + 1)
                        {
                            ratio = Math.exp(bayesratio(geneIDIter, k, length, ysum, ysum2, nsizenew, priorbeta0, priora, priorb));
                            if( ( k == 0 ) || ( win < ratio ) )
                                win = ratio;
                        }
                        if( invert )
                        {
                            for( int k = 0; k < maxShift + 1; k++ )//(2*maxShift + 1)
                            {
                                ratio = Math
                                        .exp(bayesratioinvert(geneIDIter, k, length, ysum, ysum2, nsizenew, priorbeta0, priora, priorb));
                                win = Math.min(ratio, win);
                            }
                        }
                        sum += win;
                    }
                }
                postProba[geneIDIter] = numerator / ( numerator + sum );
            }
        }
        return postProba;
    }
    double finalRatio(Cluster finalCluster, List<Cluster> finalClust, int[] finalShift, boolean[] finalSign, int length, double priorbeta0,
            double priora, double priorb, boolean invert)
    {
        int nsize = finalCluster.geneID.size();

        if( nsize == 1 )
        {
            return 0;
        }
        else
        {
            double togetherloglike = 0;
            double seperateloglike = 0;
            for( int j = 0; j < length; j++ )
            {

                double loglike = Stat.logGamma(0.5 * nsize + priora) - 0.5 * Math.log((double)nsize + 1)
                        - singleloglikelihood(priora, priorb, finalCluster.ysum2.get(j), finalCluster.ysum.get(j), priorbeta0, nsize);
                togetherloglike += loglike;
                for( int geneID : finalCluster.geneID )
                {
                    double expression = expre[geneID][j + finalShift[geneID]];
                    if( !Double.isNaN(expression) )// test if it is missing, added 12/11/05
                    {
                        if( ( invert ) && ( expre[geneID] == null ) )
                            expression = 2 * average[geneID] - expression;
                        loglike = - ( 0.5 + priora ) * Math.log(priorb + 0.5 * ( expression - priorbeta0 ) * ( expression - priorbeta0 ));
                    }
                    seperateloglike += loglike;
                }
            }
            double constant = (double)length * nsize * ( Stat.logGamma(0.5 + priora) - 0.5 * Math.log(2.0) ) + (double)length
                    * ( nsize - 1 ) * ( priora * Math.log(priorb) - Stat.logGamma(priora) );
            return ( ( togetherloglike - seperateloglike - constant ) / nsize );
        }
    }


    double loglikelihood(int length, double priorbeta0, double priora, double priorb)//column
    {
        double loglikesum = 0;
        for( Cluster cluster : clusters )
        {
            double dsize = cluster.geneID.size();
            for( int j = 0; j < length; j++ )
            {
                loglikesum -= singleloglikelihood(priora, priorb, cluster.ysum2.get(j), cluster.ysum.get(j), priorbeta0, dsize);
            }
            loglikesum += length
                    * ( -0.5 * dsize * Math.log(2 * Math.PI) + Stat.logGamma(0.5 * dsize + priora) - 0.5 * Math.log(dsize + 1) );
        }
        // the next term appears in every column of every cluster.
        return loglikesum + clusters.size() * length * ( priora * Math.log(priorb) - Stat.logGamma(priora) );
    }

    double clust(int genesNumber, int length, int maxShift, List<Cluster> finalClust, int[] finalShift, boolean[] finalSign, double priorbeta0,
            double priora, double priorb, int cycleCount, boolean invert) throws Exception
    {
        double maxLogLike = loglikelihood(length, priorbeta0, priora, priorb);
        //initialize finalClust by the current contents on allClusts
        finalClust.clear();
        finalClust.addAll(clusters);
        System.arraycopy(shift, 0, finalShift, 0, genesNumber);
        if( invert )
        {
            System.arraycopy(sign, 0, finalSign, 0, genesNumber);
        }
        for( int j = 0; j < cycleCount && go; j++ )
        {
            for( int geneID = 0; geneID < genesNumber; geneID++ )
            {
                incPreparedness(step++);
                if( invert )
                    operationinvert(geneID, length, maxShift, priorbeta0, priora, priorb, invert);
                else
                    operation(geneID, length, maxShift, priorbeta0, priora, priorb);
                double loglike = loglikelihood(length, priorbeta0, priora, priorb);
                if( loglike > maxLogLike )
                {
                    maxLogLike = loglike;
                    finalClust.clear();
                    finalClust.addAll(clusters);
                    System.arraycopy(shift, 0, finalShift, 0, genesNumber);
                    if( invert )
                    {
                        System.arraycopy(sign, 0, finalSign, 0, genesNumber);
                    }
                }
            }
        }
        clusters.clear();
        member = new Cluster[genesNumber];
        if( invert )
            sign = new boolean[genesNumber];
        shift = new int[genesNumber];
        return maxLogLike;
    }


    private static class OutcomeResult
    {
        private OutcomeResult(int outcome, boolean outsign, int newshift)
        {
            this.outcome = outcome;
            this.newshift = newshift;
            this.outsign = outsign;
        }
        int outcome;
        boolean outsign;
        int newshift;
    }

    void operationinvert(int geneID, int length, int maxShift, double priorbeta0, double priora, double priorb, boolean invert)
    {
        int outcome, newshift = 0;
        double[] ysum = new double[length];
        double[] ysum2 = new double[length];
        int nsize;
        Vector<Double> ratioVec = new Vector<>();
        boolean change = false;
        int start;
        boolean newsign;
        boolean outsign = false;

        int oldstart = shift[geneID];
        boolean oldsign = sign[geneID];
        Cluster genePlace = member[geneID];

        boolean single = ( genePlace.geneID.size() == 1 );

        if( single )
            clusters.remove(genePlace);
        for( Cluster cluster : clusters )
        {
            for( int j = 0; j < length; j++ )
            {
                ysum[j] = cluster.ysum.get(j);
                ysum2[j] = cluster.ysum2.get(j);
            }
            nsize = cluster.geneID.size();
            if( genePlace == cluster )//cluster contain this gene
            {
                start = shift[geneID];
                // ***** add 12/15/05 for testing on inversion
                if( invert && !sign[geneID] )
                {
                    for( int j = 0; j < length; j++ )
                    {
                        double val = 2 * average[geneID] - expre[geneID][j + start];
                        if( !Double.isNaN(val) )
                        {
                            ysum[j] -= val;
                            ysum2[j] -= val * val;
                        }
                        if( ysum2[j] < 0 )
                        {
                            System.out.print("");
                        }
                    }
                }
                else
                {
                    for( int j = 0; j < length; j++ )
                    {
                        double val = expre[geneID][j + start];
                        if( !Double.isNaN(val) )
                        {
                            ysum[j] -= val;
                            ysum2[j] -= val * val;

                            if( ysum2[j] < 0 )
                            {
                                System.out.print("");
                            }
                        }
                    }
                }
            }
            else
            {
                nsize++;
            }
            for( int j = 0; j < ( maxShift + 1 ); j++ )
            {
                ratioVec.add(Math.exp(bayesratio(geneID, j, length, ysum, ysum2, nsize, priorbeta0, priora, priorb)));//.push_back(ratio);
            }
            for( int j = 0; j < ( maxShift + 1 ); j++ )
            {
                ratioVec.add(Math.exp(bayesratioinvert(geneID, j, length, ysum, ysum2, nsize, priorbeta0, priora, priorb)));//.push_back(ratio);
            }
        }
        if( maxShift == 0 )
        {
            OutcomeResult result = singleCompareInvert(ratioVec, outsign, invert);
            outcome = result.outcome;
            newshift = 0;
            newsign = result.outsign;
        }
        else
        {
            OutcomeResult result = compareInvert(ratioVec, maxShift, newshift, outsign, invert);
            outcome = result.outcome;
            newsign = result.outsign;
            newshift = result.newshift;
        }
        ratioVec.clear();
        if( outcome == 0 )//new cluster is forming
        {
            //remove gene from previous cluster
            if( !single )
            {
                for( Cluster cluster : clusters )
                {
                    if( genePlace == cluster )//found cluster
                    {
                        if( cluster.geneID.size() > 0 )
                            cluster.geneID.pop();
                      
                        for( int j = 0; j < length; j++ )
                        {
                            double val = ( sign[geneID] ) ? expre[geneID][j + oldstart] : 2 * average[geneID] - expre[geneID][j + oldstart];
                            if( !Double.isNaN(val) )// test if it is missing, added 12/11/05
                            {
                                cluster.ysum.set(j, cluster.ysum.get(j) - val);
                                cluster.ysum2.set(j, cluster.ysum2.get(j) - val * val);
                            }
                        }
                        break;
                    }
                }
            }
            //create a new cluster that contain this gene only.
            Cluster tempClust2 = new Cluster();
            tempClust2.geneID.add(geneID);
            for( int j = 0; j < length; j++ )
            {
                double val = expre[geneID][j];
                if( !Double.isNaN(val) )
                {
                    tempClust2.ysum.add(val);
                    tempClust2.ysum2.add(val * val);
                }
                else
                {
                    tempClust2.ysum.add(0.0);
                    tempClust2.ysum2.add(0.0);
                }
            }
            clusters.add(tempClust2);
            member[geneID] = clusters.get(clusters.size() - 1);
            shift[geneID] = 0;
            sign[geneID] = true;
        }
        else
        //join one of exisiting clusters
        {
            if( !single )
            {
                change = false;
                int count = 0;
                for( Cluster cluster : clusters )
                {
                    if( genePlace == cluster )//found the cluster the gene was in before operation
                    {
                        if( cluster.geneID.size() > 0 )
                            cluster.geneID.pop();
                        if( ( outcome != ( count + 1 ) ) || ( oldstart != newshift ) || ( oldsign != newsign ) )//move from one cluster to another
                        {
                            change = true;
                           
                            for( int j = 0; j < length; j++ )
                            {
                                double val = ( sign[geneID] ) ? expre[geneID][j + oldstart] : 2 * average[geneID]
                                        - expre[geneID][j + oldstart];

                                if( !Double.isNaN(val) )
                                {
                                    cluster.ysum.set(j, cluster.ysum.get(j) - val);
                                    cluster.ysum2.set(j, cluster.ysum2.get(j) - val * val);
                                }
                            }
                        }
                        break;
                    }
                    else
                        count++;
                }
            }
            // add this gene to another cluster
            int count = 0;
            for( Cluster cluster : clusters )
            {
                if( outcome == ( count + 1 ) )//found cluster the gene will join
                {
                    cluster.geneID.add(geneID);
                    if( single || change )
                    {
                        for( int j = 0; j < length; j++ )
                        {
                            double val = newsign ? expre[geneID][j + newshift] : 2 * average[geneID] - expre[geneID][j + newshift];

                            if( !Double.isNaN(val) )
                            {
                                cluster.ysum.set(j, cluster.ysum.get(j) + val);
                                cluster.ysum2.set(j, cluster.ysum2.get(j) + val * val);
                            }
                        }
                        member[geneID] = cluster;
                        shift[geneID] = newshift;
                        sign[geneID] = newsign;
                    }
                    break;
                }
                else
                    count++;
            }
        }
    }

    void operation(int geneID, int length, int maxShift, double priorbeta0, double priora, double priorb)
    {
        int outcome, newshift = 0, oldstart;
        int count = 0;
        double[] ysum = new double[length];
        double[] ysum2 = new double[length];
        Vector<Double> ratioVec = new Vector<>();
        boolean change = false;
        Cluster geneCluster = member[geneID];

        boolean single = ( geneCluster.geneID.size() == 1 );
        if( single )
            clusters.remove(geneCluster);
        for( Cluster cluster : clusters )
        {
            for(int j = 0; j < length; j++ )
            {
                ysum[j] = cluster.ysum.get(j);
                ysum2[j] = cluster.ysum2.get(j);
            }
            int nsize = cluster.geneID.size();
            boolean self = ( geneCluster == cluster );
            if( self )// cluster contain this gene
            {
                int start = shift[geneID];
                for(int j = 0; j < length; j++ )
                {
                    double val = expre[geneID][j + start];
                    if( !Double.isNaN(val) )
                    {
                        ysum[j] -= val;
                        ysum2[j] -= val * val;
                    }
                }
            }
            else
                nsize++;
            for(int j = 0; j < ( maxShift + 1 ); j++ )
            {
                ratioVec.add(Math.exp(bayesratio(geneID, j, length, ysum, ysum2, nsize, priorbeta0, priora, priorb)));
            }
        }
        if( maxShift == 0 )
        {
            outcome = singleCompare(ratioVec);
            newshift = 0;
        }
        else
        {

            OutcomeResult result = compare(ratioVec, maxShift, newshift);
            outcome = result.outcome;
            newshift = result.newshift;
        }
        ratioVec.clear();
        if( outcome == 0 )//new cluster is forming
        {
            //remove gene from previous cluster
            if( !single )
            {
                count = 0;
                for( Cluster cluster : clusters )
                {
                    if( geneCluster == cluster )//found cluster
                    {
                        if( cluster.geneID.size() > 0 )
                            cluster.geneID.pop();
                        oldstart = shift[geneID];
                        for(int j = 0; j < length; j++ )
                        {
                            double val = expre[geneID][j + oldstart];
                            if( !Double.isNaN(val) )
                            {
                                cluster.ysum.set(j, cluster.ysum.get(j) - val);
                                cluster.ysum2.set(j, cluster.ysum2.get(j) - val * val);
                            }
                        }
                        break;
                    }
                    else
                        count++;
                }
            }
            //create a new cluster that contain this gene only.
            Cluster tempClust3 = new Cluster();
            tempClust3.geneID.add(geneID);
            for(int j = 0; j < length; j++ )
            {
                double val = expre[geneID][j];
                if( !Double.isNaN(val) )
                {
                    tempClust3.ysum.add(val);
                    tempClust3.ysum2.add(val * val);
                }
                else
                {
                    tempClust3.ysum.add(0.0);
                    tempClust3.ysum2.add(0.0);
                }
            }
            clusters.add(tempClust3);
            member[geneID] = clusters.get(clusters.size() - 1);
            shift[geneID] = 0;
        }
        else
        //join one of exisiting clusters
        {
            if( !single )
            {
                change = false;
                count = 0;
                for( Cluster cluster : clusters )
                {
                    if( geneCluster == cluster )//found the cluster the gene was in before operation
                    {
                        if( cluster.geneID.size() > 0 )
                            cluster.geneID.pop();
                        oldstart = shift[geneID];
                        if( ( outcome != ( count + 1 ) ) || ( oldstart != newshift ) )//move from one cluster to another
                        {
                            change = true;
                            for(int j = 0; j < length; j++ )
                            {
                                double val = expre[geneID][j + oldstart];
                                if( !Double.isNaN(val) )// test if it is missing, added 12/11/05
                                {
                                    cluster.ysum.set(j, cluster.ysum.get(j) - val);
                                    cluster.ysum2.set(j, cluster.ysum2.get(j) - val * val);
                                }

                            }
                        }
                        break;
                    }
                    else
                        count++;
                }
            }
            // add this gene to another cluster
            count = 0;
            for( Cluster cluster : clusters )
            {
                if( outcome == ( count + 1 ) )//found cluster the gene will join
                {
                    cluster.geneID.add(geneID);
                    if( ( single ) || ( change ) )
                    {
                        for(int j = 0; j < length; j++ )
                        {
                            double val = expre[geneID][j + newshift];
                            if( !Double.isNaN(val) )
                            {
                                cluster.ysum.set(j, cluster.ysum.get(j) + val);
                                cluster.ysum2.set(j, cluster.ysum2.get(j) + val * val);
                            }
                        }
                        member[geneID] = cluster;
                        shift[geneID] = newshift;
                    }
                    break;
                }
                else
                    count++;
            }
        }
    }

    int singleCompare(Vector<Double> ratioVec)
    {
        int n = ratioVec.size();
        double sum = 0;
        for( int j = 0; j < n; j++ )
            sum += ratioVec.get(j);
        double compare = Math.random() * sum;
        if( FACTOR_0 > compare )
            return 0;
        double partialsum = FACTOR_0;
        for( int j = 0; j < n; j++ )
        {
            partialsum += ratioVec.get(j);
            if( partialsum > compare )
                return j + 1;
        }
        log.info("Error in decide singelCompare");
        return 0;
    }

    OutcomeResult singleCompareInvert(Vector<Double> ratioVec, boolean outsign, boolean invert)
    {
        int number = ratioVec.size();
        double sum = DoubleStreamEx.of( ratioVec ).sum();
        double compare = Math.random() * sum;
        if( FACTOR_0 > compare )
            return new OutcomeResult(0, outsign, 0);
        double partialsum = FACTOR_0;
        for( int j = 0; j < number; j++ )
        {
            partialsum += ratioVec.get(j);
            if( partialsum > compare )
            {
                outsign = j % 2 == 0;
                return new OutcomeResult(j / 2 + 1, outsign, 0);
            }
        }
        log.info("Error in decide");
        return new OutcomeResult(0, outsign, 0);
    }

    OutcomeResult compare(Vector<Double> ratioVec, int maxShift, int shift)
    {
        int number = ratioVec.size();
        if( number % ( maxShift + 1 ) != 0 )
        {
            log.info("ratio vector size problem.");
        }
        double sum = FACTOR_0 + DoubleStreamEx.of( ratioVec ).sum();
        double compare = Math.random() * sum;
        if( FACTOR_0 > compare )
        {
            shift = 0;//maxShift;
            return new OutcomeResult(0, false, shift);
        }
        double partialsum = FACTOR_0;
        for( int j = 0; j < number; j++ )
        {
            partialsum = partialsum + ratioVec.get(j);
            if( partialsum > compare )
            {
                int decide = j / ( maxShift + 1 );
                shift = j % ( maxShift + 1 );
                return new OutcomeResult(decide + 1, false, shift);
            }
        }
        log.info("Error in decide");
        return new OutcomeResult(0, false, shift);
    }

    OutcomeResult compareInvert(Vector<Double> ratioVec, int maxShift, int shift, boolean outsign, boolean invert)
    {
        int number = ratioVec.size();
        if( number % ( ( maxShift + 1 ) * 2 ) != 0 )
        {
            log.info("ratio vector size problem.");
        }
        double sum = FACTOR_0;
        for( int j = 0; j < number; j++ )
            sum += ratioVec.get(j);
        double compare = Math.random() * sum;
        if( FACTOR_0 > compare )
        {
            return new OutcomeResult(0, outsign, 0);
        }
        double partialsum = FACTOR_0;
        for( int j = 0; j < number; j++ )
        {
            partialsum = partialsum + ratioVec.get(j);
            if( partialsum > compare )
            {
                int decide = j / ( ( maxShift + 1 ) * 2 );
                int leftover = j % ( ( maxShift + 1 ) * 2 );
                shift = leftover % ( maxShift + 1 );
                outsign = ( leftover < ( maxShift + 1 ) );
                return new OutcomeResult(decide + 1, outsign, shift);
            }
        }
        log.info("error in decide");
        return new OutcomeResult(0, outsign, shift);
    }

    double bayesratio(int geneID, int shift, int length, double[] ysum, double[] ysum2, int nsize, double priorbeta0, double priora,
            double priorb)
    {
        double[] ysumnew = new double[length];
        double[] ysum2new = new double[length];
        for( int j = 0; j < length; j++ )
        {
            double val = expre[geneID][j + shift];
            if( !Double.isNaN(val) )
            {
                ysumnew[j] = ysum[j] + val;
                ysum2new[j] = ysum2[j] + val * val;
            }
            else
            {
                ysumnew[j] = ysum[j];
                ysum2new[j] = ysum2[j];
            }
        }
        double logsum = 0;
        for( int j = 0; j < length; j++ )
        {
            double val = expre[geneID][j + shift];
            if( !Double.isNaN(val) )
            {
                double part1 = singleloglikelihood(priora, priorb, ysum2new[j], ysumnew[j], priorbeta0, nsize);
                double part2 = singleloglikelihood(priora, priorb, ysum2[j], ysum[j], priorbeta0, nsize - 1);
                double part3 = ( priora + 0.5 ) * Math.log(priorb + 0.5 * ( val - priorbeta0 ) * ( val - priorbeta0 ));
                double part4 = 0.5 * Math.log(2.0 * nsize / ( nsize + 1 ));
                double part5 = priora * Math.log(priorb) - Stat.logGamma(priora);
                double part6 = Stat.logGamma(priora + 0.5 * nsize) - Stat.logGamma(priora + 0.5)
                        - Stat.logGamma(priora + 0.5 * ( nsize - 1 ));
                logsum = logsum + part2 + part3 - part1 + part4 - part5 + part6;
            }
        }
        return logsum;
    }

    double bayesratioinvert(int geneID, int shift, int length, double[] ysum, double[] ysum2, int nsize, double priorbeta0, double priora,
            double priorb)
    {
        double[] ysumnew = new double[length];
        double[] ysum2new = new double[length];
        double[] invexpre = new double[length];
        double part1, part2, part3, part4, part5, part6;
        for( int j = 0; j < length; j++ )
        {
            double val = expre[geneID][j + shift];
            if( !Double.isNaN(val) )
            {
                invexpre[j] = 2 * average[geneID] - val;
                ysumnew[j] = ysum[j] + invexpre[j];
                ysum2new[j] = ysum2[j] + invexpre[j] * invexpre[j];
            }
            else
            {
                invexpre[j] = 0;
                ysumnew[j] = ysum[j];
                ysum2new[j] = ysum2[j];
            }
        }
        double logsum = 0;
        for( int j = 0; j < length; j++ )
        {
            if( !Double.isNaN(expre[geneID][j + shift]) )
            {
                part1 = singleloglikelihood(priora, priorb, ysum2new[j], ysumnew[j], priorbeta0, nsize);
                part2 = singleloglikelihood(priora, priorb, ysum2[j], ysum[j], priorbeta0, nsize - 1);
                part3 = ( priora + 0.5 ) * Math.log(priorb + 0.5 * ( invexpre[j] - priorbeta0 ) * ( invexpre[j] - priorbeta0 ));
                part4 = 0.5 * Math.log(2.0 * nsize / ( nsize + 1 ));
                part5 = priora * Math.log(priorb) - Stat.logGamma(priora);
                part6 = Stat.logGamma(priora + 0.5 * nsize) - Stat.logGamma(priora + 0.5) - Stat.logGamma(priora + 0.5 * ( nsize - 1 ));
                logsum = logsum + part2 + part3 - part1 + part4 - part5 + part6;
            }
        }
        return logsum;
    }

    private TableDataCollection assignClusters(int[] clusterAssignments, TableDataCollection table, String[] columnNames, int[] shift,
            boolean[] sign, double[] proba)
    {
        TableDataCollection result = parameters.getOutputTable();
        boolean shiftAllowed = false;
        boolean invertAllowed = parameters.isInvert();

        result.getColumnModel().addColumn("Cluster", Integer.class);
        result.getColumnModel().addColumn("Probability", Double.class);
        if( shiftAllowed )
            result.getColumnModel().addColumn("Shift", Integer.class);
        if( invertAllowed )
            result.getColumnModel().addColumn("Sign", String.class);

        for( String columnName : columnNames )
        {
            TableColumn column = table.getColumnModel().getColumn(columnName);
            result.getColumnModel().addColumn(column.getName(), column.getType());
        }

        for( int i = 0; i < table.getSize(); i++ )
        {
            //gene did not pass cutoff test
            if( clusterAssignments[i] == 0 )
                continue;
            String key = table.getName(i);
            Object[] inputValues = TableDataCollectionUtils.getRowValues(table, key, columnNames);
            String inverted = sign[i] ? "+" : "-";
            Vector<Object> row = new Vector<>();
            row.add(clusterAssignments[i]);
            row.add(proba[i]);
            if( shiftAllowed )
                row.add(shift[i]);
            if( invertAllowed )
                row.add(inverted);

            row.addAll(Arrays.asList(inputValues));
            TableDataCollectionUtils.addRow(result, key, row.toArray());
        }
        return result;
    }


    public static double singleloglikelihood(double a, double b, double x2sum, double xsum, double beta, double xsize)
    {
        double sum = xsum + beta;
        return ( xsize / 2 + a ) * Math.log(b + 0.5 * ( x2sum + beta * beta - sum * sum / ( xsize + 1 ) ));
    }
}
