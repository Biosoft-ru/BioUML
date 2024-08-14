package ru.biosoft.bsa;

import java.util.regex.Pattern;

import one.util.streamex.StreamEx;

/**
 * Genomic position
 * @author lan
 */
public class Position implements Comparable<Position>
{
    private String sequence;
    private final Interval interval;
    
    public Position(String position)
    {
        String positionStr = position == null ? "" : position.trim();
        int colonPos = positionStr.indexOf(':');
        sequence = "";
        if(colonPos >= 0)
        {
            String newSequence = positionStr.substring(0, colonPos).trim();
            sequence = Pattern.compile("^chr[\\.\\:]?").matcher(newSequence).replaceFirst("").trim();
            positionStr = positionStr.substring(colonPos+1).trim();
        }
        String[] coords = StreamEx.split( positionStr, "[\\-\\.]+" ).map( str -> str.replaceAll( "\\D", "" ) ).toArray( String[]::new );
        int from = coords[0].isEmpty() ? 1 : Integer.parseInt(coords[0]);
        int to = coords.length < 2 || coords[1].isEmpty() ? from+1 : Integer.parseInt(coords[1]);
        interval = new Interval(from, to);
    }
    
    public Position(Site site)
    {
        sequence = site.getOriginalSequence() != null ? site.getOriginalSequence().getName() : site.getName();
        interval = site.getInterval();
    }
    
    public Position(String sequence, Interval interval)
    {
        this.sequence = sequence;
        this.interval = interval;
    }

    @Override
    public int compareTo(Position o)
    {
        int result = sequence.compareTo(o.sequence);
        if(result != 0)
            return result;
        return interval.compareTo(o.interval);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( interval == null ) ? 0 : interval.hashCode() );
        result = prime * result + ( ( sequence == null ) ? 0 : sequence.hashCode() );
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if( this == obj )
            return true;
        if( obj == null )
            return false;
        if( getClass() != obj.getClass() )
            return false;
        Position other = (Position)obj;
        if( interval == null )
        {
            if( other.interval != null )
                return false;
        }
        else if( !interval.equals(other.interval) )
            return false;
        if( sequence == null )
        {
            if( other.sequence != null )
                return false;
        }
        else if( !sequence.equals(other.sequence) )
            return false;
        return true;
    }
    
    @Override
    public String toString()
    {
        return sequence+":"+interval.getFrom()+"-"+interval.getTo();
    }

    public String getSequence()
    {
        return sequence;
    }

    public Interval getInterval()
    {
        return interval;
    }
}
