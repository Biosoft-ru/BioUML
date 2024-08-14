package biouml.plugins.cellml;

import com.developmentontheedge.beans.Preferences;

import biouml.standard.type.BaseSupport;
import ru.biosoft.access.core.DataCollection;

/**
 * This is a wrapper class to handle RDF elemenet assotiated with CellML component.
 *
 * @pending stub
 */
@SuppressWarnings ( "serial" )
public class Species extends BaseSupport
{
    public Species(DataCollection<?> parent, String name)
    {
        super(parent, name);
        type = TYPE_SUBSTANCE;
    }

    public Species( DataCollection<?> parent, String name, String type)
    {
        super(parent, name);
        this.type = type;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Properties
    //

    public void setType(String type)
    {
        String oldValue = this.type;
        this.type = type;
        firePropertyChange("type", oldValue, type);
    }

    Preferences rdf;
    public Preferences getRdf()
    {
        return rdf;
    }

    public void setRdf(Preferences rdf)
    {
        Preferences oldValue = this.rdf;
        this.rdf = rdf;
        firePropertyChange("rdf", oldValue, rdf);
    }
}