package biouml.plugins.sbol;


import org.sbolstandard.core2.Identified;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.standard.type.Base;
import ru.biosoft.access.core.DataCollection;

public class SbolBase extends Option implements Base 
{
    private boolean isCreated = true;
    private Identified sbolObject = null;
    private String title;
    private String name;

    public SbolBase(String name)
    {
        this(name, true);
    }
    
    public SbolBase(String name, boolean isCreated)
    {
       this.name = name;
       this.title = name;
       this.isCreated = isCreated;
    }
    
    public SbolBase(Identified so)
    {
        sbolObject = so;
        setName(so.getDisplayId());
        setTitle(so.getDisplayId());
    }
    
    public void setSbolObject(Identified id)
    {
        this.sbolObject = id;
    }

    @Override
    @PropertyName("Name")
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        Object oldValue = this.name;
        this.name = name;
        firePropertyChange( "name", oldValue, name );
    }

    @Override
    public DataCollection<?> getOrigin()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getType()
    {
        return SbolUtil.getType(sbolObject);
    }

    @Override
    @PropertyName("Title")
    public String getTitle()
    {
        return title;
    }
    
    public void setTitle(String title)
    {
        Object oldValue = this.title;
        this.title = title;
        firePropertyChange( "title", oldValue, title );
    }

    @Override
    public DynamicPropertySet getAttributes()
    {
        // TODO Generate attributes by object
        return null;
    }

    public Identified getSbolObject()
    {
        return sbolObject;
    }

    public boolean isCreated()
    {
        return isCreated;
    }
    
    public void setCreated(boolean created)
    {
        this.isCreated = created;
    }
}