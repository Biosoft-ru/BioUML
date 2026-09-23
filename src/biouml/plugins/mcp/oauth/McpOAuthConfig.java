package biouml.plugins.mcp.oauth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for the BioUML MCP OAuth 2.1 authorization server (see {@link McpOAuthServlet}).
 *
 * <p>All values come from system properties so a deployment can turn the feature on and point it at
 * its own public URL without code changes (mirroring how {@code securityProviderLink} is configured
 * for the BioStore provider):</p>
 * <ul>
 * <li>{@value #ISSUER_PROPERTY} — the public base URL of this authorization server
 * (e.g. {@code https://biouml2test.biouml.org/biouml/oauth}). When unset, the base is derived from
 * the request {@code Host} header ({@code https://<host>/biouml/oauth}) so a single build works on
 * every deployment.</li>
 * <li>{@value #CLIENTS_PROPERTY} — a static client allow-list (RFC 7591 dynamic registration is
 * intentionally not implemented). Format:
 * {@code clientId=uri1|uri2;clientId2=uri3} — each client maps to the <em>exact</em> redirect URIs
 * it may use. With no clients configured the authorization server refuses every
 * {@code /authorize} request ({@code unauthorized_client}), i.e. OAuth is effectively off.</li>
 * </ul>
 */
public final class McpOAuthConfig
{
	/** System property: the public base URL of the authorization server (issuer). */
	public static final String ISSUER_PROPERTY = "biouml.mcp.oauth.issuer";

	/** System property: the static client allow-list (see the class javadoc for the format). */
	public static final String CLIENTS_PROPERTY = "biouml.mcp.oauth.clients";

	/** The MCP resource these tokens are issued for (the canonical URI in RFC 8707 terms). */
	public static final String RESOURCE = "/mcp";

	private McpOAuthConfig()
	{
	}

	/**
	 * The issuer (authorization server base) URL: the configured property, or
	 * {@code https://<host>/biouml/oauth} derived from the request {@code Host} header. Returns
	 * {@code null} if neither is available — callers treat that as "OAuth disabled".
	 *
	 * @param host the request {@code Host} header value (e.g. {@code biouml2test.biouml.org:443});
	 * may be null
	 */
	public static String issuer( String host )
	{
		String configured = System.getProperty( ISSUER_PROPERTY );
		if ( configured != null && !configured.trim().isEmpty() )
		{
			String base = configured.trim();
			while ( base.endsWith( "/" ) )
				base = base.substring( 0, base.length() - 1 );
			return base;
		}
		if ( host != null && !host.trim().isEmpty() )
		{
			String h = host.trim();
			int path = h.indexOf( '/' );
			if ( path >= 0 )
				h = h.substring( 0, path ); // strip a non-default port? no — keep host:port, drop any path
			if ( h.isEmpty() )
				return null;
			return "https://" + h + "/biouml/oauth";
		}
		return null;
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
		Map<String, List<String>> clients = clients();
		if ( clients.isEmpty() )
			return "no clients configured";
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
