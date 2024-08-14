package biouml.plugins.expasy;

import java.util.Properties;

import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.biohub.SQLBasedHub;

public class ExPASyHub extends SQLBasedHub
{

    public ExPASyHub(Properties properties)
    {
        super( properties );
    }
    
    private final Matching[] matchings = new Matching[] {
            new Matching( EnzymeExpasyType.class, ReferenceTypeRegistry.getReferenceType( "Proteins: UniProt" ).getClass(), true, 0.4 ),
            new Matching( ReferenceTypeRegistry.getReferenceType( "Proteins: UniProt" ).getClass(), EnzymeExpasyType.class, false, 0.4 )};

    @Override
    protected Matching[] getMatchings()
    {
        return matchings;
    }

}
