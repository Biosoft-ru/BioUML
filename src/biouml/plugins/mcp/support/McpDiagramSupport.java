package biouml.plugins.mcp.support;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.util.DiagramDMLExporter;
import biouml.model.util.DiagramDMLImporter;
import biouml.plugins.mcp.McpConstants;
import biouml.standard.diagram.MathDiagramType;
import biouml.standard.diagram.PathwayDiagramType;
import biouml.standard.type.Gene;
import biouml.standard.type.Protein;
import biouml.standard.type.RNA;
import biouml.standard.type.Reaction;
import biouml.standard.type.SemanticRelation;
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
		if ( "Substance".equalsIgnoreCase( type ) )
			return new Substance( null, name );
		if ( "Reaction".equalsIgnoreCase( type ) )
			return new Reaction( null, name );
		if ( "SemanticRelation".equalsIgnoreCase( type ) )
			return new SemanticRelation( null, name, "Association" );
		// Fallback: a generic stub kernel.
		return new Stub( null, name );
	}

	/** The allowed node-type keys, for {@code biouml_diagram_node_types}. */
	public static List<String> nodeTypes()
	{
		List<String> t = new ArrayList<String>();
		t.add( "Protein" );
		t.add( "Gene" );
		t.add( "RNA" );
		t.add( "Substance" );
		t.add( "Reaction" );
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
		DataElement de;
		try
		{
			de = CollectionFactory.getDataElement( path );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no diagram at path: " + path );
		}
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
	 * Describe a diagram: element count, element summaries (id, name, kind, position, node-type),
	 * and a dynamic-model summary (variables + equation count) when an {@link EModel} role is present.
	 */
	public static McpEnvelope describe( String path )
	{
		McpEnvelope d = diagram( path );
		if ( !d.isOk() )
			return d;
		Diagram diagram = (Diagram) d.getData();
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "path", path );
		m.put( "elementCount", Integer.valueOf( diagram.getSize() ) );
		List<Map<String, Object>> elements = new ArrayList<Map<String, Object>>();
		for ( DiagramElement el : diagram )
		{
			Map<String, Object> em = new LinkedHashMap<String, Object>();
			em.put( "id", el.getName() );
			em.put( "name", el.getTitle() );
			em.put( "kind", el instanceof Node ? ( el instanceof Edge ? "edge" : "node" ) : el.getClass().getSimpleName() );
			if ( el instanceof Node )
			{
				Node n = (Node) el;
				java.awt.Point p = n.getLocation();
				if ( p != null )
				{
					em.put( "x", Integer.valueOf( p.x ) );
					em.put( "y", Integer.valueOf( p.y ) );
				}
				biouml.standard.type.Base kernel = n.getKernel();
				if ( kernel != null )
					em.put( "nodeType", kernel.getType() );
			}
			elements.add( em );
		}
		m.put( "elements", elements );
		// Dynamic model summary.
		if ( diagram.getRole() instanceof EModel )
		{
			EModel em = (EModel) diagram.getRole();
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
		}
		return McpEnvelope.ok( m );
	}

	// ---------------------------------------------------------------- add / mutate

	/**
	 * Add a node. Returns the assigned element id.
	 */
	public static McpEnvelope addNode( String diagramPath, String name, String nodeType, Integer x, Integer y )
	{
		McpEnvelope d = diagram( diagramPath );
		if ( !d.isOk() )
			return d;
		Diagram diagram = (Diagram) d.getData();
		if ( name == null || name.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "name must be a non-empty string" );
		try
		{
			Node node = new Node( diagram, name, makeNodeKernel( nodeType == null ? "Stub" : nodeType, name ) );
			if ( x != null && y != null )
				node.setLocation( x, y );
			diagram.put( node );
			ru.biosoft.access.CollectionFactoryUtils.save( diagram );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "id", node.getName() );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "addNode failed: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	/**
	 * Add an edge between two nodes. Returns the assigned edge id.
	 */
	public static McpEnvelope addEdge( String diagramPath, String sourceId, String targetId, String edgeType, String label )
	{
		McpEnvelope d = diagram( diagramPath );
		if ( !d.isOk() )
			return d;
		Diagram diagram = (Diagram) d.getData();
		Node source = diagram.findNode( sourceId );
		Node target = diagram.findNode( targetId );
		if ( source == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no node with id: " + sourceId );
		if ( target == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no node with id: " + targetId );
		try
		{
			Edge edge = new Edge( diagram, Edge.getUniqEdgeName( diagram, new SemanticRelation( null, "edge" ), source, target ),
					new SemanticRelation( null, "edge" ), source, target );
			if ( label != null && !label.isEmpty() )
				edge.setTitle( label );
			diagram.put( edge );
			source.addEdge( edge );
			ru.biosoft.access.CollectionFactoryUtils.save( diagram );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "id", edge.getName() );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "addEdge failed: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	/** Remove an element by id. */
	public static McpEnvelope removeElement( String diagramPath, String elementId )
	{
		McpEnvelope d = diagram( diagramPath );
		if ( !d.isOk() )
			return d;
		Diagram diagram = (Diagram) d.getData();
		DiagramElement el = diagram.get( elementId );
		if ( el == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no element with id: " + elementId );
		try
		{
			diagram.remove( elementId );
			ru.biosoft.access.CollectionFactoryUtils.save( diagram );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "removed", elementId );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "removeElement failed: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	/** Move an element to (x, y). */
	public static McpEnvelope moveElement( String diagramPath, String elementId, int x, int y )
	{
		McpEnvelope d = diagram( diagramPath );
		if ( !d.isOk() )
			return d;
		Diagram diagram = (Diagram) d.getData();
		DiagramElement el = diagram.get( elementId );
		if ( !( el instanceof Node ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "element is not a node: " + elementId );
		try
		{
			( (Node) el ).setLocation( x, y );
			ru.biosoft.access.CollectionFactoryUtils.save( diagram );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "id", elementId );
			m.put( "x", Integer.valueOf( x ) );
			m.put( "y", Integer.valueOf( y ) );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "moveElement failed: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	/** Rename an element's display title. */
	public static McpEnvelope renameElement( String diagramPath, String elementId, String newName )
	{
		McpEnvelope d = diagram( diagramPath );
		if ( !d.isOk() )
			return d;
		Diagram diagram = (Diagram) d.getData();
		DiagramElement el = diagram.get( elementId );
		if ( el == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no element with id: " + elementId );
		try
		{
			el.setTitle( newName );
			ru.biosoft.access.CollectionFactoryUtils.save( diagram );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "id", elementId );
			m.put( "name", newName );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "renameElement failed: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
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
