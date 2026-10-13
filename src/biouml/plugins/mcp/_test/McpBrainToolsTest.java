package biouml.plugins.mcp._test;

import java.io.File;
import java.util.Map;
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
import biouml.plugins.mcp.support.McpBrainSupport;
import biouml.plugins.mcp.support.McpDiagramSupport;
import biouml.plugins.mcp.support.McpEnvelope;

/**
 * Tests for the brain-model-generation MCP tools. Building a real brain diagram (regional/cellular/
 * receptor nodes + connectivity matrices) is out of scope for a unit test, so these exercise the
 * structural/guard paths: a plain (non-brain) diagram is rejected with a clean invalid_params (the
 * action's isApplicable check), and a bad path is not_found/path_escape. This runs the full code path
 * up to and including the applicability check without needing a brain fixture.
 */
public class McpBrainToolsTest extends AbstractBioUMLTest
{
	private File dir;
	private String diagramPath;

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		dir = TempFiles.dir( "mcpBrainTest" );
		Properties properties = new ExProperties();
		properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, "mcpg" );
		properties.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName() );
		properties.setProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, dir.toString() );
		ExProperties.store( properties, new File( dir, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE ) );
		Repository repository = (Repository) CollectionFactory.createCollection( null, properties );
		CollectionFactory.registerRoot( repository );
		repository.getNameList();

		DataCollection<?> folder = GenericDataCollection.createGenericCollection( repository, repository, "diagrams", "diagrams" );
		repository.put( folder );
		McpEnvelope created = McpDiagramSupport.create( "mcpg/diagrams", "d1", "math" );
		assertTrue( "create ok, got " + created.getCode() + " " + created.getError(), created.isOk() );
		diagramPath = (String) ( (Map<String, Object>) created.getData() ).get( "path" );
	}

	@Override
	protected void tearDown() throws Exception
	{
		super.tearDown();
		ApplicationUtils.removeDir( dir );
	}

	public void testNotABrainDiagram()
	{
		// A plain math diagram is not a brain diagram, so all three generators are rejected with a
		// clean invalid_params (the action's isApplicable returns false), not a throw.
		McpEnvelope eq = McpBrainSupport.generateEquations( diagramPath );
		assertFalse( "non-brain diagram must be rejected", eq.isOk() );
		assertEquals( McpConstants.CODE_INVALID_PARAMS, eq.getCode() );

		McpEnvelope comp = McpBrainSupport.generateCompositeDiagram( diagramPath );
		assertFalse( "non-brain diagram must be rejected", comp.isOk() );
		assertEquals( McpConstants.CODE_INVALID_PARAMS, comp.getCode() );

		McpEnvelope mm = McpBrainSupport.generateMultilevelModel( diagramPath );
		assertFalse( "non-brain diagram must be rejected", mm.isOk() );
		assertEquals( McpConstants.CODE_INVALID_PARAMS, mm.getCode() );
	}

	public void testBadPath()
	{
		// A path that does not start with a registered root is path_escape.
		McpEnvelope bad = McpBrainSupport.generateEquations( "does/not/exist" );
		assertFalse( bad.isOk() );
		assertEquals( McpConstants.CODE_PATH_ESCAPE, bad.getCode() );

		// A path that resolves to a non-diagram (the diagrams folder) is invalid_params.
		McpEnvelope notDiagram = McpBrainSupport.generateEquations( "mcpg/diagrams" );
		assertFalse( notDiagram.isOk() );
		assertEquals( McpConstants.CODE_INVALID_PARAMS, notDiagram.getCode() );
	}
}
