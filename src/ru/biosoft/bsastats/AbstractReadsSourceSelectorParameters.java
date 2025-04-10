package ru.biosoft.bsastats;

import java.util.Iterator;
import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.bsa.AlignmentSite;
import ru.biosoft.bsa.Nucleotide5LetterAlphabet;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
public class AbstractReadsSourceSelectorParameters extends AbstractAnalysisParameters
{
    public static final String SOURCE_TRACK = "Track";
    public static final String SOURCE_FASTQ = "FastQ";
    public static final String SOURCE_SOLID = "Solid";
    public static final String SOURCE_CSFASTQ = "CSFastQ";

    private static final byte[] COMPLEMENT_MATRIX = Nucleotide5LetterAlphabet.getInstance().letterComplementMatrix();
    static final String[] ALIGNMENTS = new String[] {"left", "right"};
    static final String[] SOURCES = new String[] {SOURCE_TRACK, SOURCE_FASTQ, SOURCE_SOLID, SOURCE_CSFASTQ};
    protected boolean rightAlignment;
    private String source = SOURCES[0];
    private DataElementPath track;
    private DataElementPath fastq;
    private DataElementPath csfasta, qual;
    private String encoding = EncodingSelector.ENCODING_TO_OFFSET.keySet().iterator().next();
    protected boolean decodeColorSpace = true;

    public ProgressIterator<Task> getTasksIterator()
    {
        try
        {
            if( source.equals(SOURCE_FASTQ) )
            {
                return new FastqReadingIterator(fastq.getDataElement(FileDataElement.class).getFile(), getEncoding(), rightAlignment);
            }
            else if( source.equals(SOURCE_SOLID) )
            {
                return new SolidReadingIterator(csfasta.getDataElement(FileDataElement.class).getFile(),
                        qual.getDataElement(FileDataElement.class).getFile(), rightAlignment, decodeColorSpace);

            }
            else if( source.equals(SOURCE_CSFASTQ) )
            {
                return new CSFastqReadingIterator(fastq.getDataElement(FileDataElement.class).getFile(), getEncoding(), rightAlignment,
                        decodeColorSpace);
            }
            else
            {
                DataCollection<Site> allSites = getTrack().getDataElement(Track.class).getAllSites();
                final Iterator<Site> iterator = allSites.iterator();
                final int count = allSites.getSize();
                return new ProgressIterator<Task>()
                {
                    private int n = 0;

                    @Override
                    public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public Task next()
                    {
                        Task task = getTask(iterator.next());
                        n++;
                        return task;
                    }

                    @Override
                    public boolean hasNext()
                    {
                        return iterator.hasNext();
                    }

                    @Override
                    public float getProgress()
                    {
                        return ( (float)n ) / count;
                    }
                };
            }
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException(e);
        }
    }

    @PropertyName("FASTQ file")
    @PropertyDescription("FASTQ file with reads to analyze")
    public DataElementPath getFastq()
    {
        return fastq;
    }

    public void setFastq(DataElementPath fastq)
    {
        Object oldValue = this.fastq;
        this.fastq = fastq;
        firePropertyChange("fastq", oldValue, fastq);
        try
        {
            setEncoding(EncodingSelector.detectEncoding(fastq));
        }
        catch(Exception e) {}
    }

    @PropertyName("Quality encoding")
    @PropertyDescription("This specifies how phred quality values are encoded in the FASTQ file. In most of the cases system detects this value automatically. You may change it manually if auto-detection worked incorrectly.")
    public String getEncoding()
    {
        return encoding;
    }

    public void setEncoding(String encoding)
    {
        Object oldValue = this.encoding;
        this.encoding = encoding;
        firePropertyChange("encoding", oldValue, encoding);
    }

    public boolean isFastqHidden()
    {
        return !source.equals(SOURCE_FASTQ) && !source.equals(SOURCE_CSFASTQ);
    }

    @PropertyName("Input track")
    @PropertyDescription("Track to process")
    public DataElementPath getTrack()
    {
        return track;
    }

    public void setTrack(DataElementPath track)
    {
        Object oldValue = this.track;
        this.track = track;
        firePropertyChange("track", oldValue, track);
    }

    public boolean isTrackHidden()
    {
        return !source.equals(SOURCE_TRACK);
    }

    @PropertyName("CSFasta file")
    @PropertyDescription("File containing reads in color space")
    public DataElementPath getCsfasta()
    {
        return csfasta;
    }

    public void setCsfasta(DataElementPath csfasta)
    {
        Object oldValue = this.csfasta;
        this.csfasta = csfasta;
        firePropertyChange("csfasta", oldValue, csfasta);
    }

    @PropertyName("Qual file")
    @PropertyDescription("File containing corresponding quality values")
    public DataElementPath getQual()
    {
        return qual;
    }

    public void setQual(DataElementPath qual)
    {
        Object oldValue = this.qual;
        this.qual = qual;
        firePropertyChange("qual", oldValue, qual);
    }

    public boolean isSolidHidden()
    {
        return !source.equals(SOURCE_SOLID);
    }

    @PropertyName("Alignment")
    @PropertyDescription("Whether to align sites on left or right")
    public String getAlignment()
    {
        return rightAlignment?"right":"left";
    }

    public void setAlignment(String alignment)
    {
        Object oldValue = getAlignment();
        this.rightAlignment = alignment.equals("right");
        firePropertyChange("alignment", oldValue, alignment);
    }

    @PropertyName("Source")
    @PropertyDescription("Whether to get input data from track or from FASTQ")
    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        Object oldValue = getSource();
        this.source = source;
        firePropertyChange("alignment", oldValue, source);
    }

    // For auto-output
    public DataElementPath getSourcePath()
    {
        return (source == null || source.equals(SOURCE_FASTQ) || source.equals(SOURCE_CSFASTQ))?fastq:source.equals(SOURCE_TRACK)?track:csfasta;
    }

    protected Task getTask(Site site)
    {
        byte[] qualities;
        Sequence sequence;
        if(site instanceof AlignmentSite)
        {
            sequence = ((AlignmentSite)site).getReadSequence();
            qualities = ((AlignmentSite)site).getBaseQualities();
            if(qualities.length == 0) qualities = null;
        } else
        {
            sequence = site.getSequence();
            qualities = null;
        }
        int length = sequence.getLength();
        boolean reverse = site.getStrand() == StrandType.STRAND_MINUS ^ rightAlignment;
        byte[] seqBytes = new byte[length];
        int start = sequence.getStart();
        if(reverse)
        {
            if(qualities != null) ArrayUtils.reverse(qualities);
            if(site.getStrand() == StrandType.STRAND_MINUS)
            {
                for(int i=0; i<length; i++) seqBytes[length-i-1] = COMPLEMENT_MATRIX[sequence.getLetterAt(i+start)];
            } else
            {
                for(int i=0; i<length; i++) seqBytes[length-i-1] = sequence.getLetterAt(i+start);
            }
        } else
        {
            if(site.getStrand() == StrandType.STRAND_MINUS)
            {
                for(int i=0; i<length; i++) seqBytes[i] = COMPLEMENT_MATRIX[sequence.getLetterAt(i+start)];
            } else
            {
                for(int i=0; i<length; i++) seqBytes[i] = sequence.getLetterAt(i+start);
            }
        }
        return new Task(seqBytes, qualities, site);
    }

    public static String[] getSources()
    {
        return SOURCES.clone();
    }
}
