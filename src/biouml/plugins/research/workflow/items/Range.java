package biouml.plugins.research.workflow.items;

import java.math.BigDecimal;

public class Range
{
    private BigDecimal first;
    private BigDecimal second;
    private BigDecimal last;

    public Range(String expression)
    {
        String[] range = expression.split( "\\.\\." );
        if( range.length != 2 )
            throwInvalidFormatException();
        try
        {
            last = new BigDecimal( range[1].trim() );
            int commaIdx = range[0].indexOf( ',' );
            if( commaIdx >= 0 )
            {
                first = new BigDecimal( range[0].substring( 0, commaIdx ).trim() );
                second = new BigDecimal( range[0].substring( commaIdx + 1 ).trim() );
            }
            else
            {
                first = new BigDecimal( range[0].trim() );
                if( last.compareTo( first ) >= 0 )
                    second = first.add( BigDecimal.ONE );
                else
                    second = first.subtract( BigDecimal.ONE );
            }
        }
        catch( NumberFormatException e )
        {
            throwInvalidFormatException();
        }
        if( second.compareTo( first ) == 0 )
            throw new IllegalArgumentException( "Range expression has the same first and second elements" );
        if( second.compareTo( first ) > 0 && last.compareTo( first ) < 0 )
            throw new IllegalArgumentException( "Range expression has second > first but last < first " );
        if( second.compareTo( first ) < 0 && last.compareTo( first ) > 0 )
            throw new IllegalArgumentException( "Range expression has second < first but last > first " );

    }

    private void throwInvalidFormatException()
    {
        throw new IllegalArgumentException( "Range expression has invalid format. Must be first..last or first,second..last" );
    }
    
    public BigDecimal getFirst()
    {
        return first;
    }
    
    public BigDecimal getSecond()
    {
        return second;
    }
    
    public BigDecimal getLast()
    {
        return last;
    }
    
    public int getCount()
    {
        BigDecimal by = second.subtract( first );
        BigDecimal length = last.subtract( first );
        return 1 + length.divideToIntegralValue( by ).intValue();
    }
    
    public String getValueAt(int i)
    {
        if(i < 0 || i >= getCount())
            return null;
        BigDecimal by = second.subtract( first );
        BigDecimal value = first.add( by.multiply( new BigDecimal(i) ) );
        return value.stripTrailingZeros().toPlainString();
    }
}
