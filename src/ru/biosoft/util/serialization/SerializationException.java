package ru.biosoft.util.serialization;

/**
 * Created by IntelliJ IDEA.
 * User: puz
 * Date: 06.05.2006
 * Time: 15:04:11
 */
public class SerializationException extends Exception
{
    private Exception cause;

    public SerializationException( String message, Exception cause )
    {
        super( message );
        this.cause = cause;
    }

    public SerializationException( Exception cause )
    {
        this.cause = cause;
    }

    @Override
    public Throwable getCause()
    {
        return cause;
    }
}
