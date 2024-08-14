package biouml.standard.type;

import ru.biosoft.access.core.DataCollection;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.standard.type.Referrer;
import biouml.standard.type.Type;

@PropertyName ( "Species" )
@PropertyDescription ( "The terms <i>species</i> refers to entities that take part in reactions." )
public class Specie extends Referrer
{
    protected int charge;
    private String[] availableTypes;
    private static final String[] specieTypes = {Type.TYPE_SUBSTANCE, Type.TYPE_GENE, Type.TYPE_RNA, Type.TYPE_PROTEIN};
    
    public Specie(DataCollection parent, String name)
    {
        this(parent, name, TYPE_SUBSTANCE);
    }

    public Specie(DataCollection parent, String name, String type)
    {
        super(parent, name, type);
        availableTypes = specieTypes;
    }

    public Specie(DataCollection parent, String name, String type, String[] types)
    {
        super(parent, name, type);
        availableTypes = types;
    }
    
    @Override
    @PropertyName ( "Type" )
    @PropertyDescription ( "Type of species (chemical substance, enzyme, gene, etc.)" )
    public String getType()
    {
        return super.getType();
    }
    public void setType(String type)
    {
        String oldValue = this.type;
        this.type = type;
        firePropertyChange("type", oldValue, type);
    }

    @PropertyName ( "Charge" )
    @PropertyDescription ( "Indicates the charge of the species in terms of electrons." )
    public int getCharge()
    {
        return charge;
    }
    public void setCharge(int charge)
    {
        int oldValue = this.charge;
        this.charge = charge;
        firePropertyChange("charge", oldValue, charge);
    }
    
    public String[] getAvailableTypes()
    {
        return availableTypes;
    }
    public void setAvailableTypes(String[] types)
    {
        this.availableTypes = types;
        if( types.length > 0 )
            this.type = types[0];
    }
}
