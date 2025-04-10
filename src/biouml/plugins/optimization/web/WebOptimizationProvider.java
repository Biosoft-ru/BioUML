package biouml.plugins.optimization.web;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentFactory.Policy;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.undo.TransactionUndoManager;
import com.developmentontheedge.log.PatternFormatter;
import com.developmentontheedge.log.WriterHandler;
import com.eclipsesource.json.Json;

import biouml.model.Diagram;
import biouml.model.Role;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.optimization.ExperimentalTableSupport;
import biouml.plugins.optimization.Optimization;
import biouml.plugins.optimization.OptimizationConstraint;
import biouml.plugins.optimization.OptimizationConstraintCalculator;
import biouml.plugins.optimization.OptimizationExperiment;
import biouml.plugins.optimization.OptimizationMethodRegistry;
import biouml.plugins.optimization.OptimizationParameters;
import biouml.plugins.optimization.OptimizationUtils;
import biouml.plugins.optimization.ParameterEstimationProblem;
import biouml.plugins.optimization.SimulationTaskRegistry;
import biouml.plugins.optimization.diagram.OptimizationDiagramManager;
import biouml.plugins.simulation.SimulationTaskParameters;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.Util;
import biouml.standard.state.State;
import biouml.workbench.diagram.SetInitialValuesAction;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.log.JULLoggerAdapter;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.task.RunnableTask;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.analysis.optimization.OptimizationMethod;
import ru.biosoft.analysis.optimization.OptimizationMethod.OptimizationMethodJobControl;
import ru.biosoft.analysis.optimization.OptimizationMethodParameters;
import ru.biosoft.analysis.optimization.OptimizationProblem;
import ru.biosoft.analysis.optimization.Parameter;
import ru.biosoft.analysiscore.AnalysisDPSUtils;
import ru.biosoft.analysiscore.AnalysisMethodInfo;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListenerAdapter;
import ru.biosoft.journal.Journal;
import ru.biosoft.journal.JournalRegistry;
import ru.biosoft.server.Connection;
import ru.biosoft.server.JSONUtils;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.WebJob;
import ru.biosoft.server.servlets.webservices.WebServicesServlet;
import ru.biosoft.server.servlets.webservices.providers.WebDiagramsProvider;
import ru.biosoft.server.servlets.webservices.providers.WebJSONProviderSupport;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.tasks.TaskManager;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.JsonUtils;
import ru.biosoft.util.TextUtil2;

public class WebOptimizationProvider extends WebJSONProviderSupport
{
    protected static final Logger log = Logger.getLogger(WebOptimizationProvider.class.getName());

    private static void sendMethodList(JSONResponse response) throws IOException
    {
        response.sendJSON(
                OptimizationMethodRegistry.getOptimizationMethodNames().map( Json::value ).collect( JsonUtils.toArray() ) );
    }


    private static void sendOptimizationInfo(Optimization optimization, JSONResponse response)
    {
        try
        {
            JSONObject info = new JSONObject();
            OptimizationMethod<?> method = optimization.getOptimizationMethod();
            info.put("diagramPath", ( (OptimizationMethodParameters)method.getParameters() ).getDiagramPath());
            info.put("method", method.getName());
            info.put("optimizationDiagram", optimization.getOptimizationDiagramPath().toString());
            ComponentModel model = ComponentFactory.getModel(method.getParameters(), Policy.DEFAULT, true);
            JSONObject jsonDictionaries = new JSONObject();
            JSONArray jsonProperties = JSONUtils.getModelAsJSON(model);
            JSONObject methodData = new JSONObject();
            methodData.put(JSONResponse.ATTR_DICTIONARIES, jsonDictionaries);
            methodData.put(JSONResponse.ATTR_VALUES, jsonProperties);
            info.put("methodData", methodData);

            OptimizationDiagramManager diagramManager = new OptimizationDiagramManager(optimization);
            if( diagramManager.isDiagramChanged() )
            {
                response.sendAdditionalJSON(info);
            }
            else
            {
                response.sendJSON(info);
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not get optimization info", e);
        }
    }

    private static Optimization getOptimization(DataElementPath path)
    {
        Optimization optimization = path.optDataElement(Optimization.class);
        if( optimization == null )
        {
            Object optimizationObj = WebServicesServlet.getSessionCache().getObject(path.toString());
            if( optimizationObj instanceof Optimization )
            {
                optimization = (Optimization)optimizationObj;
            }
        }
        WebServicesServlet.getSessionCache().addObject(path.toString(), optimization, true);
        return optimization;
    }

    private static Optimization getOptimization(BiosoftWebRequest arguments) throws WebException
    {
        DataElementPath path = arguments.getDataElementPath();
        Optimization optimization = getOptimization(path);
        if(optimization == null) throw new WebException("EX_QUERY_NO_OPTIMIZATION", path);
        return optimization;
    }

    private static SubDiagram getSubDiagram(BiosoftWebRequest arguments, Diagram parentDiagram) throws WebException
    {
        if( arguments.get( "subDiagram" ) != null )
        {
            String subPath = arguments.getString( "subDiagram" );
            return Util.getSubDiagram( parentDiagram, subPath );
        }
        return null;
    }

    private static Set<String> getJSONRows(BiosoftWebRequest arguments) throws WebException
    {
        return new HashSet<>( Arrays.asList( arguments.getStrings( "jsonrows" ) ) );
    }

    private static void sendExperimentsCommons(JSONResponse response)
    {
        try
        {
            JSONObject data = new JSONObject();
            data.put("wmethods", ExperimentalTableSupport.WeightMethod.getWeightMethods());
            data.put("exptypes", OptimizationExperiment.ExperimentType.getExperimentTypes());
            response.sendJSON(data);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not send experiments response", e);
        }
    }

    private static void sendDiagramStates(Optimization optimization, JSONResponse response)
    {
        try
        {
            Diagram diagram = optimization.getDiagram();
            if( diagram == null )
            {
                response.error("Can not get optimization diagram");
                return;
            }
            JSONArray data = new JSONArray();
            diagram.states().map( State::getName ).forEach( data::put );
            DataCollection<Variable> vars = diagram.getRole( EModel.class ).getVariables();
            List<String> nameList = vars.getNameList();
            JSONArray diagramVars = new JSONArray();
            for( String paramName : nameList )
            {
                JSONObject var = new JSONObject();

                var.put("value", paramName);
                try
                {
                    Object variable = vars.get(paramName);
                    if( variable instanceof VariableRole )
                    {
                        paramName = ( (VariableRole)variable ).getDiagramElement().getTitle();
                    }
                }
                catch( Exception e )
                {
                }
                var.put("name", paramName);
                diagramVars.put(var);
            }
            JSONObject result = new JSONObject();
            result.put("states", data);
            result.put("diagramvars", diagramVars);
            response.sendJSON(result);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not send states response", e);
        }
    }

    private static void sendExperiments(Optimization optimization, JSONResponse response)
    {
        try
        {
            List<OptimizationExperiment> experiments = optimization.getParameters()
                    .getOptimizationExperiments();
            JSONObject data = new JSONObject();

            for( OptimizationExperiment exp : experiments )
            {
                JSONObject jsExp = new JSONObject();
                jsExp.put("diagst", exp.getDiagramStateName());
                jsExp.put("file", exp.getFilePath().getName());
                jsExp.put("wmethod", exp.getWeightMethod());
                jsExp.put("exptype", exp.getExperimentType());
                jsExp.put("cellline", exp.getCellLine());

                data.put(exp.getName(), jsExp);
            }
            response.sendJSON(data);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not send experiments response", e);
        }
    }

    private static void startOptimization(final Optimization optimization, String methodName, JSONArray jsonOptions,
            final String jobID, JSONResponse response)
    {
        try
        {
            OptimizationMethod<?> currentMethod = correctMethodParameters(methodName, optimization.getOptimizationMethod(), jsonOptions);
            optimization.setOptimizationMethod(currentMethod);

            //Workaround to use correct simulation engine
            //TODO: remove code below if beans are always the same
            String objectName = "beans/model/" + DataElementPath.create( optimization );
            Map<String, SimulationTaskParameters> taskParameters = optimization.getParameters().getSimulationTaskParameters();
            for( Map.Entry<String, SimulationTaskParameters> entry : taskParameters.entrySet() )
            {
                String innerObjectName = objectName + "/" + entry.getKey();
                Object savedSimulationTaskParameters = WebServicesServlet.getSessionCache().getObject( innerObjectName );
                if( savedSimulationTaskParameters != null )
                {
                    SimulationTaskParameters singleTaskParameters = entry.getValue();
                    Object currentSimulationTaskParameters = singleTaskParameters.getParametersBean();
                    if( singleTaskParameters != null && currentSimulationTaskParameters != savedSimulationTaskParameters )
                    {
                        BeanUtil.copyBean( savedSimulationTaskParameters, currentSimulationTaskParameters );
                    }
                }
            }

            final OptimizationConstraintCalculator calculator = new OptimizationConstraintCalculator();

            try
            {
                OptimizationUtils.checkOptimization(optimization, calculator);
            }
            catch( Exception e )
            {
                response.error(e.getMessage());
                return;
            }
            final OptimizationMethod<?> method = optimization.getOptimizationMethod();
            final OptimizationMethodJobControl jobControl = method.getJobControl();

            WebJob webJob = WebJob.getWebJob(jobID);
            Logger log = webJob.getJobLogger();
            method.setLogger( log );

            Writer writer = new Writer()
            {
                StringBuffer buffer = new StringBuffer();

                @Override
                public void close() throws IOException
                {
                }

                @Override
                public void flush() throws IOException
                {
                    webJob.addJobMessage( buffer.toString() );
                    buffer = new StringBuffer();
                }

                @Override
                public void write(char[] bytes, int offset, int len) throws IOException
                {
                    buffer.append( bytes, offset, len );
                }
            };

            Handler webLogHandler = new WriterHandler( writer, new PatternFormatter( "%4$s - %5$s%n" ) );
            webLogHandler.setLevel( Level.INFO );
            log.setLevel( Level.ALL );
            log.addHandler( webLogHandler );
            jobControl.addListener( new JobControlListenerAdapter()
            {
                @Override
                public void jobTerminated(JobControlEvent event)
                {
                    log.removeHandler( webLogHandler );
                }
            } );

            Journal journal = JournalRegistry.getCurrentJournal();
            TaskManager taskManager = TaskManager.getInstance();
            TaskInfo task = taskManager.addTask( TaskInfo.WORKFLOW, DataElementPath.create( optimization ), jobControl,
                    new JULLoggerAdapter( log ), journal,
                    AnalysisDPSUtils.getParametersAsDynamicPropertySet( optimization.getParameters() ), false, null );
            task.setTransient("parameters", optimization.getParameters());
            webJob.setTask(task);



            TaskPool.getInstance().submit(new RunnableTask("Optimization (user: "+SecurityManager.getSessionUser()+")", () -> {
                optimization.setOptimizationMethod(method);
                try
                {
                    OptimizationParameters params = optimization.getParameters();
                    OptimizationProblem problem = new ParameterEstimationProblem(params, calculator);

                    method.setOptimizationProblem(problem);

                    jobControl.run();

                    if( method.getAnalysisResults() != null )
                    {
                        try
                        {
                            WebOptimizationProvider.refreshDiagramNodes(optimization);
                        }
                        catch( Exception e1 )
                        {
                        }
                    }
                }
                catch( Throwable e2 )
                {
                    ExceptionRegistry.log(e2);
                }
            }));

            response.sendString("Started");
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot run optimization", e);
        }
    }

    private static void undo(Optimization optimization) throws WebException
    {
        try
        {
            TransactionUndoManager undoManager = WebDiagramsProvider.getUndoManager(optimization.getOptimizationDiagram());
            if( undoManager.canUndo() )
            {
                undoManager.undo();
            }
        }
        catch( Exception e )
        {
            throw new WebException(e, "EX_INTERNAL_DURING_ACTION", "undo");
        }
    }

    private static void redo(Optimization optimization) throws WebException
    {
        try
        {
            TransactionUndoManager undoManager = WebDiagramsProvider.getUndoManager(optimization.getOptimizationDiagram());
            if( undoManager.canRedo() )
            {
                undoManager.redo();
            }
        }
        catch( Exception e )
        {
            throw new WebException(e, "EX_INTERNAL_DURING_ACTION", "redo");
        }
    }

    private static void performTransaction(Optimization optimization, String action, Runnable runnable) throws WebException
    {
        WebDiagramsProvider.performTransaction(optimization.getOptimizationDiagram(), action, runnable);
    }

    private static void changeAndSendOptimization(final Optimization optimization, String methodName, JSONArray jsonOptions, JSONResponse response) throws WebException
    {
        try
        {
            OptimizationMethod<?> oldMethod = optimization.getOptimizationMethod();
            final OptimizationMethod<?> currentMethod;
            if(methodName.equals(oldMethod.getName()) && jsonOptions == null)
            {
                currentMethod = oldMethod;
            } else
            {
                currentMethod = correctMethodParameters(methodName, oldMethod, jsonOptions);

                performTransaction(optimization, "Change optimization method", () -> optimization.setOptimizationMethod(currentMethod));
            }

            ComponentModel model = ComponentFactory.getModel(currentMethod.getParameters(), Policy.DEFAULT, true);
            JSONArray jsonProperties = JSONUtils.getModelAsJSON(model);
            response.sendJSONBean(jsonProperties);
        }
        catch( Exception e )
        {
            throw new WebException(e, "EX_INTERNAL_DURING_ACTION", "optimization options");
        }
    }

    private static void changeFittingParameters(Optimization optimization, String model) throws WebException
    {
        Map<String, Double> inVals;
        Diagram diagram = optimization.getDiagram();
        if( diagram != null )
        {
            EModel emodel = diagram.getRole(EModel.class);

            DataCollection<? extends Variable> vars;
            if( model.equals( "variables" ) )
                vars = emodel.getParameters();
            else if( model.equals( "entities" ) )
                vars = emodel.getVariableRoles();
            else
                throw new WebException("EX_QUERY_PARAM_INVALID_VALUE", "what");
            inVals = vars.stream().collect( Collectors.toMap( Variable::getName, Variable::getInitialValue ) );
        } else
        {
            inVals = Collections.emptyMap();
        }
        final OptimizationParameters optParameters = optimization.getParameters();
        List<Parameter> fParams = optParameters.getFittingParameters();
        final List<Parameter> newFParams = StreamEx.of(fParams).map(Parameter::copy).toList();
        StreamEx.of(newFParams).mapToEntry( par -> inVals.get(par.getName()) ).nonNullValues().forKeyValue( Parameter::setValue );
        performTransaction(optimization, "Change fitting parameters", () -> optParameters.setFittingParameters(newFParams));
    }

    private static void removeFittingParameters(Optimization optimization, Set<String> jsNames) throws WebException
    {
        final OptimizationParameters optParameters = optimization.getParameters();
        final List<Parameter> newParams = new ArrayList<>();
        for(Parameter par: optParameters.getFittingParameters())
        {
            if( !jsNames.contains(par.getName()) )
            {
                newParams.add(par);
            }
        }
        performTransaction(optimization, "Remove fitting parameters", () -> optParameters.setFittingParameters(newParams));
    }

    private static void addFittingParameters(Optimization optimization, Set<String> jsNames, String model, SubDiagram subDiagram)
            throws WebException
    {
        final OptimizationParameters optParameters = optimization.getParameters();
        Diagram diagram = subDiagram != null ? subDiagram.getDiagram() : optimization.getDiagram();
        if( diagram == null ) return;

        List<Parameter> oldFParams = optParameters.getFittingParameters();
        final List<Parameter> newFParams = new ArrayList<>();
        HashSet<String> fParamsNames = new HashSet<>();
        for( int j = 0; j < oldFParams.size(); ++j )
        {
            Parameter par = oldFParams.get(j);
            newFParams.add(par);
            fParamsNames.add(par.getName());
        }

        EModel emodel = diagram.getRole(EModel.class);
        DataCollection<? extends Variable> vars;
        if( model.equals( "variables" ) )
            vars = emodel.getParameters();
        else if( model.equals( "entities" ) )
            vars = emodel.getVariableRoles();
        else
            throw new WebException("EX_QUERY_PARAM_INVALID_VALUE", "what");
        //String diagramPath = DiagramUtility.generatPath(diagram);
        
        String diagramPath = "";
        if( subDiagram != null )
            diagramPath = Util.getPath( subDiagram );
        for(Variable var : vars)
        {
            String paramName = DiagramUtility.generatPath(diagramPath, var.getName());
            if( !fParamsNames.contains( paramName ) && jsNames.contains( var.getName() ) )
            {

                double val = var.getInitialValue();
                Parameter param = new Parameter(paramName, val);

                if( var instanceof VariableRole )
                {
                    String title = ( (VariableRole)var ).getDiagramElement().getTitle();
                    param.setTitle(title);
                }
                else
                {
                	param.setTitle(var.getName());
                }
                param.setParentDiagramName(diagram.getName());
                param.setUnits(var.getUnits());
                newFParams.add(param);
                fParamsNames.add(paramName);
            }
        }
        performTransaction(optimization, "Add fitting parameter", () -> optParameters.setFittingParameters(newFParams));
    }

    private static void addConstraint(Optimization optimization) throws WebException
    {
        final OptimizationParameters optParameters = optimization.getParameters();
        List<OptimizationConstraint> oldConstraints = optParameters.getOptimizationConstraints();
        final List<OptimizationConstraint> newConstraints = new ArrayList<>();
        for( int i = 0; i < oldConstraints.size(); ++i )
            newConstraints.add(oldConstraints.get(i));
        OptimizationConstraint newConstr = new OptimizationConstraint();
        newConstr.setAvailableExperiments( optParameters.getOptimizationExperiments() );
        newConstr.setDiagram( optimization.getDiagram() );
        newConstraints.add(newConstr);
        performTransaction(optimization, "Add constraint", () -> optParameters.setOptimizationConstraints(newConstraints));
    }

    private static void removeConstraint(Optimization optimization, Set<String> jsNames) throws WebException
    {
        final OptimizationParameters optParameters = optimization.getParameters();
        List<OptimizationConstraint> constraints = optParameters.getOptimizationConstraints();
        final List<OptimizationConstraint> newConstraints = new ArrayList<>();
        Iterator<OptimizationConstraint> cIter = constraints.iterator();
        while( cIter.hasNext() )
        {
            OptimizationConstraint c = cIter.next();
            if( !jsNames.contains(c.getName()) )
            {
                newConstraints.add(c);
            }
        }
        performTransaction(optimization, "Remove constraints", () -> optParameters.setOptimizationConstraints(newConstraints));
    }

    private static void changeExperiment(Optimization optimization, final BiosoftWebRequest arguments) throws WebException
    {
        final String expName = arguments.getString("expname");
        final OptimizationParameters optParameters = optimization.getParameters();
        performTransaction(optimization, "Change experiment", () -> {
            OptimizationExperiment exp = optParameters.getOptimizationExperiment(expName);
            if(exp != null)
            {
                String weightMethod = arguments.get("wmethod");
                if( weightMethod != null && !exp.getWeightMethod().equals(weightMethod) )
                {
                    exp.setWeightMethod(weightMethod);
                    exp.initWeights();
                }
                String exptype = arguments.get( "exptype" );
                if( exptype != null && !exp.getExperimentType().equals( exptype ) )
                {
                    exp.setExperimentType( exptype );
                    exp.initWeights();

                    //Change experiment simulation settings after type is changed
                    List<OptimizationExperiment> experiments = optimization.getParameters().getOptimizationExperiments();
                    Map<String, SimulationTaskParameters> stp = optimization.getParameters().getSimulationTaskParameters();
                    stp.remove( expName );
                    stp = SimulationTaskRegistry.getSimulationTaskParameters( experiments, stp, optimization.getDiagram() );
                }
                String cellline = arguments.get( "celline" );
                if( cellline != null && !exp.getCellLine().equals( cellline ) )
                {
                    exp.setCellLine( cellline );
                }
            }
        });
        cacheDiagramModel( optimization, expName );
        refreshDiagramNodes(optimization);
    }

    private static void addExperiment(Optimization optimization, BiosoftWebRequest arguments) throws WebException
    {
        final OptimizationParameters optParameters = optimization.getParameters();
        String weightMethod = arguments.getString("wmethod");
        String expType = arguments.getString("exptype");
        String newExpName = arguments.getString("expname");
        String diagramStateName = arguments.getString("diagram_state");
        DataElementPath expFilePath = arguments.getDataElementPath("file");
        String cellLine = arguments.getString("celline");

        List<OptimizationExperiment> experiments = optParameters.getOptimizationExperiments();
        final List<OptimizationExperiment> newValue = new ArrayList<>( experiments );

        String overwrite = arguments.get("overwrite");
        if( overwrite != null )
        {
            OptimizationExperiment exp = optParameters.getOptimizationExperiment(newExpName);
            if(exp != null) newValue.remove(exp);
        }
        OptimizationExperiment newExp = new OptimizationExperiment(newExpName, expFilePath);
        newExp.setDiagram(optimization.getDiagram());
        newExp.setDiagramStateName(diagramStateName);
        newExp.setWeightMethod(weightMethod);
        newExp.setExperimentType(expType);
        newExp.setCellLine(cellLine);

        newValue.add(newExp);
        performTransaction(optimization, "Add experiment", () -> optParameters.setOptimizationExperiments(newValue));
        cacheDiagramModel( optimization, newExpName );
    }

    private static void removeExperiment(Optimization optimization, String experimentName) throws WebException
    {
        final OptimizationParameters optParameters = optimization.getParameters();
        List<OptimizationExperiment> experiments = optParameters.getOptimizationExperiments();
        final List<OptimizationExperiment> newValue = new ArrayList<>( experiments );
        OptimizationExperiment exp = optParameters.getOptimizationExperiment(experimentName);
        if( exp != null )
        {
            newValue.remove(exp);
            performTransaction(optimization, "Remove experiment", () -> optParameters.setOptimizationExperiments(newValue));
            refreshDiagramNodes(optimization);
        }
    }

    private static String cacheDiagramModel(Optimization optimization, String experimentName) throws WebException
    {
        Diagram diagram = optimization.getDiagram();

        Role model = diagram.getRole();
        if( !(model instanceof EModel ))
            throw new WebException("EX_QUERY_UNSUPPORTED_DIAGRAM", diagram.getCompletePath());
        String objectName = "beans/model/" + DataElementPath.create( optimization ) + "/" + experimentName;
        Map<String, SimulationTaskParameters> taskParameters = optimization.getParameters().getSimulationTaskParameters();
        SimulationTaskParameters singleTaskParameters = taskParameters.get( experimentName );
        if( singleTaskParameters != null )
        {
            WebServicesServlet.getSessionCache().addObject( objectName, singleTaskParameters.getParametersBean(), true );
        }
        return objectName;
    }

    private static String cacheDiagramModel(Optimization optimization) throws WebException
    {
        Diagram diagram = optimization.getDiagram();

        Role model = diagram.getRole();
        if( ! ( model instanceof EModel ) )
            throw new WebException( "EX_QUERY_UNSUPPORTED_DIAGRAM", diagram.getCompletePath() );
        String objectName = "beans/model/" + DataElementPath.create( optimization );
        Map<String, SimulationTaskParameters> taskParameters = optimization.getParameters().getSimulationTaskParameters();
        for( Map.Entry<String, SimulationTaskParameters> entry : taskParameters.entrySet() )
        {
            String experimentName = entry.getKey();
            String innerObjectName = objectName + "/" + experimentName;
            Object simEngine = WebServicesServlet.getSessionCache().getObject( objectName );
            if( simEngine == null )
            {

                SimulationTaskParameters singleTaskParameters = entry.getValue();
                if( singleTaskParameters != null )
            {
                    WebServicesServlet.getSessionCache().addObject( innerObjectName, singleTaskParameters.getParametersBean(), true );
                }
            }
        }

        return objectName;
    }

    private static void createNewOptimization(DataElementPath optimizationPath, DataElementPath diagramPath, String method) throws WebException
    {
        Diagram diagram = diagramPath.optDataElement(Diagram.class);
        if( diagram == null  || ( diagram.getRole() == null ) )
        {
            throw new WebException("EX_QUERY_UNSUPPORTED_DIAGRAM", diagramPath);
        }
        try
        {
            Optimization optimization = Optimization.createOptimization(optimizationPath.getName(), optimizationPath.optParentCollection(),
                    diagram);
            if( method != null )
            {
                OptimizationMethod<?> methodInfo = OptimizationMethodRegistry.getOptimizationMethod( method );
                if( methodInfo != null )
                    optimization.setOptimizationMethod( methodInfo );

            }

            optimizationPath.save(optimization);
        }
        catch( Exception e )
        {
            throw new WebException(e, "EX_INTERNAL_DURING_ACTION", "create optimization");
        }
    }

    /**
     * Save optimization
     * @throws WebException in case of other errors
     */
    private static void saveOptimization(Optimization optimization) throws WebException
    {
        DataElementPath path = DataElementPath.create(optimization);
        try
        {
            DataCollection<DataElement> parent = path.getParentCollection();
            if( !parent.isMutable() )
                throw new WebException("EX_ACCESS_CANNOT_SAVE", path.getParentPath(), "access denied");
            parent.put(optimization);
            Diagram diagram = optimization.getOptimizationDiagram();
            optimization.getOptimizationDiagramPath().save(diagram);
        }
        catch( Exception e )
        {
            throw new WebException(e, "EX_ACCESS_CANNOT_SAVE", path.getParentPath(), e.getMessage());
        }
    }

    private static void changeOptimizationDiagram(Optimization optimization, String rewrite, JSONResponse response) throws IOException
    {
        OptimizationDiagramManager diagramManager = new OptimizationDiagramManager(optimization);
        try
        {
            diagramManager.setChanged(false);
            diagramManager.changeDiagram(rewrite.equals("false"));
            WebServicesServlet.getSessionCache().removeObject(optimization.getOptimizationDiagramPath().toString());
            response.send(optimization.getOptimizationDiagramPath().toString().getBytes("UTF-16BE"), Connection.FORMAT_SIMPLE);
        }
        catch( Exception e )
        {
            response.error("Can not change diagram for optimization " + optimization);
        }
    }

    public static void refreshDiagramNodes(Optimization optimization)
    {
        try
        {
            OptimizationDiagramManager diagramManager = new OptimizationDiagramManager(optimization);
            diagramManager.refreshNodes(optimization.getOptimizationDiagram());
        }
        catch( Exception e )
        {
        }
    }

    private static OptimizationMethod<?> correctMethodParameters(String newMethodName, OptimizationMethod<?> oldMethod, JSONArray jsonParams)
            throws Exception
    {
        OptimizationMethod<?> newMethod = OptimizationMethodRegistry.getOptimizationMethod( newMethodName );
        if( jsonParams != null )
        {
            JSONUtils.correctBeanOptions(newMethod.getParameters(), jsonParams);
        } else
        {
            BeanUtil.copyBean(oldMethod.getParameters(), newMethod.getParameters());
        }
        return newMethod;
    }

    private static void sendCurrentOptimizationInfo(Optimization optimization, JSONResponse response) throws IOException
    {
        OptimizationMethod<?> method = optimization.getOptimizationMethod();
        JSONObject info = new JSONObject();
        try
        {
            info.put("deviation", String.valueOf(method.getDeviation()));
            info.put("penalty", String.valueOf(method.getPenalty()));
            info.put("evaluations", method.getOptimizationMethodInfo().getEvaluations());
        }
        catch( Exception e )
        {
        }
        response.sendJSON(info);
    }

    private static void processRemove(BiosoftWebRequest arguments, String what) throws WebException
    {
        try
        {
            if( what.equals("fittingparams") )
            {
                removeFittingParameters(getOptimization(arguments), getJSONRows(arguments));
            }
            else if( what.equals("constraint") )
            {
                removeConstraint(getOptimization(arguments), getJSONRows(arguments));
            }
            else if( what.equals("experiment") )
            {
                removeExperiment(getOptimization(arguments), arguments.getString("expname"));
            }
            else
                throw new WebException("EX_QUERY_PARAM_INVALID_VALUE", "what");
        }
        catch( WebException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            throw new WebException(e, "EX_INTERNAL_DURING_ACTION", "remove "+what);
        }
    }

    private static void processAdd(BiosoftWebRequest arguments, String what) throws WebException
    {
        try
        {
            if( what.equals( "variables" ) || what.equals( "entities" ) )
            {
                Optimization opt = getOptimization( arguments );
                addFittingParameters( opt, getJSONRows( arguments ), what,
                        getSubDiagram( arguments, opt.getDiagram() ) );
            }
            else if( what.equals("constraint") )
            {
                addConstraint(getOptimization(arguments));
            }
            else if( what.equals("experiment") )
            {
                addExperiment(getOptimization(arguments), arguments);
            }
            else
                throw new WebException("EX_QUERY_PARAM_INVALID_VALUE", "what");
        }
        catch( WebException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            throw new WebException(e, "EX_INTERNAL_DURING_ACTION", "add "+what);
        }
    }

    private static void processChange(BiosoftWebRequest arguments, String what) throws WebException
    {
        try
        {
            if( what.equals( "variables" ) || what.equals( "entities" ) )
            {
                changeFittingParameters(getOptimization(arguments), what);
            }
            else if( what.equals("experiment") )
            {
                changeExperiment(getOptimization(arguments), arguments);
            }
            else
                throw new WebException("EX_QUERY_PARAM_INVALID_VALUE", "what");
        }
        catch( WebException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            throw new WebException(e, "EX_INTERNAL_DURING_ACTION", "change "+what);
        }
    }

    private static void processDecriptionHTML(BiosoftWebRequest arguments, JSONResponse response) throws WebException, IOException
    {
        String methodName = arguments.getString( "method" );
        AnalysisMethodInfo mi = OptimizationMethodRegistry.getMethodInfo( methodName );
        String html = mi.getDescriptionHTML();
        html = html.replaceAll( "href=\"de:([^\"]+)\"", "href=\"#de=$1\"" );
        String baseId = mi.getBaseId() + "/";
        html = TextUtil2.processHTMLImages( html, baseId );
        response.sendString( html );
    }

    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws WebException, IOException
    {
        String action = arguments.getAction();
        if( action.equals("methods") )
        {
            sendMethodList(response);
            return;
        }
        else if( action.equals("info") )
        {
            sendOptimizationInfo(getOptimization(arguments), response);
            return;
        }
        else if( action.equals("experiments") )
        {
            String what = arguments.get("what");
            if( what != null )
            {
                if( what.equals("commons") )
                    sendExperimentsCommons(response);
                else if( what.equals("diagram") )
                    sendDiagramStates(getOptimization(arguments), response);
            }
            else
            {
                sendExperiments(getOptimization(arguments), response);
            }
            return;
        }
        else if( action.equals("run") )
        {
            String methodName = arguments.getString("method");
            JSONArray options = arguments.optJSONArray("options");
            String jobID = arguments.get("jobID");
            startOptimization( getOptimization( arguments ), methodName, options, jobID, response );
            return;
        }
        else if( action.equals("bean") )
        {
            changeAndSendOptimization(getOptimization(arguments), arguments.getString("method"), arguments.optJSONArray("options"), response);
            return;
        }
        else if( action.equals("opt_info") )
        {
            sendCurrentOptimizationInfo(getOptimization(arguments), response);
            return;
        }
        else if( action.equals("model") )
        {
            response.sendString(cacheDiagramModel(getOptimization(arguments)));
            return;
        }
        else if( action.equals("optimization_diagram") )
        {
            changeOptimizationDiagram(getOptimization(arguments), arguments.get("rewrite"), response);
            return;
        }
        else if( action.equals("create") )
        {
            createNewOptimization(arguments.getDataElementPath(), arguments.getDataElementPath("diagram"), arguments.get("method"));
        }
        else if( action.equals("add") )
        {
            processAdd(arguments, arguments.getString("what"));
        }
        else if( action.equals("change") )
        {
            processChange(arguments, arguments.getString("what"));
        }
        else if( action.equals("remove") )
        {
            processRemove(arguments, arguments.getString("what"));
        }
        else if( action.equals("save") )
        {
            saveOptimization(getOptimization(arguments));
        }
        else if( action.equals("undo") )
        {
            undo(getOptimization(arguments));
        }
        else if( action.equals("redo") )
        {
            redo(getOptimization(arguments));
        }
        else if( action.equals( "method_info" ) )
        {
            processDecriptionHTML( arguments, response );
            return;
        }
        else if( action.equals( "set_initial" ) )
        {
            setInitialValues( arguments, response );
            return;
        }
        else
            throw new WebException("EX_QUERY_PARAM_INVALID_VALUE", BiosoftWebRequest.ACTION);
        response.sendString("");
    }


    private void setInitialValues(BiosoftWebRequest arguments, JSONResponse response) throws WebException, IOException
    {
        Optimization optimization = getOptimization( arguments );
        DataElementPath tablePath = arguments.getDataElementPath( "table" );
        TableDataCollection table = tablePath.optDataElement( TableDataCollection.class );
        if( table == null )
        {
            response.error( "The table '" + tablePath.toString() + "' is not found" );
            return;
        }
        int ind = table.getColumnModel().optColumnIndex( SetInitialValuesAction.VALUE_COLUMN );
        if( ind == -1 )
        {
            response.error( "The table '" + table.getName() + "' must contain the column '" + SetInitialValuesAction.VALUE_COLUMN
                    + "' including new initial values to be set." );
            return;
        }

        if( !table.getColumnModel().getColumn( ind ).getType().equals( DataType.Float ) )
        {
            response.error( "The column '" + SetInitialValuesAction.VALUE_COLUMN + "' in the table '" + table.getName()
                    + "' must be of the type 'Float'." );
            return;
        }

        Set<String> missing = new HashSet<>();
        StreamEx.of( optimization.getParameters().getFittingParameters() ).forEach( p -> {
            if( table.contains( p.getName() ) )
            {
                try
                {
                    p.setValue( (double)table.get( p.getName() ).getValues()[ind] );
                }
                catch( Exception e )
                {
                    missing.add( p.getName() );
                }
            }
            else
                missing.add( p.getName() );
        } );
        if( missing.isEmpty() )
            response.sendString( "ok" );
        else
            response.sendString( "Some parameters were absent in table '" + table.getName() + "', the values remain the same for: "
                    + missing.stream().collect( Collectors.joining( ", " ) ) );

    }
}
