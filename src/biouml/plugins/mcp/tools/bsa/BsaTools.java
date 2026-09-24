package biouml.plugins.mcp.tools.bsa;

import biouml.plugins.mcp.server.McpToolCatalog;
import biouml.plugins.mcp.support.McpArgs;
import biouml.plugins.mcp.support.McpBsaSupport;
import biouml.plugins.mcp.support.McpEnvelope;

/**
 * MCP tools for headless BSA (biosequence) "save selection" operations — the headless equivalents of
 * the web UI's BSA context-menu items for saving the selected sites / site-models into a new
 * collection or track. Each perform path is a pure repository API call with no UI.
 */
public final class BsaTools
{
	private BsaTools()
	{
	}

	public static void registerAll( McpToolCatalog catalog )
	{
		catalog.register( "biouml_bsa_save_selection",
				"Save the selected site models into a new SiteModelCollection — the headless equivalent of the web UI's 'Save selection' menu item (SiteModelTransformer.createCollection + clone + save). sourcePath is the collection holding the site models; modelNames is the list of site-model names to save; destinationPath is the full path of the new collection to create. Returns {created, count}.",
				"{\"type\":\"object\",\"properties\":{\"sourcePath\":{\"type\":\"string\"},\"modelNames\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}},\"destinationPath\":{\"type\":\"string\"}},\"required\":[\"sourcePath\",\"modelNames\",\"destinationPath\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "sourcePath" );
					if ( v != null )
						return v;
					McpEnvelope vm = McpArgs.stringArray( args, "modelNames" );
					if ( vm != null )
						return vm;
					McpEnvelope vd = McpArgs.requiredString( args, "destinationPath" );
					if ( vd != null )
						return vd;
					return McpBsaSupport.saveSiteSelection( McpArgs.str( args, "sourcePath" ), McpArgs.strArray( args, "modelNames" ), McpArgs.str( args, "destinationPath" ) );
				} );

		catalog.register( "biouml_bsa_save_selection_track",
				"Save the selected sites into a new track — the headless equivalent of the web UI's 'Save selection as track' menu item (SqlTrack.createTrack + addSite + save). sourcePath is the source track; siteNames is the list of site names to save; destinationPath is the full path of the new track to create. Returns {created, count}.",
				"{\"type\":\"object\",\"properties\":{\"sourcePath\":{\"type\":\"string\"},\"siteNames\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}},\"destinationPath\":{\"type\":\"string\"}},\"required\":[\"sourcePath\",\"siteNames\",\"destinationPath\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "sourcePath" );
					if ( v != null )
						return v;
					McpEnvelope vs = McpArgs.stringArray( args, "siteNames" );
					if ( vs != null )
						return vs;
					McpEnvelope vd = McpArgs.requiredString( args, "destinationPath" );
					if ( vd != null )
						return vd;
					return McpBsaSupport.saveTrackSelection( McpArgs.str( args, "sourcePath" ), McpArgs.strArray( args, "siteNames" ), McpArgs.str( args, "destinationPath" ) );
				} );
	}
}
