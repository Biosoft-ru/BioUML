package biouml.plugins.mcp.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.util.LinkedHashMap;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.plugins.mcp.McpConstants;
import biouml.plugins.simulation.SimulatorRegistry;

import one.util.streamex.EntryStream;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.server.servlets.webservices.providers.WebJSONProviderSupport;
import ru.biosoft.server.servlets.webservices.providers.WebProvider;

/**
 * Shared, test-friendly helpers for the simulation MCP tools (phase 4).
 *
 * <p>All methods take/return plain JSON-serializable values and never throw checked exceptions —
 * failures are returned as {@link McpEnvelope} error envelopes. All simulation work is delegated
 * to the platform's {@code simulation} provider (async job model): a diagram that has no dynamic
 * model (no rate equations) is reported with {@link McpConstants#CODE_MISSING_MODEL} rather than
 * being handed to the solver.</p>
 */
public final class McpSimulationSupport
{
	private McpSimulationSupport()
	{
	}

	/**
	 * Resolve a diagram and ensure it carries a dynamic model ({@link EModel} role) that actually
	 * contains at least one rate (ODE) equation.
	 * @return envelope whose data is the {@link Diagram} on success, else a not_found /
	 *         missing_dynamic_model error.
	 */
	private static McpEnvelope diagramWithModel( String path )
	{
		if ( path == null || path.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "path must be a non-empty string" );
		// Resolve via the shared helper: tries the raw path, then a URL-decoded form, so an
		// agent-supplied %20-encoded path still resolves. Returns null if neither resolves.
		DataElement de = McpProviderSupport.resolveElement( path );
		if ( de == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND,
					"no diagram at path: " + path + " — check that the path starts with a repository root (e.g. 'data/...') and that spaces are literal, not URL-encoded (%20)" );
		if ( !( de instanceof Diagram ) )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no diagram at path: " + path );
		Diagram diagram = (Diagram) de;
		if ( !( diagram.getRole() instanceof EModel ) )
			return McpEnvelope.error( McpConstants.CODE_MISSING_MODEL, "diagram has no dynamic model: " + path );
		EModel model = (EModel) diagram.getRole();
		boolean hasRate = false;
		try
		{
			for ( biouml.model.dynamics.Equation eq : model.getEquations().toList() )
				if ( eq != null && biouml.model.dynamics.Equation.isRate( eq.getType() ) )
				{
					hasRate = true;
					break;
				}
		}
		catch ( Exception e )
		{
			// ignore — treat as no rate equations
		}
		if ( !hasRate )
			return McpEnvelope.error( McpConstants.CODE_MISSING_MODEL,
					"diagram has a dynamic model but no rate (ODE) equations to simulate: " + path );
		return McpEnvelope.ok( diagram );
	}

	// ---------------------------------------------------------------- solvers

	/**
	 * List the ODE solvers registered in the simulation engine (name, type, implementation class).
	 */
	public static McpEnvelope listSolvers()
	{
		List<Map<String, Object>> solvers = new ArrayList<Map<String, Object>>();
		try
		{
			EntryStream<String, String> reg = SimulatorRegistry.registry( "JAVA" );
			if ( reg != null )
			{
				for ( Map.Entry<String, String> e : reg.toList() )
				{
					Map<String, Object> m = new LinkedHashMap<String, Object>();
					m.put( "name", e.getKey() );
					m.put( "type", "JAVA" );
					m.put( "class", e.getValue() );
					solvers.add( m );
				}
			}
		}
		catch ( Exception e )
		{
			// fall through; report whatever we collected (possibly empty)
		}
		// The default solver is always available even if the extension registry is empty.
		boolean hasDefault = false;
		for ( Map<String, Object> m : solvers )
			if ( "JVode".equals( m.get( "name" ) ) )
				hasDefault = true;
		if ( !hasDefault )
		{
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "name", "JVode" );
			m.put( "type", "JAVA" );
			m.put( "class", "biouml.plugins.simulation.ode.jvode.JVodeSolver" );
			solvers.add( 0, m );
		}
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put( "count", Integer.valueOf( solvers.size() ) );
		result.put( "solvers", solvers );
		return McpEnvelope.ok( result );
	}

	/**
	 * Heuristically decide whether a provider error is a <em>path-resolution</em> failure (worth a
	 * retry) as opposed to a genuine client/parameter error (not worth retrying). Matches the common
	 * "cannot find ..." / "no ... at path" shapes that the web providers emit when an element path does
	 * not resolve in the provider's session view.
	 */
	private static boolean isResolutionFailure( McpEnvelope env )
	{
		if ( env == null || env.isOk() )
			return false;
		// A not_found envelope is, by definition, a resolution failure.
		if ( McpConstants.CODE_NOT_FOUND.equals( env.getCode() ) )
			return true;
		String msg = env.getError();
		if ( msg == null )
			return false;
		String lower = msg.toLowerCase();
		return lower.contains( "cannot find" ) || lower.contains( "can not find" )
				|| lower.contains( "no diagram" ) || lower.contains( "no element" )
				|| lower.contains( "element not found" ) || lower.contains( "no data element" );
	}

	// ---------------------------------------------------------------- async run (provider-delegated)

	/**
	 * Start a diagram simulation as an async job — the headless equivalent of the web UI's "Simulate"
	 * button. Delegates to the {@code simulation} provider ({@code simulate} action), which runs the
	 * simulation on a background thread. Returns a {@code jobID} immediately; poll
	 * {@link #simulationStatus(String)} until it completes, then fetch the time series with
	 * {@link #simulationResult(String)}.
	 *
	 * <p>This is the provider-based counterpart to the synchronous {@link #run} (which runs the whole
	 * simulation inline and returns the time series in one call). Use this for large/long simulations
	 * that would otherwise exceed a client's request timeout.</p>
	 *
	 * @param path the repository path to the diagram
	 * @return an envelope whose data is {@code {jobID, plotCount}} on success.
	 */
	public static McpEnvelope startSimulation( String path )
	{
		// A diagram can briefly become unresolvable right after the repository has written to it
		// (the FileSystemWatcher fires element-changed / luceneIndex notifications, and a concurrent
		// read during that window can miss the element). That is transient — a short retry resolves
		// it. Only the not-found case is retried; a genuine missing_dynamic_model / invalid_params
		// is returned immediately.
		McpEnvelope resolved = diagramWithModel( path );
		if ( !resolved.isOk() && McpConstants.CODE_NOT_FOUND.equals( resolved.getCode() ) )
		{
			for ( int attempt = 1; attempt <= 3; attempt++ )
			{
				try
				{
					Thread.sleep( 750L * attempt );
				}
				catch ( InterruptedException e )
				{
					Thread.currentThread().interrupt();
					break;
				}
				McpEnvelope retry = diagramWithModel( path );
				if ( retry.isOk() || !McpConstants.CODE_NOT_FOUND.equals( retry.getCode() ) )
				{
					resolved = retry;
					break;
				}
			}
		}
		if ( !resolved.isOk() )
			return resolved;
		WebProvider provider = McpProviderSupport.provider( "simulation" );
		if ( provider == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no provider registered for prefix: simulation" );
		String jobID = java.util.UUID.randomUUID().toString();
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		params.put( McpProviderSupport.KEY_DE, path );
		params.put( "jobID", jobID );
		params.put( "engine", new ArrayList<Object>() ); // empty engine options (use defaults)
		// The provider re-resolves the diagram from its own session view. That can transiently fail to
		// find an element that our pre-provider check just resolved (a per-session view divergence — e.g.
		// a stale snapshot or a re-entrant currentPaths guard). Retry the invoke a few times on a
		// resolution-type failure; a genuine error (e.g. a real parameter problem) is returned as-is.
		McpEnvelope env = McpProviderSupport.invoke( provider, "simulation", "simulate", params );
		for ( int attempt = 1; !env.isOk() && isResolutionFailure( env ) && attempt <= 3; attempt++ )
		{
			try
			{
				Thread.sleep( 500L * attempt );
			}
			catch ( InterruptedException e )
			{
				Thread.currentThread().interrupt();
				break;
			}
			jobID = java.util.UUID.randomUUID().toString();
			params.put( "jobID", jobID );
			env = McpProviderSupport.invoke( provider, "simulation", "simulate", params );
		}
		if ( !env.isOk() )
			return env;
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "jobID", jobID );
		// The provider returns [jobID, plotCount] as its values array.
		Object values = env.getData();
		if ( values instanceof List && ( (List<?>) values ).size() >= 2 )
			m.put( "plotCount", ( (List<?>) values ).get( 1 ) );
		return McpEnvelope.ok( m );
	}

	/**
	 * Poll the status of a simulation job started by {@link #startSimulation} — the headless
	 * equivalent of the web UI's simulation progress. Delegates to the {@code simulation} provider
	 * ({@code status} action). Returns {@code {jobID, status, progress, completed}}.
	 *
	 * @param jobID the job ID returned by {@link #startSimulation}
	 * @return an envelope whose data is the re-shaped status.
	 */
	public static McpEnvelope simulationStatus( String jobID )
	{
		WebProvider provider = McpProviderSupport.provider( "simulation" );
		if ( provider == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no provider registered for prefix: simulation" );
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		params.put( McpProviderSupport.KEY_DE, jobID ); // the status action reads the jobID from 'de'
		Map<String, Object> resp = McpProviderSupport.responseMap( (WebJSONProviderSupport) provider, "simulation", "status", params );
		if ( resp == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no simulation job named: " + jobID );
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "jobID", jobID );
		Object status = resp.get( "status" );
		m.put( "status", status instanceof Number ? Integer.valueOf( ( (Number) status ).intValue() ) : null );
		Object percent = resp.get( "percent" );
		m.put( "progress", percent instanceof Number ? Integer.valueOf( ( (Number) percent ).intValue() ) : null );
		// The provider sends the accumulated job log as the last entry of `values`
		// (SimulationProvider.sendStatus(..., imageNames) with the message in imageNames).
		Object values = resp.get( "values" );
		if ( values instanceof List && ( (List<?>) values ).size() > 0 )
			m.put( "message", ( (List<?>) values ).get( ( (List<?>) values ).size() - 1 ) );
		m.put( "completed", status instanceof Number && ( (Number) status ).intValue() >= ru.biosoft.jobcontrol.JobControl.COMPLETED );
		return McpEnvelope.ok( m );
	}

	/**
	 * Fetch the time-series result of a completed simulation job — the headless equivalent of the web
	 * UI's result table. Delegates to the {@code simulation} provider ({@code result} action), which
	 * returns {@code {vars, times, values}} (and Q1/Q2/Q3 for stochastic results).
	 *
	 * @param jobID the job ID returned by {@link #startSimulation}
	 * @return an envelope whose data is the time-series structure.
	 */
	public static McpEnvelope simulationResult( String jobID )
	{
		WebProvider provider = McpProviderSupport.provider( "simulation" );
		if ( provider == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no provider registered for prefix: simulation" );
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		params.put( McpProviderSupport.KEY_DE, jobID ); // the result action reads the jobID from 'de'
		return McpProviderSupport.invoke( provider, "simulation", "result", params );
	}
}
