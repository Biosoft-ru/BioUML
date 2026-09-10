package biouml.plugins.mcp.server;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import biouml.plugins.mcp.McpConstants;
import biouml.plugins.mcp.support.McpEnvelope;

/**
 * A self-contained MCP JSON-RPC dispatcher shared by the HTTP servlet and the in-process server.
 *
 * <p>It is deliberately NOT built on the MCP SDK's session machinery (which is in a mid-migration
 * state in 0.11.2 and whose servlet transports are {@code jakarta}-based). Instead it implements
 * the small stable subset of the MCP protocol the plugin needs — {@code initialize},
 * {@code notifications/initialized}, {@code tools/list}, {@code tools/call}, and {@code ping} — by
 * routing straight to the {@link McpToolCatalog} tool handlers. Because both transports use this
 * single dispatcher, the tool list and the error shape are guaranteed identical over HTTP and
 * in-process.</p>
 *
 * <p>Wire format: MCP JSON-RPC 2.0. Requests are {@code {jsonrpc:"2.0", id, method, params}};
 * responses are {@code {jsonrpc:"2.0", id, result}} or {@code {jsonrpc:"2.0", id, error}}.
 * Tool results are the BioUML JSON envelope ({@code {ok, data?, error?, code?}}) serialized as the
 * tool's text content, with {@code isError=true} when the envelope is an error — so no stack trace
 * ever reaches the client.</p>
 */
public class McpJsonRpcDispatcher
{
	/** MCP protocol version advertised by this server (streamable-HTTP, 2025-03-26). */
	public static final String PROTOCOL_VERSION = "2025-03-26";

	/** JSON-RPC error: method not found (unknown method). */
	public static final int ERROR_METHOD_NOT_FOUND = -32601;
	/** JSON-RPC error: invalid params. */
	public static final int ERROR_INVALID_PARAMS = -32602;
	/** JSON-RPC error: internal error (a handler threw). */
	public static final int ERROR_INTERNAL = -32603;

	private final McpToolCatalog catalog;
	private final ObjectMapper mapper = new ObjectMapper();

	public McpJsonRpcDispatcher( McpToolCatalog catalog )
	{
		this.catalog = catalog;
	}

	/**
	 * Dispatch one JSON-RPC request.
	 * @param request a parsed JSON-RPC request map (keys: jsonrpc, id, method, params)
	 * @return a JSON-RPC response map: {@code {jsonrpc, id, result}} for a response,
	 *         {@code {jsonrpc, id, error:{code,message}}} for a protocol error, or an empty map for
	 *         a notification (no {@code id} → no response).
	 */
	@SuppressWarnings( "unchecked" )
	public Map<String, Object> handle( Map<String, Object> request )
	{
		Object id = request.get( "id" );
		String method = request.get( "method" ) == null ? null : String.valueOf( request.get( "method" ) );
		boolean isNotification = id == null;

		if ( "initialize".equals( method ) )
			return response( id, initializeResult( request ) );
		if ( "notifications/initialized".equals( method ) )
			return isNotification ? emptyResponse() : response( id, new LinkedHashMap<String, Object>() );
		if ( "tools/list".equals( method ) )
			return response( id, toolsListResult() );
		if ( "tools/call".equals( method ) )
		{
			Map<String, Object> params = params( request );
			String name = params.get( "name" ) == null ? null : String.valueOf( params.get( "name" ) );
			if ( findTool( name ) == null )
			{
				if ( isNotification )
					return emptyResponse();
				return errorResponse( id, ERROR_METHOD_NOT_FOUND, "unknown tool: " + name );
			}
			return response( id, toolsCallResult( request ) );
		}
		if ( "ping".equals( method ) )
			return response( id, new LinkedHashMap<String, Object>() );
		// Unknown method → JSON-RPC method-not-found.
		if ( isNotification )
			return emptyResponse();
		return errorResponse( id, ERROR_METHOD_NOT_FOUND, "method not found: " + method );
	}

	/**
	 * The {@code result} payload for an {@code initialize} request: negotiated protocol version,
	 * server info, and capabilities.
	 */
	private Object initializeResult( Map<String, Object> request )
	{
		Map<String, Object> params = params( request );
		String clientVersion = params.get( "protocolVersion" ) == null ? null : String.valueOf( params.get( "protocolVersion" ) );
		// Advertise the server's protocol version (the client may request a different one; we accept
		// and negotiate back our own, per the MCP spec).
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put( "protocolVersion", PROTOCOL_VERSION );
		Map<String, Object> serverInfo = new LinkedHashMap<String, Object>();
		serverInfo.put( "name", McpConstants.SERVER_NAME );
		serverInfo.put( "version", McpConstants.SERVER_VERSION );
		result.put( "serverInfo", serverInfo );
		Map<String, Object> caps = new LinkedHashMap<String, Object>();
		Map<String, Object> toolsCap = new LinkedHashMap<String, Object>();
		toolsCap.put( "listChanged", Boolean.FALSE );
		caps.put( "tools", toolsCap );
		result.put( "capabilities", caps );
		if ( clientVersion != null )
			result.put( "instructions", "BioUML MCP server. Protocol " + PROTOCOL_VERSION + " (client requested " + clientVersion + ")." );
		return result;
	}

	/** The {@code result} payload for a {@code tools/list} request. */
	private Object toolsListResult()
	{
		List<Map<String, Object>> tools = new java.util.ArrayList<Map<String, Object>>();
		for ( McpToolCatalog.Tool tool : catalog.getTools() )
		{
			Map<String, Object> t = new LinkedHashMap<String, Object>();
			t.put( "name", tool.name );
			t.put( "description", tool.description );
			// inputSchema is delivered as the parsed JSON-Schema object (the MCP wire form).
			try
			{
				t.put( "inputSchema", mapper.readValue( tool.inputSchema, Object.class ) );
			}
			catch ( Exception e )
			{
				t.put( "inputSchema", new LinkedHashMap<String, Object>() {{ put( "type", "object" ); }} );
			}
			tools.add( t );
		}
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put( "tools", tools );
		return result;
	}

	/** The {@code result} payload for a {@code tools/call} request. The tool must exist (checked by the caller). */
	@SuppressWarnings( "unchecked" )
	private Object toolsCallResult( Map<String, Object> request )
	{
		Map<String, Object> params = params( request );
		String name = params.get( "name" ) == null ? null : String.valueOf( params.get( "name" ) );
		Map<String, Object> args = params.get( "arguments" ) instanceof Map ? (Map<String, Object>) params.get( "arguments" ) : new LinkedHashMap<String, Object>();

		McpToolCatalog.Tool tool = findTool( name );

		McpEnvelope env;
		try
		{
			env = tool.handler.handle( null, args );
		}
		catch ( Exception e )
		{
			// Never leak a stack trace — a structured internal error.
			env = McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"unexpected error in tool " + tool.name + ": " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
		try
		{
			String json = mapper.writeValueAsString( env.toMap() );
			Map<String, Object> result = new LinkedHashMap<String, Object>();
			Map<String, Object> content = new LinkedHashMap<String, Object>();
			content.put( "type", "text" );
			content.put( "text", json );
			result.put( "content", java.util.Collections.singletonList( content ) );
			result.put( "isError", Boolean.valueOf( !env.isOk() ) );
			return result;
		}
		catch ( Exception e )
		{
			return null;
		}
	}

	private McpToolCatalog.Tool findTool( String name )
	{
		if ( name == null )
			return null;
		for ( McpToolCatalog.Tool t : catalog.getTools() )
			if ( name.equals( t.name ) )
				return t;
		return null;
	}

	@SuppressWarnings( "unchecked" )
	private Map<String, Object> params( Map<String, Object> request )
	{
		Object p = request.get( "params" );
		return p instanceof Map ? (Map<String, Object>) p : new LinkedHashMap<String, Object>();
	}

	// ---------------------------------------------------------------- wire helpers

	private static Map<String, Object> response( Object id, Object result )
	{
		Map<String, Object> r = new LinkedHashMap<String, Object>();
		r.put( "jsonrpc", "2.0" );
		r.put( "id", id );
		r.put( "result", result );
		return r;
	}

	private static Map<String, Object> errorResponse( Object id, int code, String message )
	{
		Map<String, Object> r = new LinkedHashMap<String, Object>();
		r.put( "jsonrpc", "2.0" );
		r.put( "id", id );
		Map<String, Object> error = new LinkedHashMap<String, Object>();
		error.put( "code", Integer.valueOf( code ) );
		error.put( "message", message );
		r.put( "error", error );
		return r;
	}

	private static Map<String, Object> emptyResponse()
	{
		return new LinkedHashMap<String, Object>();
	}

	/**
	 * Serialize a dispatch response to a JSON string (for the HTTP transport).
	 */
	public String toJson( Map<String, Object> response )
	{
		try
		{
			return mapper.writeValueAsString( response );
		}
		catch ( Exception e )
		{
			return "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":" + ERROR_INTERNAL + ",\"message\":\"serialization error\"}}";
		}
	}
}
