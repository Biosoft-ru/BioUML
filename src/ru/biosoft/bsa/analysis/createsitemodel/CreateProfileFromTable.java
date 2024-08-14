package ru.biosoft.bsa.analysis.createsitemodel;


import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.transformer.SiteModelTransformer;
import ru.biosoft.table.TableDataCollection;

/**
 * @author lan
 *
 */
@ClassIcon("resources/profile_from_table.gif")
public class CreateProfileFromTable extends AnalysisMethodSupport<CreateProfileFromTableParameters>
{
    public CreateProfileFromTable(DataCollection<?> origin, String name)
    {
        super(origin, name, new CreateProfileFromTableParameters());
    }

    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        final TableDataCollection table = parameters.getTable().getDataElement(TableDataCollection.class);
        final DataCollection<SiteModel> profile = parameters.getProfile().getDataCollection(SiteModel.class);
        Set<String> profileNames = profile.names().collect( Collectors.toSet() );
        List<String> names = table.names().filter( profileNames::contains ).collect( Collectors.toList() );
        if(names.size() < table.getSize())
        {
            if(names.size() == 0)
            {
                log.warning("No table rows match to profile site models: empty result will be created");
                log.warning("Check whether input table is correct");
            } else
            {
                log.info(names.size()+" of "+table.getSize()+" table rows matched to profile");
            }
        }
        
        final DataCollection<SiteModel> result = SiteModelTransformer.createCollection(parameters.getOutputProfile());
        final int index = table.getColumnModel().optColumnIndex(parameters.getThresholdsColumn());
        jobControl.forCollection(names, name -> {
            try
            {
                SiteModel siteModel = profile.get(name);
                SiteModel clone = siteModel.clone(result, name);
                if(index >= 0) clone.setThreshold(((Number)table.get(name).getValues()[index]).doubleValue());
                result.put(clone);
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Error adding "+name+" to profile: "+e.getMessage());
            }
            return true;
        });
        if(jobControl.isStopped())
        {
            result.getCompletePath().remove();
            return null;
        }
        CollectionFactoryUtils.save(result);
        return result;
    }
}
