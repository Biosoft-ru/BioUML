package biouml.standard.type;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.ClassIcon;

@ClassIcon( "resources/compartment.gif" )
@PropertyName("Compartment")
@PropertyDescription("Compartment.")
public class Compartment extends Concept
{
    private int spatialDimension = 3;// default value
    
    public Compartment( DataCollection origin, String name )
    {
        super(origin,name);
    }

    @Override
    public String getType()
    {
        return TYPE_COMPARTMENT;
    }

    @PropertyName("Spatial dimension")
    @PropertyDescription("Spatial dimension of the compartment.")
    public int getSpatialDimension()
    {
        return spatialDimension;
    }
    public void setSpatialDimension(int dimension)
    {
        this.spatialDimension = dimension;
    }
}
