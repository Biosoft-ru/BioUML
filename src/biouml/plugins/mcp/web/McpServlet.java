package biouml.plugins.mcp.web;

import java.io.IOException;
import java.io.OutputStream;
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
 * {@link #init(String[])} at startup, then {@code service(String, Object, Map, OutputStream, Object)}
 * (the {@code Object}-typed response overload) per request, passing Tomcat's {@code HttpSession} in the
 * {@code session} slot and the request body (as the {@code sessionId} parameter value) in the
 * {@code params} map. The MCP protocol (raw JSON-RPC over {@code application/json} with its own HTTP
 * status codes) does not fit the form/multipart pipeline {@code ConnectionServlet} uses for the other
 * web providers, so this servlet writes its own response. The {@code JSESSIONID} is recovered from the
 * {@code HttpSession} reflectively so the class stays free of a compile-time servlet dependency.</p>
 *
 * <p><b>Auth is the standard BioUML login</b> — the same check {@code WebServicesServlet} performs.
 * The session is the Tomcat {@code JSESSIONID} (the one the {@code /biouml/web/login} flow created) or
 * an explicit {@code sessionId} query parameter, then validated with
 * {@link SecurityManager#isSessionDead} and {@link SecurityManager#getSessionUser}. There is
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
	 * Entry point invoked by {@code ConnectionServlet.executeQueryWithExtensionServlet} via the
	 * {@code service(String, Object, Map, OutputStream, Object)} overload.
	 *
	 * @param localAddress the request servlet path (e.g. {@code /mcp})
	 * @param session      Tomcat's {@code HttpSession} (carries the {@code JSESSIONID}); may be null
	 * @param params       the parsed request parameters; the raw JSON-RPC body is carried here under
	 *                     {@link SecurityManager#SESSION_ID}
	 * @param out          the response output stream (unused; the response is written via {@code respObj})
	 * @param respObj      the {@code HttpServletResponse}
	 * @return the content type (the reflection contract expects a non-null string)
	 */
	@SuppressWarnings( "unchecked" )
	public String service( String localAddress, Object session, Map params, OutputStream out, Object respObj )
	{
		Object resp = respObj;
		String path = localAddress == null ? "/mcp" : localAddress;
		String query = params == null ? null : String.valueOf( params.get( SecurityManager.SESSION_ID ) );
		String body = params == null || params.get( SecurityManager.SESSION_ID ) == null
				? "" : String.valueOf( params.get( SecurityManager.SESSION_ID ) );

		HandleResult r = handle( "POST", path, query, body, session );
		writeJson( resp, r.status, r.body );
		return "application/json";
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

	private void writeJson( Object resp, int status, String body )
	{
		try
		{
			java.lang.reflect.Method setStatus = resp.getClass().getMethod( "setStatus", int.class );
			setStatus.invoke( resp, status );
			resp.getClass().getMethod( "setContentType", String.class ).invoke( resp, "application/json; charset=UTF-8" );
			OutputStream os = (OutputStream) resp.getClass().getMethod( "getOutputStream" ).invoke( resp );
			os.write( body.getBytes( java.nio.charset.StandardCharsets.UTF_8 ) );
			os.flush();
		}
		catch ( IOException e )
		{
			log.log( Level.WARNING, "Client aborted while writing MCP response", e );
		}
		catch ( Exception e )
		{
			log.log( Level.SEVERE, "Failed to write MCP response", e );
		}
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
