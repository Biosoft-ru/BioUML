package biouml.plugins.mcp.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import biouml.plugins.mcp.support.McpEnvelope;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * The MCP tool catalog.
 *
 * <p>Collects all tools exposed by the BioUML MCP server and turns each into an SDK
 * {@link McpServerFeatures.SyncToolSpecification}. Tool implementations return
 * {@link McpEnvelope} objects which are serialized to JSON as the tool's text content (with
 * {@code isError=true} when the envelope is an error).</p>
 *
 * <p>Tools are registered by phase: repository tools (phase 2), analysis tools (phase 3), and
 * diagram/simulation tools (phase 4) each add themselves via {@link #register}.</p>
 */
public class McpToolCatalog
{
	/** A single MCP tool: name, description, JSON input schema, and a handler. */
	public static final class Tool
	{
		public final String name;
		public final String description;
		public final String inputSchema; // JSON Schema (string form)
		public final Handler handler;

		public interface Handler
		{
			McpEnvelope handle( McpSyncServerExchange ex, Map<String, Object> args ) throws Exception;
		}

		public Tool( String name, String description, String inputSchema, Handler handler )
		{
			this.name = name;
			this.description = description;
			this.inputSchema = inputSchema;
			this.handler = handler;
		}
	}

	private final List<Tool> tools = new ArrayList<Tool>();
	private final ObjectMapper mapper = new ObjectMapper();

	/**
	 * Register a tool.
	 */
	public McpToolCatalog register( String name, String description, String inputSchema, Tool.Handler handler )
	{
		tools.add( new Tool( name, description, inputSchema, handler ) );
		return this;
	}

	public List<Tool> getTools()
	{
		return tools;
	}

	/**
	 * Convert the catalog into SDK tool specifications for the MCP server.
	 */
	public List<McpServerFeatures.SyncToolSpecification> toSpecs()
	{
		List<McpServerFeatures.SyncToolSpecification> specs = new ArrayList<McpServerFeatures.SyncToolSpecification>();
		for ( final Tool tool : tools )
		{
			specs.add( McpServerFeatures.SyncToolSpecification.builder()
					.tool( McpSchema.Tool.builder()
							.name( tool.name )
							.description( tool.description )
							.inputSchema( tool.inputSchema )
							.build() )
					.callHandler( new java.util.function.BiFunction<McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult>()
					{
						@Override
						public McpSchema.CallToolResult apply( McpSyncServerExchange ex, McpSchema.CallToolRequest req )
						{
							return invoke( tool, ex, req.arguments() == null ? new LinkedHashMap<String, Object>() : req.arguments() );
						}
					} )
					.build() );
		}
		return specs;
	}

	private McpSchema.CallToolResult invoke( Tool tool, McpSyncServerExchange ex, Map<String, Object> args )
	{
		McpEnvelope env;
		try
		{
			env = tool.handler.handle( ex, args );
		}
		catch ( Throwable e )
		{
			// Catch Throwable (not just Exception): a handler can fail with an *Error* (e.g.
			// NoClassDefFoundError from a missing OSGi Require-Bundle), not only an Exception. A
			// structured internal error is returned either way — never a stack trace, and never an
			// empty/absent result.
			env = McpEnvelope.error( biouml.plugins.mcp.McpConstants.CODE_INTERNAL,
					"unexpected error in tool " + tool.name + ": " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
		String json;
		try
		{
			json = mapper.writeValueAsString( env.toMap() );
		}
		catch ( Exception e )
		{
			json = "{\"ok\":false,\"code\":\"serialization_error\"}";
		}
		boolean isError = !env.isOk();
		java.util.List<McpSchema.Content> content = new java.util.ArrayList<McpSchema.Content>();
		content.add( new McpSchema.TextContent( json ) );
		// Mirror the HTTP dispatcher: an envelope carrying a rendered image side-channel also emits an
		// MCP image content block (after the text block) so the in-process transport is identical.
		Object image = env.isOk() ? env.getAttribute( "mcp.image" ) : null;
		if ( image instanceof byte[] )
			content.add( new McpSchema.ImageContent( null,
					java.util.Base64.getEncoder().encodeToString( (byte[]) image ), "image/png" ) );
		return new McpSchema.CallToolResult( content, Boolean.valueOf( isError ) );
	}
}
