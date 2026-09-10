package biouml.plugins.mcp._test;

import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import biouml.plugins.mcp.McpConstants;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Phase-1 transport spike.
 *
 * <p>Proves the chosen transport end-to-end at the protocol level, in-process and without
 * network: an MCP Java SDK ({@code io.modelcontextprotocol.sdk:mcp:0.11.2}) server with one
 * tool, driven through the stdio transport by raw JSON-RPC lines written to a piped input
 * stream. If this passes, the SDK is the server engine for the whole plugin; the HTTP and
 * in-process transports (phase 5) only need to plug into the same {@link McpSyncServer}.</p>
 */
public class McpTransportSpikeTest extends TestCase
{
	private final ObjectMapper mapper = new ObjectMapper();
	private McpSyncServer server;
	private StdioServerTransportProvider provider;
	private ByteArrayOutputStream stdout;
	private PipedOutputStream stdin;

	public static TestSuite suite()
	{
		return new TestSuite( McpTransportSpikeTest.class );
	}

	protected void setUp() throws Exception
	{
		super.setUp();
		stdout = new ByteArrayOutputStream();
		final PipedInputStream in = new PipedInputStream();
		stdin = new PipedOutputStream( in );

		provider = new StdioServerTransportProvider( mapper, in, stdout );

		server = McpServer.sync( provider )
				.serverInfo( new McpSchema.Implementation( McpConstants.SERVER_NAME, McpConstants.SERVER_VERSION ) )
				.capabilities( McpSchema.ServerCapabilities.builder().tools( true ).build() )
				.tools( McpServerFeatures.SyncToolSpecification.builder()
						.tool( McpSchema.Tool.builder()
								.name( "mcp_spike_ping" )
								.description( "Spike tool: echoes its argument to prove tools/call works" )
								.inputSchema( "{\"type\":\"object\",\"properties\":{\"message\":{\"type\":\"string\"}}}" )
								.build() )
						.callHandler( new java.util.function.BiFunction<McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult>()
						{
							@Override
							public McpSchema.CallToolResult apply( McpSyncServerExchange ex, McpSchema.CallToolRequest req )
							{
								Object message = req.arguments() == null ? null : req.arguments().get( "message" );
								String text = message == null ? "pong" : String.valueOf( message );
								return new McpSchema.CallToolResult(
										Collections.<McpSchema.Content>singletonList( new McpSchema.TextContent( text ) ),
										Boolean.FALSE );
							}
						} )
						.build() )
				.build();
	}

	/**
	 * Write one JSON-RPC line to the server's stdin and wait for the next line it writes to
	 * stdout.
	 */
	private String sendAndReadLine( String jsonLine ) throws Exception
	{
		int before = stdout.size();
		stdin.write( ( jsonLine + "\n" ).getBytes( StandardCharsets.UTF_8 ) );
		stdin.flush();
		long deadline = System.currentTimeMillis() + 15000;
		while ( System.currentTimeMillis() < deadline )
		{
			synchronized ( stdout )
			{
				if ( stdout.size() > before )
				{
					String all = stdout.toString( "UTF-8" );
					int start = before;
					int nl = all.indexOf( '\n', start );
					if ( nl >= 0 )
						return all.substring( start, nl );
				}
			}
			Thread.sleep( 20 );
		}
		throw new AssertionError( "server wrote no response within 15s for: " + jsonLine );
	}

	public void testInitializeToolsListAndCall() throws Exception
	{
		// 1) initialize — request the SDK's own latest version; the server negotiates it back.
		String init = sendAndReadLine( "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\""
				+ provider.protocolVersions().get( 0 ) + "\",\"capabilities\":{},\"clientInfo\":{\"name\":\"spike\",\"version\":\"0\"}}}" );
		assertTrue( "initialize response must contain a negotiated protocol version: " + init, init.contains( "\"protocolVersion\"" ) );
		assertTrue( "initialize response must contain the server name: " + init, init.contains( McpConstants.SERVER_NAME ) );

		// 2) notifications/initialized (no response expected)
		stdin.write( "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\",\"params\":{}}\n".getBytes( StandardCharsets.UTF_8 ) );
		stdin.flush();
		Thread.sleep( 200 );

		// 3) tools/list
		String list = sendAndReadLine( "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}" );
		assertTrue( "tools/list must list mcp_spike_ping: " + list, list.contains( "mcp_spike_ping" ) );

		// 4) tools/call — the ping tool echoes its `message` argument back as text content.
		String call = sendAndReadLine( "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"mcp_spike_ping\",\"arguments\":{\"message\":\"hello\"}}}" );
		assertTrue( "tools/call must return the echoed text content: " + call, call.contains( "hello" ) );
		assertTrue( "tools/call result must not be an error: " + call, call.contains( "\"isError\":false" ) );
	}

	public void testProtocolVersions()
	{
		// The 0.11.2 SDK negotiates the 2024-11-05 MCP protocol version. Assert the provider
		// declares a non-empty, well-formed version list rather than a hardcoded date, so the
		// test stays valid if the SDK is bumped to a newer protocol version later.
		List<String> versions = provider.protocolVersions();
		assertNotNull( "provider must declare protocol versions", versions );
		assertFalse( "provider must declare at least one protocol version", versions.isEmpty() );
		for ( String v : versions )
			assertTrue( "protocol version must look like yyyy-mm-dd: " + v, v.matches( "\\d{4}-\\d{2}-\\d{2}" ) );
	}
}
