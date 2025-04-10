package ru.biosoft.server.servlets.support;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Module;
import biouml.plugins.research.ResearchBuilder;
import biouml.plugins.research.ResearchTitleIndex;
import biouml.plugins.server.access.AccessProtocol;
import biouml.plugins.server.access.AccessService;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.Repository;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.Index;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.access.core.filter.FilteredDataCollection;
import ru.biosoft.access.file.FileDataCollection;
import ru.biosoft.access.generic.GenericDataCollection;
import ru.biosoft.access.history.HistoryFacade;
import ru.biosoft.access.security.GlobalDatabaseManager;
import ru.biosoft.access.security.Permission;
import ru.biosoft.access.security.PrivilegedAction;
import ru.biosoft.access.security.ProtectedDataCollection;
import ru.biosoft.access.security.SecurityAdminUtils;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.UserPermissions;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.server.AbstractJSONServlet;
import ru.biosoft.server.servlets.webservices.WebSession;
import ru.biosoft.server.servlets.webservices.WebSession.SessionInfo;
import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.tasks.TaskManager;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.Pair;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.TextUtil2;

/**
 * BioUML server support functions.
 * Is used by bioumlsupport application.
 */
public class SupportServlet extends AbstractJSONServlet
{
    private static final String CLIENT_PROPERTY_PREFIX = "clientProperty.";

    protected static final Logger log = Logger.getLogger(SupportServlet.class.getName());

    static final String lineSeparator = System.getProperty("line.separator");

    //
    // Servlet request keys
    //
    public static final String CREATE_PROJECT = "createProject";
    public static final String GET_PROJECTS_DATA = "getProjectsData";
    public static final String SET_QUOTA = "setQuota";
    public static final String DELETE_PROJECT = "deleteProject";
    public static final String DATABASE_INFO = "dbInfo";
    public static final String KILL_TASK = "killTasks";
    public static final String GET_TASKS = "getTasks";
    public static final String GET_TASK_HISTORY = "getTaskHistory";
    public static final String UPDATE_PERMISSIONS = "updatePermissions";
    public static final String USER_INFO = "userInfo";
    public static final String CHANGE_PASSWD = "changePasswd";
    public static final String USER_LIST = "userList";
    public static final String CHANGE_INFO = "changeInfo";
    public static final String INFO_DICT = "infoDictionary";
    public static final String CREATE_PROJECT_WITH_PERMISSION = "createProjectWithPermission";
    public static final String LIST_INTERRUPTED_TASKS = "listInterruptedTasks";
    public static final String RESTART_INTERRUPTED_TASKS = "restartInterruptedTasks";
    public static final String STOP_INTERRUPTED_TASKS = "stopInterruptedTasks";
    public static final String GET_PROJECT_SIZE = "getProjectSize";
    public static final String SET_PROJECT_TITLE = "setProjectTitle";
    public static final String GET_ALL_PROJECTS_SIZE = "getAllProjectsSize";


    @Override
    public String service(String localAddress, Object session, Map params, OutputStream out, Map<String, String> header)
    {
        try
        {
            WebSession webSession = WebSession.getSession(session);
            String sessionId = webSession.getSessionId();
            SecurityManager.addThreadToSessionRecord(Thread.currentThread(), sessionId);

            JSONObject result = null;
            try
            {
                if( localAddress.endsWith(CREATE_PROJECT) )
                {
                    result = createProject(params);
                }
                else if( localAddress.endsWith(DELETE_PROJECT) )
                {
                    result = deleteProject(params);
                }
                else if( localAddress.endsWith(GET_PROJECTS_DATA) )
                {
                    result = getProjectsData(params);
                }
                else if( localAddress.endsWith(SET_QUOTA) )
                {
                    result = setQuota(params);
                }
                else if( localAddress.endsWith(DATABASE_INFO) )
                {
                    result = getDatabaseInfo(params);
                }
                else if( localAddress.endsWith(KILL_TASK) )
                {
                    result = killTask(params);
                }
                else if( localAddress.endsWith(GET_TASKS) )
                {
                    result = getTasks(params);
                }
                else if( localAddress.endsWith(GET_TASK_HISTORY) )
                {
                    result = getTaskHistory(params);
                }
                else if( localAddress.endsWith(USER_LIST) )
                {
                    result = getActiveUsers(params);
                }
                else if( localAddress.endsWith(UPDATE_PERMISSIONS) )
                {
                    result = updatePermissions(params);
                }
                else if( localAddress.endsWith(USER_INFO) )
                {
                    result = getUserInfo(params);
                }
                else if( localAddress.endsWith(CHANGE_PASSWD) )
                {
                    result = changePassword(params);
                }
                else if( localAddress.endsWith(CHANGE_INFO) )
                {
                    result = changeInfo(params);
                }
                else if( localAddress.endsWith(INFO_DICT) )
                {
                    result = getUserInfoDictionaries(params);
                }
                else if( localAddress.endsWith(CREATE_PROJECT_WITH_PERMISSION) )
                {
                    result = createProjectWithPermissions(params);
                }
                else if( localAddress.endsWith( LIST_INTERRUPTED_TASKS ) )
                {
                    result = listInterruptedTasks(params);
                }
                else if( localAddress.endsWith( RESTART_INTERRUPTED_TASKS ) )
                {
                    result = restartInterruptedTasks(params);
                }
                else if( localAddress.endsWith( STOP_INTERRUPTED_TASKS ) )
                {
                    result = stopInterruptedTasks( params );
                }
                else if( localAddress.endsWith( GET_PROJECT_SIZE ) )
                {
                    result = getProjectSize( params );
                }
                else if( localAddress.endsWith( SET_PROJECT_TITLE ) )
                {
                    result = setProjectTitle( params );
                }
                else if( localAddress.endsWith( GET_ALL_PROJECTS_SIZE ) )
                {
                    result = getAllProjectsSize( params );
                }
                else
                {
                    result = errorResponse("unknown request command");
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, localAddress, e);
                result = errorResponse(e.getMessage());
            }

            OutputStreamWriter ow = new OutputStreamWriter(out, "UTF8");
            result.write(ow);
            ow.flush();
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Support servlet exception", e);
        }
        return "text/html";
    }


    /**
     * @param params
     * @return
     * @throws Exception
     */
    private JSONObject setQuota(Map params) throws Exception
    {
        String projectName = getStrictParameter(params, "project");
        DataElementPath path = CollectionFactoryUtils.getUserProjectsPath().getChildPath(projectName);
        if(!SecurityManager.getPermissions(path).isAdminAllowed())
        {
            throw new SecurityException("Administrative access to "+projectName+" is required!");
        }
        DataCollection<?> project = path.optDataCollection();
        if(project == null)
        {
            throw new IllegalArgumentException("Project "+projectName+" not found!");
        }
        long newQuota = -1;
        try
        {
            newQuota = Long.parseLong(getStrictParameter(params, "quota"));
        }
        catch( Exception e )
        {
        }
        if( newQuota < 0 )
        { 
            throw new IllegalArgumentException("Illegal quota value");
        }
        if( newQuota == 0 )
        {
            project.getInfo().getProperties().remove(DataCollectionConfigConstants.DISK_QUOTA_PROPERTY);
        }
        else
        {
            project.getInfo().getProperties().setProperty(DataCollectionConfigConstants.DISK_QUOTA_PROPERTY, String.valueOf(newQuota));
        } 
        CollectionFactoryUtils.save(project);
        return simpleOkResponse();
    }

    /**
     * @param params
     * @return
     * @throws Exception
     */
    private JSONObject getProjectsData(Map params) throws Exception
    {
        JSONArray result = new JSONArray();
        for(DataElement de: CollectionFactoryUtils.getUserProjectsPath().getDataCollection())
        {
            if(de instanceof Module)
            {
                Module module = (Module)de;
                Permission permissions = SecurityManager.getPermissions( module.getCompletePath() );
                JSONObject moduleJSON = new JSONObject();
                moduleJSON.put("name", module.getName());
                moduleJSON.put("description", module.getDescriptionHTML());
                moduleJSON.put( "admin", permissions.isAdminAllowed() );
                moduleJSON.put( "canWrite", permissions.isWriteAllowed() );
                moduleJSON.put( "canDelete", permissions.isDeleteAllowed() );
                String quotaStr = module.getInfo().getProperty(DataCollectionConfigConstants.DISK_QUOTA_PROPERTY);
                if(quotaStr != null)
                {
                    try
                    {
                        moduleJSON.put("quota", Long.parseLong(quotaStr));
                    }
                    catch( Exception e )
                    {
                    }
                }
                
                Properties properties = module.getInfo().getProperties();
                properties.forEach( (key,value) -> {
                    String propName = key.toString();
                    if(propName.startsWith( CLIENT_PROPERTY_PREFIX ))
                    {
                        moduleJSON.put( propName.substring( CLIENT_PROPERTY_PREFIX.length() ), value );
                    }
                });
                result.put(moduleJSON);
            }
        }
        return arrayOkResponse(result);
    }

    /**
     * Create project request
     */
    protected JSONObject createProject(Map params) throws Exception
    {
        String projectName = getStrictParameter(params, "project");

        log.log( Level.INFO, "createProject.login(" + projectName + "): START" );
        long before = System.currentTimeMillis();

        login(params);

        long after = System.currentTimeMillis();
        log.log( Level.INFO, "createProject.login(" + projectName + "): END " + ( after - before ) + "ms" );

        if( !isProjectNameValid( projectName ) )
        {
            return errorResponse( "Project name contains unacceptable characters. Only latin letters, numbers, spaces and underscores ( _ ) are allowed." );
        }

        log.log( Level.INFO, "createProject.getDataCollection(" + projectName + "): START" );
        before = System.currentTimeMillis();

        DataCollection parentDC = CollectionFactoryUtils.getUserProjectsPath().getDataCollection();

        after = System.currentTimeMillis();
        log.log( Level.INFO, "createProject.getDataCollection(" + projectName + "): END " + ( after - before ) + "ms" );

        if( parentDC.contains(projectName) )
        {   
            return errorResponse("project already exists, try another name");
        } 

        log.log( Level.INFO, "createProject.getPermissions(" + projectName + "): START" );
        before = System.currentTimeMillis();
        DataElementPath projectPath = CollectionFactoryUtils.getUserProjectsPath().getChildPath(projectName);
        Permission permission = SecurityManager.getPermissions(projectPath);
        after = System.currentTimeMillis();
        log.log( Level.INFO, "createProject.getPermissions(" + projectName + "): END " + ( after - before ) + "ms" );

        log.log( Level.INFO, "createProject.checkPermission(" + projectName + "): START" );
        before = System.currentTimeMillis();
        try
        {
            checkPermission(permission, "put");
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "checkPermission failed for getUserProjectsPath() = '" + CollectionFactoryUtils.getUserProjectsPath().getName() + 
               "', projectName = " + projectName);
            throw ex;
        }    

        SecurityManager.invalidatePermissions(projectPath.toString());

        after = System.currentTimeMillis();
        log.log( Level.INFO, "createProject.checkPermission(" + projectName + "): END " + ( after - before ) + "ms" );

        log.log( Level.INFO, "createProject.createNewProject(" + projectName + "): START" );
        before = System.currentTimeMillis();

        ArrayList<String> errors = new ArrayList<>();
        if( createNewProject( projectName, null, false, errors ) == null )
        {   
            return errorResponse("cannot create research project" + ( errors.size() > 0 ? ": " + errors.get( 0 ) : "" ) );
        }   

        after = System.currentTimeMillis();
        log.log( Level.INFO, "createProject.createNewProject(" + projectName + "): END " + ( after - before ) + "ms" );

        //SecurityManager.invalidatePermissions();

        return simpleOkResponse();
    }

    /**
     * Create project with group permissions request
     */
    protected JSONObject createProjectWithPermissions(Map params) throws Exception
    {
        login(params);
        String projectName = getStrictParameter(params, "project");
        if( !isProjectNameValid( projectName ) )
        {
            return errorResponse( "Project name contains unacceptable characters. Only latin letters, numbers, spaces and underscores ( _ ) are allowed." );
        }
        String user = SecurityManager.getSessionUser();
        StringWriter sw = new StringWriter();
        DataCollection<?> userProjectsParent = CollectionFactoryUtils.getUserProjectsPath().getDataCollection();
        DataCollection<?> primaryParent = null;
        if( userProjectsParent instanceof FilteredDataCollection )
            primaryParent = ( (FilteredDataCollection<?>)userProjectsParent ).getPrimaryCollection();
        if( userProjectsParent.contains(projectName) || ( primaryParent != null && primaryParent.contains(projectName) ) )
            return errorResponse( "Project already exists, please try another name." );
        // If group cannot be created, we still try to create the project (probably it will be successful if
        // group already exists and access was granted previously
        DataElementPath projectPath = userProjectsParent.getCompletePath().getChildPath( projectName );
        boolean groupError = !SecurityAdminUtils.createUserGroup(user, projectName, false, sw)
                || !SecurityAdminUtils.addGroupPermission(user, projectName,
                        projectPath, 15, sw );
        if( groupError )
        {
            String biostoreMsg = sw.toString();
            log.log( Level.SEVERE, biostoreMsg );
            JSONObject resp = handleGroupCreateError( biostoreMsg );
            if(resp != null)
                return resp;
            if( biostoreMsg != null && !"".equals( biostoreMsg ) )
            {
                return errorResponse( "Error: " + biostoreMsg + ", please try another project name." ); 
            } 
            return errorResponse( "Unknown error while creating user's group, please try another project name. If this doesn't help, please contact the support." ); 
        }
        
        login(params);
        SecurityManager.invalidatePermissions( projectPath.toString() );

        //create project
        
        String projectTypeStr = getStringParameter( params, "projectType" );
        ProjectType projectType = ProjectType.SQL;
        if(projectTypeStr != null)
            projectType = ProjectType.valueOf( projectTypeStr );
        
        ArrayList<String> errors = new ArrayList<>();
        
        DataCollection<?> project = createNewProject( projectName, user, true, errors, projectType );
        if( project == null )
        {
            return handleProjectCreateError( errors.size() > 0 ? errors.get( 0 ) : "reason unknown" );
        } 
        String description = getStringParameter(params, "description");
        if( description != null && !description.isEmpty() )
        {
            if( project instanceof Module )
                ( (Module)project ).setDescription(description);
            else
                project.getInfo().setDescription(description);
        }
        return simpleOkResponse();
    }
    
    protected JSONObject handleGroupCreateError(String biostoreMsg) throws Exception
    {
        if( biostoreMsg == null )
        {
            return null;
        }  
        Pattern pat = Pattern.compile( "Maximal number of groups \\([0-9]+\\) is reached for user ([^\\s]+)" );
        Matcher matcher = pat.matcher( biostoreMsg );
        if( matcher.find() )
        {
            String user = matcher.group( 1 );
            return errorResponse( "Project cannot be created: the limit of maximum number of projects available to the user " + user
                    + " was reached.<br><br>" + "You can contact $1 for more info and further assistance." );
        }
        if( biostoreMsg.indexOf( "While creating group " ) >= 0 && biostoreMsg.indexOf( "already exists." ) >= 0 )
        {
            //String msg = TextUtil.subst( biostoreMsg, "While creating group ", "While creating project " ); 
            //msg = TextUtil.subst( msg, "already exists.", "already exists, please try another project name." );
            return errorResponse( "The project name you have specified is already occupied. Please select another name for your project." );
        }
        return null;
    }
    
    protected JSONObject handleProjectCreateError( String errorMessage ) throws Exception
    {
        return errorResponse( "Project cannot be created (" + errorMessage + "). Please, contact your administrator at $1" );
    }

    /**
     * Delete existing project
     */
    protected JSONObject deleteProject(Map params) throws Exception
    {
        log.info( "Delete project request = " + params ); 
        String userName = getStrictParameter(params, "user");
        if(userName.isEmpty())
            throw new SecurityException("Access denied");
        String projectName = getStrictParameter(params, "project");
        JSONArray projectNames = new JSONArray();
        projectNames.put( projectName );
        params.put( "projects", new String[] {projectNames.toString()} );
        return deleteProjects( params );
    }

    /**
     * Delete project
     * @param projectName
     * @return error message or null if complete successfully
     */
    public static String deleteProject(final String projectName) throws Exception
    {
        final DataCollection parentDC = CollectionFactoryUtils.getUserProjectsPath().getDataCollection();
        if( parentDC.contains(projectName) )
        {
            Permission permission = SecurityManager.getPermissions(CollectionFactoryUtils.getUserProjectsPath().getChildPath(projectName));
            //checkPermission(permission, "put");
            if(!permission.isDeleteAllowed())
                throw new SecurityException("Delete access denied for "+SecurityManager.getSessionUser());
            try
            {
                SecurityManager.runPrivileged(new PrivilegedAction()
                {
                    @Override
                    public Object run() throws Exception
                    {
                        DataCollection research = (DataCollection)parentDC.get(projectName);
                        DataCollection dataCollection = (DataCollection)research.get(Module.DATA);
                        research = DataCollectionUtils.fetchPrimaryCollectionPrivileged(research);
                        String localPath = research.getInfo().getProperty(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY);
                        String dbName = null;
                        if ( dataCollection != null )
                        {
                            dataCollection = DataCollectionUtils.fetchPrimaryCollectionPrivileged(dataCollection);
                            String jdbcUrl = dataCollection.getInfo().getProperty(SqlDataCollection.JDBC_URL_PROPERTY);

                            dbName = jdbcUrl;
                            int ind = jdbcUrl.lastIndexOf('/');
                            if ( ind != -1 )
                            {
                                dbName = jdbcUrl.substring(ind + 1);
                            }
                            ind = dbName.lastIndexOf('?');
                            if ( ind != -1 )
                            {
                                dbName = dbName.substring(0, ind);
                            }

                            try
                            {
                                //make a backup before removing
                                String backupAddr = Application.getGlobalValue("BackupFolder");
                                if ( !backupAddr.equals("BackupFolder") )
                                {
                                    File backupFolder = new File(backupAddr);
                                    if ( !backupFolder.exists() )
                                    {
                                        backupFolder.mkdirs();
                                    }
                                    File src = new File(localPath);
                                    String date = new SimpleDateFormat("yyyy.MM.dd_HH:mm:ss").format(new Date());
                                    File dst = new File(backupFolder, date + "_" + src.getName());

                                    if ( "Linux".equals(System.getProperty("os.name")) )
                                    {
                                        log.info("Moving deleted project '" + localPath + "' to backup folder '" + dst.getAbsolutePath() + "'...");
                                        ProcessBuilder processBuilder = new ProcessBuilder();
                                        processBuilder.command("mv", localPath, dst.getAbsolutePath());
                                        Process proc = processBuilder.start();
                                        proc.waitFor();
                                    }
                                    else
                                    {
                                        ApplicationUtils.copyFolder(dst, src);
                                    }

                                    File dumpPath = new File(dst, "_dump.sql");
                                    List<String> cmd = new ArrayList<>();
                                    cmd.add("mysqldump");
                                    String user = Application.getGlobalValue("BackupUser", "root");
                                    String password = Application.getGlobalValue("BackupPassword", "root");
                                    cmd.add("-u" + user);
                                    cmd.add("-p" + password);
                                    cmd.add(dbName);
                                    Runtime rt = Runtime.getRuntime();
                                    String[] cmdArray = cmd.toArray(new String[cmd.size()]);
                                    Process proc = rt.exec(cmdArray);
                                    ApplicationUtils.copyStream(new FileOutputStream(dumpPath), proc.getInputStream());
                                }
                            }
                            catch (Exception e)
                            {
                                log.log(Level.SEVERE, "cannot create backup copy", e);
                            }
                        }
                        parentDC.remove(projectName);
                        //remove all files manually
                        if( localPath != null )
                        {
                            File file = new File(localPath);
                            ApplicationUtils.removeDir(file);
                        }

                        if ( dbName != null )
                        {
                            Connection rootConnection = GlobalDatabaseManager.getDatabaseConnection();
                            SqlUtil.execute(rootConnection, "DROP DATABASE " + dbName);
                        }
                        return null;
                    }
                });
                return null;
            }
            catch( Exception e )
            {
                log.log( Level.SEVERE, "Cannot delete project " + projectName, e );
                return "Cannot delete project " + projectName;
            }
        }
        return "Project " + projectName + " doesn't exists";
    }

    /**
     * Get database description and statistics info
     */
    protected JSONObject getDatabaseInfo(Map params) throws Exception
    {
        login(params);
        String databaseName = getStrictParameter(params, "db");
        DataElementPath databasePath = DataElementPath.create(databaseName);
        Permission permission = SecurityManager.getPermissions(databasePath);
        checkPermission(permission, "getInfo");
        DataCollection dc = databasePath.getDataCollection();
        JSONObject result = new JSONObject();
        result.put("name", dc.getName());
        String version = dc.getInfo().getProperty(AccessProtocol.DB_VERSION);
        if( version != null )
        {
            result.put("version", version);
        }
        String update = dc.getInfo().getProperty(AccessProtocol.UPDATE);
        if( update != null )
        {
            result.put("update", update);
        }

        String protectionStatus = AccessService.getProtectionStatus(dc.getInfo().getProperty(
                ProtectedDataCollection.PROTECTION_STATUS));
        result.put("protection", protectionStatus);

        String dependency = dc.getInfo().getProperty(AccessProtocol.DEPENDENCIES);
        if( dependency != null )
        {
            result.put("dependency", dependency);
        }

        String descr = dc.getInfo().getDescription();
        if( descr != null )
        {
            result.put("description", descr);
        }

        String showStatistics = dc.getInfo().getProperty(AccessProtocol.SHOW_STATISTICS);
        if( showStatistics != null && Boolean.parseBoolean(showStatistics) )
        {
            StringBuffer statistics = new StringBuffer();
            AccessService.printStatistics(statistics, dc, "\t");
            result.put("statistics", statistics.toString());
        }

        return complexOkResponse(result);
    }

    /**
     * Kill task
     */
    protected JSONObject killTask(Map params) throws Exception
    {
        try
        {
            login(params);
            checkAdmin();
            JSONArray taskNames = new JSONArray(getStringParameter(params, "tasks"));

            TaskManager taskManager = TaskManager.getInstance();
            for(int i=0; i<taskNames.length(); i++)
            {
                TaskInfo taskInfo = taskManager.getTask(taskNames.getString(i));
                if( taskInfo != null )
                {
                    taskManager.stopTask(taskInfo);
                }
            }
            return simpleOkResponse();
        }
        finally
        {
            SecurityManager.commonLogout();
        }
    }

    protected JSONObject getTasks(Map params) throws Exception
    {
        try
        {
            login(params);
            checkAdmin();
            TaskManager taskManager = TaskManager.getInstance();
            JSONArray array = new JSONArray();
            for(TaskInfo ti: taskManager.getAllRunningTasks())
            {
                JSONObject task = new JSONObject();
                task.put("id", ti.getName());
                task.put("started", ti.getStartTime());
                task.put("username", ti.getUser());
                task.put("type", ti.getType());
                DataElementPath source = ti.getSource();
                task.put("source", source == null ? null : source.toString());
                array.put(task);
            }
            return arrayOkResponse(array);
        }
        finally
        {
            SecurityManager.commonLogout();
        }
    }

    protected JSONObject getTaskHistory(Map params) throws Exception
    {
        try
        {
            login(params);
            checkAdmin();
            Connection rootConnection = GlobalDatabaseManager.getDatabaseConnection();

            try
            {
                SqlUtil.execute( rootConnection, "CREATE INDEX IDX_TASKS_START_END_ID ON bioumlsupport2.tasks(start,end,ID)" );
                SqlUtil.execute( rootConnection, "CREATE INDEX IDX_TASKS_END_ID ON bioumlsupport2.tasks(end,ID)" );
                log.info( "Speed up indices for the table 'tasks' successfully created" );
            }
            catch( Exception e )
            {
                if( e.getMessage() != null && e.getMessage().indexOf( "Duplicate key name" ) >= 0 )
                {
                }
                else
                {
                    log.log( Level.SEVERE, "Unable to create speed up indices for the table 'tasks'", e );
                } 
            }
            

            String sql = "SELECT * FROM bioumlsupport2.tasks";  
            String since = getStringParameter( params, "since" );
            if( since != null )
            {
                since = since.replaceAll( "'", "''" ); 
                sql = sql +
                      "\nWHERE start >= '" + since + "' AND end IS NULL\n" + 
                      "UNION ALL\n" + 
                      sql + 
                      "\nWHERE end >= '" + since + "'\n" + 
                      "ORDER BY ID";
            }

            JSONArray array = new JSONArray();
            SqlUtil.iterate( rootConnection, sql, 
                rs -> {
                    JSONObject task = new JSONObject();
                    task.put("ID", rs.getString( "ID" ));
                    task.put("name", rs.getString( "name" ));
                    task.put("start", rs.getString( "start" ));
                    task.put("end", rs.getString( "end" ));
                    task.put("type", rs.getString( "type" ));
                    task.put("source", rs.getString( "source" ));
                    task.put("status", rs.getString( "status" ));
                    task.put("user", rs.getString( "user" ));
                    task.put("message", rs.getString( "message" ));
                    task.put("properties", rs.getString( "properties" ));
                    array.put(task);
                }
            );

            return arrayOkResponse(array);
        }
        finally
        {
            SecurityManager.commonLogout();
        }
    }

    /**
     * Get user list
     */
    protected JSONObject getActiveUsers(Map params) throws Exception
    {
        try
        {
            login(params);
            checkAdmin();
            List<SessionInfo> sessions = WebSession.getSessions();
            JSONArray array = new JSONArray();
            long time = System.currentTimeMillis();
            for( SessionInfo info : sessions )
            {
                JSONObject session = new JSONObject();
                session.put("username", info.getUserName());
                session.put("idle", info.getLastActivity() == -1 ? -1 : time - info.getLastActivity());
                session.put("id", info.getSessionId());
                session.put("lastPath", info.getLastPath());
                array.put(session);
            }
            return arrayOkResponse(array);
        }
        finally
        {
            SecurityManager.commonLogout();
        }
    }

    /**
     * Invalidate user permissions
     */
    protected JSONObject updatePermissions(Map params) throws Exception
    {
        login(params);
        checkAdmin();
        SecurityManager.invalidatePermissions();
        return simpleOkResponse();
    }

    public enum ProjectType {
        SQL, FILE
    }
    
    public static DataCollection createNewProject( String projectName, String user, boolean reuse, List<String> errors, ProjectType projectType )
    {
        try
        {
            DataElementPath userProjectsPath = CollectionFactoryUtils.getUserProjectsPath();
            if(!SecurityManager.getPermissions(userProjectsPath.getChildPath(projectName)).isWriteAllowed())
            {
                errors.add( "No write permission for the user '" + user + "' to create project '" + projectName + "'" );
                log.log(Level.INFO, errors.get( 0 ) );
                return null;
            }
            if( !isProjectNameValid( projectName ) )
            {
                errors.add( "Project name contains unacceptable characters. Only latin letters, numbers, spaces and underscores ( _ ) are allowed." );
                log.log( Level.INFO, errors.get( 0 ) );
                return null;
            }

            DataCollection userProjectsParent = userProjectsPath.getDataCollection();
            DataCollection primaryParent = null;
            if( userProjectsParent instanceof FilteredDataCollection )
                primaryParent = ((FilteredDataCollection)userProjectsParent).getPrimaryCollection();
            
            if( !userProjectsParent.contains(projectName) && (primaryParent == null || ! primaryParent.contains(projectName) ) )
            {
                if(projectType == ProjectType.SQL)
                    return createSQLBasedProject( projectName, userProjectsParent );
                else if(projectType == ProjectType.FILE)
                    return createFileBasedProject(projectName, userProjectsParent);
            }
            else if( reuse )
            {
                return (DataCollection)userProjectsParent.get(projectName);
            }
        }
        catch( Exception e )
        {
            errors.add( e.getMessage() );
            log.log(Level.SEVERE, "Create project error", e );
        }
        return null;
        
    }
    
    //This method is used from genexplain, and creates SQL based project
    public static DataCollection createNewProject( String projectName, String user, boolean reuse, List<String> errors )
    {
        return createNewProject( projectName, user, reuse, errors, ProjectType.SQL );
    }


    public static DataCollection createSQLBasedProject(String projectName, DataCollection userProjectsParent) throws Exception
    {
        log.log( Level.INFO, "createSQLBasedProject(" + projectName + "): START" );
        long before = System.currentTimeMillis();

        String dbName = getSQLString(projectName);

        String dbBaseProjectName = "research_" + dbName;
        String dbProjectName = dbBaseProjectName;

        for( int i = 0; 
             SqlUtil.hasDatabase( GlobalDatabaseManager.getDatabaseConnection(), dbProjectName ); 
             dbProjectName = dbBaseProjectName + "_" + ( ++i ) )
            ;

        if( dbProjectName.length() > 64 )
        {
            String rand = java.util.UUID.randomUUID().toString().replaceAll( "-", "" );
            dbProjectName = dbProjectName.substring( 0, 32 ) + rand;
        }

        String baseUser = "u_" + dbName;
        if(baseUser.length() > 12) baseUser = baseUser.substring(0, 12);
        String dbUser = baseUser;
        String dbPassword = getRandomString(8);

        //Connection rootConnection = GlobalDatabaseManager.getDatabaseConnection();

        for( int i = 0; 
             SqlUtil.hasUser( GlobalDatabaseManager.getDatabaseConnection(), dbUser ); 
             dbUser = baseUser + "_" + ( ++i ) )
            ;

        String jdbcUrl = GlobalDatabaseManager.getCurrentDBUrl() + "/" + dbProjectName + "?allowLoadLocalInfile=true";
        Properties props = new Properties();
        props.setProperty(SqlDataCollection.JDBC_DRIVER_PROPERTY, SqlDataCollection.JDBC_DEFAULT_DRIVER);
        props.setProperty(SqlDataCollection.JDBC_URL_PROPERTY, jdbcUrl);
        props.setProperty(SqlDataCollection.JDBC_USER_PROPERTY, dbUser);
        props.setProperty(SqlDataCollection.JDBC_PASSWORD_PROPERTY, dbPassword);
        props.setProperty(GenericDataCollection.PREFERED_TABLE_IMPLEMENTATION_PROPERTY, "SQL");

        String historyCollection = Application.getGlobalValue("DefaultHistoryCollection", null);
        if(historyCollection != null)
            props.setProperty(HistoryFacade.HISTORY_COLLECTION, historyCollection);

        try
        { 
            SqlUtil.createDatabase( GlobalDatabaseManager.getDatabaseConnection(), dbProjectName, dbUser, dbPassword);
        }
        catch( Exception e )
        {
              
            log.log( Level.SEVERE, "Unable to create databse with properties:\n" + props );
            throw e; 
        }

        ResearchBuilder researchBuilder = new ResearchBuilder(props);

        long after = System.currentTimeMillis();
        log.log( Level.INFO, "createSQLBasedProject(" + projectName + "): END " + ( after - before ) + "ms" );

        return researchBuilder.createResearch((Repository)userProjectsParent, projectName, true);
    }
    

    private static DataCollection createFileBasedProject(String projectName, DataCollection userProjectsParent) throws Exception
    {
        String projectsFolder = userProjectsParent.getInfo().getProperty( DataCollectionConfigConstants.FILE_PATH_PROPERTY );
        File projectFolder = new File(projectsFolder, projectName);
        projectFolder.mkdirs();
        File files = new File(projectFolder, "files");
        files.mkdirs();
        File configs = new File(projectFolder, "configs");
        configs.mkdir();

        Properties props = new Properties();
        props.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, projectName );
        props.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, FileDataCollection.class.getName() );
        props.setProperty( DataCollectionConfigConstants.FILE_PATH_PROPERTY, files.getAbsolutePath() );
        props.setProperty(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, configs.getAbsolutePath());
        
        ExProperties.store( props, new File(projectFolder, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE) );

        ((Repository)userProjectsParent).updateRepository();
        
        return (DataCollection)userProjectsParent.get( projectName );
    }



    /**
     * Check if project name is valid.
     * Other characters may cause problems with access and indexing
     */
    static boolean isProjectNameValid(String projectName)
    {
        return projectName.matches( "[a-zA-Z0-9][a-zA-Z0-9\\_\\-\\.\\,@\\(\\)\\s]+" );
    }

    /**
     * Get info for current user
     */
    protected JSONObject getUserInfo(Map params) throws Exception
    {
        UserPermissions userPermissions = SecurityManager.getCurrentUserPermission();
        if( userPermissions != null )
        {
            String user = userPermissions.getUser();
            String password = userPermissions.getPassword();

            Map<String, Object> userInfo = SecurityManager.getSecurityProvider().getUserInfo( user, password );
            Object expiration = userInfo.get("expiration");
            if( expiration instanceof Long )
            {
                if( (Long)expiration == 0 )
                {
                    expiration = "-";
                }
                else
                {
                    expiration = new SimpleDateFormat().format(new Date((Long)expiration));
                }
                userInfo.put("expiration", expiration);
            }

            JSONObject result = new JSONObject();
            result.put("username", user);
            for( Entry<String, Object> entry : userInfo.entrySet() )
            {
                result.put(entry.getKey(), entry.getValue());
            }
            return complexOkResponse(result);
        }
        return errorResponse("Undefined user");
    }

    /**
     * Change user password
     */
    protected JSONObject changePassword(Map params) throws Exception
    {
        login(params);
        String newPassword = getStrictParameter(params, "newpass");
        if( !SecurityAdminUtils.changeUserPassword(getStringParameter(params, "user"), getStringParameter(params, "pass"), newPassword) )
            return errorResponse("Cannot change password");
        SecurityManager.invalidatePermissions();
        return simpleOkResponse();
    }

    /**
     * Change user info fields
     */
    protected JSONObject changeInfo(Map params) throws Exception
    {
        login(params);
        Map<String, String> parameters = getStringParameters(params);
        try (StringWriter sw = new StringWriter())
        {
            if( SecurityAdminUtils.updateUserInfo( SecurityManager.getSessionUser(), parameters.get( "pass" ), parameters, sw ) )
                return simpleOkResponse();

            return errorResponse( sw.toString() );
        }
    }

    protected JSONObject getUserInfoDictionaries(Map params) throws Exception
    {
        String name = getStringParameter(params, "dictionary_name");
        StringWriter sw = new StringWriter();
        JSONArray result = null;
        if( name.equals("countries") )
        {
            List<Pair<String, String>> dict = SecurityAdminUtils.getDictionary("countries", "ID", "name", sw);
            if( dict != null )
            {
                result = new JSONArray();
                for( Pair<String, String> pair : dict )
                {
                    JSONObject jsPair = new JSONObject();
                    jsPair.put("id", pair.getFirst());
                    jsPair.put("name", pair.getSecond());
                    result.put(jsPair);
                }
            }
        }
        if( result != null )
        {
            return arrayOkResponse(result);
        }
        return errorResponse(sw.toString());
    }

    /**
     * Remove project with group permissions request
     */
    protected JSONObject deleteProjects(Map params) throws Exception
    {
        login( params );
        String user = getStrictParameter( params, "user" );
        String password = getStrictParameter( params, "pass" );
        String jwToken = getStringParameter( params, "jwtoken" );
        boolean askBiostore = ! ( "true".equals( getStringParameter( params, "skipBiostore" ) ) );

        String projectNamesJson = getStrictParameter( params, "projects" );
        JSONArray projectNames = new JSONArray( projectNamesJson );

        StringWriter sw = new StringWriter();
        String resp;
        StringBuilder removeErrors = new StringBuilder();
        List<String> removedSuccessfully = new ArrayList<>();
        for( int i = 0; i < projectNames.length(); i++ )
        {
            String projectName = projectNames.getString( i );
            log.info( "Deleting project '" + projectName + "', askBiostore = " + askBiostore + "..." );
            StringBuilder curRemoveErrors = new StringBuilder();
            //First remove project from repository
            try
            {
                resp = deleteProject( projectName );
                if( resp != null )
                    curRemoveErrors.append( resp );
            }
            catch( Exception e )
            {
                if( askBiostore )
                {
                    curRemoveErrors.append( "Can not remove from server:  " + e.getMessage() );
                }
                else
                    throw e;
            }

            //Then remove project from biostore or remove user from project group if no enough permissions
            if( askBiostore )
            {
                boolean groupError = !SecurityAdminUtils.removeUserProject( projectName, user, password, jwToken, sw );
                if( groupError )
                {
                    String biostoreMsg = sw.toString();
                    log.log( Level.SEVERE, biostoreMsg );
                    curRemoveErrors.append( biostoreMsg );
                }
            }
            SecurityManager.invalidatePermissions();
            if( curRemoveErrors.length() != 0 )
            {
                removeErrors.append( "\nProject " + projectName + " was not removed completely.\n" );
                removeErrors.append( curRemoveErrors );
            }
            else
                removedSuccessfully.add( projectName );
        }
        login( params ); //call login to re-read permissions from biostore
        if( removeErrors.length() == 0 )
            return simpleOkResponse();
        else
            return errorResponse( removeErrors.toString() );
    }

    private JSONObject listInterruptedTasks(Map params) throws Exception
    {
        login( params );
        checkAdmin();

        TaskManager taskManager = TaskManager.getInstance();
        Map<String, List<TaskInfo>> tasksByUser = taskManager.getAllInterruptedAnalysesByUser();
        List<TaskInfo> taskList = tasksByUser.values().stream().flatMap( List::stream ).collect( Collectors.toList() );
        JSONArray jsonArr = taskInfoListToJson( taskList );
        return arrayOkResponse( jsonArr );
    }

    private JSONObject restartInterruptedTasks(Map params) throws Exception
    {
        login( params );
        checkAdmin();
        
        String user = getStringParameter( params, "user" );
        String pass = getStringParameter( params, "pass" );
        if( pass == null || user == null )
            throw new IllegalArgumentException( "Missing parameters 'user' or 'pass'" );
        

        //Allow to restart subset of tasks
        Object[] nameParam = (Object[])params.get( "name" );
        Set<String> subset = new HashSet<>();
        if(nameParam != null)
            for(Object name : nameParam)
                subset.add( name.toString() );
            
        
        TaskManager taskManager = TaskManager.getInstance();
        Pair<List<TaskInfo>, List<TaskInfo>> result = subset.isEmpty() 
                ? taskManager.restartAllInterruptedTasks( user, pass )
                : taskManager.restartInterruptedTasks( user, pass, subset );
        
        JSONArray success = taskInfoListToJson( result.getFirst() );
        JSONArray failure = taskInfoListToJson( result.getSecond() );
        JSONObject resp = new JSONObject();
        resp.put( "success", success );
        resp.put( "failure", failure );
        return complexOkResponse( resp );
    }
    
    private JSONObject stopInterruptedTasks(Map params) throws Exception
    {
        login( params );
        checkAdmin();

        TaskManager taskManager = TaskManager.getInstance();
        Map<String, List<TaskInfo>> tasksByUser = taskManager.getAllInterruptedAnalysesByUser();
        tasksByUser.values().stream().flatMap( List::stream ).forEach( taskInfo -> taskManager.stopTask( taskInfo ) );
        return simpleOkResponse();
    }

    private JSONArray taskInfoListToJson(List<TaskInfo> tasks)
    {
        JSONArray jsonArr = new JSONArray();
        for(TaskInfo ti : tasks)
        {
            JSONObject tiJson = new JSONObject();
            tiJson.put( "name", ti.getName() );
            tiJson.put( "user", ti.getUser() );
            tiJson.put( "source", ti.getSource() );
            jsonArr.put(tiJson);
        }
        return jsonArr;
    }

    private JSONObject getProjectSize(Map params) throws Exception
    {
        login( params );
        String projectName = getStrictParameter( params, "project" );
        DataElementPath projectPath = CollectionFactoryUtils.getUserProjectsPath().getChildPath( projectName );
        DataCollection<?> project = projectPath.optDataCollection();
        if( project == null )
            return errorResponse( "Project " + projectName + " does not exist." );
        Permission permission = SecurityManager.getPermissions( CollectionFactoryUtils.getUserProjectsPath().getChildPath( projectName ) );
        checkPermission( permission, "getSize" );
        DataCollection<? extends DataElement> dc = projectPath.getChildPath( "Data" ).optDataCollection();
        if( dc == null )
            return errorResponse( "Data folder for project " + projectName + " does not exist." );
        DataCollection<?> primaryCollection = (DataCollection<?>)SecurityManager
                .runPrivileged( () -> DataCollectionUtils.fetchPrimaryCollectionPrivileged( dc ) );
        GenericDataCollection gdc = primaryCollection.cast( GenericDataCollection.class );
        long diskSize = gdc.getDiskSize();
        JSONObject sizeObj = new JSONObject();
        sizeObj.put( "size", diskSize );
        return complexOkResponse( sizeObj );
    }

    private JSONObject setProjectTitle(Map params) throws Exception
    {
        String projectTitle = getStrictParameter( params, "title" );
        String projectName = getStrictParameter( params, "project" );
        DataElementPath projectPath = CollectionFactoryUtils.getUserProjectsPath().getChildPath( projectName );
        DataCollection<?> project = projectPath.optDataCollection();
        if( project == null )
            return errorResponse( "Project " + projectName + " does not exist." );

        String oldTitle = project.getInfo().getProperties().getProperty( ru.biosoft.access.core.DataCollectionConfigConstants.DISPLAY_NAME_PROPERTY, projectName );
        try
        {
            //Check remove permission, project will be saved to parent
            Permission permission = SecurityManager.getPermissions( CollectionFactoryUtils.getUserProjectsPath().getChildPath( projectName ) );
            checkPermission( permission, "writeProperty" );
        }
        catch( SecurityException ex )
        {
            return errorResponse( "Project '" + oldTitle + "' renaming is not allowed." );
        }
        //TODO: same title check here?
        //autogenerated in TitleIndex class will be shown
        
        project.getInfo().writeProperty( ru.biosoft.access.core.DataCollectionConfigConstants.DISPLAY_NAME_PROPERTY, projectTitle );
        //displayName in info is initialized from properties on collection initialization. Set it here too to avoid errors.
        project.getInfo().setDisplayName( projectTitle );
        //Update title index manually if exists, because DC listener will not work for 'write property'
        DataCollection<?> userProjectsParent = CollectionFactoryUtils.getUserProjectsPath().getDataCollection( ru.biosoft.access.core.DataCollection.class );
        QuerySystem qs = userProjectsParent.getInfo().getQuerySystem();
        if( qs != null )
        {
            Index titleIndex = qs.getIndex( "title" );
            if( titleIndex != null && titleIndex instanceof ResearchTitleIndex )
                ( (ResearchTitleIndex)titleIndex ).put( projectName, projectTitle );
        }
        return simpleOkResponse();
    }

    private JSONObject getAllProjectsSize(Map params) throws Exception
    {
        long totalSize = 0;
        DataElementPath userProjectsPath = CollectionFactoryUtils.getUserProjectsPath();
        Iterator<DataElementPath> iter = userProjectsPath.getChildren().iterator();
        while( iter.hasNext() )
        {
            DataElementPath projectPath = iter.next();
            String projectName = projectPath.getName();
            try
            {
                UserPermissions userPermissions = SecurityManager.getCurrentUserPermission();
                if( userPermissions.isGroupAdmin( projectName ) )
                //                Permission permission = SecurityManager
                //                        .getPermissions( CollectionFactoryUtils.getUserProjectsPath().getChildPath( projectName ) );
                //                if( permission.isAdminAllowed() )
                {
                    DataCollection<? extends DataElement> dc = projectPath.getChildPath( "Data" ).optDataCollection();
                    if( dc != null )
                    {
                        DataCollection<?> primaryCollection = (DataCollection<?>)SecurityManager
                                .runPrivileged( () -> DataCollectionUtils.fetchPrimaryCollectionPrivileged( dc ) );
                        if( primaryCollection instanceof GenericDataCollection )
                        {
                            GenericDataCollection gdc = primaryCollection.cast( GenericDataCollection.class );
                            long diskSize = gdc.getDiskSize();
                            totalSize += diskSize;
                        }
                    }
                }
            }
            catch( SecurityException ex )
            {
            }
        }
        JSONObject sizeObj = new JSONObject();
        String formattedSize = TextUtil.formatSize( totalSize );
        //TODO:dirty replacement to get MB instead of bytes, maybe change formatSize method or copy here
        String newFormattedSize = formattedSize.replaceAll( "\\(.+ bytes\\)",
                formattedSize.contains( "Mb" ) ? "" : "(" + totalSize / ( 1024 * 1024 ) + " Mb)" );
        sizeObj.put( "sizeTotal", newFormattedSize );
        return complexOkResponse( sizeObj );
    }
}
