package ru.biosoft.util;

import java.util.Objects;

public class PropertyInfo implements Comparable<PropertyInfo>
{
    private final String name;
    private final String displayName;
    
    public PropertyInfo(String name, String displayName)
    {
        this.name = name;
        this.displayName = displayName;
    }

    /**
     * @return property name
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return property display name
     */
    public String getDisplayName()
    {
        return displayName;
    }
    
    @Override
    public String toString()
    {
        return getDisplayName();
    }

    @Override
    public int compareTo(PropertyInfo p)
    {
        return getDisplayName().compareTo(p.getDisplayName());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( name, displayName );
    }

    @Override
    public boolean equals(Object obj)
    {
        if( this == obj )
            return true;
        if( obj == null || getClass() != obj.getClass() )
            return false;
        PropertyInfo other = (PropertyInfo)obj;
        return Objects.equals( displayName, other.displayName ) && Objects.equals( name, other.name );
    }
}