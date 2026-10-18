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

	/**
	 * A wizard like Genome Enhancer's: an iframe to an .shtml page with server-side includes, the page's own script
	 * files (shared libraries skipped), steps with help pages, an analysis started by a script with literal and
	 * user-chosen parameters, repository paths built by the scripts and a project list with a demo flag.
	 */
	public void testWizardPage() throws Exception
	{
		dir = TempFiles.dir( "mcpIntroWizard" );
		write( "intro-wiz.html", "<script>function isWiz(p) { return p && p.name; }</script>"
				+ "<iframe id=\"wiz_iframe\" src=\"wz/index.shtml?mode=x\" width=\"99%\"></iframe>" );
		write( "wz/index.shtml", "<!DOCTYPE html><html><head><title>Wizard Tool</title>"
				+ "<script src=\"../lib/jquery/jquery.js\"></script><script src=\"steps.js\"></script>"
				+ "<script src=\"lib/x.min.js\"></script><script src=\"https://cdn.example.com/y.js\"></script>"
				+ "</head><body><!--#include file=\"wizard.shtml\"--></body></html>" );
		write( "wz/wizard.shtml", "<div id=\"step-a\" class=\"step-content\"><!--#include file=\"title.shtml\"-->"
				+ "<p>Project table, Delete project, Start new project and other controls of the step.</p></div>"
				+ "<div id=\"titles\"><div step-id=\"step-a\"><button><strong>Select<br>project</strong></button></div>"
				+ "<div step-id=\"step-b\"><button><strong>Start<br>analysis</strong></button></div></div>"
				+ "<div id=\"bullets\"><div step-id=\"step-a\"><button></button></div><div step-id=\"step-b\"><button></button></div></div>" );
		write( "wz/title.shtml", "<h1>Enhance everything</h1>" );
		write( "wz/steps.js", "var mode2maxStep = {a: 'Old step'};\n"
				+ "function projectData(name) { var root = \"wzdata/Projects\"; return root + \"/\" + name + \"/Data\"; }\n"
				+ "function diagramPath() { return \"wzdata/Projects/\" + getCurrentProject() + \"/Data/Description\"; }\n"
				+ "window.parent.queryBioUML(\"support/getProjectsData\", {}, function(data) { var demo = projects[i].demo == \"true\"; });\n"
				+ "window.parent.ServerMessages.subscribe(\"importProgress/\" + id, function() {});\n"
				+ "window.parent.refreshTreeBranch(\"wzdata/Projects\");\n"
				+ "var url = hr + window.parent.appInfo.serverPath + 'web/content/' + id;\n"
				+ "var last = names[0].name + \"/Report\";\n"
				+ "function start() {\n"
				+ "  //{name: \"species\", value: species},\n"
				+ "  /*{name: \"cutoffFC\", value: cutoffFC},\n  {name: \"old\", value: \"x\"}*/\n"
				+ "  var params = [{name: \"tissue\", value: tissue}, {name: \"notifyOnComplete\", value: true},\n"
				+ "    {\"name\":\"maxStep\", \"value\":\"Old step\"}, {name: \"mode\", value: \"exact\"}];\n"
				+ "  window.parent.queryBioUML(\"web/analysis\", { de: \"analyses/Methods/Test/mcp.test\", json: JSON.stringify(params) });\n"
				+ "  if(window.parent.isPathExists(outPath + \"/Report\")) open(\"#de=\" + outPath + \"/Report\");\n"
				+ "}\n" );
		write( "lib/jquery/jquery.js", "showAnalysisControl($('#x'), 'analyses/Methods/Lib/Should not appear');" );
		write( "wz/help/step-a.shtml", "<html><head><title>Help</title></head><body><h5>Welcome</h5>"
				+ "<p>Hello and welcome to the Wizard tool, a pipeline that analyses things for you.</p><p>You can browse projects here.</p>"
				+ "<!--#include file=\"guide.shtml\"--></body></html>" );
		write( "wz/help/guide.shtml", "<h2>User guide</h2><ul><li>First the data is read.</li><li>Then it is analysed.</li></ul>"
				+ "<a href=\"https://wiz.example.com/bioumlweb/#de=analyses/Methods/X/Y%20Z\">Y Z</a>" );
		System.setProperty( McpIntroPages.WEB_PATH_PROPERTY, dir.getPath() );
		File data = TempFiles.dir( "mcpIntroWizardData" );
		try
		{
			registerRoot( "wzdata", data );

			McpIntroPages.Intro parsed = McpIntroPages.read( "intro-wiz" );
			assertNotNull( parsed );
			assertEquals( "a wizard is named by its page title, described by its first step's help",
					"Wizard Tool: Hello and welcome to the Wizard tool, a pipeline that analyses things for you.", parsed.getDescription() );
			assertEquals( "demo", parsed.getProjectFlag() );
			// parameter values checked against the analysis: here 'Old step' is not one of its choices
			Map<String, Object> intro = parsed.toMap( Collections.<String>emptySet(),
					( analysis, parameter, value ) -> !( "maxStep".equals( parameter ) && "Old step".equals( value ) ) );
			assertEquals( Collections.singletonList( "wz/index.shtml" ), intro.get( "embeddedPages" ) );
			assertEquals( "Wizard Tool", intro.get( "title" ) );
			assertNull( "a wizard's text is its controls: no summary", intro.get( "summary" ) );

			List<Map<String, Object>> steps = (List<Map<String, Object>>) intro.get( "steps" );
			assertEquals( 2, steps.size() );
			assertEquals( "step-a", steps.get( 0 ).get( "id" ) );
			assertEquals( "Select project", steps.get( 0 ).get( "title" ) );
			assertEquals( "wz/help/step-a.shtml", steps.get( 0 ).get( "helpPage" ) );
			assertEquals( "Hello and welcome to the Wizard tool, a pipeline that analyses things for you. You can browse projects here.",
					steps.get( 0 ).get( "guide" ) );
			assertEquals( "Start analysis", steps.get( 1 ).get( "title" ) );
			assertNull( "no help page", steps.get( 1 ).get( "helpPage" ) );

			List<Map<String, Object>> presets = (List<Map<String, Object>>) intro.get( "analysisPresets" );
			assertEquals( "the library's analysis is not read: " + presets, 1, presets.size() );
			Map<String, Object> preset = presets.get( 0 );
			assertEquals( "analyses/Methods/Test/mcp.test", preset.get( "analysis" ) );
			assertEquals( "mcp.test", preset.get( "name" ) );
			Map<String, Object> params = (Map<String, Object>) preset.get( "params" );
			assertEquals( "true", params.get( "notifyOnComplete" ) );
			assertEquals( "exact", params.get( "mode" ) );
			assertFalse( "a value the analysis does not take is dropped", params.containsKey( "maxStep" ) );
			assertEquals( "the page's path does not exist", Boolean.FALSE, preset.get( "exists" ) );
			// the analysis registry knows the real path (the Genome Enhancer wizard writes "Genome enhancer" for "Genome Enhancer")
			Map<String, Object> resolved = ( (List<Map<String, Object>>) parsed.toMap( Collections.<String>emptySet(), new McpIntroPages.ParameterCheck()
			{
				@Override
				public Boolean accepts( String analysis, String parameter, String value )
				{
					return null;
				}

				@Override
				public String pathOf( String analysis )
				{
					return "mcp.test".equals( analysis ) ? "analyses/Methods/Real/mcp.test" : null;
				}
			} ).get( "analysisPresets" ) ).get( 0 );
			assertEquals( "analyses/Methods/Real/mcp.test", resolved.get( "analysis" ) );
			assertEquals( Boolean.TRUE, resolved.get( "exists" ) );
			assertEquals( "commented-out parameters are not read", Collections.singletonList( "tissue" ), preset.get( "chosenByUser" ) );
			assertFalse( params.containsKey( "old" ) );

			List<String> templates = (List<String>) intro.get( "pathTemplates" );
			assertTrue( templates.toString(), templates.contains( "wzdata/Projects/{currentProject}/Data/Description" ) );
			assertTrue( "a variable is replaced by its value: " + templates, templates.contains( "wzdata/Projects/{name}/Data" ) );
			assertTrue( templates.toString(), templates.contains( "{outPath}/Report" ) );
			assertFalse( "not a repository path: " + templates, templates.contains( "importProgress/{id}" ) );
			assertEquals( "no URL, no tail of a member access: " + templates, 3, templates.size() );
			assertNotNull( find( (List<Map<String, Object>>) intro.get( "entryPoints" ), "path", "wzdata/Projects" ) );

			// a help page in full, includes expanded
			Map<String, Object> page = McpIntroPages.pageText( "wz/help/step-a.shtml", 0 );
			assertEquals( "Help", page.get( "title" ) );
			String text = (String) page.get( "text" );
			assertTrue( text, text.startsWith( "## Welcome\nHello and welcome to the Wizard tool" ) );
			assertTrue( text, text.contains( "## User guide\n- First the data is read.\n- Then it is analysed." ) );
			assertNotNull( find( (List<Map<String, Object>>) page.get( "links" ), "path", "analyses/Methods/X/Y Z" ) );
			Map<String, Object> rest = McpIntroPages.pageText( "wz/help/step-a.shtml", 10 );
			assertEquals( text.substring( 10 ), rest.get( "text" ) );
			assertNull( "not a page", McpIntroPages.pageText( "wz/steps.js", 0 ) );
			assertNull( "outside the web root", McpIntroPages.pageText( "../wz/help/step-a.shtml", 0 ) );
			assertNull( "no such page", McpIntroPages.pageText( "wz/help/none.shtml", 0 ) );

			// a changed script is read again
			File script = new File( dir, "wz/steps.js" );
			Files.write( script.toPath(), ( new String( Files.readAllBytes( script.toPath() ), StandardCharsets.UTF_8 )
					+ "var more = [{name: \"rows\", value: 7}, {name: \"x\", value: y}];\nfunction go() { de = \"analyses/Methods/Test/mcp.test\"; }\n" )
							.getBytes( StandardCharsets.UTF_8 ) );
			script.setLastModified( script.lastModified() + 5000 );
			Map<String, Object> again = McpIntroPages.read( "intro-wiz" ).toMap( Collections.<String>emptySet() );
			Map<String, Object> againParams = (Map<String, Object>) ( (List<Map<String, Object>>) again.get( "analysisPresets" ) ).get( 0 ).get( "params" );
			assertEquals( "7", againParams.get( "rows" ) );
			assertEquals( "unchecked: all values found", "Old step", againParams.get( "maxStep" ) );
		}
		finally
		{
			ApplicationUtils.removeDir( data );
		}
	}

	private void write( String path, String content ) throws Exception
	{
		File file = new File( dir, path );
		file.getParentFile().mkdirs();
		Files.write( file.toPath(), content.getBytes( StandardCharsets.UTF_8 ) );
	}

	/** A repository root with a "Projects" folder, so path templates under it are kept. */
	private static void registerRoot( String name, File dir ) throws Exception
	{
		java.util.Properties properties = new ru.biosoft.util.ExProperties();
		properties.setProperty( ru.biosoft.access.core.DataCollectionConfigConstants.NAME_PROPERTY, name );
		properties.setProperty( ru.biosoft.access.core.DataCollectionConfigConstants.CLASS_PROPERTY, ru.biosoft.access.LocalRepository.class.getName() );
		properties.setProperty( ru.biosoft.access.core.DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, dir.toString() );
		ru.biosoft.util.ExProperties.store( properties, new File( dir, ru.biosoft.access.core.DataCollectionConfigConstants.DEFAULT_CONFIG_FILE ) );
		ru.biosoft.access.Repository repository = (ru.biosoft.access.Repository) ru.biosoft.access.core.CollectionFactory.createCollection( null, properties );
		ru.biosoft.access.core.CollectionFactory.registerRoot( repository );
		repository.put( ru.biosoft.access.generic.GenericDataCollection.createGenericCollection( repository, repository, "Projects", "Projects" ) );
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
