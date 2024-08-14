package ru.biosoft.table.access;

import java.util.List;

import com.developmentontheedge.beans.DynamicPropertySet;

import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;
import ru.biosoft.jobcontrol.SubFunctionJobControl;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;

public class SubsetTableAction extends BackgroundDynamicAction
{
    @Override
    public boolean isApplicable(Object object)
    {
        if( object instanceof TableDataCollection )
        {
            return true;
        }
        return false;
    }
    
    @Override
    public JobControl getJobControl(final Object model, final List<DataElement> selectedItems, final Object properties) throws Exception
    {
        return new AbstractJobControl(log){
            @Override
            protected void doRun() throws JobControlException
            {
                try
                {
                    DataElementPath destination = (DataElementPath)((DynamicPropertySet)properties).getValue("target");
                    // Cast everything in advance to manifest internal errors if any
                    List<RowDataElement> rows = StreamEx.of(selectedItems).map(item -> item.cast( RowDataElement.class )).toList();
                    TableRowsExporter.exportTable(destination, (TableDataCollection)model, rows, new SubFunctionJobControl( this, 0, 95 ));
                    DataCollection<DataElement> result = destination.getDataCollection();
                    DataCollectionUtils.copyPersistentInfo(result, (DataCollection)model);
                    CollectionFactoryUtils.save(result);
                    setPreparedness( 100 );
                    resultsAreReady(new Object[]{result});
                }
                catch( Exception e )
                {
                    throw new JobControlException(e);
                }
            }
        };
    }

    @Override
    public Object getProperties(Object model, List<DataElement> selectedItems)
    {
        DataElementPath sourcePath = DataElementPath.create((DataElement)model);
        return getTargetProperties(TableDataCollection.class, sourcePath.getSiblingPath(sourcePath.getName() + " subset"));
    }
}
