package ru.biosoft.server.mcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.security.SecurityManager;

/**
 * The webapp-visible MCP endpoint.
 *
 * <p>The MCP tool catalog and in-process server live in the OSGi bundle
 * {@code biouml.plugins.mcp} (class {@code biouml.plugins.mcp.server.BioumlMcpServer}). The
 * {@code bioumlweb} webapp's classloader cannot see {@code biouml.plugins.*} classes (they are loaded
 * by the OSGi framework, not the webapp), so a servlet mapped in the webapp's {@code web.xml} cannot
 * {@code extends} or {@code import} them — that is the cause of the {@code ClassNotFoundException} a
 * bundle-only servlet mapping produces.</p>
 *
 * <p>This servlet solves that: it lives in {@code ru.biosoft.server.*} (webapp-visible), and at
 * request time it loads the bundle's {@code BioumlMcpServer} via
 * {@link ClassLoading#loadClass(String, String)} and drives it reflectively. The webapp and the OSGi
 * bundle share only {@code java.*}, {@code ru.biosoft.*}, and Jackson — the MCP-specific types never
 * cross the classloader boundary, so there is no {@code ClassCastException}.</p>
 *
 * <p>Mapped at {@code /mcp} (see the webapp {@code web.xml}). Auth is the standard BioUML login flow:
 * the client authenticates via the web {@code /login} endpoint (username + password checked against
 * the {@code bioumlsupport2} user store) and then carries the resulting session on each request — as a
 * {@code sessionId} query parameter or the {@code JSESSIONID} cookie. The request thread is bound to
 * that session via {@link SecurityManager#addThreadToSessionRecord} so the data-collection
 * authorization performed inside the tool handlers runs as the logged-in user. A request without a
 * valid session is refused with HTTP 401. GET → 405. POST → MCP JSON-RPC
 * (initialize / tools/list / tools/call).</p>
 */
public class McpWebAppServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	/** The OSGi bundle id that holds the MCP server. */
	private static final String PLUGIN_ID = "biouml.plugins.mcp";
	/** The in-process MCP server class inside that bundle. */
	private static final String SERVER_CLASS = "biouml.plugins.mcp.server.BioumlMcpServer";
	/** How often (ms) to rebuild the in-process server so catalog changes are picked up. */
	private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

	private static final AtomicLong lastBuild = new AtomicLong( 0L );
	private static volatile Object cachedServer;
	private static volatile Method cachedHandle;
	private static final ObjectMapper mapper = new ObjectMapper();

	/**
	 * For tests / direct use: bypass the OSGi reflective load and point the servlet at an in-process
	 * {@link BioumlMcpServer} so the protocol core ({@link #handle}) can be exercised without a running
	 * OSGi framework. The reflective path ({@link #getServer}) is still what production uses.
	 */
	public void initForTest() throws Exception
	{
		cachedServer = loadInProcessServer();
		lastBuild.set( System.currentTimeMillis() );
	}

	/**
	 * Build the in-process server for tests. Returns an object exposing
	 * {@code String handle( Map )}; production uses {@code BioumlMcpServer.create()} reflectively.
	 */
	@SuppressWarnings( "unchecked" )
	private static Object loadInProcessServer() throws Exception
	{
		// In test mode the OSGi bundle is not running, so load the class from the classpath and call
		// its static create() factory directly. This is the same class the reflective production path
		// drives — only the loading mechanism differs.
		Class<?> serverClass = Class.forName( SERVER_CLASS );
		java.lang.reflect.Method create = serverClass.getMethod( "create" );
		return create.invoke( null );
	}

	@Override
	protected void doGet( HttpServletRequest req, HttpServletResponse resp ) throws IOException
	{
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
		// Fold the JSESSIONID cookie into the query so session resolution sees it.
		javax.servlet.http.HttpSession session = req.getSession( false );
		if ( session != null && session.getId() != null
				&& ( query == null || !query.contains( SecurityManager.SESSION_ID ) ) )
			query = SecurityManager.SESSION_ID + "=" + session.getId()
					+ ( query == null ? "" : "&" + query );

		HandleResult r = handle( "POST", req.getRequestURI(), query, body );
		resp.setStatus( r.status );
		resp.setContentType( "application/json; charset=UTF-8" );
		try
		{
			resp.getWriter().write( r.body );
			resp.getWriter().flush();
		}
		catch ( IOException e )
		{
			// client went away
		}
	}

	/**
	 * The transport-agnostic core (also exposed for tests, which drive it directly rather than
	 * stubbing {@code HttpServletRequest}). Authenticates, loads the OSGi MCP server, dispatches the
	 * JSON-RPC body, and produces the response status + body.
	 */
	public HandleResult handle( String method, String path, String query, String body )
	{
		long start = System.currentTimeMillis();

		String sessionId = resolveSessionId( query );
		if ( sessionId == null )
			return new HandleResult( 401, unauthBody( "no session" ) );

		try
		{
			SecurityManager.addThreadToSessionRecord( Thread.currentThread(), sessionId );
		}
		catch ( Exception e )
		{
			return new HandleResult( 401, unauthBody( "invalid session" ) );
		}

		// Standard BioUML auth (same check WebServicesServlet performs): the session must be alive
		// and carry a logged-in user. The session was created by the web /login flow (username +
		// password against the bioumlsupport2 user store); there is no privileged "system" shortcut
		// for external requests.
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

		Object request;
		try
		{
			request = parseBody( body );
		}
		catch ( Exception e )
		{
			return new HandleResult( 400, "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32700,\"message\":\"parse error: not valid JSON\"}}" );
		}

		// Load + drive the OSGi in-process MCP server (reflectively — the webapp cannot import it).
		String out;
		try
		{
			Object server = getServer();
			Method handle = server.getClass().getMethod( "handle", Map.class );
			Object response = handle.invoke( server, request );
			out = response == null ? "" : response.toString();
		}
		catch ( Exception e )
		{
			String msg = e.getCause() == null ? String.valueOf( e ) : String.valueOf( e.getCause() );
			return new HandleResult( 500,
					"{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32603,\"message\":\""
							+ jsonEscape( "internal error: " + msg ) + "\"}}" );
		}

		// Request logging: method + path + duration + status (no PII, no request/response bodies).
		long duration = System.currentTimeMillis() - start;
		java.util.logging.Logger.getLogger( "ru.biosoft.server.mcp" ).info(
				"MCP " + method + " " + path + " status=200 " + duration + "ms" );

		return new HandleResult( 200, out );
	}

	/**
	 * Load the OSGi {@code BioumlMcpServer} (via {@link ClassLoading}) and get a shared instance,
	 * rebuilding it at most every {@link #CACHE_TTL_MS} so catalog changes are picked up.
	 */
	private static synchronized Object getServer() throws Exception
	{
		long now = System.currentTimeMillis();
		if ( cachedServer != null && now - lastBuild.get() < CACHE_TTL_MS )
			return cachedServer;

		Class<?> serverClass = ClassLoading.loadClass( SERVER_CLASS, PLUGIN_ID );
		// BioumlMcpServer.create() — static factory returning a fresh in-process server.
		Method create = serverClass.getMethod( "create" );
		Object server = create.invoke( null );
		cachedServer = server;
		lastBuild.set( now );
		return server;
	}

	private static String resolveSessionId( String query )
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

	private static String jsonEscape( String s )
	{
		if ( s == null )
			return "";
		return s.replace( "\\", "\\\\" ).replace( "\"", "\\\"" ).replace( "\n", " " ).replace( "\r", " " );
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
