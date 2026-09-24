package biouml.plugins.mcp._test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestCase;

import biouml.plugins.mcp.McpConstants;
import biouml.plugins.mcp.support.McpEnvelope;
import biouml.plugins.mcp.support.McpProviderSupport;

import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.BiosoftWebResponse;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.providers.WebJSONProviderSupport;

/**
 * Phase-1 tests for the {@link McpProviderSupport} wrapper core. These verify the bridge in isolation
 * (request building → provider {@code process} → JSON capture → envelope translation) using a small
 * in-test {@link WebJSONProviderSupport}, since the real providers' registries (e.g. the OSGi-backed
 * analysis registry) are not wired in a plain unit-test JVM. The instance
 * {@link McpProviderSupport#invoke(WebJSONProviderSupport, String, String, Map)} overload is used so
 * no OSGi extension registry is required.
 */
public class McpProviderSupportTest extends TestCase
{
	/** A minimal provider that exercises the wrapper: echoes scalars, returns a JSON object, reads an array. */
	public static class StubProvider extends WebJSONProviderSupport
	{
		@Override
		public void process( BiosoftWebRequest arguments, JSONResponse response ) throws Exception
		{
			String action = arguments.getAction();
			switch ( action )
			{
				case "echo":
				{
					Map<String, Object> m = new LinkedHashMap<String, Object>();
					m.put( "de", arguments.getString( "de" ) );
					m.put( "flag", arguments.getBoolean( "flag" ) );
					response.sendJSON( m );
					return;
				}
				case "list":
				{
					String[] names = arguments.getStrings( "names" );
					response.sendStringArray( names );
					return;
				}
				case "fail":
					response.error( "stub failure" );
					return;
				default:
					throw new RuntimeException( "unexpected action " + action );
			}
		}
	}

	@SuppressWarnings( "unchecked" )
	private Map<String, Object> data( McpEnvelope env )
	{
		if ( !env.isOk() )
			fail( "expected ok envelope, got " + env.getCode() + " " + env.getError() );
		return (Map<String, Object>) env.getData();
	}

	public void testUnknownPrefixIsNotFound()
	{
		McpEnvelope env = McpProviderSupport.invoke( "no_such_prefix_xyz", "echo", new LinkedHashMap<String, Object>() );
		assertFalse( "unknown prefix must be not_found", env.isOk() );
		assertEquals( McpConstants.CODE_NOT_FOUND, env.getCode() );
	}

	public void testEchoRoundTrip()
	{
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		params.put( "de", "mcpg/diagrams/d1" );
		params.put( "flag", Boolean.TRUE );
		McpEnvelope env = McpProviderSupport.invoke( new StubProvider(), "stub", "echo", params );
		Map<String, Object> d = data( env );
		assertEquals( "de passed through as element path", "mcpg/diagrams/d1", d.get( "de" ) );
		assertEquals( "boolean passed through", Boolean.TRUE, d.get( "flag" ) );
	}

	public void testArrayParamReadAsStrings()
	{
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		params.put( "names", Arrays.asList( "a", "b", "c" ) );
		McpEnvelope env = McpProviderSupport.invoke( new StubProvider(), "stub", "list", params );
		assertTrue( "array round-trip must be ok, got " + env.getCode() + " " + env.getError(), env.isOk() );
		// sendStringArray emits {type:OK, values:[a,b,c]} — the top-level `values` is a JSON array,
		// so the envelope data is a List (not a Map).
		Object values = env.getData();
		assertTrue( "values must be a list, was " + values, values instanceof java.util.List );
		assertEquals( 3, ( (java.util.List<?>) values ).size() );
		assertEquals( "b", ( (java.util.List<?>) values ).get( 1 ) );
	}

	public void testErrorEnvelopeTranslation()
	{
		McpEnvelope env = McpProviderSupport.invoke( new StubProvider(), "stub", "fail", new LinkedHashMap<String, Object>() );
		assertFalse( "provider error must produce a non-ok envelope", env.isOk() );
		assertNotNull( "error must carry a message", env.getError() );
	}

	public void testPathAliasBecomesDe()
	{
		// The `path` alias is promoted to the `de` key the provider reads.
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		params.put( "path", "mcpg/x/y" );
		Map<String, String> req = McpProviderSupport.buildRequest( "echo", params );
		assertEquals( "path alias -> de", "mcpg/x/y", req.get( "de" ) );
		assertEquals( "action present", "echo", req.get( "action" ) );
	}

	public void testNonJsonProviderRejected()
	{
		// A provider that is not a WebJSONProviderSupport is rejected cleanly.
		ru.biosoft.server.servlets.webservices.providers.WebProvider notJson =
				new ru.biosoft.server.servlets.webservices.providers.WebProvider()
				{
					@Override
					public void process( BiosoftWebRequest req, BiosoftWebResponse resp ) throws Exception
					{
					}
				};
		McpEnvelope env = McpProviderSupport.invoke( notJson, "stub", "echo", new LinkedHashMap<String, Object>() );
		assertFalse( env.isOk() );
		assertEquals( McpConstants.CODE_INTERNAL, env.getCode() );
	}
}
