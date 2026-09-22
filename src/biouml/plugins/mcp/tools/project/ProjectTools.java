package biouml.plugins.mcp.tools.project;

import java.util.Map;

import biouml.plugins.mcp.server.McpToolCatalog;
import biouml.plugins.mcp.support.McpArgs;
import biouml.plugins.mcp.support.McpEnvelope;
import biouml.plugins.mcp.support.McpProjectSupport;

/**
 * Project / user-management MCP tools. Registered into a {@link McpToolCatalog}.
 *
 * <p>These expose the web edition's {@code SupportServlet} convenience operations (create/delete/list
 * projects, project size, and the current user's profile) as MCP-callable actions, run against the
 * authenticated session. Each handler validates its arguments at the boundary and returns a structured
 * envelope — never a raw stack trace.</p>
 */
public final class ProjectTools
{
	private ProjectTools()
	{
	}

	public static void registerAll( McpToolCatalog catalog )
	{
		catalog.register( "biouml_project_list",
				"List the current user's projects with their permissions (admin/canWrite/canDelete) and disk quota.",
				"{}",
				( ex, args ) -> McpProjectSupport.projects() );

		catalog.register( "biouml_project_create",
				"Create a new project (a SQL-backed research collection) for the current user. The name may only contain latin letters, numbers, spaces and a few symbols.",
				"{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\",\"description\":\"Project name\"}},\"required\":[\"name\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "name" );
					if ( v != null )
						return v;
					return McpProjectSupport.create( McpArgs.str( args, "name" ) );
				} );

		catalog.register( "biouml_project_delete",
				"Delete a project (and its data) for the current user. Requires delete permission.",
				"{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\",\"description\":\"Project name\"}},\"required\":[\"name\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "name" );
					if ( v != null )
						return v;
					return McpProjectSupport.delete( McpArgs.str( args, "name" ) );
				} );

		catalog.register( "biouml_project_size",
				"Report the disk usage (in bytes) of a single project's data folder.",
				"{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\",\"description\":\"Project name\"}},\"required\":[\"name\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "name" );
					if ( v != null )
						return v;
					return McpProjectSupport.projectSize( McpArgs.str( args, "name" ) );
				} );

		catalog.register( "biouml_user_info",
				"Return the current user's profile (username and profile fields) from the security provider.",
				"{}",
				( ex, args ) -> McpProjectSupport.userInfo() );

		catalog.register( "biouml_user_change_password",
				"Change the current user's password. Requires the current (old) password and a new one.",
				"{\"type\":\"object\",\"properties\":{\"oldPassword\":{\"type\":\"string\"},\"newPassword\":{\"type\":\"string\"}},\"required\":[\"oldPassword\",\"newPassword\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "oldPassword" );
					if ( v != null )
						return v;
					McpEnvelope w = McpArgs.requiredString( args, "newPassword" );
					if ( w != null )
						return w;
					return McpProjectSupport.changePassword( McpArgs.str( args, "oldPassword" ), McpArgs.str( args, "newPassword" ) );
				} );

		catalog.register( "biouml_user_change_info",
				"Update the current user's profile fields (e.g. name, email) in the security provider. Pass a flat object of field name -> value.",
				"{\"type\":\"object\",\"properties\":{\"fields\":{\"type\":\"object\",\"description\":\"Profile fields to update (e.g. email, name)\"}},\"required\":[\"fields\"]}",
				( ex, args ) -> {
					Object f = args.get( "fields" );
					if ( !( f instanceof Map ) )
						return McpEnvelope.error( "invalid_params", "fields must be an object" );
					@SuppressWarnings( "unchecked" )
					Map<String, Object> fields = (Map<String, Object>) f;
					return McpProjectSupport.changeInfo( fields );
				} );
	}
}
