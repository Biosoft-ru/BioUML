package biouml.standard.type;

import ru.biosoft.access.core.DataCollection;

/**
 * Many data elements can refer to other databases through its database references field.
 * This class is used to describe basic information about this databases
 * as well as to describe rules how information from corresponding databases can be retrieved.
 */
public class DatabaseInfo extends Referrer
{
    public DatabaseInfo(DataCollection parent, String name)
    {
        super(parent, name, DATABASE_INFO);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Properties
    //
    private String url;
    /** Database URL. */
    public String getURL()
    {
        return url;
    }
    public void setURL(String url)
    {
        String oldValue = this.url;
        this.url = url;
        firePropertyChange("url", oldValue, url);
    }
    
    private String urn;
    /** Database URN. */
    public String getURN()
    {
        return urn;
    }
    public void setURN(String urn)
    {
        String oldValue = this.urn;
        this.urn = urn;
        firePropertyChange("urn", oldValue, urn);
    }

    private String queryById;
    /**
     * Template to get database entry through http.
     * For this purpose it is necessary to replace '$id$'
     * by real id (primary key) in the template.
     */
    public String getQueryById()
    {
        return queryById;
    }
    public void setQueryById(String queryById)
    {
        String oldValue = this.queryById;
        this.queryById = queryById;
        firePropertyChange("queryById", oldValue, queryById);
    }

    private String queryByAc;
    /**
     * Template to get database entry through http.
     * For this purpose it is necessary to replace '$ac$'
     * by real ac (secondary key or accession  number) in the template.
     */
    public String getQueryByAc()
    {
        return queryByAc;
    }
    public void setQueryByAc(String queryByAc)
    {
        String oldValue = this.queryByAc;
        this.queryByAc = queryByAc;
        firePropertyChange("queryByAc", oldValue, queryByAc);
    }
}