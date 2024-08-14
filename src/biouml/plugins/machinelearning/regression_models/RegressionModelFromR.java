/* $Id$ */

package biouml.plugins.machinelearning.regression_models;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;
import biouml.plugins.machinelearning.rscript.RHelper;
import biouml.plugins.machinelearning.rscript.Rutils;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.DataMatrixString;
import biouml.plugins.machinelearning.utils.TableAndFileUtils;

/**
 * @author yura
 *
 * Input:
 * Object[] additionalInputParameters :
 *          additionalInputParameters[0] = DataElementPath pathToFolderWithRmodel;
 *          additionalInputParameters[1] = String regressionRmodelFileName;
 *          additionalInputParameters[2] = String nameOfFileWithRscriptToCreateRmodel;
 *          additionalInputParameters[3] = String[] outputRObjectsNames;
 *          additionalInputParameters[4] = String nameOfFileWithRscriptToLoadRmodelAndPredict;
 *          additionalInputParameters[5] = String[] inputRobjectsNames;
 *          additionalInputParameters[6] = Object[] inputRobjects;
 */
public class RegressionModelFromR extends RegressionModel
{
    public static final String NAME_OF_TABLE_WITH_GENERAL_PARAMETERS_FOR_R = "general_parameters_for_R";

    private DataElementPath pathToFolderWithRmodel;
    private String regressionRmodelFileName, nameOfFileWithRscriptToCreateRmodel, nameOfFileWithRscriptToLoadRmodelAndPredict;
    // Important remark: outputObjectsNames[0] must be "predictedResponse" every time !!!
    protected String[] outputRObjectsNames = new String[]{"predictedResponse"};
    protected Object[] outputRObjects;
    
    public RegressionModelFromR(String regressionType, String responseName, double[] response, DataMatrix dataMatrix, Object[] additionalInputParameters, boolean doCalculateAccompaniedInformationWhenFit)
    {
        super(regressionType,  responseName, response, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformationWhenFit);
    }
    
    public RegressionModelFromR(DataElementPath pathToInputFolderWithRModel)
    {
        super(pathToInputFolderWithRModel);
    }
    
    @Override
    public void fitModelParticular(double[][] matrix, Object[] additionalInputParameters)
    {
        pathToFolderWithRmodel = (DataElementPath)additionalInputParameters[0];
        regressionRmodelFileName = (String)additionalInputParameters[1];
        nameOfFileWithRscriptToCreateRmodel = (String)additionalInputParameters[2];
        outputRObjectsNames = (String[])additionalInputParameters[3];
        nameOfFileWithRscriptToLoadRmodelAndPredict = (String)additionalInputParameters[4];
        String rScriptToCreateAndWriteRmodel = RHelper.getScript("RegressionAnalysis", nameOfFileWithRscriptToCreateRmodel);
        String outputNames[] = doCalculateAccompaniedInformationWhenFit ? outputRObjectsNames : new String[]{"predictedResponse"};
        String[] inputRobjectsNames = (String[])additionalInputParameters[5];
        Object[] inputRobjects = (Object[])additionalInputParameters[6];
        String[] names = new String[]{"matrixFilePath", "response"};
        Object[] objects;
        
        TempFile dataMatrixTmpFile = null;
        try {
        	dataMatrixTmpFile = writeDataMatrix( matrix );
        } catch (IOException e) {
        	e.printStackTrace();
        }
        objects = new Object[]{dataMatrixTmpFile.getAbsolutePath(), response};

        if( inputRobjectsNames != null )
        {
            names = (String[])ArrayUtils.addAll(names, inputRobjectsNames);
            objects = (String[])ArrayUtils.addAll(objects, inputRobjects);
        }
        objects = Rutils.executeRscript(rScriptToCreateAndWriteRmodel, names, objects, outputNames, pathToFolderWithRmodel, regressionRmodelFileName, null, null, log);
        dataMatrixTmpFile.delete();
        predictedResponse = (double[])objects[0];
        if( outputRObjectsNames.length > 1 )
            outputRObjects = objects; 
    }
    
    @Override
    public void saveModelParticular(DataElementPath pathToOutputFolder)
    {
        TableAndFileUtils.writeColumnToStringTable(variableNames, "variable_names", variableNames, pathToOutputFolder, NAME_OF_TABLE_WITH_VARIABLE_NAMES);
        TableAndFileUtils.writeColumnToStringTable(new String[]{"pathToFolderWithRmodel", "regressionRmodelFileName", "nameOfFileWithRscriptToLoadRmodelAndPredict"}, "value", new String[]{pathToFolderWithRmodel.toString(), regressionRmodelFileName, nameOfFileWithRscriptToLoadRmodelAndPredict}, pathToOutputFolder, NAME_OF_TABLE_WITH_GENERAL_PARAMETERS_FOR_R);
    }
    
    @Override
    public void loadModelParticular(DataElementPath pathToInputFolder)
    {
        variableNames = TableAndFileUtils.getRowNamesInTable(pathToInputFolder.getChildPath(NAME_OF_TABLE_WITH_VARIABLE_NAMES));
        DataMatrixString dms = new DataMatrixString(pathToInputFolder.getChildPath(NAME_OF_TABLE_WITH_GENERAL_PARAMETERS_FOR_R), new String[]{"value"});
        String[] array = dms.getColumn(0);
        pathToFolderWithRmodel = array[0].equals(pathToInputFolder.toString()) ? pathToInputFolder : null;
        regressionRmodelFileName = array[1];
        nameOfFileWithRscriptToLoadRmodelAndPredict = array[2];
    }

    @Override
    public double[] predict(double[][] matrix)
    {
        String rScriptToLoadRmodelAndPredict = RHelper.getScript("RegressionAnalysis", nameOfFileWithRscriptToLoadRmodelAndPredict);
        try {
			TempFile dataMatrixFile = writeDataMatrix( matrix );
			double[] result = (double[])Rutils.executeRscript(rScriptToLoadRmodelAndPredict, new String[]{"matrixFilePath"}, new Object[]{dataMatrixFile.getAbsolutePath()}, new String[]{"predictedResponse"}, null, null, pathToFolderWithRmodel, regressionRmodelFileName, log)[0];
			dataMatrixFile.delete();
			return result;
		} catch (IOException e) {
			e.printStackTrace();
		}
        return null;
    }
    
    private static TempFile writeDataMatrix(double[][] dataMatrix) throws IOException
    {
    	TempFile dmTmpFile = TempFiles.file(".txt");
		BufferedWriter writer = new BufferedWriter(new FileWriter( dmTmpFile ) );
		for( int i = 0; i < dataMatrix.length; i++ )
		{
			StringBuilder rowBuilder = new StringBuilder();
			for( int j = 0; j < dataMatrix[i].length; j++ )
			{
				rowBuilder.append( dataMatrix[i][j] );
				if( j != dataMatrix[i].length - 1 )
					rowBuilder.append("\t");
			}
			writer.append( rowBuilder );
			if( i != dataMatrix.length - 1 )
				writer.append("\n");
		}
		writer.close();
		return dmTmpFile;
    }
}
