package biouml.plugins.ensembl.homology;

import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.BioHubRegistry.MatchingStep;
import ru.biosoft.analysis.TableConverter;
import ru.biosoft.analysiscore.AnalysisParameters;
import biouml.standard.type.Species;

/**
 * Table converter via homology
 * @author lan
 */
public class HomologyMapping extends TableConverter
{
    public HomologyMapping(DataCollection<?> origin, String name)
    {
        super(origin, name, new HomologyMappingParameters());
    }

    @Override
    public MatchingStep[] getMatchingPlan() throws Exception
    {
        Species inputSpecies = getParameters().getSpecies();
        Species outputSpecies = getParameters().getTargetSpecies();
        if( inputSpecies == outputSpecies )
            return super.getMatchingPlan();
        Properties inputProperties = BioHubSupport.createProperties( inputSpecies, getParameters().getSourceTypeObject() );
        Properties outputProperties = BioHubSupport.createProperties( outputSpecies, getParameters().getTargetTypeObject() );
        MatchingStep[] matchingPath = BioHubRegistry.getMatchingPath(inputProperties, outputProperties);
        if( matchingPath == null )
            throw new Exception("Unable to convert '" + getParameters().getSourceTypeObject() + "' to '" + getParameters().getTargetTypeObject()
                    + "': check parameters.");
        log.info("Matching plan:");
        log.info("* " + getParameters().getSourceTypeObject() + " (" + inputSpecies.getLatinName() + ")");
        for( MatchingStep step : matchingPath )
        {
            log.info("* " + step.getType().getDisplayName() + " (" + step.getProperties().getProperty(BioHub.SPECIES_PROPERTY) + ")");
        }
        return matchingPath;
    }
    @Override
    protected Species getOutputSpecies()
    {
        return ( (HomologyMappingParameters)parameters ).getTargetSpecies();
    }

    @Override
    public void setParameters(AnalysisParameters params)
    {
        if( ! ( params instanceof HomologyMappingParameters ) )
            throw new IllegalArgumentException("Invalid parameters");
        super.setParameters(params);
    }

    @Override
    public HomologyMappingParameters getParameters()
    {
        return (HomologyMappingParameters)parameters;
    }
}
