package ru.biosoft.bsa.analysis;

import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.util.bean.BeanInfoEx2;

public class GenomeCoverageParametersBeanInfo extends BeanInfoEx2<GenomeCoverageParameters>
{
    public GenomeCoverageParametersBeanInfo()
    {
        super(GenomeCoverageParameters.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        property( "inputTrack" ).inputElement( Track.class ).add();
        property( "regionsTrack" ).inputElement( Track.class ).add();

        addExpert( "siteLength" );

        property( "outputTrack" ).outputElement( SqlTrack.class ).auto( "$inputTrack$ coverage" ).add();
    }
}
