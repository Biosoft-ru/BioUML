package biouml.plugins.mcp.support;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import biouml.model.Module;
import biouml.plugins.mcp.McpConstants;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.generic.GenericDataCollection;
import ru.biosoft.access.security.Permission;
import ru.biosoft.access.security.SecurityAdminUtils;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.server.servlets.support.SupportServlet;

/**
 * Project / user-management operations exposed as MCP tools, mirroring the convenience methods of the
 * web edition's {@code SupportServlet} ({@code /biouml/support/...}). These run against the
 * authenticated session's projects (the same user BioStore authenticated via the {@code Authorization}
 * header), so a BioStore-backed deployment is required for the mutating operations.
 *
 * <p>Each method returns an {@link McpEnvelope} — a success payload or a structured error (no stack
 * traces leak to the client).</p>
 */
public final class McpProjectSupport
{
	private McpProjectSupport()
	{
	}

	/**
	 * A project name is valid when it matches the web UI's rule (leading letter/digit, then letters,
	 * digits, spaces and a small set of punctuation).
	 */
	public static boolean isProjectNameValid( String name )
	{
		return name != null && name.matches( "[a-zA-Z0-9][a-zA-Z0-9_\\-\\.\\,@\\(\\)\\s]+" );
	}

	/** The parent collection that holds the current user's projects. */
	public static DataCollection userProjectsParent()
	{
		return CollectionFactoryUtils.getUserProjectsPath().getDataCollection();
	}

	/**
	 * List the current user's projects with their permissions and quota — the same data the web edition's
	 * {@code getProjectsData} returns.
	 */
	public static McpEnvelope projects()
	{
		DataCollection parent;
		try
		{
			parent = userProjectsParent();
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "cannot resolve the user projects path: " + e.getMessage() );
		}
		List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
		for ( Object obj : parent )
		{
			if ( !( obj instanceof Module ) )
				continue;
			Module module = (Module) obj;
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "name", module.getName() );
			String desc = null;
			try
			{
				desc = module.getDescriptionHTML();
			}
			catch ( Exception ignore )
			{
			}
			if ( desc != null && !desc.isEmpty() )
				m.put( "description", desc );
			try
			{
				Permission p = SecurityManager.getPermissions( module.getCompletePath() );
				m.put( "admin", p.isAdminAllowed() );
				m.put( "canWrite", p.isWriteAllowed() );
				m.put( "canDelete", p.isDeleteAllowed() );
			}
			catch ( Exception ignore )
			{
			}
			try
			{
				String quota = module.getInfo().getProperty( DataCollectionConfigConstants.DISK_QUOTA_PROPERTY );
				if ( quota != null )
					m.put( "quota", Long.parseLong( quota ) );
			}
			catch ( Exception ignore )
			{
			}
			out.add( m );
		}
		return McpEnvelope.ok( out );
	}

	/**
	 * Create a new project (a SQL-backed research collection) for the current user. Mirrors
	 * {@code SupportServlet.createProject}.
	 */
	public static McpEnvelope create( String name )
	{
		if ( name == null || name.trim().isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "project name is required" );
		if ( !isProjectNameValid( name ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS,
					"project name contains unacceptable characters (only latin letters, numbers, spaces and a few symbols)" );
		DataCollection parent;
		try
		{
			parent = userProjectsParent();
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "cannot resolve the user projects path: " + e.getMessage() );
		}
		if ( parent.contains( name ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "project '" + name + "' already exists" );
		List<String> errors = new ArrayList<String>();
		try
		{
			DataCollection created = SupportServlet.createNewProject( name, null, false, errors );
			if ( created == null )
				return McpEnvelope.error( McpConstants.CODE_INTERNAL,
						"cannot create project" + ( errors.size() > 0 ? ": " + errors.get( 0 ) : "" ) );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "cannot create project: " + e.getMessage() );
		}
		Map<String, Object> data = new LinkedHashMap<String, Object>();
		data.put( "name", name );
		data.put( "path", parent.getCompletePath().getName() + "/" + name );
		return McpEnvelope.ok( data );
	}

	/**
	 * Delete a project for the current user. Mirrors {@code SupportServlet.deleteProject}.
	 */
	public static McpEnvelope delete( String name )
	{
		if ( name == null || name.trim().isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "project name is required" );
		DataCollection parent;
		try
		{
			parent = userProjectsParent();
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "cannot resolve the user projects path: " + e.getMessage() );
		}
		if ( !parent.contains( name ) )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "project '" + name + "' does not exist" );
		try
		{
			String error = SupportServlet.deleteProject( name, parent );
			if ( error != null )
				return McpEnvelope.error( McpConstants.CODE_INTERNAL, error );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "cannot delete project: " + e.getMessage() );
		}
		return McpEnvelope.ok( null );
	}

	/**
	 * Disk usage (bytes) of a single project's Data folder, computed the same way the web edition does
	 * (primary collection cast to {@link GenericDataCollection}, then {@code getDiskSize()}).
	 */
	public static McpEnvelope projectSize( String name )
	{
		if ( name == null || name.trim().isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "project name is required" );
		DataCollection parent = userProjectsParent();
		if ( !parent.contains( name ) )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "project '" + name + "' does not exist" );
		try
		{
			Object projectEl = parent.get( name );
			if ( !( projectEl instanceof DataCollection ) )
				return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "project '" + name + "' is not a collection" );
			DataCollection project = (DataCollection) projectEl;
			Object dataEl = project.get( Module.DATA );
			if ( !( dataEl instanceof DataCollection ) )
				return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "project '" + name + "' has no Data folder" );
			DataCollection<?> data = (DataCollection<?>) dataEl;
			Object primaryObj = SecurityManager.runPrivileged( () -> DataCollectionUtils.fetchPrimaryCollectionPrivileged( data ) );
			if ( !( primaryObj instanceof DataCollection ) )
				return McpEnvelope.error( McpConstants.CODE_INTERNAL, "cannot resolve the project's primary collection" );
			GenericDataCollection gdc = ( (DataCollection<?>) primaryObj ).cast( GenericDataCollection.class );
			long size = gdc.getDiskSize();
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "project", name );
			m.put( "size", size );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "cannot compute project size: " + e.getMessage() );
		}
	}

	/**
	 * The current user's profile (name + profile fields) from the security provider.
	 */
	public static McpEnvelope userInfo()
	{
		try
		{
			String user = SecurityManager.getSessionUser();
			if ( user == null || user.isEmpty() )
				return McpEnvelope.error( McpConstants.CODE_INTERNAL, "no authenticated user" );
			String password = SecurityManager.getCurrentUserPermission() == null ? null
					: SecurityManager.getCurrentUserPermission().getPassword();
			Map<String, Object> userInfo = SecurityManager.getSecurityProvider().getUserInfo( user, password );
			Map<String, Object> result = new LinkedHashMap<String, Object>();
			result.put( "username", user );
			if ( userInfo != null )
				result.putAll( userInfo );
			return McpEnvelope.ok( result );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "cannot read user info: " + e.getMessage() );
		}
	}

	/**
	 * Change the current user's password (requires the old password).
	 */
	public static McpEnvelope changePassword( String oldPassword, String newPassword )
	{
		if ( newPassword == null || newPassword.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "new password is required" );
		try
		{
			String user = SecurityManager.getSessionUser();
			if ( user == null || user.isEmpty() )
				return McpEnvelope.error( McpConstants.CODE_INTERNAL, "no authenticated user" );
			boolean ok = SecurityAdminUtils.changeUserPassword( user, oldPassword, newPassword );
			return ok ? McpEnvelope.ok( null ) : McpEnvelope.error( McpConstants.CODE_INTERNAL, "cannot change password" );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "cannot change password: " + e.getMessage() );
		}
	}

	/**
	 * Update the current user's profile fields (e.g. name, email) in the security provider.
	 */
	public static McpEnvelope changeInfo( Map<String, Object> fields )
	{
		if ( fields == null || fields.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "at least one field is required" );
		try
		{
			String user = SecurityManager.getSessionUser();
			if ( user == null || user.isEmpty() )
				return McpEnvelope.error( McpConstants.CODE_INTERNAL, "no authenticated user" );
			Map<String, String> params = new LinkedHashMap<String, String>();
			for ( Map.Entry<String, Object> e : fields.entrySet() )
				params.put( e.getKey(), e.getValue() == null ? "" : String.valueOf( e.getValue() ) );
			StringWriter sw = new StringWriter();
			boolean ok = SecurityAdminUtils.updateUserInfo( user, "", params, sw );
			String err = sw.toString();
			return ok ? McpEnvelope.ok( null ) : McpEnvelope.error( McpConstants.CODE_INTERNAL,
					!err.isEmpty() ? err : "cannot update user info" );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "cannot update user info: " + e.getMessage() );
		}
	}
}
