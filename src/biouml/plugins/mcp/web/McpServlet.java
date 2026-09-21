package biouml.plugins.mcp.web;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import ru.biosoft.access.security.SecurityManager;

import biouml.plugins.mcp.server.McpJsonRpcDispatcher;
import biouml.plugins.mcp.server.McpServerFactory;

/**
 * The BioUML MCP (Model Context Protocol) HTTP endpoint.
 *
 * <p>Registered as an extension servlet (extension point {@code ru.biosoft.server.servlet}, prefix
 * {@code mcp}) inside the OSGi web application. The launcher servlet
 * ({@code biouml.launcher.BioUMLLauncher}) dispatches any sub-path by its first segment — {@code /mcp} →
 * this servlet — so the endpoint is reachable at {@code /biouml/mcp} with no separate
 * {@code <servlet-mapping>}.</p>
 *
 * <p>This class is <em>not</em> a {@code HttpServlet} subclass on purpose. It is instantiated by the
 * OSGi classloader (via the plugin's {@code Require-Bundle}), which may not resolve {@code
 * javax.servlet} the same way the webapp classloader does, so it deliberately has <em>no</em> hard
 * compile-time dependency on the servlet API. {@code ConnectionServlet} drives it purely reflectively:
 * {@link #init(String[])} at startup, then per request the
 * {@code service(String, Object, Map, OutputStream, Map)} overload, passing the servlet path, the
 * Tomcat {@code HttpSession} (the {@code JSESSIONID} the client carries), and the parsed request
 * parameters. The JSON-RPC request is sent as a {@code application/x-www-form-urlencoded} POST with the
 * body in the {@value #MCP_BODY_KEY} form field, so {@code ConnectionServlet}'s {@code getParameterMap}
 * delivers it in the params map (a raw {@code application/json} body would be discarded by form
 * parsing). The response body is returned as the method's {@code String} result, which
 * {@code ConnectionServlet} writes to the response stream.</p>
 *
 * <p><b>Auth is the standard BioUML login</b> — the same check {@code WebServicesServlet} performs. The
 * {@code JSESSIONID} the {@code /biouml/web/login} flow created is taken from the {@code HttpSession}
 * (the MCP client carries the cookie) or an explicit {@code sessionId} query parameter, then validated
 * with {@link SecurityManager#isSessionDead} and {@link SecurityManager#getSessionUser}. There is
 * <em>no</em> privileged {@code system} shortcut: the {@code system} session carries no user record by
 * design and is the server's internal identity, not an external credential. A request without a live,
 * logged-in session is refused with HTTP 401.</p>
 *
 * <p>Non-POST → 405. POST → MCP JSON-RPC (initialize / tools/list / tools/call).</p>
 */
public class McpServlet
{
	private static final Logger log = Logger.getLogger( McpServlet.class.getName() );

	private final ObjectMapper mapper = new ObjectMapper();
	private McpJsonRpcDispatcher dispatcher;

	/**
	 * Called by the servlet registry at server startup (mirrors {@code WebServicesServlet.init}).
	 * {@code args} are the repository root folders; building the dispatcher is cheap, so it happens here
	 * rather than on the first request.
	 */
	public void init( String[] args ) throws Exception
	{
		this.dispatcher = McpServerFactory.createDispatcher();
	}

	/**
	 * For tests / direct use: build a dispatcher over the full catalog without the registry.
	 */
	public void initForTest()
	{
		this.dispatcher = McpServerFactory.createDispatcher();
	}

	/**
	 * Entry point invoked by {@code ConnectionServlet.executeQueryWithExtensionServlet} (the
	 * {@code (String, Object, Map, OutputStream, Map)} overload). It passes the servlet path, the
	 * Tomcat {@code HttpSession} (the {@code JSESSIONID} the client carries), and the parsed request
	 * parameters. The JSON-RPC body arrives in the params map under {@link #MCP_BODY_KEY} (a form field)
	 * and the session under {@link SecurityManager#SESSION_ID}. We return the JSON-RPC response as the
	 * method's {@code String} result, which {@code ConnectionServlet} writes to the response stream.
	 *
	 * @param localAddress the request servlet path (e.g. {@code /mcp})
	 * @param session      Tomcat's {@code HttpSession} (carries the {@code JSESSIONID}); may be null
	 * @param params       the parsed request parameters; the body is under {@link #MCP_BODY_KEY}
	 * @param out          a response stream (unused — the body is returned as the String result)
	 * @param header       the header map (unused)
	 * @return the response body (the JSON-RPC response)
	 */
	public String service( String localAddress, Object session, Map params, java.io.OutputStream out, Map<String, String> header )
	{
		String path = localAddress == null ? "/mcp" : localAddress;
		// The session id and the JSON-RPC body arrive in the parsed params map under distinct keys:
		//   sessionId  -> the caller's session (the JSESSIONID, or an explicit sessionId param)
		//   mcpBody    -> the raw JSON-RPC request body (a form field, so getParameterMap parses it)
		// The JSESSIONID is also on the HttpSession (session slot); the params-map sessionId wins.
		String query = null;
		String body = "";
		if ( params != null )
		{
			Object sid = params.get( SecurityManager.SESSION_ID );
			if ( sid != null )
				query = SecurityManager.SESSION_ID + "=" + first( sid );
			Object b = params.get( MCP_BODY_KEY );
			if ( b != null )
				body = first( b );
		}

		HandleResult r = handle( "POST", path, query, body, session );

		// ConnectionServlet's (…, OutputStream, Map) branch contract: write the response bytes to the
		// `out` stream (it writes `out`'s bytes to the client) and RETURN the content type (it calls
		// resp.setContentType(returned) and copies `header` onto the response). The HTTP status is
		// carried in the header map.
		try
		{
			out.write( r.body.getBytes( java.nio.charset.StandardCharsets.UTF_8 ) );
			out.flush();
		}
		catch ( Exception e )
		{
			log.log( Level.WARNING, "Client aborted while writing MCP response", e );
		}
		if ( header != null )
			header.put( "X-MCP-Status", String.valueOf( r.status ) );
		return "application/json";
	}

	/**
	 * The form-field name under which the raw JSON-RPC request body is sent (and read back from the
	 * parsed params map). Kept distinct from {@link SecurityManager#SESSION_ID} so the session id and the
	 * body do not collide in the parsed-params map.
	 */
	public static final String MCP_BODY_KEY = "mcpBody";

	/**
	 * The params-map value for a form parameter is a {@code String[]}; return its first element as a
	 * string. Handles both array and scalar values.
	 */
	private static String first( Object value )
	{
		if ( value == null )
			return "";
		if ( value instanceof Object[] )
		{
			Object[] arr = (Object[]) value;
			return arr.length == 0 ? "" : String.valueOf( arr[ 0 ] );
		}
		return String.valueOf( value );
	}

	/**
	 * The transport-agnostic core (also driven directly by the test suite): authenticate, dispatch the
	 * JSON-RPC body, and produce the response status + body.
	 *
	 * @param method  the HTTP method (POST expected)
	 * @param path    the request path (e.g. {@code /mcp})
	 * @param query   the query string, or null
	 * @param body    the raw JSON-RPC request body
	 * @param session the caller's {@code HttpSession} (carries the {@code JSESSIONID}); may be null
	 * @return the HTTP status and the response body
	 */
	public HandleResult handle( String method, String path, String query, String body, Object session )
	{
		long start = System.currentTimeMillis();

		String sessionId = resolveSessionId( query, session );
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

		// Standard BioUML auth (the same check WebServicesServlet performs): the session must be alive
		// and carry a logged-in user. There is no privileged "system" shortcut for external requests —
		// the system session has no user record by design.
		String user;
		try
		{
			if ( SecurityManager.isSessionDead( sessionId ) )
				return new HandleResult( 401, unauthBody( "invalid session" ) );
			user = SecurityManager.getSessionUser();
		}
		catch ( Exception e )
		{
			user = null;
		}
		if ( user == null )
			return new HandleResult( 401, unauthBody( "unauthenticated" ) );

		if ( dispatcher == null )
			initForTest();

		Map<String, Object> request;
		try
		{
			request = parseBody( body );
		}
		catch ( Exception e )
		{
			return new HandleResult( 400,
					"{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32700,\"message\":\"parse error: not valid JSON\"}}" );
		}

		String m = request.get( "method" ) == null ? null : String.valueOf( request.get( "method" ) );
		Map<String, Object> response = dispatcher.handle( request );
		String out = dispatcher.toJson( response );

		// Request logging: method, tool name, user, duration — never the params/response body.
		String tool = null;
		Object params = request.get( "params" );
		if ( params instanceof Map )
		{
			Object n = ( (Map<?, ?>) params ).get( "name" );
			tool = n == null ? null : String.valueOf( n );
		}
		long duration = System.currentTimeMillis() - start;
		log.info( "MCP " + method + " " + path + " method=" + m + " tool=" + tool + " user=" + user + " status=200 " + duration + "ms" );

		return new HandleResult( 200, out );
	}

	/**
	 * Resolve the session id: an explicit {@code sessionId} query parameter wins (matching the rest of
	 * the BioUML web stack, which passes {@link SecurityManager#SESSION_ID}); otherwise the
	 * {@code JSESSIONID} from the caller's {@code HttpSession}. The {@code HttpSession} is read
	 * reflectively so this class has no compile-time servlet-API dependency.
	 */
	private String resolveSessionId( String query, Object session )
	{
		if ( query != null )
		{
			for ( String pair : query.split( "&" ) )
			{
				int idx = pair.indexOf( '=' );
				if ( idx > 0 && SecurityManager.SESSION_ID.equals( pair.substring( 0, idx ) ) )
					return pair.substring( idx + 1 );
			}
		}
		if ( session != null )
		{
			try
			{
				Object id = session.getClass().getMethod( "getId" ).invoke( session );
				if ( id != null && !id.toString().isEmpty() )
					return id.toString();
			}
			catch ( Exception ignore )
			{
				// not a session object — no JSESSIONID to fall back to
			}
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
}
