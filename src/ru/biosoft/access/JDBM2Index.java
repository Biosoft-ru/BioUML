package ru.biosoft.access;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import jdbm.PrimaryTreeMap;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.RecordManagerOptions;
import jdbm.Serializer;
import jdbm.SerializerInput;
import jdbm.SerializerOutput;
import jdbm.helper.ComparableComparator;
import ru.biosoft.access.core.Index;
import ru.biosoft.access.core.Key;
import ru.biosoft.exception.ExceptionRegistry;

public class JDBM2Index implements Index
{
    private static final Logger log = Logger.getLogger( JDBM2Index.class.getName() );

    private static final String RECORD_NAME = "index";
    private static final String TABLE_NAME = "indexTable";

    private static final int NOT_COMMITED_OPERATIONS_LIMIT = 100000;

    private int notCommitedOperations = 0;

    private RecordManager recordManager;

    private File indexFile;
    private File tmpIndexFile;

    private boolean valid = true;

    PrimaryTreeMap impl;

    public JDBM2Index(File indexFile) throws Exception
    {
        this.indexFile = indexFile;
        if( !indexFile.isDirectory() )
        {
            if( indexFile.exists() )
                indexFile.delete();
            indexFile.mkdir();
            valid = false;
        }
        tmpIndexFile = new File( indexFile.getParentFile(), indexFile.getName() + ".tmp" );
        if( tmpIndexFile.exists() )
            valid = false;
        recordManager = reuqestRecordManager(indexFile.getAbsolutePath());
        impl = recordManager.treeMap(TABLE_NAME, ComparableComparator.INSTANCE, new IndexEntrySerializer(), null);
    }
    
    static class RecordManagerLink
    {
        RecordManager recordManager;
        int usage;
    }
    
    private static Map<String, RecordManagerLink> registry = new HashMap<>();
    
    private static Object sync = new Object();
    
    private static RecordManager reuqestRecordManager(String indexFile)
    {
        log.severe( "Request record manager for " + indexFile );
        synchronized( sync )
        {
            RecordManagerLink rmLink = registry.computeIfAbsent( indexFile, k -> {
                Properties properties = new Properties();
                properties.put( RecordManagerOptions.THREAD_SAFE, "true" );
                String recordName = new File(indexFile, RECORD_NAME).getAbsolutePath();
                RecordManager rm;
                try
                {
                    rm = RecordManagerFactory.createRecordManager( recordName, properties );
                    
                }
                catch( IOException e )
                {
                	releaseFileLocks(indexFile);
                	throw new RuntimeException( e );
                }
                log.severe( "Create record manager for " + indexFile );
                RecordManagerLink link = new RecordManagerLink();
                link.recordManager = rm;
                return link;
            } );

            rmLink.usage++;
            return rmLink.recordManager;
        }
    }
    
    private static void releaseFileLocks(String indexDir) {
    	for(File f : new File(indexDir).listFiles())
    	{
    		try {
				new RandomAccessFile(f, "r").close();
			} catch (IOException e) {
				log.log(Level.SEVERE, e, ()->"Closing file " + f.getAbsolutePath());
			}
    	}
	}

	private static void releaseRecordManager(String indexFile ) throws IOException
    {
        log.severe( "Release record manager for " + indexFile );
        synchronized(sync)
        {
            RecordManagerLink link = registry.get( indexFile );
            if(link == null)
            	return;
            link.usage--;
            if(link.usage <= 0)
            {
                registry.remove( indexFile );
                link.recordManager.close();
                log.severe( "Close record manager for " + indexFile );
            }
        }
        
    }
    
    

    /**
     *  Just for compatibility with BTreeIndex
     */
    public JDBM2Index(File dataFile, String indexName, String indexPath, int blockSize) throws Exception
    {
        this(new File( ( ( indexPath == null ) ? dataFile.getPath() : indexPath ) + "."
                + ( ( indexName == null ) ? DEFAULT_INDEX_NAME : indexName )));
    }

    @Override
    public String getName()
    {
        return getIndexFile().getName();
    }

    @Override
    public boolean isValid()
    {
        return valid;
    }

    @Override
    public File getIndexFile()
    {
        return indexFile;
    }

    @Override
    public void close() throws Exception
    {
        releaseRecordManager( indexFile.getAbsolutePath() );
        clearTmpIndexFile();
    }

    @Override
    public void flush() throws IOException
    {
        recordManager.commit();
        clearTmpIndexFile();
    }

    @Override
    public Iterator nodeIterator(Key key)
    {
        throw new UnsupportedOperationException();
    }

    //Map implementation

    @Override
    public int size()
    {
        return impl.size();
    }


    @Override
    public boolean isEmpty()
    {
        return impl.isEmpty();
    }


    @Override
    public boolean containsKey(Object key)
    {
        return impl.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value)
    {
        return impl.containsValue(value);
    }

    @Override
    public Object get(Object key)
    {
        return impl.get(key);
    }

    @Override
    public Set keySet()
    {
        return impl.keySet();
    }

    @Override
    public Collection values()
    {
        return impl.values();
    }

    @Override
    public Set entrySet()
    {
        return impl.entrySet();
    }

    @Override
    public Object put(Object key, Object value)
    {
        Object oldValue = impl.put(key, value);
        notCommitedOperations++;
        commitIfNeed();
        return oldValue;
    }

    @Override
    public void putAll(Map m)
    {
        impl.putAll(m);
        notCommitedOperations += m.size();
        commitIfNeed();
    }

    @Override
    public Object remove(Object key)
    {
        Object value = impl.remove(key);
        notCommitedOperations++;
        commitIfNeed();
        return value;
    }

    @Override
    public void clear()
    {
        notCommitedOperations += size();
        impl.clear();
        commitIfNeed();
    }

    private void commitIfNeed()
    {
        if( notCommitedOperations >= NOT_COMMITED_OPERATIONS_LIMIT )
        {
            try
            {
                recordManager.commit();
            }
            catch( IOException e )
            {
                throw new RuntimeException(e);
            }
            clearTmpIndexFile();
            notCommitedOperations = 0;
        }
        else
        {
            initTmpIndexFile();
        }
    }

    static
    {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor( r -> new Thread( r, "JDBM index flusher" ) );
        executor.scheduleAtFixedRate( JDBM2Index::flushOpenIndexes, 10, 10, TimeUnit.SECONDS );
    }
    private static final Map<String, JDBM2Index> OPEN_INDEXES = new ConcurrentHashMap<>();
    private static void flushOpenIndexes()
    {
        JDBM2Index[] indexes = OPEN_INDEXES.values().toArray( new JDBM2Index[0] );
        for( JDBM2Index index : indexes )
        {
            try
            {
                index.flush();
            }
            catch( Exception e )
            {
                ExceptionRegistry.log( e );
            }
        }
    }

    private void clearTmpIndexFile()
    {
        try
        {
            OPEN_INDEXES.remove( indexFile.getAbsolutePath() );
            if( tmpIndexFile.exists() && !tmpIndexFile.delete() )
                log.log( Level.WARNING, "Can not delete temp file for JDBM index: '" + tmpIndexFile.getAbsolutePath() + "'." );
        }
        catch( Exception e )
        {
            ExceptionRegistry.log( e );
        }
    }

    private void initTmpIndexFile()
    {
        try
        {
            OPEN_INDEXES.put( indexFile.getAbsolutePath(), this );
            if( !tmpIndexFile.exists() )
                tmpIndexFile.createNewFile();
        }
        catch( IOException e )
        {
            log.log( Level.SEVERE, "Can not create temp file for JDBM index: '" + tmpIndexFile.getAbsolutePath() + "'." );
        }
        catch( Exception e )
        {
            ExceptionRegistry.log( e );
        }
    }

    @Override
    public boolean equals(Object o)
    {
        return impl.equals(o);
    }

    @Override
    public int hashCode()
    {
        return impl.hashCode();
    }

    public static class IndexEntrySerializer implements Serializer<Index.IndexEntry>
    {
        @Override
        public void serialize(SerializerOutput out, Index.IndexEntry obj) throws IOException
        {
            out.writeLong(obj.from);
            out.writeLong(obj.len);
        }

        @Override
        public Index.IndexEntry deserialize(SerializerInput in) throws IOException, ClassNotFoundException
        {
            long from = in.readLong();
            long len = in.readLong();
            return new Index.IndexEntry(from, len);
        }
    }

}
