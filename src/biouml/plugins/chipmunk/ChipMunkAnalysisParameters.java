package biouml.plugins.chipmunk;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;


/**
 * @author lan
 */
@PropertyName("ChIPMunk analysis")
@PropertyDescription("Motif search")
public class ChipMunkAnalysisParameters extends AbstractChipMunkParameters
{
    private static final long serialVersionUID = 1L;
    double gcPercent = 0.5;

    @PropertyName("GC percent")
    @PropertyDescription("Fraction of GC nucleotides (0..1), set to -1 for auto")
    public double getGcPercent()
    {
        return gcPercent;
    }

    public void setGcPercent(double gcPercent)
    {
        Object oldValue = this.gcPercent;
        this.gcPercent = gcPercent;
        firePropertyChange("gcPercent", oldValue, gcPercent);
    }
}
