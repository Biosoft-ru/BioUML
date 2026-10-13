package biouml.plugins.mcp.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import biouml.plugins.mcp.McpConstants;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.access.TableRowsExporter;
import ru.biosoft.util.TextUtil2;

/**
 * Shared, test-friendly helpers for the table MCP tools.
 *
 * <p>These mirror the perform path of the web UI's table context-menu items ("Add row", "Subset
 * table", "Replace content"), each of which is a pure {@code TableDataCollection} /
 * {@code TableDataCollectionUtils} API call with no UI. All methods take/return plain JSON-
 * serializable values and never throw checked exceptions — failures are returned as
 * {@link McpEnvelope} error envelopes.</p>
 */
public final class McpTableSupport
{
	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger( McpTableSupport.class.getName() );

	private McpTableSupport()
	{
	}

	/**
	 * Resolve a repository path to a {@link TableDataCollection}.
	 * @return an envelope whose data is the table on success, or not_found/path_escape/invalid.
	 */
	private static McpEnvelope resolveTable( String path )
	{
		McpEnvelope resolved = McpRepositorySupport.resolve( path );
		if ( !resolved.isOk() )
			return resolved;
		DataElement de = (DataElement) resolved.getData();
		if ( !( de instanceof TableDataCollection ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "element is not a table: " + path );
		return McpEnvelope.ok( de );
	}

	/**
	 * Add a new row to a table — headless "Add row". The row is initialised with each column's
	 * default value (mirroring {@code AddRowTableAction}).
	 */
	public static McpEnvelope addRow( String path, String name )
	{
		if ( name == null || name.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "row name must be a non-empty string" );
		McpEnvelope resolved = resolveTable( path );
		if ( !resolved.isOk() )
			return resolved;
		TableDataCollection table = (TableDataCollection) resolved.getData();
		if ( !table.isMutable() )
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "table is read-only: " + path );
		if ( table.contains( name ) )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "a row already exists: " + name );
		try
		{
			Object[] defaults = table.columns().map( c -> c.getType().getDefaultValue() ).toArray();
			TableDataCollectionUtils.addRow( table, name, defaults, true );
			table.finalizeAddition();
			CollectionFactoryUtils.save( table );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "added", name );
			m.put( "path", path );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"could not add row '" + name + "': " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	/**
	 * Replace string content within a table's cells — headless "Replace content". Mirrors
	 * {@code ReplaceContentAction}: walks the rows (all, or the named subset when selectionOnly),
	 * and for each cell whose string form matches (whole-cell when exactMatch, else substring)
	 * re-converts it with the replacement.
	 */
	public static McpEnvelope replace( String path, String from, String to, boolean exactMatch, boolean selectionOnly, String[] rows )
	{
		if ( from == null || from.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "search string (from) must be non-empty" );
		if ( to == null )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "replacement string (to) must be provided" );
		if ( from.equals( to ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "replacement equals search string" );
		McpEnvelope resolved = resolveTable( path );
		if ( !resolved.isOk() )
			return resolved;
		TableDataCollection tdc = (TableDataCollection) resolved.getData();
		if ( !tdc.isMutable() )
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "table is read-only: " + path );
		try
		{
			List<DataType> types = tdc.columns().map( TableColumn::getType ).toList();
			java.util.Iterator<? extends DataElement> itemsIterator;
			if ( selectionOnly )
			{
				if ( rows == null || rows.length == 0 )
					return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "selectionOnly=true requires a non-empty rows list" );
				List<RowDataElement> subset = new ArrayList<RowDataElement>();
				for ( String rowName : rows )
				{
					DataElement de = CollectionFactory.getDataElement( path + "/" + rowName );
					if ( !( de instanceof RowDataElement ) )
						return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "row not found: " + rowName );
					subset.add( (RowDataElement) de );
				}
				itemsIterator = subset.iterator();
			}
			else
			{
				itemsIterator = tdc.iterator();
			}
			int changed = 0;
			while ( itemsIterator.hasNext() )
			{
				DataElement de = itemsIterator.next();
				if ( !( de instanceof RowDataElement ) )
					continue;
				RowDataElement row = (RowDataElement) de;
				Object[] values = row.getValues();
				boolean rowChanged = false;
				for ( int j = 0; j < values.length; j++ )
				{
					String value = TextUtil2.toString( values[ j ] );
					if ( exactMatch )
					{
						if ( value.equals( from ) )
						{
							values[ j ] = TextUtil2.fromString( types.get( j ).getType(), to );
							rowChanged = true;
						}
					}
					else
					{
						if ( value.contains( from ) )
						{
							value = value.replace( from, to );
							values[ j ] = TextUtil2.fromString( types.get( j ).getType(), value );
							rowChanged = true;
						}
					}
				}
				if ( rowChanged )
				{
					tdc.put( row );
					changed++;
				}
			}
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "changedRows", Integer.valueOf( changed ) );
			m.put( "path", path );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"replace failed: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	/**
	 * Create a subset (copy) of a table containing only the given rows — headless "Subset table".
	 * Mirrors {@code SubsetTableAction}: {@code TableRowsExporter.exportTable(destination, source,
	 * rows)} + copy persistent info + save.
	 */
	public static McpEnvelope subset( String path, String destinationPath, String[] rows )
	{
		if ( rows == null || rows.length == 0 )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "rows must be a non-empty list of row names" );
		McpEnvelope resolved = resolveTable( path );
		if ( !resolved.isOk() )
			return resolved;
		TableDataCollection source = (TableDataCollection) resolved.getData();
		DataElementPath destPath = DataElementPath.create( destinationPath );
		Object parentDe = CollectionFactory.getDataElement( destPath.getParentPath().toString() );
		if ( !( parentDe instanceof DataCollection ) )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "destination parent does not exist: " + destPath.getParentPath() );
		try
		{
			List<RowDataElement> rowsToExport = new ArrayList<RowDataElement>();
			for ( String rowName : rows )
			{
				DataElement de = CollectionFactory.getDataElement( path + "/" + rowName );
				if ( !( de instanceof RowDataElement ) )
					return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "row not found: " + rowName );
				rowsToExport.add( (RowDataElement) de );
			}
			// exportTable itself creates the new table, copies columns/rows, calls finalizeAddition,
			// copies persistent info, and saves (it handles a null job control).
			TableRowsExporter.exportTable( destPath, source, rowsToExport, null );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "created", destinationPath );
			m.put( "rowCount", Integer.valueOf( rowsToExport.size() ) );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"subset failed: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}
}
