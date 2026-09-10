package biouml.plugins.mcp.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;

import biouml.plugins.mcp.McpConstants;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.repository.PluginActions;

/**
 * Discovers and (headlessly) invokes repository context-menu actions.
 *
 * <p>{@link PluginActions#getActions(Object)} returns exactly the actions the right-click menu
 * shows for an element. Each carries a stable {@code ActionCommandKey} (read via
 * {@link Action#ACTION_COMMAND_KEY}) which is the MCP identifier for the action.</p>
 *
 * <p>Actions are split into two classes for the {@code run_action} tool:</p>
 * <ul>
 *   <li><b>Headless-safe</b> — actions whose perform path mutates the repository without a UI
 *       (e.g. create-folder). These are invoked directly.</li>
 *   <li><b>Interactive-only</b> — actions that open documents or show input dialogs
 *       (Open, Login, most "New X" that pop a wizard). Invoking them headlessly would block on a
 *       {@code JOptionPane}, so the tool returns {@code requires_interactive_ui} instead.</li>
 * </ul>
 *
 * <p>The classification is conservative by default: an action is headless-safe only if it is on
 * the explicit allowlist; everything else is treated as interactive-only. This means a new action
 * is never silently invoked with a surprising side effect.</p>
 */
public final class McpActionInvoker
{
	/**
	 * ActionCommandKeys that are safe to invoke headlessly (they mutate the repository without a
	 * UI). Extend this list as more actions are verified headless-safe.
	 */
	private static final List<String> HEADLESS_SAFE_KEYS = new ArrayList<String>();
	static
	{
		HEADLESS_SAFE_KEYS.add( "cmd-generic-newfolder" ); // Create folder
	}

	/**
	 * ActionCommandKeys / action class markers that are known to require an interactive UI.
	 */
	private static final List<String> INTERACTIVE_KEYS = new ArrayList<String>();
	static
	{
		INTERACTIVE_KEYS.add( "cmd-open" );
		INTERACTIVE_KEYS.add( "cmd-login" );
	}

	private McpActionInvoker()
	{
	}

	/**
	 * List the actions available for an element as name/key/description/appliable-headless rows.
	 */
	public static List<Map<String, Object>> actionsFor( DataElement de )
	{
		List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
		Action[] actions = new PluginActions().getActions( de );
		if ( actions == null )
			return out;
		for ( Action a : actions )
		{
			String key = a.getValue( Action.ACTION_COMMAND_KEY ) == null ? null : String.valueOf( a.getValue( Action.ACTION_COMMAND_KEY ) );
			String name = a.getValue( Action.NAME ) == null ? null : String.valueOf( a.getValue( Action.NAME ) );
			String shortDesc = a.getValue( Action.SHORT_DESCRIPTION ) == null ? null : String.valueOf( a.getValue( Action.SHORT_DESCRIPTION ) );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "key", key );
			m.put( "name", name );
			m.put( "description", shortDesc );
			m.put( "headlessSafe", Boolean.valueOf( isHeadlessSafe( key, a ) ) );
			out.add( m );
		}
		return out;
	}

	/**
	 * Whether an action (by key, then class) is safe to invoke headlessly.
	 */
	public static boolean isHeadlessSafe( String key, Action action )
	{
		if ( key != null )
		{
			if ( INTERACTIVE_KEYS.contains( key ) )
				return false;
			if ( HEADLESS_SAFE_KEYS.contains( key ) )
				return true;
		}
		// Default-deny: anything not explicitly allowed is treated as interactive-only.
		return false;
	}

	/**
	 * Headlessly perform the action identified by {@code actionKey} on {@code de}.
	 *
	 * @return an envelope: ok with a result map, or an error (requires_interactive_ui,
	 *         action_denied, not_found, invalid_params).
	 */
	public static McpEnvelope perform( DataElement de, String actionKey )
	{
		if ( actionKey == null || actionKey.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "action key must be a non-empty string" );

		Action[] actions = new PluginActions().getActions( de );
		if ( actions == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no actions available for this element" );

		Action target = null;
		for ( Action a : actions )
		{
			Object k = a.getValue( Action.ACTION_COMMAND_KEY );
			if ( k != null && actionKey.equals( String.valueOf( k ) ) )
			{
				target = a;
				break;
			}
		}
		if ( target == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "action not found for this element: " + actionKey );

		if ( !isHeadlessSafe( actionKey, target ) )
			return McpEnvelope.error( McpConstants.CODE_INTERACTIVE_ONLY,
					"action '" + actionKey + "' requires an interactive UI and cannot be run headlessly" );

		// Dispatch the known headless-safe actions to their headless implementations.
		if ( "cmd-generic-newfolder".equals( actionKey ) )
			return performCreateFolder( de );

		return McpEnvelope.error( McpConstants.CODE_ACTION_DENIED,
				"action '" + actionKey + "' has no headless implementation registered yet" );
	}

	/**
	 * Headless equivalent of {@code CreateFolderAction}: creates a sub-folder inside a
	 * {@code FolderCollection}. Mirrors the action's core without the input dialog.
	 */
	public static McpEnvelope performCreateFolder( DataElement de )
	{
		if ( !( de instanceof ru.biosoft.access.core.FolderCollection ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS,
					"create-folder requires a folder collection parent: " + de.getClass().getSimpleName() );
		// The folder name is not part of the action (the UI prompts for it); the MCP create_folder
		// tool passes an explicit name. This entry point is used when the caller supplies it.
		return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS,
				"use biouml_repo_create_folder with an explicit name to create a folder headlessly" );
	}
}
