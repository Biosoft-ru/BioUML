package ru.biosoft.bsa.analysis.createsitemodel;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.DynamicProperty;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Const;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.TranscriptionFactor;
import ru.biosoft.bsa.transformer.SiteModelTransformer;
import ru.biosoft.jobcontrol.SubFunctionJobControl;
import ru.biosoft.table.TableDataCollection;

/**
 * @author lan
 *
 */
@ClassIcon("resources/filter_profile.gif")
public class FilterProfileByGeneSet extends AnalysisMethodSupport<FilterProfileByGeneSetParameters>
{
    public FilterProfileByGeneSet(DataCollection<?> origin, String name)
    {
        super(origin, name, new FilterProfileByGeneSetParameters());
    }

    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        Application.getPreferences().add(new DynamicProperty(Const.LAST_PROFILE_PREFERENCE, String.class, parameters.getProfile().toString()));
        log.info("Gathering profile factor types...");
        boolean warnings = false;
        final DataCollection<SiteModel> profile = parameters.getProfile().getDataCollection(SiteModel.class);
        final Map<ReferenceType, Set<String>> mapping = new IdentityHashMap<>();
        jobControl.pushProgress(0, 20);
        jobControl.forCollection(profile.getNameList(), modelName -> {
            try
            {
                SiteModel model = profile.get(modelName);
                for(TranscriptionFactor factor: model.getBindingElement().getFactors())
                {
                    if((factor.getSpeciesName() == null || factor.getSpeciesName().equals(parameters.getSpecies().getLatinName()))
                            && !mapping.containsKey(factor.getType()))
                    {
                        mapping.put(factor.getType(), new HashSet<String>());
                    }
                }
            }
            catch( Exception e )
            {
            }
            return true;
        });
        jobControl.popProgress();
        if(jobControl.isStopped()) return null;
        if(mapping.isEmpty())
        {
            log.warning("No applicable factor types detected: result will be empty");
            log.warning("Please check input profile and species");
            warnings = true;
        }

        jobControl.pushProgress(20, 70);
        TableDataCollection table = parameters.getTable().getDataElement(TableDataCollection.class);
        final String[] inputList = table.names().toArray( String[]::new );
        final Properties inputProperties = BioHubSupport.createProperties( parameters.getSpecies().getLatinName(), table.getReferenceType() );
        jobControl.forCollection(mapping.keySet(), type -> {
            log.info("Matching gene set to "+type);
            Properties outputProperties = BioHubSupport.createProperties( parameters.getSpecies(), type );
            Set<String> references = BioHubRegistry.getReferencesFlat(inputList, inputProperties, outputProperties, new SubFunctionJobControl(jobControl));
            if(references != null)
            {
                mapping.get(type).addAll(references);
            }
            return true;
        });
        jobControl.popProgress();
        if(jobControl.isStopped()) return null;
        int nRefs = 0;
        for(Set<String> references: mapping.values())
            nRefs+=references.size();
        if(nRefs == 0 && !warnings)
        {
            warnings = true;
            log.warning("Nothing was matched: empty result will be created");
            log.warning("Please check the type of an input table and the species parameter");
        }

        log.info("Generating result...");
        jobControl.pushProgress(70, 100);
        parameters.getOutput().remove();

        final DataCollection<SiteModel> result = SiteModelTransformer.createCollection(parameters.getOutput());
        final String speciesName = parameters.getSpecies().getLatinName();
        jobControl.forCollection(profile.getNameList(), modelName -> {
            try
            {
                SiteModel model = profile.get(modelName);
                for(TranscriptionFactor factor: model.getBindingElement().getFactors())
                {
                    if((factor.getSpeciesName() == null || factor.getSpeciesName().contains(speciesName))
                            && mapping.containsKey(factor.getType()) && mapping.get(factor.getType()).contains(factor.getName()))
                    {
                        result.put(model.clone(result, model.getName()));
                        break;
                    }
                }
            }
            catch( Exception e )
            {
            }
            return true;
        });
        if(result.isEmpty() && !warnings)
        {
            log.warning("There were no matches of in the reference profile: please check profile and input genes table.");
        }
        if(jobControl.isStopped())
        {
            result.getCompletePath().remove();
            return null;
        }
        CollectionFactoryUtils.save(result);
        return result;
    }
}
