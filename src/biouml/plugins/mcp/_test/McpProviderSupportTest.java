package biouml.plugins.mcp._test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestCase;

import biouml.plugins.mcp.McpConstants;
import biouml.plugins.mcp.support.McpEnvelope;
import biouml.plugins.mcp.support.McpProviderSupport;

import ru.biosoft.access._test.AbstractBioUMLTest;
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
 * no OSGi extension registry is required. Extends {@link AbstractBioUMLTest} so the access
 * {@code Environment} (class loading, etc.) is initialized for the repository-based test.
 */
public class McpProviderSupportTest extends AbstractBioUMLTest
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

	public void testEnsureWebSessionBootstrap()
	{
		// Verify that ensureWebSession() successfully bootstraps a headless WebSession.
		Object ws = McpProviderSupport.ensureWebSession();
		assertNotNull( "WebSession bootstrap failed", ws );
		assertTrue( "WebSession is not a WebSession instance",
				ws instanceof ru.biosoft.server.servlets.webservices.WebSession );
		// Verify that getCurrentSession() can now find it on this thread.
		ru.biosoft.server.servlets.webservices.WebSession current =
				ru.biosoft.server.servlets.webservices.WebSession.getCurrentSession();
		assertNotNull( "getCurrentSession() returned null after bootstrap", current );
	}

	/**
	 * resolveElement must fall back to a URL-decoded form when the raw path does not resolve. Agents
	 * (and the web UI's own cookies) pass paths with spaces encoded as %20; a repo element whose name
	 * contains a space is stored with a literal space, so the raw (encoded) path misses and only the
	 * decoded form resolves. A path that resolves raw must be returned untouched (never corrupted).
	 */
	public void testResolveElementUrlDecodeFallback() throws Exception
	{
		java.io.File dir = ru.biosoft.util.TempFiles.dir( "mcpResolveTest" );
		try
		{
			java.util.Properties properties = new ru.biosoft.util.ExProperties();
			properties.setProperty( ru.biosoft.access.core.DataCollectionConfigConstants.NAME_PROPERTY, "mcpresolve" );
			properties.setProperty( ru.biosoft.access.core.DataCollectionConfigConstants.CLASS_PROPERTY,
					ru.biosoft.access.LocalRepository.class.getName() );
			properties.setProperty( ru.biosoft.access.core.DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, dir.toString() );
			ru.biosoft.util.ExProperties.store( properties,
					new java.io.File( dir, ru.biosoft.access.core.DataCollectionConfigConstants.DEFAULT_CONFIG_FILE ) );
			ru.biosoft.access.Repository repository = (ru.biosoft.access.Repository)
					ru.biosoft.access.core.CollectionFactory.createCollection( null, properties );
			ru.biosoft.access.core.CollectionFactory.registerRoot( repository );
			repository.getNameList();

			// A folder whose NAME contains a space.
			ru.biosoft.access.core.DataCollection<?> spaced = ru.biosoft.access.generic.GenericDataCollection
					.createGenericCollection( repository, repository, "has space", "has space" );
			assertNotNull( "spaced folder created", spaced );
			repository.put( spaced );

			String rawPath = "mcpresolve/has space";
			String encodedPath = "mcpresolve/has%20space";

			// The raw path resolves directly.
			assertNotNull( "raw (literal-space) path resolves", McpProviderSupport.resolveElement( rawPath ) );
			// The encoded path does NOT resolve raw, but resolveElement's fallback decodes it and finds it.
			assertNull( "encoded path does not resolve raw", ru.biosoft.access.core.CollectionFactory.getDataElement( encodedPath ) );
			ru.biosoft.access.core.DataElement de = McpProviderSupport.resolveElement( encodedPath );
			assertNotNull( "encoded path resolves via the URL-decode fallback", de );
			assertEquals( "decoded to the literal-space element", rawPath, de.getCompletePath().toString() );

			// A path that has no %XX is returned untouched (no spurious decode).
			assertNull( "nonexistent plain path is null", McpProviderSupport.resolveElement( "mcpresolve/nope" ) );
		}
		finally
		{
			com.developmentontheedge.application.ApplicationUtils.removeDir( dir );
		}
	}
}
