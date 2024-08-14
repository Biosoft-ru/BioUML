package ru.biosoft.server.servlets.genetics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import one.util.streamex.DoubleStreamEx;

import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.Util;

/**
 * Statistics functions
 */
public class Statistics
{
    /**
     * Get statistic for numeric column
     */
    public static StatInfo getNumericColumnStatistics(TableDataCollection tdc, TableColumn column)
    {
        if( !column.getType().isNumeric() ) return null;
        StatInfo result = new StatInfo();

        String cName = column.getName();
        Iterator<RowDataElement> iter = tdc.iterator();
        List<Double> values = new ArrayList<>();
        while( iter.hasNext() )
        {
            RowDataElement row = iter.next();
            Object value = row.getValue(cName);
            if( value != null )
            {
                try
                {
                    double val = Double.parseDouble(value.toString());
                    values.add(val);
                    result.number++;
                    if( val > result.max )
                    {
                        result.max = val;
                    }
                    if( val < result.min )
                    {
                        result.min = val;
                    }
                    result.average += val;
                }
                catch( NumberFormatException e )
                {
                    //just skip this row
                }
            }
        }
        result.average = result.average / ( result.number );
        for( Double val : values )
        {
            result.dispersion += ( val - result.average ) * ( val - result.average );
        }
        result.dispersion /= result.number;
        result.deviation = Math.sqrt(result.dispersion);

        result.median = DoubleStreamEx.of( values ).collect( Util.median() ).orElse( 0.0 );

        return result;
    }

    /**
     * Get statistic for non-numeric column
     */
    public static StatInfo2 getStringColumnStatistics(TableDataCollection tdc, TableColumn column)
    {
        if( column.getName().equals("EntityID") || column.getType().isNumeric() )
        {
            return null;
        }
        StatInfo2 result = new StatInfo2();

        String cName = column.getName();
        Iterator<RowDataElement> iter = tdc.iterator();
        while( iter.hasNext() )
        {
            RowDataElement row = iter.next();
            Object value = row.getValue(cName);
            if( value != null )
            {
                result.number++;
                result.addValue(value.toString());
            }
        }
        return result;
    }
}
