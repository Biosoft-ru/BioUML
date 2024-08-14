package biouml.plugins.riboseq.ingolia;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.BAMTrack;
import ru.biosoft.util.bean.BeanInfoEx2;

public class CoreParametersBeanInfo extends BeanInfoEx2<CoreParameters>
{
    protected CoreParametersBeanInfo(Class<? extends CoreParameters> beanClass)
    {
        super( beanClass );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add( DataElementPathEditor.registerInputMulti( "bamFiles", beanClass, BAMTrack.class ) );
        add( "transcriptSet" );
        add( "transcriptOverhangs" );
        add( "strandSpecific" );
    }
}
