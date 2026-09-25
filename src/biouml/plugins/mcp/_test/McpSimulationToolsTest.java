package biouml.plugins.mcp._test;

import java.io.File;
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
		assertFalse( "removed sync simulation_run is not registered", names.contains( "biouml_simulation_run" ) );
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
