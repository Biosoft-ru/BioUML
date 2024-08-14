package biouml.plugins.reactome.biohub;

import java.util.Objects;
import biouml.plugins.reactome.ReactomeIDMatcher;

public class ReactomeElement
{
    private final String accession;

    public ReactomeElement(String accession)
    {
        if( !ReactomeIDMatcher.matches( accession ) )
        {
            if( accession == null )
                accession = "null";
            throw new IllegalArgumentException( "Given accession: '" + accession + "' is not valid REACTOME ID." );
        }
        this.accession = accession;
    }

    public String getAccession()
    {
        return accession;
    }

    @Override
    public String toString()
    {
        return getAccession();
    }

    @Override
    public int hashCode()
    {
        return accession.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if( this == obj )
            return true;
        if( obj == null || getClass() != obj.getClass() )
            return false;
        ReactomeElement objAsRI = (ReactomeElement)obj;
        return Objects.equals( accession, objAsRI.accession );
    }
}
