package ru.biosoft.bsa;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Logger;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;

public class SequenceFactory
{
    protected static final Logger cat = Logger.getLogger(SequenceFactory.class.getName());

    public final static int FAST_BUFFER = 0;
    public final static int SOFT_BUFFER = 1;

    private static int errorPolicy = ErrorLetterPolicy.REPLACE_BY_ANY;
    public static int getDefaultErrorLetterPolicy()
    {
        return errorPolicy;
    }
    public static void setDefaultErrorLetterPolicy(int errorPolicy)
    {
        SequenceFactory.errorPolicy = errorPolicy;
    }

    /**
     * Creates sequence
     *
     * @param reader
     * @param start
     * @param length
     * @param alphabet
     * @param errorPolicy
     *
     */
    public static Sequence createSequence(Reader reader, long start, long length, Alphabet alphabet, int errorPolicy, boolean longSequence)
            throws Exception
    {
        if( longSequence )
            return new LongSequence(reader, (int)start, alphabet, (int)length);

        Sequence seq;
        try (BufferedReader bufferedReader = reader instanceof BufferedReader ? (BufferedReader)reader : new BufferedReader( reader ))
        {
            while( start-- > 0 )
                bufferedReader.readLine();

            StringWriter writer = new StringWriter();
            SequenceFactory.parseSequence(writer, bufferedReader, alphabet, errorPolicy);

            seq = new LinearSequence(writer.toString(), alphabet);
        }

        return seq;
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    //

    /**
     * Reads sequence data from reader, validate it and store it writer.
     *
     * @param reader reader positioned to the sequence beginning.
     * @param alphabet the sequence alphabet
     * @errorPolicy indicates what should be done if error letter is occur.
     */
    public static void parseSequence(Writer writer, Reader reader, Alphabet alphabet, int errorPolicy) throws Exception
    {
        byte[] code = alphabet.letterToCodeMatrix();
        int current = 0;

        BufferedReader bufferedReader;
        if( reader instanceof BufferedReader )
            bufferedReader = (BufferedReader)reader;
        else
            bufferedReader = new BufferedReader(reader);

        String line;
        int lineNum = 0;
        int length;
        while( ( line = bufferedReader.readLine() ) != null )
        {
            lineNum++;
            length = line.length();
            for( int i = 0; i < length; i++ )
            {
                byte letter = (byte)line.charAt(i);
                switch( code[letter] )
                {
                    case Alphabet.IGNORED_CHAR:
                        continue;
                    case Alphabet.ERROR_CHAR:
                        if( errorPolicy == ErrorLetterPolicy.SKIP )
                            continue;
                        if( errorPolicy == ErrorLetterPolicy.EXCEPTION )
                        {
                            throw new ErrorLetterException((char)letter, current, lineNum, i);
                        }
                        //otherwise replace by 'any'
                        cat.warning(new ErrorLetterException((char)letter, current, lineNum, i).getMessage());
                        letter = alphabet.letterForAny();

                    default:
                        current++;
                        writer.write(letter);
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////

    public static boolean compareSequences(Sequence seq1, Sequence seq2)
    {
        if( seq1 == seq2 )
            return true;
        if( seq1 == null || seq2 == null )
            return false;
        if( seq1.getLength() != seq2.getLength() )
            return false;
        /** @todo - how to compare alphabets? */
        //if( seq1.getAlphabet() != seq2.getAlphabet() )
        //    return false;
        int len = seq1.getLength();
        for( int i = 1; i <= len; i++ )
        {
            if( seq1.getLetterAt(i) != seq2.getLetterAt(i) )
                return false;
        }
        return true;
    }

    /**
     * Returns sequence by corresponding Map name
     * @param sequence complete path to ru.biosoft.bsa.AnnotatedSequence object
     */
    public static Sequence getSequence(String sequence)
    {
        DataElement mapDE = CollectionFactory.getDataElement(sequence);
        if( mapDE == null || ! ( mapDE instanceof AnnotatedSequence ) )
            return null;
        return ( (AnnotatedSequence)mapDE ).getSequence();
    }
}
