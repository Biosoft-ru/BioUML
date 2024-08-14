package biouml.plugins.riboseq.db.model;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class Condition extends DataElementSupport
{
    public Condition(DataCollection<Condition> origin, String name)
    {
        super( name, origin );
    }

    private String description;

    @PropertyName("Description")
    @PropertyDescription("Condition description")
    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }
    
    @Override
    public String toString()
    {
        return description;
    }
}
