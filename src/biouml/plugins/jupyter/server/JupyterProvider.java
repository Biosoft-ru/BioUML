package biouml.plugins.jupyter.server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.PosixFileAttributeView;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.eclipsesource.json.JsonObject;

import biouml.plugins.jupyter.access.IPythonElement;
import biouml.plugins.jupyter.auth.AccessorFactory;
import biouml.plugins.jupyter.auth.JupyterAccessor;
import biouml.plugins.jupyter.configuration.JupyterConfiguration;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.Permission;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.UserPermissions;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.BiosoftWebResponse;
import ru.biosoft.server.servlets.webservices.providers.WebProviderSupport;
import ru.biosoft.server.servlets.webservices.BiosoftWebResponse.CookieTemplate;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.message.BasicNameValuePair;

public class JupyterProvider extends WebProviderSupport
{
    private static final String PYTHON_KERNEL = "python3";
    private static final String R_KERNEL = "rkernel";
    private static final String BIOUML_KERNEL = "bioumlkernel";
    private static final String SOS_KERNEL = "soskernel";

    private static final String JUPYTER_OPEN = "open";
    private static final String JUPYTER_OPENKERNEL = "openKernel";
    private static final String JUPYTER_CLOSE = "close";
    private static final String JUPYTER_CREATE = "create";
    private static final Map<String, String> LINKS_MAP = new HashMap<>();

    @Override
    public void process(BiosoftWebRequest req, BiosoftWebResponse resp) throws Exception
    {
        String action = req.getAction();

        log.log( Level.INFO, "JupyterProvider.proecess arguments = " + req.getArguments() );
        
        JupyterResponse response = new JupyterResponse( resp );
        JupyterConfiguration jupyterConf = JupyterConfiguration.getConfiguration();
        if( !jupyterConf.isConfigured() )
        {
            response.error( "Jupyter is not configured on this server." );
            return;
        }

        UserPermissions currentUserPermission = SecurityManager.getCurrentUserPermission();
        final String user = currentUserPermission.getUser().toLowerCase( Locale.ENGLISH );
        String password = currentUserPermission.getPassword();
        /*
        if( user == null || user.isEmpty() )
        {
            response.error( "Sorry, jupyter is not enabled for anonymous users for now." );
            return;
        }
        */

        JupyterAccessor accessor = AccessorFactory.getAccessor( jupyterConf );

        boolean bHomeDirCreated = false;
        String jupyterHomePath = jupyterConf.getHomePath( user );
        File homeDir = new File( jupyterHomePath );
        if( !jupyterConf.useLocalNotebook() && homeDir.exists() && !homeDir.isDirectory() )
        {
            log.log( Level.SEVERE, "Deleting '" + jupyterHomePath + "' since it is not directory..." );
            homeDir.delete();
        }
        if( !homeDir.exists() )
        {  
            log.log( Level.SEVERE, "Creating '" + jupyterHomePath + "'..." );
            homeDir.mkdirs();
            bHomeDirCreated = true;
        } 

        if( !jupyterConf.useLocalNotebook() )
        {  
            try
            {
                if( bHomeDirCreated )
                { 
                    updatePermissions( jupyterHomePath, "rwxrwx---" );
                }
                File userData = new File( homeDir, ".user.txt" );
                if( userData.exists() )
                    userData.delete();
                try( PrintWriter pw = new PrintWriter( userData ) )
                {
                    if( user == null || "".equals( user ) )
                    {
                        pw.println( "user\tanonymous" );
                        pw.println( "pass\tanonymous" );
                    }
                    else
                    {
                        pw.println( "user\t" + user );
                        pw.println( "pass\t" + password );
                    }
                    String serverName = jupyterConf.getBioumlServer() != null ? jupyterConf.getBioumlServer() : 
                                   SecurityManager.getSecurityProvider().getServerName();
                    if( !serverName.startsWith( "http://" ) && !serverName.startsWith( "https://" ) )
                    {
                        serverName = ( jupyterConf.useHttpsForBiouml() ? "https://" : "http://" ) + serverName;
                    }
                    pw.println( "url\t" + serverName );
                }
                catch( IOException e )
                {
                    log.log( Level.WARNING, "Cannot create file with user data for Jupyter.", e );
                }
                updatePermissions( jupyterHomePath + "/.user.txt", jupyterConf.getFilePermissionsMask() );
            }
            catch( IOException e )
            {
                log.log( Level.SEVERE, "Cannot change rights on '" + user + "' jupyter home directory ('" + jupyterHomePath + "')", e );
            }
        }

        if( JUPYTER_OPEN.equals( action ) )
        {
            String reqDE = req.get( "de" );
            log.info( "Opening notebook for '" + user + "' via data element '" + reqDE + "'..." );
            DataElementPath dep = DataElementPath.create( reqDE );
            if( dep == null )
            {
                response.error( "Incorrect element." );
                return;
            }
            IPythonElement element = dep.optDataElement( IPythonElement.class );
            if( element == null )
            {
                response.error( "Element '" + dep + "' has incorrect type." );
                return;
            }

            List<String> authCookies = ( user == null || "".equals( user ) ) ? 
                 accessor.getAuthCookies( "anonymous", "anonymous" ) :
                 accessor.getAuthCookies( user, password );

            String filePath = element.getFilePath();
            log.info( "Got filePath = '" + filePath + "'." );
            if( !jupyterConf.useLocalNotebook() )
            {  
                try
                {
                    updatePermissions( filePath, jupyterConf.getFilePermissionsMask() );
                }
                catch( IOException e )
                {
                    log.log( Level.SEVERE, "Cannot process .ipynb file " + filePath, e );
                    String link = jupyterConf.getFileLinkUrl( user, "" );
                    log.info( "Opening notebook for '" + user + "' via " + link );
                    sendResultJson( response, link, "", authCookies ); //TODO: maybe send error here?
                    return;
                }
            }

            String tmpName = element.getName();
            log.info( "Got element.getName() = '" + tmpName + "'." );
            JupyterFileLink jfl = new JupyterFileLink( jupyterConf, jupyterHomePath, tmpName, null );
            
            String linkFileName = null; 

            Permission permissions = SecurityManager.getPermissions( dep.getParentPath() ); 
            if( !permissions.isWriteAllowed() )
            {
                linkFileName = jfl.copyFile( filePath );
            }
            else 
            {
                linkFileName = jfl.linkFile( filePath );
            }

            String uuid = null;
            if( !linkFileName.isEmpty() )
            {
                uuid = UUID.randomUUID().toString();
                LINKS_MAP.put( uuid, jfl.getCompletePath() );
            }

            String link = jupyterConf.getFileLinkUrl( user, linkFileName, jupyterConf.getRepositoryDockerImage() );
            log.info( "Opening local notebook for '" + user + "' via " + link );
            sendResultJson( response, link, uuid, authCookies );
        }
        else if( JUPYTER_CLOSE.equals( action ) )
        {
            String uuid = req.getOrDefault( "uuid", "" );
            String pathStr = LINKS_MAP.get( uuid );
            log.info( "Closing notebook '" + pathStr + "'..." );
            if( pathStr != null )
            {
                Path path = Paths.get( pathStr );
                String fileName = path.getFileName().toString();
                log.info( "Notebook's file name = '" + fileName + "'." );

                if( Files.exists( path ) )
                {  
                    Files.delete( path );
                    log.info( "Deleted file '" + pathStr + "'." );
                }

                final String hubApi = jupyterConf.getHubApiUrl();
                final String api = jupyterConf.getApiUrl( user, jupyterConf.getRepositoryDockerImage() );

                if( hubApi == null )
                {
                    response.sendSimpleOK();
                    return;
                } 

                ByteArrayOutputStream out = new ByteArrayOutputStream();

                String body = ( user == null || "".equals( user ) ) ? 
                         "{\"username\": \"anonymous\", \"password\": \"anonymous\"}" :
                         "{\"username\": \"" + user + "\", \"password\": \"" + password.replace("\"", "\\\"") + "\"}";

                String getTokenUrl = hubApi + "authorizations/token";
                HttpResponse aresp = Request.Post( getTokenUrl )
                    .bodyString( body, org.apache.http.entity.ContentType.APPLICATION_JSON )
                    .execute()
                    .returnResponse();

                aresp.getEntity().writeTo( out );
                String responseString = out.toString();

                if( responseString == null || responseString.indexOf( "\"token\":" ) < 0 )
                {
                    log.log( Level.SEVERE, "Unable to get auth token to stop the kernel = " + responseString + 
                        "\n" + getTokenUrl + "\n" + body );
                    response.error( "Unable to get auth token to stop the kernel = " + responseString  );
                    return; 
                }

                org.json.JSONObject json = new org.json.JSONObject( responseString );
                final String token = json.getString( "token" ); 

                log.info( "Retrieving list of notebooks via '" + api + "sessions'..." );
                try
                {  
                    aresp = Request.Get( api + "sessions" )
                            .addHeader( "Authorization", "token " + token )
                            .execute()
                            .returnResponse();

                    aresp.getEntity().writeTo( out = new ByteArrayOutputStream() );

                    responseString = out.toString();
                    log.info( "Got response = " + responseString );
                    org.json.JSONArray sessions = new org.json.JSONArray( responseString );
                    for( int i = 0; i < sessions.length(); i++ )
                    {
                        if( !fileName.equals( sessions.getJSONObject( i ).getString( "path" ) ) )
                        {
                            continue;
                        }

                        String method = api + "sessions/" + sessions.getJSONObject( i ).getString( "id" );  
                        aresp = Request.Delete( method )
                                .addHeader( "Authorization", "token " + token )
                                .execute()
                                .returnResponse();

                        log.info( "DELETE " + method + ": " + aresp.getStatusLine().getStatusCode() );

                        break; 
                    }
                }
                finally
                {
                    new Thread(() -> {
                        try
                        {
                            String method2 = hubApi + "users/" + user + "/tokens";  
                            HttpResponse aresp2 = Request.Get( method2 )
                                    .addHeader( "Authorization", "token " + token )
                                    .execute()
                                    .returnResponse();

                            ByteArrayOutputStream out2 = new ByteArrayOutputStream();
                            aresp2.getEntity().writeTo( out2 );
                            String responseString2 = out2.toString();

                            org.json.JSONObject tokenObj = new org.json.JSONObject( responseString2 );
                            org.json.JSONArray tokens = tokenObj.getJSONArray( "api_tokens" );
                            java.util.TreeMap<String,String> sorted = new java.util.TreeMap<>(); 
                            for( int i = 0; i < tokens.length(); i++ )
                            {
                                if( "Requested via deprecated api".equals( tokens.getJSONObject( i ).getString( "note" ) ) )
                                {
                                    sorted.put( tokens.getJSONObject( i ).getString( "created" ), tokens.getJSONObject( i ).getString( "id" ) );
                                } 
                            }
                            for( java.util.Map.Entry<String,String> entry: sorted.entrySet() )
                            {
                                method2 = hubApi + "users/" + user + "/tokens/" + entry.getValue();  
                                aresp2 = Request.Delete( method2 )
                                        .addHeader( "Authorization", "token " + token )
                                        .execute()
                                        .returnResponse();
                                log.info( "DELETE " + method2 + ": " + aresp2.getStatusLine().getStatusCode() );
                            }
                        }
                        catch( Throwable t )
                        {
                           log.log( Level.WARNING, "Unable to clean auth tokens", t );
                        }
                    }).start();
                }   
            }
            response.sendSimpleOK();
        }
        else if( JUPYTER_CREATE.equals( action ) )
        {
            DataElementPath fullPath = req.getDataElementPath();
            DataCollection<DataElement> parent = fullPath.getParentCollection();
            String name = fullPath.getName();
            File file = DataCollectionUtils.getChildFile( parent, name );
            String kernelType = req.getOrDefault( "kernel", PYTHON_KERNEL );
            String content = constructEmptyFile( kernelType );
            IPythonElement de = new IPythonElement( name, parent, content, file.getAbsolutePath() );
            parent.put( de );

            response.sendSimpleOK();
        }
        else if( JUPYTER_OPENKERNEL.equals( action ) )
        {
            DataElementPath fullPath = req.getDataElementPath();
            String []completePath = fullPath.getPathComponents();
            String dockerImage = completePath[ completePath.length - 3 ] + "/" + completePath[ completePath.length - 2 ];
            String kernelType = completePath[ completePath.length - 1 ];

            File imageFile = new File( homeDir, ".dockerImage.txt" );
            if( imageFile.exists() )
                imageFile.delete();
            File imageFilePrev = new File( homeDir, ".dockerImage.txt.prev" );
            if( imageFilePrev.exists() )
                imageFilePrev.delete();

            try( PrintWriter pw = new PrintWriter( imageFile ) )
            {
                pw.println( dockerImage );
            }
            catch( IOException e )
            {
                log.log( Level.WARNING, "Cannot create file with docker image for Jupyter.", e );
            }
            updatePermissions( jupyterHomePath + "/.dockerImage.txt", jupyterConf.getFilePermissionsMask() );
            String content = constructEmptyFile( kernelType );

            JupyterFileLink jfl = new JupyterFileLink( jupyterConf, jupyterHomePath, "Jupyter Analysis", content );

            List<String> authCookies = ( user == null || "".equals( user ) ) ? 
                 accessor.getAuthCookies( "anonymous", "anonymous" ) :
                 accessor.getAuthCookies( user, password );

            try
            {
                updatePermissions( jfl.getCompletePath(), jupyterConf.getFilePermissionsMask() );
            }
            catch( IOException e )
            {
                log.log( Level.SEVERE, "Cannot process .ipynb file " + jfl.getCompletePath(), e );
                String link = jupyterConf.getFileLinkUrl( user, "", dockerImage );
                sendResultJson( response, link, "", authCookies ); //TODO: maybe send error here?
                return;
            }

            String link = jupyterConf.getFileLinkUrl( user, jfl.getLinkName(), dockerImage );
            sendResultJson( response, link, "", authCookies );
        }
        else
        { 
            response.error( "Incorrect action for jupyter: " + action );
        }
    }

    //TODO: think about better solution
    private String constructEmptyFile(String kernelType)
    {
        StringBuilder sb = new StringBuilder("{\"cells\":");
        sb.append( "[{\"cell_type\":\"code\",\"execution_count\":null,\"metadata\":{},\"outputs\":[],\"source\":[]}]," );

        switch( kernelType )
        {
            case R_KERNEL:
                sb.append( "\"metadata\":{\"kernelspec\":{\"display_name\":\"R\",\"language\":\"R\",\"name\":\"ir\"},\"language_info\":" )
                        .append( "{\"codemirror_mode\":\"r\",\"file_extension\":\".r\",\"mimetype\":\"text/x-r-source\", " )
                        .append( "\"name\":\"R\",\"pygments_lexer\": \"r\",\"version\": \"3.6.1\"}}," );
                break;
            case BIOUML_KERNEL:
                sb.append( "\"metadata\":{\"kernelspec\":{\"display_name\":\"JS_BioUML\",\"language\":\"JavaScript\",\"name\":\"js_biouml\"},\"language_info\":" )
                        .append( "{\"codemirror_mode\":\"javascript\",\"file_extension\":\".js\",\"mimetype\":\"text/javascript\", " )
                        .append( "\"name\":\"ECMAScript\",\"pygments_lexer\": \"javascript\",\"version\": \"ECMA - 262 Edition 5.1\"}}," );
                break;
            case SOS_KERNEL:
                sb.append( "\"metadata\":{\"kernelspec\":{\"display_name\":\"SoS\",\"language\":\"sos\",\"name\":\"sos\"},\"language_info\":" )
                        .append( "{\"codemirror_mode\":\"sos\",\"file_extension\":\".sos\",\"mimetype\":\"text/x-sos\", " )
                        .append( "\"name\":\"sos\",\"pygments_lexer\": \"sos\",\"nbconvert_exporter\": \"sos_notebook.converter.SoS_Exporter\"}}," );
                break;
            case PYTHON_KERNEL:
                sb.append( "\"metadata\":{\"kernelspec\":{\"display_name\":\"Python 3\",\"language\":\"python\",\"name\":\"python3\"}," )
                        .append( "\"language_info\":{\"codemirror_mode\":{\"name\":\"ipython\",\"version\":3},\"name\":\"python\"}}," );
                break;
            default:
                log.log( Level.WARNING, "Invalid jupyter notebook kernel type passed : '" + kernelType + "'. Python kernel will be used." );
                sb.append( "\"metadata\":{\"kernelspec\":{\"display_name\":\"Python 3\",\"language\":\"python\",\"name\":\"python3\"}," )
                        .append( "\"language_info\":{\"codemirror_mode\":{\"name\":\"ipython\",\"version\":3},\"name\":\"python\"}}," );
                break;
        }
        sb.append( "\"nbformat\":4,\"nbformat_minor\":2}" );
        return sb.toString();
    }

    void updatePermissions( String pathToFile, String chmod ) throws IOException
    {
        Path path = Paths.get( pathToFile );
        Files.setPosixFilePermissions( path, PosixFilePermissions.fromString( chmod ) );

        java.nio.file.attribute.UserPrincipalLookupService service = path.getFileSystem().getUserPrincipalLookupService();
 
        String nbUser = "1000";
        String nbGroup = "100";

        java.nio.file.attribute.UserPrincipal userPrincipal = service.lookupPrincipalByName( nbUser );
        if( userPrincipal != null )
        {
            try
            {
                Files.setOwner( path, userPrincipal );
            }
            catch( java.nio.file.FileSystemException fse )
            {
                log.log( Level.SEVERE, "Unable to change owner for '" + path + "' to UID " + nbUser + ": " + fse.getMessage() );
            }  
        }
        else
        {
            log.log( Level.SEVERE, "Unable to find user principal for: " + nbUser );
        } 
        java.nio.file.attribute.GroupPrincipal groupPrincipal = service.lookupPrincipalByGroupName( nbGroup );
        if( groupPrincipal != null )
        {
            PosixFileAttributeView view = Files.getFileAttributeView( path, PosixFileAttributeView.class );
            try
            {
                view.setGroup( groupPrincipal );
            }
            catch( java.nio.file.FileSystemException fse )
            {
                log.log( Level.SEVERE, "Unable to change group for '" + path + "' to GID " + nbGroup + ": " + fse.getMessage() );
            }  
        }  
        else
        {
            log.log( Level.SEVERE, "Unable to find group principal for: " + nbGroup );
        } 
    }

    private class JupyterFileLink
    {
        private final Logger log = Logger.getLogger( JupyterFileLink.class.getName() );

        private final JupyterConfiguration conf;
        private final String dirPath;
        private String linkName;

        public JupyterFileLink( JupyterConfiguration conf, 
            String dirPath, String defaultName, String content ) throws java.io.FileNotFoundException
        {
            this.conf = conf;
            this.dirPath = dirPath;
            if( defaultName.endsWith( ".ipynb" ) )
            {  
                defaultName = defaultName.substring( 0, defaultName.length() - 6 );
                defaultName = defaultName + "_" + System.currentTimeMillis();
            }  

            String name = defaultName + ".ipynb";
            String tmpPath = dirPath + "/" + name;
            int i = 0;
            while( Files.exists( Paths.get( tmpPath ) ) )
            {
                name = defaultName + "_" + i + ".ipynb";
                tmpPath = dirPath + "/" + name;
                i++;
            }

            if( content != null )
            {
                try( PrintWriter pw = new PrintWriter( new File( tmpPath ) ) )
                {
                    pw.print( content );
                }
            }

            this.linkName = name;
        }

        public String getCompletePath()
        {
            return dirPath + "/" + linkName;
        }

        public String getLinkName()
        {
            return linkName;
        }
        /**
         * Returns name of the linked file
         */
        public String linkFile(String sourceFilePath) throws Exception
        {
            if( conf.useLocalNotebook() )
            {
                return sourceFilePath;
            }

            try
            {
                log.info( "Linking '" + sourceFilePath + "' to '" + dirPath + "/" + linkName + "'" );
                Files.createLink( Paths.get( dirPath + "/" + linkName ), Paths.get( sourceFilePath ) );
            }
            catch( Exception e )
            {
                log.log( Level.SEVERE, "Cannot create link", e );
                return "";
            }
            return linkName;
        }

        public String copyFile(String sourceFilePath) throws Exception
        {
            try
            {
                String target = dirPath + "/" + linkName;
                log.info( "Copying '" + sourceFilePath + "' to '" + target + "'" );
                Files.copy( Paths.get( sourceFilePath ), Paths.get(target ) );
                log.info( "Making read-only '" + target + "'" );
                JupyterProvider.this.updatePermissions( target, "r--r--r--" );
            }
            catch( Exception e )
            {
                log.log( Level.SEVERE, "Cannot copy", e );
                return "";
            }
            return linkName;
        }

    }

    private void sendResultJson(JupyterResponse response, String link, String uuid, List<String> cookies) throws IOException
    {
        JsonObject result = new JsonObject();
        result.add( "type", "ok" );
        result.add( "link", link );
        if( uuid != null && !uuid.isEmpty() )
            result.add( "uuid", uuid );

        log.info( "Result JSON = " + result );

        response.setCookies( cookies );
        response.sendJSON( result );
    }
}
