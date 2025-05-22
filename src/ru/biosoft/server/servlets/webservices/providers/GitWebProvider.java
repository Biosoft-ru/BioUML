package ru.biosoft.server.servlets.webservices.providers;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        for( String repo : Framework.getRepositoryPaths() )
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
        if( "isEnabled".equals( action ) )
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
            response.sendString( "false" );
            return;
        }
        else if( "clone".equals( action ) )
        {
            DataCollection<?> dc = (DataCollection<?>) de;

            String repository = arguments.get( "repository" );
            String username = arguments.get( "username" );
            String password = arguments.get( "password" );

            String branch = arguments.getOrDefault( "branch", "" );
            username = TextUtil2.encodeURL( username );
            password = TextUtil2.encodeURL( password );

            String fullUrl = repository;
            if( fullUrl.indexOf( "@" ) == -1 )
            {
                String repl = "";
                if( !"".equals( username ) )
                {
                     repl += username;
                     if( !"".equals( password ) )
                     {
                         repl += ":" + password;
                     }
                     repl += "@";
                }
                fullUrl = TextUtil2.subst( fullUrl, "https://", "https://" + repl );
                fullUrl = TextUtil2.subst( fullUrl, "http://", "http://" + repl );
            }
 
            String command = "git clone " + fullUrl;
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
                commands.add( fullUrl );

                ProcessBuilder processBuilder = new ProcessBuilder()
                    .directory( tmpGitFolder )
                        .command( commands.toArray( new String[] {} ) );

                Pair<Integer, String> gitOut = gitCommand( processBuilder );
                String gitOutStr = gitOut.getSecond();

                String prjName = repository;
                prjName = TextUtil2.subst( prjName, "http://", "" );
                prjName = TextUtil2.subst( prjName, "https://", "" );
                //prjName = TextUtil2.subst( prjName, "/", "_" );
                prjName = TextUtil2.subst( prjName, ".git", "" );
                prjName = DataElementPath.create( prjName ).getName() + branchSuffix;
                //prjName = username + "@" + prjName;
                String projectAltName = arguments.getOrDefault( "projectAltName", "" );
                if( projectAltName.isEmpty() )
                    projectAltName = TextUtil2.decodeURL( username ) + "@" + prjName + branchSuffix;

                if( dc.contains( prjName ) )
                    prjName = projectAltName;

                boolean bCloneSuccess = gitOut.getFirst() == 0;//gitOut.endsWith( "done." );   
                boolean bProjectSuccess = false;   

                if( bCloneSuccess )
                { 
                    ArrayList<String> errors = new ArrayList<>();
                    Properties properties = new Properties();
                    properties.put( DataCollectionConfigConstants.CLASS_PROPERTY, GitDataCollection.class.getName() );
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
                            if( fileName.endsWith( ".config" ) || fileName.endsWith( ".dat" ) || fileName.endsWith( ".dat.id" ) )
                            {
                                continue;
                            }
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

                        processBuilder = new ProcessBuilder()
                                .directory( prjDir )
                            .command( "git", "config", "user.email", bioumlUser );

                        Pair<Integer, String> gitOut1 = gitCommand( processBuilder );

                        processBuilder = new ProcessBuilder()
                                .directory( prjDir )
                            .command( "git", "config", "user.name", bioumlUser );

                        Pair<Integer, String> gitOut2 = gitCommand( processBuilder );

                        gitOutStr += gitOut1.getSecond() + gitOut2.getSecond();

                        if( primaryProject instanceof GitDataCollection )
                            ((GitDataCollection) primaryProject).initGitFilter();

                        bProjectSuccess = true;
                    }
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
                GitDataCollection project = (GitDataCollection) de;
                File workDir = getProjectDirectory( project );

                ProcessBuilder processBuilder = new ProcessBuilder()
                    .directory( workDir )
                    .command( "git", "push" );

                Pair<Integer, String> gitOut1 = gitCommand( processBuilder );

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
    }
}
