package biouml.plugins.research._test;

import java.awt.Point;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.editors.StringTagEditor;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.log.PatternFormatter;
import com.developmentontheedge.log.WriterHandler;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.util.ImageGenerator;
import biouml.plugins.research.workflow.WorkflowDiagramType;
import biouml.plugins.research.workflow.WorkflowSemanticController;
import biouml.plugins.research.workflow.engine.WorkflowEngine;
import biouml.plugins.research.workflow.engine.WorkflowEngineListener;
import biouml.plugins.research.workflow.items.CollectionCycleType;
import biouml.plugins.research.workflow.items.CycleType;
import biouml.plugins.research.workflow.items.DataElementType;
import biouml.plugins.research.workflow.items.EnumCycleType;
import biouml.plugins.research.workflow.items.RangeCycleType;
import biouml.plugins.research.workflow.items.SetCycleType;
import biouml.plugins.research.workflow.items.TableColumnsCycleType;
import biouml.plugins.research.workflow.items.TableNumericalColumnsCycleType;
import biouml.plugins.research.workflow.items.VariableType;
import biouml.plugins.research.workflow.items.VariablesTreeModel;
import biouml.plugins.research.workflow.items.WorkflowCycleVariable;
import biouml.plugins.research.workflow.items.WorkflowExpression;
import biouml.plugins.research.workflow.items.WorkflowItemFactory;
import biouml.plugins.research.workflow.items.WorkflowParameter;
import biouml.standard.type.Base;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;
import biouml.workbench.diagram.DiagramEditorHelper;
import biouml.workbench.diagram.ViewEditorPaneStub;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysis.FilterTable;
import ru.biosoft.analysis.FilterTableParameters;
import ru.biosoft.analysiscore.AnalysisDPSUtils;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.ImageView;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.journal.JournalRegistry;
import ru.biosoft.plugins.javascript.JSElement;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.TextUtil;

/**
 * @author lan
 *
 */
public class WorkflowTest extends AbstractBioUMLTest
{
    private boolean finished;
    private String error = null;
    private Diagram workflow;
    private WorkflowSemanticController workflowController;
    private ViewEditorPane viewEditor;

    public void testVariableType() throws Exception
    {
        VariableType type = VariableType.getType(String.class);
        assertNotNull(type);
        assertEquals("String", type.getName());
        VariableType type2 = VariableType.getType("String");
        assertTrue(type == type2);
    }

    public void testDataElementType() throws Exception
    {
        DataElementType type = DataElementType.getType(TableDataCollection.class);
        assertEquals(TableDataCollection.class, type.getTypeClass());
        assertEquals("Table", type.toString());

        assertTrue(type == DataElementType.getType("Table"));
    }

    public void testCycleTypes() throws Exception
    {
        CycleType type = new EnumCycleType();
        assertEquals(3, type.getCount("a;b;c"));
        assertEquals("a", type.getValue("a;b;c", 0));

        type = new RangeCycleType();
        assertEquals(10, type.getCount("6..15"));
        assertEquals("6", type.getValue("6..15", 0));
        assertEquals("15", type.getValue("6..15", 9));

        assertEquals(5, type.getCount( "6,8..14" ));
        assertEquals("6", type.getValue( "6,8..14",0 ));
        assertEquals("8", type.getValue( "6,8..14",1 ));
        assertEquals("10", type.getValue( "6,8..14",2 ));
        assertEquals("12", type.getValue( "6,8..14",3 ));
        assertEquals("14", type.getValue( "6,8..14",4 ));

        assertEquals(6, type.getCount( "1,1.2..2" ));
        assertEquals("1", type.getValue( "1,1.2..2",0 ));
        assertEquals("1.2", type.getValue( "1,1.2..2",1 ));
        assertEquals("1.4", type.getValue( "1,1.2..2",2 ));
        assertEquals("1.6", type.getValue( "1,1.2..2",3 ));
        assertEquals("1.8", type.getValue( "1,1.2..2",4 ));
        assertEquals("2", type.getValue( "1,1.2..2",5 ));

        assertEquals(5, type.getCount( "6,5..2" ));
        assertEquals("6", type.getValue( "6,5..2",0 ));
        assertEquals("5", type.getValue( "6,5..2",1 ));
        assertEquals("4", type.getValue( "6,5..2",2 ));
        assertEquals("3", type.getValue( "6,5..2",3 ));
        assertEquals("2", type.getValue( "6,5..2",4 ));

        type = new SetCycleType();
        DataElementPathSet set = new DataElementPathSet();
        set.add(DataElementPath.create("test/a"));
        set.add(DataElementPath.create("test/b"));
        set.add(DataElementPath.create("test/c"));
        set.add(DataElementPath.create("test/d"));
        String val = set.toString();
        assertEquals(4, type.getCount(val));
        assertEquals("test/c", type.getValue(val, 2));

        VectorDataCollection<TableDataCollection> vdc = new VectorDataCollection<>( "test", StandardTableDataCollection.class, null );
        CollectionFactory.registerRoot(vdc);
        TableDataCollection tdc = new StandardTableDataCollection(vdc, "path");
        tdc.getColumnModel().addColumn("col1", String.class);
        tdc.getColumnModel().addColumn("col2", Double.class);
        tdc.getColumnModel().addColumn("col3", Integer.class);
        vdc.put(tdc);

        type = new CollectionCycleType();
        assertEquals(1, type.getCount("test"));
        assertEquals("test/path", type.getValue("test", 0));

        type = new TableColumnsCycleType();
        assertEquals(3, type.getCount("test/path"));
        assertEquals("col1", type.getValue("test/path", 0));
        assertEquals("col3", type.getValue("test/path", 2));

        type = new TableNumericalColumnsCycleType();
        assertEquals(2, type.getCount("test/path"));
        assertEquals("col2", type.getValue("test/path", 0));
        assertEquals("col3", type.getValue("test/path", 1));

        CollectionFactory.unregisterAllRoot();
    }

    public void testVariablesTreeModel() throws Exception
    {
        VectorDataCollection<DataElement> vdc = new VectorDataCollection<>( "test" );
        CollectionFactory.registerRoot(vdc);

        createWorkflow();

        // Prepare
        WorkflowExpression expression = (WorkflowExpression)WorkflowItemFactory.getWorkflowItem(workflow, Type.ANALYSIS_EXPRESSION);
        expression.setName("expression");
        expression.setExpression("Hello");
        expression.setType(VariableType.getType(String.class));
        assertTrue(workflowController.canAccept((Compartment)expression.getNode().getOrigin(), expression.getNode()));
        viewEditor.add(expression.getNode(), new Point(10,10));

        expression = (WorkflowExpression)WorkflowItemFactory.getWorkflowItem(workflow, Type.ANALYSIS_EXPRESSION);
        expression.setName("path");
        expression.setExpression("test");
        expression.setType(VariableType.getType(DataElementPath.class));
        assertTrue(workflowController.canAccept((Compartment)expression.getNode().getOrigin(), expression.getNode()));
        viewEditor.add(expression.getNode(), new Point(10,10));

        VariablesTreeModel model = new VariablesTreeModel(workflow);
        assertEquals("", model.getRoot());
        assertTrue(model.getChildCount("")>=2);
        assertEquals(-1, model.getIndexOfChild("", "invalidChild"));
        assertTrue(model.getIndexOfChild("", "expression") >= 0);
        assertFalse(model.isLeaf("expression"));
        assertTrue(model.getIndexOfChild("", "path") >= 0);

        // see DataElementPathBeanInfo
        Set<String> expected = new HashSet<>( Arrays.asList( "path/name", "path/element", "path/parent", "path/empty" ) );
        assertTrue(model.getChildCount("path") >= expected.size());
        for(int i=0; i<model.getChildCount("path"); i++)
        {
            Object child = model.getChild("path", i);
            assertEquals(i, model.getIndexOfChild("path", child));
            expected.remove(child);
        }
        assertTrue(expected.isEmpty());

        assertEquals(vdc, model.getProperty("path/element").getValue());

        CollectionFactory.unregisterAllRoot();
    }

    public void testSimpleWorkflow() throws Exception
    {
        String analysisName = "Filter table";
        AnalysisMethodRegistry.addTestAnalysis(analysisName, FilterTable.class);

        createWorkflow();

        // Analysis node
        Compartment analysis = (Compartment)workflowController.addAnalysis(workflow, analysisName, new Point(10,10), viewEditor);
        FilterTableParameters baseParameters = new FilterTableParameters();
        baseParameters.setFilterExpression("ID");
        baseParameters.setValuesCount(2);
        AnalysisDPSUtils.writeParametersToNodeAttributes(analysisName, baseParameters, analysis.getAttributes());

        assertNotNull(analysis);
        assertEquals(2, analysis.getSize());

        // 'input' workflow parameter
        WorkflowParameter parameter = connectInput((Node)analysis.get("inputPath"), "input");
        parameter.setDataElementType(DataElementType.getType(TableDataCollection.class));

        // 'mode' workflow parameter
        parameter = (WorkflowParameter)WorkflowItemFactory.getWorkflowItem(workflow, Type.ANALYSIS_PARAMETER);
        parameter.setName("mode");
        parameter.setDropDownOptions("(auto)");
        parameter.setType(VariableType.getType(String.class));
        viewEditor.add(parameter.getNode(), new Point(10,10));
        Node modeElement = (Node)workflow.get("mode");
        assertNotNull(modeElement);

        // 'mode' -> 'Filter table:filteringMode' edge
        ComponentModel model = ComponentFactory.getModel(new FilterTableParameters());
        workflowController.bindParameter(modeElement, analysis, model.findProperty("filteringMode"), viewEditor);

        // 'output' workflow parameter
        parameter = connectOutput((Node)analysis.get("outputPath"), "output");
        parameter.setExpression("$input$ out");
        parameter.setDataElementType(DataElementType.getType(TableDataCollection.class));
        parameter.setType(VariableType.getType(VariableType.TYPE_AUTOOPEN));

        // Creation finished: check parameters bean and set their values
        DynamicPropertySet parameters = WorkflowItemFactory.getWorkflowParameters(workflow);

        DynamicProperty property;
        property = parameters.getProperty("input");
        assertNotNull(property);
        assertEquals(TableDataCollection.class, property.getDescriptor().getValue(DataElementPathEditor.ELEMENT_CLASS));
        property.setValue(TextUtil.fromString(property.getType(), "test/path"));

        property = parameters.getProperty("mode");
        assertNotNull(property);
        StringTagEditor editor = (StringTagEditor)property.getDescriptor().getPropertyEditorClass().newInstance();
        assertNotNull(editor);
        editor.setBean(parameters);
        editor.setDescriptor(property.getDescriptor());
        editor.setValue(property.getValue());
        String[] tags = editor.getTags();
        String[] expectedTags = FilterTableParameters.MODES;
        assertEquals(expectedTags.length, tags.length);
        assertArrayEquals( "Tags", expectedTags, tags );
        property.setValue(TextUtil.fromString(property.getType(), expectedTags[1]));

        // Check whether values were passed correctly to analysis node
        FilterTableParameters analysisParameters = (FilterTableParameters)WorkflowEngine.getAnalysisParametersByNode(analysis);
        assertEquals(1, analysisParameters.getFilteringMode());
        assertEquals(2, analysisParameters.getValuesCount());
        assertEquals("ID", analysisParameters.getFilterExpression());
        assertEquals(DataElementPath.create("test/path"), analysisParameters.getInputPath());
        assertEquals(DataElementPath.create("test/path out"), analysisParameters.getOutputPath());

        // Prepare data for workflow
        VectorDataCollection<TableDataCollection> vdc = new VectorDataCollection<>( "test",
                StandardTableDataCollection.class, null);
        CollectionFactory.registerRoot(vdc);
        TableDataCollection tdc = new StandardTableDataCollection(vdc, "path");
        TableDataCollectionUtils.addRow(tdc, "A", new Object[0]);
        TableDataCollectionUtils.addRow(tdc, "B", new Object[0]);
        TableDataCollectionUtils.addRow(tdc, "C", new Object[0]);
        TableDataCollectionUtils.addRow(tdc, "D", new Object[0]);
        TableDataCollectionUtils.addRow(tdc, "E", new Object[0]);
        vdc.put(tdc);

        // Run workflow
        WorkflowEngine engine = runWorkflow(parameters, null);

        // Check results
        Collection<ru.biosoft.access.core.DataElementPath> autoOpenPaths = engine.getAutoOpenPaths();
        assertEquals(1, autoOpenPaths.size());
        assertEquals(DataElementPath.create("test/path out"), autoOpenPaths.iterator().next());

        TableDataCollection table = DataElementPath.create("test/path out").getDataElement(TableDataCollection.class);
        assertEquals(2, table.getSize());
        assertEquals( new HashSet<>( Arrays.asList( "D", "E" ) ), new HashSet<>( table.getNameList() ) );

        // Cleanup
        CollectionFactory.unregisterAllRoot();
    }

    public void testScriptWorkflow() throws Exception
    {
        createWorkflow();
        String script = "importClass(java.lang.System);System.out.print('Hello!')";
        Compartment node = workflowController.createScriptNode(workflow, "", "js");
        assertTrue(workflowController.canAccept(workflow, node));
        viewEditor.add(node, new Point(10,10));
        Object scriptParameters = WorkflowEngine.getScriptParameters(node);
        assertEquals("", BeanUtil.getBeanPropertyValue(scriptParameters, "script"));
        ComponentFactory.getModel(scriptParameters).findProperty("script").setValue(script);
        assertEquals(script, BeanUtil.getBeanPropertyValue(scriptParameters, "script"));

        Logger logger = getLogger();

        Writer writer = new StringWriter();
        Handler[] hh = logger.getHandlers();
        for( Handler h : hh )
        {
            logger.removeHandler( h );
        }
        logger.addHandler( new WriterHandler( writer, new PatternFormatter( "%4$s - %5$s%n" ) ) );
        logger.setLevel(Level.INFO);
        runWorkflow(null, logger);
        assertEquals("INFO - Hello!", writer.toString().trim());
    }

    private Logger getLogger()
    {
        return Logger.getLogger("workflow-test-logger");
    }

    public void testScriptParametersWorkflow() throws Exception
    {
        createWorkflow();

        WorkflowParameter inputParameter = (WorkflowParameter)WorkflowItemFactory.getWorkflowItem(workflow, Type.ANALYSIS_PARAMETER);
        inputParameter.setName("input");
        inputParameter.setType(VariableType.getType(String.class));
        viewEditor.add(inputParameter.getNode(), new Point(10,10));

        WorkflowExpression testExpression = (WorkflowExpression)WorkflowItemFactory.getWorkflowItem(workflow, Type.ANALYSIS_EXPRESSION);
        testExpression.setName("test");
        testExpression.setType(VariableType.getType(String.class));
        testExpression.setExpression("$workflowPath$:$input$+bar");
        viewEditor.add(testExpression.getNode(), new Point(10,10));

        VectorDataCollection<DataElement> vdc = new VectorDataCollection<>( "test" );
        CollectionFactory.registerRoot(vdc);
        vdc.put(new JSElement(vdc, "js", "Packages.java.lang.System.out.print('Parameter: '+$['test'])"));

        DataElementPath path = DataElementPath.create("test/js");
        Compartment node = workflowController.createScriptNode(workflow, DataElementPath.EMPTY_PATH);
        viewEditor.add(node, new Point(10,10));
        Object scriptParameters = WorkflowEngine.getScriptParameters(node);
        assertEquals(DataElementPath.EMPTY_PATH, BeanUtil.getBeanPropertyValue(scriptParameters, "scriptPath"));
        ComponentFactory.getModel(scriptParameters).findProperty("scriptPath").setValue(path);
        assertEquals(path, BeanUtil.getBeanPropertyValue(scriptParameters, "scriptPath"));

        DynamicPropertySet parameters = WorkflowItemFactory.getWorkflowParameters(workflow);
        parameters.setValue("input", "foo");

        Logger logger = getLogger();
        Writer writer = new StringWriter();
        logger.addHandler( new WriterHandler( writer, new PatternFormatter( "%4$s - %5$s%n" ) ) );
        logger.setLevel(Level.INFO);
        runWorkflow(parameters, logger);
        assertEquals("INFO - Parameter: workflow:foo+bar", writer.toString().trim());

        CollectionFactory.unregisterAllRoot();
    }

    public void testCycleWorkflow() throws Exception
    {
        createWorkflow();

        WorkflowCycleVariable item = (WorkflowCycleVariable)WorkflowItemFactory.getWorkflowItem(workflow, Type.ANALYSIS_CYCLE_VARIABLE);
        item.setName("cycle");
        item.setType(VariableType.getType(String.class));
        item.setCycleType(new RangeCycleType());
        item.setExpression("1..10");
        assertEquals("Range (first[,second]..last)", item.getCycleType().toString());
        Compartment cycleNode = workflowController.createCycleNode(workflow, item);
        assertTrue(workflowController.canAccept(workflow, cycleNode));
        viewEditor.add(cycleNode, new Point(10,10));

        Compartment node = workflowController.createScriptNode(cycleNode, "Packages.java.lang.System.out.print(cycle)", "js");
        assertTrue(workflowController.canAccept(cycleNode, node));
        viewEditor.add(node, new Point(10,10));

        Logger logger = getLogger();
        Writer writer = new StringWriter();
        logger.addHandler( new WriterHandler( writer, new PatternFormatter( "%4$s - %5$s%n" ) ) );
        logger.setLevel(Level.INFO);
        runWorkflow(null, logger);
        String[] strings = writer.toString().trim().split("\n");
        assertEquals(10, strings.length);
        for(int i=0; i<strings.length; i++)
            assertEquals("INFO - "+(i+1), strings[i].trim());
    }

    public void testParallelCycleWorkflow() throws Exception
    {
        createWorkflow();

        WorkflowCycleVariable item = (WorkflowCycleVariable)WorkflowItemFactory.getWorkflowItem(workflow, Type.ANALYSIS_CYCLE_VARIABLE);
        item.setName("cycle");
        item.setType(VariableType.getType(String.class));
        item.setCycleType(new RangeCycleType());
        item.setExpression("1..10");
        item.setParallel(true);
        Compartment cycleNode = workflowController.createCycleNode(workflow, item);
        assertTrue(workflowController.canAccept(workflow, cycleNode));
        viewEditor.add(cycleNode, new Point(10,10));

        String analysisName = "Filter table";
        AnalysisMethodRegistry.addTestAnalysis(analysisName, FilterTable.class);

        // Analysis node
        Compartment analysis = (Compartment)workflowController.addAnalysis(cycleNode, analysisName, new Point(10,10), viewEditor);

        // 'input' workflow parameter
        WorkflowParameter parameter = connectInput((Node)analysis.get("inputPath"), "input");
        parameter.setDataElementType(DataElementType.getType(TableDataCollection.class));
        parameter.setExpression("test/input");

        // 'expression' workflow parameter
        WorkflowExpression expression = (WorkflowExpression)WorkflowItemFactory.getWorkflowItem(cycleNode, Type.ANALYSIS_EXPRESSION);
        expression.setName("expression");
        expression.setExpression("Value > $cycle$");
        expression.setType(VariableType.getType(String.class));
        assertTrue(workflowController.canAccept((Compartment)expression.getNode().getOrigin(), expression.getNode()));
        viewEditor.add(expression.getNode(), new Point(10,10));
        Node expressionElement = (Node)cycleNode.get("expression");
        assertNotNull(expressionElement);

        // 'expression' -> 'Filter table:filterExpression' edge
        ComponentModel model = ComponentFactory.getModel(new FilterTableParameters());
        workflowController.bindParameter(expressionElement, analysis, model.findProperty("filterExpression"), viewEditor);

        // 'output' workflow parameter
        parameter = connectOutput((Node)analysis.get("outputPath"), "output");
        parameter.setExpression("test/out $cycle$");
        parameter.setDataElementType(DataElementType.getType(TableDataCollection.class));

        // Prepare data for workflow
        VectorDataCollection<TableDataCollection> vdc = new VectorDataCollection<>( "test", StandardTableDataCollection.class, null );
        CollectionFactory.registerRoot(vdc);
        TableDataCollection tdc = new StandardTableDataCollection(vdc, "input");
        tdc.getColumnModel().addColumn("Value", Integer.class);
        for(int i=1; i<=11; i++)
        {
            TableDataCollectionUtils.addRow(tdc, String.valueOf(i), new Object[] {i});
        }
        vdc.put(tdc);

        // Run workflow
        runWorkflow(null, null);

        for(int i=1; i<=10; i++)
        {
            TableDataCollection out = DataElementPath.create("test/out "+i).getDataElement(TableDataCollection.class);
            assertEquals(11-i, out.getSize());
        }
        CollectionFactory.unregisterAllRoot();
    }

    public void testViewBuilderAnalysis() throws Exception
    {
        createWorkflow();

        String analysisName = "Filter table";
        AnalysisMethodRegistry.addTestAnalysis(analysisName, FilterTable.class);
        Compartment analysis = (Compartment)workflowController.addAnalysis(workflow, analysisName, new Point(10,20), viewEditor);

        workflow.setView(null);
        ImageGenerator.generateDiagramView(workflow, ApplicationUtils.getGraphics());
        assertTrue(workflow.getView() instanceof CompositeView);
        assertTrue(analysis.getView() instanceof CompositeView);
        CompositeView analysisView = (CompositeView)analysis.getView();
        assertEquals(analysis, analysisView.getModel());
        Point workflowPos = workflow.getView().getBounds().getLocation();
        Point analysisPos = analysisView.getBounds().getLocation();
        assertEquals(10, analysisPos.x-workflowPos.x);
        assertEquals(20, analysisPos.y-workflowPos.y);
        int found = 0;
        for(View view: analysisView)
        {
            Node model = (Node)view.getModel();
            if(model != null && model.getOrigin() == analysis)
            {
                assertTrue(view instanceof CompositeView);
                View childView = ((CompositeView)view).elementAt(0);
                assertTrue(childView instanceof ImageView);
                assertEquals(16.0, ((ImageView)childView).getBounds().getWidth());
                assertEquals(16.0, ((ImageView)childView).getBounds().getHeight());
                found++;
            }
        }
        assertEquals(2, found);
    }

    private void createWorkflow() throws Exception
    {
        workflow = new WorkflowDiagramType().createDiagram(null, "workflow", null);
        workflowController = (WorkflowSemanticController)workflow.getType().getSemanticController();
        DiagramEditorHelper helper = new DiagramEditorHelper(workflow);
        viewEditor = new ViewEditorPaneStub(helper, workflow);
        ImageGenerator.generateDiagramView(workflow, ApplicationUtils.getGraphics());
    }

    private WorkflowParameter connectInput(Node analysisInput, String inputName)
    {
        WorkflowParameter parameter = (WorkflowParameter)WorkflowItemFactory.getWorkflowItem(workflow, Type.ANALYSIS_PARAMETER);
        parameter.setName(inputName);
        parameter.setType(VariableType.getType(DataElementPath.class));
        parameter.setRole(WorkflowParameter.ROLE_INPUT);
        assertTrue(workflowController.canAccept(workflow, parameter.getNode()));
        viewEditor.add(parameter.getNode(), new Point(10,10));

        // edge
        Edge edge = new Edge(new Stub(null, "edge", Base.TYPE_DIRECTED_LINK), parameter.getNode(), analysisInput);
        workflowController.annotateEdge(edge);
        assertTrue(workflowController.canAccept(workflow, edge));
        viewEditor.add(edge, new Point(10,10));
        return parameter;
    }

    private WorkflowParameter connectOutput(Node analysisOutput, String outputName)
    {
        WorkflowParameter parameter = (WorkflowParameter)WorkflowItemFactory.getWorkflowItem((Compartment)analysisOutput.getOrigin().getOrigin(), Type.ANALYSIS_PARAMETER);
        parameter.setName(outputName);
        parameter.setType(VariableType.getType(DataElementPath.class));
        parameter.setRole(WorkflowParameter.ROLE_OUTPUT);
        assertTrue(workflowController.canAccept((Compartment)parameter.getNode().getOrigin(), parameter.getNode()));
        viewEditor.add(parameter.getNode(), new Point(10,10));

        // edge
        Edge edge = new Edge(new Stub(null, "edge", Base.TYPE_DIRECTED_LINK), analysisOutput, parameter.getNode());
        workflowController.annotateEdge(edge);
        assertTrue(workflowController.canAccept((Compartment)edge.getOrigin(), edge));
        viewEditor.add(edge, new Point(10,10));
        return parameter;
    }

    private WorkflowEngine runWorkflow(DynamicPropertySet parameters, Logger logger) throws Exception, InterruptedException
    {
        JournalRegistry.setJournalUse(false);
        WorkflowEngine engine = new WorkflowEngine();
        engine.setWorkflow(workflow);
        if(logger != null)
            engine.setLogger( java.util.logging.Logger.getLogger( logger.getName() ) );
        if(parameters == null)
            parameters = WorkflowItemFactory.getWorkflowParameters(workflow);
        engine.setParameters(parameters);
        engine.initWorkflow();
        finished = false;
        engine.addEngineListener(new WorkflowEngineListener()
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
            }

            @Override
            public void parameterErrorDetected(String error)
            {
                WorkflowTest.this.error = error;
            }

            @Override
            public void finished()
            {
                finished = true;
            }

            @Override
            public void errorDetected(String error)
            {
                WorkflowTest.this.error = error;
                finished = true;
            }
        });
        engine.start();
        while(!finished)
            Thread.sleep(100);
        if(error != null)
            fail(error);
        return engine;
    }
}
