package biouml.plugins.mcp._test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import junit.framework.TestCase;

import biouml.plugins.mcp.server.BioumlMcpServer;
import biouml.plugins.mcp.server.McpToolCatalog;
import biouml.plugins.mcp.server.McpServerFactory;

/**
 * Phase-5 test: the in-process MCP server runs the full initialize → tools/list → tools/call cycle
 * without HTTP, and exposes the same tool set as the factory catalog.
 */
public class McpInProcessServerTest extends TestCase
{
	private final ObjectMapper mapper = new ObjectMapper();
	private BioumlMcpServer server;

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		server = BioumlMcpServer.create();
	}

	@SuppressWarnings( "unchecked" )
	private Map<String, Object> parse( String json ) throws Exception
	{
		return (Map<String, Object>) mapper.readValue( json, Map.class );
	}

	@SuppressWarnings( "unchecked" )
	private List<String> toolNames( String toolsListJson ) throws Exception
	{
		Map<String, Object> resp = parse( toolsListJson );
		Map<String, Object> result = (Map<String, Object>) resp.get( "result" );
		List<Map<String, Object>> tools = (List<Map<String, Object>>) result.get( "tools" );
		List<String> names = new java.util.ArrayList<String>();
		for ( Map<String, Object> t : tools )
			names.add( (String) t.get( "name" ) );
		return names;
	}

	public void testInitializeToolsListAndCall() throws Exception
	{
		// 1) initialize
		String init = server.handle( parse( "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-03-26\",\"capabilities\":{},\"clientInfo\":{\"name\":\"test\",\"version\":\"0\"}}}" ) );
		Map<String, Object> initResp = parse( init );
		Map<String, Object> initResult = (Map<String, Object>) initResp.get( "result" );
		assertNotNull( "initialize result present", initResult );
		assertEquals( "protocol version negotiated", "2025-03-26", initResult.get( "protocolVersion" ) );
		Map<String, Object> info = (Map<String, Object>) initResult.get( "serverInfo" );
		assertEquals( "server name", "biouml", info.get( "name" ) );
		assertNotNull( "server version", info.get( "version" ) );

		// 2) notifications/initialized (no response)
		String notif = server.handle( parse( "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\",\"params\":{}}" ) );
		assertEquals( "notification produces no response body", "", notif );

		// 3) tools/list
		String list = server.handle( parse( "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}" ) );
		List<String> names = toolNames( list );
		assertTrue( "repo tool present", names.contains( "biouml_repo_collections" ) );
		assertTrue( "analysis tool present", names.contains( "biouml_analysis_list" ) );
		assertTrue( "diagram tool present", names.contains( "biouml_diagram_create" ) );
		assertTrue( "simulation tool present", names.contains( "biouml_simulation_run" ) );
		assertTrue( "project tool present", names.contains( "biouml_project_create" ) );
		assertTrue( "project list tool present", names.contains( "biouml_project_list" ) );
		assertTrue( "user info tool present", names.contains( "biouml_user_info" ) );
		assertTrue( "at least 10 tools, was " + names.size(), names.size() >= 10 );

		// 4) tools/call — repo_collections works headlessly (lists registered roots, possibly empty)
		String call = server.handle( parse( "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"biouml_repo_collections\",\"arguments\":{}}}" ) );
		Map<String, Object> callResp = parse( call );
		Map<String, Object> callResult = (Map<String, Object>) callResp.get( "result" );
		assertNotNull( "tools/call result present", callResult );
		List<Map<String, Object>> content = (List<Map<String, Object>>) callResult.get( "content" );
		assertTrue( "content present", content != null && !content.isEmpty() );
		String text = (String) content.get( 0 ).get( "text" );
		assertTrue( "tool result is the JSON envelope (ok field)", text.contains( "\"ok\"" ) );
		assertEquals( "tools/call is not an error", Boolean.FALSE, callResult.get( "isError" ) );
	}

	/**
	 * The project tools are registered and enforce their argument contracts: a missing required argument
	 * yields a structured invalid_params envelope, and the name validator rejects malformed names.
	 */
	@SuppressWarnings( "unchecked" )
	public void testProjectToolsValidation() throws Exception
	{
		// biouml_project_create with no name → invalid_params (not an exception, not a crash).
		String call = server.handle( parse( "{\"jsonrpc\":\"2.0\",\"id\":10,\"method\":\"tools/call\",\"params\":{\"name\":\"biouml_project_create\",\"arguments\":{}}}" ) );
		Map<String, Object> resp = parse( call );
		Map<String, Object> result = (Map<String, Object>) resp.get( "result" );
		List<Map<String, Object>> content = (List<Map<String, Object>>) result.get( "content" );
		String text = (String) content.get( 0 ).get( "text" );
		assertTrue( "missing name → invalid_params envelope: " + text,
				text.contains( "invalid_params" ) || text.contains( "project name is required" ) );

		// biouml_project_size with no name → invalid_params.
		String call2 = server.handle( parse( "{\"jsonrpc\":\"2.0\",\"id\":11,\"method\":\"tools/call\",\"params\":{\"name\":\"biouml_project_size\",\"arguments\":{}}}" ) );
		Map<String, Object> resp2 = parse( call2 );
		Map<String, Object> result2 = (Map<String, Object>) resp2.get( "result" );
		List<Map<String, Object>> content2 = (List<Map<String, Object>>) result2.get( "content" );
		String text2 = (String) content2.get( 0 ).get( "text" );
		assertTrue( "missing name → invalid_params envelope: " + text2,
				text2.contains( "invalid_params" ) || text2.contains( "project name is required" ) );

		// Name validator: accepts normal names, rejects empty / bad-leading-character names.
		assertTrue( "valid name accepted", biouml.plugins.mcp.support.McpProjectSupport.isProjectNameValid( "My Project_1" ) );
		assertFalse( "empty name rejected", biouml.plugins.mcp.support.McpProjectSupport.isProjectNameValid( "" ) );
		assertFalse( "bad leading char rejected", biouml.plugins.mcp.support.McpProjectSupport.isProjectNameValid( "-bad" ) );
	}

	@SuppressWarnings( "unchecked" )
	public void testUnknownToolIsMethodNotFound() throws Exception
	{
		String call = server.handle( parse( "{\"jsonrpc\":\"2.0\",\"id\":9,\"method\":\"tools/call\",\"params\":{\"name\":\"biouml_no_such_tool\",\"arguments\":{}}}" ) );
		Map<String, Object> resp = parse( call );
		Map<String, Object> error = (Map<String, Object>) resp.get( "error" );
		assertNotNull( "unknown tool → JSON-RPC error", error );
		assertEquals( "error code -32601 (method not found)", -32601, ( (Number) error.get( "code" ) ).intValue() );
		// No stack trace / raw exception in the message.
		String msg = (String) error.get( "message" );
		assertTrue( "message has no stack trace", msg != null && !msg.contains( "at " ) && !msg.contains( "Exception" ) );
	}

	@SuppressWarnings( "unchecked" )
	public void testInProcessToolListMatchesFactory() throws Exception
	{
		// The in-process tool list must equal the factory catalog's tool set (acceptance #5: HTTP and
		// in-process are identical; both derive from the same McpServerFactory catalog).
		String list = server.handle( parse( "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}" ) );
		List<String> fromServer = toolNames( list );

		McpToolCatalog catalog = McpServerFactory.createCatalog();
		List<String> fromCatalog = new java.util.ArrayList<String>();
		for ( McpToolCatalog.Tool t : catalog.getTools() )
			fromCatalog.add( t.name );

		assertEquals( "in-process list size == catalog size", fromCatalog.size(), fromServer.size() );
		assertEquals( "in-process tool set == catalog tool set", new java.util.HashSet<String>( fromCatalog ), new java.util.HashSet<String>( fromServer ) );
	}
}
