package biouml.plugins.mcp.support;

import java.util.Map;

import biouml.plugins.mcp.McpConstants;

/**
 * Argument-extraction helpers for MCP tool handlers.
 *
 * <p>Tools declare their parameters in a JSON-Schema {@code inputSchema}; the MCP client (and the
 * LLM) is expected to send the right types. These helpers enforce that contract at the tool
 * boundary: a missing required argument, a non-boolean where a boolean is expected, or a
 * non-numeric where a number is expected returns a structured {@code invalid_params} envelope —
 * never a {@code ClassCastException} leaking a stack trace to the client.</p>
 */
public final class McpArgs
{
	private McpArgs()
	{
	}

	/**
	 * Required string argument. Missing or not a String → {@code invalid_params}.
	 */
	public static McpEnvelope requiredString( Map<String, Object> args, String key )
	{
		if ( args == null || !args.containsKey( key ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "missing required argument: " + key );
		Object v = args.get( key );
		if ( !( v instanceof String ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "argument '" + key + "' must be a string" );
		String s = (String) v;
		if ( s.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "argument '" + key + "' must be a non-empty string" );
		return null; // ok
	}

	/**
	 * Optional string argument: null when absent, or a structured error if present but not a String.
	 */
	public static McpEnvelope optionalString( Map<String, Object> args, String key )
	{
		if ( args == null || !args.containsKey( key ) )
			return null;
		Object v = args.get( key );
		if ( v == null || v instanceof String )
			return null;
		return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "argument '" + key + "' must be a string" );
	}

	/**
	 * Boolean argument with a default. Missing → the default; wrong type → {@code invalid_params}.
	 */
	public static McpEnvelope boolArg( Map<String, Object> args, String key, boolean defaultValue )
	{
		if ( args == null || !args.containsKey( key ) || args.get( key ) == null )
			return null;
		Object v = args.get( key );
		if ( v instanceof Boolean )
			return null;
		if ( v instanceof String )
			return null; // tolerate "true"/"false" strings; the tool interprets them
		return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "argument '" + key + "' must be a boolean" );
	}

	/**
	 * Integer argument with a default. Missing → the default; non-numeric → {@code invalid_params}.
	 */
	public static McpEnvelope intArg( Map<String, Object> args, String key, int defaultValue )
	{
		if ( args == null || !args.containsKey( key ) || args.get( key ) == null )
			return null;
		Object v = args.get( key );
		if ( v instanceof Number )
			return null;
		if ( v instanceof String )
		{
			try
			{
				Integer.parseInt( ( (String) v ).trim() );
				return null;
			}
			catch ( NumberFormatException e )
			{
				return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "argument '" + key + "' must be an integer" );
			}
		}
		return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "argument '" + key + "' must be an integer" );
	}

	/**
	 * The raw string value of an argument (after validation has passed), or null if absent.
	 */
	public static String str( Map<String, Object> args, String key )
	{
		Object v = args == null ? null : args.get( key );
		return v instanceof String ? (String) v : ( v == null ? null : String.valueOf( v ) );
	}

	/** The boolean value of an argument, honoring a default and tolerating "true"/"false" strings. */
	public static boolean bool( Map<String, Object> args, String key, boolean defaultValue )
	{
		Object v = args == null ? null : args.get( key );
		if ( v instanceof Boolean )
			return (Boolean) v;
		if ( v instanceof String )
			return Boolean.parseBoolean( ( (String) v ).trim() );
		return defaultValue;
	}

	/** The integer value of an argument, honoring a default and tolerating numeric strings. */
	public static int intVal( Map<String, Object> args, String key, int defaultValue )
	{
		Object v = args == null ? null : args.get( key );
		if ( v instanceof Number )
			return ( (Number) v ).intValue();
		if ( v instanceof String )
		{
			try
			{
				return Integer.parseInt( ( (String) v ).trim() );
			}
			catch ( NumberFormatException e )
			{
				return defaultValue;
			}
		}
		return defaultValue;
	}
}
