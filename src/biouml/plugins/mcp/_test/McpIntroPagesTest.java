package biouml.plugins.mcp._test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.util.TempFiles;

import biouml.plugins.mcp.support.McpIntroPages;

/**
 * Tests for reading perspective intro pages: against the web UI pages in the source tree (analysis presets,
 * links, help, an embedded page), a packed war (embedded pages followed, escapes ignored), cache refresh,
 * and a missing web root or page.
 */
@SuppressWarnings( "unchecked" )
public class McpIntroPagesTest extends AbstractBioUMLTest
{
	/** The web UI files in the source tree (tests run in BioUML/src). */
	static final String WEB_FILES = "ru/biosoft/server/servlets/webservices/webfiles";

	private String previous;
	private File dir;

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		previous = System.getProperty( McpIntroPages.WEB_PATH_PROPERTY );
		System.setProperty( McpIntroPages.WEB_PATH_PROPERTY, WEB_FILES );
	}

	@Override
	protected void tearDown() throws Exception
	{
		if ( previous == null )
			System.clearProperty( McpIntroPages.WEB_PATH_PROPERTY );
		else
			System.setProperty( McpIntroPages.WEB_PATH_PROPERTY, previous );
		if ( dir != null )
			ApplicationUtils.removeDir( dir );
		super.tearDown();
	}

	public void testAnalysisPresetsAndImportTargets()
	{
		Map<String, Object> intro = read( "intro-hocomoco" );
		assertEquals( "intro-hocomoco.html", intro.get( "source" ) );
		Map<String, Object> siteSearch = find( (List<Map<String, Object>>) intro.get( "analysisPresets" ), "analysis",
				"analyses/Methods/TF binding site search/Site search on track" );
		Map<String, String> params = (Map<String, String>) siteSearch.get( "params" );
		assertEquals( "HOCOMOCO/Matrices and profiles/PWM_HUMAN_mono_pval=0.0001", params.get( "profilePath" ) );
		assertEquals( "HOCOMOCO/Public datasets/human_TP53_gene_promoter_-1000+100", params.get( "trackPath" ) );
		assertTrue( ( (List<String>) intro.get( "importTargets" ) ).contains( "HOCOMOCO/User data" ) );
	}

	public void testLinksHelpAndSummary()
	{
		Map<String, Object> intro = read( "intro-microbiome" );
		List<Map<String, Object>> entryPoints = (List<Map<String, Object>>) intro.get( "entryPoints" );
		Map<String, Object> workflow = find( entryPoints, "path", "analyses/Workflows/Microbiome/Assembly basic" );
		assertEquals( "Basic assembly workflow", workflow.get( "title" ) );
		assertEquals( "no such element in the test repository", Boolean.FALSE, workflow.get( "exists" ) );
		Map<String, Object> spades = find( entryPoints, "path", "analyses/Galaxy/hybrid/spades" );
		assertTrue( "context from the enclosing list item: " + spades, String.valueOf( spades.get( "context" ) ).contains( "St. Petersburg" ) );
		assertNotNull( find( (List<Map<String, Object>>) intro.get( "help" ), "url", "http://wiki.biouml.org/index.php/SPAdes" ) );
		assertTrue( String.valueOf( intro.get( "summary" ) ).contains( "SPAdes" ) );
	}

	public void testEmbeddedPageAndLinkContext()
	{
		Map<String, Object> intro = read( "intro-regulatory-mutations" );
		assertEquals( Collections.singletonList( "intro-regulatory-mutations-content.html" ), intro.get( "embeddedPages" ) );
		List<Map<String, Object>> entryPoints = (List<Map<String, Object>>) intro.get( "entryPoints" );
		assertNotNull( find( entryPoints, "path", "analyses/Workflows/GTRD/Discover regulatory mutations using GTRD and TRANSFAC" ) );
		Map<String, Object> table = find( entryPoints, "path", "data/Examples/Regulatory mutations/Data/Results/Mutatated TF binding sites table" );
		assertEquals( "a table", table.get( "title" ) );
		assertTrue( "context from the link's line: " + table, String.valueOf( table.get( "context" ) ).contains( "open example results as a table" ) );
		assertTrue( String.valueOf( intro.get( "summary" ) ).contains( "Regulatory mutation is a change in DNA" ) );
	}

	public void testKnownEntryPointsSkipped()
	{
		McpIntroPages.Intro intro = McpIntroPages.read( "intro-chipmunk" );
		assertNotNull( intro );
		List<Map<String, Object>> all = (List<Map<String, Object>>) intro.toMap( Collections.<String>emptySet() ).get( "entryPoints" );
		String first = (String) all.get( 0 ).get( "path" );
		List<Map<String, Object>> rest = (List<Map<String, Object>>) intro.toMap( Collections.singleton( first ) ).get( "entryPoints" );
		assertEquals( all.size() - 1, rest.size() );
	}

	public void testWarEmbeddedPagesAndEscapes() throws Exception
	{
		dir = TempFiles.dir( "mcpIntroWar" );
		File war = new File( dir, "myweb.war" );
		try (ZipOutputStream zip = new ZipOutputStream( new FileOutputStream( war ) ))
		{
			put( zip, "intro-x.html", "<html><head><title>ignored</title></head><body><h1>X tools</h1>"
					+ "<p>Everything the X perspective offers for analysing your data.</p>"
					+ "<iframe src=\"sub/page.html\"></iframe><iframe src=\"../../etc/passwd.html\"></iframe>"
					+ "<iframe src=\"/other/x.html\"></iframe><iframe src=\"http://example.com/x.html\"></iframe>"
					+ "<script>$(\"#a\").load(\"/myweb/sub/more.html?v=1\");</script></body></html>" );
			put( zip, "sub/page.html", "<a href=\"/myweb/#de=data/Examples/My%20data&docOnly=true\">My data</a>" );
			put( zip, "sub/more.html", "<script>showAnalysisControl($(\"#b\"), \"analyses/Methods/X/Run\", {params: ["
					+ "{name: \"input\", value: \"data/Examples/My data\"}, {name: \"dynamic\", value: prefix + \"x\"}]});</script>" );
			put( zip, "other/x.html", "<a href=\"#de=data/Should/Not/Appear\">no</a>" );
		}
		System.setProperty( McpIntroPages.WEB_PATH_PROPERTY, war.getPath() );

		McpIntroPages.Intro parsed = McpIntroPages.read( "intro-x" );
		assertNotNull( parsed );
		assertEquals( "X tools: Everything the X perspective offers for analysing your data.", parsed.getDescription() );
		Map<String, Object> intro = parsed.toMap( Collections.<String>emptySet() );
		assertEquals( Arrays.asList( "sub/page.html", "sub/more.html" ), intro.get( "embeddedPages" ) );
		List<Map<String, Object>> entryPoints = (List<Map<String, Object>>) intro.get( "entryPoints" );
		assertEquals( 1, entryPoints.size() );
		assertEquals( "data/Examples/My data", entryPoints.get( 0 ).get( "path" ) );
		assertEquals( "My data", entryPoints.get( 0 ).get( "title" ) );
		Map<String, Object> preset = ( (List<Map<String, Object>>) intro.get( "analysisPresets" ) ).get( 0 );
		assertEquals( "analyses/Methods/X/Run", preset.get( "analysis" ) );
		assertEquals( "only literal values", Collections.singletonMap( "input", "data/Examples/My data" ), preset.get( "params" ) );
	}

	public void testChangedPageIsReread() throws Exception
	{
		dir = TempFiles.dir( "mcpIntroDir" );
		File page = new File( dir, "intro-y.html" );
		Files.write( page.toPath(), "<h1>First</h1>".getBytes( StandardCharsets.UTF_8 ) );
		System.setProperty( McpIntroPages.WEB_PATH_PROPERTY, dir.getPath() );
		assertEquals( "First", McpIntroPages.read( "intro-y" ).getDescription() );
		Files.write( page.toPath(), "<h1>Second</h1>".getBytes( StandardCharsets.UTF_8 ) );
		page.setLastModified( page.lastModified() + 5000 );
		assertEquals( "Second", McpIntroPages.read( "intro-y" ).getDescription() );
	}

	public void testMissingWebRootOrPage()
	{
		assertNull( "missing page", McpIntroPages.read( "intro-no-such-page" ) );
		assertNull( "page outside the web root", McpIntroPages.read( "../../../pom" ) );
		System.setProperty( McpIntroPages.WEB_PATH_PROPERTY, "no/such/web/root" );
		assertNull( "no web root", McpIntroPages.read( "intro-microbiome" ) );
	}

	private static Map<String, Object> read( String name )
	{
		McpIntroPages.Intro intro = McpIntroPages.read( name );
		assertNotNull( "intro page read: " + name, intro );
		return intro.toMap( Collections.<String>emptySet() );
	}

	private static void put( ZipOutputStream zip, String name, String content ) throws Exception
	{
		zip.putNextEntry( new ZipEntry( name ) );
		zip.write( content.getBytes( StandardCharsets.UTF_8 ) );
		zip.closeEntry();
	}

	private static Map<String, Object> find( List<Map<String, Object>> items, String key, String value )
	{
		assertNotNull( "list present", items );
		for ( Map<String, Object> item : items )
			if ( value.equals( item.get( key ) ) )
				return item;
		fail( "no item with " + key + "=" + value + " in " + items );
		return null;
	}
}
