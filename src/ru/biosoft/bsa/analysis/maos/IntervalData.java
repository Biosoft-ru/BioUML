package ru.biosoft.bsa.analysis.maos;

import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.analysis.SequenceAccessor.CachedSequenceRegion;
import ru.biosoft.bsa.analysis.maos.coord_mapping.CoordinateMapping;
import ru.biosoft.bsa.analysis.maos.coord_mapping.InverseMapping;
import ru.biosoft.bsa.analysis.maos.coord_mapping.ReverseStrandMapping;

public class IntervalData
{
    protected final CachedSequenceRegion reference;
    protected final Sequence alternative;
    protected final Variation[] variations, invVariations;
    protected final CoordinateMapping ref2alt, alt2ref;
    protected final TSSCollection genes;
    
    public IntervalData(CachedSequenceRegion reference, Sequence alternative, Variation[] variations, Variation[] invVariations,
            CoordinateMapping ref2alt, CoordinateMapping alt2ref, TSSCollection genes)
    {
        this.reference = reference;
        this.alternative = alternative;
        this.variations = variations;
        this.invVariations = invVariations;
        this.ref2alt = ref2alt;
        this.alt2ref = alt2ref;
        this.genes = genes;
    }
    
    protected IntervalData(IntervalData that)
    {
        this( that.reference, that.alternative, that.variations, that.invVariations, that.ref2alt, that.alt2ref, that.genes );
    }
    
    public IntervalData getReverseComplement()
    {
        CachedSequenceRegion referenceRC = reference.getReverseComplement();
        SequenceRegion alternativeRC = SequenceRegion.getReversedSequence( alternative );
        
        Variation[] variationsRC = Variation.mapToRC(reference.getInterval(), variations, referenceRC.getStart(), reference.getAlphabet());
        Variation[] invVariationsRC =  Variation.invertVariations( variationsRC );
        
        ReverseStrandMapping ref2altRC = new ReverseStrandMapping( ref2alt, alternative.getInterval() );
        CoordinateMapping alt2refRC = new InverseMapping( alternativeRC.getInterval(), ref2altRC );

        TSSCollection genesRC = genes.getReverseComplement( reference.getInterval(), referenceRC.getStart() );
        
        return new IntervalData( referenceRC, alternativeRC, variationsRC, invVariationsRC, ref2altRC, alt2refRC, genesRC );
    }

    public Sequence getReference()
    {
        return reference;
    }

    public Sequence getAlternative()
    {
        return alternative;
    }

    public Variation[] getVariations()
    {
        return variations;
    }

    public Variation[] getInvertedVariations()
    {
        return invVariations;
    }

    public CoordinateMapping getRef2Alt()
    {
        return ref2alt;
    }

    public CoordinateMapping getAlt2Ref()
    {
        return alt2ref;
    }

    public TSSCollection getGenes()
    {
        return genes;
    }
}
