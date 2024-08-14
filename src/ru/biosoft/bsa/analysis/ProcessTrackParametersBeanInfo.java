package ru.biosoft.bsa.analysis;

import ru.biosoft.bsa.SequenceCollection;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.util.bean.BeanInfoEx2;

public class ProcessTrackParametersBeanInfo extends BeanInfoEx2<ProcessTrackParameters>
{
    public ProcessTrackParametersBeanInfo()
    {
        super( ProcessTrackParameters.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        property( "sourcePath" ).inputElement( Track.class ).add();
        property( "sequences" ).inputElement( SequenceCollection.class ).add();
        property( "shrinkMode" ).tags( ProcessTrackParameters.NO_SHRINK, ProcessTrackParameters.SHRINK_TO_START,
                ProcessTrackParameters.SHRINK_TO_CENTER, ProcessTrackParameters.SHRINK_TO_END, ProcessTrackParameters.SHRINK_TO_SUMMIT ).add();
        property( "enlargeStart" ).add();
        property( "enlargeEnd" ).add();
        property( "mergeOverlapping" ).add();
        property( "removeSmallSites" ).add();
        property( "minimalSize" ).add();
        property( "destPath" ).outputElement( SqlTrack.class ).auto( "$sourcePath$ processed" ).add();
    }
}
