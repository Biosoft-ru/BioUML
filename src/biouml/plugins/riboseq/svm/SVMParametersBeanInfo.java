package biouml.plugins.riboseq.svm;

import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.util.bean.BeanInfoEx2;

public class SVMParametersBeanInfo extends BeanInfoEx2<SVMParameters>
{
    public SVMParametersBeanInfo()
    {
        super( SVMParameters.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        property( "inputYesTrack" ).inputElement( Track.class ).add();

        property( "inputNoTrack" ).inputElement( Track.class ).add();

        property( "inputUndefinedTrack" ).inputElement( Track.class ).add();

        property( "outputClassifiedYesTrack" ).outputElement( SqlTrack.class ).auto( "$inputYesTrack$ classified" ).add();
    }
}
