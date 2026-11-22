package biouml.plugins.mcp.oauth;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import biouml.plugins.mcp.oauth.OAuthTokenStore.RegisteredClient;
import ru.biosoft.access.security.GlobalDatabaseManager;

/** Durable dynamic client registrations in the BioUML support database. */
final class OAuthClientRepository
{
	private static final ObjectMapper JSON = new ObjectMapper();
	private static volatile boolean initialized;

	private OAuthClientRepository()
	{
	}

	private static Connection connection() throws SQLException
	{
		Connection connection = GlobalDatabaseManager.getDatabaseConnection();
		if ( !initialized )
			initialize( connection );
		return connection;
	}

	private static synchronized void initialize( Connection connection ) throws SQLException
	{
		if ( initialized )
			return;
		try ( Statement statement = connection.createStatement() )
		{
			statement.executeUpdate( "CREATE TABLE IF NOT EXISTS mcp_oauth_clients ("
					+ "client_id VARCHAR(39) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,"
					+ "redirect_uris MEDIUMTEXT NOT NULL, client_name TEXT NOT NULL,"
					+ "secret_hash VARCHAR(43) CHARACTER SET ascii) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4" );
			initialized = true;
		}
	}

	static void insert( RegisteredClient client )
	{
		try ( PreparedStatement statement = connection().prepareStatement(
				"INSERT INTO mcp_oauth_clients (client_id, redirect_uris, client_name, secret_hash) VALUES (?, ?, ?, ?)" ) )
		{
			statement.setString( 1, client.clientId );
			statement.setString( 2, JSON.writeValueAsString( client.redirectUris ) );
			statement.setString( 3, client.clientName );
			statement.setString( 4, client.secretHash );
			statement.executeUpdate();
		}
		catch ( SQLException | IOException e )
		{
			throw new IllegalStateException( "Could not save MCP OAuth client registration", e );
		}
	}

	static RegisteredClient find( String clientId )
	{
		try ( PreparedStatement statement = connection().prepareStatement(
				"SELECT redirect_uris, client_name, secret_hash FROM mcp_oauth_clients WHERE client_id = ?" ) )
		{
			statement.setString( 1, clientId );
			try ( ResultSet result = statement.executeQuery() )
			{
				if ( !result.next() )
					return null;
				return new RegisteredClient( clientId, List.of( JSON.readValue( result.getString( 1 ), String[].class ) ),
						result.getString( 2 ), result.getString( 3 ) );
			}
		}
		catch ( SQLException | IOException e )
		{
			throw new IllegalStateException( "Could not read MCP OAuth client registration", e );
		}
	}
}
