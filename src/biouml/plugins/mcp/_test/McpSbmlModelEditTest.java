package biouml.plugins.mcp._test;

import java.awt.Point;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.VariableRole;
import biouml.model.javascript.JavaScriptModel;
import biouml.plugins.mcp.support.McpDiagramEditSupport;
import biouml.plugins.mcp.support.McpDiagramSupport;
import biouml.plugins.mcp.support.McpEnvelope;
import biouml.plugins.sbgn.SbgnDiagramType;
import biouml.plugins.sbml.converters.SBGNConverterNew;
import biouml.standard.diagram.CreatorElementWithName;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Reaction;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
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
 * Model-editing tests against an SBML-backed SBGN diagram — the storage format of every SBML model
 * on a BioUML server. Saving such a diagram converts it back to SBML (SbmlDiagramTransformer →
 * SBGNConverterNew.restore), so elements without the right roles break the save of the whole diagram.
 *
 * <p>Covers the MCP diagram tools (add_node / add_edge / add_reaction / describe / remove) and the
 * {@code model} JavaScript host object (addSpecies / addReaction / addEquation).</p>
 */
public class McpSbmlModelEditTest extends AbstractBioUMLTest
{
	private File dir;
	private DataCollection<Diagram> folder;
	private String diagramPath;

	@Override
	@SuppressWarnings( "unchecked" )
	protected void setUp() throws Exception
	{
		super.setUp();
		dir = TempFiles.dir( "mcpSbmlEditTest" );
		Properties properties = new ExProperties();
		properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, "mcps" );
		properties.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName() );
		properties.setProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, dir.toString() );
		ExProperties.store( properties, new File( dir, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE ) );
		Repository repository = (Repository) CollectionFactory.createCollection( null, properties );
		CollectionFactory.registerRoot( repository );
		repository.getNameList();

		folder = (DataCollection<Diagram>) (DataCollection<?>) GenericDataCollection.createGenericCollection( repository, repository, "models", "models" );
		repository.put( folder );

		// An SBML model: compartment "default" holding species s1 -> s2, stored as an SBML file.
		Diagram diagram = new SbgnDiagramType().createDiagram( folder, "m1", new DiagramInfo( "m1" ) );
		CreatorElementWithName controller = (CreatorElementWithName) diagram.getType().getSemanticController();
		controller.createInstance( diagram, biouml.standard.type.Compartment.class, "default", new Point( 0, 0 ), null ).putToCompartment();
		Compartment def = (Compartment) diagram.get( "default" );
		def.setShapeSize( new java.awt.Dimension( 400, 300 ) );
		controller.createInstance( def, Specie.class, "s1", new Point( 20, 20 ), null ).putToCompartment();
		controller.createInstance( def, Specie.class, "s2", new Point( 200, 20 ), null ).putToCompartment();
		diagram.getRole( EModel.class ).getVariable( "$default.s1" ).setInitialValue( 10 );
		folder.put( diagram );
		diagramPath = "mcps/models/m1";
	}

	@Override
	protected void tearDown() throws Exception
	{
		super.tearDown();
		ApplicationUtils.removeDir( dir );
	}

	private Diagram diagram()
	{
		return (Diagram) CollectionFactory.getDataElement( diagramPath );
	}

	/** Re-read the diagram from its SBML file, bypassing the in-memory cache. */
	private Diagram reload() throws Exception
	{
		folder.release( "m1" );
		return folder.get( "m1" );
	}

	@SuppressWarnings( "unchecked" )
	private Map<String, Object> data( McpEnvelope env )
	{
		if ( !env.isOk() )
			fail( "expected ok envelope but got error: " + env.getCode() + " " + env.getError() );
		return (Map<String, Object>) env.getData();
	}

	// ------------------------------------------------------------------ MCP tools

	/** The reported bug: add_node(Reaction) left a role-less reaction that broke every later save. */
	public void testAddReactionNodeKeepsDiagramSaveable() throws Exception
	{
		data( McpDiagramSupport.addNode( diagramPath, "default/re_test", "Reaction", 100, 60 ) );

		Node reaction = diagram().findNode( "re_test" );
		assertNotNull( "reaction created inside the compartment", reaction );
		assertEquals( "default", reaction.getCompartment().getName() );
		assertTrue( "reaction has its rate equation", reaction.getRole() instanceof Equation );
		assertNotNull( "rate variable declared", diagram().getRole( EModel.class ).getVariable( "$$rate_re_test" ) );

		// Later edits still save.
		data( McpDiagramSupport.addNode( diagramPath, "default/s3", "Substance", 300, 20 ) );
		assertNotNull( "survives a reload from SBML", reload().findNode( "re_test" ) );
	}

	/** A "compartment/name" id places the node inside the compartment, as a model variable. */
	public void testAddSpeciesNodeInsideCompartment() throws Exception
	{
		Map<String, Object> m = data( McpDiagramSupport.addNode( diagramPath, "default/V", "Substance", 300, 100 ) );
		assertEquals( "V", m.get( "id" ) );
		assertEquals( "$default.V", m.get( "variable" ) );
		assertNull( "no top-level node named 'default/V'", diagram().get( "default/V" ) );
		Node v = diagram().findNode( "default.V" );
		assertNotNull( v );
		assertTrue( v.getRole() instanceof VariableRole );

		Diagram reloaded = reload();
		assertNotNull( "species persisted in SBML", reloaded.getRole( EModel.class ).getVariable( "$default.V" ) );
	}

	/** add_node(Reaction) + add_edge(species -> reaction) builds a real reactant reference. */
	public void testAddEdgeLinksSpeciesToReaction() throws Exception
	{
		data( McpDiagramSupport.addNode( diagramPath, "default/r1", "Reaction", 100, 60 ) );
		data( McpDiagramSupport.addEdge( diagramPath, "s1", "r1", null, null ) );
		data( McpDiagramSupport.addEdge( diagramPath, "r1", "s2", null, null ) );

		Node r1 = diagram().findNode( "r1" );
		Reaction reaction = (Reaction) r1.getKernel();
		assertEquals( 2, reaction.getSize() );
		for ( Edge e : r1.getEdges() )
			assertFalse( "source/sink glyph replaced by " + e.getName(), biouml.standard.diagram.Util.isSourceSink( e.getOtherEnd( r1 ) ) );
		assertEquals( "not_found", McpDiagramSupport.addEdge( diagramPath, "s1", "nope", null, null ).getCode() );
		for ( Edge e : r1.getEdges() )
			if ( e.getKernel() instanceof SpecieReference && ( (SpecieReference) e.getKernel() ).isReactantOrProduct() )
				assertTrue( "rate equation on " + e.getName(), e.getRole() instanceof Equation );

		Reaction reloaded = (Reaction) reload().findNode( "r1" ).getKernel();
		assertEquals( "participants persisted in SBML", 2, reloaded.getSize() );
	}

	/** biouml_diagram_add_reaction creates the whole reaction in one call. */
	@SuppressWarnings( "unchecked" )
	public void testAddReactionTool() throws Exception
	{
		Map<String, String> parameters = new java.util.HashMap<String, String>();
		parameters.put( "k1", "0.5" );
		Map<String, Object> m = data( McpDiagramSupport.addReaction( diagramPath, "conv", "k1*$default.s1*kx",
				new String[] { "s1" }, new String[] { "2*s2" }, null, null, parameters, null, null ) );
		assertEquals( "conv", m.get( "id" ) );
		assertEquals( "$$rate_conv", m.get( "rateVariable" ) );
		assertEquals( java.util.Arrays.asList( "k1" ), m.get( "parametersSet" ) );
		List<String> declared = (List<String>) m.get( "declaredParameters" );
		assertTrue( "undeclared kx reported, got " + declared, declared != null && declared.contains( "kx" ) );

		Diagram d = reload();
		Reaction reaction = (Reaction) d.findNode( "conv" ).getKernel();
		assertEquals( "k1*$default.s1*kx", reaction.getKineticLaw().getFormula() );
		assertEquals( 2, reaction.getSize() );
		SpecieReference product = reaction.stream().filter( SpecieReference::isProduct ).findFirst().get();
		assertEquals( "2", product.getStoichiometry() );
		assertEquals( 0.5, d.getRole( EModel.class ).getVariable( "k1" ).getInitialValue() );
		assertNotNull( d.getRole( EModel.class ).getVariable( "$$rate_conv" ) );
	}

	/** biouml_diagram_add_equation and biouml_diagram_set_parameters. */
	public void testAddEquationAndSetParameters() throws Exception
	{
		Map<String, Object> m = data( McpDiagramSupport.addEquation( diagramPath, null, "Total", "$default.s1 + $default.s2", null, null, null ) );
		assertEquals( "scalar", m.get( "type" ) );
		assertEquals( "Total", m.get( "variable" ) );

		Map<String, String> values = new java.util.HashMap<String, String>();
		values.put( "s2", "3" );
		values.put( "kdeg", "0.1" );
		data( McpDiagramSupport.setParameters( diagramPath, values, Boolean.TRUE ) );

		EModel em = reload().getRole( EModel.class );
		assertEquals( 3.0, em.getVariable( "$default.s2" ).getInitialValue() );
		assertEquals( 0.1, em.getVariable( "kdeg" ).getInitialValue() );
		assertTrue( em.getVariable( "kdeg" ).isConstant() );
		assertNotNull( "assignment target declared", em.getVariable( "Total" ) );
		assertFalse( McpDiagramSupport.addEquation( diagramPath, "rate", "$default.nope", "1", null, null, null ).isOk() );
	}

	/** Unknown participants are rejected up front, leaving the diagram untouched. */
	public void testAddReactionUnknownSpecies()
	{
		int before = diagram().recursiveStream().toList().size();
		McpEnvelope env = McpDiagramSupport.addReaction( diagramPath, "bad", "1", new String[] { "nope" }, null, null, null, null, null, null );
		assertFalse( env.isOk() );
		assertTrue( env.getError(), env.getError().contains( "nope" ) );
		assertEquals( before, diagram().recursiveStream().toList().size() );
	}

	/** describe lists nodes nested in compartments; remove finds them by name. */
	@SuppressWarnings( "unchecked" )
	public void testDescribeAndRemoveNestedElements() throws Exception
	{
		data( McpDiagramSupport.addReaction( diagramPath, "r2", "1", new String[] { "s1" }, new String[] { "s2" }, null, null, null, null, null ) );
		List<Map<String, Object>> elements = (List<Map<String, Object>>) data( McpDiagramSupport.describe( diagramPath ) ).get( "elements" );
		boolean seen = false;
		for ( Map<String, Object> e : elements )
			if ( "r2".equals( e.get( "id" ) ) )
			{
				seen = true;
				assertEquals( "default.r2", e.get( "path" ) );
				assertEquals( "1", e.get( "formula" ) );
			}
		assertTrue( "nested reaction listed", seen );

		data( McpDiagramSupport.removeElement( diagramPath, "r2" ) );
		assertNull( diagram().findNode( "r2" ) );
		assertNull( reload().findNode( "r2" ) );
	}

	/** A diagram that already holds a role-less reaction (written by the old add_node) still saves. */
	public void testConverterToleratesReactionWithoutRole() throws Exception
	{
		Diagram d = diagram();
		Compartment def = (Compartment) d.get( "default" );
		Node bare = new Node( def, new Reaction( null, "bare" ) );
		def.put( bare );
		assertNull( bare.getRole() );
		Diagram sbml = SBGNConverterNew.restore( d );
		assertNotNull( sbml.findNode( "bare" ) );

		folder.put( d );
		assertNotNull( "saved as SBML", reload().findNode( "bare" ) );
	}

	/**
	 * The point of it all: reactions built with the MCP tool and the script API give the species their
	 * ODE terms (s1 -> 2 s2 -> V), and those survive the round trip through SBML.
	 */
	public void testEditedModelHasRateEquations() throws Exception
	{
		Map<String, String> k1 = new java.util.HashMap<String, String>();
		k1.put( "k1", "0.5" );
		data( McpDiagramSupport.addReaction( diagramPath, "conv", "k1*$default.s1", new String[] { "s1" }, new String[] { "2*s2" }, null,
				null, k1, null, null ) );

		Diagram d = reload();
		JavaScriptModel model = new JavaScriptModel();
		model.addSpecies( d, "default/V", 0 );
		d.getRole( EModel.class ).declareVariable( "k2", 0.1 );
		model.addReaction( d, "transfer", "k2*$default.s2",
				new SpecieReference[] { model.createSpecieReference( d, "s2", "reactant" ), model.createSpecieReference( d, "V", "product" ) } );
		d.save();

		Map<String, List<String>> odeTerms = new java.util.TreeMap<String, List<String>>();
		Map<String, String> rates = new java.util.TreeMap<String, String>();
		Diagram reloaded = reload();
		for ( DiagramElement de : reloaded.recursiveStream().toList() )
		{
			if ( ! ( de.getRole() instanceof Equation ) )
				continue;
			Equation eq = (Equation) de.getRole();
			if ( Equation.TYPE_RATE.equals( eq.getType() ) )
				odeTerms.computeIfAbsent( eq.getVariable(), v -> new java.util.ArrayList<String>() ).add( eq.getFormula() );
			else if ( eq.getVariable().startsWith( "$$rate_" ) )
				rates.put( eq.getVariable(), eq.getFormula() );
		}
		assertEquals( "{$default.V=[$$rate_transfer], $default.s1=[-$$rate_conv], $default.s2=[$$rate_conv*(2), -$$rate_transfer]}",
				sorted( odeTerms ).toString() );
		assertEquals( "{$$rate_conv=k1*$default.s1, $$rate_transfer=k2*$default.s2}", rates.toString() );
		EModel em = reloaded.getRole( EModel.class );
		for ( String v : new String[] { "$$rate_conv", "$$rate_transfer", "$default.V" } )
			assertNotNull( v + " declared", em.getVariable( v ) );
		assertEquals( 0.5, em.getVariable( "k1" ).getInitialValue() );
		assertEquals( 0.1, em.getVariable( "k2" ).getInitialValue() );
	}

	private static Map<String, List<String>> sorted( Map<String, List<String>> map )
	{
		for ( List<String> list : map.values() )
			java.util.Collections.sort( list );
		return map;
	}

	/** No edge of a node may point elsewhere: a stale edge breaks the next clone (getOtherEnd throws). */
	private static void assertEdgesConnected( Node node )
	{
		for ( Edge e : node.getEdges() )
			assertTrue( node.getName() + " still lists edge " + e.getName() + " of " + e.getInput().getName() + " -> " + e.getOutput().getName(),
					e.getInput() == node || e.getOutput() == node );
	}

	/** Cloning the same species twice (and merging a clone back) keeps the edge lists consistent. */
	public void testCloneSameNodeTwice() throws Exception
	{
		data( McpDiagramSupport.addReaction( diagramPath, "r1", "1", new String[] { "s1" }, new String[] { "s2" }, null, null, null, null, null ) );
		data( McpDiagramSupport.addReaction( diagramPath, "r2", "1", new String[] { "s1" }, new String[] { "s2" }, null, null, null, null, null ) );

		data( McpDiagramEditSupport.cloneNode( diagramPath, "s1", "s1_a", false, new String[] { "r1" } ) );
		Node s1 = diagram().findNode( "s1" );
		assertEdgesConnected( s1 );
		assertEquals( "only r2 left on the original", 1, s1.getEdges().length );
		assertEdgesConnected( diagram().findNode( "s1_a" ) );

		data( McpDiagramEditSupport.cloneNode( diagramPath, "s1", "s1_b", false, new String[] { "r2" } ) );
		assertEquals( 0, s1.getEdges().length );
		assertEdgesConnected( diagram().findNode( "s1_b" ) );

		data( McpDiagramEditSupport.mergeClone( diagramPath, "s1_a" ) );
		assertNull( diagram().findNode( "s1_a" ) );
		assertEdgesConnected( s1 );
		assertEquals( "r1 back on the original", 1, s1.getEdges().length );

		folder.put( diagram() );
		Diagram reloaded = reload();
		for ( String r : new String[] { "r1", "r2" } )
			assertEquals( r + " keeps its participants", 2, ( (Reaction) reloaded.findNode( r ).getKernel() ).getSize() );
	}

	// ------------------------------------------------------------------ model host object

	public void testScriptModelAddSpeciesReactionEquation() throws Exception
	{
		Diagram d = diagram();
		JavaScriptModel model = new JavaScriptModel();

		VariableRole v = model.addSpecies( d, "default/V", 5 );
		assertNotNull( "addSpecies returns the variable", v );
		assertEquals( "$default.V", v.getName() );
		assertEquals( 5.0, v.getInitialValue() );

		SpecieReference[] refs = new SpecieReference[] { model.createSpecieReference( d, "s1", "reactant" ),
				model.createSpecieReference( d, "V", "product", 2 ) };
		Reaction r = model.addReaction( d, "k_v*$default.s1", refs );
		assertNotNull( "addReaction returns the reaction", r );
		assertEquals( "k_v*$default.s1", r.getKineticLaw().getFormula() );
		assertEquals( 2, r.getSize() );

		Equation eq = model.addEquation( d, "scalar", "Total", "$default.s1 + $default.V" );
		assertNotNull( "addEquation returns the equation", eq );
		assertEquals( "Total", eq.getVariable() );
		assertEquals( "$default.s1 + $default.V", eq.getFormula() );
		assertNotNull( "assignment target declared", d.getRole( EModel.class ).getVariable( "Total" ) );

		d.save();
		Diagram reloaded = reload();
		assertNotNull( reloaded.findNode( "V" ) );
		Reaction rr = (Reaction) reloaded.findNode( r.getName() ).getKernel();
		assertEquals( 2, rr.getSize() );
		boolean found = false;
		for ( DiagramElement de : reloaded.recursiveStream().toList() )
			if ( de.getRole() instanceof Equation && "Total".equals( ( (Equation) de.getRole() ).getVariable() ) )
				found = true;
		assertTrue( "assignment rule persisted", found );
	}

	public void testScriptModelReportsBadInput()
	{
		Diagram d = diagram();
		JavaScriptModel model = new JavaScriptModel();
		try
		{
			model.createSpecieReference( d, "missing", "reactant" );
			fail( "unknown species must be reported" );
		}
		catch ( IllegalArgumentException expected )
		{
			assertTrue( expected.getMessage(), expected.getMessage().contains( "missing" ) );
		}
		try
		{
			model.addEquation( d, "rate", "$default.unknown", "1" );
			fail( "unknown species variable must be reported" );
		}
		catch ( IllegalArgumentException expected )
		{
		}
	}
}
