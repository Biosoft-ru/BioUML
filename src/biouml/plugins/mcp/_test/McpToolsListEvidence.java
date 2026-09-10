package biouml.plugins.mcp._test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import biouml.plugins.mcp.server.McpToolCatalog;
import biouml.plugins.mcp.tools.repo.RepoTools;

/**
 * Evidence runner (NOT a test — no JUnit). Registers the phase-2 repository tools and prints the
 * in-process equivalent of an MCP {@code tools/list} result, so the phase-2 spec's
 * "tool list printed" acceptance criterion has a transcript artifact.
 *
 * <p>Run: java -cp <full classpath> biouml.plugins.mcp._test.McpToolsListEvidence</p>
 */
public final class McpToolsListEvidence
{
	private McpToolsListEvidence()
	{
	}

	public static void main( String[] args ) throws Exception
	{
		McpToolCatalog catalog = new McpToolCatalog();
		RepoTools.registerAll( catalog );

		List<McpToolCatalog.Tool> tools = catalog.getTools();
		List<Map<String, Object>> out = new java.util.ArrayList<Map<String, Object>>();
		for ( McpToolCatalog.Tool t : tools )
		{
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "name", t.name );
			m.put( "description", t.description );
			m.put( "inputSchema", t.inputSchema );
			out.add( m );
		}
		ObjectMapper mapper = new ObjectMapper();
		System.out.println( "tools/list — count=" + tools.size() );
		System.out.println( mapper.writerWithDefaultPrettyPrinter().writeValueAsString( out ) );
	}
}
