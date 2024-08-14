package biouml.standard.type;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;

abstract public class GenericEntity extends BaseSupport
{
    private String date;
    protected String comment;
    private static final long serialVersionUID = 8399362520570319378L;
    
    protected GenericEntity(DataCollection origin, String name)
    {
        super(origin, name);
    }
    
    protected GenericEntity(DataCollection origin, String name, String type)
    {
        super(origin, name, type);
    }
    
    /**
     * @pending use special structure for creation and edition dates.
     * @pending automaticlly update this field when the entry is edited.
     */
    @PropertyName("Date")
    @PropertyDescription("Date of the object creation.")
    public String getDate()
    {
        return date;
    }
    public void setDate(String date)
    {
        String oldValue = this.date;
        this.date = date;
        firePropertyChange("date", oldValue, date);
    }

    @PropertyName("Comment")
    @PropertyDescription("Arbitrary text comments.")
    public String getComment()
    {
        return comment;
    }
    public void setComment(String comment)
    {
        String oldValue = this.comment;
        this.comment = comment;
        firePropertyChange("comment", oldValue, comment);
    }
}