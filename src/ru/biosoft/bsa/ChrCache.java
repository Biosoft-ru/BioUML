package ru.biosoft.bsa;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.RepositoryException;

public class ChrCache
{
    private DataElementPath seqBase = null;
    private Map<String, Sequence> sequenceCache = new HashMap<>();
    private Map<String, Sequence> stubSequenceCache = new HashMap<>();
    
    public ChrCache(DataElementPath seqBase)
    {
        this.seqBase = seqBase;
    }
    
    public ChrCache()
    {
    }
    
    public void setSeqBase(DataElementPath seqBase)
    {
        this.seqBase = seqBase;
    }
    
    public @Nonnull Sequence getSequence(String name)
    {
        Sequence result = sequenceCache.get(name);
        if( result != null )
            return result;

        result = stubSequenceCache.get(name);
        if( result != null )
            return result;

        if( seqBase != null )
        {
            try
            {
                result = seqBase.getChildPath(name).getDataElement(AnnotatedSequence.class).getSequence();
            }
            catch( RepositoryException e )
            {
                // no sequence available: ignore
            }
            if( result != null )
            {
                sequenceCache.put(name, result);
                return result;
            }
        }

        result = new StubSequence(name, Nucleotide15LetterAlphabet.getInstance());
        stubSequenceCache.put(name, result);

        return result;
    }

    public void clear()
    {
        sequenceCache.clear();
        stubSequenceCache.clear();
    }
}
