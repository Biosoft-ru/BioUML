package ru.biosoft.bsa.analysis;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysis.aggregate.NumericAggregatorEditor;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.trackutil.TrackPropertiesMultiSelector;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author lan
 *
 */
public class FilterTrackAnalysisParametersBeanInfo extends BeanInfoEx2<FilterTrackAnalysisParameters>
{
    public FilterTrackAnalysisParametersBeanInfo()
    {
        super(FilterTrackAnalysisParameters.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(DataElementPathEditor.registerInput("inputTrack", beanClass, Track.class));
        add(DataElementPathEditor.registerInput("filterTrack", beanClass, Track.class));
        add("maxDistance");
        add( "fieldNames", InputTrackPropertiesMultiSelector.class );
        addExpert( "selectedFilterFieldNames", FilterTrackPropertiesMultiSelector.class );
        addExpert( "ignoreNaNInAggregator" );
        property( "aggregator" ).expert().simple().editor( NumericAggregatorEditor.class ).add();
        property( "mode" ).tags( FilterTrackAnalysisParameters.MODES ).add();
        add(OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("outputTrack", beanClass, SqlTrack.class), "$inputTrack$ filtered"));
    }

    public static class InputTrackPropertiesMultiSelector extends TrackPropertiesMultiSelector
    {
        @Override
        protected DataElementPath getTrackPath()
        {
            return ( (FilterTrackAnalysisParameters)getBean() ).getInputTrack();
        }
        @Override
        protected DataElementPath getSkipTrackPath()
        {
            return null;
        }
    }
    public static class FilterTrackPropertiesMultiSelector extends TrackPropertiesMultiSelector
    {
        @Override
        protected DataElementPath getTrackPath()
        {
            return ( (FilterTrackAnalysisParameters)getBean() ).getFilterTrack();
        }

        @Override
        protected DataElementPath getSkipTrackPath()
        {
            return ( (FilterTrackAnalysisParameters)getBean() ).getInputTrack();
        }
    }
}
