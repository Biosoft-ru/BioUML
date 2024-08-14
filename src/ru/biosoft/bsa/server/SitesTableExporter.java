package ru.biosoft.bsa.server;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.filter.Filter;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.access.SitesTableCollection;
import ru.biosoft.bsa.access.TransformedSite;
import ru.biosoft.jobcontrol.StackProgressJobControl;
import ru.biosoft.server.servlets.webservices.providers.WebTablesProvider.TableExporter;
import ru.biosoft.table.RowFilter;

public class SitesTableExporter implements TableExporter<TransformedSite, SitesTableCollection>
{
    @Override
    public void export(SitesTableCollection table,
            DataElementPath path, String filterExpression, StackProgressJobControl jc)
    {
        Track track = table.getTrack();
        final SqlTrack result = SqlTrack.createTrack( path, track, track.getClass() );
        Filter<? super TransformedSite> filter = new RowFilter( filterExpression, table );
        jc.pushProgress( 1, 90 );
        jc.forCollection( DataCollectionUtils.asCollection( table, TransformedSite.class ), site -> {
            if(filter.isAcceptable( site ))
                result.addSite( site.getSite() );
            return true;
        });
        jc.popProgress();
        result.finalizeAddition();
        CollectionFactoryUtils.save( result );
    }

    @Override
    public Class<?> getSupportedCollectionType()
    {
        return SitesTableCollection.class;
    }
}