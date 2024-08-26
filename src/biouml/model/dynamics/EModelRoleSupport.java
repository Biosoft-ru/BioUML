package biouml.model.dynamics;

import biouml.model.DiagramElement;
import biouml.model.Role;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class EModelRoleSupport extends Option implements Role
{
    public static final String ODE_EMODEL_TYPE_UNKNOWN = "Unknown EModel";
    
    public EModelRoleSupport()
    {
    }

    public EModelRoleSupport(DiagramElement diagramElement)
    {
        super(diagramElement);
    }
    
    public String getType()
    {
        return ODE_EMODEL_TYPE_UNKNOWN;
    }

    /** Throws UnsupportedOperationException. Should be redefined in subclasses. */
    @Override
    public Role clone(DiagramElement de)
    {
        throw new UnsupportedOperationException("EModel cloning is not supported.");
    }

    @Override
    public DiagramElement getDiagramElement()
    {
        return getParent();
    }
    
    @Override
    public DiagramElement getParent()
    {
        return (DiagramElement)super.getParent();
    }

    protected String comment;
    @PropertyName("Comment")
    @PropertyDescription("Comment.")
    public String getComment()
    {
        return comment;
    }

    public void setComment(String comment)
    {
        String oldValue = comment;
        this.comment = comment;
        firePropertyChange("comment", oldValue, comment);
    }
}
