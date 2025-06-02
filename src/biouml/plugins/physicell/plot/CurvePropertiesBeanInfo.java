package biouml.plugins.physicell.plot;

import ru.biosoft.util.bean.BeanInfoEx2;

public class CurvePropertiesBeanInfo extends BeanInfoEx2<CurveProperties>
{
    public CurvePropertiesBeanInfo()
    {
        super( CurveProperties.class );
    }

    @Override
    public void initProperties()
    {
        add("name");
        property( "cellType" ).tags( bean -> bean.getCellTypes() ).add();
        property( "signal" ).tags( bean -> bean.getSignals() ).add();
        addWithTags( "relation", CurveProperties.getRelations() );
        add( "value" );
    }
}