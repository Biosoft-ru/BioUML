package ru.biosoft.table.datatype;

import ru.biosoft.graphics.chart.Chart;

public class ChartDataType extends DataType
{
    public ChartDataType()
    {
        super( Chart.class, "Chart", null );
    }
    @Override
    public Object convertValue(Object value)
    {
        if( value instanceof Chart )
            return value;
        if(value == null || value.equals( "null" ))
            return new Chart();
        CharSequence initString = value instanceof CharSequence ? (CharSequence)value : value
                .toString();
        return new Chart(initString);
    }
    @Override
    public boolean supportsLazyCharSequenceInit()
    {
        return true;
    }
}