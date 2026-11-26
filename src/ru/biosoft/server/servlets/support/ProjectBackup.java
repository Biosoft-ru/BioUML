package ru.biosoft.server.servlets.support;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import com.developmentontheedge.application.Application;

import ru.biosoft.access.security.GlobalDatabaseManager;

final class ProjectBackup
{
    @FunctionalInterface
    interface DatabaseDump
    {
        void write(String database, Path output) throws Exception;
    }

    private ProjectBackup()
    {
    }

    static ProjectDeletion.Pending ensureComplete(ProjectDeletion deletion, ProjectDeletion.Pending pending, DatabaseDump dump)
            throws Exception
    {
        deletion.verifyDirectory( pending );
        cleanStaging( deletion, pending.backupAttempt() );

        if( Files.notExists( deletion.directory(), LinkOption.NOFOLLOW_LINKS ) )
        {
            if( !pending.backupComplete() )
                throw new IOException( "Project directory disappeared before its backup completed: " + deletion.directory() );
            if( pending.backup() != null )
                verify( pending.backup(), pending.database() );
            return pending;
        }

        String configured = Application.getGlobalValue( "BackupFolder" );
        if( "BackupFolder".equals( configured ) )
        {
            ProjectDeletion.Pending noBackup = pending.withBackup( pending.backup(), true, null );
            deletion.save( noBackup );
            return noBackup;
        }
        if( configured.isBlank() )
            throw new IOException( "BackupFolder is empty" );

        Path backupRoot = Path.of( configured ).toFile().getCanonicalFile().toPath();
        if( backupRoot.startsWith( deletion.directory() ) )
            throw new IOException( "Backup location is inside the project: " + backupRoot );
        backupRoot = Files.createDirectories( backupRoot ).toRealPath();
        if( backupRoot.startsWith( deletion.directory() ) )
            throw new IOException( "Backup location is inside the project: " + backupRoot );

        Path destination = backupRoot.resolve( deletion.directory().getFileName() + "-" + UUID.randomUUID() );
        Path staging = stagingDirectory( destination );
        Files.createDirectory( staging );
        try
        {
            deletion.save( pending.withBackup( pending.backup(), pending.backupComplete(), destination ) );
            copyProject( deletion.directory(), staging.resolve( "project" ) );
            if( pending.database() != null )
                dump.write( pending.database(), staging.resolve( "_dump.sql" ) );
            verify( staging, pending.database() );
            Files.move( staging, destination, StandardCopyOption.ATOMIC_MOVE );
        }
        catch( Exception failure )
        {
            try
            {
                cleanStaging( deletion, destination );
            }
            catch( IOException cleanup )
            {
                failure.addSuppressed( cleanup );
            }
            throw failure;
        }

        ProjectDeletion.Pending complete = pending.withBackup( destination, true, null );
        deletion.save( complete );
        return complete;
    }

    private static Path stagingDirectory(Path destination)
    {
        return destination.resolveSibling( destination.getFileName() + ".partial" );
    }

    private static void cleanStaging(ProjectDeletion deletion, Path destination) throws IOException
    {
        if( destination == null )
            return;
        String prefix = deletion.directory().getFileName() + "-";
        String name = destination.getFileName().toString();
        if( !destination.isAbsolute() || !name.startsWith( prefix ) || destination.normalize().startsWith( deletion.directory() ) )
            throw new IOException( "Invalid project backup destination: " + destination );
        try
        {
            UUID.fromString( name.substring( prefix.length() ) );
        }
        catch( IllegalArgumentException e )
        {
            throw new IOException( "Invalid project backup destination: " + destination, e );
        }
        Path staging = stagingDirectory( destination );
        if( Files.notExists( staging, LinkOption.NOFOLLOW_LINKS ) )
            return;
        if( staging.getParent().toRealPath().startsWith( deletion.directory() ) )
            throw new IOException( "Backup staging directory is inside the project: " + staging );
        ProjectDeletion.deleteTree( staging );
    }

    private static void copyProject(Path source, Path destination) throws IOException
    {
        Files.walkFileTree( source, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) throws IOException
            {
                Files.createDirectory( destination.resolve( source.relativize( dir ) ) );
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException
            {
                Files.copy( file, destination.resolve( source.relativize( file ) ), LinkOption.NOFOLLOW_LINKS,
                        StandardCopyOption.COPY_ATTRIBUTES );
                return FileVisitResult.CONTINUE;
            }
        } );
    }

    private static void verify(Path backup, String database) throws IOException
    {
        if( !Files.isDirectory( backup, LinkOption.NOFOLLOW_LINKS )
                || !Files.isDirectory( backup.resolve( "project" ), LinkOption.NOFOLLOW_LINKS ) )
            throw new IOException( "Project backup is incomplete: " + backup );
        if( database != null )
        {
            Path dump = backup.resolve( "_dump.sql" );
            if( !Files.isRegularFile( dump, LinkOption.NOFOLLOW_LINKS ) || Files.size( dump ) == 0 )
                throw new IOException( "Project database dump is incomplete: " + dump );
        }
    }

    private static final Pattern MYSQL_AUTHORITY = Pattern.compile( "(?:\\[([0-9A-Fa-f:.%a-zA-Z_-]+)\\]|([A-Za-z0-9._-]+))(?::([0-9]+))?" );

    static List<String> dumpConnectionOptions(String jdbcUrl) throws IOException
    {
        try
        {
            if( !jdbcUrl.startsWith( "jdbc:" ) )
                throw new IllegalArgumentException();
            URI server = URI.create( jdbcUrl.substring( "jdbc:".length() ) );
            if( !( "mysql".equals( server.getScheme() ) || "mariadb".equals( server.getScheme() ) ) || server.getRawAuthority() == null )
                throw new IllegalArgumentException();
            var match = MYSQL_AUTHORITY.matcher( server.getRawAuthority() );
            if( !match.matches() )
                throw new IllegalArgumentException();
            String host = match.group( 1 ) == null ? match.group( 2 ) : match.group( 1 );
            String configuredPort = match.group( 3 );
            if( configuredPort == null && "localhost".equalsIgnoreCase( host ) )
                return List.of( "--host=localhost" ); // Retain the client's local socket default.
            int port = configuredPort == null ? 3306 : Integer.parseInt( configuredPort );
            if( port <= 0 || port > 65535 )
                throw new IllegalArgumentException();
            return List.of( "--protocol=TCP", "--host=" + host, "--port=" + port );
        }
        catch( IllegalArgumentException e )
        {
            throw new IOException( "Cannot resolve a supported database server for project backup" );
        }
    }

    static void dumpDatabase(String database, Path output) throws Exception
    {
        List<String> arguments = new ArrayList<>();
        arguments.add( "mysqldump" );
        arguments.addAll( dumpConnectionOptions( GlobalDatabaseManager.getCurrentDBUrl() ) );
        arguments.add( "-u" + Application.getGlobalValue( "BackupUser", "root" ) );
        arguments.add( "--" );
        arguments.add( database );
        ProcessBuilder command = new ProcessBuilder( arguments );
        String password = Application.getGlobalValue( "BackupPassword", "root" );
        command.environment().put( "MYSQL_PWD", password );

        Path stderr = Files.createTempFile( output.getParent(), ".mysqldump-", ".stderr" );
        Process process = command.redirectOutput( output.toFile() ).redirectError( stderr.toFile() ).start();
        try
        {
            int status = process.waitFor();
            if( status != 0 )
            {
                String diagnostic;
                try( var input = Files.newInputStream( stderr ) )
                {
                    diagnostic = new String( input.readNBytes( 8192 ), StandardCharsets.UTF_8 ).strip();
                }
                if( !password.isEmpty() )
                    diagnostic = diagnostic.replace( password, "[redacted]" );
                throw new IOException( "mysqldump exited with status " + status + " for " + database
                        + ( diagnostic.isEmpty() ? "" : ": " + diagnostic ) );
            }
            Files.delete( stderr );
        }
        catch( InterruptedException e )
        {
            process.destroyForcibly();
            process.onExit().join();
            Thread.currentThread().interrupt();
            throw e;
        }
    }
}
