package biouml.plugins.mcp.support;

/**
 * Per-request context that the MCP servlet layer sets so tool handlers can access request-scoped
 * information (currently: the public host headers for constructing absolute URLs).
 *
 * <p>Set by {@code McpServlet.service()} before dispatching to the tool handler and cleared
 * immediately after. Tool handlers that need the public host call {@link #publicOrigin()};
 * it returns {@code null} when no host was available (e.g. in-process or test invocations), in
 * which case the handler falls back to a relative URL.</p>
 */
public final class McpRequestContext
{
	private static final ThreadLocal<String> PUBLIC_ORIGIN = new ThreadLocal<>();

	private McpRequestContext()
	{
	}

	/** Set the public origin (e.g. {@code https://strange.genexplain.com/biouml}) for the current request thread. */
	public static void setPublicOrigin( String origin )
	{
		PUBLIC_ORIGIN.set( origin );
	}

	/**
	 * The public origin for the current request thread, or {@code null} when none was set
	 * (in-process calls, tests, or a request with no recognizable Host header).
	 */
	public static String getPublicOrigin()
	{
		return PUBLIC_ORIGIN.get();
	}

	/** Clear the context (call in a {@code finally} block after dispatch). */
	public static void clear()
	{
		PUBLIC_ORIGIN.remove();
	}
}
