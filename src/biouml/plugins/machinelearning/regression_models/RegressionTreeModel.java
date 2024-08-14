/* $Id$ */

package biouml.plugins.machinelearning.regression_models;

import ru.biosoft.access.core.DataElementPath;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.TableAndFileUtils;
import biouml.plugins.machinelearning.utils.TreeUtils.RegressionNode;
import biouml.plugins.machinelearning.utils.TreeUtils.RegressionTree;

/**
 * @author yura
 * Input:
 * Object[] additionalInputParameters :
 *          additionalInputParameters[0] = int minimalNodeSize;
 *          additionalInputParameters[1] = double minimalVariance; 
 */

public class RegressionTreeModel extends RegressionModel
{
    public static final String NAME_OF_TABLE_WITH_LEAVES = "leaves";

    private RegressionNode[] leaves;

    public RegressionTreeModel(String responseName, double[] response, DataMatrix dataMatrix, Object[] additionalInputParameters, boolean doCalculateAccompaniedInformationWhenFit)
    {
        super(RegressionModel.REGRESSION_4_RT, responseName, response, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformationWhenFit);
    }
    
    public RegressionTreeModel(DataElementPath pathToInputFolder)
    {
        super(pathToInputFolder);
    }
    
    @Override
    public void fitModel(DataMatrix dataMatrix, Object[] additionalInputParameters)
    {
        int minimalNodeSize = (int)additionalInputParameters[0];
        double minimalVariance = (double)additionalInputParameters[1];
        leaves = RegressionTree.growTree(dataMatrix, response, minimalNodeSize, minimalVariance);
        double[][] matrix = dataMatrix.getMatrix();
        if( doCalculateAccompaniedInformationWhenFit )
            predictedResponse = predict(matrix);
        fitModel(matrix, additionalInputParameters);
    }

    @Override
    public double[] predict(double[][] matrix)
    {
        return RegressionTree.predict(matrix, leaves);
    }
    
    @Override
    public void saveModelParticular(DataElementPath pathToOutputFolder)
    {
        TableAndFileUtils.writeColumnToStringTable(variableNames, "variable_names", variableNames, pathToOutputFolder, NAME_OF_TABLE_WITH_VARIABLE_NAMES);
        String s = RegressionTree.writeToString(leaves);
        TableAndFileUtils.writeStringToFile(s, pathToOutputFolder, NAME_OF_TABLE_WITH_LEAVES, log);
    }
    
    @Override
    public void loadModelParticular(DataElementPath pathToInputFolder)
    {
        String[] lines = TableAndFileUtils.readLinesInFile(pathToInputFolder.getChildPath(NAME_OF_TABLE_WITH_LEAVES));
        leaves = RegressionTree.readNodesInLines(lines);
        variableNames = TableAndFileUtils.getRowNamesInTable(pathToInputFolder.getChildPath(NAME_OF_TABLE_WITH_VARIABLE_NAMES));
    }
}
