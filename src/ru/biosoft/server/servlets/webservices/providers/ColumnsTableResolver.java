package ru.biosoft.server.servlets.webservices.providers;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.access.TableResolver;
import ru.biosoft.table.document.editors.TableElement;

/**
 * Table resolver for column structure.
 * Is used in Columns view part
 */

public class ColumnsTableResolver extends TableResolver
{
    protected TableResolver baseResolver;

    public ColumnsTableResolver(BiosoftWebRequest arguments) throws Exception
    {
        String type = arguments.get( "type" );
        Class<? extends TableResolver> resolverClass = WebTablesProvider.resolverRegistry.getExtension( type );
        if( resolverClass != null )
        {
            baseResolver = resolverClass.getConstructor( BiosoftWebRequest.class ).newInstance( arguments );
        }
    }
    public ColumnsTableResolver(TableResolver baseResolver)
    {
        this.baseResolver = baseResolver;
    }

    @Override
    public DataCollection<?> getTable(DataElement de) throws Exception
    {
        if( baseResolver != null )
        {
            de = baseResolver.getTable( de );
        }
        TableDataCollection dc = de.cast( TableDataCollection.class );
        VectorDataCollection<TableElement> columns = new VectorDataCollection<>( "Columns", TableElement.class, null );
        columns.put( new TableElement( dc, -1 ) );
        int columnCount = dc.getColumnModel().getColumnCount();
        for( int i = 0; i < columnCount; i++ )
        {
            columns.put( new TableElement( dc, i ) );
        }
        return columns;
    }
}