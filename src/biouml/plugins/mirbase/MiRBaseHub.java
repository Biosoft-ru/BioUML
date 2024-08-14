package biouml.plugins.mirbase;

import java.util.Properties;

import biouml.plugins.ensembl.tabletype.EntrezGeneTableType;
import biouml.plugins.mirbase.MiRBaseName;

import ru.biosoft.access.biohub.SQLBasedHub;

public class MiRBaseHub extends SQLBasedHub
{
    private final Matching[] matchings = new Matching[] {
            new Matching( MiRBaseName.class, EntrezGeneTableType.class, true, 0.8 ),
            new Matching( EntrezGeneTableType.class, MiRBaseName.class, false, 0.8 ),
            new Matching( MiRBaseStemLoopMiRNA.class, EntrezGeneTableType.class, true, 0.8 ),
            new Matching( EntrezGeneTableType.class, MiRBaseStemLoopMiRNA.class, false, 0.8 ),
            new Matching( MiRBaseMatureMiRNA.class, MiRBaseStemLoopMiRNA.class, true, 0.8 ),
            new Matching( MiRBaseStemLoopMiRNA.class, MiRBaseMatureMiRNA.class, false, 0.8 ),
            new Matching( MiRBaseMixture.class, MiRBaseName.class, true, 0.8 ),
            new Matching( MiRBaseAccession.class, MiRBaseName.class, true, 0.8 ),
            new Matching( MiRBaseName.class, MiRBaseAccession.class, false, 0.8 ),
            
    };
    
    public MiRBaseHub(Properties properties)
    {
        super( properties );
    }

    @Override
    protected Matching[] getMatchings()
    {
        return matchings;
    }
}
