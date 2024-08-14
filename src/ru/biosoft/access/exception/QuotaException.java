package ru.biosoft.access.exception;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.exception.ExceptionDescriptor;
import ru.biosoft.util.TextUtil;

public class QuotaException extends RepositoryException
{
    private static String KEY_QUOTA = "quota";
    private static String KEY_PROJECT_SIZE = "projectSize";

    public static final ExceptionDescriptor ED_QUOTA = new ExceptionDescriptor( "Quota",
            LoggingLevel.Summary, "Quota exceeded in $path/name$. Quota: $quota$; used: $projectSize$");

    public QuotaException(DataElementPath path, long quota, long projectSize)
    {
        super(ED_QUOTA, path);
        properties.put( KEY_QUOTA, TextUtil.formatSize( quota ) );
        properties.put( KEY_PROJECT_SIZE, TextUtil.formatSize( projectSize ) );
    }
}