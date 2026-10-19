package biouml.plugins.mcp._test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;

import junit.framework.TestCase;

import biouml.plugins.mcp.McpConstants;
import biouml.plugins.mcp.server.BioumlMcpServer;
import biouml.plugins.mcp.server.McpLiteCatalog;
import biouml.plugins.mcp.server.McpServerFactory;
import biouml.plugins.mcp.server.McpToolCatalog;
import biouml.plugins.mcp.support.McpEnvelope;

/**
 * Tools with an {@code op} argument ({@link McpToolCatalog#registerOps}), the hidden tools they route
 * to, and the lite mode's {@code biouml_catalog} / {@code biouml_call} ({@link McpLiteCatalog}).
 */
@SuppressWarnings( "unchecked" )
public class McpToolOpsTest extends TestCase
{
	private final ObjectMapper mapper = new ObjectMapper();

	/** A catalog with two hidden tools that echo their arguments, merged into biouml_demo. */
	private static McpToolCatalog demo( String defaultOp )
	{
		McpToolCatalog catalog = new McpToolCatalog();
		catalog.registerHidden( "biouml_demo_a", "{\"type\":\"object\",\"properties\":{\"x\":{\"type\":\"string\"}}}", ( ex, args ) -> echo( "a", args ) );
		catalog.registerHidden( "biouml_demo_b", "{}", ( ex, args ) -> echo( "b", args ) );
		catalog.registerOps( "biouml_demo", "Demo. op a: echo. op b: echo renamed.",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}},\"required\":[\"path\"]}", defaultOp,
				McpToolCatalog.op( "a", "biouml_demo_a" ),
				McpToolCatalog.op( "b", "biouml_demo_b", "path>target" ) );
		return catalog;
	}

	private static McpEnvelope echo( String tool, Map<String, Object> args )
	{
		Map<String, Object> m = new LinkedHashMap<String, Object>( args );
		m.put( "tool", tool );
		return McpEnvelope.ok( m );
	}

	private static Map<String, Object> args( Object... kv )
	{
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		for ( int i = 0; i + 1 < kv.length; i += 2 )
			m.put( (String) kv[ i ], kv[ i + 1 ] );
		return m;
	}

	private static Map<String, Object> data( McpEnvelope env )
	{
		assertTrue( env.toString(), env.isOk() );
		return (Map<String, Object>) env.getData();
	}

	public void testOpRoutesToHiddenTool() throws Exception
	{
		McpToolCatalog catalog = demo( null );
		assertEquals( 1, catalog.getTools().size() );
		McpToolCatalog.Tool tool = catalog.find( "biouml_demo" );
		Map<String, Object> m = data( tool.handler.handle( null, args( "op", "a", "path", "p", "x", "1" ) ) );
		assertEquals( "a", m.get( "tool" ) );
		assertEquals( "1", m.get( "x" ) );
		m = data( tool.handler.handle( null, args( "op", "b", "path", "p" ) ) );
		assertEquals( "b", m.get( "tool" ) );
		assertEquals( "renamed for the hidden tool", "p", m.get( "target" ) );
		assertFalse( m.containsKey( "path" ) );
	}

	public void testHiddenToolCallableButNotListed()
	{
		McpToolCatalog catalog = demo( null );
		for ( McpToolCatalog.Tool t : catalog.getTools() )
			assertFalse( t.name.startsWith( "biouml_demo_" ) );
		assertNotNull( catalog.find( "biouml_demo_a" ) );
		assertNull( catalog.find( "biouml_demo_c" ) );
	}

	public void testMissingAndUnknownOp() throws Exception
	{
		McpToolCatalog.Tool tool = demo( null ).find( "biouml_demo" );
		McpEnvelope env = tool.handler.handle( null, args( "path", "p" ) );
		assertEquals( McpConstants.CODE_INVALID_PARAMS, env.getCode() );
		assertTrue( env.getError(), env.getError().contains( "[a, b]" ) );
		env = tool.handler.handle( null, args( "op", "c", "path", "p" ) );
		assertEquals( McpConstants.CODE_INVALID_PARAMS, env.getCode() );
	}

	public void testDefaultOp() throws Exception
	{
		McpToolCatalog.Tool tool = demo( "b" ).find( "biouml_demo" );
		assertEquals( "b", data( tool.handler.handle( null, args( "path", "p" ) ) ).get( "tool" ) );
	}

	public void testOpSchema() throws Exception
	{
		Map<String, Object> schema = mapper.readValue( demo( null ).find( "biouml_demo" ).inputSchema, Map.class );
		Map<String, Object> properties = (Map<String, Object>) schema.get( "properties" );
		assertEquals( "op comes first", "op", properties.keySet().iterator().next() );
		assertEquals( java.util.Arrays.asList( "a", "b" ), ( (Map<String, Object>) properties.get( "op" ) ).get( "enum" ) );
		assertEquals( java.util.Arrays.asList( "op", "path" ), schema.get( "required" ) );

		schema = mapper.readValue( demo( "a" ).find( "biouml_demo" ).inputSchema, Map.class );
		assertEquals( "with a default op, op is optional", java.util.Arrays.asList( "path" ), schema.get( "required" ) );
	}

	/** Every op of the real catalog routes to a tool that exists — a typo in a route would fail only when called. */
	public void testEveryOpRouteResolves()
	{
		assertEquals( new ArrayList<String>(), McpServerFactory.createCatalog().unresolvedOps() );
		McpToolCatalog broken = demo( null );
		broken.registerOps( "biouml_broken", "Broken.", "{}", null, McpToolCatalog.op( "x", "biouml_missing" ) );
		assertEquals( java.util.Arrays.asList( "biouml_broken x -> biouml_missing" ), broken.unresolvedOps() );
	}

	/** Listed descriptions name listed tools only, so a model is never sent to a tool it cannot see. */
	public void testDescriptionsNameListedTools()
	{
		McpToolCatalog catalog = McpServerFactory.createCatalog();
		List<String> listed = new ArrayList<String>();
		for ( McpToolCatalog.Tool tool : catalog.getTools() )
			listed.add( tool.name );
		Pattern name = Pattern.compile( "biouml_[a-z_]+[a-z]" );
		for ( McpToolCatalog.Tool tool : catalog.getTools() )
		{
			Matcher m = name.matcher( tool.description );
			while ( m.find() )
				assertTrue( tool.name + " names the unlisted " + m.group(), listed.contains( m.group() ) );
		}
	}

	public void testLiteListsTwoTools() throws Exception
	{
		BioumlMcpServer server = BioumlMcpServer.createLite();
		Map<String, Object> request = mapper.readValue( "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\",\"params\":{}}", Map.class );
		Map<String, Object> result = (Map<String, Object>) mapper.readValue( server.handle( request ), Map.class ).get( "result" );
		List<String> names = new ArrayList<String>();
		for ( Map<String, Object> tool : (List<Map<String, Object>>) result.get( "tools" ) )
			names.add( (String) tool.get( "name" ) );
		assertEquals( java.util.Arrays.asList( McpLiteCatalog.CATALOG, McpLiteCatalog.CALL ), names );
	}

	public void testLiteCatalogDrillDown() throws Exception
	{
		McpToolCatalog lite = McpLiteCatalog.create( McpServerFactory.createCatalog() );
		McpToolCatalog.Tool catalog = lite.find( McpLiteCatalog.CATALOG );

		Map<String, Object> root = data( catalog.handler.handle( null, args() ) );
		List<Map<String, Object>> groups = (List<Map<String, Object>>) root.get( "groups" );
		assertEquals( "perspective", groups.get( 0 ).get( "group" ) );
		Map<String, Object> diagram = null;
		for ( Map<String, Object> g : groups )
		{
			assertNotNull( g.get( "group" ) + " has a summary", g.get( "summary" ) );
			if ( "diagram".equals( g.get( "group" ) ) )
				diagram = g;
		}
		assertTrue( ( (List<String>) diagram.get( "tools" ) ).contains( "biouml_diagram_edit" ) );

		Map<String, Object> group = data( catalog.handler.handle( null, args( "group", "diagram" ) ) );
		Map<String, Object> edit = null;
		for ( Map<String, Object> t : (List<Map<String, Object>>) group.get( "tools" ) )
		{
			assertTrue( t.get( "name" ) + " summary is short", ( (String) t.get( "summary" ) ).length() <= 200 );
			if ( "biouml_diagram_edit".equals( t.get( "name" ) ) )
				edit = t;
		}
		assertTrue( ( (List<String>) edit.get( "ops" ) ).contains( "move" ) );
		assertEquals( java.util.Arrays.asList( "op", "diagramPath" ), edit.get( "required" ) );

		Map<String, Object> tool = data( catalog.handler.handle( null, args( "tool", "biouml_diagram_edit" ) ) );
		assertTrue( tool.get( "inputSchema" ) instanceof Map );
		assertTrue( ( (String) tool.get( "description" ) ).contains( "op move" ) );

		assertEquals( McpConstants.CODE_NOT_FOUND, catalog.handler.handle( null, args( "group", "nope" ) ).getCode() );
		assertEquals( McpConstants.CODE_NOT_FOUND, catalog.handler.handle( null, args( "tool", "biouml_diagram_nope" ) ).getCode() );
	}

	public void testFirstSentence() throws Exception
	{
		McpToolCatalog lite = McpLiteCatalog.create( McpServerFactory.createCatalog() );
		Map<String, Object> group = data( lite.find( McpLiteCatalog.CATALOG ).handler.handle( null, args( "group", "diagram" ) ) );
		for ( Map<String, Object> t : (List<Map<String, Object>>) group.get( "tools" ) )
			assertFalse( t.get( "name" ) + ": " + t.get( "summary" ), ( (String) t.get( "summary" ) ).endsWith( "e.g." ) );
	}

	public void testLiteCall() throws Exception
	{
		McpToolCatalog full = demo( null );
		McpToolCatalog lite = McpLiteCatalog.create( full );
		McpToolCatalog.Tool call = lite.find( McpLiteCatalog.CALL );

		Map<String, Object> m = data( call.handler.handle( null, args( "tool", "biouml_demo", "arguments", args( "op", "a", "path", "p" ) ) ) );
		assertEquals( "a", m.get( "tool" ) );

		m = data( call.handler.handle( null, args( "tool", "biouml_demo", "arguments", "{\"op\":\"b\",\"path\":\"p\"}" ) ) );
		assertEquals( "arguments given as a JSON string", "b", m.get( "tool" ) );

		m = data( call.handler.handle( null, args( "tool", "biouml_demo_a", "arguments", args( "x", "1" ) ) ) );
		assertEquals( "a hidden tool is callable", "a", m.get( "tool" ) );

		McpEnvelope env = call.handler.handle( null, args( "tool", "biouml_demo", "arguments", args( "op", "a" ) ) );
		assertEquals( McpConstants.CODE_INVALID_PARAMS, env.getCode() );
		assertTrue( env.getError(), env.getError().contains( "[path]" ) && env.getError().contains( "input schema" ) );

		assertEquals( McpConstants.CODE_NOT_FOUND, call.handler.handle( null, args( "tool", "biouml_nope" ) ).getCode() );
		assertEquals( McpConstants.CODE_INVALID_PARAMS, call.handler.handle( null, args() ).getCode() );
		assertEquals( McpConstants.CODE_INVALID_PARAMS,
				call.handler.handle( null, args( "tool", "biouml_demo", "arguments", "not json" ) ).getCode() );

		assertNotNull( "the full catalog's tools are callable directly in lite mode", lite.find( "biouml_demo" ) );
	}
}
