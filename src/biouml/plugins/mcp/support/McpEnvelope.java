package biouml.plugins.mcp.support;

import java.util.LinkedHashMap;
import java.util.Map;

import biouml.plugins.mcp.McpConstants;

/**
 * The uniform JSON envelope every MCP tool response of this server uses:
 * {@code {ok, data?, error?, code?}}.
 *
 * <p>Tools must never let raw exceptions or stack traces reach the client — all failures
 * are converted into a structured envelope via {@link #error(String, String)}.</p>
 */
public final class McpEnvelope
{
	private final boolean ok;
	private final Object data;
	private final String error;
	private final String code;

	/**
	 * Non-serialized side-channel for data that must reach the client but must NOT go into the JSON
	 * envelope — currently binary blobs (a rendered PNG) that the dispatcher emits as a separate MCP
	 * {@code image} content block instead of base64 text. Never appears in {@link #toMap}.
	 */
	private final java.util.Map<String, Object> attributes = new java.util.LinkedHashMap<String, Object>();

	private McpEnvelope( boolean ok, Object data, String error, String code )
	{
		this.ok = ok;
		this.data = data;
		this.error = error;
		this.code = code;
	}

	/** Attach a non-JSON side-channel value (e.g. a binary image) the dispatcher can emit separately. */
	public void setAttribute( String key, Object value )
	{
		attributes.put( key, value );
	}

	/** Read back a side-channel value, or {@code null} if absent. */
	public Object getAttribute( String key )
	{
		return attributes.get( key );
	}

	/**
	 * Success envelope with a data payload.
	 * @param data the payload (any JSON-serializable object)
	 */
	public static McpEnvelope ok( Object data )
	{
		return new McpEnvelope( true, data, null, null );
	}

	/**
	 * Error envelope.
	 * @param code one of the {@link McpConstants} {@code CODE_*} values (or a tool-specific code)
	 * @param error a short human-readable message (no stack traces)
	 */
	public static McpEnvelope error( String code, String error )
	{
		return new McpEnvelope( false, null, error, code );
	}

	public boolean isOk()
	{
		return ok;
	}

	public Object getData()
	{
		return data;
	}

	public String getError()
	{
		return error;
	}

	public String getCode()
	{
		return code;
	}

	/**
	 * Convert this envelope to a JSON object suitable for serialization.
	 */
	public Map<String, Object> toMap()
	{
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		map.put( "ok", Boolean.valueOf( ok ) );
		if ( data != null )
			map.put( "data", data );
		if ( error != null )
			map.put( "error", error );
		if ( code != null )
			map.put( "code", code );
		return map;
	}

	@Override
	public String toString()
	{
		return "McpEnvelope" + toMap();
	}
}
