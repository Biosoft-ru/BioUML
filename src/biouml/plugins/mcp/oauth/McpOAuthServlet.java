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
 * renders a consent page (client + redirect host, Allow/Deny); only submitting it issues a one-time
 * code and 302-redirects the client with {@code code} + {@code state}. Without a session it renders
 * the login form, whose successful submission is the consent.</li>
 * <li>{@code POST /biouml/oauth/token} — the token endpoint. {@code grant_type=authorization_code}
 * validates PKCE (S256), the exact {@code redirect_uri}/{@code client_id}, consumes the code, copies
 * the authorizing user's permissions into a fresh session, and returns an opaque bearer access token
 * (1 h) <em>plus a refresh token (7 days)</em>. {@code grant_type=refresh_token} exchanges a still-
 * valid refresh token for a fresh access token bound to the same session (RFC 6749 §6) — this is how
 * a connected client stays authenticated across the 1-hour access-token window without re-authorizing.</li>
 * </ul>
 *
 * <p>Security: redirect URIs must match the registered set ({@link McpOAuthConfig#checkClient})
 * <em>exactly</em> (open-redirect defense), and self-registered ones must be https, loopback http, or
 * a private-use scheme ({@link #checkRedirectUri}); a code is never issued without an explicit user
 * action on a non-frameable page (consent defense); PKCE S256 is mandatory; codes are one-time (replay defense); the {@code state}
 * parameter round-trips untouched (CSRF defense); access tokens are opaque {@link java.security.SecureRandom}
 * values, single-JVM (see {@link OAuthTokenStore}). Refresh tokens are long-lived (7 days) but bound
 * to the same platform session: if the session dies, the refresh token stops working too, and an
 * actively-refreshing client slides its session's idle expiry forward so it stays alive for as long
 * as it keeps refreshing within the 7-day window.</p>
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
			// Some MCP/OAuth clients (notably Claude.ai's connector) probe the standard OIDC
			// discovery path (.well-known/openid-configuration) as a fallback. We are not an OpenID
			// provider (no userinfo, no id_token), but answering this with our authorization-server
			// metadata — the same document clients fetch at /oauth/.well-known/oauth-authorization-server
			// — lets discovery succeed on either path.
			if ( ".well-known/openid-configuration".equals( sub ) )
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
		String issuer = issuer( p );
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
			grantTypes.add( "refresh_token" );
			doc.put( "grant_types_supported", grantTypes );
			java.util.List<String> methods = new java.util.ArrayList<String>();
			methods.add( "S256" );
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

	/**
	 * The user step. Nothing here ever redirects to the client without an explicit action by the user
	 * on a page this server rendered:
	 * <ul>
	 * <li>A live, logged-in session gets a <em>consent</em> page naming the client and the host it
	 * will be redirected to. Only a submission of that page (carrying its one-time, session-bound
	 * {@code consent_token}) issues a code. An attacker who links a logged-in victim to
	 * {@code /authorize} with their own registered client therefore gets a page, not a code.</li>
	 * <li>Otherwise the login form (which also names the client); submitting valid credentials there is
	 * the consent.</li>
	 * </ul>
	 * Both pages are served with anti-framing headers so they cannot be clickjacked.
	 */
	private HandleResult authorize( Object session, Map<String, Object> p ) throws Exception
	{
		String clientId = first( p.get( "client_id" ) );
		String redirectUri = first( p.get( "redirect_uri" ) );
		String state = first( p.get( "state" ) );
		String challenge = first( p.get( "code_challenge" ) );
		String challengeMethod = first( p.get( "code_challenge_method" ) );
		String resource = first( p.get( "resource" ) );
		AuthorizeRequest req = new AuthorizeRequest( clientId, redirectUri, state, challenge, challengeMethod, resource );

		// Validation — every failure is an error page (no redirect: we must not bounce the user to an
		// unvalidated URI before we know the request is sane).
		String issuer = issuer( p );
		String bad = validateAuthorize( clientId, redirectUri, challenge, challengeMethod, resource, issuer );
		if ( bad != null )
			return htmlPage( 400, errorPage( bad ) );

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
		{
			String consentToken = first( p.get( "consent_token" ) );
			if ( consentToken.isEmpty() )
				return htmlPage( 200, consentPage( req, user, newConsent( req, sessionId, user ) ) );

			OAuthTokenStore.Consent consent = OAuthTokenStore.consumeConsent( consentToken );
			if ( consent == null || !consent.matches( sessionId, user, clientId, redirectUri, challenge ) )
			{
				// Stale, replayed, or minted for a different session/request: ask again.
				log.warning( "MCP OAuth consent rejected (invalid or mismatched consent_token) user=" + user
						+ " client=" + clientId );
				return htmlPage( 200, consentPage( req, user, newConsent( req, sessionId, user ) ) );
			}
			if ( first( p.get( "approve" ) ).isEmpty() )
				return denied( redirectUri, state );
			return issueCode( clientId, redirectUri, challenge, state, sessionId, user );
		}

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
				return issueCode( clientId, redirectUri, challenge, state, fresh, user );
			return htmlPage( 200, formPage( "login failed — check your credentials", req ) );
		}

		// Path 3: no credentials yet — show the form.
		return htmlPage( 200, formPage( null, req ) );
	}

	/**
	 * Validate the authorize request; {@code null} when valid. PKCE with {@code S256} is mandatory
	 * (OAuth 2.1 / MCP authorization spec). The {@code resource} indicator (RFC 8707) must be absent or
	 * refer to this MCP server (see {@link McpOAuthConfig#isMcpResource}) — a different resource would
	 * mean the code is being requested for another server, which this AS does not serve.
	 */
	static String validateAuthorize( String clientId, String redirectUri, String challenge,
			String challengeMethod, String resource, String issuer )
	{
		String bad = McpOAuthConfig.checkClient( clientId, redirectUri );
		if ( bad != null )
			return bad;
		if ( !"S256".equals( challengeMethod ) )
			return "PKCE is required: code_challenge_method must be S256";
		// RFC 7636 §4.2: BASE64URL(SHA256(verifier)) is exactly 43 characters.
		if ( !challenge.matches( "[A-Za-z0-9_-]{43}" ) )
			return "code_challenge must be a base64url-encoded SHA-256 digest (43 characters)";
		if ( !resource.isEmpty() && !McpOAuthConfig.isMcpResource( resource, issuer ) )
			return "unsupported resource";
		return null;
	}

	private static String newConsent( AuthorizeRequest req, String sessionId, String user )
	{
		return OAuthTokenStore.issueConsent( sessionId, user, req.clientId, req.redirectUri, req.challenge ).value;
	}

	private HandleResult issueCode( String clientId, String redirectUri, String challenge, String state,
			String sessionId, String user )
	{
		OAuthTokenStore.AuthCode code = OAuthTokenStore.issueCode( clientId, redirectUri, challenge,
				sessionId, user );
		log.info( "MCP OAuth code issued user=" + user + " client=" + clientId );
		return redirect( redirectUri, "code=" + urlEncode( code.value ), state );
	}

	/** The user declined on the consent page: RFC 6749 §4.1.2.1 {@code access_denied} to the (validated) redirect URI. */
	private HandleResult denied( String redirectUri, String state )
	{
		return redirect( redirectUri, "error=access_denied", state );
	}

	private static HandleResult redirect( String redirectUri, String query, String state )
	{
		String sep = redirectUri.indexOf( '?' ) >= 0 ? "&" : "?";
		String location = redirectUri + sep + query + ( state.isEmpty() ? "" : "&state=" + urlEncode( state ) );
		Map<String, String> headers = new LinkedHashMap<String, String>();
		headers.put( "Location", location );
		headers.put( "Cache-Control", "no-store" );
		return new HandleResult( 302, "text/plain", headers, "" );
	}

	/** The validated parameters of an authorize request, carried through the login/consent pages. */
	private static final class AuthorizeRequest
	{
		final String clientId;
		final String redirectUri;
		final String state;
		final String challenge;
		final String challengeMethod;
		final String resource;

		AuthorizeRequest( String clientId, String redirectUri, String state, String challenge,
				String challengeMethod, String resource )
		{
			this.clientId = clientId;
			this.redirectUri = redirectUri;
			this.state = state;
			this.challenge = challenge;
			this.challengeMethod = challengeMethod;
			this.resource = resource;
		}
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
		if ( uris.size() > MAX_REDIRECT_URIS )
			return new HandleResult( 400, "application/json",
					errorJson( "invalid_redirect_uri", "at most " + MAX_REDIRECT_URIS + " redirect_uris are allowed" ) );
		for ( String uri : uris )
		{
			String bad = checkRedirectUri( uri );
			if ( bad != null )
				return new HandleResult( 400, "application/json",
						errorJson( "invalid_redirect_uri", bad ) ); // not echoing the URI: errorJson does not escape
		}

		String clientName = body.get( "client_name" ) == null ? "" : body.get( "client_name" ).toString();
		String clientSecret = body.get( "client_secret" ) == null ? null : body.get( "client_secret" ).toString();
		if ( clientSecret != null && clientSecret.isEmpty() )
			clientSecret = null; // treat an empty secret as a public client

		OAuthTokenStore.RegisteredClient client = OAuthTokenStore.registerClient( uris, clientName, clientSecret );
		log.info( "MCP OAuth client registered id=" + client.clientId + " name=" + clientName
				+ " uris=" + uris + " confidential=" + ( clientSecret != null ) );

		String issuer = issuer( p );
		Map<String, Object> out = new LinkedHashMap<String, Object>();
		out.put( "client_id", client.clientId );
		if ( clientSecret != null )
			out.put( "client_secret", clientSecret );
		out.put( "client_id_issuer", issuer == null ? "" : issuer );
		out.put( "client_name", clientName );
		out.put( "redirect_uris", uris );
		out.put( "grant_types", java.util.Arrays.asList( "authorization_code", "refresh_token" ) );
		out.put( "response_types", java.util.Arrays.asList( "code" ) );
		out.put( "token_endpoint_auth_method", clientSecret == null ? "none" : "client_secret_basic" );
		return new HandleResult( 201, "application/json", mapper.writeValueAsString( out ) );
	}

	/** Upper bound on the redirect URIs one self-registered client may declare. */
	static final int MAX_REDIRECT_URIS = 10;

	/**
	 * Validate a redirect URI offered at registration (RFC 7591 §2, RFC 8252, OAuth 2.1 §2.3.1);
	 * {@code null} when acceptable. Allowed: {@code https} with a host; {@code http} only to a
	 * loopback host (native clients such as Claude Code listen on localhost); and private-use schemes
	 * in reverse-domain form ({@code com.example.app:/cb}, RFC 8252 §7.1). Rejected: fragments,
	 * user-info, and everything else — notably {@code javascript:}, {@code data:}, {@code file:} and
	 * plain {@code http} to a remote host, where the authorization code would leak.
	 */
	static String checkRedirectUri( String uri )
	{
		if ( uri.length() > 2048 )
			return "redirect_uri is too long";
		java.net.URI parsed;
		try
		{
			parsed = new java.net.URI( uri );
		}
		catch ( java.net.URISyntaxException e )
		{
			return "redirect_uri is not a valid URI";
		}
		String scheme = parsed.getScheme() == null ? "" : parsed.getScheme().toLowerCase( java.util.Locale.ROOT );
		if ( scheme.isEmpty() || !parsed.isAbsolute() )
			return "redirect_uri must be an absolute URI";
		if ( parsed.getRawFragment() != null )
			return "redirect_uri must not contain a fragment";
		if ( parsed.getRawUserInfo() != null )
			return "redirect_uri must not contain user info";
		String host = parsed.getHost();
		if ( "https".equals( scheme ) )
			return host == null || host.isEmpty() ? "https redirect_uri must have a host" : null;
		if ( "http".equals( scheme ) )
			return isLoopback( host ) ? null : "http redirect_uri is only allowed for a loopback host (use https)";
		// RFC 8252 §7.1 private-use scheme: must contain a period (reverse domain name).
		if ( scheme.indexOf( '.' ) > 0 && scheme.matches( "[a-z][a-z0-9+.-]*" ) )
			return null;
		return "unsupported redirect_uri scheme '" + scheme + "'";
	}

	private static boolean isLoopback( String host )
	{
		if ( host == null )
			return false;
		String h = host.toLowerCase( java.util.Locale.ROOT );
		return h.equals( "localhost" ) || h.equals( "[::1]" ) || h.equals( "::1" ) || h.matches( "127(\\.\\d{1,3}){3}" );
	}

	// ------------------------------------------------------------------ token

	private HandleResult token( Map<String, Object> p ) throws Exception
	{
		String grantType = first( p.get( "grant_type" ) );
		if ( "refresh_token".equals( grantType ) )
			return refreshToken( p );
		if ( !"authorization_code".equals( grantType ) )
			return new HandleResult( 400, "application/json",
					errorJson( "unsupported_grant_type", "only authorization_code and refresh_token are supported" ) );

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
		// PKCE is mandatory: /authorize never issues a code without an S256 challenge.
		if ( stored.codeChallenge == null || stored.codeChallenge.isEmpty() || verifier.isEmpty()
				|| !OAuthTokenStore.pkceMatches( stored.codeChallenge, OAuthTokenStore.pkceChallenge( verifier ) ) )
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
		String refreshToken = OAuthTokenStore.refreshTokenFor( token.value );
		log.info( "MCP OAuth token issued user=" + user + " client=" + stored.clientId );
		Map<String, Object> body = new LinkedHashMap<String, Object>();
		body.put( "access_token", token.value );
		body.put( "token_type", "Bearer" );
		body.put( "expires_in", OAuthTokenStore.ACCESS_TOKEN_TTL_MS / 1000L );
		if ( refreshToken != null )
		{
			body.put( "refresh_token", refreshToken );
			body.put( "refresh_expires_in", OAuthTokenStore.REFRESH_TOKEN_TTL_MS / 1000L );
		}
		return new HandleResult( 200, "application/json", mapper.writeValueAsString( body ) );
	}

	/**
	 * The {@code refresh_token} grant (RFC 6749 §6): exchange a still-valid refresh token for a fresh
	 * access token bound to the same session. This is what lets a connected MCP client (e.g. Claude
	 * Web's connector) stay authenticated across the 1-hour access-token window WITHOUT re-running
	 * the authorization flow. The session's idle expiry is slid forward on each refresh (via
	 * {@link SecurityManager#touchSessionExpiry}), so an actively-refreshing client keeps its session
	 * alive for as long as it keeps refreshing within the 7-day refresh-token window.
	 *
	 * <p>Per the standard, a reused refresh token within its lifetime is valid (we do not rotate it).
	 * An expired/unknown refresh token or a dead session yields {@code invalid_grant} (400).</p>
	 */
	private HandleResult refreshToken( Map<String, Object> p ) throws Exception
	{
		String refreshTokenValue = first( p.get( "refresh_token" ) );
		String clientId = first( p.get( "client_id" ) );
		OAuthTokenStore.RefreshToken stored = OAuthTokenStore.consumeRefresh( refreshTokenValue );
		if ( stored == null )
			return new HandleResult( 400, "application/json",
					errorJson( "invalid_grant", "unknown or expired refresh token (or its session has ended)" ) );
		if ( !clientId.isEmpty() && !stored.clientId.equals( clientId ) )
			return new HandleResult( 400, "application/json",
					errorJson( "invalid_grant", "client_id does not match the refresh token" ) );

		// Mint a fresh access token paired with the SAME refresh token value (no rotation: the client
		// keeps using the refresh token it was handed).
		OAuthTokenStore.AccessToken token = OAuthTokenStore.reissue( stored.sessionId, stored.clientId,
				stored.user, stored.value );
		// Sliding window: keep the session alive.
		SecurityManager.touchSessionExpiry( stored.sessionId );

		log.info( "MCP OAuth token refreshed user=" + stored.user + " client=" + stored.clientId );
		Map<String, Object> body = new LinkedHashMap<String, Object>();
		body.put( "access_token", token.value );
		body.put( "token_type", "Bearer" );
		body.put( "expires_in", OAuthTokenStore.ACCESS_TOKEN_TTL_MS / 1000L );
		body.put( "refresh_token", stored.value ); // echo the same refresh token (no rotation)
		return new HandleResult( 200, "application/json", mapper.writeValueAsString( body ) );
	}

	// ------------------------------------------------------------------ pages

	/**
	 * An HTML response with anti-framing headers (clickjacking defense for the login and consent
	 * pages) and no caching (the consent page carries a one-time token).
	 */
	private static HandleResult htmlPage( int status, String body )
	{
		Map<String, String> headers = new LinkedHashMap<String, String>();
		headers.put( "X-Frame-Options", "DENY" );
		headers.put( "Content-Security-Policy", "frame-ancestors 'none'" );
		headers.put( "Cache-Control", "no-store" );
		headers.put( "Referrer-Policy", "no-referrer" );
		return new HandleResult( status, "text/html; charset=utf-8", headers, body );
	}

	private static StringBuilder pageStart()
	{
		StringBuilder sb = new StringBuilder();
		sb.append( "<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"utf-8\">\n" );
		sb.append( "<title>Sign in to BioUML (MCP)</title>\n" );
		sb.append( "<style>body{font-family:sans-serif;max-width:28rem;margin:3rem auto;padding:0 1rem}" );
		sb.append( "input{width:100%;margin:0.3rem 0;padding:0.4rem;box-sizing:border-box}" );
		sb.append( "button{margin-top:0.8rem;margin-right:0.5rem;padding:0.4rem 1rem}" );
		sb.append( "code{word-break:break-all}</style>\n</head>\n<body>\n" );
		sb.append( "<h2>BioUML — MCP sign-in</h2>\n" );
		return sb;
	}

	private static String errorPage( String error )
	{
		StringBuilder sb = pageStart();
		sb.append( "<p style=\"color:#a00\">" ).append( escape( error ) ).append( "</p>\n" );
		sb.append( "</body>\n</html>\n" );
		return sb.toString();
	}

	/**
	 * Who is asking and where the user will be sent. The redirect host is what the user should judge
	 * by: the client name is self-asserted at registration and can say anything.
	 */
	private static void appendClientInfo( StringBuilder sb, AuthorizeRequest req )
	{
		OAuthTokenStore.RegisteredClient dynamic = OAuthTokenStore.findClient( req.clientId );
		String name = dynamic != null && dynamic.clientName != null && !dynamic.clientName.isEmpty()
				? dynamic.clientName : req.clientId;
		sb.append( "<p>The application <b>" ).append( escape( name ) ).append( "</b>" );
		if ( dynamic != null )
			sb.append( " (self-registered)" );
		sb.append( " is requesting access to your BioUML account. It will be able to read, change and "
				+ "delete your data and run analyses and scripts on your behalf.</p>\n" );
		sb.append( "<p>After you allow it, you will be sent to <b>" ).append( escape( redirectHost( req.redirectUri ) ) )
				.append( "</b><br><code>" ).append( escape( req.redirectUri ) ).append( "</code></p>\n" );
		sb.append( "<p>Only continue if you started this connection yourself and recognize this address.</p>\n" );
	}

	private static String redirectHost( String redirectUri )
	{
		try
		{
			java.net.URI uri = new java.net.URI( redirectUri );
			String host = uri.getHost();
			return host != null ? host : uri.getScheme() + ":";
		}
		catch ( Exception e )
		{
			return redirectUri;
		}
	}

	/** The original authorize parameters as hidden fields, so the page's POST re-validates them. */
	private static void appendHiddenFields( StringBuilder sb, AuthorizeRequest req )
	{
		hidden( sb, "client_id", req.clientId );
		hidden( sb, "redirect_uri", req.redirectUri );
		hidden( sb, "state", req.state );
		hidden( sb, "code_challenge", req.challenge );
		hidden( sb, "code_challenge_method", req.challengeMethod );
		hidden( sb, "resource", req.resource );
	}

	private static void hidden( StringBuilder sb, String name, String value )
	{
		if ( value == null || value.isEmpty() )
			return;
		sb.append( "<input type=\"hidden\" name=\"" ).append( name ).append( "\" value=\"" )
				.append( escape( value ) ).append( "\">\n" );
	}

	/**
	 * Render the login form. All reflected request values are HTML-escaped (they are attacker-
	 * controlled: client_id / redirect_uri / state come from the MCP client, not the user).
	 */
	private static String formPage( String error, AuthorizeRequest req )
	{
		StringBuilder sb = pageStart();
		appendClientInfo( sb, req );
		if ( error != null )
			sb.append( "<p style=\"color:#a00\">" ).append( escape( error ) ).append( "</p>\n" );
		sb.append( "<form method=\"post\" action=\"\">\n" );
		appendHiddenFields( sb, req );
		sb.append( "<label>Username<br><input type=\"text\" name=\"username\" autocomplete=\"username\"></label>\n" );
		sb.append( "<label>Password<br><input type=\"password\" name=\"password\" autocomplete=\"current-password\"></label>\n" );
		sb.append( "<button type=\"submit\">Sign in and allow</button>\n" );
		sb.append( "</form>\n</body>\n</html>\n" );
		return sb.toString();
	}

	/** The consent page for an already-signed-in user: Allow / Deny, carrying a one-time consent token. */
	private static String consentPage( AuthorizeRequest req, String user, String consentToken )
	{
		StringBuilder sb = pageStart();
		sb.append( "<p>Signed in as <b>" ).append( escape( user ) ).append( "</b>.</p>\n" );
		appendClientInfo( sb, req );
		sb.append( "<form method=\"post\" action=\"\">\n" );
		appendHiddenFields( sb, req );
		hidden( sb, "consent_token", consentToken );
		sb.append( "<button type=\"submit\" name=\"approve\" value=\"1\">Allow</button>\n" );
		sb.append( "<button type=\"submit\" name=\"deny\" value=\"1\">Deny</button>\n" );
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

	/**
	 * The issuer for this request, derived from the request's public hostname. The proxy's
	 * {@code X-Forwarded-Host} / {@code X-Forwarded-Proto} take precedence over the {@code Host}
	 * header (which a reverse proxy typically rewrites to the internal backend); see
	 * {@link McpOAuthConfig#issuer(String, String, String)}.
	 */
	private static String issuer( Map<String, Object> p )
	{
		return McpOAuthConfig.issuer( first( p.get( "Host" ) ),
				first( p.get( "X-Forwarded-Host" ) ), first( p.get( "X-Forwarded-Proto" ) ) );
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
