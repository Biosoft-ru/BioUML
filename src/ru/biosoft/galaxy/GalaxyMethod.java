package ru.biosoft.galaxy;

import java.io.File;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringEscapeUtils;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;

import biouml.plugins.ensembl.access.EnsemblDatabase;
import biouml.plugins.ensembl.access.EnsemblDatabaseSelector;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.CodePrivilege;
import ru.biosoft.access.security.CodePrivilegeType;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.bsa.Track;
import ru.biosoft.galaxy.GalaxyAnalysisParameters.GalaxyParameter;
import ru.biosoft.galaxy.ProcessSupport.SystemUser;
import ru.biosoft.galaxy.javascript.JavaScriptGalaxy;
import ru.biosoft.galaxy.parameters.ArrayParameter;
import ru.biosoft.galaxy.parameters.BaseFileParameter;
import ru.biosoft.galaxy.parameters.ConfigParameter;
import ru.biosoft.galaxy.parameters.FileParameter;
import ru.biosoft.galaxy.parameters.MetaParameter;
import ru.biosoft.galaxy.parameters.Parameter;
import ru.biosoft.jobcontrol.SubFunctionJobControl;

/**
 * Adaptor to Galaxy tool with {@link AnalysisMethod} interface
 */
@CodePrivilege( CodePrivilegeType.SHARED_FOLDER_ACCESS )
public class GalaxyMethod extends AnalysisMethodSupport
{
    protected static final Logger glog = Logger.getLogger( GalaxyMethod.class.getName() );

    protected GalaxyAnalysisParameters parameters;
    protected GalaxyMethodInfo methodInfo;
    protected Command command;
    protected GalaxyAnalysisJobControl jobControl = new GalaxyAnalysisJobControl();

    public GalaxyMethod(DataCollection<?> origin, String name)
    {
        super(origin, name, (AnalysisParameters)null);
    }

    public GalaxyMethodInfo getMethodInfo()
    {
        return methodInfo;
    }

    public void setMethodInfo(GalaxyMethodInfo methodInfo)
    {
        this.methodInfo = methodInfo;
    }

    @Override
    public AnalysisParameters getParameters()
    {
        if( parameters == null )
        {
            parameters = new GalaxyAnalysisParameters(methodInfo);
        }
        return parameters;
    }

    @Override
    protected AnalysisParameters getDefaultParameters() throws Exception
    {
        return new GalaxyAnalysisParameters( methodInfo );
    }

    protected Command getCommand()
    {
        if( command == null )
        {
            Command templateCommand = methodInfo.getCommand();
            command = new Command(templateCommand.getInterpreter(), templateCommand.getCommand(), templateCommand.getToolDir(), methodInfo);
        }
        return command;
    }

    @Override
    public GalaxyAnalysisJobControl getJobControl()
    {
        return jobControl;
    }

    @Override
    public void setParameters(AnalysisParameters parameters)
    {
        if( ! ( parameters instanceof GalaxyAnalysisParameters ) )
            throw new IllegalArgumentException();
        this.parameters = (GalaxyAnalysisParameters)parameters;
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        for( DynamicProperty property : parameters )
        {
            if( property instanceof GalaxyParameter && !property.isHidden())
            {
                Parameter p = ( (GalaxyParameter)property ).getParameter();
                try
                {
                    p.validate();
                }
                catch( IllegalArgumentException e )
                {
                    throw new IllegalArgumentException(property.getDisplayName() + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Run galaxy method with current parameters
     */
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        log.info("Preparing input data...");

        Command command;
        SystemUser systemUser;
        try
        {
            command = getCommand();
            systemUser = ProcessSupport.getSystemUser();

            for( DynamicProperty property : parameters )
            {
                if( property instanceof GalaxyParameter )
                {
                    Parameter p = ( (GalaxyParameter)property ).getParameter();
                    GalaxyFactory.correctParameter(p, command.getTempPath(), command.getTempPath());
                }
            }
            parameters.exportParameters();
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Internal error", e);
            throw e;
        }

        jobControl.setPreparedness(5);
        log.info( "Preparing Galaxy parameters..." );
        List<ru.biosoft.access.core.DataElementPath> results = new ArrayList<>();

        ParametersContainer infoParameters = parameters.getParametersContainer();
        boolean ok = false;
        try
        {
            jobControl.pushProgress(5, 80);
            log.info( "Executing Galaxy command..." );
            ok = command.execute(infoParameters, systemUser, log, new SubFunctionJobControl(jobControl));
            glog.info( "Galaxy command = " + command.getRealCommandLine() );
            jobControl.popProgress();
            if( ok )
            {
                jobControl.setPreparedness(80);
                log.info("Importing output...");
                //delete temporary files and save output to data collection
                for( DynamicProperty property : parameters)
                {
                    if( property instanceof GalaxyParameter )
                    {
                        Parameter p = ( (GalaxyParameter)property ).getParameter();
                        if( p.isOutput() && ( p instanceof FileParameter ) && !( (GalaxyParameter)property ).getDescriptor().isHidden() )
                        {
                            Object newValue = property.getValue();
                            if( newValue instanceof DataElementPath )
                            {
                                DataElementPath dePath = (DataElementPath)newValue;
                                dePath.remove();
                                File file = ( (FileParameter)p ).getFile();
                                if( file.exists() )
                                {
                                    Object galaxyFormat = p.getAttributes().get("format");
                                    if(p.getAttributes().get("source") != null)
                                    {
                                        Parameter sourceParameter = infoParameters.getParameter(p.getAttributes().get("source").toString());
                                        if(sourceParameter.getAttributes().get("usedFormat") != null)
                                            galaxyFormat = sourceParameter.getAttributes().get("usedFormat");
                                    }
                                    DataCollection<?> parent = dePath.getParentCollection();
                                    DataElementImporter importer = FormatRegistry.getImporter((String)galaxyFormat, dePath, file, ((BaseFileParameter)p).getFormatProperties());
                                    DataElement result = importer.doImport(parent, file, dePath.getName(), null, log);
                                    if( p.getAttributes().get("source") != null )
                                    {
                                        Parameter sourceParameter = infoParameters.getParameter(p.getAttributes().get("source").toString());
                                        if( sourceParameter instanceof FileParameter )
                                        {
                                            DataElementPath sourcePath = ( (FileParameter)sourceParameter ).getDataElementPath();
                                            if( sourcePath != null )
                                            {
                                                DataCollection<?> sourceDc = sourcePath.optDataCollection();
                                                if( sourceDc != null && result instanceof DataCollection )
                                                {
                                                    DataCollectionUtils.copyPersistentInfo((DataCollection<?>)result, sourceDc);
                                                    String seqCollection = sourceDc.getInfo().getProperty(Track.SEQUENCES_COLLECTION_PROPERTY);
                                                    if( seqCollection != null )
                                                        ( (DataCollection<?>)result ).getInfo().getProperties().setProperty(Track.SEQUENCES_COLLECTION_PROPERTY, seqCollection);
                                                }
                                            }
                                        }
                                    }
                                    MetaParameter dbkey = p.getMetadata().get( "dbkey" );
                                    if( dbkey != null )
                                    {
                                        String dbkeyValue = (String)dbkey.getValue();
                                        if( dbkeyValue != null && !dbkeyValue.isEmpty() && result instanceof DataCollection )
                                        {
                                            ( (DataCollection<?>)result ).getInfo().getProperties()
                                                    .setProperty( Track.GENOME_ID_PROPERTY, dbkeyValue );
                                        }
                                    }
                                    if( result instanceof DataCollection )
                                    {
                                        DataCollection<?> dc = (DataCollection<?>)result;
                                        Properties properties = dc.getInfo().getProperties();

                                        if( properties.containsKey( Track.GENOME_ID_PROPERTY ) )
                                        {
                                            String genomeId = properties.getProperty( Track.GENOME_ID_PROPERTY );
                                            EnsemblDatabase ensembl = getEnsembl( dc );
                                            if( ensembl == null || !genomeId.equals( ensembl.getGenomeBuild() ) )
                                            {
                                                for( EnsemblDatabase db : EnsemblDatabaseSelector.getEnsemblDatabases() )
                                                    if( db.getGenomeBuild() != null && db.getGenomeBuild().equals( genomeId ) )
                                                    {
                                                        properties.setProperty( Track.SEQUENCES_COLLECTION_PROPERTY,
                                                                db.getPrimarySequencesPath().toString() );
                                                        break;
                                                    }
                                            }
                                        }
                                    }
                                    dePath.save( result );
                                    if( dePath.exists() )
                                        results.add(dePath);
                                }
                                else
                                {
                                    log.warning("Cannot import output '" + property.getName() + "': file doesn't exist");
                                }
                            }
                        }

                        if( p instanceof FileParameter )
                        {
                            File file = ( (FileParameter)p ).getFile();
                            if(file != null)
                                file.delete();
                        }

                        //remove configuration files
                        if( p instanceof ConfigParameter )
                        {
                            File file = ( (ConfigParameter)p ).getConfigFile();
                            if(file != null)
                                file.delete();
                        }
                    }
                }

                for(DataElementPath resultPath : results)
                    writeProperties( resultPath.optDataElement() );

                jobControl.resultsAreReady(results.toArray());
            }
            else
            {
                throw new Exception("Galaxy process failed");
            }
        }
        finally
        {
            ProcessSupport.releaseSystemUser(systemUser);
            if(ok || !GalaxyDataCollection.isPreserveOnError())
            {
                ApplicationUtils.removeDir(command.getTempPath());
                ApplicationUtils.removeDir(command.getWorkDir());
            }
        }
        return null;
    }

    private EnsemblDatabase getEnsembl(DataCollection<?> track)
    {
        try{
            String chrPathStr = track.getInfo().getProperty( Track.SEQUENCES_COLLECTION_PROPERTY );
            DataElementPath ensPath = DataElementPath.create( chrPathStr ).getParentPath().getParentPath();
            return new EnsemblDatabase( ensPath );
        } catch(Exception e)
        {
            return null;
        }
    }

    /**
     * Execute Galaxy test
     */
    public boolean processTest(GalaxyMethodTest test, Writer errors) throws Exception
    {
        boolean result = true;

        SystemUser user = null;
        for( Parameter p : test.getParameters().values() )
            GalaxyFactory.correctParameter(p, GalaxyDataCollection.getGalaxyDistFiles().getTestDataFolder(),
                    getCommand().getTempPath());

        try
        {
            //TODO:APPENDER
            /*WriterAppender writerAppender = new WriterAppender(new SimpleLayout(), errors);
            writerAppender.setThreshold(Level.ERROR);
            log.addAppender(writerAppender);*/
            methodInfo.getCommand().execute(test.getParameters(), null, log, null);
            if( !checkResult(test, GalaxyDataCollection.getGalaxyDistFiles().getTestDataFolder(), errors, user) )
            {
                result = false;
            }
        }
        catch( Exception e )
        {
            errors.write("Error: " + e.toString() + "\n'");
            result = false;
        }
        return result;
    }

    protected boolean checkResult(GalaxyMethodTest test, File testData, Writer errors, SystemUser user) throws Exception
    {
        boolean result = true;
        for( Map.Entry<String, Parameter> e : test.getParameters().entrySet() )
        {
            Parameter p = e.getValue();
            if( p.isOutput() && p instanceof FileParameter )
            {
                String name = ( (FileParameter)p ).getName();
                if( name == null ) //unused output, see solid2fastq.xml for example
                    continue;
                if( !test.getComparator(e.getKey()).compare(new File(testData, name),
                        new File(command.getTempPath(), name), errors) )
                {
                    result = false;
                }
            }
        }
        return result;
    }

    @Override
    public String generateJavaScript(Object parameters)
    {
        if( ! ( parameters instanceof GalaxyAnalysisParameters ) )
            return null;
        GalaxyAnalysisParameters params = (GalaxyAnalysisParameters)parameters;
        GalaxyAnalysisParameters defaultParams = new GalaxyAnalysisParameters(methodInfo);
        StringBuilder parametersStr = new StringBuilder();
        for( DynamicProperty property : params )
        {
            if( property.isHidden() )
                continue;
            DynamicProperty defaultProperty = defaultParams.getProperty(property.getName());
            String propertyValue = ( (GalaxyParameter)property ).getValueString();
            String defaultValue = defaultProperty == null ? null : ( (GalaxyParameter)defaultProperty ).getValueString();
            if( ( defaultValue == null && propertyValue != null ) || ( defaultValue != null && !defaultValue.equals( propertyValue ) ) )
            {
                if( parametersStr.length() > 0 )
                    parametersStr.append(", ");
                String simpleName = property.getName();
                if( simpleName.lastIndexOf(GalaxyAnalysisParameters.NESTED_PARAMETER_DELIMETER) >= 0 )
                    simpleName = simpleName.substring(simpleName.lastIndexOf(GalaxyAnalysisParameters.NESTED_PARAMETER_DELIMETER) + 1);
                if( ( (GalaxyParameter)property ).getParameter() instanceof ArrayParameter )
                    parametersStr.append("'" + StringEscapeUtils.escapeJavaScript(simpleName) + "': " + propertyValue);
                else
                    parametersStr.append("'" + StringEscapeUtils.escapeJavaScript(simpleName) + "': '"
                            + StringEscapeUtils.escapeJavaScript(propertyValue) + "'");
            }
        }
        StringBuilder result = new StringBuilder("galaxy.");
        String[] pathComponents = DataElementPath.create(methodInfo).getPathComponents();
        result.append(JavaScriptGalaxy.convertName(pathComponents[pathComponents.length - 2])).append(".")
                .append(JavaScriptGalaxy.convertName(pathComponents[pathComponents.length - 1]));
        result.append("(");
        if( parametersStr.length() > 0 )
        {
            result.append("{").append(parametersStr).append("}");
        }
        result.append(")");
        return result.toString();
    }

    public class GalaxyAnalysisJobControl extends AnalysisJobControl
    {
        public GalaxyAnalysisJobControl()
        {
            super(GalaxyMethod.this);
        }

        @Override
        public void terminate()
        {
            super.terminate();
            if( getCommand() != null )
                getCommand().interrupt();
        }
    }
}
