package biouml.model;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.exception.InternalException;

public class CollectionDescription implements Cloneable
{
    protected String moduleName;
    protected String sectionName;
    protected String typeName;
    protected boolean readOnly;
    protected DataCollection<?> dc;

    @Override
    public CollectionDescription clone()
    {
        try
        {
            CollectionDescription result = (CollectionDescription)super.clone();
            result.dc = null;
            return result;
        }
        catch( CloneNotSupportedException e )
        {
            throw new InternalException(e);
        }
    }

    public String getModuleName()
    {
        return moduleName;
    }
    public void setModuleName(String moduleName)
    {
        this.moduleName = moduleName;
    }
    public boolean isReadOnly()
    {
        return readOnly;
    }
    public void setReadOnly(boolean readOnly)
    {
        this.readOnly = readOnly;
    }
    public String getSectionName()
    {
        return sectionName;
    }
    public void setSectionName(String sectionName)
    {
        this.sectionName = sectionName;
    }
    public String getTypeName()
    {
        return typeName;
    }
    public void setTypeName(String typeName)
    {
        this.typeName = typeName;
    }
    public DataCollection<?> getDc()
    {
        return dc;
    }
    public void setDc(DataCollection<?> dc)
    {
        this.dc = dc;
    }
}
