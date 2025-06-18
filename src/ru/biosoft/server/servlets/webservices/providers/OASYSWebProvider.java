package ru.biosoft.server.servlets.webservices.providers;

import java.io.InputStream;
import java.io.IOException;
import java.io.File;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.PosixFileAttributeView;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.CollectionFactoryUtils;

import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.WebSession;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Volume;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.CreateNetworkResponse;
import com.github.dockerjava.api.command.LogContainerCmd;

import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;

import ru.biosoft.util.TempFileManager;
import ru.biosoft.util.TextUtil2;

import ru.biosoft.workbench.Framework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.logging.Level;
import java.util.logging.Logger;

public class OASYSWebProvider extends WebJSONProviderSupport
{
    protected static final Logger log = Logger.getLogger( OASYSWebProvider.class.getName() );

    private File getFileLocation( String path ) throws IOException
    {
        if( System.getProperty( "DOCKER_BIOUML_RESOURCES" ) == null )
        {
            return null;  
        }

        String basePath = TextUtil2.subst( path, "data/", "" );
        int ind = basePath.lastIndexOf( "/" );
        basePath = basePath.substring( 0, ind ) + "/file_collection.files" + basePath.substring( ind );
        String hostPath = "" + System.getProperty( "DOCKER_BIOUML_RESOURCES" ) + "/" + basePath;

        for( String repo : Framework.getRepositoryPaths() )
        {
            File file = new File( repo + "/" + basePath );
            if( file.exists() )
            {
                Path path_ = file.toPath();
                Files.setPosixFilePermissions( path_, PosixFilePermissions.fromString( "rw-rw-rw-" ) );
                break;
            }
        }

        return new File( hostPath );
    }


    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        String action = arguments.getAction();
        if( "start".equals( action ) )
        {
            DataElementPath path = arguments.getDataElementPath();
            log.info( "start = " + path );
            File file = getFileLocation( path.toString() ); 
            log.info( "file = " + file );
            DataElement de = path.getDataElement();
            if( de == null )
            {
                response.error( "Element not found " + path );
                return;
            }

            String data = arguments.getOrDefault( "data", "" );
            String urlPrefix = arguments.getOrDefault( "urlPrefix", "http://bioumlweb:8080" );

            DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost( "unix:///var/run/docker.sock" )
                .build();

            DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost( config.getDockerHost() )
                .build();

            DockerClient dockerClient = DockerClientImpl.getInstance( config, httpClient );

            String displayNum = "64";
            String uniqId = "-" + System.currentTimeMillis(); 

            String networkName = System.getProperty( "DOCKER_NETWORK_NAME" );

            if( networkName == null )
            {
                CreateNetworkResponse networkResponse = dockerClient.createNetworkCmd()
                    .withName( networkName = "oasys-network" + uniqId )
                    .withDriver("bridge")
                    .withAttachable(true)
                    .exec();
            }

            List<Integer> takenPorts = new ArrayList<>();
            for( Container c : dockerClient.listContainersCmd().exec() )
            {
                for( ContainerPort p : c.getPorts() )
                {
                    if( p.getPublicPort() != null )
                    { 
                        takenPorts.add( p.getPublicPort() );
                    } 
                }
            }

            log.info( "taken ports = " + takenPorts );
           
            for( int port = Integer.parseInt( displayNum ); ; port++ )
            {
                Integer testPort = Integer.parseInt( "71" + port );
                if( takenPorts.contains( testPort ) )
                {
                    continue; 
                }

                displayNum = "" + port;
                break;
            }

            HostConfig hostConfigXpra = HostConfig.newHostConfig()
               .withPortBindings( PortBinding.parse( "71" + displayNum + ":10000" ) )
               .withNetworkMode( networkName );

            CreateContainerResponse xpra = dockerClient.createContainerCmd( "biouml/tools:xpra-html5" )
                .withName( "xpra-html5" + uniqId ) 
                .withHostName( "xpra-html5" + uniqId ) 
                .withHostConfig( hostConfigXpra ) 
                .withCmd( "xpra", "start", "--bind-tcp=0.0.0.0:10000", "--html=on", "--daemon=no", "--xvfb=/usr/bin/Xvfb  :" + displayNum + " -ac +extension Composite -screen 0 1920x1080x24+32 -listen tcp -noreset", "--pulseaudio=no", "--notifications=no", "--bell=no" )
                .exec();

            dockerClient.startContainerCmd( xpra.getId() ).exec(); 
            //log.info( "xpra = " + dockerClient.startContainerCmd( xpra.getId() ) );

            String url = urlPrefix + "/biouml/web/content/";
            url += path + "?sessionId=";
            url += WebSession.getCurrentSession().getSessionId();

            HostConfig hostConfigOasys = HostConfig.newHostConfig()
               .withNetworkMode( networkName );

            if( file != null )
            {
                //Volume workdir = new Volume("/workdir");
                //hostConfigOasys.withBinds( new Bind( file.getParent(), workdir ) );
                Volume editFile = new Volume( "/workdir/" + file.getName() );
                hostConfigOasys.withBinds( new Bind( file.getAbsolutePath(), editFile ) );
                url = "/workdir/" + file.getName();
            }

            CreateContainerResponse oasys = dockerClient.createContainerCmd( "biouml/tools:oasys" )
                .withName( "oasys" + uniqId ) 
                .withHostName( "oasys" + uniqId ) 
                .withHostConfig( hostConfigOasys ) 
                .withEnv( "QT_X11_NO_MITSHM=1", "DISPLAY=xpra-html5" + uniqId + ":" + displayNum )
                .withUser( "local" )                
                .withEntrypoint( "" )
                .withCmd( "/bin/bash", "-c", "while true; do     python -m oasys.canvas --no-update --no-splash --no-welcome " + url + ";     sleep 1; done" )
                .exec();

            dockerClient.startContainerCmd( oasys.getId() ).exec(); 
            //log.info( "oasys = " + dockerClient.startContainerCmd( oasys.getId() ) );

            LogContainerCmd logContainerCmd = dockerClient.logContainerCmd( xpra.getId() );
            logContainerCmd.withStdOut( true ).withStdErr( true );
           
            final ArrayList<Boolean> bXpraIsready = new ArrayList<>();
            bXpraIsready.add( Boolean.FALSE );

            while( !bXpraIsready.get( 0 ) )
            {
                logContainerCmd.exec( new LogContainerResultCallback() 
                {
                    @Override
                    public void onNext( Frame item ) 
                    {
                        //log.info( item.toString() );
                        bXpraIsready.set( 0, item.toString().indexOf( "xpra is ready." ) != -1 );
                        if( bXpraIsready.get( 0 )  )
                        {
                            return;
                        }
                    }
                })/*.awaitCompletion()*/;
            }

            response.sendStringArray( displayNum, uniqId.substring( 1 ), urlPrefix );
        }
        else if( "stop".equals( action ) )
        {
            DataElementPath path = arguments.getDataElementPath();
            DataElement de = path.getDataElement();
            if( de == null )
            {
                response.error( "Element not found " + path );
                return;
            }

            String data = arguments.getOrDefault( "data", "" );
            String uniqueId = arguments.getOrDefault( "uniqueId", "" );
            
            log.info( "stop, uniqueId =  " + uniqueId );

            String uniqId = "-" + uniqueId; 

            DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost( "unix:///var/run/docker.sock" )
                .build();

            DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost( config.getDockerHost() )
                .build();

            DockerClient dockerClient = DockerClientImpl.getInstance( config, httpClient );
            for( Container c : dockerClient.listContainersCmd().exec() )
            {
                List<String> names = Arrays.asList( c.getNames() );
                log.info( "Found container: " + names );
                if( names.contains( "/xpra-html5" + uniqId ) || names.contains( "/oasys" + uniqId ) )
                {
                    dockerClient.removeContainerCmd( c.getId() ).withForce( true ).exec();
                    log.info( "Removing container: " + names );
                }
            }

            response.sendString( "ok" );
        }
    }
}
