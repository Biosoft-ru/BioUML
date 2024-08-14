package ru.biosoft.util.serialization.utils;

import java.util.Calendar;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.text.ParseException;

public class TimeUtils
{
    private static final SimpleDateFormat dateFormatterSQL = new SimpleDateFormat( "yyyy-MM-dd" );
    private static final SimpleDateFormat timeFormatter = new SimpleDateFormat( "HH:mm:ss" );
    private static final SimpleDateFormat dateTimeFormatterSQL = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
    private static final SimpleDateFormat GMTdateTimeFormatterSQL = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
    public static final TimeZone gmtTimeZone = TimeZone.getTimeZone( "GMT" );

    public static int string2Seconds( String time )
    {
        int d = time.indexOf( ':' );
        if( d < 0 )
        {
            return 0;
        }
        String hs = time.substring( 0, d ).trim();
        int res = 0;
        try
        {
            res = Integer.parseInt( hs ) * 60 * 60; //hours
            int d1 = time.indexOf( ':', d + 1 );
            if( d1 < 0 )
            {
                hs = time.substring( d + 1 ).trim();
            }
            else
            {
                hs = time.substring( d + 1, d1 ).trim();
            }
            try
            {
                res += Integer.parseInt( hs ) * 60; //minutes
                int d2 = time.indexOf( ':', d1 + 1 );
                if( d2 < 0 )
                {
                    hs = time.substring( d1 + 1 ).trim();
                }
                else
                {
                    hs = time.substring( d1 + 1, d2 ).trim();
                }
                res += Integer.parseInt( hs ); //seconds
            }
            catch( NumberFormatException e )
            {
            }
        }
        catch( NumberFormatException e )
        {
        }
        return res;
    }

    public static String milliseconds2TimeString( long time )
    {
        return seconds2TimeString( ( int )time / 1000 );
    }

    public static String seconds2TimeString( int time )
    {
        if( time < 0 )
        {
            return "N/A";
        }
        int hours = time / ( 60 * 60 );
        time = time % ( 60 * 60 );
        int minutes = time / 60;
        int seconds = time % 60;
        StringBuffer sb = new StringBuffer();
        add2( sb, hours ).append( ':' );
        add2( sb, minutes ).append( ':' );
        add2( sb, seconds );
        return sb.toString();
    }

    private static StringBuffer add2( StringBuffer sb, int t )
    {
        if( t < 10 )
        {
            sb.append( '0' );
        }
        sb.append( t );
        return sb;
    }

    public static String[] generateDateRangeValues( String fromDateString, String toDateString )
    {
        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();

        if( fromDateString == null || toDateString == null )
        {
            return null;
        }
        try
        {
            java.sql.Date fromDate = java.sql.Date.valueOf( fromDateString );
            java.sql.Date toDate = java.sql.Date.valueOf( toDateString );

            from.setTime( fromDate );
            to.setTime( toDate );
        }
        catch( IllegalArgumentException e )
        {
            String fr = fromDateString;
            String toD = toDateString;
            if( fr.equals( toD ) )
            {
                return new String[]{ fr };
            }
            return new String[]{ fr, toD };
        }
        if( from.after( to ) )
        {
            return null;
        }

        ArrayList<String> list = new ArrayList<>();
        while( from.before( to ) )
        {
            list.add( formatAsSQLDate( from.getTime() ) );
            from.add( Calendar.DATE, 1 );
        }
        list.add( formatAsSQLDate( to.getTime() ) );

        return list.toArray( new String[list.size()] );
    }

    public static String formatAsSQLDate( Date date )
    {
        synchronized( dateFormatterSQL )
        {
            return dateFormatterSQL.format( date );
        }
    }

    public static List<String> formatAsSQLDate( List<Date> dates )
    {
        if( dates == null )
        {
            return null;
        }

        List<String> result = new ArrayList<>();
        for(Date date : dates)
        {
            result.add( formatAsSQLDate( date ) );
        }
        return result;
    }

    public static String formatAsTime( Date date )
    {
        synchronized( timeFormatter )
        {
            return timeFormatter.format( date );
        }
    }

    public static String formatAsSQLDateTime( Date date )
    {
        synchronized( dateTimeFormatterSQL )
        {
            return dateTimeFormatterSQL.format( date );
        }
    }

    public static Date getDateFromSQLString( String sDate ) throws Exception
    {
        synchronized( dateFormatterSQL )
        {
            return dateFormatterSQL.parse( sDate );
        }
    }

    public static Date getDateTimeFromSQLString( String sDate ) throws Exception
    {
        synchronized( dateTimeFormatterSQL )
        {
            return dateTimeFormatterSQL.parse( sDate );
        }
    }

    /**
     * Parse string as date represented in GMT tz.
     *
     * @param sDate
     * @return
     * @throws Exception
     */
    public static Date getGMTDateTimeFromSQLString( String sDate ) throws Exception
    {
        synchronized( GMTdateTimeFormatterSQL )
        {
            synchronized( gmtTimeZone )
            {
                GMTdateTimeFormatterSQL.setTimeZone( gmtTimeZone );
                return GMTdateTimeFormatterSQL.parse( sDate );
            }
        }

    }

    public static String formatAsSQLGMTDateTime( Date date )
    {
        synchronized( GMTdateTimeFormatterSQL )
        {
            synchronized( gmtTimeZone )
            {
                GMTdateTimeFormatterSQL.setTimeZone( gmtTimeZone );
                return GMTdateTimeFormatterSQL.format( date );
            }
        }
    }

    public static String getDayOfWeek()
    {
        int dayOfWeek = Calendar.getInstance().getTime().getDay();
        switch( dayOfWeek )
        {
            case Calendar.MONDAY:
                return "monday";
            case Calendar.TUESDAY:
                return "tuesday";
            case Calendar.WEDNESDAY:
                return "wednesday";
            case Calendar.THURSDAY:
                return "thursday";
            case Calendar.FRIDAY:
                return "friday";
            case Calendar.SATURDAY:
                return "saturday";
            default:
                return "sunday";
        }
    }

    public static String getAsNumber( Date date )
    {
        final String sDate = formatAsSQLDate( date );
        return sDate.substring( 0, 4 ) + sDate.substring( 5, 7 ) + sDate.substring( 8, 10 );
    }

    public static String formatAsLocalizedDateTime( Date date, Locale locale )
    {
        return DateFormat.getDateTimeInstance( DateFormat.DEFAULT, DateFormat.DEFAULT, locale ).format( date );
    }

    public static Date parseLocalizedDateTime( String date, Locale locale ) throws ParseException
    {
        return DateFormat.getDateTimeInstance( DateFormat.DEFAULT, DateFormat.DEFAULT, locale ).parse( date );
    }

    public static String formatAsLocalizedDate( Date date, Locale locale )
    {
        return DateFormat.getDateInstance( DateFormat.DEFAULT, locale ).format( date );
    }

    public static Date parseLocalizedDate( String date, Locale locale ) throws ParseException
    {
        return DateFormat.getDateInstance( DateFormat.DEFAULT, locale ).parse( date );
    }

    public static String formatAsLocalizedTime( Date date, Locale locale )
    {
        return DateFormat.getTimeInstance( DateFormat.DEFAULT, locale ).format( date );
    }

    public static Date parseLocalizedTime( String date, Locale locale ) throws ParseException
    {
        return DateFormat.getTimeInstance( DateFormat.DEFAULT, locale ).parse( date );
    }

}
