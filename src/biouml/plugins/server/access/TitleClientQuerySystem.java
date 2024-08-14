package biouml.plugins.server.access;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.Index;

import java.util.logging.Level;

import biouml.standard.type.access.TitleIndex;
import biouml.standard.type.access.WrapperTitleIndex;

/**
 * Special wrapper over ClientQuerySystem for
 * "title" indexes supporting
 */
public class TitleClientQuerySystem extends ClientQuerySystemSupport
{

    public TitleClientQuerySystem ( DataCollection dc ) throws Exception
    {
        super ( dc );
    }

    @Override
    public Index[] getIndexes()
    {
        //init indexes
        Index[] indexes = super.getIndexes();
        for ( int i = 0; i < indexes.length; i++ )
        {
            if ( indexes[i].getName ( ).equals ( "title" ) )
                try
                {
                    if ( ! ( indexes[i] instanceof TitleIndex ) )
                            indexesMap.put ( "title", new WrapperTitleIndex ( dc, indexes[i] ) );
                }
                catch ( Exception e )
                {
                    log.log( Level.SEVERE, "Wrapper creation error", e );
                }
        }
        return indexesMap.values ( ).toArray ( new Index[indexesMap.size ( )] );
    }

}
