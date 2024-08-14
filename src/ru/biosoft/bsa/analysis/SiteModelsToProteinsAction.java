package ru.biosoft.bsa.analysis;

import java.util.List;

import one.util.streamex.StreamEx;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.bsa.SiteModelCollection;
import ru.biosoft.jobcontrol.JobControl;

/**
 * @author lan
 *
 */
@SuppressWarnings ( "serial" )
public class SiteModelsToProteinsAction extends BackgroundDynamicAction
{

    @Override
    public JobControl getJobControl(Object model, List<DataElement> selectedItems, Object properties) throws Exception
    {
        AnalysisMethod method = AnalysisMethodRegistry.getAnalysisMethod(SiteModelsToProteins.class);
        method.setParameters((AnalysisParameters)properties);
        return method.getJobControl();
    }

    public static class ActionProperties extends SiteModelsToProteinsParameters
    {
        public ActionProperties(DataCollection<?> dc, List<DataElement> selectedItems)
        {
            DataElementPath input = DataElementPath.create(dc);
            setSitesCollection(input);
            setModels(selectedItems == null ? null : StreamEx.of(selectedItems).map(DataElement::getName).toArray(String[]::new));
        }
    }

    public static class ActionPropertiesBeanInfo extends SiteModelsToProteinsParametersBeanInfo
    {
        public ActionPropertiesBeanInfo()
        {
            super(ActionProperties.class);
        }

        @Override
        public void initProperties() throws Exception
        {
            super.initProperties();
            findPropertyDescriptor("sitesCollection").setHidden(true);
        }
    }

    @Override
    public Object getProperties(Object model, List<DataElement> selectedItems)
    {
        return new ActionProperties((DataCollection<?>)model, selectedItems);
    }

    @Override
    public boolean isApplicable(Object model)
    {
        if( ! ( model instanceof DataCollection ) )
            return false;
        DataCollection<?> dc = (DataCollection<?>)model;
        String profilePath = dc.getInfo().getProperty(SiteSearchResult.PROFILE_PROPERTY);
        if( profilePath == null )
            return false;
        return DataCollectionUtils.checkPrimaryElementType(CollectionFactory.getDataCollection(profilePath), SiteModelCollection.class);
    }
}
