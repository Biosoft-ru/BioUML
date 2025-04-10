package ru.biosoft.access.exception;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.exception.ExceptionDescriptor;
import ru.biosoft.util.TextUtil2;

/**
 * @author lan
 *
 */
public class CollectionLoginException extends RepositoryException
{
    private static String KEY_USER = "user";

    public static final ExceptionDescriptor ED_CANNOT_LOGIN = new ExceptionDescriptor( "CannotLogin",
            LoggingLevel.Summary, "Unable to login to collection $path$ as $user$.");
    public static final ExceptionDescriptor ED_LOGIN_INCORRECT = new ExceptionDescriptor( "LoginIncorrect",
            LoggingLevel.Summary, "Incorrect user name or password for collection $path$ (user name: $user$).");

    public CollectionLoginException(Throwable t, DataElementPath path, String user)
    {
        super(t, ED_CANNOT_LOGIN, path);
        properties.put( KEY_USER, TextUtil2.isEmpty( user ) ? "anonymous" : user );
    }

    public CollectionLoginException(DataElementPath path, String user)
    {
        super(null, ED_LOGIN_INCORRECT, path);
        properties.put( KEY_USER, TextUtil2.isEmpty( user ) ? "anonymous" : user );
    }

    public CollectionLoginException(Throwable t, DataElement de, String user)
    {
        this(t, DataElementPath.create(de), user);
    }

    public CollectionLoginException(DataElement de, String user)
    {
        this(DataElementPath.create(de), user);
    }
}
