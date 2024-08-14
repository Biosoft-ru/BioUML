package biouml.plugins.biopax;

import java.util.logging.Level;

import biouml.standard.type.access.TitleSqlNoHtmlIndex;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DefaultQuerySystem;
import ru.biosoft.access.core.Index;

public class BioPAXQuerySystem extends DefaultQuerySystem
{
    protected DataCollection dc;

    public BioPAXQuerySystem(DataCollection dc)
    {
        super(dc);
        this.dc = dc;
    }

    @Override
    public Index getIndex(String name)
    {
        try
        {
            /*if( name.equals("title"))
                return new TitleIndex(dc, "title");*/
            
            if( name.equals("title") && !indexes.containsKey("title"))
                indexes.put("title", new TitleSqlNoHtmlIndex(dc, "title"));
        }
        catch( Exception e )
        {
            cat.log(Level.SEVERE, "Can't create title index: dc = " + dc.getName() + " ; " + e);
        }
        return super.getIndex(name);
    }
}
