package ru.biosoft.server.servlets.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Properties;
import java.util.UUID;

/** Records the target outside the project tree so cleanup can be retried after its config is removed. */
final class ProjectDeletion
{
    private final Path directory;
    private final Path pending;

    record Pending(String directoryIdentity, String database)
    {
    }

    ProjectDeletion(Path projectsRoot, String projectName) throws IOException
    {
        Path root = projectsRoot.toRealPath();
        directory = root.resolve( projectName ).normalize();
        if( !root.equals( directory.getParent() ) || !projectName.equals( directory.getFileName().toString() ) )
            throw new IOException( "Invalid project directory: " + directory );
        pending = root.resolve( ".project-deletion-" + UUID.nameUUIDFromBytes( projectName.getBytes( StandardCharsets.UTF_8 ) )
                + ".pending" );
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
        return new Pending( identity, state.getProperty( "database" ) );
    }

    Pending begin(Path declaredDirectory, String database) throws IOException
    {
        Path declared = declaredDirectory.toAbsolutePath().normalize();
        if( declared.getParent() == null || !directory.equals( declared.getParent().toRealPath().resolve( declared.getFileName() ) ) )
            throw new IOException( "Project directory is outside its expected location: " + declaredDirectory );

        Properties state = new Properties();
        String identity = directoryIdentity();
        state.setProperty( "directoryIdentity", identity );
        if( database != null )
            state.setProperty( "database", database );
        Path temporary = Files.createTempFile( pending.getParent(), ".project-deletion-", ".tmp" );
        try
        {
            try( var output = Files.newOutputStream( temporary ) )
            {
                state.store( output, null );
            }
            try
            {
                Files.move( temporary, pending, StandardCopyOption.ATOMIC_MOVE );
            }
            catch( AtomicMoveNotSupportedException e )
            {
                Files.move( temporary, pending );
            }
        }
        finally
        {
            Files.deleteIfExists( temporary );
        }
        return new Pending( identity, database );
    }

    void verifyDirectory(Pending state) throws IOException
    {
        String identity = directoryIdentity();
        if( !identity.isEmpty() && !identity.equals( state.directoryIdentity() ) )
            throw new IOException( "Project directory was replaced during deletion: " + directory );
    }

    /** Unlink project symlinks before legacy repository removal can resolve them as child collections. */
    void removeSymbolicLinks(Pending state) throws IOException
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
                    Files.deleteIfExists( file );
                return FileVisitResult.CONTINUE;
            }
        } );
    }

    void removeDirectory(Pending state) throws IOException
    {
        verifyDirectory( state );
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
