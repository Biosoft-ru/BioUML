package ru.biosoft.access;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import ru.biosoft.access.core.Key;
import ru.biosoft.access.core.Index;



/**
 * Maps keys to entries. A index cannot contain duplicate keys;
 * each key can map to at most one entry.<p>
 *
 * Key and entries stored in file as Balanced Tree.<p>
 *
 * Keys are {@link String strings} and entryes are {@link Index.IndexEntry}.
 *
 * @see Index.IndexEntry
 * @todo Comment
 * @todo Optimize for speed
 * @todo Decrease size of index file when indexes removed.
 * @todo HIGH way for specify location for index (or create in temporary) - if dataFile on CD.
 * @todo Describe perfomance.
 */
public class BTreeIndex extends AbstractMap<String, Index.IndexEntry> implements Index<Index.IndexEntry>
{
    /** Default file block size. */
    final public static int DEFAULT_BLOCK_SIZE = 4096;

    /** Root block offset */
    final public static int ROOT_BLOCK_OFFSET = 4;

    private String indexName = DEFAULT_INDEX_NAME;

    /**
     * Construct index based on specified file.
     * If index file already created and blockSize of file not equals specified blockSize,
     * then all operations with index will be invalid and index file may be damaged.
     *
     * @param dataFile File for which index will be created. (or already created)
     * @param indexName Name of the index. Will be added to index file name.
     * @param blockSize Size of block for file operations.
     *
     * @throws IOException  if an I/O error occurs.
     * @todo Extended Comments needed.
     */
    public BTreeIndex(File dataFile, String indexName, String indexPath, int blockSize) throws IOException
    {
        this.blockSize = blockSize;

        if( dataFile == null )
            throw new IllegalArgumentException("dataFile not specified.");

        if( indexName == null )
            indexName = DEFAULT_INDEX_NAME;
        this.indexName = indexName;

        if( indexPath == null )
            indexPath = dataFile.getPath();

        indexFileName = new File(indexPath + "." + indexName);
        if( indexFileName.exists() )
        {
            if( !indexFileName.canRead() )
                throw new IOException("Cannot read file " + indexFileName);
            if( !indexFileName.canWrite() )
                throw new IOException("Cannot write file " + indexFileName);
            if( indexFileName.isDirectory() )
            {
                Path path = Paths.get( indexFileName.getAbsolutePath() );
                Files.walkFileTree( path, new SimpleFileVisitor<Path>()
                {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
                    {
                        Files.delete( file );
                        return FileVisitResult.CONTINUE;
                    }
                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
                    {
                        Files.delete( dir );
                        return FileVisitResult.CONTINUE;
                    }
                } );
                valid = false;
            }
            else
                valid = true;
        }

        if( valid )
        {
            synchronized( fileLock )
            {
                RandomAccessFile indexFile = FilePool.acquire( indexFileName.getAbsolutePath() );
                try
                {
                    indexFile.seek(0);
                    currSize = indexFile.readInt();
                }
                catch( EOFException exc )
                {
                    currSize = 0;
                    valid = false;
                }
                finally
                {
                    FilePool.release( indexFileName.getAbsolutePath(), indexFile );
                }
            }
        }
    }

    /**
     * Create index with {@link #DEFAULT_BLOCK_SIZE}.
     *
     * @param dataFile File for which index will be created. (or already created)
     * @param indexName Name of the index. Will be added to index file name.
     *
     * @throws IOException  if an I/O error occurs.
     *
     * @see #BTreeIndex(File,String,int)
     * @todo Extended Comments needed.
     */
    public BTreeIndex(File dataFile, String indexName, String indexPath) throws IOException
    {
        this(dataFile, indexName, indexPath, DEFAULT_BLOCK_SIZE);
    }

    @Override
    public String getName()
    {
        return indexName;
    }

    protected Node createNode(byte[] buffer, int pos, String key, IndexEntry value)
    {
        return new Node(buffer, pos, key, value);
    }

    protected Node createNode(byte[] buffer, int pos)
    {
        return new Node(buffer, pos);
    }

    public Iterator keyIterator(Key key)
    {
        throw new UnsupportedOperationException("Method keyIterator() not yet implemented.");
    }

    @Override
    public Iterator nodeIterator(Key key)
    {
        throw new UnsupportedOperationException("Method nodeIterator() not yet implemented.");
    }

    /**
     * Check is index file is valid.
     * Check existence and format of index file.
     * If file corrupt you must clear index.
     *
     * @see #clear()
     */
    @Override
    public boolean isValid()
    {
        return valid;
    }

    /**
     * Returns the number of key-entry mappings in this index.  If the
     * index contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
     * <tt>Integer.MAX_VALUE</tt>.
     *
     * @return the number of key-entry mappings in this index.
     */
    @Override
    public int size()
    {
        return currSize;
    }

    /**
     * Returns a set view of the mappings contained in this index. Each element
     * in the returned set is a <tt>Map.Entry</tt>.  The set is backed by the
     * index, so changes to the index are reflected in the set, and vice-versa.
     * If the index is modified while an iteration over the set is in progress,
     * the results of the iteration are undefined.  The set supports element
     * removal, which removes the corresponding mapping from the index, via the
     * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>, <tt>removeAll</tt>,
     * <tt>retainAll</tt> and <tt>clear</tt> operations.  It does not support
     * the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a set view of the mappings contained in this index.
     * @todo HIGH Test changes reflection!!!.
     */
    @Override
    public Set<Entry<String, IndexEntry>> entrySet()
    {
        return new BTreeIndexSet();
    }

    /**
     * Returns <tt>true</tt> if this index contains entry for the specified
     * key.
     *
     * @param key key whose presence in this index is to be tested.
     * @return <tt>true</tt> if this index contains entry for the specified
     * key.
     *
     * @throws ClassCastException if the key is not of type {@link String}.
     * @throws NullPointerException key is <tt>null</tt> and this index does not
     *         permit <tt>null</tt> keys.
     * @throws RuntimeException  if an I/O error occurs.
     */
    @Override
    public boolean containsKey(Object key)
    {
        return get(key) != null;
    }

    /**
     * Associates the specified entry with the specified key in this index.
     * If the index previously contained entry for this key,
     * the old entry is replaced.
     *
     * @param _key key with which the specified entry is to be associated.
     * @param entry entry to be associated with the specified key.
     * @return previous entry associated with specified key, or <tt>null</tt>
     *         if there was no mapping for key.
     *
     * @throws ClassCastException if the key is not of type {@link String}.
     *            or entry is not of type {@link Index.IndexEntry}.
     * @throws IllegalArgumentException if some aspect of this key or entry
     *            prevents it from being stored in this index.
     * @throws NullPointerException this index does not permit <tt>null</tt>
     *            keys or entries, and the specified key or entry is
     *            <tt>null</tt>.
     * @throws RuntimeException  if an I/O error occurs.
     */
    @Override
    public IndexEntry put(String key, IndexEntry entry)
    {
        try
        {
            if( root == null )
                root = getBlock(ROOT_BLOCK_OFFSET);
            IndexEntry ret = root.put(key, entry, null, null, false);
            if( ret == null )
            {
                if( currSize != Integer.MAX_VALUE )
                    currSize++;

                writeCurrSize();
            }
            return ret;
        }
        catch( IOException exc )
        {
            throw new RuntimeException("put(Object,Object) failed.", exc);
        }
    }
    
    protected void writeCurrSize() throws IOException
    {
        synchronized( fileLock )
        {
            RandomAccessFile indexFile = FilePool.acquire( indexFileName.getAbsolutePath() );
            try
            {
                indexFile.seek(0);
                indexFile.writeInt(currSize);
            }
            finally
            {
                FilePool.release( indexFileName.getAbsolutePath(), indexFile );
            }
        }

    }

    /**
     * Returns the entry to which this index maps the specified key. Returns
     * <tt>null</tt> if the index contains no entry for this key.
     *
     * @param key key whose associated entry is to be returned.
     * @return the {@link Index.IndexEntry entry} to which this index maps the specified key,
     *         or <tt>null</tt> if the index not contains entry for this key.
     *
     * @throws ClassCastException if the key is not of type {@link String}.
     * @throws NullPointerException key is <tt>null</tt> and this index does not
     *         permit <tt>null</tt> keys.
     * @throws RuntimeException  if an I/O error occurs.
     *
     * @see Index.IndexEntry
     */
    @Override
    public IndexEntry get(Object key)
    {
        try
        {
            if( root == null )
                root = getBlock(ROOT_BLOCK_OFFSET);
            return root.get((String)key);
        }
        catch( IOException exc )
        {
            throw new RuntimeException("get(" + key + ") failed.", exc);
        }
    }

    /**
     * Removes the entry for this key from this index if present.
     *
     * @param key key whose entry is to be removed from the index.
     * @return previous {@link Index.IndexEntry entry} associated with
     *         specified key, or <tt>null</tt> if there was no entry for key.
     *
     * @throws ClassCastException if the key is not of type {@link String}.
     * @throws RuntimeException  if an I/O error occurs.
     */
    @Override
    public IndexEntry remove(Object key)
    {
        try
        {
            if( root == null )
                root = getBlock(ROOT_BLOCK_OFFSET);
            Status status = new Status();
            IndexEntry removed = root.remove((String)key, status);
            if(removed != null)
                currSize--;
            BTreeIndex.comparator.setUseValue(false);

            writeCurrSize();
            return removed;
        }
        catch( IOException exc )
        {
            throw new RuntimeException("remove(" + key + ") failed.", exc);
        }
    }

    /**
     * Returns index file. This information is essential to remove index files.
     *
     * @return index file.
     */
    @Override
    public File getIndexFile()
    {
        return indexFileName;
    }

    /**
     * Removes all keys and entries from this index.
     * File stored key-entries recreates.
     *
     * @throws RuntimeException  if an I/O error occurs.
     */
    @Override
    public void clear()
    {
        try
        {
            cache.clear();
            root = null;
            currSize = 0;

            synchronized( fileLock )
            {
                RandomAccessFile indexFile = FilePool.acquire( indexFileName.getAbsolutePath() );
                try
                {
                    indexFile.setLength( 0 );
                    indexFile.seek( 0 );
                    indexFile.writeInt( currSize );
                }
                finally
                {
                    FilePool.release( indexFileName.getAbsolutePath(), indexFile );
                }
            }
        }
        catch( IOException exc )
        {
            throw new RuntimeException("clear() failed.", exc);
        }
    }

    /**
     * Close this index and releases resources (index file).
     * A closed index cannot perform any operations and cannot be reopened.
     *
     * @throws  IOException  if an I/O error occurs.
     */
    @Override
    public void close() throws IOException
    {
        FilePool.close( indexFileName.getAbsolutePath() );
        cache.clear();
        root = null;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Private

    /** Comparator for compare keys of nodes. */
    final private static NodeComparator comparator = new NodeComparator();

    /** Is index valid */
    private boolean valid = false;

    /** Name of index file */
    private File indexFileName = null;

    /** Index file */
    protected Object fileLock = new Object();

    /** Block size for file operations. */
    private int blockSize = DEFAULT_BLOCK_SIZE;

    /** Current size of index */
    protected int currSize = 0;

    /** Root */
    protected Block root = null;

    /** Cache */
    private final TIntObjectMap<Reference<Block>> cache = new TIntObjectHashMap<>();

    protected Block getBlock(int offset) throws IOException
    {
        Reference<Block> ref = cache.get(offset);
        Block block = null;
        if( ref != null )
            block = ref.get();
        if( block == null )
        {
            block = new Block(offset);
            cache.put(offset, new SoftReference<>(block));
        }
        return block;
    }

    /**
     * One block of index file.
     */
    protected class Block
    {
        protected boolean parsed = false;
        /** Buffer for block. */
        private final byte[] buffer = new byte[blockSize];
        /** Offset of the block in the index file. */
        private int offset = 0;

        protected List<Node> nodes = new ArrayList<>(64);
        protected List<Pointer> childs = new ArrayList<>(64);

        /**
         * Construct block that already stored in index file.
         * @param offset Offset of the block in index file.
         */
        public Block(int offset) throws IOException
        {
            this.offset = offset;

            synchronized( fileLock )
            {
                RandomAccessFile indexFile = FilePool.acquire( indexFileName.getAbsolutePath() );
                try
                {
                    indexFile.seek( offset );
                    indexFile.read( buffer );
                }
                finally
                {
                    FilePool.release( indexFileName.getAbsolutePath(), indexFile );
                }
            }
        }

        /**
         * Construct new block at the end of the file.
         * Increase file size on size of the block.
         */
        public Block() throws IOException
        {
            synchronized( fileLock )
            {
                RandomAccessFile indexFile = FilePool.acquire( indexFileName.getAbsolutePath() );
                try
                {
                    this.offset = (int)indexFile.length();
                    indexFile.seek( offset );
                    indexFile.setLength( offset + blockSize );
                }
                finally
                {
                    FilePool.release( indexFileName.getAbsolutePath(), indexFile );
                }
            }
        }

        /**
         * Clear block.
         * Fill block with 0.
         */
        public void clear(int from)
        {
            java.util.Arrays.fill(buffer, from, blockSize, (byte)0x00);
        }

        /**
         * Put key,value in index.
         * @param key Key of inserted element.
         * @param value Value of inserted element.
         * @param retInsertNode Node which should be added to parent (used for return it from recursion).
         * @param retInsertPointer Pointer to right child from returned retInsertNode.
         * @return Previous value for key or null.
         */
        final synchronized IndexEntry put(String key, IndexEntry value, Node retInsertNode, Pointer retInsertPointer, boolean append) throws IOException
        {
            comparator.setUseValue(append);

            IndexEntry retValue = null;
            // Read all elements in block.
            int emptySize = parseBlock();

            // If leaf is empty, add element
            int nodeCount = nodes.size();
            if( nodeCount == 0 )
            {
                add(key, value, 0, 0, countSize(key, value));
                store();
                return null;
            }

            // Looking for child which should contain specified key.
            Node keyNode = createNode(buffer, 0, key, value);
            int childIndex = java.util.Collections.binarySearch(nodes, keyNode, comparator);
            if( childIndex < 0 )
                childIndex = -1 - childIndex;
            else
            {
                if( append )
                {
                }
                else
                {
                    // This key already exists, so simply change its value
                    Node node = nodes.get(childIndex);
                    retValue = node.getValue();
                    node.value = value;
                    node.write();
                    store();
                    return retValue;
                }
            }

            int childOffset = ( childs.get(childIndex) ).getPointer();
            if( childOffset > 0 )
            {
                // Recursively put element in child
                Node insertNode = createNode(buffer, 0);
                insertNode.key = null;
                Pointer insertPointer = new Pointer(buffer, 0);
                retValue = getBlock(childOffset).put(key, value, insertNode, insertPointer, append);

                // Returns if rebalancing not needed
                if( insertNode.key == null )
                    return retValue;

                // Rebalancing: put in this block element returned from below
                key = insertNode.key;
                value = insertNode.getValue();
                childOffset = insertPointer.getPointer();
            }
            int sz = countSize(key, value);
            if( emptySize >= sz )
            {
                add(key, value, childOffset, childIndex, sz);
                store();
            }
            else
            {
                if( offset == ROOT_BLOCK_OFFSET )
                    splitRoot(key, value, childOffset);
                else
                {
                    Block newBlock = new Block();
                    int middleIndex = nodes.size() / 2;
                    List<Node> oldNodes = new ArrayList<>(nodes);
                    List<Pointer> oldChilds = new ArrayList<>(childs);

                    read(oldNodes, oldChilds);

                    place(oldNodes, oldChilds, 0, middleIndex);
                    Node min = oldNodes.get(middleIndex);
                    newBlock.place(oldNodes, oldChilds, middleIndex + 1, oldNodes.size() - middleIndex - 1);
                    int compared = comparator.compare(min, keyNode);// min.key.compareTo(key);
                    if( compared <= 0 ) //VLADZ!!! TMP was compared<0
                    {
                        newBlock.add(key, value, childOffset);
                        newBlock.store();
                    }
                    else
                    {
                        //VLADZ!!!
                        if( childIndex >= childs.size() )
                            System.out.println("VLADZ!!!! ERROR");
                        add(key, value, childOffset, childIndex, sz);
                        store();
                    }

                    retInsertNode.value = min.getValue();
                    retInsertNode.key = min.key;
                    retInsertPointer.setPointer(newBlock.getOffset());
                }
            }
            return retValue;
        }

        /**
         * Returns element for specified key.
         * @param key Key of element.
         * @return IndexEntry for specified key.
         * @throws IOException If IO error occured.
         */
        synchronized IndexEntry get(String key) throws IOException
        {
            parseBlock();
            int nextBlockOffset;
            Node keyNode = createNode(buffer, 0, key, null);
            int childIndex = java.util.Collections.binarySearch(nodes, keyNode, comparator);
            if( childIndex >= 0 )
            {
                Node node = nodes.get(childIndex);
                return node.getValue();
            }
            childIndex = -1 - childIndex;
            nextBlockOffset = ( childs.get(childIndex) ).getPointer();
            if( nextBlockOffset <= 0 )
                return null;
            return getBlock(nextBlockOffset).get(key);
        }

        /**
         *
         */
        synchronized IndexEntry remove(String key, Status retStatus) throws IOException
        {
            IndexEntry retValue = null;
            parseBlock();

            Node keyNode = createNode(buffer, 0, key, null);
            int childIndex = java.util.Collections.binarySearch(nodes, keyNode, comparator);
            if( childIndex < 0 )
            {
                childIndex = -1 - childIndex;
                // remove from child
                Pointer p = childs.get(childIndex);
                int nextBlockOffset = p.getPointer();
                if( nextBlockOffset > 0 )
                {
                    Block nextBlock = getBlock(nextBlockOffset);
                    Status status = new Status();
                    retValue = nextBlock.remove(key, status);
                    if( status.status == EMPTY )
                    {
                        p.setPointer(0);
                        p.write();
                        store();
                    }
                }
            }
            else
            {
                int childCount = countChildren();
                if( childCount == 0 )
                {
                    Node node = nodes.get(childIndex);
                    retValue = node.getValue();
                    removeFromLeaf(childIndex);
                    if( nodes.size() == 0 )
                    {
                        retStatus.status = EMPTY;
                        /*if( getOffset() == ROOT_BLOCK_OFFSET )
                        {
                            // root cleared
                            @todo Clear index.
                        }
                        else
                        {
                        }*/
                    }
                    store();
                }
                else
                {
                    /** @todo remove */
                    int nodeCount = nodes.size();
                    if( nodeCount == 1 )
                    {
                        Node node = nodes.get(childIndex);
                        retValue = node.getValue();
                        // remove last
                        Pointer left = childs.get(childIndex);
                        Pointer right = childs.get(childIndex + 1);
                        int leftPointer = left.getPointer();
                        int rightPointer = right.getPointer();
                        if( leftPointer > 0 && rightPointer > 0 )
                        {
                            removeFromLeaf(childIndex);
                            Block block = getBlock(right.getPointer());
                            Iterator<Entry<String, IndexEntry>> iter = block.iterator();
                            Entry<String, IndexEntry> entry = iter.next();
                            String rKey = entry.getKey();
                            IndexEntry rValue = entry.getValue();
                            Status status = new Status();
                            block.remove(rKey, status);
                            if( status.status == EMPTY )
                            {
                                right.setPointer(0);
                            }
                            add(rKey, rValue, right.getPointer());
                            store();
                        }
                        else if( right.getPointer() > 0 )
                        {
                            Block block = getBlock(right.getPointer());
                            Iterator<Entry<String, IndexEntry>> iter = block.iterator();
                            Entry<String, IndexEntry> entry = iter.next();
                            String rKey = entry.getKey();
                            IndexEntry rValue = entry.getValue();
                            Status status = new Status();
                            block.remove(rKey, status);
                            Node added = createNode(buffer, left.end, rKey, rValue);
                            added.write();
                            right.offset = added.end;
                            right.end = right.offset + Pointer.SIZE;
                            if( status.status == EMPTY )
                            {
                                right.setPointer(0);
                            }
                            right.write();
                            nodes.clear();
                            nodes.add(added);
                            store();
                        }
                        else
                        {
                            Block block = getBlock(left.getPointer());
                            Iterator<Entry<String, IndexEntry>> iter = block.iterator();
                            Entry<String, IndexEntry> entry = null;
                            while( iter.hasNext() )
                                entry = iter.next();
                            String rKey = entry.getKey();
                            IndexEntry rValue = entry.getValue();
                            Status status = new Status();
                            block.remove(rKey, status);
                            Node added = createNode(buffer, left.end, rKey, rValue);
                            added.write();
                            right.offset = added.end;
                            right.end = right.offset + Pointer.SIZE;
                            if( status.status == EMPTY )
                            {
                                left.setPointer(0);
                            }
                            left.write();
                            right.write();
                            nodes.clear();
                            nodes.add(added);
                            store();
                        }
                    }
                    else
                    {
                        Node node = nodes.get(childIndex);
                        retValue = node.getValue();
                        Pointer left = childs.get(childIndex);
                        Pointer right = childs.get(childIndex + 1);
                        int leftPointer = left.getPointer();
                        int rightPointer = right.getPointer();
                        if( leftPointer > 0 && rightPointer > 0 )
                        {
                            removeFromLeaf(childIndex);

                            Block block = getBlock(right.getPointer());
                            Iterator<Entry<String, IndexEntry>> iter = block.iterator();
                            Entry<String, IndexEntry> entry = iter.next();
                            String rKey = entry.getKey();
                            IndexEntry rValue = entry.getValue();
                            Status status = new Status();
                            block.remove(rKey, status);

                            if( status.status == EMPTY )
                            {
                                right.setPointer(0);
                            }
                            add(rKey, rValue, right.getPointer());
                            store();
                        }
                        else if( leftPointer > 0 )
                        {
                            removeFromLeaf(childIndex);
                            store();
                        }
                        else
                        {
                            removeFromLeaf(childIndex);
                            left.setPointer(rightPointer);
                            left.write();
                            store();
                        }
                    }

                }
            }
            return retValue;
        }

        /**
         * Remove nodes[childIndex] and childs[childIndex+1].
         */
        final private void removeFromLeaf(int childIndex) throws IOException
        {
            Pointer left = childs.get(childIndex);
            Pointer right = childs.get(childIndex + 1);
            left.getPointer();
            right.getPointer();
            int move = right.end - left.end;
            System.arraycopy(buffer, right.end, buffer, left.end, blockSize - right.end);

            nodes.remove(childIndex);
            childs.remove(childIndex + 1);
            int nodeCount = nodes.size();
            for( int i = childIndex; i < nodeCount; i++ )
            {
                Node n = nodes.get(i);
                n.offset -= move;
                n.end -= move;
                Pointer p = childs.get(i + 1);//+1 );
                p.offset -= move;
                p.end -= move;
            }
            if( nodeCount > 0 )
            {
                Pointer last = childs.get(childs.size() - 1);
                clear(last.end);
            }
            else
                clear(Pointer.SIZE);
            store();
        }

        /**
         *
         */
        Iterator<Entry<String, IndexEntry>> iterator()
        {
            return new BlockIterator();
        }

        /**
         * @todo VLADZ!!! STUB optimize it!!!
         */
        Iterator<Entry<String, IndexEntry>> iterator(Key indexKey)
        {
            String key = indexKey.serializeToString();
            Iterator<Entry<String, IndexEntry>> iter = new BlockIterator();
            int count = 0;
            while( iter.hasNext() )
            {
                Entry<String, IndexEntry> entry = iter.next();
                if( key.compareTo(entry.getKey()) <= 0 )
                    break;
                count++;
            }

            iter = new BlockIterator();
            while( count > 0 )
            {
                count--;
                iter.next();
            }
            return iter;
        }

        // @todo VLADZ!!! There is some bugs
        //        Iterator iterator( Key indexKey ) throws IOException
        //        {
        //            String key = indexKey.serializeToString();
        //            parseBlock();
        //            int nextBlockOffset;
        //                Node keyNode = (Node)createNode(buffer,0,key,null);
        //                int childIndex = java.util.Collections.binarySearch( nodes,keyNode,comparator );
        //                if ( childIndex < 0 )
        //                    childIndex = -1-childIndex;
        //                else
        //                {
        //                    return new BlockIterator( childIndex );
        //                }
        //                nextBlockOffset = ((Pointer)childs.get( childIndex )).getPointer();
        //
        //                if( nextBlockOffset==0 )
        //                {
        //                    return new BlockIterator( childIndex );
        //                }
        //            if ( nextBlockOffset<=0 )
        //                return null;
        //            return getBlock(nextBlockOffset).iterator( indexKey );//            return new BlockIterator( key );
        //        }

        /**
         *
         */
        class BlockIterator implements Iterator<Entry<String, IndexEntry>>
        {
            Iterator<Entry<String, IndexEntry>> currIterator = null;
            int currChild = 0;
            boolean readNode = false;
            boolean has = false;
            boolean hasPresent = false;

            //            public BlockIterator( Key key )
            //            {
            //                parseBlock();
            //            }

            public BlockIterator(int index)
            {
                parseBlock();
                currChild = index;
            }

            public BlockIterator()
            {
                parseBlock();
            }

            protected Block getOwnerBlock()
            {
                return Block.this;
            }

            @Override
            public boolean hasNext()
            {
                try
                {
                    if( hasPresent )
                        return has;
                    has = hasNextInternal();
                    hasPresent = true;
                    return has;
                }
                catch( IOException exc )
                {
                    throw new RuntimeException("BlockIterator.hasNext() failed.", exc);
                }
            }
            
            private boolean hasNextInternal() throws IOException
            {
                if( !readNode )
                {
                    if( currIterator != null && currIterator.hasNext() )
                        return true;
                    int childOffset = childs.get(currChild).getPointer();
                    if( childOffset > 0 )
                    {
                        currIterator = getBlock(childOffset).iterator();
                        return currIterator.hasNext();
                    }
                }
                return currChild < nodes.size();
            }

            @Override
            public Entry<String, IndexEntry> next()
            {
                if( hasNext() == false )
                    throw new NoSuchElementException("No more elements in iterator.");
                hasPresent = false;

                try
                {
                    if( readNode == false )
                    {
                        if( currIterator != null )
                        {
                            Entry<String, IndexEntry> ret = currIterator.next();
                            if( !currIterator.hasNext() )
                            {
                                currIterator = null;
                                readNode = true;
                            }
                            return ret;
                        }

                        Pointer child = childs.get(currChild);
                        if( child.getPointer() > 0 )
                        {
                            currIterator = getBlock(child.getPointer()).iterator();
                            currChild++;
                            return currIterator.next();
                        }
                    }
                    readNode = false;
                    if( currChild >= nodes.size() )
                        throw new NoSuchElementException("No more elements in iterator.");
                    return nodes.get(currChild++);
                }
                catch( IOException exc )
                {
                    throw new RuntimeException("BlockIterator.next() failed", exc);
                }
            }
        }

        final private int countChildren()
        {
            int count = 0;
            for(Pointer p : childs)
            {
                if( p.getPointer() > 0 )
                    count++;
            }
            return count;
        }

        final private void add(String key, IndexEntry value, int childOffset)
        {
            Node keyNode = createNode(buffer, 0, key, value);
            int childIndex = java.util.Collections.binarySearch(nodes, keyNode, comparator);
            if( childIndex < 0 )
                childIndex = -1 - childIndex;
            int sz = countSize(key, value);
            add(key, value, childOffset, childIndex, sz);
        }

        final private void add(String key, IndexEntry value, int childOffset, int childIndex, int sz)
        {
            Pointer child = childs.get(childIndex);
            int length = blockSize - child.offset - sz;

            System.arraycopy(buffer, child.offset, buffer, child.offset + sz, length);
            int end = child.end;

            int nodeCount = nodes.size();
            for( int i = childIndex; i < nodeCount; i++ )
            {
                Pointer p = childs.get(i + 1);
                p.offset += sz;
                p.end += sz;
                Node n = nodes.get(i);
                n.offset += sz;
                n.end += sz;
            }

            Node insertNode = createNode(buffer, end, key, value);
            insertNode.write();
            nodes.add(childIndex, insertNode);

            Pointer p = new Pointer(buffer, insertNode.end, childOffset);
            p.write();
            childs.add(childIndex + 1, p);
        }

        final private void splitRoot(String key, IndexEntry value, int rightChildOffset) throws IOException
        {
            int middleIndex = nodes.size() / 2;
            Node middleNode = nodes.get(middleIndex);

            IndexEntry middleValue = middleNode.getValue();

            read(nodes, childs);
            Block leftBlock = new Block();
            leftBlock.place(nodes, childs, 0, middleIndex);
            Block rightBlock = new Block();
            rightBlock.place(nodes, childs, middleIndex + 1, nodes.size() - middleIndex - 1);

            this.nodes.clear();
            this.childs.clear();

            Pointer leftChild = new Pointer(buffer, 0);
            Node node = createNode(buffer, leftChild.end, middleNode.key, middleValue);
            Pointer rightChild = new Pointer(buffer, node.end);
            leftChild.setPointer(leftBlock.getOffset());
            leftChild.write();
            this.childs.add(leftChild);
            rightChild.setPointer(rightBlock.getOffset());
            rightChild.write();
            this.childs.add(rightChild);
            node.write();
            this.nodes.add(node);

            clear(rightChild.end);
            store();

            Block childBlock = rightBlock;

            Node keyNode = createNode(null, 0, key, value);
            if( comparator.compare(keyNode, middleNode) < 0 )
            //            if ( key.compareTo(middleNode.key)<0 )
            {
                childBlock = leftBlock;
            }

            childBlock.add(key, value, rightChildOffset);
            leftBlock.store();
            rightBlock.store();
        }

        final private void place(List<Node> nodes, List<Pointer> childs, int from, int size) throws IOException
        {
            this.childs.clear();
            this.nodes.clear();

            int pos = 0;
            for( int i = from; i < from + size; i++ )
            {
                Pointer p = childs.get(i);
                int pointer = p.getPointer();
                Pointer newp = new Pointer(buffer, pos);
                newp.setPointer(pointer);
                newp.write();
                this.childs.add(newp);

                Node n = nodes.get(i);
                Node newNode = createNode(buffer, newp.end, n.key, n.getValue());
                newNode.write();
                pos = newNode.end;
                this.nodes.add(newNode);
            }
            Pointer p = childs.get(from + size);
            int pointer = p.getPointer();
            Pointer newp = new Pointer(buffer, pos, pointer);
            //            newp.setPointer( pointer );
            newp.write();
            this.childs.add(newp);

            clear(newp.end);
            store();
        }

        final void store() throws IOException
        {

            synchronized( fileLock )
            {
                RandomAccessFile indexFile = FilePool.acquire( indexFileName.getAbsolutePath() );
                try
                {
                    indexFile.seek( offset );
                    indexFile.write( buffer );
                }
                finally
                {
                    FilePool.release( indexFileName.getAbsolutePath(), indexFile );
                }
            }

        }

        final void read(List<Node> nodes, List<Pointer> childs)
        {
            Pointer p;
            int nodeCount = nodes.size();
            for( int i = 0; i < nodeCount; i++ )
            {
                Node node = nodes.get(i);
                node.getValue();
                p = childs.get(i);
                p.getPointer();
            }
            p = childs.get(nodeCount);
            p.getPointer();
        }

        final int getOffset()
        {
            return offset;
        }

        final int countSize(String key, IndexEntry value)
        {
            return value.length() + key.length() + 1+ 8 + 4;
        }

        int parseBlock()
        {
            if( parsed )
                return blockSize - ( childs.get(childs.size() - 1) ).end;

            Pointer child;
            Node node;
            int pos = 0;
            do
            {
                child = new Pointer(buffer, pos);
                childs.add(child);
                pos = child.end;
                node = createNode(buffer, pos);
                if( node.key == null )
                    break;
                nodes.add(node);
                pos = node.end;
            }
            while( true );
            parsed = true;
            return blockSize - pos;
        }
    }// end of class Block


    final static int NOTHING = 0;
    final static int EMPTY = 1;
    private static class Status
    {
        int status = NOTHING;
    }

    /**
     * Represent node entry in index block (pair key-value).
     */
    static class Node implements Map.Entry<String, IndexEntry>
    {
        int offset;
        int end;
        String key = null;
        IndexEntry value = null;
        byte[] buffer = null;

        public Node(byte[] buffer, int pos, String key, IndexEntry value)
        {
            this.buffer = buffer;
            this.key = key;
            this.value = value;
            offset = pos;
            end = offset + key.length() + 9;
        }

        public Node(byte[] buffer, int pos)
        {
            init(buffer, pos);
        }

        protected void init(byte[] buffer, int pos)
        {
            this.buffer = buffer;
            offset = pos;
            end = pos + 8;

            if( end >= buffer.length || buffer[end] == 0 )
                return;

            byte ch;
            int i = 0;
            char[] tmp = new char[1024];

            while( ( ch = buffer[end++] ) != '\r' )
            {
                tmp[i++] = (char)ch;
            }
            key = new String(tmp, 0, i);
        }

        @Override
        final public String getKey()
        {
            return key;
        }

        @Override
        public IndexEntry getValue()
        {
            if( value == null )
            {
                value = new IndexEntry(readInt(offset), readInt(offset + 4));
            }
            return value;
        }

        @Override
        final public IndexEntry setValue(IndexEntry newValue)
        {
            IndexEntry oldValue = getValue();
            value = newValue;
            return oldValue;
        }

        private void write(String key, IndexEntry value)
        {
            writeInt(offset, (int)value.from);
            writeInt(offset + 4, (int)value.len);

            int l = key.length();
            int pos = offset + 8;
            for( int i = 0; i < l; i++ )
            {
                buffer[pos + i] = (byte)key.charAt(i);
            }
            buffer[pos + l] = (byte)'\r';
        }

        private void write()
        {
            write(key, value);
        }

        private final void writeInt(int pos, int value)
        {
            buffer[pos++] = (byte) ( ( value >>> 24 ) & 0xFF );
            buffer[pos++] = (byte) ( ( value >>> 16 ) & 0xFF );
            buffer[pos++] = (byte) ( ( value >>> 8 ) & 0xFF );
            buffer[pos] = (byte) ( value );
        }

        private final int readInt(int pos)
        {
            int ch1, ch2, ch3, ch4;
            ch1 = buffer[pos++];
            ch2 = ( buffer[pos++] ) & 0xFF;
            ch3 = ( buffer[pos++] ) & 0xFF;
            ch4 = ( buffer[pos] ) & 0xFF;
            return ( ( ch1 << 24 ) + ( ch2 << 16 ) + ( ch3 << 8 ) + ( ch4 ) );
        }
    }// end of class Node

    static private class Pointer
    {
        public static final int SIZE = 4;
        int end;
        int pointer = 0;
        int offset;
        byte[] buffer;
        boolean pointerReaded = false;

        public Pointer(byte[] buffer, int pos)
        {
            this.buffer = buffer;
            offset = pos;
            end = pos + SIZE;
        }

        public Pointer(byte[] buffer, int pos, int pointer)
        {
            this.buffer = buffer;
            offset = pos;
            pointerReaded = true;
            this.pointer = pointer;
            end = pos + SIZE;
        }

        final int getPointer()
        {
            if( !pointerReaded )
            {
                pointerReaded = true;
                pointer = readInt(offset);
            }
            return pointer;
        }

        final void setPointer(int pointer)
        {
            pointerReaded = true;
            this.pointer = pointer;
        }

        final public void write()
        {
            writeInt(offset, pointer);
        }

        final void writeInt(int pos, int value)
        {
            buffer[pos++] = (byte) ( ( value >>> 24 ) & 0xFF );
            buffer[pos++] = (byte) ( ( value >>> 16 ) & 0xFF );
            buffer[pos++] = (byte) ( ( value >>> 8 ) & 0xFF );
            buffer[pos] = (byte) ( value );
        }

        final int readInt(int pos)
        {
            int ch1, ch2, ch3, ch4;
            ch1 = buffer[pos++];
            ch2 = ( buffer[pos++] ) & 0xFF;
            ch3 = ( buffer[pos++] ) & 0xFF;
            ch4 = ( buffer[pos] ) & 0xFF;
            return ( ( ch1 << 24 ) + ( ch2 << 16 ) + ( ch3 << 8 ) + ( ch4 ) );
        }
    }// end of class Pointer

    private class BTreeIndexSet extends AbstractSet<Entry<String, IndexEntry>>
    {
        @Override
        public int size()
        {
            return BTreeIndex.this.size();
        }

        @Override
        public Iterator<Entry<String, IndexEntry>> iterator()
        {
            try
            {
                return getBlock(ROOT_BLOCK_OFFSET).iterator();
            }
            catch( IOException exc )
            {
                throw new RuntimeException("BTreeIndexSet.iterator() failed.", exc);
            }
        }
    }// end of class BTreeIndexSet

    static private class NodeComparator implements java.util.Comparator<Node>
    {
        @Override
        public int compare(Node n1, Node n2)
        {
            int compare = n1.key.compareTo(n2.key);
            if( compare == 0 )
            {
                Object v1 = n1.getValue();
                Object v2 = n2.getValue();
                if( ( v1 instanceof StringIndexEntry ) && ( v2 instanceof StringIndexEntry ) )
                {
                    String s1 = ( (StringIndexEntry)v1 ).value;
                    String s2 = ( (StringIndexEntry)v2 ).value;
                    return s1.compareTo(s2);
                }
            }
            return compare;
        }
        @Override
        public boolean equals(Object o)
        {
            return ( this == o );
        }
        public void setUseValue(boolean useValue)
        {
            this.useValue = useValue;
        }
        private boolean useValue = false;
    }// end of class NodeComparator

    public static class IntKey implements Key
    {
        public IntKey(int number)
        {
            key = number;
        }

        public IntKey(String strKey)
        {
            key = Integer.parseInt(strKey);
        }

        @Override
        public String serializeToString()
        {
            DecimalFormat formatter = new DecimalFormat("0000000000");
            return formatter.format(key);
        }

        @Override
        public boolean accept(Object arg)
        {
            int number = Integer.parseInt((String)arg);
            return number >= key;
        }

        @Override
        public String removeKey()
        {
            return "delete____";
        }

        private int key = 0;
    }

}// end of class BTreeIndex