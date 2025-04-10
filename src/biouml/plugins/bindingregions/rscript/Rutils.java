
package biouml.plugins.bindingregions.rscript;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import java.util.logging.Logger;

import biouml.plugins.bindingregions.utils.Classification;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.FileImporter;
import ru.biosoft.access.script.LogScriptEnvironment;
import ru.biosoft.access.script.ScriptDataElement;
import ru.biosoft.access.script.ScriptTypeRegistry;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.util.TempFiles;

/**
 * @author yura
 *
 */
public class Rutils
{
    public static Object[] executeRscript(String rScriptForExecution, String[] inputObjectsNames, Object[] inputObjects, String[] outputObjectsNames, DataElementPath pathToOutputs, String fileNameForWritingRobect, DataElementPath pathToInputs, String fileNameForReadingRobect, Logger log, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        // 1. Initialization of inputData
        Map<String, Object> inputData = new HashMap<>();
        if( inputObjectsNames != null )
            for( int i = 0; i < inputObjectsNames.length; i++ )
                inputData.put(inputObjectsNames[i], inputObjects[i]);
        File tempFileForRobject = null;
        if( pathToOutputs != null && fileNameForWritingRobect != null )
        {
            tempFileForRobject = TempFiles.file("");
            inputData.put("tempFileForRobject", tempFileForRobject);
        }
        if( pathToInputs != null && fileNameForReadingRobect != null )
        {
            FileDataElement fileDataElement = pathToInputs.getChildPath(fileNameForReadingRobect).getDataElement(FileDataElement.class);
            inputData.put("pathToRobject", fileDataElement.getFile().getAbsolutePath());
        }

        // 2. Initialization of inputData
        Map<String, Object> outputData = new HashMap<>();
        if( outputObjectsNames != null )
            for( int i = 0; i < outputObjectsNames.length; i++ )
                outputData.put(outputObjectsNames[i], null);
        if( jobControl != null ) jobControl.setPreparedness(from + 2 * (to - from) / 5);
        
        // Old version : Read R-script
        /***
        File file = new File(pathToRscriptsForPrediction);
        String script;
        try
        {
            script = ApplicationUtils.readAsString(file);
        }
        catch(Exception e)
        {
            log.info("Appropriate R-script file is not available");
            throw e;
        }
        if( jobControl != null ) jobControl.setPreparedness(from + (to - from) / 2);
        ***/
        
        // 3. Execute R-script
        ScriptDataElement sde = ScriptTypeRegistry.createScript("R", null, "");
        final LogScriptEnvironment env = new LogScriptEnvironment(log);
        sde.execute(rScriptForExecution, env, inputData, outputData, false);
        if( jobControl != null ) jobControl.setPreparedness(from + 9 * (to - from) / 10);
        
        // 4. Import file with saved R-object to BioUML
        if( pathToOutputs != null && fileNameForWritingRobect != null )
        {
            FileImporter importer = new FileImporter();
            DataElementPath pathToModel = pathToOutputs.getChildPath(fileNameForWritingRobect);
            importer.doImport(pathToModel.getParentCollection(), tempFileForRobject, pathToModel.getName(), null, log);
        }
        if( jobControl != null ) jobControl.setPreparedness(to);

        // 5. Output the results
        if( outputObjectsNames == null ) return null;
        Object[] result = new Object[outputObjectsNames.length];
        for( int i = 0; i < outputObjectsNames.length; i++ )
            result[i] = outputData.get(outputObjectsNames[i]);
        return result;
    }
    
    public static int[] readClassificationModelAndPredictIndicesOfClassesUsingR(double[][] dataMatrix, String[] namesOfClasses, String scriptToReadClassificationModelAndPredict , DataElementPath pathToFolder, String classificationModelFileName, Logger log, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        Object[] objects = executeRscript(scriptToReadClassificationModelAndPredict, new String[]{"dataMatrix"}, new Object[]{dataMatrix}, new String[]{"predictedNamesOfClassesForEachObject"}, null, null, pathToFolder, classificationModelFileName, log, jobControl, from, to);
        return Classification.getIndicesOfClasses((String[])objects[0], namesOfClasses);
    }

    public static double[] readRegressionModelAndPredictResponseUsingR(double[][] dataMatrix, String scriptToReadRegressionModelAndPredict, DataElementPath pathToInputFolder, String regressionModelFileName, Logger log, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        return (double[])executeRscript(scriptToReadRegressionModelAndPredict, new String[]{"dataMatrix"}, new Object[]{dataMatrix}, new String[]{"predictedResponse"}, null, null, pathToInputFolder, regressionModelFileName, log, jobControl, from, to)[0];
    }
}
