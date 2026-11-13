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
	private static final String[] BUNDLED = { "biouml.txt" };

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

	/** Every bundled entry has a summary of one moderate line, and no file describes a name twice. */
	public void testBundledFilesAreWellFormed() throws Exception
	{
		int count = 0;
		for ( String file : BUNDLED )
		{
			Set<String> seen = new HashSet<String>(); // a later file may replace an entry of an earlier one
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
						McpGuides.ANALYSIS.equals( kind ) || McpGuides.WORKFLOW.equals( kind ) || McpGuides.TOPIC.equals( kind )
								|| McpGuides.PARAMETERS.equals( kind ) );
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

		assertSame( "aliases share one entry", McpGuides.analysis( "Join two tables" ), McpGuides.analysis( "Join tables" ) );

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

	public void testSaysNothing()
	{
		assertTrue( McpGuides.saysNothing( null, "promoterFrom", "Promoter from" ) );
		assertTrue( McpGuides.saysNothing( " ", "promoterFrom", "Promoter from" ) );
		assertTrue( "repeats the display name", McpGuides.saysNothing( "Promoter from", "expertSettings/promoterFrom", "Promoter from" ) );
		assertTrue( "repeats the key", McpGuides.saysNothing( "namesDescription", "group/namesDescription", null ) );
		assertFalse( McpGuides.saysNothing( "Start of the promoter relative to the TSS", "promoterFrom", "Promoter from" ) );
	}

	/** Guides describe the parameters the analysis leaves undescribed, and add a hint to the others. */
	public void testDescribeParameters() throws Exception
	{
		File dir = TempFiles.dir( "mcpGuidesParams" );
		try
		{
			File folder = new File( dir, "analysis-guides" );
			assertTrue( folder.mkdirs() );
			Files.write( new File( folder, "site.txt" ).toPath(), ( "== analysis: Site analysis\nsummary: Ours.\n"
					+ "param cutoff: Scores below it are dropped;\n  0.05 is usual.\n"
					+ "param settings/window: The window.\n"
					+ "param items/name: An item.\n"
					+ "param steps/Trim low quality/limit: With spaces.\n"
					+ "param described: More about it.\n"
					+ "== parameters: ours\nparam outputFolder: Where results go.\nparam cutoff: Not used: the analysis has its own.\n" )
							.getBytes( StandardCharsets.UTF_8 ) );
			System.setProperty( McpPerspectiveSupport.GUIDES_PROPERTY, dir.getPath() );
			List<Map<String, Object>> properties = new java.util.ArrayList<Map<String, Object>>();
			properties.add( property( "cutoff", "Cutoff", "Cutoff" ) );
			properties.add( property( "settings/window", "Window", null ) );
			properties.add( property( "items/0/name", "Name", "" ) );
			properties.add( property( "steps/Trim low quality/limit", "Limit", "Limit" ) );
			properties.add( property( "described", "Described", "The analysis says what it is." ) );
			properties.add( property( "nested/outputFolder", "Output folder", "Output folder" ) );
			properties.add( property( "unknown", "Unknown", "Unknown" ) );
			McpGuides.describeParameters( McpGuides.analysis( "Site analysis" ), properties );
			assertEquals( "Scores below it are dropped; 0.05 is usual.", properties.get( 0 ).get( "description" ) );
			assertEquals( "The window.", properties.get( 1 ).get( "description" ) );
			assertEquals( "an array item, by its key without the index", "An item.", properties.get( 2 ).get( "description" ) );
			assertEquals( "With spaces.", properties.get( 3 ).get( "description" ) );
			assertEquals( "a declared description is kept", "The analysis says what it is.", properties.get( 4 ).get( "description" ) );
			assertEquals( "More about it.", properties.get( 4 ).get( "hint" ) );
			assertEquals( "by the last part of the key, from the common entries", "Where results go.", properties.get( 5 ).get( "description" ) );
			assertEquals( "Unknown", properties.get( 6 ).get( "description" ) );
			assertNull( properties.get( 0 ).get( "hint" ) );

			// without a guide of the analysis only the common entries apply
			List<Map<String, Object>> other = new java.util.ArrayList<Map<String, Object>>();
			other.add( property( "cutoff", "Cutoff", "Cutoff" ) );
			McpGuides.describeParameters( null, other );
			assertEquals( "Not used: the analysis has its own.", other.get( 0 ).get( "description" ) );
		}
		finally
		{
			System.clearProperty( McpPerspectiveSupport.GUIDES_PROPERTY );
			deleteAll( dir );
		}
	}

	/** The bundled guides describe parameters too. */
	public void testBundledParameters()
	{
		List<Map<String, Object>> properties = new java.util.ArrayList<Map<String, Object>>();
		properties.add( property( "topCount", "Top max count", "Top max count" ) );
		properties.add( property( "outputTable", "Output table", "Output table" ) );
		McpGuides.describeParameters( McpGuides.analysis( "Select top rows" ), properties );
		for ( Map<String, Object> property : properties )
			assertFalse( "described: " + property.get( "name" ),
					McpGuides.saysNothing( (String) property.get( "description" ), (String) property.get( "name" ), property.get( "displayName" ) ) );
	}

	private static Map<String, Object> property( String name, String displayName, String description )
	{
		Map<String, Object> property = new java.util.LinkedHashMap<String, Object>();
		property.put( "name", name );
		property.put( "displayName", displayName );
		if ( description != null )
			property.put( "description", description );
		return property;
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
					+ "== analysis: Site analysis\nsummary: Ours.\n== workflow: Common/Our flow || Other/Second flow\nsummary: A flow.\n== topic: site | Our topic\nsummary: Ours too.\nText.\n" ).getBytes( StandardCharsets.UTF_8 ) );
			System.setProperty( McpPerspectiveSupport.GUIDES_PROPERTY, dir.getPath() );
			assertEquals( "Replaced.", McpGuides.analysis( "Functional classification" ).getSummary() );
			assertEquals( "Ours.", McpGuides.analysis( "Site analysis" ).getSummary() );
			assertEquals( "Our topic", McpGuides.topic( "site" ).title );
			McpGuides.Guide flow = McpGuides.workflow( "Common/Our flow" );
			assertEquals( "A flow.", flow.getSummary() );
			assertSame( "aliases share one entry", flow, McpGuides.workflow( "Other/Second flow" ) );
			assertSame( "a workflow kept elsewhere is found by its name", flow, McpGuides.workflow( "data/Projects/Mine/Our flow" ) );
			assertNull( McpGuides.workflow( "Common/No such flow" ) );
			assertNotNull( "bundled entries stay", McpGuides.topic( "basics" ) );
		}
		finally
		{
			System.clearProperty( McpPerspectiveSupport.GUIDES_PROPERTY );
			deleteAll( dir );
		}
		assertEquals( bundled, McpGuides.analysis( "Functional classification" ).getSummary() );
		assertNull( McpGuides.analysis( "Site analysis" ) );
		assertNull( McpGuides.workflow( "Common/Our flow" ) );
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
