package ru.biosoft.bsa.transformer;

import java.io.BufferedReader;

import ru.biosoft.access.Entry;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Sequence;

public class FastqTransformer extends SequenceTransformer
{

    @Override
    protected int seekSequenceStartLine(BufferedReader seqReader)
            throws Exception
    {
        seqReader.readLine();
        return 1;
    }

    @Override
    public Entry transformOutput(AnnotatedSequence map) throws Exception
    {
        Sequence sequence = map.getSequence();
        StringBuffer strBuf = new StringBuffer( "@" );
        strBuf.append( map.getName() );
        strBuf.append( "\n" );
        int sequenceLength = sequence.getLength();
        for(int i = 1; i <= sequenceLength; i++)
            strBuf.append((char)sequence.getLetterAt(i));
        
        strBuf.append( "\n+" );
        strBuf.append( map.getName() );
        for(int i = 1; i <= sequenceLength; i++)
            strBuf.append('I');
        
        return new Entry( getPrimaryCollection(), map.getName(), strBuf.toString(), Entry.TEXT_FORMAT );
    }
    
    @Override
    public AnnotatedSequence transformInput(Entry entry) throws Exception
    {
        String data = entry.getData();
        data = data.substring(0, data.indexOf("+"));
        Entry sequenceEntry = new Entry(entry.getOrigin(), entry.getName(), data, entry.getFormat());
        return super.transformInput(sequenceEntry);
    }

}
