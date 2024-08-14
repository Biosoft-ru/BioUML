package biouml.plugins.sbgn;

import java.beans.IntrospectionException;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

public class PhenotypePropertiesBeanInfo extends BeanInfoEx2<PhenotypeProperties>
{
    public PhenotypePropertiesBeanInfo()
    {
        super(PhenotypeProperties.class);
    }
    
    @Override
    public void initProperties() throws IntrospectionException
    {
        add( "name" );
        add( "properties" );
        add( "nodeNames", PhenotypeEditor.class );
    }
    
    public static class PhenotypeEditor extends GenericMultiSelectEditor
    {
        @Override
        public String[] getAvailableValues()
        {
            return ((PhenotypeProperties)getBean()).getAvailableNames();
        }
    }
}