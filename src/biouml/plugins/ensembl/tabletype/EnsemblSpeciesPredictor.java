package biouml.plugins.ensembl.tabletype;

public interface EnsemblSpeciesPredictor
{
    default String predict(String[] ids)
    {
        String result = "Unspecified";
        int homoCount = 0;
        int mouseCount = 0;
        int ratCount = 0;
        for( String id : ids )
        {
            if( id == null || id.isEmpty() )
                continue;
            if( id.startsWith( "ENSMUS" ) )
                mouseCount++;
            else if( id.startsWith( "ENSRNO" ) )
                ratCount++;
            else if( id.startsWith( "ENS" ) && id.length() > 6 )
            {
                char d1 = id.charAt( 4 );
                char d2 = id.charAt( 5 );
                if( d1 >= '0' && d1 <= '9' && d2 >= '0' && d2 <= '9' )
                    homoCount++;
            }
        }
        //TODO: filter through available species
        if( homoCount > 0 && homoCount >= mouseCount && homoCount >= ratCount )
            result = "Homo sapiens";
        else if( mouseCount > 0 && mouseCount >= ratCount )
            result = "Mus musculus";
        else if( ratCount > 0 )
            result = "Rattus norvegicus";
        return result;
    }
}
