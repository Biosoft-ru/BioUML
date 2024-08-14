
package biouml.plugins.cellml;

import ru.biosoft.access.core.DataCollection;

import biouml.standard.type.Reaction;

import com.developmentontheedge.beans.DynamicPropertySet;

public class CellMLReaction extends Reaction
{
    public CellMLReaction(DataCollection origin, String name)
    {
        super(origin, name);
    }

    DynamicPropertySet rdf;
    public DynamicPropertySet getRdf()
    {
        return rdf;
    }

    public void setRdf(DynamicPropertySet rdf)
    {
        DynamicPropertySet oldValue = this.rdf;
        this.rdf = rdf;
        firePropertyChange("rdf", oldValue, rdf);
    }
}


