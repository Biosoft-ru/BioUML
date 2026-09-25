package biouml.plugins.mcp.support;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import biouml.plugins.mcp.McpConstants;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.providers.WebJSONProviderSupport;
import ru.biosoft.server.servlets.webservices.providers.WebProvider;
import ru.biosoft.server.servlets.webservices.providers.WebProviderFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

/**
 * The shared bridge that turns an MCP tool call into a BioUML {@code WebJSONProviderSupport} provider
 * invocation.
 *
 * <p>Instead of re-implementing BioUML functionality in hand-written support classes, MCP tools are
 * a thin projection of the platform's own web-service providers: each provider is registered by a
 * {@code prefix} (OSGi extension point {@code ru.biosoft.server.servlets.webProvider}), and its
 * {@code process(BiosoftWebRequest, JSONResponse)} switches on an {@code action} and reads its
 * parameters from the request. This class builds that request, invokes the provider on a bound
 * BioUML session, and translates the provider's JSON response back into a structured
 * {@link McpEnvelope}.</p>
 *
 * <p>Parameters follow the provider's own convention: scalars pass through as strings, and
 * array/object parameters are serialised to a JSON string (the provider reads them back with
 * {@link BiosoftWebRequest#getJSONArray} / {@link BiosoftWebRequest#getStrings}). Element paths are
 * passed under the {@code de} key ({@code AccessProtocol.KEY_DE}).</p>
 */
public final class McpProviderSupport
{
	private McpProviderSupport()
	{
	}

	/** The request key a provider reads its primary data-element path from. */
	public static final String KEY_DE = "de";

	/**
	 * A single shared session ID for all headless (non-servlet) MCP calls on this JVM. Async (job-based)
	 * providers store their {@code WebJob} per-{@code WebSession} (via
	 * {@code session.putValue("webJob/"+jobID, ...)}), so a job started in one provider call must be
	 * polled from the <em>same</em> {@code WebSession}. Using one stable ID (instead of a fresh UUID per
	 * call, as {@link #ensureSession} historically did) keeps the start and poll in the same session.
	 * The ID is deliberately not a {@code node_}-prefixed id (see {@code SecurityManager.getSession},
	 * which strips that prefix and would mismatch the cache lookup).
	 */
	private static final String SHARED_HEADLESS_SESSION_ID =
			"mcp-headless-" + java.util.UUID.randomUUID();

	/**
	 * A single shared carrier for the headless {@code WebSession}. The carrier backs the
	 * {@code WebSession}'s attribute store (via its {@code getValue}/{@code putValue} methods), which is
	 * where {@code WebJob.attach} stores the {@code WebJob} under {@code "webJob/"+jobID}. A fresh
	 * carrier per call would create a fresh empty attribute map, so a job started in one call would be
	 * invisible to a later poll call — the start and poll must share <em>one</em> carrier.
	 */
	private static final HeadlessHttpSession SHARED_HEADLESS_CARRIER =
			new HeadlessHttpSession( SHARED_HEADLESS_SESSION_ID );

	/**
	 * One stable carrier per real session id. The carrier backs the {@code WebSession}'s attribute store
	 * where {@code WebJob.attach} stores the job under {@code "webJob/"+jobID}; a fresh carrier per call
	 * would create a fresh empty map and make a job started in one call invisible to a later poll call.
	 * The shared headless carrier (above) is the same idea for the single headless session; this map
	 * generalizes it to a caller's real (Bearer/Basic/JSESSIONID) session id.
	 */
	private static final java.util.concurrent.ConcurrentHashMap<String, HeadlessHttpSession> CARRIERS =
			new java.util.concurrent.ConcurrentHashMap<String, HeadlessHttpSession>();

	/**
	 * Look up a provider by its registered prefix.
	 * @return the provider, or {@code null} if the prefix is not registered.
	 */
	public static WebProvider provider( String prefix )
	{
		try
		{
			return WebProviderFactory.getProvider( prefix );
		}
		catch ( Throwable t )
		{
			return null;
		}
	}

	/**
	 * Resolve an element path, tolerating a URL-encoded form. Agents (and the web UI's own cookies)
	 * frequently pass element paths with spaces encoded as {@code %20} (e.g.
	 * {@code Westerhoff%20and%20Kolodkin}), but BioUML repository paths use <em>literal</em> spaces. To
	 * make such paths self-correcting without ever breaking a path that already resolves, this tries the
	 * raw path first and only falls back to the URL-decoded form when the raw path does not resolve. A
	 * path that resolves raw is therefore returned untouched.
	 * @param path the raw (possibly URL-encoded) element path
	 * @return the resolved {@link ru.biosoft.access.core.DataElement}, or {@code null} if neither the raw
	 *         nor the decoded form resolves.
	 */
	public static ru.biosoft.access.core.DataElement resolveElement( String path )
	{
		if ( path == null || path.isEmpty() )
			return null;
		ru.biosoft.access.core.DataElement de = tryResolve( path );
		if ( de != null )
			return de;
		String decoded = urlDecodeIfEncoded( path );
		if ( decoded != null )
			return tryResolve( decoded );
		return null;
	}

	private static ru.biosoft.access.core.DataElement tryResolve( String path )
	{
		try
		{
			return ru.biosoft.access.core.CollectionFactory.getDataElement( path );
		}
		catch ( Exception e )
		{
			return null;
		}
	}

	/**
	 * URL-decode {@code s} if it contains a well-formed {@code %XX} escape.
	 * @return the decoded string if it differs from the input, or {@code null} if the input has no
	 *         {@code %XX} sequence (i.e. it should be left as-is).
	 */
	private static String urlDecodeIfEncoded( String s )
	{
		if ( s == null || s.indexOf( '%' ) < 0 || !s.matches( ".*%[0-9a-fA-F]{2}.*" ) )
			return null;
		try
		{
			String decoded = java.net.URLDecoder.decode( s, java.nio.charset.StandardCharsets.UTF_8 );
			return decoded.equals( s ) ? null : decoded;
		}
		catch ( Exception e )
		{
			return null; // not actually URL-encoded; leave untouched
		}
	}

	/**
	 * Invoke a provider action with the given parameters.
	 *
	 * @param prefix   the provider's registered prefix (e.g. {@code "simulation"}), or {@code null}
	 *                 to use the current prefix context
	 * @param action   the provider's {@code action} value (the {@code case} label in its
	 *                 {@code process} switch)
	 * @param params   the action's parameters. Scalars are passed through; {@link Map}/{@link java.util.List}/
	 *                 arrays are serialised to a JSON string so the provider can read them back with
	 *                 {@link BiosoftWebRequest#getJSONArray}. A value under {@link #KEY_DE} (or the
	 *                 {@code path} alias) is treated as the element path.
	 * @return a {@link McpEnvelope} whose data is the provider's response {@code values} on success,
	 *         or a structured error envelope (code + message) on failure.
	 */
	public static McpEnvelope invoke( String prefix, String action, Map<String, Object> params )
	{
		if ( prefix == null || prefix.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "a provider prefix is required" );
		if ( action == null || action.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "an action is required" );
		WebProvider provider = provider( prefix );
		if ( provider == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no provider registered for prefix: " + prefix );
		return invoke( provider, prefix, action, params );
	}

	/**
	 * Invoke a specific provider instance with the given action and parameters. The {@code name} is
	 * used only in error messages; pass the provider's prefix.
	 */
	public static McpEnvelope invoke( WebProvider provider, String name, String action, Map<String, Object> params )
	{
		if ( action == null || action.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "an action is required" );
		if ( !( provider instanceof WebJSONProviderSupport ) )
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"provider '" + name + "' is not a WebJSONProviderSupport: " + provider.getClass().getName() );
		return runProvider( (WebJSONProviderSupport) provider, name, action, params );
	}

	/**
	 * Invoke a provider with a <em>pre-built</em> {@code Map<String,String>} request, bypassing
	 * {@link #buildRequest}'s {@code path}→{@code de} promotion. Use this for providers that read a
	 * literal {@code path} key (e.g. {@code CopyFolderProvider}), which would otherwise be rewritten
	 * to {@code de} and lost.
	 *
	 * @param provider the provider to invoke (must be a {@code WebJSONProviderSupport})
	 * @param name     the provider's prefix, used only in error messages
	 * @param action   the provider's {@code action} value
	 * @param request  the fully-built request map (keys exactly as the provider reads them)
	 * @return a {@link McpEnvelope} whose data is the provider's response on success.
	 */
	public static McpEnvelope invokeRaw( WebProvider provider, String name, String action, Map<String, String> request )
	{
		if ( action == null || action.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "an action is required" );
		if ( !( provider instanceof WebJSONProviderSupport ) )
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"provider '" + name + "' is not a WebJSONProviderSupport: " + provider.getClass().getName() );
		return runProviderRaw( (WebJSONProviderSupport) provider, name, action, request );
	}

	private static McpEnvelope runProviderRaw( WebJSONProviderSupport provider, String name, String action, Map<String, String> request )
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		boolean bound = false;
		try
		{
			bound = ensureSession();
			provider.process( new BiosoftWebRequest( request ), new JSONResponse( out ) );
		}
		catch ( WebException e )
		{
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, e.getMessage() );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"provider '" + name + "' action '" + action + "' failed: "
							+ e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
		finally
		{
			if ( bound )
				SecurityManager.removeThreadFromSessionRecord();
		}
		return parseResponse( out );
	}

	private static McpEnvelope runProvider( WebJSONProviderSupport provider, String name, String action, Map<String, Object> params )
	{
		Map<String, String> request = buildRequest( action, params );
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		boolean bound = false;
		try
		{
			bound = ensureSession();
			// Call the JSONResponse overload directly so we control the output stream.
			provider.process( new BiosoftWebRequest( request ), new JSONResponse( out ) );
		}
		catch ( WebException e )
		{
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, e.getMessage() );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"provider '" + name + "' action '" + action + "' failed: "
							+ e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
		finally
		{
			if ( bound )
				SecurityManager.removeThreadFromSessionRecord();
		}
		return parseResponse( out );
	}

	/**
	 * Invoke a provider action and return the <em>entire</em> top-level response object (not just its
	 * {@code values} field), so callers can read sibling keys like {@code status} and {@code percent}
	 * that {@link JSONResponse#sendStatus} emits alongside {@code values}. Returns {@code null} on any
	 * failure.
	 */
	public static Map<String, Object> responseMap( WebJSONProviderSupport provider, String name, String action, Map<String, Object> params )
	{
		if ( !( provider instanceof WebJSONProviderSupport ) )
			return null;
		Map<String, String> request = buildRequest( action, params );
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		boolean bound = false;
		try
		{
			bound = ensureSession();
			provider.process( new BiosoftWebRequest( request ), new JSONResponse( out ) );
		}
		catch ( Exception e )
		{
			return null;
		}
		finally
		{
			if ( bound )
				SecurityManager.removeThreadFromSessionRecord();
		}
		byte[] bytes = out.toByteArray();
		if ( bytes.length == 0 )
			return new LinkedHashMap<String, Object>();
		try
		{
			JsonObject obj = Json.parse( new String( bytes, java.nio.charset.StandardCharsets.UTF_8 ) ).asObject();
			if ( obj.getInt( JSONResponse.ATTR_TYPE, JSONResponse.TYPE_OK ) == JSONResponse.TYPE_ERROR )
				return null;
			return (Map<String, Object>) fromJson( obj );
		}
		catch ( Exception e )
		{
			return null;
		}
	}

	/**
	 * Build the {@code Map<String,String>} request a provider expects: the {@code action} plus each
	 * parameter, with complex values (maps/lists/arrays) serialised to a JSON string and the
	 * {@code path} alias promoted to the {@code de} element-path key.
	 */
	public static Map<String, String> buildRequest( String action, Map<String, Object> params )
	{
		Map<String, String> request = new LinkedHashMap<String, String>();
		request.put( BiosoftWebRequest.ACTION, action );
		if ( params != null )
		{
			for ( Map.Entry<String, Object> e : params.entrySet() )
			{
				Object v = e.getValue();
				if ( v == null )
					continue;
				String key = KEY_DE.equals( e.getKey() ) || "path".equals( e.getKey() ) ? KEY_DE : e.getKey();
				request.put( key, scalarOrJson( v ) );
			}
		}
		return request;
	}

	private static String scalarOrJson( Object v )
	{
		if ( v instanceof String || v instanceof Number || v instanceof Boolean )
			return String.valueOf( v );
		if ( v instanceof Map || v instanceof java.util.List || v.getClass().isArray() )
			return toJson( v ).toString();
		return String.valueOf( v );
	}

	/** Serialise a map/list/array to a JSON string (the form the provider reads back). */
	@SuppressWarnings( "unchecked" )
	private static JsonValue toJson( Object v )
	{
		try
		{
			if ( v instanceof Map )
			{
				JsonObject o = new JsonObject();
				for ( Map.Entry<Object, Object> e : ( (Map<Object, Object>) v ).entrySet() )
					o.add( String.valueOf( e.getKey() ), scalarOrJsonValue( e.getValue() ) );
				return o;
			}
			if ( v instanceof java.util.List )
			{
				JsonArray a = new JsonArray();
				for ( Object o : (java.util.List<Object>) v )
					a.add( scalarOrJsonValue( o ) );
				return a;
			}
			if ( v.getClass().isArray() )
			{
				JsonArray a = new JsonArray();
				int len = java.lang.reflect.Array.getLength( v );
				for ( int i = 0; i < len; i++ )
					a.add( scalarOrJsonValue( java.lang.reflect.Array.get( v, i ) ) );
				return a;
			}
		}
		catch ( Exception e )
		{
			// fall through to string
		}
		return Json.value( String.valueOf( v ) );
	}

	private static JsonValue scalarOrJsonValue( Object v )
	{
		if ( v == null )
			return Json.NULL;
		if ( v instanceof Boolean )
			return Json.value( (Boolean) v );
		if ( v instanceof Number )
			// double() for all numeric types — JSON numbers are lossless for doubles, and this avoids
			// the Integer/Long → long widening overload that would truncate fractional values.
			return Json.value( ( (Number) v ).doubleValue() );
		if ( v instanceof Map || v instanceof java.util.List || v.getClass().isArray() )
			return toJson( v );
		return Json.value( String.valueOf( v ) );
	}

	/**
	 * Ensure the current thread has a BioUML session bound (providers reach for
	 * {@code WebSession.getCurrentSession()} / {@code SecurityManager.getSession()} and the per-session
	 * cache). If the MCP servlet already bound one, this is a no-op.
	 * @return {@code true} if this method bound a fresh session that the caller must unbind.
	 */
	private static boolean ensureSession()
	{
		String current = SecurityManager.getSession();
		if ( current != null && !current.isEmpty() && !SecurityManager.SYSTEM_SESSION.equals( current ) )
		{
			// A real (non-system) session is already bound (Bearer token, Basic-auth login, or a
			// JSESSIONID). If it has no WebSession yet, build one FOR THIS SESSION'S id — not the shared
			// headless one — so providers that call WebSession.getCurrentSession() (e.g. the simulation
			// provider re-resolving a diagram) find it. Falling back to the shared headless session here
			// would silently re-point the thread off the caller's real session and break resolution.
			if ( ru.biosoft.server.servlets.webservices.WebSession.getCurrentSession() == null )
				ensureWebSessionFor( current );
			return false; // the caller's own session stays bound
		}
		// No real session bound (a plain headless / in-process call): bind the shared headless session
		// so providers that need a session (cache, WebJob) get a working, <em>stable</em> one (async jobs
		// must survive across calls).
		ensureWebSession();
		return true;
	}

	/**
	 * Bind a <em>named</em> (logged-in) session and log in the given user. Needed by providers that gate
	 * actions on the session user — e.g. {@code TaskProvider} and {@code JobControlProvider} call
	 * {@code SecurityManager.getSessionUser()} and refuse to act on a task whose owner differs from the
	 * current user. The {@code system} session carries no user record by design, so a task created under
	 * it is invisible to those providers; this helper establishes a real user identity for the current
	 * thread for the duration of the block.
	 *
	 * <p>When a real security provider is not configured (a plain test JVM), a {@link TestSecurityProvider}
	 * is installed so the login succeeds; under a real deployment the servlet has already logged the user
	 * in, and this binds the thread to a live session of that user.</p>
	 *
	 * @param user      the username to log in as
	 * @param body      the block to run while the named session is bound; may throw
	 * @return the value returned by {@code body}
	 */
	public static <T> T withUserSession( String user, UserAction<T> body )
	{
		ensureSecurityProvider();
		String sid = SecurityManager.getSession();
		boolean fresh = ( sid == null || sid.isEmpty() || SecurityManager.SYSTEM_SESSION.equals( sid ) );
		if ( fresh )
			SecurityManager.addThreadToSessionRecord( Thread.currentThread(), SecurityManager.generateSessionId() );
		try
		{
			if ( fresh )
				SecurityManager.commonLogin( user, user, "127.0.0.1", null );
			return body.run();
		}
		catch ( Exception e )
		{
			throw ( e instanceof RuntimeException ) ? (RuntimeException) e : new RuntimeException( e );
		}
		finally
		{
			if ( fresh )
				SecurityManager.removeThreadFromSessionRecord();
		}
	}

	/**
	 * Make sure a {@link SecurityProvider} is present (install the {@link TestSecurityProvider} in a plain
	 * JVM that has none configured), so {@link #withUserSession(String, UserAction)} can log a user in.
	 */
	static void ensureSecurityProvider()
	{
		if ( SecurityManager.getSecurityProvider() == null )
		{
			try
			{
				java.lang.reflect.Field f = SecurityManager.class.getDeclaredField( "securityProvider" );
				f.setAccessible( true );
				ru.biosoft.access.security.TestSecurityProvider p = new ru.biosoft.access.security.TestSecurityProvider();
				java.util.Properties props = new java.util.Properties();
				p.init( props );
				f.set( null, p );
			}
			catch ( Exception e )
			{
				// leave as-is; commonLogin will surface the failure
			}
		}
	}

	/** A block of work to run while a named session is bound (for {@link #withUserSession}). */
	public interface UserAction<T>
	{
		T run() throws Exception;
	}

	/**
	 * Bootstrap a headless {@code WebSession} so that {@code WebJob.getWebJob(jobID)} and other
	 * session-dependent providers can find it. This is needed for async (job-based) providers like
	 * {@code CopyFolderProvider}, {@code SimulationProvider}, and {@code WebScriptsProvider}.
	 *
	 * <p>Uses a <em>single shared</em> session ID for all headless MCP calls on this JVM, so that a job
	 * started in one provider call (e.g. {@code folder/copy}) is still visible to a later poll call
	 * ({@code jobcontrol}) — the {@code WebJob} is stored per-{@code WebSession} via
	 * {@code session.putValue("webJob/"+jobID, ...)}, and both calls must land in the same
	 * {@code WebSession}. The method creates a synthetic carrier with a non-null {@code getId()},
	 * registers the current thread with that shared ID, ensures a {@code SessionCache} exists, and
	 * builds the {@code WebSession} via the real factory. After this call,
	 * {@code WebSession.getCurrentSession()} on the same thread returns the shared {@code WebSession}.</p>
	 *
	 * @return the constructed {@code WebSession}, or {@code null} if bootstrap failed
	 */
	public static Object ensureWebSession()
	{
		return ensureWebSessionFor( SHARED_HEADLESS_SESSION_ID );
	}

	/**
	 * Bootstrap a {@code WebSession} for a <em>specific</em> session id (the shared headless id by
	 * default, but also the caller's real session id when that is what is bound). Constructs a carrier
	 * whose {@code getId()} is {@code sid}, ensures a {@link ru.biosoft.access.security.SessionCache}
	 * exists for it, and builds the {@code WebSession} via the real factory (which re-uses one already
	 * cached for that id). After this call, {@code WebSession.getCurrentSession()} on a thread bound to
	 * {@code sid} returns that {@code WebSession} instead of logging "Unknown session requested".
	 *
	 * <p>This is what the Bearer-token and (Basic / JSESSIONID) auth paths need: the OAuth token is
	 * issued against a fresh {@link ru.biosoft.access.security.SecurityManager} session that has a
	 * {@code SessionCache} but no {@code WebSession} object, so any provider that calls
	 * {@code WebSession.getCurrentSession()} (e.g. the simulation provider re-resolving a diagram)
	 * would otherwise fail. Binding the {@code WebSession} for the caller's own id — rather than
	 * falling back to the shared headless one — keeps the request on the caller's real session.</p>
	 *
	 * @param sid the session id to bootstrap a {@code WebSession} for
	 * @return the constructed {@code WebSession}, or {@code null} if bootstrap failed
	 */
	public static Object ensureWebSessionFor( String sid )
	{
		if ( sid == null || sid.isEmpty() )
			return null;
		try
		{
			// A STABLE carrier whose getId() is the requested sid, so the factory binds the WebSession
			// to it and — crucially — the same carrier (and its attribute map holding the WebJob) is
			// reused across calls for that session, so a job started in one call is visible to a poll.
			Object carrier = CARRIERS.computeIfAbsent( sid, HeadlessHttpSession::new );

			// Ensure a SessionCache exists for sid.
			if ( ru.biosoft.access.security.SessionCacheManager.getSessionCache( sid ) == null )
				ru.biosoft.access.security.SessionCacheManager.addSessionCache( sid );

			// Build via the real factory (returns the existing WebSession if one is already cached).
			// Note: WebSession.getSession() also re-registers this thread with sid, which is exactly
			// what we want for the caller's real session.
			return ru.biosoft.server.servlets.webservices.WebSession.getSession( carrier );
		}
		catch ( Exception e )
		{
			// Log but don't throw — the caller can check for null
			java.util.logging.Logger.getLogger( McpProviderSupport.class.getName() )
					.log( java.util.logging.Level.WARNING, "Failed to bootstrap WebSession for " + sid, e );
			return null;
		}
	}

	/**
	 * Parse the provider's JSON response ({@code {"type":OK|ERROR,"values":…,"message":…,"code":…}})
	 * into an envelope. An empty response (some providers write nothing) is treated as an OK with an
	 * empty result.
	 */
	static McpEnvelope parseResponse( ByteArrayOutputStream out )
	{
		byte[] bytes = out.toByteArray();
		if ( bytes.length == 0 )
			return McpEnvelope.ok( new LinkedHashMap<String, Object>() );
		try
		{
			JsonObject obj = Json.parse( new String( bytes, java.nio.charset.StandardCharsets.UTF_8 ) ).asObject();
			int type = obj.getInt( JSONResponse.ATTR_TYPE, JSONResponse.TYPE_OK );
			if ( type == JSONResponse.TYPE_ERROR )
			{
				String message = obj.getString( JSONResponse.ATTR_MESSAGE, "provider error" );
				String code = obj.getString( JSONResponse.ATTR_ERROR_CODE, McpConstants.CODE_INTERNAL );
				return McpEnvelope.error( code, message );
			}
			JsonValue values = obj.get( JSONResponse.ATTR_VALUES );
			return McpEnvelope.ok( fromJson( values ) );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"could not parse provider response: " + new String( bytes, java.nio.charset.StandardCharsets.UTF_8 ) );
		}
	}

	/** Convert a JSON value into a plain JSON-serializable Java structure (maps/lists/scalars). */
	@SuppressWarnings( "unchecked" )
	private static Object fromJson( JsonValue v )
	{
		if ( v == null || v.isNull() )
			return null;
		if ( v.isObject() )
		{
			JsonObject o = v.asObject();
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			for ( String name : o.names() )
				m.put( name, fromJson( o.get( name ) ) );
			return m;
		}
		if ( v.isArray() )
		{
			JsonArray a = v.asArray();
			java.util.List<Object> list = new java.util.ArrayList<Object>();
			for ( JsonValue item : a )
				list.add( fromJson( item ) );
			return list;
		}
		if ( v.isString() )
			return v.asString();
		if ( v.isBoolean() )
			return v.asBoolean();
		if ( v.isNumber() )
		{
			double d = v.asDouble();
			if ( d == Math.floor( d ) && !Double.isInfinite( d ) && Math.abs( d ) < 9.007199254740992E15 )
				return Long.valueOf( (long) d );
			return Double.valueOf( d );
		}
		return v.toString();
	}
}
