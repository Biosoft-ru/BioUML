package biouml.plugins.mcp.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.util.LinkedHashMap;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.plugins.mcp.McpConstants;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulatorRegistry;
import biouml.plugins.simulation.Simulator;
import biouml.standard.simulation.SimulationResult;

import one.util.streamex.EntryStream;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.server.servlets.webservices.providers.WebJSONProviderSupport;
import ru.biosoft.server.servlets.webservices.providers.WebProvider;

/**
 * Shared, test-friendly helpers for the simulation MCP tools (phase 4).
 *
 * <p>All methods take/return plain JSON-serializable values and never throw checked exceptions —
 * failures are returned as {@link McpEnvelope} error envelopes. A diagram that has no dynamic
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
		DataElement de;
		try
		{
			de = CollectionFactory.getDataElement( path );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no diagram at path: " + path );
		}
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

	// ---------------------------------------------------------------- run

	/**
	 * Simulate a diagram headlessly and return the time series.
	 * @param path            repository path to the diagram
	 * @param initialTime     t0 (default 0)
	 * @param completionTime  tf (default 100)
	 * @param timeIncrement   step (default 1.0; 0 means auto)
	 * @param solverName      solver name, e.g. "JVode" (default: the engine's default)
	 * @param maxPoints       cap on the number of time-series points returned (default 50)
	 */
	public static McpEnvelope run( String path, Double initialTime, Double completionTime,
			Double timeIncrement, String solverName, Integer maxPoints )
	{
		McpEnvelope resolved = diagramWithModel( path );
		if ( !resolved.isOk() )
			return resolved;
		Diagram diagram = (Diagram) resolved.getData();

		double t0 = initialTime == null ? 0.0 : initialTime.doubleValue();
		double tf = completionTime == null ? 100.0 : completionTime.doubleValue();
		double inc = timeIncrement == null ? 1.0 : timeIncrement.doubleValue();
		int cap = maxPoints == null ? 50 : maxPoints.intValue();
		if ( tf <= t0 )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS,
					"completionTime (" + tf + ") must be greater than initialTime (" + t0 + ")" );

		JavaSimulationEngine engine = new JavaSimulationEngine();
		try
		{
			// Register the diagram in the repository so the engine's model code-generation can
			// resolve its path (a detached, never-saved diagram has no stable path and fails code-gen).
			try
			{
				ru.biosoft.access.CollectionFactoryUtils.save( diagram );
			}
			catch ( Exception e )
			{
				// ignore — the diagram may already be registered
			}
			engine.setDiagram( diagram );
			engine.setInitialTime( t0 );
			engine.setCompletionTime( tf );
			engine.setTimeIncrement( inc );
			if ( solverName != null && !solverName.isEmpty() )
			{
				Simulator sim = SimulatorRegistry.getSimulator( solverName );
				if ( sim == null )
					return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "unknown solver: " + solverName );
				engine.setSolver( sim );
			}

			Model model = engine.createModel();
			if ( model == null )
				return McpEnvelope.error( McpConstants.CODE_MISSING_MODEL,
						"could not build a simulation model from the diagram (no dynamic equations)" );

			SimulationResult result = engine.simulateSimple( model );
			if ( result == null || result.getCount() <= 0 )
				return McpEnvelope.error( McpConstants.CODE_MISSING_MODEL,
						"simulation produced no time series" );

			String[] variables = result.getVariables();
			double[] times = result.getTimes();
			int n = result.getCount();

			// Build a compact, capped time series: points = [{t, var1, var2, ...}, ...]
			List<Map<String, Object>> points = new ArrayList<Map<String, Object>>();
			int step = Math.max( 1, n / Math.max( 1, cap ) );
			for ( int i = 0; i < n; i += step )
			{
				Map<String, Object> pt = new LinkedHashMap<String, Object>();
				pt.put( "t", Double.valueOf( times[ i ] ) );
				for ( String v : variables )
					pt.put( v, Double.valueOf( result.getValues( v )[ i ] ) );
				points.add( pt );
			}

			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "status", "done" );
			m.put( "solver", solverName == null || solverName.isEmpty() ? "JVode" : solverName );
			m.put( "variables", new ArrayList<String>( java.util.Arrays.asList( variables ) ) );
			m.put( "pointCount", Integer.valueOf( n ) );
			m.put( "returnedPoints", Integer.valueOf( points.size() ) );
			m.put( "truncated", Boolean.valueOf( points.size() < n ) );
			m.put( "initial", result.getInitial( variables.length > 0 ? variables[ 0 ] : "" ) );
			m.put( "final", result.getFinal( variables.length > 0 ? variables[ 0 ] : "" ) );
			m.put( "points", points );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"simulation failed: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
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
		McpEnvelope resolved = diagramWithModel( path );
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
		McpEnvelope env = McpProviderSupport.invoke( provider, "simulation", "simulate", params );
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
