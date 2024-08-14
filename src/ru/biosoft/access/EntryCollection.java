package ru.biosoft.access;

import java.util.Properties;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;

/**
 * @author DevelopmentOnTheEdge
 * @version 1.0
 */
public abstract class EntryCollection extends AbstractDataCollection<Entry>
{
    /**
     * Property for storing delimiters that not conatains in the entry key.
     * Space ' ' added automatically.
     */
    public static final String ENTRY_DELIMITERS_PROPERTY = "entry.delimiters";
    /** Property for storing start field of entry */
    public static final String ENTRY_START_PROPERTY = "entry.start";
    /** Property for storing ID field of entry */
    public static final String ENTRY_ID_PROPERTY = "entry.id";
    /** Property for storing end field of entry */
    public static final String ENTRY_END_PROPERTY = "entry.end";
    /** Property which shows if file cannot be modified **/
    public static final String UNMODIFIABLE_PROPERTY = "unmodifiable";
    /** Property for storing key for start indexing **/
    public static final String BLOCK_START_INDEXED_PROPERTY = "block.start";
    /** Property for storing key for stop indexing **/
    public static final String BLOCK_END_INDEXED_PROPERTY = "block.end";
    /** Property for extracting full key after ID field of entry **/
    public static final String ENTRY_KEY_FULL = "key.full";
    /** Property to indicate whether special chars like '/' should be replaced */
    public static final String ENTRY_KEY_ESCAPE_SPECIAL_CHARS = "key.escape";


    /**
     * Standard data collection constructor.
     */
    public EntryCollection(DataCollection<?> parent, Properties properties)
    {
        super(parent, properties);
    }

    /** @return <code>Entry.class</code>. */
    @Override
    public @Nonnull Class<? extends Entry> getDataElementType()
    {
        return Entry.class;
    }
}
