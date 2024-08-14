package biouml.plugins.riboseq.db.model;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class CellSource extends DataElementSupport
{
    public CellSource(DataCollection<CellSource> origin, String name)
    {
        super( name, origin );
    }

    private String title;
    @PropertyName("Title")
    @PropertyDescription("Cell source title")
    public String getTitle()
    {
        return title;
    }
    public void setTitle(String title)
    {
        this.title = title;
    }
    
    @Override
    public String toString()
    {
        return title;
    }
}
