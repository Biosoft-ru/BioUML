package biouml.plugins.sbml;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import ru.biosoft.util.ApplicationUtils;
import ru.biosoft.util.TextUtil;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.Specie;

public class SbmlTypeMapper
{
    public static final String MAP_FILE_NAME = "types.map";
    static SbmlTypeMapper typeMapper = null;

    public synchronized static SbmlTypeMapper getInstance()
    {
        if( typeMapper == null )
        {
            typeMapper = new SbmlTypeMapper();
        }
        return typeMapper;
    }

    protected Map<String, String> typeMap = new HashMap<>();

    public SbmlTypeMapper()
    {
        try
        {
            URL url = ApplicationUtils.getResourceURL( "biouml.plugins.sbml", "types.map" );
            try( BufferedReader br = new BufferedReader( new InputStreamReader( url.openStream() ) ) )
            {
                String line = null;
                while( ( line = br.readLine() ) != null )
                {
                    String[] values = TextUtil.split( line, ' ' );
                    if( values.length >= 2 )
                    {
                        typeMap.put( values[0], values[1] );
                    }
                }
            }
        }
        catch( Throwable t )
        {
        }
    }

    public String getSpecieType(Specie specie)
    {
        if( specie.getDatabaseReferences() != null )
        {
            for( DatabaseReference dr : specie.getDatabaseReferences() )
            {
                for( Map.Entry<String, String> entry : typeMap.entrySet() )
                {
                    if( dr.getDatabaseName().matches(entry.getKey()) )
                    {
                        return entry.getValue();
                    }
                }
            }
        }
        return null;
    }
}
