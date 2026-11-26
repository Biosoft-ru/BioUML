package ru.biosoft.server.servlets.support;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;

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
        if( pending.backupComplete() )
        {
            if( pending.backup() != null )
                verify( pending.backup(), pending.database() );
            return pending;
        }
        String configured = Application.getGlobalValue( "BackupFolder" );
        if( pending.backup() == null && "BackupFolder".equals( configured ) )
        {
            ProjectDeletion.Pending noBackup = new ProjectDeletion.Pending( pending.directoryIdentity(), pending.database(), null, true );
            deletion.save( noBackup );
            return noBackup;
        }
        if( pending.backup() == null && configured.isBlank() )
            throw new IOException( "BackupFolder is empty" );

        Path backupRoot = pending.backup() == null ? Path.of( configured ).toFile().getCanonicalFile().toPath() : pending.backup().getParent();
        if( backupRoot.startsWith( deletion.directory() ) )
            throw new IOException( "Backup location is inside the project: " + backupRoot );
        backupRoot = Files.createDirectories( backupRoot ).toRealPath();
        if( backupRoot.startsWith( deletion.directory() ) )
            throw new IOException( "Backup location is inside the project: " + backupRoot );

        deletion.verifyDirectory( pending );
        Path destination = backupRoot.resolve( deletion.directory().getFileName() + "-" + UUID.randomUUID() );
        ProjectDeletion.Pending attempt = new ProjectDeletion.Pending(
                pending.directoryIdentity(), pending.database(), destination, false );
        deletion.save( attempt );

        Path staging = Files.createDirectory( destination.resolveSibling( destination.getFileName() + ".partial" ) );
        copyProject( deletion.directory(), staging.resolve( "project" ) );
        if( attempt.database() != null )
            dump.write( attempt.database(), staging.resolve( "_dump.sql" ) );
        verify( staging, attempt.database() );
        Files.move( staging, destination, StandardCopyOption.ATOMIC_MOVE );

        ProjectDeletion.Pending complete = new ProjectDeletion.Pending(
                attempt.directoryIdentity(), attempt.database(), destination, true );
        deletion.save( complete );
        return complete;
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

    static void dumpDatabase(String database, Path output) throws Exception
    {
        URI server = URI.create( GlobalDatabaseManager.getCurrentDBUrl().substring( "jdbc:".length() ) );
        if( server.getHost() == null || !( "mysql".equals( server.getScheme() ) || "mariadb".equals( server.getScheme() ) ) )
            throw new IOException( "Cannot resolve the database server for project backup" );
        int port = server.getPort() == -1 ? 3306 : server.getPort();
        ProcessBuilder command = new ProcessBuilder( "mysqldump", "--protocol=TCP", "--host=" + server.getHost(), "--port=" + port,
                "-u" + Application.getGlobalValue( "BackupUser", "root" ), "--", database );
        command.environment().put( "MYSQL_PWD", Application.getGlobalValue( "BackupPassword", "root" ) );
        Process process = command.redirectOutput( output.toFile() ).redirectError( ProcessBuilder.Redirect.DISCARD ).start();
        try
        {
            int status = process.waitFor();
            if( status != 0 )
                throw new IOException( "mysqldump exited with status " + status + " for " + database );
        }
        catch( InterruptedException e )
        {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw e;
        }
    }
}
