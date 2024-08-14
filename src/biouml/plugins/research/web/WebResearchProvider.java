package biouml.plugins.research.web;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;
import com.developmentontheedge.beans.undo.TransactionUndoManager;
import com.developmentontheedge.log.PatternFormatter;
import com.developmentontheedge.log.WriterHandler;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.plugins.research.workflow.WorkflowSemanticController;
import biouml.plugins.research.workflow.engine.WorkflowEngine;
import biouml.plugins.research.workflow.engine.WorkflowEngineListener;
import biouml.plugins.research.workflow.items.VariablesTreeModel;
import biouml.plugins.research.workflow.items.WorkflowItem;
import biouml.plugins.research.workflow.items.WorkflowItemFactory;
import biouml.plugins.research.workflow.items.WorkflowParameter;
import biouml.plugins.research.workflow.items.WorkflowVariable;
import biouml.standard.type.Type;
import biouml.workbench.diagram.DiagramEditorHelper;
import biouml.workbench.diagram.ViewEditorPaneStub;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.undo.DataCollectionUndoListener;
import ru.biosoft.access.log.JULLoggerAdapter;
import ru.biosoft.access.security.Permission;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.SessionThread;
import ru.biosoft.analysiscore.AnalysisDPSUtils;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.journal.Journal;
import ru.biosoft.journal.JournalRegistry;
import ru.biosoft.server.JSONUtils;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.WebJob;
import ru.biosoft.server.servlets.webservices.WebSession;
import ru.biosoft.server.servlets.webservices.providers.WebBeanProvider;
import ru.biosoft.server.servlets.webservices.providers.WebDiagramsProvider;
import ru.biosoft.server.servlets.webservices.providers.WebJSONProviderSupport;
import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.tasks.TaskManager;

/**
 * Provides research functions
 */
public class WebResearchProvider extends WebJSONProviderSupport
{
    private static final class WebJobWriter extends Writer
    {

        private WebJob webJob;
        private StringBuffer buffer;

        public WebJobWriter(WebJob webJob)
        {
            this.webJob = webJob;
            this.buffer = new StringBuffer();
        }
        @Override
        public void close() throws SecurityException
        {
            flush();
            webJob = null;
            buffer = null;
        }

        @Override
        public void flush()
        {
            if( webJob == null )
                return;
            JobControl jobControl = webJob.getJobControl();
            if( jobControl != null && webJob.getJobControl().getStatus() != JobControl.TERMINATED_BY_ERROR )
                webJob.addJobMessage( buffer.toString() );
            buffer = new StringBuffer();
        }

        @Override
        public void write(char[] bytes, int offset, int len) throws IOException
        {
            if( webJob == null )
                return;
            buffer.append( bytes, offset, len );
        }
    }

    /**
     * Start workflow execution
     * @param json
     * @param saveResearchPath if not null, research diagram will be generated
     * @throws Exception
     */
    private static void startWorkflow(DataElementPath workflowPath, final String jobID, JSONArray jsonParams, final DataElementPath saveResearchPath, boolean useJsonOrder) throws Exception
    {
        final Diagram diagram = WebDiagramsProvider.getDiagramChecked(workflowPath);
        try
        {
            if( ( SecurityManager.getPermissions(workflowPath).getPermissions() & Permission.WRITE ) != 0 )
                CollectionFactoryUtils.save(diagram);
        }
        catch( Exception e1 )
        {
            // Diagram was not saved
        }
        final WebJob webJob = WebJob.getWebJob(jobID);
        final Logger log = webJob.getJobLogger();

        final Writer writer = new WebJobWriter( webJob );
        final Handler handler = new WriterHandler( writer, new PatternFormatter( "%4$s - %5$s%n" ) );//new WebJobHandler( webJob );
        handler.setLevel( Level.INFO );
        log.setLevel( Level.ALL );
        log.addHandler( handler );

        final Diagram finalDiagram = diagram.clone(diagram.getOrigin(), diagram.getName());
        final WorkflowEngine workflowEngine = new WorkflowEngine();
        DynamicPropertySet workflowParameters = null;
        if(jsonParams != null)
        {
            workflowParameters = WorkflowItemFactory.getWorkflowParameters(finalDiagram);
            WebBeanProvider.preprocessJSON(workflowParameters, jsonParams);
            JSONUtils.correctBeanOptions(workflowParameters, jsonParams, useJsonOrder);
            workflowEngine.setSkipCompleted((Boolean)workflowParameters.getValue(WorkflowEngine.SKIP_COMPLETED_PROPERTY));
            workflowEngine.setIgnoreFail( (Boolean)workflowParameters.getValue( WorkflowEngine.IGNORE_FAIL_PROPERTY ) );
        }
        final FunctionJobControl jobControl = new FunctionJobControl(null)
        {
            @Override
            public void terminate()
            {
                super.terminate();
                workflowEngine.stop();
            }

            @Override
            public void pause()
            {
                super.pause();
                workflowEngine.pause();
            }

            @Override
            public void resume()
            {
                super.resume();
                workflowEngine.resume();
            }
        };

        Journal journal = JournalRegistry.getCurrentJournal();
        TaskManager taskManager = TaskManager.getInstance();
        final TaskInfo task = taskManager.addTask( TaskInfo.WORKFLOW, workflowPath, jobControl, new JULLoggerAdapter( log ), journal,
                workflowParameters == null ? null : (DynamicPropertySet)workflowParameters, false, null);

        final WorkflowEngineListener workflowListener = new WorkflowEngineListener()
        {
            @Override
            public void stateChanged()
            {
            }

            @Override
            public void started()
            {
            }

            @Override
            public void resultsReady(Object[] results)
            {
                if( results == null )
                    return;
                Collection<ru.biosoft.access.core.DataElementPath> paths = workflowEngine.getAutoOpenPaths();
                WebSession session = WebSession.getCurrentSession();
                List<ru.biosoft.access.core.DataElementPath> pathsToOpen = StreamEx.of( results )
                        .select( ru.biosoft.access.core.DataElement.class )
                        .map( DataElementPath::create )
                        .peek( session::pushRefreshPath )
                        .filter( paths::contains )
                        .toList();
                if(!pathsToOpen.isEmpty())
                {
                    webJob.addJobResults(pathsToOpen);
                }
            }

            @Override
            public void parameterErrorDetected(String error)
            {
            }

            @Override
            public void errorDetected(String error)
            {
            }

            @Override
            public void finished()
            {
                handler.close();
                /*try
                {
                    writer.close();
                }
                catch( IOException e )
                {
                }*/
                // TODO: free memory wisely
                //task.setTransient("diagram", null);
            }
        };

        workflowEngine.setWorkflow(finalDiagram);
        workflowEngine.setParameters(workflowParameters);
        workflowEngine.setLogger(log);
        workflowEngine.setJobControl(jobControl);
        workflowEngine.addEngineListener(workflowListener);
        jobControl.functionStarted();
        webJob.setTask(task);
        task.setTransient("parameters", workflowParameters);
        task.setTransient("diagram", finalDiagram);
        try
        {
            if(saveResearchPath != null && !saveResearchPath.isEmpty())
            {
                workflowEngine.createResearchDiagram(saveResearchPath);
            }
            workflowEngine.initWorkflow();
        }
        catch(Exception e)
        {
            jobControl.functionTerminatedByError(e);
            return;
        }
        (new SessionThread()
        {
            @Override
            public void doRun()
            {
                workflowEngine.start();
            }
        }).start();
    }

    /**
     * Returns properties for workflow element
     */
    public static Object getWorkflowElementProperties(DataElementPath path)
    {
        Compartment parent = path.getParentPath().optDataElement(Compartment.class);
        if(parent == null) return null;
        Diagram diagram = Diagram.optDiagram(parent);
        if(diagram == null) return null;
        DataElement node = parent.get(path.getName());
        if( node instanceof Node )
        {
            String type = ( (Node)node ).getKernel().getType();
            if( type.equals(Type.ANALYSIS_METHOD) )
            {
                AnalysisParameters parameters = WorkflowEngine.getAnalysisParametersByNode((Node)node);
                parameters.setExpertMode(true);
                return parameters;
            }
            if( type.equals(Type.ANALYSIS_SCRIPT) )
            {
                return WorkflowEngine.getScriptParameters((Node)node);
            }
            if( type.equals(Type.TYPE_PLOT) )
            {
                return WorkflowEngine.getPlotParameters((Node)node);
            }
            if( type.equals(Type.TYPE_DATA_GENERATOR) )
            {
                return WorkflowEngine.getDataGeneratorParameters((Node)node);
            }
            DiagramEditorHelper helper = new DiagramEditorHelper(diagram);
            DataCollectionUndoListener diagramListener = WebDiagramsProvider.getUndoListener(diagram);
            ViewEditorPane viewEditor = new ViewEditorPaneStub(helper, diagram, (TransactionUndoManager)diagramListener.getTransactionListener());
            WorkflowItem item = WorkflowItemFactory.getWorkflowItem((Node)node, viewEditor);
            return item;
        }
        return null;
    }

    /**
     * Save properties for workflow element
     */
    public static void saveWorkflowElementProperties(Object properties, String completeName)
    {
        Compartment compartment = DataElementPath.create(completeName).optDataElement( Compartment.class );
        if( compartment != null && compartment.getKernel().getType().equals( Type.ANALYSIS_METHOD )
                && ( properties instanceof AnalysisParameters ) )
        {
            AnalysisDPSUtils.writeParametersToNodeAttributes(null, (AnalysisParameters)properties, compartment.getAttributes());
            Diagram diagram = Diagram.optDiagram(compartment);
            if(diagram != null)
            {
                SemanticController semanticController = diagram.getType().getSemanticController();
                if(semanticController instanceof WorkflowSemanticController)
                {
                    ((WorkflowSemanticController)semanticController).updateAnalysisNode(compartment);
                }
            }
        }
    }

    private static void bindVariableToAnalysis(DataElementPath path, String analysisNodeName, String propertyName, String variableNodeName) throws Exception
    {
        Diagram diagram = WebDiagramsProvider.getDiagramChecked(path);
        WorkflowSemanticController semanticController;
        try
        {
            semanticController = (WorkflowSemanticController)diagram.getType().getSemanticController();
        }
        catch( Exception e )
        {
            throw new Exception("Supplied diagram is not workflow: "+path);
        }
        Compartment analysis = path.getRelativePath(analysisNodeName).getDataElement(Compartment.class);
        AnalysisParameters parameters;
        try
        {
            parameters = WorkflowEngine.getAnalysisParametersByNode(analysis, true);
        }
        catch( Exception e )
        {
            throw new Exception("Unable to get analysis node: "+analysisNodeName, e);
        }
        Node variable = path.getRelativePath(variableNodeName).getDataElement(Node.class);
        WorkflowItem item = WorkflowItemFactory.getWorkflowItem(variable);
        if(!(item instanceof WorkflowVariable))
        {
            throw new Exception("You should select variable to bind parameter to: "+variableNodeName);
        }
        ComponentModel model = ComponentFactory.getModel(parameters);
        Property property = model.findProperty(propertyName);
        if(property == null)
        {
            throw new Exception("Invalid property: "+propertyName);
        }
        DiagramEditorHelper helper = new DiagramEditorHelper(diagram);
        DataCollectionUndoListener diagramListener = WebDiagramsProvider.getUndoListener(diagram);
        ViewEditorPane viewEditor = new ViewEditorPaneStub(helper, diagram, (TransactionUndoManager)diagramListener.getTransactionListener());
        semanticController.bindParameter(variable, analysis, property, viewEditor);
    }

    private static void sendWorkflowVariablesTreeBranch(Compartment compartment, String branch, JSONResponse response) throws IOException
    {
        JsonArray branchData = new JsonArray();
        VariablesTreeModel model = new VariablesTreeModel(compartment);
        int count = model.getChildCount(branch);
        for(int i=0; i<count; i++)
        {
            try
            {
                String path = model.getChild(branch, i).toString();
                String value = null;
                Property property = model.getProperty(path);
                if(property != null)
                {
                    Object valueObj = property.getValue();
                    if(valueObj != null) value = valueObj.toString();
                }
                JsonObject item = new JsonObject();
                item.add("name", path);
                if(value != null)
                    item.add("value", value);
                item.add("leaf", model.isLeaf(path));
                branchData.add(item);
            }
            catch( Exception e )
            {
            }
        }
        response.sendJSON(new JsonObject().add("children", branchData));
    }

    private static void sendOverwritePaths(DataElementPath path, JSONResponse response) throws WebException, IOException
    {
        Diagram diagram = WebDiagramsProvider.getDiagramChecked(path);
        JsonArray jsonArray = new JsonArray();
        for(DiagramElement de: diagram)
        {
            if( de instanceof Node && ( (Node)de ).getKernel() != null
                    && ( (Node)de ).getKernel().getType().equals(Type.ANALYSIS_PARAMETER) )
            {
                WorkflowItem item = WorkflowItemFactory.getWorkflowItem((Node)de);

                if( item instanceof WorkflowParameter && ( (WorkflowParameter)item ).getRole().equals(WorkflowParameter.ROLE_OUTPUT)
                        && ( (WorkflowParameter)item ).getType().getTypeClass().isAssignableFrom(DataElementPath.class) )
                {
                    DataElementPath wpPath =  DataElementPath.create(( (WorkflowParameter)item ).getCurrentValue().toString());
                    if(wpPath.exists())
                    {
                        jsonArray.add(wpPath.toString());
                    }
                }
            }
        }
        response.sendJSON(jsonArray);
    }

    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        DataElementPath dePath = arguments.getDataElementPath();
        String action = arguments.getAction();
        if( action.equals("start_workflow") )
        {
            String jobID = arguments.getString("jobID");
            JSONArray json = arguments.optJSONArray("json");
            String savePathStr = arguments.get("researchPath");
            DataElementPath savePath = DataElementPath.create(savePathStr == null ? "" : savePathStr);
            boolean useJsonOrder = WebBeanProvider.isUseJsonOrder( arguments );
            startWorkflow(dePath, jobID, json, savePath, useJsonOrder);
            response.sendString("");
        }
        else if( action.equals("var_tree") )
        {
            String branch = arguments.get("branch");
            Compartment de = dePath.getDataElement(Compartment.class);
            sendWorkflowVariablesTreeBranch(de, branch == null?"":branch, response);
        }
        else if( action.equals("bind_parameter") )
        {
            String analysisNodeName = arguments.getString("analysis");
            String propertyName = arguments.getString("property");
            String variableNodeName = arguments.getString("variable");
            try
            {
                bindVariableToAnalysis(dePath, analysisNodeName, propertyName, variableNodeName);
            }
            catch( Exception e )
            {
                throw new IllegalArgumentException("Unable to bind parameter: " + e.getMessage(), e);
            }
            response.sendString("");
        }
        else if( action.equals("overwritePrompt") )
        {
            sendOverwritePaths(dePath, response);
        }
        else
            throw new WebException("EX_QUERY_PARAM_INVALID_VALUE", BiosoftWebRequest.ACTION);
    }
}
