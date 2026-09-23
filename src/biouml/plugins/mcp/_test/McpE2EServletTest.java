package biouml.plugins.mcp._test;

import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import com.fasterxml.jackson.databind.ObjectMapper;

import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.Repository;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.generic.GenericDataCollection;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.TempFiles;

import biouml.plugins.mcp.server.BioumlMcpServer;
import biouml.plugins.mcp.server.McpToolCatalog;
import biouml.plugins.mcp.server.McpServerFactory;
import biouml.plugins.mcp.support.McpEnvelope;
import biouml.plugins.mcp.web.McpServlet;
import biouml.plugins.mcp.web.McpServlet.HandleResult;

/**
 * Phase-6 end-to-end + hardening test.
 *
 * <p>Covers the full MCP client journey over a <b>real socket</b> (initialize → tools/list →
 * collections → analysis_list → create-folder → re-list verification) — the raw-HTTP e2e the phase
 * spec makes mandatory — plus the hardening assertions: malformed input (unknown tool, wrong arg
 * type, missing required arg) → structured errors with no stack-trace frames; path-escape →
 * structured error with no file read; a denied (interactive) action → clear denial; truncation on a
 * &gt;cap collection → {@code truncated:true}; and performance bounds (tools/list &lt; 500ms,
 * analysis_list &lt; 2s). It drives the servlet's real protocol core
 * ({@link McpServlet#handle}) both over the wire and directly.</p>
 *
 * <p>The fixture is a temp-dir {@link LocalRepository} (same pattern as {@code McpRepositoryToolsTest})
 * plus the {@link McpTestAnalysis} stub registered in the analysis registry, so {@code analysis_list}
 * returns a non-empty list without a MySQL-backed repository.</p>
 */
public class McpE2EServletTest extends AbstractBioUMLTest
{
	private final ObjectMapper mapper = new ObjectMapper();
	private File dir;
	private Repository repository;
	private McpServlet servlet;
	private BioumlMcpServer inProcess;
	/** A live session with a logged-in user (created via anonymousLogin under a non-system id). */
	private static final String SYS = "mcp-e2e-auth-session";

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		dir = TempFiles.dir( "mcpE2ETest" );

		Properties properties = new ExProperties();
		properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, "mcpdata" );
		properties.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName() );
		properties.setProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, dir.toString() );
		ExProperties.store( properties, new File( dir, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE ) );
		repository = (Repository) CollectionFactory.createCollection( null, properties );
		CollectionFactory.registerRoot( repository );
		repository.getNameList();

		// A folder collection so create-folder works, and a big collection for the truncation test.
		DataCollection<?> folder = GenericDataCollection.createGenericCollection( repository, repository, "projects", "projects" );
		repository.put( folder );
		DataCollection<?> big = GenericDataCollection.createGenericCollection( repository, repository, "big", "big" );
		repository.put( big );
		for ( int i = 0; i < 150; i++ )
			((DataCollection<DataElement>) big).put( new ru.biosoft.access.core.TextDataElement( "item" + i, big, "x" ) );

		// Register the analysis stub so analysis_list is non-empty.
		AnalysisMethodRegistry.getAnalysisGroups().findAny();
		AnalysisMethodRegistry.addMethodToGroup( McpTestAnalysis.NAME, "McpE2ETest",
				new ru.biosoft.analysiscore.AnalysisMethodInfo( McpTestAnalysis.NAME, "Test stub analysis", null, McpTestAnalysis.class ) );

		// Create a real, live session bound to a user under a NON-system id (mirrors /login). The
		// `system` session is intentionally not used: it has no user record by design.
		SecurityManager.addThreadToSessionRecord( Thread.currentThread(), SYS );
		SecurityManager.anonymousLogin();
		servlet = new McpServlet();
		servlet.initForTest();
		inProcess = BioumlMcpServer.create();
	}

	@Override
	protected void tearDown() throws Exception
	{
		try
		{
			if ( repository != null )
				CollectionFactory.unregisterRoot( repository );
		}
		catch ( Exception ignore )
		{
		}
		super.tearDown();
	}

	@SuppressWarnings( "unchecked" )
	private Map<String, Object> parse( String json ) throws Exception
	{
		return json == null || json.isEmpty() ? new java.util.LinkedHashMap<String, Object>()
				: (Map<String, Object>) mapper.readValue( json, Map.class );
	}

	/** Call a tool directly (in-process) and return the envelope map (unwrapped from the result text). */
	@SuppressWarnings( "unchecked" )
	private Map<String, Object> callTool( String name, Map<String, Object> args ) throws Exception
	{
		Map<String, Object> req = new java.util.LinkedHashMap<String, Object>();
		req.put( "jsonrpc", "2.0" );
		req.put( "id", 1 );
		req.put( "method", "tools/call" );
		Map<String, Object> params = new java.util.LinkedHashMap<String, Object>();
		params.put( "name", name );
		params.put( "arguments", args );
		req.put( "params", params );
		String out = inProcess.handle( req );
		Map<String, Object> resp = parse( out );
		Map<String, Object> result = (Map<String, Object>) resp.get( "result" );
		if ( result == null )
			return resp;
		List<Map<String, Object>> content = (List<Map<String, Object>>) result.get( "content" );
		String text = (String) content.get( 0 ).get( "text" );
		return parse( text );
	}

	private Map<String, Object> args( Object... kv )
	{
		Map<String, Object> m = new java.util.LinkedHashMap<String, Object>();
		for ( int i = 0; i + 1 < kv.length; i += 2 )
			m.put( (String) kv[ i ], kv[ i + 1 ] );
		return m;
	}

	// =================================================================== E2E (real socket)

	/**
	 * The full client journey over a real socket: initialize → tools/list (≥10) → collections →
	 * analysis_list → create-folder → re-list verification.
	 */
	@SuppressWarnings( "unchecked" )
	public void testEndToEndOverRealHttp() throws Exception
	{
		HttpMcpServer server = new HttpMcpServer( servlet );
		try
		{
			// 1) initialize
			Map<String, Object> init = server.post( SYS,
					"{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-03-26\",\"capabilities\":{},\"clientInfo\":{\"name\":\"e2e\",\"version\":\"0\"}}}" );
			assertEquals( "initialize → 200", "200", init.get( "status" ).toString() );
			Map<String, Object> initResult = (Map<String, Object>) parse( (String) init.get( "body" ) ).get( "result" );
			assertNotNull( "initialize result", initResult );
			assertEquals( "protocol 2025-11-25", "2025-11-25", initResult.get( "protocolVersion" ) );

			// 2) tools/list — ≥10 tools
			Map<String, Object> list = server.post( SYS,
					"{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}" );
			assertEquals( "tools/list → 200", "200", list.get( "status" ).toString() );
			List<Map<String, Object>> tools = (List<Map<String, Object>>) ( (Map<String, Object>) parse( (String) list.get( "body" ) ).get( "result" ) ).get( "tools" );
			assertTrue( "≥10 tools, was " + tools.size(), tools.size() >= 10 );

			// 3) tools/call collections
			Map<String, Object> coll = server.post( SYS,
					"{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"biouml_repo_collections\",\"arguments\":{}}}" );
			assertEquals( "collections → 200", "200", coll.get( "status" ).toString() );
			Map<String, Object> collEnv = parse( toolText( coll ) );
			assertEquals( "collections ok", Boolean.TRUE, collEnv.get( "ok" ) );
			Map<String, Object> collData = (Map<String, Object>) collEnv.get( "data" );
			assertTrue( "collections non-empty", ((Number) collData.get( "count" )).intValue() >= 1 );

			// 4) tools/call analysis_list
			Map<String, Object> al = server.post( SYS,
					"{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/call\",\"params\":{\"name\":\"biouml_analysis_list\",\"arguments\":{}}}" );
			assertEquals( "analysis_list → 200", "200", al.get( "status" ).toString() );
			Map<String, Object> alEnv = parse( toolText( al ) );
			assertEquals( "analysis_list ok", Boolean.TRUE, alEnv.get( "ok" ) );

			// 5) mutating: create-folder, then 6) re-list to verify
			Map<String, Object> create = server.post( SYS,
					"{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"tools/call\",\"params\":{\"name\":\"biouml_repo_create_folder\",\"arguments\":{\"parentPath\":\"mcpdata/projects\",\"name\":\"e2eFolder\"}}}" );
			assertEquals( "create-folder → 200", "200", create.get( "status" ).toString() );
			Map<String, Object> createEnv = parse( toolText( create ) );
			assertEquals( "create-folder ok", Boolean.TRUE, createEnv.get( "ok" ) );

			Map<String, Object> relist = server.post( SYS,
					"{\"jsonrpc\":\"2.0\",\"id\":6,\"method\":\"tools/call\",\"params\":{\"name\":\"biouml_repo_list\",\"arguments\":{\"path\":\"mcpdata/projects\"}}}" );
			assertEquals( "re-list → 200", "200", relist.get( "status" ).toString() );
			Map<String, Object> relistEnv = parse( toolText( relist ) );
			assertEquals( "re-list ok", Boolean.TRUE, relistEnv.get( "ok" ) );
			Map<String, Object> relistData = (Map<String, Object>) relistEnv.get( "data" );
			List<Map<String, Object>> children = (List<Map<String, Object>>) relistData.get( "children" );
			boolean found = false;
			for ( Map<String, Object> c : children )
				if ( "e2eFolder".equals( c.get( "name" ) ) )
					found = true;
			assertTrue( "created folder appears in re-list", found );
		}
		finally
		{
			server.stop();
		}
	}

	@SuppressWarnings( "unchecked" )
	private String toolText( Map<String, Object> resp ) throws Exception
	{
		Map<String, Object> result = (Map<String, Object>) parse( (String) resp.get( "body" ) ).get( "result" );
		List<Map<String, Object>> content = (List<Map<String, Object>>) result.get( "content" );
		return (String) content.get( 0 ).get( "text" );
	}

	// =================================================================== hardening

	/** #3: three distinct bad calls → structured errors, no stack-trace frames. */
	public void testMalformedInputStructuredErrors() throws Exception
	{
		// (a) unknown tool
		HandleResult r1 = post( SYS,
				"{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"biouml_does_not_exist\",\"arguments\":{}}}" );
		assertEquals( "unknown tool → 200", 200, r1.status );
		Map<String, Object> err1 = (Map<String, Object>) parse( r1.body ).get( "error" );
		assertNotNull( "unknown tool has JSON-RPC error", err1 );
		assertEquals( "-32601", -32601, ( (Number) err1.get( "code" ) ).intValue() );
		assertNoStackTrace( r1.body );

		// (b) wrong arg type (path must be a string)
		Map<String, Object> env2 = callTool( "biouml_repo_list", args( "path", 123 ) );
		assertEquals( "wrong type → ok:false", Boolean.FALSE, env2.get( "ok" ) );
		assertEquals( "invalid_params code", "invalid_params", env2.get( "code" ) );
		assertNoStackTrace( mapper.writeValueAsString( env2 ) );

		// (c) missing required arg
		Map<String, Object> env3 = callTool( "biouml_repo_list", args() );
		assertEquals( "missing arg → ok:false", Boolean.FALSE, env3.get( "ok" ) );
		assertEquals( "invalid_params code", "invalid_params", env3.get( "code" ) );
	}

	/** #4: path-escape attempt → structured error, no file outside the roots is read. */
	@SuppressWarnings( "unchecked" )
	public void testPathEscapeDenied() throws Exception
	{
		Map<String, Object> env = callTool( "biouml_repo_describe", args( "path", "../../etc/passwd" ) );
		assertEquals( "path-escape → ok:false", Boolean.FALSE, env.get( "ok" ) );
		assertEquals( "path_escape code", "path_escape", env.get( "code" ) );
		// The error is a structural denial (the path doesn't start with a registered root) — it must
		// not contain the *contents* of any file. (The message may echo the rejected path string.)
		String msg = String.valueOf( env.get( "error" ) );
		assertTrue( "message is a path-escape denial: " + msg,
				msg.contains( "path" ) && msg.contains( "root" ) );
		String serialized = mapper.writeValueAsString( env );
		assertNoStackTrace( serialized );
	}

	/** #5: a denied (interactive) action via run_action → clear denial error. */
	@SuppressWarnings( "unchecked" )
	public void testDeniedAction() throws Exception
	{
		// "cmd-open" is on the INTERACTIVE_KEYS list → denied.
		Map<String, Object> env = callTool( "biouml_repo_run_action",
				args( "path", "mcpdata/projects", "action", "cmd-open" ) );
		assertEquals( "denied action → ok:false", Boolean.FALSE, env.get( "ok" ) );
		Object code = env.get( "code" );
		assertTrue( "denial code, was " + code,
				"requires_interactive_ui".equals( code ) || "action_denied".equals( code ) || "not_found".equals( code ) );
		assertNoStackTrace( mapper.writeValueAsString( env ) );
	}

	/** #6: listing a >cap collection → truncated:true. */
	@SuppressWarnings( "unchecked" )
	public void testTruncation() throws Exception
	{
		Map<String, Object> env = callTool( "biouml_repo_list", args( "path", "mcpdata/big" ) );
		assertEquals( "list big → ok", Boolean.TRUE, env.get( "ok" ) );
		Map<String, Object> data = (Map<String, Object>) env.get( "data" );
		assertEquals( "truncated:true", Boolean.TRUE, data.get( "truncated" ) );
		assertTrue( "count > cap", ((Number) data.get( "count" )).intValue() > 100 );
		List<Map<String, Object>> children = (List<Map<String, Object>>) data.get( "children" );
		assertTrue( "returned ≤ cap", children.size() <= 100 );
	}

	/** #7: performance bounds on the fixture. */
	@SuppressWarnings( "unchecked" )
	public void testPerformance() throws Exception
	{
		// tools/list < 500ms
		long t0 = System.currentTimeMillis();
		HandleResult list = post( SYS, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\",\"params\":{}}" );
		long listMs = System.currentTimeMillis() - t0;
		assertEquals( "tools/list → 200", 200, list.status );
		List<Map<String, Object>> tools = (List<Map<String, Object>>) ( (Map<String, Object>) parse( list.body ).get( "result" ) ).get( "tools" );
		assertTrue( "tools/list returned tools", tools.size() >= 10 );
		System.out.println( "### PERF tools/list: " + listMs + "ms (limit 500ms)" );
		assertTrue( "tools/list < 500ms, was " + listMs, listMs < 500 );

		// analysis_list < 2s
		long t1 = System.currentTimeMillis();
		Map<String, Object> env = callTool( "biouml_analysis_list", args() );
		long alMs = System.currentTimeMillis() - t1;
		assertEquals( "analysis_list ok", Boolean.TRUE, env.get( "ok" ) );
		System.out.println( "### PERF analysis_list: " + alMs + "ms (limit 2000ms)" );
		assertTrue( "analysis_list < 2s, was " + alMs, alMs < 2000 );
	}

	/** Assert the response body contains no Java stack-trace frames. */
	private static void assertNoStackTrace( String body )
	{
		assertFalse( "no 'at com.' frames: " + snippet( body ), body != null && body.contains( "at com." ) );
		assertFalse( "no 'at ru.' frames: " + snippet( body ), body != null && body.contains( "at ru." ) );
		assertFalse( "no 'Exception' class leak: " + snippet( body ), body != null && body.contains( "Exception" ) );
	}

	private static String snippet( String s )
	{
		return s == null ? "null" : ( s.length() > 200 ? s.substring( 0, 200 ) : s );
	}

	/** Bind the thread to the session and call the servlet's real handle() for a POST. */
	private HandleResult post( String sessionId, String jsonBody )
	{
		SecurityManager.addThreadToSessionRecord( Thread.currentThread(), sessionId );
		return servlet.handle( "POST", "/mcp", SecurityManager.SESSION_ID + "=" + sessionId, jsonBody, null );
	}

	// =================================================================== real-socket helper (mirrors McpServletTest)

	private static final class HttpMcpServer
	{
		private final McpServlet servlet;
		private final ServerSocket serverSocket;
		private final int port;
		private Thread acceptor;
		private volatile boolean running = true;

		HttpMcpServer( McpServlet servlet ) throws Exception
		{
			this.servlet = servlet;
			Random random = new Random();
			ServerSocket sock = null;
			int port = -1;
			for ( int i = 0; i < 100; i++ )
			{
				int candidate = random.nextInt( 25000 - 20000 ) + 20000;
				try
				{
					sock = new ServerSocket( candidate, 50, InetAddress.getByName( "127.0.0.1" ) );
					port = candidate;
					break;
				}
				catch ( Exception ignore )
				{
				}
			}
			if ( sock == null )
				throw new Exception( "Unable to start MCP E2E HTTP test server" );
			this.port = port;
			this.serverSocket = sock;
			this.acceptor = new AcceptorThread();
			this.acceptor.setDaemon( true );
			this.acceptor.start();
		}

		@SuppressWarnings( "unchecked" )
		Map<String, Object> post( String sessionId, String body ) throws Exception
		{
			byte[] bodyBytes = body.getBytes( StandardCharsets.UTF_8 );
			String raw = "POST /mcp?" + SecurityManager.SESSION_ID + "=" + sessionId + " HTTP/1.1\r\n"
					+ "Host: 127.0.0.1:" + port + "\r\n"
					+ "Content-Type: application/json\r\n"
					+ "Content-Length: " + bodyBytes.length + "\r\n"
					+ "Connection: close\r\n"
					+ "\r\n"
					+ new String( bodyBytes, StandardCharsets.UTF_8 );

			try (Socket socket = new Socket( "127.0.0.1", port ))
			{
				socket.setSoTimeout( 20000 );
				OutputStream out = socket.getOutputStream();
				out.write( raw.getBytes( StandardCharsets.UTF_8 ) );
				out.flush();

				DataInputStream in = new DataInputStream( socket.getInputStream() );
				String text = readHttpRequest( in );
				String[] statusParts = text.split( "\r\n", 2 )[ 0 ].split( " " );
				String status = statusParts.length > 1 ? statusParts[ 1 ] : "?";
				int headerEnd = text.indexOf( "\r\n\r\n" );
				String responseBody = headerEnd >= 0 ? text.substring( headerEnd + 4 ) : "";

				Map<String, Object> result = new java.util.LinkedHashMap<String, Object>();
				result.put( "status", status );
				result.put( "body", responseBody );
				return result;
			}
		}

		void stop()
		{
			running = false;
			try
			{
				serverSocket.close();
			}
			catch ( Exception ignore )
			{
			}
		}

		private final class AcceptorThread extends Thread
		{
			AcceptorThread()
			{
				super( "MCP-E2E-acceptor" );
			}

			@Override
			public void run()
			{
				while ( running )
				{
					try
					{
						Socket socket = serverSocket.accept();
						new RequestWorker( socket ).start();
					}
					catch ( Exception e )
					{
						if ( !running )
							return;
					}
				}
			}
		}

		private final class RequestWorker extends Thread
		{
			private final Socket socket;

			RequestWorker( Socket socket )
			{
				super( "MCP-E2E-worker" );
				this.socket = socket;
			}

			@Override
			public void run()
			{
				try
				{
					DataInputStream in = new DataInputStream( socket.getInputStream() );
					String raw = readHttpRequest( in );

					String[] lines = raw.split( "\r\n" );
					String[] rl = lines[ 0 ].split( " " );
					String method = rl[ 0 ];
					String target = rl.length > 1 ? rl[ 1 ] : "/mcp";
					int qpos = target.indexOf( '?' );
					String path = qpos >= 0 ? target.substring( 0, qpos ) : target;
					String query = qpos >= 0 ? target.substring( qpos + 1 ) : null;
					int bodyStart = raw.indexOf( "\r\n\r\n" );
					String body = bodyStart >= 0 ? raw.substring( bodyStart + 4 ) : "";

					String sessionId = parseSessionId( query );
					if ( sessionId != null )
						SecurityManager.addThreadToSessionRecord( Thread.currentThread(), sessionId );

					HandleResult result = "POST".equalsIgnoreCase( method )
							? servlet.handle( method, path, query, body, null )
							: new HandleResult( 405, "{\"error\":\"method not allowed\"}" );

					String statusText = result.status == 200 ? "OK" : result.status == 401 ? "Unauthorized" : "Error";
					byte[] bodyBytes = result.body.getBytes( StandardCharsets.UTF_8 );
					String resp = "HTTP/1.1 " + result.status + " " + statusText + "\r\n"
							+ "Content-Type: application/json\r\n"
							+ "Content-Length: " + bodyBytes.length + "\r\n"
							+ "Connection: close\r\n"
							+ "\r\n";
					OutputStream out = socket.getOutputStream();
					out.write( resp.getBytes( StandardCharsets.UTF_8 ) );
					out.write( bodyBytes );
					out.flush();
					socket.close();
				}
				catch ( Exception ignore )
				{
				}
			}
		}
	}

	private static String parseSessionId( String query )
	{
		if ( query == null )
			return null;
		for ( String pair : query.split( "&" ) )
		{
			int idx = pair.indexOf( '=' );
			if ( idx > 0 && SecurityManager.SESSION_ID.equals( pair.substring( 0, idx ) ) )
				return pair.substring( idx + 1 );
		}
		return null;
	}

	/** Read one HTTP request: header region up to the first blank line + exactly Content-Length body bytes. */
	private static String readHttpRequest( DataInputStream in ) throws Exception
	{
		ByteArrayAccumulator headerAcc = new ByteArrayAccumulator();
		StringBuilder headerText = new StringBuilder();
		int b;
		while ( ( b = in.read() ) != -1 )
		{
			char c = (char) b;
			headerAcc.add( (byte) b );
			headerText.append( c );
			int n = headerText.length();
			if ( n >= 4 && headerText.charAt( n - 4 ) == '\r' && headerText.charAt( n - 3 ) == '\n'
					&& headerText.charAt( n - 2 ) == '\r' && headerText.charAt( n - 1 ) == '\n' )
				break;
		}
		int contentLength = 0;
		String ht = headerText.toString();
		int clIdx = ht.indexOf( "Content-Length:" );
		if ( clIdx >= 0 )
		{
			int lineEnd = ht.indexOf( '\n', clIdx );
			String value = ht.substring( clIdx + "Content-Length:".length(), lineEnd < 0 ? ht.length() : lineEnd ).trim();
			try
			{
				contentLength = Integer.parseInt( value );
			}
			catch ( NumberFormatException ignore )
			{
			}
		}
		String headers = new String( headerAcc.bytes(), StandardCharsets.UTF_8 );
		if ( contentLength > 0 )
		{
			byte[] bodyBytes = new byte[ contentLength ];
			in.readFully( bodyBytes );
			return headers + new String( bodyBytes, StandardCharsets.UTF_8 );
		}
		return headers;
	}

	private static final class ByteArrayAccumulator
	{
		private byte[] buffer = new byte[ 1024 ];
		private int size;

		void add( byte b )
		{
			if ( size == buffer.length )
			{
				byte[] next = new byte[ buffer.length * 2 ];
				System.arraycopy( buffer, 0, next, 0, size );
				buffer = next;
			}
			buffer[ size++ ] = b;
		}

		byte[] bytes()
		{
			byte[] out = new byte[ size ];
			System.arraycopy( buffer, 0, out, 0, size );
			return out;
		}
	}
}
