package biouml.plugins.ensembl.analysis.mutationeffect;

import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.analysis.maos.IntervalData;
import ru.biosoft.bsa.analysis.maos.TSSCollection;
import ru.biosoft.bsa.analysis.maos.Variation;
import ru.biosoft.bsa.analysis.maos.coord_mapping.CoordinateMapping;
import ru.biosoft.bsa.analysis.maos.coord_mapping.InverseMapping;
import ru.biosoft.bsa.analysis.maos.coord_mapping.ReverseStrandMapping;

public class IntervalDataBySNP extends IntervalData
{
    private final Sequence reference;

    public IntervalDataBySNP(Sequence reference, Sequence alternative, Variation[] variations, Variation[] invVariations,
            CoordinateMapping ref2alt, CoordinateMapping alt2ref, TSSCollection genes)
    {
        super( null, alternative, variations, invVariations, ref2alt, alt2ref, genes );
        this.reference = reference;
    }

    @Override
    public IntervalData getReverseComplement()
    {
        SequenceRegion referenceRC = SequenceRegion.getReversedSequence( reference );
        SequenceRegion alternativeRC = SequenceRegion.getReversedSequence( alternative );

        Variation[] variationsRC = Variation.mapToRC( reference.getInterval(), variations, referenceRC.getStart(),
                reference.getAlphabet() );
        Variation[] invVariationsRC = Variation.invertVariations( variationsRC );

        ReverseStrandMapping ref2altRC = new ReverseStrandMapping( ref2alt, alternative.getInterval() );
        CoordinateMapping alt2refRC = new InverseMapping( alternativeRC.getInterval(), ref2altRC );

        TSSCollection genesRC = genes.getReverseComplement( reference.getInterval(), referenceRC.getStart() );

        return new IntervalDataBySNP( referenceRC, alternativeRC, variationsRC, invVariationsRC, ref2altRC, alt2refRC, genesRC );
    }

    @Override
    public Sequence getReference()
    {
        return reference;
    }
}
