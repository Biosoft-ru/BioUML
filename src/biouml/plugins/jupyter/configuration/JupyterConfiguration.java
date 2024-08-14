package biouml.plugins.jupyter.configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import java.io.File;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.Preferences;

public class JupyterConfiguration
{
    private static final char ESCAPE_DELIM = '_';
    private static final char ESCAPE_DELIM_NEW = '-';
    private static final Logger log = Logger.getLogger( JupyterConfiguration.class.getName() );
    private static final JupyterConfiguration instance = new JupyterConfiguration();
    public static JupyterConfiguration getConfiguration()
    {
        return instance;
    }

    private static final String JUPYTER_HOME = "JupyterHome";
    private static final String JUPYTER_BASE_LINK = "JupyterBaseLink";
    private static final String JUPYTER_BASE_LOGIN_LINK = "JupyterBaseLoginLink";
    private static final String JUPYTER_VIEW_SUFFIX = "JupyterViewLinkSuffix";
    private static final String JUPYTER_OPEN_SUFFIX = "JupyterLinkSuffix";
    private static final String JUPYTER_OPEN_LAB_SUFFIX = "JupyterLabLinkSuffix";
    private static final String ACCESSOR_CLASS = "AccessorClass";
    private static final String SINGLE_USER_NAME = "UserName";
    private static final String SINGLE_USER_PASSWORD = "UserPassword";

    private static final String BIOUML_SERVER = "BioumlServer";
    private static final String REPOSITORY_DOCKER_IMAGE = "RepositoryDockerImage";

    private static final String USE_HTTPS_FOR_BIOUML = "UseHttpsForBiouml";
    private static final String USE_LAB = "UseJupyterLab";
    // Notebook is installed on the same machine as BioUML
    // just open it in this case
    private static final String USE_LOCAL_NOTEBOOK = "UseLocalNotebook";

    private static final String FILE_PERMISSIONS_MASK = "FilePermissionsMask";

    private final boolean isConfigured;
    private final String jupyterHome;
    private final String jupyterLoginLink;
    private final String jupyterViewFileLink;
    private final String jupyterFileLink;
    private final String jupyterAccessorClass;
    private final String jupyterSingleUserName;
    private final String jupyterSingleUserPassword;

    private final String jupyterBaseLink;
    private final String jupyterBaseLoginLink;

    private final String openSuffix;
    private final String viewSuffix;
    private final String openLabSuffix;

    private final String bioumlServer;
    private final String repositoryDockerImage;

    //shows if biouml server uses only https
    //this is necessary for correct use of R-kernel
    private final boolean useHttpsForBiouml;
    private final boolean useLab;
    private final boolean useLocalNotebook;

    private final String filePermissionsMask;

    //TODO: think about refactoring (e.g. make one class from this one and AccessorFactory)
    private JupyterConfiguration()
    {
        Preferences preferences = Application.getPreferences().getPreferencesValue( "JupyterConfig" );

        log.info( "Got preferences = " + preferences );

        if( preferences == null )
        {
            isConfigured = false;
            jupyterHome = null;
            jupyterLoginLink = null;
            jupyterBaseLoginLink = null;
            jupyterViewFileLink = null;
            jupyterFileLink = null;
            jupyterAccessorClass = null;
            jupyterSingleUserName = null;
            jupyterSingleUserPassword = null;

            jupyterBaseLink = null;
            viewSuffix = null;
            openSuffix = null;
            openLabSuffix = null;

            bioumlServer = null;
            repositoryDockerImage = null;
            useHttpsForBiouml = false;
            useLab = false;
            useLocalNotebook = false; 

            filePermissionsMask = null; 
            return;
        }
        jupyterAccessorClass = preferences.getStringValue( ACCESSOR_CLASS, null );
        jupyterHome = preferences.getStringValue( JUPYTER_HOME, null );
        jupyterBaseLink = preferences.getStringValue( JUPYTER_BASE_LINK, null );
        jupyterBaseLoginLink = preferences.getStringValue( JUPYTER_BASE_LOGIN_LINK, null );
        viewSuffix = preferences.getStringValue( JUPYTER_VIEW_SUFFIX, "user/$user$/nbconvert/html/$file$" );
        openSuffix = preferences.getStringValue( JUPYTER_OPEN_SUFFIX, "user/$user$/notebooks/$file$" );
        openLabSuffix = preferences.getStringValue( JUPYTER_OPEN_LAB_SUFFIX, "user/$user$/lab/tree/$file$" );
        jupyterSingleUserName = preferences.getStringValue( SINGLE_USER_NAME, null );
        jupyterSingleUserPassword = preferences.getStringValue( SINGLE_USER_PASSWORD, null );

        bioumlServer = preferences.getStringValue( BIOUML_SERVER, null );
        repositoryDockerImage = preferences.getStringValue( REPOSITORY_DOCKER_IMAGE, null );

        useHttpsForBiouml = Boolean.parseBoolean( preferences.getStringValue( USE_HTTPS_FOR_BIOUML, "false" ) );
        useLab = Boolean.parseBoolean( preferences.getStringValue( USE_LAB, "false" ) );
        useLocalNotebook = Boolean.parseBoolean( preferences.getStringValue( USE_LOCAL_NOTEBOOK, "false" ) );

        filePermissionsMask = preferences.getStringValue( FILE_PERMISSIONS_MASK, "rw-rw----" );

        boolean missHome = jupyterHome == null;
        boolean missLink = jupyterBaseLink == null;
        if( !useLocalNotebook && ( missHome || missLink ) )
        {
            if( missHome )
                log.log( Level.SEVERE, "Incorrect jupyter configuration: cannot retrieve 'JupyterHome' variable" );
            if( missLink )
                log.log( Level.SEVERE, "Incorrect jupyter configuration: cannot retrieve 'JupyterBaseLink' variable" );

            jupyterViewFileLink = null;
            jupyterFileLink = null;
            jupyterLoginLink = null;

            isConfigured = false;
            return;
        }
        jupyterLoginLink = ( jupyterBaseLoginLink != null ? jupyterBaseLoginLink : jupyterBaseLink ) + "/hub/login";
        jupyterViewFileLink = jupyterBaseLink + "/" + viewSuffix;
        jupyterFileLink = jupyterBaseLink + "/" + ( useLab ? openLabSuffix : openSuffix );
        isConfigured = true;
    }

    public boolean isConfigured()
    {
        return isConfigured;
    }

    public String getAccessorClass()
    {
        return jupyterAccessorClass;
    }

    //TODO: maybe UnsopportedOperationException should be thrown if jupyter is not configured
    public String getHomePath(String user)
    {
        if( useLocalNotebook )
        {
            return jupyterHome;
        }

        if( user == null || "".equals( user ) )
        {
            return jupyterHome.replace( "$user$", "anonymous" );
        }   
        String escapedUser = escapeUserName( user );
        return jupyterHome.replace( "$user$", escapedUser );
    }

    /**
     * Emulates work of username escaping in the dockerspawner code
     */
    private String escapeUserName(String username)
    {
        StringBuilder sb = new StringBuilder();
        for( int i = 0; i < username.length(); i++ )
        {
            char curChar = username.charAt( i );
            //if( ( curChar >= 'a' && curChar <= 'z' ) || ( curChar >= 'A' && curChar <= 'Z' ) /*|| curChar == '-'*/ )
            if( ( curChar >= '0' && curChar <= '9' ) || ( curChar >= 'a' && curChar <= 'z' ) || ( curChar >= 'A' && curChar <= 'Z' ) /*|| curChar == '-'*/ )
                sb.append( curChar );
            else
                sb.append( ESCAPE_DELIM_NEW ).append( Integer.toHexString( curChar ).toLowerCase( Locale.ENGLISH ) );
        }
        return sb.toString();
    }

    private String escapeServerName(String serverName)
    {
        StringBuilder sb = new StringBuilder();
        for( int i = 0; i < serverName.length(); i++ )
        {
            char curChar = serverName.charAt( i );
            if( ( curChar >= '0' && curChar <= '9' ) || ( curChar >= 'a' && curChar <= 'z' ) || ( curChar >= 'A' && curChar <= 'Z' ) )
                sb.append( curChar );
            else
                sb.append( ESCAPE_DELIM_NEW ).append( Integer.toHexString( curChar ).toLowerCase( Locale.ENGLISH ) );
        }
        return sb.toString();
    }

    private String escapeUserNameOld(String username)
    {
        StringBuilder sb = new StringBuilder();
        for( int i = 0; i < username.length(); i++ )
        {
            char curChar = username.charAt( i );
            if( ( curChar >= 'a' && curChar <= 'z' ) || ( curChar >= 'A' && curChar <= 'Z' ) || curChar == '-' )
                sb.append( curChar );
            else
                sb.append( ESCAPE_DELIM ).append( Integer.toHexString( curChar ).toUpperCase( Locale.ENGLISH ) );
        }
        return sb.toString();
    }

    private String escapeServerNameOld(String serverName)
    {
        StringBuilder sb = new StringBuilder();
        for( int i = 0; i < serverName.length(); i++ )
        {
            char curChar = serverName.charAt( i );
            if( ( curChar >= '0' && curChar <= '9' ) || ( curChar >= 'a' && curChar <= 'z' ) || ( curChar >= 'A' && curChar <= 'Z' ) )
                sb.append( curChar );
            else
                sb.append( ESCAPE_DELIM ).append( Integer.toHexString( curChar ).toUpperCase( Locale.ENGLISH ) );
        }
        return sb.toString();
    }

    public String getLoginUrl()
    {
        return jupyterLoginLink;
    }

    public String getBioumlServer()
    {
        return bioumlServer;
    }

    public String getRepositoryDockerImage()
    {
        return repositoryDockerImage;
    }

    public String getFileLinkUrl( String user, String fileName )
    {
        if( useLocalNotebook )
        {
            return getFileLinkUrl( user, fileName, "" );
        }

        if( user == null || "".equals( user ) )
        {
            return jupyterViewFileLink.replace( "$user$", "anonymous" ).replace( "$file$", fileName );
        }   
        return jupyterFileLink.replace( "$user$", user ).replace( "$file$", fileName );
    }

    public String getFileLinkUrl( String user, String fileName, String image )
    {
        if( useLocalNotebook )
        {
            java.nio.file.Path currentRelativePath = java.nio.file.Paths.get("");
            String localFolder = currentRelativePath.toAbsolutePath().toString();

            log.info( "Looking for '" + fileName + "' in local folder '" + localFolder + "'..." );

            List <String> parts = Arrays.asList( fileName.split( "/" ) );

            // find local path by trimming folders one by one 
            for( int i = 1; i < parts.size(); i++ )
            {
                String testPath = parts.subList( i, parts.size() ).stream().collect( Collectors.joining( "/" ) );
                if( new File( testPath ).exists() )
                {
                    log.info( "Found local notebook file '" + testPath + "'" );
                    String path = jupyterFileLink;
                    if( !path.endsWith( "/" ) )
                    {
                         path += "/";
                    }
                    path += testPath;
                    if( System.getenv( "JUPYTERHUB_SERVICE_PREFIX" ) != null )
                    {
                        return System.getenv( "JUPYTERHUB_SERVICE_PREFIX" ) + path;
                    }

                    log.info( "Resulting path = '" + path + "'" );
                    return path; 
                }
            }

            log.log( Level.SEVERE, "Unable to find file '" + fileName + "' in local folder '" + localFolder + "'" );

            // this is wrong path but at least it will give us an idea of what we have
            return fileName; 
        }

        String serverName = escapeServerName( image );

        if( user == null || "".equals( user ) )
        {
            user = "anonymous";
        }   

        String link = jupyterBaseLink + "/hub/spawn/" + user + "/" + serverName;

        String next = "/jupyter/hub/";
        if( "anonymous".equals( user ) )
        {
            log.info( "View Link is '" + viewSuffix + "'" );
            next += viewSuffix;
        }
        else 
        {
            next += ( useLab ? openLabSuffix : openSuffix );
        }  
        next = next.replace( "$user$", user + "/" + serverName ).replace( "$file$", fileName );
        next = next.replace( "%", "%20" ).replace( "%", "%25" ).replace( "/", "%2F" ); // exacltly this order

        link += "?next=" + next;
        link += "%3F_ts_=" + System.currentTimeMillis();

        return link;
    }

    public String getHubApiUrl()
    {
        String link = ( jupyterBaseLoginLink != null ? jupyterBaseLoginLink : jupyterBaseLink ) + "/hub/api/";

        // retrun null for binder
        return "".equals( jupyterBaseLink ) ? null : link;
    }

    public String getApiUrl( String user, String image )
    {
        String serverName = escapeServerName( image );

        if( user == null || "".equals( user ) )
        {
            user = "anonymous";
        }   

        String link = ( jupyterBaseLoginLink != null ? jupyterBaseLoginLink : jupyterBaseLink ) + "/user/" + user + "/" + serverName + "/api/";

        // retrun null for binder
        return "".equals( jupyterBaseLink ) ? null : link;
    }
        
    public String getSingleUserName()
    {
        return jupyterSingleUserName;
    }

    public String getSingleUserPassword()
    {
        return jupyterSingleUserPassword;
    }

    public boolean useHttpsForBiouml()
    {
        return useHttpsForBiouml;
    }

    public boolean useLocalNotebook()
    {
        return useLocalNotebook;
    }

    public String getFilePermissionsMask()
    {
        return filePermissionsMask;
    }
}
