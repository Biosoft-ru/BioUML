package biouml.plugins.sbol;


import org.sbolstandard.core2.Identified;

import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.standard.type.Base;
import ru.biosoft.access.core.DataCollection;

public class SbolBase implements Base
{

    private Identified sbolObject = null;

    public SbolBase(Identified so)
    {
        sbolObject = so;
    }
    
    public void setSbolObject(Identified id)
    {
        this.sbolObject = id;
    }

    @Override
    public String getName()
    {
        return sbolObject.getDisplayId();
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
    public String getTitle()
    {
        String name;
        name = sbolObject.getName();

        if ( name != null )
            return name;
        return sbolObject.getDisplayId();
        // exception handling return sbolObject.getUri().toString();
    }

    @Override
    public DynamicPropertySet getAttributes()
    {
        // TODO Generate attributes by object
        return null;
    }

}
