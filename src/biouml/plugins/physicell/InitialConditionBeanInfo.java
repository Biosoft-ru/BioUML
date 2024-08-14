package biouml.plugins.physicell;

import ru.biosoft.access.script.ScriptDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class InitialConditionBeanInfo extends BeanInfoEx2<InitialCondition>
{
    public InitialConditionBeanInfo()
    {
        super( InitialCondition.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add( "customCondition" );
        property( "customConditionCode" ).hidden( "isDefaultCondition" ).inputElement( ScriptDataElement.class ).add();
        property( "customConditionTable" ).hidden( "isDefaultCondition" ).inputElement( TableDataCollection.class ).add();
    }
}