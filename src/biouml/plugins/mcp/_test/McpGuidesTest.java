package biouml.plugins.mcp._test;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import biouml.plugins.mcp.McpConstants;
import biouml.plugins.mcp.support.McpAnalysisSupport;
import biouml.plugins.mcp.support.McpEnvelope;
import biouml.plugins.mcp.support.McpGuides;
import biouml.plugins.mcp.support.McpPerspectiveSupport;
import junit.framework.TestCase;
import ru.biosoft.util.TempFiles;

/**
 * The guides for agents ({@link McpGuides}): the file format, the bundled files, the server's own guide folder,
 * and the biouml_analysis_guide tool.
 */
public class McpGuidesTest extends TestCase
{
	private static final String[] BUNDLED = { "biouml.txt", "genexplain.txt" };

	private String savedDir;

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		savedDir = System.getProperty( McpPerspectiveSupport.GUIDES_PROPERTY );
		System.clearProperty( McpPerspectiveSupport.GUIDES_PROPERTY );
	}

	@Override
	protected void tearDown() throws Exception
	{
		if ( savedDir == null )
			System.clearProperty( McpPerspectiveSupport.GUIDES_PROPERTY );
		else
			System.setProperty( McpPerspectiveSupport.GUIDES_PROPERTY, savedDir );
		super.tearDown();
	}

	public void testParse() throws Exception
	{
		List<McpGuides.Guide> guides = McpGuides.parse( "# comment\n"
				+ "== analysis: First || Second one\n"
				+ "summary: What it does.\n"
				+ "use: When to pick it,\n"
				+ "  on two lines.\n"
				+ "unknown: not a field, so it continues 'use'\n"
				+ "tips: A tip: with a colon.\n"
				+ "\n"
				+ "== workflow: Folder/Flow\n"
				+ "summary: A pipeline.\n"
				+ "== topic: genes | I have genes\n"
				+ "summary: From genes to functions.\n"
				+ "Start: with a table.\n"
				+ "\n"
				+ "1. Classify.\n" );
		assertEquals( 3, guides.size() );
		McpGuides.Guide analysis = guides.get( 0 );
		assertEquals( McpGuides.ANALYSIS, analysis.kind );
		assertEquals( "First", analysis.name );
		Map<String, Object> fields = analysis.toMap();
		assertEquals( "What it does.", fields.get( "summary" ) );
		assertEquals( "When to pick it, on two lines. unknown: not a field, so it continues 'use'", fields.get( "use" ) );
		assertEquals( "A tip: with a colon.", fields.get( "tips" ) );
		assertFalse( fields.containsKey( "input" ) );
		assertEquals( "A pipeline.", guides.get( 1 ).getSummary() );
		McpGuides.Guide topic = guides.get( 2 );
		assertEquals( "genes", topic.name );
		assertEquals( "I have genes", topic.title );
		assertEquals( "From genes to functions.", topic.getSummary() );
		assertEquals( "Start: with a table.\n\n1. Classify.", topic.getBody() );
	}

	/** Every bundled entry has a summary of one moderate line, and no name is described twice. */
	public void testBundledFilesAreWellFormed() throws Exception
	{
		Set<String> seen = new HashSet<String>();
		int count = 0;
		for ( String file : BUNDLED )
		{
			String text = read( file );
			for ( McpGuides.Guide guide : McpGuides.parse( text ) )
			{
				count++;
				String summary = guide.getSummary();
				assertNotNull( file + ": no summary for " + guide.name, summary );
				assertTrue( file + ": summary of " + guide.name + " is too long for a list (" + summary.length() + ")", summary.length() <= 320 );
				if ( McpGuides.TOPIC.equals( guide.kind ) )
					assertTrue( file + ": topic " + guide.name + " has no text", guide.getBody().length() > 200 );
			}
			for ( String line : text.split( "\n" ) )
			{
				if ( !line.startsWith( "== " ) )
					continue;
				int colon = line.indexOf( ':' );
				assertTrue( file + ": bad entry line: " + line, colon > 0 );
				String kind = line.substring( 3, colon ).trim();
				assertTrue( file + ": unknown kind in: " + line,
						McpGuides.ANALYSIS.equals( kind ) || McpGuides.WORKFLOW.equals( kind ) || McpGuides.TOPIC.equals( kind ) );
				String names = McpGuides.TOPIC.equals( kind ) ? line.substring( colon + 1 ).split( "\\|" )[0] : line.substring( colon + 1 );
				for ( String name : names.split( "\\|\\|" ) )
					assertTrue( file + ": described twice: " + kind + " " + name.trim(),
							seen.add( kind + ":" + name.trim().replaceAll( "\\s+", " " ).toLowerCase() ) );
			}
		}
		assertTrue( "bundled guides were read, got " + count, count > 100 );
	}

	public void testBundledCatalog()
	{
		McpGuides.Guide guide = McpGuides.analysis( "functional classification" );
		assertNotNull( "names are matched ignoring case", guide );
		assertNotNull( guide.toMap().get( "use" ) );
		assertNull( McpGuides.analysis( "No such analysis" ) );
		assertFalse( McpGuides.topics().isEmpty() );
		assertNotNull( McpGuides.topic( "basics" ) );

		McpGuides.Guide byPath = McpGuides.workflow( "Common/Explain my genes" );
		assertNotNull( byPath );
		assertSame( "a workflow kept elsewhere is found by its name", byPath, McpGuides.workflow( "Explain my genes" ) );
		assertSame( "aliases share one entry", McpGuides.workflow( "TRANSFAC/Identify enriched motifs in promoters (TRANSFAC(R))" ),
				McpGuides.workflow( "GTRD/Identify enriched motifs in promoters (GTRD)" ) );

		assertTrue( McpGuides.matches( guide, null, McpGuides.words( "GO  Pathways" ) ) );
		assertFalse( McpGuides.matches( guide, null, McpGuides.words( "zzzz" ) ) );
		assertTrue( McpGuides.matches( null, "some name", McpGuides.words( "name" ) ) );
	}

	@SuppressWarnings( "unchecked" )
	public void testGuideTool()
	{
		McpEnvelope all = McpAnalysisSupport.guide( null );
		assertTrue( all.isOk() );
		List<Map<String, Object>> topics = (List<Map<String, Object>>) ( (Map<String, Object>) all.getData() ).get( "topics" );
		assertFalse( topics.isEmpty() );
		for ( Map<String, Object> topic : topics )
		{
			McpEnvelope one = McpAnalysisSupport.guide( (String) topic.get( "id" ) );
			assertTrue( "topic " + topic.get( "id" ), one.isOk() );
			Map<String, Object> data = (Map<String, Object>) one.getData();
			assertNotNull( data.get( "title" ) );
			assertTrue( ( (String) data.get( "text" ) ).length() > 200 );
		}
		McpEnvelope missing = McpAnalysisSupport.guide( "no-such-topic" );
		assertFalse( missing.isOk() );
		assertEquals( McpConstants.CODE_NOT_FOUND, missing.getCode() );
	}

	/** A server's own guide folder adds entries and replaces bundled ones; removing it restores them. */
	public void testExternalGuides() throws Exception
	{
		String bundled = McpGuides.analysis( "Functional classification" ).getSummary();
		File dir = TempFiles.dir( "mcpGuidesTest" );
		try
		{
			File folder = new File( dir, "analysis-guides" );
			assertTrue( folder.mkdirs() );
			Files.write( new File( folder, "site.txt" ).toPath(), ( "== analysis: Functional classification\nsummary: Replaced.\n"
					+ "== analysis: Site analysis\nsummary: Ours.\n== topic: site | Our topic\nsummary: Ours too.\nText.\n" ).getBytes( StandardCharsets.UTF_8 ) );
			System.setProperty( McpPerspectiveSupport.GUIDES_PROPERTY, dir.getPath() );
			assertEquals( "Replaced.", McpGuides.analysis( "Functional classification" ).getSummary() );
			assertEquals( "Ours.", McpGuides.analysis( "Site analysis" ).getSummary() );
			assertEquals( "Our topic", McpGuides.topic( "site" ).title );
			assertNotNull( "bundled entries stay", McpGuides.topic( "basics" ) );
		}
		finally
		{
			System.clearProperty( McpPerspectiveSupport.GUIDES_PROPERTY );
			deleteAll( dir );
		}
		assertEquals( bundled, McpGuides.analysis( "Functional classification" ).getSummary() );
		assertNull( McpGuides.analysis( "Site analysis" ) );
	}

	private static void deleteAll( File file )
	{
		File[] children = file.listFiles();
		if ( children != null )
			for ( File child : children )
				deleteAll( child );
		file.delete();
	}

	private static String read( String file ) throws Exception
	{
		try (InputStream in = McpGuides.class.getResourceAsStream( "resources/analysis-guides/" + file ))
		{
			assertNotNull( "bundled guide file " + file, in );
			return new String( in.readAllBytes(), StandardCharsets.UTF_8 );
		}
	}
}
