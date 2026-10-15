package biouml.plugins.mcp.tools.diagram;

import java.util.Map;

import biouml.plugins.mcp.McpConstants;
import biouml.plugins.mcp.server.McpToolCatalog;
import biouml.plugins.mcp.support.McpArgs;
import biouml.plugins.mcp.support.McpDiagramSupport;
import biouml.plugins.mcp.support.McpEnvelope;

/**
 * Phase-4 diagram MCP tools. Registered into a {@link McpToolCatalog}.
 *
 * <p>These tools let an agent create and edit BioUML diagrams headlessly — create a diagram,
 * inspect its elements, add/remove/move/rename nodes and edges, and save/import/export to DML
 * files — without opening the desktop UI.</p>
 */
public final class DiagramTools
{
	private DiagramTools()
	{
	}

	public static void registerAll( McpToolCatalog catalog )
	{
		catalog.register( "biouml_diagram_create",
				"Create a new diagram in a target collection. type is 'math' (default) or 'pathway'. Returns the new diagram's path.",
				"{\"type\":\"object\",\"properties\":{\"parentPath\":{\"type\":\"string\"},\"name\":{\"type\":\"string\"},\"type\":{\"type\":\"string\"}},\"required\":[\"parentPath\",\"name\"]}",
				( ex, args ) -> McpDiagramSupport.create( str( args, "parentPath" ), str( args, "name" ), str( args, "type" ) ) );

		catalog.register( "biouml_diagram_describe",
				"Describe a diagram: element count, per-element summaries for every element including those nested in compartments (id, path, name, kind, position, node type; the variable of a species; the formula of a reaction or equation; source/target/role of an edge) and — when a dynamic model is present — its variables and equation count. reload=true first drops the copy the server keeps in memory for this session and re-reads the diagram from storage, i.e. describes what is actually saved (unsaved changes made by a script are lost).",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"reload\":{\"type\":\"boolean\"}},\"required\":[\"path\"]}",
				( ex, args ) -> McpDiagramSupport.describe( str( args, "path" ), McpArgs.bool( args, "reload", false ) ) );

		catalog.register( "biouml_diagram_node_types",
				"List the node types that biouml_diagram_add_node accepts.",
				"{}",
				( ex, args ) -> McpEnvelope.ok( McpDiagramSupport.nodeTypes() ) );

		catalog.register( "biouml_diagram_add_node",
				"Add a node to a diagram. Returns {id, path, title} and, for a species or compartment of a model, its variable. name may be qualified by a compartment ('default/V' creates V inside compartment default); title is the displayed label (default: the name); width/height set the node size; styleFrom copies the look (fill, border, font) of an existing node, e.g. a neighbouring species; fillColor sets the fill, e.g. '#c8e6ff'. nodeType is one of the keys from biouml_diagram_node_types (default 'Stub'). In a diagram with a mathematical model (e.g. any SBML model) species, compartments and reactions are created by the diagram type, with their model variables and rate equations; for SBML models the species may be given its SBGN entity type ('unspecified', 'simple chemical', 'macromolecule', 'nucleic acid feature', 'perturbing agent', 'complex'; 'Species' is a macromolecule). A reaction created this way has no participants and rate 0 — prefer biouml_diagram_add_reaction. A failed add is undone, so the diagram stays saveable.",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"},\"name\":{\"type\":\"string\"},\"nodeType\":{\"type\":\"string\"},\"title\":{\"type\":\"string\"},\"x\":{\"type\":\"integer\"},\"y\":{\"type\":\"integer\"},\"width\":{\"type\":\"integer\"},\"height\":{\"type\":\"integer\"},\"styleFrom\":{\"type\":\"string\"},\"fillColor\":{\"type\":\"string\"}},\"required\":[\"diagramPath\",\"name\"]}",
				( ex, args ) -> McpDiagramSupport.addNode( str( args, "diagramPath" ), str( args, "name" ), str( args, "nodeType" ),
						optInt( args, "x" ), optInt( args, "y" ), str( args, "title" ), optInt( args, "width" ), optInt( args, "height" ),
						str( args, "styleFrom" ), str( args, "fillColor" ) ) );

		catalog.register( "biouml_diagram_add_edge",
				"Add an edge between two nodes (ids are names or paths such as 'default.s1'). Between a reaction and a species of a model the edge makes the species a participant of the reaction: edgeType is its role — reactant, product or modifier (default: reactant for species -> reaction, product for reaction -> species) — and the participant's rate equation is created. Otherwise a plain relation edge is added. Returns the assigned edge id.",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"},\"sourceId\":{\"type\":\"string\"},\"targetId\":{\"type\":\"string\"},\"edgeType\":{\"type\":\"string\"},\"label\":{\"type\":\"string\"}},\"required\":[\"diagramPath\",\"sourceId\",\"targetId\"]}",
				( ex, args ) -> McpDiagramSupport.addEdge( str( args, "diagramPath" ), str( args, "sourceId" ), str( args, "targetId" ),
						str( args, "edgeType" ), str( args, "label" ) ) );

		catalog.register( "biouml_diagram_add_reaction",
				"Add a reaction to a model diagram in one step: the reaction node, its participant edges, the $$rate_<name> variable and the participants' rate equations, as the diagram editor does. formula is the kinetic law (species as variables, e.g. 'k1*$default.s1'); reactants/products/modifiers are species given as 's1', '2*s1' or '2 s1' (stoichiometry 2) by name, path or variable; name defaults to a generated one; compartment defaults to the first participant's; parameters is an object {name: value} that declares or sets the parameters of the formula. Any other undeclared identifier in the formula is declared with value 0 and listed in declaredParameters. Returns {id, path, rateVariable, participants}.",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"},\"name\":{\"type\":\"string\"},\"formula\":{\"type\":\"string\"},\"reactants\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}},\"products\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}},\"modifiers\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}},\"compartment\":{\"type\":\"string\"},\"parameters\":{\"type\":\"object\",\"additionalProperties\":{\"type\":\"number\"}},\"x\":{\"type\":\"integer\"},\"y\":{\"type\":\"integer\"}},\"required\":[\"diagramPath\",\"formula\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "diagramPath" );
					if ( v == null )
						v = McpArgs.requiredString( args, "formula" );
					if ( v == null )
						v = McpArgs.stringArray( args, "reactants" );
					if ( v == null )
						v = McpArgs.stringArray( args, "products" );
					if ( v == null )
						v = McpArgs.stringArray( args, "modifiers" );
					if ( v != null )
						return v;
					return McpDiagramSupport.addReaction( str( args, "diagramPath" ), str( args, "name" ), str( args, "formula" ),
							McpArgs.strArray( args, "reactants" ), McpArgs.strArray( args, "products" ), McpArgs.strArray( args, "modifiers" ),
							str( args, "compartment" ), McpArgs.stringMap( args, "parameters" ), optInt( args, "x" ), optInt( args, "y" ) );
				} );

		catalog.register( "biouml_diagram_add_equation",
				"Add an equation to a model diagram: type 'scalar' (assignment rule, default), 'rate' (rate rule), 'algebraic' or 'initial assignment'. variable is the target — a parameter (declared if missing) or a species by name, path or variable; formula is the right-hand side; name is the node id (default generated); parameters is an object {name: value} that declares or sets parameters of the formula. Any other undeclared identifier in the formula is declared with value 0 and listed in declaredParameters. Returns {id, type, variable, formula}.",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"},\"name\":{\"type\":\"string\"},\"type\":{\"type\":\"string\"},\"variable\":{\"type\":\"string\"},\"formula\":{\"type\":\"string\"},\"parameters\":{\"type\":\"object\",\"additionalProperties\":{\"type\":\"number\"}},\"x\":{\"type\":\"integer\"},\"y\":{\"type\":\"integer\"}},\"required\":[\"diagramPath\",\"formula\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "diagramPath" );
					if ( v == null )
						v = McpArgs.requiredString( args, "formula" );
					if ( v != null )
						return v;
					return McpDiagramSupport.addEquation( str( args, "diagramPath" ), str( args, "name" ), str( args, "type" ), str( args, "variable" ),
							str( args, "formula" ), McpArgs.stringMap( args, "parameters" ), optInt( args, "x" ), optInt( args, "y" ) );
				} );

		catalog.register( "biouml_diagram_add_event",
				"Add an event to a model diagram: when trigger (a boolean formula, e.g. 'time >= t_start') becomes true, after delay (a formula, default 0), the assignments are performed in the given order. assignments is a list of 'variable = formula' strings, e.g. ['Vaccination_on = 1', '$default.s4 = 0'] (an object {variable: formula} is accepted too, but clients may reorder its keys) — variables are parameters or species by name, path or variable and must exist (declare new parameters through parameters, an object {name: value} applied first); name is the node id (default generated); x, y place the node. Any other undeclared identifier is declared with value 0 and listed in declaredParameters. Returns {id, trigger, delay, assignments}.",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"},\"name\":{\"type\":\"string\"},\"trigger\":{\"type\":\"string\"},\"delay\":{\"type\":\"string\"},\"assignments\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}},\"parameters\":{\"type\":\"object\",\"additionalProperties\":{\"type\":\"number\"}},\"x\":{\"type\":\"integer\"},\"y\":{\"type\":\"integer\"}},\"required\":[\"diagramPath\",\"trigger\",\"assignments\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "diagramPath" );
					if ( v == null )
						v = McpArgs.requiredString( args, "trigger" );
					if ( v != null )
						return v;
					Map<String, String> assignments;
					try
					{
						assignments = assignments( args.get( "assignments" ) );
					}
					catch ( IllegalArgumentException e )
					{
						return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, e.getMessage() );
					}
					return McpDiagramSupport.addEvent( str( args, "diagramPath" ), str( args, "name" ), str( args, "trigger" ), str( args, "delay" ),
							assignments, McpArgs.stringMap( args, "parameters" ), optInt( args, "x" ), optInt( args, "y" ) );
				} );

		catalog.register( "biouml_diagram_set_parameters",
				"Set initial values of model variables: parameters is an object {name: value}; names are parameters (declared if missing) or species by name, path or variable. constant (optional) marks them constant or not. Returns {set}.",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"},\"parameters\":{\"type\":\"object\",\"additionalProperties\":{\"type\":\"number\"}},\"constant\":{\"type\":\"boolean\"}},\"required\":[\"diagramPath\",\"parameters\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "diagramPath" );
					if ( v == null )
						v = McpArgs.boolArg( args, "constant", false );
					if ( v != null )
						return v;
					Boolean constant = args.get( "constant" ) == null ? null : Boolean.valueOf( McpArgs.bool( args, "constant", false ) );
					return McpDiagramSupport.setParameters( str( args, "diagramPath" ), McpArgs.stringMap( args, "parameters" ), constant );
				} );

		catalog.register( "biouml_diagram_add_data_element",
				"Add a repository element (a track, table, diagram, ...) to a diagram — the headless equivalent of dragging it from the repository tree onto the diagram in the web UI. The diagram type decides what node it becomes. E.g. in a Genome Enhancer annotation diagram (input of 'Generate workflow from annotation diagram'), drop the input track into the data compartment ('Drag and drop all files you want to analyze into this box') and into each category compartment it belongs to (e.g. 'Experiment'); remove the nodes of the old track with biouml_diagram_remove_element. diagramPath is the diagram; elementPath is the repository path of the element; compartment (optional) is the id of the compartment to drop it into; x/y (optional) the drop point. Returns {element, added[{id, path, name, kind, ...}]}.",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"},\"elementPath\":{\"type\":\"string\"},\"compartment\":{\"type\":\"string\"},\"x\":{\"type\":\"integer\"},\"y\":{\"type\":\"integer\"}},\"required\":[\"diagramPath\",\"elementPath\"]}",
				( ex, args ) -> McpDiagramSupport.addDataElement( str( args, "diagramPath" ), str( args, "elementPath" ), str( args, "compartment" ),
						optInt( args, "x" ), optInt( args, "y" ) ) );

		catalog.register( "biouml_diagram_remove_element",
				"Remove an element (node or edge) from a diagram by its id (a name or a path such as 'default.s1'), with the edges and reaction participants that depend on it; the rate variables of removed reactions are removed too (listed in removedVariables).",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"},\"elementId\":{\"type\":\"string\"}},\"required\":[\"diagramPath\",\"elementId\"]}",
				( ex, args ) -> McpDiagramSupport.removeElement( str( args, "diagramPath" ), str( args, "elementId" ) ) );

		catalog.register( "biouml_diagram_move_element",
				"Move a node to a new (x, y) position, as the diagram editor does: a compartment takes its content along and the attached edges are re-routed. Returns {id, x, y}, plus warnings when the node or a node of a moved compartment ends up at a negative x or y, i.e. beyond the left or top edge of the canvas.",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"},\"elementId\":{\"type\":\"string\"},\"x\":{\"type\":\"integer\"},\"y\":{\"type\":\"integer\"}},\"required\":[\"diagramPath\",\"elementId\",\"x\",\"y\"]}",
				( ex, args ) -> {
					Integer x = optInt( args, "x" );
					Integer y = optInt( args, "y" );
					if ( x == null || y == null )
						return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "x and y must be integers" );
					return McpDiagramSupport.moveElement( str( args, "diagramPath" ), str( args, "elementId" ), x, y );
				} );

		catalog.register( "biouml_diagram_move_elements",
				"Shift several nodes by (dx, dy) in one call, e.g. a column of equations or a group of species. elements are node ids (names or paths such as 'default.s1'). A compartment takes its content along; listed elements inside a listed compartment move with it, once. Edges are re-routed. Returns {moved, movedWithCompartment, dx, dy}, plus warnings when a moved node (or a node of a moved compartment) ends up at a negative x or y, i.e. beyond the left or top edge of the canvas.",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"},\"elements\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}},\"dx\":{\"type\":\"integer\"},\"dy\":{\"type\":\"integer\"}},\"required\":[\"diagramPath\",\"elements\",\"dx\",\"dy\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "diagramPath" );
					if ( v == null )
						v = McpArgs.stringArray( args, "elements" );
					if ( v != null )
						return v;
					Integer dx = optInt( args, "dx" );
					Integer dy = optInt( args, "dy" );
					if ( dx == null || dy == null )
						return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "dx and dy must be integers" );
					return McpDiagramSupport.moveElements( str( args, "diagramPath" ), McpArgs.strArray( args, "elements" ), dx.intValue(), dy.intValue() );
				} );

		catalog.register( "biouml_diagram_resize_element",
				"Resize a node, e.g. enlarge a compartment to make room for new species and reactions. elementId is a name or a path such as 'default'; width and height are in pixels. Returns {id, width, height}.",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"},\"elementId\":{\"type\":\"string\"},\"width\":{\"type\":\"integer\"},\"height\":{\"type\":\"integer\"}},\"required\":[\"diagramPath\",\"elementId\",\"width\",\"height\"]}",
				( ex, args ) -> {
					Integer w = optInt( args, "width" );
					Integer h = optInt( args, "height" );
					if ( w == null || h == null )
						return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "width and height must be integers" );
					return McpDiagramSupport.resizeElement( str( args, "diagramPath" ), str( args, "elementId" ), w.intValue(), h.intValue() );
				} );

		catalog.register( "biouml_diagram_set_formula",
				"Change the formula of an existing reaction (its kinetic law; the rate equation follows) or equation (assignment rule, rate rule, ...). Give elementId (the reaction or equation node) or variable (the variable a rule defines, e.g. 'Population'). parameters is an object {name: value} that declares or sets parameters of the formula; any other undeclared identifier is declared with value 0 and listed in declaredParameters. Returns {id, kind, variable, oldFormula, formula}.",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"},\"elementId\":{\"type\":\"string\"},\"variable\":{\"type\":\"string\"},\"formula\":{\"type\":\"string\"},\"parameters\":{\"type\":\"object\",\"additionalProperties\":{\"type\":\"number\"}}},\"required\":[\"diagramPath\",\"formula\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "diagramPath" );
					if ( v == null )
						v = McpArgs.requiredString( args, "formula" );
					if ( v != null )
						return v;
					return McpDiagramSupport.setFormula( str( args, "diagramPath" ), str( args, "elementId" ), str( args, "variable" ),
							str( args, "formula" ), McpArgs.stringMap( args, "parameters" ) );
				} );

		catalog.register( "biouml_diagram_set_simulation_options",
				"Set the simulation time span and step stored with a model diagram (used when the diagram is simulated): initialTime, completionTime and timeIncrement; values not given are kept. biouml_diagram_describe shows the current ones. Returns {engine, initialTime, completionTime, timeIncrement}.",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"},\"initialTime\":{\"type\":\"number\"},\"completionTime\":{\"type\":\"number\"},\"timeIncrement\":{\"type\":\"number\"}},\"required\":[\"diagramPath\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "diagramPath" );
					if ( v != null )
						return v;
					Double[] values = new Double[ 3 ];
					String[] keys = { "initialTime", "completionTime", "timeIncrement" };
					for ( int i = 0; i < keys.length; i++ )
					{
						Object value = args.get( keys[ i ] );
						if ( value == null )
							continue;
						try
						{
							values[ i ] = Double.valueOf( Double.parseDouble( String.valueOf( value ) ) );
						}
						catch ( NumberFormatException e )
						{
							return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, keys[ i ] + " must be a number" );
						}
					}
					return McpDiagramSupport.setSimulationOptions( str( args, "diagramPath" ), values[ 0 ], values[ 1 ], values[ 2 ] );
				} );

		catalog.register( "biouml_diagram_rename_element",
				"Rename an element's display title.",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"},\"elementId\":{\"type\":\"string\"},\"newName\":{\"type\":\"string\"}},\"required\":[\"diagramPath\",\"elementId\",\"newName\"]}",
				( ex, args ) -> McpDiagramSupport.renameElement( str( args, "diagramPath" ), str( args, "elementId" ), str( args, "newName" ) ) );

		catalog.register( "biouml_diagram_image",
				"Render a diagram as a PNG picture returned to the caller (an MCP image block, like biouml_simulation_plot), to check the layout. The view is built from the current state of the diagram. scale is the zoom factor (default 1); the picture is scaled down to fit maxWidth x maxHeight pixels (default 1600 x 1600). Returns {width, height, scale} plus the image.",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"scale\":{\"type\":\"number\"},\"maxWidth\":{\"type\":\"integer\"},\"maxHeight\":{\"type\":\"integer\"}},\"required\":[\"path\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "path" );
					if ( v != null )
						return v;
					Object scale = args.get( "scale" );
					Double k = null;
					if ( scale != null )
					{
						try
						{
							k = Double.valueOf( Double.parseDouble( String.valueOf( scale ) ) );
						}
						catch ( NumberFormatException e )
						{
							return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "scale must be a number" );
						}
					}
					return McpDiagramSupport.diagramImage( str( args, "path" ), k, optInt( args, "maxWidth" ), optInt( args, "maxHeight" ) );
				} );

		catalog.register( "biouml_diagram_save",
				"Save a diagram to a DML file on disk (in targetDir, or the current directory). Returns the file path and size.",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"},\"targetDir\":{\"type\":\"string\"}},\"required\":[\"diagramPath\"]}",
				( ex, args ) -> McpDiagramSupport.save( str( args, "diagramPath" ), str( args, "targetDir" ) ) );

		catalog.register( "biouml_diagram_export",
				"Export a diagram to a file (DML by default). Returns the file path and size.",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"},\"targetDir\":{\"type\":\"string\"},\"format\":{\"type\":\"string\"}},\"required\":[\"diagramPath\"]}",
				( ex, args ) -> McpDiagramSupport.exportDiagram( str( args, "diagramPath" ), str( args, "targetDir" ), str( args, "format" ) ) );

		catalog.register( "biouml_diagram_import",
				"Import a diagram file (DML by default) into a target collection. Returns the new element's path.",
				"{\"type\":\"object\",\"properties\":{\"parentPath\":{\"type\":\"string\"},\"file\":{\"type\":\"string\"},\"format\":{\"type\":\"string\"},\"name\":{\"type\":\"string\"}},\"required\":[\"parentPath\",\"file\"]}",
				( ex, args ) -> McpDiagramSupport.importDiagram( str( args, "parentPath" ), str( args, "file" ), str( args, "format" ), str( args, "name" ) ) );
	}

	/**
	 * Event assignments in their order: a list of "variable = formula" strings, or (order as received)
	 * an object {variable: formula}.
	 */
	@SuppressWarnings( "unchecked" )
	static Map<String, String> assignments( Object value )
	{
		Map<String, String> result = new java.util.LinkedHashMap<String, String>();
		if ( value instanceof java.util.List )
		{
			for ( Object item : (java.util.List<Object>) value )
			{
				String text = item == null ? "" : String.valueOf( item );
				int eq = text.indexOf( '=' );
				if ( eq <= 0 || eq == text.length() - 1 || text.charAt( eq + 1 ) == '=' )
					throw new IllegalArgumentException( "assignment must look like 'variable = formula', got: " + text );
				result.put( text.substring( 0, eq ).trim(), text.substring( eq + 1 ).trim() );
			}
		}
		else if ( value instanceof Map )
		{
			for ( Map.Entry<Object, Object> e : ( (Map<Object, Object>) value ).entrySet() )
				result.put( String.valueOf( e.getKey() ), e.getValue() == null ? null : String.valueOf( e.getValue() ) );
		}
		else if ( value != null )
			throw new IllegalArgumentException( "assignments must be a list of 'variable = formula' strings" );
		return result;
	}

	private static String str( Map<String, Object> args, String key )
	{
		Object v = args == null ? null : args.get( key );
		return v == null ? null : String.valueOf( v );
	}

	private static Integer optInt( Map<String, Object> args, String key )
	{
		Object v = args == null ? null : args.get( key );
		if ( v == null )
			return null;
		try
		{
			return Integer.valueOf( Integer.parseInt( String.valueOf( v ).trim() ) );
		}
		catch ( Exception e )
		{
			return null;
		}
	}
}
