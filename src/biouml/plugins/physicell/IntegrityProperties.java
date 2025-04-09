package biouml.plugins.physicell;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.CellIntegrity;

@PropertyName ( "Cell Integrity" )
public class IntegrityProperties
{
    private CellIntegrity integrity = new CellIntegrity();

    public IntegrityProperties()
    {
    }

    public IntegrityProperties(CellIntegrity integrity)
    {
        this.integrity = integrity;
    }

    public void createIntegrity(CellDefinition cd)
    {
        cd.phenotype.cellIntegrity = integrity.clone();
    }

    @PropertyName ( "Damage rate" )
    public double getDamageRate()
    {
        return integrity.damage_rate;
    }
    public void setDamageRate(double damageRate)
    {
        integrity.damage_rate = damageRate;
    }

    @PropertyName ( "Damage repair rate" )
    public double getDamageRepairRate()
    {
        return integrity.damage_repair_rate;
    }
    public void setDamageRepairRate(double repairRate)
    {
        integrity.damage_repair_rate = repairRate;
    }
}