package biouml.plugins.downloadext;

import java.io.File;
import java.net.URL;

import biouml.plugins.download.FileDownloader;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.DataElementImporterRegistry;
import ru.biosoft.access.DataElementImporterRegistry.ImporterInfo;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.TextUtil;

import ru.biosoft.jobcontrol.SubFunctionJobControl;

@ClassIcon ( "resources/import-analysis.gif" )
public class FTPUploadAnalysis extends AnalysisMethodSupport<FTPUploadAnalysisParameters>
{
    public FTPUploadAnalysis(DataCollection<?> parent, String name)
    {
        super(parent, name, new FTPUploadAnalysisParameters());
    }

    @Override
    public DataElement justAnalyzeAndPut() throws Exception
    {
        DataCollection parent = parameters.getResultPath().getParentCollection();
        String elementName = parameters.getResultPath().getName();
        DataElement de = null;
        ImporterInfo info = DataElementImporterRegistry.getImporterInfo(parameters.getImportFormat());

        StringBuilder convertMessages = new StringBuilder();
        URL url = FileDownloader.convertURL( new URL( TextUtil.decodeURL( parameters.getFileURL() ) ), convertMessages );
        if( convertMessages.length() > 0 )
            log.info(convertMessages.toString());
        File destinationFile = TempFiles.file(".tmp");
        try
        {
            String fileName = FileDownloader.downloadFile(url, destinationFile, new SubFunctionJobControl(jobControl, 2, 60));
            try
            {
                File newName = new File(destinationFile.getParentFile(), destinationFile.getName()+fileName);
                if(destinationFile.renameTo(newName)) destinationFile = newName;
            }
            catch( Exception ex )
            {
            }
            jobControl.setPreparedness(60);

            DataElementImporter importer = info.cloneImporter();
            if( importer.accept(parent, destinationFile) == DataElementImporter.ACCEPT_UNSUPPORTED )
                throw new IllegalArgumentException("Import doesn't want to accept this file");
            if( parameters.getImporterProperties() != null )
            {
                Object importerProperties = importer.getProperties(parent, destinationFile, elementName);
                if( importerProperties != null )
                    BeanUtil.copyBean(parameters.getImporterProperties(), importerProperties);
            }
            de = importer.doImport(parent, destinationFile, elementName, new SubFunctionJobControl(jobControl, 60, 100), log);
        }
        finally
        {
            destinationFile.delete();
        }
        return de;
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkPaths(new String[0], new String[] {"resultPath"});
        DataElementPath path = parameters.getResultPath();
        DataCollection parent = path.optParentCollection();
        ImporterInfo info = DataElementImporterRegistry.getImporterInfo(parameters.getImportFormat());
        if( info == null )
            throw new IllegalArgumentException("Specified import format for file is not found: " + parameters.getImportFormat());
        DataElementImporter importer = info.cloneImporter();
        if( importer.accept(parent, null) <= DataElementImporter.ACCEPT_UNSUPPORTED )
            throw new IllegalArgumentException("Specified output collection is unsupported by importer");
        try
        {
            FileDownloader.convertURL( new URL( TextUtil.decodeURL( parameters.getFileURL() ) ), null );
        }
        catch( Exception e1 )
        {
            throw new IllegalArgumentException("Cannot upload file: incorrect URL");
        }
    }
}
