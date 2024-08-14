package biouml.plugins.reactome.imports;

import javax.annotation.CheckForNull;

public class RSpecies
{
    public static final String REACTOME_SPECIES_PROPERTY = "RSpecies";

    private final String name;
    private final long innerId;

    public RSpecies(String name, long innerID)
    {
        this.name = name;
        this.innerId = innerID;
    }

    public String getName()
    {
        return name;
    }

    public long getInnerId()
    {
        return innerId;
    }

    /**
     * Creates RSpecies object using given string
     * @param speciesStr RSpecies string representation
     * @return new RSpecies object for correct input string or <code>null</code> if string is incorrect
     */
    public static @CheckForNull RSpecies fromString(String speciesStr)
    {
        if( speciesStr == null )
            return null;
        String[] parts = speciesStr.split( ";" );
        if( parts.length != 2 )
            return null;
        try
        {
            return new RSpecies( parts[0], Long.parseLong( parts[1] ) );
        }
        catch( NumberFormatException e )
        {
            return null;
        }
    }

    @Override
    public String toString()
    {
        return name + ";" + innerId;
    }
}
