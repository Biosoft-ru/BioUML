package ru.biosoft.table.export;

import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableDataCollection;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

/**
 * {@link TableDataCollection} wrapper for {@link DynamicPropertySet} array.
 */
public class TableDPSWrapper extends StandardTableDataCollection
{
    protected DynamicPropertySet[] dpsArray;
    public TableDPSWrapper(DynamicPropertySet[] dpsArray)
    {
        super(null, "dpsTable");

        getInfo().getProperties().setProperty(INTEGER_IDS, "true");
        this.dpsArray = dpsArray;

        if( dpsArray.length > 0 )
        {
            for(DynamicProperty dp : dpsArray[0])
            {
                columnModel.addColumn( dp.getDisplayName(), dp.getDisplayName(), dp.getShortDescription(), dp.getType(), null );
            }
            sortOrder.set();
        }
        for(int i=0; i<dpsArray.length; i++)
        {
            try
            {
                doPut(createRow(i), true);
            }
            catch( Exception e )
            {
                throw ExceptionRegistry.translateException( e );
            }
        }
    }

    private RowDataElement createRow(int index)
    {
        RowDataElement rde = new RowDataElement(Integer.toString(index), this);
        DynamicPropertySet dps = dpsArray[index];
        rde.setValues( columns().map( column -> dps.getValue( column.getName() ) ).toArray() );
        return rde;
    }
}