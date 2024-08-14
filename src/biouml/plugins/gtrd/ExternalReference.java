package biouml.plugins.gtrd;

public class ExternalReference
{
    private String externalDB;
    private String id;
    
    public ExternalReference()
    {
    }
    
    public ExternalReference(String externalDB, String id)
    {
        this.externalDB = externalDB;
        this.id = id;
    }

    public String getExternalDB()
    {
        return externalDB;
    }
    
    public void setExternalDB(String externalDB)
    {
        this.externalDB = externalDB;
    }
    
    public String getId()
    {
        return id;
    }
    
    public void setId(String id)
    {
        this.id = id;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( externalDB == null ) ? 0 : externalDB.hashCode() );
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if( this == obj )
            return true;
        if( obj == null )
            return false;
        if( getClass() != obj.getClass() )
            return false;
        ExternalReference other = (ExternalReference)obj;
        if( externalDB == null )
        {
            if( other.externalDB != null )
                return false;
        }
        else if( !externalDB.equals( other.externalDB ) )
            return false;
        if( id == null )
        {
            if( other.id != null )
                return false;
        }
        else if( !id.equals( other.id ) )
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        return "ExternalReference [externalDB=" + externalDB + ", id=" + id + "]";
    }
    
    
}
