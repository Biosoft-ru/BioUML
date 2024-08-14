package biouml.standard.type.access;

import java.io.File;
import java.util.Iterator;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.Index;
import ru.biosoft.access.core.Key;

/**
 * Title wrapper for any index
 */
public class WrapperTitleIndex extends TitleIndex
{
    protected Index<String> client;

    public WrapperTitleIndex(DataCollection dc, Index<String> client) throws Exception
    {
        super(dc, client.getName());
        this.client = client;

        for( Entry<String, String> entry : client.entrySet())
        {
            String id = entry.getKey();
            String title = entry.getValue();
            putInternal(id, title);
        }
    }

    protected void init()
    {
    }

    @Override
    public String getName()
    {
        return client.getName();
    }

    @Override
    public Iterator nodeIterator(Key key)
    {
        return client.nodeIterator(key);
    }

    @Override
    public void close() throws Exception
    {
        client.close();
        super.close();
    }

    @Override
    public boolean isValid()
    {
        return client.isValid();
    }

    @Override
    public File getIndexFile()
    {
        return client.getIndexFile();
    }

    /*
     public Object put ( Object arg0, Object arg1 )
     {
     //System.out.println ( "Put " + arg0 );
     return super.put ( arg0, arg1 );
     }

     public Object remove ( Object arg0 )
     {
     //System.out.println ( "Remove " + arg0 );
     return super.remove ( arg0 );
     }
     */

}
