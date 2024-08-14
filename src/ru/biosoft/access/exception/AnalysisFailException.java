package ru.biosoft.access.exception;

import ru.biosoft.exception.ExceptionDescriptor;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.LoggedException;

/**
 * @author lan
 *
 */
public class AnalysisFailException extends LoggedException
{
    private static String KEY_TITLE = "title";

    public static final ExceptionDescriptor ED_ANALYSIS_FAIL = new ExceptionDescriptor( "Analysis",
            LoggingLevel.Summary, "Analysis '$title$' failed.");

    public AnalysisFailException(Throwable cause, String title)
    {
        super(ExceptionRegistry.translateException(cause), ED_ANALYSIS_FAIL);
        properties.put( KEY_TITLE, title );
    }
}
