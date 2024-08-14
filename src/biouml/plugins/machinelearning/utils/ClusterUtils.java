/* $Id$ */

package biouml.plugins.machinelearning.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;

import biouml.plugins.machinelearning.utils.StatUtils.SimilaritiesAndDissimilarities;
import biouml.plugins.machinelearning.utils.UtilsGeneral.ListInteger;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

/**
 * @author yura
 *
 */
public class ClusterUtils
{
    /*********************** NodeForAhr : start *****************/
    // Node for Agglomerative Hierarchical Clustering
    public static class NodeForAhr
    {
        private int nodeIndex, oldNodeIndex1, oldNodeIndex2;
        private double dissimilarity;
        private DataMatrix dataMatrixForNode;
        private double[] scores;
        
        // dataMatrixForNode.length = scores.length
        public NodeForAhr(int nodeIndex, int oldNodeIndex1, int oldNodeIndex2, double dissimilarity, DataMatrix dataMatrixForNode, double[] scores)
        {
            this.nodeIndex = nodeIndex;
            this.oldNodeIndex1 = oldNodeIndex1;
            this.oldNodeIndex2 = oldNodeIndex2;
            this.dissimilarity = dissimilarity;
            this.dataMatrixForNode = dataMatrixForNode;
            this.scores = scores;
        }
        
        public int getNodeIndex()
        {
            return nodeIndex;
        }
        
        public int getOldNodeIndex1()
        {
            return oldNodeIndex1;
        }
        
        public int getOldNodeIndex2()
        {
            return oldNodeIndex2;
        }
        
        public double getDissimilarity()
        {
            return dissimilarity;
        }
        
        public DataMatrix getDataMatrix()
        {
            return dataMatrixForNode;
        }
        
        public void replaceDataMatrix(DataMatrix dataMatrixForNode)
        {
            this.dataMatrixForNode = dataMatrixForNode;
        }
        
        public double[] getScores()
        {
            return scores;
        }
    }
    /*********************** NodeForAhr : end *******************/
    
    /*********************** Dissimilarity : start **************/
    public static class Dissimilarity
    {
        private int nodeIndex1, nodeIndex2;
        private double dissimilarity;
        
        public Dissimilarity(int nodeIndex1,int nodeIndex2, double dissimilarity)
        {
            this.nodeIndex1 = nodeIndex1;
            this.nodeIndex2 = nodeIndex2;
            this.dissimilarity = dissimilarity;
        }
        
        // TODO: To remove after replacing
        public Dissimilarity(NodeForAhr node1, NodeForAhr node2)
        {
            this(node1.getNodeIndex(), node2.getNodeIndex(), calculateDissimilarity(node1, node2));
        }
        
        protected static double calculateDissimilarity(NodeForAhr node1, NodeForAhr node2)
        {
            DataMatrix dm1 = node1.getDataMatrix(), dm2 = node2.getDataMatrix(), dm = DataMatrix.concatinateDataMatricesRowWise(new DataMatrix[]{dm1, dm2});
            double[][] ranks = dm.getMatrix();
            double[] scores1 = node1.getScores(), scores2 = node2.getScores(), tieCorrections2 = ArrayUtils.addAll(scores1, scores2);
            return 1.0 - SimilaritiesAndDissimilarities.getKendallCoefficientOfConcordance(ranks, tieCorrections2, true);
        }
        
        public int getNodeIndex1()
        {
            return nodeIndex1;
        }
        
        public int getNodeIndex2()
        {
            return nodeIndex2;
        }
        
        public double getDissimilarity()
        {
            return dissimilarity;
        }
        
        /*************** static methods ********************/
        public static int getIndexForMin(List<Dissimilarity> dissimilarities)
        {
            int index = 0;
            double min = dissimilarities.get(0).getDissimilarity();
            for( int i = 1; i < dissimilarities.size(); i++ )
            {
                double x = dissimilarities.get(i).getDissimilarity();
                if( x < min )
                {
                    index = i;
                    min = x;
                }
            }
            return index;
        }
        
        public static List<Dissimilarity> removeDissimilaritiesWithGinenNode(List<Dissimilarity> dissimilarities, NodeForAhr node)
        {
            List<Dissimilarity> result = new ArrayList<>();
            int index1 = node.getOldNodeIndex1(), index2 = node.getOldNodeIndex2();
            for( Dissimilarity dissimilarity : dissimilarities )
            {
                int nodeIndex1 = dissimilarity.getNodeIndex1(), nodeIndex2 = dissimilarity.getNodeIndex2();
                if( nodeIndex1 == index1 || nodeIndex1 == index2 || nodeIndex2 == index1 || nodeIndex2 == index2) continue;
                result.add(dissimilarity);
            }
            return result;
        }
        
        public static List<Dissimilarity> addDissimilaritiesWithNewNode(List<Dissimilarity> dissimilarities, NodeForAhr nodeNew, List<NodeForAhr> nodes)
        {
            List<Dissimilarity> result = new ArrayList<>();
            for( NodeForAhr node : nodes )
                if( node.getDataMatrix() != null )
                    result.add(new Dissimilarity(node, nodeNew));
            result.addAll(dissimilarities);
            return result;
        }
    }
    /*********************** Dissimilarity : end *****************************************/

    /*********************** AgglomerativeHierarchicalClustering : start *****************/
    public static class AgglomerativeHierarchicalClustering
    {
        // The History is defined as list of subsequently merged nodes.
        private String[] objectNames;
        private NodeForAhr[] history;
        
        // dataMatrix.length = scores.length 
        public AgglomerativeHierarchicalClustering(DataMatrix dataMatrix, double[] scores)
        {
            determineHistory(dataMatrix, scores);
            objectNames = dataMatrix.getRowNames();
        }
        
        private void determineHistory(DataMatrix dataMatrix, double[] scores)
        {
            // 1. Determine initial nodes and dissimilarities between them.
            List<NodeForAhr> nodes = determineInitialNodes(dataMatrix, scores);
            List<Dissimilarity> dissimilarities = determineInitialDissimilarities(nodes);
            int n = dataMatrix.getSize(), dim = n - 1;
            
            // 2. Determine History.
            for( int i = 0; i < dim; i++ )
            {
                // 2.1. Create nodeNew. Remove data matrices in two old nodes.
                int indexWithMin = Dissimilarity.getIndexForMin(dissimilarities);
                Dissimilarity dissimilarity = dissimilarities.get(indexWithMin);
                int index1 = dissimilarity.getNodeIndex1(), index2 = dissimilarity.getNodeIndex2();
                NodeForAhr node1 = nodes.get(index1), node2 = nodes.get(index2); 
                DataMatrix dm1 = node1.getDataMatrix(), dm2 = node2.getDataMatrix();
                double[] scores1 = node1.getScores(), scores2 = node2.getScores(), scrs = scores1 == null || scores2 == null ? null : ArrayUtils.addAll(scores1, scores2);
                NodeForAhr nodeNew = new NodeForAhr(nodes.size(), index1, index2, dissimilarity.getDissimilarity(), DataMatrix.concatinateDataMatricesRowWise(new DataMatrix[]{dm1, dm2}), scrs);
                nodes.get(index1).replaceDataMatrix(null);
                nodes.get(index2).replaceDataMatrix(null);
                
                // TODO: temp
                log.info("i = " + i + " nodeNew = " + nodeNew.getNodeIndex() + " oldNodeIndex1 = " + nodeNew.getOldNodeIndex1() + " oldNodeIndex2 = " + nodeNew.getOldNodeIndex2() + " dissimilarity = " + nodeNew.getDissimilarity());
                String string = "";
                for( String s : nodeNew.getDataMatrix().getRowNames() )
                    string += s + " ";
                log.info("i = " + i + " " + string);
                
                // 2.2. Change dissimilarities and add nodeNew
                dissimilarities = Dissimilarity.removeDissimilaritiesWithGinenNode(dissimilarities, nodeNew);
                dissimilarities = Dissimilarity.addDissimilaritiesWithNewNode(dissimilarities, nodeNew, nodes);
                nodes.add(nodeNew);
            }
            
            // 3. Remove primitive nodes (n first nodes).
            this.history = nodes.subList(n, nodes.size()).toArray(new NodeForAhr[0]);
        }
        
        // TODO: To remove?
//        public Object[] getHistory()
//        {
//            return new Object[]{newNodeIndices, oldNodeIndices1, oldNodeIndices2, similarityMeasures};
//        }
        
        public TableDataCollection saveHistory(DataElementPath pathToOutputFolder, String tableName)
        {
            TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(pathToOutputFolder.getChildPath(tableName));
            
            // 1. Create column models. 
            String[] columnNames = new String[]{"Node_indices", "Old_node_indices1", "Old_node_indices2", "Dissimilarities"};
            for( int i = 0; i < 3; i++ )
                table.getColumnModel().addColumn(columnNames[i], Integer.class);
            table.getColumnModel().addColumn(columnNames[3], Double.class);

            // 2. Create rows.
            for( int i = 0; i < history.length; i++ )
            {
                Object[] row = new Object[]{history[i].getNodeIndex(), history[i].getOldNodeIndex1(), history[i].getOldNodeIndex2(), history[i].getDissimilarity()};
                TableDataCollectionUtils.addRow(table, "Node_" + Integer.toString(i), row, true);
            }
            
            // 3. Save table.
            table.finalizeAddition();
            CollectionFactoryUtils.save(table);
            return table;
        }
        
        private static List<Dissimilarity> determineInitialDissimilarities(List<NodeForAhr> nodes)
        {
            List<Dissimilarity> result = new ArrayList<>();
            for( int i = 0; i < nodes.size() - 1; i++ )
            {
                NodeForAhr node = nodes.get(i);
                for(int j = i + 1; j < nodes.size(); j++ )
                    result.add(new Dissimilarity(node, nodes.get(j)));
            }
            return result;
        }
        
        private static List<NodeForAhr> determineInitialNodes(DataMatrix dataMatrix, double[] scores)
        {
            List<NodeForAhr> result = new ArrayList<>();
            int n = dataMatrix.getSize();
            for( int i = 0; i < n; i++ )
            {
                double[] scrs = scores == null ? null : new double[]{scores[i]};
                result.add(new NodeForAhr(i, i, i, 0.0, dataMatrix.getRow(i), scrs));
            }
            return result;
        }
        
        // Output: int[][] result; result[i] contains vector of indices of objects that belong to i-th cluster, i=0,...,k-1.
        private int[][] getKclusters(int k)
        {
            if( k > objectNames.length ) return null;
            List<ListInteger> indicesOfObjects = new ArrayList<>();
            for( int i = 0; i < objectNames.length; i++ )
                indicesOfObjects.add(new ListInteger(i));
            for( int i = 0; i < objectNames.length - k; i++ )
            {
                int nodeIndex = history[i].getNodeIndex(), index1 = history[i].getOldNodeIndex1(), index2 = history[i].getOldNodeIndex2();
                if( i + objectNames.length != nodeIndex ) return null;
                indicesOfObjects.add(ListInteger.mergeLists(indicesOfObjects.get(index1), indicesOfObjects.get(index2)));
                indicesOfObjects.set(index1, null);
                indicesOfObjects.set(index2, null);
            }
            int index = 0;
            int[][] result = new int[k][];
            for( ListInteger list : indicesOfObjects )
                if( list != null )
                    result[index++] = UtilsGeneral.fromListIntegerToArray(list.getList());
            return result;
        }
        
        public void saveClusters(int k, boolean doWriteToFile, DataElementPath pathToOutputFolder, String fileOrTableName)
        {
            // 1. Create String matrix with cluster contents.
            int[][] clusters = getKclusters(k);
            int[] lengths = new int[clusters.length];
            for( int i = 0; i < clusters.length; i++ )
                lengths[i] = clusters[i].length;
            int max = (int)PrimitiveOperations.getMax(lengths)[1];
            String[][] clustersContent = new String[clusters.length][max];
            for( int i = 0; i < clusters.length; i++ )
                for( int j = 0; j < clusters[i].length; j++ )
                    clustersContent[i][j] = objectNames[clusters[i][j]];
            
            // 2. Create and save DataMatrixString.
            String[] rowNames = new String[clusters.length], columnNames = new String[max];
            for( int i = 0; i < rowNames.length; i++ )
                rowNames[i] = "Cluster_" + Integer.toString(i);
            for( int i = 0; i < columnNames.length; i++ )
                columnNames[i] = "Element_" + Integer.toString(i);
            DataMatrixString dms = new DataMatrixString(rowNames, columnNames, clustersContent);
            dms.writeDataMatrixString(doWriteToFile, pathToOutputFolder, fileOrTableName, log);
        }
    }
    /*********************** AgglomerativeHierarchicalClustering : end *****************/
    
    private static Logger log = Logger.getLogger(ClusterUtils.class.getName());
}
