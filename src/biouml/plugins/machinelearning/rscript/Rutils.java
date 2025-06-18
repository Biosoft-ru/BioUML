/* $Id$ */

package biouml.plugins.machinelearning.rscript;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.FileImporter;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.access.script.LogScriptEnvironment;
import ru.biosoft.access.script.ScriptDataElement;
import ru.biosoft.access.script.ScriptTypeRegistry;
import ru.biosoft.util.TempFiles;

/**
 * @author yura
 *
 */
public class Rutils
{
    public static Object[] executeRscript(String rScriptForExecution, String[] inputObjectsNames, Object[] inputObjects, String[] outputObjectsNames, DataElementPath pathToOutputFolder, String fileNameForWritingRobect, DataElementPath pathToFolderWithSavedModel, String fileNameForReadingRobect, Logger log)
    {
        // 1. Initialization of inputData.
        Map<String, Object> inputData = new HashMap<>();
        if( inputObjectsNames != null )
            for( int i = 0; i < inputObjectsNames.length; i++ )
                inputData.put(inputObjectsNames[i], inputObjects[i]);
        File tempFileForRobject = null;
        if( pathToOutputFolder != null && fileNameForWritingRobect != null )
        {
            try
            {
                tempFileForRobject = TempFiles.file("");
            }
            catch( IOException e )
            {
                e.printStackTrace();
            }
            inputData.put("tempFileForRobject", tempFileForRobject);
        }
        if( pathToFolderWithSavedModel != null && fileNameForReadingRobect != null )
        {
            FileDataElement fileDataElement = pathToFolderWithSavedModel.getChildPath(fileNameForReadingRobect).getDataElement(FileDataElement.class);
            inputData.put("pathToRobject", fileDataElement.getFile().getAbsolutePath());
        }

        // 2. Initialization of outputData.
        Map<String, Object> outputData = new HashMap<>();
        if( outputObjectsNames != null )
            for( int i = 0; i < outputObjectsNames.length; i++ )
                outputData.put(outputObjectsNames[i], null);
        
        // 3. Execute R-script
        ScriptDataElement sde = ScriptTypeRegistry.createScript("R", null, "");
        final LogScriptEnvironment env = new LogScriptEnvironment(log);
        sde.execute(rScriptForExecution, env, inputData, outputData, false);
        
        // 4. Import file with saved R-object to BioUML
        if( pathToOutputFolder != null && fileNameForWritingRobect != null )
        {
            FileImporter importer = new FileImporter();
            DataElementPath pathToModel = pathToOutputFolder.getChildPath(fileNameForWritingRobect);
            try
            {
                importer.doImport(pathToModel.getParentCollection(), tempFileForRobject, pathToModel.getName(), null, log);
            }
            catch( RepositoryException e )
            {
                e.printStackTrace();
            }
            catch( Exception e )
            {
                e.printStackTrace();
            }
        }

        // 5. Output the results
        if( outputObjectsNames == null ) return null;
        Object[] result = new Object[outputObjectsNames.length];
        for( int i = 0; i < outputObjectsNames.length; i++ )
            result[i] = outputData.get(outputObjectsNames[i]);
        return result;
    }
}
