package ru.biosoft.server.servlets.support;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Properties;
import ru.biosoft.access.ProjectStorage;

/** Records the target outside the project tree so cleanup can be retried after its config is removed. */
final class ProjectDeletion
{
    private final Path directory;
    private final Path pending;

    record Pending(String directoryIdentity, String database, Path backup, boolean backupComplete, Path backupAttempt, boolean groupPending)
    {
        Pending withBackup(Path complete, boolean ready, Path attempt)
        {
            return new Pending( directoryIdentity, database, complete, ready, attempt, groupPending );
        }

        Pending withGroupPending()
        {
            return new Pending( directoryIdentity, database, backup, backupComplete, backupAttempt, true );
        }
    }

    ProjectDeletion(Path projectsRoot, String projectName) throws IOException
    {
        Path root = projectsRoot.toRealPath();
        directory = root.resolve( projectName ).normalize();
        if( !root.equals( directory.getParent() ) || !projectName.equals( directory.getFileName().toString() ) )
            throw new IOException( "Invalid project directory: " + directory );
        pending = ProjectStorage.deletionRecord( root, projectName );
    }

    Path directory()
    {
        return directory;
    }

    void validateTarget() throws IOException
    {
        directoryIdentity();
    }

    Pending readPending() throws IOException
    {
        Properties state = new Properties();
        try( var input = Files.newInputStream( pending, LinkOption.NOFOLLOW_LINKS ) )
        {
            state.load( input );
        }
        catch( NoSuchFileException e )
        {
            return null;
        }
        String identity = state.getProperty( "directoryIdentity" );
        if( identity == null )
            throw new IOException( "Invalid project deletion record: " + pending );
        String backup = state.getProperty( "backup" );
        String attempt = state.getProperty( "backupAttempt" );
        boolean complete = Boolean.parseBoolean( state.getProperty( "backupComplete" ) );
        // Earlier deletion records used the backup field for the in-progress attempt.
        if( !complete && attempt == null )
        {
            attempt = backup;
            backup = null;
        }
        return new Pending( identity, state.getProperty( "database" ), backup == null ? null : Path.of( backup ), complete,
                attempt == null ? null : Path.of( attempt ), Boolean.parseBoolean( state.getProperty( "groupPending" ) ) );
    }

    Pending begin(Path declaredDirectory, String database) throws IOException
    {
        validateDeclaredDirectory( declaredDirectory );
        Pending state = new Pending( directoryIdentity(), database, null, false, null, false );
        save( state );
        return state;
    }

    void validateDeclaredDirectory(Path declaredDirectory) throws IOException
    {
        Path declared = declaredDirectory.toAbsolutePath().normalize();
        if( declared.getParent() == null || !directory.equals( declared.getParent().toRealPath().resolve( declared.getFileName() ) ) )
            throw new IOException( "Project directory is outside its expected location: " + declaredDirectory );
    }

    void save(Pending record) throws IOException
    {
        Properties state = new Properties();
        state.setProperty( "directoryIdentity", record.directoryIdentity() );
        if( record.database() != null )
            state.setProperty( "database", record.database() );
        if( record.backup() != null )
            state.setProperty( "backup", record.backup().toString() );
        state.setProperty( "backupComplete", Boolean.toString( record.backupComplete() ) );
        if( record.backupAttempt() != null )
            state.setProperty( "backupAttempt", record.backupAttempt().toString() );
        state.setProperty( "groupPending", Boolean.toString( record.groupPending() ) );
        Path temporary = Files.createTempFile( pending.getParent(), ".project-deletion-", ".tmp" );
        try
        {
            try( var output = Files.newOutputStream( temporary ) )
            {
                state.store( output, null );
            }
            try
            {
                Files.move( temporary, pending, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING );
            }
            catch( AtomicMoveNotSupportedException e )
            {
                throw new IOException( "Project deletion record requires atomic replacement: " + pending, e );
            }
        }
        finally
        {
            Files.deleteIfExists( temporary );
        }
    }

    void verifyDirectory(Pending state) throws IOException
    {
        String identity = directoryIdentity();
        if( !identity.isEmpty() && !identity.equals( state.directoryIdentity() ) )
            throw new IOException( "Project directory was replaced during deletion: " + directory );
    }

    /** Unlink project symlinks before legacy repository removal can resolve them as child collections. */
    void removeSymbolicLinks(Pending state, Map<Path, Path> links) throws IOException
    {
        verifyDirectory( state );
        if( Files.notExists( directory, LinkOption.NOFOLLOW_LINKS ) )
            return;

        Files.walkFileTree( directory, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException
            {
                if( attributes.isSymbolicLink() )
                {
                    Path target = Files.readSymbolicLink( file );
                    if( Files.deleteIfExists( file ) )
                        links.put( file, target );
                }
                return FileVisitResult.CONTINUE;
            }
        } );
    }

    void restoreSymbolicLinks(Pending state, Map<Path, Path> links) throws IOException
    {
        if( links.isEmpty() )
            return;
        verifyDirectory( state );
        for( var link : links.entrySet() )
        {
            Path file = link.getKey();
            if( Files.isDirectory( file.getParent(), LinkOption.NOFOLLOW_LINKS ) && Files.notExists( file, LinkOption.NOFOLLOW_LINKS ) )
                Files.createSymbolicLink( file, link.getValue() );
        }
    }

    void removeDirectory(Pending state) throws IOException
    {
        verifyDirectory( state );
        deleteTree( directory );
    }

    static void deleteTree(Path directory) throws IOException
    {
        if( Files.notExists( directory, LinkOption.NOFOLLOW_LINKS ) )
            return;

        Files.walkFileTree( directory, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException
            {
                Files.deleteIfExists( file );
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException error) throws IOException
            {
                if( error != null )
                    throw error;
                Files.deleteIfExists( dir );
                return FileVisitResult.CONTINUE;
            }
        } );

        if( !Files.notExists( directory, LinkOption.NOFOLLOW_LINKS ) )
            throw new IOException( "Project directory remains after deletion: " + directory );
    }

    void complete() throws IOException
    {
        Files.delete( pending );
    }

    private String directoryIdentity() throws IOException
    {
        BasicFileAttributes attributes;
        try
        {
            attributes = Files.readAttributes( directory, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS );
        }
        catch( NoSuchFileException e )
        {
            return "";
        }
        if( !attributes.isDirectory() )
            throw new IOException( "Expected a project directory: " + directory );
        Object fileKey = attributes.fileKey();
        return fileKey == null ? attributes.creationTime().toString() : fileKey.toString();
    }
}
