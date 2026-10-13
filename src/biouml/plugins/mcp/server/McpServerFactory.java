package biouml.plugins.mcp.server;

import biouml.plugins.mcp.tools.analysis.AnalysisTools;
import biouml.plugins.mcp.tools.brain.BrainTools;
import biouml.plugins.mcp.tools.bsa.BsaTools;
import biouml.plugins.mcp.tools.diagram.DiagramEditTools;
import biouml.plugins.mcp.tools.diagram.DiagramTools;
import biouml.plugins.mcp.tools.project.ProjectTools;
import biouml.plugins.mcp.tools.repo.RepoTools;
import biouml.plugins.mcp.tools.simulation.SimulationTools;
import biouml.plugins.mcp.tools.table.TableTools;

/**
 * Assembles the complete MCP tool catalog (repository tools from phase 2, analysis/task tools from
 * phase 3, diagram tools from phase 4, and simulation tools from phase 4) into a single
 * {@link McpToolCatalog} and the {@link McpJsonRpcDispatcher} that routes JSON-RPC requests to it.
 *
 * <p>Both the HTTP servlet ({@code biouml.plugins.mcp.web.McpServlet}) and the in-process server
 * ({@code biouml.plugins.mcp.server.BioumlMcpServer}) obtain their dispatcher from here, which is
 * what guarantees the two transports expose an identical tool set.</p>
 */
public final class McpServerFactory
{
	private McpServerFactory()
	{
	}

	/** Build the full tool catalog with every registered MCP tool. */
	public static McpToolCatalog createCatalog()
	{
		McpToolCatalog catalog = new McpToolCatalog();
		RepoTools.registerAll( catalog );
		AnalysisTools.registerAll( catalog );
		DiagramTools.registerAll( catalog );
		DiagramEditTools.registerAll( catalog );
		SimulationTools.registerAll( catalog );
		ProjectTools.registerAll( catalog );
		TableTools.registerAll( catalog );
		BsaTools.registerAll( catalog );
		BrainTools.registerAll( catalog );
		return catalog;
	}

	/** Build a fresh dispatcher over the full catalog. */
	public static McpJsonRpcDispatcher createDispatcher()
	{
		return new McpJsonRpcDispatcher( createCatalog() );
	}
}
