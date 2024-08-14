package ru.biosoft.analysis;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.filter.Filter;
import ru.biosoft.access.core.filter.FilteredDataCollection;
import ru.biosoft.access.exception.ParameterNotAcceptableException;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListenerAdapter;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowCountFilter;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.RowFilter;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

@ClassIcon("resources/filter-table.gif")
public class FilterTable extends AnalysisMethodSupport<FilterTableParameters>
{
    public static final String ANALYSIS_NAME = "Filter table";

    public FilterTable(DataCollection<?> origin, String name)
    {
        super(origin, name, new FilterTableParameters());
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkPaths();
        checkNotEmpty("filterExpression");
    }

    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        TableDataCollection inputTable = parameters.getInputPath().getDataElement(TableDataCollection.class);
        String expression = parameters.getFilterExpression();
        Filter<DataElement> rowFilter;
        try
        {
            if( parameters.getFilteringMode() == 0 )
            {
                rowFilter = new RowFilter( expression, inputTable );
            }
            else
            {
                rowFilter = new RowCountFilter( inputTable, expression, parameters.getValuesCount(), parameters.getFilteringMode() == 2 );
            }
        }
        catch( IllegalArgumentException e )
        {
            throw new ParameterNotAcceptableException(ExceptionRegistry.translateException(e), parameters, "filterExpression");
        }
        jobControl.pushProgress(0, 70);
        log.info("Filtering...");
        FunctionJobControl fjc = new FunctionJobControl( log, new JobControlListenerAdapter()
        {
            @Override
            public void valueChanged(JobControlEvent event)
            {
                jobControl.setPreparedness(event.getPreparedness());
            }
        });
        final FilteredDataCollection<RowDataElement> collection = new FilteredDataCollection<>( inputTable.getOrigin(),
                inputTable.getName(), inputTable, rowFilter, fjc, null );
        jobControl.popProgress();
        if(jobControl.isStopped()) return null;
        final TableDataCollection resultTable = TableDataCollectionUtils.createTableDataCollection(parameters.getOutputPath());
        ColumnModel oldCm = inputTable.getColumnModel();
        ColumnModel newCm = resultTable.getColumnModel();
        for( TableColumn tc : oldCm )
        {
            newCm.addColumn(newCm.cloneTableColumn(tc));
        }
        TableDataCollectionUtils.copySortOrder( inputTable, resultTable );
        jobControl.pushProgress(70, 100);
        log.info("Writing result...");
        jobControl.forCollection(collection.getNameList(), element -> {
            try
            {
                resultTable.addRow(collection.get(element).clone());
            }
            catch( Throwable t )
            {
                throw ExceptionRegistry.translateException(t);
            }
            return true;
        });
        collection.close();
        if(jobControl.isStopped())
        {
            parameters.getOutputPath().remove();
            return null;
        }
        jobControl.popProgress();
        resultTable.finalizeAddition();
        DataCollectionUtils.copyPersistentInfo(resultTable, inputTable);
        CollectionFactoryUtils.save(resultTable);
        return resultTable;
    }
}
