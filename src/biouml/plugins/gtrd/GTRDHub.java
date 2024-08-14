package biouml.plugins.gtrd;

import java.util.Properties;

import biouml.plugins.ensembl.tabletype.EnsemblGeneTableType;
import biouml.plugins.ensembl.tabletype.UniprotProteinTableType;

import ru.biosoft.access.biohub.SQLBasedHub;

/**
 * Hub for Gene Transcription Regulation Database
 * Match TF ID to:
 *  - uniprot ID
 *  - TF isoforms
 *  - site model
 */
public class GTRDHub extends SQLBasedHub
{
    public GTRDHub(Properties properties)
    {
        super(properties);
    }

    private final Matching[] matchings = new Matching[] {new Matching(ProteinGTRDType.class, UniprotProteinTableType.class, true, 0.4),
            new Matching(ProteinGTRDType.class, EnsemblGeneTableType.class, true, 0.8),
            new Matching(ProteinGTRDType.class, IsoformGTRDType.class, true, 0.99),
            new Matching(ProteinGTRDType.class, SiteModelGTRDType.class, true, 0.99),
            new Matching(SiteModelGTRDType.class, ProteinGTRDType.class, false, 0.99),
            new Matching(IsoformGTRDType.class, UniprotProteinTableType.class, true, 0.4),
            new Matching(UniprotProteinTableType.class, ProteinGTRDType.class, false, 0.2),
            new Matching(EnsemblGeneTableType.class, ProteinGTRDType.class, false, 0.1),
            new Matching(IsoformGTRDType.class, ProteinGTRDType.class, false, 0.7),
            new Matching(UniprotProteinTableType.class, IsoformGTRDType.class, false, 0.1),
            new Matching(MatrixGTRDType.class, PeaksGTRDType.class, true, 0.8),
            new Matching(MatrixGTRDType.class, ClassGTRDType.class, false, 0.99)
    };

    @Override
    protected Matching[] getMatchings()
    {
        return matchings;
    }
}
