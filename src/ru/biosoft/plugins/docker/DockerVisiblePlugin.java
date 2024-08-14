package ru.biosoft.plugins.docker;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.io.File;

import javax.annotation.Nonnull;

import org.json.JSONArray;
import org.json.JSONObject;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.plugins.VisiblePlugin;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
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

public class DockerVisiblePlugin extends VisiblePlugin<DataCollection>
{
    Properties properties;

    static DockerVisiblePlugin instance;
    public static DockerVisiblePlugin getInstance()
    {
        return instance;
    }

    Set<String> dockerPaths; 
    PathDataCollection backend;

    /**
     * Constructor to be used by {@link CollectionFactoryUtils} to create a Plugin.
     */
    public DockerVisiblePlugin(DataCollection parent, Properties properties)
    {
        super(parent, properties);
        instance = this;
    }

    @Override
    public void startup()
    {
    }

    @Override
    public @Nonnull Class<? extends DataCollection> getDataElementType()
    {
        return ru.biosoft.access.core.DataCollection.class;
    }


    @Override
    protected DataCollection doGet(String name) throws Exception
    {
        return backend != null ? ( DataCollection )backend.get( name ) : null;
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        if( dockerPaths == null )
        {
            dockerPaths = new HashSet<String>();

            if( new File( "/var/run/docker.sock" ).exists() )
            {
                DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .withDockerHost( "unix:///var/run/docker.sock" )
                    .build();

                DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                    .dockerHost( config.getDockerHost() )
                    .build();

                DockerClient dockerClient = DockerClientImpl.getInstance( config, httpClient );

                for( Image im : dockerClient.listImagesCmd().withLabelFilter( "biouml" ).exec() )
                {
                    JSONObject label = new JSONObject( im.getLabels().get( "biouml" ) );
                    if( !label.has( "cwls" ) )
                    {
                        continue; 
                    }

                    String imgName = null;
                    for( String tag: im.getRepoTags() )
                    {
                        imgName = tag;
                        if( imgName.endsWith( ":latest" ) )
                        {
                            imgName = imgName.substring( 0, imgName.length() - 7 );
                            break;
                        }
                    }

                    Object cwls = label.get( "cwls" );
                    Iterable list = cwls instanceof JSONArray ? ( JSONArray )cwls : ( ( JSONObject )cwls ).keySet();
                    for( Object cwl : list )
                    {
                        String cwlEntry = ( String )cwl;
                        if( !cwlEntry.startsWith( "/" ) )
                        {
                            cwlEntry = "/" + cwlEntry; 
                        }

                        if( backend == null )
                        {                    
                            backend = new PathDataCollection(
                                    getName(), getOrigin(), null /*properties*/, new HashSet() );
                        }

                        backend.addPath( "local/" + imgName + cwlEntry );
                    }                    
                    //System.out.println( "" + Arrays.asList( im.getRepoTags() ) );
                    //System.out.println( "" + im.getLabels() );
                }
            }

            Set<String> allPaths = SecurityManager.getCurrentUserPermission().getDbToPermission().keySet();
            for( String path : allPaths )
            { 
                //System.out.println( getName() + ": " + path );    
                String myPath = "analyses/" + getName() + "/";
                if( path.startsWith( myPath ) )
                { 
                    if( backend == null )
                    {                    
                        backend = new PathDataCollection( 
                                getName(), getOrigin(), null /*properties*/, new HashSet() );
                    }
                    backend.addPath( path.substring( myPath.length() ) );
                }     
            }
        }  

        if( backend != null )
        {
            backend.fill(); 
        }

        return backend != null ? backend.getNameList() : java.util.Collections.emptyList();
    }
}
