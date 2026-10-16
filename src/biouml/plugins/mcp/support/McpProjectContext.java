package biouml.plugins.mcp.support;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.SessionCache;
import ru.biosoft.access.security.SessionCacheManager;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.journal.Journal;
import ru.biosoft.journal.JournalRegistry;

/**
 * The "current project" of an MCP session.
 *
 * <p>In the web UI, opening a project makes its journal the session's current journal: analyses are then
 * recorded in the project's Journal, and code that asks {@link JournalRegistry#getProjectPath()} for the
 * current project (workflow intermediate results, report headers, ...) finds it. An MCP client never
 * opens a project, so before running an analysis or an import the session is bound to the project the
 * output goes to.</p>
 */
public final class McpProjectContext
{
	private static final Logger log = Logger.getLogger( McpProjectContext.class.getName() );

	private McpProjectContext()
	{
	}

	/** Bind the session to the project of the first of the analysis' paths that lies in a project (outputs first). */
	public static void useProjectOf( AnalysisParameters parameters )
	{
		for ( DataElementPath path : paths( parameters ) )
			if ( useProjectOf( path ) )
				return;
	}

	/**
	 * Make the journal of the project containing {@code path} the session's current journal.
	 * @return whether {@code path} is in a project whose journal is now current
	 */
	public static boolean useProjectOf( DataElementPath path )
	{
		try
		{
			DataElementPath project = projectOf( path );
			if ( project == null )
				return false;
			SessionCache cache = SessionCacheManager.getSessionCache( ru.biosoft.access.security.SecurityManager.getSession() );
			if ( cache == null )
				return false;
			DataElementPath current = JournalRegistry.getProjectPath();
			if ( current != null && current.getTargetPath().equals( project ) )
				return true;
			String[] names = JournalRegistry.getJournalNames();
			if ( names == null )
				return false;
			for ( String name : names )
			{
				// journal names end with the project name (e.g. "Research: <project>"); check the rest only for those
				if ( name == null || !name.endsWith( project.getName() ) )
					continue;
				Journal journal = JournalRegistry.getCurrentJournal( name );
				DataElementPath journalProject = JournalRegistry.getProjectPath( journal );
				if ( journalProject != null && journalProject.getTargetPath().equals( project ) )
				{
					cache.addObject( JournalRegistry.JOURNAL_NAME_KEY, name, true );
					return true;
				}
			}
		}
		catch ( Exception e )
		{
			log.log( Level.FINE, "could not bind the project of " + path, e );
		}
		return false;
	}

	/** The project (a child of the user projects folder) containing {@code path}, or null. */
	public static DataElementPath projectOf( DataElementPath path )
	{
		if ( path == null )
			return null;
		DataElementPath projects = CollectionFactoryUtils.getUserProjectsPath().getTargetPath();
		DataElementPath target = path.getTargetPath();
		for ( DataElementPath p = target; p != null && !p.isEmpty(); p = p.getParentPath() )
			if ( projects.equals( p.getParentPath() ) )
				return p;
		return null;
	}

	/** The path values of the analysis parameters: output properties first, then the others. */
	private static List<DataElementPath> paths( AnalysisParameters parameters )
	{
		List<DataElementPath> outputs = new ArrayList<DataElementPath>();
		List<DataElementPath> others = new ArrayList<DataElementPath>();
		List<String> outputNames = new ArrayList<String>();
		try
		{
			String[] names = parameters.getOutputNames();
			if ( names != null )
				for ( String n : names )
					outputNames.add( n );
		}
		catch ( Exception e )
		{
			// no output names declared
		}
		collect( ComponentFactory.getModel( parameters, ComponentFactory.Policy.DEFAULT, true ), "", outputNames, outputs, others, 0 );
		outputs.addAll( others );
		return outputs;
	}

	private static void collect( ComponentModel model, String prefix, List<String> outputNames, List<DataElementPath> outputs,
			List<DataElementPath> others, int depth )
	{
		if ( model == null || depth > 3 )
			return;
		for ( int i = 0; i < model.getPropertyCount(); i++ )
		{
			Property p = model.getPropertyAt( i );
			String name = prefix.isEmpty() ? p.getName() : prefix + "/" + p.getName();
			Object value;
			try
			{
				value = p.getValue();
			}
			catch ( Exception e )
			{
				continue;
			}
			if ( value instanceof DataElementPath )
				( outputNames.contains( name ) ? outputs : others ).add( (DataElementPath) value );
			else if ( p instanceof ComponentModel )
				collect( (ComponentModel) p, name, outputNames, outputs, others, depth + 1 );
		}
	}
}
