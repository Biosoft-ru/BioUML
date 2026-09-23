package biouml.plugins.mcp._test;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import junit.framework.TestCase;

import biouml.plugins.mcp.oauth.McpOAuthConfig;
import biouml.plugins.mcp.oauth.McpOAuthServlet;
import biouml.plugins.mcp.oauth.McpOAuthServlet.HandleResult;
import biouml.plugins.mcp.oauth.OAuthTokenStore;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.TestSecurityProvider;

/**
 * Tests for the MCP OAuth 2.1 authorization server ({@link McpOAuthServlet}) — the RFC 9728/8414
 * discovery documents, the {@code /authorize} step (session + login-form paths), and the
 * {@code /token} code exchange (PKCE S256, one-time codes, exact redirect_uri/client binding).
 *
 * <p>Mirrors {@link McpServletTest}: the servlet is driven through its real 5-arg
 * {@code service(...)} entry point with hand-built params/header maps exactly as
 * {@code ConnectionServlet.getParameterMap} would deliver them, so the write-to-stream +
 * return-content-type + header-map contract is exercised. {@code TestSecurityProvider} (the surefire
 * default) accepts every credential, so logins succeed in the test environment.</p>
 */
public class McpOAuthServletTest extends TestCase
{
	/** A registered client (set in setUp) with two allowed redirect URIs. */
	private static final String CLIENT_ID = "test-client";
	private static final String REDIRECT_URI = "https://client.example/callback";
	private static final String REDIRECT_URI_LOCAL = "http://localhost:8443/callback";
	private static final String CLIENTS_PROPERTY =
			CLIENT_ID + "=" + REDIRECT_URI + "|" + REDIRECT_URI_LOCAL;

	private final ObjectMapper mapper = new ObjectMapper();
	private McpOAuthServlet servlet;
	/** A live session with a logged-in user (created via anonymousLogin under a non-system id). */
	private static final String AUTH_SESSION = "mcp-oauth-auth-session";
	/** The verifier whose S256 challenge the authorize step registers. */
	private static final String VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
	private static final String CHALLENGE = OAuthTokenStore.pkceChallenge( VERIFIER );

	private String savedClients;

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		OAuthTokenStore.clearForTest();
		// The suite normally initializes the (static) SecurityProvider before the MCP tests run; when
		// this class runs in isolation the provider is null, so install the lenient TestSecurityProvider
		// (the same trick McpServletTest uses for its provider swaps).
		installProvider();
		SecurityManager.addThreadToSessionRecord( Thread.currentThread(), AUTH_SESSION );
		SecurityManager.commonLogin( "alice", "pw", "127.0.0.1", null );
		servlet = new McpOAuthServlet();
		servlet.init( new String[ 0 ] );
		savedClients = System.getProperty( McpOAuthConfig.CLIENTS_PROPERTY );
		System.setProperty( McpOAuthConfig.CLIENTS_PROPERTY, CLIENTS_PROPERTY );
		System.clearProperty( McpOAuthConfig.ISSUER_PROPERTY );
	}

	@Override
	protected void tearDown() throws Exception
	{
		if ( savedClients == null )
			System.clearProperty( McpOAuthConfig.CLIENTS_PROPERTY );
		else
			System.setProperty( McpOAuthConfig.CLIENTS_PROPERTY, savedClients );
		System.clearProperty( McpOAuthConfig.ISSUER_PROPERTY );
		OAuthTokenStore.clearForTest();
		SecurityManager.removeThreadFromSessionRecord();
		super.tearDown();
	}

	// ------------------------------------------------------------------ helpers

	/** Install the lenient TestSecurityProvider (no-op if one is already installed). */
	private static void installProvider() throws Exception
	{
		if ( SecurityManager.getSecurityProvider() != null )
			return;
		java.lang.reflect.Field f = SecurityManager.class.getDeclaredField( "securityProvider" );
		f.setAccessible( true );
		f.set( null, new TestSecurityProvider() );
	}

	/** Drive the real 5-arg service() entry point (the path the live server takes). */
	private HandleResult service( String path, Map<String, Object> params ) throws Exception
	{
		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		Map<String, String> header = new LinkedHashMap<String, String>();
		String contentType = servlet.service( path, null, params, out, header );
		assertEquals( "service() returns a content type", contentType != null && !contentType.isEmpty(), true );
		String body = new String( out.toByteArray(), StandardCharsets.UTF_8 );
		int status = 200;
		if ( header.get( "Status" ) != null )
			status = Integer.parseInt( header.get( "Status" ) );
		return new HandleResult( status, contentType, header, body );
	}

	/** A params map as ConnectionServlet.getParameterMap would deliver query/form fields. */
	private static Map<String, Object> params( String... kv )
	{
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		for ( int i = 0; i + 1 < kv.length; i += 2 )
			map.put( kv[ i ], new String[] { kv[ i + 1 ] } );
		map.put( "Host", new String[] { "biouml2test.biouml.org" } );
		map.put( "Remote-address", new String[] { "127.0.0.1" } );
		return map;
	}

	private static String authorizeParams( String clientId, String redirectUri, String state,
			String challenge, String method )
	{
		return "client_id=" + clientId + "&redirect_uri=" + redirectUri
				+ "&state=" + state + "&code_challenge=" + challenge + "&code_challenge_method=" + method
				+ "&resource=/biouml/mcp";
	}

	/** Run the /authorize step with a live logged-in session → expect a 302 carrying the code. */
	private HandleResult authorizeAsSession( Map<String, Object> extra ) throws Exception
	{
		Map<String, Object> p = params( "sessionId", AUTH_SESSION );
		for ( Map.Entry<String, Object> e : extra.entrySet() )
			p.put( e.getKey(), e.getValue() );
		return service( "/oauth/authorize", p );
	}

	/** Extract {@code code=...} from a redirect Location. */
	private static String codeFrom( HandleResult r )
	{
		String loc = r.headers.get( "Location" );
		assertNotNull( "redirect Location present", loc );
		for ( String pair : loc.split( "[?&]" ) )
		{
			if ( pair.startsWith( "code=" ) )
				return pair.substring( "code=".length() );
		}
		return null;
	}

	/** Exchange a code at /token with the given verifier (and client/redirect as registered). */
	private HandleResult exchange( String code, String verifier, String clientId, String redirectUri )
			throws Exception
	{
		Map<String, Object> p = params( "grant_type", "authorization_code", "code", code,
				"client_id", clientId, "redirect_uri", redirectUri, "code_verifier", verifier );
		return service( "/oauth/token", p );
	}

	@SuppressWarnings( "unchecked" )
	private static Map<String, Object> json( String body ) throws Exception
	{
		return new ObjectMapper().readValue( body, Map.class );
	}

	// ------------------------------------------------------------------ config / discovery

	/** The client allow-list parses from the property and validates exact redirect matches. */
	public void testClientAllowList()
	{
		assertNull( "registered client+uri passes", McpOAuthConfig.checkClient( CLIENT_ID, REDIRECT_URI ) );
		assertNull( "second registered uri passes", McpOAuthConfig.checkClient( CLIENT_ID, REDIRECT_URI_LOCAL ) );
		assertNotNull( "unknown client rejected", McpOAuthConfig.checkClient( "nope", REDIRECT_URI ) );
		assertNotNull( "unregistered redirect rejected",
				McpOAuthConfig.checkClient( CLIENT_ID, "https://evil.example/callback" ) );
		assertNotNull( "prefix match rejected",
				McpOAuthConfig.checkClient( CLIENT_ID, REDIRECT_URI + "/extra" ) );
	}

	/** The issuer derives from the Host header when the property is unset, stripping a port. */
	public void testIssuerDerivation()
	{
		System.clearProperty( McpOAuthConfig.ISSUER_PROPERTY );
		assertEquals( "https://biouml2test.biouml.org/biouml/oauth",
				McpOAuthConfig.issuer( "biouml2test.biouml.org" ) );
		assertEquals( "https://biouml2test.biouml.org:8443/biouml/oauth",
				McpOAuthConfig.issuer( "biouml2test.biouml.org:8443" ) );
		assertEquals( "portless host wins over a path", "https://h.example:1/biouml/oauth",
				McpOAuthConfig.issuer( "h.example:1/some/path" ) );
		assertNull( "no host → no issuer", McpOAuthConfig.issuer( null ) );
		System.setProperty( McpOAuthConfig.ISSUER_PROPERTY, "https://configured.example/biouml/oauth/" );
		assertEquals( "explicit property wins (trailing slash trimmed)",
				"https://configured.example/biouml/oauth", McpOAuthConfig.issuer( "ignored.example" ) );
		System.clearProperty( McpOAuthConfig.ISSUER_PROPERTY );
	}

	/** The RFC 9728 protected-resource document names the MCP resource + this authorization server. */
	@SuppressWarnings( "unchecked" )
	public void testProtectedResourceMetadata() throws Exception
	{
		HandleResult r = service( "/oauth/.well-known/oauth-protected-resource", params() );
		assertEquals( "200", 200, r.status );
		assertEquals( "application/json", r.contentType );
		Map<String, Object> doc = json( r.body );
		assertEquals( "https://biouml2test.biouml.org/biouml/mcp", doc.get( "resource" ) );
		List<String> servers = (List<String>) doc.get( "authorization_servers" );
		assertEquals( 1, servers.size() );
		assertEquals( "https://biouml2test.biouml.org/biouml/oauth", servers.get( 0 ) );
	}

	/** The RFC 8414 authorization-server metadata advertises code + PKCE S256 + public clients. */
	@SuppressWarnings( "unchecked" )
	public void testAuthorizationServerMetadata() throws Exception
	{
		HandleResult r = service( "/oauth/.well-known/oauth-authorization-server", params() );
		assertEquals( "200", 200, r.status );
		Map<String, Object> doc = json( r.body );
		assertEquals( "https://biouml2test.biouml.org/biouml/oauth", doc.get( "issuer" ) );
		assertEquals( "https://biouml2test.biouml.org/biouml/oauth/authorize", doc.get( "authorization_endpoint" ) );
		assertEquals( "https://biouml2test.biouml.org/biouml/oauth/token", doc.get( "token_endpoint" ) );
		assertEquals( java.util.Collections.singletonList( "code" ), doc.get( "response_types_supported" ) );
		assertEquals( java.util.Collections.singletonList( "authorization_code" ), doc.get( "grant_types_supported" ) );
		assertEquals( "registration endpoint advertised (RFC 7591)",
				"https://biouml2test.biouml.org/biouml/oauth/register", doc.get( "registration_endpoint" ) );
		assertEquals( java.util.Arrays.asList( "S256", "none" ), doc.get( "code_challenge_methods_supported" ) );
		assertEquals( java.util.Arrays.asList( "none", "client_secret_basic" ),
				doc.get( "token_endpoint_auth_methods_supported" ) );
	}

	/** Unknown sub-paths 404; the flow works with an explicit issuer property too. */
	public void testUnknownPathIs404() throws Exception
	{
		HandleResult r = service( "/oauth/nope", params() );
		assertEquals( 404, r.status );
		assertTrue( r.body.contains( "invalid_request" ) );
	}

	// ------------------------------------------------------------------ DCR (RFC 7591)

	/**
	 * Dynamic Client Registration: a JSON body with redirect_uris → 201 with a server-generated
	 * client_id + client_id_issuer, and the client is immediately usable (authorize accepts it).
	 * This is the path Claude's "Register automatically" mode takes.
	 */
	@SuppressWarnings( "unchecked" )
	public void testRegisterClientSucceedsAndIsUsable() throws Exception
	{
		String regBody = "{\"client_name\":\"claude-connector\","
				+ "\"redirect_uris\":[\"https://claude.ai/api/mcp/oauth/callback\"],"
				+ "\"grant_types\":[\"authorization_code\"],\"response_types\":[\"code\"]}";
		Map<String, Object> p = params( "mcpRawBody", regBody );
		HandleResult reg = service( "/oauth/register", p );
		assertEquals( "201 created — body=" + reg.body, 201, reg.status );
		Map<String, Object> regResp = json( reg.body );
		String clientId = (String) regResp.get( "client_id" );
		assertNotNull( "client_id returned", clientId );
		assertTrue( "server-generated id (mcp_cl_ prefix)", clientId.startsWith( "mcp_cl_" ) );
		assertEquals( "client_id_issuer is the AS issuer",
				"https://biouml2test.biouml.org/biouml/oauth", regResp.get( "client_id_issuer" ) );
		assertEquals( "public client → token_endpoint_auth_method none", "none", regResp.get( "token_endpoint_auth_method" ) );

		// The freshly-registered client must now pass authorization validation.
		assertNull( "registered client + its exact uri validates",
				McpOAuthConfig.checkClient( clientId, "https://claude.ai/api/mcp/oauth/callback" ) );
		assertNotNull( "a different uri is still rejected",
				McpOAuthConfig.checkClient( clientId, "https://evil.example/callback" ) );
	}

	/** A DCR-registered client can run the full authorize → token flow. */
	@SuppressWarnings( "unchecked" )
	public void testRegisteredClientRunsFullFlow() throws Exception
	{
		String regBody = "{\"redirect_uris\":[\"https://client.example/oauth/callback\"]}";
		HandleResult reg = service( "/oauth/register", params( "mcpRawBody", regBody ) );
		String clientId = (String) json( reg.body ).get( "client_id" );

		HandleResult authz = service( "/oauth/authorize", params(
				"sessionId", AUTH_SESSION, "client_id", clientId,
				"redirect_uri", "https://client.example/oauth/callback",
				"state", "s", "code_challenge", CHALLENGE, "code_challenge_method", "S256" ) );
		assertEquals( "302 for a registered client", 302, authz.status );
		String code = codeFrom( authz );

		HandleResult tok = exchange( code, VERIFIER, clientId, "https://client.example/oauth/callback" );
		assertEquals( "200 token for registered client — body=" + tok.body, 200, tok.status );
		assertNotNull( json( tok.body ).get( "access_token" ) );
	}

	/** A registration without redirect_uris is rejected (400 invalid_redirect_uri). */
	public void testRegisterWithoutRedirectUrisIs400() throws Exception
	{
		HandleResult r = service( "/oauth/register", params( "mcpRawBody", "{\"client_name\":\"x\"}" ) );
		assertEquals( 400, r.status );
		assertTrue( r.body.contains( "invalid_redirect_uri" ) );
	}

	/** A registration with a malformed JSON body is rejected (400 invalid_client_metadata). */
	public void testRegisterBadJsonIs400() throws Exception
	{
		HandleResult r = service( "/oauth/register", params( "mcpRawBody", "{not valid json" ) );
		assertEquals( 400, r.status );
		assertTrue( r.body.contains( "invalid_client_metadata" ) );
	}

	/** A confidential registration echoes the secret and reports client_secret_basic. */
	@SuppressWarnings( "unchecked" )
	public void testRegisterConfidentialClient() throws Exception
	{
		String regBody = "{\"redirect_uris\":[\"https://c.example/cb\"],\"client_secret\":\"s3cret\"}";
		HandleResult reg = service( "/oauth/register", params( "mcpRawBody", regBody ) );
		assertEquals( 201, reg.status );
		Map<String, Object> resp = json( reg.body );
		assertEquals( "secret echoed", "s3cret", resp.get( "client_secret" ) );
		assertEquals( "client_secret_basic", resp.get( "token_endpoint_auth_method" ) );
	}

	/** Redirect URIs given as a comma-separated string are also accepted. */
	@SuppressWarnings( "unchecked" )
	public void testRegisterRedirectUrisAsString() throws Exception
	{
		String regBody = "{\"redirect_uris\":\"https://a.example/cb, https://b.example/cb\"}";
		HandleResult reg = service( "/oauth/register", params( "mcpRawBody", regBody ) );
		assertEquals( 201, reg.status );
		List<String> uris = (List<String>) json( reg.body ).get( "redirect_uris" );
		assertEquals( 2, uris.size() );
		assertEquals( "https://a.example/cb", uris.get( 0 ) );
		assertEquals( "https://b.example/cb", uris.get( 1 ) );
	}

	// ------------------------------------------------------------------ authorize

	/** A logged-in session + valid params → 302 with a code and the state echoed. */
	public void testAuthorizeWithSessionIssuesCode() throws Exception
	{
		Map<String, Object> p = params();
		String[] kv = authorizeParams( CLIENT_ID, REDIRECT_URI, "st-123", CHALLENGE, "S256" ).split( "&" );
		for ( String pair : kv )
			p.put( pair.substring( 0, pair.indexOf( '=' ) ), new String[] { pair.substring( pair.indexOf( '=' ) + 1 ) } );

		HandleResult r = authorizeAsSession( p );
		assertEquals( "302 redirect", 302, r.status );
		String loc = r.headers.get( "Location" );
		assertTrue( "redirects to the registered URI", loc.startsWith( REDIRECT_URI + "?" ) );
		assertTrue( "state round-trips", loc.contains( "state=st-123" ) );
		assertNotNull( "code present", codeFrom( r ) );
	}

	/** A request with a username/password (no session) logs in and still issues a code. */
	public void testAuthorizeWithLoginFormIssuesCode() throws Exception
	{
		Map<String, Object> p = params( "username", "alice", "password", "secret" );
		String[] kv = authorizeParams( CLIENT_ID, REDIRECT_URI, "", CHALLENGE, "S256" ).split( "&" );
		for ( String pair : kv )
			p.put( pair.substring( 0, pair.indexOf( '=' ) ), new String[] { pair.substring( pair.indexOf( '=' ) + 1 ) } );

		HandleResult r = service( "/oauth/authorize", p );
		assertEquals( "302 after form login", 302, r.status );
		assertNotNull( codeFrom( r ) );
	}

	/** A request with no credentials renders the login form (200, not a redirect). */
	public void testAuthorizeWithoutCredentialsShowsForm() throws Exception
	{
		Map<String, Object> p = params();
		String[] kv = authorizeParams( CLIENT_ID, REDIRECT_URI, "st", CHALLENGE, "S256" ).split( "&" );
		for ( String pair : kv )
			p.put( pair.substring( 0, pair.indexOf( '=' ) ), new String[] { pair.substring( pair.indexOf( '=' ) + 1 ) } );

		HandleResult r = service( "/oauth/authorize", p );
		assertEquals( 200, r.status );
		assertTrue( "html form", r.contentType.startsWith( "text/html" ) );
		assertTrue( "has a username field", r.body.contains( "name=\"username\"" ) );
		assertTrue( "has a password field", r.body.contains( "name=\"password\"" ) );
		assertTrue( "client_id hidden field", r.body.contains( "value=\"" + CLIENT_ID + "\"" ) );
	}

	/** An unregistered redirect_uri is rejected with 400 and never becomes a redirect. */
	public void testAuthorizeUnknownRedirectIs400() throws Exception
	{
		Map<String, Object> p = params( "sessionId", AUTH_SESSION,
				"client_id", CLIENT_ID, "redirect_uri", "https://evil.example/callback",
				"state", "s", "code_challenge", CHALLENGE, "code_challenge_method", "S256" );
		HandleResult r = service( "/oauth/authorize", p );
		assertEquals( 400, r.status );
		assertNull( "no Location on rejection", r.headers.get( "Location" ) );
	}

	/** A non-S256 code_challenge_method is rejected. */
	public void testAuthorizeBadChallengeMethodIs400() throws Exception
	{
		Map<String, Object> p = params( "sessionId", AUTH_SESSION,
				"client_id", CLIENT_ID, "redirect_uri", REDIRECT_URI,
				"state", "s", "code_challenge", CHALLENGE, "code_challenge_method", "plain" );
		HandleResult r = service( "/oauth/authorize", p );
		assertEquals( 400, r.status );
	}

	/** A foreign resource indicator (not the MCP resource) is rejected. */
	public void testAuthorizeForeignResourceIs400() throws Exception
	{
		Map<String, Object> p = params( "sessionId", AUTH_SESSION,
				"client_id", CLIENT_ID, "redirect_uri", REDIRECT_URI,
				"state", "s", "code_challenge", CHALLENGE, "code_challenge_method", "S256",
				"resource", "https://other.example/biouml/mcp" );
		HandleResult r = service( "/oauth/authorize", p );
		assertEquals( 400, r.status );
	}

	/** The canonical absolute resource URL (the one in the PRM document) is accepted. */
	public void testAuthorizeCanonicalResourceAccepted() throws Exception
	{
		Map<String, Object> p = params( "sessionId", AUTH_SESSION,
				"client_id", CLIENT_ID, "redirect_uri", REDIRECT_URI,
				"state", "s", "code_challenge", CHALLENGE, "code_challenge_method", "S256",
				"resource", "https://biouml2test.biouml.org/biouml/mcp" );
		HandleResult r = service( "/oauth/authorize", p );
		assertEquals( "canonical absolute resource → 302", 302, r.status );
	}

	// ------------------------------------------------------------------ token

	/** The full in-JVM flow: session → code → (correct PKCE) → access token → usable. */
	@SuppressWarnings( "unchecked" )
	public void testFullAuthorizeTokenFlow() throws Exception
	{
		Map<String, Object> p = params( "sessionId", AUTH_SESSION,
				"client_id", CLIENT_ID, "redirect_uri", REDIRECT_URI,
				"state", "st", "code_challenge", CHALLENGE, "code_challenge_method", "S256" );
		HandleResult authz = service( "/oauth/authorize", p );
		assertEquals( 302, authz.status );
		String code = codeFrom( authz );
		assertNotNull( code );

		HandleResult tok = exchange( code, VERIFIER, CLIENT_ID, REDIRECT_URI );
		assertEquals( "200 on successful exchange — body=" + tok.body, 200, tok.status );
		Map<String, Object> body = json( tok.body );
		String token = (String) body.get( "access_token" );
		assertNotNull( token );
		assertTrue( "token is opaque with the mcp_at_ prefix", token.startsWith( "mcp_at_" ) );
		assertEquals( "Bearer", body.get( "token_type" ) );
		assertEquals( "expires_in", OAuthTokenStore.ACCESS_TOKEN_TTL_MS / 1000L,
				( (Number) body.get( "expires_in" ) ).longValue() );

		OAuthTokenStore.AccessToken validated = OAuthTokenStore.validate( token );
		assertNotNull( "minted token validates (session alive)", validated );
		assertEquals( "anonymous-login user carried over", "alice", validated.user );
	}

	/** The S256 challenge of the RFC 7636 appendix-B verifier matches the recorded digest. */
	public void testPkceMatchesRfcVector()
	{
		// Verifier "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk" from the RFC 7636 appendix B example.
		String challenge = OAuthTokenStore.pkceChallenge( "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk" );
		assertEquals( "43 chars (32-byte SHA-256, base64url, no padding)", 43, challenge.length() );
		for ( int i = 0; i < challenge.length(); i++ )
		{
			char c = challenge.charAt( i );
			boolean ok = ( c >= 'A' && c <= 'Z' ) || ( c >= 'a' && c <= 'z' ) || ( c >= '0' && c <= '9' )
					|| c == '-' || c == '_';
			assertTrue( "base64url alphabet at " + i + " (" + c + ")", ok );
		}
		assertEquals( "recorded digest of the RFC verifier",
				"E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", challenge );
	}

	/** A wrong code_verifier → 400 invalid_grant. */
	public void testTokenWrongVerifierIs400() throws Exception
	{
		String code = newCode();
		HandleResult r = exchange( code, "wrong-verifier", CLIENT_ID, REDIRECT_URI );
		assertEquals( 400, r.status );
		assertTrue( r.body.contains( "invalid_grant" ) );
	}

	/** A code can be exchanged exactly once; the second attempt fails. */
	public void testCodeReplayIsRejected() throws Exception
	{
		String code = newCode();
		assertEquals( 200, exchange( code, VERIFIER, CLIENT_ID, REDIRECT_URI ).status );
		HandleResult replay = exchange( code, VERIFIER, CLIENT_ID, REDIRECT_URI );
		assertEquals( "replay → 400", 400, replay.status );
		assertTrue( replay.body.contains( "invalid_grant" ) );
	}

	/** An unknown code → 400. */
	public void testTokenUnknownCodeIs400() throws Exception
	{
		HandleResult r = exchange( "mcp_ac_does-not-exist", VERIFIER, CLIENT_ID, REDIRECT_URI );
		assertEquals( 400, r.status );
	}

	/** A client_id that does not match the code → 400. */
	public void testTokenClientMismatchIs400() throws Exception
	{
		String code = newCode();
		HandleResult r = exchange( code, VERIFIER, "other-client", REDIRECT_URI );
		assertEquals( 400, r.status );
	}

	/** A redirect_uri that does not match the code → 400. */
	public void testTokenRedirectMismatchIs400() throws Exception
	{
		String code = newCode();
		HandleResult r = exchange( code, VERIFIER, CLIENT_ID, REDIRECT_URI_LOCAL );
		assertEquals( 400, r.status );
	}

	/** A non-authorization_code grant_type is rejected. */
	public void testTokenWrongGrantTypeIs400() throws Exception
	{
		Map<String, Object> p = params( "grant_type", "password", "code", "x",
				"client_id", CLIENT_ID, "redirect_uri", REDIRECT_URI, "code_verifier", VERIFIER );
		HandleResult r = service( "/oauth/token", p );
		assertEquals( 400, r.status );
		assertTrue( r.body.contains( "unsupported_grant_type" ) );
	}

	/** Issue a fresh code bound to the live session (helper for the negative token tests). */
	private String newCode()
	{
		OAuthTokenStore.AuthCode code = OAuthTokenStore.issueCode( CLIENT_ID, REDIRECT_URI, CHALLENGE,
				AUTH_SESSION, "alice" );
		return code.value;
	}
}
