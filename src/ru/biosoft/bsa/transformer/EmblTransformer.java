
package ru.biosoft.bsa.transformer;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.Entry;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Track;

/**
 * Converts {@link Entry} to the {@link AnnotatedSequence} and back.
 *
 * @see ru.biosoft.access.core.TransformedDataCollection
 * @todo Embl - full Description is needed
 */
public class EmblTransformer extends SequenceWithFeatureTableTransformer
{
    protected static final Logger log = Logger.getLogger(EmblTransformer.class.getName());

    public final static String SEQUENCE_START_TAG = "SQ";

    public EmblTransformer()
    {
        sequenceStartTag = SEQUENCE_START_TAG;
    }

    ////////////////////////////////////////////////////////////////////////////

    /**
     * Converts Map  to the Entry
     * @param output Map
     * @return Entry of EMBL FileEntryCollection
     */
    @Override
    public Entry transformOutput(AnnotatedSequence siteSet) throws IOException
    {
        StringBuffer strBuf = new StringBuffer("ID   " + siteSet.getName());
        strBuf.append("\nXX\n");

        // write feature table
        if( siteSet.getSize() > 0 )
        {
            try
            {
                Track track = siteSet.iterator().next();
                EmblTrackTransformer trackTransformer = new EmblTrackTransformer();
                Entry entry = trackTransformer.transformOutput(track);
                strBuf.append(entry.getData());
                strBuf.append("\nXX\n");
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not get track", e);
            }
        }

        //-------Save sequences ---------------------------
        Sequence sequence = siteSet.getSequence();
        int sequenceLength = sequence.getLength();

        if( sequenceLength <= MAX_LENGTH )
        {
            //SQ   Sequence 1145 BP; 193 A; 435 C; 291 G; 226 T; 0 other;
            strBuf.append(SEQUENCE_START_TAG);
            strBuf.append("   Sequence " + sequenceLength + " BP;");

            int[] counts = new int[sequence.getAlphabet().size()];
            for( int i = 1; i <= sequenceLength; i++ )
                counts[sequence.getLetterCodeAt(i)]++;

            int other = sequenceLength;
            for( byte i = 0; i < 4; i++ )
            {
                strBuf.append(" ");
                strBuf.append(counts[i]);
                strBuf.append(" ");
                strBuf.append(sequence.getAlphabet().codeToLetters(i).toUpperCase());
                strBuf.append(";");
                // Count 'other' letters
                other -= counts[i];
            }

            strBuf.append(" ");
            strBuf.append(other);
            strBuf.append(" other;");

            for( int i = 0; i < sequenceLength; i++ )
            {
                if( i % 60 == 0 )
                {
                    if( i != 0 )
                        strBuf.append(itoa(i, 10));
                    strBuf.append(lineSep).append("     ");
                }
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
