package biouml.standard.type;

import ru.biosoft.access.core.DataCollection;

public class DNA extends Biopolymer
{
    public DNA(DataCollection origin, String name)
    {
        super(origin, name);
    }

    @Override
    public String getType()
    {
        return TYPE_RNA;
    }
}
