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

		catalog.register( "biouml_bsa_track_genome",
				"Report or set the genome of a track (genome build id + sequences collection) — the headless equivalent of the track's 'Genome (sequences collection)' property. Analyses that convert between builds rely on it to tell which build the positions are in (e.g. whether a track needs a liftover to hg38). VCF imports take the genome from the file's ##reference header when it names one; use this tool to check or correct it. path is the track; genome (optional) is a build id or alias ('hg19', 'GRCh37', 'hg38', ...) or the path of a sequences collection; omit it to only report. Returns {path, genomeId, sequences}, plus changed:true when set, plus the installed genomes (availableGenomes[{genomeId, name, sequences}]) when listGenomes is true or the track has no sequences collection.",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"genome\":{\"type\":\"string\"},\"listGenomes\":{\"type\":\"boolean\"}},\"required\":[\"path\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "path" );
					if ( v != null )
						return v;
					return McpBsaSupport.trackGenome( McpArgs.str( args, "path" ), McpArgs.str( args, "genome" ), McpArgs.bool( args, "listGenomes", false ) );
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
