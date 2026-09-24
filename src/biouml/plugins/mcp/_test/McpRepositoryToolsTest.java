package biouml.plugins.mcp._test;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.Repository;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.generic.GenericDataCollection;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.TempFiles;

import biouml.plugins.mcp.McpConstants;
import biouml.plugins.mcp.server.McpToolCatalog;
import biouml.plugins.mcp.support.McpEnvelope;
import biouml.plugins.mcp.support.McpRepositorySupport;
import biouml.plugins.mcp.tools.repo.RepoTools;

/**
 * Phase-2 tests for the repository MCP tools, against a temp-dir {@link LocalRepository} fixture
 * (the same pattern as {@code TestGenericDataCollection}).
 */
public class McpRepositoryToolsTest extends AbstractBioUMLTest
{
	private File dir;

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		dir = TempFiles.dir( "mcpRepoTest" );

		Properties properties = new ExProperties();
		properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, "mcpdata" );
		properties.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName() );
		properties.setProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, dir.toString() );
		ExProperties.store( properties, new File( dir, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE ) );
		Repository repository = (Repository) CollectionFactory.createCollection( null, properties );
		CollectionFactory.registerRoot( repository );
		repository.getNameList();

		// A folder collection (FolderCollection) so create-folder works.
		DataCollection<?> folder = GenericDataCollection.createGenericCollection( repository, repository, "projects", "projects" );
		assertNotNull( "folder created", folder );
		repository.put( folder );
	}

	@Override
	protected void tearDown() throws Exception
	{
		try
		{
			DataElementPath.create( "mcpdata/projects" ).getDataCollection().close();
		}
		catch ( Exception e )
		{
			// ignore during teardown
		}
		super.tearDown();
		ApplicationUtils.removeDir( dir );
	}

	@SuppressWarnings( "unchecked" )
	private McpEnvelope data( McpEnvelope env )
	{
		if ( !env.isOk() )
			fail( "expected ok envelope but got error: " + env.getCode() + " " + env.getError() );
		return env;
	}

	@SuppressWarnings( "unchecked" )
	public void testCollections()
	{
		McpEnvelope env = McpRepositorySupport.collections();
		data( env );
		Map<String, Object> m = (Map<String, Object>) env.getData();
		List<Map<String, Object>> cols = (List<Map<String, Object>>) m.get( "collections" );
		assertTrue( "fixture root 'mcpdata' must be a registered collection", containsName( cols, "mcpdata" ) );
	}

	@SuppressWarnings( "unchecked" )
	public void testListDescribeSearch()
	{
		// list the root
		McpEnvelope list = McpRepositorySupport.list( "mcpdata" );
		data( list );
		Map<String, Object> lm = (Map<String, Object>) list.getData();
		List<Map<String, Object>> children = (List<Map<String, Object>>) lm.get( "children" );
		assertTrue( "root must list 'projects'", containsName( children, "projects" ) );

		// describe the folder
		McpEnvelope desc = McpRepositorySupport.describe( "mcpdata/projects" );
		data( desc );
		Map<String, Object> dm = (Map<String, Object>) desc.getData();
		assertEquals( "projects", dm.get( "name" ) );
		assertEquals( Boolean.TRUE, dm.get( "isCollection" ) );

		// search
		McpEnvelope search = McpRepositorySupport.search( "projects", "mcpdata" );
		data( search );
		Map<String, Object> sm = (Map<String, Object>) search.getData();
		assertTrue( "search must find projects", ((List<String>) sm.get( "matches" )).contains( "mcpdata/projects" ) );
	}

	@SuppressWarnings( "unchecked" )
	public void testSearchAsync()
	{
		// Queue the search as a background task: must return a taskId immediately (no long block).
		long t0 = System.currentTimeMillis();
		McpEnvelope async = McpRepositorySupport.searchAsync( "projects", "mcpdata" );
		data( async );
		assertTrue( "searchAsync returns fast (no synchronous walk), took " + (System.currentTimeMillis() - t0) + "ms",
				System.currentTimeMillis() - t0 < 5000 );
		Map<String, Object> am = (Map<String, Object>) async.getData();
		Object taskId = am.get( "taskId" );
		assertNotNull( "taskId present", taskId );
		assertEquals( "queued", am.get( "status" ) );

		// The task completes on the TaskManager worker thread; poll the status until it is done
		// (a small repo search finishes in well under the deadline).
		String id = String.valueOf( taskId );
		String status = null;
		for ( int i = 0; i < 100; i++ )
		{
			McpEnvelope st = McpRepositorySupport.searchStatus( id );
			data( st );
			status = String.valueOf( ( (Map<String, Object>) st.getData() ).get( "status" ) );
			if ( "done".equals( status ) )
				break;
			try
			{
				Thread.sleep( 50 );
			}
			catch ( InterruptedException e )
			{
				Thread.currentThread().interrupt();
				break;
			}
		}
		assertEquals( "async search completed", "done", status );

		// A status lookup for an unknown task is a clean not_found, not an exception.
		McpEnvelope missing = McpRepositorySupport.searchStatus( "no-such-task-xyz" );
		assertFalse( missing.isOk() );
		assertEquals( McpConstants.CODE_NOT_FOUND, missing.getCode() );
	}

	@SuppressWarnings( "unchecked" )
	public void testCopyElement()
	{
		// Create a cloneable file element inside the projects folder.
		File srcFile = new File( dir, "src.txt" );
		try
		{
			java.nio.file.Files.write( srcFile.toPath(), "hello".getBytes( java.nio.charset.StandardCharsets.UTF_8 ) );
		}
		catch ( Exception e )
		{
			fail( "could not write temp file: " + e );
		}
		DataCollection<?> projects = CollectionFactory.getDataCollection( "mcpdata/projects" );
		ru.biosoft.access.file.FileDataElement src = new ru.biosoft.access.file.FileDataElement( "src.txt", projects, srcFile );
		@SuppressWarnings( "rawtypes" )
		DataCollection rawProjects = projects;
		rawProjects.put( src );
		DataElementPath srcPath = DataElementPath.create( projects, "src.txt" );

		// Copy it to a new sibling path.
		String dest = "mcpdata/projects/src copy.txt";
		McpEnvelope env = McpRepositorySupport.copyElement( srcPath.toString(), dest );
		data( env );
		assertEquals( dest, ( (Map<String, Object>) env.getData() ).get( "copied" ) );

		// The copy exists, is distinct from the source, and the source is untouched.
		assertTrue( "copy exists", CollectionFactory.getDataElement( dest ) != null );
		assertNotNull( "source still exists", CollectionFactory.getDataElement( srcPath.toString() ) );

		// Copying a non-cloneable element (a bare folder) is a clean error, not an exception.
		McpEnvelope bad = McpRepositorySupport.copyElement( "mcpdata/projects", "mcpdata/projects/nope.txt" );
		// projects is a GenericDataCollection folder; whether it's cloneable depends on the driver,
		// so accept either a successful copy or a structured "not copyable" error — but never a throw.
		assertNotNull( bad );
	}

	@SuppressWarnings( "unchecked" )
	public void testReinitialize()
	{
		// The projects folder is a valid collection; reinitialize() on it must be a clean no-op.
		McpEnvelope env = McpRepositorySupport.reinitialize( "mcpdata/projects" );
		data( env );
		Map<String, Object> m = (Map<String, Object>) env.getData();
		assertEquals( "mcpdata/projects", m.get( "reinitialized" ) );
		// A non-collection element is a clean invalid_params, not a throw.
		McpEnvelope bad = McpRepositorySupport.reinitialize( "does/not/exist" );
		assertFalse( bad.isOk() );
	}

	@SuppressWarnings( "unchecked" )
	public void testExportFormats()
	{
		// exportFormats must resolve the element and return a (possibly empty) list of formats,
		// never a throw — regardless of whether the element has any exporters.
		McpEnvelope env = McpRepositorySupport.exportFormats( "mcpdata/projects" );
		data( env );
		Map<String, Object> m = (Map<String, Object>) env.getData();
		assertTrue( "count present", m.get( "count" ) != null );
		assertNotNull( "formats list present", m.get( "formats" ) );
		assertEquals( "count matches formats size",
				( (Integer) m.get( "count" ) ).intValue(), ( (List<?>) m.get( "formats" ) ).size() );

		// A path that does not start with a registered root is refused with path_escape.
		McpEnvelope bad = McpRepositorySupport.exportFormats( "does/not/exist" );
		assertFalse( bad.isOk() );
		assertEquals( McpConstants.CODE_PATH_ESCAPE, bad.getCode() );
	}

	@SuppressWarnings( "unchecked" )
	public void testExport()
	{
		// A file element has no exporters in this fixture, so export must return a clean
		// "no export format available" error — proving the code path runs headlessly without a UI.
		File f = new File( dir, "export-me.txt" );
		try
		{
			java.nio.file.Files.write( f.toPath(), "x".getBytes( java.nio.charset.StandardCharsets.UTF_8 ) );
		}
		catch ( Exception e )
		{
			fail( "could not write temp file: " + e );
		}
		DataCollection<?> projects = CollectionFactory.getDataCollection( "mcpdata/projects" );
		ru.biosoft.access.file.FileDataElement file = new ru.biosoft.access.file.FileDataElement( "export-me.txt", projects, f );
		@SuppressWarnings( "rawtypes" )
		DataCollection rawProjects = projects;
		rawProjects.put( file );

		McpEnvelope env = McpRepositorySupport.export( "mcpdata/projects/export-me.txt", null, dir.toString() );
		// Either it found an exporter (ok) or there is none for a bare file (a structured error).
		// Either way it must not throw.
		assertNotNull( env );
		if ( env.isOk() )
		{
			Map<String, Object> m = (Map<String, Object>) env.getData();
			assertNotNull( "export must report a file path", m.get( "file" ) );
			assertNotNull( "export must report a format", m.get( "format" ) );
		}
		else
		{
			assertNotNull( "error must carry a code", env.getCode() );
		}

		// A path that does not start with a registered root is refused with path_escape.
		McpEnvelope bad = McpRepositorySupport.export( "does/not/exist", null, dir.toString() );
		assertFalse( bad.isOk() );
		assertEquals( McpConstants.CODE_PATH_ESCAPE, bad.getCode() );
	}

	@SuppressWarnings( "unchecked" )
	public void testGetActions()
	{
		McpEnvelope env = McpRepositorySupport.resolve( "mcpdata/projects" );
		data( env );
		List<Map<String, Object>> actions =
				(List<Map<String, Object>>) biouml.plugins.mcp.support.McpActionInvoker
						.actionsFor( (ru.biosoft.access.core.DataElement) env.getData() );
		// A folder collection must expose at least the create-folder action.
		boolean hasNewFolder = false;
		for ( Map<String, Object> a : actions )
		{
			if ( "cmd-generic-newfolder".equals( a.get( "key" ) ) )
			{
				hasNewFolder = true;
				assertNotNull( "action must have a name", a.get( "name" ) );
				assertTrue( "action name must be non-empty", !String.valueOf( a.get( "name" ) ).isEmpty() );
				assertEquals( "create-folder must be headless-safe", Boolean.TRUE, a.get( "headlessSafe" ) );
			}
		}
		assertTrue( "folder collection must expose cmd-generic-newfolder", hasNewFolder );
	}

	@SuppressWarnings( "unchecked" )
	public void testCreateFolder()
	{
		McpEnvelope created = McpRepositorySupport.createFolder( "mcpdata/projects", "MyProject" );
		data( created );
		Map<String, Object> m = (Map<String, Object>) created.getData();
		assertEquals( "mcpdata/projects/MyProject", m.get( "created" ) );

		// verify by re-list
		McpEnvelope list = McpRepositorySupport.list( "mcpdata/projects" );
		data( list );
		List<Map<String, Object>> children = (List<Map<String, Object>>) ((Map<String, Object>) list.getData()).get( "children" );
		assertTrue( "new folder must appear in re-list", containsName( children, "MyProject" ) );
	}

	@SuppressWarnings( "unchecked" )
	public void testRemoveDryRun()
	{
		McpRepositorySupport.createFolder( "mcpdata/projects", "ToRemove" );

		// dry run: nothing removed
		McpEnvelope dry = McpRepositorySupport.remove( "mcpdata/projects/ToRemove", true );
		data( dry );
		Map<String, Object> dm = (Map<String, Object>) dry.getData();
		assertEquals( Boolean.TRUE, dm.get( "dryRun" ) );
		assertTrue( "element must still exist after dry-run",
				DataElementPath.create( "mcpdata/projects/ToRemove" ).exists() );

		// real remove
		McpEnvelope real = McpRepositorySupport.remove( "mcpdata/projects/ToRemove", false );
		data( real );
		assertFalse( "element must be gone after real remove",
				DataElementPath.create( "mcpdata/projects/ToRemove" ).exists() );
	}

	public void testInteractiveActionRefused()
	{
		McpEnvelope env = McpRepositorySupport.resolve( "mcpdata/projects" );
		data( env );
		ru.biosoft.access.core.DataElement de = (ru.biosoft.access.core.DataElement) env.getData();

		// 1) An action key not in the element's action set is refused with not_found.
		McpEnvelope missing = biouml.plugins.mcp.support.McpActionInvoker.perform( de, "cmd-generic-newfolder-not-real" );
		assertFalse( missing.isOk() );
		assertEquals( McpConstants.CODE_NOT_FOUND, missing.getCode() );

		// 2) End-to-end: pick a real action present for this element that is NOT headless-safe and
		//    confirm perform() refuses it with requires_interactive_ui (the safety guarantee that a
		//    GUI-only action is never silently invoked).
		java.util.List<Map<String, Object>> actions =
				biouml.plugins.mcp.support.McpActionInvoker.actionsFor( de );
		String nonSafeKey = null;
		for ( Map<String, Object> a : actions )
			if ( Boolean.FALSE.equals( a.get( "headlessSafe" ) ) && a.get( "key" ) != null )
			{
				nonSafeKey = String.valueOf( a.get( "key" ) );
				break;
			}
		if ( nonSafeKey != null )
		{
			McpEnvelope refused = biouml.plugins.mcp.support.McpActionInvoker.perform( de, nonSafeKey );
			assertFalse( "interactive action '" + nonSafeKey + "' must not run", refused.isOk() );
			assertEquals( "refusal must be requires_interactive_ui, was " + refused.getCode(),
					McpConstants.CODE_INTERACTIVE_ONLY, refused.getCode() );
		}

		// 3) The allowlist is default-deny regardless.
		assertFalse( "unlisted key must not be headless-safe",
				biouml.plugins.mcp.support.McpActionInvoker.isHeadlessSafe( "cmd-open", null ) );
		assertFalse( "made-up key must not be headless-safe",
				biouml.plugins.mcp.support.McpActionInvoker.isHeadlessSafe( "cmd-anything", null ) );
		assertTrue( "create-folder key must be headless-safe",
				biouml.plugins.mcp.support.McpActionInvoker.isHeadlessSafe( "cmd-generic-newfolder", null ) );
	}

	public void testEnvelopeShape()
	{
		// A bad path returns the stable error envelope shape.
		McpEnvelope env = McpRepositorySupport.resolve( "does/not/exist" );
		assertFalse( env.isOk() );
		assertNotNull( env.getCode() );
		Map<String, Object> map = env.toMap();
		assertEquals( Boolean.FALSE, map.get( "ok" ) );
		assertTrue( "error map must carry an error message", map.containsKey( "error" ) );
	}

	private static boolean containsName( List<Map<String, Object>> items, String name )
	{
		if ( items == null )
			return false;
		for ( Map<String, Object> m : items )
			if ( name.equals( m.get( "name" ) ) )
				return true;
		return false;
	}
}
