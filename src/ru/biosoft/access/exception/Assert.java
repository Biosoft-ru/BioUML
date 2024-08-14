package ru.biosoft.access.exception;

import ru.biosoft.exception.InternalException;

/**
 * @author lan
 *
 */
public class Assert
{
    public static void isTrue(String title, boolean value) throws InternalException
    {
        if(!value)
            throw new InternalException(title);
    }

    public static <T> T notNull(String title, T value) throws InternalException
    {
        if(value == null)
            throw new InternalException("Null encountered where non-null expected: "+title);
        return value;
    }
}
