package biouml.plugins.riboseq.riboseqanalysis;

import ru.biosoft.bsa.BAMTrack;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.util.bean.BeanInfoEx2;

public class RiboSeqParametersBeanInfo extends BeanInfoEx2<RiboSeqParameters>
{
    public RiboSeqParametersBeanInfo()
    {
        super( RiboSeqParameters.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        property( "inputPath" ).inputElement( BAMTrack.class ).add();

        add( "minNumberSites" );
        add( "maxLengthCluster" );

        property( "outputPath" ).outputElement( SqlTrack.class ).auto( "$inputPath$ filtered" ).add();
    }
}
