package biouml.plugins.mcp._test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import biouml.plugins.mcp.server.McpServerFactory;
import biouml.plugins.mcp.server.McpToolCatalog;

/**
 * Phase-6 documentation test: renders the live tool catalog to a markdown table and asserts that
 * the tool reference table in both READMEs has the same row count as the live catalog — so the
 * table cannot silently drift from the code.
 *
 * <p>The renderer is {@link #render(McpToolCatalog)}; it is intentionally plain (no build-tooling
 * dependency) so the test is the single source of truth for the table shape.</p>
 */
public class McpReadmeTableTest extends TestCase
{
	/**
	 * Render the catalog to a markdown table (one row per tool).
	 */
	public static String render( McpToolCatalog catalog )
	{
		List<McpToolCatalog.Tool> tools = catalog.getTools();
		StringBuilder sb = new StringBuilder();
		sb.append( "| Tool | Description |\n" );
		sb.append( "|------|-------------|\n" );
		for ( McpToolCatalog.Tool t : tools )
			sb.append( "| `" ).append( t.name ).append( "` | " ).append( escape( t.description ) ).append( " |\n" );
		return sb.toString();
	}

	private static String escape( String s )
	{
		return s == null ? "" : s.replace( "\n", " " ).replace( "|", "\\|" ).trim();
	}

	/**
	 * The tool-table row count in a README == the live catalog tool count.
	 */
	public void testReadmeToolTableMatchesCatalog() throws Exception
	{
		McpToolCatalog catalog = McpServerFactory.createCatalog();
		int liveCount = catalog.getTools().size();
		assertTrue( "catalog is non-empty, was " + liveCount, liveCount > 0 );

		String src = "..";
		File repoRoot = new File( src ).getCanonicalFile();
		String[] readmePaths = {
			repoRoot.getPath() + "/src/biouml/plugins/mcp/README.md",
			repoRoot.getPath() + "/plugconfig/biouml.plugins.mcp/README.md"
		};
		for ( String path : readmePaths )
		{
			File readme = new File( path );
			assertTrue( "README exists: " + path, readme.exists() );
			byte[] bytes = Files.readAllBytes( readme.toPath() );
			String content = new String( bytes, StandardCharsets.UTF_8 );
			int rows = countToolTableRows( content );
			assertEquals( "README tool table row count (" + path + ") == live catalog count (" + liveCount + ")",
					liveCount, rows );
		}
	}

	/**
	 * Count the markdown table rows that are tool rows (a cell containing `biouml_...`). The header
	 * and separator rows are not tool rows, so this counts exactly the tools documented.
	 */
	static int countToolTableRows( String content )
	{
		List<String> rows = new ArrayList<String>();
		for ( String line : content.split( "\n" ) )
		{
			String t = line.trim();
			if ( t.startsWith( "|" ) && t.contains( "biouml_" ) && !t.contains( "------" ) )
				rows.add( t );
		}
		return rows.size();
	}
}
