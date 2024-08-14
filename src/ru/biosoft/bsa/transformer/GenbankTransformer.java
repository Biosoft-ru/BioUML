
package ru.biosoft.bsa.transformer;

import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.Entry;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Track;

/**
 * Converts {@link Entry} to the {@link AnnotatedSequence} and back
 * This transformer is used for GENBANK file format converting.
 * @see ru.biosoft.access.core.TransformedDataCollection
 * @todo GENBANK - full Description is needed
 */
public class GenbankTransformer extends SequenceWithFeatureTableTransformer
{
    protected static final Logger log = Logger.getLogger(GenbankTransformer.class.getName());
    
    public final static String SEQUENCE_START_TAG = "ORIGIN";
    private final static String LOCUS_TAG = "LOCUS       ";
    private final static String BASE_TAG = "BASE COUNT  ";

    public GenbankTransformer()
    {
        sequenceStartTag = SEQUENCE_START_TAG;
    }

    /**
     * Converts {@link ru.biosoft.bsa.AnnotatedSequence} to {@link ru.biosoft.access.Entry}.
     * @todo Write all sites into feature table.
     * @param output
     * @return  Entry of GENBANK FileEntryCollection
     * @exception IOException
     */
    @Override
    public Entry transformOutput(AnnotatedSequence siteSet) throws IOException
    {
        Sequence sequence = siteSet.getSequence();

        // write name
        StringBuffer strBuf = new StringBuffer(LOCUS_TAG);
        strBuf.append(siteSet.getName() + "\n");

        // write feature table
        if( siteSet.getSize() > 0 )
        {
            try
            {
                Track track = siteSet.iterator().next();
                EmblTrackTransformer trackTransformer = new EmblTrackTransformer();
                trackTransformer.setFormat(EmblTrackTransformer.GENBANK_FORMAT);
                Entry entry = trackTransformer.transformOutput(track);
                strBuf.append(entry.getData());
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not get track", e);
            }
        }

        // write sequence
        int sequenceLength = sequence.getLength();
        if( sequenceLength <= MAX_LENGTH )
        {
            strBuf.append(BASE_TAG);

            int[] counts = new int[sequence.getAlphabet().size()];
            for( int i = 1; i <= sequenceLength; i++ )
                counts[sequence.getLetterCodeAt(i)]++;

            for( byte i = 0; i < counts.length; i++ )
            {
                if( counts[i] == 0 )
                    continue;
                strBuf.append("  ");
                strBuf.append(counts[i]);
                strBuf.append(" ");
                strBuf.append(sequence.getAlphabet().codeToLetters(i));
            }

            strBuf.append("\n" + SEQUENCE_START_TAG);

            for( int i = 0; i < sequenceLength; i++ )
            {
                if( i % 60 == 0 )
                    strBuf.append(lineSep + itoa(i + 1, 9) + " ");
                else if( i % 10 == 0 )
                    strBuf.append(" ");
                strBuf.append((char)sequence.getLetterAt(i + 1));
            }
            strBuf.append("\n");
        }
        strBuf.append("//\n");

        return new Entry(getPrimaryCollection(), siteSet.getName(), "" + strBuf, Entry.TEXT_FORMAT);
    }
}
