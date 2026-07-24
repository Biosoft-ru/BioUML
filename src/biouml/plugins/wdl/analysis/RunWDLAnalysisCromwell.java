package biouml.plugins.wdl.analysis;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.json.JSONObject;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.plugins.wdl.FileScriptLoader;
import biouml.plugins.wdl.ScriptLoader;
import biouml.plugins.wdl.WorkflowSettings;
import biouml.plugins.wdl.analysis.WDLScriptLogs.LogConsumer;
import biouml.plugins.wdl.analysis.WDLScriptLogs.LogType;
import biouml.plugins.wdl.diagram.DiagramGenerator;
import biouml.plugins.wdl.diagram.WDLDiagramType;
import biouml.plugins.wdl.diagram.WDLImporter;
import biouml.plugins.wdl.model.ScriptInfo;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.TextDataElement;
import ru.biosoft.access.file.FileBasedCollection;
import ru.biosoft.access.generic.GenericDataCollection;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.util.bean.JSONBean;

public class RunWDLAnalysisCromwell extends AnalysisMethodSupport<RunWDLAnalysisCromwell.Parameters>
{
    private static final Logger serverLog = Logger.getLogger( RunWDLAnalysisCromwell.class.getName() );

    public RunWDLAnalysisCromwell(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        WDLScript wdlScriptElement = parameters.getWdlPath().getDataElement( WDLScript.class );
        String wdlScript = wdlScriptElement.getContent();

        String jsonStr = getInputsJson();

        log.info( "Will run with the following parameters:" );
        log.info( jsonStr );

        byte[] dependenciesZipBytes = null;
        if( parameters.getIncludes() != null && parameters.getIncludes().length > 0 )
        {
            ByteArrayOutputStream zipBytes = new ByteArrayOutputStream();
            ZipOutputStream zip = new ZipOutputStream( zipBytes );
            for ( IncludePath iPath : parameters.getIncludes() )
            {
                DataElementPath folder = iPath.getPath();
                List<DataElementPath> wdlFiles = findWDLFiles( folder, iPath.recursive );
                for ( DataElementPath p : wdlFiles )
                {
                    String scriptContent = p.getDataElement( WDLScript.class ).getContent();
                    String name = p.toString().substring( iPath.getPath().toString().length() );
                    if( name.startsWith( "/" ) )
                        name = name.substring( 1 );
                    ZipEntry entry = new ZipEntry( name );
                    zip.putNextEntry( entry );
                    zip.write( scriptContent.getBytes( StandardCharsets.UTF_8 ) );
                    zip.closeEntry();
                }
            }
            zip.close();
            dependenciesZipBytes = zipBytes.toByteArray();
        }

        /*
        JSONObject wfDescr = CromwellAPI.INSTANCE.describeWorkflow( wdlScript, jsonStr, dependenciesZipBytes );
        
        log.info( "Checking WDL script" );
        if(!wfDescr.getBoolean( "valid" ))
        {
            JSONArray errors = wfDescr.getJSONArray( "errors" );
            for(int i = 0; i < errors.length(); i++)
            {
                String error = errors.getString( i );
                log.severe( error );
            }
            throw new Exception("wdlScript is not valid: " + parameters.getWdlScriptPath());
        }
        log.info("WDL script is valid");
        */

        JSONObject optionsJSON = getWorkflowOptionsJSON();
        String options = optionsJSON.toString();

        log.info( "Starting WDL script" );
        JSONObject resp = CromwellAPI.INSTANCE.submitWorkflow( wdlScript, jsonStr, options, dependenciesZipBytes );
        String taskId = resp.getString( "id" );

        WDLScriptLogs logs = new WDLScriptLogs( new LogConsumer()
        {
            @Override
            public void consume(String msg, String taskName, LogType type, int attempt, int shardIndex)
            {
                if( type == LogType.STDOUT )
                    log.info( taskName + ": " + msg );
                else if( type == LogType.STDERR )
                    log.severe( taskName + ": " + msg );
            }
        } );

        log.info( "Waiting for finish" );
        while ( true )
        {
            String state;
            Thread.sleep( 1000 );
            state = CromwellAPI.INSTANCE.getTaskStatus( taskId );
            if( state.equals( "Succeeded" ) )
                break;
            if( state.equals( "Failed" ) )
                throw new Exception( "WDL script failed" );//TODO: extract logs somehow
            if( !state.equals( "Not found" ) )
            {
                JSONObject logsResponse = CromwellAPI.INSTANCE.getLogs( taskId );
                logs.updateLogs( logsResponse );
            }
        }

        log.info( "WDL finished" );

        log.info( "Fetching outputs" );
        JSONObject outputs = CromwellAPI.INSTANCE.getOutputs( taskId );
        log.log( Level.INFO, "Outputs: " + outputs.toString() );
        //importResults( outputs, optionsJSON.getString( "final_workflow_outputs_dir" ) );
        return new Object[0];
    }


    public JSONObject getWorkflowOptionsJSON()
    {
        DataElementPath optionsPath = parameters.getJsonPath();
        JSONObject optionsJSON = new JSONObject();
        if( optionsPath != null && optionsPath.exists() )
        {
            String userOptionsStr = optionsPath.getDataElement( TextDataElement.class ).getContent();
            optionsJSON = new JSONObject( userOptionsStr );
        }
        DataElementPath outPath = parameters.getOutputPath();
        if( outPath != null )
        {
            DataCollectionUtils.createFoldersForPath( outPath );

            String name = outPath.getName();
            DataCollection<?> parent = outPath.optParentCollection();
            File folder = DataCollectionUtils.getChildFile( parent, name );
        
            optionsJSON.put( "use_relative_output_paths", true );
            optionsJSON.put( "final_workflow_outputs_dir", folder );
        }
        return optionsJSON;
    }

    private String getInputsJson() throws IOException
    {
        if( parameters.isUseJson() )
        {
            if( parameters.getJsonPath() != null )
            {
                TextDataElement jsonDe = parameters.getJsonPath().getDataElement( TextDataElement.class );
                return jsonDe.getContent();
            }
        }
        else
        {

            StringWriter stream = new StringWriter();
            try (BufferedWriter bw = new BufferedWriter( stream ))
            {
                bw.write( "{\n" );
                boolean first = true;
                for ( DynamicProperty dp : parameters.getSettings().getParameters() )
                {
                    Object value = dp.getValue();
                    if( value instanceof DataElementPath dep )
                        value = "\"" + dep.getName() + "\"";
                    else
                    {
                        String valueStr = value.toString();
                        if( valueStr.startsWith( "\"" ) && valueStr.endsWith( "\"" ) )
                            value = valueStr;
                        else
                            value = "\"" + valueStr + "\"";
                    }
                    if( !first )
                        bw.write( "," );
                    first = false;
                    bw.write( "\"" + dp.getName() + "\"" + " : " + value + "\n" );
                }
                bw.write( "}\n" );
            }
            return stream.toString();
        }
        return "";

    }



    private static List<DataElementPath> findWDLFiles(DataElementPath folder, boolean recursive)
    {
        List<DataElementPath> result = new ArrayList<>();
        for ( DataElementPath child : folder.getChildrenArray() )
        {
            DataElement de = child.optDataElement();
            if( de instanceof WDLScript )
            {
                result.add( child );
            }
            else if( de instanceof FolderCollection && recursive )
            {
                result.addAll( findWDLFiles( child, recursive ) );
            }
        }
        return result;
    }

    public static class IncludePath extends OptionEx
    {
        DataElementPath path;
        boolean recursive;

        public DataElementPath getPath()
        {
            return path;
        }

        public void setPath(DataElementPath path)
        {
            Object oldValue = this.path;
            this.path = path;
            firePropertyChange( "path", oldValue, path );
        }

        public boolean isRecursive()
        {
            return recursive;
        }

        public void setRecursive(boolean recursive)
        {
            boolean oldValue = this.recursive;
            this.recursive = recursive;
            firePropertyChange( "recursive", oldValue, recursive );
        }
    }

    public static class IncludePathBeanInfo extends BeanInfoEx2
    {
        public IncludePathBeanInfo()
        {
            super( IncludePath.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            super.initProperties();
            property( "path" ).inputElement( FolderCollection.class ).add();
            property( "recursive" ).add();
        }
    }

    public static class Parameters extends AbstractAnalysisParameters implements PropertyChangeListener, JSONBean
    {
        private static final Logger log = Logger.getLogger( Parameters.class.getName() );
        private DataElementPath wdlPath;
        private WorkflowSettings settings = new WorkflowSettings();
        IncludePath[] includes = new IncludePath[0];

        public Parameters()
        {
            settings.setParent( this );
        }

        public WorkflowSettings getSettings()
        {
            return settings;
        }

        @PropertyName("Parameters")
        public DynamicPropertySet getParameters()
        {
            return settings.getParameters();
        }

        public void setParameters(DynamicPropertySet parameters)
        {
            Object oldValue = settings.getParameters();
            settings.setParameters( parameters );
            firePropertyChange( "parameters", oldValue, parameters );
        }

        @PropertyName("Output")
        public DataElementPath getOutputPath()
        {
            return settings.getOutputPath();
        }

        public void setOutputPath(DataElementPath outputPath)
        {
            Object oldValue = settings.getOutputPath();
            settings.setOutputPath( outputPath );
            firePropertyChange( "outputPath", oldValue, outputPath );
        }

        @PropertyName("WDL")
        public DataElementPath getWdlPath()
        {
            return wdlPath;
        }

        public void setWdlPath(DataElementPath wdlPath)
        {
            DataElementPath oldValue = this.wdlPath;
            this.wdlPath = wdlPath;
            firePropertyChange( "wdlPath", oldValue, wdlPath );
            if( wdlPath != null && wdlPath.optDataElement() instanceof WDLScript )
            {
                WDLScript script = wdlPath.optDataElement( WDLScript.class );
                WDLImporter importer = new WDLImporter();
                File file = script.getFile();
                try
                {
                    String originalWDL = ApplicationUtils.readAsString( file );
                    importer.setScriptLoader( new FileScriptLoader( ScriptLoader.WDL_TYPE, file.getParentFile() ) );
                    Diagram diagram = null;
                    ScriptInfo info = importer.readScript( wdlPath.getName(), originalWDL );
                    diagram = new DiagramGenerator().generateDiagram( info, new WDLDiagramType().createDiagram( null, wdlPath.getName() ) );

                    if( diagram != null )
                    {
                        settings.initParameters( diagram );
                        settings.setParent( this );
                        firePropertyChange( "*", null, null );
                    }
                }
                catch (Exception e)
                {
                }
                IncludePath curFolder = new IncludePath();
                curFolder.setPath( wdlPath.getParentPath() );
                curFolder.setRecursive( true );
                setIncludes( new IncludePath[] { curFolder } );
            }
        }

        @PropertyName("Includes")
        @PropertyDescription("These folders will be searched for wdl files")
        public IncludePath[] getIncludes()
        {
            return includes;
        }

        public void setIncludes(IncludePath[] includes)
        {
            Object oldValue = this.includes;
            this.includes = includes;
            firePropertyChange( "includes", oldValue, includes );
            //if(inputStyle.equals( INPUT_STYLE_BIOUML_PARAMS ))
            //   updateBioumlParams();
        }

        @PropertyName("Parameters Json")
        public DataElementPath getJsonPath()
        {
            return settings.getJson();
        }

        public void setJsonPath(DataElementPath jsonPath)
        {
            DataElementPath oldValue = settings.getJson();
            settings.setJson( jsonPath );
            firePropertyChange( "jsonPath", oldValue, jsonPath );
        }

        @PropertyName("Use Json")
        public boolean isUseJson()
        {
            return settings.isUseJson();
        }

        public boolean isNotUseJson()
        {
            return !isUseJson();
        }

        public void setUseJson(boolean useJson)
        {
            boolean oldValue = settings.isUseJson();
            settings.setUseJson( useJson );
            firePropertyChange( "useJson", oldValue, useJson );
        }

        @PropertyName("Run in docker")
        public boolean isUseDocker()
        {
            return settings.isUseDocker();
        }

        public void setUseDocker(boolean useDocker)
        {
            boolean oldValue = settings.isUseDocker();
            settings.setUseDocker( useDocker );
            firePropertyChange( "useDocker", oldValue, useDocker );
        }

        public void reloadParameters(String wdl) throws Exception
        {
            Diagram diagram = new WDLImporter().generateDiagram( wdl, "analysisDiagram", null );
            settings.initParameters( diagram );
            firePropertyChange( "*", null, null );
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            // TODO Auto-generated method stub
        }
    }

    public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "outputPath" ).outputElement( DataCollection.class ).add();
            property( "wdlPath" ).inputElement( WDLScript.class ).add();
            property( "useJson" ).structureChanging().add();
            property( "jsonPath" ).inputElement( TextDataElement.class ).hidden( "isNotUseJson" ).add();
            addHidden( "parameters", "isUseJson" );
            property( "includes" ).structureChanging().add();
        }
    }
}
