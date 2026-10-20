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
		catalog.registerHidden( "biouml_project_list",
				"{}",
				( ex, args ) -> McpProjectSupport.projects() );

		catalog.registerHidden( "biouml_project_create",
				"{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\",\"description\":\"Project name\"}},\"required\":[\"name\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "name" );
					if ( v != null )
						return v;
					return McpProjectSupport.create( McpArgs.str( args, "name" ) );
				} );

		catalog.registerHidden( "biouml_project_delete",
				"{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\",\"description\":\"Project name\"}},\"required\":[\"name\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "name" );
					if ( v != null )
						return v;
					return McpProjectSupport.delete( McpArgs.str( args, "name" ) );
				} );

		catalog.registerHidden( "biouml_project_size",
				"{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\",\"description\":\"Project name\"}},\"required\":[\"name\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "name" );
					if ( v != null )
						return v;
					return McpProjectSupport.projectSize( McpArgs.str( args, "name" ) );
				} );

		catalog.registerHidden( "biouml_user_info",
				"{}",
				( ex, args ) -> McpProjectSupport.userInfo() );

		catalog.registerHidden( "biouml_user_change_password",
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

		catalog.registerHidden( "biouml_user_change_info",
				"{\"type\":\"object\",\"properties\":{\"fields\":{\"type\":\"object\",\"description\":\"Profile fields to update (e.g. email, name)\"}},\"required\":[\"fields\"]}",
				( ex, args ) -> {
					Object f = args.get( "fields" );
					if ( !( f instanceof Map ) )
						return McpEnvelope.error( "invalid_params", "fields must be an object" );
					@SuppressWarnings( "unchecked" )
					Map<String, Object> fields = (Map<String, Object>) f;
					return McpProjectSupport.changeInfo( fields );
				} );

		catalog.registerOps( "biouml_project",
				"The current user's projects. op list: the projects with their permissions (admin/canWrite/canDelete), disk quota and the properties the web UI reads (e.g. demo: \"true\" for a Genome Enhancer demo project, an example with finished results). op create: create the project name (a SQL-backed research collection; latin letters, numbers, spaces and a few symbols). op delete: delete the project name with its data (needs delete permission). op size: the disk usage in bytes of the project's data folder. The user may work in the BioUML web UI in a separate session at the same time, so projects can be created, renamed, shared or deleted outside this conversation and an earlier list may be out of date: when a project is not found, re-read the list with op list (it reloads the session's permissions) before concluding it does not exist.",
				"{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\",\"description\":\"Project name\"}}}",
				null,
				McpToolCatalog.op( "list", "biouml_project_list" ),
				McpToolCatalog.op( "create", "biouml_project_create" ),
				McpToolCatalog.op( "delete", "biouml_project_delete" ),
				McpToolCatalog.op( "size", "biouml_project_size" ) );

		catalog.registerOps( "biouml_user",
				"The current user's account. op info: the profile (username and profile fields). op change_password: change the password from oldPassword to newPassword. op change_info: update the profile fields given as a flat object fields (e.g. name, email).",
				"{\"type\":\"object\",\"properties\":{\"oldPassword\":{\"type\":\"string\"},\"newPassword\":{\"type\":\"string\"},\"fields\":{\"type\":\"object\",\"description\":\"Profile fields to update (e.g. email, name)\"}}}",
				null,
				McpToolCatalog.op( "info", "biouml_user_info" ),
				McpToolCatalog.op( "change_password", "biouml_user_change_password" ),
				McpToolCatalog.op( "change_info", "biouml_user_change_info" ) );
	}
}
