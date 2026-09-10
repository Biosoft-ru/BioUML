package biouml.plugins.mcp.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import biouml.plugins.mcp.server.McpJsonRpcDispatcher;
import biouml.plugins.mcp.server.McpServerFactory;
import ru.biosoft.access.security.SecurityManager;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The MCP streamable-HTTP servlet, exposed at {@code /mcp}.
 *
 * <p>It is a plain {@link HttpServlet} (the repo is {@code javax.servlet}; the MCP SDK's own
 * servlet transports are {@code jakarta}-based and cannot be used under the Tomcat-6 container).
 * It speaks the MCP JSON-RPC protocol by reading the request body, dispatching it through the
 * shared {@link McpJsonRpcDispatcher} (the same one the in-process server uses, so the tool list
 * and error shape are identical), and writing the JSON response back.</p>
 *
 * <p>Auth reuses the BioUML web session: the request thread is bound to the session
 * ({@code JSESSIONID} cookie or an explicit {@code sessionId} parameter) and the call is refused
 * with HTTP 401 when there is no logged-in user. No new auth system is introduced.</p>
 *
 * <p>POST → JSON-RPC request/response (application/json). GET → 405. The protocol logic lives in
 * {@link #handle} (transport-agnostic), which the servlet's {@code doPost} delegates to and which
 * tests drive directly.</p>
 */
public class McpServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	/** The shared JSON-RPC dispatcher (one per servlet; the catalog is built in init). */
	private McpJsonRpcDispatcher dispatcher;
	private final ObjectMapper mapper = new ObjectMapper();

	@Override
	public void init() throws ServletException
	{
		super.init();
		this.dispatcher = McpServerFactory.createDispatcher();
	}

	/**
	 * For tests / direct use: build a dispatcher over the full catalog.
	 */
	public void initForTest()
	{
		this.dispatcher = McpServerFactory.createDispatcher();
	}

	@Override
	protected void doGet( HttpServletRequest req, HttpServletResponse resp ) throws IOException
	{
		// The MCP streamable-HTTP spec reserves GET for opening an SSE stream; this build serves
		// plain-JSON responses, so GET is not used.
		resp.setStatus( HttpServletResponse.SC_METHOD_NOT_ALLOWED );
		resp.setContentType( "application/json" );
		resp.getWriter().write( "{\"error\":\"method not allowed; use POST for MCP JSON-RPC\"}" );
		resp.getWriter().flush();
	}

	@Override
	protected void doPost( HttpServletRequest req, HttpServletResponse resp ) throws IOException
	{
		String body = readBody( req );
		String query = req.getQueryString();
		// Fold the JSESSIONID cookie into the query so the core's session resolution sees it.
		javax.servlet.http.HttpSession session = req.getSession( false );
		if ( session != null && session.getId() != null
				&& ( query == null || !query.contains( SecurityManager.SESSION_ID ) ) )
			query = SecurityManager.SESSION_ID + "=" + session.getId()
					+ ( query == null ? "" : "&" + query );
		HandleResult r = handle( "POST", req.getRequestURI(), query, null, body );
		resp.setStatus( r.status );
		resp.setContentType( "application/json; charset=UTF-8" );
		resp.getWriter().write( r.body );
		resp.getWriter().flush();
	}

	/**
	 * The transport-agnostic core: authenticate, dispatch the JSON-RPC body, and produce the
	 * response status + body. The servlet's {@code doPost} wraps this with the {@code ServletResponse};
	 * tests call this directly.
	 *
	 * @param method  the HTTP method (POST expected)
	 * @param path    the request path (e.g. {@code /mcp})
	 * @param query   the query string, or null
	 * @param headers request headers (case-insensitive lookup), or null
	 * @param body    the raw request body
	 * @return the HTTP status and the response body
	 */
	/**
	 * The transport-agnostic core: authenticate, dispatch the JSON-RPC body, and produce the
	 * response status + body. The servlet's {@code doPost} wraps this with the {@code ServletResponse};
	 * tests call this directly.
	 *
	 * @param method  the HTTP method (POST expected)
	 * @param path    the request path (e.g. {@code /mcp})
	 * @param query   the query string, or null
	 * @param headers request headers (case-insensitive lookup), or null
	 * @param body    the raw request body
	 * @return the HTTP status and the response body
	 */
	public HandleResult handle( String method, String path, String query, Map<String, String> headers, String body )
	{
		long start = System.currentTimeMillis();

		String sessionId = resolveSessionId( path, query, body );
		if ( sessionId == null )
			return new HandleResult( 401, unauthBody( "no session" ) );

		// Bind the thread to the session so SecurityManager calls resolve the caller's identity.
		try
		{
			SecurityManager.addThreadToSessionRecord( Thread.currentThread(), sessionId );
		}
		catch ( Exception e )
		{
			return new HandleResult( 401, unauthBody( "invalid session" ) );
		}
		// The BioUML "system" session is privileged (the server's own identity) — it carries no
		// user record, so getSessionUser() is null for it by design. Any other session must have a
		// logged-in user or the request is refused with 401 (reuses the existing web auth; no new
		// auth system).
		boolean systemSession = SecurityManager.SYSTEM_SESSION.equals( sessionId );
		String user = systemSession ? SecurityManager.SYSTEM_SESSION : null;
		if ( !systemSession )
		{
			try
			{
				user = SecurityManager.getSessionUser();
			}
			catch ( Exception e )
			{
				user = null;
			}
			if ( user == null )
				return new HandleResult( 401, unauthBody( "unauthenticated" ) );
		}

		if ( dispatcher == null )
			initForTest();

		Map<String, Object> request;
		try
		{
			request = parseBody( body );
		}
		catch ( Exception e )
		{
			return new HandleResult( 400, "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32700,\"message\":\"parse error: not valid JSON\"}}" );
		}

		String m = request.get( "method" ) == null ? null : String.valueOf( request.get( "method" ) );
		Map<String, Object> response = dispatcher.handle( request );
		String out = dispatcher.toJson( response );

		// Request logging: method, tool name, duration, status — never the params/response body.
		String tool = null;
		Object params = request.get( "params" );
		if ( params instanceof Map )
		{
			Object n = ( (Map<?, ?>) params ).get( "name" );
			tool = n == null ? null : String.valueOf( n );
		}
		long duration = System.currentTimeMillis() - start;
		java.util.logging.Logger.getLogger( "biouml.plugins.mcp" ).info(
				"MCP " + method + " " + path + " method=" + m + " tool=" + tool
						+ " user=" + user + " status=200 " + duration + "ms" );

		return new HandleResult( 200, out );
	}

	/** The HTTP status + body produced by {@link #handle}. */
	public static final class HandleResult
	{
		public final int status;
		public final String body;

		public HandleResult( int status, String body )
		{
			this.status = status;
			this.body = body;
		}
	}

	/**
	 * Resolve the session id from the query string: an explicit {@code sessionId} parameter wins
	 * (matching the rest of the BioUML web stack, which passes {@link SecurityManager#SESSION_ID}).
	 * The {@code JSESSIONID} cookie is read by the servlet's {@code doPost} and folded into the
	 * query before calling this (see the servlet wrapper), so this only needs the query.
	 */
	private String resolveSessionId( String path, String query, String body )
	{
		if ( query == null )
			return null;
		for ( String pair : query.split( "&" ) )
		{
			int idx = pair.indexOf( '=' );
			if ( idx > 0 && SecurityManager.SESSION_ID.equals( pair.substring( 0, idx ) ) )
				return pair.substring( idx + 1 );
		}
		return null;
	}

	private String unauthBody( String reason )
	{
		return "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":401,\"message\":\"unauthorized: " + reason
				+ " — a BioUML session is required\"}}";
	}

	@SuppressWarnings( "unchecked" )
	private Map<String, Object> parseBody( String body ) throws Exception
	{
		if ( body == null || body.trim().isEmpty() )
			return new LinkedHashMap<String, Object>();
		return (Map<String, Object>) mapper.readValue( body, Map.class );
	}

	private static String readBody( HttpServletRequest req )
	{
		StringBuilder sb = new StringBuilder();
		try (BufferedReader reader = new BufferedReader( new InputStreamReader( req.getInputStream(), StandardCharsets.UTF_8 ) ))
		{
			String line;
			while ( ( line = reader.readLine() ) != null )
				sb.append( line ).append( '\n' );
		}
		catch ( IOException e )
		{
			return "";
		}
		return sb.toString().trim();
	}
}
