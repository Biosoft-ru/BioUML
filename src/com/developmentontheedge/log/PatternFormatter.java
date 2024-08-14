package com.developmentontheedge.log;

import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class PatternFormatter extends Formatter
{
    private String pattern;

    public PatternFormatter(String pattern)
    {
        this.pattern = pattern;
    }

    @Override
    public String format(LogRecord record)
    {
        return String.format( pattern, new Date( record.getMillis() ), record.getSourceClassName(), record.getLoggerName(),
                record.getLevel(), record.getMessage(), record.getThrown() );
    }

}
