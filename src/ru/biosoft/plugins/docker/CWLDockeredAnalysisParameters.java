package ru.biosoft.plugins.docker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import ru.biosoft.access.core.DataElementPath;

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

import org.yaml.snakeyaml.Yaml;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

public class CWLDockeredAnalysisParameters extends AbstractAnalysisParameters
{
    DynamicPropertySet parameters = new DynamicPropertySetAsMap();
    DynamicPropertySet outputs = new DynamicPropertySetAsMap();

    String cwlFile;
    String dockerImage;
    DataElementPath cwlPath;

    public CWLDockeredAnalysisParameters()
    {
    }

    private DataElementPath outputFolder;

    @PropertyName("Result Folder")
    public DataElementPath getOutputFolder()
    {
        return outputFolder;
    }
    public void setOutputFolder(DataElementPath outputFolder)
    {
        this.outputFolder = outputFolder;
    }
    
    public DynamicPropertySet getParameters()
    {
        return parameters;
    }
    public void setParameters(DynamicPropertySet dps)
    {
        Object oldValue = this.parameters;
        this.parameters = dps;
        firePropertyChange("parameters", parameters, oldValue );
        firePropertyChange("*", null, null );
    }
    
    public DynamicPropertySet getOutputs()
    {
        return outputs;
    }
    public void setOutputs(DynamicPropertySet dps)
    {
        Object oldValue = this.outputs;
        this.outputs = dps;
        firePropertyChange("outputs", outputs, oldValue );
        firePropertyChange("*", null, null );
    }

    void extractParametersAndOutputs()
    {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost( "unix:///var/run/docker.sock" )
            .build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost( config.getDockerHost() )
            .build();

        DockerClient dockerClient = DockerClientImpl.getInstance( config, httpClient );

        String uniqId = "-" + System.currentTimeMillis(); 

        CreateContainerResponse cwlContainer = null; 

        try
        {           
            cwlContainer = dockerClient.createContainerCmd( dockerImage )
                .withName( cwlFile + uniqId ) 
                .withHostName( cwlFile + uniqId ) 
                .exec();

            //java.io.InputStream cwlStream = dockerClient.copyFileFromContainerCmd( cwlContainer.getId(), "/CWL/" + cwlFile ).exec();
            java.io.InputStream tarStream = dockerClient.copyArchiveFromContainerCmd( cwlContainer.getId(), "/CWL/" + cwlFile ).exec();

            try (TarArchiveInputStream tarInputStream = new TarArchiveInputStream(tarStream)) 
            {
                TarArchiveEntry nextTarEntry = tarInputStream.getNextTarEntry();
                Yaml parser = new Yaml();
                Map<String, Object> cwlYaml = (Map<String, Object>)parser.load( tarInputStream );

                Object inputsObj = cwlYaml.get("inputs");
                if( inputsObj instanceof Map )
                {
                    DynamicPropertySet parameterSet = new DynamicPropertySetAsMap();

                    Map<String, Object> inputs = (Map<String, Object>)inputsObj;
                    for( Entry<String, Object> e : inputs.entrySet() )
                    {
                        String type = null;
                        Object defaultValue = null;
                        if( e.getValue() instanceof Map )
                        {
                            Map<String, Object> value = (Map<String, Object>)e.getValue();
                            type = value.get("type").toString();
                            defaultValue = value.get("default");
                        }
                        else
                            type = e.getValue().toString();

                        boolean optional = false;
                        if( type.endsWith("?") )
                        {
                            optional = true;
                            type = type.substring(0, type.length() - 1);
                        }

                        Class clazz = type2Class.get(type);
                        Object value = defaultValue;
                        if( value == null )
                            value = type2Object.get(type);
                        DynamicProperty dp = new DynamicProperty(e.getKey(), clazz, value);
                        dp.setCanBeNull(optional);
                        parameterSet.add(dp);

                        //info.addParameter(e.getKey(), type, optional, defaultValue);
                    }

                    System.out.println( "parameterSet = " + parameterSet );
                    setParameters( parameterSet );
                }

                Object outputsObj = cwlYaml.get("outputs");
                if( outputsObj instanceof Map )
                {
                    DynamicPropertySet outputSet = new DynamicPropertySetAsMap();

                    Map<String, Object> outputs = (Map<String, Object>)outputsObj;
                    for( Entry<String, Object> e : outputs.entrySet() )
                    {
                        String type = null;
                        if( e.getValue() instanceof Map )
                        {
                            Map<String, Object> value = (Map<String, Object>)e.getValue();
                            type = value.get("type").toString();
                        }
                        else
                            type = e.getValue().toString();

                        Class clazz = type2Class.get(type);
                        Object value = type2Object.get(type);
                        DynamicProperty dp = new DynamicProperty(e.getKey(), clazz, value);        
                        dp.setCanBeNull(false);
                        outputSet.add(dp);

                        //info.addOutput(e.getKey(), type);
                    }

                    System.out.println( "outputSet = " + outputSet );
                    setOutputs( outputSet );
                }
            }
            catch( Exception exc )
            {
                throw new RuntimeException( exc ); 
            }
        }
        finally
        {
            if( cwlContainer != null )
            {
                dockerClient.removeContainerCmd( cwlContainer.getId() ).exec();
            } 
        } 
    }

    @PropertyName("CWL Path")
    public DataElementPath getCwlPath()
    {
        return cwlPath;
    }
    public void setCwlPath(DataElementPath cwlPath)
    {
        if (cwlPath == null)
            return;
        Object oldValue = this.cwlPath;
        
        String spath = "" + cwlPath; 
        if( spath.startsWith( "analyses/Docker/local/" ) )
        {
            spath = spath.substring( "analyses/Docker/local/".length() );
            int ind2 = spath.lastIndexOf( '/' );
            String newDockerImage = spath.substring( 0, ind2 ); 
            String newCwlFile = spath.substring( ind2 + 1 ); 
            if( !newDockerImage.equals( dockerImage ) || !newCwlFile.equals( cwlFile ) )
            {
                setDockerImage( newDockerImage );
                setCwlFile( newCwlFile );

                extractParametersAndOutputs();
            }
        }
        if( cwlPath.equals( oldValue ) ) 
        {
            return; 
        }

        this.cwlPath = cwlPath;
        firePropertyChange("cwlPath", cwlPath, oldValue );
        firePropertyChange("*", null, null );
    }

    @PropertyName("CWL File")
    public String getCwlFile()
    {
        return cwlFile;
    }
    public void setCwlFile(String cwlFile)
    {
        if( cwlFile == null )
        {
            return; 
        }
        Object oldValue = this.cwlFile;
        if( cwlFile.equals( oldValue ) ) 
        {
            return; 
        }

        this.cwlFile = cwlFile;
        firePropertyChange("cwlFile", cwlFile, oldValue );
        firePropertyChange("*", null, null );
    }

    @PropertyName("Docker Image")
    public String getDockerImage()
    {
        return dockerImage;
    }
    public void setDockerImage(String dockerImage)
    {
        if( dockerImage == null )
        {
            return; 
        }
        Object oldValue = this.dockerImage;
        if( dockerImage.equals( oldValue ) ) 
        {
            return; 
        }

        this.dockerImage = dockerImage;
        firePropertyChange("dockerImage", dockerImage, oldValue );
        firePropertyChange("*", null, null );
    }

    
    @Override
    public @Nonnull String[] getInputNames()
    {
        List<String> result = new ArrayList();
        Iterator<DynamicProperty> it = getParameters().propertyIterator();
        while (it.hasNext())
        {
            DynamicProperty dp = it.next();
            if (dp.getType().equals(DataElementPath.class))
                result.add("parameters/"+dp.getName());
        }        
        return result.toArray(new String[result.size()]);
    }
    
    @Override
    public @Nonnull String[] getOutputNames()
    {
        List<String> result = new ArrayList();
        Iterator<DynamicProperty> it = getOutputs().propertyIterator();
        while (it.hasNext())
        {           
            DynamicProperty dp = it.next();           
            if ( dp.getType().equals(DataElementPath.class))
                result.add("outputs/"+dp.getName());
        }        
        return result.toArray(new String[result.size()]);
    }

    private static Map<String, Class> type2Class = new HashMap()
    {
        {
            put("boolean", Boolean.class);
            put("string", String.class);
            put("File", DataElementPath.class);
            put("Directory", DataElementPath.class);
            put("int", Integer.class);
            put("double", Double.class);
        }
    };
    
    private static Map<String, Object> type2Object = new HashMap()
    {
        {
            put("boolean", false);
            put("string", "");
            put("File", null);
            put("Directory", null);
            put("int", 0);
            put("double", 0.0);
        }
    };
}