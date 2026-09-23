package biouml.plugins.mcp.oauth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration for the BioUML MCP OAuth 2.1 authorization server (see {@link McpOAuthServlet}).
 *
 * <p>All values come from system properties so a deployment can turn the feature on and point it at
 * its own public URL without code changes (mirroring how {@code securityProviderLink} is configured
 * for the BioStore provider):</p>
 * <ul>
 * <li>{@value #ISSUER_PROPERTY} — the public base URL of this authorization server
 * (e.g. {@code https://biouml2test.biouml.org/biouml/oauth}). When set, it is used verbatim and
 * wins over every derived value. When unset, the base is derived from the request's public
 * hostname — {@code X-Forwarded-Host} (with {@code X-Forwarded-Proto}) when present, else the
 * {@code Host} header — so a single build works on every deployment <em>including behind a proxy
 * that forwards an internal {@code Host}</em>.</li>
 * <li>{@value #PUBLIC_HOST_PROPERTY} — an optional, operator-declared public hostname (e.g.
 * {@code biouml2test.biouml.org}). When set and the derived public host differs from it, a
 * one-time WARNING is logged — this surfaces the classic misconfiguration where the app sits
 * behind a proxy and would otherwise emit an unreachable internal URL in its OAuth metadata.</li>
 * <li>{@value #CLIENTS_PROPERTY} — a static client allow-list. Format:
 * {@code clientId=uri1|uri2;clientId2=uri3} — each client maps to the <em>exact</em> redirect URIs
 * it may use. (RFC 7591 dynamic registration is also supported and takes precedence, so this list
 * is only needed for fixed-id clients.)</li>
 * </ul>
 */
public final class McpOAuthConfig
{
	private static final Logger log = Logger.getLogger( McpOAuthConfig.class.getName() );

	/** System property: the public base URL of the authorization server (issuer). */
	public static final String ISSUER_PROPERTY = "biouml.mcp.oauth.issuer";

	/** System property: the operator-declared public hostname, used to detect a mis-derived issuer. */
	public static final String PUBLIC_HOST_PROPERTY = "biouml.mcp.oauth.public.host";

	/** System property: the static client allow-list (see the class javadoc for the format). */
	public static final String CLIENTS_PROPERTY = "biouml.mcp.oauth.clients";

	/** The MCP resource these tokens are issued for (the canonical URI in RFC 8707 terms). */
	public static final String RESOURCE = "/mcp";

	/** One-time warning guard: log the misconfiguration at most once per JVM. */
	private static volatile boolean warned = false;

	private McpOAuthConfig()
	{
	}

	/**
	 * The issuer (authorization server base) URL.
	 *
	 * <p>Resolution order:</p>
	 * <ol>
	 * <li>{@value #ISSUER_PROPERTY}, when set — used verbatim (trailing slash trimmed).</li>
	 * <li>Otherwise derived from the request's <b>public</b> hostname: {@code X-Forwarded-Host}
	 * (taking the first host, and the first {@code X-Forwarded-Proto} as the scheme) when present,
	 * else the {@code Host} header — as {@code <scheme>://<host>/biouml/oauth}.</li>
	 * </ol>
	 *
	 * <p>Returning {@code null} (no hostname available) is treated by callers as "OAuth disabled".</p>
	 *
	 * @param host             the request {@code Host} header value; may be null
	 * @param forwardedHost    the request {@code X-Forwarded-Host} value; may be null
	 * @param forwardedProto   the request {@code X-Forwarded-Proto} value; may be null
	 */
	public static String issuer( String host, String forwardedHost, String forwardedProto )
	{
		String configured = System.getProperty( ISSUER_PROPERTY );
		if ( configured != null && !configured.trim().isEmpty() )
		{
			String base = configured.trim();
			while ( base.endsWith( "/" ) )
				base = base.substring( 0, base.length() - 1 );
			return base;
		}

		// Prefer the proxy-forwarded public host over the (possibly internal) Host header.
		String publicHost = firstCommaValue( forwardedHost );
		if ( publicHost == null || publicHost.isEmpty() )
			publicHost = firstCommaValue( host );
		if ( publicHost == null || publicHost.isEmpty() )
			return null;

		String scheme = "https";
		if ( forwardedProto != null && !forwardedProto.trim().isEmpty() )
		{
			String proto = firstCommaValue( forwardedProto ).trim().toLowerCase();
			if ( proto.equals( "http" ) || proto.equals( "https" ) )
				scheme = proto;
		}
		String issuer = scheme + "://" + publicHost + "/biouml/oauth";

		// Misconfiguration guard: if the operator declared the public host and we derived a
		// different one, the app is almost certainly behind a proxy that forwards an internal Host.
		warnIfHostMismatch( publicHost );
		return issuer;
	}

	/**
	 * Backward-compatible overload (no forwarded headers) — equivalent to passing {@code null} for
	 * both. Kept for existing callers and tests.
	 */
	public static String issuer( String host )
	{
		return issuer( host, null, null );
	}

	/**
	 * If {@value #PUBLIC_HOST_PROPERTY} is set and differs from the derived public host, log a
	 * one-time WARNING naming both values. This turns a silent "unreachable internal URL in the
	 * OAuth metadata" failure into an obvious, greppable log line.
	 */
	private static void warnIfHostMismatch( String derivedHost )
	{
		String expected = System.getProperty( PUBLIC_HOST_PROPERTY );
		if ( expected == null || expected.trim().isEmpty() )
			return;
		String exp = expected.trim();
		// Compare host:port ignoring scheme; a portless expected host matches a derived "host:port"
		// when the port is the scheme default.
		if ( hostEquals( exp, derivedHost ) )
			return;
		if ( warned )
			return;
		synchronized ( McpOAuthConfig.class )
		{
			if ( warned )
				return;
			warned = true;
			log.log( Level.WARNING,
					"MCP OAuth: derived public host '" + derivedHost
							+ "' differs from the configured public host '" + exp
							+ "'. The OAuth metadata URLs will point at the derived host, which may be "
							+ "unreachable by external MCP clients (e.g. the app is behind a proxy that "
							+ "forwards an internal Host header). Set " + ISSUER_PROPERTY
							+ "=https://" + exp + "/biouml/oauth (or ensure the proxy forwards the public "
							+ "X-Forwarded-Host) so clients can reach the authorization server." );
		}
	}

	/** True if two {@code host} or {@code host:port} values refer to the same host (port-agnostic when default). */
	static boolean hostEquals( String a, String b )
	{
		if ( a == null || b == null )
			return false;
		String h1 = stripPort( a.trim() );
		String h2 = stripPort( b.trim() );
		return h1.equalsIgnoreCase( h2 ) && portOf( a ) == portOf( b );
	}

	/** Strip a {@code :port} suffix (and any path) from a host value. */
	private static String stripPort( String host )
	{
		int slash = host.indexOf( '/' );
		if ( slash >= 0 )
			host = host.substring( 0, slash );
		int colon = host.lastIndexOf( ':' );
		if ( colon >= 0 && !host.substring( colon + 1 ).contains( ":" ) )
			host = host.substring( 0, colon );
		return host;
	}

	/** The explicit port of a host value, or the scheme default (443) when absent. */
	private static int portOf( String host )
	{
		int slash = host.indexOf( '/' );
		if ( slash >= 0 )
			host = host.substring( 0, slash );
		int colon = host.lastIndexOf( ':' );
		if ( colon < 0 )
			return 443;
		try
		{
			return Integer.parseInt( host.substring( colon + 1 ) );
		}
		catch ( NumberFormatException e )
		{
			return 443;
		}
	}

	/** The first comma-separated value of a forwarded header (proxies chain multiple). */
	private static String firstCommaValue( String value )
	{
		if ( value == null )
			return null;
		String v = value.trim();
		int comma = v.indexOf( ',' );
		if ( comma >= 0 )
			v = v.substring( 0, comma );
		// A Host header may carry a path; keep only host:port.
		int slash = v.indexOf( '/' );
		if ( slash >= 0 )
			v = v.substring( 0, slash );
		return v.trim().isEmpty() ? null : v.trim();
	}

	/** The absolute MCP resource URL (the {@code resource} value in the metadata documents). */
	public static String resourceUrl( String issuer )
	{
		// The issuer is "<origin>/biouml/oauth"; the resource lives at "<origin>/biouml/mcp".
		return issuer == null ? null : issuer.substring( 0, issuer.length() - "/oauth".length() ) + RESOURCE;
	}

	/** The protected-resource metadata (RFC 9728) document URL. */
	public static String protectedResourceMetadataUrl( String issuer )
	{
		return issuer + "/.well-known/oauth-protected-resource";
	}

	/** The authorization-server metadata (RFC 8414) document URL. */
	public static String authorizationServerMetadataUrl( String issuer )
	{
		return issuer + "/.well-known/oauth-authorization-server";
	}

	/** The {@code /authorize} endpoint URL. */
	public static String authorizationEndpoint( String issuer )
	{
		return issuer + "/authorize";
	}

	/** The {@code /token} endpoint URL. */
	public static String tokenEndpoint( String issuer )
	{
		return issuer + "/token";
	}

	/**
	 * The static client allow-list: {@code clientId → [exact redirect URIs]}. The property is read on
	 * every call (cheap string parse) so tests can flip it via {@link System#setProperty} without
	 * reloading.
	 */
	public static Map<String, List<String>> clients()
	{
		String raw = System.getProperty( CLIENTS_PROPERTY );
		if ( raw == null || raw.trim().isEmpty() )
			return Collections.emptyMap();
		Map<String, List<String>> clients = new LinkedHashMap<String, List<String>>();
		for ( String entry : raw.split( ";" ) )
		{
			int eq = entry.indexOf( '=' );
			if ( eq <= 0 )
				continue;
			String clientId = entry.substring( 0, eq ).trim();
			String uris = entry.substring( eq + 1 ).trim();
			if ( clientId.isEmpty() || uris.isEmpty() )
				continue;
			List<String> list = new ArrayList<String>();
			for ( String uri : uris.split( "\\|" ) )
			{
				if ( !uri.trim().isEmpty() )
					list.add( uri.trim() );
			}
			if ( !list.isEmpty() )
				clients.put( clientId, list );
		}
		return clients;
	}

	/**
	 * Validate an authorization request's {@code client_id} + {@code redirect_uri} against the
	 * allow-list. The redirect URI must match a registered value <em>exactly</em> (string equality) —
	 * this is the open-redirect defense; prefix/suffix matches are deliberately rejected.
	 *
	 * @return {@code null} if valid, otherwise a human-readable reason (safe to show in logs; the
	 * client itself only ever receives the {@code unauthorized_client} error code)
	 */
	public static String checkClient( String clientId, String redirectUri )
	{
		if ( clientId == null || clientId.trim().isEmpty() )
			return "missing client_id";
		// A dynamically-registered client (RFC 7591) takes precedence; it carries its own exact
		// redirect-URI set. (A DCR client is in-memory and single-JVM, so it cannot collide with a
		// statically-configured one in practice — server-generated ids never match hand-set ids.)
		OAuthTokenStore.RegisteredClient dynamic = OAuthTokenStore.findClient( clientId );
		if ( dynamic != null )
			return OAuthTokenStore.clientAllowsRedirect( dynamic, redirectUri ) ? null
					: "redirect_uri not registered for this client";
		// Fall back to the static allow-list (biouml.mcp.oauth.clients).
		Map<String, List<String>> clients = clients();
		if ( clients.isEmpty() )
			return "unknown client_id (no clients configured and none registered)";
		List<String> uris = clients.get( clientId );
		if ( uris == null )
			return "unknown client_id";
		if ( redirectUri == null || !uris.contains( redirectUri ) )
			return "redirect_uri not registered for this client";
		return null;
	}

	/**
	 * Whether a {@code resource} indicator value (RFC 8707) refers to this MCP server. Accepts the
	 * canonical absolute URL (e.g. {@code https://host/biouml/mcp}) or the context-relative form
	 * ({@code /biouml/mcp}); anything else is a foreign resource this AS does not serve.
	 */
	public static boolean isMcpResource( String resource, String issuer )
	{
		if ( resource == null || resource.isEmpty() )
			return false;
		// Context-relative form ("/mcp" or "/biouml/mcp") — what a same-origin browser client sends.
		if ( resource.equals( RESOURCE ) || resource.equals( "/biouml" + RESOURCE ) )
			return true;
		if ( issuer == null )
			return false;
		// The canonical absolute URL emitted in the PRM document (issuer "<origin>/biouml/oauth"
		// → resource "<origin>/biouml/mcp").
		String absolute = issuer.substring( 0, issuer.length() - "/oauth".length() ) + "/mcp";
		return resource.equals( absolute );
	}
}
