package ru.biosoft.bsa.analysis;

import ru.biosoft.bsa.Track;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author lan
 *
 */
public class TrackCoverageAnalysisParametersBeanInfo extends BeanInfoEx2<TrackCoverageAnalysisParameters>
{
    public TrackCoverageAnalysisParametersBeanInfo()
    {
        super(TrackCoverageAnalysisParameters.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        property( "track" ).inputElement( Track.class ).add();
        add("sequences");
        add("window");
        add("step");
        add("outputEmptyIntervals");
        property( "output" ).outputElement( TableDataCollection.class ).auto( "$track$ coverage" ).add();
    }
}
