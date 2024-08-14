package ru.biosoft.server.servlets.webservices.providers;

import java.util.logging.Level;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.filter.Filter;
import ru.biosoft.analysis.FilterTable;
import ru.biosoft.analysis.FilterTableParameters;
import ru.biosoft.jobcontrol.StackProgressJobControl;
import ru.biosoft.server.servlets.webservices.providers.WebTablesProvider.TableExporter;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.RowFilter;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.access.TableRowsExporter;
import ru.biosoft.tasks.TaskManager;

public class RegularTableExporter implements TableExporter<RowDataElement, TableDataCollection>
{
    @Override
    public void export(TableDataCollection table,
            DataElementPath path, String filterExpression, StackProgressJobControl jc)
    {
        FilterTable analysis = new FilterTable( null, FilterTable.ANALYSIS_NAME );
        FilterTableParameters parameters = analysis.getParameters();
        analysis.setLogger( WebTablesProvider.log );
        parameters.setInputPath( DataElementPath.create( table ) );
        parameters.setOutputPath( path );
        parameters.setFilterExpression( filterExpression );
        try
        {
            TaskManager.getInstance().addAnalysisTask( analysis, jc, false );
        }
        catch( Exception e )
        {
            WebTablesProvider.log.log(Level.SEVERE,  "Error while creating history entry for filter: ", e );
        }
        Filter<DataElement> filter = new RowFilter( filterExpression, table );
        TableRowsExporter.exportTable( path, table, DataCollectionUtils.asCollection( table, RowDataElement.class ), filter::isAcceptable, jc );
    }

    @Override
    public Class<?> getSupportedCollectionType()
    {
        return TableDataCollection.class;
    }
}