package biouml.plugins.reactome;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReactomeIDMatcher
{
    private static final Pattern OLD_ID_PATTERN = Pattern.compile( "REACT_\\d+(\\.\\d+|)" );
    private static final Pattern ID_PATTERN = Pattern.compile( "R-[A-Z]{3}-\\d+(-\\d+)?(\\.\\d+)?" );

    public static boolean matches(String acc)
    {
        if( acc == null )
            return false;
        Matcher matcher = ID_PATTERN.matcher( acc );
        if( !matcher.matches() )
        {
            Matcher oldMatcher = OLD_ID_PATTERN.matcher( acc );
            if( !oldMatcher.matches() )
                return false;
        }
        return true;
    }
}
