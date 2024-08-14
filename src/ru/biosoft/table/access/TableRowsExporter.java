package ru.biosoft.table.access;

import java.util.Collection;
import java.util.function.Predicate;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPutException;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

/**
 * Exporter for table rows
 */
public class TableRowsExporter
{
    /**
     * Export rows to separate table located by path
     */
    public static void exportTable(DataElementPath path, TableDataCollection source, Collection<? extends RowDataElement> rows, JobControl jc)
    {
        exportTable( path, source, rows, de -> true, jc );
    }
    
    public static void exportTable(DataElementPath path, TableDataCollection source, Collection<? extends RowDataElement> rows, Predicate<DataElement> pred, JobControl jc)
    {
        TableDataCollection newTable = TableDataCollectionUtils.createTableDataCollection(path);
        ColumnModel oldCm = source.getColumnModel();
        ColumnModel newCm = newTable.getColumnModel();
        for( TableColumn tc : oldCm )
        {
            newCm.addColumn(newCm.cloneTableColumn(tc));
        }
        int size = rows.size();
        int i = 0;
        for(RowDataElement rde: rows)
        {
            if(pred.test( rde ))
            {
                try
                {
                    newTable.addRow(rde.clone());
                }
                catch( Exception e )
                {
                    throw new DataElementPutException( e, newTable.getCompletePath().getChildPath( rde.getName() ) );
                }
            }
            if(jc != null)
            {
                jc.setPreparedness( ++i/size );
                if(jc.getStatus() == JobControl.TERMINATED_BY_REQUEST)
                {
                    path.remove();
                    return;
                }
            }
        }
        newTable.finalizeAddition();
        DataCollectionUtils.copyPersistentInfo(newTable, source);
        CollectionFactoryUtils.save( newTable );
    }
}
