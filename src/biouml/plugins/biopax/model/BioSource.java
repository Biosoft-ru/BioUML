package biouml.plugins.biopax.model;

import biouml.standard.type.Concept;
import ru.biosoft.access.core.DataCollection;

public class BioSource extends Concept
{
    public BioSource(DataCollection<?> origin, String name)
    {
        super(origin, name);
    }

    @Override
    public String getType()
    {
        return TYPE_CONCEPT;
    }
    
    private String celltype;
    
    public String getCelltype()
    {
        return celltype;
    }

    public void setCelltype(String celltype)
    {
        this.celltype = celltype;
    }

    private String tissue;
    
    public String getTissue()
    {
        return tissue;
    }

    public void setTissue(String tissue)
    {
        this.tissue = tissue;
    }
    
}
