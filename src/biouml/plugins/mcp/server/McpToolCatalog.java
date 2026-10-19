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
 *
 * <p>To keep {@code tools/list} short, related operations are listed as one tool with an {@code op}
 * argument ({@link #registerOps}) that routes each op to a <em>hidden</em> tool ({@link #registerHidden}).
 * A hidden tool is not listed but stays callable by its name, so agents and skills written against the
 * earlier one-tool-per-operation names keep working.</p>
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

	/**
	 * One op of a tool registered with {@link #registerOps}: the op value, the hidden tool it runs, and
	 * the argument renames from the merged tool's names to that tool's ({@code "nodes>elements"}).
	 */
	public static final class Op
	{
		final String op;
		final String tool;
		final String[] renames;

		Op( String op, String tool, String[] renames )
		{
			this.op = op;
			this.tool = tool;
			this.renames = renames;
		}
	}

	public static Op op( String op, String tool, String... renames )
	{
		return new Op( op, tool, renames );
	}

	private final List<Tool> tools = new ArrayList<Tool>();
	private final List<Tool> hidden = new ArrayList<Tool>();
	/** The ops of each tool registered with {@link #registerOps}. */
	private final Map<String, Op[]> opsByTool = new LinkedHashMap<String, Op[]>();
	/** Tools found by {@link #find} when this catalog has no tool of that name; may be null. */
	private final McpToolCatalog fallback;
	private final ObjectMapper mapper = new ObjectMapper();

	public McpToolCatalog()
	{
		this( null );
	}

	/**
	 * A catalog whose {@link #find} falls back to {@code fallback}: its own tools are listed, and every
	 * tool of {@code fallback} stays callable.
	 */
	public McpToolCatalog( McpToolCatalog fallback )
	{
		this.fallback = fallback;
	}

	/**
	 * Register a tool.
	 */
	public McpToolCatalog register( String name, String description, String inputSchema, Tool.Handler handler )
	{
		tools.add( new Tool( name, description, inputSchema, handler ) );
		return this;
	}

	/**
	 * Register a tool that is callable by name but not listed: an op of a tool registered with
	 * {@link #registerOps}, under the name it had before it was merged.
	 */
	public McpToolCatalog registerHidden( String name, String inputSchema, Tool.Handler handler )
	{
		hidden.add( new Tool( name, null, inputSchema, handler ) );
		return this;
	}

	/**
	 * Register a tool that runs one of several hidden tools, chosen by its {@code op} argument. The
	 * {@code op} property (a string enum of the op values) is added to {@code inputSchema}; it is
	 * required unless {@code defaultOp} is given. The arguments are passed on to the op's tool, renamed
	 * as the op says; arguments of other ops are passed too and ignored.
	 */
	@SuppressWarnings( "unchecked" )
	public McpToolCatalog registerOps( String name, String description, String inputSchema, final String defaultOp, final Op... ops )
	{
		final List<String> names = new ArrayList<String>();
		for ( Op o : ops )
			names.add( o.op );
		Map<String, Object> schema;
		try
		{
			schema = mapper.readValue( inputSchema, LinkedHashMap.class );
		}
		catch ( Exception e )
		{
			throw new IllegalArgumentException( "bad input schema of tool " + name, e );
		}
		Map<String, Object> opProperty = new LinkedHashMap<String, Object>();
		opProperty.put( "type", "string" );
		opProperty.put( "enum", names );
		if ( defaultOp != null )
			opProperty.put( "description", "default: " + defaultOp );
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put( "op", opProperty );
		if ( schema.get( "properties" ) instanceof Map )
			properties.putAll( (Map<String, Object>) schema.get( "properties" ) );
		schema.put( "type", "object" );
		schema.put( "properties", properties );
		List<Object> required = new ArrayList<Object>();
		if ( defaultOp == null )
			required.add( "op" );
		if ( schema.get( "required" ) instanceof List )
			required.addAll( (List<Object>) schema.get( "required" ) );
		schema.remove( "required" );
		if ( !required.isEmpty() )
			schema.put( "required", required );
		String schemaJson;
		try
		{
			schemaJson = mapper.writeValueAsString( schema );
		}
		catch ( Exception e )
		{
			throw new IllegalArgumentException( "bad input schema of tool " + name, e );
		}
		opsByTool.put( name, ops );
		return register( name, description, schemaJson, ( ex, args ) -> {
			Object value = args.get( "op" );
			String opName = value == null ? defaultOp : String.valueOf( value );
			if ( opName == null )
				return McpEnvelope.error( biouml.plugins.mcp.McpConstants.CODE_INVALID_PARAMS, "missing required argument: op (one of " + names + ")" );
			for ( Op o : ops )
				if ( o.op.equals( opName ) )
				{
					Tool tool = find( o.tool );
					Map<String, Object> routed = new LinkedHashMap<String, Object>( args );
					for ( String rename : o.renames )
					{
						String from = rename.substring( 0, rename.indexOf( '>' ) );
						String to = rename.substring( rename.indexOf( '>' ) + 1 );
						if ( routed.containsKey( from ) && !routed.containsKey( to ) )
							routed.put( to, routed.remove( from ) );
					}
					return tool.handler.handle( ex, routed );
				}
			return McpEnvelope.error( biouml.plugins.mcp.McpConstants.CODE_INVALID_PARAMS, "op must be one of " + names + ", got: " + opName );
		} );
	}

	/** The ops whose tool this catalog does not find, as {@code "tool op -> missing tool"}; empty when all resolve. */
	public List<String> unresolvedOps()
	{
		List<String> result = new ArrayList<String>();
		for ( Map.Entry<String, Op[]> e : opsByTool.entrySet() )
			for ( Op o : e.getValue() )
				if ( find( o.tool ) == null )
					result.add( e.getKey() + " " + o.op + " -> " + o.tool );
		return result;
	}

	/** The listed tools. */
	public List<Tool> getTools()
	{
		return tools;
	}

	/** A listed or hidden tool of this catalog, or else of the fallback catalog; null when there is none. */
	public Tool find( String name )
	{
		if ( name == null )
			return null;
		for ( Tool t : tools )
			if ( name.equals( t.name ) )
				return t;
		for ( Tool t : hidden )
			if ( name.equals( t.name ) )
				return t;
		return fallback == null ? null : fallback.find( name );
	}

	/**
	 * A short hash of the tools' names, descriptions and input schemas. It changes exactly when the
	 * tool list changes, so it is part of the server version: clients that cache tool definitions per
	 * server version then fetch the new list after a deploy.
	 */
	public String fingerprint()
	{
		try
		{
			java.security.MessageDigest digest = java.security.MessageDigest.getInstance( "SHA-256" );
			for ( Tool tool : tools )
				for ( String part : new String[] { tool.name, tool.description, tool.inputSchema } )
				{
					digest.update( String.valueOf( part ).getBytes( java.nio.charset.StandardCharsets.UTF_8 ) );
					digest.update( (byte) 0 );
				}
			StringBuilder hex = new StringBuilder();
			for ( byte b : java.util.Arrays.copyOf( digest.digest(), 4 ) )
				hex.append( String.format( "%02x", Integer.valueOf( b & 0xff ) ) );
			return hex.toString();
		}
		catch ( java.security.NoSuchAlgorithmException e )
		{
			return Integer.toHexString( tools.size() );
		}
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
