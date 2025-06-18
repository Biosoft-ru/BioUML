package ru.biosoft.analysis;

import java.io.File;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.TextFileImporter;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.access.script.LogScriptEnvironment;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.tasks.process.LauncherFactory;
import ru.biosoft.tasks.process.ProcessLauncher;

public class StubAnalysis extends AnalysisMethodSupport<StubAnalysisParameters>
{
    public StubAnalysis(DataCollection origin, String name)
    {
        super( origin, name, new StubAnalysisParameters() );
    }

    @Override
    public Object justAnalyzeAndPut()
    {
        DataElement de = null;
        try
        {
            Thread.sleep( 1000 );
            de = parameters.getInput().getDataElement();
        }
        catch( RepositoryException ex )
        {
            log.info( "Error during retrieving data element " + ex.getMessage() );
        }
        catch( InterruptedException e )
        {
            log.info( "Error during analysis " + e.getMessage() );
        }

        if( de == null )
            log.info( "Error during retrieving data element" );

        String launchType = parameters.getLaunchType();

        File inputFile = ( (FileDataElement)de ).getFile();
        String command = "echo Hello, $(basename ${inputFile0})! Your first line is: $(head -n 1 ${inputFile0}) | tee ${inputFile0}.out";
        String commandSlurmSSCC = "sudo docker run --rm " +
            "-v /ifs/home/inp_f/inp_f/slurm/scripts:/scripts " +
            "docker-registry-sscc-private.biouml.org:5000/test-image " +
            "sh -c 'echo Hello from c-tau Docker, $(basename ${inputFile0})! Your first line is: $(head -n 1 ${inputFile0}) | tee ${inputFile0}.out'";
        String commandSlurmDOTE = "sudo docker run --rm " +
            "-v /ifs/home/inp_f/inp_f/slurm/scripts:/scripts " +
            "docker-registry-dote-private.biouml.org:5000/test-image " +
            "sh -c 'echo Hello from Docker, $(basename ${inputFile0})! Your first line is: $(head -n 1 ${inputFile0}) | tee ${inputFile0}.out'";
        ProcessLauncher launcher = null;

        switch( launchType )
        {
            case StubAnalysisParameters.LAUNCH_SLURM:
            {
                command = commandSlurmSSCC; 
                try
                {
                    launcher = LauncherFactory.getLauncher( "C-tau SLURM" );
                }
                catch( Exception e )
                {
                    log.info( "Could not get C-tau SLURM launcher: " + e.getMessage() );
                }
                if( launcher == null )
                    log.info( "Could not get C-tau SLURM launcher" );
                break;
            }
            case StubAnalysisParameters.LAUNCH_LOCAL:
            {
                launcher = LauncherFactory.getDefaultLauncher();
            }
        }

        if( launcher != null )
        {
            launcher.setEnvironment( new LogScriptEnvironment( log ) );
            launcher.setInputFiles( inputFile );
            launcher.setCommand( command );
            try
            {
                launcher.executeAndWait();
                File[] files = launcher.getOutputFiles();
                
                DataElementPath path = parameters.getOutputFolder();
                DataCollection collection = null;
                if (!path.exists())
                    collection = (DataCollection)DataCollectionUtils.createSubCollection( path );
                else
                    collection = path.getDataCollection();

                if( files != null )
                {
                    for (File file: files)
                    {
                        new TextFileImporter().doImport( collection, file, file.getName(), null, log );
    //                    FileDataElement fde = new FileDataElement(file.getName(), collection, file);
    //                    collection.put(fde);
                    }
                }
            }
            catch( Exception e )
            {
                log.log( java.util.logging.Level.SEVERE, "Error during command launch: " + e.getMessage(), e );
            }
        }

        getJobControl().setPreparedness( 100 );
        return null;
    }
}