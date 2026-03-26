package ru.biosoft.server.servlets.webservices.providers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.filter.FilteredDataCollection;
import ru.biosoft.access.git.GitConstants;
import ru.biosoft.access.git.GitDataCollection;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.server.servlets.support.SupportServlet;
import ru.biosoft.server.servlets.support.SupportServlet.ProjectType;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.util.Pair;
import ru.biosoft.util.TempFileManager;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.workbench.Framework;

public class GitWebProvider extends WebJSONProviderSupport
{
    protected static final Logger log = Logger.getLogger( GitWebProvider.class.getName() );
    private File getProjectDirectory( String prjName )
    {
        String userPath = TextUtil2.subst( CollectionFactoryUtils.getUserProjectsPath().toString(), "data/", "" );
        for ( String repo : Framework.getRepositoryPaths() )
        {
            File prjDir = new File( repo + "/" + userPath + "/" + prjName );
            if( prjDir.exists() )
            {
                return prjDir;
            }
        }

        return null;
    }

    private File getProjectDirectory(DataCollection dc)
    {
        if( dc instanceof GitDataCollection )
            return new File( ((GitDataCollection) dc).getInfo().getProperties().getProperty( DataCollectionConfigConstants.FILE_PATH_PROPERTY ) );
        return getProjectDirectory( dc.getName() );
    }


    private Pair<Integer, String> gitCommand(ProcessBuilder processBuilder) throws Exception
    {
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        StringBuilder processOutput = new StringBuilder();

        int exitCode = -1;
        try (BufferedReader processOutputReader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));)
        {
            String readLine;

            while ((readLine = processOutputReader.readLine()) != null)
            {
                if( !readLine.startsWith( "remote: " ) )
                {
                    readLine = TextUtil2.subst( readLine, "remote: ", "\remote: " );
                }
                processOutput.append(readLine + "\n");
            }

            exitCode = process.waitFor();

        }

        return new Pair<>( exitCode, processOutput.toString().trim() );
    }

    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        String bioumlUser = SecurityManager.getSessionUser();
        //log.info( "bioumlUser = " + bioumlUser );

        String action = arguments.getAction();
        DataElementPath path = arguments.getDataElementPath();
        DataElement de = path.getDataElement();
        //log.info( "DataElementPath = " + path );
        //log.info( "DataElement = " + de );
        //TODO: check git repository pattern, report invalid
        if( "isEnabled".equals( action ) )
        {
            if( de != null && de instanceof DataCollection )
            {
                DataCollection<?> dc = (DataCollection<?>) de;
                if( dc instanceof GitDataCollection || Boolean.valueOf( dc.getInfo().getProperties().getProperty( GitConstants.GIT_ENABLED_PROPERTY, "false" ) ) )
                {
                    ProcessBuilder processBuilder = new ProcessBuilder().command( "git", "--version" );
                    Pair<Integer, String> gitOut = gitCommand( processBuilder );
                    if( gitOut != null && gitOut.getSecond().startsWith( "git version" ) )
                    {
                        response.sendString( "ok" );
                        return;
                    }
                }
            }
            response.sendString( "false" );
            return;
        }
        else if( "clone".equals( action ) )
        {
            DataCollection<?> dc = (DataCollection<?>) de;

            String repository = arguments.get( "repository" );
            String username = arguments.get( "username" );
            String password = arguments.get( "password" );

            GitUrlParser parsedUrl = null;
            try
            {
                parsedUrl = GitUrlParser.parse( repository );
            }
            catch (IllegalArgumentException e)
            {
                response.error( "Error parsing git url: " + e.getMessage() );
                return;
            }

            String branch = arguments.getOrDefault( "branch", "" );
            username = TextUtil2.encodeURL( username );
            password = TextUtil2.encodeURL( password );

            ValidationResult result = checkGitCredentials( parsedUrl, username, password );
            if( !result.status.equals( TokenStatus.VALID ) )
            {
                response.error( "Git validation failed: " + result.message );
                return;
            }

            String fullUrl = parsedUrl.toHttpsUrl();//createGitRepoUrl( repository, username, password );
            String fullUrlWithCredentials = parsedUrl.toHttpsUrlWithCredentials( username, password );
 
            //TODO: !!!!! use credential helper for cloning
            String command = "git clone " + fullUrlWithCredentials;
            log.info( "\ncommand = " + command );

            try
            { 
                TempFileManager tempFileManager = TempFileManager.getDefaultManager();
                File tmpGitFolder = tempFileManager.dir( "git" );
                String branchSuffix = "";

                List<String> commands = new ArrayList<>( Arrays.asList( new String[] { "git", "clone" } ) );
                if( !branch.isEmpty() )
                {
                    commands.add( "-b" + TextUtil2.encodeURL( branch ) );
                    commands.add( "--single-branch" );
                    branchSuffix = " (" + branch + ")";
                }
                commands.add( "--progress" );
                commands.add( fullUrlWithCredentials );

                ProcessBuilder processBuilder = new ProcessBuilder()
                    .directory( tmpGitFolder )
                        .command( commands.toArray( new String[] {} ) );
                //TODO: credential helper should work but it does not
                //configureCredentialHelper( username, password, fullUrl, workDir );

                Pair<Integer, String> gitOut = gitCommand( processBuilder );
                String gitOutStr = gitOut.getSecond();

                //clearCredentials( fullUrl );
                String prjName = parsedUrl.getRepo() + branchSuffix;
                String projectAltName = arguments.getOrDefault( "projectAltName", "" );
                if( projectAltName.isEmpty() )
                    projectAltName = TextUtil2.decodeURL( username ) + "@" + prjName;

                if( dc.contains( prjName ) )
                    prjName = projectAltName;

                boolean bCloneSuccess = gitOut.getFirst() == 0;//gitOut.endsWith( "done." );   
                boolean bProjectSuccess = false;   

                List<String> setupErrors = new ArrayList<>();
                List<String> setupMessages = new ArrayList<>();

                if( bCloneSuccess )
                { 

                    ArrayList<String> errors = new ArrayList<>();
                    Properties properties = new Properties();
                    properties.put( DataCollectionConfigConstants.CLASS_PROPERTY, GitDataCollection.class.getName() );
                    //TODO: what do we need here
                    properties.setProperty( GitConstants.GIT_REPOSITORY_PROPERTY, repository );
                    if( !branch.isEmpty() )
                        properties.setProperty( GitConstants.GIT_BRANCH_PROPERTY, branch );

                    DataCollection<?> project = SupportServlet.createNewProject( prjName, path, bioumlUser, true, errors, ProjectType.FILE, properties );

                    if( project == null )
                    {
                        response.error( errors.size() > 0 ? errors.get( 0 ) : "Failed to create project: reason unknown" );
                    }
                    else
                    {
                        DataCollection primaryProject = project;
                        if( project instanceof FilteredDataCollection )
                            primaryProject = ((FilteredDataCollection<?>) project).getPrimaryCollection();
                        File prjDir = getProjectDirectory( primaryProject );
                        log.info( "\nprjDir = " + prjDir );

                        // skip directory created by git
                        File gitDir = null;
                        for( File file : tmpGitFolder.listFiles() )
                        {
                            gitDir = file;
                            break;
                        } 
                         
                        for( File file : gitDir.listFiles() )
                        {
                            String fileName = file.getName();
                            //log.info( "\npull: got filename = " + fileName );
                            if( file.isDirectory() )
                            {
                                ApplicationUtils.copyFolder( new File( prjDir, file.getName() ), file );
                                continue;
                            }                    
                            if( ".git".equals( fileName ) )
                            {
                                continue;
                            }
//                            if( fileName.endsWith( ".config" ) || fileName.endsWith( ".dat" ) || fileName.endsWith( ".dat.id" ) )
//                            {
//                                continue;
//                            }
                            if( project.contains( fileName ) )
                            {
                                //log.info( "!!!!file '" + fileName + "' already exists!" );
                                continue;
                            }
                            ApplicationUtils.copyFile( new File( prjDir, file.getName() ), file );
                        }

                        File gitConfDir = new File( gitDir, ".git" );
                        if( new File( prjDir, ".git" ).exists() )
                        {
                            Files.walk( new File( prjDir, ".git" ).toPath() )
                                 .sorted( Comparator.reverseOrder() )
                                 .map( Path::toFile )
                                 .forEach( File::delete );
                        }

                        Process mv = Runtime.getRuntime().exec( new String[] { "mv", gitConfDir.getCanonicalPath(), prjDir.getCanonicalPath() } );
                        mv.waitFor();


                        //TODO: why do we change credentials? git user is different from biouml user
                        processBuilder = new ProcessBuilder()
                                .directory( prjDir )
                            .command( "git", "config", "user.email", bioumlUser );

                        Pair<Integer, String> gitOut1 = gitCommand( processBuilder );

                        processBuilder = new ProcessBuilder()
                                .directory( prjDir )
                            .command( "git", "config", "user.name", bioumlUser );

                        Pair<Integer, String> gitOut2 = gitCommand( processBuilder );


                        //change url to one without user and token
                        runSetupCommand( prjDir, setupMessages, setupErrors, "git", "remote", "set-url", "origin", fullUrl );
                        //set credential helper (not working)
                        runSetupCommand( prjDir, setupMessages, setupErrors, "git", "config", "credential.helper", "cache" );
                        //set remote branch automatically for git push
                        runSetupCommand( prjDir, setupMessages, setupErrors, "git", "config", "push.autoSetupRemote", "true" );

                        gitOutStr += gitOut1.getSecond() + gitOut2.getSecond();

                        if( primaryProject instanceof GitDataCollection )
                            ((GitDataCollection) primaryProject).initGitFilter();

                        bProjectSuccess = true;
                    }
                }   

                if( !setupErrors.isEmpty() )
                {
                    gitOutStr += "Errors during project setup:\n" + StringUtils.join( setupErrors, "\n" );
                }

                if( bCloneSuccess && bProjectSuccess )
                { 
                    response.sendStringArray( prjName, gitOutStr );
                }
                else if( !bCloneSuccess )
                {
                    response.error( gitOutStr );
                } 
            }
            catch( Exception exc )
            {
                log.log( Level.SEVERE, "Error in " + path.getName(), exc ); 
                String errorMessage = "";
                //errorMessage += "<font color=\"red\">" + exc.getMessage() + "</font><br />\n";
                errorMessage += exc.getMessage() + "\n";
                response.error( errorMessage );
            } 

            return;
        }
        else if( "pull".equals( action ) )
        {
            try
            { 
                GitDataCollection project = ( GitDataCollection )de;

                File prjDir = getProjectDirectory( project );
                String url = null;
                List<String> lines = Files.readAllLines( new File( new File( prjDir, ".git" ), "config" ).toPath() );
                for( String line : lines )
                {
                    if( line.indexOf( "url = " ) > 0 )
                    {                        
                        url = TextUtil2.subst( line, "url = ", "" ).trim();
                        break;
                    }
                }

                if( url == null )
                {
                    log.log( Level.SEVERE, "pull: url not found in git config, lines=\n" + lines ); 
                }
                else
                {
                    log.info( "\npull: found repository url = " + url ); 
                }

                ProcessBuilder processBuilder = new ProcessBuilder()
                        .directory( prjDir )
                    .command( "git", "pull", "--ff-only" );

                String gitOutStr = gitCommand( processBuilder ).getSecond();

                response.sendStringArray( path.getName(), gitOutStr );
                return;
            }
            catch( Exception exc )
            {
                log.log( Level.SEVERE, "Error in " + path.getName(), exc ); 
                String errorMessage = "";
                //errorMessage += "<font color=\"red\">" + exc.getMessage() + "</font><br />\n";
                errorMessage += exc.getMessage() + "\n";
                response.error( errorMessage );
            } 
            return;
        }
        else if( "commit".equals( action ) )
        {
            GitDataCollection project = (GitDataCollection) de;
            String message = arguments.get( "message" );
            try
            { 
                File workDir = getProjectDirectory( project );

                ProcessBuilder processBuilder = new ProcessBuilder()
                    .directory( workDir )
                    .command( "git", "add", /*"--progress",*/ "." );

                Pair<Integer, String> gitOut1 = gitCommand( processBuilder );

                processBuilder = new ProcessBuilder()
                    .directory( workDir )
                    .command( "git", "commit", /*"--progress",*/ "-a", "-m", message );

                Pair<Integer, String> gitOut2 = gitCommand( processBuilder );

                response.sendStringArray( path.getName(), gitOut1.getSecond() + gitOut2.getSecond() );
                return;
            }
            catch( Exception exc )
            {
                log.log( Level.SEVERE, "Error in " + path.getName(), exc ); 
                String errorMessage = "";
                //errorMessage += "<font color=\"red\">" + exc.getMessage() + "</font><br />\n";
                errorMessage += exc.getMessage() + "\n";
                response.error( errorMessage );
            } 
        }
        else if( "push".equals( action ) )
        {
            try
            { 
                String username = arguments.get( "username" );
                String password = arguments.get( "password" );

                username = TextUtil2.encodeURL( username );
                password = TextUtil2.encodeURL( password );

                GitDataCollection project = (GitDataCollection) de;
                String repository = getRepositoryPath( project );
                File workDir = getProjectDirectory( project );

                GitUrlParser parsedUrl = null;
                try
                {
                    parsedUrl = GitUrlParser.parse( repository );
                }
                catch (IllegalArgumentException e)
                {
                    response.error( "Error parsing git url: " + e.getMessage() );
                    return;
                }
                
                String urlWithCredentials = parsedUrl.toHttpsUrlWithCredentials( username, password );

                //TODO: credential helper should work but it does not
                //TODO: git push with url doesn't update origin reference, so git status will show "1 commit ahead". git pull will fix the situation.
                //configureCredentialHelper( username, password, repository, workDir );
                ProcessBuilder processBuilder = new ProcessBuilder()
                    .directory( workDir )
                        .command( "git", "push", urlWithCredentials );

                Pair<Integer, String> gitOut1 = gitCommand( processBuilder );
                //clearCredentials( repository );

                response.sendStringArray( path.getName(), gitOut1.getSecond() );
                return;
            }
            catch( Exception exc )
            {
                log.log( Level.SEVERE, "Error in " + path.getName(), exc ); 
                String errorMessage = "";
                //errorMessage += "<font color=\"red\">" + exc.getMessage() + "</font><br />\n";
                errorMessage += exc.getMessage() + "\n";
                response.error( errorMessage );
            } 
            return;
        }

        else if( "status".equals( action ) )
        {
            try
            {
                GitDataCollection project = (GitDataCollection) de;
                File workDir = getProjectDirectory( project );

                ProcessBuilder processBuilder = new ProcessBuilder().directory( workDir ).command( "git", "status" );

                Pair<Integer, String> gitOut1 = gitCommand( processBuilder );

                response.sendString( gitOut1.getSecond() );
                return;
            }
            catch (Exception exc)
            {
                log.log( Level.SEVERE, "Error in " + path.getName(), exc );
                String errorMessage = "";
                //errorMessage += "<font color=\"red\">" + exc.getMessage() + "</font><br />\n";
                errorMessage += exc.getMessage() + "\n";
                response.error( errorMessage );
            }
            return;
        }
        else if( "console".equals( action ) )
        {
            try
            {
                GitDataCollection project = (GitDataCollection) de;
                File workDir = getProjectDirectory( project );
                String command = arguments.getString( "command" );
                String checkResult = checkGitCommand( command );
                if( checkResult != null )
                {
                    response.error( checkResult );
                    return;
                }
                List<String> list = new ArrayList<String>();
                Matcher m = Pattern.compile( "([^\"]\\S*|\".+?\")\\s*" ).matcher( command );
                while ( m.find() )
                    list.add( m.group( 1 ).replace( "\"", "" ) ); // Add .replace("\"", "") to remove surrounding quotes.

                String[] commandSplit = list.toArray( new String[] {} );//.split( "\\s+" );
                ProcessBuilder processBuilder = new ProcessBuilder().directory( workDir ).command( commandSplit );
                Pair<Integer, String> gitOut1 = gitCommand( processBuilder );
                if( gitOut1.getFirst() != 0 )
                    response.error( gitOut1.getSecond() );
                else
                    response.sendString( gitOut1.getSecond() );
                return;
            }
            catch (Exception exc)
            {
                log.log( Level.SEVERE, "Error in " + path.getName(), exc );
                String errorMessage = "";
                //errorMessage += "<font color=\"red\">" + exc.getMessage() + "</font><br />\n";
                errorMessage += exc.getMessage() + "\n";
                response.error( errorMessage );
            }
            return;
        }
    }

    private void runSetupCommand(File workDir, List<String> okMessages, List<String> errorMessages, String... command)
    {
        ProcessBuilder processBuilder = new ProcessBuilder().directory( workDir ).command( command );
        try
        {
            Pair<Integer, String> gitOut = gitCommand( processBuilder );
            if( gitOut.getFirst() == 0 )
                okMessages.add( gitOut.getSecond() );
            else
                errorMessages.add( gitOut.getSecond() );
        }
        catch (Exception e)
        {
            errorMessages.add( e.getMessage() );
        }
    }

    private void configureCredentialHelper(String username, String token, String url, File workDir) throws IOException, InterruptedException
    {
        ProcessBuilder pb = new ProcessBuilder( "git", "credential", "approve" );
        pb.redirectErrorStream( true );
        pb.directory( workDir );
        Process process = pb.start();

        try (OutputStream os = process.getOutputStream())
        {
            StringBuilder sb = new StringBuilder();
            if( url != null )
            {
                sb.append( "url=" ).append( url ).append( "\n" );
            }
            if( username != null )
            {
                sb.append( "username=" ).append( username ).append( "\n" );
            }
            if( token != null )
            {
                sb.append( "password=" ).append( token ).append( "\n" );
            }
            sb.append( "\n" );
            os.write( sb.toString().getBytes( StandardCharsets.UTF_8 ) );
            os.flush();
        }

        int exitCode = process.waitFor();
        String output = readProcessOutput( process.getInputStream() );
        log.info( "git credential approve: " + output );
    }

    private static void clearCredentials(String url) throws IOException, InterruptedException
    {
        ProcessBuilder pb = new ProcessBuilder( "git", "credential", "reject" );
        pb.redirectErrorStream( true );
        Process process = pb.start();

        try (OutputStream os = process.getOutputStream())
        {
            os.write( ("url=" + url + "\n").getBytes() );
            os.write( "\n".getBytes() );
            os.flush();
        }

        int exitCode = process.waitFor();
        String output = readProcessOutput( process.getInputStream() );
        log.info( "git credential reject: " + output );
    }

    private static void checkCredentials(String url) throws IOException, InterruptedException
    {
        ProcessBuilder pb = new ProcessBuilder( "git", "credential", "fill" );
        pb.redirectErrorStream( true );
        Process process = pb.start();

        try (OutputStream os = process.getOutputStream())
        {
            os.write( ("url=" + url + "\n").getBytes() );
            os.write( "\n".getBytes() );
            os.flush();
        }

        int exitCode = process.waitFor();
        String output = readProcessOutput( process.getInputStream() );
        log.info( "git cred check: " + output );
    }

    private static String readProcessOutput(InputStream is) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader( new InputStreamReader( is, StandardCharsets.UTF_8 ) ))
        {
            String line;
            while ( (line = reader.readLine()) != null )
            {
                sb.append( line ).append( "\n" );
            }
        }
        return sb.toString();
    }


    private String checkGitCommand(String command)
    {
        if( !command.startsWith( "git " ) )
            return "Command should start with 'git'";
        if( command.contains( "|" ) )
            return "Command contains not allowed symbols";
        if( command.contains( "--output" ) )
            return "'output' argument is not allowed";
        String[] commandSplit = command.split( "\\s+" );
        if( commandSplit.length > 1 && commandSplit[1].equalsIgnoreCase( "clone" ) )
            return "'clone' operation can not be done with console. Please, use tree action instead.";
        return null;
    }

    /**
     * Verify credentials by running git ls-remote
     * 
     * @param parsedUrl
     * @param username
     * @param password
     * @return
     * @throws IOException
     */
    private ValidationResult checkGitCredentials(GitUrlParser parsedUrl, String username, String token) throws IOException
    {
        String host = parsedUrl.getHost();
        String owner = parsedUrl.getOwner();
        String repo = parsedUrl.getRepo();

        String fullUrl = parsedUrl.toHttpsUrlWithCredentials( username, token );
        String noCredentialsUrl = parsedUrl.toHttpsUrlWithoutCredentials();

        ProcessBuilder processBuilder = new ProcessBuilder().command( "git", "ls-remote", fullUrl );
        try
        {
            Pair<Integer, String> gitOut = gitCommand( processBuilder );
            if( gitOut.getFirst() == 0 )
            {
                return new ValidationResult( TokenStatus.VALID, PermissionLevel.READ, "Token validated successfully", owner, repo, host );
            }
            else
            {
                String errorMessage = interpretGitError( gitOut.getSecond(), noCredentialsUrl );
                return new ValidationResult( TokenStatus.ERROR, PermissionLevel.NONE, errorMessage, owner, repo, host );
            }
        }
        catch (Exception e)
        {
            return new ValidationResult( TokenStatus.ERROR, PermissionLevel.NONE, "Validation failed: " + e.getMessage(), owner, repo, host );
        }
    }

    private static String interpretGitError(String errorOutput, String repoUrl)
    {
        String errorLC = errorOutput.toLowerCase();

        if (errorLC.contains("authentication failed") || errorLC.contains("invalid credentials") || errorLC.contains("401")) {
            return "Authentication failed. Please check your username and password/token.";
        }
        if (errorLC.contains("repository not found") || errorLC.contains("not found") || errorLC.contains("404")) {
            return "Repository not found. Please verify the repository URL: " + repoUrl;
        }
        if (errorLC.contains("could not resolve hostname") || errorLC.contains("name resolution failed")) {
            return "Cannot resolve server hostname. Please check your network connection and server URL.";
        }
        if (errorLC.contains("connection timed out") || errorLC.contains("connection reset")) {
            return "Connection timed out. The server may be down or unreachable.";
        }
        if (errorLC.contains("permission denied") || errorLC.contains("403")) {
            return "Access denied. Your account may not have permission to access this repository.";
        }
        if (errorLC.contains("ssl certificate problem") || errorLC.contains("certificate verify failed")) {
            return "SSL certificate verification failed. The server's certificate may be self-signed or expired.";
        }
        if (errorLC.contains("fatal:")) {
            return "Git error: " + errorOutput.trim().replaceAll("fatal:\\s*", "");
        }

        return "Unknown error occurred. Please check your network and repository settings.";
    }

    public enum TokenStatus
    {
        VALID, INVALID, EXPIRED, NO_ACCESS, ERROR
    }

    public enum PermissionLevel
    {
        NONE, READ, WRITE, ADMIN
    }

    public static class ValidationResult
    {
        public final TokenStatus status;
        public final PermissionLevel permission;
        public final String message;
        public final String owner;
        public final String repo;
        public final String host;

        public ValidationResult(TokenStatus status, PermissionLevel permission, String message, String owner, String repo, String host)
        {
            this.status = status;
            this.permission = permission;
            this.message = message;
            this.owner = owner;
            this.repo = repo;
            this.host = host;
        }

        @Override
        public String toString()
        {
            return String.format( "ValidationResult{status=%s, permission=%s, message='%s', repo='%s/%s'}", status, permission, message, owner, repo );
        }
    }

    private String getRepositoryPath(GitDataCollection project) throws Exception
    {
        if( project.getInfo().getProperty( GitConstants.GIT_REPOSITORY_PROPERTY ) != null )
        {
            return project.getInfo().getProperty( GitConstants.GIT_REPOSITORY_PROPERTY );
        }
        else
        {
            //Get repo name by git process
            File workDir = getProjectDirectory( project );
            ProcessBuilder processBuilder = new ProcessBuilder().directory( workDir ).command( "git", "config", "--get", "remote.origin.url" );
            Pair<Integer, String> gitOut = gitCommand( processBuilder );

            return gitOut.getSecond();
        }

    }

    /**
     * Parses Git repository URLs (HTTPS, SSH, HTTP, Git protocol) and extracts components: protocol, host, owner, repo,
     * etc.
     */
    public static class GitUrlParser
    {

        // Patterns for different URL formats
        private static final Pattern HTTPS_PATTERN = Pattern.compile( "^(https?)://(?:([^@:]+)@)?([^/:]+)(?::(\\d+))?/(.+?)(?:\\.git)?/?$" );

        private static final Pattern SSH_PATTERN = Pattern.compile( "^git@([^:]+):(.+?)(?:\\.git)?/?$" );

        private static final Pattern GIT_PATTERN = Pattern.compile( "^git://([^/:]+)(?::(\\d+))?/(.+?)(?:\\.git)?/?$" );

        private final String protocol;
        private final String username;      // Optional: user@host
        private final String host;
        private final Integer port;         // Optional: custom port
        private final String owner;         // Organization or user
        private final String repo;          // Repository name
        private final String path;          // Full path: owner/repo or org/team/repo
        private final boolean hasGitSuffix;

        /**
         * Private constructor - use parse() method instead
         */
        private GitUrlParser(String protocol, String username, String host, Integer port, String owner, String repo, String path, boolean hasGitSuffix)
        {
            this.protocol = protocol;
            this.username = username;
            this.host = host;
            this.port = port;
            this.owner = owner;
            this.repo = repo;
            this.path = path;
            this.hasGitSuffix = hasGitSuffix;
        }

        // Getters
        public String getProtocol()
        {
            return protocol;
        }

        public String getUsername()
        {
            return username;
        }

        public String getHost()
        {
            return host;
        }

        public Integer getPort()
        {
            return port;
        }

        public String getOwner()
        {
            return owner;
        }

        public String getRepo()
        {
            return repo;
        }

        public String getPath()
        {
            return path;
        }

        public boolean hasGitSuffix()
        {
            return hasGitSuffix;
        }

        /**
         * Get full repository identifier: owner/repo
         */
        public String getRepository()
        {
            return owner + "/" + repo;
        }

        /**
         * Reconstruct HTTPS URL from parsed components
         */
        public String toHttpsUrl()
        {
            StringBuilder sb = new StringBuilder( "https://" );
            if( username != null )
            {
                sb.append( username ).append( "@" );
            }
            sb.append( host );
            if( port != null )
            {
                sb.append( ":" ).append( port );
            }
            sb.append( "/" ).append( path );
            if( hasGitSuffix )
            {
                sb.append( ".git" );
            }
            return sb.toString();
        }

        public String toHttpsUrlWithCredentials(String user, String token)
        {
            StringBuilder sb = new StringBuilder( "https://" );
            sb.append( user ).append( ":" ).append( token ).append( "@" );
            sb.append( host );
            if( port != null )
            {
                sb.append( ":" ).append( port );
            }
            sb.append( "/" ).append( path );
            if( hasGitSuffix )
            {
                sb.append( ".git" );
            }
            return sb.toString();
        }

        public String toHttpsUrlWithoutCredentials()
        {
            StringBuilder sb = new StringBuilder( "https://" );
            sb.append( host );
            if( port != null )
            {
                sb.append( ":" ).append( port );
            }
            sb.append( "/" ).append( path );
            if( hasGitSuffix )
            {
                sb.append( ".git" );
            }
            return sb.toString();
        }

        /**
         * Reconstruct SSH URL from parsed components
         */
        public String toSshUrl()
        {
            return "git@" + host + ":" + path + (hasGitSuffix ? ".git" : "");
        }

        @Override
        public String toString()
        {
            return String.format( "GitUrl{protocol=%s, host=%s, owner=%s, repo=%s}", protocol, host, owner, repo );
        }

        @Override
        public boolean equals(Object o)
        {
            if( this == o )
                return true;
            if( !(o instanceof GitUrlParser) )
                return false;
            GitUrlParser that = (GitUrlParser) o;
            return hasGitSuffix == that.hasGitSuffix && Objects.equals( protocol, that.protocol ) && Objects.equals( username, that.username ) && Objects.equals( host, that.host )
                    && Objects.equals( port, that.port ) && Objects.equals( owner, that.owner ) && Objects.equals( repo, that.repo ) && Objects.equals( path, that.path );
        }

        @Override
        public int hashCode()
        {
            return Objects.hash( protocol, username, host, port, owner, repo, path, hasGitSuffix );
        }

        /**
         * Parse a Git repository URL
         * 
         * @param url Git URL (HTTPS, SSH, HTTP, or Git protocol)
         * @return Parsed GitUrlParser instance
         * @throws IllegalArgumentException if URL format is not recognized
         */
        public static GitUrlParser parse(String url)
        {
            if( url == null || url.trim().isEmpty() )
            {
                throw new IllegalArgumentException( "URL cannot be empty" );
            }

            url = url.trim();

            // Try HTTPS/HTTP pattern
            Matcher httpsMatcher = HTTPS_PATTERN.matcher( url );
            if( httpsMatcher.matches() )
            {
                return parseHttpsPattern( httpsMatcher, url );
            }

            // Try SSH pattern
            Matcher sshMatcher = SSH_PATTERN.matcher( url );
            if( sshMatcher.matches() )
            {
                return parseSshPattern( sshMatcher, url );
            }

            // Try Git protocol pattern
            Matcher gitMatcher = GIT_PATTERN.matcher( url );
            if( gitMatcher.matches() )
            {
                return parseGitPattern( gitMatcher, url );
            }

            throw new IllegalArgumentException( "Unrecognized Git URL format: " + url );
        }

        private static GitUrlParser parseHttpsPattern(Matcher matcher, String originalUrl)
        {
            String protocol = matcher.group( 1 );
            String username = matcher.group( 2 );
            String host = matcher.group( 3 );
            String portStr = matcher.group( 4 );
            String path = matcher.group( 5 );

            Integer port = portStr != null ? Integer.parseInt( portStr ) : null;
            boolean hasGitSuffix = originalUrl.endsWith( ".git" ) || originalUrl.endsWith( ".git/" );

            String[] pathParts = path.split( "/", 2 );
            if( pathParts.length < 2 )
            {
                throw new IllegalArgumentException( "Invalid repository path: " + path );
            }

            String owner = pathParts[0];
            String repo = pathParts[1].replaceAll( "/+$", "" ); // Remove trailing slashes

            return new GitUrlParser( protocol, username, host, port, owner, repo, path, hasGitSuffix );
        }

        private static GitUrlParser parseSshPattern(Matcher matcher, String originalUrl)
        {
            String host = matcher.group( 1 );
            String path = matcher.group( 2 );
            boolean hasGitSuffix = originalUrl.endsWith( ".git" ) || originalUrl.endsWith( ".git/" );

            String[] pathParts = path.split( "/", 2 );
            if( pathParts.length < 2 )
            {
                throw new IllegalArgumentException( "Invalid repository path: " + path );
            }

            String owner = pathParts[0];
            String repo = pathParts[1].replaceAll( "/+$", "" );

            return new GitUrlParser( "ssh", null, host, null, owner, repo, path, hasGitSuffix );
        }

        private static GitUrlParser parseGitPattern(Matcher matcher, String originalUrl)
        {
            String host = matcher.group( 1 );
            String portStr = matcher.group( 2 );
            String path = matcher.group( 3 );
            boolean hasGitSuffix = originalUrl.endsWith( ".git" ) || originalUrl.endsWith( ".git/" );

            Integer port = portStr != null ? Integer.parseInt( portStr ) : null;

            String[] pathParts = path.split( "/", 2 );
            if( pathParts.length < 2 )
            {
                throw new IllegalArgumentException( "Invalid repository path: " + path );
            }

            String owner = pathParts[0];
            String repo = pathParts[1].replaceAll( "/+$", "" );

            return new GitUrlParser( "git", null, host, port, owner, repo, path, hasGitSuffix );
        }
    }
}
