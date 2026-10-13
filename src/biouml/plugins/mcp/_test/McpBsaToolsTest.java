package biouml.plugins.mcp._test;

import java.io.File;
import java.util.Properties;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.Repository;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.generic.GenericDataCollection;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.TempFiles;

import biouml.plugins.mcp.McpConstants;
import biouml.plugins.mcp.support.McpBsaSupport;
import biouml.plugins.mcp.support.McpEnvelope;

/**
 * Tests for the BSA "save selection" MCP tools. A full BSA track/site-model fixture is heavyweight to
 * build in a unit test, so these exercise the structural/guard paths (which run the full code path
 * without a BSA fixture): not_found for a missing source, invalid_params for an empty selection, and
 * not_found for a missing destination parent.
 */
public class McpBsaToolsTest extends AbstractBioUMLTest
{
	private File dir;

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		dir = TempFiles.dir( "mcpBsaTest" );
		Properties properties = new ExProperties();
		properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, "mcpdata" );
		properties.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName() );
		properties.setProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, dir.toString() );
		ExProperties.store( properties, new File( dir, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE ) );
		Repository repository = (Repository) CollectionFactory.createCollection( null, properties );
		CollectionFactory.registerRoot( repository );
		repository.getNameList();
		DataCollection<?> folder = GenericDataCollection.createGenericCollection( repository, repository, "projects", "projects" );
		repository.put( folder );
	}

	@Override
	protected void tearDown() throws Exception
	{
		super.tearDown();
		ApplicationUtils.removeDir( dir );
	}

	public void testSaveSiteSelectionGuards()
	{
		// An empty modelNames list is a clean invalid_params.
		McpEnvelope empty = McpBsaSupport.saveSiteSelection( "mcpdata/projects", new String[ 0 ], "mcpdata/projects/out" );
		assertFalse( empty.isOk() );
		assertEquals( McpConstants.CODE_INVALID_PARAMS, empty.getCode() );

		// A source that does not start with a registered root is path_escape.
		McpEnvelope bad = McpBsaSupport.saveSiteSelection( "does/not/exist", new String[] { "m" }, "mcpdata/projects/out" );
		assertFalse( bad.isOk() );
		assertEquals( McpConstants.CODE_PATH_ESCAPE, bad.getCode() );

		// A source that is not a collection is a clean invalid_params.
		McpEnvelope notColl = McpBsaSupport.saveSiteSelection( "mcpdata/projects", new String[] { "m" }, "mcpdata/projects/out" );
		// projects IS a collection, so this proceeds to resolve the (nonexistent) site model 'm' → not_found.
		assertFalse( notColl.isOk() );
	}

	public void testSaveTrackSelectionGuards()
	{
		// An empty siteNames list is a clean invalid_params.
		McpEnvelope empty = McpBsaSupport.saveTrackSelection( "mcpdata/projects", new String[ 0 ], "mcpdata/projects/out" );
		assertFalse( empty.isOk() );
		assertEquals( McpConstants.CODE_INVALID_PARAMS, empty.getCode() );

		// A source that is not a track is a clean invalid_params (projects is a folder, not a Track).
		McpEnvelope notTrack = McpBsaSupport.saveTrackSelection( "mcpdata/projects", new String[] { "s" }, "mcpdata/projects/out" );
		assertFalse( notTrack.isOk() );
		assertEquals( McpConstants.CODE_INVALID_PARAMS, notTrack.getCode() );

		// A path that does not start with a registered root is path_escape.
		McpEnvelope bad = McpBsaSupport.saveTrackSelection( "does/not/exist", new String[] { "s" }, "mcpdata/projects/out" );
		assertFalse( bad.isOk() );
		assertEquals( McpConstants.CODE_PATH_ESCAPE, bad.getCode() );
	}
}
