package biouml.plugins.physicell;

import biouml.model.Diagram;
import ru.biosoft.util.bean.BeanInfoEx2;

public class CyclePropertiesBeanInfo extends BeanInfoEx2<CycleProperties>
{
    public CyclePropertiesBeanInfo()
    {
        super( CycleProperties.class );
    }

    @Override
    public void initProperties()
    {
        try
        {
            property( "deathModel" ).hidden().add();
            property( "cycleName" ).tags( bean -> bean.getAvailableModels() ).structureChanging().add();
            property( "customCycle" ).readOnly( "isDefaultCycle" ).inputElement( Diagram.class ).add();
            property( "phases" ).fixedLength()
                    .childDisplayName( beanClass.getMethod( "getPhaseName", new Class[] {Integer.class, Object.class} ) ).add();
            property( "transitions" ).fixedLength()
                    .childDisplayName( beanClass.getMethod( "getTransitionName", new Class[] {Integer.class, Object.class} ) ).add();
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }
}