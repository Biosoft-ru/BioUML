package biouml.plugins.mcp._test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import com.fasterxml.jackson.databind.ObjectMapper;

import biouml.plugins.mcp.server.McpJsonRpcDispatcher;
import biouml.plugins.mcp.server.McpToolCatalog;
import biouml.plugins.mcp.support.McpEnvelope;
import biouml.plugins.mcp.support.McpJsonSafe;
import ru.biosoft.access.core.DataElementPath;

/**
 * A tool payload carrying raw domain objects (e.g. a {@link DataElementPath} parameter default, or a
 * bean whose getters throw) must not turn the whole tool result into {@code serialization_error}:
 * {@link McpJsonSafe} reduces such values to plain JSON, and the dispatcher falls back to it.
 */
public class McpJsonSafeTest extends TestCase
{
	/** A bean Jackson cannot serialize: its only getter throws. */
	public static class Unserializable
	{
		public String getBoom()
		{
			throw new IllegalStateException( "boom" );
		}

		@Override
		public String toString()
		{
			return "unserializable";
		}
	}

	private enum Mode
	{
		FAST
	}

	@SuppressWarnings( "unchecked" )
	public void testToJsonSafe() throws Exception
	{
		Map<String, Object> in = new LinkedHashMap<String, Object>();
		in.put( "path", DataElementPath.create( "data/Projects/x" ) );
		in.put( "mode", Mode.FAST );
		in.put( "bean", new Unserializable() );
		in.put( "array", new Object[] { DataElementPath.create( "data/a" ), Integer.valueOf( 1 ), null } );
		in.put( "doubles", new double[] { 1.5, 2.5 } );
		in.put( "num", Double.valueOf( 3 ) );

		Map<String, Object> out = (Map<String, Object>) McpJsonSafe.toJsonSafe( in );
		assertEquals( "data/Projects/x", out.get( "path" ) );
		assertEquals( "FAST", out.get( "mode" ) );
		assertEquals( "unserializable", out.get( "bean" ) );
		assertEquals( Arrays.asList( "data/a", Integer.valueOf( 1 ), null ), out.get( "array" ) );
		assertEquals( Arrays.asList( Double.valueOf( 1.5 ), Double.valueOf( 2.5 ) ), out.get( "doubles" ) );
		assertEquals( Double.valueOf( 3 ), out.get( "num" ) );
		// and it really serializes
		new ObjectMapper().writeValueAsString( out );
	}

	@SuppressWarnings( "unchecked" )
	public void testDispatcherSanitizesUnserializablePayload() throws Exception
	{
		McpToolCatalog catalog = new McpToolCatalog();
		catalog.register( "t_raw", "raw payload tool", "{}", ( ex, args ) ->
		{
			Map<String, Object> data = new LinkedHashMap<String, Object>();
			data.put( "default", new Unserializable() );
			data.put( "path", DataElementPath.create( "data/Projects/x" ) );
			return McpEnvelope.ok( data );
		} );
		String json = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"t_raw\",\"arguments\":{}}}";
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> resp = new McpJsonRpcDispatcher( catalog ).handle( mapper.readValue( json, Map.class ) );
		Map<String, Object> result = (Map<String, Object>) resp.get( "result" );
		assertEquals( "sanitized result is not an error", Boolean.FALSE, result.get( "isError" ) );
		String text = (String) ( (List<Map<String, Object>>) result.get( "content" ) ).get( 0 ).get( "text" );
		Map<String, Object> env = mapper.readValue( text, Map.class );
		assertEquals( Boolean.TRUE, env.get( "ok" ) );
		Map<String, Object> data = (Map<String, Object>) env.get( "data" );
		assertEquals( "unserializable", data.get( "default" ) );
		assertEquals( "data/Projects/x", data.get( "path" ) );
	}
}
