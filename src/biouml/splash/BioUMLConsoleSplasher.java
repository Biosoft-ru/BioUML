package biouml.splash;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class BioUMLConsoleSplasher
{
    private static final Logger log = Logger.getLogger( BioUMLConsoleSplasher.class.getName() );

    private static final char[][] appBioUMLName = new char[][] {
            {' ', '_', '_', '_', '_', '_', '_', ' ', '_', '_', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', '_', '_', '_', '_', '_', '_',
                    '_', ' ', '_', '_', '_', '_', '_', '_', '_', ' ', '_', '_', '_', '_', '_', ' ', ' ', ' '},
            {'|', ' ', ' ', ' ', '_', '_', ' ', '\\', '_', '_', '|', '.', '-', '-', '-', '-', '-', '.', '|', ' ', ' ', ' ', '|', ' ', ' ',
                    ' ', '|', ' ', ' ', ' ', '|', ' ', ' ', ' ', '|', ' ', ' ', ' ', ' ', ' ', '|', '_', ' '},
            {'|', ' ', ' ', ' ', '_', '_', ' ', '<', ' ', ' ', '|', '|', ' ', ' ', '_', ' ', ' ', '|', '|', ' ', ' ', ' ', '|', ' ', ' ',
                    ' ', '|', ' ', ' ', ' ', ' ', ' ', ' ', ' ', '|', ' ', ' ', ' ', ' ', ' ', ' ', ' ', '|'},
            {'|', '_', '_', '_', '_', '_', '_', '/', '_', '_', '|', '|', '_', '_', '_', '_', '_', '|', '|', '_', '_', '_', '_', '_', '_',
                    '_', '|', '_', '_', '|', '_', '|', '_', '_', '|', '_', '_', '_', '_', '_', '_', '_', '|'}
    };

    private static final int indent = 15;

    public static void main(String[] args)
    {
        printSplash();
    }

    public static void printSplash()
    {
        String version = readVersion();
        List<VersionElement> versionElements = parseVersion( version );
        int versionSize = versionElements.stream().mapToInt( VersionElement::getShift ).sum();

        int linesNumber = appBioUMLName.length;
        int appNameLenth = appBioUMLName[0].length;
        int resultSize = indent + appNameLenth + 3 + versionSize;

        char[][] result = new char[linesNumber][resultSize];
        char[] delim = new char[resultSize + indent];

        for( int i = 0; i < linesNumber; i++ )
        {
            Arrays.fill( result[i], ' ' );
            Arrays.fill( delim, '_' );
            for( int j = 0; j < appNameLenth; j++ )
                result[i][j + indent] = appBioUMLName[i][j];
        }
        int shift = indent + appNameLenth + 2;
        for( VersionElement ve : versionElements )
            shift = ve.addElement( result, shift );

        StringBuilder sb = new StringBuilder();
        sb.append( '\n' ).append( delim ).append( '\n' ).append( delim ).append( '\n' ).append( delim ).append( '\n' ).append( '\n' )
                .append( '\n' ).append( '\n' );

        for( int i = 0; i < linesNumber; i++ )
            sb.append( result[i] ).append( '\n' );

        sb.append( '\n' ).append( '\n' ).append( '\n' ).append( delim ).append( '\n' ).append( delim ).append( '\n' ).append( delim )
                .append( '\n' );

        log.info( sb.toString() );
    }

    private static String readVersion()
    {
        try( InputStream is = BioUMLConsoleSplasher.class.getResourceAsStream( "version.txt" );
                BufferedReader br = new BufferedReader( new InputStreamReader( is ) ) )
        {
            String line = br.readLine();
            if( line != null )
                return line.trim();
        }
        catch( Exception e )
        {
        }
        return "";
    }

    private static List<VersionElement> parseVersion(String version)
    {
        List<VersionElement> result = new ArrayList<>();
        for( int i = 0; i < version.length(); i++ )
        {
            VersionElement ve = null;
            switch( version.charAt( i ) )
            {
                case '1':
                    ve = VersionElement.V_ONE;
                    break;
                case '2':
                    ve = VersionElement.V_TWO;
                    break;
                case '3':
                    ve = VersionElement.V_THREE;
                    break;
                case '4':
                    ve = VersionElement.V_FOUR;
                    break;
                case '5':
                    ve = VersionElement.V_FIVE;
                    break;
                case '6':
                    ve = VersionElement.V_SIX;
                    break;
                case '7':
                    ve = VersionElement.V_SEVEN;
                    break;
                case '8':
                    ve = VersionElement.V_EIGHT;
                    break;
                case '9':
                    ve = VersionElement.V_NINE;
                    break;
                case '0':
                    ve = VersionElement.V_ZERO;
                    break;
                case '.':
                    ve = VersionElement.V_DOT;
                    break;
                default:
                    //ignore all other chars for now
                    break;
            }
            if( ve != null )
                result.add( ve );
        }
        return result;
    }

}
