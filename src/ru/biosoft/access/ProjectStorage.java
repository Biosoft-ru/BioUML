package ru.biosoft.access;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/** Coordinates creation and deletion of a project name in a local repository. */
public final class ProjectStorage
{
    private static final Map<Path, Entry> locks = new HashMap<>();

    private static final class Entry
    {
        private final ReentrantLock lock = new ReentrantLock();
        private int users;
    }

    public static final class Lease implements AutoCloseable
    {
        private final Path key;
        private final Entry entry;

        private Lease(Path key, Entry entry)
        {
            this.key = key;
            this.entry = entry;
        }

        @Override
        public void close()
        {
            entry.lock.unlock();
            synchronized( locks )
            {
                if( --entry.users == 0 )
                    locks.remove( key );
            }
        }
    }

    private ProjectStorage()
    {
    }

    public static Lease lock(Path root, String name)
    {
        Path key = deletionRecord( root, name );
        Entry entry;
        synchronized( locks )
        {
            entry = locks.computeIfAbsent( key, ignored -> new Entry() );
            entry.users++;
        }
        entry.lock.lock();
        return new Lease( key, entry );
    }

    public static Path deletionRecord(Path root, String name)
    {
        return root.resolve( ".project-deletion-" + UUID.nameUUIDFromBytes( name.getBytes( StandardCharsets.UTF_8 ) ) + ".pending" );
    }

    public static void checkCreation(Path root, String name) throws IOException
    {
        if( !Files.notExists( deletionRecord( root, name ), LinkOption.NOFOLLOW_LINKS ) )
            throw new IOException( "Finish deleting project " + name + " before creating it again" );
    }
}
