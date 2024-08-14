package biouml.plugins.enrichment;

import java.util.Properties;

import biouml.plugins.ensembl.tabletype.EnsemblGeneTableType;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.TableDataCollection;

/**
 * Helper functions
 * @author lan
 */
public abstract class FunctionalAnalysisSupport<T extends FunctionalClassificationParameters> extends AnalysisMethodSupport<T>
{
    public FunctionalAnalysisSupport(DataCollection<?> origin, String name, T parameters)
    {
        super(origin, name, parameters);
    }

    /**
     * Write additional metadata to the result
     * @param resTable
     * @throws Exception
     */
    protected void writeMetaData(TableDataCollection resTable) throws Exception
    {
        resTable.getInfo().getProperties().setProperty(DataCollectionUtils.SPECIES_PROPERTY, parameters.getSpecies().getLatinName());
        Properties matchingProperties = BioHubSupport.createProperties( parameters.getSpecies(),
                ReferenceTypeRegistry.getReferenceType( EnsemblGeneTableType.class ) );
        matchingProperties.setProperty(FunctionalHubConstants.FUNCTIONAL_CLASSIFICATION_RECORD, "true");
        Properties[] supportedMatching = parameters.getFunctionalHub().getSupportedMatching(matchingProperties);
        if(supportedMatching != null && supportedMatching.length > 0)
        {
            String typeProperty = supportedMatching[0].getProperty(BioHub.TYPE_PROPERTY);
            if(typeProperty != null)
                resTable.setReferenceType(typeProperty);
            else
            {
                String urlProperty = supportedMatching[0].getProperty(DataCollectionConfigConstants.URL_TEMPLATE);
                if(urlProperty != null)
                {
                    resTable.getInfo().getProperties().setProperty(DataCollectionConfigConstants.URL_TEMPLATE, urlProperty);
                }
            }
        }
        CollectionFactoryUtils.save(resTable);
    }
}
