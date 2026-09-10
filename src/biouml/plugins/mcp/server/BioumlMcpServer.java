package biouml.plugins.mcp.server;

import java.util.Map;

/**
 * The in-process MCP server: the same MCP JSON-RPC protocol as the HTTP servlet, but with no
 * transport and no auth — for in-JVM agents (e.g. a future geneXplain assistant running inside
 * OSGi) that can call the platform directly.
 *
 * <p>It delegates to the exact same {@link McpJsonRpcDispatcher} the servlet uses (built by
 * {@link McpServerFactory}), so the tool set, the initialize response, and the error shape are
 * byte-for-byte identical to the HTTP transport.</p>
 *
 * <p>Usage:</p>
 * <pre>
 *   BioumlMcpServer server = BioumlMcpServer.create();
 *   String init = server.handle( parse( "{jsonrpc:2.0,id:1,method:initialize,params:{...}}" ) );
 *   String list = server.handle( parse( "{jsonrpc:2.0,id:2,method:tools/list,params:{}}" ) );
 * </pre>
 */
public final class BioumlMcpServer
{
	private final McpJsonRpcDispatcher dispatcher;

	private BioumlMcpServer( McpJsonRpcDispatcher dispatcher )
	{
		this.dispatcher = dispatcher;
	}

	/** Create a fresh in-process server with the full tool catalog. */
	public static BioumlMcpServer create()
	{
		return new BioumlMcpServer( McpServerFactory.createDispatcher() );
	}

	/**
	 * Handle one JSON-RPC request map and return the JSON response string (empty string for a
	 * notification).
	 */
	public String handle( Map<String, Object> request )
	{
		Map<String, Object> response = dispatcher.handle( request );
		if ( response.isEmpty() )
			return "";
		return dispatcher.toJson( response );
	}

	/**
	 * The raw dispatcher (exposed so callers can drive lower-level operations if needed).
	 */
	public McpJsonRpcDispatcher dispatcher()
	{
		return dispatcher;
	}
}
