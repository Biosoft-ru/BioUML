package biouml.plugins.cellml;

import ru.biosoft.access.core.DataCollection;

import biouml.standard.type.DiagramInfo;

import com.developmentontheedge.beans.DynamicPropertySet;

public class CellMLDiagramInfo extends DiagramInfo
{
    public CellMLDiagramInfo ( DataCollection origin, String name )
    {
        super ( origin, name );
    }

    public CellMLDiagramInfo ( String name )
    {
        super ( null, name );
    }

    DynamicPropertySet rdf;

    public DynamicPropertySet getRdf ( )
    {
        return rdf;
    }

    public void setRdf ( DynamicPropertySet rdf )
    {
        DynamicPropertySet oldValue = this.rdf;
        this.rdf = rdf;
        firePropertyChange ( "rdf", oldValue, rdf );
    }

}
