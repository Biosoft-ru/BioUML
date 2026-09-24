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
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.generic.GenericDataCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.TempFiles;

import biouml.plugins.mcp.McpConstants;
import biouml.plugins.mcp.support.McpEnvelope;
import biouml.plugins.mcp.support.McpTableSupport;

/**
 * Tests for the table MCP tools (add-row, replace, subset), against a temp-dir {@link LocalRepository}
 * fixture that holds a real {@link TableDataCollection}.
 */
public class McpTableToolsTest extends AbstractBioUMLTest
{
	private File dir;
	private static final String TABLE = "mcpdata/projects/mytable";

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		dir = TempFiles.dir( "mcpTableTest" );

		Properties properties = new ExProperties();
		properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, "mcpdata" );
		properties.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName() );
		properties.setProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, dir.toString() );
		ExProperties.store( properties, new File( dir, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE ) );
		Repository repository = (Repository) CollectionFactory.createCollection( null, properties );
		CollectionFactory.registerRoot( repository );
		repository.getNameList();

		// A folder collection to hold the table.
		DataCollection<?> folder = GenericDataCollection.createGenericCollection( repository, repository, "projects", "projects" );
		repository.put( folder );

		// A real table with 3 string rows, registered in the repository so it is findable by path.
		TableDataCollection tdc = TableDataCollectionUtils.createTableDataCollection( folder, "mytable" );
		tdc.getColumnModel().addColumn( "label", String.class );
		for ( String r : new String[] { "alpha", "beta", "gamma" } )
			TableDataCollectionUtils.addRow( tdc, r, new Object[] { r + " label" }, true );
		tdc.finalizeAddition();
		@SuppressWarnings( "unchecked" )
		DataCollection<ru.biosoft.access.core.DataElement> rawFolder = (DataCollection<ru.biosoft.access.core.DataElement>) folder;
		rawFolder.put( tdc );
	}

	@Override
	protected void tearDown() throws Exception
	{
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

	public void testAddRow()
	{
		McpEnvelope env = data( McpTableSupport.addRow( TABLE, "delta" ) );
		Map<String, Object> m = (Map<String, Object>) env.getData();
		assertEquals( "delta", m.get( "added" ) );

		// The new row exists and was initialised with the column default (null/empty for a string).
		assertNotNull( "new row exists", CollectionFactory.getDataElement( TABLE + "/delta" ) );

		// A duplicate row name is a clean not_found.
		McpEnvelope dup = McpTableSupport.addRow( TABLE, "delta" );
		assertFalse( dup.isOk() );
		assertEquals( McpConstants.CODE_NOT_FOUND, dup.getCode() );

		// A non-table path is a clean invalid_params.
		McpEnvelope notTable = McpTableSupport.addRow( "mcpdata/projects", "x" );
		assertFalse( notTable.isOk() );
		assertEquals( McpConstants.CODE_INVALID_PARAMS, notTable.getCode() );
	}

	public void testReplace()
	{
		// Substring replace over the whole table: "a" appears in alpha/beta/gamma labels.
		McpEnvelope env = data( McpTableSupport.replace( TABLE, "label", "tag", false, false, null ) );
		Map<String, Object> m = (Map<String, Object>) env.getData();
		assertTrue( "at least one row changed", ( (Integer) m.get( "changedRows" ) ).intValue() > 0 );

		// from == to is a clean invalid_params.
		McpEnvelope same = McpTableSupport.replace( TABLE, "x", "x", false, false, null );
		assertFalse( same.isOk() );
		assertEquals( McpConstants.CODE_INVALID_PARAMS, same.getCode() );

		// selectionOnly without rows is a clean invalid_params.
		McpEnvelope noRows = McpTableSupport.replace( TABLE, "a", "b", false, true, null );
		assertFalse( noRows.isOk() );
		assertEquals( McpConstants.CODE_INVALID_PARAMS, noRows.getCode() );
	}

	public void testSubset()
	{
		McpEnvelope env = data( McpTableSupport.subset( TABLE, "mcpdata/projects/mytable subset", new String[] { "alpha", "beta" } ) );
		Map<String, Object> m = (Map<String, Object>) env.getData();
		assertEquals( "mcpdata/projects/mytable subset", m.get( "created" ) );
		assertEquals( Integer.valueOf( 2 ), m.get( "rowCount" ) );
		assertNotNull( "subset table exists", CollectionFactory.getDataElement( "mcpdata/projects/mytable subset" ) );

		// An unknown row is a clean not_found.
		McpEnvelope badRow = McpTableSupport.subset( TABLE, "mcpdata/projects/other subset", new String[] { "nonexistent" } );
		assertFalse( badRow.isOk() );
		assertEquals( McpConstants.CODE_NOT_FOUND, badRow.getCode() );

		// An empty rows list is a clean invalid_params.
		McpEnvelope empty = McpTableSupport.subset( TABLE, "mcpdata/projects/other subset", new String[ 0 ] );
		assertFalse( empty.isOk() );
		assertEquals( McpConstants.CODE_INVALID_PARAMS, empty.getCode() );
	}
}
