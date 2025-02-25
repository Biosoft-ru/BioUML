package ru.biosoft.util;

public class Pair<T, U>
{
    private T first;
    private U second;

    public Pair(T first, U second)
    {
        this.first = first;
        this.second = second;
    }

    public Pair()
    {
    }

    public T getFirst()
    {
        return first;
    }

    public void setFirst(T first)
    {
        this.first = first;
    }

    public U getSecond()
    {
        return second;
    }

    public void setSecond(U second)
    {
        this.second = second;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( first == null ) ? 0 : first.hashCode() );
        result = prime * result + ( ( second == null ) ? 0 : second.hashCode() );
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
        Pair<?,?> other = (Pair<?,?>)obj;
        if( first == null )
        {
            if( other.first != null )
                return false;
        }
        else if( !first.equals( other.first ) )
            return false;
        if( second == null )
        {
            if( other.second != null )
                return false;
        }
        else if( !second.equals( other.second ) )
            return false;
        return true;
    }
    
    @Override
    public String toString()
    {
        return "[ "+getFirst()+" , "+ getSecond()+" ] ";
    }
}
