package ru.biosoft.bsa.transformer;

import java.io.BufferedReader;
import java.io.File;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.Entry;
import ru.biosoft.access.core.AbstractTransformer;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataCollectionInfo;
import ru.biosoft.access.core.Index;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.bsa.Alphabet;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.ErrorLetterPolicy;
import ru.biosoft.bsa.MapAsVector;
import ru.biosoft.bsa.Nucleotide15LetterAlphabet;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceFactory;
import ru.biosoft.util.ExProperties;

/**
 * Support class for sequence transformers.
 */
public abstract class SequenceTransformer extends AbstractTransformer<Entry, AnnotatedSequence>
{
    /**
     * Maximal length of sequence to be created as LinearSequence,
     * otherwise it will be created as LongSequence.
     */
    public static final int MAX_MEMORY_SEQUENCE_LENGTH = 32000;

    protected static final Logger cat = Logger.getLogger(SequenceTransformer.class.getName());

    private DataCollection primaryCollection;
    
    /* @return Entry.class */
    private Index sequenceIndex = null;

    @Override
    public void init(DataCollection primaryCollection, DataCollection transformedCollection)
    {
        super.init(primaryCollection, transformedCollection);

        this.primaryCollection = primaryCollection;
        
        DataCollectionInfo collectionInfo = transformedCollection.getInfo();
        if( collectionInfo!=null )
        {
            QuerySystem querySystem = collectionInfo.getQuerySystem();
            if( querySystem == null )
            {
                querySystem = new SequenceQuerySystem(transformedCollection);
                collectionInfo.setQuerySystem(querySystem);

                try
                {
                    sequenceIndex = querySystem.getIndex(SequenceQuerySystem.SEQUENCE_INDEX);
                    File indexFile = sequenceIndex.getIndexFile();
                    if( indexFile != null )
                        collectionInfo.addUsedFile(indexFile);
                }
                catch(Throwable t)
                {
                    cat.log(Level.SEVERE, "Can not add SequenceQuerySystem index to list of used files. " +
                              "ru.biosoft.access.core.DataCollection: " + transformedCollection.getName() + ".", t);
                }

                // update data collection properties
                // for future usage
                try
                {
                    if( collectionInfo.getProperties().containsKey( DataCollectionConfigConstants.CONFIG_FILE_PROPERTY ) )
                        collectionInfo.writeProperty( QuerySystem.QUERY_SYSTEM_CLASS, SequenceQuerySystem.class.getName() );
                }
                catch(Throwable t)
                {
                    cat.log(Level.SEVERE, "Can not add SequenceQuerySystem properties to config file. " +
                              "ru.biosoft.access.core.DataCollection: " + transformedCollection.getName() + ".", t);
                }
            }
            sequenceIndex = querySystem.getIndex(SequenceQuerySystem.SEQUENCE_INDEX);
        }
    }

    protected Properties createMapProperties(Sequence seq, Entry entry) throws Exception
    {
        Properties properties = new Properties();
        String entryName = getEntryName(entry);
        properties.put( DataCollectionConfigConstants.NAME_PROPERTY,   entryName);
        properties.put( DataCollectionConfigConstants.CLASS_PROPERTY,  ru.biosoft.bsa.MapAsVector.class.getName());
        properties.put( AnnotatedSequence.SITE_SEQUENCE_PROPERTY,     seq);
        ExProperties.addPlugin(properties, MapAsVector.class);
        return properties;
    }

    abstract protected int seekSequenceStartLine(BufferedReader seqReader) throws Exception;

    ////////////////////////////////////////////////////////////////////////////
    // Transformer interface implimentation
    //

    @Override
    public Class<? extends Entry> getInputType()
    {
        return Entry.class;
    }

    /** @return Map.class */
    @Override
    public Class<? extends AnnotatedSequence> getOutputType()
    {
        return AnnotatedSequence.class;
    }

    @Override
    public boolean isOutputType(Class<?> type)
    {
        return AnnotatedSequence.class.isAssignableFrom(type);
    }

    /**
     * Converts Entry to the Map
     *
     * @param input  Entry of EMBL  FileEntryCollection
     * @return Map data collection
     * @exception Exception If any error
     */
    @Override
    public AnnotatedSequence transformInput( Entry entry ) throws Exception
    {
        Alphabet alphabet = Nucleotide15LetterAlphabet.getInstance();

        // Lazily create sequence
        Index.IndexEntry indexEntry = null;
        Sequence seq = null;
        boolean isLongSequence = false;
        String entryName = getEntryName(entry);

        if( sequenceIndex!=null )
        {
            indexEntry = (Index.IndexEntry)sequenceIndex.get( entryName );
            if( indexEntry!=null )
            {
                if( indexEntry.len > MAX_MEMORY_SEQUENCE_LENGTH)
                                           isLongSequence = true;

                seq = SequenceFactory.createSequence(entry.getReader(), indexEntry.from, indexEntry.len,
                                                    alphabet, ErrorLetterPolicy.REPLACE_BY_ANY,
                                                    isLongSequence);
            }
        }

        if( seq == null )
        {
            BufferedReader seqReader = new BufferedReader(entry.getReader());
            int seqLineOffset = seekSequenceStartLine(seqReader);

            if( entry.getSize() > MAX_MEMORY_SEQUENCE_LENGTH)
                isLongSequence = true;

            seq = SequenceFactory.createSequence(seqReader, 0, -1, alphabet,
                                                 ErrorLetterPolicy.REPLACE_BY_ANY, isLongSequence);
            if( sequenceIndex != null)
            {
                indexEntry = new Index.IndexEntry(seqLineOffset, seq.getLength());
                sequenceIndex.put(entryName, indexEntry);
            }
        }

        // create site set
        Properties properties = createMapProperties(seq, entry);
        return (AnnotatedSequence)CollectionFactory.createCollection(getTransformedCollection(), properties);
    }
    
    private String getEntryName(Entry entry) 
    {
    	if("true".equals(this.primaryCollection.getInfo().getProperty(FastaSequenceCollection.DO_GET_SEQUENCEID_ONLY)))
    		return entry.getName().trim().split(" ")[0];
    	return entry.getName();
    }
}