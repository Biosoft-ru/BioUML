package biouml.standard.type;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.ClassIcon;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@PropertyName ( "Substance" )
@ClassIcon ( "resources/substance.gif" )
public class Substance extends Molecule
{
    private String casRegistryNumber;
    private String formula;

    public Substance(DataCollection origin, String name)
    {
        super(origin, name);
    }

    @Override
    public String getType()
    {
        return TYPE_SUBSTANCE;
    }

    @PropertyName ( "Formula" )
    @PropertyDescription ( "Substance chemical formula." )
    public String getFormula()
    {
        return formula;
    }
    public void setFormula(String formula)
    {
        String oldValue = this.formula;
        this.formula = formula;
        firePropertyChange("formula", oldValue, formula);
    }

    @PropertyName ( "CAS" )
    @PropertyDescription ( "CAS Registry Number. <br>" + "It provides unique substance numeric identifier and <br>"
            + "can contain up to 9 digits, divided by hyphens into 3 parts. <br>"
            + "For example, 58-08-2 is the CAS Registry Number for caffeine." )
    public String getCasRegistryNumber()
    {
        return casRegistryNumber;
    }
    public void setCasRegistryNumber(String casRegistryNumber)
    {
        String oldValue = this.casRegistryNumber;
        this.casRegistryNumber = casRegistryNumber;
        firePropertyChange("casRegistryNumber", oldValue, casRegistryNumber);
    }
}
