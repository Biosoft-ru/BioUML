package ru.biosoft.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class NumberFormatter
{
    protected FormatInterval[] intervals;

    public NumberFormatter(FormatInterval[] intervals)
    {
        this.intervals = intervals;
    }

    public String format(double d)
    {
        for( FormatInterval fi : intervals )
        {
            if( d >= fi.from && d < fi.to )
            {
                return fi.numberFormat.format(d);
            }
        }
        return String.valueOf(d);
    }

    public static class FormatInterval
    {
        double from;
        double to;
        NumberFormat numberFormat;

        public FormatInterval(double from, double to, String format)
        {
            this.from = from;
            this.to = to;
            this.numberFormat = new DecimalFormat(format);
        }

        public FormatInterval(double from, double to, NumberFormat format)
        {
            this.from = from;
            this.to = to;
            this.numberFormat = format;
        }
    }
}
