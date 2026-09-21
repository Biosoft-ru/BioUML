package ru.biosoft.server.mcp._test;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import junit.framework.TestCase;

import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.server.mcp.McpWebAppServlet;

/**
 * Tests for the webapp-visible MCP proxy ({@link McpWebAppServlet}) — specifically its <em>standard
 * BioUML authentication</em>, which must match the rest of the server: a request is allowed only when
 * the {@code sessionId} it carries is a <em>live</em> session ({@link SecurityManager#isSessionDead}
 * false) that has a <em>logged-in user</em> ({@link SecurityManager#getSessionUser} non-null). There is
 * no privileged {@code system}-session shortcut — that was the bug.
 *
 * <p>The protocol core under test is {@code McpWebAppServlet.handle(method, path, query, body)},
 * which (unlike the {@code doPost} wrapper) takes no {@code HttpServletRequest}, so it can be driven
 * without stubbing the servlet API. The server it dispatches to is an in-process
 * {@code BioumlMcpServer} installed via {@link McpWebAppServlet#initForTest()} (the reflective OSGi
 * load is a production-only detail).</p>
 */
public class McpWebAppServletTest extends TestCase
{
	private final ObjectMapper mapper = new ObjectMapper();
	private McpWebAppServlet servlet;

	/** A live session with a logged-in user: created via anonymousLogin() under a non-system session. */
	private static final String AUTH_SESSION = "mcp-webapp-auth-session";
	/** A session that was never logged in → isSessionDead() true → 401. */
	private static final String DEAD_SESSION = "mcp-webapp-dead-session";

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		servlet = new McpWebAppServlet();
		servlet.initForTest();

		// Create a real, live session bound to a user under a NON-system session id, mirroring what
		// the web /login flow produces. anonymousLogin() records a UserPermissions for the current
		// thread's session, making isSessionDead() false and getSessionUser() non-null for it.
		SecurityManager.addThreadToSessionRecord( Thread.currentThread(), AUTH_SESSION );
		SecurityManager.anonymousLogin();
	}

	@Override
	protected void tearDown() throws Exception
	{
		// Detach so the test thread is not left bound to a session that a later test may inspect.
		SecurityManager.removeThreadFromSessionRecord();
		super.tearDown();
	}

	/** Bind the thread to the given session and call the servlet's handle() for a POST. */
	private McpWebAppServlet.HandleResult post( String sessionId, String jsonBody )
	{
		SecurityManager.addThreadToSessionRecord( Thread.currentThread(), sessionId );
		String query = sessionId == null ? null : SecurityManager.SESSION_ID + "=" + sessionId;
		return servlet.handle( "POST", "/mcp", query, jsonBody );
	}

	@SuppressWarnings( "unchecked" )
	private Map<String, Object> parse( String json ) throws Exception
	{
		return json == null || json.isEmpty() ? new java.util.LinkedHashMap<String, Object>()
				: (Map<String, Object>) mapper.readValue( json, Map.class );
	}

	/** A live session with a logged-in user is accepted: tools/list → 200 with ≥10 tools. */
	@SuppressWarnings( "unchecked" )
	public void testLiveLoggedInSessionIsAccepted() throws Exception
	{
		McpWebAppServlet.HandleResult r = post( AUTH_SESSION,
				"{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\",\"params\":{}}" );
		assertEquals( "live logged-in session → 200", 200, r.status );
		Map<String, Object> resp = parse( r.body );
		Map<String, Object> result = (Map<String, Object>) resp.get( "result" );
		assertNotNull( "tools/list result present", result );
		java.util.List<Map<String, Object>> tools = (java.util.List<Map<String, Object>>) result.get( "tools" );
		assertTrue( "at least 10 tools, was " + ( tools == null ? 0 : tools.size() ),
				tools != null && tools.size() >= 10 );
	}

	/** A dead session (never logged in) is refused with 401, even though it is non-empty. */
	@SuppressWarnings( "unchecked" )
	public void testDeadSessionIs401() throws Exception
	{
		McpWebAppServlet.HandleResult r = post( DEAD_SESSION,
				"{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}" );
		assertEquals( "dead session → 401", 401, r.status );
		Map<String, Object> resp = parse( r.body );
		assertNotNull( "401 body has an error", resp.get( "error" ) );
	}

	/** No session at all (no sessionId in the query) → 401. */
	public void testNoSessionIs401() throws Exception
	{
		// Even though the test thread may be bound to AUTH_SESSION, handle() resolves the session from
		// the query string, which is null here → 401 before any session lookup.
		McpWebAppServlet.HandleResult r = servlet.handle( "POST", "/mcp", null,
				"{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/list\",\"params\":{}}" );
		assertEquals( "no session → 401", 401, r.status );
	}

	/**
	 * The key regression guard: the {@code system} session must NOT be treated as privileged for an
	 * external request. Binding the thread to the system session (which has no user record by design)
	 * and presenting it as the sessionId must be refused with 401.
	 */
	@SuppressWarnings( "unchecked" )
	public void testSystemSessionIsNotPrivileged() throws Exception
	{
		SecurityManager.addThreadToSessionRecord( Thread.currentThread(), SecurityManager.SYSTEM_SESSION );
		String query = SecurityManager.SESSION_ID + "=" + SecurityManager.SYSTEM_SESSION;
		McpWebAppServlet.HandleResult r = servlet.handle( "POST", "/mcp", query,
				"{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/list\",\"params\":{}}" );
		// system session has no UserPermissions record → isSessionDead true (or no user) → 401.
		assertEquals( "system session is not privileged for external requests → 401", 401, r.status );
	}

	/** A live logged-in session dispatches a real tool call: repo_collections → 200, isError false. */
	@SuppressWarnings( "unchecked" )
	public void testToolsCallUnderLiveSession() throws Exception
	{
		McpWebAppServlet.HandleResult r = post( AUTH_SESSION,
				"{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"tools/call\",\"params\":{\"name\":\"biouml_repo_collections\",\"arguments\":{}}}" );
		assertEquals( "tools/call → 200", 200, r.status );
		Map<String, Object> resp = parse( r.body );
		Map<String, Object> result = (Map<String, Object>) resp.get( "result" );
		assertNotNull( "tools/call result present", result );
		assertEquals( "tools/call not an error", Boolean.FALSE, result.get( "isError" ) );
	}
}
