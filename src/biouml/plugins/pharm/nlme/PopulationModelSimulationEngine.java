package biouml.plugins.pharm.nlme;

import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.script.LogScriptEnvironment;
import ru.biosoft.access.script.ScriptTypeRegistry;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.table.export.TableElementExporter;
import ru.biosoft.util.ApplicationUtils;
import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.TextUtil2;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import ru.biosoft.access.core.PluginEntry;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Node;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.TableElement;
import biouml.model.dynamics.TableElement.Variable;
import biouml.plugins.pharm.ParameterProperties;
import biouml.plugins.pharm.PopulationEModel;
import biouml.plugins.pharm.PopulationModuleType;
import biouml.plugins.pharm.PopulationVariable;
import biouml.plugins.pharm.StructuralModel;
import biouml.plugins.pharm.Type;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.SimulationEngineLogger;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.simulation.ResultListener;
import biouml.standard.type.Stub;

public class PopulationModelSimulationEngine extends SimulationEngine
{

    private static final String PARAMETER_NAME = "parameterName";

    public final static String TIME_VARIABLE = "time"; //Time variable name is reserved

    public final static String METHOD_REML = "Restricted log-likelihood (REML)";
    public final static String METHOD_ML = "Log-likelihood (ML)";

    public final static String RANDOM_DIAG = "Diagonal";
    public final static String RANDOM_DENSE = "Dense";

    private final static String NLME_R_SCRIPT_TEMPLATE = "resources/nlmeScript.vm";
    private final static String NLME_JAVA_MODEL_TEMPLATE = "resources/nlmeModel.vm";
    private final static String EXPERIMENT_INFO_TEMPLATE = "resources/experimentInfoTemplate.vm";

    public final static String TIME_COURSE = "Time course";
    public final static String STEADY_STATE = "Steady state";

    private static final String TYPE = "type";
    
    private String method = METHOD_ML;
    private String randomEffectsType = RANDOM_DIAG;
    private String simulationType = TIME_COURSE;

    private double aTolSteadyState = 1E-7;
    private double rTolSteadyState = 1E-7;
    
    private static final String DOSE_COLUMN_NAME = "Dose";
    private static final String TIME_COLUMN_NAME = "Time";
    private static final String SUBJECT_COLUMN_NAME = "Subject";

    private String doseVariable;
    private String resultVariable;

    public PopulationModelSimulationEngine()
    {
        log = new SimulationEngineLogger( PopulationModelSimulationEngine.class );
        engine = new JavaSimulationEngine();
        engine.setAbsTolerance( 1E-12 );
        engine.setRelTolerance( 1E-8 );
    }
    
    @Override
    public void setDiagram(Diagram diagram)
    {
        super.setDiagram(diagram);
        
        StructuralModel model = diagram.recursiveStream().select(StructuralModel.class).findAny().orElse(null);
        if (model != null)
        engine.setDiagram( model.getDiagram() );
    }
    
    public String[] getColumnNames()
    {
        return TableDataCollectionUtils.getColumnNames( tableData );
    }

    public double[] getColumn(String columnName)
    {
        DataType type = tableData.getColumnModel().getColumn(columnName).getType();
        if( type.equals(DataType.Integer) || type.equals(DataType.Float) )
            return TableDataCollectionUtils.getColumn(tableData, columnName);
        else
            return new double[tableData.getSize()];
    }

    public String generateDataPath()
    {
        try
        {
            TableElementExporter exporter = new TableElementExporter();
            Properties properties = new Properties();
            properties.setProperty("suffix", "txt");
            if( !exporter.init(properties) )
                throw new UnsupportedOperationException();
            TempFile file = TempFiles.file(".txt");
            exporter.doExport(tableData, file);
            return file.getPath();
        }
        catch( Exception e )
        {
            log.error("PopulationModelSimulationEngine: can not generate temp data file.");
        }
        return null;
    }

    public double[] getDataForSubject(String columnName) // first subject
    {
        double[] subject = getColumn( SUBJECT_COLUMN_NAME );
        double[] col = getColumn( columnName );
        int i = 1;
        while( subject[i] == subject[0] )
            i++;

        double[] result = new double[i];
        System.arraycopy( col, 0, result, 0, result.length );

        return result;
    }

    public void initParameterNames(StructuralModel model)
    {
        List<String> params = new ArrayList<>();
        model.stream().filter(de -> Type.TYPE_PORT.equals(de.getKernel().getType())).forEach(de ->
        {
            String parameterName = de.getAttributes().getValueAsString(PARAMETER_NAME);
            String type = de.getAttributes().getValueAsString(TYPE);
            switch (type)
            {
                case  ParameterProperties.DOSE_TYPE:
                {
                    doseVariable = parameterName;
                    break;
                }
                case  ParameterProperties.OBSERVED_TYPE:
                {
                    resultVariable = parameterName;
                    break;
                }
                default:
                    params.add(parameterName);
            }
        });
        parameterNames = params.toArray(new String[params.size()]);
    }

    private String[] parameterNames;
    public String[] getParameterNames()
    {
        return parameterNames;
    }

    private String doseColumn;
    private String observedColumn;
    private String timeColumn;
    public void initColumns(TableElement model) throws Exception
    {
        doseColumn = null;
        observedColumn = null;
        timeColumn = null;
        String formula = model.getFormula();
        String[] parts = TextUtil2.split( formula, '~' ); //TODO: check formula
        observedColumn = parts[0];
        DataCollection<PopulationVariable> variables = ( (PopulationEModel)executableModel ).getPopulationVariables();
        for( Variable var : model.getVariables() )
        {
            String varName = var.getName();
            if( varName.equals( "-" ) || varName.isEmpty() )
                continue;

            if( TIME_VARIABLE.equals( varName ) )
            {
                if( timeColumn != null )
                    log.error( "More than one table column corresponds to time variable! Only one of them will be used." );
                timeColumn = var.getColumnName();
                continue;
            }

            if( !variables.contains( varName ) )
            {
                log.error( "Table column " + var.getColumnName() + " corresponds to non-existing variable " + varName );
                continue;
            }

            PopulationVariable populationVar = variables.get( varName );
            Node node = (Node)populationVar.getDiagramElement();
            long doseNodes = node.edges().map(e-> e.getOtherEnd(node).getAttributes().getValueAsString( "type" )).filter(type->ParameterProperties.DOSE_TYPE.equals( type )).count();
            
            if (doseNodes > 1)
                log.error( "More than one table column corresponds to dose variable! Only one of them will be used." );
            else if (doseNodes > 0)
                doseColumn = var.getColumnName();
        }
    }

    private String[] doseTimes;
    public String[] getDoseTimes()
    {
        return doseTimes;
    }

    public int getResultIndex()
    {
        return this.engine.getVarIndexMapping().get( resultVariable );
    }

    public String getTimeColumnName()
    {
        return timeColumn;
    }
    
    public String getObservedColumnName()
    {
        return observedColumn;
    }

    public int getTimeIndex()
    {
        return TableDataCollectionUtils.getColumnIndexes( tableData, new String[] {timeColumn} )[0];
    }
    public int getSubjectIndex()
    {
        return TableDataCollectionUtils.getColumnIndexes( tableData, new String[] {SUBJECT_COLUMN_NAME} )[0];
    }
    public int getDoseIndex()
    {
        return TableDataCollectionUtils.getColumnIndexes( tableData, new String[] {doseColumn} )[0];
    }

    public boolean hasDoseColumn()
    {
        return doseColumn != null;
    }
    
    public boolean hasTimeColumn()
    {
        return timeColumn != null;
    }

    String dataFormula;
    public String getDataFormula()
    {
        return dataFormula;
    }

    private String[] doseVals;
    public String[] getDoseVals()
    {
        return doseVals;
    }

    private String odeModelName;
    public String getOdeModelName()
    {
        return odeModelName;
    }

    public String getExpInfoName()
    {
        return diagram == null ? "" : escapeWrongSymbols(diagram.getName()) + "_info";
    }

    public String getNLMEModelName()
    {
        return diagram == null ? "" : escapeWrongSymbols(diagram.getName()) + "_nlme";
    }

    JavaSimulationEngine engine = new JavaSimulationEngine();

    TableDataCollection tableData;

    public void init() throws Exception
    {
        Diagram innerDiagram = null;
        StructuralModel model = null;
        TableElement te = null;
        for( DiagramElement de : diagram )
        {
            if( de.getRole() instanceof TableElement )
            {
                te = de.getRole( TableElement.class );
                tableData = te.getTable();
                dataFormula = te.getFormula();
            }
            else if( de instanceof StructuralModel )
            {
                model = (StructuralModel)de;
                innerDiagram = ( (StructuralModel)de ).getDiagram();
            }
        }

        if( innerDiagram == null )
            throw new Exception( "Structural model not found" );

        if( tableData == null )
            throw new Exception( "Table data not found" );

        if( dataFormula == null )
            throw new Exception( "Please specify table data formula" );

        initParameterNames( model );
        initColumns( te );
        innerDiagram = preprocess( innerDiagram, tableData );

        engine.setDiagram( innerDiagram );
    }

    //Preprocessing issues
    public Diagram preprocess(Diagram diagram, TableDataCollection tableData) throws Exception
    {
        Diagram result = diagram.clone( null, diagram.getName() );

        if( tableData == null )
            return result;

        if( tableData.getColumnModel().hasColumn( "Dose" ) )
            addDosingEvent( result );

        return result;
    }


    public void addDosingEvent(Diagram diagram) throws Exception
    {
        EModel emodel = diagram.getRole(EModel.class);
        double[] doseColumn = getDataForSubject( DOSE_COLUMN_NAME ); //for now only data for first subject takes into account
        List<String> doseTimeList = new ArrayList<>();
        List<String> doseValList = new ArrayList<>();
        for( double dose : doseColumn )
        {
            if( dose != 0 )
            {
                String nextDoseTime = generateUniqueVariable( emodel, "doseTime" );
                String nextDoseVal = generateUniqueVariable( emodel, "doseVal" );

                String name = DefaultSemanticController.generateUniqueNodeName( diagram, biouml.standard.type.Type.MATH_EVENT );

                Node node = new Node( diagram, new Stub( null, name, biouml.standard.type.Type.MATH_EVENT ) );
                Event event = new Event( node );
                event.setTriggerInitialValue( false );
                event.setTrigger( "time > " + nextDoseTime );
                event.addEventAssignment( new Assignment( doseVariable, nextDoseVal ), false );
                node.setRole( event );
                diagram.put( node );

                doseTimeList.add( nextDoseTime );
                doseValList.add( nextDoseVal );
            }
        }
        int doseNumber = doseTimeList.size();
        doseTimes = doseTimeList.toArray( new String[doseNumber] );
        doseVals = doseValList.toArray( new String[doseNumber] );
    }
    
    protected synchronized String generateUniqueVariable(EModel emodel, String base)
    {
        if( !emodel.containsVariable( base ) )
            return base;
        int i = 1;
        String v = base + String.valueOf( i );
        while( emodel.containsVariable( v ) )
            v = base + String.valueOf( i++ );
        return v;
    }
    
    @Override
    public File[] generateModel(boolean forceRewrite) throws Exception
    {
        log.info(getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
        engine.setOutputDir( getOutputDir() );
        Model structModel = engine.createModel();
        odeModelName = structModel.getClass().getName();

        File dir = new File( getOutputDir() );
        dir.mkdirs();

        File experimentInfo = new File( dir, getExpInfoName() + ".java" );
        File nlmeModel = new File( dir, getNLMEModelName() + ".java" );

        try (BufferedWriter bw = com.developmentontheedge.application.ApplicationUtils.utfWriter( experimentInfo ) )
        {
            generateCode( EXPERIMENT_INFO_TEMPLATE, bw );
        }
        catch( Exception ex )
        {
            throw new Exception( "Error during experiment info generating: " + ex.getMessage() );
        }

        try (BufferedWriter bw = com.developmentontheedge.application.ApplicationUtils.utfWriter( nlmeModel ) )
        {
            generateCode( NLME_JAVA_MODEL_TEMPLATE, bw );
        }
        catch( Exception ex )
        {
            throw new Exception( "Error during NLME model generating: " + ex.getMessage() );
        }

        log.info( "Model " + diagram.getName() + ": Java code generating..." );

        return new File[] {experimentInfo, nlmeModel};
    }

    @Override
    public MixedEffectModel createModel() throws Exception
    {
        init();
        Object[] objs = compileModel( generateModel( true ), true, getOutputDir() );
        return (MixedEffectModel)objs[objs.length - 1];
    }

    @Override
    public Object getSolver()
    {
        return null;
    }

    protected InputStream getTemplateInputStream(String templateName)
    {
        return PopulationModuleType.class.getResourceAsStream( templateName );
    }

    public void generateCode(String templateName, Writer writer) throws Exception
    {
        RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
        SimpleNode node = runtimeServices.parse(new InputStreamReader(getTemplateInputStream(templateName), StandardCharsets.UTF_8),
                "fileName");
        Template velocityTemplate = new Template();
        velocityTemplate.setRuntimeServices(runtimeServices);
        velocityTemplate.setData(node);
        velocityTemplate.initDocument();
        Velocity.init();
        VelocityContext context = new VelocityContext();
        context.put("rutil", new RUtils());
        context.put("de", this);
        context.put("creationTime", new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(Calendar.getInstance().getTime()));
        velocityTemplate.merge(context, writer);
    }


    
    protected String outputDir = TempFiles.path( "simulation" ).getAbsolutePath();

    @Override
    public String getOutputDir()
    {
        return outputDir;
    }

    @Override
    public void setOutputDir(String outputDir)
    {
        String oldValue = this.outputDir;
        this.outputDir = outputDir;
        firePropertyChange( "outputDir", oldValue, outputDir );
    }

    @Override
    public String getEngineDescription()
    {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public String getVariableCodeName(String varName)
    {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public String getVariableCodeName(String diagramName, String varName)
    {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public Map<String, Integer> getVarIndexMapping()
    {
        return this.engine.getVarIndexMapping();
    }

    @Override
    public void setSolver(Object solver)
    {
        // TODO Auto-generated method stub

    }
    @Override
    public String simulate(Model model, ResultListener[] resultListeners) throws Exception
    {
        StringWriter writer = new StringWriter();
        generateCode( NLME_R_SCRIPT_TEMPLATE, writer );
        String rCode = writer.getBuffer().toString();

        try
        {
            NlmeUtils.callConsole( rCode );
        }
        catch( Exception ex )
        {
            //try another way
            log.info( rCode );
            executeRcode( rCode );
        }
        return "";
    }

    @Override
    protected List<PluginEntry> getClassPathEntries()
    {
        try
        {
            return Arrays.asList( ApplicationUtils.resolvePluginPath( "biouml.plugins.pharm:src.jar" ) );
        }
        catch( Exception e )
        {
            return Collections.emptyList();
        }
    }

    protected void executeRcode(String rCode) throws Exception
    {
        log.info( "Connecting to R..." );
        final LogScriptEnvironment env = new LogScriptEnvironment( log.getLogger() );

        log.info("Invoking R command...");
        SecurityManager.runPrivileged(() -> ScriptTypeRegistry.execute("R", rCode, env, false));
    }

    @PropertyName ( "Simulation engine" )
    public JavaSimulationEngine getEngine()
    {
        return engine;
    }
    public void setEngine(JavaSimulationEngine engine)
    {
        this.engine = engine;
    }

    public String getMethodScript()
    {
        switch( method )
        {
            case METHOD_ML:
                return "ML";
            case METHOD_REML:
                return "REML";
            default:
                return "ML";
        }
    }

    @PropertyName ( "NLME Method" )
    @PropertyDescription ( "NLME Method." )
    public String getMethod()
    {
        return method;
    }
    public void setMethod(String method)
    {
        this.method = method;
    }
     
    @PropertyName("Random effects")
    @PropertyDescription("Random effects type.")
    public String getRandomEffectsType()
    {
        return randomEffectsType;
    }
    public void setRandomEffectsType(String randomEffectsType)
    {
        this.randomEffectsType = randomEffectsType;
    }

    public double getAtol()
    {
        return engine.getAbsTolerance();
    }

    public double getRtol()
    {
        return engine.getRelTolerance();
    }

    public boolean isRandom(PopulationVariable var)
    {
        return Type.TYPE_STOCHASTIC.equals( var.getType() );
    }
   
    @PropertyName("Absolute tolerance for steady state")
    @PropertyDescription("Absolute tolearance for steady state finding.")
    public double getATolSteadyState()
    {
        return aTolSteadyState;
    }
    public void setATolSteadyState(double aTolSteadyState)
    {
        this.aTolSteadyState = aTolSteadyState;
    }
    
    
    @PropertyName("Relative tolerance for steady state")
    @PropertyDescription("Relative tolearance for steady state finding.")
    public double getRTolSteadyState()
    {
        return rTolSteadyState;
    }
    public void setRTolSteadyState(double rTolSteadyState)
    {
        this.rTolSteadyState = rTolSteadyState;
    }
    
    
    //Util methods for template to call
    
    public String[] getPlugins()
    {
        return NlmeUtils.getPluginPathes();
    }

    public String getAbsoluteOutputPath()
    {
        return new File( getOutputDir() ).getAbsolutePath().replace( "\\", "/" );
    }

    public String join(ArrayList<String> array, String delim)
    {
        return StringUtils.join( array, delim );
    }
    
    @Override
    public boolean hasVariablesToPlot()
    {
        return true;
    }
    
    @Override
    public Map<String, Integer> getVarPathIndexMapping()
    {
        return getVarIndexMapping();
    }

}
