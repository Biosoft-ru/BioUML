package biouml.plugins.physicell;

import java.beans.IntrospectionException;

import ru.biosoft.util.bean.BeanInfoEx2;

public class CellDefinitionPropertiesBeanInfo extends BeanInfoEx2<CellDefinitionProperties>
{
    public CellDefinitionPropertiesBeanInfo()
    {
        super( CellDefinitionProperties.class );
    }

    @Override
    public void initProperties() throws IntrospectionException
    {
        add( "name" );
        add( "initialNumber" );

        addHidden( "motilityProperties" );
        addHidden( "mechanicsProperties" );
        addHidden( "volumeProperties" );
        addHidden( "geometryProperties" );
        addHidden( "cycleProperties" );
        addHidden( "deathProperties" );
        addHidden( "functionsProperties" );
        addHidden( "secretionsProperties" );
        addHidden( "interactionsProperties" );
        addHidden( "transformationsProperties" );
        addHidden( "customDataProperties" );
        addHidden( "intracellularProperties" );
        addHidden( "rulesProperties" );
    }
}