package biouml.plugins.mcp.tools.table;

import biouml.plugins.mcp.server.McpToolCatalog;
import biouml.plugins.mcp.support.McpArgs;
import biouml.plugins.mcp.support.McpEnvelope;
import biouml.plugins.mcp.support.McpTableSupport;

/**
 * MCP tools for headless table operations — the headless equivalents of the web UI's table
 * context-menu items ("Add row", "Subset table", "Replace content"), each of whose perform path is a
 * pure {@code TableDataCollection} / {@code TableDataCollectionUtils} API call with no UI.
 *
 * <p>Every handler validates its arguments at the boundary ({@link McpArgs}) and returns structured
 * {@code invalid_params} / {@code not_found} / {@code path_escape} envelopes — never a raw stack
 * trace.</p>
 */
public final class TableTools
{
	private TableTools()
	{
	}

	public static void registerAll( McpToolCatalog catalog )
	{
		catalog.register( "biouml_table_add_row",
				"Add a new row to a table — the headless equivalent of the web UI's 'Add row' menu item (TableDataCollectionUtils.addRow + finalizeAddition + save). path is the table; name is the new row's name (a duplicate name is a clean error); the row is initialised with each column's default value.",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"name\":{\"type\":\"string\"}},\"required\":[\"path\",\"name\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "path" );
					if ( v != null )
						return v;
					McpEnvelope vn = McpArgs.requiredString( args, "name" );
					if ( vn != null )
						return vn;
					return McpTableSupport.addRow( McpArgs.str( args, "path" ), McpArgs.str( args, "name" ) );
				} );

		catalog.register( "biouml_table_replace",
				"Replace string content within a table's cells — the headless equivalent of the web UI's 'Replace content' menu item. path is the table; from is the search string, to is the replacement (both required; from==to or empty from is a clean error); exactMatch (default false) makes the match whole-cell instead of substring; selectionOnly limits the operation to rows listed in rows (omit to operate on the whole table). Returns the number of changed rows.",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"from\":{\"type\":\"string\"},\"to\":{\"type\":\"string\"},\"exactMatch\":{\"type\":\"boolean\"},\"selectionOnly\":{\"type\":\"boolean\"},\"rows\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}}},\"required\":[\"path\",\"from\",\"to\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "path" );
					if ( v != null )
						return v;
					McpEnvelope vf = McpArgs.requiredString( args, "from" );
					if ( vf != null )
						return vf;
					McpEnvelope vt = McpArgs.requiredString( args, "to" );
					if ( vt != null )
						return vt;
					McpEnvelope vb = McpArgs.boolArg( args, "exactMatch", false );
					if ( vb != null )
						return vb;
					McpEnvelope vs = McpArgs.boolArg( args, "selectionOnly", false );
					if ( vs != null )
						return vs;
					McpEnvelope vr = McpArgs.stringArray( args, "rows" );
					if ( vr != null )
						return vr;
					return McpTableSupport.replace( McpArgs.str( args, "path" ), McpArgs.str( args, "from" ), McpArgs.str( args, "to" ),
							McpArgs.bool( args, "exactMatch", false ), McpArgs.bool( args, "selectionOnly", false ),
							McpArgs.strArray( args, "rows" ) );
				} );

		catalog.register( "biouml_table_subset",
				"Create a subset (copy) of a table containing only the given rows — the headless equivalent of the web UI's 'Subset table' menu item (TableRowsExporter.exportTable). path is the source table; destinationPath is the full path of the new table to create (its parent must exist); rows is the list of row names to include. Returns the new table's path.",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"destinationPath\":{\"type\":\"string\"},\"rows\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}}},\"required\":[\"path\",\"destinationPath\",\"rows\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "path" );
					if ( v != null )
						return v;
					McpEnvelope vd = McpArgs.requiredString( args, "destinationPath" );
					if ( vd != null )
						return vd;
					McpEnvelope vr = McpArgs.stringArray( args, "rows" );
					if ( vr != null )
						return vr;
					return McpTableSupport.subset( McpArgs.str( args, "path" ), McpArgs.str( args, "destinationPath" ), McpArgs.strArray( args, "rows" ) );
				} );
	}
}
