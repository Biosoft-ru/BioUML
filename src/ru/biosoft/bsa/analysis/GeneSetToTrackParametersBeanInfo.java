package ru.biosoft.bsa.analysis;

import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;

public class GeneSetToTrackParametersBeanInfo extends ExtractPromotersParametersBeanInfo<GeneSetToTrackParameters>
{
    public GeneSetToTrackParametersBeanInfo()
    {
        super( GeneSetToTrackParameters.class );
    }
    
    @Override
    protected Class<? extends ReferenceType> getSourceReferenceType()
    {
        // Note: indirect link to EnsemblGeneTableType, because we cannot refer here to this class directly
        // as it will imply dependency to biouml.plugins.ensembl
        // TODO: extract site search analyzes to separate plugin and make proper dependencies
        return ReferenceTypeRegistry.getReferenceType("Genes: Ensembl").getClass();
    }
}
