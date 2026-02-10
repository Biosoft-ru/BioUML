package ru.biosoft.server.servlets.webservices.providers;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import com.developmentontheedge.beans.DynamicProperty;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.DataElementImporterRegistry;
import ru.biosoft.access.DataElementImporterRegistry.ImporterInfo;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.SessionCache;
import ru.biosoft.access.task.RunnableTask;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListenerAdapter;
import ru.biosoft.journal.Journal;
import ru.biosoft.journal.JournalRegistry;
import ru.biosoft.server.JSONUtils;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebJob;
import ru.biosoft.server.servlets.webservices.WebServicesServlet;
import ru.biosoft.server.servlets.webservices.WebSession;
import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.tasks.TaskManager;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.DPSUtils;
import ru.biosoft.util.FileItem;

/**
 * @author lan
 *
 */
public class ImportProvider extends WebJSONProviderSupport
{
    private static final String TYPE_DELETE_UPLOAD = "deleteUpload";
    private static final String TYPE_IMPORT = "import";
    private static final String TYPE_PROPERTIES = "properties";
    private static final String TYPE_DETECT = "detect";
    private static final String TYPE_DE_INFO = "deInfo";
    private static final String LIST_ACTION = "list";

    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse resp) throws Exception
    {
        if(LIST_ACTION.equals(arguments.optAction()))
        {
            resp.sendJSON( JSONUtils.toSimpleJSONArray( DataElementImporterRegistry.importers().toList() ) );
            return;
        }

        String type = arguments.getString("type");
        String fileID = arguments.get( "fileID" );
        final FileItem file = WebServicesServlet.getUploadedFile( fileID );

        if( TYPE_DELETE_UPLOAD.equals( type ) )
        {
            //upload actually not started
            if( file == null )
                return;
            boolean deleted = file.delete();
            WebSession.getCurrentSession().removeValue( "uploadedFile_" + fileID );
            WebSession.getCurrentSession().removeValue( "uploadedFileSuffix_" + fileID );
            if( !deleted )
            {
                log.log( Level.WARNING,
                        "Can not delete file for import (fileID='" + fileID + "', file name='" + file.getOriginalName() + "')." );
            }
            resp.sendString( deleted ? "deleted" : "not deleted" );
            return;
        }

        //lock import functions for trial users
        if( !SecurityManager.isProductAvailable("Import") )
        {
            throw new SecurityException("Import is not available for current user");
        }
        final DataCollection<?> dc = arguments.getDataCollection();

        final String format = arguments.get("format");
        final String jobID = arguments.get("jobID");
        SessionCache sessionCache = WebServicesServlet.getSessionCache();

        if( TYPE_DE_INFO.equals( type ) )
        {
            ImporterInfo[] info = DataElementImporterRegistry.getAutoDetectImporter(null, dc, true);
            JSONArray jsonFormats = new JSONArray();
            if( info != null )
            {
                for( ImporterInfo importer : info )
                    jsonFormats.put( createFormatOption( importer ) );
            }
            resp.sendJSON(jsonFormats);
        }
        else if( TYPE_DETECT.equals( type ) )
        {
            if( file == null )
            {
                throw new IllegalArgumentException("Unable to open requested file; try to upload again");
            }
            ImporterInfo[] importerInfos = DataElementImporterRegistry.getAutoDetectImporter(file, dc, true);
            if( importerInfos != null )
            {
                if( importerInfos.length >= 1 )
                {
                    JSONArray jsonFormats = new JSONArray();
                    for( ImporterInfo importer : importerInfos )
                        jsonFormats.put( createFormatOption( importer ) );
                    resp.sendJSON(jsonFormats);
                    return;
                }
            }
            else
            {
                throw new IllegalArgumentException( "No appropriate importer found for this file: maybe it's not supported" );
            }
        }
        else if( TYPE_PROPERTIES.equals( type ) )
        {
            if( format == null || jobID == null )
            {
                throw new IllegalArgumentException("Invalid arguments");
            }
            JSONArray jsonArray = arguments.optJSONArray("json");
            String fileName = file == null ? null : file.getOriginalName().replaceFirst("\\.\\w+$","");
            Object oldFormat = sessionCache.getObject(WebBeanProvider.BEANS_PREFIX + jobID + "/format");
            Object oldFileID = sessionCache.getObject(WebBeanProvider.BEANS_PREFIX + jobID + "/fileID");
            Object bean = sessionCache.getObject(WebBeanProvider.BEANS_PREFIX + jobID);
            ImporterInfo importerInfo = DataElementImporterRegistry.getImporterInfo(format);
            if( oldFormat == null || !oldFormat.equals(format) )
            {
                bean = importerInfo == null ? null : importerInfo.cloneImporter().getProperties(dc, file, fileName);
                if( bean != null )
                    sessionCache.addObject( WebBeanProvider.BEANS_PREFIX + jobID, bean, true );
                else
                    sessionCache.removeObject( WebBeanProvider.BEANS_PREFIX + jobID );
                sessionCache.addObject(WebBeanProvider.BEANS_PREFIX + jobID + "/format", format, true);
                if( fileID != null )
                    sessionCache.addObject(WebBeanProvider.BEANS_PREFIX + jobID + "/fileID", fileID, true);
            }
            else if( ( oldFileID == null && fileID != null ) || ( fileID == null && oldFileID != null )
                    || ( fileID != null && oldFileID != null && !fileID.equals(oldFileID) ) )
            {
                Object newBean = importerInfo == null ? null : importerInfo.cloneImporter().getProperties(dc, file, fileName);
                if( newBean != null )
                    BeanUtil.copyBean(bean, newBean);
                bean = newBean;
                if( bean != null )
                    sessionCache.addObject( WebBeanProvider.BEANS_PREFIX + jobID, bean, true );
                else
                    sessionCache.removeObject( WebBeanProvider.BEANS_PREFIX + jobID );
                if( fileID != null )
                    sessionCache.addObject(WebBeanProvider.BEANS_PREFIX + jobID + "/fileID", fileID, true);
            }
            if( jsonArray != null && bean != null )
            {
                JSONUtils.correctBeanOptions(bean, jsonArray);
            }
            if(bean != null)
                WebBeanProvider.sendBeanStructure("", bean, resp);
            else
                resp.error( "No import parameters for " + format );
        }
        else if( TYPE_IMPORT.equals( type ) )
        {
            if( file == null || fileID == null || format == null || jobID == null )
            {
                throw new IllegalArgumentException("Invalid arguments");
            }
            JSONArray jsonArray = arguments.optJSONArray("json");
            final String origFileName = file.getOriginalName();
            final String fileName = origFileName.replaceFirst("\\.\\w+$", "");
            final DataElementImporter importer = DataElementImporterRegistry.getImporter(file, format, dc);
            if( importer == null )
            {
                sessionCache.removeObject(WebBeanProvider.BEANS_PREFIX + jobID + "/format");
                sessionCache.removeObject(WebBeanProvider.BEANS_PREFIX + jobID + "/fileID");
                sessionCache.removeObject(WebBeanProvider.BEANS_PREFIX + jobID);
                throw new IllegalArgumentException("Format is not supported by selected importer: select another one or try autodetect");
            }
            Object oldFormat = sessionCache.getObject(WebBeanProvider.BEANS_PREFIX + jobID + "/format");
            Object oldFileID = sessionCache.getObject(WebBeanProvider.BEANS_PREFIX + jobID + "/fileID");
            Object bean = sessionCache.getObject(WebBeanProvider.BEANS_PREFIX + jobID);
            ImporterInfo importerInfo = DataElementImporterRegistry.getImporterInfo(format);
            if( oldFormat == null || !oldFormat.equals(format) )
            {
                bean = importerInfo == null ? null : importerInfo.cloneImporter().getProperties(dc, file, fileName);
            }
            else if( oldFileID == null || !fileID.equals(oldFileID) )
            {
                Object newBean = importerInfo == null ? null : importerInfo.cloneImporter().getProperties(dc, file, fileName);
                if( newBean != null )
                    BeanUtil.copyBean(bean, newBean);
                bean = newBean;
            }
            if( jsonArray != null && bean != null )
            {
                JSONUtils.correctBeanOptions(bean, jsonArray);
            }
            Object properties = importer.getProperties(dc, file, fileName);
            if( bean != null && properties != null )
            {
                BeanUtil.copyBean(bean, properties);
            }
            final Journal journal = JournalRegistry.getCurrentJournal();
            final TaskInfo task = journal == null ? null : journal.getEmptyAction();
            sessionCache.removeObject(WebBeanProvider.BEANS_PREFIX + jobID + "/format");
            sessionCache.removeObject(WebBeanProvider.BEANS_PREFIX + jobID + "/fileID");
            sessionCache.removeObject(WebBeanProvider.BEANS_PREFIX + jobID);
            final Object finalBean = bean;
            // TODO pass logger messages to the client
            TaskPool.getInstance().submit(
                    new RunnableTask("Import of " + origFileName + " (format: " + format + "; user: " + SecurityManager.getSessionUser()
                            + ")", () -> {
                                final WebJob webJob = WebJob.getWebJob(jobID);
                                ImportJobControl job = new ImportJobControl(webJob.getJobLogger());
                                job.addListener( new JobControlListenerAdapter() {
                                    @Override
                                    public void jobTerminated(JobControlEvent event)
                                    {
                                        if(event.getException() != null)
                                        {
                                            webJob.addJobMessage( ExceptionRegistry.log(event.getException().getError()) );
                                        }
                                    }
                                });
                                webJob.setJobControl(job);
                                job.functionStarted();
                                DataElement result = null;
                                try
                                {
                                    result = importer.doImport(dc, file, fileName, job, log);
                                }
                                catch( Exception e )
                                {
                                    job.functionTerminatedByError(e);
                                }
                                if( result != null )
                                {
                                    job.resultsAreReady( new Object[] { result } );
                                    webJob.addJobMessage( DataElementPath.create( result ).toString() );
                                    job.reallyFinished();
                                }
                                if( webJob.getJobControl().getStatus() == JobControl.COMPLETED)
                                    WebSession.getCurrentSession().pushRefreshPath( DataElementPath.create( dc ) );
                                if( webJob.getJobControl().getStatus() == JobControl.COMPLETED && task != null )
                                {
                                    task.setType(TaskInfo.IMPORT);
                                    task.setData(origFileName);
                                    task.getAttributes().add(
                                            new DynamicProperty(TaskInfo.IMPORT_OUTPUT_PROPERTY_DESCRIPTOR, String.class, DataElementPath
                                                    .create(dc, fileName).toString()));
                                    task.getAttributes().add(
                                            new DynamicProperty(TaskInfo.IMPORT_FORMAT_PROPERTY_DESCRIPTOR, String.class, format));
                                    Long len = file.length();
                                    task.getAttributes().add( new DynamicProperty( TaskInfo.IMPORT_FILESIZE_PROPERTY_DESCRIPTOR, Long.class, len ) );
                                    if( finalBean != null )
                                    {
                                        DPSUtils.writeBeanToDPS(finalBean, task.getAttributes(), DPSUtils.PARAMETER_ANALYSIS_PARAMETER + ".");
                                    }
                                    task.setEndTime();
                                    task.setUser( SecurityManager.getSessionUser() );
                                    journal.addAction(task);
                                    //Add hidden task record to tasks table
                                    TaskManager.logHiddenTaskRecord( task );
                                }

                            }));
            resp.sendString(jobID);
        }
    }

    private static JSONObject createFormatOption(ImporterInfo importer)
    {
        JSONObject jsonFormat = new JSONObject();
        jsonFormat.put( "format", importer.getFormat() );
        jsonFormat.put( "displayName", importer.getDisplayName() );
        return jsonFormat;
    }

    private static class ImportJobControl extends FunctionJobControl
    {
        public ImportJobControl(Logger l)
        {
            super( l );
        }
        @Override
        public void functionFinished(String msg)
        {
        }

        public void reallyFinished()
        {
            super.functionFinished();
        }
    }
}
