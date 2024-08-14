package biouml.standard.diagram;

import one.util.streamex.StreamEx;

public enum EquationStyle
{
    SIMPLE, FULL;

    public final static String SIMPLE_NAME = "Simple";
    public final static String FULL_NAME = "Full";

    /**
     * Get {@link String} for current object value
     */
    @Override
    public String toString()
    {
        if (this.equals(SIMPLE))
            return SIMPLE_NAME;
        
        return FULL_NAME;
    }

    /**
     * Get {@link PortOrientation} object by {@link String}
     */
    public static EquationStyle fromString(String value)
    {
        if( value != null )
        {
            if( value.equals(SIMPLE_NAME) )
            {
                return SIMPLE;
            }
            else if( value.equals(FULL_NAME) )
            {
                return FULL;
            }
        }
        return FULL;//default
    }
    
    public static String[] getAvailableTags()
    {
        return StreamEx.of(values()).map( Object::toString ).toArray( String[]::new );
    }
}
