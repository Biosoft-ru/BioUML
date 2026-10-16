package biouml.plugins.mcp.support;

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.VariableRole;
import biouml.model.util.DiagramDMLExporter;
import biouml.model.util.DiagramDMLImporter;
import biouml.plugins.mcp.McpConstants;
import biouml.standard.diagram.MathDiagramType;
import biouml.standard.diagram.ModelElementFactory;
import biouml.standard.diagram.PathwayDiagramType;
import biouml.standard.type.Gene;
import biouml.standard.type.Protein;
import biouml.standard.type.RNA;
import biouml.standard.type.Reaction;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Substance;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.RepositoryException;

/**
 * Shared, test-friendly helpers for the diagram MCP tools (phase 4).
 *
 * <p>All methods take/return plain JSON-serializable values and never throw checked exceptions —
 * failures are returned as {@link McpEnvelope} error envelopes.</p>
 *
 * <p>A {@link Diagram} is itself a {@link DataCollection} of {@link DiagramElement}s (nodes/edges),
 * so elements are added via {@code diagram.put(element)} and retrieved by their string id (the
 * element name).</p>
 */
public final class McpDiagramSupport
{
	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger( McpDiagramSupport.class.getName() );

	private McpDiagramSupport()
	{
	}

	/** The node-type kernel factory: maps a type key to a freshly-constructed kernel. */
	private static biouml.standard.type.Base makeNodeKernel( String type, String name )
	{
		if ( "Protein".equalsIgnoreCase( type ) )
			return new Protein( null, name );
		if ( "Gene".equalsIgnoreCase( type ) )
			return new Gene( null, name );
		if ( "RNA".equalsIgnoreCase( type ) )
			return new RNA( null, name );
		if ( "Substance".equalsIgnoreCase( type ) || "Species".equalsIgnoreCase( type ) )
			return new Substance( null, name );
		if ( "Reaction".equalsIgnoreCase( type ) )
			return new Reaction( null, name );
		if ( "SemanticRelation".equalsIgnoreCase( type ) )
			return new SemanticRelation( null, name, "Association" );
		// Fallback: a generic stub kernel.
		return new Stub( null, name );
	}

	/** Species types of SBML models (SBGN entity pools); "Species" gives the default, a macromolecule. */
	private static final List<String> SBGN_ENTITY_TYPES = java.util.Arrays.asList( "macromolecule", "simple chemical", "unspecified",
			"nucleic acid feature", "perturbing agent", "complex" );

	/** The allowed node-type keys, for {@code biouml_diagram_node_types}. */
	public static List<String> nodeTypes()
	{
		List<String> t = new ArrayList<String>();
		t.add( "Protein" );
		t.add( "Gene" );
		t.add( "RNA" );
		t.add( "Substance" );
		t.add( "Species" );
		t.add( "Compartment" );
		t.add( "Reaction" );
		t.addAll( SBGN_ENTITY_TYPES );
		t.add( "SemanticRelation" );
		t.add( "Stub" );
		return t;
	}

	/**
	 * Resolve a diagram by path. {@link CollectionFactory#getDataElement(String)} returns null for a
	 * missing leaf but throws {@link RepositoryException} for a missing intermediate segment, so any
	 * resolution failure is converted to a {@code not_found} envelope rather than leaking out.
	 */
	private static McpEnvelope diagram( String path )
	{
		if ( path == null || path.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "path must be a non-empty string" );
		DataElement de = McpProviderSupport.resolveElement( path );
		if ( de == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no diagram at path: " + path );
		if ( !( de instanceof Diagram ) )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no diagram at path: " + path );
		return McpEnvelope.ok( de );
	}

	/**
	 * Resolve a parent collection by path, tolerating both a missing leaf (null) and a missing
	 * intermediate segment (a thrown {@link RepositoryException}).
	 * @return envelope whose data is the {@link DataCollection}, else a not_found error.
	 */
	private static McpEnvelope parentCollection( String path )
	{
		if ( path == null || path.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "path must be a non-empty string" );
		DataCollection<?> parent;
		try
		{
			parent = DataElementPath.create( path ).getDataCollection();
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no collection at path: " + path );
		}
		if ( parent == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no collection at path: " + path );
		return McpEnvelope.ok( parent );
	}

	// ---------------------------------------------------------------- create

	/**
	 * Create a new diagram in the target collection. type is "math" (default) or "pathway".
	 * Returns the diagram's path.
	 */
	public static McpEnvelope create( String parentPath, String name, String type )
	{
		if ( name == null || name.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "name must be a non-empty string" );
		McpEnvelope pe = parentCollection( parentPath );
		if ( !pe.isOk() )
			return pe;
		DataCollection<?> parent = (DataCollection<?>) pe.getData();
		try
		{
			biouml.model.DiagramType dt = "pathway".equalsIgnoreCase( type )
					? new PathwayDiagramType()
					: new MathDiagramType();
			Diagram diagram = dt.createDiagram( parent, name, null );
			if ( diagram == null )
				return McpEnvelope.error( McpConstants.CODE_INTERNAL, "could not create diagram: " + name );
			ru.biosoft.access.CollectionFactoryUtils.save( diagram );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "path", DataElementPath.create( diagram ).toString() );
			m.put( "type", dt.getClass().getName() );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "create failed: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	// ---------------------------------------------------------------- describe

	/**
	 * Describe a diagram: element count, element summaries (id, path, name, kind, position, node-type,
	 * plus the variable of a species and the formula of a reaction or equation) for every element
	 * including those nested in compartments, and a dynamic-model summary (variables + equation count)
	 * when an {@link EModel} role is present.
	 */
	public static McpEnvelope describe( String path )
	{
		return describe( path, false );
	}

	/**
	 * @param reload drop the in-memory copies of the diagram first (the server session's working copy
	 *            included), so the diagram is re-read from storage and the description shows what is saved
	 */
	public static McpEnvelope describe( String path, boolean reload )
	{
		McpEnvelope d = diagram( path );
		if ( !d.isOk() )
			return d;
		if ( reload )
		{
			try
			{
				ru.biosoft.access.CollectionFactoryUtils.releaseCachedCopies( DataElementPath.create( (Diagram) d.getData() ) );
				// the path asked for may lead to the diagram through another collection than its origin
				ru.biosoft.access.CollectionFactoryUtils.releaseCachedCopies( DataElementPath.create( path ) );
			}
			catch ( Exception e )
			{
				return McpEnvelope.error( McpConstants.CODE_INTERNAL, "can not re-read the diagram from storage: " + describeError( e ) );
			}
			d = diagram( path );
			if ( !d.isOk() )
				return d;
		}
		Diagram diagram = (Diagram) d.getData();
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "path", path );
		List<Map<String, Object>> elements = new ArrayList<Map<String, Object>>();
		diagram.recursiveStream().skip( 1 ).forEach( el -> elements.add( summary( el ) ) );
		m.put( "elementCount", Integer.valueOf( elements.size() ) );
		m.put( "elements", elements );
		// Dynamic model summary.
		if ( diagram.getRole() instanceof EModel )
		{
			EModel em = (EModel) diagram.getRole();
			// A freshly-loaded (headless) model has variables defaulting to "Not used" until their
			// types are detected; detectVariableTypes() is the same idempotent pass the simulation
			// engine and model view run, so calling it here makes the reported types correct even
			// when the reactive load hook did not fire.
			try
			{
				em.detectVariableTypes();
			}
			catch ( Exception e )
			{
				// ignore — fall back to whatever types are set
			}
			List<Map<String, Object>> vars = new ArrayList<Map<String, Object>>();
			try
			{
				for ( biouml.model.dynamics.Variable v : em.getVariables() )
				{
					Map<String, Object> vm = new LinkedHashMap<String, Object>();
					vm.put( "name", v.getName() );
					vm.put( "type", v.getType() );
					vm.put( "initial", Double.valueOf( v.getInitialValue() ) );
					vars.add( vm );
				}
			}
			catch ( Exception e )
			{
				// ignore
			}
			int eqCount = 0;
			try
			{
				eqCount = em.getEquations().toList().size();
			}
			catch ( Exception e )
			{
				// ignore
			}
			Map<String, Object> dyn = new LinkedHashMap<String, Object>();
			dyn.put( "variables", vars );
			dyn.put( "equationCount", Integer.valueOf( eqCount ) );
			m.put( "dynamicModel", dyn );
			biouml.plugins.simulation.SimulationEngine engine = biouml.standard.diagram.DiagramUtility.getPreferredEngine( diagram );
			if ( engine != null )
				m.put( "simulationOptions", simulationOptions( engine ) );
		}
		return McpEnvelope.ok( m );
	}

	private static Map<String, Object> summary( DiagramElement el )
	{
		Map<String, Object> em = new LinkedHashMap<String, Object>();
		em.put( "id", el.getName() );
		em.put( "path", el.getCompleteNameInDiagram() );
		em.put( "name", el.getTitle() );
		em.put( "kind", el instanceof Edge ? "edge" : el instanceof Compartment ? "compartment" : "node" );
		if ( el instanceof Node )
		{
			Node n = (Node) el;
			java.awt.Point p = n.getLocation();
			if ( p != null )
			{
				em.put( "x", Integer.valueOf( p.x ) );
				em.put( "y", Integer.valueOf( p.y ) );
			}
		}
		biouml.standard.type.Base kernel = el.getKernel();
		if ( kernel != null )
			em.put( "nodeType", kernel.getType() );
		if ( el.getRole() instanceof VariableRole )
			em.put( "variable", el.getRole( VariableRole.class ).getName() );
		if ( kernel instanceof Reaction )
			em.put( "formula", ( (Reaction) kernel ).getFormula() );
		else if ( el instanceof Node && el.getRole() instanceof Equation )
		{
			Equation eq = el.getRole( Equation.class );
			em.put( "equationType", eq.getType() );
			em.put( "variable", eq.getVariable() );
			em.put( "formula", eq.getFormula() );
		}
		if ( el instanceof Edge )
		{
			Edge e = (Edge) el;
			em.put( "source", e.getInput().getName() );
			em.put( "target", e.getOutput().getName() );
			if ( kernel instanceof SpecieReference )
			{
				em.put( "role", ( (SpecieReference) kernel ).getRole() );
				em.put( "stoichiometry", ( (SpecieReference) kernel ).getStoichiometry() );
			}
		}
		return em;
	}

	// ---------------------------------------------------------------- add / mutate

	/**
	 * State of a diagram before an edit, to undo the edit when it fails half-way or the edited diagram
	 * can not be saved. Without it a failed edit stays in the cached diagram and breaks every later save.
	 */
	private static final class Snapshot
	{
		private final Set<DiagramElement> elements = java.util.Collections.newSetFromMap( new java.util.IdentityHashMap<DiagramElement, Boolean>() );
		private final Map<String, Double> values = new LinkedHashMap<String, Double>();

		Snapshot( Diagram diagram )
		{
			diagram.recursiveStream().forEach( elements::add );
			if ( diagram.getRole() instanceof EModel )
				for ( biouml.model.dynamics.Variable v : diagram.getRole( EModel.class ).getVariables() )
					values.put( v.getName(), Double.valueOf( v.getInitialValue() ) );
		}

		/** Names of the parameters (non-species variables) declared since the snapshot. */
		List<String> newParameters( Diagram diagram )
		{
			List<String> result = new ArrayList<String>();
			if ( diagram.getRole() instanceof EModel )
				for ( biouml.model.dynamics.Variable v : diagram.getRole( EModel.class ).getVariables() )
					if ( !values.containsKey( v.getName() ) && !v.getName().startsWith( "$" ) )
						result.add( v.getName() );
			return result;
		}

		void restore( Diagram diagram )
		{
			List<DiagramElement> added = diagram.recursiveStream().filter( de -> !elements.contains( de ) ).toList();
			for ( DiagramElement de : added )
			{
				if ( ! ( de instanceof Edge ) )
					continue;
				try
				{
					if ( de.getKernel() instanceof SpecieReference && de.getKernel().getOrigin() instanceof Reaction )
						( (Reaction) de.getKernel().getOrigin() ).remove( de.getKernel().getName() );
					de.getOrigin().remove( de.getName() );
				}
				catch ( Exception e )
				{
					log.log( java.util.logging.Level.WARNING, "Can not undo edge " + de.getName(), e );
				}
			}
			java.util.Collections.reverse( added ); // nested elements first
			for ( DiagramElement de : added )
			{
				try
				{
					if ( ! ( de instanceof Edge ) && de.getOrigin() != null && ( (DataCollection<?>) de.getOrigin() ).contains( de.getName() ) )
						de.getOrigin().remove( de.getName() );
				}
				catch ( Exception e )
				{
					log.log( java.util.logging.Level.WARNING, "Can not undo element " + de.getName(), e );
				}
			}
			if ( diagram.getRole() instanceof EModel )
			{
				DataCollection<biouml.model.dynamics.Variable> variables = diagram.getRole( EModel.class ).getVariables();
				for ( String name : variables.names().filter( n -> !values.containsKey( n ) ).toArray( String[]::new ) )
				{
					try
					{
						variables.remove( name );
					}
					catch ( Exception e )
					{
						log.log( java.util.logging.Level.WARNING, "Can not undo variable " + name, e );
					}
				}
				EModel emodel = diagram.getRole( EModel.class );
				for ( Map.Entry<String, Double> e : values.entrySet() )
				{
					biouml.model.dynamics.Variable v = emodel.getVariable( e.getKey() );
					if ( v != null && v.getInitialValue() != e.getValue().doubleValue() )
						v.setInitialValue( e.getValue().doubleValue() );
				}
			}
		}
	}

	/** An element named in the request is missing: reported as {@code not_found}. */
	private static final class NotFoundException extends IllegalArgumentException
	{
		private static final long serialVersionUID = 1L;

		NotFoundException( String message )
		{
			super( message );
		}
	}

	/** A diagram edit: changes the diagram in memory and returns the result data. */
	@FunctionalInterface
	private interface Edit
	{
		Map<String, Object> apply( Diagram diagram ) throws Exception;
	}

	/**
	 * Apply an edit and save the diagram. If the edit or the save fails, the edit is undone in the
	 * cached diagram, so the diagram stays saveable, and the error names the root cause.
	 */
	private static McpEnvelope edit( String diagramPath, String what, Edit edit )
	{
		McpEnvelope d = diagram( diagramPath );
		if ( !d.isOk() )
			return d;
		Diagram diagram = (Diagram) d.getData();
		ensureView( diagram );
		Snapshot snapshot = new Snapshot( diagram );
		Map<String, Object> result;
		try
		{
			result = edit.apply( diagram );
		}
		catch ( IllegalArgumentException e )
		{
			snapshot.restore( diagram );
			return McpEnvelope.error( e instanceof NotFoundException ? McpConstants.CODE_NOT_FOUND : McpConstants.CODE_INVALID_PARAMS,
					what + " failed: " + e.getMessage() );
		}
		catch ( Exception e )
		{
			snapshot.restore( diagram );
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, what + " failed: " + describeError( e ) );
		}
		try
		{
			ru.biosoft.access.CollectionFactoryUtils.save( diagram );
		}
		catch ( Exception e )
		{
			snapshot.restore( diagram );
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, what + " failed, the change was undone: " + describeError( e ) );
		}
		List<String> declared = snapshot.newParameters( diagram );
		if ( !declared.isEmpty() && !result.containsKey( "declaredParameters" ) )
			result.put( "declaredParameters", declared );
		return McpEnvelope.ok( result );
	}

	/**
	 * Build the diagram's view if it (or any of its nodes) has none, as rendering it would. Some semantic
	 * controllers lay out or shift elements through their views and
	 * fail with a NullPointerException on a diagram that was loaded but never shown.
	 */
	static void ensureView( Diagram diagram )
	{
		if ( diagram.getView() != null && diagram.recursiveStream().skip( 1 ).allMatch( de -> de.getView() != null ) )
			return;
		try
		{
			synchronized ( diagram )
			{
				diagram.getType().getDiagramViewBuilder().createDiagramView( diagram, com.developmentontheedge.application.ApplicationUtils.getGraphics() );
			}
		}
		catch ( Throwable t )
		{
			log.log( java.util.logging.Level.FINE, "could not build the view of " + diagram.getName(), t );
		}
	}

	/** An exception with its chain of causes, e.g. "DataElementPutException: Cannot save ... <- NullPointerException: ...". */
	static String describeError( Throwable t )
	{
		StringBuilder sb = new StringBuilder();
		java.util.Set<Throwable> seen = java.util.Collections.newSetFromMap( new java.util.IdentityHashMap<Throwable, Boolean>() );
		for ( Throwable c = t; c != null && seen.add( c ); c = c.getCause() )
		{
			if ( sb.length() > 0 )
				sb.append( " <- " );
			sb.append( c.getClass().getSimpleName() ).append( ": " ).append( c.getMessage() );
			StackTraceElement[] trace = c.getStackTrace();
			if ( c.getCause() == null && trace.length > 0 )
				sb.append( " at " ).append( trace[ 0 ] );
		}
		return sb.toString();
	}

	private static Point point( Integer x, Integer y )
	{
		return x != null && y != null ? new Point( x.intValue(), y.intValue() ) : null;
	}

	/**
	 * Add a node. name may be qualified by a compartment ("default/V"); the node is then created
	 * inside that compartment. In a diagram with a mathematical model (e.g. any SBML model) species,
	 * compartments and reactions are created by the diagram type's semantic controller, so they get
	 * their model variables and equations. Returns the assigned element id (and the variable, if any).
	 */
	public static McpEnvelope addNode( String diagramPath, String name, String nodeType, Integer x, Integer y )
	{
		return addNode( diagramPath, name, nodeType, x, y, null, null, null );
	}

	public static McpEnvelope addNode( String diagramPath, String name, String nodeType, Integer x, Integer y, String title, Integer width,
			Integer height )
	{
		return addNode( diagramPath, name, nodeType, x, y, title, width, height, null, null );
	}

	/**
	 * Add a node with an optional display title and size (width and height in pixels). For SBML models
	 * nodeType may also be an SBGN entity type ("unspecified", "simple chemical", ...).
	 */
	public static McpEnvelope addNode( String diagramPath, String name, String nodeType, Integer x, Integer y, String title, Integer width,
			Integer height, String styleFrom, String fillColor )
	{
		java.awt.Color fill = null;
		if ( fillColor != null && !fillColor.isEmpty() )
		{
			try
			{
				fill = java.awt.Color.decode( fillColor.trim() );
			}
			catch ( NumberFormatException e )
			{
				return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "fillColor must be a color like #c8e6ff, got: " + fillColor );
			}
		}
		final java.awt.Color fillPaint = fill;
		if ( name == null || name.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "name must be a non-empty string" );
		String type = nodeType == null || nodeType.isEmpty() ? "Stub" : nodeType;
		if ( !nodeTypes().stream().anyMatch( type::equalsIgnoreCase ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "unknown nodeType '" + type + "', expected one of " + nodeTypes() );
		return edit( diagramPath, "addNode", diagram -> {
			String[] parts = ModelElementFactory.splitQualifiedName( name );
			Compartment parent = ModelElementFactory.findCompartment( diagram, parts[ 0 ] );
			String id = parts[ 1 ];
			Point location = point( x, y );
			boolean reactionModel = isReactionModel( diagram );
			Node node;
			if ( reactionModel && "Reaction".equalsIgnoreCase( type ) )
				node = ModelElementFactory.addReaction( parent, id, null, null, location );
			else if ( "Compartment".equalsIgnoreCase( type ) )
				node = ModelElementFactory.addCompartment( parent, id, location, null );
			else if ( reactionModel && isSpeciesType( type ) )
				node = ModelElementFactory.addSpecies( parent, id, entityType( type ), makeNodeKernel( type, id ), 0.0, location );
			else
			{
				if ( diagram.containsRecursively( id ) )
					throw new IllegalArgumentException( "diagram already contains an element named '" + id + "'" );
				node = new Node( parent, id, makeNodeKernel( type, id ) );
				if ( location != null )
					node.setLocation( location );
				parent.put( node );
			}
			if ( title != null && !title.isEmpty() )
				node.setTitle( title );
			if ( width != null && height != null )
				node.setShapeSize( new java.awt.Dimension( width.intValue(), height.intValue() ) );
			if ( styleFrom != null && !styleFrom.isEmpty() )
			{
				Node source = ModelElementFactory.findNode( diagram, styleFrom );
				if ( source == null )
					throw new NotFoundException( "no node to take the style from: " + styleFrom );
				copyStyle( source, node );
			}
			if ( fillPaint != null )
			{
				biouml.model.DiagramElementStyle style = node.isStylePredefined() ? node.getCustomStyle().clone() : node.getCustomStyle();
				style.setBrush( new ru.biosoft.graphics.Brush( fillPaint ) );
				node.setPredefinedStyle( biouml.model.DiagramElementStyle.STYLE_NOT_SELECTED );
				node.setCustomStyle( style );
			}
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "id", node.getName() );
			m.put( "path", node.getCompleteNameInDiagram() );
			m.put( "title", node.getTitle() );
			if ( node.getRole() instanceof VariableRole )
				m.put( "variable", node.getRole( VariableRole.class ).getName() );
			return m;
		} );
	}

	/** Give a node the look of another one: its predefined style, or a copy of its custom style. */
	private static void copyStyle( Node source, Node node )
	{
		String predefined = source.getPredefinedStyle();
		if ( biouml.model.DiagramElementStyle.STYLE_NOT_SELECTED.equals( predefined ) )
		{
			node.setPredefinedStyle( predefined );
			node.setCustomStyle( source.getCustomStyle().clone() );
		}
		else
			node.setPredefinedStyle( predefined );
	}

	/**
	 * Whether the diagram is a reaction-based mathematical model (SBML/SBGN, pathway simulation), whose
	 * species and reactions must carry model roles. Other diagrams (plain math, pathway) get plain nodes.
	 */
	private static boolean isReactionModel( Diagram diagram )
	{
		return diagram.getRole() instanceof EModel && java.util.Arrays.asList( diagram.getType().getNodeTypes() ).contains( Reaction.class );
	}

	private static boolean isSpeciesType( String type )
	{
		return "Species".equalsIgnoreCase( type ) || "Substance".equalsIgnoreCase( type ) || "Protein".equalsIgnoreCase( type )
				|| "Gene".equalsIgnoreCase( type ) || "RNA".equalsIgnoreCase( type ) || entityType( type ) != null;
	}

	/** The SBGN entity type named by a node type, or null. */
	private static String entityType( String type )
	{
		return SBGN_ENTITY_TYPES.stream().filter( type::equalsIgnoreCase ).findFirst().orElse( null );
	}

	/**
	 * Add an edge between two nodes. When one end is a reaction and the other a species of the
	 * model, the edge makes the species a participant of the reaction: edgeType is its role
	 * (reactant, product or modifier; by default reactant for species -> reaction and product for
	 * reaction -> species). Otherwise a plain relation edge is added. Returns the assigned edge id.
	 */
	public static McpEnvelope addEdge( String diagramPath, String sourceId, String targetId, String edgeType, String label )
	{
		return edit( diagramPath, "addEdge", diagram -> {
			Node source = ModelElementFactory.findNode( diagram, sourceId );
			Node target = ModelElementFactory.findNode( diagram, targetId );
			if ( source == null )
				throw new NotFoundException( "no node with id: " + sourceId );
			if ( target == null )
				throw new NotFoundException( "no node with id: " + targetId );
			Edge edge;
			boolean sourceIsReaction = source.getKernel() instanceof Reaction;
			boolean targetIsReaction = target.getKernel() instanceof Reaction;
			if ( sourceIsReaction != targetIsReaction && ( sourceIsReaction ? target : source ).getRole() instanceof VariableRole )
			{
				String role = edgeType == null || edgeType.isEmpty() ? ( sourceIsReaction ? SpecieReference.PRODUCT : SpecieReference.REACTANT ) : edgeType;
				edge = sourceIsReaction ? ModelElementFactory.addReactionParticipant( source, target, role, null )
						: ModelElementFactory.addReactionParticipant( target, source, role, null );
			}
			else
			{
				SemanticRelation relation = new SemanticRelation( null, "edge" );
				edge = new Edge( Edge.getUniqEdgeName( diagram, relation, source, target ), relation, source, target );
				edge.getOrigin().put( edge );
			}
			if ( label != null && !label.isEmpty() )
				edge.setTitle( label );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "id", edge.getName() );
			if ( edge.getKernel() instanceof SpecieReference )
				m.put( "role", ( (SpecieReference) edge.getKernel() ).getRole() );
			return m;
		} );
	}

	/**
	 * Parse a reaction participant: "s1", "2*s1" or "2 s1" (stoichiometry 2).
	 * @return {stoichiometry or null, species}
	 */
	private static String[] participant( String spec )
	{
		String s = spec == null ? "" : spec.trim();
		java.util.regex.Matcher m = java.util.regex.Pattern.compile( "^([0-9]+(?:\\.[0-9]+)?(?:[eE][-+]?[0-9]+)?)\\s*(?:\\*|\\s)\\s*(\\S+)$" ).matcher( s );
		if ( m.matches() )
			return new String[] { m.group( 1 ), m.group( 2 ) };
		return new String[] { null, s };
	}

	/** Parse parameter values, rejecting non-numbers before anything is changed. */
	private static Map<String, Double> parameterValues( Map<String, String> parameters )
	{
		Map<String, Double> values = new LinkedHashMap<String, Double>();
		if ( parameters == null )
			return values;
		for ( Map.Entry<String, String> e : parameters.entrySet() )
		{
			try
			{
				values.put( e.getKey(), Double.valueOf( Double.parseDouble( e.getValue().trim() ) ) );
			}
			catch ( Exception ex )
			{
				throw new IllegalArgumentException( "value of parameter '" + e.getKey() + "' is not a number: " + e.getValue() );
			}
		}
		return values;
	}

	/** Set initial values of model variables, declaring the missing ones as parameters. */
	private static List<String> applyParameters( Diagram diagram, Map<String, Double> values )
	{
		List<String> set = new ArrayList<String>();
		if ( values.isEmpty() )
			return set;
		if ( ! ( diagram.getRole() instanceof EModel ) )
			throw new IllegalArgumentException( "diagram has no mathematical model" );
		EModel emodel = diagram.getRole( EModel.class );
		for ( Map.Entry<String, Double> e : values.entrySet() )
		{
			String name = ModelElementFactory.resolveVariableName( diagram, e.getKey() );
			if ( name == null )
			{
				if ( !e.getKey().matches( "[_a-zA-Z]\\w*" ) )
					throw new IllegalArgumentException( "no variable '" + e.getKey() + "' and it is not a valid parameter name" );
				emodel.declareVariable( e.getKey(), e.getValue() );
				name = e.getKey();
			}
			else
				emodel.getVariable( name ).setInitialValue( e.getValue().doubleValue() );
			set.add( name );
		}
		return set;
	}

	/**
	 * Add a reaction with its participants and kinetic law in one step: the reaction node, its edges,
	 * the {@code $$rate_<name>} variable and the participants' rate equations. Participants are given
	 * as "s1", "2*s1" or "2 s1" (stoichiometry 2), by species name, path ("default.s1") or variable.
	 * parameters declares (or sets) parameter values used by the formula; any other undeclared
	 * identifier in the formula is declared with value 0 and reported in declaredParameters.
	 */
	public static McpEnvelope addReaction( String diagramPath, String name, String formula, String[] reactants, String[] products,
			String[] modifiers, String compartment, Map<String, String> parameters, Integer x, Integer y )
	{
		if ( formula == null || formula.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "formula (the kinetic law) must be a non-empty string" );
		return edit( diagramPath, "addReaction", diagram -> {
			Map<String, Double> values = parameterValues( parameters );
			List<SpecieReference> components = new ArrayList<SpecieReference>();
			String[][] groups = { reactants, products, modifiers };
			String[] roles = { SpecieReference.REACTANT, SpecieReference.PRODUCT, SpecieReference.MODIFIER };
			for ( int g = 0; g < groups.length; g++ )
				if ( groups[ g ] != null )
					for ( String spec : groups[ g ] )
					{
						String[] p = participant( spec );
						components.add( ModelElementFactory.createSpecieReference( diagram, p[ 1 ], roles[ g ], p[ 0 ] ) );
					}
			Compartment parent = diagram;
			if ( compartment != null && !compartment.isEmpty() )
				parent = ModelElementFactory.findCompartment( diagram, compartment );
			else if ( !components.isEmpty() )
			{
				Node first = ModelElementFactory.findVariableNode( diagram, components.get( 0 ).getSpecie() );
				if ( ! ( first.getCompartment().getKernel() instanceof biouml.standard.type.Specie ) )
					parent = first.getCompartment();
			}
			if ( name != null && name.contains( "/" ) )
				throw new IllegalArgumentException( "name must not contain '/': use the compartment argument" );
			Node node = ModelElementFactory.addReaction( parent, name == null || name.isEmpty() ? null : name, formula, components, point( x, y ) );
			List<String> set = applyParameters( diagram, values );
			ModelElementFactory.declareFormulaParameters( diagram, formula, node.getRole() );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "id", node.getName() );
			m.put( "path", node.getCompleteNameInDiagram() );
			m.put( "rateVariable", node.getRole() instanceof Equation ? node.getRole( Equation.class ).getVariable() : null );
			m.put( "participants", Integer.valueOf( components.size() ) );
			if ( !set.isEmpty() )
				m.put( "parametersSet", set );
			return m;
		} );
	}

	/**
	 * Add an equation: an assignment rule ('scalar', the default), a rate rule ('rate'), an
	 * algebraic rule ('algebraic') or an initial assignment ('initial assignment'). variable is a
	 * parameter, or a species by name, path or variable; a missing parameter is declared.
	 * parameters declares (or sets) parameter values used by the formula.
	 */
	public static McpEnvelope addEquation( String diagramPath, String type, String variable, String formula,
			Map<String, String> parameters, Integer x, Integer y )
	{
		return addEquation( diagramPath, null, type, variable, formula, parameters, x, y );
	}

	/** Add an equation with the given node name (null: generated). */
	public static McpEnvelope addEquation( String diagramPath, String name, String type, String variable, String formula,
			Map<String, String> parameters, Integer x, Integer y )
	{
		return edit( diagramPath, "addEquation", diagram -> {
			Map<String, Double> values = parameterValues( parameters );
			Node node = ModelElementFactory.addEquation( diagram, name, type, variable, formula, point( x, y ) );
			List<String> set = applyParameters( diagram, values );
			Equation eq = node.getRole( Equation.class );
			ModelElementFactory.declareFormulaParameters( diagram, formula, eq );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "id", node.getName() );
			m.put( "type", eq.getType() );
			m.put( "variable", eq.getVariable() );
			m.put( "formula", eq.getFormula() );
			if ( !set.isEmpty() )
				m.put( "parametersSet", set );
			return m;
		} );
	}

	/**
	 * Add an event: when trigger becomes true (after delay), each assignment {variable: formula} is
	 * performed in order. Variables are parameters or species (by name, path or variable); parameters
	 * declares or sets parameter values first, so assignments may target new parameters.
	 */
	public static McpEnvelope addEvent( String diagramPath, String name, String trigger, String delay, Map<String, String> assignments,
			Map<String, String> parameters, Integer x, Integer y )
	{
		if ( trigger == null || trigger.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "trigger must be a non-empty string" );
		if ( assignments == null || assignments.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "assignments must be a non-empty object {variable: formula}" );
		return edit( diagramPath, "addEvent", diagram -> {
			List<String> set = applyParameters( diagram, parameterValues( parameters ) );
			List<biouml.model.dynamics.Assignment> actions = new ArrayList<biouml.model.dynamics.Assignment>();
			for ( Map.Entry<String, String> a : assignments.entrySet() )
				actions.add( new biouml.model.dynamics.Assignment( a.getKey(), a.getValue() ) );
			Node node = ModelElementFactory.addEvent( diagram, name, trigger, delay, actions.toArray( new biouml.model.dynamics.Assignment[ 0 ] ),
					point( x, y ) );
			biouml.model.dynamics.Event event = node.getRole( biouml.model.dynamics.Event.class );
			ModelElementFactory.declareFormulaParameters( diagram, trigger, event );
			Map<String, Object> done = new LinkedHashMap<String, Object>();
			for ( biouml.model.dynamics.Assignment a : event.getEventAssignment() )
			{
				ModelElementFactory.declareFormulaParameters( diagram, a.getMath(), event );
				done.put( a.getVariable(), a.getMath() );
			}
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "id", node.getName() );
			m.put( "trigger", event.getTrigger() );
			m.put( "delay", event.getDelay() );
			m.put( "assignments", done );
			if ( !set.isEmpty() )
				m.put( "parametersSet", set );
			return m;
		} );
	}

	/**
	 * Render a diagram as a PNG image, returned as an MCP image content block (like
	 * biouml_simulation_plot). The view is built afresh, so it shows the current state of the diagram.
	 *
	 * @param scale     zoom factor (default 1)
	 * @param maxWidth  the image is scaled down to fit (default 1600)
	 * @param maxHeight the image is scaled down to fit (default 1600)
	 */
	public static McpEnvelope diagramImage( String path, Double scale, Integer maxWidth, Integer maxHeight )
	{
		McpEnvelope d = diagram( path );
		if ( !d.isOk() )
			return d;
		Diagram diagram = (Diagram) d.getData();
		try
		{
			java.awt.Graphics g = com.developmentontheedge.application.ApplicationUtils.getGraphics();
			ru.biosoft.graphics.View view;
			synchronized ( diagram )
			{
				view = diagram.getType().getDiagramViewBuilder().createDiagramView( diagram, g );
			}
			java.awt.Rectangle bounds = view.getBounds();
			if ( bounds.width <= 0 || bounds.height <= 0 )
				return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "diagram is empty: " + path );
			int maxW = maxWidth == null || maxWidth.intValue() <= 0 ? 1600 : maxWidth.intValue();
			int maxH = maxHeight == null || maxHeight.intValue() <= 0 ? 1600 : maxHeight.intValue();
			double k = scale == null || scale.doubleValue() <= 0 ? 1 : scale.doubleValue();
			k = Math.min( k, Math.min( (double) maxW / ( bounds.width + 20 ), (double) maxH / ( bounds.height + 20 ) ) );
			view.setLocation( 10, 10 );
			java.awt.image.BufferedImage image = ru.biosoft.graphics.ImageGenerator.generateImage( view, k, true );
			java.io.ByteArrayOutputStream png = new java.io.ByteArrayOutputStream();
			javax.imageio.ImageIO.write( image, "png", png );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "path", path );
			m.put( "imageFormat", "png" );
			m.put( "width", Integer.valueOf( image.getWidth() ) );
			m.put( "height", Integer.valueOf( image.getHeight() ) );
			m.put( "scale", Double.valueOf( k ) );
			McpEnvelope env = McpEnvelope.ok( m );
			env.setAttribute( "mcp.image", png.toByteArray() );
			return env;
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "could not render the diagram: " + describeError( e ) );
		}
	}

	/**
	 * Set initial values of model variables — parameters by name, species by name, path or variable;
	 * a missing parameter is declared. Optionally marks them constant / not constant.
	 */
	public static McpEnvelope setParameters( String diagramPath, Map<String, String> parameters, Boolean constant )
	{
		if ( parameters == null || parameters.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "parameters must be a non-empty object {name: value}" );
		return edit( diagramPath, "setParameters", diagram -> {
			List<String> set = applyParameters( diagram, parameterValues( parameters ) );
			if ( constant != null )
				for ( String name : set )
					diagram.getRole( EModel.class ).getVariable( name ).setConstant( constant.booleanValue() );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "set", set );
			return m;
		} );
	}

	/** Find an element by its id: a name ("s1") or a path in the diagram ("default.s1" or "default/s1"). */
	private static DiagramElement findElement( Diagram diagram, String elementId )
	{
		if ( elementId == null || elementId.isEmpty() )
			return null;
		return diagram.findDiagramElement( elementId.replace( '/', '.' ) );
	}

	/**
	 * Add a repository element (a track, table, diagram, ...) to a diagram, optionally into a compartment —
	 * the headless equivalent of dragging it from the repository tree onto the diagram in the web UI
	 * ({@code WebDiagramsProvider.addDroppedDiagramElement}). The diagram type decides what node it becomes,
	 * e.g. a data node that refers to the track.
	 */
	public static McpEnvelope addDataElement( String diagramPath, String elementPath, String compartmentId, Integer x, Integer y )
	{
		DataElementPath path = DataElementPath.create( elementPath );
		if ( path == null || path.optDataElement() == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no element at: " + elementPath );
		return edit( diagramPath, "addDataElement", diagram -> {
			Compartment target = diagram;
			if ( compartmentId != null && !compartmentId.isEmpty() )
			{
				DiagramElement el = findElement( diagram, compartmentId );
				if ( el == null )
					throw new NotFoundException( "no compartment with id: " + compartmentId );
				if ( !( el instanceof Compartment ) )
					throw new IllegalArgumentException( "not a compartment: " + compartmentId );
				target = (Compartment) el;
			}
			Point location = point( x, y );
			if ( location == null && target != diagram && target.getLocation() != null )
				location = new Point( target.getLocation().x + 10, target.getLocation().y + 30 );
			Set<DiagramElement> before = java.util.Collections.newSetFromMap( new java.util.IdentityHashMap<DiagramElement, Boolean>() );
			diagram.recursiveStream().forEach( before::add );
			ru.biosoft.access.biohub.Element element = new ru.biosoft.access.biohub.Element( path );
			biouml.model.util.AddElementsUtils.addElements( target, new ru.biosoft.access.biohub.Element[] { element }, location );
			List<Map<String, Object>> added = new ArrayList<Map<String, Object>>();
			diagram.recursiveStream().filter( de -> !before.contains( de ) ).forEach( de -> added.add( summary( de ) ) );
			if ( added.isEmpty() )
				throw new IllegalArgumentException( "the diagram did not accept " + elementPath
						+ ( target == diagram ? "" : " in compartment " + compartmentId ) + " (or it is already there)" );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "element", elementPath );
			m.put( "added", added );
			final Compartment requested = target;
			if ( requested != diagram && diagram.recursiveStream().filter( de -> !before.contains( de ) ).noneMatch( de -> de instanceof Node && isInside( (Node) de, requested ) ) )
				m.put( "warning", "the diagram type placed the element outside compartment " + compartmentId
						+ "; to put a node into that compartment, copy it there with biouml_diagram_copy_node" );
			return m;
		} );
	}

	/**
	 * Copy a node into a compartment of the same diagram — the headless equivalent of dragging a node onto
	 * another compartment in the web UI ({@code WebDiagramsProvider.addNodeCopy}).
	 */
	public static McpEnvelope copyNode( String diagramPath, String elementId, String compartmentId, Integer x, Integer y )
	{
		return edit( diagramPath, "copyNode", diagram -> {
			DiagramElement el = findElement( diagram, elementId );
			if ( !( el instanceof Node ) || el == diagram )
				throw new NotFoundException( "no node with id: " + elementId );
			Compartment target = diagram;
			if ( compartmentId != null && !compartmentId.isEmpty() )
			{
				DiagramElement c = findElement( diagram, compartmentId );
				if ( c == null )
					throw new NotFoundException( "no compartment with id: " + compartmentId );
				if ( !( c instanceof Compartment ) )
					throw new IllegalArgumentException( "not a compartment: " + compartmentId );
				target = (Compartment) c;
			}
			Point location = point( x, y );
			if ( location == null )
				location = target == diagram || target.getLocation() == null ? new Point( 0, 0 )
						: new Point( target.getLocation().x + 10, target.getLocation().y + 30 );
			biouml.model.SemanticController controller = diagram.getType().getSemanticController();
			String newName = biouml.model.DefaultSemanticController.generateUniqueNodeName( target, el.getName() );
			Node copy = controller.copyNode( (Node) el, newName, target, location );
			if ( copy == null || !controller.canAccept( target, copy ) )
				throw new IllegalArgumentException( "compartment " + ( compartmentId == null ? "(diagram)" : compartmentId )
						+ " does not accept a copy of " + elementId );
			new biouml.workbench.diagram.DiagramEditorHelper( diagram ).add( copy, location );
			if ( copy.getOrigin() == null || !copy.getOrigin().contains( copy.getName() ) )
				target.put( copy );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "copied", elementId );
			m.put( "added", summary( copy ) );
			return m;
		} );
	}

	/** Remove an element by id, with the elements that depend on it (edges, reaction participants). */
	public static McpEnvelope removeElement( String diagramPath, String elementId )
	{
		return edit( diagramPath, "removeElement", diagram -> {
			DiagramElement el = findElement( diagram, elementId );
			if ( el == null || el == diagram )
				throw new NotFoundException( "no element with id: " + elementId );
			if ( el instanceof Edge && el.getKernel() instanceof SpecieReference && el.getKernel().getOrigin() instanceof Reaction )
				( (Reaction) el.getKernel().getOrigin() ).remove( el.getKernel().getName() );
			if ( !diagram.getType().getSemanticController().remove( el ) && el.getOrigin() != null )
				el.getOrigin().remove( el.getName() );
			List<String> variables = ModelElementFactory.removeUnusedRateVariables( diagram );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "removed", elementId );
			if ( !variables.isEmpty() )
				m.put( "removedVariables", variables );
			return m;
		} );
	}

	/**
	 * Move an element to (x, y) the way the diagram editor does: a compartment takes its content along,
	 * and the attached edges are re-routed.
	 */
	public static McpEnvelope moveElement( String diagramPath, String elementId, int x, int y )
	{
		return edit( diagramPath, "moveElement", diagram -> {
			DiagramElement el = findElement( diagram, elementId );
			if ( !( el instanceof Node ) || el == diagram )
				throw new IllegalArgumentException( "element is not a node: " + elementId );
			Node node = (Node) el;
			Point location = node.getLocation();
			move( diagram, node, x - location.x, y - location.y );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "id", elementId );
			m.put( "x", Integer.valueOf( x ) );
			m.put( "y", Integer.valueOf( y ) );
			putOffCanvasWarnings( m, java.util.Collections.singletonList( node ) );
			return m;
		} );
	}

	/**
	 * Shift several nodes by (dx, dy) in one step. A compartment takes its content along; elements inside
	 * a compartment that is moved too are moved with it (once).
	 */
	public static McpEnvelope moveElements( String diagramPath, String[] elementIds, int dx, int dy )
	{
		if ( elementIds == null || elementIds.length == 0 )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "elements must be a non-empty list of node ids" );
		return edit( diagramPath, "moveElements", diagram -> {
			List<Node> nodes = new ArrayList<Node>();
			for ( String id : elementIds )
			{
				DiagramElement el = findElement( diagram, id );
				if ( el == null || el == diagram )
					throw new NotFoundException( "no element with id: " + id );
				if ( !( el instanceof Node ) )
					throw new IllegalArgumentException( "element is not a node: " + id );
				if ( !nodes.contains( el ) )
					nodes.add( (Node) el );
			}
			List<String> moved = new ArrayList<String>();
			List<String> withCompartment = new ArrayList<String>();
			List<Node> movedNodes = new ArrayList<Node>();
			for ( Node node : nodes )
			{
				if ( nodes.stream().anyMatch( other -> other != node && other instanceof Compartment && isInside( node, (Compartment) other ) ) )
				{
					withCompartment.add( node.getName() );
					continue;
				}
				move( diagram, node, dx, dy );
				moved.add( node.getName() );
				movedNodes.add( node );
			}
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "moved", moved );
			if ( !withCompartment.isEmpty() )
				m.put( "movedWithCompartment", withCompartment );
			m.put( "dx", Integer.valueOf( dx ) );
			m.put( "dy", Integer.valueOf( dy ) );
			putOffCanvasWarnings( m, movedNodes );
			return m;
		} );
	}

	/**
	 * Put a "warnings" list into a move result when a moved node, or a node carried along inside a moved
	 * compartment, ended up at a negative coordinate: it is then partly or fully beyond the left or top
	 * edge of the canvas, where the diagram view does not show it.
	 */
	private static void putOffCanvasWarnings( Map<String, Object> result, List<Node> movedNodes )
	{
		List<String> warnings = new ArrayList<String>();
		movedNodes.stream().flatMap( node -> node.recursiveStream().select( Node.class ) ).distinct().forEach( node -> {
			Point p = node.getLocation();
			if ( p == null || ( p.x >= 0 && p.y >= 0 ) )
				return;
			String edge = p.x < 0 && p.y < 0 ? "left and top edges" : p.x < 0 ? "left edge" : "top edge";
			warnings.add( node.getCompleteNameInDiagram() + " is at (" + p.x + ", " + p.y + "), beyond the " + edge + " of the canvas" );
		} );
		if ( !warnings.isEmpty() )
			result.put( "warnings", warnings );
	}

	private static boolean isInside( Node node, Compartment compartment )
	{
		for ( Object origin = node.getOrigin(); origin instanceof DiagramElement; origin = ( (DiagramElement) origin ).getOrigin() )
			if ( origin == compartment )
				return true;
		return false;
	}

	/** Move a node by an offset through the diagram's semantic controller, keeping its compartment. */
	private static void move( Diagram diagram, Node node, int dx, int dy ) throws Exception
	{
		if ( dx == 0 && dy == 0 )
			return;
		Point before = node.getLocation() == null ? null : new Point( node.getLocation() );
		diagram.getType().getSemanticController().move( node, node.getCompartment(), new java.awt.Dimension( dx, dy ), null );
		// some controllers do not move a plain node themselves
		if ( before != null && before.equals( node.getLocation() ) && !( node instanceof Compartment ) )
			node.setLocation( before.x + dx, before.y + dy );
	}

	/** Resize a node (e.g. enlarge a compartment to make room for new elements). */
	public static McpEnvelope resizeElement( String diagramPath, String elementId, int width, int height )
	{
		if ( width <= 0 || height <= 0 )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "width and height must be positive" );
		return edit( diagramPath, "resizeElement", diagram -> {
			DiagramElement el = findElement( diagram, elementId );
			if ( el == null || el == diagram )
				throw new NotFoundException( "no element with id: " + elementId );
			if ( !( el instanceof Node ) )
				throw new IllegalArgumentException( "element is not a node: " + elementId );
			( (Node) el ).setShapeSize( new java.awt.Dimension( width, height ) );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "id", el.getName() );
			m.put( "width", Integer.valueOf( width ) );
			m.put( "height", Integer.valueOf( height ) );
			return m;
		} );
	}

	/**
	 * Change the formula of an existing reaction (its kinetic law) or equation. The element is given
	 * by its id, or — for a rule — by the variable it defines (e.g. "Population"). parameters declares
	 * or sets parameters of the formula.
	 */
	public static McpEnvelope setFormula( String diagramPath, String elementId, String variable, String formula, Map<String, String> parameters )
	{
		if ( formula == null || formula.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "formula must be a non-empty string" );
		if ( ( elementId == null || elementId.isEmpty() ) == ( variable == null || variable.isEmpty() ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "give either elementId (a reaction or equation) or variable (the variable a rule defines)" );
		return edit( diagramPath, "setFormula", diagram -> {
			Map<String, Double> values = parameterValues( parameters );
			DiagramElement el;
			if ( elementId != null && !elementId.isEmpty() )
			{
				el = findElement( diagram, elementId );
				if ( el == null || el == diagram )
					throw new NotFoundException( "no element with id: " + elementId );
			}
			else
			{
				String name = ModelElementFactory.resolveVariableName( diagram, variable );
				if ( name == null )
					throw new NotFoundException( "no variable: " + variable );
				List<DiagramElement> defining = diagram.recursiveStream()
						.filter( de -> de instanceof Node && de.getRole() instanceof Equation && name.equals( de.getRole( Equation.class ).getVariable() ) )
						.toList();
				if ( defining.isEmpty() )
					throw new NotFoundException( "no equation defines " + name );
				if ( defining.size() > 1 )
					throw new IllegalArgumentException( "several equations define " + name + ": give elementId, one of "
							+ defining.stream().map( DiagramElement::getName ).collect( java.util.stream.Collectors.toList() ) );
				el = defining.get( 0 );
			}
			String kind;
			String oldFormula;
			if ( el.getKernel() instanceof Reaction )
			{
				Reaction reaction = (Reaction) el.getKernel();
				kind = "reaction";
				oldFormula = reaction.getFormula();
				reaction.setFormula( formula ); // the rate equation reads the kinetic law
			}
			else if ( el instanceof Node && el.getRole() instanceof Equation )
			{
				Equation eq = el.getRole( Equation.class );
				kind = "equation";
				oldFormula = eq.getFormula();
				eq.setFormula( formula );
			}
			else
				throw new IllegalArgumentException( "element " + el.getName() + " is neither a reaction nor an equation" );
			List<String> set = applyParameters( diagram, values );
			ModelElementFactory.declareFormulaParameters( diagram, formula, el.getRole() );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "id", el.getName() );
			m.put( "kind", kind );
			if ( el.getRole() instanceof Equation )
				m.put( "variable", el.getRole( Equation.class ).getVariable() );
			m.put( "oldFormula", oldFormula );
			m.put( "formula", formula );
			if ( !set.isEmpty() )
				m.put( "parametersSet", set );
			return m;
		} );
	}

	/**
	 * Set the simulation time span and step stored with the diagram (what a simulation of the diagram
	 * uses). Values left null are kept.
	 */
	public static McpEnvelope setSimulationOptions( String diagramPath, Double initialTime, Double completionTime, Double timeIncrement )
	{
		if ( initialTime == null && completionTime == null && timeIncrement == null )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "give at least one of initialTime, completionTime, timeIncrement" );
		if ( timeIncrement != null && timeIncrement.doubleValue() <= 0 )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "timeIncrement must be positive" );
		return edit( diagramPath, "setSimulationOptions", diagram -> {
			biouml.plugins.simulation.SimulationEngine engine = biouml.standard.diagram.DiagramUtility.getPreferredEngine( diagram );
			if ( engine == null )
			{
				engine = biouml.standard.diagram.DiagramUtility.getEngine( diagram );
				if ( engine == null )
					throw new IllegalArgumentException( "no simulation engine for this diagram" );
				engine.setDiagram( diagram );
				biouml.standard.diagram.DiagramUtility.setPreferredEngine( diagram, engine );
			}
			double start = initialTime != null ? initialTime.doubleValue() : engine.getInitialTime();
			double end = completionTime != null ? completionTime.doubleValue() : engine.getCompletionTime();
			if ( end <= start )
				throw new IllegalArgumentException( "completionTime (" + end + ") must be after initialTime (" + start + ")" );
			engine.setInitialTime( start );
			engine.setCompletionTime( end );
			if ( timeIncrement != null )
				engine.setTimeIncrement( timeIncrement.doubleValue() );
			return simulationOptions( engine );
		} );
	}

	private static Map<String, Object> simulationOptions( biouml.plugins.simulation.SimulationEngine engine )
	{
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "engine", engine.getClass().getSimpleName() );
		m.put( "initialTime", Double.valueOf( engine.getInitialTime() ) );
		m.put( "completionTime", Double.valueOf( engine.getCompletionTime() ) );
		m.put( "timeIncrement", Double.valueOf( engine.getTimeIncrement() ) );
		return m;
	}

	/** Rename an element's display title. */
	public static McpEnvelope renameElement( String diagramPath, String elementId, String newName )
	{
		return edit( diagramPath, "renameElement", diagram -> {
			DiagramElement el = findElement( diagram, elementId );
			if ( el == null || el == diagram )
				throw new NotFoundException( "no element with id: " + elementId );
			el.setTitle( newName );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "id", elementId );
			m.put( "name", newName );
			return m;
		} );
	}

	// ---------------------------------------------------------------- save / import / export

	/**
	 * Save the diagram to a DML file on disk. Returns the file path.
	 */
	public static McpEnvelope save( String diagramPath, String targetDir )
	{
		McpEnvelope d = diagram( diagramPath );
		if ( !d.isOk() )
			return d;
		Diagram diagram = (Diagram) d.getData();
		try
		{
			File dir = targetDir == null || targetDir.isEmpty() ? new File( "." ) : new File( targetDir );
			if ( !dir.exists() )
				dir.mkdirs();
			File file = new File( dir, diagram.getName() + ".dml" );
			DiagramDMLExporter exporter = new DiagramDMLExporter();
			exporter.doExport( diagram, file );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "file", file.getAbsolutePath() );
			m.put( "size", Long.valueOf( file.length() ) );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "save failed: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	/**
	 * Import a diagram file (DML by default) into a target collection.
	 */
	public static McpEnvelope importDiagram( String parentPath, String file, String format, String name )
	{
		File f = new File( file );
		if ( !f.exists() )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no file at: " + file );
		McpEnvelope pe = parentCollection( parentPath );
		if ( !pe.isOk() )
			return pe;
		DataCollection<?> parent = (DataCollection<?>) pe.getData();
		try
		{
			DiagramDMLImporter importer = new DiagramDMLImporter();
			DataElement de = importer.doImport( parent, f, name == null || name.isEmpty() ? f.getName() : name, null, null );
			if ( de == null )
				return McpEnvelope.error( McpConstants.CODE_INTERNAL, "import produced no element" );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "path", DataElementPath.create( de ).toString() );
			m.put( "elementCount", de instanceof Diagram ? Integer.valueOf( ( (Diagram) de ).getSize() ) : null );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "import failed: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	/**
	 * Export a diagram to a file. Returns the file path.
	 */
	public static McpEnvelope exportDiagram( String diagramPath, String targetDir, String format )
	{
		McpEnvelope d = diagram( diagramPath );
		if ( !d.isOk() )
			return d;
		Diagram diagram = (Diagram) d.getData();
		try
		{
			File dir = targetDir == null || targetDir.isEmpty() ? new File( "." ) : new File( targetDir );
			if ( !dir.exists() )
				dir.mkdirs();
			String suffix = format == null || format.isEmpty() ? "dml" : format;
			File file = new File( dir, diagram.getName() + "." + suffix );
			DiagramDMLExporter exporter = new DiagramDMLExporter();
			exporter.doExport( diagram, file );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "file", file.getAbsolutePath() );
			m.put( "size", Long.valueOf( file.length() ) );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "export failed: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}
}
