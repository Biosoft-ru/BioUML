package biouml.plugins.mcp._test;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.plugins.mcp.McpConstants;
import biouml.plugins.mcp.server.McpToolCatalog;
import biouml.plugins.mcp.support.McpAnalysisSupport;
import biouml.plugins.mcp.support.McpEnvelope;
import biouml.plugins.mcp.tools.analysis.AnalysisTools;

import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.Repository;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.generic.GenericDataCollection;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.TempFiles;

/**
 * Phase-3 tests for the analysis + task MCP tools, using the {@link McpTestAnalysis} stub.
 * Exercises list / describe / run(sync+async) / task-status / cancel / list-tasks / repeat /
 * get-result headlessly against a temp-dir {@link LocalRepository} fixture.
 */
public class McpAnalysisToolsTest extends AbstractBioUMLTest
{
	private static boolean stubRegistered;
	private File dir;
	/** A folder collection the stub can write its result table into. */
	private String outRoot;

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		dir = TempFiles.dir( "mcpAnalysisTest" );
		Properties properties = new ExProperties();
		properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, "mcpanalysis" );
		properties.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName() );
		properties.setProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, dir.toString() );
		ExProperties.store( properties, new File( dir, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE ) );
		Repository repository = (Repository) CollectionFactory.createCollection( null, properties );
		CollectionFactory.registerRoot( repository );
		repository.getNameList();
		// A folder collection to hold the stub's output table.
		DataCollection<?> folder = GenericDataCollection.createGenericCollection( repository, repository, "out", "out" );
		assertNotNull( "folder created", folder );
		repository.put( folder );
		outRoot = "mcpanalysis/out";
		ensureStub();
	}

	@Override
	protected void tearDown() throws Exception
	{
		super.tearDown();
		ApplicationUtils.removeDir( dir );
	}

	/** Ensure the stub analysis is registered once for the whole test JVM. */
	private static synchronized void ensureStub()
	{
		if ( stubRegistered )
			return;
		// isTestMode() is true in a plain JVM (no OSGi bundle). Force the registry to initialize
		// (instance is created lazily) — a read call like getAnalysisGroups() triggers init().
		AnalysisMethodRegistry.getAnalysisGroups().findAny();
		// Register the stub in BOTH the in-memory methods index (so getMethodInfo/getAnalysisMethod
		// can resolve it) and a group's DataCollection (so getAnalysisNamesWithGroup — which
		// listMethods iterates — sees it). addMethodToGroup does both.
		ru.biosoft.analysiscore.AnalysisMethodInfo info =
				new ru.biosoft.analysiscore.AnalysisMethodInfo( McpTestAnalysis.NAME, "Test stub analysis", null, McpTestAnalysis.class );
		AnalysisMethodRegistry.addMethodToGroup( McpTestAnalysis.NAME, "McpTest", info );
		stubRegistered = true;
	}

	private Map<String, Object> params( int rows )
	{
		Map<String, Object> p = new LinkedHashMap<String, Object>();
		p.put( "rows", rows );
		p.put( "output", outRoot + "/result" );
		return p;
	}

	@SuppressWarnings( "unchecked" )
	public void testListMethods()
	{
		McpEnvelope env = McpAnalysisSupport.listMethods();
		assertTrue( "list should be ok, got " + env.getCode(), env.isOk() );
		Map<String, Object> m = (Map<String, Object>) env.getData();
		Map<String, List<Map<String, Object>>> groups = (Map<String, List<Map<String, Object>>>) m.get( "groups" );
		assertNotNull( "groups present", groups );
		boolean found = false;
		for ( List<Map<String, Object>> list : groups.values() )
			for ( Map<String, Object> mm : list )
				if ( McpTestAnalysis.NAME.equals( mm.get( "name" ) ) )
					found = true;
		assertTrue( "stub analysis must be listed", found );
	}

	/**
	 * Phase-3 evidence transcript: prints the concrete outputs of the analysis tools (list, describe,
	 * bad-param error, sync run → result rows) to the test log. This is the transcript artifact the
	 * phase-3 spec requires; the assertions live in the other test methods.
	 */
	public void testEvidenceTranscript() throws Exception
	{
		com.fasterxml.jackson.databind.ObjectMapper m = new com.fasterxml.jackson.databind.ObjectMapper();
		System.out.println( "========== MCP ANALYSIS EVIDENCE TRANSCRIPT ==========" );
		System.out.println( "### biouml_analysis_list" );
		System.out.println( m.writerWithDefaultPrettyPrinter().writeValueAsString( McpAnalysisSupport.listMethods().toMap() ) );
		System.out.println( "### biouml_analysis_describe " + McpTestAnalysis.NAME );
		System.out.println( m.writerWithDefaultPrettyPrinter().writeValueAsString( McpAnalysisSupport.describeMethod( McpTestAnalysis.NAME ).toMap() ) );
		Map<String, Object> bad = new LinkedHashMap<String, Object>();
		bad.put( "noSuchKey", 1 );
		System.out.println( "### biouml_analysis_run (bad param - valid-keys error)" );
		System.out.println( m.writerWithDefaultPrettyPrinter().writeValueAsString( McpAnalysisSupport.runAnalysis( McpTestAnalysis.NAME, bad, null, true ).toMap() ) );
		Map<String, Object> good = params( 3 );
		System.out.println( "### biouml_analysis_run (sync) result" );
		System.out.println( m.writerWithDefaultPrettyPrinter().writeValueAsString( McpAnalysisSupport.runAnalysis( McpTestAnalysis.NAME, good, null, true ).toMap() ) );
		System.out.println( "### biouml_analysis_get_result " + ( outRoot + "/result" ) );
		System.out.println( m.writerWithDefaultPrettyPrinter().writeValueAsString( McpAnalysisSupport.getAnalysisResult( outRoot + "/result" ).toMap() ) );
		System.out.println( "========== END ANALYSIS EVIDENCE TRANSCRIPT ==========" );
	}

	@SuppressWarnings( "unchecked" )
	public void testDescribeMethod()
	{
		McpEnvelope env = McpAnalysisSupport.describeMethod( McpTestAnalysis.NAME );
		assertTrue( "describe should be ok, got " + env.getCode() + " " + env.getError(), env.isOk() );
		Map<String, Object> m = (Map<String, Object>) env.getData();
		List<Map<String, Object>> props = (List<Map<String, Object>>) m.get( "properties" );
		assertNotNull( "properties present", props );
		boolean hasRows = false;
		boolean hasDefault = false;
		for ( Map<String, Object> p : props )
		{
			if ( "rows".equals( p.get( "name" ) ) )
			{
				hasRows = true;
				assertNotNull( "rows must have a type", p.get( "type" ) );
				hasDefault = p.containsKey( "default" );
			}
		}
		assertTrue( "describe must expose the 'rows' property", hasRows );
		assertTrue( "describe must capture a default value", hasDefault );
	}

	@SuppressWarnings( "unchecked" )
	public void testRunSync()
	{
		McpEnvelope env = McpAnalysisSupport.runAnalysis( McpTestAnalysis.NAME, params( 4 ), null, true );
		assertTrue( "sync run should be ok, got " + env.getCode() + " " + env.getError(), env.isOk() );
		Object data = env.getData();
		assertTrue( "result should be a map", data instanceof Map );
		Map<String, Object> r = (Map<String, Object>) data;
		assertTrue( "resultType is a table", r.get( "resultType" ) != null && ((String) r.get( "resultType" ) ).contains( "TableDataCollection" ) );
		String resultPath = (String) r.get( "resultPath" );
		assertNotNull( "resultPath present", resultPath );
		assertEquals( "resultPath is the requested path", outRoot + "/result", resultPath );

		// Read the result back.
		McpEnvelope res = McpAnalysisSupport.getAnalysisResult( resultPath );
		assertTrue( "get_result should be ok", res.isOk() );
		Map<String, Object> rm = (Map<String, Object>) res.getData();
		assertEquals( "row count is 4", 4, ((Number) rm.get( "rowCount" ) ).intValue() );
		List<String> cols = (List<String>) rm.get( "columns" );
		assertTrue( "columns include id", cols.contains( "id" ) );
		assertTrue( "columns include value", cols.contains( "value" ) );
	}

	@SuppressWarnings( "unchecked" )
	public void testRunAsyncThenPoll()
	{
		// Queue under the same named user the delegated task_status polls as (TASK_USER), mirroring
		// production where the servlet authenticates every request.
		McpEnvelope env = McpAnalysisSupport.runAnalysisAs( McpConstants.TASK_USER, McpTestAnalysis.NAME, params( 2 ), null, false );
		assertTrue( "async run should be ok, got " + env.getCode() + " " + env.getError(), env.isOk() );
		Map<String, Object> m = (Map<String, Object>) env.getData();
		String taskId = (String) m.get( "taskId" );
		assertNotNull( "taskId present", taskId );

		// Poll the delegated task_status until the provider reports the JobControl done status (2).
		long deadline = System.currentTimeMillis() + 30000;
		Integer status = null;
		while ( System.currentTimeMillis() < deadline )
		{
			McpEnvelope st = McpAnalysisSupport.taskStatus( taskId );
			assertTrue( "task_status ok: " + st.getCode() + " " + st.getError(), st.isOk() );
			Object s = ((Map<String, Object>) st.getData()).get( "status" );
			status = s instanceof Number ? ( (Number) s ).intValue() : null;
			if ( status != null && status == ru.biosoft.jobcontrol.JobControl.COMPLETED )
				break;
			try
			{
				Thread.sleep( 50 );
			}
			catch ( InterruptedException ie )
			{
				Thread.currentThread().interrupt();
				break;
			}
		}
		assertNotNull( "task_status should return a status", status );
		assertEquals( "task should reach done (JobControl.COMPLETED=2), was " + status,
				ru.biosoft.jobcontrol.JobControl.COMPLETED, status.intValue() );
	}

	public void testBadParamListsValidKeys()
	{
		Map<String, Object> p = params( 1 );
		p.remove( "rows" );
		p.put( "noSuchKey", 1 );
		McpEnvelope env = McpAnalysisSupport.runAnalysis( McpTestAnalysis.NAME, p, null, true );
		assertFalse( "bad param must be refused", env.isOk() );
		assertEquals( "code is invalid_params", McpConstants.CODE_INVALID_PARAMS, env.getCode() );
		assertTrue( "error must list a valid key ('rows')", env.getError() != null && env.getError().contains( "rows" ) );
	}

	@SuppressWarnings( "unchecked" )
	public void testListTasks()
	{
		McpEnvelope env = McpAnalysisSupport.listTasks();
		assertTrue( "list tasks ok", env.isOk() );
		Map<String, Object> m = (Map<String, Object>) env.getData();
		assertNotNull( "tasks present", m.get( "tasks" ) );
	}

	@SuppressWarnings( "unchecked" )
	public void testRepeatAnalysis()
	{
		McpEnvelope env = McpAnalysisSupport.runAnalysis( McpTestAnalysis.NAME, params( 3 ), null, true );
		assertTrue( env.isOk() );
		String resultPath = (String) ((Map<String, Object>) env.getData()).get( "resultPath" );
		assertNotNull( resultPath );

		McpEnvelope rep = McpAnalysisSupport.repeatAnalysis( resultPath, true );
		assertTrue( "repeat should be ok, got " + rep.getCode() + " " + rep.getError(), rep.isOk() );
		assertNotNull( "repeat returns a result", rep.getData() );
	}

	@SuppressWarnings( "unchecked" )
	public void testCancelQueuedTask()
	{
		McpEnvelope env = McpAnalysisSupport.runAnalysisAs( McpConstants.TASK_USER, McpTestAnalysis.NAME, params( 1 ), null, false );
		assertTrue( env.isOk() );
		String taskId = (String) ((Map<String, Object>) env.getData()).get( "taskId" );
		McpEnvelope cancel = McpAnalysisSupport.cancelTask( taskId );
		assertTrue( "cancel should be ok, got " + cancel.getCode() + " " + cancel.getError(), cancel.isOk() );
		Map<String, Object> m = (Map<String, Object>) cancel.getData();
		assertTrue( "cancelled flag set", Boolean.TRUE.equals( m.get( "cancelled" ) ) );
	}

	public void testToolsListRegistersAnalysisTools()
	{
		McpToolCatalog catalog = new McpToolCatalog();
		AnalysisTools.registerAll( catalog );
		List<String> names = new java.util.ArrayList<String>();
		for ( McpToolCatalog.Tool t : catalog.getTools() )
			names.add( t.name );
		assertTrue( "analysis_list registered", names.contains( "biouml_analysis_list" ) );
		assertTrue( "analysis_run registered", names.contains( "biouml_analysis_run" ) );
		assertTrue( "task_status registered", names.contains( "biouml_task_status" ) );
		assertTrue( "analysis_get_result registered", names.contains( "biouml_analysis_get_result" ) );
	}

	/**
	 * geneXplain-compatibility constraint: listing must work even when no external
	 * (geneXplain-style) analysis registry is present — i.e. with only the in-repo stub.
	 */
	@SuppressWarnings( "unchecked" )
	public void testListingToleratesAbsentGeneXplainRegistry()
	{
		McpEnvelope env = McpAnalysisSupport.listMethods();
		assertTrue( "listing must be ok with no geneXplain registry", env.isOk() );
		Map<String, Object> m = (Map<String, Object>) env.getData();
		assertTrue( "total >= 1 (the stub)", ((Number) m.get( "total" ) ).intValue() >= 1 );
	}
}
