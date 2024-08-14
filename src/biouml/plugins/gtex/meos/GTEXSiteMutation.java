package biouml.plugins.gtex.meos;

import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.analysis.maos.SiteMutation;
import ru.biosoft.bsa.analysis.maos.Variation;
import ru.biosoft.bsa.analysis.maos.coord_mapping.CoordinateMapping;

public class GTEXSiteMutation extends SiteMutation
{
    public String type;
    

    public GTEXSiteMutation(String type, SiteModel model, Sequence refSeq, int refPos, double refScore, double refPValue, Sequence altSeq, int altPos,
            double altScore, double altPValue, double scoreDiff, double pValueLogFC, CoordinateMapping ref2alt, CoordinateMapping alt2ref,
            Variation[] variations)
    {
        super( model, refSeq, refPos, refScore, refPValue, altSeq, altPos, altScore, altPValue, scoreDiff, pValueLogFC, ref2alt, alt2ref,
                variations );
        this.type = type;
    }

}
