package biouml.plugins.research;

import java.util.Properties;

import biouml.standard.type.access.TitleIndex;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataElement;

/**
 * Title index for Research collection
 * Title is stored as displayName DataCollection property
 */
@SuppressWarnings ( "serial" )
public class ResearchTitleIndex extends TitleIndex
{
    public ResearchTitleIndex(DataCollection<?> dc, String indexName) throws Exception
    {
        super( dc, indexName );
    }

    @Override
    public String getTitle(DataElement de)
    {
        if( de instanceof DataCollection )
        {
            DataCollection<?> dc2 = (DataCollection<?>)de;
            Properties props = dc2.getInfo().getProperties();
            if( props.containsKey( ru.biosoft.access.core.DataCollectionConfigConstants.DISPLAY_NAME_PROPERTY ) )
                return props.getProperty( ru.biosoft.access.core.DataCollectionConfigConstants.DISPLAY_NAME_PROPERTY );
        }
        return de.getName();
    }

    @Override
    public void elementChanged(DataCollectionEvent e) throws Exception
    {
        DataElement de = e.getDataElement();
        if( de != null )
        {
            String newTitle = getTitle( de );
            remove( de.getName() );
            put( de.getName(), newTitle );
        }
    }

    @Override
    protected String putInternal(String id, String title)
    {
        String newTitle = title2id.containsKey( title ) ? getCompositeName( title, id ) : title;
        return super.putInternal( id, newTitle );
    }
}
