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
import biouml.plugins.mcp.support.McpProviderSupport;
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

	/**
	 * A simulation result (e.g. produced by the "Simulation analysis" method) must be readable via
	 * biouml_analysis_get_result as a {vars, times, values} time series — not just tables/folders.
	 */
	@SuppressWarnings( "unchecked" )
	public void testGetResultSimulationResult() throws Exception
	{
		biouml.standard.simulation.SimulationResult sr =
				new biouml.standard.simulation.SimulationResult( null, "simresult" );
		// Two variables across two time points, values stored as [point][var]: X(t)=[1,2], Y(t)=[10,20].
		sr.add( 0.0, new double[] { 1.0, 10.0 } );
		sr.add( 1.0, new double[] { 2.0, 20.0 } );
		java.util.Map<String, Integer> varMap = new java.util.LinkedHashMap<String, Integer>();
		varMap.put( "X", Integer.valueOf( 0 ) );
		varMap.put( "Y", Integer.valueOf( 1 ) );
		sr.setVariableMap( varMap );

		// Persist into the temp repo folder so getAnalysisResult can resolve it by path.
		DataCollection<?> folder = (DataCollection<?>) CollectionFactory.getDataCollection( outRoot );
		assertNotNull( "out folder exists", folder );
		@SuppressWarnings( "unchecked" )
		DataCollection<ru.biosoft.access.core.DataElement> p = (DataCollection<ru.biosoft.access.core.DataElement>) folder;
		p.put( sr );
		String simPath = outRoot + "/" + sr.getName();

		McpEnvelope env = McpAnalysisSupport.getAnalysisResult( simPath );
		assertTrue( "get_result should be ok, got " + env.getCode() + " " + env.getError(), env.isOk() );
		Map<String, Object> m = (Map<String, Object>) env.getData();
		assertEquals( "type is simulation_result", "simulation_result", m.get( "type" ) );
		Map<String, Object> vars = (Map<String, Object>) m.get( "vars" );
		assertTrue( "vars include X", vars.containsKey( "X" ) );
		assertTrue( "vars include Y", vars.containsKey( "Y" ) );
		List<List<Object>> times = (List<List<Object>>) m.get( "times" );
		assertEquals( "two time points", 2, times.size() );
		List<List<Object>> values = (List<List<Object>>) m.get( "values" );
		assertEquals( "two variables (rows)", 2, values.size() );
		// Row 0 = variable X (index 0) over the two time points: [1, 2].
		List<Object> xRow = values.get( 0 );
		assertEquals( "X[0]", 1.0, ( (Number) xRow.get( 0 ) ).doubleValue(), 1e-9 );
		assertEquals( "X[1]", 2.0, ( (Number) xRow.get( 1 ) ).doubleValue(), 1e-9 );
		// Row 1 = variable Y: [10, 20].
		List<Object> yRow = values.get( 1 );
		assertEquals( "Y[0]", 10.0, ( (Number) yRow.get( 0 ) ).doubleValue(), 1e-9 );
		assertEquals( "Y[1]", 20.0, ( (Number) yRow.get( 1 ) ).doubleValue(), 1e-9 );
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

	/** Repeated log problems collapse into one entry with a count; the latest distinct ones are kept. */
	@SuppressWarnings( "unchecked" )
	public void testSummarizeLogGroupsRepeats()
	{
		StringBuilder log = new StringBuilder( "INFO: start\n" );
		for ( int i = 0; i < 1000; i++ )
			log.append( "ERROR: trying to get description of gene ENSMUSG0000" ).append( 10000 + i ).append( '\n' );
		log.append( "WARNING: Skip failed analysis Filter table\n" ).append( "INFO: done\n" );
		Map<String, Object> m = McpAnalysisSupport.summarizeLog( log.toString() );
		List<Map<String, Object>> problems = (List<Map<String, Object>>) m.get( "problems" );
		assertEquals( "two kinds of problems", 2, problems.size() );
		assertEquals( Integer.valueOf( 1000 ), problems.get( 0 ).get( "count" ) );
		assertTrue( ( (String) problems.get( 1 ).get( "message" ) ).contains( "Skip failed analysis Filter table" ) );
		assertEquals( Integer.valueOf( 1001 ), m.get( "problemCount" ) );
		assertEquals( Integer.valueOf( 2 ), m.get( "problemKinds" ) );
		List<String> tail = (List<String>) m.get( "logTail" );
		assertEquals( "INFO: done", tail.get( tail.size() - 1 ) );
		assertTrue( "1000 lines differing in numbers are one tail line", tail.contains( "ERROR: trying to get description of gene ENSMUSG000010999 [×1000]" ) );
		assertEquals( 4, tail.size() );
		assertTrue( McpAnalysisSupport.summarizeLog( null ).isEmpty() );
	}

	/**
	 * The level of a tool's line relayed into the log decides (lines of run 4's Genome Enhancer log): Picard's INFO
	 * lines and its echoed command line are no problems, its WARNING lines are, and a cut-off copy of a line counts
	 * with the whole one.
	 */
	@SuppressWarnings( "unchecked" )
	public void testSummarizeLogLevels()
	{
		String contig = "INFO: WARNING\t2026-10-16 08:30:33\tLiftoverVcf\tEncountered a contig, chr7_KI270803v1_alt that is not part of";
		StringBuilder log = new StringBuilder();
		log.append( "INFO: [Fri Oct 16 08:30:26 CEST 2026] LiftoverVcf INPUT=/tmp/sorted.vcf OUTPUT=/tmp/out.vcf WARN_ON_MISSING_CONTIG=true LOG_FAILED_INTERVALS=true\n" );
		log.append( "INFO: INFO\t2026-10-16 08:30:33\tLiftOver\tInterval chr4:177605081-177605084 failed to match chain 4 because intersection length 1 < minMatchSize 4.0\n" );
		for ( int i = 0; i < 12; i++ )
			log.append( contig ).append( " the target reference.\n" );
		log.append( contig ).append( '\n' );
		log.append( "ERROR: INFO\t2026-10-16 08:30:40\tLiftoverVcf\tWriting out sorted records to final VCF.\n" );
		log.append( "INFO: Skip failed analysis Filter table with following error: x\n" );
		log.append( "WARN: Can not find promoter for ENSG00000184701\n" );
		log.append( "ERROR: java.io.IOException: disk full\n" ).append( "\tat ru.biosoft.Some.method(Some.java:1)\n" );
		Map<String, Object> m = McpAnalysisSupport.summarizeLog( log.toString() );
		List<Map<String, Object>> problems = (List<Map<String, Object>>) m.get( "problems" );
		assertEquals( "problems: " + problems, 4, problems.size() );
		assertEquals( contig + " the target reference.", problems.get( 0 ).get( "message" ) );
		assertEquals( Integer.valueOf( 13 ), problems.get( 0 ).get( "count" ) );
		assertTrue( ( (String) problems.get( 1 ).get( "message" ) ).contains( "Skip failed analysis" ) );
		assertTrue( ( (String) problems.get( 2 ).get( "message" ) ).contains( "Can not find promoter" ) );
		assertTrue( ( (String) problems.get( 3 ).get( "message" ) ).contains( "disk full" ) );
		assertEquals( Integer.valueOf( 16 ), m.get( "problemCount" ) );
		assertEquals( Integer.valueOf( 4 ), m.get( "problemKinds" ) );
	}

	/** Table rows come with their ids and can be paged. */
	@SuppressWarnings( "unchecked" )
	public void testGetResultPagingAndRowIds()
	{
		Map<String, Object> p = params( 5 );
		p.put( "output", outRoot + "/paged" );
		assertTrue( McpAnalysisSupport.runAnalysis( McpTestAnalysis.NAME, p, null, true ).isOk() );
		Map<String, Object> all = (Map<String, Object>) McpAnalysisSupport.getAnalysisResult( outRoot + "/paged" ).getData();
		assertEquals( java.util.Arrays.asList( "row0", "row1", "row2", "row3", "row4" ), all.get( "rowIds" ) );
		assertEquals( Boolean.FALSE, all.get( "truncated" ) );
		Map<String, Object> page = (Map<String, Object>) McpAnalysisSupport.getAnalysisResult( outRoot + "/paged", 2, 2 ).getData();
		assertEquals( java.util.Arrays.asList( "row2", "row3" ), page.get( "rowIds" ) );
		assertEquals( 2, ( (List<?>) page.get( "rows" ) ).size() );
		assertEquals( Integer.valueOf( 2 ), page.get( "offset" ) );
		assertEquals( Boolean.TRUE, page.get( "truncated" ) );
		// only the columns asked for, in that order
		Map<String, Object> narrow = (Map<String, Object>) McpAnalysisSupport
				.getAnalysisResult( outRoot + "/paged", 0, 2, java.util.Arrays.asList( "value", "id" ) ).getData();
		assertEquals( java.util.Arrays.asList( "value", "id" ), narrow.get( "columns" ) );
		assertEquals( java.util.Arrays.asList( "java.lang.Double", "java.lang.Integer" ), narrow.get( "columnTypes" ) );
		List<List<Object>> rows = (List<List<Object>>) narrow.get( "rows" );
		assertEquals( 2, rows.get( 0 ).size() );
		assertEquals( ( (List<List<Object>>) all.get( "rows" ) ).get( 1 ).get( 0 ), rows.get( 1 ).get( 1 ) );
		McpEnvelope bad = McpAnalysisSupport.getAnalysisResult( outRoot + "/paged", null, null, java.util.Arrays.asList( "nope" ) );
		assertEquals( McpConstants.CODE_INVALID_PARAMS, bad.getCode() );
		assertTrue( bad.getError().contains( "value" ) );
	}

	/** A track's sites are read like table rows: chromosome, position, strand and the site properties, paged. */
	@SuppressWarnings( "unchecked" )
	public void testGetResultTrackSites() throws Exception
	{
		ru.biosoft.access.core.VectorDataCollection<ru.biosoft.access.core.DataElement> root = new ru.biosoft.access.core.VectorDataCollection<>( "mcptracks" );
		CollectionFactory.registerRoot( root );
		ru.biosoft.bsa.Sequence seq = new ru.biosoft.bsa.LinearSequence( "chr1", "acgtacgtacgtacgtacgt",
				ru.biosoft.bsa.Nucleotide5LetterAlphabet.getInstance() );
		ru.biosoft.bsa.TrackImpl track = new ru.biosoft.bsa.TrackImpl( "variants", root );
		for ( int i = 0; i < 5; i++ )
		{
			ru.biosoft.bsa.SiteImpl site = new ru.biosoft.bsa.SiteImpl( null, "s" + i, 2 + i, 1, ru.biosoft.bsa.StrandType.STRAND_PLUS, seq );
			site.getProperties().add( new com.developmentontheedge.beans.DynamicProperty( "Format_GT_x", String.class, i % 2 == 0 ? "0/1" : "1/1" ) );
			track.addSite( site );
		}
		root.put( track );

		Map<String, Object> page = (Map<String, Object>) McpAnalysisSupport.getAnalysisResult( "mcptracks/variants", 1, 2 ).getData();
		assertEquals( "track", page.get( "type" ) );
		assertEquals( Integer.valueOf( 5 ), page.get( "rowCount" ) );
		assertEquals( java.util.Arrays.asList( "s1", "s2" ), page.get( "rowIds" ) );
		assertEquals( java.util.Arrays.asList( "chromosome", "from", "to", "strand", "type", "Format_GT_x" ), page.get( "columns" ) );
		List<List<Object>> rows = (List<List<Object>>) page.get( "rows" );
		assertEquals( java.util.Arrays.asList( "chr1", Integer.valueOf( 3 ), Integer.valueOf( 3 ), "+", "misc_feature", "1/1" ), rows.get( 0 ) );
		assertEquals( Boolean.TRUE, page.get( "truncated" ) );

		Map<String, Object> gt = (Map<String, Object>) McpAnalysisSupport
				.getAnalysisResult( "mcptracks/variants", 3, 10, java.util.Arrays.asList( "Format_GT_x" ) ).getData();
		assertEquals( java.util.Arrays.asList( java.util.Arrays.asList( "1/1" ), java.util.Arrays.asList( "0/1" ) ), gt.get( "rows" ) );
		assertEquals( Boolean.FALSE, gt.get( "truncated" ) );
		assertEquals( McpConstants.CODE_INVALID_PARAMS,
				McpAnalysisSupport.getAnalysisResult( "mcptracks/variants", 0, 1, java.util.Arrays.asList( "nope" ) ).getCode() );
	}

	/** The project of a path is the child of the user projects folder that contains it. */
	public void testProjectOf()
	{
		ru.biosoft.access.core.DataElementPath projects = ru.biosoft.access.CollectionFactoryUtils.getUserProjectsPath();
		assertEquals( projects.getChildPath( "P" ),
				biouml.plugins.mcp.support.McpProjectContext.projectOf( projects.getChildPath( "P", "Data", "Results", "x" ) ) );
		assertEquals( projects.getChildPath( "P" ), biouml.plugins.mcp.support.McpProjectContext.projectOf( projects.getChildPath( "P" ) ) );
		assertNull( biouml.plugins.mcp.support.McpProjectContext.projectOf( projects ) );
		assertNull( biouml.plugins.mcp.support.McpProjectContext.projectOf( ru.biosoft.access.core.DataElementPath.create( "databases/x/y" ) ) );
	}

	/** A choice parameter lists its accepted values; a free-form one does not. */
	@SuppressWarnings( "unchecked" )
	public void testDescribeListsAllowedValues()
	{
		McpEnvelope env = McpAnalysisSupport.describeMethod( McpTestAnalysis.NAME );
		assertTrue( env.isOk() );
		Map<String, Object> mode = null, rows = null;
		for ( Map<String, Object> p : (List<Map<String, Object>>) ( (Map<String, Object>) env.getData() ).get( "properties" ) )
		{
			if ( "mode".equals( p.get( "name" ) ) )
				mode = p;
			if ( "rows".equals( p.get( "name" ) ) )
				rows = p;
		}
		assertNotNull( "mode described", mode );
		assertEquals( java.util.Arrays.asList( "fast", "exact" ), mode.get( "allowedValues" ) );
		assertFalse( "free-form int has no allowed values", rows.containsKey( "allowedValues" ) );
	}

	/**
	 * describe binds params first, so array items are listed with the keys analysis_run takes; properties lists only
	 * the parameters named; filter narrows the allowed values and, alone, lists only the choices with matches; a bad
	 * param is refused as by analysis_run.
	 */
	@SuppressWarnings( "unchecked" )
	public void testDescribeWithParamsAndFilter()
	{
		Map<String, Object> params = new java.util.LinkedHashMap<String, Object>();
		params.put( "conditions/0/condition", "Experiment" );
		params.put( "conditions/1/condition", "Control" );
		Map<String, Map<String, Object>> byName = properties( McpAnalysisSupport.describeMethod( McpTestAnalysis.NAME, params, null,
				java.util.Arrays.asList( "conditions" ) ) );
		assertEquals( "only the conditions and their items: " + byName.keySet(),
				java.util.Arrays.asList( "conditions", "conditions/0", "conditions/0/condition", "conditions/1", "conditions/1/condition" ),
				new java.util.ArrayList<String>( byName.keySet() ) );
		assertEquals( Integer.valueOf( 2 ), byName.get( "conditions" ).get( "itemCount" ) );
		assertEquals( "Experiment", byName.get( "conditions/0/condition" ).get( "default" ) );
		assertEquals( "Control", byName.get( "conditions/1/condition" ).get( "default" ) );

		McpEnvelope filtered = McpAnalysisSupport.describeMethod( McpTestAnalysis.NAME, null, "EX" );
		assertEquals( "EX", ( (Map<String, Object>) filtered.getData() ).get( "filter" ) );
		byName = properties( filtered );
		assertEquals( "only the choices with matches", java.util.Collections.singleton( "mode" ), byName.keySet() );
		Map<String, Object> mode = byName.get( "mode" );
		assertEquals( java.util.Arrays.asList( "exact" ), mode.get( "allowedValues" ) );
		assertEquals( Integer.valueOf( 1 ), mode.get( "matchingCount" ) );
		assertEquals( Integer.valueOf( 2 ), mode.get( "allowedValuesCount" ) );
		assertEquals( "no choice matches", 0, properties( McpAnalysisSupport.describeMethod( McpTestAnalysis.NAME, null, "zzz" ) ).size() );
		assertTrue( "a named parameter is kept, its values filtered",
				( (List<?>) properties( McpAnalysisSupport.describeMethod( McpTestAnalysis.NAME, null, "zzz", java.util.Arrays.asList( "mode" ) ) )
						.get( "mode" ).get( "allowedValues" ) ).isEmpty() );

		Map<String, Object> bad = new java.util.LinkedHashMap<String, Object>();
		bad.put( "nope", "x" );
		assertEquals( McpConstants.CODE_INVALID_PARAMS, McpAnalysisSupport.describeMethod( McpTestAnalysis.NAME, bad, null ).getCode() );
	}

	@SuppressWarnings( "unchecked" )
	private static Map<String, Map<String, Object>> properties( McpEnvelope env )
	{
		assertTrue( env.toString(), env.isOk() );
		Map<String, Map<String, Object>> byName = new java.util.LinkedHashMap<String, Map<String, Object>>();
		for ( Map<String, Object> p : (List<Map<String, Object>>) ( (Map<String, Object>) env.getData() ).get( "properties" ) )
			byName.put( (String) p.get( "name" ), p );
		return byName;
	}

	/** The values a page passes to an analysis are checked against its parameters. */
	public void testAcceptsParameterValues()
	{
		com.developmentontheedge.beans.model.ComponentModel model = McpAnalysisSupport.parametersModel( McpTestAnalysis.NAME );
		assertNotNull( model );
		assertTrue( McpAnalysisSupport.accepts( model, "mode", "exact" ) );
		assertFalse( "not one of the choices", McpAnalysisSupport.accepts( model, "mode", "slow" ) );
		assertTrue( McpAnalysisSupport.accepts( model, "rows", "5" ) );
		assertFalse( "not a number", McpAnalysisSupport.accepts( model, "rows", "many" ) );
		assertTrue( "exists", McpAnalysisSupport.accepts( model, "conditions", null ) );
		assertFalse( "no such parameter", McpAnalysisSupport.accepts( model, "nope", null ) );
		assertNull( McpAnalysisSupport.parametersModel( "no such analysis" ) );
	}

	/** Array-of-bean params bind from both the 'name/0/field' key form and a JSON list of objects. */
	public void testArrayOfBeanParams()
	{
		Map<String, Object> p = params( 1 );
		p.put( "output", outRoot + "/conditionsKeys" );
		p.put( "conditions/0/condition", "Experiment: rosacea" );
		p.put( "conditions/1/condition", "Control" );
		McpEnvelope env = McpAnalysisSupport.runAnalysis( McpTestAnalysis.NAME, p, null, true );
		assertTrue( "index-key form should bind, got " + env.getError(), env.isOk() );
		assertConditions( "Experiment: rosacea", "Control" );

		p = params( 1 );
		Map<String, Object> c = new LinkedHashMap<String, Object>();
		c.put( "condition", "Experiment: rosacea" );
		p.put( "output", outRoot + "/conditionsList" );
		p.put( "conditions", java.util.Collections.singletonList( c ) );
		env = McpAnalysisSupport.runAnalysis( McpTestAnalysis.NAME, p, null, true );
		assertTrue( "list form should bind, got " + env.getError(), env.isOk() );
		assertConditions( "Experiment: rosacea" );
	}

	/** A wrongly shaped value names the parameter and the expected shape. */
	public void testWrongShapeNamesParameter()
	{
		Map<String, Object> p = params( 1 );
		p.put( "conditions", "Experiment: rosacea" );
		McpEnvelope env = McpAnalysisSupport.runAnalysis( McpTestAnalysis.NAME, p, null, true );
		assertFalse( "a string for an array must be refused", env.isOk() );
		assertEquals( McpConstants.CODE_INVALID_PARAMS, env.getCode() );
		assertTrue( "error names the parameter: " + env.getError(), env.getError().contains( "'conditions'" ) );
		assertTrue( "error hints the array shape: " + env.getError(), env.getError().contains( "an array" ) );
	}

	private static void assertConditions( String... expected )
	{
		McpTestAnalysis.Condition[] actual = McpTestAnalysis.lastConditions;
		assertNotNull( "conditions bound", actual );
		assertEquals( "condition count", expected.length, actual.length );
		for ( int i = 0; i < expected.length; i++ )
			assertEquals( "condition " + i, expected[ i ], actual[ i ].getCondition() );
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

	/**
	 * A task is its owner's: another user gets not_found from status (and no log) and does not see it in the list,
	 * the owner sees it; each queued task captures its log from a logger of its own, not the shared default one.
	 */
	@SuppressWarnings( "unchecked" )
	public void testTasksAreIsolatedPerUser() throws Exception
	{
		McpEnvelope env = McpAnalysisSupport.runAnalysisAs( McpConstants.TASK_USER, McpTestAnalysis.NAME, params( 1 ), null, false );
		assertTrue( env.isOk() );
		String taskId = (String) ( (Map<String, Object>) env.getData() ).get( "taskId" );

		McpEnvelope foreign = McpProviderSupport.withUserSession( "someone-else", () -> McpAnalysisSupport.taskStatus( taskId ) );
		assertFalse( "another user's task is not readable", foreign.isOk() );
		assertEquals( McpConstants.CODE_NOT_FOUND, foreign.getCode() );
		McpEnvelope foreignList = McpProviderSupport.withUserSession( "someone-else", McpAnalysisSupport::listTasks );
		assertFalse( "another user's task is not listed", listed( foreignList, taskId ) );
		assertTrue( "the owner lists the task", listed( McpAnalysisSupport.listTasks(), taskId ) );

		java.util.logging.Logger shared = java.util.logging.Logger.getLogger( ru.biosoft.analysiscore.AnalysisMethodSupport.class.getName() );
		ru.biosoft.tasks.TaskInfo info = ru.biosoft.tasks.TaskManager.getInstance().getTask( taskId );
		shared.info( "line from another analysis" );
		assertFalse( "a task log does not capture the shared analysis logger",
				String.valueOf( info.getLogInfo() ).contains( "line from another analysis" ) );
	}

	@SuppressWarnings( "unchecked" )
	private static boolean listed( McpEnvelope list, String taskId )
	{
		assertTrue( "list tasks ok", list.isOk() );
		List<Map<String, Object>> tasks = (List<Map<String, Object>>) ( (Map<String, Object>) list.getData() ).get( "tasks" );
		return tasks.stream().anyMatch( t -> taskId.equals( t.get( "taskId" ) ) );
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
		assertTrue( "task registered", names.contains( "biouml_task" ) );
		assertNotNull( "task_status stays callable", catalog.find( "biouml_task_status" ) );
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
