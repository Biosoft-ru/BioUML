package ru.biosoft.bsa.analysis;

import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author lan
 *
 */
public class FilterTrackByConditionParametersBeanInfo extends BeanInfoEx2<FilterTrackByConditionParameters>
{
    public FilterTrackByConditionParametersBeanInfo()
    {
        super(FilterTrackByConditionParameters.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        property( "inputTrack" ).inputElement( Track.class ).add();
        add("condition");
        property( "outputTrack" ).outputElement( SqlTrack.class ).auto( "$inputTrack$ filtered" ).add();
    }
}
