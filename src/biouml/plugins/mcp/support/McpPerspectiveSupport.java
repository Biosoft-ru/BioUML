package biouml.plugins.mcp.support;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.plugins.mcp.McpConstants;
import biouml.workbench.perspective.PerspectiveRegistry;
import ru.biosoft.server.servlets.webservices.providers.PerspectivesProvider;

/**
 * Perspectives — the task-oriented entry points users pick in the web UI (Default, GTRD, Microbiome, ...) —
 * exposed as MCP tools.
 *
 * <p>A perspective selected through MCP applies to the MCP session only: it is selected via the platform's
 * {@code perspective} provider with {@code scope=session}, which keeps it in the session cache instead of the
 * user's perspective preference, so the web UI keeps the user's own choice. The perspective in effect decides
 * which import/export formats the platform offers (its importer/exporter rules), so selecting one changes what
 * {@code biouml_repo_import_formats} and {@code biouml_repo_export_formats} return.</p>
 *
 * <p>When the web UI files are deployed next to the server, the perspective's intro page is read too
 * (see {@link McpIntroPages}): it supplies a description for perspectives that declare none and, on selection,
 * what the page offers users (links, analysis parameter presets, import folders, help links).</p>
 */
public final class McpPerspectiveSupport
{
	private static final Logger log = Logger.getLogger( McpPerspectiveSupport.class.getName() );

	/** Keys of the perspective JSON that matter to an agent; the rest (viewparts, template, ...) is UI-only. */
	private static final String[] SUMMARY_KEYS = { "name", "description", "repository", "entryPoints" };

	/** Keys of the perspective JSON holding allow/deny rules that affect other MCP tools. */
	private static final String[] RULE_KEYS = { "importers", "exporters", "actions" };

	private static final String SELECT_NOTE = "Selected for this MCP session only; the user's saved perspective (used by the web UI) is unchanged. "
			+ "biouml_repo_import_formats, biouml_repo_export_formats and import autodetection now follow this perspective's importer/exporter rules.";

	private McpPerspectiveSupport()
	{
	}

	/**
	 * List the perspectives the current user may open and the one in effect for this session.
	 * @return an envelope whose data is {@code {current, count, perspectives:[{name, description, repository, entryPoints, current}]}}
	 */
	public static McpEnvelope list()
	{
		try
		{
			return McpProviderSupport.inSession( () -> {
				Set<String> available = PerspectivesProvider.getAvailablePerspectives();
				if ( available.isEmpty() )
					return noPerspectives();
				String current = PerspectivesProvider.getCurrentPerspective( available );
				List<Map<String, Object>> perspectives = new ArrayList<Map<String, Object>>();
				for ( String name : available )
				{
					Map<String, Object> json = toMap( PerspectiveRegistry.getPerspective( name ).toJSON() );
					McpIntroPages.Intro intro = json.get( "description" ) == null ? McpIntroPages.read( (String) json.get( "intro" ) ) : null;
					Map<String, Object> m = summary( json, intro );
					m.put( "current", Boolean.valueOf( name.equals( current ) ) );
					perspectives.add( m );
				}
				Map<String, Object> result = new LinkedHashMap<String, Object>();
				result.put( "current", current );
				result.put( "count", Integer.valueOf( perspectives.size() ) );
				result.put( "perspectives", perspectives );
				return McpEnvelope.ok( result );
			} );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"could not list perspectives: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	/**
	 * Select a perspective for the current (MCP) session only. Unlike the web UI, an unknown or unavailable
	 * name is an error rather than a silent fallback to the default perspective.
	 * @return an envelope whose data is {@code {name, description, repository, entryPoints, scope, rules, intro, note}};
	 *         {@code intro} is present when the perspective's intro page is found
	 */
	@SuppressWarnings( "unchecked" )
	public static McpEnvelope select( String name )
	{
		Set<String> available;
		try
		{
			available = McpProviderSupport.inSession( PerspectivesProvider::getAvailablePerspectives );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"could not list perspectives: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
		if ( available.isEmpty() )
			return noPerspectives();
		if ( !available.contains( name ) )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND,
					"unknown or unavailable perspective: '" + name + "'; available: " + available );

		Map<String, Object> params = new LinkedHashMap<String, Object>();
		params.put( "name", name );
		params.put( "scope", PerspectivesProvider.SCOPE_SESSION );
		McpEnvelope env = McpProviderSupport.invoke( "perspective", "select", params );
		if ( !env.isOk() )
			return env;
		Object perspective = env.getData() instanceof Map ? ( (Map<String, Object>) env.getData() ).get( "perspective" ) : null;
		if ( !( perspective instanceof Map ) )
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "unexpected perspective provider response: " + env.getData() );
		Map<String, Object> selected = (Map<String, Object>) perspective;
		if ( !name.equals( selected.get( "name" ) ) )
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"perspective provider selected '" + selected.get( "name" ) + "' instead of '" + name + "'" );

		McpIntroPages.Intro intro = McpIntroPages.read( (String) selected.get( "intro" ) );
		Map<String, Object> result = summary( selected, intro );
		result.put( "scope", PerspectivesProvider.SCOPE_SESSION );
		Map<String, Object> rules = new LinkedHashMap<String, Object>();
		for ( String key : RULE_KEYS )
		{
			Object value = selected.get( key );
			if ( value instanceof List && !( (List<?>) value ).isEmpty() )
				rules.put( key, value );
		}
		result.put( "rules", rules );
		if ( intro != null )
		{
			Set<String> known = entryPointPaths( selected );
			try
			{
				// entry points are checked for existence with the caller's permissions
				result.put( "intro", McpProviderSupport.inSession( () -> intro.toMap( known ) ) );
			}
			catch ( Exception e )
			{
				log.log( Level.WARNING, "Cannot describe the intro page of perspective " + name, e );
			}
		}
		result.put( "note", SELECT_NOTE );
		return McpEnvelope.ok( result );
	}

	private static McpEnvelope noPerspectives()
	{
		return McpEnvelope.error( McpConstants.CODE_NOT_FOUND,
				"no perspectives available to the current user (this can be caused by IP limitations of the security provider)" );
	}

	@SuppressWarnings( "unchecked" )
	private static Map<String, Object> toMap( com.eclipsesource.json.JsonObject json )
	{
		return (Map<String, Object>) McpProviderSupport.fromJson( json );
	}

	/**
	 * @param intro the perspective's intro page, describes the perspective when it declares no description; may be null
	 */
	private static Map<String, Object> summary( Map<String, Object> perspective, McpIntroPages.Intro intro )
	{
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		for ( String key : SUMMARY_KEYS )
		{
			Object value = perspective.get( key );
			if ( value == null && "description".equals( key ) && intro != null )
				value = intro.getDescription();
			if ( value != null )
				m.put( key, value );
		}
		return m;
	}

	@SuppressWarnings( "unchecked" )
	private static Set<String> entryPointPaths( Map<String, Object> perspective )
	{
		Set<String> paths = new HashSet<String>();
		Object entries = perspective.get( "entryPoints" );
		if ( entries instanceof List )
			for ( Object entry : (List<Object>) entries )
				if ( entry instanceof Map )
					paths.add( String.valueOf( ( (Map<String, Object>) entry ).get( "path" ) ) );
		return paths;
	}
}
