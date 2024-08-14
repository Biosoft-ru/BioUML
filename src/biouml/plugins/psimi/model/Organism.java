package biouml.plugins.psimi.model;

import biouml.standard.type.Concept;
import ru.biosoft.access.core.DataCollection;

@SuppressWarnings ( "serial" )
public class Organism extends Concept
{
    public Organism(DataCollection<?> origin, String name)
    {
        super(origin, name);
    }

    @Override
    public String getType()
    {
        return TYPE_CONCEPT;
    }
    
    private Concept celltype;
    private Concept compartment;
    private Concept tissue;
    
    public Concept getCelltype()
    {
        return celltype;
    }

    public void setCelltype(Concept celltype)
    {
        this.celltype = celltype;
    }

    public Concept getCompartment()
    {
        return compartment;
    }

    public void setCompartment(Concept compartment)
    {
        this.compartment = compartment;
    }

    public Concept getTissue()
    {
        return tissue;
    }

    public void setTissue(Concept tissue)
    {
        this.tissue = tissue;
    }
    
    
}
