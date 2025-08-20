package biouml.plugins.research._test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.research.workflow.engine.WorkflowEngine;
import biouml.plugins.research.workflow.items.DataElementType;
import biouml.plugins.research.workflow.items.WorkflowItemFactory;
import biouml.plugins.research.workflow.items.WorkflowParameter;
import biouml.standard.type.Type;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.analysis.FilterTableParameters;
import ru.biosoft.journal.JournalRegistry;
import ru.biosoft.server.servlets.webservices._test.AbstractProviderTest;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

/**
 * @author lan
 *
 */
public class TestWebResearchProvider extends AbstractProviderTest
{
    public void testCreateAndRunWorkflow() throws Exception
    {
        createRoot();
        JournalRegistry.setJournalUse(false);

        Diagram diagram = createWorkflow();

        createWorkflowElement(diagram, Type.ANALYSIS_METHOD,
                createParametersJSON("analysisMethod", "Data/Filter table"));
        assertNotNull(diagram.get("Filter table"));
        createWorkflowElement(diagram, Type.ANALYSIS_PARAMETER,
                createParametersJSON("name", "Input", "defaultValueString", ""));
        assertNotNull(diagram.get("Input"));
        createWorkflowElement(diagram, Type.ANALYSIS_PARAMETER,
                createParametersJSON("name", "Output", "defaultValueString", "test/output"));
        assertNotNull(diagram.get("Output"));

        Map<String, String> vars = getVariablesTree(diagram);
        assertEquals("", vars.get("Input"));
        assertEquals("test/output", vars.get("Output"));
        assertEquals("test/workflow", vars.get("workflowPath"));

        createWorkflowEdge(diagram, "Input", "Filter table.inputPath");
        createWorkflowEdge(diagram, "Filter table.outputPath", "Output");
        WorkflowParameter parameter = (WorkflowParameter)WorkflowItemFactory.getWorkflowItem((Node)diagram.get("Input"));
        assertEquals(WorkflowParameter.ROLE_INPUT, parameter.getRole());
        assertEquals(DataElementType.getType(TableDataCollection.class), parameter.getDataElementType());

        setBean("properties/workflow/"+diagram.getCompletePath()+"/Filter table", "filterExpression", "A > 5");
        assertEquals("A > 5", ((FilterTableParameters)WorkflowEngine.getAnalysisParametersByNode((Node)diagram.get("Filter table"))).getFilterExpression());

        prepareTestTable(DataElementPath.create("test/table"));

        setBean("workflow/parameters/"+diagram.getCompletePath(), "Output", "test/table");
        JsonObject responseJSON = getResponseJSON("research/overwritePrompt", Collections.singletonMap("de", diagram.getCompletePath().toString()));
        assertOk(responseJSON);
        assertEquals("test/table", responseJSON.get("values").asArray().get(0).asString());

        assertOk(runWorkflow(diagram, createParametersJSON(), "WorkflowJob0"));
        boolean catched = false;
        try
        {
            waitForJob("WorkflowJob0");
        }
        catch( Exception e )
        {
            assertEquals( "Job exception: SEVERE - Input table is not acceptable: \nReason: Element not found: \n",
                    e.getMessage().replaceAll( "\r", "" ) );
            catched = true;
        }
        assertTrue(catched);

        assertOk(runWorkflow(diagram, createParametersJSON("Input", "test/table", "Output", "test/table out"), "WorkflowJob1"));
        waitForJob("WorkflowJob1");
        assertEquals(2, DataElementPath.create("test/table out").getDataElement(TableDataCollection.class).getSize());

        createWorkflowElement(diagram, Type.ANALYSIS_EXPRESSION,
                createParametersJSON("name", "Filter", "expression", "A > 7"));
        Map<String, String> arguments = new HashMap<>();
        arguments.put("de", diagram.getCompletePath().toString());
        arguments.put("analysis", "Filter table");
        arguments.put("property", "filterExpression");
        arguments.put("variable", "Filter");
        assertOk(getResponseJSON("research/bind_parameter", arguments));
        assertOk(runWorkflow(diagram, createParametersJSON("Input", "test/table", "Output", "test/table out"), "WorkflowJob2"));
        waitForJob("WorkflowJob2");
        assertEquals(1, DataElementPath.create("test/table out").getDataElement(TableDataCollection.class).getSize());
    }

    private JsonObject runWorkflow(Diagram diagram, JsonArray workflowParams, String jobID) throws Exception, JSONException
    {
        Map<String, String> arguments = new HashMap<>();
        arguments.put("de", diagram.getCompletePath().toString());
        arguments.put("json", workflowParams.toString());
        arguments.put("jobID", jobID);
        JsonObject response = getResponseJSON("research/start_workflow", arguments);
        return response;
    }

    private Map<String, String> getVariablesTree(Diagram diagram) throws Exception, JSONException
    {
        JsonObject response = getResponseJSON("research/var_tree", Collections.singletonMap("de", diagram.getCompletePath().toString()));
        assertOk(response);
        JsonArray varTree = response.get("values").asObject().get("children").asArray();
        Map<String, String> vars = new HashMap<>();
        for(JsonValue val : varTree)
        {
            JsonObject entry = val.asObject();
            vars.put(entry.get("name").asString(), entry.getString("value", ""));
        }
        return vars;
    }

    private void prepareTestTable(DataElementPath path)
    {
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(path);
        table.getColumnModel().addColumn("A", Integer.class);
        TableDataCollectionUtils.addRow(table, "A", new Object[] {1});
        TableDataCollectionUtils.addRow(table, "B", new Object[] {2});
        TableDataCollectionUtils.addRow(table, "C", new Object[] {7});
        TableDataCollectionUtils.addRow(table, "D", new Object[] {8});
        CollectionFactoryUtils.save(table);
    }

    private void createWorkflowEdge(Diagram diagram, String in, String out) throws Exception
    {
        Map<String, String> arguments = new HashMap<>();
        arguments.put("de", diagram.getCompletePath().toString());
        arguments.put("resptype", "json");
        arguments.put("type", "");
        arguments.put("dc", "n/a");
        arguments.put("input", in);
        arguments.put("output", out);
        assertOk( getResponseJSON( "diagram/add_edge", arguments ) );
    }

    private void createWorkflowElement(Diagram diagram, String type, JsonArray json) throws JSONException, Exception
    {
        Map<String, String> arguments = new HashMap<>();
        arguments.put("de", diagram.getCompletePath().toString());
        arguments.put("resptype", "json");
        arguments.put("type", type);
        arguments.put("x", "10");
        arguments.put("y", "10");
        arguments.put("dc", "n/a");
        arguments.put("json", json.toString());
        assertOk(getResponseJSON("diagram/add", arguments));
    }

    private Diagram createWorkflow() throws JSONException, Exception
    {
        Map<String, String> arguments = new HashMap<>();
        arguments.put("dc", "test");
        arguments.put("de", "workflow");
        arguments.put("type", "workflow");
        assertOk(getResponseJSON("journal/create", arguments));
        return DataElementPath.create("test/workflow").getDataElement(Diagram.class);
    }

    private void createRoot()
    {
        VectorDataCollection<TableDataCollection> vdc = new VectorDataCollection<>("test", StandardTableDataCollection.class, null);
        CollectionFactory.registerRoot(vdc);
    }
}
