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
import biouml.plugins.mcp.McpConstants;
import biouml.plugins.mcp.server.McpToolCatalog;
import biouml.plugins.mcp.support.McpDiagramEditSupport;
import biouml.plugins.mcp.support.McpDiagramSupport;
import biouml.plugins.mcp.support.McpEnvelope;
import biouml.plugins.mcp.tools.diagram.DiagramTools;
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
		( (Node) def.get( "s1" ) ).setLocation( 30, 60 ); // the SBGN controller ignores the point
		( (Node) def.get( "s2" ) ).setLocation( 260, 60 );
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

		Map<String, Object> cloned = data( McpDiagramEditSupport.cloneNode( diagramPath, "s1", "s1_a", false, new String[] { "r1" } ) );
		assertEquals( Boolean.TRUE, cloned.get( "saved" ) );
		assertNotNull( "clone saved", reload().findNode( "s1_a" ) );
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

		Diagram reloaded = reload();
		assertNull( "merge saved", reloaded.findNode( "s1_a" ) );
		assertNotNull( reloaded.findNode( "s1_b" ) );
		for ( String r : new String[] { "r1", "r2" } )
			assertEquals( r + " keeps its participants", 2, ( (Reaction) reloaded.findNode( r ).getKernel() ).getSize() );
	}

	/**
	 * Edit tools save the diagram; one that can not save it discards the in-memory change, so the next
	 * access re-reads the saved version and a broken element can not block later saves.
	 */
	public void testUnsaveableEditIsDiscarded() throws Exception
	{
		Map<String, Object> m = data( McpDiagramEditSupport.layout( diagramPath, "align-up", new String[] { "s1", "s2" } ) );
		assertEquals( Boolean.TRUE, m.get( "saved" ) );

		// the action fails (the view builder trips over the broken element)
		Diagram before = breakDiagram();
		McpEnvelope env = McpDiagramEditSupport.layout( diagramPath, "align-left", new String[] { "s1", "s2" } );
		assertFalse( "layout must fail", env.isOk() );
		assertTrue( env.getError(), env.getError().contains( "re-read from its last saved version" ) );
		assertNotSame( "cached diagram dropped", before, diagram() );
		assertNull( "broken element discarded", diagram().findNode( "broken" ) );

		// the action succeeds but the SBML writer fails on the broken element
		before = breakDiagram();
		env = McpDiagramEditSupport.cloneNode( diagramPath, "s1", "s1_c", false, null );
		assertFalse( "save must fail", env.isOk() );
		assertTrue( env.getError(), env.getError().contains( "could not be saved" ) );
		assertNotSame( "cached diagram dropped", before, diagram() );
		assertNull( "clone discarded", diagram().findNode( "s1_c" ) );
		assertNull( "broken element discarded", diagram().findNode( "broken" ) );

		data( McpDiagramSupport.addNode( diagramPath, "default/s3", "Species", 10, 10 ) ); // later edits save again
	}

	/** Put an element with an untyped kernel into the cached diagram: nothing can render or save it. */
	private Diagram breakDiagram() throws Exception
	{
		Diagram d = diagram();
		Compartment def = (Compartment) d.get( "default" );
		def.put( new Node( def, new biouml.standard.type.Stub( null, "broken", null ) ) );
		return d;
	}

	/** add_node creates SBGN entity pools of a given type, with a title and a drawable size. */
	public void testAddNodeEntityTypeTitleSize() throws Exception
	{
		Map<String, Object> m = data( McpDiagramSupport.addNode( diagramPath, "default/U", "unspecified", 50, 150, "Unvaccinated", null, null ) );
		assertEquals( "$default.U", m.get( "variable" ) );
		data( McpDiagramSupport.addNode( diagramPath, "default/G", "simple chemical", 150, 150, null, 60, 30 ) );
		data( McpDiagramSupport.addNode( diagramPath, "default/M", "Species", 250, 150 ) );

		Diagram d = reload();
		Node u = d.findNode( "U" );
		assertEquals( "unspecified", u.getKernel().getType() );
		assertEquals( "Unvaccinated", u.getTitle() );
		assertEquals( new java.awt.Dimension( 70, 40 ), u.getShapeSize() );
		assertTrue( u.getRole() instanceof VariableRole );
		Node g = d.findNode( "G" );
		assertEquals( "simple chemical", g.getKernel().getType() );
		assertEquals( new java.awt.Dimension( 60, 30 ), g.getShapeSize() );
		assertEquals( "macromolecule", d.findNode( "M" ).getKernel().getType() );
		assertEquals( new java.awt.Dimension( 70, 40 ), d.findNode( "M" ).getShapeSize() );
	}

	/** Removing a reaction also removes its $$rate_ variable. */
	@SuppressWarnings( "unchecked" )
	public void testRemoveReactionRemovesRateVariable() throws Exception
	{
		data( McpDiagramSupport.addReaction( diagramPath, "r1", "1", new String[] { "s1" }, new String[] { "s2" }, null, null, null, null, null ) );
		assertNotNull( diagram().getRole( EModel.class ).getVariable( "$$rate_r1" ) );
		Map<String, Object> m = data( McpDiagramSupport.removeElement( diagramPath, "r1" ) );
		assertEquals( java.util.Arrays.asList( "$$rate_r1" ), m.get( "removedVariables" ) );
		assertNull( diagram().getRole( EModel.class ).getVariable( "$$rate_r1" ) );
		assertNull( reload().getRole( EModel.class ).getVariable( "$$rate_r1" ) );
	}

	/** Events and equations can be given their node names (and positions) when they are created. */
	@SuppressWarnings( "unchecked" )
	public void testAddNamedEventAndEquation() throws Exception
	{
		Map<String, String> assignments = new java.util.LinkedHashMap<String, String>();
		assignments.put( "vac_on", "1" );
		assignments.put( "s2", "0" );
		Map<String, String> parameters = new java.util.HashMap<String, String>();
		parameters.put( "t0", "30" );
		parameters.put( "vac_on", "0" );
		Map<String, Object> m = data( McpDiagramSupport.addEvent( diagramPath, "vaccination_start", "time >= t0", null, assignments, parameters, 40, 400 ) );
		assertEquals( "vaccination_start", m.get( "id" ) );
		assertEquals( "{vac_on=1, $default.s2=0}", m.get( "assignments" ).toString() );
		data( McpDiagramSupport.addEquation( diagramPath, "population_rule", null, "Total", "$default.s1 + $default.s2", null, 40, 500 ) );
		assertFalse( "unknown assignment variable", McpDiagramSupport.addEvent( diagramPath, null, "time > 1", null,
				java.util.Collections.singletonMap( "nope", "1" ), null, null, null ).isOk() );

		Diagram d = reload();
		Node event = d.findNode( "vaccination_start" );
		assertNotNull( "event saved under its name", event );
		assertEquals( "label follows the name", "vaccination_start", event.getTitle() );
		assertEquals( "population_rule", d.findNode( "population_rule" ).getTitle() );
		assertEquals( new java.awt.Point( 40, 400 ), event.getLocation() );
		biouml.model.dynamics.Event e = event.getRole( biouml.model.dynamics.Event.class );
		assertTrue( e.getTrigger(), e.getTrigger().matches( "time >= t0" ) );
		assertEquals( 2, e.getEventAssignment().length );
		assertEquals( "order kept through SBML", "vac_on", e.getEventAssignment()[ 0 ].getVariable() );
		assertEquals( "$default.s2", e.getEventAssignment()[ 1 ].getVariable() );
		assertEquals( 30.0, d.getRole( EModel.class ).getVariable( "t0" ).getInitialValue() );
		assertEquals( "Total", d.findNode( "population_rule" ).getRole( Equation.class ).getVariable() );

		JavaScriptModel model = new JavaScriptModel();
		biouml.model.dynamics.Event named = model.addEvent( d, "stop", "time > 50", 0,
				new biouml.model.dynamics.Assignment[] { model.createAssignment( d, "vac_on", "0" ) } );
		assertEquals( "stop", named.getDiagramElement().getName() );
		assertEquals( "stop", named.getDiagramElement().getTitle() );
		assertEquals( "rule_2", model.addEquation( d, "rule_2", "scalar", "Total2", "2*$default.s1" ).getDiagramElement().getName() );
	}

	/** Assignments given as a list keep their order (a JSON object's keys may be reordered by the client). */
	@SuppressWarnings( "unchecked" )
	public void testEventAssignmentsAsOrderedList() throws Exception
	{
		McpToolCatalog catalog = new McpToolCatalog();
		DiagramTools.registerAll( catalog );
		McpToolCatalog.Tool tool = catalog.getTools().stream().filter( t -> t.name.equals( "biouml_diagram_add_event" ) ).findFirst().get();
		assertTrue( "published as a list", tool.inputSchema.contains( "\"assignments\":{\"type\":\"array\"" ) );

		Map<String, Object> args = new java.util.HashMap<String, Object>();
		args.put( "diagramPath", diagramPath );
		args.put( "name", "switch" );
		args.put( "trigger", "time > 10" );
		args.put( "assignments", java.util.Arrays.asList( "$default.s2 = $default.s1", "s1 = 0" ) );
		Map<String, Object> m = data( tool.handler.handle( null, args ) );
		assertEquals( "{$default.s2=$default.s1, $default.s1=0}", m.get( "assignments" ).toString() );

		args.put( "name", "bad" );
		args.put( "assignments", java.util.Arrays.asList( "s1 == 0" ) );
		assertEquals( McpConstants.CODE_INVALID_PARAMS, tool.handler.handle( null, args ).getCode() );

		biouml.model.dynamics.Assignment[] saved = reload().findNode( "switch" ).getRole( biouml.model.dynamics.Event.class ).getEventAssignment();
		assertEquals( "$default.s2", saved[ 0 ].getVariable() );
		assertEquals( "$default.s1", saved[ 1 ].getVariable() );
	}

	/** add_node can copy the look of an existing node or set a fill color. */
	public void testAddNodeStyle() throws Exception
	{
		Node s1 = diagram().findNode( "s1" );
		s1.setPredefinedStyle( biouml.model.DiagramElementStyle.STYLE_NOT_SELECTED );
		s1.getCustomStyle().setBrush( new ru.biosoft.graphics.Brush( new java.awt.Color( 200, 230, 255 ) ) );
		diagram().save();

		data( McpDiagramSupport.addNode( diagramPath, "default/A", "unspecified", 10, 200, null, null, null, "s1", null ) );
		data( McpDiagramSupport.addNode( diagramPath, "default/B", "unspecified", 110, 200, null, null, null, null, "#ff8800" ) );
		assertFalse( McpDiagramSupport.addNode( diagramPath, "default/C", "unspecified", 10, 10, null, null, null, null, "orange-ish" ).isOk() );
		assertEquals( McpConstants.CODE_NOT_FOUND,
				McpDiagramSupport.addNode( diagramPath, "default/D", "unspecified", 10, 10, null, null, null, "nope", null ).getCode() );

		Diagram d = reload();
		assertEquals( new java.awt.Color( 200, 230, 255 ), d.findNode( "A" ).getCustomStyle().getBrush().getPaint() );
		assertEquals( new java.awt.Color( 0xff, 0x88, 0x00 ), d.findNode( "B" ).getCustomStyle().getBrush().getPaint() );
		assertNull( "failed add undone", d.findNode( "C" ) );
	}

	/** resize_element, set_formula and set_simulation_options: the edits that needed a script. */
	@SuppressWarnings( "unchecked" )
	public void testResizeSetFormulaSimulationOptions() throws Exception
	{
		data( McpDiagramSupport.resizeElement( diagramPath, "default", 640, 480 ) );

		data( McpDiagramSupport.addReaction( diagramPath, "r1", "1", new String[] { "s1" }, new String[] { "s2" }, null, null, null, null, null ) );
		Map<String, String> k1 = new java.util.HashMap<String, String>();
		k1.put( "k1", "0.3" );
		Map<String, Object> m = data( McpDiagramSupport.setFormula( diagramPath, "r1", null, "k1*$default.s1", k1 ) );
		assertEquals( "reaction", m.get( "kind" ) );
		assertEquals( "1", m.get( "oldFormula" ) );

		data( McpDiagramSupport.addEquation( diagramPath, "total_rule", null, "Total", "$default.s1 + $default.s2", null, null, null ) );
		m = data( McpDiagramSupport.setFormula( diagramPath, null, "Total", "2*$default.s1", null ) );
		assertEquals( "total_rule", m.get( "id" ) );
		assertEquals( "equation", m.get( "kind" ) );

		assertEquals( McpConstants.CODE_INVALID_PARAMS, McpDiagramSupport.setFormula( diagramPath, "r1", "Total", "1", null ).getCode() );
		assertEquals( McpConstants.CODE_NOT_FOUND, McpDiagramSupport.setFormula( diagramPath, "nope", null, "1", null ).getCode() );
		assertEquals( McpConstants.CODE_INVALID_PARAMS, McpDiagramSupport.setFormula( diagramPath, "s1", null, "1", null ).getCode() );

		m = data( McpDiagramSupport.setSimulationOptions( diagramPath, null, 730.0, 0.1 ) );
		assertEquals( 730.0, m.get( "completionTime" ) );
		assertFalse( McpDiagramSupport.setSimulationOptions( diagramPath, 800.0, null, null ).isOk() );

		Diagram d = reload();
		assertEquals( new java.awt.Dimension( 640, 480 ), d.findNode( "default" ).getShapeSize() );
		Node r1 = d.findNode( "r1" );
		assertEquals( "k1*$default.s1", ( (Reaction) r1.getKernel() ).getFormula() );
		assertEquals( "rate equation follows", "k1*$default.s1", r1.getRole( Equation.class ).getFormula() );
		assertEquals( 0.3, d.getRole( EModel.class ).getVariable( "k1" ).getInitialValue() );
		String rule = d.findNode( "total_rule" ).getRole( Equation.class ).getFormula();
		assertTrue( rule, rule.matches( "2(\\.0)?\\*\\$default\\.s1" ) ); // MathML writes 2 as 2.0
		Map<String, Object> options = (Map<String, Object>) data( McpDiagramSupport.describe( diagramPath ) ).get( "simulationOptions" );
		assertNotNull( "simulation options saved", options );
		assertEquals( 730.0, options.get( "completionTime" ) );
		assertEquals( 0.1, options.get( "timeIncrement" ) );
	}

	/** The server version changes with the tool list, so clients caching tool definitions refresh them after a deploy. */
	public void testCatalogFingerprint()
	{
		McpToolCatalog catalog = new McpToolCatalog();
		DiagramTools.registerAll( catalog );
		String before = catalog.fingerprint();
		assertEquals( "stable", before, catalog.fingerprint() );
		assertEquals( 8, before.length() );
		catalog.register( "biouml_extra", "an extra tool", "{}", ( ex, args ) -> null );
		assertFalse( "changes with the tools", before.equals( catalog.fingerprint() ) );
	}

	/** move_element / move_elements move like the editor: a compartment takes its content along, once. */
	@SuppressWarnings( "unchecked" )
	public void testMoveElements() throws Exception
	{
		data( McpDiagramSupport.addReaction( diagramPath, "r1", "1", new String[] { "s1" }, new String[] { "s2" }, null, null, null, 150, 60 ) );
		java.awt.Point s1 = diagram().findNode( "s1" ).getLocation();
		java.awt.Point s2 = diagram().findNode( "s2" ).getLocation();
		java.awt.Point def = diagram().findNode( "default" ).getLocation();

		Map<String, Object> m = data( McpDiagramSupport.moveElements( diagramPath, new String[] { "s1", "s2" }, 10, 20 ) );
		assertEquals( java.util.Arrays.asList( "s1", "s2" ), m.get( "moved" ) );
		assertEquals( new java.awt.Point( s1.x + 10, s1.y + 20 ), diagram().findNode( "s1" ).getLocation() );

		m = data( McpDiagramSupport.moveElements( diagramPath, new String[] { "default", "s1" }, 100, 0 ) );
		assertEquals( java.util.Arrays.asList( "default" ), m.get( "moved" ) );
		assertEquals( java.util.Arrays.asList( "s1" ), m.get( "movedWithCompartment" ) );

		data( McpDiagramSupport.moveElement( diagramPath, "default", def.x + 100, def.y + 50 ) ); // +0, +50 more

		Diagram d = reload();
		assertEquals( new java.awt.Point( def.x + 100, def.y + 50 ), d.findNode( "default" ).getLocation() );
		assertEquals( "moved once with its compartment", new java.awt.Point( s1.x + 110, s1.y + 70 ), d.findNode( "s1" ).getLocation() );
		assertEquals( new java.awt.Point( s2.x + 110, s2.y + 70 ), d.findNode( "s2" ).getLocation() );
		assertEquals( new java.awt.Point( 250, 110 ), d.findNode( "r1" ).getLocation() );
		assertEquals( McpConstants.CODE_NOT_FOUND, McpDiagramSupport.moveElements( diagramPath, new String[] { "nope" }, 1, 1 ).getCode() );
	}

	/** biouml_diagram_image returns the rendered diagram as a PNG. */
	public void testDiagramImage() throws Exception
	{
		data( McpDiagramSupport.addReaction( diagramPath, "r1", "k*$default.s1", new String[] { "s1" }, new String[] { "s2" }, null, null, null, 100, 40 ) );
		McpEnvelope env = McpDiagramSupport.diagramImage( diagramPath, null, 300, 300 );
		Map<String, Object> m = data( env );
		byte[] png = (byte[]) env.getAttribute( "mcp.image" );
		assertNotNull( "image attached", png );
		java.awt.image.BufferedImage image = javax.imageio.ImageIO.read( new java.io.ByteArrayInputStream( png ) );
		assertNotNull( "valid PNG", image );
		assertTrue( "fits the limit: " + m, image.getWidth() <= 300 && image.getHeight() <= 300 );
		assertEquals( m.get( "width" ), Integer.valueOf( image.getWidth() ) );
		assertFalse( McpDiagramSupport.diagramImage( "mcps/models/nope", null, null, null ).isOk() );
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

		biouml.model.dynamics.Event ev = model.addEvent( d, "time > 5", 1, new biouml.model.dynamics.Assignment[] { model.createAssignment( d, "V", "0" ) } );
		assertEquals( "time > 5", ev.getTrigger() );
		assertEquals( "1", ev.getDelay() );
		assertEquals( "$default.V", ev.getEventAssignment()[ 0 ].getVariable() );

		d.save();
		Diagram reloaded = reload();
		assertNotNull( reloaded.findNode( "V" ) );
		boolean eventSaved = false;
		for ( DiagramElement de : reloaded.recursiveStream().toList() )
			if ( de.getRole() instanceof biouml.model.dynamics.Event
					&& ( (biouml.model.dynamics.Event) de.getRole() ).getTrigger().matches( "time > 5(\\.0)?" ) ) // MathML writes 5 as 5.0
				eventSaved = "$default.V".equals( ( (biouml.model.dynamics.Event) de.getRole() ).getEventAssignment()[ 0 ].getVariable() );
		assertTrue( "event persisted in SBML", eventSaved );
		Reaction rr = (Reaction) reloaded.findNode( r.getName() ).getKernel();
		assertEquals( 2, rr.getSize() );
		boolean found = false;
		for ( DiagramElement de : reloaded.recursiveStream().toList() )
			if ( de.getRole() instanceof Equation && "Total".equals( ( (Equation) de.getRole() ).getVariable() ) )
				found = true;
		assertTrue( "assignment rule persisted", found );
	}

	/** The script recipe of the biouml-model-editing skill, run through Rhino with the real host objects. */
	public void testSkillScriptRecipe() throws Exception
	{
		String script = String.join( "\n",
				"var diagram = data.get('" + diagramPath + "');",
				"var em = diagram.getRole();",
				"var params = {k_vac: 0.006, start_day: 30, vac_on: 0};",
				"for (var name in params)",
				"  if (!em.containsVariable(name)) em.declareVariable(name, params[name]);",
				"em.getVariable('k_vac').setConstant(true);",
				"model.addSpecies(diagram, 'default/V', 0);",
				"model.addReaction(diagram, 're_vac', 'k_vac*vac_on*$default.s1', [",
				"  model.createSpecieReference(diagram, 's1', 'reactant'),",
				"  model.createSpecieReference(diagram, 'V', 'product')",
				"]);",
				"model.addEquation(diagram, 'population_rule', 'scalar', 'Population', '$default.s1 + $default.V');",
				"var ev = model.addEvent(diagram, 'vaccination_start', 'time >= start_day', 0, [model.createAssignment(diagram, 'vac_on', '1')]);",
				"ev.getDiagramElement().setLocation(new java.awt.Point(40, 385));",
				"diagram.findNode('re_vac').getKernel().getKineticLaw().setFormula('2*k_vac*vac_on*$default.s1');",
				"diagram.save();",
				"print('recipe done');" );
		StringBuilder out = new StringBuilder();
		ru.biosoft.access.script.ScriptEnvironment env = new ru.biosoft.access.script.NullScriptEnvironment()
		{
			@Override
			public void print( String msg )
			{
				out.append( msg ).append( '\n' );
			}

			@Override
			public void error( String msg )
			{
				out.append( "ERROR: " ).append( msg ).append( '\n' );
			}
		};
		if ( ru.biosoft.plugins.javascript.JScriptVisiblePlugin.getInstance() == null ) // the server starts it at boot: it defines data, model, ...
		{
			Properties js = new Properties();
			js.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, "JavaScript" );
			new ru.biosoft.plugins.javascript.JScriptVisiblePlugin( null, js ).startup();
		}
		ru.biosoft.access.script.ScriptTypeRegistry.createScript( "js", null, script ).execute( script, env, false );
		assertTrue( out.toString(), out.toString().contains( "recipe done" ) );

		Diagram d = reload();
		EModel em = d.getRole( EModel.class );
		assertNotNull( em.getVariable( "$default.V" ) );
		assertTrue( em.getVariable( "k_vac" ).isConstant() );
		assertEquals( 30.0, em.getVariable( "start_day" ).getInitialValue() );
		Reaction r = (Reaction) d.findNode( "re_vac" ).getKernel();
		assertEquals( 2, r.getSize() );
		assertEquals( "2*k_vac*vac_on*$default.s1", r.getFormula().replace( "2.0*", "2*" ) ); // MathML writes 2 as 2.0
		assertEquals( "rate equation follows the kinetic law", r.getFormula(), d.findNode( "re_vac" ).getRole( Equation.class ).getFormula() );
		boolean rule = false, event = false;
		for ( DiagramElement de : d.recursiveStream().toList() )
		{
			if ( de.getRole() instanceof Equation && "Population".equals( ( (Equation) de.getRole() ).getVariable() ) )
				rule = true;
			if ( de.getRole() instanceof biouml.model.dynamics.Event
					&& "vac_on".equals( ( (biouml.model.dynamics.Event) de.getRole() ).getEventAssignment()[ 0 ].getVariable() ) )
				event = true;
		}
		assertTrue( "assignment rule saved", rule );
		assertTrue( "event saved", event );
		assertNotNull( d.findNode( "population_rule" ) );
		assertEquals( new java.awt.Point( 40, 385 ), d.findNode( "vaccination_start" ).getLocation() );
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
