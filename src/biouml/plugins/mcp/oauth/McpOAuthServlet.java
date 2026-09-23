package biouml.plugins.mcp.oauth;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import ru.biosoft.access.security.SecurityManager;

/**
 * The BioUML MCP OAuth 2.1 authorization server.
 *
 * <p>Registered as an extension servlet (extension point {@code ru.biosoft.server.servlet}, prefix
 * {@code oauth}) next to the MCP endpoint servlet ({@code prefix="mcp"}), so it is reachable at
 * {@code /biouml/oauth/...} through the same {@code ConnectionServlet} reflective dispatch that
 * drives {@link biouml.plugins.mcp.web.McpServlet}. Like that servlet it is <em>not</em> a
 * {@code HttpServlet} subclass: {@code init(String[])} at startup, then per request the
 * {@code service(String, Object, Map, OutputStream, Map)} overload (servlet path, Tomcat session,
 * parsed params, response stream, response-header map). The body is written to the stream, the
 * content type returned, and any response header (including the reserved {@code Status} key that
 * {@code ConnectionServlet} turns into the real HTTP status line) is taken from the header map.</p>
 *
 * <h2>Why this exists</h2>
 * <p>The MCP endpoint authenticates with a {@code Authorization} header (a BioStore token) or a
 * JSESSIONID. Remote MCP clients — notably Claude.ai custom connectors — cannot present either up
 * front: they discover authorization from a {@code 401} challenge and complete the MCP-spec OAuth
 * flow (RFC 9728 protected-resource metadata → RFC 8414 authorization-server metadata → OAuth 2.1
 * authorization-code + PKCE). This servlet is that authorization server, self-hosted in BioUML and
 * bound to existing BioUML users (the BioStore credential server cannot mint tokens we could later
 * validate offline, so BioUML issues its own opaque access tokens mapped to platform sessions).</p>
 *
 * <h2>Endpoints</h2>
 * <ul>
 * <li>{@code GET /biouml/oauth/.well-known/oauth-protected-resource} — RFC 9728 document pointing
 * at this authorization server.</li>
 * <li>{@code GET /biouml/oauth/.well-known/oauth-authorization-server} — RFC 8414 metadata
 * (endpoints + supported grant/response types; S256 PKCE; public clients only).</li>
 * <li>{@code GET|POST /biouml/oauth/authorize} — the user step. A request with a logged-in session
 * (or the username/password form) issues a one-time code and 302-redirects the client with
 * {@code code} + {@code state}. Without credentials it renders the login form.</li>
 * <li>{@code POST /biouml/oauth/token} — the code exchange: validates PKCE (S256), the exact
 * {@code redirect_uri}/{@code client_id}, consumes the code, copies the authorizing user's
 * permissions into a fresh session, and returns an opaque bearer access token (1 h).</li>
 * </ul>
 *
 * <p>Security: redirect URIs must match the static allow-list ({@link McpOAuthConfig#clients})
 * <em>exactly</em> (open-redirect defense); codes are one-time (replay defense); the {@code state}
 * parameter round-trips untouched (CSRF defense); access tokens are opaque {@link java.security.SecureRandom}
 * values, single-JVM (see {@link OAuthTokenStore}). There is no refresh-token grant in v1 — the
 * renewal path is to run the authorization flow again.</p>
 */
public class McpOAuthServlet
{
	private static final Logger log = Logger.getLogger( McpOAuthServlet.class.getName() );

	private final ObjectMapper mapper = new ObjectMapper();

	/**
	 * Called by the servlet registry at server startup (mirrors {@code McpServlet.init}). The OAuth
	 * server is stateless at startup (config is read per request, stores are static), so this is a
	 * no-op that exists to satisfy the registry's {@code init(String[])} contract.
	 */
	public void init( String[] args )
	{
		// nothing to build; config is per-request, stores are static
	}

	/**
	 * Entry point driven reflectively by {@code ConnectionServlet.executeQueryWithExtensionServlet}
	 * (the {@code (String, Object, Map, OutputStream, Map)} overload).
	 *
	 * @param localAddress the request servlet path (e.g. {@code /oauth/authorize})
	 * @param session      Tomcat's {@code HttpSession} (carries the {@code JSESSIONID}); may be null
	 * @param params       the parsed request parameters (query + form fields) plus the surfaced
	 *                     headers ({@code Host}, {@code Remote-address}, ...)
	 * @param out          the response stream (the body is written here)
	 * @param header       the response-header map; the reserved {@code Status} key becomes the real
	 *                     HTTP status line (see {@code ConnectionServlet})
	 * @return the response content type
	 */
	public String service( String localAddress, Object session, Map params, OutputStream out,
			Map<String, String> header )
	{
		String path = localAddress == null ? "/oauth" : localAddress;
		HandleResult r = handle( path, session, params );
		try
		{
			out.write( r.body.getBytes( StandardCharsets.UTF_8 ) );
			out.flush();
		}
		catch ( Exception e )
		{
			log.log( Level.WARNING, "Client aborted while writing OAuth response", e );
		}
		if ( header != null )
		{
			header.put( "Status", String.valueOf( r.status ) );
			for ( Map.Entry<String, String> e : r.headers.entrySet() )
				header.put( e.getKey(), e.getValue() );
		}
		return r.contentType;
	}

	/**
	 * The transport-agnostic core (also driven directly by the test suite): route the request and
	 * produce status + content type + response headers + body.
	 *
	 * @param path    the request servlet path (e.g. {@code /oauth/token})
	 * @param session the caller's {@code HttpSession}; may be null
	 * @param params  the parsed request parameters (form/query fields + surfaced headers)
	 * @return the HTTP status, content type, extra headers, and body
	 */
	public HandleResult handle( String path, Object session, Map<String, Object> params )
	{
		Map<String, Object> p = params == null ? new LinkedHashMap<String, Object>() : params;
		String sub = path;
		if ( sub.startsWith( "/oauth/" ) )
			sub = sub.substring( "/oauth/".length() );
		else if ( sub.equals( "/oauth" ) )
			sub = "";

		try
		{
			if ( ".well-known/oauth-protected-resource".equals( sub ) )
				return wellKnown( "protected-resource", p );
			if ( ".well-known/oauth-authorization-server".equals( sub ) )
				return wellKnown( "authorization-server", p );
			if ( "authorize".equals( sub ) )
				return authorize( session, p );
			if ( "token".equals( sub ) )
				return token( p );
			if ( "register".equals( sub ) )
				return register( p );
			return new HandleResult( 404, "application/json",
					errorJson( "invalid_request", "unknown OAuth path" ) );
		}
		catch ( Exception e )
		{
			log.log( Level.SEVERE, "OAuth endpoint failed: " + path, e );
			return new HandleResult( 500, "application/json",
					errorJson( "server_error", "internal error" ) );
		}
	}

	// ------------------------------------------------------------------ well-known

	private HandleResult wellKnown( String kind, Map<String, Object> p ) throws Exception
	{
		String host = first( p.get( "Host" ) );
		String issuer = McpOAuthConfig.issuer( host );
		if ( issuer == null )
			return new HandleResult( 500, "application/json",
					errorJson( "server_error", "cannot determine the OAuth issuer (no Host header)" ) );

		Map<String, Object> doc = new LinkedHashMap<String, Object>();
		if ( "protected-resource".equals( kind ) )
		{
			doc.put( "resource", McpOAuthConfig.resourceUrl( issuer ) );
			java.util.List<String> servers = new java.util.ArrayList<String>();
			servers.add( issuer );
			doc.put( "authorization_servers", servers );
		}
		else
		{
			doc.put( "issuer", issuer );
			doc.put( "authorization_endpoint", McpOAuthConfig.authorizationEndpoint( issuer ) );
			doc.put( "token_endpoint", McpOAuthConfig.tokenEndpoint( issuer ) );
			// RFC 7591: advertise the registration endpoint so clients (Claude's "Register
			// automatically") know they can self-register instead of needing a static client.
			doc.put( "registration_endpoint", issuer + "/register" );
			java.util.List<String> responseTypes = new java.util.ArrayList<String>();
			responseTypes.add( "code" );
			doc.put( "response_types_supported", responseTypes );
			java.util.List<String> grantTypes = new java.util.ArrayList<String>();
			grantTypes.add( "authorization_code" );
			doc.put( "grant_types_supported", grantTypes );
			java.util.List<String> methods = new java.util.ArrayList<String>();
			methods.add( "S256" );
			methods.add( "none" );
			doc.put( "code_challenge_methods_supported", methods );
			java.util.List<String> tokenAuth = new java.util.ArrayList<String>();
			tokenAuth.add( "none" );
			tokenAuth.add( "client_secret_basic" );
			doc.put( "token_endpoint_auth_methods_supported", tokenAuth );
			doc.put( "scopes_supported", new java.util.ArrayList<String>() );
		}
		return new HandleResult( 200, "application/json", mapper.writeValueAsString( doc ) );
	}

	// ------------------------------------------------------------------ authorize

	private HandleResult authorize( Object session, Map<String, Object> p ) throws Exception
	{
		String clientId = first( p.get( "client_id" ) );
		String redirectUri = first( p.get( "redirect_uri" ) );
		String state = first( p.get( "state" ) );
		String challenge = first( p.get( "code_challenge" ) );
		String challengeMethod = first( p.get( "code_challenge_method" ) );
		String resource = first( p.get( "resource" ) );

		// Validation — every failure is an error page (no redirect: we must not bounce the user to an
		// unvalidated URI before we know the request is sane).
		String issuer = McpOAuthConfig.issuer( first( p.get( "Host" ) ) );
		String bad = validateAuthorize( clientId, redirectUri, challenge, challengeMethod, resource, issuer );
		if ( bad != null )
			return new HandleResult( 400, "text/html; charset=utf-8", formPage( bad, clientId, redirectUri, state,
					challenge, challengeMethod, resource ) );

		// Path 1: a live, logged-in session (the browser already authenticated the user).
		String sessionId = resolveSessionId( session, p );
		String user = null;
		if ( sessionId != null )
		{
			try
			{
				SecurityManager.addThreadToSessionRecord( Thread.currentThread(), sessionId );
				if ( !SecurityManager.isSessionDead( sessionId ) )
					user = SecurityManager.getSessionUser();
			}
			catch ( Exception ignore )
			{
				// not a usable session — fall through to the form path
			}
		}
		if ( user != null && !user.isEmpty() )
			return issueCode( clientId, redirectUri, challenge, state, sessionId, user, null );

		// Path 2: the login form (username/password in this request).
		String username = first( p.get( "username" ) );
		String password = first( p.get( "password" ) );
		if ( !username.isEmpty() || !password.isEmpty() )
		{
			String remote = first( p.get( "Remote-address" ) );
			String fresh = SecurityManager.generateSessionId();
			String loginError = null;
			try
			{
				SecurityManager.addThreadToSessionRecord( Thread.currentThread(), fresh );
				SecurityManager.commonLogin( username, password, remote, null );
				user = SecurityManager.getSessionUser();
			}
			catch ( Exception e )
			{
				loginError = "login failed";
			}
			if ( loginError == null && user != null && !user.isEmpty() )
				return issueCode( clientId, redirectUri, challenge, state, fresh, user, null );
			return new HandleResult( 200, "text/html; charset=utf-8", formPage( "login failed — check your credentials",
					clientId, redirectUri, state, challenge, challengeMethod, resource ) );
		}

		// Path 3: no credentials yet — show the form.
		return new HandleResult( 200, "text/html; charset=utf-8",
				formPage( null, clientId, redirectUri, state, challenge, challengeMethod, resource ) );
	}

	/**
	 * Validate the authorize request; {@code null} when valid. The {@code resource} indicator
	 * (RFC 8707) must be absent or refer to this MCP server (see
	 * {@link McpOAuthConfig#isMcpResource}) — a different resource would mean the code is being
	 * requested for another server, which this AS does not serve.
	 */
	static String validateAuthorize( String clientId, String redirectUri, String challenge,
			String challengeMethod, String resource, String issuer )
	{
		String bad = McpOAuthConfig.checkClient( clientId, redirectUri );
		if ( bad != null )
			return bad;
		// PKCE: S256 (challenge required) or none (challenge must be absent). An empty method with an
		// empty challenge is treated as "none" (no PKCE) — accepted for permissive interop.
		if ( "S256".equals( challengeMethod ) )
		{
			if ( challenge.isEmpty() )
				return "code_challenge_method=S256 requires a code_challenge";
		}
		else if ( "none".equals( challengeMethod ) && !challenge.isEmpty() )
		{
			return "code_challenge_method=none must not send a code_challenge";
		}
		else if ( !challengeMethod.isEmpty() && !"none".equals( challengeMethod ) )
		{
			return "code_challenge_method must be S256 or none";
		}
		if ( !resource.isEmpty() && !McpOAuthConfig.isMcpResource( resource, issuer ) )
			return "unsupported resource";
		return null;
	}

	private HandleResult issueCode( String clientId, String redirectUri, String challenge, String state,
			String sessionId, String user, String ignored )
	{
		OAuthTokenStore.AuthCode code = OAuthTokenStore.issueCode( clientId, redirectUri, challenge,
				sessionId, user );
		log.info( "MCP OAuth code issued user=" + user + " client=" + clientId );
		String sep = redirectUri.indexOf( '?' ) >= 0 ? "&" : "?";
		String location = redirectUri + sep + "code=" + urlEncode( code.value )
				+ ( state.isEmpty() ? "" : "&state=" + urlEncode( state ) );
		Map<String, String> headers = new LinkedHashMap<String, String>();
		headers.put( "Location", location );
		return new HandleResult( 302, "text/plain", headers, "" );
	}

	// ------------------------------------------------------------------ register (RFC 7591)

	/**
	 * Dynamic Client Registration (RFC 7591). MCP clients (notably Claude's "Register automatically"
	 * mode) register here on connect instead of a static allow-list. The request is a JSON body with
	 * at least {@code redirect_uris}; the response (HTTP 201) is the client record — {@code client_id}
	 * (server-generated), {@code client_secret} (echoed, for confidential clients), and
	 * {@code client_id_issuer} (RFC 9728 §4.3, the authorization-server issuer that issued the id).
	 */
	private HandleResult register( Map<String, Object> p ) throws Exception
	{
		// The body arrives as a raw JSON string under a params key (see ConnectionServlet.getParameterMap
		// for mcpRawBody) or as individual form fields. Prefer the raw JSON body; fall back to fields.
		String raw = first( p.get( "mcpRawBody" ) );
		Map<String, Object> body;
		if ( !raw.isEmpty() )
		{
			try
			{
				body = new ObjectMapper().readValue( raw, Map.class );
			}
			catch ( Exception e )
			{
				return new HandleResult( 400, "application/json",
						errorJson( "invalid_client_metadata", "request body is not valid JSON" ) );
			}
		}
		else
		{
			body = new LinkedHashMap<String, Object>();
			for ( Map.Entry<String, Object> e : p.entrySet() )
				body.put( e.getKey(), e.getValue() );
		}

		// redirect_uris is required (RFC 7591 §3.2.1). Accept a JSON array or a comma/space-separated string.
		Object urisObj = body.get( "redirect_uris" );
		List<String> uris = new java.util.ArrayList<String>();
		if ( urisObj instanceof List )
		{
			for ( Object o : (List<?>) urisObj )
				if ( o != null && !o.toString().trim().isEmpty() )
					uris.add( o.toString().trim() );
		}
		else if ( urisObj != null )
		{
			for ( String s : urisObj.toString().split( "[,\\s]+" ) )
				if ( !s.trim().isEmpty() )
					uris.add( s.trim() );
		}
		if ( uris.isEmpty() )
			return new HandleResult( 400, "application/json",
					errorJson( "invalid_redirect_uri", "redirect_uris is required and must be non-empty" ) );

		String clientName = body.get( "client_name" ) == null ? "" : body.get( "client_name" ).toString();
		String clientSecret = body.get( "client_secret" ) == null ? null : body.get( "client_secret" ).toString();
		if ( clientSecret != null && clientSecret.isEmpty() )
			clientSecret = null; // treat an empty secret as a public client

		OAuthTokenStore.RegisteredClient client = OAuthTokenStore.registerClient( uris, clientName, clientSecret );
		log.info( "MCP OAuth client registered id=" + client.clientId + " name=" + clientName
				+ " uris=" + uris + " confidential=" + ( clientSecret != null ) );

		String issuer = McpOAuthConfig.issuer( first( p.get( "Host" ) ) );
		Map<String, Object> out = new LinkedHashMap<String, Object>();
		out.put( "client_id", client.clientId );
		if ( clientSecret != null )
			out.put( "client_secret", clientSecret );
		out.put( "client_id_issuer", issuer == null ? "" : issuer );
		out.put( "client_name", clientName );
		out.put( "redirect_uris", uris );
		out.put( "grant_types", java.util.Arrays.asList( "authorization_code" ) );
		out.put( "response_types", java.util.Arrays.asList( "code" ) );
		out.put( "token_endpoint_auth_method", clientSecret == null ? "none" : "client_secret_basic" );
		return new HandleResult( 201, "application/json", mapper.writeValueAsString( out ) );
	}

	// ------------------------------------------------------------------ token

	private HandleResult token( Map<String, Object> p ) throws Exception
	{
		String grantType = first( p.get( "grant_type" ) );
		if ( !"authorization_code".equals( grantType ) )
			return new HandleResult( 400, "application/json",
					errorJson( "unsupported_grant_type", "only authorization_code is supported" ) );

		String code = first( p.get( "code" ) );
		String clientId = first( p.get( "client_id" ) );
		String redirectUri = first( p.get( "redirect_uri" ) );
		String verifier = first( p.get( "code_verifier" ) );

		OAuthTokenStore.AuthCode stored = OAuthTokenStore.consumeCode( code );
		if ( stored == null )
			return new HandleResult( 400, "application/json",
					errorJson( "invalid_grant", "unknown or expired authorization code" ) );
		if ( !stored.clientId.equals( clientId ) )
			return new HandleResult( 400, "application/json",
					errorJson( "invalid_grant", "client_id does not match the code" ) );
		if ( !stored.redirectUri.equals( redirectUri ) )
			return new HandleResult( 400, "application/json",
					errorJson( "invalid_grant", "redirect_uri does not match the code" ) );
		if ( stored.codeChallenge == null || stored.codeChallenge.isEmpty() )
		{
			// No PKCE was required at /authorize (method "none"); accept any (or no) verifier.
		}
		else if ( verifier.isEmpty() || !OAuthTokenStore.pkceMatches( stored.codeChallenge,
				OAuthTokenStore.pkceChallenge( verifier ) ) )
			return new HandleResult( 400, "application/json",
					errorJson( "invalid_grant", "code_verifier does not satisfy the code_challenge" ) );

		// Copy the authorizing user's permissions into a fresh session: the token must outlive the
		// web login that minted it (and never expose that login's JSESSIONID as a bearer credential).
		String remote = first( p.get( "Remote-address" ) );
		String newSession = SecurityManager.generateSessionId();
		String user = null;
		try
		{
			SecurityManager.addThreadToSessionRecord( Thread.currentThread(), newSession );
			SecurityManager.commonLoginViaOtherSession( stored.authorizingSessionId, remote, false );
			user = SecurityManager.getSessionUser();
		}
		catch ( Exception e )
		{
			log.log( Level.SEVERE, "Failed to copy session for OAuth token", e );
		}
		if ( user == null || user.isEmpty() )
			return new HandleResult( 401, "application/json",
					errorJson( "invalid_grant", "the authorizing session is no longer valid" ) );

		OAuthTokenStore.AccessToken token = OAuthTokenStore.issueToken( newSession, stored.clientId, user );
		log.info( "MCP OAuth token issued user=" + user + " client=" + stored.clientId );
		Map<String, Object> body = new LinkedHashMap<String, Object>();
		body.put( "access_token", token.value );
		body.put( "token_type", "Bearer" );
		body.put( "expires_in", OAuthTokenStore.ACCESS_TOKEN_TTL_MS / 1000L );
		return new HandleResult( 200, "application/json", mapper.writeValueAsString( body ) );
	}

	// ------------------------------------------------------------------ form

	/**
	 * Render the login form. All reflected request values are HTML-escaped (they are attacker-
	 * controlled: client_id / redirect_uri / state come from the MCP client, not the user).
	 */
	private String formPage( String error, String clientId, String redirectUri, String state,
			String challenge, String challengeMethod, String resource )
	{
		StringBuilder sb = new StringBuilder();
		sb.append( "<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"utf-8\">\n" );
		sb.append( "<title>Sign in to BioUML (MCP)</title>\n" );
		sb.append( "<style>body{font-family:sans-serif;max-width:24rem;margin:3rem auto;padding:0 1rem}" );
		sb.append( "input{width:100%;margin:0.3rem 0;padding:0.4rem;box-sizing:border-box}" );
		sb.append( "button{margin-top:0.8rem;padding:0.4rem 1rem}</style>\n</head>\n<body>\n" );
		sb.append( "<h2>BioUML — MCP sign-in</h2>\n" );
		sb.append( "<p>An MCP client requested access to this BioUML server. Sign in with your BioUML " );
		sb.append( "account to allow it.</p>\n" );
		if ( error != null )
			sb.append( "<p style=\"color:#a00\">" ).append( escape( error ) ).append( "</p>\n" );
		sb.append( "<form method=\"post\" action=\"\">" );
		sb.append( "<input type=\"hidden\" name=\"client_id\" value=\"" ).append( escape( clientId ) ).append( "\">\n" );
		sb.append( "<input type=\"hidden\" name=\"redirect_uri\" value=\"" ).append( escape( redirectUri ) ).append( "\">\n" );
		if ( !state.isEmpty() )
			sb.append( "<input type=\"hidden\" name=\"state\" value=\"" ).append( escape( state ) ).append( "\">\n" );
		if ( !challenge.isEmpty() )
		{
			sb.append( "<input type=\"hidden\" name=\"code_challenge\" value=\"" ).append( escape( challenge ) ).append( "\">\n" );
			sb.append( "<input type=\"hidden\" name=\"code_challenge_method\" value=\"" )
					.append( escape( challengeMethod ) ).append( "\">\n" );
		}
		if ( resource != null && !resource.isEmpty() )
			sb.append( "<input type=\"hidden\" name=\"resource\" value=\"" ).append( escape( resource ) ).append( "\">\n" );
		sb.append( "<label>Username<br><input type=\"text\" name=\"username\" autocomplete=\"username\"></label>\n" );
		sb.append( "<label>Password<br><input type=\"password\" name=\"password\" autocomplete=\"current-password\"></label>\n" );
		sb.append( "<button type=\"submit\">Sign in</button>\n" );
		sb.append( "</form>\n</body>\n</html>\n" );
		return sb.toString();
	}

	// ------------------------------------------------------------------ helpers

	/** Resolve the caller's session id: an explicit {@code sessionId} param, else the JSESSIONID. */
	private static String resolveSessionId( Object session, Map<String, Object> p )
	{
		String sid = first( p.get( SecurityManager.SESSION_ID ) );
		if ( !sid.isEmpty() )
			return sid;
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
				// not a session object
			}
		}
		return null;
	}

	/** The params-map value for a parameter is a {@code String[]}; return its first element. */
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

	/** Minimal HTML escaping for reflected values in the login form. */
	private static String escape( String s )
	{
		if ( s == null )
			return "";
		return s.replace( "&", "&amp;" ).replace( "<", "&lt;" ).replace( ">", "&gt;" )
				.replace( "\"", "&quot;" ).replace( "'", "&#39;" );
	}

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

	/** RFC 6749 §5.2 error body. */
	private static String errorJson( String error, String description )
	{
		return "{\"error\":\"" + error + "\",\"error_description\":\"" + description + "\"}";
	}

	/**
	 * The HTTP result of {@link #handle}. The reserved {@code Status} header key is written by
	 * {@link #service} (never inside {@code headers}, which holds only client-visible headers).
	 */
	public static final class HandleResult
	{
		public final int status;
		public final String contentType;
		public final Map<String, String> headers;
		public final String body;

		public HandleResult( int status, String contentType, String body )
		{
			this( status, contentType, new LinkedHashMap<String, String>(), body );
		}

		public HandleResult( int status, String contentType, Map<String, String> headers, String body )
		{
			this.status = status;
			this.contentType = contentType;
			this.headers = headers;
			this.body = body;
		}
	}
}
