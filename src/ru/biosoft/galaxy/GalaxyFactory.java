package ru.biosoft.galaxy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.CodePrivilege;
import ru.biosoft.access.security.CodePrivilegeType;
import ru.biosoft.bsa.BAMTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.galaxy.parameters.ArrayParameter;
import ru.biosoft.galaxy.parameters.BaseFileParameter;
import ru.biosoft.galaxy.parameters.ConditionalParameter;
import ru.biosoft.galaxy.parameters.FileParameter;
import ru.biosoft.galaxy.parameters.MetaParameter;
import ru.biosoft.galaxy.parameters.Parameter;
import ru.biosoft.galaxy.parameters.StringParameter;
import ru.biosoft.tasks.process.ProcessLauncher;
import ru.biosoft.util.ApplicationUtils;
import ru.biosoft.util.StreamGobbler;
import ru.biosoft.util.TempFiles;

/**
 * Factory for galaxy methods.
 * Invokes special python scripts to run (get info from) Galaxy code.
 */
@CodePrivilege(CodePrivilegeType.LAUNCH)
public class GalaxyFactory
{
    protected static final Logger log = Logger.getLogger(GalaxyFactory.class.getName());

    protected static final String PYTHON_METADATA_FILE = "Metadata.py";
    protected static final String PYTHON_TEMPLATE_FILE = "FillTemplate.py";
    protected static final String PYTHON_FILTER_FILE = "Filter.py";
    protected static final String PYTHON_RST_TO_HTML_FILE = "ParseRST.py";

    private static Map<String, String> pythonEnv = new HashMap<>();

    private static Parameter GALAXY_ROOT_DIR;
    private static Parameter GALAXY_DATA_INDEX_DIR;

    private static File SCRIPT_PATH;

    private static boolean isInit = false;
    public static void reInit()
    {
        isInit = false;
        initConstants();
    }
    synchronized static void initConstants()
    {
        if( !isInit )
        {
            isInit = true;

            GalaxyDistFiles distFiles = GalaxyDataCollection.getGalaxyDistFiles();

            String pythonPath = distFiles.getLibFolder().getAbsolutePath();
            File eggsDir = distFiles.getEggsFolder();
            if( eggsDir.exists() )
            {
                File[] eggs = eggsDir.listFiles();
                if( eggs == null )
                {
                    throw new IllegalArgumentException(
                            "Error reading directory '" + eggsDir + "': make sure that Galaxy is properly configured" );
                }
                for( File egg : eggs )
                    pythonPath = egg.getAbsolutePath() + File.pathSeparator + pythonPath;
            }
            pythonEnv.put( "PYTHONPATH", pythonPath );

            if( distFiles.getVEnvFolder().exists() )
            {
                pythonEnv.put( "VIRTUAL_ENV", distFiles.getVEnvFolder().getAbsolutePath() );
                pythonEnv.put( "PATH", new File(distFiles.getVEnvFolder(), "bin").getAbsolutePath() );
            }


            GALAXY_ROOT_DIR = new StringParameter(false, distFiles.getRootFolder().getAbsolutePath());
            GALAXY_DATA_INDEX_DIR = new StringParameter(false, distFiles.getToolDataFolder().getAbsolutePath());

            SCRIPT_PATH = ApplicationUtils.getPluginPath("ru.biosoft.galaxy");

            try
            {
                Map<String, MetaParameter> defaultMetaData = parseMetadataString("{"
                        + "'data_lines': {'default': 0, 'no_value': 0, 'type': 'MetadataParameter', 'description': 'Number of data lines'}, "
                        + "'dbkey': {'default': '?', 'no_value': '?', 'type': 'DBKeyParameter', 'description': 'Database/Build'}" + "}");
                metadata.put("dat", defaultMetaData);
                metadata.put("null", defaultMetaData);
                metadata.put(
                        "fasta",
                        parseMetadataString("{"
                                + "'data_lines': {'default': 0, 'no_value': 0, 'type': 'MetadataParameter', 'description': 'Number of data lines'}, "
                                + "'dbkey': {'default': '?', 'no_value': '?', 'type': 'DBKeyParameter', 'description': 'Database/Build'}, "
                                + "'sequences': {'default': 0, 'no_value': 0, 'type': 'MetadataParameter', 'description': 'Number of sequences'}"
                                + "}"));
                metadata.put(
                        "tabular",
                        parseMetadataString("{"
                                + "'column_types': {'default': [], 'no_value': [], 'type': 'ColumnTypesParameter', 'description': 'Column types'}, "
                                + "'comment_lines': {'default': 0, 'no_value': 0, 'type': 'MetadataParameter', 'description': 'Number of comment lines'}, "
                                + "'columns': {'default': 0, 'no_value': 0, 'type': 'MetadataParameter', 'description': 'Number of columns'}, "
                                + "'data_lines': {'default': 0, 'no_value': 0, 'type': 'MetadataParameter', 'description': 'Number of data lines'}, "
                                + "'dbkey': {'default': '?', 'no_value': '?', 'type': 'DBKeyParameter', 'description': 'Database/Build'}"
                                + "}"));
                metadata.put(
                        "bed",
                        parseMetadataString("{"
                                + "'chromCol': {'default': 1, 'no_value': None, 'type': 'ColumnParameter', 'description': 'Chrom column'}, "
                                + "'nameCol': {'default': None, 'no_value': 0, 'type': 'ColumnParameter', 'description': 'Name/Identifier column (click box & select)'}, "
                                + "'comment_lines': {'default': 0, 'no_value': 0, 'type': 'MetadataParameter', 'description': 'Number of comment lines'}, "
                                + "'endCol': {'default': 3, 'no_value': None, 'type': 'ColumnParameter', 'description': 'End column'}, "
                                + "'dbkey': {'default': '?', 'no_value': '?', 'type': 'DBKeyParameter', 'description': 'Database/Build'}, "
                                + "'strandCol': {'default': None, 'no_value': 0, 'type': 'ColumnParameter', 'description': 'Strand column (click box & select)'}, "
                                + "'startCol': {'default': 2, 'no_value': None, 'type': 'ColumnParameter', 'description': 'Start column'}, "
                                + "'data_lines': {'default': 0, 'no_value': 0, 'type': 'MetadataParameter', 'description': 'Number of data lines'}, "
                                + "'column_types': {'default': [], 'no_value': [], 'type': 'ColumnTypesParameter', 'description': 'Column types'}, "
                                + "'viz_filter_cols': {'default': [4], 'no_value': None, 'type': 'ColumnParameter', 'description': 'Score column for visualization'}, "
                                + "'columns': {'default': 3, 'no_value': None, 'type': 'MetadataParameter', 'description': 'Number of columns'}"
                                + "}"));
                metadata.put(
                        "interval",
                        parseMetadataString("{"
                                + "'chromCol': {'default': 1, 'no_value': None, 'type': 'ColumnParameter', 'description': 'Chrom column'}, "
                                + "'nameCol': {'default': None, 'no_value': 0, 'type': 'ColumnParameter', 'description': 'Name/Identifier column (click box & select)'}, "
                                + "'comment_lines': {'default': 0, 'no_value': 0, 'type': 'MetadataParameter', 'description': 'Number of comment lines'}, "
                                + "'endCol': {'default': 3, 'no_value': None, 'type': 'ColumnParameter', 'description': 'End column'}, "
                                + "'dbkey': {'default': '?', 'no_value': '?', 'type': 'DBKeyParameter', 'description': 'Database/Build'}, "
                                + "'strandCol': {'default': None, 'no_value': 0, 'type': 'ColumnParameter', 'description': 'Strand column (click box & select)'}, "
                                + "'startCol': {'default': 2, 'no_value': None, 'type': 'ColumnParameter', 'description': 'Start column'}, "
                                + "'data_lines': {'default': 0, 'no_value': 0, 'type': 'MetadataParameter', 'description': 'Number of data lines'}, "
                                + "'column_types': {'default': 'str', 'no_value': [], 'type': 'ColumnTypesParameter', 'description': 'Column types'}, "
                                + "'columns': {'default': 1, 'no_value': None, 'type': 'MetadataParameter', 'description': 'Number of columns'}"
                                + "}"));
                metadata.put(
                        "gff",
                        parseMetadataString("{"
                                + "'column_types': {'default': ['str', 'str', 'str', 'int', 'int', 'int', 'str', 'str', 'str'], 'no_value': None, 'type': 'ColumnTypesParameter', 'description': 'Column types'}, "
                                + "'comment_lines': {'default': 0, 'no_value': 0, 'type': 'MetadataParameter', 'description': 'Number of comment lines'}, "
                                + "'columns': {'default': 9, 'no_value': None, 'type': 'MetadataParameter', 'description': 'Number of columns'}, "
                                + "'data_lines': {'default': 0, 'no_value': 0, 'type': 'MetadataParameter', 'description': 'Number of data lines'}, "
                                + "'dbkey': {'default': '?', 'no_value': '?', 'type': 'DBKeyParameter', 'description': 'Database/Build'}"
                                + "}"));
            }
            catch( JSONException e )
            {
            }
        }
    }

    public static void setupPythonEnvironment(Map<String, String> env)
    {
        for(Map.Entry<String, String> e : pythonEnv.entrySet())
        {
            String name = e.getKey();
            String value = e.getValue();

            String oldValue = env.get( name );
            if(oldValue != null)
                value = value + File.pathSeparator + oldValue;

            env.put( name, value );
        }
    }

    public static void setupPythonEnvironment(ProcessLauncher launcher)
    {
        for(Map.Entry<String, String> e : pythonEnv.entrySet())
        {
            String name = e.getKey();
            String value = e.getValue();
            String oldValue = System.getenv( name );
            if(oldValue != null)
                value = value + File.pathSeparator + oldValue;
            launcher.addEnv( name, value );
        }
    }

    public static String convertRstToHtml(String rst)
    {
        if( rst == null )
        {
            return null; 
        }

        try
        {
            initConstants();
            File rstFile = TempFiles.file(".rst", rst);

            String[] command = new String[] {"python", new File(getScriptPath(), PYTHON_RST_TO_HTML_FILE).getAbsolutePath(), rstFile.getAbsolutePath()};
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            setupPythonEnvironment( processBuilder.environment() );
            processBuilder.directory(getScriptPath());
            Process proc = processBuilder.start();
            StreamGobbler inputReader = new StreamGobbler(proc.getInputStream(), true);
            StreamGobbler errorReader = new StreamGobbler(proc.getErrorStream(), true);
            proc.waitFor();
            rstFile.delete();
            if( proc.exitValue() == 0 )
            {
                return inputReader.getData();
            }
            else
            {
                log.log(Level.SEVERE, "Unable to convert RST to HTML: "+errorReader.getData());
                return rst;
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Unable to convert RST to HTML: ", e);
        }
        return rst;
    }

    public static String fillTemplate(String template, Map<String, Parameter> parameters, File toolPath) throws Exception
    {
        initConstants();
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("GALAXY_DATA_INDEX_DIR", convertParameterToJSON(GALAXY_DATA_INDEX_DIR));

        Parameter galaxyNewFile = new StringParameter(false, TempFiles.dir( "galaxy_new_files" ).getAbsolutePath());
        jsonParam.put("__new_file_path__", convertParameterToJSON(galaxyNewFile));
        jsonParam.put("__tool_data_path__", convertParameterToJSON(new StringParameter(false, toolPath.getAbsolutePath())));
        jsonParam.put("__root_dir__", convertParameterToJSON(GALAXY_ROOT_DIR));
        for( Map.Entry<String, Parameter> entry : parameters.entrySet() )
        {
            jsonParam.put(entry.getKey(), convertParameterToJSON(entry.getValue()));
        }

        File metaFile = TempFiles.file("galaxymeta.json", jsonParam+"\n");
        File templateFile = TempFiles.file("galaxytemp.txt", template+"\n");

        GalaxyDistFiles distFiles = GalaxyDataCollection.getGalaxyDistFiles();
        String[] command = new String[] {"python", new File(getScriptPath(), PYTHON_TEMPLATE_FILE).getAbsolutePath(),
                templateFile.getAbsolutePath(), metaFile.getAbsolutePath(),
                distFiles.getRootFolder().getAbsolutePath(), distFiles.getDataTypesConfXML().getAbsolutePath() };

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        setupPythonEnvironment( processBuilder.environment() );
        processBuilder.directory(getScriptPath());
        Process proc = processBuilder.start();
        StreamGobbler inputReader = new StreamGobbler(proc.getInputStream(), true);
        StreamGobbler errorReader = new StreamGobbler(proc.getErrorStream(), true);
        proc.waitFor();
        metaFile.delete();
        templateFile.delete();
        if( proc.exitValue() == 0 )
        {
            return inputReader.getData();
        }
        else
        {
            throw new Exception("Fill command template: " + errorReader.getData());
        }
    }

    protected static JSONObject convertParameterToJSON(Parameter p) throws Exception
    {
        JSONObject result = new JSONObject();
        if( p instanceof ArrayParameter )
        {
            JSONArray jsonArray = new JSONArray();
            for( Map<String, Parameter> listElement : ( (ArrayParameter)p ).getValues() )
            {
                JSONObject innerElement = new JSONObject();
                for( Map.Entry<String, Parameter> entry : listElement.entrySet() )
                {
                    innerElement.put(entry.getKey(), convertParameterToJSON(entry.getValue()));
                }
                jsonArray.put(innerElement);
            }
            result.put("_value", jsonArray);
        }
        else if( p instanceof ConditionalParameter )
        {
            ConditionalParameter cp = (ConditionalParameter)p;
            JSONObject jsonObj = new JSONObject();
            jsonObj.put(cp.getKeyParameterName(), cp.getKeyParameter().toString());
            Map<String, Parameter> whenParameters = cp.getWhenParameters(cp.getKeyParameter().toString());
            for( Map.Entry<String, Parameter> entry : whenParameters.entrySet() )
            {
                jsonObj.put(entry.getKey(), convertParameterToJSON(entry.getValue()));
            }
            result.put("_value", jsonObj);
        }
        else
        {
            result.put("_value", p.toString()==null?"None":p.toString());

            Map<String, MetaParameter> metadata = p.getMetadata();
            if( metadata.size() > 0 )
            {
                JSONObject jsonMetadata = new JSONObject();
                for( Map.Entry<String, MetaParameter> entry : metadata.entrySet() )
                {
                    MetaParameter metaParam = entry.getValue();
                    jsonMetadata.put(entry.getKey(), metaParam.getValue());
                }
                result.put("metadata", jsonMetadata);
            }

            if(p instanceof FileParameter)
                p.getParameterFields().put("files_path", GalaxyFactory.getUniqueFolder(( (FileParameter)p ).getPath()).getAbsolutePath());

            for(Map.Entry<String, Object> e : p.getParameterFields().entrySet())
                result.put(e.getKey(), e.getValue());

        }
        return result;
    }

    private static final Map<String, Map<String, MetaParameter>> metadata = new HashMap<>();

    public static Map<String, MetaParameter> getMetadata(FileParameter param) throws Exception
    {
        initConstants();

        Map<String, MetaParameter> result = null;

        if(param.getExtension().equals("bam"))
            return getBAMFileMetadata(param);

        boolean needsMetadata = param.getAttributes().get("needs_metadata") instanceof Boolean ? (Boolean)param.getAttributes().get("needs_metadata") : true;

        if( needsMetadata )
        {
            List<String> cmd = new ArrayList<>();
            cmd.add("python");
            cmd.add(new File(getScriptPath(), PYTHON_METADATA_FILE).getAbsolutePath());
            cmd.add(GALAXY_ROOT_DIR.toString());
            cmd.add( GalaxyDataCollection.getGalaxyDistFiles().getDataTypesConfXML().getAbsolutePath() );
            cmd.add(param.toString());
            if( param.isExtensionSet() )
                cmd.add(param.getExtension());

            ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            setupPythonEnvironment( processBuilder.environment() );
            processBuilder.directory(getScriptPath());
            Process proc = processBuilder.start();
            StreamGobbler errorReader = new StreamGobbler(proc.getErrorStream(), true);
            StreamGobbler inputReader = new StreamGobbler(proc.getInputStream(), true);
            proc.waitFor();

            if( proc.exitValue() == 0 )
            {
                String mstr = inputReader.getData();  
                try
                { 
                    result = parseMetadataString( mstr );
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "Bad metadata string: '" + mstr + "'\nCommand was = " + cmd );
                    throw e;
                }
            }
            else
            {
                log.warning("Unable to get metadata for " + param.getName() + "=" + param.toString() + ": exit value = " + proc.exitValue()
                        + "\n" + errorReader.getData());

            }
        }

        if(result == null) {
            if( param.getExtension() != null && metadata.containsKey(param.getExtension()) )
                result = new HashMap<>( metadata.get( param.getExtension() ) );
            else
                result = new HashMap<>( metadata.get( "null" ) );
        }

        DataElementPath path = param.getDataElementPath();
        if(path != null)
        {
            annotateFromElement(result, path);
        }

        return result;
    }

    private static Map<String, MetaParameter> getBAMFileMetadata(FileParameter param) throws Exception
    {
        File indexFile = BAMTrack.getIndexFile(param.getFile());
        BAMTrack track = param.getDataElementPath().getDataElement( BAMTrack.class );
        String dbkey = track.getGenomeSelector().getGenomeId();
        if(dbkey == null || dbkey.isEmpty())
            dbkey = "?";
        return parseMetadataString("{'bam_index': {'default': '" + indexFile.getAbsolutePath() + "', 'no_value': None, 'type': 'FileParameter', 'description': 'BAM Index File'},"
                + " 'extension': {'default': 'bam', 'no_value': '', 'type': 'MetadataParameter', 'description': ''},"
                + " 'dbkey': {'default': '" + dbkey + "', 'no_value': '?', 'type': 'DBKeyParameter', 'description': 'Database/Build'}}");
    }

    /**
     * Adds additional metadata annotation based on ru.biosoft.access.core.DataElement
     * @param result
     * @param path
     */
    protected static void annotateFromElement(Map<String, MetaParameter> result, DataElementPath path)
    {
        DataElementDescriptor descriptor = path.getDescriptor();
        if(descriptor != null)
        {
            String genomeId = descriptor.getValue(Track.GENOME_ID_PROPERTY);
            if(genomeId != null)
            {
                result.put("dbkey", new MetaParameter("dbkey", genomeId, "DBKeyParameter", "Database/Build"));
            }
        }
    }

    public static String detectExtension(File file) throws Exception
    {
        FileParameter fileParameter = new FileParameter(false);
        fileParameter.setPath(file.getParentFile());
        fileParameter.setValue(file.getName());
        Map<String, MetaParameter> metadata = getMetadata(fileParameter);
        if(metadata == null || metadata.get("extension") == null) return "tabular";
        return metadata.get("extension").getValue().toString();
    }

    /**
     * @param metadataString
     * @return
     * @throws JSONException
     */
    protected static Map<String, MetaParameter> parseMetadataString(String metadataString) throws JSONException
    {
        Map<String, MetaParameter> result;
        result = new HashMap<>();
        JSONObject json = new JSONObject(metadataString);
        Iterator<String> iter = json.keys();
        while( iter.hasNext() )
        {
            String name = iter.next();
            JSONObject value = json.getJSONObject(name);
            Object defaultValue = value.get("default");
            if( defaultValue.equals("None") )
            {
                defaultValue = value.get("no_value");
            }
            result.put(name, new MetaParameter(name, defaultValue, value.getString("type"), value.getString("description")));
        }
        return result;
    }

    protected static String readStream(InputStream is) throws IOException
    {
        try (InputStreamReader inputReader = new InputStreamReader( is ))
        {
            int read = inputReader.read();
            StringBuffer charBuffer = new StringBuffer();
            while( read != -1 )
            {
                charBuffer.append( (char)read );
                read = inputReader.read();
            }
            return charBuffer.toString();
        }
    }

    public static File getUniqueFolder(File parent)
    {
        String prefix = "galaxy_tmp_";
        int i = 0;
        while( true )
        {
            File f = new File(parent, prefix + i);
            if( !f.exists() )
                return f;
            i++;
        }
    }

    static void correctParameter(Parameter p, File inputPath, File outputPath)
    {
        if( p instanceof ArrayParameter )
        {
            for( Map<String, Parameter> pMap : ( (ArrayParameter)p ).getValues() )
            {
                for( Parameter childParam : pMap.values() )
                {
                    correctParameter(childParam, inputPath, outputPath);
                }
            }
        }
        else if( p instanceof ConditionalParameter )
        {
            for( String key : ( (ConditionalParameter)p ).getWhenSet() )
            {
                for( Parameter childParam : ( (ConditionalParameter)p ).getWhenParameters(key).values() )
                {
                    correctParameter(childParam, inputPath, outputPath);
                }
            }
        }

        if( p instanceof BaseFileParameter )
        {
            File path;
            if( p.isOutput() )
            {
                path = outputPath;
                //create parent directory for output file
                String name = ((BaseFileParameter)p).getName();
                if(name != null)
                {
                    File dir = new File(path, name).getParentFile();
                    dir.mkdirs();
                }
            }
            else
            {
                path = inputPath;
            }
            ( (BaseFileParameter)p ).setPath(path);
        }
    }

    /**
     * @return path where additional helper scripts are stored (usually plugin path for galaxy plugin)
     */
    public static File getScriptPath()
    {
        return SCRIPT_PATH;
    }
}