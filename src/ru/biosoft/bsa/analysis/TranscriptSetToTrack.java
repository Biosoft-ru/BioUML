package ru.biosoft.bsa.analysis;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.bsa.TrackUtils;

@ClassIcon("resources/TranscriptSetToTrack.gif")
public class TranscriptSetToTrack extends ExtractPromoters<TranscriptSetToTrack.Parameters>
{

    public TranscriptSetToTrack(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    @Override
    protected DataCollection<?> getEnsemblDataCollection()
    {
        return TrackUtils.getTranscriptsCollection( parameters.getSpecies(), parameters.getDestPath() );
    }

    @SuppressWarnings ( "serial" )
    public static class Parameters extends ExtractPromotersParameters
    {
    }

    public static class ParametersBeanInfo extends ExtractPromotersParametersBeanInfo<Parameters>
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }

        @Override
        protected Class<? extends ReferenceType> getSourceReferenceType()
        {
            // Note: indirect link to EnsemblGeneTableType, because we cannot refer here to this class directly
            // as it will imply dependency to biouml.plugins.ensembl
            // TODO: extract site search analyzes to separate plugin and make proper dependencies
            return ReferenceTypeRegistry.getReferenceType("Transcripts: Ensembl").getClass();
        }
    }

   
}
