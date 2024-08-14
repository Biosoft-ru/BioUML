package biouml.plugins.riboseq.db.model;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class SequenceAdapter extends DataElementSupport
{
    public SequenceAdapter(DataCollection<SequenceAdapter> origin, String name)
    {
        super( name, origin );
    }

    private String title;
    @PropertyName("Title")
    @PropertyDescription("Title of adapter")
    public String getTitle()
    {
        return title;
    }
    public void setTitle(String title)
    {
        this.title = title;
    }

    private String sequence;
    @PropertyName("Sequence")
    @PropertyDescription("Adapter sequence")
    public String getSequence()
    {
        return sequence;
    }
    public void setSequence(String sequence)
    {
        this.sequence = sequence;
    }
    
    @Override
    public String toString()
    {
        return title;
    }
    
}
