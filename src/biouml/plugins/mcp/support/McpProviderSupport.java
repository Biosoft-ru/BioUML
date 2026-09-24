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
			return false; // a real (non-system) session is already bound
		// Bind a fresh session so providers that need a session (cache, WebJob) get a working one.
		String sid = SecurityManager.generateSessionId();
		SecurityManager.addThreadToSessionRecord( Thread.currentThread(), sid );
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
