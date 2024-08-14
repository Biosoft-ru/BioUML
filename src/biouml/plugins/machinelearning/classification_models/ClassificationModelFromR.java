/* $Id$ */

package biouml.plugins.machinelearning.classification_models;
 
import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.access.core.DataElementPath;
import biouml.plugins.machinelearning.rscript.RHelper;
import biouml.plugins.machinelearning.rscript.Rutils;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.DataMatrixString;
import biouml.plugins.machinelearning.utils.TableAndFileUtils;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;

/**
 * @author yura
 * Input:
 * Object[] additionalInputParameters :
 *          additionalInputParameters[0] = DataElementPath pathToFolderWithRmodel;
 *          additionalInputParameters[1] = String classificationRmodelFileName;
 *          additionalInputParameters[2] = String nameOfFileWithRscriptToCreateRmodel;
 *          additionalInputParameters[3] = String[] outputRObjectsNames;
 *          additionalInputParameters[4] = String nameOfFileWithRscriptToLoadRmodelAndPredict;
 *          additionalInputParameters[5] = String[] inputRobjectsNames;
 *          additionalInputParameters[6] = Object[] inputRobjects;
 */

public class ClassificationModelFromR extends ClassificationModel
{
    public static final String NAME_OF_TABLE_WITH_GENERAL_PARAMETERS_FOR_R = "General_parameters_for_R";

    private DataElementPath pathToFolderWithRmodel;
    private String classificationRmodelFileName, nameOfFileWithRscriptToCreateRmodel, nameOfFileWithRscriptToLoadRmodelAndPredict;
    // Important remark: outputObjectsNames[0] must be "predictedResponse" every time !!!
    protected String[] outputRObjectsNames = new String[]{"predictedResponse"};
    protected Object[] outputRObjects;
    
    public ClassificationModelFromR(String classificationType, String responseName, String[] response, DataMatrix dataMatrix, Object[] additionalInputParameters, boolean doCalculateAccompaniedInformationWhenFit)
    {
        super(classificationType, responseName, response, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformationWhenFit);
    }
    
    public ClassificationModelFromR(DataElementPath pathToInputFolder)
    {
        super(pathToInputFolder);
    }
    
    @Override
    public void fitModelParticular(double[][] matrix, Object[] additionalInputParameters)
    {
        pathToFolderWithRmodel = (DataElementPath)additionalInputParameters[0];
        classificationRmodelFileName = (String)additionalInputParameters[1];
        nameOfFileWithRscriptToCreateRmodel = (String)additionalInputParameters[2];
        outputRObjectsNames = (String[])additionalInputParameters[3];
        nameOfFileWithRscriptToLoadRmodelAndPredict = (String)additionalInputParameters[4];
        String rScriptToCreateAndWriteRmodel = RHelper.getScript("ClassificationAnalysis", nameOfFileWithRscriptToCreateRmodel);
        String outputNames[] = doCalculateAccompaniedInformationWhenFit ? outputRObjectsNames : new String[]{"predictedResponse"};
        String[] inputRobjectsNames = (String[])additionalInputParameters[5];
        Object[] inputRobjects = (Object[])additionalInputParameters[6];
        String[] names = new String[]{"matrixInput", "response"};
        Object[] objects = new Object[]{matrix, transform(responseIndices)};
        if( inputRobjectsNames != null )
        {
            names = (String[])ArrayUtils.addAll(names, inputRobjectsNames);
            objects = (String[])ArrayUtils.addAll(objects, inputRobjects);
        }
        objects = Rutils.executeRscript(rScriptToCreateAndWriteRmodel, names, objects, outputNames, pathToFolderWithRmodel, classificationRmodelFileName, null, null, log);
        String[] predictedResponse = (String[])objects[0];
        predictedIndices = UtilsForArray.getIndicesOfStrings(predictedResponse, distinctClassNames);
        if( outputRObjectsNames.length > 1 )
            outputRObjects = objects; 
    }
    
    @Override
    public void saveModelParticular(DataElementPath pathToOutputFolder)
    {
        TableAndFileUtils.writeColumnToStringTable(variableNames, "variable_names", variableNames, pathToOutputFolder, NAME_OF_TABLE_WITH_VARIABLE_NAMES);
        TableAndFileUtils.writeColumnToStringTable(new String[]{"pathToFolderWithRmodel", "classificationRmodelFileName", "nameOfFileWithRscriptToLoadRmodelAndPredict"}, "value", new String[]{pathToFolderWithRmodel.toString(), classificationRmodelFileName, nameOfFileWithRscriptToLoadRmodelAndPredict}, pathToOutputFolder, NAME_OF_TABLE_WITH_GENERAL_PARAMETERS_FOR_R);
    }

    @Override
    public int[] predict(double[][] matrix)
    {
        String rScriptToLoadRmodelAndPredict = RHelper.getScript("ClassificationAnalysis", nameOfFileWithRscriptToLoadRmodelAndPredict);
        String[] predictedResponse = (String[])Rutils.executeRscript(rScriptToLoadRmodelAndPredict, new String[]{"matrixInput"}, new Object[]{matrix}, new String[]{"predictedResponse"}, null, null, pathToFolderWithRmodel, classificationRmodelFileName, log)[0];
        return UtilsForArray.getIndicesOfStrings(predictedResponse, distinctClassNames);
    }
    
    @Override
    public void loadModelParticular(DataElementPath pathToInputFolder)
    {
        variableNames = TableAndFileUtils.getRowNamesInTable(pathToInputFolder.getChildPath(NAME_OF_TABLE_WITH_VARIABLE_NAMES));
        DataMatrixString dms = new DataMatrixString(pathToInputFolder.getChildPath(NAME_OF_TABLE_WITH_GENERAL_PARAMETERS_FOR_R), new String[]{"value"});
        String[] array = dms.getColumn(0);
        pathToFolderWithRmodel = array[0].equals(pathToInputFolder.toString()) ? pathToInputFolder : null;
        classificationRmodelFileName = array[1];
        nameOfFileWithRscriptToLoadRmodelAndPredict = array[2];
    }
}
