package ru.biosoft.bsastats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang.ArrayUtils;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.bsa.Nucleotide5LetterAlphabet;
import ru.biosoft.util.ReadAheadIterator;
import ru.biosoft.util.TextUtil;

/**
 * @author lan
 *
 */
public class SolidReadingIterator extends ReadAheadIterator<Task> implements ProgressIterator<Task>
{
    private static final byte[] LETTER_TO_CODE = Nucleotide5LetterAlphabet.getInstance().letterToCodeMatrix();
    private static final byte[] COLOR_SPACE_TO_CODE = new byte[] {
        4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4, 4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,
        4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4, 0,1,2,3,4,4,4,4,4,4,4,4,4,4,4,4,
        4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4, 4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,
        4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4, 4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,
        4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4, 4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,
        4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4, 4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,
        4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4, 4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,
        4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4, 4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4
    };
    private static final byte[][] COLOR_SPACE_NEXT = new byte[][] {
        new byte[] {'A','C','G','T','N'},
        new byte[] {'C','A','T','G','N'},
        new byte[] {'G','T','A','C','N'},
        new byte[] {'T','G','C','A','N'},
        new byte[] {'N','N','N','N','N'}
    };
    private final FileChannel ch;
    private final BufferedReader inputCSFasta;
    private final BufferedReader inputQual;
    private final boolean rightAlignment;
    private final boolean decode;

    public SolidReadingIterator(File csfasta, File qual, boolean rightAlignment) throws IOException
    {
        this( csfasta, qual, rightAlignment, true );
    }
    
    public SolidReadingIterator(File csfasta, File qual, boolean rightAlignment, boolean decode) throws IOException
    {
        FileInputStream is = new FileInputStream(csfasta);
        inputCSFasta = new BufferedReader(new InputStreamReader(is, StandardCharsets.ISO_8859_1));
        inputQual = ApplicationUtils.asciiReader( qual );
        ch = is.getChannel();
        this.rightAlignment = rightAlignment;
        this.decode = decode;
    }

    @Override
    protected Task advance()
    {
        try
        {
            String l1;
            do { l1 = inputCSFasta.readLine(); } while(l1 != null && l1.startsWith("#"));
            String l2;
            do { l2 = inputCSFasta.readLine(); } while(l2 != null && l2.startsWith("#"));
            String l3;
            do { l3 = inputQual.readLine(); } while(l3 != null && l3.startsWith("#"));
            String l4;
            do { l4 = inputQual.readLine(); } while(l4 != null && l4.startsWith("#"));
            if( l1 == null || l2 == null || l3 == null || l4 == null || !l1.startsWith(">") || !l3.startsWith(">") )
                throw new IllegalArgumentException("Supplied FASTQ file has an invalid format");
            byte[] sequence = l2.getBytes();
            int seqLength = sequence.length - 1;
            
            if( decode )
                sequence = decode(sequence);
            
            byte[] qualities = new byte[seqLength];
            String[] qualValues = TextUtil.split( l4, ' ' );
            for(int i=0; i<seqLength; i++)
            {
                qualities[i] = Byte.parseByte(qualValues[i]);
                if(qualities[i] < 0) qualities[i] = 0;
            }
            if(rightAlignment)
            {
                ArrayUtils.reverse(sequence);
                ArrayUtils.reverse(qualities);
            }
            return new Task(sequence, qualities, new String[] {l1+"\n"+l2, l3+"\n"+l4});
        }
        catch( Exception e )
        {
            try
            {
                close();
            }
            catch( IOException e1 )
            {
            }
        }
        return null;
    }
    
    public void close() throws IOException
    {
        if(inputCSFasta != null) inputCSFasta.close();
        if(inputQual != null) inputQual.close();
    }

   

    @Override
    public float getProgress()
    {
        if(!hasNext()) return 1;
        try
        {
            return ((float)ch.position())/ch.size();
        }
        catch( IOException e )
        {
            return 0;
        }
    }

    /**
     * Decode color space sequence with known first nucleotide to nucleotide space
     * @param sequence in csfasta format T021230...
     * @return nucleotide sequence, the first letter is not included (typically from adapter)
     */
    public static byte[] decode(byte[] sequence)
    {
        byte[] ntSequence = new byte[sequence.length - 1];
        ntSequence[0] = COLOR_SPACE_NEXT[LETTER_TO_CODE[sequence[0]]][COLOR_SPACE_TO_CODE[sequence[1]]];
        for( int i = 2; i < sequence.length; i++ )
        {
            ntSequence[i - 1] = COLOR_SPACE_NEXT[LETTER_TO_CODE[ntSequence[i - 2]]][COLOR_SPACE_TO_CODE[sequence[i]]];
        }
        return ntSequence;
    }
}
