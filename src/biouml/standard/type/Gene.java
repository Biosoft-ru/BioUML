package biouml.standard.type;

import java.beans.PropertyDescriptor;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.util.bean.StaticDescriptor;

@ClassIcon ( "resources/gene.gif" )
public class Gene extends Biopolymer
{
    public static final PropertyDescriptor LOCATION_PD = StaticDescriptor.create("location", "Location");

    public Gene(DataCollection origin, String name)
    {
        super(origin, name);
    }

    @Override
    public String getType()
    {
        return TYPE_GENE;
    }
}
