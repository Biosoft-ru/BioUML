package biouml.plugins.mcp;

/**
 * Constants for the BioUML MCP (Model Context Protocol) service plugin.
 *
 * <p>The plugin exposes BioUML platform functionality (repository browsing, context-menu
 * actions, analyses, diagrams, simulations) as MCP tools so that AI agents can drive the
 * platform programmatically instead of through the user interface.</p>
 */
public final class McpConstants
{
	/** MCP server name reported in the initialize response. */
	public static final String SERVER_NAME = "biouml";

	/** MCP server version reported in the initialize response. */
	public static final String SERVER_VERSION = "1.0.0";

	/**
	 * MCP protocol version implemented (streamable HTTP). We advertise {@code 2025-11-25}, the
	 * revision Claude's connector (and current MCP clients) speak. The revision is additive over
	 * {@code 2025-03-26} — it extends authorization discovery (OIDC / OAuth Client ID Metadata) and
	 * adds optional features (tool icons, elicitation, tasks) that a {@code tools}-only server need
	 * not implement — so the core initialize/tools/ping wire shape is unchanged. Advertising a
	 * version the client speaks is essential: per the spec, if the client cannot support the version
	 * the server responds with, it MUST disconnect, which is what was breaking Claude's connector.
	 */
	public static final String PROTOCOL_VERSION = "2025-11-25";

	/** Context path of the HTTP MCP endpoint on the embedded Tomcat. */
	public static final String SERVLET_PATH = "/mcp";

	/** Prefix of all MCP tool names exposed by this server. */
	public static final String TOOL_PREFIX = "biouml_";

	/**
	 * Error code returned in the JSON envelope when a tool argument is missing or of the
	 * wrong type.
	 */
	public static final String CODE_INVALID_PARAMS = "invalid_params";

	/**
	 * Error code returned when a requested repository path does not resolve to an element.
	 */
	public static final String CODE_NOT_FOUND = "not_found";

	/**
	 * Error code returned when a repository path resolves outside the registered
	 * repository roots (traversal attempt).
	 */
	public static final String CODE_PATH_ESCAPE = "path_escape";

	/**
	 * Error code returned when a requested action can only be performed from an
	 * interactive UI (opens dialogs/documents).
	 */
	public static final String CODE_INTERACTIVE_ONLY = "requires_interactive_ui";

	/**
	 * Error code returned when a requested action is denied for safety reasons
	 * (interactive dialogs, admin actions, or anything mutating outside the repository).
	 */
	public static final String CODE_ACTION_DENIED = "action_denied";

	/**
	 * Error code returned when a diagram has no dynamic model to simulate.
	 */
	public static final String CODE_MISSING_MODEL = "missing_dynamic_model";

	/**
	 * Error code returned when an internal error escaped a tool handler. The message is
	 * a short, structured description — never a raw stack trace.
	 */
	public static final String CODE_INTERNAL = "internal_error";

	/** Maximum number of children returned by list tools before truncation. */
	public static final int LIST_CHILDREN_CAP = 100;

	/** Maximum number of table rows returned by result tools before truncation. */
	public static final int TABLE_ROWS_CAP = 50;

	/** Maximum serialized size (bytes) of a single describe/result payload. */
	public static final int PAYLOAD_BYTES_CAP = 200 * 1024;

	private McpConstants()
	{
	}
}
