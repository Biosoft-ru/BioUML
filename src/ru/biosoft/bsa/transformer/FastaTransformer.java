package ru.biosoft.bsa.transformer;

import java.io.BufferedReader;
import ru.biosoft.access.Entry;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Sequence;

/**
 * Converts {@link Entry} to the {@link AnnotatedSequence} and back
 * This transformer is used for FASTA file format converting.
 *
 * @todo FASTA - full Description is needed
 */
public class FastaTransformer extends SequenceTransformer
{
    /**
     * In FASTA format the sequence starts from the second line.
     * So we read one line and return 1.
     *
     * @return 1
     */
    @Override
    protected int seekSequenceStartLine(BufferedReader seqReader) throws Exception
    {
        seqReader.readLine();
        return 1;
    }

    /**
     * Converts Map  to the Entry
     *
     * @param output Map
     * @return Entry of FASTA FileEntryCollection
     */
    @Override
    public Entry transformOutput( AnnotatedSequence map )
    {
        Sequence sequence = map.getSequence();

        StringBuffer strBuf = new StringBuffer( "> " );
        strBuf.append( map.getName() );
        strBuf.append( ';' );

        int sequenceLength = sequence.getLength();
        for ( int i = 0; i < sequenceLength; i++ )
        {
            if ( i % 60 == 0 )    strBuf.append( lineSep );
            else if ( i % 10 == 0 ) strBuf.append( ' ' );
            strBuf.append( (char)sequence.getLetterAt( i + 1 ) );
        }
        return new Entry( getPrimaryCollection(), map.getName(), strBuf.toString(), Entry.TEXT_FORMAT );
    }
}