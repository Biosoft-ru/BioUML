package ru.biosoft.analysis;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.columnbeans.ColumnGroup;

@ClassIcon("resources/select-columns.gif")
public class SelectColumns extends AnalysisMethodSupport<SelectColumnsParameters>
{
    public SelectColumns(DataCollection origin, String name)
    {
        super(origin, name, new SelectColumnsParameters());
    }

    @Override
    public void validateParameters()
    {
        super.validateParameters();
        ColumnGroup columnGroup = parameters.getColumnGroup();
        if( columnGroup.getTable() == null )
            throw new IllegalArgumentException("Please specify table");
    }
    
    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        TableDataCollection table = parameters.getColumnGroup().getTable();
        DataElementPath output = parameters.getOutput();
        String[] columns = parameters.getColumnGroup().getNames();
        String[] newColumns = parameters.getColumnGroup().getNewNames();

        TableDataCollection result = TableDataCollectionUtils.join(TableDataCollectionUtils.INNER_JOIN, table, table, output, columns, new String[0],
                newColumns, new String[0]);
        DataCollectionUtils.copyPersistentInfo(result, table);
        CollectionFactoryUtils.save(result);
        getJobControl().setPreparedness(100);
        return result;
    }
}
