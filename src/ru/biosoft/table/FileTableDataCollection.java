package ru.biosoft.table;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Properties;

import javax.annotation.Nonnull;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.BTreeIndex;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.Entry;
import ru.biosoft.access.EntryCollection;
import ru.biosoft.access.FileEntryCollection2;
import ru.biosoft.access.core.Index.IndexEntry;
import ru.biosoft.table.access.FileDataTagCommand;
import ru.biosoft.table.access.TableDataEntryTransformer;
import ru.biosoft.table.access.TableDataTagCommand;
import ru.biosoft.util.TransformedIterator;

public class FileTableDataCollection extends StandardTableDataCollection
{
    protected static final Logger log = Logger.getLogger(FileTableDataCollection.class.getName());

    public static final String VALUE_DELIMITERS_PROPERTY = "value.delimiters";

    /**
     * Max size size for not indexed collection
     */
    public static final int SIZE_LIMIT = 10 * 1024 * 1024;

    /**
     * Indicates when use BTreeIndex
     */
    protected boolean useIndex = false;

    /**
     * Source file object
     */
    protected File file;

    /**
     * Entry collection with indexes support
     */
    protected IndexedCollection indexedCollection = null;

    public FileTableDataCollection(DataCollection<?> parent, String name)
    {
        this(parent, getProperties(name));
    }

    public static Properties getProperties(String name)
    {
        Properties properties = new Properties();
        properties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, name);
        return properties;
    }

    public FileTableDataCollection(DataCollection<?> parent, Properties properties)
    {
        super(parent, properties);
        
        if( properties.containsKey(DataCollectionConfigConstants.FILE_PATH_PROPERTY) )
        {
        	// fix for remote instance
        	file = new File(properties.getProperty(DataCollectionConfigConstants.FILE_PATH_PROPERTY) + properties.getProperty(DataCollectionConfigConstants.FILE_PROPERTY));
        }
        else
        {
        	file = DataCollectionUtils.getChildFile(parent, getName());
        }
        try
        {
            useIndex = file.length() > SIZE_LIMIT;
            if( useIndex )
            {
                //init indexed collection
                loadMetaInfoFromFile(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
                properties.put(EntryCollection.BLOCK_START_INDEXED_PROPERTY, TableDataTagCommand.DATA_TAG);
                properties.put(EntryCollection.BLOCK_END_INDEXED_PROPERTY, "//");
                properties.put(EntryCollection.ENTRY_ID_PROPERTY, "");
                properties.put(EntryCollection.ENTRY_START_PROPERTY, "");
                properties.put(EntryCollection.ENTRY_END_PROPERTY, "");
                indexedCollection = new IndexedCollection(parent, properties, file);
            }
            else
            {
                //load all data to memory
                if( file.canRead() )
                    loadFromFile(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
                sortOrder.set();
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not load data", e);
        }
    }

    @Override
    protected RowDataElement doGet(String name) throws Exception
    {
        if( useIndex )
        {
            Entry entry = indexedCollection.get(name);
            if( entry != null )
            {
                return FileDataTagCommand.parseLine(this, columnModel, entry.getData());
            }
            return null;
        }
        return super.doGet(name);
    }

    @Override
    protected void doPut(RowDataElement dataElement, boolean isNew) throws Exception
    {
        if( useIndex )
        {
            RowDataElement rde = dataElement;
            StringBuffer line = new StringBuffer();
            TableDataTagCommand.writeDataLine(rde, line);
            indexedCollection.put(new Entry(indexedCollection, dataElement.getName(), line.toString()));
        }
        else
        {
            super.doPut(dataElement, isNew);
        }
    }

    @Override
    protected void doRemove(String name) throws Exception
    {
        if( useIndex )
        {
            indexedCollection.remove(name);
        }
        else
        {
            super.doRemove(name);
        }
    }

    @Override
    public void close() throws Exception
    {
        if( useIndex )
        {
            indexedCollection.close();
        }
        super.close();
    }

    @Override
    public RowDataElement getAt(int rowIdx)
    {
        if( useIndex )
        {
            String name = getName(rowIdx);
            if( name != null )
            {
                try
                {
                    Entry entry = indexedCollection.get(name);
                    if( entry != null )
                    {
                        return FileDataTagCommand.parseLine(this, columnModel, entry.getData());
                    }
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "Can not get row: ", e);
                }
            }
            return null;
        }
        return super.getAt(rowIdx);
    }

    @Override
    public Object getValueAt(int rowIdx, int columnIdx)
    {
        if( useIndex )
        {
            RowDataElement row = getAt(rowIdx);
            if( row != null )
            {
                return row.getValues()[columnIdx];
            }
            return null;
        }
        return super.getValueAt(rowIdx, columnIdx);
    }

    @Override
    public void setValueAt(int rowIdx, int columnIdx, Object value)
    {
        if( useIndex )
        {
            RowDataElement row = getAt(rowIdx);
            if( row != null )
            {
                Object oldValue = row.getValues()[columnIdx];
                row.getValues()[columnIdx] = value;
                firePropertyChange("values", oldValue, value);
            }
        }
        else
        {
            super.setValueAt(rowIdx, columnIdx, value);
        }
    }

    @Override
    public void sortTable(int columnNumber, boolean dir)
    {
        if( useIndex )
        {
            throw new UnsupportedOperationException("sort method is unsupported yet for indexed tables");
        }
        super.sortTable(columnNumber, dir);
    }

    @Override
    public boolean isSortingSupported()
    {
        return !useIndex;
    }

    @Override
    public @Nonnull Iterator<RowDataElement> iterator()
    {
        if( useIndex )
        {
            return new TransformedIterator<Entry, RowDataElement>(indexedCollection.iterator())
            {
                @Override
                protected RowDataElement transform(Entry entry)
                {
                    return FileDataTagCommand.parseLine(FileTableDataCollection.this, columnModel, entry.getData());
                }
            };
        }
        return super.iterator();
    }

    @Override
    protected void updateRowIndexToId()
    {
        if( !useIndex )
            super.updateRowIndexToId();
    }

    ///////////////////////////

    /**
     * Directly add row without any notifications. Is used for load optimization
     */
    public void addDataDirectly(RowDataElement rde)
    {
        idToRow.put(rde.getName(), rde);
        rowIndexToId.add(rde.getName());
    }

    protected void loadMetaInfoFromFile(Reader reader) throws Exception
    {
        TableDataEntryTransformer transformer = new TableDataEntryTransformer(false);
        transformer.setBreakTag(TableDataTagCommand.DATA_TAG);
        transformer.readObject(this, reader);
    }

    protected void loadFromFile(Reader reader) throws Exception
    {
        TableDataEntryTransformer transformer = new TableDataEntryTransformer(false);
        transformer.setDelimitersPriority(new String[] {"\t"});
        transformer.addCommand(new FileDataTagCommand(TableDataTagCommand.DATA_TAG, transformer, this));
        transformer.readObject(this, reader);
    }

    /**
     * Save table to file
     */
    public void saveChanges()
    {
        if( useIndex )
        {
            //TODO: save actions
        }
        else
        {
            try
            {
                TableDataEntryTransformer transformer = new TableDataEntryTransformer(false);
                transformer.addCommand(new FileDataTagCommand(TableDataTagCommand.DATA_TAG, transformer, this));
                try(Writer fileWriter = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))
                {
                    fileWriter.write("ID\t");
                    fileWriter.write(getName());
                    fileWriter.write(TableDataEntryTransformer.endl);
                    transformer.writeObject(this, fileWriter);
                    fileWriter.write("//");
                    fileWriter.write(TableDataEntryTransformer.endl);
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not save table to file", e);
            }
        }
    }

    ///////////////////////////
    // Index support
    ///////////////////////////
    protected class IndexedCollection extends FileEntryCollection2
    {
        protected File indexFile;

        public IndexedCollection(DataCollection<?> parent, Properties properties, File baseFile) throws Exception
        {
            super(parent, properties, false);

            indexFile = new File(baseFile.getAbsolutePath() + ".id.names");
            if( indexFile.exists() )
            {
                readNameIndex();
            }
            createIndex( fileName.getAbsolutePath(), null, BTreeIndex.DEFAULT_BLOCK_SIZE );
        }

        @Override
        protected void doPut(Entry element, boolean isNew) throws Exception
        {
            super.doPut(element, isNew);
            rowIndexToId.add(element.getName());
        }

        @Override
        protected void doRemove(String name) throws Exception
        {
            super.doRemove(name);
            rowIndexToId.remove(name);
        }

        @Override
        protected void addToIndex(String key, IndexEntry ind)
        {
            super.addToIndex(key, ind);
            rowIndexToId.add(key);
        }

        @Override
        protected void removeFromIndex(String key)
        {
            rowIndexToId.remove(key);
        }

        @Override
        protected void clearIndex()
        {
            super.clearIndex();
            rowIndexToId.clear();
        }

        @Override
        protected void closeIndex() throws Exception
        {
            super.closeIndex();
            saveNameIndex();
        }

        protected void readNameIndex()
        {
            rowIndexToId.clear();
            try(BufferedReader br = ApplicationUtils.utfReader(indexFile))
            {
                String line = null;
                while( ( line = br.readLine() ) != null )
                {
                    rowIndexToId.add(line);
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not load name index", e);
            }
        }

        protected void saveNameIndex()
        {
            try(PrintWriter file = new PrintWriter(new OutputStreamWriter(new FileOutputStream(indexFile), StandardCharsets.UTF_8)))
            {
                for(String key : rowIndexToId)
                {
                    file.println(key);
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not save name index", e);
            }
        }
    }
}
