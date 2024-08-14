package biouml.plugins.obo;

import java.util.logging.Level;

import biouml.standard.type.access.TitleIndex;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DefaultQuerySystem;
import ru.biosoft.access.core.Index;

public class OboQuerySystem extends DefaultQuerySystem
{
    protected DataCollection dc;

    public OboQuerySystem(DataCollection dc)
    {
        super(dc);
        this.dc = dc;
    }

    @Override
    public Index getIndex(String name)
    {
        try
        {
            if( name.equals("title") )
            {
                return new TitleIndex(dc, "title");
            }
        }
        catch( Exception e )
        {
            cat.log(Level.SEVERE, "Can't create title index: dc = " + dc.getName() + " ; " + e);
        }
        return super.getIndex(name);
    }
}
