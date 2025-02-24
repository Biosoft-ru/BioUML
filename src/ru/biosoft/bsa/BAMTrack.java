package ru.biosoft.bsa;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import one.util.streamex.StreamEx;

import net.sf.samtools.AbstractBAMFileIndex;
import net.sf.samtools.BAMIndexMetaData;
import net.sf.samtools.BAMIndexer;
import net.sf.samtools.Cigar;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecord.SAMTagAndValue;
import net.sf.samtools.SAMRecordIterator;
import net.sf.samtools.SAMSequenceRecord;
import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.CloneableDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.Repository;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.security.Permission;
import ru.biosoft.bsa.GenomeSelector.GenomeSelectorTrackUpdater;
import ru.biosoft.bsa.view.BamTrackViewBuilder;
import ru.biosoft.bsa.view.TrackViewBuilder;
import ru.biosoft.util.ListUtil;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import ru.biosoft.util.bean.StaticDescriptor;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;
import com.developmentontheedge.application.ApplicationUtils;
import ru.biosoft.jobcontrol.JobControl;

public class BAMTrack extends AbstractDataCollection<DataElement> implements Track, CloneableDataElement
{
    static
    {
        SAMFileReader.setDefaultValidationStringency(ValidationStringency.SILENT);
    }

    public static final String BAM_INDEX_FILE_PROPERTY = "bamIndex";

    public static final String CIGAR_PROPERTY = "Cigar";
    public static final String READ_SEQUENCE = "Read sequence";
    public static final String PHRED_QUAL = "Phred quality";
    public static final String MAPPING_QUAL_PROPERTY = "Mapping quality";
    public static final String READ_PAIRED_PROPERTY = "Paired";
    public static final String READ_PROPER_PAIR_PROPERTY = "Both reads in pair aligned";
    public static final String READ_UNMAPPED_PROPERTY = "Unmapped";
    public static final String MATE_UNMAPPED_PROPERTY = "Mate unmapped";
    public static final String READ_NEGATIVE_STRAND_PROPERTY = "Mapped to negative strand";
    public static final String MATE_NEGATIVE_STRAND_PROPERTY = "Mate mapped to negative strand";
    public static final String FIRST_OF_PAIR_PROPERTY = "First of pair";
    public static final String SECOND_OF_PAIR_PROPERTY = "Second of pair";
    public static final String PRIMARY_ALIGNMENT_PROPERTY = "Primary alignment";
    public static final String FAILS_VENDOR_QC_PROPERTY = "Fails vendor quality check";
    public static final String DUPLICATE_PROPERTY = "PCR or optical duplicate";
    public static final String MATE_CHROMOSOME_PROPERTY = "Mate sequence name";
    public static final String MATE_ALIGNMENT_START_PROPERTY = "Mate alignment start";
    public static final String INSERT_SIZE_PROPERTY = "Insert size";

    protected static final PropertyDescriptor READ_SEQUENCE_DESCRIPTOR = StaticDescriptor.create(READ_SEQUENCE);
    protected static final PropertyDescriptor PHRED_QUAL_DESCRIPTOR = StaticDescriptor.create(PHRED_QUAL);
    protected static final PropertyDescriptor CIGAR_DESCRIPTOR = StaticDescriptor.create(CIGAR_PROPERTY);
    protected static final PropertyDescriptor MAPPING_QUAL_DESCRIPTOR = StaticDescriptor.create(MAPPING_QUAL_PROPERTY);
    protected static final PropertyDescriptor READ_PAIRED_DESCRIPTOR = StaticDescriptor.create(READ_PAIRED_PROPERTY);
    protected static final PropertyDescriptor READ_PROPER_PAIR_DESCRIPTOR = StaticDescriptor.create(READ_PROPER_PAIR_PROPERTY);
    protected static final PropertyDescriptor READ_UNMAPPED_DESCRIPTOR = StaticDescriptor.create(READ_UNMAPPED_PROPERTY);
    protected static final PropertyDescriptor MATE_UNMAPPED_DESCRIPTOR = StaticDescriptor.create(MATE_UNMAPPED_PROPERTY);
    protected static final PropertyDescriptor READ_NEGATIVE_STRAND_DESCRIPTOR = StaticDescriptor.create(READ_NEGATIVE_STRAND_PROPERTY);
    protected static final PropertyDescriptor MATE_NEGATIVE_STRAND_DESCRIPTOR = StaticDescriptor.create(MATE_NEGATIVE_STRAND_PROPERTY);
    protected static final PropertyDescriptor FIRST_OF_PAIR_DESCRIPTOR = StaticDescriptor.create(FIRST_OF_PAIR_PROPERTY);
    protected static final PropertyDescriptor SECOND_OF_PAIR_DESCRIPTOR = StaticDescriptor.create(SECOND_OF_PAIR_PROPERTY);
    protected static final PropertyDescriptor PRIMARY_ALIGNMENT_DESCRIPOTR = StaticDescriptor.create(PRIMARY_ALIGNMENT_PROPERTY);
    protected static final PropertyDescriptor FAILS_VENDOR_QC_DESCRIPTOR = StaticDescriptor.create(FAILS_VENDOR_QC_PROPERTY);
    protected static final PropertyDescriptor DUPLICATE_DESCRIPTOR = StaticDescriptor.create(DUPLICATE_PROPERTY);
    protected static final PropertyDescriptor MATE_CHROMOSOME_DESCRIPTOR = StaticDescriptor.create(MATE_CHROMOSOME_PROPERTY);
    protected static final PropertyDescriptor MATE_ALIGNMENT_START_DESCRIPTOR = StaticDescriptor.create(MATE_ALIGNMENT_START_PROPERTY);
    protected static final PropertyDescriptor INSERT_SIZE_DESCRIPTOR = StaticDescriptor.create(INSERT_SIZE_PROPERTY);

    private final File bamFile;
    private final File indexFile;
    private final Set<BAMIterator> iterators = Collections.synchronizedSet( Collections.newSetFromMap(new WeakHashMap<BAMIterator, Boolean>()) );

    private int alignedCount = -1;
    private int unalignedCount = -1;

    private GenomeSelector genomeSelector;

    private Boolean bamSequencesWithChrPrefix = null;

    public BAMTrack(DataCollection parent, Properties properties)
    {
        super(parent, properties);
        String bamFileName = properties.getProperty(DataCollectionConfigConstants.FILE_PROPERTY);
        if( bamFileName == null )
            throw new IllegalArgumentException(DataCollectionConfigConstants.FILE_PROPERTY + " property not set for " + DataElementPath.create(this));
        String filePath = properties.getProperty(DataCollectionConfigConstants.FILE_PATH_PROPERTY);
        bamFile = new File(filePath, bamFileName);
        if(properties.containsKey( BAM_INDEX_FILE_PROPERTY ))
            indexFile = new File(filePath, properties.getProperty( BAM_INDEX_FILE_PROPERTY ));
        else
            indexFile = getIndexFile(bamFile);
        getInfo().addUsedFile(getBAMFile());
        getInfo().addUsedFile(getIndexFile());

        setGenomeSelector(new GenomeSelector(this));
    }

    public File getBAMFile()
    {
        return bamFile;
    }


    public static File getIndexFile(File bamFile)
    {
        String name = bamFile.getName();
        if( name.endsWith(".bam") )
            name = name.substring(0, name.length() - ".bam".length());
        name = name + ".bai";
        return new File(bamFile.getParentFile(), name);
    }

    public File getIndexFile()
    {
        return indexFile;
    }

    @Override
    public int countSites(String sequence, int from, int to) throws Exception
    {
        return getSites(sequence, from, to).getSize();
    }

    @Override
    public @Nonnull DataCollection<Site> getAllSites()
    {
        return new SitesCollection();
    }

    @Override
    public Site getSite(String sequence, String siteName, int from, int to) throws Exception
    {
        for( Site s : getSites(sequence, from, to) )
            if( s.getName().equals(siteName) )
                return s;
        return null;
    }

    @Override
    public DataCollection<Site> getSites(String sequence, int from, int to)
    {
        String chromosomeName = fromEnsembl(DataElementPath.create(sequence).getName());
        return new SitesCollection(chromosomeName, from, to);
    }

    private final TrackViewBuilder viewBuilder = new BamTrackViewBuilder();

    @Override
    public TrackViewBuilder getViewBuilder()
    {
        return viewBuilder;
    }

    public void createIndex(JobControl jc)
    {
        File indexFile = getIndexFile();
        if( indexFile.exists() )
            indexFile.delete();
        try (SAMFileReader reader = new SAMFileReader( bamFile ))
        {
            reader.enableFileSource(true);
            BAMIndexer indexer = new BAMIndexer(indexFile, reader.getFileHeader());
            SAMRecordIterator it = reader.iterator();
            while( it.hasNext() )
            {
                SAMRecord record = it.next();
                fixInvalidAlignment( record );
                indexer.processAlignment( record );
            }
            it.close();
            indexer.finish();
        }
        catch( RuntimeException e )
        {
            indexFile.delete();
            throw e;
        }
    }

    private static void fixInvalidAlignment(SAMRecord record)
    {
        if(record.getReferenceIndex() == -1 && record.getAlignmentStart() != 0)
            record.setAlignmentStart( 0 );
    }

    public class SitesCollection extends AbstractDataCollection<Site> implements LimitedSizeSitesCollection
    {
        private int size = -1;
        private int from, to;
        private String chromosomeName;

        public SitesCollection()
        {
            super(BAMTrack.this.getName(), BAMTrack.this.getOrigin(), null);
        }

        public SitesCollection(String chromosomeName, int from, int to)
        {
            super(BAMTrack.this.getName(), BAMTrack.this.getOrigin(), null);
            this.from = from;
            this.to = to;
            this.chromosomeName = chromosomeName;
        }

        protected SAMRecordIterator samRecordIterator(SAMFileReader reader)
        {
            return chromosomeName == null ? reader.iterator() : reader.queryOverlapping(chromosomeName, from, to);
        }

        @Override
        public synchronized int getSizeLimited(int limit)
        {
            if( size == -1 )
            {
                if(chromosomeName == null)
                    return getSize();
                try (SAMFileReader reader = new SAMFileReader( bamFile, getIndexFile() ))
                {
                    int limitedSize = 0;
                    SAMRecordIterator it = samRecordIterator( reader );
                    for( ; it.hasNext() && limitedSize < limit; it.next() )
                        limitedSize++;
                    it.close();
                    return limitedSize;
                }
            }
            return size;
        }

        public SitesCollection getSubCollection(int from, int to)
        {
            if( chromosomeName == null )
                throw new UnsupportedOperationException();
            return new SitesCollection(chromosomeName, from, to);
        }

        @Override
        public synchronized int getSize()
        {
            if( size == -1 )
            {
                if( chromosomeName == null )
                {
                    size = getAlignedCount() + getUnalignedCount();
                }
                else
                {
                    try (SAMFileReader reader = new SAMFileReader( bamFile, getIndexFile() ))
                    {
                        size = 0;
                        SAMRecordIterator it = samRecordIterator( reader );
                        for( ; it.hasNext(); it.next() )
                            size++;
                        it.close();
                    }
                }
            }
            return size;
        }

        @Override
        public @Nonnull List<String> getNameList()
        {
            List<String> list = new ArrayList<>();
            for(Site s: this)
            {
                list.add(s.getName());
            }
            return list;
        }

        @Override
        protected Site doGet(String name) throws Exception
        {
            for(Site s: this)
            {
                if(s.getName().equals(name))
                    return s;
            }
            return null;
        }

        @Override
        public @Nonnull Iterator<Site> iterator()
        {
            final SAMFileReader reader = new SAMFileReader(bamFile, getIndexFile());

            BAMIterator iterator = new BAMIterator(this, reader);
            iterators.add(iterator);
            return iterator;
        }

        @Override
        public StreamEx<Site> stream()
        {
            return StreamEx.of( spliterator() );
        }

        protected Site siteFromSAMRecord(SAMRecord record)
        {
            return new BAMSite(record);
        }

        public class BAMSite implements AlignmentSite
        {
            private final SAMRecord record;
            private DynamicPropertySet properties;
            private String name;

            public BAMSite(SAMRecord record)
            {
                this.record = record;
            }

            @Override
            public String getName()
            {
                if( name == null )
                {
                    synchronized( this )
                    {
                        name = record.getReadName().replace( '/', '_' ).replace( ';', '_' );
                        if( isPaired() )
                            name += isFirstOfPair() ? "_1" : "_2";
                    }
                }
                return name;
            }

            @Override
            public DataCollection<?> getOrigin()
            {
                return SitesCollection.this;
            }

            @Override
            public String getType()
            {
                return "Alignment";
            }

            @Override
            public int getBasis()
            {
                return BASIS_USER;
            }

            @Override
            public int getStart()
            {
                return record.getReadNegativeStrandFlag() ? record.getAlignmentEnd() : record.getAlignmentStart();
            }

            @Override
            public int getLength()
            {
                return record.getAlignmentEnd() - record.getAlignmentStart() + 1;
            }

            @Override
            public int getFrom()
            {
                return record.getAlignmentStart();
            }

            @Override
            public int getTo()
            {
                return record.getAlignmentEnd();
            }

            @Override
            public Interval getInterval()
            {
                return new Interval(getFrom(), getTo());
            }

            @Override
            public int getPrecision()
            {
                return PRECISION_EXACTLY;
            }

            @Override
            public int getStrand()
            {
                return record.getReadNegativeStrandFlag() ? StrandType.STRAND_MINUS : StrandType.STRAND_PLUS;
            }

            @Override
            public Sequence getSequence()
            {
                return new SequenceRegion(getOriginalSequence(), getStart(), getLength(), 1, record.getReadNegativeStrandFlag(), false);
            }

            @Override
            public Sequence getOriginalSequence()
            {
                return BAMTrack.this.getSequence(record.getReferenceName());
            }

            @Override
            public String getComment()
            {
                return null;
            }

            @Override
            public DynamicPropertySet getProperties()
            {
                if( properties == null )
                {
                    properties = new DynamicPropertySetSupport();
                    properties.add(new DynamicProperty(READ_SEQUENCE_DESCRIPTOR, String.class, record.getReadString()));
                    properties.add(new DynamicProperty(PHRED_QUAL_DESCRIPTOR, String.class, record.getBaseQualityString()));
                    properties.add(new DynamicProperty(CIGAR_DESCRIPTOR, String.class, record.getCigarString()));
                    properties.add(new DynamicProperty(MAPPING_QUAL_DESCRIPTOR, String.class, record.getMappingQuality()));

                    if( record.getMateReferenceName() != null )
                        properties.add(new DynamicProperty(MATE_CHROMOSOME_DESCRIPTOR, String.class, record.getMateReferenceName()));

                    if( record.getMateAlignmentStart() != 0 )
                        properties.add(new DynamicProperty(MATE_ALIGNMENT_START_DESCRIPTOR, Integer.class, record.getMateAlignmentStart()));

                    if( record.getInferredInsertSize() != 0 )
                        properties.add(new DynamicProperty(INSERT_SIZE_DESCRIPTOR, Integer.class, record.getInferredInsertSize()));

                    //Flags
                    properties.add(new DynamicProperty(READ_PAIRED_DESCRIPTOR, Boolean.class, record.getReadPairedFlag()));
                    if( isPaired() ) properties.add(new DynamicProperty(READ_PROPER_PAIR_DESCRIPTOR, Boolean.class, record.getProperPairFlag()));
                    properties.add(new DynamicProperty(READ_UNMAPPED_DESCRIPTOR, Boolean.class, record.getReadUnmappedFlag()));
                    if( isPaired() ) properties.add(new DynamicProperty(MATE_UNMAPPED_DESCRIPTOR, Boolean.class, record.getMateUnmappedFlag()));
                    properties.add(new DynamicProperty(READ_NEGATIVE_STRAND_DESCRIPTOR, Boolean.class, record.getReadNegativeStrandFlag()));
                    if( isPaired() )
                    {
                        properties.add(new DynamicProperty(MATE_NEGATIVE_STRAND_DESCRIPTOR, Boolean.class, record.getMateNegativeStrandFlag()));
                        properties.add(new DynamicProperty(FIRST_OF_PAIR_DESCRIPTOR, Boolean.class, record.getFirstOfPairFlag()));
                        properties.add(new DynamicProperty(SECOND_OF_PAIR_DESCRIPTOR, Boolean.class, record.getSecondOfPairFlag()));
                    }
                    properties.add(new DynamicProperty(PRIMARY_ALIGNMENT_DESCRIPOTR, Boolean.class, !record.getNotPrimaryAlignmentFlag()));
                    properties.add(new DynamicProperty(FAILS_VENDOR_QC_DESCRIPTOR, Boolean.class, record.getReadFailsVendorQualityCheckFlag()));
                    properties.add(new DynamicProperty(DUPLICATE_DESCRIPTOR, Boolean.class, record.getDuplicateReadFlag()));

                    for(SAMTagAndValue attrib : record.getAttributes())
                        properties.add(new DynamicProperty(attrib.tag, attrib.value.getClass(), attrib.value));

                }
                return properties;
            }

            @Override
            public Sequence getReadSequence()
            {
                return new LinearSequence(record.getReadBases(), Nucleotide5LetterAlphabet.getInstance());
            }

            @Override
            public byte[] getBaseQualities()
            {
                return record.getBaseQualities();
            }

            public Cigar getCigar()
            {
                return record.getCigar();
            }

            public boolean isFirstOfPair()
            {
                return record.getFirstOfPairFlag();
            }

            public boolean isPaired()
            {
                return record.getReadPairedFlag();
            }

            @Override
            public double getScore()
            {
                return record.getMappingQuality();
            }
        }
    }

    public static class SitesCollectionBeanInfo extends BeanInfoEx
    {
        public SitesCollectionBeanInfo()
        {
            super(SitesCollection.class, MessageBundle.class.getName());

            beanDescriptor.setDisplayName(getResourceString("CN_TRACK_INFO"));
            beanDescriptor.setShortDescription(getResourceString("CD_TRACK_INFO"));
        }
    }

    private static final class BAMIterator implements Iterator<Site>
    {
        private final SAMRecordIterator it;
        private final SAMFileReader reader;
        private final SitesCollection collection;
        boolean closed = false;

        private BAMIterator(SitesCollection collection, SAMFileReader reader)
        {
            this.collection = collection;
            this.it = collection.samRecordIterator(reader);
            this.reader = reader;
        }
        @Override
        public boolean hasNext()
        {
            return it.hasNext();
        }
        @Override
        public Site next()
        {
            Site result = collection.siteFromSAMRecord(it.next());
            if( !hasNext() )
                close();
            return result;
        }
        public void close()
        {
            if( closed )
                return;
            closed = true;
            it.close();
            reader.close();
        }
        @Override
        protected void finalize()
        {
            close();
        }
    }

    public String fromEnsembl(String chr)
    {
        if(bamSequencesWithChrPrefix == null)
        {
            synchronized( this )
            {
                bamSequencesWithChrPrefix = true;
                SAMFileReader reader = new SAMFileReader(bamFile);
                for(SAMSequenceRecord s : reader.getFileHeader().getSequenceDictionary().getSequences())
                    if(!s.getSequenceName().startsWith("chr"))
                        bamSequencesWithChrPrefix = false;
                reader.close();
            }
        }
        if(!bamSequencesWithChrPrefix)
            return chr;
        if( chr.equals("MT") )
            chr = "M";
        if( !chr.startsWith("chr") )
            chr = "chr" + chr;
        return chr;
    }

    protected String toEnsembl(String chr)
    {
        if( chr.startsWith("chr") )
            chr = chr.substring("chr".length());
        if( chr.equals("M") )
            chr = "MT";
        return chr;
    }

    private final java.util.Map<String, Sequence> sequenceCache = new HashMap<>();
    private final java.util.Map<String, Sequence> stubSequenceCache = new HashMap<>();

    private Sequence getSequence(String name)
    {
        name = toEnsembl(name);
        Sequence result = sequenceCache.get(name);
        if( result != null )
            return result;

        result = stubSequenceCache.get(name);
        if( result != null )
            return result;

        DataCollection<AnnotatedSequence> seqBase = genomeSelector.getSequenceCollection();
        if( seqBase != null )
        {
            try
            {
                result = seqBase.get(name).getSequence();
            }
            catch( Exception e )
            {
            }
            if( result != null )
            {
                sequenceCache.put(name, result);
                return result;
            }
        }

        result = new LinearSequence(name, new byte[0], Nucleotide15LetterAlphabet.getInstance());
        stubSequenceCache.put(name, result);

        return result;
    }

    @Override
    @Nonnull
    public List<String> getNameList()
    {
        return ListUtil.emptyList();
    }

    private void initCounts()
    {
        alignedCount = 0;
        unalignedCount = 0;

        try (SAMFileReader reader = new SAMFileReader( bamFile, getIndexFile() ))
        {
            AbstractBAMFileIndex index = (AbstractBAMFileIndex)reader.getIndex();
            int nRef = index.getNumberOfReferences();
            for( int i = 0; i < nRef; i++ )
            {
                BAMIndexMetaData metaData = index.getMetaData(i);
                unalignedCount += metaData.getUnalignedRecordCount();
                alignedCount += metaData.getAlignedRecordCount();
            }
            unalignedCount += index.getNoCoordinateCount();
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not init site counts of BAMTrack", e);
        }

    }

    public int getAlignedCount()
    {
        if( alignedCount == -1 )
            initCounts();
        return alignedCount;
    }

    public int getUnalignedCount()
    {
        if( unalignedCount == -1 )
            initCounts();
        return unalignedCount;
    }

    @PropertyName ( "Genome" )
    @PropertyDescription ( "Genome (sequences collection)" )
    public GenomeSelector getGenomeSelector()
    {
        return genomeSelector;
    }

    public void setGenomeSelector(GenomeSelector genomeSelector)
    {
        if( genomeSelector != this.genomeSelector )
        {
            GenomeSelectorTrackUpdater listener = new GenomeSelectorTrackUpdater( BAMTrack.this, genomeSelector, () -> {
                sequenceCache.clear();
                stubSequenceCache.clear();
                return;
            } );
            genomeSelector.addPropertyChangeListener( listener );
        }
        this.genomeSelector = genomeSelector;
    }

    @Override
    public BAMTrack clone(DataCollection origin, String name) throws CloneNotSupportedException
    {
        try
        {
            origin.remove(name);
            Properties trackProperties = new Properties();
            trackProperties.put(DataCollectionConfigConstants.NAME_PROPERTY, name);
            trackProperties.put(DataCollectionConfigConstants.CLASS_PROPERTY, BAMTrack.class.getName());
            trackProperties.put(DataCollectionConfigConstants.FILE_PROPERTY, name);
            if(getInfo().getProperty(Track.SEQUENCES_COLLECTION_PROPERTY) != null)
                trackProperties.put(Track.SEQUENCES_COLLECTION_PROPERTY, getInfo().getProperty(Track.SEQUENCES_COLLECTION_PROPERTY));
            Repository parentRepository = (Repository)DataCollectionUtils.getTypeSpecificCollection(origin, BAMTrack.class);
            BAMTrack track = (BAMTrack)DataCollectionUtils.fetchPrimaryCollection(parentRepository.createDataCollection(name, trackProperties, null, null, null), Permission.WRITE);

            File indexFile = getIndexFile(bamFile);
            ApplicationUtils.linkOrCopyFile(track.getBAMFile(), bamFile, null);
            if(indexFile.exists())
                ApplicationUtils.linkOrCopyFile(track.getIndexFile(), indexFile, null);
            DataCollectionUtils.copyAnalysisParametersInfo( this, track );
            origin.put(track);
            return track;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "While copying "+getCompletePath(), e);
            try
            {
                origin.remove(name);
            }
            catch( Exception e1 )
            {
            }
            throw ExceptionRegistry.translateException(e);
        }
    }

    @Override
    protected DataElement doGet(String name) throws Exception
    {
        return null;
    }

    @Override
    public void close() throws Exception
    {
        for(BAMIterator iterator : iterators)
        {
            if(iterator != null)
                iterator.close();
        }
    }
}
