package ru.biosoft.server.servlets.support;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.server.AbstractServlet;

/**
 * Servlet for downloading exported repository files without authentication.
 * Files are registered with a UUID and can be downloaded via a public URL.
 * Files are stored in a temporary directory and deleted when the JVM shuts down.
 */
public class ExportDownloadServlet extends AbstractServlet
{
    private static final Logger log = Logger.getLogger(ExportDownloadServlet.class.getName());

    /** Map of downloadId -> ExportedFile. */
    private static final ConcurrentHashMap<String, ExportedFile> EXPORTS = new ConcurrentHashMap<>();

    /** Base directory for exported files (temp directory, cleared on JVM exit). */
    public static final File BASE_DIR;

    static
    {
        File tmp = new File(System.getProperty("java.io.tmpdir"), "biouml-export-download");
        if (!tmp.isDirectory())
            tmp.mkdirs();
        BASE_DIR = tmp;
        // Register shutdown hook to clean up on JVM exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Cleaning up export download directory: " + BASE_DIR);
            deleteRecursive(BASE_DIR);
        }));
    }

    /** An exported file with its metadata. */
    public static final class ExportedFile
    {
        public final File file;
        public final String originalName;
        public final long created;
        public final String mimeType;
        public final String sha256;

        public ExportedFile(File file, String originalName, String mimeType) throws java.io.IOException
        {
            this.file = file;
            this.originalName = originalName;
            this.created = System.currentTimeMillis();
            this.mimeType = mimeType;
            this.sha256 = sha256( file );
        }

        private static String sha256( File file ) throws java.io.IOException
        {
            try ( java.io.InputStream in = new java.io.FileInputStream( file ) )
            {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance( "SHA-256" );
                byte[] buffer = new byte[ 64 * 1024 ];
                for ( int n; ( n = in.read( buffer ) ) > 0; )
                    digest.update( buffer, 0, n );
                StringBuilder hex = new StringBuilder();
                for ( byte b : digest.digest() )
                    hex.append( String.format( "%02x", b & 0xff ) );
                return hex.toString();
            }
            catch ( java.security.NoSuchAlgorithmException e )
            {
                throw new IllegalStateException( e );
            }
        }
    }

    /**
     * Register an exported file and return a download ID.
     * The file must already exist in the BASE_DIR or a subdirectory.
     */
    public static String registerExport(File file, String originalName, String mimeType) throws java.io.IOException
    {
        if (!file.isFile() || !file.getAbsolutePath().startsWith(BASE_DIR.getAbsolutePath()))
            throw new IllegalArgumentException("File must be inside the export download base directory: " + BASE_DIR);
        String id = UUID.randomUUID().toString();
        EXPORTS.put(id, new ExportedFile(file, originalName, mimeType));
        return id;
    }

    /**
     * Get an exported file by download ID, or null if not found/expired.
     */
    public static ExportedFile getExport(String downloadId)
    {
        ExportedFile exported = EXPORTS.get(downloadId);
        if (exported == null)
            return null;
        if (!exported.file.isFile() || exported.file.length() == 0)
        {
            EXPORTS.remove(downloadId);
            return null;
        }
        return exported;
    }

    /**
     * Remove an exported file from the registry and delete it.
     */
    public static void removeExport(String downloadId)
    {
        ExportedFile exported = EXPORTS.remove(downloadId);
        if (exported != null)
        {
            exported.file.delete();
            File parent = exported.file.getParentFile();
            if (parent != null && parent != BASE_DIR && parent.list() != null && parent.list().length == 0)
                parent.delete();
        }
    }

    @Override
    public void init(String[] args) throws Exception
    {
        super.init(args);
    }

    @Override
    public String service(String localAddress, Object session, Map params, OutputStream out, Map<String, String> header)
    {
        // Header-based branch: buffer the body into `out` and declare all headers (and the logical
        // status) in `header` — the caller copies them to the response after this method returns.
        serve(localAddress, out, header);
        return header.get("Content-Type");
    }

    /**
     * Stream a file registered by {@link #registerExport} to the caller, without any authentication.
     * `respObj` is the HTTP response object (or a header map in the fallback branch) — the headers are
     * applied through it and the body is written to `out`.
     */
    public void service(String localAddress, Object session, Map params, OutputStream out, Object respObj)
    {
        java.util.Map<String, String> headers = new java.util.LinkedHashMap<>();
        serve(localAddress, out, headers);
        // Apply the headers to the real HTTP response object.
        try
        {
            for ( java.util.Map.Entry<String, String> e : headers.entrySet() )
            {
                if ( "Status".equals( e.getKey() ) )
                {
                    respObj.getClass().getMethod( "setStatus", int.class ).invoke( respObj, Integer.parseInt( e.getValue() ) );
                }
                else
                {
                    respObj.getClass().getMethod( "setHeader", String.class, String.class ).invoke( respObj, e.getKey(), e.getValue() );
                }
            }
        }
        catch ( Exception ignore )
        {
            // respObj may be a wrapper without the expected methods; the body is still written to `out`.
        }
    }

    private static void serve( String localAddress, OutputStream out, Map<String, String> headers )
    {
        String path = localAddress;
        while ( path.startsWith( "/" ) )
            path = path.substring( 1 );

        // URL format: .../export-download/<downloadId>
        int sep = path.lastIndexOf( '/' );
        String downloadId = sep == -1 ? path : path.substring( sep + 1 );
        ExportedFile exported = getExport(downloadId);
        if ( exported == null )
        {
            headers.put( "Content-Type", "text/plain" );
            headers.put( "Status", "404" );
            write( out, "Error: no exported file with id " + downloadId + " (exports exist only for the running server; export again with biouml_repo_export)" );
            return;
        }

        headers.put( "Content-Type", exported.mimeType );
        headers.put( "Content-Length", String.valueOf( exported.file.length() ) );
        headers.put( "Content-Disposition", "attachment; filename=\"" + exported.originalName + "\"" );
        headers.put( "Cache-Control", "no-cache, no-store, must-revalidate" );
        try
        {
            Files.copy( exported.file.toPath(), out );
            out.flush();
        }
        catch ( Exception e )
        {
            log.log( Level.SEVERE, "Error streaming file: " + exported.file, e );
        }
    }

    private static void write( OutputStream out, String message )
    {
        try
        {
            out.write( message.getBytes( "UTF-8" ) );
        }
        catch ( IOException ignore )
        {
        }
    }

    private static void deleteRecursive(File file)
    {
        if (file.isDirectory())
        {
            File[] children = file.listFiles();
            if (children != null)
                for (File child : children)
                    deleteRecursive(child);
        }
        file.delete();
    }
}
