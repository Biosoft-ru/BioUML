package biouml.plugins.biopax.model;

import biouml.standard.type.Concept;
import ru.biosoft.access.core.DataCollection;

public class SequenceInterval extends Concept
{
    public SequenceInterval(DataCollection origin, String name)
    {
        super(origin, name);
    }

    @Override
    public String getType()
    {
        return TYPE_CONCEPT;
    }
    
    private SequenceSite begin;
    
    public SequenceSite getBegin()
    {
        return begin;
    }

    public void setBegin(SequenceSite begin)
    {
        this.begin = begin;
    }
    
    private SequenceSite end;
    
    public SequenceSite getEnd()
    {
        return end;
    }

    public void setEnd(SequenceSite end)
    {
        this.end = end;
    }
    

}
