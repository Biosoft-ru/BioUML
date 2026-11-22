package biouml.plugins.mcp.oauth;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import ru.biosoft.access.security.GlobalDatabaseManager;

/** Persists token digests and an encrypted BioStore login token for each recoverable grant. */
final class OAuthGrantRepository
{
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final int NONCE_BYTES = 12;
	private static volatile boolean initialized;

	record Grant( String refreshHash, String clientId, String user, long expiresAt, byte[] credential, long accessExpiresAt )
	{
	}

	private OAuthGrantRepository()
	{
	}

	static SecretKey deploymentKey()
	{
		String value = System.getenv( "BIOUML_MCP_OAUTH_KEY" );
		if ( value == null || value.isBlank() )
			return null;
		try
		{
			byte[] key = Base64.getDecoder().decode( value );
			if ( key.length == 32 )
				return new SecretKeySpec( key, "AES" );
		}
		catch ( IllegalArgumentException e )
		{
			// Report the configuration error without including the key.
		}
		throw new IllegalStateException( "BIOUML_MCP_OAUTH_KEY must encode 32 bytes in Base64" );
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
			statement.executeUpdate( "CREATE TABLE IF NOT EXISTS mcp_oauth_grants ("
					+ "refresh_hash VARCHAR(43) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,"
					+ "client_id TEXT NOT NULL, user_name TEXT NOT NULL,"
					+ "expires_at BIGINT NOT NULL, credential BLOB NOT NULL,"
					+ "INDEX (expires_at)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4" );
			statement.executeUpdate( "CREATE TABLE IF NOT EXISTS mcp_oauth_access_tokens ("
					+ "access_hash VARCHAR(43) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,"
					+ "refresh_hash VARCHAR(43) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,"
					+ "expires_at BIGINT NOT NULL, INDEX (expires_at),"
					+ "FOREIGN KEY (refresh_hash) REFERENCES mcp_oauth_grants (refresh_hash) ON DELETE CASCADE"
					+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4" );
			initialized = true;
		}
	}

	static void insert( OAuthTokenStore.AccessToken access, OAuthTokenStore.RefreshToken refresh,
			String bioStoreToken, SecretKey key )
	{
		String refreshHash = digest( refresh.value );
		try
		{
			Cipher cipher = Cipher.getInstance( "AES/GCM/NoPadding" );
			byte[] nonce = new byte[NONCE_BYTES];
			RANDOM.nextBytes( nonce );
			cipher.init( Cipher.ENCRYPT_MODE, key, new GCMParameterSpec( 128, nonce ) );
			cipher.updateAAD( binding( refreshHash, refresh.clientId, refresh.user, refresh.expiresAt ) );
			byte[] encrypted = cipher.doFinal( bioStoreToken.getBytes( StandardCharsets.UTF_8 ) );
			Connection connection = connection();
			try ( PreparedStatement cleanup = connection.prepareStatement( "DELETE FROM mcp_oauth_grants WHERE expires_at <= ?" ) )
			{
				cleanup.setLong( 1, System.currentTimeMillis() );
				cleanup.executeUpdate();
			}
			try ( PreparedStatement statement = connection.prepareStatement( "INSERT INTO mcp_oauth_grants "
					+ "(refresh_hash, client_id, user_name, expires_at, credential) VALUES (?, ?, ?, ?, ?)" ) )
			{
				statement.setString( 1, refreshHash );
				statement.setString( 2, refresh.clientId );
				statement.setString( 3, refresh.user );
				statement.setLong( 4, refresh.expiresAt );
				statement.setBytes( 5, ByteBuffer.allocate( nonce.length + encrypted.length ).put( nonce ).put( encrypted ).array() );
				statement.executeUpdate();
			}
			insertAccess( connection, refreshHash, access );
		}
		catch ( SQLException | GeneralSecurityException e )
		{
			throw new IllegalStateException( "Could not save MCP OAuth grant", e );
		}
	}

	static Grant find( String token, boolean refresh )
	{
		String sql = refresh
				? "SELECT refresh_hash, client_id, user_name, expires_at, credential, 0 FROM mcp_oauth_grants "
						+ "WHERE refresh_hash = ? AND expires_at > ?"
				: "SELECT g.refresh_hash, g.client_id, g.user_name, g.expires_at, g.credential, a.expires_at "
						+ "FROM mcp_oauth_access_tokens a JOIN mcp_oauth_grants g ON g.refresh_hash = a.refresh_hash "
						+ "WHERE a.access_hash = ? AND LEAST(a.expires_at, g.expires_at) > ?";
		try ( PreparedStatement statement = connection().prepareStatement( sql ) )
		{
			statement.setString( 1, digest( token ) );
			statement.setLong( 2, System.currentTimeMillis() );
			try ( ResultSet result = statement.executeQuery() )
			{
				return result.next() ? new Grant( result.getString( 1 ), result.getString( 2 ), result.getString( 3 ),
						result.getLong( 4 ), result.getBytes( 5 ), result.getLong( 6 ) ) : null;
			}
		}
		catch ( SQLException e )
		{
			throw new IllegalStateException( "Could not read MCP OAuth grant", e );
		}
	}

	static void insertAccess( String refreshToken, OAuthTokenStore.AccessToken access )
	{
		try
		{
			insertAccess( connection(), digest( refreshToken ), access );
		}
		catch ( SQLException e )
		{
			throw new IllegalStateException( "Could not save refreshed MCP OAuth token", e );
		}
	}

	private static void insertAccess( Connection connection, String refreshHash, OAuthTokenStore.AccessToken access )
			throws SQLException
	{
		try ( PreparedStatement cleanup = connection.prepareStatement(
				"DELETE FROM mcp_oauth_access_tokens WHERE expires_at <= ?" ) )
		{
			cleanup.setLong( 1, System.currentTimeMillis() );
			cleanup.executeUpdate();
		}
		try ( PreparedStatement statement = connection.prepareStatement(
				"INSERT INTO mcp_oauth_access_tokens (access_hash, refresh_hash, expires_at) "
						+ "SELECT ?, refresh_hash, ? FROM mcp_oauth_grants WHERE refresh_hash = ? AND expires_at > ?" ) )
		{
			statement.setString( 1, digest( access.value ) );
			statement.setLong( 2, access.expiresAt );
			statement.setString( 3, refreshHash );
			statement.setLong( 4, System.currentTimeMillis() );
			if ( statement.executeUpdate() != 1 )
				throw new SQLException( "MCP OAuth grant expired or was revoked during refresh" );
		}
	}

	static void revoke( String refreshHash )
	{
		try ( PreparedStatement statement = connection().prepareStatement( "DELETE FROM mcp_oauth_grants WHERE refresh_hash = ?" ) )
		{
			statement.setString( 1, refreshHash );
			statement.executeUpdate();
		}
		catch ( SQLException e )
		{
			throw new IllegalStateException( "Could not revoke MCP OAuth grant", e );
		}
	}

	static String decrypt( Grant grant, SecretKey key ) throws GeneralSecurityException
	{
		ByteBuffer buffer = ByteBuffer.wrap( grant.credential );
		byte[] nonce = new byte[NONCE_BYTES];
		if ( buffer.remaining() < NONCE_BYTES + 16 )
			throw new GeneralSecurityException( "Invalid encrypted grant" );
		buffer.get( nonce );
		byte[] encrypted = new byte[buffer.remaining()];
		buffer.get( encrypted );
		Cipher cipher = Cipher.getInstance( "AES/GCM/NoPadding" );
		cipher.init( Cipher.DECRYPT_MODE, key, new GCMParameterSpec( 128, nonce ) );
		cipher.updateAAD( binding( grant.refreshHash, grant.clientId, grant.user, grant.expiresAt ) );
		return new String( cipher.doFinal( encrypted ), StandardCharsets.UTF_8 );
	}

	static String digest( String token )
	{
		return OAuthTokenStore.pkceChallenge( token );
	}

	private static byte[] binding( String refreshHash, String client, String user, long expiresAt )
	{
		return ( refreshHash + "\n" + client + "\n" + expiresAt + "\n" + user )
				.getBytes( StandardCharsets.UTF_8 );
	}
}
