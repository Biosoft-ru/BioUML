package ru.biosoft.server.servlets.webservices.imports;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import biouml.plugins.download.FileDownloader;
import biouml.plugins.download.FileDownloader.RemoteFileInfo;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.DataElementImporterRegistry;
import ru.biosoft.access.DataElementImporterRegistry.ImporterInfo;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.generic.GenericDataCollection;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.task.RunnableTask;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListenerAdapter;
import ru.biosoft.server.JSONUtils;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.WebServicesServlet;
import ru.biosoft.server.servlets.webservices.providers.WebBeanProvider;
import ru.biosoft.server.servlets.webservices.providers.WebJSONProviderSupport;
import ru.biosoft.table.TableCSVImporter.NullImportProperties;
import ru.biosoft.util.BeanUtil;

public class ImportProvider extends WebJSONProviderSupport
{

    private static ImportDAO dao = new ImportDAO();
    private static Map<Integer, JobControl> importsRunning = new HashMap<>();

    @Override
    public void process(BiosoftWebRequest args, JSONResponse resp) throws Exception
    {
        String action = args.getAction();
        if("list".equals(action))
        {
            resp.sendJSON( JSONUtils.toSimpleJSONArray( DataElementImporterRegistry.importers().toList() ) );
            return;
        }
        //lock import functions for trial users
        if( !SecurityManager.isProductAvailable("Import") )
        {
            throw new SecurityException("Import is not available for current user");
        }
        switch(action)
        {
            case "create_upload":
                processCreateUpload(args, resp);
                break;
            case "create_remote_upload":
                processCreateRemoteUpload(args, resp);
                break;
            case "start_upload":
                processStartUpload(args, resp);
                break;
            case "remote_upload":
                processRemoteUpload(args, resp);
                break;
            case "set_target_folder":
                processSetTargetFolder(args, resp);
                break;
            case "detect_format":
                processDetectFormat(args, resp);
                break;
            case "get_format":
                processGetFormat( args, resp );
                break;
            case "set_format":
                processSetFormat(args, resp);
                break;
            case "get_format_options":
                processGetFormatOptions(args, resp);
                break;
            case "set_format_options":
                processSetFormatOptions(args, resp);
                break;
            case "get_omics_type":
                processGetOmicsType(args, resp);
                break;
            case "set_omics_type":
                processSetOmicsType(args, resp);
                break;
            case "start_import":
                processStartImport(args, resp);
                break;
            case "cur_imports":
                processCurrentImports(args, resp);
                break;
            case "cancel":
                processCancel(args, resp);
                break;
            case "delete_file":
                processDeleteUploadedFile( args, resp );
                break;
            default:
                throw new WebException( "EX_QUERY_PARAM_INVALID_VALUE", "action" );
        }
    }


    private ImportRecord getImportRecord(BiosoftWebRequest args) throws WebException
    {
        return optImportRecord( args ).orElseThrow( () -> new RuntimeException("No such id") );
    }

    private Optional<ImportRecord> optImportRecord(BiosoftWebRequest args) throws WebException
    {
        int id = args.getInt( "id" );
        return dao.findByIdAndUser( id, SecurityManager.getSessionUser() );
    }


    private void processCreateUpload(BiosoftWebRequest args, JSONResponse resp) throws WebException, IOException
    {
        String fileName = args.getString( "fileName" );
        String fileSizeStr = args.getString( "fileSize" );
        long fileSize;
        try
        {
            fileSize = Long.parseLong( fileSizeStr );
        }
        catch( NumberFormatException e )
        {
            throw new WebException( "EX_QUERY_PARAM_INVALID_VALUE", "fileSize" );
        }
        DataElementPath targetFolder = DataElementPath.create( args.getString( "targetFolder" ) );

        ImportRecord rec = new ImportRecord( targetFolder, fileName, fileSize );
        dao.insertNewRecord( rec );

        resp.sendString( String.valueOf( rec.getId() )  );
    }

    private void processCreateRemoteUpload(BiosoftWebRequest args, JSONResponse resp) throws WebException, IOException, JSONException
    {
        String urlSpec = args.getString( "url" );
        URL url = new URL( urlSpec );
        RemoteFileInfo fileInfo = FileDownloader.getFileInfoFromHeader( url );
        String fileName = fileInfo.getFileName();
        int size = fileInfo.getFileSize();

        DataElementPath targetFolder = DataElementPath.create( args.getString( "targetFolder" ) );

        ImportRecord rec = new ImportRecord( targetFolder, fileName, 0 );
        rec.setSourceURL( url );
        dao.insertNewRecord( rec );

        JSONObject res = new JSONObject();
        res.put( "id", rec.getId() );
        res.put( "fileName", fileName );
        if( size != -1 )
            res.put( "size", size );
        resp.sendJSON( res );
    }

    private static String getFileSuffixByName(String fileName)
    {
        String suffix = "";
        Matcher m = Pattern.compile(".+(\\.\\w+)").matcher(fileName);
        if( m.matches() )
        {
            suffix = m.group(1);
        }
        return suffix;
    }

    private void processStartUpload(BiosoftWebRequest args, JSONResponse resp) throws Exception
    {
        ImportRecord rec = getImportRecord( args );
        File uploadDir = new File( WebServicesServlet.UPLOAD_DIRECTORY, "upload_" + rec.getId() );
        uploadDir.mkdir();
        File destFile = new File( uploadDir, rec.getFileName() );
        boolean error = false;
        try
        {
            Object item = args.getFileItem();
            if( item == null )
                throw new Exception( "Upload failure" );
            String fileName = (String)item.getClass().getMethod( "getName", new Class<?>[] {} ).invoke( item, new Object[] {} );
            long fileSize = (Long)item.getClass().getMethod( "getSize", new Class<?>[] {} ).invoke( item, new Object[] {} );

            if( rec.getFileSize() != fileSize )
                throw new WebException( "EX_QUERY_PARAM_INVALID_VALUE", "fileSize" );

            item.getClass().getMethod( "write", new Class<?>[] {File.class} ).invoke( item, new Object[] {destFile} );
        }
        catch( Exception e )
        {
            log.log( Level.SEVERE, "While uploading " + rec.getId(), e );
            error = true;
            removeUpload( destFile );
            throw e;
        }
        finally
        {
            Optional<ImportRecord> optRec = optImportRecord( args );
            if( optRec.isPresent() )
            {
                rec = optRec.get();
                rec.setFile( destFile );
                if( error )
                {
                    rec.setStatus( ImportStatus.UPLOAD_ERROR );
                }
                else
                {
                    rec.setStatus( ImportStatus.UPLOAD_FINISHED );
                    rec.setUploadProgress( 100 );
                }
                dao.updateRecord( rec );
            }
            else
            {
                removeUpload( destFile );
            }
        }
        resp.sendString( "ok" );
    }

    private void processRemoteUpload(BiosoftWebRequest args, JSONResponse resp) throws Exception
    {
        ImportRecord rec = getImportRecord( args );
        rec.setStatus( ImportStatus.UPLOADING );
        dao.updateRecord( rec );

        startRemoteUpload( rec, log );

        resp.sendString( "ok" );
    }

    private static void startRemoteUpload(ImportRecord rec, Logger log)
    {
        TaskPool.getInstance().submit( new RunnableTask( "Remote upload " + rec.getId(), ()->{
            try
            {
                remoteUpload( rec, log );
            }
            catch( Exception e )
            {
                throw new RuntimeException();
            }
        }));
    }

    private static void remoteUpload(ImportRecord rec, Logger log) throws Exception
    {
        boolean error = false;
        File destFile = null;
        FunctionJobControl job = new FunctionJobControl( log );
        UploadProgressReporter progress = new UploadProgressReporter( rec.getId(), rec.getUser() );
        job.addListener( progress );
        job.functionStarted();
        try
        {
            String fileName = rec.getFileName();
            String suffix = getFileSuffixByName(fileName);
            File uploadDir = new File( WebServicesServlet.UPLOAD_DIRECTORY, "upload_" + rec.getId() );
            uploadDir.mkdir();
            destFile = new File( uploadDir, rec.getFileName() );

            String newFileName = FileDownloader.downloadFile(rec.getSourceURL(), destFile, job, destFile.exists());
            rec.setFileName( newFileName );
            if(!newFileName.equals(fileName))
            {
                String newSuffix = getFileSuffixByName(newFileName);
                if(!newSuffix.equals(suffix))
                {
                    File newFile = new File( uploadDir, newFileName );
                    if( destFile.renameTo( newFile ) )
                        destFile = newFile;
                }
            }
            progress.setTotalSize( destFile.length() );
            job.functionFinished();
        }
        catch(Exception e)
        {
            log.log(Level.SEVERE,  "While uploading " + rec.getId(), e );
            error = true;
            if( destFile != null && !destFile.delete() )
                log.log( Level.WARNING, "Can not delete file " + destFile.getAbsolutePath() );
            job.functionTerminatedByError( e );
            throw e;
        }
        finally
        {
            Optional<ImportRecord> optRec = dao.findByIdAndUser( rec.getId(), rec.getUser() );
            if( optRec.isPresent() )
            {
                rec = optRec.get();
                rec.setFile( destFile );
                if( error )
                {
                    rec.setStatus( ImportStatus.UPLOAD_ERROR );
                }
                else
                {
                    rec.setStatus( ImportStatus.UPLOAD_FINISHED );
                    rec.setUploadProgress( 100 );
                    rec.setFileSize( destFile.length() );
                }
                dao.updateRecord( rec );
            }
            else
            {
                removeUpload( destFile );
            }
        }
    }


    private void processSetTargetFolder(BiosoftWebRequest args, JSONResponse resp) throws WebException, IOException
    {
        ImportRecord rec = getImportRecord( args );
        DataElementPath targetFolder = DataElementPath.create( args.getString( "targetFolder" ) );
        rec.setTargetFolder( targetFolder );
        dao.updateRecord( rec );
        resp.sendString( "ok" );
    }

    private void processDetectFormat(BiosoftWebRequest args, JSONResponse resp) throws IOException, WebException, JSONException
    {
        ImportRecord rec = getImportRecord( args );
        File file = rec.getFile();
        if(file == null || !file.exists())
        {
            resp.error( "No file for id=" + rec.getId() );
            return;
        }
        DataCollection<?> dc = rec.getTargetFolder().optDataCollection();
        if(dc == null)
        {
            resp.error( "No target folder for id=" + rec.getId() );
            return;
        }
        boolean needReliable = args.getBoolean( "reliable" );
        ImporterInfo[] importerInfos = DataElementImporterRegistry.getAutoDetectImporter( file, dc, true,
                needReliable ? DataElementImporter.ACCEPT_BELOW_MEDIUM_PRIORITY : DataElementImporter.ACCEPT_UNSUPPORTED );

        List<ImporterInfo> primaryImporters = new ArrayList<>();
        Set<ImporterInfo> secondaryImporters = DataElementImporterRegistry.importers().toSet();

        if( ( importerInfos == null || importerInfos.length == 0 ) && secondaryImporters.isEmpty() )
        {
            resp.error( "No import formats available" );
            return;
        }

        if( importerInfos != null && importerInfos.length > 0 )
        {
            for( ImporterInfo importer : importerInfos )
            {
                primaryImporters.add( importer );
                secondaryImporters.remove( importer );
            }

            String format = primaryImporters.get( 0 ).getFormat();
            String formatName = primaryImporters.get( 0 ).getDisplayName();
            List<String> primaryFormats = StreamEx.of( primaryImporters ).map( ImporterInfo::getFormat ).toList();
            rec.setFormatList( primaryFormats );
            rec.setFormat( format );
            OmicsType omicsType = autoDetectOmicsType( format, formatName, rec.getFormatOptions(), rec.getFileName() );
            rec.setStatus( ImportStatus.FORMAT_DETECTED );
            rec.setOmicsType( omicsType );
            dao.updateRecord( rec );
        }

        JSONObject res = new JSONObject();
        JSONArray primary = new JSONArray();
        for( ImporterInfo importer : primaryImporters )
            primary.put( createFormatOption( importer ) );
        res.put( "primary", primary );

        JSONArray secondary = new JSONArray();
        StreamEx.of( secondaryImporters ).sortedBy( ImporterInfo::getFormat ).map( ImportProvider::createFormatOption )
                .forEach( primary::put );
        res.put( "secondary", secondary );

        if( rec.getFormat() != null )
            res.put( "format", rec.getFormat() );
        res.put( "omicsType", rec.getOmicsType() != null ? rec.getOmicsType().toString() : "" );
        resp.sendJSON( res );
    }
    private static JSONObject createFormatOption(ImporterInfo importer)
    {
        JSONObject importerObj = new JSONObject();
        importerObj.put( "format", importer.getFormat() );
        importerObj.put( "displayName", importer.getDisplayName() );
        return importerObj;
    }

    private void processGetFormat(BiosoftWebRequest args, JSONResponse resp) throws IOException, WebException, JSONException
    {
        ImportRecord rec = getImportRecord( args );
        File file = rec.getFile();
        if( file == null || !file.exists() )
        {
            resp.error( "No file for id=" + rec.getId() );
            return;
        }
        DataCollection<?> dc = rec.getTargetFolder().optDataCollection();
        if( dc == null )
        {
            resp.error( "No target folder for id=" + rec.getId() );
            return;
        }
        List<ImporterInfo> primaryImporters = new ArrayList<>();
        Set<ImporterInfo> secondaryImporters = DataElementImporterRegistry.importers().toSet();

        List<String> formats = rec.getFormatList();
        if( ( formats == null || formats.isEmpty() ) && secondaryImporters.isEmpty() )
        {
            resp.error( "No import formats available" );
            return;
        }

        for( String format : formats )
        {
            ImporterInfo importer = secondaryImporters.stream().filter( imp -> imp.getFormat().equals( format ) ).findAny().orElse( null );
            if( importer != null )
            {
                primaryImporters.add( importer );
                secondaryImporters.remove( importer );
            }
        }

        JSONObject res = new JSONObject();
        JSONArray primary = new JSONArray();
        for( ImporterInfo importer : primaryImporters )
            primary.put( createFormatOption( importer ) );
        res.put( "primary", primary );

        JSONArray secondary = new JSONArray();
        StreamEx.of( secondaryImporters ).sortedBy( ImporterInfo::getFormat ).map( ImportProvider::createFormatOption )
                .forEach( primary::put );
        res.put( "secondary", secondary );

        if( rec.getFormat() != null )
            res.put( "format", rec.getFormat() );
        res.put( "omicsType", rec.getOmicsType() != null ? rec.getOmicsType().toString() : "" );

        resp.sendJSON( res );
    }

    private OmicsType autoDetectOmicsType(String format, String formatDisplayName, Object formatOptions, String fileName)
    {
        if(formatOptions instanceof NullImportProperties)
        {
            NullImportProperties tableOptions = (NullImportProperties)formatOptions;
            String referenceTypeStr = tableOptions.getTableType();
            if(referenceTypeStr == null || referenceTypeStr.equals( "(auto)" ))
                return null;
            ReferenceType referenceType = ReferenceTypeRegistry.getReferenceType( referenceTypeStr );
            return OmicsType.getByReferenceType( referenceType );
        }
        String formatBasic = formatDisplayName;//format.replaceAll( " GE$", "" );
        if( formatBasic.equals( "BED format (*.bed)" ) || formatBasic.equals( "Interval format (*.interval)" )
                || formatBasic.equals( "Wiggle format (*.wig)" ) )
            return OmicsType.Epigenomics;
        if( formatBasic.equals( "VCF format (*.vcf)" ) || formatBasic.equals( "SAM or BAM alignment file (*.sam, *.bam)" ) )
            return OmicsType.Genomics;
        if( formatBasic.equals( "Affymetrix CEL file (*.cel)" ) || formatBasic.equals( "Agilent microarray file (*.txt)" )
                || formatBasic.equals( "Illumina microarray file (*.txt)" ) )
            return OmicsType.Transcriptomics;
        return null;
    }

    private void processSetFormat(BiosoftWebRequest args, JSONResponse resp) throws WebException, IOException
    {
        ImportRecord rec = getImportRecord( args );
        String format = args.getString( "format" );
        String displayName = args.getString( "displayName" );
        DataCollection<?> targetDC = rec.getTargetFolder().getDataCollection();
        File file = rec.getFile();
        DataElementImporter importer = DataElementImporterRegistry.getImporter( file, format, targetDC );
        if( importer != null )
        {
            try
            {
                if( importer.accept( targetDC, file ) > DataElementImporter.ACCEPT_UNSUPPORTED )
                {
                    rec.setFormat( format );
                    rec.setStatus( ImportStatus.FORMAT_SET );
                    OmicsType omicsType = autoDetectOmicsType( format, displayName, rec.getFormatOptions(), rec.getFileName() );
                    rec.setOmicsType( omicsType );
                    dao.updateRecord( rec );

                    JSONObject res = new JSONObject();
                    if( rec.getFormat() != null )
                        res.put( "format", rec.getFormat() );
                    res.put( "omicsType", rec.getOmicsType() != null ? rec.getOmicsType().toString() : "" );
                    resp.sendJSON( res );
                    //resp.sendString( "ok" );
                    return;
                }
            }
            catch( Exception e )
            {
            }
        }
        resp.error( "Can not set format" );
    }

    private void processGetFormatOptions(BiosoftWebRequest args, JSONResponse resp) throws IOException, WebException
    {
        ImportRecord rec = getImportRecord( args );
        Object options = rec.getFormatOptions();
        if(options == null)
            resp.sendString( "No options available" );
        else
            WebBeanProvider.sendBeanStructure( "", options, resp );
    }

    private void processSetFormatOptions(BiosoftWebRequest args, JSONResponse resp) throws Exception
    {
        ImportRecord rec = getImportRecord( args );
        Object options = rec.getFormatOptions();
        if(options == null)
            resp.error( "No options available" );
        JSONArray json = args.getJSONArray( "json" );
        JSONUtils.correctBeanOptions( options, json, true );
        dao.updateRecord( rec );
        WebBeanProvider.sendBeanStructure( "", options, resp );
    }


    private void processGetOmicsType(BiosoftWebRequest args, JSONResponse resp) throws WebException, IOException
    {
        ImportRecord rec = getImportRecord( args );
        OmicsType omicsType = rec.getOmicsType();
        resp.sendString( omicsType == null ? null : omicsType.toString() );
    }

    private void processSetOmicsType(BiosoftWebRequest args, JSONResponse resp) throws WebException, IOException
    {
        ImportRecord rec = getImportRecord( args );
        String omicsTypeStr = args.get( "omicsType" );
        OmicsType omicsType = omicsTypeStr == null ? null : OmicsType.valueOf( omicsTypeStr );
        rec.setOmicsType( omicsType );
        dao.updateRecord( rec );
        resp.sendString( "ok" );
    }

    private void processStartImport(BiosoftWebRequest args, JSONResponse resp) throws Exception
    {
        ImportRecord rec = getImportRecord( args );

        try {
            startImport( rec, log );
        } catch(Exception e)
        {
            resp.error( e.getMessage() );
            return;
        }


        resp.sendString( "ok" );
    }

    private static void startImport(ImportRecord rec, Logger log) throws Exception
    {
        final int id = rec.getId();
        if(rec.getStatus().ordinal() < ImportStatus.FORMAT_DETECTED.ordinal())
            throw new Exception("Format not set");

        DataCollection<?> targetDC = rec.getTargetFolder().getDataCollection();
        File file = rec.getFile();
        if(file == null || !file.exists())
            throw new Exception("Uploaded file not found");

        DataElementImporter importer = DataElementImporterRegistry.getImporter( file, rec.getFormat(), targetDC );
        Object options = importer.getProperties( targetDC, file, rec.getTargetElementName() );
        BeanUtil.copyBean( rec.getFormatOptions(), options );

        rec.setStatus( ImportStatus.IMPORTING );
        dao.updateRecord( rec );

        String taskName = "Import of " + rec.getTargetElementName() + " (format: " + rec.getFormat() + "; user: " + rec.getUser() + "; id=" + rec.getId() + ")";
        TaskPool.getInstance().submit( new RunnableTask( taskName, () -> {
            FunctionJobControl jobControl = new FunctionJobControl( log );
            importsRunning.put( id, jobControl );
            jobControl.addListener( new ImportProgressReporter( id ) );
            jobControl.addListener( new JobControlListenerAdapter()
            {
                @Override
                public void jobTerminated(JobControlEvent event)
                {
                    importsRunning.remove( id );
                }
            } );
            try
            {
                DataElement result = importer.doImport( targetDC, file, rec.getTargetElementName(), jobControl, log );
                if( result != null )
                {
                    Optional<ImportRecord> optRec = dao.findByIdAndUser( id, SecurityManager.getSessionUser() );
                    if( optRec.isPresent() )
                    {
                        ImportRecord curRec = optRec.get();
                        curRec.setDEName( result.getName() );
                        dao.updateRecord( curRec );
                    }
                }
                //TODO: add file removal here
                OmicsType omicsType = rec.getOmicsType();
                if( omicsType != null )
                {
                    DataCollection<?> primaryParent = (DataCollection<?>)SecurityManager.runPrivileged( () -> {
                        return DataCollectionUtils.fetchPrimaryCollectionPrivileged( targetDC );
                    } );
                    if( primaryParent instanceof GenericDataCollection )
                    {
                        OmicsTypeHelper.setChildOmicsType( (GenericDataCollection)primaryParent, result, omicsType );
                    }
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE,  "Import id=" + rec.getId(), e );
                jobControl.functionTerminatedByError( e );
            }
        } ) );
    }

    private void processCurrentImports(BiosoftWebRequest args, JSONResponse resp) throws JSONException, IOException
    {
        List<ImportRecord> jobs = dao.fetchImports( SecurityManager.getSessionUser() );
        JSONArray res = new JSONArray();
        for(ImportRecord job : jobs)
        {
            if( job.getStatus().ordinal() >= ImportStatus.UPLOAD_FINISHED.ordinal() && job.getStatus() != ImportStatus.UPLOAD_ERROR
                    && job.getStatus() != ImportStatus.DONE )
            {
                if(!job.getFile().exists())
                {
                    log.warning( "File not found for import " + job.getId() + ": " + job.getFile() );
                    continue;
                }
            }
            JSONObject jobInfo = new JSONObject();
            jobInfo.put( "id", job.getId() );
            jobInfo.put( "importID", job.getImportId() );
            jobInfo.put( "uploadID", job.getUploadId() );
            jobInfo.put( "status", job.getStatus().toString() );
            jobInfo.put( "fileName", job.getFileName() );
            jobInfo.put( "fileSize", job.getFileSize() );
            jobInfo.put( "startTime", job.getStartTime() );
            jobInfo.put( "targetFolder", job.getTargetFolder().toString() );
            jobInfo.put( "uploadProgress", job.getUploadProgress() );
            jobInfo.put( "formatList", job.getFormatList() );
            jobInfo.put( "format", job.getFormat() );
            jobInfo.put( "importProgress", job.getImportProgress() );
            if(job.getSourceURL() != null)
                jobInfo.put( "sourceURL", job.getSourceURL());
            res.put( jobInfo );
        }
        resp.sendJSON( res );
    }

    private void processCancel(BiosoftWebRequest args, JSONResponse resp) throws WebException, IOException
    {
        ImportRecord rec = getImportRecord( args );
        ImportStatus status = rec.getStatus();

        //TODO: cancel running jobs
        removeUpload( rec.getFile() );
        dao.removeById( rec.getId() );

        if( status == ImportStatus.IMPORTING )
        {
            if( importsRunning.containsKey( rec.getId() ) )
                importsRunning.get( rec.getId() ).terminate();
        }
        else if( status == ImportStatus.DONE )
        {
            String realName = rec.getDEName() == null ? rec.getTargetElementName() : rec.getDEName();
            DataElementPath targetElementPath = rec.getTargetFolder().getChildPath( realName );
            targetElementPath.remove();
            JSONObject remove = new JSONObject();
            remove.put( "path", targetElementPath.toString() );
            resp.sendJSON( remove );
            return;
        }

        resp.sendString( "ok" );
    }


    private void processDeleteUploadedFile(BiosoftWebRequest args, JSONResponse resp) throws WebException, IOException
    {
        ImportRecord rec = getImportRecord( args );
        File file = rec.getFile();
        if( file == null || !file.exists() )
            return;
        removeUpload( file );
        resp.sendString( "ok" );
    }

    private static void removeUpload(File file)
    {
        if( file != null )
        {
            File uploadDir = file.getParentFile();
            file.delete();
            if( uploadDir != null && uploadDir.exists() && uploadDir.list().length == 0 )
                uploadDir.delete();
        }
    }

    public static void init() throws Exception
    {
        continueAbortedRemoteUploads();

        SecurityManager.runPrivileged( () -> {
            resetActiveImports();
            return null;
        } );
    }

    private static final Logger staticLogger = Logger.getLogger( ImportProvider.class.getName() );

    private static void continueAbortedRemoteUploads()
    {
        for(ImportRecord rec : dao.fetchActiveRemoteUploads())
        {
            startRemoteUpload( rec, staticLogger );
        }
    }

    private static void resetActiveImports()
    {
        for(ImportRecord rec : dao.fetchActiveImports())
        {
            rec.setStatus( ImportStatus.FORMAT_DETECTED );
            dao.updateRecord( rec );
        }
    }
}
