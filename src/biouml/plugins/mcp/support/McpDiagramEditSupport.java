package biouml.plugins.mcp.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import biouml.plugins.mcp.McpConstants;

import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.SubDiagram;
import biouml.workbench.diagram.action.AlignCenterXAction;
import biouml.workbench.diagram.action.AlignCenterYAction;
import biouml.workbench.diagram.action.AlignDownAction;
import biouml.workbench.diagram.action.AlignLeftAction;
import biouml.workbench.diagram.action.AlignRightAction;
import biouml.workbench.diagram.action.AlignUpAction;
import biouml.workbench.diagram.action.DistributeHorizontalAction;
import biouml.workbench.diagram.action.DistributeVerticalAction;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import ru.biosoft.jobcontrol.JobControl;

/**
 * Shared, test-friendly helpers for the headless diagram-editing MCP tools.
 *
 * <p>These mirror the perform path of the web UI's diagram context-menu items (layout operations,
 * split/clone/subdiagram/port operations, save-subset, add-upstream/downstream). Each is a
 * {@code BackgroundDynamicAction} whose {@code getJobControl(model, selectedItems, properties).run()}
 * does the whole of the work with no UI. The "selection" that a live editor would supply is passed
 * explicitly as a list of element paths (resolved to {@link DataElement}s for {@code selectedItems}).
 * All methods take/return plain JSON-serializable values and never throw checked exceptions —
 * failures are returned as {@link McpEnvelope} error envelopes.</p>
 */
public final class McpDiagramEditSupport
{
	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger( McpDiagramEditSupport.class.getName() );

	private McpDiagramEditSupport()
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
	 * Resolve a list of element paths (relative to the diagram's repository path or absolute) to a
	 * {@link DataElement} list, for use as a {@code BackgroundDynamicAction}'s {@code selectedItems}.
	 * @return an envelope whose data is the {@code List<DataElement>} on success, or not_found.
	 */
	private static McpEnvelope resolveSelection( String diagramPath, String[] elementPaths )
	{
		if ( elementPaths == null || elementPaths.length == 0 )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "a non-empty selection (list of element paths) is required" );
		List<DataElement> items = new ArrayList<DataElement>();
		for ( String p : elementPaths )
		{
			String full = p.startsWith( diagramPath + "/" ) ? p : ( diagramPath.contains( p ) ? p : diagramPath + "/" + p );
			DataElement de = CollectionFactory.getDataElement( full );
			if ( de == null )
				return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no element at path: " + full );
			items.add( de );
		}
		return McpEnvelope.ok( items );
	}

	/**
	 * Apply a layout operation to the selected nodes — the headless equivalent of the web UI's
	 * "Align ..." / "Distribute ..." diagram context-menu items. The operation regenerates the
	 * diagram view headlessly and moves the nodes via the diagram's {@code SemanticController}, so it
	 * is safe without a live editor (provided the node selection is passed explicitly).
	 *
	 * @param diagramPath full path of the diagram
	 * @param op          one of: {@code align-up}, {@code align-down}, {@code align-left},
	 *                    {@code align-right}, {@code align-centerx}, {@code align-centery},
	 *                    {@code distribute-hor}, {@code distribute-ver}
	 * @param nodePaths   the paths of the nodes to lay out (at least 2 for align, at least 3 for
	 *                    distribute)
	 * @return an envelope whose data is {@code {applied, op, count}} on success.
	 */
	public static McpEnvelope layout( String diagramPath, String op, String[] nodePaths )
	{
		McpEnvelope resolved = resolveDiagram( diagramPath );
		if ( !resolved.isOk() )
			return resolved;
		Diagram diagram = (Diagram) resolved.getData();
		McpEnvelope sel = resolveSelection( diagramPath, nodePaths );
		if ( !sel.isOk() )
			return sel;
		BackgroundDynamicAction action;
		switch ( op == null ? "" : op )
		{
			case "align-up":        action = new AlignUpAction(); break;
			case "align-down":      action = new AlignDownAction(); break;
			case "align-left":      action = new AlignLeftAction(); break;
			case "align-right":     action = new AlignRightAction(); break;
			case "align-centerx":   action = new AlignCenterXAction(); break;
			case "align-centery":   action = new AlignCenterYAction(); break;
			case "distribute-hor":  action = new DistributeHorizontalAction(); break;
			case "distribute-ver":  action = new DistributeVerticalAction(); break;
			default:
				return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS,
						"unknown layout op: " + op + " (expected align-up/align-down/align-left/align-right/align-centerx/align-centery/distribute-hor/distribute-ver)" );
		}
		if ( !action.isApplicable( diagram ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "layout is not applicable to this diagram: " + diagramPath );
		try
		{
			@SuppressWarnings( "unchecked" )
			List<DataElement> items = (List<DataElement>) sel.getData();
			action.getJobControl( diagram, items, null ).run();
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "applied", diagramPath );
			m.put( "op", op );
			m.put( "count", Integer.valueOf( items.size() ) );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"layout '" + op + "' failed: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	/**
	 * A finished job control's status as a short string.
	 */
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

	/**
	 * Update the sub-diagrams of a composite diagram — the headless equivalent of the web UI's
	 * "Update submodel" menu item ({@code UpdateSubModelAction}): recursively sets each sub-diagram's
	 * diagram and clears its view so the composite re-reads the current sub-models.
	 */
	public static McpEnvelope updateSubmodel( String diagramPath )
	{
		McpEnvelope resolved = resolveDiagram( diagramPath );
		if ( !resolved.isOk() )
			return resolved;
		Diagram diagram = (Diagram) resolved.getData();
		try
		{
			biouml.standard.diagram.UpdateSubModelAction action = new biouml.standard.diagram.UpdateSubModelAction();
			if ( !action.isApplicable( diagram ) )
				return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "update-submodel is not applicable to this diagram: " + diagramPath );
			JobControl jc = action.getJobControl( diagram, new ArrayList<DataElement>(), null );
			jc.run();
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "applied", diagramPath );
			m.put( "status", statusString( jc ) );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"update-submodel failed: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}
}
