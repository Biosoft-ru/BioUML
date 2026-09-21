package biouml.plugins.wdl.nextflow.converter;

public class ConverterParameters
{
    String resultPath;
    String filePath = null;
    boolean showHelp;
    boolean showVersion;
    boolean showLicense;
    boolean showImage = false;

    public static String HELP_CMD = "--help";
    public static String SHORT_HELP_CMD = "-h";
    public static String VERSION_CMD = "--version";
    public static String SHORT_VERSION_CMD = "-v";
    public static String LICENSE_CMD = "--license";

    public static String IMAGE_CMD = "-i";

    public ConverterParameters(String ... args)
    {
        if( args.length == 0 )
        {
            System.out.println("Please specify WDL file as first argument. or use --help");
            return;
        }

        if( HELP_CMD.equals(args[0]) || SHORT_HELP_CMD.equals(args[0]) )
        {
            showHelp = true;
            return;
        }
        else if( VERSION_CMD.equals(args[0]) || SHORT_VERSION_CMD.equals(args[0]) )
        {
            showVersion = true;
            return;
        }
        else if( LICENSE_CMD.equals(args[0]) )
        {
            this.showLicense = true;
            return;
        }
        else
        {
            filePath = args[0];
        }

        for( int i = 1; i < args.length; i++ )
        {
            if( IMAGE_CMD.equals(args[i]) )
            {
                showImage = true;
            }
        }
    }
}