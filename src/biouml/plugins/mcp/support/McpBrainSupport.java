package biouml.plugins.mcp.support;

import java.util.LinkedHashMap;
import java.util.Map;

import biouml.plugins.mcp.McpConstants;

import biouml.model.Diagram;
import biouml.plugins.brain.diagram.BrainGenerateCompositeDiagramAction;
import biouml.plugins.brain.diagram.BrainGenerateEquationsAction;
import biouml.plugins.brain.diagram.BrainGenerateMultilevelModelAction;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import ru.biosoft.jobcontrol.JobControl;

/**
 * Shared, test-friendly helpers for the brain-model-generation MCP tools.
 *
 * <p>These mirror the perform path of the web UI's "Generate ... brain model" context-menu items,
 * each of which is a {@code BackgroundDynamicAction} whose {@code getJobControl(...).run()} does the
 * whole of the work (deploying the regional/cellular/receptor models into new diagrams and saving
 * them) with no UI. The caller drives the returned {@link JobControl} synchronously, exactly as the
 * in-repo test {@code TestAddRowTableAction} does for the table actions.</p>
 */
public final class McpBrainSupport
{
	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger( McpBrainSupport.class.getName() );

	private McpBrainSupport()
	{
	}

	/**
	 * Resolve a repository path to a {@link Diagram}.
	 * @return an envelope whose data is the diagram on success, or not_found/path_escape/invalid.
	 */
	private static McpEnvelope resolveDiagram( String path )
	{
		McpEnvelope resolved = McpRepositorySupport.resolve( path );
		if ( !resolved.isOk() )
			return resolved;
		DataElement de = (DataElement) resolved.getData();
		if ( !( de instanceof Diagram ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "element is not a diagram: " + path );
		return McpEnvelope.ok( de );
	}

	/**
	 * Generate the brain-model equations from a brain diagram — headless "Generate equations". Runs
	 * {@code BrainGenerateEquationsAction.getJobControl(diagram).run()} synchronously.
	 */
	public static McpEnvelope generateEquations( String path )
	{
		return runBrainAction( path, "generateEquations", new BrainGenerateEquationsAction() );
	}

	/**
	 * Generate the brain composite diagram from a brain diagram — headless "Generate composite
	 * diagram". Runs {@code BrainGenerateCompositeDiagramAction.getJobControl(diagram).run()}
	 * synchronously.
	 */
	public static McpEnvelope generateCompositeDiagram( String path )
	{
		return runBrainAction( path, "generateCompositeDiagram", new BrainGenerateCompositeDiagramAction() );
	}

	/**
	 * Generate the brain multi-level model from a brain diagram — headless "Generate multi-level
	 * model". Runs {@code BrainGenerateMultilevelModelAction.getJobControl(diagram).run()}
	 * synchronously.
	 */
	public static McpEnvelope generateMultilevelModel( String path )
	{
		return runBrainAction( path, "generateMultilevelModel", new BrainGenerateMultilevelModelAction() );
	}

	/**
	 * Common driver: resolve the diagram, check the action is applicable to it, obtain the job
	 * control, run it synchronously, and report the resulting status.
	 */
	private static McpEnvelope runBrainAction( String path, String operation, BackgroundDynamicAction action )
	{
		McpEnvelope resolved = resolveDiagram( path );
		if ( !resolved.isOk() )
			return resolved;
		Diagram diagram = (Diagram) resolved.getData();
		if ( !action.isApplicable( diagram ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS,
					"diagram is not a brain diagram (the model does not apply): " + path );
		try
		{
			JobControl jc = action.getJobControl( diagram, null, null );
			jc.run();
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "operation", operation );
			m.put( "path", path );
			m.put( "status", statusString( jc ) );
			m.put( "completed", Boolean.valueOf( jc.getStatus() == JobControl.COMPLETED ) );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"brain " + operation + " failed: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	/** A human-readable status for a finished job control. */
	private static String statusString( JobControl jc )
	{
		int s = jc.getStatus();
		if ( s == JobControl.COMPLETED )
			return "done";
		if ( s == JobControl.TERMINATED_BY_ERROR )
			return "error";
		if ( s == JobControl.TERMINATED_BY_REQUEST )
			return "cancelled";
		return String.valueOf( s );
	}
}
