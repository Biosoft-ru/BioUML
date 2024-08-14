package biouml.plugins.bionetgen.diagram;

/**
 * Utility methods for fast (compared to regexps) extraction of Bionetgen molecule and components name parts
 * @author lan
 */
public class ParseUtils
{
    private static final byte[] IDENTIFIER_SYMBOLS = new byte[128];
    
    static
    {
        for(short b = 0; b<IDENTIFIER_SYMBOLS.length; b++)
        {
            if(b == '-' || b == '_' || (b >='A' && b<='Z') || (b>='a' && b<='z') || (b>='0' && b<='9'))
                IDENTIFIER_SYMBOLS[b] = 1;
            else
                IDENTIFIER_SYMBOLS[b] = 0;
        }
    }

    /**
     * Extracts the name part from the molecule definition
     * @param moleculeDefinition molecule definition to parse
     * @return String array of 2 elements containing [name part, the rest] if parsing was successful
     * null if moleculeDefinition is invalid
     * 
     * Equivalent to regexp "([\\w\\-]+)(.*)"
     */
    public static String[] parseName(String moleculeDefinition)
    {
        int i = 0, l = moleculeDefinition.length();
        for( ; i < l; i++ )
        {
            char c = moleculeDefinition.charAt(i);
            if( c >= IDENTIFIER_SYMBOLS.length || IDENTIFIER_SYMBOLS[c] == 0 )
            {
                if( i == 0 )
                    return null;
                break;
            }
        }
        return new String[] {moleculeDefinition.substring(0, i), moleculeDefinition.substring(i)};
    }
    
    /**
     * Extract state parts from the state definition
     * @param stateDefinition state definition to parse
     * @return String array of 3 elements containing [state type, state, rest of the string] if parsing was successful
     * null if stateDefinition is invalid
     * Equivalent to regexp "([~%!@])([\\w\\-]+|\\+|\\?)(.*)"
     */
    public static String[] parseState(String stateDefinition)
    {
        if(stateDefinition.length() < 2) return null;
        char stateType = stateDefinition.charAt(0);
        if(stateType != '~' && stateType != '%' && stateType != '!' && stateType != '@') return null;
        char stateStart = stateDefinition.charAt(1);
        if(stateStart == '+' || stateStart == '?')
            return new String[] {String.valueOf(stateType), String.valueOf(stateStart), stateDefinition.substring(2)};
        int i = 1, l = stateDefinition.length();
        for( ; i < l; i++ )
        {
            char c = stateDefinition.charAt(i);
            if( c >= IDENTIFIER_SYMBOLS.length || IDENTIFIER_SYMBOLS[c] == 0 )
            {
                if( i == 1 )
                    return null;
                break;
            }
        }
        return new String[] {String.valueOf(stateType), stateDefinition.substring(1, i), stateDefinition.substring(i)};
    }
}
