package ru.biosoft.plugins.docker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.log.Logger;
//import com.developmentontheedge.beans.log.Level;

import biouml.model.Diagram;

import java.util.logging.Level;

import ru.biosoft.access.DataCollectionUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;

import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.TextFileImporter;

import ru.biosoft.access.script.LogScriptEnvironment;

import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.util.TempFiles;

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
import com.github.dockerjava.core.util.CompressArchiveUtil;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;


@PropertyName ( "CWL Dockered Analysis" )
public class CWLDockeredAnalysis extends AnalysisMethodSupport<CWLDockeredAnalysisParameters>
{
    public CWLDockeredAnalysis(DataCollection origin, String name)
    {
        super(origin, name, new CWLDockeredAnalysisParameters());

        String spath = "" + origin.getCompletePath(); 
        if( spath.startsWith( "analyses/Docker/local/" ) )
        {
            getParameters().setDockerImage( spath.substring( "analyses/Docker/local/".length() ) );
            getParameters().setCwlFile( name );

            getParameters().extractParametersAndOutputs();
        }

        //System.out.println( "origin = " + origin.getCompletePath() );
        //System.out.println( "name = " + name );
    }

    private String cwlFile;

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        String ymlPath = generateYml();
        cwlFile = getParameters().getCwlFile();

        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost( "unix:///var/run/docker.sock" )
            .build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost( config.getDockerHost() )
            .build();

        DockerClient dockerClient = DockerClientImpl.getInstance( config, httpClient );

        String uniqId = "-run-" + System.currentTimeMillis(); 

        log.info( "Creating container " + cwlFile + uniqId + "..." );

        HostConfig hostConfigCwl = HostConfig.newHostConfig()
           /*.withNetworkMode( networkName )*/;

        Volume sockFile = new Volume( "/var/run/docker.sock" );
        hostConfigCwl.withBinds( new Bind( "/var/run/docker.sock", sockFile ) );

        CreateContainerResponse cwlContainer = dockerClient.createContainerCmd( getParameters().getDockerImage() )
            .withName( cwlFile + uniqId ) 
            .withHostName( cwlFile + uniqId ) 
            .withHostConfig( hostConfigCwl ) 
            .withCmd( "/bin/bash", "-c", "/usr/bin/env cwl-runner --disable-color --no-read-only /CWL/" + cwlFile + " /CWL/job.yaml"  )
            //.withCmd( "/bin/bash", "-c", "while true; do echo hello; sleep 1; done"  )
            .withTty( true )
            .exec();

        /*
        dockerClient.copyArchiveToContainerCmd( cwlContainer.getId() )
            .withRemotePath( "/CWL" )
            .withHostResource( ymlPath )
            .exec();
        */

        Path temp = Files.createTempFile("", ".tar.gz");
        Path binaryFile = Paths.get( ymlPath );
        CompressArchiveUtil.tar( binaryFile, temp, true, false );

        try( InputStream uploadStream = Files.newInputStream(temp) ) 
        {
            dockerClient.copyArchiveToContainerCmd( cwlContainer.getId() )
                .withRemotePath( "/CWL" )
                .withTarInputStream(uploadStream).exec();
        }

        dockerClient.startContainerCmd( cwlContainer.getId() ).exec(); 

        LogContainerCmd logContainerCmd = dockerClient.logContainerCmd( cwlContainer.getId() );
        logContainerCmd
            .withStdOut( true )
            .withStdErr( true )
            .withFollowStream(true)
            .withTailAll();

//        try
//        {
//            logContainerCmd.exec( new LogContainerResultCallback() 
//            {
//                @Override
//                public void onNext( Frame item ) 
//                {
//                    log.info( item.toString() );
//                }
//            }).awaitCompletion();
//        }
//        catch( Throwable e )
//        {
//            log.log(Level.SEVERE, "" + e.getMessage(), e);
//        }

        getJobControl().setPreparedness(90);

        log.info( "Removing container " + cwlFile + uniqId + "..." );

        dockerClient.removeContainerCmd( cwlContainer.getId() ).withForce( true ).exec();


/*
        if( files != null )
        {
            try
            {
                ru.biosoft.access.core.DataElementPath outputPath = getParameters().getOutputFolder();
                DataCollection collection = null;
                if( !outputPath.exists() )
                    collection = (DataCollection)DataCollectionUtils.createSubCollection(outputPath);
                else
                    collection = outputPath.getDataCollection();

                if( files != null )
                {
                    for( File file : files )
                    {
                        new TextFileImporter().doImport(collection, file, file.getName(), null, log);
                    }
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Error during processing of output files: " + e.getMessage(), e);
            }
        }
*/

        getJobControl().setPreparedness(100);

        return null;
    }

    /*
    private String initCWLPath() throws IOException
    {
        File resultFile = cwlFile;

        if( resultFile == null || !resultFile.exists() )
        {
            DataElementPath path = getParameters().getCwlPath();
            DataElement de = path.getDataElement();
            if( de instanceof Diagram )
            {
                Diagram diagram = (Diagram)de;
                String content = diagram.getAttributes().getValueAsString("CWL");
                File dir = TempFiles.dir("CWL_Workflow");
                resultFile = new File(dir, diagram.getName());
                ApplicationUtils.writeString(resultFile, content);
                for( File stepCWL : CWLUtil.getStepCWLs(diagram) )
                {
                    File newStep = new File(dir, stepCWL.getName());
                    ApplicationUtils.copyFile(newStep, stepCWL);
                }
            }
            else
                resultFile = new File(path.toString());
        }
        return resultFile.getAbsolutePath();
    }
    */

    public String generateYml()
    {
        File outDir = new File(TempFiles.path("simulation").getAbsolutePath());

        if( !outDir.exists() )
            outDir.mkdirs();

        File yml = new File(outDir, "job.yaml");
        log.info("Generate YML: " + yml.getAbsolutePath());

        CWLDockeredAnalysisParameters parameters = getParameters();

        try (BufferedWriter bw = ApplicationUtils.utfWriter(yml))
        {
            for( DynamicProperty dp : parameters.getParameters() )
            {
                String name = dp.getName();
                Object value = dp.getValue();

                if( value instanceof DataElementPath )
                {
                    DataElement element = ( (DataElementPath)dp.getValue() ).getDataElement();
                    if( element instanceof FileDataElement )
                    {
                        File file = ( (DataElementPath)dp.getValue() ).getDataElement(FileDataElement.class).getFile();
                        bw.write(name + ":\n");
                        bw.write(" class: File\n");
                        bw.write(" path: " + file.getAbsolutePath());
                    }
                    else if( element instanceof DataCollection )
                    {
                        DataCollection<?> dc = (DataCollection)element;
                        for( String n : dc.getNameList() )
                        {
                            DataElement de = dc.get(n);
                            if( de instanceof FileDataElement )
                            {
                                File innerFile = ( (FileDataElement)de ).getFile();
                                String path = innerFile.getParentFile().getAbsolutePath();
                                bw.write(name + ":\n");
                                bw.write(" class: Directory\n");
                                bw.write(" path: " + path);
                                break;
                            }
                        }
                    }
                }
                else if( value instanceof String )
                {
                    bw.write(name + ": \"" + dp.getValue() + "\"");
                }
                else
                    bw.write(name + ": " + dp.getValue());
                bw.write("\n");
            }
        }
        catch( Exception ex )
        {
            Logger.getLogger().error("generateYml: ", ex);
        }
        return yml.getAbsolutePath();
    }
}
