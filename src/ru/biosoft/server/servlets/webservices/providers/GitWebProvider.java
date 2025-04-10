package ru.biosoft.server.servlets.webservices.providers;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import java.nio.file.Files;
import java.nio.file.Path;
//import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;

import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;

import ru.biosoft.server.servlets.support.SupportServlet;

import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.NetworkDataCollection;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.TextFileImporter;

import ru.biosoft.util.TempFileManager;
import ru.biosoft.util.TextUtil2;

import ru.biosoft.workbench.Framework;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import java.util.logging.Level;
import java.util.logging.Logger;

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


    private String gitCommand( ProcessBuilder processBuilder ) throws Exception
    {
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        StringBuilder processOutput = new StringBuilder();

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

            process.waitFor();
        }

        return processOutput.toString().trim();
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
            if( "Collaboration".equals( path.getName() ) )
            {
                ProcessBuilder processBuilder = new ProcessBuilder().command( "git", "--version" );
                String gitOut = gitCommand( processBuilder );
                if( gitOut != null && gitOut.startsWith( "git version" ) )
                {
                    response.sendString( "ok" );
                    return;
                }
            }
            else
            {
                //log.info( "\n0 = " + path.getName() );
                File prjDir = getProjectDirectory( path.getName() );
                //log.info( "\n1 = " + prjDir );
                if( prjDir != null )
                {
                    File workDir = new File( new File( prjDir, "Data" ), "file_collection.files" );
                    //log.info( "\n2 = " + workDir );
                    if( new File( workDir, ".git" ).exists() )
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
            String repository = arguments.get( "repository" );
            String username = arguments.get( "username" );
            String password = arguments.get( "password" );

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

                ProcessBuilder processBuilder = new ProcessBuilder()
                    .directory( tmpGitFolder )
                    .command( "git", "clone", "--progress", fullUrl );

                String gitOut = gitCommand( processBuilder );

                String prjName = repository;
                prjName = TextUtil2.subst( prjName, "http://", "" );
                prjName = TextUtil2.subst( prjName, "https://", "" );
                prjName = TextUtil2.subst( prjName, "/", "_" );
                prjName = username + "@" + prjName;

                boolean bCloneSuccess = gitOut.endsWith( "done." );   
                boolean bProjectSuccess = false;   

                if( bCloneSuccess )
                { 
                    ArrayList<String> errors = new ArrayList<>();
                    DataCollection<?> project = SupportServlet.createNewProject( prjName, bioumlUser, true, errors );
                    if( project == null )
                    {
                        response.error( errors.size() > 0 ? errors.get( 0 ) : "Failed to create project: reason unknown" );
                    }
                    else
                    {
                        File prjDir = getProjectDirectory( prjName );
                        log.info( "\nprjDir = " + prjDir );
                        NetworkDataCollection Data = ( NetworkDataCollection )project.get( "Data" ); 
                        DataCollection<?> target = Data; // or project?

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
                            if( file.isDirectory() ) // for now
                            {
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
                            if( target.contains( fileName ) )
                            {
                                //log.info( "!!!!file '" + fileName + "' already exists!" );
                                continue;
                            }
                            new TextFileImporter().doImport( target, file, file.getName(), null, log );
                        }

                        File gitConfDir = new File( gitDir, ".git" );
                        File file_collection = new File( new File( prjDir, "Data" ), "file_collection.files" );
                        if( new File( file_collection, ".git" ).exists() )
                        {
                            Files.walk( new File( file_collection, ".git" ).toPath() )
                                 .sorted( Comparator.reverseOrder() )
                                 .map( Path::toFile )
                                 .forEach( File::delete );
                        }

                        Process mv = Runtime.getRuntime().exec( new String[] {"mv", gitConfDir.getCanonicalPath(), file_collection.getCanonicalPath() } );
                        mv.waitFor();

                        processBuilder = new ProcessBuilder()
                            .directory( file_collection )
                            .command( "git", "config", "user.email", bioumlUser );

                        String gitOut1 = gitCommand( processBuilder );

                        processBuilder = new ProcessBuilder()
                            .directory( file_collection )
                            .command( "git", "config", "user.name", bioumlUser );

                        String gitOut2 = gitCommand( processBuilder );

                        gitOut += gitOut1 + gitOut2;

                        //log.info( "\nData.getPrimaryCollection() = " + Data.getPrimaryCollection() );
                        bProjectSuccess = true;
                    }
                }   

                if( bCloneSuccess && bProjectSuccess )
                { 
                    response.sendStringArray( prjName, gitOut );
                }
                else if( !bCloneSuccess )
                {
                    response.error( gitOut );
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

                DataCollection<?> project = ( DataCollection<?> )de;
                NetworkDataCollection Data = ( NetworkDataCollection )project.get( "Data" ); 
                DataCollection<?> target = Data; // or project?

                TempFileManager tempFileManager = TempFileManager.getDefaultManager();
                File tmpGitFolder = tempFileManager.dir( "git" );

                File prjDir = getProjectDirectory( path.getName() );
                File workDir = new File( new File( prjDir, "Data" ), "file_collection.files" );
                String url = null;
                List<String> lines = Files.readAllLines( new File( new File( workDir, ".git" ), "config" ).toPath() );
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
                    .directory( tmpGitFolder )
                    .command( "git", "clone", /*"--progress",*/ url );

                String gitOut = gitCommand( processBuilder );

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
                    if( file.isDirectory() ) // for now
                    {
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
                    if( target.contains( fileName ) )
                    {
                        //log.info( "\npull: file '" + fileName + "' already exists!" );
                        continue;
                    }
                    new TextFileImporter().doImport( target, file, file.getName(), null, log );
                    processBuilder = new ProcessBuilder()
                        .directory( workDir )
                        .command( "git", "add", file.getName() );

                    gitOut += gitCommand( processBuilder );
                }

                processBuilder = new ProcessBuilder()
                    .directory( workDir )
                    .command( "git", "pull", "--ff-only" );

                gitOut += "\n" + gitCommand( processBuilder );

                response.sendStringArray( path.getName(), gitOut );
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
            String message = arguments.get( "message" );
            try
            { 
                File prjDir = getProjectDirectory( path.getName() );
                File workDir = new File( new File( prjDir, "Data" ), "file_collection.files" );

                ProcessBuilder processBuilder = new ProcessBuilder()
                    .directory( workDir )
                    .command( "git", "add", /*"--progress",*/ "." );

                String gitOut1 = gitCommand( processBuilder );

                processBuilder = new ProcessBuilder()
                    .directory( workDir )
                    .command( "git", "commit", /*"--progress",*/ "-a", "-m", message );

                String gitOut2 = gitCommand( processBuilder );

                response.sendStringArray( path.getName(), gitOut1 + gitOut2 );
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
                File prjDir = getProjectDirectory( path.getName() );
                File workDir = new File( new File( prjDir, "Data" ), "file_collection.files" );

                ProcessBuilder processBuilder = new ProcessBuilder()
                    .directory( workDir )
                    .command( "git", "push" );

                String gitOut1 = gitCommand( processBuilder );

                response.sendStringArray( path.getName(), gitOut1 );
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
