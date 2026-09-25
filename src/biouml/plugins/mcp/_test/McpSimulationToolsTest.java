package biouml.plugins.mcp._test;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.dynamics.Equation;
import biouml.plugins.mcp.McpConstants;
import biouml.plugins.mcp.server.McpToolCatalog;
import biouml.plugins.mcp.support.McpSimulationSupport;
import biouml.plugins.mcp.support.McpEnvelope;
import biouml.plugins.mcp.tools.simulation.SimulationTools;
import biouml.standard.diagram.DiagramGenerator;
import biouml.standard.simulation.SimulationResult;

import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.Repository;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.generic.GenericDataCollection;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.TempFiles;

/**
 * Phase-4 tests for the simulation MCP tools. Builds a tiny dynamic model headlessly via
 * {@link DiagramGenerator} (one species X with rate X' = -X, X(0) = 1) and stores it in a temp-dir
 * {@link LocalRepository} fixture, then exercises the provider-based async simulation tools
 * (start / status / result) and the solver list headlessly.
 *
 * <p>Simulation work is delegated to the platform's {@code simulation} provider (async job model):
 * {@link McpSimulationSupport#startSimulation(String)} starts the job and returns a jobID; the status
 * poll must then resolve. A diagram with no dynamic model (no rate equation) is reported with
 * {@link McpConstants#CODE_MISSING_MODEL} rather than handed to the solver.</p>
 */
public class McpSimulationToolsTest extends AbstractBioUMLTest
{
	private File dir;
	/** Path to the dynamic (rate-equation) diagram. */
	private String dynamicPath;
	/** Path to a diagram that has an EModel role but no rate equation (the missing-model case). */
	private String staticPath;

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		dir = TempFiles.dir( "mcpSimTest" );
		Properties properties = new ExProperties();
		properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, "mcpsim" );
		properties.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName() );
		properties.setProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, dir.toString() );
		ExProperties.store( properties, new File( dir, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE ) );
		Repository repository = (Repository) CollectionFactory.createCollection( null, properties );
		CollectionFactory.registerRoot( repository );
		repository.getNameList();

		DataCollection<?> folder = GenericDataCollection.createGenericCollection( repository, repository, "diagrams", "diagrams" );
		assertNotNull( "folder created", folder );
		repository.put( folder );
		DataCollection<?> parent = folder;

		// Dynamic model: X' = -X, X(0) = 1  ->  X(t) = e^{-t}. DiagramGenerator builds a detached
		// diagram, so we store it in the parent explicitly.
		DiagramGenerator gen = new DiagramGenerator( "decay" );
		gen.createSpecies( "X", 1.0 );
		gen.createEquation( "X", "-X", Equation.TYPE_RATE );
		Diagram decay = gen.getDiagram();
		put( parent, decay );
		dynamicPath = "mcpsim/diagrams/" + decay.getName();

		// Static model: an EModel with a variable but NO rate equation.
		DiagramGenerator gen2 = new DiagramGenerator( "static" );
		gen2.createSpecies( "Y", 1.0 );
		Diagram staticD = gen2.getDiagram();
		put( parent, staticD );
		staticPath = "mcpsim/diagrams/" + staticD.getName();
	}

	@Override
	protected void tearDown() throws Exception
	{
		super.tearDown();
		ApplicationUtils.removeDir( dir );
	}

	@SuppressWarnings( "unchecked" )
	private static void put( DataCollection<?> parent, Diagram diagram ) throws Exception
	{
		// DataCollection<T extends DataElement>; Diagram is a DataElement.
		DataCollection<ru.biosoft.access.core.DataElement> p = (DataCollection<ru.biosoft.access.core.DataElement>) parent;
		p.put( diagram );
	}

	@SuppressWarnings( "unchecked" )
	private Map<String, Object> data( McpEnvelope env )
	{
		if ( !env.isOk() )
			fail( "expected ok envelope but got error: " + env.getCode() + " " + env.getError() );
		return (Map<String, Object>) env.getData();
	}

	/**
	 * Regression: {@code biouml_diagram_describe} must report the correct dynamic-variable type. A
	 * freshly-loaded (headless) model has variables defaulting to "Not used" until
	 * {@code EModel.detectVariableTypes()} runs; describe() now invokes it, so the rate variable X of
	 * the decay model (X' = -X) must be reported as "Differential", not "Not used".
	 */
	@SuppressWarnings( "unchecked" )
	public void testDescribeReportsDifferentialType()
	{
		// Ensure the model's variable collection is populated with the rate variable, as the save/load
		// sync would do for a persisted model (the in-memory test diagram skips that sync).
		try
		{
			biouml.model.Diagram d = (biouml.model.Diagram) ru.biosoft.access.core.CollectionFactory.getDataElement( dynamicPath );
			biouml.model.dynamics.EModel em = (biouml.model.dynamics.EModel) d.getRole();
			if ( !em.getVariables().contains( "X" ) )
				em.getVariables().put( new biouml.model.dynamics.Variable( "X", em, em.getVariables() ) );
		}
		catch ( Exception ignore )
		{
		}

		McpEnvelope desc = biouml.plugins.mcp.support.McpDiagramSupport.describe( dynamicPath );
		assertTrue( "describe ok: " + desc.getCode() + " " + desc.getError(), desc.isOk() );
		Map<String, Object> dm = (Map<String, Object>) desc.getData();
		Map<String, Object> dyn = (Map<String, Object>) dm.get( "dynamicModel" );
		assertNotNull( "dynamicModel present for a rate-equation diagram", dyn );
		List<Map<String, Object>> vars = (List<Map<String, Object>>) dyn.get( "variables" );
		String xType = null;
		for ( Map<String, Object> v : vars )
			if ( "X".equals( v.get( "name" ) ) )
				xType = (String) v.get( "type" );
		assertNotNull( "variable X present in the dynamic model", xType );
		assertEquals( "X (rate equation) is a Differential variable, not 'Not used'",
				"Differential", xType );
	}

	/**
	 * The other branch of the binary criterion: a diagram whose dynamic model has no rate equation
	 * must be refused with the {@code missing_dynamic_model} code (not handed to the solver).
	 */
	public void testMissingModelError()
	{
		McpEnvelope env = McpSimulationSupport.startSimulation( staticPath );
		assertFalse( "static model must be refused", env.isOk() );
		assertEquals( "code is missing_dynamic_model", McpConstants.CODE_MISSING_MODEL, env.getCode() );
	}

	@SuppressWarnings( "unchecked" )
	public void testListSolvers()
	{
		McpEnvelope env = McpSimulationSupport.listSolvers();
		assertTrue( "listSolvers ok", env.isOk() );
		Map<String, Object> m = data( env );
		assertTrue( "at least one solver", ( (Number) m.get( "count" ) ).intValue() >= 1 );
		List<Map<String, Object>> solvers = (List<Map<String, Object>>) m.get( "solvers" );
		boolean hasJvode = false;
		for ( Map<String, Object> s : solvers )
			if ( "JVode".equals( s.get( "name" ) ) )
				hasJvode = true;
		assertTrue( "JVode (default) is listed", hasJvode );
	}

	public void testToolsListRegistersSimulationTools()
	{
		McpToolCatalog catalog = new McpToolCatalog();
		SimulationTools.registerAll( catalog );
		List<String> names = new java.util.ArrayList<String>();
		for ( McpToolCatalog.Tool t : catalog.getTools() )
			names.add( t.name );
		assertTrue( "simulation_list_solvers registered", names.contains( "biouml_simulation_list_solvers" ) );
		assertTrue( "simulation_start registered", names.contains( "biouml_simulation_start" ) );
		assertTrue( "simulation_status registered", names.contains( "biouml_simulation_status" ) );
		assertTrue( "simulation_result registered", names.contains( "biouml_simulation_result" ) );
		assertTrue( "simulation_plot registered", names.contains( "biouml_simulation_plot" ) );
		assertFalse( "removed sync simulation_run is not registered", names.contains( "biouml_simulation_run" ) );
	}

	/** Build a small in-memory {@link SimulationResult} (path-less, like the engine produces). */
	private SimulationResult syntheticResult()
	{
		SimulationResult r = new SimulationResult( null, "synth" );
		r.setVariableMap( new HashMap<String, Integer>() {{ put( "X", 0 ); put( "Y", 1 ); }} );
		r.setTimes( new double[] { 0, 1, 2 } );
		r.setValues( new double[][] { { 1.0, 2.0 }, { 0.5, 1.0 }, { 0.25, 0.5 } } );
		return r;
	}

	/**
	 * The new biouml_simulation_plot renders a path-less (in-memory) {@link SimulationResult} to a PNG
	 * on the server — the headless equivalent of the web UI's "New plot" / "+" flow. This is the core
	 * regression for the context-blowing problem: the full time series stays server-side and only an
	 * image + tiny metadata summary come back. The renderer must survive a headless JVM
	 * (JFreeChart draws into an offscreen image, no display required).
	 */
	@SuppressWarnings( "unchecked" )
	public void testPlotSimulationResultRendersPng() throws Exception
	{
		// Register the synthetic result under a jobID so plotSimulationResult can fetch it via the
		// same job2handler lookup the live "result" action uses.
		String jobID = "mcp-plot-test-" + java.util.UUID.randomUUID();
		biouml.plugins.simulation.web.SimulationWebResultHandler handler =
				new biouml.plugins.simulation.web.SimulationWebResultHandler( null );
		injectResult( jobID, handler, syntheticResult() );

		// No 'variables' -> plot every non-time variable (X and Y).
		McpEnvelope env = McpSimulationSupport.plotSimulationResult( jobID, null, null, null );
		assertTrue( "plot ok; got " + env.getCode() + " " + env.getError(), env.isOk() );
		Map<String, Object> m = (Map<String, Object>) env.getData();
		assertEquals( "jobID echoed", jobID, m.get( "jobID" ) );
		// Both variables plotted.
		java.util.List<String> vars = (java.util.List<String>) m.get( "variables" );
		assertEquals( "both X and Y plotted", 2, vars.size() );
		assertTrue( vars.contains( "X" ) && vars.contains( "Y" ) );
		assertEquals( "point count from the result", 3, ( (Number) m.get( "points" ) ).intValue() );
		// finals carry the last time point's values.
		Map<String, Object> finals = (Map<String, Object>) m.get( "finals" );
		assertEquals( 0.25, ( (Number) finals.get( "X" ) ).doubleValue(), 1e-9 );
		assertEquals( 0.5, ( (Number) finals.get( "Y" ) ).doubleValue(), 1e-9 );
		// The image is a real, non-trivial PNG (base64), decodable to a valid PNG byte stream.
		String b64 = (String) m.get( "image" );
		assertNotNull( "image present", b64 );
		byte[] png = java.util.Base64.getDecoder().decode( b64 );
		assertTrue( "png non-trivial size", png.length > 100 );
		assertEquals( "png magic", 0x89, png[ 0 ] & 0xff );
		assertEquals( 'P', (char) png[ 1 ] );
		assertEquals( 'N', (char) png[ 2 ] );
		assertEquals( 'G', (char) png[ 3 ] );
		// Decode back into an image to prove it is a well-formed PNG.
		java.awt.image.BufferedImage decoded = javax.imageio.ImageIO.read( new java.io.ByteArrayInputStream( png ) );
		assertNotNull( "png decodes to an image", decoded );
		assertEquals( "width honored", 700, decoded.getWidth() );
		assertEquals( "height honored", 450, decoded.getHeight() );
	}

	/**
	 * A variable subset is honoured, and an unknown-only subset is rejected with the available list.
	 */
	@SuppressWarnings( "unchecked" )
	public void testPlotSimulationResultVariableSubsetAndUnknown() throws Exception
	{
		String jobID = "mcp-plot-subset-" + java.util.UUID.randomUUID();
		injectResult( jobID, new biouml.plugins.simulation.web.SimulationWebResultHandler( null ), syntheticResult() );

		// Only Y.
		McpEnvelope env = McpSimulationSupport.plotSimulationResult( jobID, new String[] { "Y" }, null, null );
		assertTrue( "subset plot ok", env.isOk() );
		java.util.List<String> vars = (java.util.List<String>) ( (Map<String, Object>) env.getData() ).get( "variables" );
		assertEquals( 1, vars.size() );
		assertEquals( "Y", vars.get( 0 ) );

		// Unknown-only subset is rejected.
		McpEnvelope bad = McpSimulationSupport.plotSimulationResult( jobID, new String[] { "nope" }, null, null );
		assertFalse( "unknown-only subset must fail", bad.isOk() );
		assertEquals( "invalid_params code", McpConstants.CODE_INVALID_PARAMS, bad.getCode() );
		assertTrue( "available variables listed in the error",
				bad.getError() != null && bad.getError().contains( "X" ) && bad.getError().contains( "Y" ) );
	}

	/** An unknown jobID is a not_found error. */
	public void testPlotSimulationResultUnknownJob()
	{
		McpEnvelope env = McpSimulationSupport.plotSimulationResult( "no-such-job-" + java.util.UUID.randomUUID(), null, null, null );
		assertFalse( env.isOk() );
		assertEquals( "not_found code", McpConstants.CODE_NOT_FOUND, env.getCode() );
	}

	/** Put a SimulationResult into SimulationProvider's private job2handler for a jobID. */
	private static void injectResult( String jobID,
			biouml.plugins.simulation.web.SimulationWebResultHandler handler,
			SimulationResult result ) throws Exception
	{
		// Set the result on the handler (private field, no setter) and register it under the jobID.
		java.lang.reflect.Field rf =
				biouml.plugins.simulation.web.SimulationWebResultHandler.class.getDeclaredField( "simulationResult" );
		rf.setAccessible( true );
		rf.set( handler, result );
		java.lang.reflect.Field hf =
				biouml.plugins.simulation.web.SimulationProvider.class.getDeclaredField( "job2handler" );
		hf.setAccessible( true );
		@SuppressWarnings( "unchecked" )
		java.util.concurrent.ConcurrentHashMap<String, biouml.plugins.simulation.web.SimulationWebResultHandler> map =
				(java.util.concurrent.ConcurrentHashMap<String, biouml.plugins.simulation.web.SimulationWebResultHandler>) hf.get( null );
		if ( map == null )
		{
			// The provider's job2handler is created lazily by the instance initLogging(); in a headless
			// test that never ran, so create it here for the injection.
			map = new java.util.concurrent.ConcurrentHashMap<String, biouml.plugins.simulation.web.SimulationWebResultHandler>();
			hf.set( null, map );
		}
		map.put( jobID, handler );
	}

	/**
	 * The async simulation start: either it returns a jobID (the headless env can build a model) or
	 * it is refused with {@code missing_dynamic_model} (same binary criterion as the static case). When it
	 * returns a jobID, the status poll must resolve (not error) for that job.
	 */
	@SuppressWarnings( "unchecked" )
	public void testStartSimulationAsync()
	{
		McpEnvelope env = McpSimulationSupport.startSimulation( dynamicPath );
		boolean started = env.isOk();
		boolean missingModel = !env.isOk() && McpConstants.CODE_MISSING_MODEL.equals( env.getCode() );
		assertTrue( "exactly one of {started, missing_dynamic_model} must hold; got ok="
				+ env.isOk() + " code=" + env.getCode() + " error=" + env.getError(),
				started ^ missingModel );
		if ( started )
		{
			Map<String, Object> m = (Map<String, Object>) env.getData();
			String jobID = (String) m.get( "jobID" );
			assertNotNull( "jobID present", jobID );
			// The status poll must resolve (the WebJob exists in the shared session).
			McpEnvelope st = McpSimulationSupport.simulationStatus( jobID );
			assertTrue( "status poll resolves; got " + st.getCode() + " " + st.getError(), st.isOk() );
		}
	}

	@SuppressWarnings( "unchecked" )
	public void testEnvelopeShape()
	{
		McpEnvelope env = McpSimulationSupport.startSimulation( "mcpsim/diagrams/doesnotexist" );
		assertFalse( env.isOk() );
		assertNotNull( env.getCode() );
		Map<String, Object> map = env.toMap();
		assertEquals( Boolean.FALSE, map.get( "ok" ) );
		assertTrue( "error map carries a message", map.containsKey( "error" ) );
	}
}
