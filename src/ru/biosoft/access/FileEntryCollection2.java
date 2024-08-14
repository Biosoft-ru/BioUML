package ru.biosoft.access;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.Index;
import ru.biosoft.access.core.Index.IndexEntry;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.exception.InternalException;

import com.developmentontheedge.application.ApplicationStatusBar;
import ru.biosoft.jobcontrol.FunctionJobControl;


/**
 * @todo Document!!!
 * @todo Algorithm for select index implementation.
 * @author DevelopmentOnTheEdge
 * @version 1.0
 */
public class FileEntryCollection2 extends EntryCollection
{
    /** @todo Document */
    public final static int BUFFER_SIZE = 32 * 1024;
    /** @todo Document */
    public final static int MAX_ENTRY_STR_SIZE = 16 * 1024;

    /** Property name to specify class of index */
    public static final String INDEX_TYPE_PROPERTY = "index.type";
    public static final String FILE_ENCODING_PROPERTY = "file.encoding";
    public static final String ORIGINAL_ORDER_PROPERTY = "original.order";
    public static final String INDEX_DIR = "index.dir";

    //    public static final String DEFAULT_INDEX_TYPE = JDBM2Index.class.getName();
    public static final String DEFAULT_INDEX_TYPE = BTreeIndex.class.getName();

    /** Index */
    private Index<IndexEntry> index = null;

    /** Unmodifiable synchronized name list. */
    private Vector<String> nameList = null;//new ArrayList(256);

    /** Name of data file */
    protected File fileName = null;

    private final Object fileLock = new Object();

    /** Length of file */
    private long fileLength = 0;

    /** @todo Document */
    private String start = "ID";

    /** @todo Document */
    private String startKey = "ID";

    /** @todo Document */
    private String startIndexedBlock = null;

    /** @todo Document */
    private String endIndexedBlock = null;

    private boolean fullKey = false;

    private boolean escapeKeys = false;

    /**
     * String by which any entry is finished.
     * Default value is: <code>"//" </code>.
     */
    private String end = "//";

    /** @todo Document */
    private String delimiters = ";\t\r\n";

    private final String encoding;

    private final boolean originalOrder;


    /**
     * Standard data collection constructor
     * @param parent parent data collection
     * @param properties data collection properties
     */
    public FileEntryCollection2(DataCollection<?> parent, Properties properties) throws Exception
    {
        this(parent, properties, true);
    }

    /**
     * Special constructor
     * @param parent parent data collection
     * @param properties data collection properties
     * @param createIndex indicates when index creation is necessary
     */
    public FileEntryCollection2(DataCollection<?> parent, Properties properties, boolean createIndex) throws Exception
    {
        super(parent, properties);

        // Read properties
        start = properties.getProperty(ENTRY_START_PROPERTY, start);
        startKey = properties.getProperty(ENTRY_ID_PROPERTY, startKey);
        delimiters = properties.getProperty(ENTRY_DELIMITERS_PROPERTY, delimiters);
        end = properties.getProperty(ENTRY_END_PROPERTY, end);
        mutable = !Boolean.parseBoolean(properties.getProperty(UNMODIFIABLE_PROPERTY));
        startIndexedBlock = properties.getProperty(BLOCK_START_INDEXED_PROPERTY);
        endIndexedBlock = properties.getProperty(BLOCK_END_INDEXED_PROPERTY);
        fullKey = Boolean.parseBoolean(properties.getProperty(ENTRY_KEY_FULL));
        escapeKeys = Boolean.parseBoolean(properties.getProperty(ENTRY_KEY_ESCAPE_SPECIAL_CHARS));
        encoding = properties.getProperty(FILE_ENCODING_PROPERTY, System.getProperty("file.encoding"));
        originalOrder = Boolean.parseBoolean(properties.getProperty(ORIGINAL_ORDER_PROPERTY));

        // Create/Open data file
        String path = properties.getProperty(DataCollectionConfigConstants.FILE_PATH_PROPERTY, properties.getProperty(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, "."));
        fileName = new File(path, properties.getProperty(DataCollectionConfigConstants.FILE_PROPERTY));

        fileLength = fileName.length();

        FunctionJobControl fjc = (FunctionJobControl)properties.get(DataCollectionConfigConstants.JOB_CONTROL_PROPERTY);

        // Create index
        int blockSize = BTreeIndex.DEFAULT_BLOCK_SIZE;
        String blockSizeProperty = properties.getProperty(QuerySystem.INDEX_BLOCK_SIZE);
        if( blockSizeProperty != null )
        {
            try
            {
                blockSize = Integer.parseInt(blockSizeProperty);
            }
            catch( NumberFormatException nfe )
            {
                log.log(Level.SEVERE, "Can not parse index block size '" + blockSizeProperty + "'.", nfe);
            }
        }

        if( createIndex )
        {
            String indexDir = properties.getProperty( INDEX_DIR, path );
            File indexPath = new File(indexDir, fileName.getName());
            createIndex(indexPath.getAbsolutePath(), fjc, blockSize);
        }

        // Initialize name list
        //        nameList = new ArrayList(index.keySet());

        // Fill used files
        getInfo().addUsedFile(fileName);
    }

    /**
     * @todo Document
     */
    @Override
    public boolean contains(String name)
    {
        checkState();
        if( v_cache.get(name) != null )
            return true;
        return index.containsKey(name);
    }

    private void checkState()
    {
        if(index == null)
            throw new InternalException( "Collection is closed: "+getCompletePath() );
    }

    /**
     * @todo Document
     */
    @Override
    public int getSize()
    {
        checkState();
        return index.size();
    }

    public synchronized @Nonnull Vector<String> getInternalNameList()
    {
        if( nameList == null )
        {
            checkState();
            Set<String> keys = index.keySet();
            if( keys != null && keys.size() > 0 )
            {
                nameList = new Vector<>(keys);
                if( originalOrder )
                {
                    Collections.sort( nameList, Comparator.comparingLong( name -> index.get( name ).from ) );
                }
            }
            else
                nameList = new Vector<>();
        }
        return nameList;
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        return getInternalNameList();
    }

    /**
     * Close data collection.
     * Release all resources (data file and index file).
     * After call this method collection is invalid and any other methods may throw exceptions.
     * @throws Exception If error occurred.
     */
    @Override
    public void close() throws Exception
    {
        super.close();

        nameList = null;

        FilePool.close( fileName.getAbsolutePath() );

        if( index != null )
        {
            closeIndex();
            index = null;
        }
    }

    public void flush() throws Exception
    {
        index.flush();
    }

    ////////////////////////////////////////////////////////////////////////////
    // Protected

    /**
     * @todo Document
     * @todo high Optimize with other Entry constructor.
     */
    @Override
    protected Entry doGet(String name) throws Exception
    {
        checkState();
        IndexEntry indexEntry = index.get(name);
        return createEntry(name, indexEntry);
    }

    /**
     * @todo Document
     */
    @Override
    protected void doPut(Entry entry, boolean isNew) throws Exception
    {
        if( !isNew )
            doRemove(entry.getName());

        long from = fileLength;

        synchronized( fileLock )
        {
            RandomAccessFile file = FilePool.acquire( fileName.getAbsolutePath() );
            try
            {
                file.seek( from );
                // Write entry
                Reader reader = entry.getReader();
                int readed;
                long bytesWritten = 0;
                char[] buffer = new char[BUFFER_SIZE];
                while( ( readed = reader.read( buffer ) ) != -1 )
                {
                    byte[] bytes = new String( buffer, 0, readed ).getBytes( encoding );
                    file.write( bytes );
                    fileLength += readed;
                    bytesWritten += bytes.length;
                }
                // Save index
                IndexEntry indexEntry = new IndexEntry(from, bytesWritten);
                addToIndex(entry.getName(), indexEntry);
            }
            finally
            {
                FilePool.release( fileName.getAbsolutePath(), file );
            }
        }
        String name = entry.getName();
        int idx = -1;
        if( getInternalNameList().size() > 0 )
            idx = Collections.binarySearch(getInternalNameList(), name);
        if( idx >= 0 )
        {
            getInternalNameList().setElementAt(name, idx);
        }
        else
        {
            idx = -idx - 1;
            getInternalNameList().insertElementAt(name, idx);
        }
        //getInternalNameList().add(element.getName());
    }

    /**
     * @todo Document
     * @todo  do not write data  to the file, only start shars,key,"delete",end chars
     * @todo Rewrite (now this code pasted from old FileEntryCollection)
     */
    @Override
    protected void doRemove(String name) throws Exception
    {
        if( log.isLoggable( Level.FINE ) )
            log.log(Level.FINE, "doRemove <" + name + ">, start");
        Entry element = get(name);
        BufferedReader reader = new BufferedReader(element.getReader());
        synchronized( fileLock )
        {
            RandomAccessFile file = FilePool.acquire( fileName.getAbsolutePath() );
            try
            {
                file.seek( fileLength );
                String lastLine = null;
                String line;

                while( ( line = reader.readLine() ) != null )
                {
                    lastLine = line;
                    file.writeBytes( "\r\n" + line );
                    fileLength += line.length() + 2;
                }

                if( lastLine != null && end.length() == 0 )
                {
                    file.writeBytes( "\r\n" );
                    fileLength += 2;
                }
                file.writeBytes( "deleted\r\n" );
            }
            finally
            {
                FilePool.release( fileName.getAbsolutePath(), file );
            }
        }
        fileLength += 9;

        removeFromIndex(name);

        int idx = -1;
        if( getInternalNameList().size() > 0 )
            idx = Collections.binarySearch(getInternalNameList(), name);
        if( idx >= 0 )
        {
            getInternalNameList().remove(idx);
        }
        //getInternalNameList().remove(name);
    }


    /**
     * Create index.
     * @todo HIGH Use BTreeIndex.
     */
    protected void createIndex(String indexPath, FunctionJobControl fjc, int blockSize) throws Exception
    {
        //int autoNumber = 1;
        final String indexName = "id";
        //index = new MemIndex(fileName, indexName);
        //index = new BTreeIndex(fileName, indexName, indexPath, blockSize);
        //index = new JDBM2Index(new File(indexPath + "." + indexName));

        Properties properties = getInfo().getProperties();
        String indexType = properties.getProperty(INDEX_TYPE_PROPERTY, DEFAULT_INDEX_TYPE);
        Class<? extends Index> clazz = ClassLoading.loadSubClass( indexType, Index.class );
        Constructor<? extends Index> constructor = clazz.getConstructor(File.class, String.class, String.class, int.class);
        index = constructor.newInstance(fileName, indexName, indexPath, blockSize);

//        ApplicationFrame frame = Application.getApplicationFrame();
//        ApplicationStatusBar sb = null;
//        if( frame != null )
//        {
//            sb = frame.getStatusBar();
//        }
        String message = "Creating indexes for " + fileName.getName() + ". Please wait. Progress: ";
//        if( sb != null )
//        {
//            sb.startProgressBar();
//            sb.setValue(0);
//            sb.setMessage(message + 0 + "%");
//        }

        //File indexFile = new File ( indexPath + "." + indexName );
        try
        {
            //getInfo ( ).addUsedFile ( indexFile );
            if( !index.isValid() )
            {
                // Rebuild index
                clearIndex();
                synchronized( fileLock )
                {
                    RandomAccessFile file = FilePool.acquire( fileName.getAbsolutePath() );
                    try
                    {
                        buildIndex( file, fjc, null, message );
                    }
                    finally
                    {
                        FilePool.release( fileName.getAbsolutePath(), file );
                    }
                }
            }
            File indexFile = index.getIndexFile();
            if( indexFile != null )
                getInfo().addUsedFile(indexFile);

        }
        catch( Exception exc )
        {
            closeIndex();
            index = null;
            // indexFile.delete ( );
            throw exc;
        }
        finally
        {
//            if( sb != null )
//            {
//                sb.setMessage("");
//                sb.stopProgressBar();
//            }
            if( index != null )
                index.flush();
        }
    }

    private void buildIndex(RandomAccessFile file, FunctionJobControl fjc, ApplicationStatusBar sb, String message) throws Exception
    {
        long fileSize = fileName.length();
        // Parse data file and add entries to index
        String key = null;
        String str = null;
        String preStr;

        long pos = 0;
        long prePos = 0;

        file.seek( 0 );
        if( fjc != null )
            fjc.functionStarted( "Indexes creating" );

        FastFileReader fastReader = new FastFileReader( file, FastFileReader.DEFAULT_BUFSIZE );

        boolean EOF;
        boolean BOF;
        boolean isStartEntry;
        boolean isStartKey;
        boolean isEndEntry;
        int progress = 0;
        int oldProgress = 0;
        boolean startIndex = ( startIndexedBlock == null );
        do
        {
            pos = file.getFilePointer();
            if( !startIndex )
            {
                str = fastReader.fastReadLine( pos );
                if( ( str != null ) && ( str.startsWith( startIndexedBlock ) ) )
                {
                    startIndex = true;
                }
            }
            else
            {
                preStr = str;
                str = fastReader.fastReadLine( pos );

                if( ( endIndexedBlock != null ) && ( str.startsWith( endIndexedBlock ) ) )
                {
                    startIndex = false;
                }

                EOF = str == null;
                BOF = preStr == null;
                isStartEntry = str != null && start != null && str.startsWith( start );
                isStartKey = str != null && startKey != null && str.startsWith( startKey );
                isEndEntry = str != null && end.length() > 0 && str.startsWith( end );

                if( !BOF && key != null && ( EOF || isStartEntry || isEndEntry ) )
                {
                    if( fjc != null )
                    {
                        fjc.setPreparedness( (int) ( ( 100.0 * prePos ) / ( fileLength == 0 ? 1 : fileLength ) ) );
                    }

                    IndexEntry ind;
                    if( !isStartEntry )
                    {
                        ind = new IndexEntry( prePos, file.getFilePointer() - prePos );
                    }
                    else
                    {
                        ind = new IndexEntry( prePos, pos - prePos );
                        //Pos = pos;
                    }
                    //prePos = pos;

                    boolean isDeleted = ( !EOF && str.indexOf( "deleted" ) != -1 ) || preStr.indexOf( "deleted" ) != -1;

                    if( isDeleted )
                    {
                        removeFromIndex( key );
                    }
                    else
                    {
                        //check is this key already exists
                        //TODO - check if it is necessary
                        //to use autonumbers
                        /*while ( index.get ( key ) != null )
                         {
                         if( ! key.endsWith ( "_" ) )
                         key += "_";
                         key += autoNumber;
                         autoNumber++;
                         }*/
                        addToIndex( key, ind );
                        progress = (int) ( ind.from * 100l / ( fileSize == 0 ? 1 : fileSize ) );
                        if( progress != oldProgress )
                        {
                            if( sb != null )
                            {
                                sb.setValue( progress );
                                sb.setMessage( message + progress + "%" );
                            }
                            oldProgress = progress;
                        }
                    }

                    key = null;
                }
                if( isStartEntry )
                {
                    prePos = pos;
                }

                if( !EOF && isStartKey )
                {
                    /////
                    // Tricks for FASTA
                    ////
                    if( startKey.equals( ">" ) )
                    {
                        key = extractFastaKey( str );
                        if( key == null || key.length() == 0 )
                        {
                            key = "seq";
                        }
                    }
                    else
                        key = extractKey( str );
                }
            }
        }
        while( str != null );

        if( fjc != null )
            fjc.functionFinished();
    }

    private String extractFastaKey(String data)
    {
        data = data.replace('\\', '_');
        data = data.replace('/', '_');
        data = data.replace(':', '_');
        data = data.replace('*', '_');
        data = data.replace('?', '_');
        data = data.replace('<', '_');
        //data = data.replace('|', '_');

        if( fullKey )
            return data.substring(1).trim();
        String key = null;
        StringTokenizer strTok = new StringTokenizer(data, " >;");
        if( strTok.hasMoreTokens() )
        {
            key = strTok.nextToken();
            if( key.equalsIgnoreCase("id") )
            {
                if( strTok.hasMoreTokens() )
                    key = strTok.nextToken();
            }
            key = key.replace('>', '_');
        }
        return key;
    }

    private String escapeKey(String key)
    {
        if( key == null )
            return null;
        if( escapeKeys )
            return key.replace('/', '_').replaceAll("[\u0001-\u001F]", "");
        return key;
    }

    /**
     * Parse data and extract key from it.
     * @param data Text with key.
     * @param startKey Start tag after which key is placed.
     * @param delimiters Set of symbols that not contained in the key.
     * @return Key of the entry.
     * @todo Rewrite (now its cut&paste from IndexTable)
     */
    protected String extractKey(String data) throws Exception
    {
        int startPos = data.indexOf(startKey);
        if( startPos == -1 )
        {
            throw new Exception("error start field of key not found");
        }
        if( fullKey )
            return escapeKey(data.substring(startPos + startKey.length()).trim());
        StringTokenizer strTok = new StringTokenizer(data, delimiters);
        if( startKey.length() > 0 )
        {
            while( strTok.hasMoreTokens() )
            {
                if( strTok.nextToken().equals(startKey.trim()) )
                    break;
            }
        }
        if( !strTok.hasMoreTokens() )
        {
            throw new Exception("Error of extracting key. Key must follow '" + startKey + "' and delimited by ' ' or " + delimiters);
        }

        String key = strTok.nextToken(delimiters).trim();
        return escapeKey(key.split("          ")[0]);
    }

    public File getFile()
    {
        return fileName;
    }

    protected String getStartKey()
    {
        return startKey;
    }

    /**
     * Create entry.
     * @param name Name of the entry.
     * @param indexEntry Position of entry in the file.
     * @return Entry
     */
    final private Entry createEntry(String name, final IndexEntry indexEntry) throws IOException
    {
        Entry entry = null;
        if( indexEntry != null )
        {
            if( indexEntry.len > MAX_ENTRY_STR_SIZE )
                entry = new Entry(this, name, fileName, encoding, indexEntry.from, indexEntry.len);
            else
            {
                synchronized( fileLock )
                {
                    RandomAccessFile file = FilePool.acquire( fileName.getAbsolutePath() );
                    try
                    {
                        file.seek( indexEntry.from );
                        byte[] byte_buffer = new byte[(int)indexEntry.len];
                        file.read( byte_buffer, 0, (int)indexEntry.len );
                        entry = doCreateEntry(name, new String(byte_buffer, encoding));
                    }
                    finally
                    {
                        FilePool.release( fileName.getAbsolutePath(), file );
                    }
                }

            }
        }
        return entry;
    }

    protected Entry doCreateEntry(String name, String data)
    {
        return new Entry(this, name, data);
    }

    protected void addToIndex(String key, IndexEntry ind)
    {
        index.put(key, ind);
    }

    protected void removeFromIndex(String key)
    {
        index.remove(key);
    }

    protected void clearIndex()
    {
        index.clear();
    }

    protected void closeIndex() throws Exception
    {
        index.close();
    }

    protected static class FastFileReader
    {
        public static final int DEFAULT_BUFSIZE = 4096;

        private final RandomAccessFile raf;
        private final byte inbuf[];
        private long startpos = -1;
        private long endpos = -1;
        private final int bufsize;

        public FastFileReader(RandomAccessFile raf, int bufsize)
        {
            this.raf = raf;
            this.inbuf = new byte[bufsize];
            this.bufsize = bufsize;
        }

        protected int read(long pos)
        {
            if( pos < startpos || pos > endpos )
            {
                long blockstart = ( pos / bufsize ) * bufsize;
                int n;
                try
                {
                    raf.seek(blockstart);
                    n = raf.read(inbuf);
                }
                catch( IOException e )
                {
                    return -1;
                }
                startpos = blockstart;
                endpos = blockstart + n - 1;
                if( pos < startpos || pos > endpos )
                    return -1;
            }

            return inbuf[(int) ( pos - startpos )] & 0xffff;
        }

        /**
         * Optimized read line method
         */
        public final String fastReadLine(long pos) throws IOException
        {
            StringBuilder input = new StringBuilder(128);
            int c = -1;
            boolean eol = false;
            while( !eol )
            {
                switch( c = read(pos++) )
                {
                    case -1:
                    case '\n':
                        eol = true;
                        break;
                    case '\r':
                        eol = true;
                        if( ( read(pos++) ) != '\n' )
                        {
                            pos--;
                        }
                        break;
                    default:
                        input.append((char)c);
                        //input.append( Character.toChars( c ) );
                        break;
                }
            }

            raf.seek(pos);
            if( ( c == -1 ) && ( input.length() == 0 ) )
            {
                return null;
            }
            return input.toString();
        }
    }

    @Override
    protected void finalize() throws Throwable
    {
        close();
    }
}
