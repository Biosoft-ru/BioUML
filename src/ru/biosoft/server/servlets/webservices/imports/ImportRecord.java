package ru.biosoft.server.servlets.webservices.imports;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.DataElementImporterRegistry;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.server.JSONUtils;
import ru.biosoft.util.LazyValue;

public class ImportRecord
{
    private static final Logger log = Logger.getLogger( ImportRecord.class.getName() );
    
    ImportRecord() {}
    public ImportRecord(DataElementPath targetFolder, String fileName, long fileSize)
    {
        user = SecurityManager.getSessionUser();
        status = ImportStatus.UPLOAD_CREATED;
        startTime = System.currentTimeMillis();
        this.targetFolder = targetFolder;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }
    
    private int id;
    public int getId() {
        return id;
    }
    void setId(int id)
    {
        this.id = id;
    }
    
    private String user;
    public String getUser()
    {
        return user;
    }
    public void setUser(String user)
    {
        this.user = user;
    }
    
    private ImportStatus status;
    public ImportStatus getStatus()
    {
        return status;
    }
    public void setStatus(ImportStatus status)
    {
        this.status = status;
    }
    
    private long startTime;
    public long getStartTime()
    {
        return startTime;
    }
    public void setStartTime(long startTime)
    {
        this.startTime = startTime;
    }

    private DataElementPath targetFolder;
    public DataElementPath getTargetFolder()
    {
        return targetFolder;
    }
    public void setTargetFolder(DataElementPath targetFolder)
    {
        this.targetFolder = targetFolder;
    }
 
    private String fileName;
    public String getFileName()
    {
        return fileName;
    }
    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }
    public String getTargetElementName() {
        return fileName.replaceFirst("\\.\\w+$", "");
    }
    
    private String deName;
    public String getDEName()
    {
        return deName;
    }
    public void setDEName(String deName)
    {
        this.deName = deName;
    }

    private URL sourceURL;
    public URL getSourceURL()
    {
        return sourceURL;
    }
    public void setSourceURL(URL url)
    {
        this.sourceURL = url;
    }
    
    private File file;
    public File getFile()
    {
        return file;
    }
    public void setFile(File file)
    {
        this.file = file;
    }

    private long fileSize;
    public long getFileSize()
    {
        return fileSize;
    }
    public void setFileSize(long fileSize)
    {
        this.fileSize = fileSize;
    }
    
    private int uploadProgress;
    public int getUploadProgress()
    {
        return uploadProgress;
    }
    public void setUploadProgress(int uploadProgress)
    {
        this.uploadProgress = uploadProgress;
    }
    
    private List<String> formatList = new ArrayList<>();
    public List<String> getFormatList()
    {
        return formatList;
    }
    public void setFormatList(List<String> formatList)
    {
        this.formatList = formatList;
    }
    
    private String format;
    public String getFormat()
    {
        return format;
    }
    public void setFormat(String format)
    {
        this.format = format;
        setFormatOptions( null );
        setFormatOptionsStr( null );
    }
    
    private Object formatOptions;
    private String formatOptionsStr;

    private final LazyValue<Object> formatOptionsLazy = new LazyValue<>( "Format import options", () -> {

        if( formatOptions == null )
        {
            formatOptions = getDefaultImportOptions();
            if( formatOptionsStr != null && formatOptions != null )
            {
                try
                {
                    JSONArray formatOptionsJSON = new JSONArray( formatOptionsStr );
                    JSONUtils.correctBeanOptions( formatOptions, formatOptionsJSON, false );
                }
                catch( Exception e )
                {
                    log.log( Level.WARNING, "Can not read import options, for import record " + id + ", using default options", e );
                }
            }
        }
        return formatOptions;
    } );

    public Object getFormatOptions()
    {
        if( format == null )
            return null;
        return formatOptionsLazy.get();
    }
    public void setFormatOptions(Object formatOptions)
    {
        this.formatOptions = formatOptions;
    }


    public Object getFormatOptionsStr()
    {
        return formatOptionsStr;
    }
    public void setFormatOptionsStr(String formatOptionsStr)
    {
        this.formatOptionsStr = formatOptionsStr;
    }
    
    private Object getDefaultImportOptions()
    {
        if(format == null)
            return null;
        DataCollection<?> targetDC = targetFolder.getDataCollection();
        File file = getFile();
        if(file != null && !file.exists())
            file = null;
        DataElementImporter importer = DataElementImporterRegistry.getImporter( file, format, targetDC );
        if(importer == null) {
            log.warning( "Can not find importer for " + format );
            return null;
        }
        Object result = null;
        try {
            result = importer.getProperties( targetDC, file, getTargetElementName() );
        } catch(Exception e)
        {
            log.log(Level.WARNING, "Can not get initial properties for importer " + format, e );
            return null;
        }
        return result;
    }
    
    private OmicsType omicsType;
    public OmicsType getOmicsType()
    {
        return omicsType;
    }
    public void setOmicsType(OmicsType omicsType)
    {
        this.omicsType = omicsType;
    }

    private int importProgress;
    public int getImportProgress()
    {
        return importProgress;
    }
    public void setImportProgress(int importProgress)
    {
        this.importProgress = importProgress;
    }
    

    private String uploadId;
    public String getUploadId()
    {
        return uploadId;
    }
    public void setUploadId(String uploadId)
    {
        this.uploadId = uploadId;
    }
    
    private String importId;
    public String getImportId()
    {
        return importId;
    }
    public void setImportId(String importId)
    {
        this.importId = importId;
    }
}
