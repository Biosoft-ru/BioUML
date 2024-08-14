package biouml.plugins.downloadext;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.ImporterFormat;
import ru.biosoft.plugins.javascript.JSAnalysis;
import ru.biosoft.plugins.javascript.JSProperty;
import ru.biosoft.plugins.javascript.JavaScriptHostObjectBase;

public class JavaScriptFTP extends JavaScriptHostObjectBase
{

    @JSAnalysis(FTPUploadAnalysis.class)
    public void upload(
            @JSProperty("fileURL") String fileURL,
            @JSProperty("resultPath") String resultPath,
            @JSProperty("importFormat") String importFormat,
            @JSProperty("importerProperties") Object importerProperties) throws Exception
    {
        FTPUploadAnalysis analysis = new FTPUploadAnalysis(null, "");
        FTPUploadAnalysisParameters parameters = analysis.getParameters();
        parameters.setFileURL(fileURL);
        parameters.setResultPath(DataElementPath.create(resultPath));
        parameters.setImporterFormat( new ImporterFormat( importFormat ) );
        parameters.setImporterProperties(importerProperties);
        analysis.getJobControl().run();
    }
}
