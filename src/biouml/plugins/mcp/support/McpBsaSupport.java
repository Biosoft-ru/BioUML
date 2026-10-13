package biouml.plugins.mcp.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import biouml.plugins.mcp.McpConstants;

import ru.biosoft.access.core.CloneableDataElement;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.transformer.SiteModelTransformer;

/**
 * Shared, test-friendly helpers for the BSA (biosequence) MCP tools.
 *
 * <p>These mirror the perform path of the web UI's BSA "save selection" context-menu items
 * (saving the selected sites/site-models to a new collection or track), each of which is a pure
 * repository API call with no UI. All methods take/return plain JSON-serializable values and never
 * throw checked exceptions — failures are returned as {@link McpEnvelope} error envelopes.</p>
 */
public final class McpBsaSupport
{
	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger( McpBsaSupport.class.getName() );

	private McpBsaSupport()
	{
	}

	/**
	 * Save the selected site models into a new {@code SiteModelCollection} — the headless equivalent
	 * of the web UI's "Save selection" menu item ({@code SubsetCollectionAction}):
	 * {@code SiteModelTransformer.createCollection(path)} + clone each selected model in + save.
	 *
	 * @param sourcePath  full repository path of the collection containing the site models
	 * @param modelNames  the names of the site models to save (resolved inside {@code sourcePath})
	 * @param destinationPath full path of the new collection to create (its parent must exist)
	 * @return an envelope whose data is {@code {created, count}} on success.
	 */
	public static McpEnvelope saveSiteSelection( String sourcePath, String[] modelNames, String destinationPath )
	{
		if ( modelNames == null || modelNames.length == 0 )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "modelNames must be a non-empty list" );
		McpEnvelope resolved = McpRepositorySupport.resolve( sourcePath );
		if ( !resolved.isOk() )
			return resolved;
		Object srcObj = resolved.getData();
		if ( !( srcObj instanceof DataCollection ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "source is not a collection: " + sourcePath );
		DataElementPath destPath = DataElementPath.create( destinationPath );
		Object parentDe = CollectionFactory.getDataElement( destPath.getParentPath().toString() );
		if ( !( parentDe instanceof DataCollection ) )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "destination parent does not exist: " + destPath.getParentPath() );
		try
		{
			ru.biosoft.bsa.SiteModelCollection sc = SiteModelTransformer.createCollection( destPath );
			@SuppressWarnings( "rawtypes" )
			DataCollection rawDc = sc;
			int saved = 0;
			for ( String name : modelNames )
			{
				DataElement de = CollectionFactory.getDataElement( sourcePath + "/" + name );
				if ( de == null )
					return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "site model not found: " + name );
				if ( de instanceof CloneableDataElement )
				{
					rawDc.put( ( (CloneableDataElement) de ).clone( rawDc, de.getName() ) );
					saved++;
				}
			}
			destPath.save( sc );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "created", destinationPath );
			m.put( "count", Integer.valueOf( saved ) );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"could not save site selection: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	/**
	 * Save the selected sites into a new {@link SqlTrack} — the headless equivalent of the web UI's
	 * "Save selection as track" menu item ({@code SubsetTrackAction}):
	 * {@code SqlTrack.createTrack(path, source, source.getClass())} + add each selected site +
	 * {@code finalizeAddition} + save.
	 *
	 * @param sourcePath  full repository path of the source {@link Track}
	 * @param siteNames   the names of the sites to save (resolved inside the source track's sites)
	 * @param destinationPath full path of the new track to create (its parent must exist)
	 * @return an envelope whose data is {@code {created, count}} on success.
	 */
	public static McpEnvelope saveTrackSelection( String sourcePath, String[] siteNames, String destinationPath )
	{
		if ( siteNames == null || siteNames.length == 0 )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "siteNames must be a non-empty list" );
		McpEnvelope resolved = McpRepositorySupport.resolve( sourcePath );
		if ( !resolved.isOk() )
			return resolved;
		Object srcObj = resolved.getData();
		if ( !( srcObj instanceof Track ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "source is not a track: " + sourcePath );
		Track source = (Track) srcObj;
		DataElementPath destPath = DataElementPath.create( destinationPath );
		Object parentDe = CollectionFactory.getDataElement( destPath.getParentPath().toString() );
		if ( !( parentDe instanceof DataCollection ) )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "destination parent does not exist: " + destPath.getParentPath() );
		try
		{
			SqlTrack result = SqlTrack.createTrack( destPath, source, source.getClass() );
			DataCollection<Site> allSites = source.getAllSites();
			int saved = 0;
			for ( String name : siteNames )
			{
				Site site = allSites.get( name );
				if ( site == null )
					return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "site not found: " + name );
				result.addSite( site );
				saved++;
			}
			result.finalizeAddition();
			destPath.save( result );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "created", destinationPath );
			m.put( "count", Integer.valueOf( saved ) );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"could not save track selection: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}
}
