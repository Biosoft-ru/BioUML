package biouml.plugins.mcp._test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import biouml.plugins.mcp.server.McpJsonRpcDispatcher;
import biouml.plugins.mcp.server.McpToolCatalog;
import biouml.plugins.mcp.support.McpEnvelope;

/**
 * The shared {@link McpJsonRpcDispatcher} must emit an MCP {@code image} content block (after the
 * text block) for a tool envelope that carries a rendered-image side-channel — so a plot PNG reaches
 * the client as a vision image, not a base64 text token bomb. Every other tool still returns a single
 * text content item (the envelope JSON), and the text block is always {@code content[0]}.
 */
public class McpDispatcherImageContentTest extends TestCase
{
	private static final byte[] FAKE_PNG = { (byte) 0x89, 'P', 'N', 'G', 1, 2, 3, 4, 5, 6, 7, 8, 9 };

	/** A dispatcher over a catalog of two tools: one returns a plain envelope, one carries an image. */
	private McpJsonRpcDispatcher dispatcher()
	{
		McpToolCatalog catalog = new McpToolCatalog();
		catalog.register( "t_plain", "plain tool", "{}", ( ex, args ) -> McpEnvelope.ok( "hello" ) );
		catalog.register( "t_image", "image tool", "{}", ( ex, args ) ->
		{
			McpEnvelope env = McpEnvelope.ok( "plot metadata" );
			env.setAttribute( "mcp.image", FAKE_PNG );
			return env;
		} );
		return new McpJsonRpcDispatcher( catalog );
	}

	@SuppressWarnings( "unchecked" )
	private Map<String, Object> call( McpJsonRpcDispatcher d, Object id, String name ) throws Exception
	{
		String json = "{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"method\":\"tools/call\",\"params\":{\"name\":\""
				+ name + "\",\"arguments\":{}}}";
		Map<String, Object> req = new com.fasterxml.jackson.databind.ObjectMapper().readValue( json, Map.class );
		return d.handle( req );
	}

	@SuppressWarnings( "unchecked" )
	public void testPlainToolIsSingleTextBlock() throws Exception
	{
		Map<String, Object> result = (Map<String, Object>) call( dispatcher(), 1, "t_plain" ).get( "result" );
		List<Map<String, Object>> content = (List<Map<String, Object>>) result.get( "content" );
		assertEquals( "single text content item", 1, content.size() );
		assertEquals( "text type", "text", content.get( 0 ).get( "type" ) );
		assertTrue( "envelope json in text", ( (String) content.get( 0 ).get( "text" ) ).contains( "hello" ) );
	}

	@SuppressWarnings( "unchecked" )
	public void testImageToolEmitsTextThenImage() throws Exception
	{
		Map<String, Object> result = (Map<String, Object>) call( dispatcher(), 2, "t_image" ).get( "result" );
		List<Map<String, Object>> content = (List<Map<String, Object>>) result.get( "content" );
		assertEquals( "two content items (text + image)", 2, content.size() );
		// Text block is FIRST: the envelope JSON (so content[0].text readers are unaffected).
		assertEquals( "first is text", "text", content.get( 0 ).get( "type" ) );
		assertTrue( "envelope json in text block", ( (String) content.get( 0 ).get( "text" ) ).contains( "plot metadata" ) );
		// Image block is SECOND, carrying base64 + mime.
		assertEquals( "second is image", "image", content.get( 1 ).get( "type" ) );
		assertEquals( "png mime", "image/png", content.get( 1 ).get( "mimeType" ) );
		byte[] decoded = java.util.Base64.getDecoder().decode( (String) content.get( 1 ).get( "data" ) );
		assertTrue( "image data round-trips", Arrays.equals( FAKE_PNG, decoded ) );
		// The base64 must NOT leak into the text block.
		assertFalse( "no base64 in text block",
				( (String) content.get( 0 ).get( "text" ) ).contains( java.util.Base64.getEncoder().encodeToString( FAKE_PNG ) ) );
	}

	@SuppressWarnings( "unchecked" )
	public void testErrorEnvelopeHasNoImageBlock() throws Exception
	{
		McpToolCatalog catalog = new McpToolCatalog();
		catalog.register( "t_err", "err tool", "{}", ( ex, args ) ->
		{
			McpEnvelope env = McpEnvelope.error( "invalid_params", "boom" );
			env.setAttribute( "mcp.image", FAKE_PNG ); // must be ignored for an error envelope
			return env;
		} );
		Map<String, Object> result = (Map<String, Object>) call( new McpJsonRpcDispatcher( catalog ), 3, "t_err" ).get( "result" );
		List<Map<String, Object>> content = (List<Map<String, Object>>) result.get( "content" );
		assertEquals( "error is a single text item (no image)", 1, content.size() );
		assertEquals( "isError true", Boolean.TRUE, result.get( "isError" ) );
	}
}
