package biouml.plugins.physicell.ode;

import java.beans.IntrospectionException;
import java.util.stream.Stream;

import ru.biosoft.util.bean.BeanInfoEx2;

public class PhenotypeVariableBeanInfo extends BeanInfoEx2<PhenotypeVariable>
{
    public PhenotypeVariableBeanInfo()
    {
        super( PhenotypeVariable.class );
    }

    @Override
    public void initProperties() throws IntrospectionException
    {
        property( "varName" ).tags( bean -> bean.getVariableNames().stream() ).add();
        property( "phenotypeName" ).tags( bean -> bean.getPhenotypeNames().stream() ).add();
        property( "type" ).tags( bean -> Stream.of( bean.getTypes() ) ).add();
    }
}