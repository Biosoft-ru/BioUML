package biouml.plugins.mcp._test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.SecurityProvider;
import ru.biosoft.access.security.TestSecurityProvider;
import ru.biosoft.util.TempFiles;

import biouml.plugins.mcp.McpConstants;
import biouml.plugins.mcp.server.McpServerFactory;
import biouml.plugins.mcp.server.McpToolCatalog;
import biouml.plugins.mcp.support.McpEnvelope;
import biouml.plugins.mcp.support.McpIntroPages;
import biouml.plugins.mcp.support.McpPerspectiveSupport;
import biouml.plugins.mcp.support.McpProviderSupport;
import biouml.plugins.mcp.web.McpServlet;
import biouml.workbench.perspective.PerspectiveRegistry;

/**
 * Tests for the perspective MCP tools against the perspectives declared in plugconfig: listing with
 * descriptions/entry points, a clean error for an unknown name, and a selection that applies to the MCP
 * session only (the user's perspective preference is untouched) and survives across HTTP requests.
 */
@SuppressWarnings( "unchecked" )
public class McpPerspectiveToolsTest extends AbstractBioUMLTest
{
	@Override
	protected void tearDown() throws Exception
	{
		// The headless MCP session is shared by all tests in this JVM: do not leave a perspective selected.
		McpProviderSupport.inSession( () -> PerspectiveRegistry.setSessionPerspective( null ) );
		super.tearDown();
	}

	public void testToolsRegistered()
	{
		McpToolCatalog.Tool select = null;
		boolean list = false;
		for ( McpToolCatalog.Tool tool : McpServerFactory.createCatalog().getTools() )
		{
			if ( "biouml_perspective_list".equals( tool.name ) )
				list = true;
			if ( "biouml_perspective_select".equals( tool.name ) )
				select = tool;
		}
		assertTrue( "biouml_perspective_list registered", list );
		assertNotNull( "biouml_perspective_select registered", select );
	}

	public void testSelectRequiresName() throws Exception
	{
		for ( McpToolCatalog.Tool tool : McpServerFactory.createCatalog().getTools() )
			if ( "biouml_perspective_select".equals( tool.name ) )
			{
				McpEnvelope env = tool.handler.handle( null, new LinkedHashMap<String, Object>() );
				assertFalse( env.isOk() );
				assertEquals( McpConstants.CODE_INVALID_PARAMS, env.getCode() );
			}
	}

	public void testListDescribesPerspectives()
	{
		McpEnvelope env = McpPerspectiveSupport.list();
		assertTrue( env.toString(), env.isOk() );
		Map<String, Object> data = (Map<String, Object>) env.getData();
		List<Map<String, Object>> perspectives = (List<Map<String, Object>>) data.get( "perspectives" );
		assertEquals( perspectives.size(), ( (Number) data.get( "count" ) ).intValue() );
		assertEquals( "Default", perspectives.get( 0 ).get( "name" ) );

		int current = 0;
		for ( Map<String, Object> p : perspectives )
			if ( Boolean.TRUE.equals( p.get( "current" ) ) )
			{
				current++;
				assertEquals( data.get( "current" ), p.get( "name" ) );
			}
		assertEquals( "exactly one perspective is current", 1, current );

		Map<String, Object> microbiome = find( perspectives, "Microbiome" );
		assertTrue( String.valueOf( microbiome.get( "description" ) ).contains( "SPAdes" ) );
		List<Map<String, Object>> entryPoints = (List<Map<String, Object>>) microbiome.get( "entryPoints" );
		assertEquals( "analyses/Workflows/Microbiome/Assembly basic", entryPoints.get( 0 ).get( "path" ) );
		assertEquals( "Basic assembly workflow", entryPoints.get( 0 ).get( "title" ) );
		List<Map<String, Object>> repository = (List<Map<String, Object>>) microbiome.get( "repository" );
		assertEquals( "databases", repository.get( 0 ).get( "path" ) );
		assertFalse( "UI-only keys are dropped", microbiome.containsKey( "viewparts" ) );
		assertFalse( "UI-only keys are dropped", microbiome.containsKey( "intro" ) );
	}

	public void testSelectUnknownPerspective()
	{
		McpEnvelope env = McpPerspectiveSupport.select( "No such perspective" );
		assertFalse( env.isOk() );
		assertEquals( McpConstants.CODE_NOT_FOUND, env.getCode() );
		assertTrue( "error lists the available perspectives: " + env.getError(), env.getError().contains( "Default" ) );
	}

	public void testSelectAppliesToSessionOnly() throws Exception
	{
		Object preference = Application.getPreferences() == null ? null
				: Application.getPreferences().getValue( PerspectiveRegistry.PERSPECTIVE_PREFERENCE );

		McpEnvelope env = McpPerspectiveSupport.select( "GTRD" );
		assertTrue( env.toString(), env.isOk() );
		Map<String, Object> data = (Map<String, Object>) env.getData();
		assertEquals( "GTRD", data.get( "name" ) );
		assertEquals( "session", data.get( "scope" ) );
		assertNotNull( data.get( "entryPoints" ) );
		Map<String, Object> rules = (Map<String, Object>) data.get( "rules" );
		assertTrue( "GTRD has importer rules", rules.containsKey( "importers" ) );
		assertTrue( "GTRD denies export actions", rules.containsKey( "actions" ) );

		// The selection is what the platform's importer/exporter registries now see in this session ...
		assertEquals( "GTRD", McpProviderSupport.inSession( () -> PerspectiveRegistry.getCurrentPerspectiveName( "Default" ) ) );
		assertEquals( "GTRD", ( (Map<String, Object>) McpPerspectiveSupport.list().getData() ).get( "current" ) );
		// ... while the user's perspective preference is untouched.
		Object after = Application.getPreferences() == null ? null
				: Application.getPreferences().getValue( PerspectiveRegistry.PERSPECTIVE_PREFERENCE );
		assertEquals( "perspective preference unchanged", preference, after );

		assertTrue( McpPerspectiveSupport.select( "Default" ).isOk() );
		assertEquals( "Default", ( (Map<String, Object>) McpPerspectiveSupport.list().getData() ).get( "current" ) );
	}

	public void testSelectReadsIntroPage()
	{
		Map<String, Object> plain = (Map<String, Object>) McpPerspectiveSupport.select( "HOCOMOCO" ).getData();
		assertNull( "no web UI files found: no intro block", plain.get( "intro" ) );

		String previous = System.getProperty( McpIntroPages.WEB_PATH_PROPERTY );
		System.setProperty( McpIntroPages.WEB_PATH_PROPERTY, McpIntroPagesTest.WEB_FILES );
		try
		{
			McpEnvelope env = McpPerspectiveSupport.select( "HOCOMOCO" );
			assertTrue( env.toString(), env.isOk() );
			Map<String, Object> intro = (Map<String, Object>) ( (Map<String, Object>) env.getData() ).get( "intro" );
			assertNotNull( "intro block", intro );
			assertEquals( "intro-hocomoco.html", intro.get( "source" ) );
			List<Map<String, Object>> presets = (List<Map<String, Object>>) intro.get( "analysisPresets" );
			assertEquals( "analyses/Methods/TF binding site search/Site search on track", presets.get( 0 ).get( "analysis" ) );
			assertNotNull( presets.get( 0 ).get( "params" ) );
		}
		finally
		{
			if ( previous == null )
				System.clearProperty( McpIntroPages.WEB_PATH_PROPERTY );
			else
				System.setProperty( McpIntroPages.WEB_PATH_PROPERTY, previous );
		}
	}

	/** Guides are bundled for the Genome Enhancer perspectives; a configured directory wins over the bundled ones. */
	public void testGuides() throws Exception
	{
		String guide = McpPerspectiveSupport.guide( "Genome Enhancer" );
		assertNotNull( "bundled guide", guide );
		assertTrue( guide.contains( "Generate workflow from annotation diagram" ) );
		assertEquals( "the same guide for both perspectives", guide, McpPerspectiveSupport.guide( "Omics Suite (Genome Enhancer)" ) );
		assertNull( McpPerspectiveSupport.guide( "Default" ) );

		File dir = TempFiles.dir( "mcpGuides" );
		String previous = System.getProperty( McpPerspectiveSupport.GUIDES_PROPERTY );
		try
		{
			Files.write( new File( dir, "index.properties" ).toPath(),
					"Default=default.txt\nGTRD=../secret.txt\n".getBytes( StandardCharsets.UTF_8 ) );
			Files.write( new File( dir, "default.txt" ).toPath(), "  Start from analyses/Methods.\n".getBytes( StandardCharsets.UTF_8 ) );
			System.setProperty( McpPerspectiveSupport.GUIDES_PROPERTY, dir.getPath() );
			assertEquals( "Start from analyses/Methods.", McpPerspectiveSupport.guide( "Default" ) );
			assertNull( "not a plain file name", McpPerspectiveSupport.guide( "GTRD" ) );
			assertEquals( "not in the directory: the bundled one", guide, McpPerspectiveSupport.guide( "Genome Enhancer" ) );
		}
		finally
		{
			if ( previous == null )
				System.clearProperty( McpPerspectiveSupport.GUIDES_PROPERTY );
			else
				System.setProperty( McpPerspectiveSupport.GUIDES_PROPERTY, previous );
			ApplicationUtils.removeDir( dir );
		}
	}

	/** biouml_perspective_page reads a page of the web UI as text; a missing page is a clean error. */
	public void testPage()
	{
		String previous = System.getProperty( McpIntroPages.WEB_PATH_PROPERTY );
		System.setProperty( McpIntroPages.WEB_PATH_PROPERTY, McpIntroPagesTest.WEB_FILES );
		try
		{
			McpEnvelope env = McpPerspectiveSupport.page( "intro-microbiome.html", 0 );
			assertTrue( env.toString(), env.isOk() );
			Map<String, Object> page = (Map<String, Object>) env.getData();
			assertTrue( String.valueOf( page.get( "text" ) ).contains( "SPAdes" ) );
			assertEquals( McpConstants.CODE_NOT_FOUND, McpPerspectiveSupport.page( "no-such-page.html", 0 ).getCode() );
			assertEquals( McpConstants.CODE_NOT_FOUND, McpPerspectiveSupport.page( "../../pom.xml", 0 ).getCode() );
		}
		finally
		{
			if ( previous == null )
				System.clearProperty( McpIntroPages.WEB_PATH_PROPERTY );
			else
				System.setProperty( McpIntroPages.WEB_PATH_PROPERTY, previous );
		}
	}

	/**
	 * The Basic-auth path logs in on every request; the session state must survive that, so a perspective
	 * selected with biouml_perspective_select is still in effect on the next request with the same credentials.
	 */
	public void testBasicAuthKeepsSelectionAcrossRequests() throws Exception
	{
		java.lang.reflect.Field field = SecurityManager.class.getDeclaredField( "securityProvider" );
		field.setAccessible( true );
		SecurityProvider previous = (SecurityProvider) field.get( null );
		field.set( null, new TestSecurityProvider() );
		try
		{
			McpServlet servlet = new McpServlet();
			servlet.initForTest();
			String authorization = "Basic " + Base64.getEncoder().encodeToString(
					( "mcpuser:token:" + UUID.randomUUID() ).getBytes( StandardCharsets.UTF_8 ) );

			Map<String, Object> select = callTool( servlet, authorization, "biouml_perspective_select", "{\"name\":\"GTRD\"}" );
			assertEquals( "select: " + select, Boolean.TRUE, select.get( "ok" ) );
			Map<String, Object> list = callTool( servlet, authorization, "biouml_perspective_list", "{}" );
			assertEquals( "list: " + list, Boolean.TRUE, list.get( "ok" ) );
			assertEquals( "selection kept across requests", "GTRD", ( (Map<String, Object>) list.get( "data" ) ).get( "current" ) );
		}
		finally
		{
			field.set( null, previous );
		}
	}

	/** Call a tool through the servlet's real entry point and return its envelope ({ok, data, ...}). */
	private static Map<String, Object> callTool(McpServlet servlet, String authorization, String tool, String arguments) throws Exception
	{
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		params.put( McpServlet.MCP_RAW_BODY_KEY, new String[] { "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\""
				+ tool + "\",\"arguments\":" + arguments + "}}" } );
		params.put( "Authorization", new String[] { authorization } );
		params.put( "Remote-address", new String[] { "127.0.0.1" } );
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Map<String, String> header = new HashMap<String, String>();
		servlet.service( "/mcp", null, params, out, header );
		String body = new String( out.toByteArray(), StandardCharsets.UTF_8 );
		assertEquals( "HTTP status, body=" + body, "200", header.get( "X-MCP-Status" ) );
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> result = (Map<String, Object>) mapper.readValue( body, Map.class ).get( "result" );
		List<Map<String, Object>> content = (List<Map<String, Object>>) result.get( "content" );
		return mapper.readValue( (String) content.get( 0 ).get( "text" ), Map.class );
	}

	private static Map<String, Object> find( List<Map<String, Object>> perspectives, String name )
	{
		for ( Map<String, Object> p : perspectives )
			if ( name.equals( p.get( "name" ) ) )
				return p;
		fail( "perspective not listed: " + name );
		return null;
	}
}
