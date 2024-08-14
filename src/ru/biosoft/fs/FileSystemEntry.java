package ru.biosoft.fs;

public class FileSystemEntry implements Comparable<FileSystemEntry>
{
    private final String name;
    private final boolean directory;
    
    public FileSystemEntry(String name, boolean directory)
    {
        super();
        this.name = name;
        this.directory = directory;
    }
    
    public String getName()
    {
        return name;
    }
    
    public boolean isDirectory()
    {
        return directory;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( directory ? 1231 : 1237 );
        result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
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
        FileSystemEntry other = (FileSystemEntry)obj;
        if( directory != other.directory )
            return false;
        if( name == null )
        {
            if( other.name != null )
                return false;
        }
        else if( !name.equals( other.name ) )
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        return name+" ["+(directory?'d':'f')+"]";
    }

    @Override
    public int compareTo(FileSystemEntry o)
    {
        if(directory && !o.directory)
            return -1;
        if(!directory && o.directory)
            return 1;
        return name.compareTo( o.name );
    }
}
