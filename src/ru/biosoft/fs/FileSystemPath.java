package ru.biosoft.fs;

import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import ru.biosoft.access.exception.Assert;

public final class FileSystemPath
{
    private static final FileSystemPath ROOT = new FileSystemPath();
    
    private final FileSystemPath parent;
    private final String name;
    
    private FileSystemPath()
    {
        this.parent = null;
        this.name = null;
    }

    private FileSystemPath(FileSystemPath parent, String name)
    {
        this.parent = parent;
        this.name = name;
    }
    
    public @Nonnull FileSystemPath child(String name)
    {
        Assert.notNull( "name", name );
        return new FileSystemPath( this, name );
    }
    
    public @Nullable FileSystemPath parent()
    {
        return parent;
    }
    
    public @Nullable String name()
    {
        return name;
    }
    
    public @Nonnull String[] components()
    {
        if(this == ROOT)
            return new String[0];
        int n=0;
        FileSystemPath ancestor = parent;
        while(ancestor != null)
        {
            n++;
            ancestor = ancestor.parent;
        }
        String[] result = new String[n];
        ancestor = this;
        do
        {
            result[--n] = ancestor.name;
            ancestor = ancestor.parent;
        } while(ancestor != ROOT);
        return result;
    }

    public static FileSystemPath of()
    {
        return ROOT;
    }
    
    public static FileSystemPath of(String... components)
    {
        FileSystemPath current = of();
        for(String component : components)
        {
            current = current.child( component );
        }
        return current;
    }

    @Override
    public int hashCode()
    {
        if(this == ROOT)
            return 1;
        final int prime = 31;
        int result = 1;
        result = prime * result + name.hashCode();
        result = prime * result + parent.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if( this == obj )
            return true;
        if( obj == null )
            return false;
        if( this == ROOT )
            return obj == ROOT;
        if( getClass() != obj.getClass() )
            return false;
        FileSystemPath other = (FileSystemPath)obj;
        if( !name.equals( other.name ) )
            return false;
        if( !parent.equals( other.parent ) )
            return false;
        return true;
    }

    /**
     * Caution: different paths may produce equal toString() results
     * Use for debug purposes only
     */
    @Override
    public String toString()
    {
        return Arrays.toString( components() );
    }
}
