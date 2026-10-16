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
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.access.SequencesDatabaseInfo;
import ru.biosoft.bsa.access.SequencesDatabaseInfoSelector;
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

	/**
	 * Report or set the genome (build id + sequences collection) of a track — the headless equivalent of
	 * the "Genome (sequences collection)" field in the track's properties. Analyses that convert between
	 * builds (e.g. a liftover to hg38) decide from it which build the positions are in.
	 *
	 * @param path   full repository path of the track
	 * @param genome null/empty to only report; otherwise a genome build id or alias ("hg19", "GRCh37",
	 *               "hg38", ...), or the path of a sequences collection
	 * @return {@code {path, genomeId, sequences}}; when set, also {@code changed: true}; plus the installed genomes
	 *         ({@code availableGenomes}) when the track has no sequences collection.
	 */
	public static McpEnvelope trackGenome( String path, String genome )
	{
		return trackGenome( path, genome, false );
	}

	/** Like {@link #trackGenome(String, String)}; {@code listGenomes} lists the installed genomes in any case. */
	public static McpEnvelope trackGenome( String path, String genome, boolean listGenomes )
	{
		McpEnvelope resolved = McpRepositorySupport.resolve( path );
		if ( !resolved.isOk() )
			return resolved;
		if ( !( resolved.getData() instanceof Track ) || !( resolved.getData() instanceof DataCollection ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "not a track: " + path );
		Track track = (Track) resolved.getData();
		boolean changed = false;
		if ( genome != null && !genome.trim().isEmpty() )
		{
			genome = genome.trim();
			SequencesDatabaseInfo db;
			String genomeId;
			if ( genome.contains( "/" ) )
			{
				DataElementPath sequences = DataElementPath.create( genome );
				if ( sequences.optDataCollection() == null )
					return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no sequences collection at: " + genome );
				db = SequencesDatabaseInfoSelector.getDatabase( sequences );
				genomeId = db == SequencesDatabaseInfo.CUSTOM_SEQUENCES ? null : db.getGenomeBuild();
				setTrackGenome( track, sequences, genomeId );
			}
			else
			{
				genomeId = TrackUtils.normalizeGenomeBuild( genome );
				if ( genomeId == null )
					genomeId = genome;
				db = SequencesDatabaseInfoSelector.getDatabaseByGenomeBuild( genomeId );
				setTrackGenome( track, db == null ? null : db.getChromosomePath(), genomeId );
			}
			changed = true;
			// The loaded track keeps a genome selector built from the old properties: drop it from the
			// collection cache so the next read builds a fresh one.
			DataCollection<?> origin = track.getOrigin();
			if ( origin != null )
			{
				try
				{
					origin.release( track.getName() );
				}
				catch ( Exception e )
				{
					log.log( java.util.logging.Level.WARNING, "could not release " + path + " from cache", e );
				}
				DataElement reloaded = DataElementPath.create( path ).optDataElement();
				if ( reloaded instanceof Track )
					track = (Track) reloaded;
			}
		}
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "path", path );
		m.put( "genomeId", TrackUtils.getGenomeId( track ) );
		String sequences = ( (DataCollection<?>) track ).getInfo().getProperty( Track.SEQUENCES_COLLECTION_PROPERTY );
		m.put( "sequences", sequences );
		if ( changed )
			m.put( "changed", Boolean.TRUE );
		// the list (some 40 genomes) helps to choose one; a track that has its sequences does not need it
		if ( !listGenomes && sequences != null && !sequences.isEmpty() )
			return McpEnvelope.ok( m );
		List<Map<String, Object>> available = new ArrayList<Map<String, Object>>();
		for ( SequencesDatabaseInfo db : SequencesDatabaseInfoSelector.getDatabases() )
		{
			if ( db == SequencesDatabaseInfo.CUSTOM_SEQUENCES || db == SequencesDatabaseInfo.NULL_SEQUENCES
					|| db.getBasePath().optDataCollection() == null )
				continue;
			Map<String, Object> g = new LinkedHashMap<String, Object>();
			g.put( "genomeId", db.getGenomeBuild() );
			g.put( "name", db.getName() );
			g.put( "sequences", db.getChromosomePath().toString() );
			available.add( g );
		}
		m.put( "availableGenomes", available );
		return McpEnvelope.ok( m );
	}

	private static void setTrackGenome( Track track, DataElementPath sequences, String genomeId )
	{
		java.util.Properties properties = ( (DataCollection<?>) track ).getInfo().getProperties();
		if ( sequences != null )
			properties.setProperty( Track.SEQUENCES_COLLECTION_PROPERTY, sequences.toString() );
		else
			properties.remove( Track.SEQUENCES_COLLECTION_PROPERTY );
		if ( genomeId != null )
			properties.setProperty( Track.GENOME_ID_PROPERTY, genomeId );
		else
			properties.remove( Track.GENOME_ID_PROPERTY );
		ru.biosoft.access.CollectionFactoryUtils.save( track );
	}
}
