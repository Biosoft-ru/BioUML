package biouml.plugins.mcp.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.SubDiagram;
import biouml.plugins.mcp.McpConstants;
import biouml.standard.diagram.ChangePortTypeAction;
import biouml.standard.diagram.ChangeSubdiagramAction;
import biouml.standard.diagram.CloneNodeAction;
import biouml.standard.diagram.MergeCloneAction;
import biouml.standard.diagram.SplitDiagramAction;
import biouml.workbench.diagram.action.AlignCenterXAction;
import biouml.workbench.diagram.action.AlignCenterYAction;
import biouml.workbench.diagram.action.AlignDownAction;
import biouml.workbench.diagram.action.AlignLeftAction;
import biouml.workbench.diagram.action.AlignRightAction;
import biouml.workbench.diagram.action.AlignUpAction;
import biouml.workbench.diagram.action.DistributeHorizontalAction;
import biouml.workbench.diagram.action.DistributeVerticalAction;
import biouml.workbench.diagram.action.SaveDiagramSubsetAction;
import biouml.workbench.graphsearch.actions.AddFromSearchDownAction;
import biouml.workbench.graphsearch.actions.AddFromSearchUpAction;
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

	/**
	 * Run a {@link BackgroundDynamicAction} against a diagram's semantic controller. The diagram
	 * must pass the action's model-level {@code isApplicable}; otherwise a clean {@code invalid_params}
	 * envelope is returned.
	 */
	private static McpEnvelope runDiagramAction( Diagram diagram, BackgroundDynamicAction action, String what )
	{
		if ( !action.isApplicable( diagram ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, what + " is not applicable to this diagram: " + DataElementPath.create( diagram ) );
		try
		{
			JobControl jc = action.getJobControl( diagram, new ArrayList<DataElement>(), null );
			jc.run();
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "applied", DataElementPath.create( diagram ).toString() );
			m.put( "status", statusString( jc ) );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					what + " failed: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	/**
	 * Split the selected elements of a composite diagram into a new diagram — the headless
	 * equivalent of the web UI's "Split diagram" menu item ({@link SplitDiagramAction}). The new
	 * diagram is created in {@code targetCollectionPath}/{@code name} (default: the diagram's own
	 * collection, name "Module"), and optionally added back as a module with connection ports.
	 *
	 * @param diagramPath         full path of the composite diagram to split
	 * @param elementPaths        the nodes/edges to move to the new diagram (at least one)
	 * @param name                name of the new diagram (default "Module")
	 * @param targetCollectionPath the collection the new diagram is created in (default: source's)
	 * @param autoIncludeReactions also pull in every reaction whose participants are all selected
	 * @param addModule            additionally embed the new diagram back as a sub-diagram module
	 * @return an envelope whose data is {@code {applied, status, path, created}} on success.
	 */
	public static McpEnvelope splitDiagram( String diagramPath, String[] elementPaths, String name,
			String targetCollectionPath, boolean autoIncludeReactions, boolean addModule )
	{
		McpEnvelope resolved = resolveDiagram( diagramPath );
		if ( !resolved.isOk() )
			return resolved;
		Diagram diagram = (Diagram) resolved.getData();
		McpEnvelope sel = resolveSelection( diagramPath, elementPaths );
		if ( !sel.isOk() )
			return sel;
		if ( !biouml.standard.diagram.DiagramUtility.isComposite( diagram ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "split-diagram requires a composite diagram: " + diagramPath );
		SplitDiagramAction action = new SplitDiagramAction();
		SplitDiagramAction.SplitDiagramActionParameters params = new SplitDiagramAction.SplitDiagramActionParameters();
		params.setSubDiagramName( name == null || name.isEmpty() ? "Module" : name );
		params.setAutoIncludeReactions( autoIncludeReactions );
		params.setAddModule( addModule );
		if ( targetCollectionPath != null && !targetCollectionPath.isEmpty() )
		{
			McpEnvelope tp = McpRepositorySupport.resolve( targetCollectionPath );
			if ( !tp.isOk() )
				return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "target collection not found: " + targetCollectionPath );
			DataElement tDe = (DataElement) tp.getData();
			if ( ! ( tDe instanceof DataCollection ) )
				return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "target collection is not a collection: " + targetCollectionPath );
			params.setResultPath( DataElementPath.create( (DataCollection<?>) tDe ).getChildPath( params.getSubDiagramName() ) );
		}
		try
		{
			@SuppressWarnings( "unchecked" )
			List<DataElement> items = (List<DataElement>) sel.getData();
			JobControl jc = action.getJobControl( diagram, items, params );
			jc.run();
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "applied", diagramPath );
			m.put( "status", statusString( jc ) );
			String resultPath = params.getResultPath() != null ? params.getResultPath().toString() : null;
			if ( resultPath != null )
				m.put( "path", resultPath );
			m.put( "created", Boolean.valueOf( CollectionFactory.getDataElement( resultPath ) != null ) );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"split-diagram failed: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	/**
	 * Clone a pathway node — the headless equivalent of the web UI's "Clone node" menu item
	 * ({@link CloneNodeAction}). The clone inherits the node's variable and the listed reactions are
	 * redirected to it. Requires a pathway diagram and a node that carries a {@code VariableRole}.
	 *
	 * @param diagramPath   full path of the pathway diagram
	 * @param nodeName      the name of the node to clone
	 * @param cloneName     name of the new node (default: auto-generated)
	 * @param separateClones when true, one clone is created per reaction (named cloneName_reaction)
	 * @param reactions     names of the reactions whose edges are redirected to the clone
	 * @return an envelope whose data is {@code {applied, status, clone}} on success.
	 */
	public static McpEnvelope cloneNode( String diagramPath, String nodeName, String cloneName,
			boolean separateClones, String[] reactions )
	{
		McpEnvelope resolved = resolveDiagram( diagramPath );
		if ( !resolved.isOk() )
			return resolved;
		Diagram diagram = (Diagram) resolved.getData();
		if ( nodeName == null || nodeName.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "a node name is required" );
		Node node = (Node) diagram.findNode( nodeName );
		if ( node == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no node named '" + nodeName + "' in " + diagramPath );
		List<DataElement> items = new ArrayList<DataElement>();
		items.add( node );
		CloneNodeAction action = new CloneNodeAction();
		if ( !action.isApplicable( diagram ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS,
					"clone-node requires a pathway diagram: " + diagramPath );
		CloneNodeAction.CloneNodeActionParameters params = new CloneNodeAction.CloneNodeActionParameters( items );
		if ( cloneName != null && !cloneName.isEmpty() )
			params.setNodeName( cloneName );
		params.setSeparateClones( separateClones );
		if ( reactions != null && reactions.length > 0 )
		{
			if ( params.getAvailableReactions() != null && !params.getAvailableReactions().containsAll( java.util.Arrays.asList( reactions ) ) )
				return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS,
						"unknown reaction(s): " + java.util.Arrays.toString( reactions ) + "; available: " + params.getAvailableReactions() );
			params.setReactions( reactions );
		}
		try
		{
			JobControl jc = action.getJobControl( diagram, items, params );
			jc.run();
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "applied", diagramPath );
			m.put( "status", statusString( jc ) );
			m.put( "clone", params.getNodeName() );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"clone-node failed: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	/**
	 * Change a sub-diagram's target diagram — the headless equivalent of the web UI's "Change
	 * subdiagram" menu item ({@link ChangeSubdiagramAction}). The existing sub-diagram is replaced by a
	 * fresh one pointing at {@code targetPath}, preserving the node name and rewiring the ports.
	 * Requires a composite diagram and a sub-diagram element.
	 *
	 * @param diagramPath  full path of the composite diagram containing the sub-diagram
	 * @param subdiagramPath the path of the sub-diagram to replace
	 * @param targetPath   the path of the new diagram the sub-diagram should point at
	 * @return an envelope whose data is {@code {applied, status, subdiagram}} on success.
	 */
	public static McpEnvelope changeSubdiagram( String diagramPath, String subdiagramPath, String targetPath )
	{
		McpEnvelope resolved = resolveDiagram( diagramPath );
		if ( !resolved.isOk() )
			return resolved;
		Diagram diagram = (Diagram) resolved.getData();
		if ( subdiagramPath == null || subdiagramPath.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "a subdiagram path is required" );
		McpEnvelope st = McpRepositorySupport.resolve( subdiagramPath );
		if ( !st.isOk() )
			return st;
		DataElement subDe = (DataElement) st.getData();
		if ( ! ( subDe instanceof SubDiagram ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "element is not a sub-diagram: " + subdiagramPath );
		if ( targetPath == null || targetPath.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "a target path is required" );
		McpEnvelope tt = McpRepositorySupport.resolve( targetPath );
		if ( !tt.isOk() )
			return tt;
		if ( !( (DataElement) tt.getData() instanceof Diagram ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "target is not a diagram: " + targetPath );
		List<DataElement> items = new ArrayList<DataElement>();
		items.add( subDe );
		ChangeSubdiagramAction action = new ChangeSubdiagramAction();
		if ( !action.isApplicable( diagram ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "change-subdiagram requires a composite diagram: " + diagramPath );
		ChangeSubdiagramAction.ChangeSubdiagramActionParameters params = new ChangeSubdiagramAction.ChangeSubdiagramActionParameters( items );
		params.setDataElementPath( DataElementPath.create( (Diagram) tt.getData() ) );
		try
		{
			JobControl jc = action.getJobControl( diagram, items, params );
			jc.run();
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "applied", diagramPath );
			m.put( "status", statusString( jc ) );
			m.put( "subdiagram", subdiagramPath );
			m.put( "target", targetPath );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"change-subdiagram failed: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	/**
	 * Change a pathway port's type — the headless equivalent of the web UI's "Change port type" menu
	 * item ({@link ChangePortTypeAction}). The port is recreated with the new type and its edges are
	 * rewired. Requires a pathway diagram and a port node.
	 *
	 * @param diagramPath full path of the pathway diagram
	 * @param portName    the name of the port node
	 * @param portType    the new type: {@code input}, {@code output}, or {@code contact}
	 * @return an envelope whose data is {@code {applied, status, port, portType}} on success.
	 */
	public static McpEnvelope changePortType( String diagramPath, String portName, String portType )
	{
		McpEnvelope resolved = resolveDiagram( diagramPath );
		if ( !resolved.isOk() )
			return resolved;
		Diagram diagram = (Diagram) resolved.getData();
		if ( portName == null || portName.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "a port name is required" );
		if ( portType == null || !( "input".equals( portType ) || "output".equals( portType ) || "contact".equals( portType ) ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS,
					"portType must be one of: input, output, contact (got '" + portType + "')" );
		Node port = (Node) diagram.findNode( portName );
		if ( port == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no port named '" + portName + "' in " + diagramPath );
		if ( !biouml.standard.diagram.Util.isPort( port ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "element is not a port: " + portName );
		List<DataElement> items = new ArrayList<DataElement>();
		items.add( port );
		ChangePortTypeAction action = new ChangePortTypeAction();
		if ( !action.isApplicable( diagram ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "change-port requires a pathway diagram: " + diagramPath );
		ChangePortTypeAction.ChangePortTypeActionParameters params = new ChangePortTypeAction.ChangePortTypeActionParameters( items );
		params.setPortType( portType );
		try
		{
			JobControl jc = action.getJobControl( diagram, items, params );
			jc.run();
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "applied", diagramPath );
			m.put( "status", statusString( jc ) );
			m.put( "port", portName );
			m.put( "portType", portType );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"change-port failed: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	/**
	 * Merge a cloned pathway node back into its original — the headless equivalent of the web UI's
	 * "Merge clone" menu item ({@link MergeCloneAction}). Edges of the clone are redirected to the
	 * original node and the clone is removed (its variable association is dropped). Requires a pathway
	 * diagram and a cloned node whose {@code VariableRole} references a different original element.
	 *
	 * @param diagramPath full path of the pathway diagram
	 * @param cloneName   the name of the clone node to merge away
	 * @return an envelope whose data is {@code {applied, status, merged}} on success.
	 */
	public static McpEnvelope mergeClone( String diagramPath, String cloneName )
	{
		McpEnvelope resolved = resolveDiagram( diagramPath );
		if ( !resolved.isOk() )
			return resolved;
		Diagram diagram = (Diagram) resolved.getData();
		if ( cloneName == null || cloneName.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "a clone node name is required" );
		Node clone = (Node) diagram.findNode( cloneName );
		if ( clone == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no node named '" + cloneName + "' in " + diagramPath );
		if ( !( clone.getRole() instanceof biouml.model.dynamics.VariableRole ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "element is not a variable-role node (clone): " + cloneName );
		List<DataElement> items = new ArrayList<DataElement>();
		items.add( clone );
		MergeCloneAction action = new MergeCloneAction();
		if ( !action.isApplicable( diagram ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "merge-clone requires a pathway diagram: " + diagramPath );
		try
		{
			JobControl jc = action.getJobControl( diagram, items, null );
			jc.run();
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "applied", diagramPath );
			m.put( "status", statusString( jc ) );
			m.put( "merged", cloneName );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"merge-clone failed: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	/**
	 * Save a subset of a diagram's elements to a new diagram — the headless equivalent of the web UI's
	 * "Save subset" menu item ({@link SaveDiagramSubsetAction}). The new diagram is created at
	 * {@code targetCollectionPath}/{@code name} (default: source's collection, name "<diagram> subset")
	 * containing only the selected nodes/edges (plus the compartments/edges that hold them).
	 *
	 * @param diagramPath          full path of the source diagram
	 * @param elementPaths         the nodes/edges to keep (at least one)
	 * @param name                 name of the new diagram (default "<source> subset")
	 * @param targetCollectionPath the collection the new diagram is created in (default: source's)
	 * @return an envelope whose data is {@code {applied, status, path, created, kept}} on success.
	 */
	public static McpEnvelope saveSubset( String diagramPath, String[] elementPaths, String name, String targetCollectionPath )
	{
		McpEnvelope resolved = resolveDiagram( diagramPath );
		if ( !resolved.isOk() )
			return resolved;
		Diagram diagram = (Diagram) resolved.getData();
		McpEnvelope sel = resolveSelection( diagramPath, elementPaths );
		if ( !sel.isOk() )
			return sel;
		String srcPath = diagram.getCompletePath().toString();
		String targetColl = targetCollectionPath == null || targetCollectionPath.isEmpty()
				? srcPath.substring( 0, srcPath.lastIndexOf( '/' ) )
				: targetCollectionPath;
		String subsetName = name == null || name.isEmpty() ? DataElementPath.create( diagram ).getName() + " subset" : name;
		McpEnvelope tc = McpRepositorySupport.resolve( targetColl );
		if ( !tc.isOk() )
			return tc;
		if ( !( (DataElement) tc.getData() instanceof DataCollection ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "target is not a collection: " + targetColl );
		DataElementPath target = DataElementPath.create( (DataCollection<?>) tc.getData() ).getChildPath( subsetName );
		@SuppressWarnings( "unchecked" )
		List<DataElement> items = (List<DataElement>) sel.getData();
		DynamicPropertySet props = new DynamicPropertySetAsMap();
		props.add( new DynamicProperty( "target", DataElementPath.class, target ) );
		SaveDiagramSubsetAction action = new SaveDiagramSubsetAction();
		if ( !action.isApplicable( diagram ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "save-subset is not applicable to this diagram: " + diagramPath );
		try
		{
			JobControl jc = action.getJobControl( diagram, items, props );
			jc.run();
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "applied", diagramPath );
			m.put( "status", statusString( jc ) );
			m.put( "path", target.toString() );
			m.put( "created", Boolean.valueOf( CollectionFactory.getDataElement( target.toString() ) != null ) );
			m.put( "kept", Integer.valueOf( items.size() ) );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"save-subset failed: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	/**
	 * Add upstream/downstream neighbours from a graph search to a diagram — the headless equivalent of
	 * the web UI's "Add upstream elements" / "Add downstream elements" menu items
	 * ({@link AddFromSearchUpAction} / {@link AddFromSearchDownAction}). For each selected element the
	 * action queries the BioHub for linked elements in the given direction and adds them to the diagram.
	 * Requires a BioHub query engine to be configured for the element's source; a missing engine is
	 * reported as a clean {@code invalid_params} envelope rather than a failure.
	 *
	 * @param diagramPath   full path of the diagram to add elements to
	 * @param elementPaths  the nodes to expand (at least one)
	 * @param direction     {@code up} or {@code down}
	 * @return an envelope whose data is {@code {applied, status, direction, count}} on success.
	 */
	public static McpEnvelope addFromSearch( String diagramPath, String[] elementPaths, String direction )
	{
		McpEnvelope resolved = resolveDiagram( diagramPath );
		if ( !resolved.isOk() )
			return resolved;
		Diagram diagram = (Diagram) resolved.getData();
		if ( direction == null || !( "up".equals( direction ) || "down".equals( direction ) ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "direction must be 'up' or 'down' (got '" + direction + "')" );
		McpEnvelope sel = resolveSelection( diagramPath, elementPaths );
		if ( !sel.isOk() )
			return sel;
		BackgroundDynamicAction action = "up".equals( direction ) ? new AddFromSearchUpAction() : new AddFromSearchDownAction();
		if ( !action.isApplicable( diagram ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "add-from-search is not applicable to this diagram: " + diagramPath );
		@SuppressWarnings( "unchecked" )
		List<DataElement> items = (List<DataElement>) sel.getData();
		try
		{
			JobControl jc = action.getJobControl( diagram, items, null );
			jc.run();
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "applied", diagramPath );
			m.put( "status", statusString( jc ) );
			m.put( "direction", direction );
			m.put( "count", Integer.valueOf( items.size() ) );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"add-from-search failed: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}
}
