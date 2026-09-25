package biouml.plugins.mcp._test;

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.fasterxml.jackson.databind.ObjectMapper;

import junit.framework.TestCase;

import biouml.plugins.mcp.server.BioumlMcpServer;
import biouml.plugins.mcp.web.McpServlet;
import biouml.plugins.mcp.web.McpServlet.HandleResult;
import ru.biosoft.access.security.SecurityManager;

/**
 * Phase-5 test for the MCP servlet.
 *
 * <p>The servlet's protocol logic lives in its transport-agnostic
 * {@link McpServlet#handle(String, String, String, Map, String)} method; the {@code doPost} wrapper
 * only reads the body, folds the {@code JSESSIONID} cookie into the query, and writes the result.
 * Two layers of verification:</p>
 * <ol>
 *   <li><b>Over real HTTP</b> — a JDK {@link ServerSocket} listener (mirroring the repo's own
 *   {@code TestHTTPServer} harness) accepts a raw request on the wire, reconstructs the
 *   method/path/query/body, routes it through the servlet's real {@link McpServlet#handle} core,
 *   and writes the HTTP status line + body back. The test asserts on the bytes the socket actually
 *   returned.</li>
 *   <li><b>Direct {@code handle()}</b> — the auth (401) and edge paths, driven without a socket: an
 *   unauthenticated session → 401, no session → 401, a deliberately-bad tool call → a structured
 *   JSON-RPC error with no stack trace, malformed JSON → 400, and the servlet's tool set equals the
 *   in-process one.</li>
 * </ol>
 *
 * <p>The repo has no servlet-container test fixture (its {@code javax.servlet} API version differs
 * from the build's), so the test drives the servlet's real protocol core ({@code handle}) rather than
 * implementing {@code HttpServletRequest}/{@code HttpServletResponse} stubs; the thin {@code doPost}
 * wrapper is a trivial body-read + write that the war's web.xml mapping exercises in production.</p>
 */
public class McpServletTest extends TestCase
{
	private final ObjectMapper mapper = new ObjectMapper();
	private McpServlet servlet;
	/** A live session with a logged-in user (created via anonymousLogin under a non-system id). */
	private static final String AUTH_SESSION = "mcp-servlet-auth-session";
	/** A session id with no logged-in user → unauthenticated. */
	private static final String UNAUTH_SESSION = "no-such-session-mcp-test";

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		// Create a real, live session bound to a user under a NON-system session id, mirroring what the
		// web /login flow produces. anonymousLogin() records a UserPermissions for the current thread's
		// session, making isSessionDead() false and getSessionUser() non-null for it. (The `system`
		// session is intentionally NOT used: it carries no user record by design and is not a valid
		// external credential.)
		// A real, live session with a NON-empty username: commonLogin (TestSecurityProvider accepts
		// everything) records a UserPermissions whose user is "alice". (anonymousLogin() would work too,
		// but its user is "" — fine for the MCP servlet's non-null check, not for identity-bearing flows.)
		// The static SecurityProvider is normally initialized by the suite before the MCP tests run;
		// when this class runs in isolation it is null, so install the lenient provider here.
		if ( SecurityManager.getSecurityProvider() == null )
		{
			java.lang.reflect.Field f = SecurityManager.class.getDeclaredField( "securityProvider" );
			f.setAccessible( true );
			f.set( null, new ru.biosoft.access.security.TestSecurityProvider() );
		}
		SecurityManager.addThreadToSessionRecord( Thread.currentThread(), AUTH_SESSION );
		SecurityManager.commonLogin( "alice", "pw", "127.0.0.1", null );
		servlet = new McpServlet();
		servlet.initForTest();
	}

	@Override
	protected void tearDown() throws Exception
	{
		SecurityManager.removeThreadFromSessionRecord();
		super.tearDown();
	}

	/**
	 * Call the servlet's real 5-arg {@code service} entry point (the path the live server takes): the
	 * session travels in the params map under {@code sessionId} and the JSON-RPC body under
	 * {@code mcpBody}, exactly as {@code ConnectionServlet.getParameterMap} delivers a form POST.
	 */
	private HandleResult post( String sessionId, String jsonBody ) throws Exception
	{
		SecurityManager.addThreadToSessionRecord( Thread.currentThread(), sessionId );
		Map<String, Object> params = new java.util.LinkedHashMap<String, Object>();
		params.put( SecurityManager.SESSION_ID, new String[] { sessionId } );
		params.put( McpServlet.MCP_BODY_KEY, new String[] { jsonBody } );
		return serviceRoundTrip( params );
	}

	/**
	 * Drive the connection-servlet entry point {@code service(String,Object,Map,OutputStream,Map)}
	 * (the path the live server takes): the session and body travel in the params map (as
	 * {@code ConnectionServlet.getParameterMap} delivers a form POST), the response body is written to
	 * the {@code out} stream, and the content type is returned. This mirrors
	 * {@code ConnectionServlet}'s {@code (…, OutputStream, Map)} branch exactly, so it guards the
	 * write-to-stream + return-content-type contract.
	 */
	private HandleResult serviceRoundTrip( Map<String, Object> params ) throws Exception
	{
		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		Map<String, String> header = new java.util.HashMap<String, String>();
		String contentType = servlet.service( "/mcp", null, params, out, header );
		assertEquals( "service() returns the content type", "application/json", contentType );
		String body = new String( out.toByteArray(), java.nio.charset.StandardCharsets.UTF_8 );
		int status = 200;
		if ( header.get( "X-MCP-Status" ) != null )
			status = Integer.parseInt( header.get( "X-MCP-Status" ) );
		return new HandleResult( status, body );
	}

	/**
	 * Direct service() round-trip: a tools/list request through the real 5-arg entry point must produce
	 * a non-empty JSON-RPC response written to the output stream (regression: the body was once
	 * returned as the method's String result, which {@code ConnectionServlet} discards in this branch).
	 */
	@SuppressWarnings( "unchecked" )
	public void testServiceReturnsJsonBody() throws Exception
	{
		// Bind the thread to the live logged-in session so SecurityManager resolves the user.
		SecurityManager.addThreadToSessionRecord( Thread.currentThread(), AUTH_SESSION );
		Map<String, Object> params = new java.util.LinkedHashMap<String, Object>();
		// getParameterMap delivers each form parameter as a String[]; mirror that here.
		params.put( SecurityManager.SESSION_ID, new String[] { AUTH_SESSION } );
		params.put( McpServlet.MCP_BODY_KEY,
				new String[] { "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}" } );

		HandleResult r = serviceRoundTrip( params );
		assertFalse( "response body is non-empty", r.body.isEmpty() );
		Map<String, Object> resp = parse( r.body );
		Map<String, Object> result = (Map<String, Object>) resp.get( "result" );
		assertNotNull( "tools/list result present via service() — body=" + r.body, result );
		java.util.List<Map<String, Object>> tools = (java.util.List<Map<String, Object>>) result.get( "tools" );
		assertTrue( "tools present via service()", tools != null && tools.size() >= 10 );
	}

	/**
	 * Every tool's {@code inputSchema} must be a JSON Schema with {@code type: "object"} on the wire.
	 * The no-argument tools register a bare "{}" schema; the dispatcher must normalize that to
	 * {@code {"type":"object"}} or strict MCP clients (Claude Code, etc.) reject the whole
	 * tools/list result with "inputSchema.type: Invalid input".
	 */
	@SuppressWarnings( "unchecked" )
	public void testAllToolsInputSchemaHasObjectType() throws Exception
	{
		SecurityManager.addThreadToSessionRecord( Thread.currentThread(), AUTH_SESSION );
		Map<String, Object> params = new java.util.LinkedHashMap<String, Object>();
		params.put( SecurityManager.SESSION_ID, new String[] { AUTH_SESSION } );
		params.put( McpServlet.MCP_BODY_KEY,
				new String[] { "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}" } );

		HandleResult r = serviceRoundTrip( params );
		Map<String, Object> result = (Map<String, Object>) parse( r.body ).get( "result" );
		assertNotNull( "tools/list result present", result );
		List<Map<String, Object>> tools = (List<Map<String, Object>>) result.get( "tools" );
		assertNotNull( "tools present", tools );
		for ( Map<String, Object> t : tools )
		{
			Object schemaObj = t.get( "inputSchema" );
			assertTrue( "inputSchema is an object for " + t.get( "name" ), schemaObj instanceof Map );
			Map<String, Object> schema = (Map<String, Object>) schemaObj;
			assertEquals( "inputSchema.type must be 'object' for " + t.get( "name" ),
					"object", schema.get( "type" ) );
		}
	}

	// =================================================================== token auth (Authorization header)

	/**
	 * Build an {@code Authorization: Basic <b64>} header from a raw {@code user::token:uuid} payload —
	 * the same shape the BioStore token flow produces (decoded, {@code username::token:<uuid>}).
	 */
	private static String basicAuth( String username, String password )
	{
		// The Basic payload is `user:pass` (split on the first ':'); the password is the verbatim BioStore
		// token, e.g. "token:<uuid>". (The live token starts with a colon, so the full string is
		// "user::token:<uuid>"; the split still yields the whole token as the password.)
		return "Basic " + java.util.Base64.getEncoder().encodeToString(
				( username + ":" + password ).getBytes( StandardCharsets.UTF_8 ) );
	}

	/**
	 * Drive the real 5-arg {@code service} entry point with an arbitrary params map (session + body +
	 * Authorization header + client IP, exactly as {@code ConnectionServlet.getParameterMap} delivers
	 * them) and return the HTTP status (from the {@code X-MCP-Status} header) + response body.
	 */
	private HandleResult serviceWith( Map<String, Object> params ) throws Exception
	{
		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		Map<String, String> header = new java.util.HashMap<String, String>();
		String contentType = servlet.service( "/mcp", null, params, out, header );
		assertEquals( "service() returns the content type", "application/json", contentType );
		String body = new String( out.toByteArray(), StandardCharsets.UTF_8 );
		int status = 200;
		if ( header.get( "X-MCP-Status" ) != null )
			status = Integer.parseInt( header.get( "X-MCP-Status" ) );
		return new HandleResult( status, body );
	}

	/** A params map with a raw JSON body (mcpRawBody) + a Basic Authorization header, no session. */
	private Map<String, Object> tokenParams( String username, String token, String jsonBody )
	{
		Map<String, Object> params = new java.util.LinkedHashMap<String, Object>();
		params.put( McpServlet.MCP_RAW_BODY_KEY, new String[] { jsonBody } );
		params.put( "Authorization", new String[] { basicAuth( username, token ) } );
		params.put( "Remote-address", new String[] { "127.0.0.1" } );
		return params;
	}

	/**
	 * A token whose {@code user::token} payload is well-formed must authenticate against a
	 * {@code SecurityProvider} that accepts it (the deployed BioStore provider), then dispatch the
	 * request — proving the header-credential path (no session cookie) works end-to-end.
	 */
	@SuppressWarnings( "unchecked" )
	public void testTokenAuthSuccess() throws Exception
	{
		setProvider( new TokenAcceptingProvider() );
		try
		{
			// user:pass where pass is the verbatim BioStore token (the live token itself starts with ":").
			Map<String, Object> params = tokenParams( "mcpuser", "token:62e63796-9cfc-41b5-be91-b9ebb21da098",
					"{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}" );
			HandleResult r = serviceWith( params );
			assertEquals( "token auth → 200 — body=" + r.body, 200, r.status );
			Map<String, Object> resp = parse( r.body );
			Map<String, Object> result = (Map<String, Object>) resp.get( "result" );
			assertNotNull( "tools/list result present via token auth", result );
			java.util.List<Map<String, Object>> tools = (java.util.List<Map<String, Object>>) result.get( "tools" );
			assertTrue( "tools present via token auth", tools != null && tools.size() >= 10 );
		}
		finally
		{
			restoreProvider();
		}
	}

	/**
	 * A Basic header whose decoded payload has no {@code :} separator is malformed → 401, regardless of
	 * whether a session is also present.
	 */
	public void testTokenAuthBadHeaderIs401() throws Exception
	{
		// "just-a-header" has no `user:pass` separator.
		String bad = "Basic " + java.util.Base64.getEncoder().encodeToString(
				"just-a-header".getBytes( StandardCharsets.UTF_8 ) );
		Map<String, Object> params = new java.util.LinkedHashMap<String, Object>();
		params.put( McpServlet.MCP_RAW_BODY_KEY,
				new String[] { "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}" } );
		params.put( "Authorization", new String[] { bad } );
		HandleResult r = serviceWith( params );
		assertEquals( "malformed Authorization header → 401", 401, r.status );
	}

	/**
	 * A well-formed token that the security provider rejects (returns a null permission) → 401. The
	 * session path must not be used as a fallback: with no session present the request is refused.
	 */
	public void testTokenAuthInvalidCredentialsIs401() throws Exception
	{
		setProvider( new TokenRejectingProvider() );
		try
		{
			Map<String, Object> params = tokenParams( "mcpuser", "token:00000000-0000-0000-0000-000000000000",
					"{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}" );
			HandleResult r = serviceWith( params );
			assertEquals( "provider-rejected token → 401", 401, r.status );
		}
		finally
		{
			restoreProvider();
		}
	}

	/** No Authorization header and no session → 401 (unchanged behavior). */
	public void testNoAuthNoSessionIs401() throws Exception
	{
		Map<String, Object> params = new java.util.LinkedHashMap<String, Object>();
		params.put( McpServlet.MCP_RAW_BODY_KEY,
				new String[] { "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}" } );
		HandleResult r = serviceWith( params );
		assertEquals( "no header, no session → 401", 401, r.status );
	}

	/**
	 * A raw JSON body (delivered under {@code mcpRawBody}) plus a live session → 200. Proves the
	 * {@code application/json} body path parses and dispatches when authenticated by the existing
	 * session (the form field is absent).
	 */
	public void testRawJsonBodyDispatch() throws Exception
	{
		Map<String, Object> params = new java.util.LinkedHashMap<String, Object>();
		params.put( SecurityManager.SESSION_ID, new String[] { AUTH_SESSION } );
		params.put( McpServlet.MCP_RAW_BODY_KEY,
				new String[] { "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}" } );
		HandleResult r = serviceWith( params );
		assertEquals( "raw JSON body + session → 200 — body=" + r.body, 200, r.status );
	}

	// =================================================================== bearer auth (MCP OAuth tokens)

	/** Mint an access token bound to the live test session (as /token would). */
	private String mintBearerToken()
	{
		String user = null;
		SecurityManager.addThreadToSessionRecord( Thread.currentThread(), AUTH_SESSION );
		try
		{
			user = SecurityManager.getSessionUser();
		}
		finally
		{
			SecurityManager.removeThreadFromSessionRecord();
		}
		return biouml.plugins.mcp.oauth.OAuthTokenStore
				.issueToken( AUTH_SESSION, "test-client", user == null ? "alice" : user ).value;
	}

	/**
	 * A Bearer token minted by the OAuth server authenticates and dispatches — the remote-client path
	 * (no JSESSIONID, no Basic header).
	 */
	@SuppressWarnings( "unchecked" )
	public void testBearerTokenAuthSuccess() throws Exception
	{
		biouml.plugins.mcp.oauth.OAuthTokenStore.clearForTest();
		Map<String, Object> params = new java.util.LinkedHashMap<String, Object>();
		params.put( McpServlet.MCP_RAW_BODY_KEY,
				new String[] { "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}" } );
		params.put( "Authorization", new String[] { "Bearer " + mintBearerToken() } );
		HandleResult r = serviceWith( params );
		assertEquals( "bearer token → 200 — body=" + r.body, 200, r.status );
		Map<String, Object> result = (Map<String, Object>) parse( r.body ).get( "result" );
		assertNotNull( "tools/list result via bearer auth", result );
	}

	/**
	 * An unknown/garbage Bearer token → 401, and the 401 response carries the OAuth discovery
	 * headers: {@code WWW-Authenticate: Bearer resource_metadata="…"} (when the issuer is
	 * determinable) and the reserved {@code Status} key (the real HTTP status on the wire).
	 */
	public void testBearerInvalidTokenIs401WithDiscovery() throws Exception
	{
		biouml.plugins.mcp.oauth.OAuthTokenStore.clearForTest();
		Map<String, Object> params = new java.util.LinkedHashMap<String, Object>();
		params.put( McpServlet.MCP_RAW_BODY_KEY,
				new String[] { "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}" } );
		params.put( "Authorization", new String[] { "Bearer mcp_at_garbage" } );
		params.put( "Host", new String[] { "biouml2test.biouml.org" } );

		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		Map<String, String> header = new java.util.HashMap<String, String>();
		String ct = servlet.service( "/mcp", null, params, out, header );
		assertEquals( "application/json", ct );
		assertEquals( "invalid bearer → 401", "401", header.get( "X-MCP-Status" ) );
		assertEquals( "reserved Status key set for ConnectionServlet", "401", header.get( "Status" ) );
		String www = header.get( "WWW-Authenticate" );
		assertNotNull( "WWW-Authenticate present", www );
		assertTrue( "challenges with Bearer", www.startsWith( "Bearer " ) );
		assertTrue( "points at the protected-resource metadata",
				www.contains( "resource_metadata=\"https://biouml2test.biouml.org/biouml/oauth/.well-known/oauth-protected-resource\"" ) );
	}

	/** No credentials at all → 401 with the reserved Status key (real 401 on the wire). */
	public void testNoCredsIs401WithStatusHeader() throws Exception
	{
		Map<String, Object> params = new java.util.LinkedHashMap<String, Object>();
		params.put( McpServlet.MCP_RAW_BODY_KEY,
				new String[] { "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}" } );
		params.put( "Host", new String[] { "biouml2test.biouml.org" } );

		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		Map<String, String> header = new java.util.HashMap<String, String>();
		servlet.service( "/mcp", null, params, out, header );
		assertEquals( "401", header.get( "X-MCP-Status" ) );
		assertEquals( "401", header.get( "Status" ) );
	}

	// =================================================================== CORS (browser clients)

	/**
	 * A browser preflight (OPTIONS carrying {@code Origin} + {@code Access-Control-Request-Method}) must
	 * be answered <em>before</em> auth with a 204 that echoes the {@code Origin} and allows the
	 * {@code Authorization} header — otherwise the real POST is never sent and the client fails with
	 * "Failed to fetch" (the llama.cpp / Open WebUI connection error).
	 */
	public void testCorsPreflightIs204BeforeAuth() throws Exception
	{
		// No session, no Authorization: a real MCP request here would 401, but a preflight must not.
		Map<String, Object> params = new java.util.LinkedHashMap<String, Object>();
		params.put( "Origin", new String[] { "http://llm.dote.ru:8080" } );
		params.put( "Access-Control-Request-Method", new String[] { "POST" } );
		params.put( "Access-Control-Request-Headers", new String[] { "authorization, content-type" } );

		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		Map<String, String> header = new java.util.HashMap<String, String>();
		String ct = servlet.service( "/mcp", null, params, out, header );
		assertEquals( "application/json", ct );
		assertEquals( "preflight → 204 (not a 401)", "204", header.get( "X-MCP-Status" ) );
		assertEquals( "reserved Status key = 204 (real on the wire)", "204", header.get( "Status" ) );
		// Echo the origin (never *), so the Authorization header is permitted by the browser.
		assertEquals( "Access-Control-Allow-Origin echoes the caller", "http://llm.dote.ru:8080",
				header.get( "Access-Control-Allow-Origin" ) );
		assertEquals( "credentials allowed", "true", header.get( "Access-Control-Allow-Credentials" ) );
		assertTrue( "Authorization is in the allow list",
				header.get( "Access-Control-Allow-Headers" ).contains( "Authorization" ) );
		assertTrue( "POST is allowed", header.get( "Access-Control-Allow-Methods" ).contains( "POST" ) );
	}

	/**
	 * A preflight that requests a custom MCP client header (e.g. {@code mcp-protocol-version}, which the
	 * streamable-HTTP client sends on {@code notifications/initialized}) must be allowed — the allowed
	 * headers echo whatever the browser asked for, not just the hard-coded MCP set. This is the exact
	 * header whose absence broke the llama.cpp connection after {@code initialize} succeeded.
	 */
	public void testCorsPreflightAllowsRequestedCustomHeader() throws Exception
	{
		Map<String, Object> params = new java.util.LinkedHashMap<String, Object>();
		params.put( "Origin", new String[] { "http://llm.dote.ru:8080" } );
		params.put( "Access-Control-Request-Method", new String[] { "POST" } );
		// The real llama.cpp client requests these on notifications/initialized.
		params.put( "Access-Control-Request-Headers",
				new String[] { "authorization, content-type, accept, mcp-protocol-version" } );

		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		Map<String, String> header = new java.util.HashMap<String, String>();
		servlet.service( "/mcp", null, params, out, header );
		assertEquals( "204", header.get( "X-MCP-Status" ) );
		String allow = header.get( "Access-Control-Allow-Headers" );
		assertNotNull( "allow-headers present", allow );
		// Case-insensitive containment: the browser compares header names case-insensitively.
		String allowLower = allow.toLowerCase( java.util.Locale.ROOT );
		assertTrue( "mcp-protocol-version is allowed — " + allow, allowLower.contains( "mcp-protocol-version" ) );
		assertTrue( "authorization still allowed — " + allow, allowLower.contains( "authorization" ) );
	}

	/** A hostile Access-Control-Request-Headers value (token junk, not a header name) is not echoed. */
	public void testCorsPreflightDoesNotEchoJunk() throws Exception
	{
		Map<String, Object> params = new java.util.LinkedHashMap<String, Object>();
		params.put( "Origin", new String[] { "https://x" } );
		params.put( "Access-Control-Request-Method", new String[] { "POST" } );
		params.put( "Access-Control-Request-Headers", new String[] { "authorization, not-a-header!?" } );

		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		Map<String, String> header = new java.util.HashMap<String, String>();
		servlet.service( "/mcp", null, params, out, header );
		String allow = header.get( "Access-Control-Allow-Headers" );
		assertFalse( "junk header name not echoed — " + allow, allow.contains( "not-a-header" ) );
	}

	/**
	 * A normal (non-preflight) request from a browser origin gets {@code Access-Control-Allow-Origin}
	 * echoed so the browser is allowed to read the response body. A request with <em>no</em> Origin
	 * (curl / Claude Code / in-process) gets none — CORS headers must not leak to non-browser clients.
	 */
	public void testCorsAllowOriginOnNormalResponse() throws Exception
	{
		SecurityManager.addThreadToSessionRecord( Thread.currentThread(), AUTH_SESSION );
		String json = "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}";

		// With an Origin → the header is echoed.
		Map<String, Object> withOrigin = new java.util.LinkedHashMap<String, Object>();
		withOrigin.put( SecurityManager.SESSION_ID, new String[] { AUTH_SESSION } );
		withOrigin.put( McpServlet.MCP_RAW_BODY_KEY, new String[] { json } );
		withOrigin.put( "Origin", new String[] { "https://app.example" } );
		java.io.ByteArrayOutputStream out1 = new java.io.ByteArrayOutputStream();
		Map<String, String> h1 = new java.util.HashMap<String, String>();
		servlet.service( "/mcp", null, withOrigin, out1, h1 );
		assertEquals( "200", h1.get( "X-MCP-Status" ) );
		assertEquals( "origin echoed on a normal response", "https://app.example",
				h1.get( "Access-Control-Allow-Origin" ) );

		// Without an Origin → no CORS header at all.
		Map<String, Object> noOrigin = new java.util.LinkedHashMap<String, Object>();
		noOrigin.put( SecurityManager.SESSION_ID, new String[] { AUTH_SESSION } );
		noOrigin.put( McpServlet.MCP_RAW_BODY_KEY, new String[] { json } );
		java.io.ByteArrayOutputStream out2 = new java.io.ByteArrayOutputStream();
		Map<String, String> h2 = new java.util.HashMap<String, String>();
		servlet.service( "/mcp", null, noOrigin, out2, h2 );
		assertEquals( "200", h2.get( "X-MCP-Status" ) );
		assertNull( "no Origin → no Access-Control-Allow-Origin", h2.get( "Access-Control-Allow-Origin" ) );
	}

	/** Swap the (private, static) SecurityProvider on SecurityManager for the duration of a test. */
	private void setProvider( ru.biosoft.access.security.SecurityProvider provider ) throws Exception
	{
		java.lang.reflect.Field f = SecurityManager.class.getDeclaredField( "securityProvider" );
		f.setAccessible( true );
		f.set( null, provider );
	}

	/** Restore a lenient TestSecurityProvider (the test default) after a token test. */
	private void restoreProvider() throws Exception
	{
		setProvider( new ru.biosoft.access.security.TestSecurityProvider() );
	}

	/** Provider that accepts any BioStore {@code :token:…} password (mirrors the deployed provider). */
	private static final class TokenAcceptingProvider extends ru.biosoft.access.security.TestSecurityProvider
	{
		@Override
		public ru.biosoft.access.security.UserPermissions authorize( String username, String password,
				String remoteAddress, String jwToken )
		{
			// The password is the verbatim BioStore credential, e.g. ":token:<uuid>" (itself starts with ':').
			if ( password != null && password.indexOf( "token:" ) >= 0 && username != null && !username.isEmpty() )
				return new ru.biosoft.access.security.UserPermissions( username, password, new String[] { "Server" },
						java.util.Collections.<String, Long>emptyMap() );
			return null;
		}
	}

	/** Provider that rejects every credential (authorize → null). */
	private static final class TokenRejectingProvider extends ru.biosoft.access.security.TestSecurityProvider
	{
		@Override
		public ru.biosoft.access.security.UserPermissions authorize( String username, String password,
				String remoteAddress, String jwToken )
		{
			return null;
		}
	}

	@SuppressWarnings( "unchecked" )
	private Map<String, Object> parse( String json ) throws Exception
	{
		return json == null || json.isEmpty() ? new java.util.LinkedHashMap<String, Object>()
				: (Map<String, Object>) mapper.readValue( json, Map.class );
	}

	@SuppressWarnings( "unchecked" )
	private List<String> toolNames( Map<String, Object> toolsListResp )
	{
		Map<String, Object> result = (Map<String, Object>) toolsListResp.get( "result" );
		List<Map<String, Object>> tools = (List<Map<String, Object>>) result.get( "tools" );
		List<String> names = new ArrayList<String>();
		for ( Map<String, Object> t : tools )
			names.add( (String) t.get( "name" ) );
		return names;
	}

	// =================================================================== over real HTTP

	/**
	 * Over a real socket: initialize → 200 + serverInfo, then tools/list → ≥10 tools.
	 */
	@SuppressWarnings( "unchecked" )
	public void testInitializeAndToolsListOverRealHttp() throws Exception
	{
		HttpMcpServer server = new HttpMcpServer( servlet );
		try
		{
			String init = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-03-26\",\"capabilities\":{},\"clientInfo\":{\"name\":\"http\",\"version\":\"0\"}}}";
			String list = "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}";

			// 1) initialize over the wire
			Map<String, Object> initResp = server.post( AUTH_SESSION, init );
			assertEquals( "initialize → 200 over HTTP", "200", initResp.get( "status" ).toString() );
			Map<String, Object> body = parse( (String) initResp.get( "body" ) );
			Map<String, Object> result = (Map<String, Object>) body.get( "result" );
			assertNotNull( "initialize result present", result );
			assertEquals( "protocol version", "2025-11-25", result.get( "protocolVersion" ) );
			Map<String, Object> info = (Map<String, Object>) result.get( "serverInfo" );
			assertEquals( "server name", "biouml", info.get( "name" ) );
			assertNotNull( "server version", info.get( "version" ) );

			// 2) tools/list over the wire
			Map<String, Object> listResp = server.post( AUTH_SESSION, list );
			assertEquals( "tools/list → 200 over HTTP", "200", listResp.get( "status" ).toString() );
			List<String> names = toolNames( parse( (String) listResp.get( "body" ) ) );
			assertTrue( "repo tool present", names.contains( "biouml_repo_collections" ) );
			assertTrue( "analysis tool present", names.contains( "biouml_analysis_list" ) );
			assertTrue( "diagram tool present", names.contains( "biouml_diagram_create" ) );
			assertTrue( "simulation tool present", names.contains( "biouml_simulation_start" ) );
			assertTrue( "≥10 tools, was " + names.size(), names.size() >= 10 );
		}
		finally
		{
			server.stop();
		}
	}

	/** Over a real socket: tools/call → the tool's JSON envelope. */
	@SuppressWarnings( "unchecked" )
	public void testToolsCallOverRealHttp() throws Exception
	{
		HttpMcpServer server = new HttpMcpServer( servlet );
		try
		{
			String call = "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"biouml_repo_collections\",\"arguments\":{}}}";

			Map<String, Object> callResp = server.post( AUTH_SESSION, call );
			assertEquals( "tools/call → 200 over HTTP", "200", callResp.get( "status" ).toString() );
			Map<String, Object> body = parse( (String) callResp.get( "body" ) );
			Map<String, Object> result = (Map<String, Object>) body.get( "result" );
			assertNotNull( "tools/call result present", result );
			List<Map<String, Object>> content = (List<Map<String, Object>>) result.get( "content" );
			assertTrue( "content present", content != null && !content.isEmpty() );
			String text = (String) content.get( 0 ).get( "text" );
			assertTrue( "tool result is the JSON envelope (ok field)", text.contains( "\"ok\"" ) );
			assertEquals( "not an error", Boolean.FALSE, result.get( "isError" ) );
		}
		finally
		{
			server.stop();
		}
	}

	/** Over a real socket: an unauthenticated session (no logged-in user) → 401 with an error body. */
	@SuppressWarnings( "unchecked" )
	public void testUnauthenticatedIs401OverRealHttp() throws Exception
	{
		HttpMcpServer server = new HttpMcpServer( servlet );
		try
		{
			String list = "{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/list\",\"params\":{}}";

			Map<String, Object> resp = server.post( UNAUTH_SESSION, list );
			assertEquals( "unauthenticated → 401 over HTTP", "401", resp.get( "status" ).toString() );
			Map<String, Object> body = parse( (String) resp.get( "body" ) );
			assertNotNull( "401 body has an error", body.get( "error" ) );
		}
		finally
		{
			server.stop();
		}
	}

	// =================================================================== direct handle()

	@SuppressWarnings( "unchecked" )
	public void testUnauthenticatedIs401() throws Exception
	{
		SecurityManager.addThreadToSessionRecord( Thread.currentThread(), UNAUTH_SESSION );
		HandleResult r = servlet.handle( "POST", "/mcp", SecurityManager.SESSION_ID + "=" + UNAUTH_SESSION,
				"{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/list\",\"params\":{}}", null );
		assertEquals( "unauthenticated → 401", 401, r.status );
		Map<String, Object> resp = parse( r.body );
		Map<String, Object> error = (Map<String, Object>) resp.get( "error" );
		assertNotNull( "401 body has an error", error );
		assertNotNull( "error has a message", error.get( "message" ) );
	}

	public void testNoSessionIs401() throws Exception
	{
		// No session at all (no sessionId in the query) → 401.
		SecurityManager.addThreadToSessionRecord( Thread.currentThread(), AUTH_SESSION );
		HandleResult r = servlet.handle( "POST", "/mcp", null,
				"{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"tools/list\",\"params\":{}}", null );
		assertEquals( "no session → 401", 401, r.status );
	}

	@SuppressWarnings( "unchecked" )
	public void testHttpToolListEqualsInProcess() throws Exception
	{
		// Acceptance #5: the HTTP (servlet) tool list is identical to the in-process one.
		HandleResult r = post( AUTH_SESSION, "{\"jsonrpc\":\"2.0\",\"id\":6,\"method\":\"tools/list\",\"params\":{}}" );
		List<String> servletNames = toolNames( parse( r.body ) );

		BioumlMcpServer inProcess = BioumlMcpServer.create();
		String list = inProcess.handle( parse( "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}" ) );
		List<String> inProcessNames = toolNames( parse( list ) );

		assertEquals( "servlet and in-process tool counts equal", inProcessNames.size(), servletNames.size() );
		assertEquals( "servlet and in-process tool sets equal",
				new HashSet<String>( inProcessNames ), new HashSet<String>( servletNames ) );
	}

	@SuppressWarnings( "unchecked" )
	public void testUnknownToolIsMethodNotFound() throws Exception
	{
		// Acceptance #10: a deliberately-bad tool call returns a structured JSON-RPC error, with no
		// stack trace leaked.
		HandleResult r = post( AUTH_SESSION, "{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"tools/call\",\"params\":{\"name\":\"biouml_no_such_tool\",\"arguments\":{}}}" );
		assertEquals( "unknown tool → 200 with a JSON-RPC error object", 200, r.status );
		Map<String, Object> resp = parse( r.body );
		Map<String, Object> error = (Map<String, Object>) resp.get( "error" );
		assertNotNull( "unknown tool → JSON-RPC error", error );
		assertEquals( "error code -32601", -32601, ( (Number) error.get( "code" ) ).intValue() );
		String msg = (String) error.get( "message" );
		assertTrue( "message has no stack trace", msg != null && !msg.contains( "at " ) && !msg.contains( "Exception" ) );
	}

	@SuppressWarnings( "unchecked" )
	public void testMalformedJsonIs400() throws Exception
	{
		// Drive handle() directly (the 5-arg service() returns only the body string, always 200 at the
		// transport level; the JSON-RPC error code carries the parse error).
		SecurityManager.addThreadToSessionRecord( Thread.currentThread(), AUTH_SESSION );
		HandleResult r = servlet.handle( "POST", "/mcp", SecurityManager.SESSION_ID + "=" + AUTH_SESSION,
				"this is not json", null );
		assertEquals( "malformed body → 400", 400, r.status );
		Map<String, Object> resp = parse( r.body );
		Map<String, Object> error = (Map<String, Object>) resp.get( "error" );
		assertNotNull( "400 body has an error", error );
	}

	// =================================================================== real-HTTP helper

	/**
	 * A minimal real-socket HTTP listener (mirroring the repo's own {@code TestHTTPServer} harness):
	 * it accepts a raw request on the wire, parses the request line + body, routes it through the
	 * servlet's real protocol core {@link McpServlet#handle}, and writes the HTTP status line + body
	 * back. The request thread is bound to the requested session so the servlet's
	 * {@code SecurityManager}-based auth resolves the caller's identity exactly as it does for a real
	 * request.
	 */
	private static final class HttpMcpServer
	{
		private final McpServlet servlet;
		private final ServerSocket serverSocket;
		private final int port;
		private Thread acceptor;
		private volatile boolean running = true;

		HttpMcpServer( McpServlet servlet ) throws Exception
		{
			this.servlet = servlet;
			Random random = new Random();
			int port = -1;
			ServerSocket sock = null;
			for ( int i = 0; i < 100; i++ )
			{
				int candidate = random.nextInt( 25000 - 20000 ) + 20000;
				try
				{
					sock = new ServerSocket( candidate, 50, InetAddress.getByName( "127.0.0.1" ) );
					port = candidate;
					break;
				}
				catch ( Exception ignore )
				{
					// port taken, retry
				}
			}
			if ( sock == null )
				throw new Exception( "Unable to start MCP HTTP test server on any port in 20000..25000" );
			this.port = port;
			this.serverSocket = sock;
			this.acceptor = new AcceptorThread();
			this.acceptor.setDaemon( true );
			this.acceptor.start();
		}

		/**
		 * Send one POST request over the wire (the JSON-RPC body as the {@code mcpBody} form field, the
		 * session as the {@code sessionId} query parameter) and return the parsed status + body.
		 */
		@SuppressWarnings( "unchecked" )
		Map<String, Object> post( String sessionId, String body ) throws Exception
		{
			// Form-encode the body exactly as ConnectionServlet.getParameterMap expects.
			String form = SecurityManager.SESSION_ID + "=" + urlEncode( sessionId )
					+ "&" + McpServlet.MCP_BODY_KEY + "=" + urlEncode( body );
			byte[] bodyBytes = form.getBytes( StandardCharsets.UTF_8 );
			String raw = "POST /mcp HTTP/1.1\r\n"
					+ "Host: 127.0.0.1:" + port + "\r\n"
					+ "Content-Type: application/x-www-form-urlencoded\r\n"
					+ "Content-Length: " + bodyBytes.length + "\r\n"
					+ "Connection: close\r\n"
					+ "\r\n"
					+ new String( bodyBytes, StandardCharsets.UTF_8 );

			try (Socket socket = new Socket( "127.0.0.1", port ))
			{
				socket.setSoTimeout( 20000 );
				OutputStream out = socket.getOutputStream();
				out.write( raw.getBytes( StandardCharsets.UTF_8 ) );
				out.flush();

				InputStream in = socket.getInputStream();
				ByteArrayAccumulator acc = new ByteArrayAccumulator();
				int b;
				while ( ( b = in.read() ) != -1 )
					acc.add( (byte) b );
				String text = new String( acc.bytes(), StandardCharsets.UTF_8 );

				String[] statusParts = text.split( "\r\n", 2 )[ 0 ].split( " " );
				String status = statusParts.length > 1 ? statusParts[ 1 ] : "?";
				int headerEnd = text.indexOf( "\r\n\r\n" );
				String responseBody = headerEnd >= 0 ? text.substring( headerEnd + 4 ) : "";

				Map<String, Object> result = new java.util.LinkedHashMap<String, Object>();
				result.put( "status", status );
				result.put( "body", responseBody );
				return result;
			}
		}

		void stop()
		{
			running = false;
			try
			{
				serverSocket.close();
			}
			catch ( Exception ignore )
			{
			}
		}

		/** Accepts connections and dispatches each raw request through the servlet. */
		private final class AcceptorThread extends Thread
		{
			AcceptorThread()
			{
				super( "MCP-HTTP-test-acceptor" );
			}

			@Override
			public void run()
			{
				while ( running )
				{
					try
					{
						Socket socket = serverSocket.accept();
						new RequestWorker( socket ).start();
					}
					catch ( Exception e )
					{
						if ( !running )
							return;
					}
				}
			}
		}

		/** Parses one raw HTTP request and routes it through the servlet's real handle(). */
		private final class RequestWorker extends Thread
		{
			private final Socket socket;

			RequestWorker( Socket socket )
			{
				super( "MCP-HTTP-test-worker" );
				this.socket = socket;
			}

			@Override
			public void run()
			{
				try
				{
					DataInputStream in = new DataInputStream( socket.getInputStream() );
					String raw = readHttpRequest( in );

					String[] lines = raw.split( "\r\n" );
					String requestLine = lines[ 0 ];
					String[] rl = requestLine.split( " " );
					String method = rl[ 0 ];
					String target = rl.length > 1 ? rl[ 1 ] : "/mcp";

					int qpos = target.indexOf( '?' );
					String path = qpos >= 0 ? target.substring( 0, qpos ) : target;
					String query = qpos >= 0 ? target.substring( qpos + 1 ) : null;

					// Body = everything after the first blank line (a form-encoded body).
					int bodyStart = raw.indexOf( "\r\n\r\n" );
					String formBody = bodyStart >= 0 ? raw.substring( bodyStart + 4 ) : "";

					// Decode the form fields: sessionId (auth) + mcpBody (the JSON-RPC request).
					String sessionId = urlDecode( formValue( formBody, SecurityManager.SESSION_ID ) );
					String jsonBody = urlDecode( formValue( formBody, McpServlet.MCP_BODY_KEY ) );
					if ( sessionId == null && query != null )
						sessionId = parseSessionId( query );
					if ( sessionId != null )
						SecurityManager.addThreadToSessionRecord( Thread.currentThread(), sessionId );

					McpServlet.HandleResult result =
						"POST".equalsIgnoreCase( method )
								? servlet.handle( method, path,
										sessionId == null ? null : SecurityManager.SESSION_ID + "=" + sessionId,
										jsonBody, null )
								: methodNotAllowed();

					// Write the HTTP response back.
					String statusText = result.status == 200 ? "OK"
							: result.status == 400 ? "Bad Request"
							: result.status == 401 ? "Unauthorized"
							: result.status == 405 ? "Method Not Allowed"
							: "Internal Server Error";
					byte[] bodyBytes = result.body.getBytes( StandardCharsets.UTF_8 );
					String resp = "HTTP/1.1 " + result.status + " " + statusText + "\r\n"
							+ "Content-Type: application/json\r\n"
							+ "Content-Length: " + bodyBytes.length + "\r\n"
							+ "Connection: close\r\n"
							+ "\r\n";
					OutputStream out = socket.getOutputStream();
					out.write( resp.getBytes( StandardCharsets.UTF_8 ) );
					out.write( bodyBytes );
					out.flush();
					socket.close();
				}
				catch ( Exception ignore )
				{
					// connection closed or parse error — the accept loop keeps going
				}
			}

			private McpServlet.HandleResult methodNotAllowed()
			{
				// Mirror doGet(): GET/other → 405.
				return new McpServlet.HandleResult( 405,
						"{\"error\":\"method not allowed; use POST for MCP JSON-RPC\"}" );
			}
		}
	}

	/**
	 * Read one HTTP request off the wire: the request line + headers (up to the first blank line)
	 * plus exactly {@code Content-Length} body bytes. Not reading to EOF is important: an HTTP/1.1
	 * client that keeps the connection open would otherwise make the read block until timeout.
	 */
	private static String readHttpRequest( DataInputStream in ) throws Exception
	{
		// 1) Read the header region byte-for-byte up to the first blank line (CRLF CRLF).
		ByteArrayAccumulator headerAcc = new ByteArrayAccumulator();
		StringBuilder headerText = new StringBuilder();
		int b;
		while ( ( b = in.read() ) != -1 )
		{
			char c = (char) b;
			headerAcc.add( (byte) b );
			headerText.append( c );
			int n = headerText.length();
			if ( n >= 4
					&& headerText.charAt( n - 4 ) == '\r'
					&& headerText.charAt( n - 3 ) == '\n'
					&& headerText.charAt( n - 2 ) == '\r'
					&& headerText.charAt( n - 1 ) == '\n' )
				break;
		}
		// 2) Pull Content-Length out of the header text.
		int contentLength = 0;
		String ht = headerText.toString();
		int clIdx = ht.indexOf( "Content-Length:" );
		if ( clIdx >= 0 )
		{
			int lineEnd = ht.indexOf( '\n', clIdx );
			String value = ht.substring( clIdx + "Content-Length:".length(), lineEnd < 0 ? ht.length() : lineEnd ).trim();
			try
			{
				contentLength = Integer.parseInt( value );
			}
			catch ( NumberFormatException ignore )
			{
				contentLength = 0;
			}
		}
		// 3) Read exactly Content-Length body bytes (not to EOF).
		String headers = new String( headerAcc.bytes(), StandardCharsets.UTF_8 );
		if ( contentLength > 0 )
		{
			byte[] bodyBytes = new byte[ contentLength ];
			in.readFully( bodyBytes );
			return headers + new String( bodyBytes, StandardCharsets.UTF_8 );
		}
		return headers;
	}

	/** URL-encode a string for a form body. */
	private static String urlEncode( String s )
	{
		try
		{
			return java.net.URLEncoder.encode( s, "UTF-8" );
		}
		catch ( Exception e )
		{
			return s;
		}
	}

	/** URL-decode a string from a form body. */
	private static String urlDecode( String s )
	{
		if ( s == null )
			return null;
		try
		{
			return java.net.URLDecoder.decode( s, "UTF-8" );
		}
		catch ( Exception e )
		{
			return s;
		}
	}

	/** Extract a single form-field value ({@code name=value&...}) from a form body, or null. */
	private static String formValue( String form, String name )
	{
		if ( form == null || form.isEmpty() )
			return null;
		for ( String pair : form.split( "&" ) )
		{
			int idx = pair.indexOf( '=' );
			if ( idx > 0 && pair.substring( 0, idx ).equals( name ) )
				return pair.substring( idx + 1 );
		}
		return null;
	}

	/** Extract the {@code sessionId=...} value from a query string, or null. */
	private static String parseSessionId( String query )
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

	/** A simple growable byte accumulator. */
	private static final class ByteArrayAccumulator
	{
		private byte[] buffer = new byte[ 1024 ];
		private int size;

		void add( byte b )
		{
			if ( size == buffer.length )
			{
				byte[] next = new byte[ buffer.length * 2 ];
				System.arraycopy( buffer, 0, next, 0, size );
				buffer = next;
			}
			buffer[ size++ ] = b;
		}

		byte[] bytes()
		{
			byte[] out = new byte[ size ];
			System.arraycopy( buffer, 0, out, 0, size );
			return out;
		}
	}
}
