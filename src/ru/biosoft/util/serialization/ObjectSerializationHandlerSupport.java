package ru.biosoft.util.serialization;

import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: puz
 * Date: 05.05.2006
 * Time: 12:49:13
 */
public abstract class ObjectSerializationHandlerSupport implements ObjectSerializationHandler
{
    protected Map<Object, String> aliases = new HashMap<>();
    protected Writer writer = null;

    @Override
    public void alias( Class<?> clazz, String alias ) throws SerializationException
    {
        if( aliases.containsKey( clazz.getName() ) )
        {
            throw new SerializationException( "Two aliases for type " + clazz.getName(), null );
        }

        if( aliases.containsValue( alias ) )
        {
            throw new SerializationException( "Duplicate alias '" + alias + "' for type " + clazz.getName(), null );
        }

        aliases.put( clazz.getName(), alias );
    }

    protected String getObjectName( String name )
    {
        String alias = aliases.get( name );
        if( alias != null )
        {
            return alias;
        }
        return name;
    }

    protected boolean hasAlias( Class<?> clazz )
    {
        return aliases.containsKey( clazz.getName() );
    }

    @Override
    public void setWriter( Writer writer )
    {
        this.writer = writer;
    }
}
