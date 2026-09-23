package biouml.plugins.mcp.oauth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import ru.biosoft.access.security.SecurityManager;

/**
 * In-memory store for the BioUML MCP OAuth 2.1 authorization server: one-time authorization codes
 * (issued by {@code /authorize}, consumed by {@code /token}) and opaque bearer access tokens
 * (issued by {@code /token}, validated by the MCP endpoint on every request).
 *
 * <p><b>Single-JVM by design.</b> Both stores are in-memory {@link ConcurrentHashMap}s, so a
 * deployment must run one JVM per instance (biouml2test does). A multi-node deployment would need a
 * shared backend (e.g. the support database) — out of scope for v1.</p>
 *
 * <p><b>Token ↔ session coupling.</b> An access token does not store a user directly: it references
 * a BioUML session id whose {@code UserPermissions} were copied from the authorizing user's session
 * (via {@link SecurityManager#commonLoginViaOtherSession}). Every token use therefore re-checks
 * {@link SecurityManager#isSessionDead}, so a token can never outlive the platform session it was
 * minted from, and killing the session revokes the token without touching the store. Tokens also
 * carry their own expiry (1 hour, matching {@code UserPermissions.TIMEOUT}) so the store stays
 * bounded even if sessions outlive them.</p>
 */
public final class OAuthTokenStore
{
	/** Access-token lifetime (1 h) — chosen to match {@code UserPermissions.TIMEOUT}. */
	public static final long ACCESS_TOKEN_TTL_MS = 60L * 60L * 1000L;

	/** Authorization-code lifetime (10 min) — standard OAuth bound for the code → token exchange. */
	public static final long AUTH_CODE_TTL_MS = 10L * 60L * 1000L;

	/**
	 * Lifetime of a dynamically-registered client (RFC 7591). Clients are in-memory and the
	 * deployment is single-JVM, so a registration is useful for as long as this process lives; 24 h
	 * bounds how long a leaked client_id stays valid and matches the "ephemeral, re-register on
	 * connect" model MCP clients use.
	 */
	public static final long CLIENT_TTL_MS = 24L * 60L * 60L * 1000L;

	/** Access-token prefix: a namespace sentinel so a token can never be misread as a Basic payload. */
	public static final String TOKEN_PREFIX = "mcp_at_";

	/** Authorization-code prefix (for log grepping; codes are opaque otherwise). */
	public static final String CODE_PREFIX = "mcp_ac_";

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final ConcurrentMap<String, AuthCode> CODES = new ConcurrentHashMap<String, AuthCode>();
	private static final ConcurrentMap<String, AccessToken> TOKENS = new ConcurrentHashMap<String, AccessToken>();
	private static final ConcurrentMap<String, RegisteredClient> CLIENTS =
			new ConcurrentHashMap<String, RegisteredClient>();

	private OAuthTokenStore()
	{
	}

	/**
	 * An OAuth client registered dynamically (RFC 7591) or via the static allow-list. Holds the
	 * exact redirect URIs the client may use (the open-redirect defense) and its authentication mode
	 * (public/PKCE vs. confidential with a secret).
	 */
	public static final class RegisteredClient
	{
		/** The client_id (the value the client presents at {@code /authorize} and {@code /token}). */
		public final String clientId;
		/** The exact redirect URIs the client is allowed to use. */
		public final java.util.List<String> redirectUris;
		/** The human-readable client name (informational). */
		public final String clientName;
		/** {@code null} for a public client (PKCE auth); the secret for a confidential one. */
		public final String clientSecret;
		/** Epoch millis when this registration expires (DCR clients are single-JVM and ephemeral). */
		public final long expiresAt;

		RegisteredClient( String clientId, java.util.List<String> redirectUris, String clientName,
				String clientSecret, long ttlMs )
		{
			this.clientId = clientId;
			this.redirectUris = redirectUris;
			this.clientName = clientName;
			this.clientSecret = clientSecret;
			this.expiresAt = System.currentTimeMillis() + ttlMs;
		}

		boolean expired()
		{
			return System.currentTimeMillis() > expiresAt;
		}
	}

	/** A one-time authorization code issued by {@code /authorize}. */
	public static final class AuthCode
	{
		/** The (opaque) code string. */
		public final String value;
		/** The client that will exchange it. */
		public final String clientId;
		/** The exact redirect URI the exchange must present. */
		public final String redirectUri;
		/** The PKCE {@code code_challenge} (S256) the exchange must satisfy. */
		public final String codeChallenge;
		/** The authorizing user's session id (permissions are copied from it at exchange time). */
		public final String authorizingSessionId;
		/** The authenticated username (recorded for logging). */
		public final String user;
		/** Absolute expiry (epoch millis). */
		public final long expiresAt;

		AuthCode( String value, String clientId, String redirectUri, String codeChallenge,
				String authorizingSessionId, String user )
		{
			this.value = value;
			this.clientId = clientId;
			this.redirectUri = redirectUri;
			this.codeChallenge = codeChallenge;
			this.authorizingSessionId = authorizingSessionId;
			this.user = user;
			this.expiresAt = System.currentTimeMillis() + AUTH_CODE_TTL_MS;
		}

		boolean expired()
		{
			return System.currentTimeMillis() > expiresAt;
		}
	}

	/** An opaque bearer access token issued by {@code /token}. */
	public static final class AccessToken
	{
		/** The (opaque) token string. */
		public final String value;
		/** The BioUML session the token maps to (validated on every use). */
		public final String sessionId;
		/** The client the token was issued to. */
		public final String clientId;
		/** The authenticated username (recorded for logging). */
		public final String user;
		/** Absolute expiry (epoch millis). */
		public final long expiresAt;

		AccessToken( String value, String sessionId, String clientId, String user )
		{
			this.value = value;
			this.sessionId = sessionId;
			this.clientId = clientId;
			this.user = user;
			this.expiresAt = System.currentTimeMillis() + ACCESS_TOKEN_TTL_MS;
		}

		boolean expired()
		{
			return System.currentTimeMillis() > expiresAt;
		}
	}

	// ------------------------------------------------------------------ codes

	/**
	 * Mint a one-time authorization code.
	 *
	 * @return the new code (never {@code null})
	 */
	public static AuthCode issueCode( String clientId, String redirectUri, String codeChallenge,
			String authorizingSessionId, String user )
	{
		String value = CODE_PREFIX + randomToken( 32 );
		AuthCode code = new AuthCode( value, clientId, redirectUri, codeChallenge, authorizingSessionId, user );
		CODES.put( value, code );
		return code;
	}

	/**
	 * Consume an authorization code (authorization-code exchange). Removal is the read: a code can
	 * be used exactly once, and an expired or unknown code returns {@code null}.
	 *
	 * @return the code's record, or {@code null} if unknown or expired
	 */
	public static AuthCode consumeCode( String value )
	{
		if ( value == null )
			return null;
		AuthCode code = CODES.remove( value );
		if ( code == null || code.expired() )
			return null;
		return code;
	}

	// ------------------------------------------------------------------ tokens

	/**
	 * Mint an access token bound to a fresh session (see the class javadoc for the session coupling).
	 *
	 * @param sessionId the fresh, already-populated session id the token maps to
	 * @param clientId  the client the token is issued to
	 * @param user      the authenticated username
	 * @return the new token (never {@code null})
	 */
	public static AccessToken issueToken( String sessionId, String clientId, String user )
	{
		String value = TOKEN_PREFIX + randomToken( 32 );
		AccessToken token = new AccessToken( value, sessionId, clientId, user );
		TOKENS.put( value, token );
		return token;
	}

	/**
	 * Validate a bearer token for MCP endpoint use: the token must exist, be unexpired, and its
	 * session must still be alive (a dead session revokes the token).
	 *
	 * @return the token record, or {@code null} if invalid
	 */
	public static AccessToken validate( String value )
	{
		if ( value == null || !value.startsWith( TOKEN_PREFIX ) )
			return null;
		AccessToken token = TOKENS.get( value );
		if ( token == null || token.expired() )
			return null;
		if ( SecurityManager.isSessionDead( token.sessionId ) )
			return null;
		return token;
	}

	/**
	 * Drop a token (e.g. after its session dies) so the store stays bounded. Idempotent.
	 */
	public static void revoke( String value )
	{
		if ( value != null )
			TOKENS.remove( value );
	}

	// ------------------------------------------------------------------ PKCE

	/**
	 * Compute the S256 {@code code_challenge} for a {@code code_verifier}:
	 * {@code BASE64URL( SHA-256( ASCII( verifier ) ) )} without padding (RFC 7636 §4.2).
	 *
	 * @return the challenge, or {@code null} if the SHA-256 provider is missing (it always is on a
	 * compliant JVM)
	 */
	public static String pkceChallenge( String verifier )
	{
		if ( verifier == null )
			return null;
		try
		{
			MessageDigest sha = MessageDigest.getInstance( "SHA-256" );
			byte[] digest = sha.digest( verifier.getBytes( StandardCharsets.US_ASCII ) );
			return Base64.getUrlEncoder().withoutPadding().encodeToString( digest );
		}
		catch ( NoSuchAlgorithmException e )
		{
			return null;
		}
	}

	/**
	 * Constant-time comparison of a computed challenge against the registered one.
	 */
	public static boolean pkceMatches( String expected, String actual )
	{
		if ( expected == null || actual == null )
			return false;
		byte[] a = expected.getBytes( StandardCharsets.US_ASCII );
		byte[] b = actual.getBytes( StandardCharsets.US_ASCII );
		if ( a.length != b.length )
			return false;
		int diff = 0;
		for ( int i = 0; i < a.length; i++ )
			diff |= a[ i ] ^ b[ i ];
		return diff == 0;
	}

	// ------------------------------------------------------------------ clients

	/**
	 * Register a new OAuth client (RFC 7591). The client_id is generated server-side (a
	 * {@code mcp_cl_}-prefixed random value) — the client cannot choose its own id, which prevents
	 * a malicious client from registering a confusingly-named id.
	 *
	 * @param redirectUris the exact redirect URIs the client will use (must be non-empty)
	 * @param clientName   an optional human-readable name (informational only)
	 * @param clientSecret an optional secret; when present the client is confidential, when absent
	 *                     public (PKCE)
	 * @return the new client (never {@code null})
	 */
	public static RegisteredClient registerClient( java.util.List<String> redirectUris, String clientName,
			String clientSecret )
	{
		String clientId = "mcp_cl_" + randomToken( 24 );
		RegisteredClient client = new RegisteredClient( clientId, redirectUris, clientName,
				clientSecret, CLIENT_TTL_MS );
		CLIENTS.put( clientId, client );
		return client;
	}

	/**
	 * Look up a registered client by id (live or, if the static allow-list has been consulted
	 * first, already merged in by the caller). Returns {@code null} for an unknown or expired id.
	 */
	public static RegisteredClient findClient( String clientId )
	{
		if ( clientId == null )
			return null;
		RegisteredClient client = CLIENTS.get( clientId );
		if ( client == null || client.expired() )
			return null;
		return client;
	}

	/**
	 * Whether {@code redirectUri} is an exact member of the client's registered set (the
	 * open-redirect defense).
	 */
	public static boolean clientAllowsRedirect( RegisteredClient client, String redirectUri )
	{
		return client != null && redirectUri != null && client.redirectUris.contains( redirectUri );
	}

	// ------------------------------------------------------------------ internals

	/**
	 * A cryptographically random, base64url token fragment ({@code n} bytes, no padding).
	 */
	static String randomToken( int bytes )
	{
		byte[] buf = new byte[ bytes ];
		RANDOM.nextBytes( buf );
		return Base64.getUrlEncoder().withoutPadding().encodeToString( buf );
	}

	/**
	 * Remove expired entries (test hook + opportunistic housekeeping; production relies on TTL checks
	 * at read time, so this is never on a hot path).
	 */
	static void purgeExpired()
	{
		long now = System.currentTimeMillis();
		for ( AuthCode code : CODES.values() )
			if ( code.expiresAt < now )
				CODES.remove( code.value );
		for ( AccessToken token : TOKENS.values() )
			if ( token.expiresAt < now )
				TOKENS.remove( token.value );
	}

	/**
	 * Clear both stores (test isolation only).
	 */
	public static void clearForTest()
	{
		CODES.clear();
		TOKENS.clear();
		CLIENTS.clear();
	}
}
