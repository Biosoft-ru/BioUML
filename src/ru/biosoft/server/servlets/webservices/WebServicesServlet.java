package ru.biosoft.server.servlets.webservices;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;

import biouml.plugins.download.FileDownloader;
import biouml.plugins.download.FileDownloader.RemoteFileInfo;
import biouml.plugins.server.access.AccessProtocol;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.SessionCache;
import ru.biosoft.access.security.SessionCacheManager;
import ru.biosoft.access.security.UserPermissions;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.plugins.Plugins;
import ru.biosoft.server.AbstractServlet;
import ru.biosoft.server.Connection;
import ru.biosoft.server.JSONUtils;
import ru.biosoft.server.servlets.webservices.providers.WebProvider;
import ru.biosoft.server.servlets.webservices.providers.WebProviderFactory;
import ru.biosoft.util.FileItem;
import ru.biosoft.util.TextUtil2;

/**
 * Access to BioUML services via servlet
 */
public class WebServicesServlet extends AbstractServlet
{
    protected static final Logger log = Logger.getLogger(WebServicesServlet.class.getName());

    //
    //Request types
    //
    public static final String PING = "ping";
    public static final String TABLE = "table";
    public static final String LOGIN = "login";
    public static final String LOGOUT = "logout";
    public static final String EXPORT = "export";
    public static final String IMPORT = "import";
    public static final String UPLOAD = "upload";

    //
    //Request parameters constants
    //
    public static final String TYPE = "type";
    public static final String SHOW_MODE = "showMode";

    public static final String IMAGES = "images";
    protected static final String JSON_ATTR = "json";

    //
    //Directory for temporary files
    //
    public static final String UPLOAD_DIRECTORY = System.getProperty( "biouml.upload_dir", System.getProperty("java.io.tmpdir"));

    @Override
    public void init(String[] args) throws Exception
    {
        super.init(args);
        //initialize embedded JavaScript functions
        Plugins.getPlugins();
    }

    @Override
    public String service(String localAddress, Object session, Map params, OutputStream out, Map<String, String> header)
    {
        return "text/html";
    }

    public void service(String localAddress, Object session, Map params, OutputStream out, Object respObj)
    {
        //correct map values
        Map<String, String> arguments = convertParams(params);

        try
        {
            BiosoftWebResponse resp = new BiosoftWebResponse(respObj, out);
            WebSession webSession = WebSession.getSession(session);

            WebSession authWebSession = null;
            String sessionId;
            if( arguments.containsKey( SecurityManager.SESSION_ID ) )
            {
                String authSessionId = arguments.get( SecurityManager.SESSION_ID );
                authWebSession = WebSession.findSession( authSessionId );
                log.log( Level.INFO, "Autorizing via SessonID = " + authSessionId + "', arguments = \n" + arguments );
                log.log( Level.INFO, "authWebSession = " + authWebSession );
                sessionId = webSession.getSessionId();
            }
            else
            {
                sessionId = webSession.getSessionId();
                //set session argument for Service requests
                arguments.put(SecurityManager.SESSION_ID, sessionId);
            }

            int subServletIndex = localAddress.lastIndexOf("web/");
            String subServlet = subServletIndex == -1 ? localAddress : localAddress.substring(subServletIndex+4);
            int actionIndex = subServlet.indexOf("/");
            if(actionIndex > -1)
            {
                arguments.put(BiosoftWebRequest.ACTION, subServlet.substring(actionIndex+1));
                subServlet = subServlet.substring(0, actionIndex);
            }

            if( subServlet.equals(LOGIN) )
            {
                if( authWebSession != null )
                {
                    loginFromSessionId( arguments, webSession, authWebSession );
                    try
                    {
                        sendInitialInfo( arguments, new JSONResponse(resp), arguments.get( "username" ) );
                    }
                    catch( Exception e )
                    {
                        try
                        {
                            new JSONResponse( resp ).error( new WebException( "EX_ACCESS_INVALID_LOGIN", e.getMessage() ) );
                        }
                        catch( IOException ioe )
                        {
                            log.log( Level.SEVERE, "Can not send login response", ioe );
                        }
                    }
                    return;
                } 

                if( arguments.containsKey( SecurityManager.SESSION_ID ) )
                {   
                    sessionId = arguments.get( SecurityManager.SESSION_ID );  
                    SecurityManager.addThreadToSessionRecord( Thread.currentThread(), sessionId );
                    sessionId = SecurityManager.getSession();
                }

                login(arguments, arguments.get("Remote-address"), new JSONResponse(resp));
                return;
            }

            if( arguments.containsKey( SecurityManager.SESSION_ID ) )
            {
                sessionId = arguments.get( SecurityManager.SESSION_ID );  
                SecurityManager.addThreadToSessionRecord( Thread.currentThread(), sessionId );
                sessionId = SecurityManager.getSession();
            }

            if( subServlet.equals(LOGOUT) )
            {
                logout(out, resp);
                return;
            }
            if( SecurityManager.isSessionDead(sessionId) ) //check if session is still valid
            {
                invalidSessionResponse(new JSONResponse(resp));
                return;
            }
            if( subServlet.equals(PING) )
            {
                processPingRequest(new JSONResponse(resp));
                return;
            }
            webSession.updateActivity(arguments);

            WebProvider provider = WebProviderFactory.getProvider(subServlet);
            if(provider != null)
            {
                try
                {
                    BiosoftWebRequest request = new BiosoftWebRequest( arguments );
                    Object item = params.containsKey( "file" ) ? Array.get( params.get( "file" ), 0 ) : null;
                    request.setFileItem( item );
                    provider.process( request, resp );
                    return;
                }
                catch(Throwable e)
                {
                    try
                    {
                        JSONResponse response = new JSONResponse(resp);
                        arguments.remove("sessionId");
                        String exceptionInfo = "\n\tParameters: (" + localAddress + " " + arguments + ")" + "\n\tSession: " + webSession
                                + ( e.getStackTrace() != null && e.getStackTrace().length > 0 ? "\n\tCode: " + e.getStackTrace()[0] : "" );
                        if(e instanceof LoggedException)
                        {
                            ( (LoggedException)e ).log();
                            log.log(Level.SEVERE, e.getMessage() + exceptionInfo);
                        }
                        else if(e instanceof IllegalArgumentException)
                            log.log(Level.SEVERE, "Illegal argument: " + e.getMessage() + exceptionInfo);
                        else if(e instanceof SecurityException)
                            log.log(Level.SEVERE, "Security exception: " + e.getMessage() + exceptionInfo);
                        else if(e instanceof WebException)
                        {
                            log.log(Level.SEVERE, "WebException: " + e.getMessage() + exceptionInfo);
                            if(e.getCause() != null)
                            {
                                if(e.getCause() instanceof LoggedException)
                                {
                                    ( (LoggedException)e.getCause() ).log();
                                    log.log(Level.SEVERE, "Reason: "+e.getCause().getMessage());
                                }
                                log.log(Level.SEVERE, "Caused by", e.getCause());
                            }
                        }
                        else
                            log.log(Level.SEVERE, "While processing request "+localAddress+" "+arguments, e);
                        response.error(e);
                        return;
                    }
                    catch( IOException e1 )
                    {
                        log.log(Level.SEVERE, "Unable to send response", e1);
                    }
                }
            }

            if( subServlet.equals(UPLOAD) )
            {
                if( "getName".equals( arguments.get( "type" ) ) )
                {
                    String urlSpec = arguments.get( "url" );
                    try
                    {
                        URL url = new URL( urlSpec );
                        RemoteFileInfo fileInfo = FileDownloader.getFileInfoFromHeader( url );
                        JSONObject res = new JSONObject();
                        res.put( "fileName", fileInfo.getFileName() );
                        new JSONResponse( resp ).sendJSON( res );
                    }
                    catch( IOException e )
                    {
                        try
                        {
                            new JSONResponse( resp ).error( e );
                        }
                        catch( IOException e1 )
                        {
                            log.log(Level.SEVERE, "Unable to send response", e1);
                        }
                    }
                    return;
                }
                else
                {
                    processUpload( params );
                    resp.setContentType( "text/plain" );
                    return;
                }
            }
            try
            {
                new JSONResponse(resp).error("Service not found: "+subServlet);
            }
            catch( IOException e )
            {
                log.log(Level.SEVERE, "Unable to send response", e);
            }
        }
        finally
        {
            SecurityManager.removeThreadFromSessionRecord();
        }
    }

    private void processPingRequest(JSONResponse response)
    {
        SecurityManager.getCurrentUserPermission(); // just to update expiration time
        try
        {

            long startTime = System.currentTimeMillis();
            Set<ru.biosoft.access.core.DataElementPath> refreshPaths = Collections.emptySet();
            while(System.currentTimeMillis() - startTime < 30000)
            {
                refreshPaths = WebSession.getCurrentSession().popRefreshPaths();
                if(!refreshPaths.isEmpty())
                    break;
                try
                {
                    Thread.sleep( 1000 );
                }
                catch( Exception e )
                {
                }
            }

            JSONObject result = new JSONObject();
            result.put( "refresh", JSONUtils.toSimpleJSONArray( refreshPaths ) );
            response.sendJSON( result );
        }
        catch(BiosoftInvalidSessionException e)
        {
            //Client is disconnected before we dispatched ping response
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not send ping response: "+ExceptionRegistry.log(e));
        }
    }

    /**
     * Login
     * Returns default perspective and perspectives list to spare extra query
     */
    protected void login(Map<String, String> arguments, String remoteAddress, JSONResponse response)
    {
        String username = arguments.get("username");
        String password = arguments.get("password");
        WebSession session = WebSession.getCurrentSession();
        try
        {
            try
            {
                UserPermissions cup = SecurityManager.getCurrentUserPermission();
                if( ( ( username == null && password == null ) || ( username.isEmpty() && password.isEmpty() ) ) && cup != null
                        && !cup.isExpired() && !cup.isDead() )
                {
                    session.updateInCache();
                    sendInitialInfo( arguments, response, cup.getUser() );
                    return;
                }
                if( username == null )
                    throw new WebException("EX_QUERY_PARAM_MISSING", "username");
                if( password == null )
                    throw new WebException("EX_QUERY_PARAM_MISSING", "password");
                if( username.isEmpty() && password.isEmpty()
                        && !Application.getPreferences().getValue("Global/DisableAnonymous", "").equals("true") )
                {
                    SecurityManager.anonymousLogin();
                    log.info( "Anonymous login (" + remoteAddress + ")" );
                    session.updateInCache();
                    sendInitialInfo( arguments, response );
                }
                else
                {
                    try
                    {
                        SecurityManager.commonLogin( username, password, remoteAddress, null );
                    }
                    catch( Exception e )
                    {
                        throw new WebException( "EX_ACCESS_INVALID_LOGIN", e.getMessage() );
                    }
                    log.info( "Successful login: " + username + " (" + remoteAddress + ")" );
                    if( username.contains( "$" ) )
                    { 
                        username = username.substring( username.indexOf( "$" ) + 1 );
                    }
                    session.putValue( WebSession.CURRENT_USER_NAME, username );
                    session.updateInCache();
                    sendInitialInfo( arguments, response );
                }
            }
            catch(WebException e)
            {
                response.error(e);
            }
            catch(Throwable t)
            {
                response.error(ExceptionRegistry.log(t));
            }
        }
        catch( IOException e )
        {
            log.log(Level.SEVERE, "Can not send login response", e);
        }
    }

    private static boolean loginFromSessionId(Map<String, String> arguments, WebSession webSession, WebSession authWebSession)
    {
        arguments.remove( SecurityManager.SESSION_ID );
        String username = authWebSession.getUserName();
        arguments.put( "username", username );
        String remoteAddress = arguments.getOrDefault( "Remote-address", "" );
        try
        {
            SecurityManager.commonLoginViaOtherSession( authWebSession.getSessionId(), remoteAddress, true );
        }
        catch( Exception e )
        {
            log.log( Level.WARNING, "Cannot login", e );
            return false;
        }
        log.warning( "Login via Session ID: " + username + " (" + remoteAddress + ")" );
        webSession.updateInCache();
        if( username.contains( "$" ) )
            username = username.substring( username.indexOf( "$" ) + 1 );
        webSession.putValue( WebSession.CURRENT_USER_NAME, username );
        return true;
    }

    private void sendInitialInfo(Map<String, String> arguments, JSONResponse response) throws Exception
    {
        sendInitialInfo( arguments, response, "" );
    }
    private void sendInitialInfo(Map<String, String> arguments, JSONResponse response, String user) throws Exception
    {
        WebSession.getCurrentSession().updateInCache();
        JSONObject result = new JSONObject();
        if( user != null && !user.isEmpty() )
            result.put( "username", user );

        Map<String, String> perspectiveArguments = new HashMap<>();
        perspectiveArguments.put("name", arguments.get("perspective"));
        result.put("perspective", getRequestValue("perspective", perspectiveArguments, "reading perspectives info"));

        JSONArray repositories = result.getJSONObject("perspective").getJSONObject("perspective").getJSONArray("repository");
        JSONObject roots = new JSONObject();
        Set<String> classes = new HashSet<>();
        for(int i=0; i<repositories.length(); i++)
        {
            String path = repositories.getJSONObject(i).getString("path");
            Map<String, String> accessArguments = new HashMap<>();
            accessArguments.put("service", "access.service");
            accessArguments.put("command", String.valueOf(AccessProtocol.DB_FLAGGED_LIST));
            accessArguments.put(Connection.KEY_DC, path);
            String val = (String)getRequestValue("data", accessArguments, "reading repository "+path);
            JSONObject dir = new JSONObject(val);
            JSONArray classesArray = dir.optJSONArray("classes");
            if(classesArray != null)
            {
                for(int j=0; j<classesArray.length(); j++)
                    classes.add(classesArray.getString(j));
            }
            roots.put(path, dir);
        }
        result.put("roots", roots);

        Map<String, String> preferencesArguments = new HashMap<>();
        preferencesArguments.put(BiosoftWebRequest.ACTION, "init");
        result.put("preferences", getRequestValue("preferences", preferencesArguments, "reading preferences"));

        Map<String, String> contextArguments = new HashMap<>();
        contextArguments.put(BiosoftWebRequest.ACTION, "context");
        result.put("context", getRequestValue("script", contextArguments, "reading script context"));

        Map<String, String> scriptTypesArguments = new HashMap<>();
        scriptTypesArguments.put(BiosoftWebRequest.ACTION, "types");
        result.put("scriptTypes", getRequestValue("script", scriptTypesArguments, "reading script types"));

        Map<String, String> classesArguments = new HashMap<>();
        classesArguments.put("service", "access.service");
        classesArguments.put("command", String.valueOf(AccessProtocol.DB_GET_CLASS_HIERARCHY));
        classesArguments.put(AccessProtocol.ADD_COMMON_CLASSES, "yes");
        classesArguments.put(AccessProtocol.CLASS_NAME, String.join(",", classes));
        result.put("classes", getRequestValue("data", classesArguments, "reading common classes"));

        Map<String, String> actionArguments = new HashMap<>();
        actionArguments.put("type", "toolbar");
        JSONObject actions = new JSONObject();
        actions.put("toolbar", getRequestValue("action", actionArguments, "reading toolbar actions"));
        actionArguments.put("type", "tree");
        actions.put("tree", getRequestValue("action", actionArguments, "reading tree actions"));
        actionArguments.put("type", "dynamic");
        actionArguments.put(BiosoftWebRequest.ACTION, "load");
        actions.put("dynamic", getRequestValue("action", actionArguments, "reading dynamic actions"));
        result.put("actions", actions);

        Map<String, String> journalArguments = new HashMap<>();
        journalArguments.put(BiosoftWebRequest.ACTION, "init");
        result.put("journals", getRequestValue("journal", journalArguments, "reading projects list"));

        result.put( "experimental", !SecurityManager.isExperimentalFeatureHidden() );

        response.sendJSON(result);
    }

    private Object getRequestValue(String provider, Map<String, String> arguments, String infoName) throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        arguments.put(SecurityManager.SESSION_ID, WebSession.getCurrentSession().getSessionId());
        WebProvider wProvider = WebProviderFactory.getProvider( provider );
        if( wProvider == null )
        {
            log.log( Level.SEVERE, "Unable to get provider '" + provider + "', arguments = \n" + arguments );
        }
        wProvider.process( new BiosoftWebRequest(arguments), new BiosoftWebResponse(null, out) );
        String str = out.toString("UTF-8");
        JSONObject result = new JSONObject(str);
        int type = result.getInt(JSONResponse.ATTR_TYPE);
        if(type == JSONResponse.TYPE_ERROR)
        {
            throw new WebException("EX_INTERNAL_CUSTOM", infoName, result.getString(JSONResponse.ATTR_MESSAGE));
        }
        return result.get(JSONResponse.ATTR_VALUES);
    }

    protected void logout(OutputStream out, BiosoftWebResponse resp)
    {
        try
        {
            String sessionUser = SecurityManager.getSessionUser();
            log.warning(sessionUser == null ? "Anonymous logout" : "Logout: "+sessionUser);
            JSONResponse response = new JSONResponse(resp);
            WebSession currentSession = WebSession.getCurrentSession();
            SecurityManager.commonLogout();
            currentSession.putValue(WebSession.CURRENT_USER_NAME, "");
            currentSession.invalidate();
            resp.clearSession();
            response.send(new byte[0], 0);
        }
        catch( IOException e )
        {
            log.log(Level.SEVERE, "Can not send logout", e);
        }
    }

    @Override
    public void uploadListener(Map<Object, Object> arguments, Object sessionObj, Long read, Long total)
    {
        WebSession.getSession(sessionObj);
        if( !arguments.containsKey( "fileID" ) )
            return;
        String fileID = ( (String[])arguments.get("fileID") )[0].replaceAll("\\W", "");
        WebJob webJob = WebJob.getWebJob(fileID);
        JobControl job = webJob.getJobControl();
        if( job == null || job.getStatus() != JobControl.RUNNING )
            job = webJob.createJobControl();
        if( total != -1 )
            job.setPreparedness((int) ( ( (float)read ) / total * 100 ));
    }

    /**
     * Stores uploaded file into temp directory
     * @param arguments HTTP request arguments passed from client
     * @param session CUrrent web session
     * TODO delete older not used files automatically
     */
    protected void processUpload(Map<Object, Object> params)
    {
        Map<String, String> arguments = convertParams(params);
        String fileID = arguments.get("fileID").replaceAll("\\W", "");
        WebJob webJob = WebJob.getWebJob(fileID);
        JobControl job = webJob.getJobControl();
        if( job == null || job.getStatus() != JobControl.RUNNING || ! ( job instanceof FunctionJobControl ) )
            job = webJob.createJobControl();

        if( arguments.containsKey("fileUrl") )
        {
            try
            {
                processFTPUpload(arguments);
                return;
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not upload file: fileID=" + fileID, e);
                if( job != null )
                    ( (FunctionJobControl)job ).functionTerminatedByError(new Exception("Can not upload file: incorrect URL"));
                return;
            }
        }

        try
        {
            String sessionID = WebSession.getCurrentSession().getSessionId();
            String fileName = null;
            Object item = params.containsKey("file")?Array.get(params.get("file"),0):null;
            String fileContent = arguments.get("fileContent");
            String filePath = arguments.get("filePath");
            // FileItem object was loaded by catalina class loader; thus cannot be cast to FileItem object created by OSGi
            if( item != null && item.getClass().getSimpleName().contains("FileItem") )
            {
                fileName = (String)item.getClass().getMethod("getName", new Class<?>[] {}).invoke(item, new Object[] {});
            }
            else if( fileContent != null ) //Load file by content
            {
                fileName = arguments.get("fileName");
            }
            else if( filePath != null ) //Load file from FileDataElement
            {
                DataElementPath path = DataElementPath.create(filePath);
                if( path.exists() && path.optDataElement() instanceof FileDataElement )
                {
                    fileName = path.getName();
                }
            }

            if( fileName != null )
            {
                String suffix = getFileSuffixByName(fileName);
                File destinationFile = new File(UPLOAD_DIRECTORY, "upload_" + sessionID + "_" + fileID + suffix);

                WebSession.getCurrentSession().putValue( "uploadedFile_" + fileID, destinationFile.getName() );
                WebSession.getCurrentSession().putValue( "uploadedFileSuffix_" + fileID, suffix );

                if( item != null && item.getClass().getSimpleName().contains("FileItem") )
                {
                    item.getClass().getMethod("write", new Class<?>[] {File.class}).invoke(item, new Object[] {destinationFile});
                }
                else if( fileContent != null ) //Load file by content
                {
                    ApplicationUtils.writeString(destinationFile, fileContent);
                }
                else if( filePath != null ) //Load file from FileDataElement
                {
                    DataElementPath path = DataElementPath.create(filePath);
                    File orig = path.getDataElement(FileDataElement.class).getFile();
                    ApplicationUtils.linkOrCopyFile(destinationFile, orig, job);
                }
                WebSession.getCurrentSession().putValue("uploadedFile_" + fileID, fileName);
                WebSession.getCurrentSession().putValue("uploadedFileSuffix_" + fileID, suffix);
            }
            else
            {
                log.log(Level.SEVERE, "Upload of " + fileID + " aborted");
                if( job != null )
                    ( (FunctionJobControl)job ).terminate();
                //.functionTerminatedByError(new InterruptedException("File upload aborted"));
                return;
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not write uploaded file: fileID=" + fileID, e);
            if( job != null )
                ( (FunctionJobControl)job ).functionTerminatedByError(new Exception("Can not write uploaded file"));
            return;
        }
        if( job != null )
        {
            job.setPreparedness(100);
            ( (FunctionJobControl)job ).functionFinished();
        }
        return;
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

    private void processFTPUpload(Map<String, String> arguments)
    {
        String fileID = arguments.get("fileID").replaceAll("\\W", "");
        WebJob webJob = WebJob.getWebJob(fileID);
        JobControl job = webJob.getJobControl();
        if( job == null || job.getStatus() != JobControl.RUNNING || ! ( job instanceof FunctionJobControl ) )
            job = webJob.createJobControl();

        if( job != null )
        {
            ( (FunctionJobControl)job ).functionStarted();
        }
        String sessionID = WebSession.getCurrentSession().getSessionId();
        URL url = null;
        try
        {
            url = FileDownloader.convertURL(new URL(arguments.get("fileUrl")), null);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not upload file: fileID=" + fileID, e);
            if( job != null )
                ( (FunctionJobControl)job ).functionTerminatedByError(new Exception("Can not upload file: "+e.getMessage()));
            return;
        }
        File file = new File(url.getFile());
        String fileName = file.getName();
        String suffix = getFileSuffixByName(fileName);
        File destinationFile = new File(UPLOAD_DIRECTORY, "upload_" + sessionID + "_" + fileID + suffix);

        try
        {
            WebSession.getCurrentSession().putValue( "uploadedFile_" + fileID, destinationFile.getName() );
            WebSession.getCurrentSession().putValue( "uploadedFileSuffix_" + fileID, suffix );

            String newFileName = FileDownloader.downloadFile(url, destinationFile, job);
            if(!newFileName.equals(fileName))
            {
                String newSuffix = getFileSuffixByName(newFileName);
                if(!newSuffix.equals(suffix))
                {
                    destinationFile.renameTo(new File(UPLOAD_DIRECTORY, "upload_" + sessionID + "_" + fileID + newSuffix));
                    suffix = newSuffix;
                }
                fileName = newFileName;
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not upload file from FTP: fileID=" + fileID, e.getCause());
            if( job != null )
                ( (FunctionJobControl)job ).functionTerminatedByError(e);
            return;
        }

        WebSession.getCurrentSession().putValue("uploadedFile_" + fileID, fileName);
        WebSession.getCurrentSession().putValue("uploadedFileSuffix_" + fileID, suffix);
        if( job != null )
        {
            job.setPreparedness(100);
            ( (FunctionJobControl)job ).functionFinished();
        }
        return;
    }

    /**
     * Get previously uploaded file by its fileID
     * @param fileID - ID of requested file (was supplied during upload)
     * @return File if success, null if file cannot be found/read
     */
    public static FileItem getUploadedFile(String fileID)
    {
        if(fileID == null)
            return null;
        fileID = fileID.replaceAll("\\W", "");
        String suffix = TextUtil2.nullToEmpty( (String)WebSession.getCurrentSession().getValue("uploadedFileSuffix_" + fileID) );
        String sessionID = WebSession.getCurrentSession().getSessionId();
        try
        {
            FileItem f = new FileItem(UPLOAD_DIRECTORY, "upload_" + sessionID + "_" + fileID + suffix);
            if( f.isFile() && f.canRead() )
            {
                f.setOriginalName((String)WebSession.getCurrentSession().getValue("uploadedFile_" + fileID));
                return f;
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not access previously uploaded file: fileID=" + fileID, e);
        }
        return null;
    }

    public static SessionCache getSessionCache()
    {
        return SessionCacheManager.getSessionCache(WebSession.getCurrentSession().getSessionId());
    }

    /**
     * Error response
     */
    protected void invalidSessionResponse(JSONResponse response)
    {
        try
        {
            response.sendInvalidResponse("Your session is no longer valid. Please, login again.");
        }
        catch( IOException e )
        {
            log.log(Level.SEVERE, "Can not write a result", e);
        }
    }
}
