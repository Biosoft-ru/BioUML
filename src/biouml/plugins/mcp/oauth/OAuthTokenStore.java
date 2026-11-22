package biouml.plugins.mcp.oauth;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.crypto.SecretKey;

import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.biostore.RemoteSecurityProvider;

/**
 * In-memory store for the BioUML MCP OAuth 2.1 authorization server: one-time authorization codes
 * (issued by {@code /authorize}, consumed by {@code /token}) and opaque bearer access tokens
 * (issued by {@code /token}, validated by the MCP endpoint on every request).
 *
 * <p>Consent and authorization codes are kept in memory. Client registrations are durable.
 * With a deployment key, grants issued from BioStore password logins can recover their MCP
 * session by reauthorizing with BioStore after a restart.</p>
 */
public final class OAuthTokenStore
{
	/** Access-token lifetime (1 h) — chosen to match {@code UserPermissions.TIMEOUT}. */
	public static final long ACCESS_TOKEN_TTL_MS = 60L * 60L * 1000L;

	/**
	 * Refresh-token lifetime (7 days). A refresh token lets an MCP client (e.g. Claude Web's
	 * connector) silently exchange an expired access token for a fresh one without re-running the
	 * authorization flow. It is long-lived by design (RFC 6749 §6: refresh tokens are typically valid
	 * for days-to-months) and is the only way for a connected client to stay authenticated across the
	 * 1-hour access-token window. Bounded so a leaked refresh token stops working after a week.
	 */
	public static final long REFRESH_TOKEN_TTL_MS = 7L * 24L * 60L * 60L * 1000L;

	/** Authorization-code lifetime (10 min) — standard OAuth bound for the code → token exchange. */
	public static final long AUTH_CODE_TTL_MS = 10L * 60L * 1000L;

	/** Access-token prefix: a namespace sentinel so a token can never be misread as a Basic payload. */
	public static final String TOKEN_PREFIX = "mcp_at_";

	/** Refresh-token prefix (namespace sentinel; refresh tokens are opaque otherwise). */
	public static final String REFRESH_PREFIX = "mcp_rt_";

	/** Authorization-code prefix (for log grepping; codes are opaque otherwise). */
	public static final String CODE_PREFIX = "mcp_ac_";

	/** Prefix of the one-time token that ties a consent-page submission to the page this server rendered. */
	public static final String CONSENT_PREFIX = "mcp_cs_";

	/** How long a rendered consent page stays submittable. */
	public static final long CONSENT_TTL_MS = 10L * 60L * 1000L;

	private static final Object[] RECOVERY_LOCKS = new Object[64];
	static
	{
		java.util.Arrays.setAll( RECOVERY_LOCKS, index -> new Object() );
	}
	private static final ConcurrentMap<String, SessionBinding> DURABLE_SESSIONS = new ConcurrentHashMap<>();
	private record SessionBinding( String sessionId, long expiresAt ) {}
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final ConcurrentMap<String, Consent> CONSENTS = new ConcurrentHashMap<String, Consent>();
	private static final ConcurrentMap<String, AuthCode> CODES = new ConcurrentHashMap<String, AuthCode>();
	private static final ConcurrentMap<String, AccessToken> TOKENS = new ConcurrentHashMap<String, AccessToken>();
	private static final ConcurrentMap<String, RefreshToken> REFRESHES = new ConcurrentHashMap<String, RefreshToken>();
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
		public final String clientId;
		public final java.util.List<String> redirectUris;
		public final String clientName;
		final String secretHash;

		RegisteredClient( String clientId, java.util.List<String> redirectUris, String clientName,
				String secretHash )
		{
			this.clientId = clientId;
			this.redirectUris = java.util.List.copyOf( redirectUris );
			this.clientName = clientName;
			this.secretHash = secretHash;
		}

		boolean acceptsSecret( String secret )
		{
			return secretHash == null || ( secret != null && !secret.isEmpty()
					&& pkceMatches( secretHash, hashClientSecret( secret ) ) );
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
			this( value, sessionId, clientId, user, System.currentTimeMillis() + ACCESS_TOKEN_TTL_MS );
		}

		AccessToken( String value, String sessionId, String clientId, String user, long expiresAt )
		{
			this.value = value;
			this.sessionId = sessionId;
			this.clientId = clientId;
			this.user = user;
			this.expiresAt = expiresAt;
		}

		boolean expired()
		{
			return System.currentTimeMillis() > expiresAt;
		}
	}

	/**
	 * An opaque refresh token issued alongside an access token. It carries the same session/client/
	 * user binding as the access token it was minted with, and a (much longer) lifetime. Consuming it
	 * (the {@code refresh_token} grant) mints a fresh access token for the same session — the client
	 * does NOT re-run the authorization flow.
	 */
	public static final class RefreshToken
	{
		/** The (opaque) token string. */
		public final String value;
		/** The BioUML session the refreshed access token will map to. */
		public final String sessionId;
		/** The client the token was issued to. */
		public final String clientId;
		/** The authenticated username (recorded for logging). */
		public final String user;
		/** Absolute expiry (epoch millis). */
		public final long expiresAt;

		RefreshToken( String value, String sessionId, String clientId, String user )
		{
			this( value, sessionId, clientId, user, System.currentTimeMillis() + REFRESH_TOKEN_TTL_MS );
		}

		RefreshToken( String value, String sessionId, String clientId, String user, long expiresAt )
		{
			this.value = value;
			this.sessionId = sessionId;
			this.clientId = clientId;
			this.user = user;
			this.expiresAt = expiresAt;
		}

		boolean expired()
		{
			return System.currentTimeMillis() > expiresAt;
		}
	}

	/**
	 * A pending consent: minted when the consent page is rendered for a signed-in user and consumed
	 * (once) when that page is submitted. It binds the approval to the session that saw the page and
	 * to the exact client / redirect URI / PKCE challenge shown on it, so an approval cannot be forged
	 * by a cross-site request (the token only ever appears inside the rendered page) nor replayed for
	 * a different request.
	 */
	public static final class Consent
	{
		public final String value;
		public final String sessionId;
		public final String user;
		public final String clientId;
		public final String redirectUri;
		public final String codeChallenge;
		public final long expiresAt;

		Consent( String value, String sessionId, String user, String clientId, String redirectUri,
				String codeChallenge )
		{
			this.value = value;
			this.sessionId = sessionId;
			this.user = user;
			this.clientId = clientId;
			this.redirectUri = redirectUri;
			this.codeChallenge = codeChallenge;
			this.expiresAt = System.currentTimeMillis() + CONSENT_TTL_MS;
		}

		boolean expired()
		{
			return System.currentTimeMillis() > expiresAt;
		}

		/** True if this consent was minted for exactly this session, user and authorize request. */
		public boolean matches( String sessionId, String user, String clientId, String redirectUri,
				String codeChallenge )
		{
			return this.sessionId.equals( sessionId ) && this.user.equals( user ) && this.clientId.equals( clientId )
					&& this.redirectUri.equals( redirectUri ) && this.codeChallenge.equals( codeChallenge );
		}
	}

	// ------------------------------------------------------------------ consents

	public static Consent issueConsent( String sessionId, String user, String clientId, String redirectUri,
			String codeChallenge )
	{
		purgeExpired(); // consent pages are rendered on any signed-in GET; keep the store bounded
		String value = CONSENT_PREFIX + randomToken( 32 );
		Consent consent = new Consent( value, sessionId, user, clientId, redirectUri, codeChallenge );
		CONSENTS.put( value, consent );
		return consent;
	}

	/** Remove and return the consent (single use); {@code null} if unknown or expired. */
	public static Consent consumeConsent( String value )
	{
		if ( value == null || !value.startsWith( CONSENT_PREFIX ) )
			return null;
		Consent consent = CONSENTS.remove( value );
		if ( consent == null || consent.expired() )
			return null;
		return consent;
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

	/** Issue an access token and a refresh token for an existing session. */
	public static AccessToken issueToken( String sessionId, String clientId, String user )
	{
		return issueToken( sessionId, clientId, user, null, null );
	}

	static AccessToken issueToken( String sessionId, String clientId, String user, String bioStoreToken, SecretKey key )
	{
		AccessToken access = new AccessToken( TOKEN_PREFIX + randomToken( 32 ), sessionId, clientId, user );
		RefreshToken refresh = new RefreshToken( REFRESH_PREFIX + randomToken( 32 ), sessionId, clientId, user );
		if ( bioStoreToken != null )
		{
			OAuthGrantRepository.insert( access, refresh, bioStoreToken, key );
			DURABLE_SESSIONS.put( OAuthGrantRepository.digest( refresh.value ), new SessionBinding( sessionId, refresh.expiresAt ) );
		}
		REFRESHES.put( refresh.value, refresh );
		TOKENS.put( access.value, access );
		return access;
	}

	static AccessToken reissue( RefreshToken refresh )
	{
		String hash = OAuthGrantRepository.digest( refresh.value );
		synchronized ( recoveryLock( hash ) )
		{
			if ( REFRESHES.get( refresh.value ) != refresh || refresh.expired()
					|| SecurityManager.isSessionDead( refresh.sessionId ) )
				return null;
			AccessToken access = new AccessToken( TOKEN_PREFIX + randomToken( 32 ), refresh.sessionId, refresh.clientId,
					refresh.user, Math.min( refresh.expiresAt, System.currentTimeMillis() + ACCESS_TOKEN_TTL_MS ) );
			if ( DURABLE_SESSIONS.containsKey( hash ) )
				OAuthGrantRepository.insertAccess( refresh.value, access );
			TOKENS.put( access.value, access );
			return access;
		}
	}

	/**
	 * The refresh-token value paired with an access token (for the token response). Returns
	 * {@code null} if the access token is unknown.
	 */
	public static String refreshTokenFor( String accessTokenValue )
	{
		AccessToken t = TOKENS.get( accessTokenValue );
		if ( t == null )
			return null;
		// We mint one refresh per access token; scan for the matching binding. (Cheap: the map is
		// small — a handful of connected clients.)
		for ( RefreshToken r : REFRESHES.values() )
			if ( r.sessionId.equals( t.sessionId ) && r.clientId.equals( t.clientId ) && r.user.equals( t.user ) )
				return r.value;
		return null;
	}

	/** Resolve an unexpired refresh token for its registered client. */
	static RefreshToken consumeRefresh( String value, String clientId, String secret )
	{
		if ( !validTokenFormat( value, REFRESH_PREFIX ) )
			return null;
		RefreshToken refresh = REFRESHES.get( value );
		if ( refresh != null )
		{
			if ( refresh.expired() || !clientMatches( refresh.clientId, clientId, secret ) )
				return null;
			if ( SecurityManager.isSessionDead( refresh.sessionId ) )
			{
				revokeRefresh( value );
				return null;
			}
			return refresh;
		}
		SecretKey key = OAuthGrantRepository.deploymentKey();
		if ( key == null || !( SecurityManager.getSecurityProvider() instanceof RemoteSecurityProvider ) )
			return null;
		OAuthGrantRepository.Grant grant = OAuthGrantRepository.find( value, true );
		if ( grant == null || !clientMatches( grant.clientId(), clientId, secret ) )
			return null;
		synchronized ( recoveryLock( grant.refreshHash() ) )
		{
			String sessionId = restoreSession( grant, key );
			if ( sessionId == null )
				return null;
			refresh = new RefreshToken( value, sessionId, grant.clientId(), grant.user(), grant.expiresAt() );
			RefreshToken existing = REFRESHES.putIfAbsent( value, refresh );
			return existing == null ? refresh : existing;
		}
	}

	private static boolean clientMatches( String registeredId, String suppliedId, String secret )
	{
		if ( !suppliedId.isEmpty() && !registeredId.equals( suppliedId ) )
			return false;
		RegisteredClient client = findClient( registeredId );
		return client == null ? McpOAuthConfig.clients().containsKey( registeredId )
				: ( client.secretHash == null || !suppliedId.isEmpty() ) && client.acceptsSecret( secret );
	}

	static void revokeRefresh( String value )
	{
		RefreshToken refresh = REFRESHES.remove( value );
		String refreshHash = OAuthGrantRepository.digest( value );
		if ( refresh != null && DURABLE_SESSIONS.containsKey( refreshHash ) )
			OAuthGrantRepository.revoke( refreshHash );
	}

	static void revokeGrant( String value, String clientId, String secret )
	{
		if ( !validTokenFormat( value, REFRESH_PREFIX ) )
			return;
		String refreshHash = OAuthGrantRepository.digest( value );
		synchronized ( recoveryLock( refreshHash ) )
		{
			RefreshToken token = REFRESHES.get( value );
			SessionBinding binding = DURABLE_SESSIONS.get( refreshHash );
			long expiresAt;
			if ( token != null )
			{
				if ( !clientMatches( token.clientId, clientId, secret ) )
					return;
				expiresAt = token.expiresAt;
			}
			else
			{
				if ( !canRecover() )
					return;
				OAuthGrantRepository.Grant grant = OAuthGrantRepository.find( value, true );
				if ( grant == null || !clientMatches( grant.clientId(), clientId, secret ) )
					return;
				expiresAt = grant.expiresAt();
			}
			if ( binding != null || token == null )
			{
				OAuthGrantRepository.revoke( refreshHash );
				DURABLE_SESSIONS.put( refreshHash, new SessionBinding( null, expiresAt ) );
			}
			REFRESHES.remove( value );
			String sessionId = binding == null ? ( token == null ? null : token.sessionId ) : binding.sessionId;
			if ( sessionId != null )
				TOKENS.values().removeIf( access -> access.sessionId.equals( sessionId ) );
		}
	}

	private static boolean canRecover()
	{
		return SecurityManager.getSecurityProvider() instanceof RemoteSecurityProvider
				&& OAuthGrantRepository.deploymentKey() != null;
	}

	/**
	 * Validate a bearer token for MCP endpoint use: the token must exist, be unexpired, and its
	 * session must still be alive (a dead session revokes the token).
	 *
	 * @return the token record, or {@code null} if invalid
	 */
	public static AccessToken validate( String value )
	{
		if ( !validTokenFormat( value, TOKEN_PREFIX ) )
			return null;
		AccessToken token = TOKENS.get( value );
		if ( token == null )
		{
			SecretKey key = OAuthGrantRepository.deploymentKey();
			if ( key == null || !( SecurityManager.getSecurityProvider() instanceof RemoteSecurityProvider ) )
				return null;
			OAuthGrantRepository.Grant grant = OAuthGrantRepository.find( value, false );
			if ( grant == null || grant.accessExpiresAt() <= System.currentTimeMillis()
					|| ( findClient( grant.clientId() ) == null
							&& !McpOAuthConfig.clients().containsKey( grant.clientId() ) ) )
				return null;
			synchronized ( recoveryLock( grant.refreshHash() ) )
			{
				String sessionId = restoreSession( grant, key );
				if ( sessionId == null )
					return null;
				token = new AccessToken( value, sessionId, grant.clientId(), grant.user(), grant.accessExpiresAt() );
				TOKENS.putIfAbsent( value, token );
			}
		}
		if ( token.expired() || SecurityManager.isSessionDead( token.sessionId ) )
			return null;
		SecurityManager.touchSessionExpiry( token.sessionId );
		return token;
	}

	private static boolean validTokenFormat( String value, String prefix )
	{
		return value != null && value.startsWith( prefix ) && value.length() == prefix.length() + 43
				&& value.substring( prefix.length() ).matches( "[A-Za-z0-9_-]+" );
	}

	private static Object recoveryLock( String refreshHash )
	{
		return RECOVERY_LOCKS[Math.floorMod( refreshHash.hashCode(), RECOVERY_LOCKS.length )];
	}

	private static String restoreSession( OAuthGrantRepository.Grant grant, SecretKey key )
	{
		String refreshHash = grant.refreshHash();
		SessionBinding binding = DURABLE_SESSIONS.get( refreshHash );
		if ( binding != null )
		{
			if ( binding.sessionId == null )
				return null;
			if ( SecurityManager.isSessionDead( binding.sessionId ) )
			{
				OAuthGrantRepository.revoke( refreshHash );
				return null;
			}
			return binding.sessionId;
		}
		try
		{
			String credential = OAuthGrantRepository.decrypt( grant, key );
			String sessionId = SecurityManager.generateSessionId();
			SecurityManager.addThreadToSessionRecord( Thread.currentThread(), sessionId );
			SecurityManager.commonLogin( grant.user(), null, null, credential );
			DURABLE_SESSIONS.put( refreshHash, new SessionBinding( sessionId, grant.expiresAt() ) );
			return sessionId;
		}
		catch ( GeneralSecurityException | SecurityException e )
		{
			return null;
		}
		finally
		{
			SecurityManager.removeThreadFromSessionRecord();
		}
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
				clientSecret == null ? null : hashClientSecret( clientSecret ) );
		if ( SecurityManager.isTestMode() )
			CLIENTS.put( clientId, client );
		else
			OAuthClientRepository.insert( client );
		return client;
	}

	/** Look up a client registered by this server. */
	public static RegisteredClient findClient( String clientId )
	{
		if ( clientId == null || !clientId.matches( "mcp_cl_[A-Za-z0-9_-]{32}" ) )
			return null;
		return SecurityManager.isTestMode() ? CLIENTS.get( clientId ) : OAuthClientRepository.find( clientId );
	}

	private static String hashClientSecret( String secret )
	{
		return pkceChallenge( Base64.getEncoder().encodeToString( secret.getBytes( StandardCharsets.UTF_8 ) ) );
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
		DURABLE_SESSIONS.values().removeIf( binding -> binding.expiresAt <= now );
		for ( Consent consent : CONSENTS.values() )
			if ( consent.expiresAt < now )
				CONSENTS.remove( consent.value );
		for ( AuthCode code : CODES.values() )
			if ( code.expiresAt < now )
				CODES.remove( code.value );
		for ( AccessToken token : TOKENS.values() )
			if ( token.expiresAt < now )
				TOKENS.remove( token.value );
		for ( RefreshToken r : REFRESHES.values() )
			if ( r.expiresAt < now )
				REFRESHES.remove( r.value );
	}

	/**
	 * Clear all stores (test isolation only).
	 */
	public static void clearForTest()
	{
		CONSENTS.clear();
		CODES.clear();
		TOKENS.clear();
		REFRESHES.clear();
		CLIENTS.clear();
		DURABLE_SESSIONS.clear();
	}
}
