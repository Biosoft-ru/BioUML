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
import biouml.standard.simulation.SimulationResult;
import biouml.standard.simulation.plot.Series;
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
	 * Render a string's characters as readable tokens for diagnostics: printable ASCII as-is, spaces as
	 * {@code \\u0020}, and any other (e.g. non-breaking space, control, non-ASCII) character as
	 * {@code \\uXXXX}. Used to make invisible/whitespace differences in a caller-supplied path visible
	 * in the server log.
	 */
	private static String charCodes( String s )
	{
		if ( s == null )
			return "null";
		StringBuilder sb = new StringBuilder( s.length() * 2 );
		for ( char c : s.toCharArray() )
		{
			if ( c == ' ' )
				sb.append( "\\u0020" );
			else if ( c >= 0x20 && c <= 0x7e )
				sb.append( c );
			else
				sb.append( String.format( "\\u%04x", (int) c ) );
		}
		return sb.toString();
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
		// Diagnostic: log the exact diagramPath the caller sent, with a char-code breakdown so that
		// invisible/whitespace issues (non-breaking spaces, doubled roots, trailing junk) are visible in
		// the server log when a start fails with "Cannot find Diagram". The path string is not secret.
		try
		{
			java.util.logging.Logger.getLogger( McpSimulationSupport.class.getName() )
					.info( "MCP simulation_start path=[" + path + "] len=" + ( path == null ? -1 : path.length() )
							+ " chars=" + ( path == null ? "null" : charCodes( path ) ) );
		}
		catch ( Throwable t )
		{
			// logging must never break the simulation start
		}
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
		// Diagnostic: a "no simulation job" for a job the agent JUST started is the signature of a
		// session mismatch (the WebJob was registered in a different session's cache than the poll
		// reads). Log the jobID and the session the poll is running under so the divergence is visible
		// in the server log next to the start.
		try
		{
			java.util.logging.Logger.getLogger( McpSimulationSupport.class.getName() )
					.info( "MCP simulation_status jobID=" + jobID
							+ " session=" + ru.biosoft.access.security.SecurityManager.getSession() );
		}
		catch ( Throwable t )
		{
			// logging must never break the status poll
		}
		WebProvider provider = McpProviderSupport.provider( "simulation" );
		if ( provider == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no provider registered for prefix: simulation" );
		if ( !( provider instanceof WebJSONProviderSupport ) )
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "simulation provider is not a WebJSONProviderSupport" );
		// responseMap swallows provider errors into null; use the exception-preserving path so a real
		// failure (e.g. a session mismatch or a missing WebJob) is reported verbatim, not masked as
		// "no simulation job named".
		String raw = invokeRaw( (WebJSONProviderSupport) provider, "simulation", "status", jobID );
		if ( raw == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no simulation job named: " + jobID
					+ " — the job may be in a different session; restart the simulation and poll in the same session" );
		// The status provider (JSONResponse.sendStatus) writes a root-level JSON object:
		//   {"type":0, "status":N, "percent":P, "values":[...]}
		// where status/percent are at the ROOT (not nested inside values). The shared parseResponse
		// only extracts the "values" field, which would lose status/percent. So parse the raw JSON
		// directly here to read all three root-level fields.
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "jobID", jobID );
		try
		{
			com.eclipsesource.json.JsonObject obj = com.eclipsesource.json.Json.parse( raw ).asObject();
			int type = obj.getInt( "type", ru.biosoft.server.servlets.webservices.JSONResponse.TYPE_OK );
			if ( type == ru.biosoft.server.servlets.webservices.JSONResponse.TYPE_ERROR )
			{
				String msg = obj.getString( "message", "provider error" );
				String code = obj.getString( "errorCode", McpConstants.CODE_INTERNAL );
				return McpEnvelope.error( code, msg );
			}
			// sendStatus always writes "status" at the root; -1 means the field was absent (defensive).
			int statusVal = obj.getInt( "status", -1 );
			m.put( "status", statusVal );
			com.eclipsesource.json.JsonValue percentJson = obj.get( "percent" );
			m.put( "progress", percentJson != null && percentJson.isNumber() ? Integer.valueOf( percentJson.asInt() ) : null );
			// The provider sends the accumulated job log as the last entry of the root-level "values" array.
			com.eclipsesource.json.JsonValue valuesJson = obj.get( "values" );
			if ( valuesJson != null && valuesJson.isArray() )
			{
				com.eclipsesource.json.JsonArray arr = valuesJson.asArray();
				if ( arr.size() > 0 )
					m.put( "message", arr.get( arr.size() - 1 ).asString() );
			}
			m.put( "completed", statusVal >= ru.biosoft.jobcontrol.JobControl.COMPLETED );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"could not parse simulation status response: " + e.getMessage() );
		}
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

	/**
	 * Persist the in-memory result of a completed simulation into the repository — the headless
	 * equivalent of the web UI's "Save result" button (the {@code save_result} action). By default the
	 * MCP simulation tools keep the result only in memory (the JVM-global job map), so it is gone once
	 * the server restarts; this tool clones it to a repository element at {@code destinationPath} so it
	 * is durable and addressable like any other {@code SimulationResult}.
	 *
	 * <p>{@code destinationPath} is the FULL path of the new element to create (e.g.
	 * {@code myproject/mydiagram/result1}); its parent must exist and be writable. Call it AFTER
	 * {@code biouml_simulation_status} reports {@code completed}.</p>
	 *
	 * @param jobID           the job ID returned by {@link #startSimulation}
	 * @param destinationPath the full repository path to save the result at
	 * @return an envelope whose data is the provider's response (usually the string {@code "ok"})
	 */
	public static McpEnvelope saveSimulationResult( String jobID, String destinationPath )
	{
		WebProvider provider = McpProviderSupport.provider( "simulation" );
		if ( provider == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no provider registered for prefix: simulation" );
		if ( destinationPath == null || destinationPath.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS,
					"destinationPath is required (the full repository path of the new result element)" );
		// The save_result action reads the destination from 'de' and the job from 'jobID'.
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		params.put( McpProviderSupport.KEY_DE, destinationPath );
		params.put( "jobID", jobID );
		return McpProviderSupport.invoke( provider, "simulation", "save_result", params );
	}

	/**
	 * Invoke a provider action directly and return the provider's raw JSON response string
	 * ({@code {"type":OK|ERROR,"values":...,"message":...}}), <em>preserving</em> the provider's error
	 * message — unlike {@link McpProviderSupport#responseMap}, which swallows every failure into
	 * {@code null}. Used by {@link #simulationStatus} so a real failure (a session mismatch, a missing
	 * {@code WebJob}, ...) is reported verbatim rather than masked as "no simulation job named".
	 *
	 * @return the provider's JSON string, or {@code null} if the call could not be made or the
	 *         response was empty / unparseable.
	 */
	private static String invokeRaw( WebJSONProviderSupport provider, String name, String action, String de )
	{
		Map<String, String> request = new LinkedHashMap<String, String>();
		request.put( ru.biosoft.server.servlets.webservices.BiosoftWebRequest.ACTION, action );
		request.put( McpProviderSupport.KEY_DE, de );
		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		boolean bound = false;
		try
		{
			// ensureSession binds the thread to a WebSession-backed session BEFORE the provider runs.
			// This is the critical step: the status action calls WebSession.getCurrentSession().putImage(...),
			// which NPEs if no WebSession is bound. ensureSession (McpProviderSupport) does exactly what
			// runProvider does — for a real session it calls ensureWebSessionFor(sid) (which re-registers
			// the thread to sid via WebSession.getSession), and for a headless call it binds the shared
			// headless session. We replicate that binding here so the provider sees a live WebSession.
			String currentSid = ru.biosoft.access.security.SecurityManager.getSession();
			if ( currentSid != null && !currentSid.isEmpty()
					&& !ru.biosoft.access.security.SecurityManager.SYSTEM_SESSION.equals( currentSid ) )
			{
				McpProviderSupport.ensureWebSessionFor( currentSid );
				// ensureWebSessionFor already re-registers the thread to currentSid; we don't unbind it
				// because the servlet will manage the thread's session lifecycle.
			}
			else
			{
				McpProviderSupport.ensureWebSession();
				bound = true;
			}
			provider.process( new ru.biosoft.server.servlets.webservices.BiosoftWebRequest( request ),
					new ru.biosoft.server.servlets.webservices.JSONResponse( out ) );
		}
		catch ( ru.biosoft.server.servlets.webservices.WebException e )
		{
			// A WebException is a *provider* error — surface its message so it isn't masked.
			return "{\"type\":\"" + ru.biosoft.server.servlets.webservices.JSONResponse.TYPE_ERROR
					+ "\",\"message\":" + com.eclipsesource.json.Json.value( e.getMessage() == null ? "provider error" : e.getMessage() ).toString() + "}";
		}
		catch ( Exception e )
		{
			return "{\"type\":\"" + ru.biosoft.server.servlets.webservices.JSONResponse.TYPE_ERROR
					+ "\",\"message\":" + com.eclipsesource.json.Json.value( e.getClass().getSimpleName() + ": " + e.getMessage() ).toString() + "}";
		}
		finally
		{
			if ( bound )
				ru.biosoft.access.security.SecurityManager.removeThreadFromSessionRecord();
		}
		byte[] bytes = out.toByteArray();
		if ( bytes.length == 0 )
			return null;
		return new String( bytes, java.nio.charset.StandardCharsets.UTF_8 );
	}

	// ---------------------------------------------------------------- server-side plot

	/**
	 * A {@link Series} that reads its data from a <em>directly-held</em> {@link SimulationResult}
	 * rather than resolving it by repository path. A simulation result freshly produced by a job is
	 * created in-memory by the engine with no repository path, so the stock {@link Series} (which calls
	 * {@code source.getDataElement(SimulationResult.class)}) cannot read it; overriding the public
	 * {@code getXValues}/{@code getYValues}/{@code getValuesCount} seams lets the same
	 * {@code PlotPane.redrawChart} machinery draw it. The x axis is always time.
	 */
	private static final class ResultSeries extends Series
	{
		private final SimulationResult result;

		ResultSeries( SimulationResult result, String yVar )
		{
			this.result = result;
			setXVar( "time" );
			setYVar( yVar );
			setName( yVar );
			setLegend( yVar );
		}

		@Override
		public int getValuesCount()
		{
			return result.getCount();
		}

		@Override
		public double[] getXValues()
		{
			return result.getTimes();
		}

		@Override
		public double[] getYValues()
		{
			Integer idx = result.getVariablePathMap().get( getYVar() );
			if ( idx == null )
				throw new IllegalStateException( "variable not in result: " + getYVar() );
			int size = result.getCount();
			double[] res = new double[ size ];
			for ( int i = 0; i < size; i++ )
				res[ i ] = result.getValue( i )[ idx.intValue() ];
			return res;
		}
	}

	/**
	 * Render a <em>plot</em> of a simulation result on the server and return it as a PNG, mirroring
	 * the web UI's "New plot" / "+" flow ({@code PlotProvider.drawChart}: build a {@code Plot}, add a
	 * {@code Series} per variable, draw via {@code PlotPane.redrawChart}, then
	 * {@code chart.createBufferedImage}). The raw time series is <em>not</em> sent to the caller —
	 * only a small metadata summary and the image — so an agent can inspect a large result without
	 * blowing its context.
	 *
	 * <p>The plot is rendered transiently (not saved to the repository): the agent gets the image, and
	 * the full-resolution data stays server-side, available through the plot on demand.</p>
	 *
	 * @param jobID     the job id returned by {@link #startSimulation}
	 * @param variables optional subset of variables to plot (keys of the result's variable map); when
	 *                  empty or {@code null}, every non-{"time"} variable is plotted
	 * @param width     image width in pixels ({@code null} = 700)
	 * @param height    image height in pixels ({@code null} = 450)
	 * @return an envelope whose data is {@code {jobID, plot, variables, points, timeRange, finals,
	 *         image}} — {@code image} is the base64 PNG and {@code plot} is a short handle for future
	 *         per-plot retrieval.
	 */
	public static McpEnvelope plotSimulationResult( String jobID, String[] variables, Integer width, Integer height )
	{
		if ( jobID == null || jobID.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "jobID must be a non-empty string" );
		SimulationResult result =
				biouml.plugins.simulation.web.SimulationProvider.getSimulationResult( jobID );
		if ( result == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND,
					"no simulation result for job: " + jobID
							+ " — start it with biouml_simulation_start and poll biouml_simulation_status until completed" );

		// Choose which variables to plot. Default: every non-"time" variable, in a stable order.
		java.util.List<String> vars = new java.util.ArrayList<String>();
		if ( variables != null )
			for ( String v : variables )
				if ( v != null && !v.isEmpty() )
					vars.add( v );
		if ( vars.isEmpty() )
			for ( String key : result.getVariablePathMap().keySet() )
				if ( !"time".equals( key ) )
					vars.add( key );
		if ( vars.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS,
					"the simulation result has no plottable variables (job " + jobID + ")" );
		// Drop any unknown variable names the caller passed, but fail if <em>all</em> of them were unknown.
		java.util.Map<String, Integer> vmap = result.getVariablePathMap();
		int before = vars.size();
		vars.removeIf( v -> !vmap.containsKey( v ) );
		if ( vars.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS,
					"none of the requested variables exist in the result (job " + jobID
							+ "); available: " + availableVariables( vmap ) );

		// Build the plot and add one series per variable (the web UI's "+" action), cycling the
		// standard color palette.
		biouml.standard.simulation.plot.Plot plot = new biouml.standard.simulation.plot.Plot( null, "mcp_plot_" + jobID );
		int w = ( width == null || width <= 0 ) ? 700 : width.intValue();
		int h = ( height == null || height <= 0 ) ? 450 : height.intValue();
		try
		{
			for ( int i = 0; i < vars.size(); i++ )
			{
				ResultSeries s = new ResultSeries( result, vars.get( i ) );
				java.awt.Color color = biouml.model.dynamics.plot.PlotsInfo.POSSIBLE_COLORS[i % biouml.model.dynamics.plot.PlotsInfo.POSSIBLE_COLORS.length];
				s.setSpec( new ru.biosoft.graphics.Pen( 1.5f, color ) );
				plot.addSeries( s );
			}
			org.jfree.chart.JFreeChart chart = org.jfree.chart.ChartFactory.createXYLineChart( "", "time", "value", null,
					org.jfree.chart.plot.PlotOrientation.VERTICAL, true, true, false );
			chart.setBackgroundPaint( java.awt.Color.white );
			chart.getXYPlot().setBackgroundPaint( java.awt.Color.white );
			biouml.plugins.simulation.plot.PlotPane.redrawChart( plot, chart );
			java.awt.image.BufferedImage image = chart.createBufferedImage( w, h );

			java.io.ByteArrayOutputStream png = new java.io.ByteArrayOutputStream();
			javax.imageio.ImageIO.write( image, "png", png );

			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "jobID", jobID );
			m.put( "plot", "mcp_plot_" + jobID );
			m.put( "variables", vars );
			m.put( "points", Integer.valueOf( result.getCount() ) );
			double[] times = result.getTimes();
			if ( times.length > 0 )
			{
				Map<String, Object> range = new LinkedHashMap<String, Object>();
				range.put( "from", times[ 0 ] );
				range.put( "to", times[ times.length - 1 ] );
				m.put( "timeRange", range );
			}
			Map<String, Object> finals = new LinkedHashMap<String, Object>();
			for ( String v : vars )
				finals.put( v, Double.valueOf( result.getFinal( v ) ) );
			m.put( "finals", finals );
			m.put( "imageFormat", "png" );
			m.put( "width", Integer.valueOf( w ) );
			m.put( "height", Integer.valueOf( h ) );
			// The image is NOT put in the envelope (it would be a base64 text token bomb). Instead it is
			// carried as a side-channel byte array on the envelope; the dispatcher emits it as a separate
			// MCP `image` content block (vision) AFTER the text block, so the model sees the picture, not
			// ~200K base64 chars of text.
			McpEnvelope env = McpEnvelope.ok( m );
			env.setAttribute( "mcp.image", png.toByteArray() );
			return env;
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"could not render the simulation plot: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	/** A comma-joined list of the result's variable names (excluding "time"), for error messages. */
	private static String availableVariables( java.util.Map<String, Integer> vmap )
	{
		StringBuilder sb = new StringBuilder();
		for ( String key : vmap.keySet() )
		{
			if ( "time".equals( key ) )
				continue;
			if ( sb.length() > 0 )
				sb.append( ", " );
			sb.append( key );
		}
		return sb.toString();
	}
}
