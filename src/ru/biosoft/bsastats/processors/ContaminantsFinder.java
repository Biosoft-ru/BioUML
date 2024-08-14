package ru.biosoft.bsastats.processors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.ObjectUtils;

import ru.biosoft.bsa.Nucleotide5LetterAlphabet;

/**
 * @author lan
 *
 */
public class ContaminantsFinder
{
    private static boolean init = false;
    private static final byte[] LETTER_COMPLEMENT = Nucleotide5LetterAlphabet.getInstance().letterComplementMatrix();
    private static Map<String, String> contaminants = new HashMap<>();
    private static Map<String, String> reverseContaminants = new HashMap<>();
    
    protected static void init()
    {
        if(!init)
        {
            synchronized(ContaminantsFinder.class)
            {
                if(!init)
                {
                    readContaminants();
                    init = true;
                }
            }
        }
    }

    protected static void readContaminants()
    {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader( ContaminantsFinder.class.getResourceAsStream( "resources/contaminant_list.txt" ) ) ))
        {
            while(br.ready())
            {
                String line = br.readLine().trim();
                if(line.startsWith("#") || line.isEmpty()) continue;
                String[] fields = line.split("\\t+");
                contaminants.put(fields[0], fields[1]);
                byte[] letters = fields[1].getBytes();
                byte[] reverseLetters = new byte[letters.length];
                for(int i=0; i<letters.length; i++) reverseLetters[letters.length-i-1] = LETTER_COMPLEMENT[letters[i]];
                reverseContaminants.put(fields[0], new String(reverseLetters).toUpperCase());
            }
        }
        catch( IOException e )
        {
        }
    }
    
    public static class ContaminantInfo implements Comparable<ContaminantInfo>
    {
        public static final ContaminantInfo NULL = new ContaminantInfo(null, 0, 0);
        private final String contaminantName;
        private final int percentage, length;

        public ContaminantInfo(String contaminantName, int percentage, int length)
        {
            this.contaminantName = contaminantName;
            this.percentage = percentage;
            this.length = length;
        }

        public String getContaminantName()
        {
            return contaminantName;
        }

        public int getPercentage()
        {
            return percentage;
        }

        public int getLength()
        {
            return length;
        }

        @Override
        public int compareTo(ContaminantInfo o)
        {
            if(length > o.length) return 1;
            if(length < o.length) return -1;
            if(percentage > o.percentage) return 1;
            if(percentage < o.percentage) return -1;
            return 0;
        }
        
        @Override
        public String toString()
        {
            if(this == NULL) return "No hit";
            return contaminantName+" ("+percentage+"%/"+length+"bp)";
        }
    }
    
    public static ContaminantInfo search(String sequence)
    {
        init();
        ContaminantInfo bestResult = ContaminantInfo.NULL;
        for( Map.Entry<String, String> entry : contaminants.entrySet() )
        {
            String contaminantName = entry.getKey();
            String contaminant = entry.getValue();
            String reverseContaminant = reverseContaminants.get(contaminantName);
            if(sequence.length() >= 8 && sequence.length() < 20)
            {
                if( contaminant.contains(sequence) || reverseContaminant.contains(sequence) )
                    return new ContaminantInfo(contaminantName, 100, sequence.length());
            }
            
            for(int offset = 20-contaminant.length(); offset < sequence.length()-20; offset++)
            {
                bestResult = Collections.max(Arrays.asList(
                        bestResult,
                        subSearch(contaminantName, contaminant, sequence, offset),
                        subSearch(contaminantName, reverseContaminant, sequence, offset)));
            }
        }
        return bestResult;
    }

    private static ContaminantInfo subSearch(String contaminantName, String contaminant, String sequence, int offset)
    {
        ContaminantInfo bestResult = ContaminantInfo.NULL;

        boolean mismatch = false;
        int length = 0;

        for( int i = 0; i < contaminant.length(); i++ )
        {
            if( i + offset >= sequence.length() )
                break;
            if( i + offset < 0 )
                continue;

            if( contaminant.charAt(i) == sequence.charAt(i + offset) )
            {
                length++;
            }
            else
            {
                if( mismatch )
                {
                    if( length > 20 )
                    {
                        bestResult = (ContaminantInfo)ObjectUtils.max(bestResult,
                                new ContaminantInfo(contaminantName, 100 - 100 / length, length));
                    }
                    length = 0;
                }
                mismatch = !mismatch;
            }
        }
        if( length > 20 )
        {
            bestResult = (ContaminantInfo)ObjectUtils.max(bestResult,
                    new ContaminantInfo(contaminantName, mismatch ? 100 - 100 / length : 100, length));
        }

        return bestResult;
    }
}
