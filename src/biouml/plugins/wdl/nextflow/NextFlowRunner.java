package biouml.plugins.wdl.nextflow;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.plugins.wdl.GeneSpaceContext;
import biouml.plugins.wdl.ImportProperties;
import biouml.plugins.wdl.WorkflowSettings;
import biouml.plugins.wdl.WorkflowUtil;
import one.util.streamex.StreamEx;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.TextDataElement;

public class NextFlowRunner
{
	private static final String BIOUML_FUNCTIONS_NF = "resources/biouml_function.nf";
	private static final Logger log = Logger.getLogger(NextFlowRunner.class.getName());

    public static File generateFunctions(String outputDir) throws IOException
	{
		InputStream inputStream = NextFlowRunner.class.getResourceAsStream(BIOUML_FUNCTIONS_NF);
		File result = new File(outputDir, "biouml_function.nf");
		Files.copy(inputStream, result.toPath(), StandardCopyOption.REPLACE_EXISTING);
		return result;
	}

    public static String runNextFlowByDiagram(Diagram diagram, String nextFlowScript, WorkflowSettings settings, String outputDir, boolean useWsl) throws Exception
    {
        if( settings.getOutputPath() == null )
            throw new InvalidParameterException( "Output path not specified" );
        DataCollectionUtils.createSubCollection( settings.getOutputPath() );

        new File( outputDir ).mkdirs();

        File json = settings.generateParametersJSON( outputDir );
        settings.exportCollections( outputDir );
        exportIncludes( diagram, outputDir );

        if( nextFlowScript == null )
            nextFlowScript = new NextFlowGenerator().generate( diagram );
        GeneSpaceContext context = new GeneSpaceContext( null, null, null, Paths.get( outputDir ) );

        runNextFlow( diagram.getName(), diagram.getName(), Collections.emptyMap(), nextFlowScript, useWsl, settings.isUseDocker(), null, context, json.getName() );
        importResults( diagram, settings, outputDir );
        return "";
    }

    public static void runNextFlow(String id, String name, Map<String, Object> parameters, String nextFlowScript,
            boolean useWsl, boolean useDocker, String towerAddress, GeneSpaceContext context, String jsonFile) throws Exception
	{
        File outputDir = context.getOutputDir().toFile();
        outputDir.mkdirs();
        File config = generateConfig( name, parameters, outputDir, useDocker, context );
        generateFunctions( context.getOutputDir().toString() );

		File f = new File(outputDir, name + ".nf");
		ApplicationUtils.writeString(f, nextFlowScript);

        ProcessBuilder pb = null;
        if( useDocker )
            pb = getNextflowDockerProcessBuilder( f.getName(), config.getName(), id, towerAddress, context, jsonFile );
        else
            pb = getNextflowLocalProcessBuilder( f.getName(), config.getName(), id, towerAddress, context, useWsl, jsonFile );

        log.log( Level.INFO, "COMMAND: " + StreamEx.of( pb.command() ).joining( " " ) );
		System.out.println("COMMAND: " + StreamEx.of(pb.command()).joining(" "));
		Process process = pb.start();
		executeProcess(process);
	}

    /*
     * Run nextflow as local process in Windows or Unix
     */
    private static ProcessBuilder getNextflowLocalProcessBuilder(String nextFlowScriptName, String nextFlowConfig, String id, String towerAddress, GeneSpaceContext context,
            boolean useWsl, String jsonFile)
    {

        String parent = context.getOutputDir().toAbsolutePath().toString().replace( "\\", "/" );
        List<String> cmd = new ArrayList<>();
        if( towerAddress != null )
        {
            cmd.add( "export" );
            cmd.add( "TOWER_WORKFLOW_ID=" + id );
            cmd.add( "export" );
            cmd.add( "TOWER_ACCESS_TOKEN=zzz" );
        }
        cmd.add( "nextflow" );
        cmd.add( "run" );
        cmd.add( nextFlowScriptName );
        cmd.add( "-c" );
        cmd.add( nextFlowConfig );

        if( towerAddress != null )
        {
            cmd.add( "-with-tower" );
            cmd.add( "\'" + towerAddress + "\'" );
        }

        if( jsonFile != null )
        {
            cmd.add( "-params-file" );
            cmd.add( jsonFile );
        }

        List<String> baseCommand = new ArrayList<>();

        if( useWsl )
        {
            baseCommand.add( "wsl" );
            baseCommand.add( "--cd" );
            baseCommand.add( parent );
        }
        baseCommand.addAll( cmd );

        ProcessBuilder pb = new ProcessBuilder( baseCommand );
        if( !useWsl )
            pb.directory( context.getOutputDir().toFile() );
        return pb;

    }

    private static List<String> getRunNextflowCommandsLocal(Path runDir, boolean useWsl)
    {
        String parent = runDir.toAbsolutePath().toString().replace( "\\", "/" );
        String command = "nextflow ";

        List<String> baseCommand;

        if( useWsl )
            baseCommand = List.of( "wsl", "--cd", parent, "bash", "-c", command );
        else
            baseCommand = List.of( "bash", "-c", "cd " + parent + " && " + command );
        return baseCommand;
    }

    private static List<String> getRunNextflowCommandsDocker(Path runDir, String id)
    {
        String containerName = "nf-" + id;
        String workDir = runDir.toString();//"/nf-work";

        List<String> cmd = new ArrayList<>();
        cmd.add( "docker" );
        cmd.add( "run" );
        cmd.add( "--rm" );
        cmd.add( "--name" );
        cmd.add( containerName );

        cmd.add( "-w" );
        cmd.add( runDir.toString() );

        cmd.add( "-v" );
        cmd.add( dockerVolume( Paths.get( "/var/run/docker.sock" ), "/var/run/docker.sock" ) );

        cmd.add( "-v" );
        cmd.add( dockerVolume( runDir, runDir.toString() ) );

        cmd.add( "nextflow/nextflow:25.10.3" );
        cmd.add( "nextflow" );

        return cmd;
    }

    /*
     * Run nextflow as docker container in Unix. All paths should be mapped into container as SAME paths. Otherwise if
     * nextflow script contains inner docker tools, they will not see the paths.
     * 4 folders with user projects, workflows, supporting data and working folder (can be temporary) are mapped into container. 
     * All inputs/results/used items should be descendants of one of the 4 folders above. 
     */
    private static ProcessBuilder getNextflowDockerProcessBuilder(String nextFlowScriptName, String nextFlowConfig, String id,
            String towerAddress, GeneSpaceContext context, String jsonFile)
    {

		String containerName = "nf-" + id;
        String workDir = context.getOutputDir().toString();//"/nf-work";

		List<String> cmd = new ArrayList<>();
		cmd.add("docker");
		cmd.add("run");
		cmd.add("--rm");
		cmd.add("--name");
		cmd.add(containerName);


		cmd.add("-w");
        cmd.add( context.getOutputDir().toString() );
        if( towerAddress != null )
        {
            cmd.add( "-e" );
            cmd.add( "TOWER_WORKFLOW_ID=" + id );
            cmd.add( "-e" );
            cmd.add( "TOWER_ACCESS_TOKEN=zzz" );
        }

        cmd.add( "-v" );
        cmd.add( dockerVolume( Paths.get( "/var/run/docker.sock" ), "/var/run/docker.sock" ) );

        if( context.getProjectsDir() != null )
        {
            cmd.add( "-v" );
            cmd.add( dockerVolume( context.getProjectsDir(), context.getProjectsDir().toString() ) );
        }

        if( context.getGenomeDir() != null )
        {
            cmd.add( "-v" );
            cmd.add( dockerVolume( context.getGenomeDir(), context.getGenomeDir().toString() ) );
        }

        if( context.getWorkflowsDir() != null )
        {
            cmd.add( "-v" );
            cmd.add( dockerVolume( context.getWorkflowsDir(), context.getWorkflowsDir().toString() ) );
        }

        if( context.getOutputDir() != null )
        {
            cmd.add( "-v" );
            cmd.add( dockerVolume( context.getOutputDir(), context.getOutputDir().toString() ) );
        }

		cmd.add("nextflow/nextflow:25.10.3");
		cmd.add("nextflow");
        cmd.add( "run" );
        cmd.add( nextFlowScriptName );
		cmd.add("-c");
		cmd.add(workDir + "/" + nextFlowConfig);

        if( towerAddress != null )
        {
            cmd.add( "-with-tower" );
            cmd.add( "\'" + towerAddress + "\'" );
        }

        if( jsonFile != null )
        {
            cmd.add( "-params-file" );
            cmd.add( workDir + "/" + jsonFile );
        }

		ProcessBuilder pb = new ProcessBuilder(cmd);

		return pb;

	}
	
	static String dockerVolume(Path host, String container) {
		return host.toAbsolutePath().toString().replace("\\", "/") + ":" + container;
	}

	public static void executeProcess(Process process) throws Exception
	{
		CommandRunner r = new CommandRunner(process);
		Thread thread = new Thread(r);
		thread.start();
		process.waitFor();
	}

	private static class CommandRunner implements Runnable
	{
		Process process;

		public CommandRunner(Process process)
		{
			this.process = process;
		}

		public void log(BufferedReader input) throws IOException
		{
			String line = null;
			while( ( line = input.readLine() ) != null )
			{
				System.out.println(line);
				log.info(line);
			}
		}

		public void run()
		{
			BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
			try
			{
				log(input);
			}
			catch( IOException e )
			{
				e.printStackTrace();
			}
			BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			try
			{
				log(err);
			}
			catch( IOException e )
			{
				e.printStackTrace();
			}
		}
	}

	public static Map<String, Object> linkParameters(Map<String, Object> parameters, File dir)
	{
		Map<String, Object> result = new HashMap<>();
		for( Entry<String, Object> e : parameters.entrySet() )
		{
			try
			{
				Object value = e.getValue();
				result.put(e.getKey(), e.getValue());
				if( value instanceof String )
				{
					File f = new File((String)value);
					//                    Path link =Files.createLink(Path.of(dir.getAbsolutePath(), f.getName()), f.toPath());
					File copy = copyFile(f, dir);
					result.put(e.getKey(), "./" + copy.getName());
				}
			}
			catch( Exception ex )
			{
				ex.printStackTrace();
			}

		}
		return result;
	}

	public static File copyFile(File src, File targetDir) throws Exception
	{
		File dest = new File(targetDir, src.getName());
		if( src.isFile() )
		{
			ApplicationUtils.copyFile(dest, src, null);
		}
		else if( src.isDirectory() )
		{
			dest.mkdir();
			for( File f : src.listFiles() )
				copyFile(f, dest);
		}
		return dest;
	}

    public static File generateConfig(String name, Map<String, Object> parameters, File outputDir, boolean useDocker, GeneSpaceContext context) throws Exception
	{
		File config = new File(outputDir, name + ".config");

		try (BufferedWriter bw = new BufferedWriter(new FileWriter(config)))
		{
			bw.write("docker.enabled = true");
			bw.write("\n");

            //bw.write("workDir = '/tmp/nf-work'");
			for( Entry<String, Object> e : parameters.entrySet() )
			{

				String value;
				if ( e.getValue() instanceof String ) {
					value = "\"" + e.getValue() + "\"";
				} 
				else if ( e.getValue() instanceof Path ) {
                    value = "\"" + resolvePath( (Path) e.getValue(), context, useDocker ).toString() + "\"";
				}
				else
				{
					value = e.getValue().toString();
				}
                //value = ( e.getValue() instanceof String ) ? "\"" + e.getValue() + "\"" : e.getValue().toString();
				bw.write("\n");
				bw.write("params." + e.getKey() + " = " + value + "\n");
			}
		}
		return config;
	}

    private static Path resolvePath(Path original, GeneSpaceContext context, boolean useDocker)
    {
        return Path.of( "/" ).resolve( original );
    }


	public static void importResults(Diagram diagram, WorkflowSettings settings, String outputDir) throws Exception
	{
		if( settings.getOutputPath() == null )
			return;
		DataCollection dc = settings.getOutputPath().getDataCollection();

		for( Compartment n : WorkflowUtil.getAllCalls(diagram) )
		{
			if( WorkflowUtil.getDiagramRef(n) != null )
			{
				String ref = WorkflowUtil.getDiagramRef(n);
				Diagram externalDiagram = (Diagram)diagram.getOrigin().get(ref);
				importResults(externalDiagram, settings, outputDir);
				continue;
			}
			String taskRef = WorkflowUtil.getTaskRef(n);
			String folderName = ( taskRef );
			File folder = new File(outputDir, folderName);
			if( !folder.exists() || !folder.isDirectory() )
			{
				log.info("No results for " + n.getName());
				continue;
			}
			DataCollection nested = DataCollectionUtils.createSubCollection(dc.getCompletePath().getChildPath(folderName));
			for( File f : folder.listFiles() )
			{
				String data = ApplicationUtils.readAsString(f);
				nested.put(new TextDataElement(f.getName(), nested, data));
			}
		}
	}

	public static void exportIncludes(Diagram diagram, String outputDir) throws Exception
	{
		for( Diagram d : getIncludes(diagram) )
			WorkflowUtil.export(d, new File(outputDir));
	}

	public static Set<Diagram> getIncludes(Diagram diagram)
	{
		Set<Diagram> result = new HashSet<>();
		for( ImportProperties ip : WorkflowUtil.getImports(diagram) )
		{
			DataElementPath dep = ip.getSource();
			if( dep != null )
			{
				DataElement de = dep.getDataElement();
				if( de instanceof Diagram )
				{
					result.add((Diagram)de);
					continue;
				}
			}
			String name = ip.getSourceName();
			DataElement de = DataElementPath.create(diagram.getOrigin(), name).getDataElement();
			if( de instanceof Diagram )
			{
				result.add((Diagram)de);
			}
		}
		Set<Diagram> additionals = new HashSet<Diagram>();
		for( Diagram d : result )
			additionals.addAll(getIncludes(d));
		result.addAll(additionals);
		return result;
	}

    public static Properties getNextflowConfig(File content, String id, boolean useDocker, boolean useWsl) throws Exception
    {
        File runDir = content.getParentFile();

        List<String> commands;
        if( useDocker )
            commands = getRunNextflowCommandsDocker( runDir.toPath(), id );
        else
            commands = getRunNextflowCommandsLocal( runDir.toPath(), useWsl );

        commands.add( "config" );
        commands.add( "-properties" );

        ProcessBuilder pb = new ProcessBuilder( commands );
        pb.redirectErrorStream( true );

        log.log( Level.INFO, "COMMAND: " + StreamEx.of( pb.command() ).joining( " " ) );
        System.out.println( "COMMAND: " + StreamEx.of( pb.command() ).joining( " " ) );
        Process process = pb.start();

        StringBuilder processOutput = new StringBuilder();
        int exitCode = -1;
        try (BufferedReader processOutputReader = new BufferedReader( new InputStreamReader( process.getInputStream() ) );)
        {
            String readLine;

            while ( (readLine = processOutputReader.readLine()) != null )
            {
                processOutput.append( readLine + "\n" );
            }

            exitCode = process.waitFor();
        }

        Properties properties = new Properties();
        if( exitCode != -1 )
            properties.load( new StringReader( processOutput.toString() ) );
        return properties;
    }
}
