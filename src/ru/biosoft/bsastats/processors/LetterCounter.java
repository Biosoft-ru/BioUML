package ru.biosoft.bsastats.processors;

import java.util.HashMap;
import java.util.Map;

import ru.biosoft.util.IntArray;

/**
 * Counts given letters in the sequences
 * @author lan
 */
public class LetterCounter
{
    private byte[] set = new byte[255];
    
    private LetterCounter(String letters)
    {
        for(int i=0; i<letters.length(); i++)
        {
            set[letters.codePointAt(i)]=1;
        }
    }
    
    public int count(byte[] sequence)
    {
        int result = 0;
        for(int i=sequence.length-1; i>=0; i--)
            result+=set[sequence[i]];
        return result;
    }
    
    public void countPerBase(byte[] sequence, IntArray counts)
    {
        counts.growTo(sequence.length);
        int[] data = counts.data();
        for(int i=sequence.length-1; i>=0; i--)
            data[i]+=set[sequence[i]];
    }
    
    private static Map<String, LetterCounter> counters = new HashMap<>();
    
    public synchronized static LetterCounter getLetterCounter(String letters)
    {
        if(!counters.containsKey(letters))
            counters.put(letters, new LetterCounter(letters));
        return counters.get(letters);
    }
}
