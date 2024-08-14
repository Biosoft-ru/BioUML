package biouml.plugins.antimony._test;

public class AntimonyTestUtil
{
    public static String fixEOL(String arg)
    {
        return arg.replaceAll( "\\r\\n", "\n" );
    }

    public static String removeComments(String arg)
    {
        return arg.replaceAll( "//.*+", "" );
    }

    public static String removeExcessiveLines(String arg)
    {
        return arg.replaceAll( "\\n[\\s\\n]*", "\n" );
    }

    public static String clean(String arg)
    {
        return removeExcessiveLines( removeComments( fixEOL(arg) ));
    }
}