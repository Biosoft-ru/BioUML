package biouml.plugins.riboseq.db.model;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class Species extends DataElementSupport
{
    public Species(DataCollection<Species> origin, String name)
    {
        super( name, origin );
    }

    private String latinName;
    @PropertyName("Latin name")
    @PropertyDescription("Species latin name")
    public String getLatinName()
    {
        return latinName;
    }
    public void setLatinName(String latinName)
    {
        this.latinName = latinName;
    }

    private String commonName;
    @PropertyName("Common name")
    @PropertyDescription("Common name")
    public String getCommonName()
    {
        return commonName;
    }

    public void setCommonName(String commonName)
    {
        this.commonName = commonName;
    }
    
    @Override
    public String toString()
    {
        return commonName;
    }
}
